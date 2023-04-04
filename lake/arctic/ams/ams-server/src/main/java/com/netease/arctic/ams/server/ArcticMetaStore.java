/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.netease.arctic.ams.api.ArcticTableMetastore;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.InvalidObjectException;
import com.netease.arctic.ams.api.MetaException;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.OptimizeManager;
import com.netease.arctic.ams.api.client.AmsServerInfo;
import com.netease.arctic.ams.api.client.ZookeeperService;
import com.netease.arctic.ams.api.properties.AmsHAProperties;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.config.ConfigFileProperties;
import com.netease.arctic.ams.server.config.Configuration;
import com.netease.arctic.ams.server.handler.impl.ArcticTableMetastoreHandler;
import com.netease.arctic.ams.server.handler.impl.OptimizeManagerHandler;
import com.netease.arctic.ams.server.model.Container;
import com.netease.arctic.ams.server.model.OptimizeQueueMeta;
import com.netease.arctic.ams.server.optimize.OptimizeCommitWorker;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.DDLTracerService;
import com.netease.arctic.ams.server.service.impl.DerbyService;
import com.netease.arctic.ams.server.service.impl.FileInfoCacheService;
import com.netease.arctic.ams.server.service.impl.OptimizeExecuteService;
import com.netease.arctic.ams.server.service.impl.RuntimeDataExpireService;
import com.netease.arctic.ams.server.utils.AmsUtils;
import com.netease.arctic.ams.server.utils.SecurityUtils;
import com.netease.arctic.ams.server.utils.ThreadPool;
import com.netease.arctic.ams.server.utils.UpdateTool;
import com.netease.arctic.ams.server.utils.YamlUtils;
import com.netease.arctic.utils.ConfigurationFileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ArcticMetaStore {
  public static final Logger LOG = LoggerFactory.getLogger(ArcticMetaStore.class);

  public static Configuration conf;
  private static JSONObject yamlConfig;
  private static TServer server;
  private static final List<Thread> residentThreads = new ArrayList<>();
  private static HighAvailabilityServices haService = null;
  private static final AtomicBoolean isLeader = new AtomicBoolean(false);
  private static final int checkLeaderInterval = 2000;

  private static volatile boolean shouldStop = false;

  public static void main(String[] args) throws Throwable {
    while (!shouldStop) {
      tryStartServer();
      Thread.sleep(500);
    }
  }

  public static LinkedHashMap getSystemSettingFromYaml() {
    JSONObject systemConfig = yamlConfig.getJSONObject(ConfigFileProperties.SYSTEM_CONFIG);
    LinkedHashMap<String, Object> config = new LinkedHashMap<String, Object>();

    systemConfig.put(ArcticMetaStoreConf.ARCTIC_HOME.key(), getArcticHome());
    String systemThriftPort = System.getProperty(ArcticMetaStoreConf.THRIFT_BIND_PORT.key());
    if (systemThriftPort == null) {
      systemConfig.put(
          ArcticMetaStoreConf.THRIFT_BIND_PORT.key(),
          systemConfig.getInteger(ArcticMetaStoreConf.THRIFT_BIND_PORT.key()));
    } else {
      systemConfig.put(ArcticMetaStoreConf.THRIFT_BIND_PORT.key(), Integer.parseInt(systemThriftPort));
    }

    validateConfig(systemConfig);
    config.putAll(systemConfig);

    //extension properties
    Map<String,String> extensionPro =
        yamlConfig.getObject(ConfigFileProperties.SYSTEM_EXTENSION_CONFIG, Map.class) == null ? new HashMap<>() :
            yamlConfig.getObject(ConfigFileProperties.SYSTEM_EXTENSION_CONFIG, Map.class);
    config.put(ArcticMetaStoreConf.SYSTEM_EXTENSION_PROPERTIES.key(), extensionPro);

    return config;
  }

  public static void validateConfig(JSONObject systemConfig) {
    if (!systemConfig.containsKey(ArcticMetaStoreConf.THRIFT_BIND_HOST_PREFIX.key())) {
      throw new RuntimeException("configuration " + ArcticMetaStoreConf.THRIFT_BIND_HOST_PREFIX.key() + " must be set");
    }
    InetAddress inetAddress = AmsUtils.getLocalHostExactAddress(
        systemConfig.getString(ArcticMetaStoreConf.THRIFT_BIND_HOST_PREFIX.key()));
    if (inetAddress == null) {
      throw new RuntimeException("can't find host address start with " +
          systemConfig.getString(ArcticMetaStoreConf.THRIFT_BIND_HOST_PREFIX.key()));
    }
    systemConfig.put(ArcticMetaStoreConf.THRIFT_BIND_HOST.key(), inetAddress.getHostAddress());

    //mysql config
    if (systemConfig.getString(ArcticMetaStoreConf.DB_TYPE.key()).equalsIgnoreCase("mysql")) {
      if (!systemConfig.containsKey(ArcticMetaStoreConf.MYBATIS_CONNECTION_PASSWORD.key()) ||
          !systemConfig.containsKey(ArcticMetaStoreConf.MYBATIS_CONNECTION_USER_NAME.key())) {
        throw new RuntimeException("username and password must be configured if the database type is mysql");
      }
    }

    //HA config
    if (systemConfig.containsKey(ArcticMetaStoreConf.HA_ENABLE.key()) &&
        systemConfig.getBoolean(ArcticMetaStoreConf.HA_ENABLE.key())) {
      if (!systemConfig.containsKey(ArcticMetaStoreConf.ZOOKEEPER_SERVER.key())) {
        throw new RuntimeException(ArcticMetaStoreConf.ZOOKEEPER_SERVER.key() + " must be configured when you enable " +
            "the ams high availability");
      }
    }
  }

  public static void tryStartServer() {
    try {
      String configPath = getArcticHome() + "/conf/config.yaml";
      yamlConfig = YamlUtils.load(configPath);
      Configuration conf = initSystemConfig();
      ArcticMetaStore.conf = conf;
      if (conf.getBoolean(ArcticMetaStoreConf.HA_ENABLE)) {
        String zkAddress = conf.getString(ArcticMetaStoreConf.ZOOKEEPER_SERVER);
        String cluster = conf.getString(ArcticMetaStoreConf.CLUSTER_NAME);
        if (haService != null) {
          haService.close();
        }
        haService = new HighAvailabilityServices(zkAddress, cluster);
        haService.addListener(genHAListener(zkAddress, cluster));
        haService.leaderLatch();
      } else {
        startMetaStore(conf);
      }
    } catch (Throwable t) {
      LOG.error("MetaStore Thrift Server threw an exception...", t);
      failover();
    }
  }

  private static String getArcticHome() {
    String arcticHome = System.getenv(ArcticMetaStoreConf.ARCTIC_HOME.key());
    if (arcticHome != null) {
      return arcticHome;
    }
    return System.getProperty(ArcticMetaStoreConf.ARCTIC_HOME.key());
  }

  public static void startMetaStore(Configuration conf) throws Throwable {
    //prepare env
    if (conf.getString(ArcticMetaStoreConf.DB_TYPE).equals("derby")) {
      DerbyService derbyService = new DerbyService();
      derbyService.createTable();
    }

    // init config
    initConfig();
    try {
      long maxMessageSize = conf.getLong(ArcticMetaStoreConf.SERVER_MAX_MESSAGE_SIZE);
      int selectorThreads = conf.getInteger(ArcticMetaStoreConf.THRIFT_SELECTOR_THREADS);
      int workerThreads = conf.getInteger(ArcticMetaStoreConf.THRIFT_WORKER_THREADS);
      int queueSizePerSelector = conf.getInteger(ArcticMetaStoreConf.THRIFT_QUEUE_SIZE_PER_THREAD);
      boolean useCompactProtocol = conf.get(ArcticMetaStoreConf.USE_THRIFT_COMPACT_PROTOCOL);
      int port = conf.getInteger(ArcticMetaStoreConf.THRIFT_BIND_PORT);

      LOG.info("Starting arctic metastore on port " + port);

      TMultiplexedProcessor processor = new TMultiplexedProcessor();
      final TProtocolFactory protocolFactory;
      final TProtocolFactory inputProtoFactory;
      if (useCompactProtocol) {
        protocolFactory = new TCompactProtocol.Factory();
        inputProtoFactory = new TCompactProtocol.Factory(maxMessageSize, maxMessageSize);
      } else {
        protocolFactory = new TBinaryProtocol.Factory();
        inputProtoFactory = new TBinaryProtocol.Factory(true, true, maxMessageSize, maxMessageSize);
      }

      ArcticTableMetastore.Processor<ArcticTableMetastoreHandler> tableMetastoreProcessor =
          new ArcticTableMetastore.Processor<>(
              ServiceContainer.getTableMetastoreHandler());
      processor.registerProcessor("TableMetastore", tableMetastoreProcessor);

      // register OptimizeManager
      OptimizeManager.Processor<OptimizeManagerHandler> optimizeManagerProcessor =
          new OptimizeManager.Processor<>(ServiceContainer.getOptimizeManagerHandler());
      processor.registerProcessor("OptimizeManager", optimizeManagerProcessor);

      TNonblockingServerSocket serverTransport = SecurityUtils.getServerSocket("0.0.0.0", port);
      TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(serverTransport)
          .processor(processor)
          .transportFactory(new TFramedTransport.Factory())
          .protocolFactory(protocolFactory)
          .inputProtocolFactory(inputProtoFactory)
          .workerThreads(workerThreads)
          .selectorThreads(selectorThreads)
          .acceptQueueSizePerThread(queueSizePerSelector);
      LOG.info("Started the new meta server on port [" + port + "]...");
      LOG.info("Options.thriftWorkerThreads = " + workerThreads);
      LOG.info("Options.thriftSelectorThreads = " + selectorThreads);
      LOG.info("Options.queueSizePerSelector = " + queueSizePerSelector);

      server = new TThreadedSelectorServer(args);
      // start meta store worker thread
      Lock metaStoreThreadsLock = new ReentrantLock();
      Condition startCondition = metaStoreThreadsLock.newCondition();
      AtomicBoolean startedServing = new AtomicBoolean();
      ThreadPool.initialize(conf);
      startMetaStoreThreads(conf, metaStoreThreadsLock, startCondition, startedServing);
      signalOtherThreadsToStart(server, metaStoreThreadsLock, startCondition, startedServing);
      server.serve();
    } catch (Throwable t) {
      LOG.error("ams start error", t);
      throw t;
    }
  }

  public static void stopMetaStore() {
    if (server != null && server.isServing()) {
      server.stop();
    }
    residentThreads.forEach(Thread::interrupt);
    residentThreads.clear();
    ThreadPool.shutdown();
  }

  public static boolean isStarted() {
    return server != null && server.isServing() && ServiceContainer.getOptimizeService().isInited();
  }

  public static void failover() {
    stopMetaStore();
    AmsRestServer.stopRestServer();
    isLeader.set(false);
  }

  public static void shutDown() {
    stopMetaStore();
    AmsRestServer.stopRestServer();
    shouldStop = true;
  }

  private static void startMetaStoreThreads(
      Configuration conf,
      final Lock startLock,
      final Condition startCondition,
      final AtomicBoolean startedServing) {
    Integer httpPort = conf.getInteger(ArcticMetaStoreConf.HTTP_SERVER_PORT);

    Thread t = new Thread(() -> {
      startLock.lock();
      try {
        while (!startedServing.get()) {
          startCondition.await();
        }

        startOptimizeCheck(conf.getLong(ArcticMetaStoreConf.OPTIMIZE_CHECK_STATUS_INTERVAL));
        startOptimizeCommit(conf.getInteger(ArcticMetaStoreConf.OPTIMIZE_COMMIT_THREAD_POOL_SIZE));
        startExpiredClean();
        startOrphanClean();
        startTrashClean();
        startSupportHiveSync();
        monitorOptimizerStatus();
        tableRuntimeDataExpire();
        AmsRestServer.startRestServer(httpPort);
        startSyncDDl();
        syncAndExpiredFileInfoCache();
        new UpdateTool().executeAsync();
        if (conf.getBoolean(ArcticMetaStoreConf.HA_ENABLE)) {
          checkLeader();
        }
      } catch (Throwable t1) {
        LOG.error("Failure when starting the worker threads, compact、checker、clean may not happen, ", t1);
        failover();
      } finally {
        startLock.unlock();
      }
    });

    t.setName("Metastore threads starter thread");
    t.start();
    residentThreads.add(t);
  }

  private static void signalOtherThreadsToStart(
      final TServer server, final Lock startLock,
      final Condition startCondition,
      final AtomicBoolean startedServing) {
    // A simple thread to wait until the server has started and then signal the other threads to
    // begin
    Thread t = new Thread(() -> {
      do {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          LOG.warn("Signalling thread was interrupted: " + e.getMessage());
          return;
        }
      } while (!server.isServing());
      startLock.lock();
      try {
        startedServing.set(true);
        startCondition.signalAll();
        LOG.info("service has started succefully!!!");
      } finally {
        startLock.unlock();
      }
    });
    t.start();
    residentThreads.add(t);
  }

  private static void startOptimizeCheck(final long checkInterval) {
    ThreadPool.getPool(ThreadPool.Type.OPTIMIZE_CHECK).scheduleWithFixedDelay(
        () -> ServiceContainer.getOptimizeService().checkOptimizeCheckTasks(checkInterval),
        3 * 1000L,
        60 * 1000L,
        TimeUnit.MILLISECONDS);
  }

  private static void startOptimizeCommit(int parallel) {
    for (int i = 0; i < parallel; i++) {
      ThreadPool.getPool(ThreadPool.Type.COMMIT).execute(new OptimizeCommitWorker(i));
    }
  }

  private static void startExpiredClean() {
    ThreadPool.getPool(ThreadPool.Type.EXPIRE).scheduleWithFixedDelay(
        ServiceContainer.getTableExpireService()::checkTableExpireTasks,
        3 * 1000L,
        60 * 1000L,
        TimeUnit.MILLISECONDS);
  }

  private static void startOrphanClean() {
    ThreadPool.getPool(ThreadPool.Type.ORPHAN).scheduleWithFixedDelay(
        ServiceContainer.getOrphanFilesCleanService()::checkOrphanFilesCleanTasks,
        3 * 1000L,
        60 * 1000L,
        TimeUnit.MILLISECONDS);
  }

  private static void startTrashClean() {
    ThreadPool.getPool(ThreadPool.Type.TRASH_CLEAN).scheduleWithFixedDelay(
        ServiceContainer.getTrashCleanService()::checkTrashCleanTasks,
        3 * 1000L,
        60 * 1000L,
        TimeUnit.MILLISECONDS);
  }

  private static void startSupportHiveSync() {
    ThreadPool.getPool(ThreadPool.Type.HIVE_SYNC).scheduleWithFixedDelay(
        ServiceContainer.getSupportHiveSyncService()::checkHiveSyncTasks,
        3 * 1000L,
        60 * 1000L,
        TimeUnit.MILLISECONDS);
  }

  private static void monitorOptimizerStatus() {
    OptimizeExecuteService.OptimizerMonitor monitor = new OptimizeExecuteService.OptimizerMonitor();
    ThreadPool.getPool(ThreadPool.Type.OPTIMIZER_MONITOR).scheduleWithFixedDelay(
        monitor::monitorStatus,
        3 * 1000L,
        60 * 1000L,
        TimeUnit.MILLISECONDS);
  }

  private static void tableRuntimeDataExpire() {
    RuntimeDataExpireService runtimeDataExpireService = ServiceContainer.getRuntimeDataExpireService();
    ThreadPool.getPool(ThreadPool.Type.TABLE_RUNTIME_DATA_EXPIRE).scheduleWithFixedDelay(
        runtimeDataExpireService::doExpire,
        3 * 1000L,
        60 * 60 * 1000L,
        TimeUnit.MILLISECONDS);
  }

  private static void syncAndExpiredFileInfoCache() {
    Thread t = new Thread(() -> {
      while (server.isServing()) {
        try {
          FileInfoCacheService.SyncAndExpireFileCacheTask task =
              new FileInfoCacheService.SyncAndExpireFileCacheTask();
          task.doTask();
        } catch (Exception e) {
          LOG.error("sync and expired file info cache error", e);
        }
        try {
          Thread.sleep(5 * 60 * 1000);
        } catch (InterruptedException e) {
          LOG.warn("sync and expired file info cache thread was interrupted: " + e.getMessage());
          return;
        }
      }
      try {
        Thread.sleep(60 * 1000);
      } catch (InterruptedException e) {
        LOG.warn("sync and expired file info cache thread was interrupted: " + e.getMessage());
        return;
      }
    });
    t.start();
    residentThreads.add(t);
  }

  private static void startSyncDDl() {
    Thread t = new Thread(() -> {
      while (server.isServing()) {
        try {
          DDLTracerService.DDLSyncTask task =
              new DDLTracerService.DDLSyncTask();
          task.doTask();
        } catch (Exception e) {
          LOG.error("sync schema change cache error", e);
        }
        try {
          Thread.sleep(5 * 60 * 1000);
        } catch (InterruptedException e) {
          LOG.warn("sync schema change cache thread was interrupted: " + e.getMessage());
          return;
        }
      }
      try {
        Thread.sleep(60 * 1000);
      } catch (InterruptedException e) {
        LOG.warn("sync schema change cache thread was interrupted: " + e.getMessage());
        return;
      }
    });
    t.start();
    residentThreads.add(t);
  }

  private static void checkLeader() {
    Thread t = new Thread(() -> {
      while (isLeader.get()) {
        try {
          Thread.sleep(checkLeaderInterval);
        } catch (InterruptedException e) {
          LOG.warn("notLeader thread was interrupted: " + e.getMessage());
          return;
        }
        try {
          if (haService != null &&
              !haService.getMaster().equals(haService.getNodeInfo(
                  conf.getString(ArcticMetaStoreConf.THRIFT_BIND_HOST),
                  conf.getInteger(ArcticMetaStoreConf.THRIFT_BIND_PORT)))) {
            LOG.info("there is not leader, the leader is " + JSONObject.toJSONString(haService.getMaster()));
            failover();
          }
        } catch (Exception e) {
          LOG.error("check leader error", e);
          failover();
        }
      }
    });
    t.start();
    residentThreads.add(t);
  }

  private static Configuration initSystemConfig() {
    Configuration config = Configuration.fromObjectMap(getSystemSettingFromYaml());
    config.setString(ArcticMetaStoreConf.ARCTIC_HOME, getArcticHome());
    return config;
  }

  private static void initCatalogConfig() throws IOException {
    JSONArray catalogs = yamlConfig.getJSONArray(ConfigFileProperties.CATALOG_LIST);
    List<CatalogMeta> catalogMetas = Lists.newArrayList();
    for (int i = 0; i < catalogs.size(); i++) {
      CatalogMeta catalogMeta = new CatalogMeta();
      JSONObject catalog = catalogs.getJSONObject(i);
      catalogMeta.catalogName = catalog.getString(ConfigFileProperties.CATALOG_NAME);
      catalogMeta.catalogType = catalog.getString(ConfigFileProperties.CATALOG_TYPE);

      if (catalog.containsKey(ConfigFileProperties.CATALOG_STORAGE_CONFIG)) {
        Map<String, String> storageConfig = new HashMap<>();
        JSONObject catalogStorageConfig = catalog.getJSONObject(ConfigFileProperties.CATALOG_STORAGE_CONFIG);
        if (catalogStorageConfig.containsKey(ConfigFileProperties.CATALOG_STORAGE_TYPE)) {
          storageConfig.put(
              CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
              catalogStorageConfig.getString(ConfigFileProperties.CATALOG_STORAGE_TYPE));
        }
        storageConfig.put(
            CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE,
            ConfigurationFileUtils.encodeXmlConfigurationFileWithBase64(
                catalogStorageConfig.getString(ConfigFileProperties.CATALOG_CORE_SITE)));
        storageConfig.put(
            CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE,
            ConfigurationFileUtils.encodeXmlConfigurationFileWithBase64(
                catalogStorageConfig.getString(ConfigFileProperties.CATALOG_HDFS_SITE)));
        storageConfig.put(
            CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE,
            ConfigurationFileUtils.encodeXmlConfigurationFileWithBase64(
                catalogStorageConfig.getString(ConfigFileProperties.CATALOG_HIVE_SITE)));
        catalogMeta.storageConfigs = storageConfig;
      }

      if (catalog.containsKey(ConfigFileProperties.CATALOG_AUTH_CONFIG)) {
        Map<String, String> authConfig = new HashMap<>();
        JSONObject catalogAuthConfig = catalog.getJSONObject(ConfigFileProperties.CATALOG_AUTH_CONFIG);
        if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_AUTH_TYPE)) {
          authConfig.put(
              CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
              catalogAuthConfig.getString(ConfigFileProperties.CATALOG_AUTH_TYPE));
        }
        if (catalogAuthConfig.getString(ConfigFileProperties.CATALOG_AUTH_TYPE)
            .equalsIgnoreCase(CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE)) {
          if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_SIMPLE_HADOOP_USERNAME)) {
            authConfig.put(
                CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
                catalogAuthConfig.getString(ConfigFileProperties.CATALOG_SIMPLE_HADOOP_USERNAME));
          }
        } else if (catalogAuthConfig.getString(ConfigFileProperties.CATALOG_AUTH_TYPE)
            .equalsIgnoreCase(CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_KERBEROS)) {
          if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_KEYTAB)) {
            authConfig.put(
                CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB,
                ConfigurationFileUtils.encodeConfigurationFileWithBase64(
                    catalogAuthConfig.getString(ConfigFileProperties.CATALOG_KEYTAB)));
          }
          if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_KRB5)) {
            authConfig.put(
                CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB5,
                ConfigurationFileUtils.encodeConfigurationFileWithBase64(
                    catalogAuthConfig.getString(ConfigFileProperties.CATALOG_KRB5)));
          }
          if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_PRINCIPAL)) {
            authConfig.put(
                CatalogMetaProperties.AUTH_CONFIGS_KEY_PRINCIPAL,
                catalogAuthConfig.getString(ConfigFileProperties.CATALOG_PRINCIPAL));
          }
        }
        catalogMeta.authConfigs = authConfig;
      }

      if (catalog.containsKey(ConfigFileProperties.CATALOG_PROPERTIES)) {
        catalogMeta.catalogProperties = catalog.getObject(ConfigFileProperties.CATALOG_PROPERTIES, Map.class);
      }
      catalogMetas.add(catalogMeta);
    }
    ServiceContainer.getCatalogMetadataService().addCatalog(catalogMetas);
  }

  private static void initConfig() {
    try {
      initContainerConfig();
      initOptimizeGroupConfig();
    } catch (Exception e) {
      LOG.error("init ams config error", e);
      System.exit(1);
    }
  }

  private static void initContainerConfig() {
    JSONArray containers = yamlConfig.getJSONArray(ConfigFileProperties.CONTAINER_LIST);
    for (int i = 0; i < containers.size(); i++) {
      JSONObject optimize = containers.getJSONObject(i);
      Container container = new Container();
      container.setName(optimize.getString(ConfigFileProperties.CONTAINER_NAME));
      container.setType(optimize.getString(ConfigFileProperties.CONTAINER_TYPE));
      if (optimize.containsKey(ConfigFileProperties.CONTAINER_PROPERTIES)) {
        container.setProperties(optimize.getObject(ConfigFileProperties.CONTAINER_PROPERTIES, Map.class));
      }

      ServiceContainer.getOptimizeQueueService().insertContainer(container);
    }
  }

  private static void initOptimizeGroupConfig() throws MetaException, NoSuchObjectException, InvalidObjectException {
    JSONArray optimizeGroups = yamlConfig.getJSONArray(ConfigFileProperties.OPTIMIZE_GROUP_LIST);
    List<OptimizeQueueMeta> optimizeQueueMetas = ServiceContainer.getOptimizeQueueService().getQueues();
    for (int i = 0; i < optimizeGroups.size(); i++) {
      JSONObject optimizeGroup = optimizeGroups.getJSONObject(i);
      OptimizeQueueMeta optimizeQueueMeta = new OptimizeQueueMeta();
      optimizeQueueMeta.setName(optimizeGroup.getString(ConfigFileProperties.OPTIMIZE_GROUP_NAME));
      optimizeQueueMeta.setContainer(optimizeGroup.getString(ConfigFileProperties.OPTIMIZE_GROUP_CONTAINER));

      //init schedule policy
      String schedulePolicy =
          StringUtils.trim(optimizeGroup.getString(ConfigFileProperties.OPTIMIZE_SCHEDULING_POLICY));
      if (StringUtils.isBlank(schedulePolicy)) {
        schedulePolicy = ConfigFileProperties.OPTIMIZE_SCHEDULING_POLICY_QUOTA;
      } else if (
          !(ConfigFileProperties.OPTIMIZE_SCHEDULING_POLICY_QUOTA.equalsIgnoreCase(schedulePolicy) ||
              ConfigFileProperties.OPTIMIZE_SCHEDULING_POLICY_BALANCED.equalsIgnoreCase(schedulePolicy))) {
        throw new IllegalArgumentException(String.format(
            "Scheduling policy only can be %s and %s",
            ConfigFileProperties.OPTIMIZE_SCHEDULING_POLICY_QUOTA,
            ConfigFileProperties.OPTIMIZE_SCHEDULING_POLICY_BALANCED));
      }
      optimizeQueueMeta.setSchedulingPolicy(schedulePolicy);

      List<Container> containers = ServiceContainer.getOptimizeQueueService().getContainers();

      boolean checkContainer =
          containers.stream()
              .anyMatch(e -> e.getName()
                  .equalsIgnoreCase(optimizeGroup.getString(ConfigFileProperties.OPTIMIZE_GROUP_CONTAINER)));
      if (!checkContainer) {
        throw new NoSuchObjectException(
            "can not find such container config named:" +
                optimizeGroup.getString(ConfigFileProperties.OPTIMIZE_GROUP_CONTAINER));
      }
      if (optimizeGroup.containsKey(ConfigFileProperties.OPTIMIZE_GROUP_PROPERTIES)) {
        optimizeQueueMeta.setProperties(
            optimizeGroup.getObject(ConfigFileProperties.OPTIMIZE_GROUP_PROPERTIES, Map.class));
      }
      boolean updated = false;
      for (OptimizeQueueMeta meta : optimizeQueueMetas) {
        if (meta.getName().equals(optimizeQueueMeta.getName())) {
          optimizeQueueMeta.setQueueId(meta.getQueueId());
          ServiceContainer.getOptimizeQueueService().updateQueue(optimizeQueueMeta);
          updated = true;
          break;
        }
      }
      if (!updated) {
        if (optimizeQueueMeta.getQueueId() == 0) {
          optimizeQueueMeta.setQueueId(1);
        }
        ServiceContainer.getOptimizeQueueService().createQueue(optimizeQueueMeta);
      }
    }
  }

  private static LeaderLatchListener genHAListener(String zkServerAddress, String namespace) {
    return new LeaderLatchListener() {
      @Override
      public void isLeader() {
        LOG.info("i am leader");
        try {
          ZookeeperService zkService = ZookeeperService.getInstance(zkServerAddress);
          String masterPath = AmsHAProperties.getMasterPath(namespace);
          zkService.create(masterPath);
          AmsServerInfo serverInfo = new AmsServerInfo();
          serverInfo.setHost(conf.getString(ArcticMetaStoreConf.THRIFT_BIND_HOST));
          serverInfo.setThriftBindPort(conf.getInteger(ArcticMetaStoreConf.THRIFT_BIND_PORT));
          zkService.setData(masterPath, JSONObject.toJSONString(serverInfo));
          isLeader.set(true);
          startMetaStore(initSystemConfig());
        } catch (Throwable throwable) {
          failover();
        }
      }

      @Override
      public void notLeader() {
        //do nothing
        LOG.info("i am salver");
      }
    };
  }
}