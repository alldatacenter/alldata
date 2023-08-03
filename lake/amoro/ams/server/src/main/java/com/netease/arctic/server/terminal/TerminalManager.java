/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.terminal;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.server.ArcticManagementConf;
import com.netease.arctic.server.catalog.CatalogType;
import com.netease.arctic.server.dashboard.model.LatestSessionInfo;
import com.netease.arctic.server.dashboard.model.LogInfo;
import com.netease.arctic.server.dashboard.model.SqlResult;
import com.netease.arctic.server.dashboard.utils.AmsUtil;
import com.netease.arctic.server.table.TableService;
import com.netease.arctic.server.terminal.TerminalSessionFactory.SessionConfigOptions;
import com.netease.arctic.server.terminal.kyuubi.KyuubiTerminalSessionFactory;
import com.netease.arctic.server.terminal.local.LocalSessionFactory;
import com.netease.arctic.server.utils.ConfigOptions;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.table.TableMetaStore;
import com.netease.arctic.utils.CatalogUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TerminalManager {

  private static final Logger LOG = LoggerFactory.getLogger(TerminalManager.class);

  private static final int SESSION_TIMEOUT_CHECK_INTERVAL = 5 * 60 * 1000; // 5min

  private final Configurations serviceConfig;
  private final AtomicLong threadPoolCount = new AtomicLong();
  private final TableService tableService;
  private final TerminalSessionFactory sessionFactory;
  private final int resultLimits;
  private final boolean stopOnError;

  private final int sessionTimeout;

  private final Object sessionMapLock = new Object();
  private final Map<String, TerminalSessionContext> sessionMap = Maps.newHashMap();
  private final Thread gcThread;
  private boolean stop = false;

  private final ThreadPoolExecutor executionPool = new ThreadPoolExecutor(
      1, 50, 30, TimeUnit.MINUTES,
      new LinkedBlockingQueue<>(),
      r -> new Thread(null, r, "terminal-execute-" + threadPoolCount.incrementAndGet()));

  public TerminalManager(Configurations conf, TableService tableService) {
    this.serviceConfig = conf;
    this.tableService = tableService;
    this.resultLimits = conf.getInteger(ArcticManagementConf.TERMINAL_RESULT_LIMIT);
    this.stopOnError = conf.getBoolean(ArcticManagementConf.TERMINAL_STOP_ON_ERROR);
    this.sessionTimeout = conf.getInteger(ArcticManagementConf.TERMINAL_SESSION_TIMEOUT);
    this.sessionFactory = loadTerminalSessionFactory(conf);
    gcThread = new Thread(new SessionCleanTask());
    gcThread.setName("terminal-session-gc");
    gcThread.start();
  }

  /**
   * execute script, return terminal sessionId
   *
   * @param terminalId - id to mark different terminal windows
   * @param catalog    - current catalog to execute script
   * @param script     - sql script to be executed
   * @return - sessionId, session refer to a sql execution context
   */
  public String executeScript(String terminalId, String catalog, String script) {
    CatalogMeta catalogMeta = tableService.getCatalogMeta(catalog);
    TableMetaStore metaStore = getCatalogTableMetaStore(catalogMeta);
    String sessionId = getSessionId(terminalId, metaStore, catalog);
    String connectorType = catalogConnectorType(catalogMeta);
    applyClientProperties(catalogMeta);
    Configurations configuration = new Configurations();
    configuration.setInteger(SessionConfigOptions.FETCH_SIZE, resultLimits);
    configuration.set(SessionConfigOptions.CATALOGS, Lists.newArrayList(catalog));
    configuration.set(SessionConfigOptions.catalogConnector(catalog), connectorType);
    configuration.set(SessionConfigOptions.CATALOG_URL_BASE, AmsUtil.getAMSThriftAddress(serviceConfig,
        Constants.THRIFT_TABLE_SERVICE_NAME));
    for (String key : catalogMeta.getCatalogProperties().keySet()) {
      String value = catalogMeta.getCatalogProperties().get(key);
      configuration.set(SessionConfigOptions.catalogProperty(catalog, key), value);
    }


    TerminalSessionContext context;
    synchronized (sessionMapLock) {
      sessionMap.computeIfAbsent(
          sessionId, id -> new TerminalSessionContext(id, metaStore, executionPool, sessionFactory, configuration)
      );

      context = sessionMap.get(sessionId);
    }

    if (!context.isReadyToExecute()) {
      throw new IllegalStateException("current session is not ready to execute script. status:" + context.getStatus());
    }
    context.submit(catalog, script, resultLimits, stopOnError);
    return sessionId;
  }

  /**
   * getRuntime execution status and logs
   */
  public LogInfo getExecutionLog(String sessionId) {
    if (sessionId == null) {
      return new LogInfo(ExecutionStatus.Expired.name(), Lists.newArrayList());
    }
    TerminalSessionContext sessionContext;
    synchronized (sessionMapLock) {
      sessionContext = sessionMap.get(sessionId);
    }
    if (sessionContext == null) {
      return new LogInfo(ExecutionStatus.Expired.name(), Lists.newArrayList());
    }
    return new LogInfo(sessionContext.getStatus().name(), sessionContext.getLogs());
  }

  /**
   * getRuntime execution result.
   */
  public List<SqlResult> getExecutionResults(String sessionId) {
    if (sessionId == null) {
      return Lists.newArrayList();
    }
    TerminalSessionContext context;
    synchronized (sessionMapLock) {
      context = sessionMap.get(sessionId);
    }
    if (context == null) {
      return Lists.newArrayList();
    }
    return context.getStatementResults().stream().map(statement -> {
      SqlResult sql = new SqlResult();
      sql.setId("line:" + statement.getLineNumber() + " - " + statement.getStatement());
      sql.setColumns(statement.getColumns());
      sql.setRowData(statement.getDataAsStringList());
      sql.setStatus(statement.isSuccess() ? ExecutionStatus.Finished.name() : ExecutionStatus.Failed.name());
      return sql;
    }).collect(Collectors.toList());
  }

  /**
   * cancel execution
   */
  public void cancelExecution(String sessionId) {
    if (sessionId == null) {
      return;
    }
    TerminalSessionContext context;
    synchronized (sessionMapLock) {
      context = sessionMap.get(sessionId);
    }
    if (context != null) {
      context.cancel();
    }
  }

  /**
   * getRuntime last execution info
   *
   * @param terminalId - id of terminal window
   * @return last session info
   */
  public LatestSessionInfo getLastSessionInfo(String terminalId) {
    String prefix = terminalId + "-";
    long lastExecutionTime = -1;
    String sessionId = "";
    String script = "";
    synchronized (sessionMapLock) {
      for (String sid : sessionMap.keySet()) {
        if (sid.startsWith(prefix)) {
          TerminalSessionContext context = sessionMap.get(sid);
          if (context == null) {
            continue;
          }
          if (lastExecutionTime < context.lastExecutionTime()) {
            lastExecutionTime = context.lastExecutionTime();
            sessionId = sid;
            script = context.lastScript();
          }
        }
      }
    }
    return new LatestSessionInfo(sessionId, script);
  }

  public void dispose() {
    stop = true;
    if (gcThread != null) {
      gcThread.interrupt();
    }
    executionPool.shutdown();
  }

  // ========================== private method =========================

  private String catalogConnectorType(CatalogMeta catalogMeta) {
    String catalogType = catalogMeta.getCatalogType();
    String tableFormats = catalogMeta.getCatalogProperties().get(CatalogMetaProperties.TABLE_FORMATS);
    if (catalogType.equalsIgnoreCase(CatalogType.AMS.name())) {
      if (StringUtils.containsIgnoreCase(tableFormats, TableFormat.MIXED_ICEBERG.name())) {
        return "arctic";
      } else if (StringUtils.containsIgnoreCase(tableFormats, TableFormat.ICEBERG.name())) {
        return "iceberg";
      }
    } else if (catalogType.equalsIgnoreCase(CatalogType.HIVE.name()) ||
        catalogType.equalsIgnoreCase(CatalogType.HADOOP.name())) {
      if (StringUtils.containsIgnoreCase(tableFormats, TableFormat.MIXED_HIVE.name()) ||
          StringUtils.containsIgnoreCase(tableFormats, TableFormat.MIXED_ICEBERG.name())) {
        return "arctic";
      } else if (StringUtils.containsIgnoreCase(tableFormats, TableFormat.ICEBERG.name())) {
        return "iceberg";
      }
    } else if (catalogType.equalsIgnoreCase(CatalogType.CUSTOM.name())) {
      return "iceberg";
    }
    throw new IllegalStateException("unknown catalog type: " + catalogType);
  }

  private String getSessionId(String loginId, TableMetaStore auth, String catalog) {
    String authName = auth.getHadoopUsername();
    if (auth.isKerberosAuthMethod()) {
      authName = auth.getKrbPrincipal();
    }
    String sessionId = loginId + "-" + auth.getAuthMethod() + "-" + authName + "-" + catalog;
    sessionId = sessionId.replace("/", "_");
    return sessionId;
  }

  private TableMetaStore getCatalogTableMetaStore(CatalogMeta catalogMeta) {
    TableMetaStore.Builder builder = TableMetaStore.builder()
        .withBase64MetaStoreSite(
            catalogMeta.getStorageConfigs().get(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE))
        .withBase64CoreSite(
            catalogMeta.getStorageConfigs().get(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE))
        .withBase64HdfsSite(
            catalogMeta.getStorageConfigs().get(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE));
    if (catalogMeta.getAuthConfigs()
        .get(CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE)
        .equalsIgnoreCase(CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE)) {
      builder.withSimpleAuth(catalogMeta.getAuthConfigs()
          .get(CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME));
    } else {
      builder.withBase64Auth(
          catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE),
          catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME),
          catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB),
          catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB5),
          catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_PRINCIPAL));
    }
    return builder.build();
  }

  private TerminalSessionFactory loadTerminalSessionFactory(Configurations conf) {
    String backend = conf.get(ArcticManagementConf.TERMINAL_BACKEND);
    if (backend == null) {
      throw new IllegalArgumentException("lack terminal implement config.");
    }
    String backendImplement;
    switch (backend.toLowerCase()) {
      case "local":
        backendImplement = LocalSessionFactory.class.getName();
        break;
      case "kyuubi":
        backendImplement = KyuubiTerminalSessionFactory.class.getName();
        break;
      case "custom":
        Optional<String> customFactoryClz = conf.getOptional(ArcticManagementConf.TERMINAL_SESSION_FACTORY);
        if (!customFactoryClz.isPresent()) {
          throw new IllegalArgumentException("terminal backend type is custom, but terminal session factory is not " +
              "configured");
        }
        backendImplement = customFactoryClz.get();
        break;
      default:
        throw new IllegalArgumentException("illegal terminal implement: " + backend + ", local, kyuubi, " +
            "custom is available");
    }
    TerminalSessionFactory factory;
    try {
      factory = (TerminalSessionFactory) Class.forName(backendImplement).newInstance();
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      throw new RuntimeException("failed to init session factory", e);
    }

    String factoryPropertiesPrefix = ArcticManagementConf.TERMINAL_PREFIX + backend + ".";
    Configurations configuration = new Configurations();

    for (String key : conf.keySet()) {
      if (!key.startsWith(ArcticManagementConf.TERMINAL_PREFIX)) {
        continue;
      }
      String value = conf.getValue(ConfigOptions.key(key).stringType().noDefaultValue());
      key = key.substring(factoryPropertiesPrefix.length());
      configuration.setString(key, value);
    }
    configuration.set(TerminalSessionFactory.FETCH_SIZE, this.resultLimits);
    factory.initialize(configuration);
    return factory;
  }

  private void applyClientProperties(CatalogMeta catalogMeta) {
    Set<TableFormat> formats = CatalogUtil.tableFormats(catalogMeta);
    if (CatalogMetaProperties.CATALOG_TYPE_AMS.equalsIgnoreCase(catalogMeta.getCatalogType())) {
      if (formats.contains(TableFormat.ICEBERG)) {
        catalogMeta.putToCatalogProperties(CatalogMetaProperties.KEY_WAREHOUSE, catalogMeta.getCatalogName());
      }
    } else if (formats.contains(TableFormat.ICEBERG)) {
      if (!catalogMeta.getCatalogProperties().containsKey(CatalogProperties.CATALOG_IMPL)) {
        catalogMeta.putToCatalogProperties("type", catalogMeta.getCatalogType());
      }
    }
  }

  private class SessionCleanTask implements Runnable {
    private static final long MINUTE_IN_MILLIS = 60 * 1000;

    @Override
    public void run() {
      LOG.info("Terminal Session Clean Task started");
      LOG.info("Terminal Session Clean Task, check interval: " + SESSION_TIMEOUT_CHECK_INTERVAL + " ms");
      LOG.info("Terminal Session Timeout: " + sessionTimeout + " minutes");
      while (!stop) {
        try {
          List<TerminalSessionContext> sessionToRelease = checkIdleSession();
          sessionToRelease.forEach(this::releaseSession);
          if (!sessionToRelease.isEmpty()) {
            LOG.info("Terminal Session release count: " + sessionToRelease.size());
          }
        } catch (Throwable t) {
          LOG.error("error when check and release session", t);
        }

        try {
          TimeUnit.MILLISECONDS.sleep(SESSION_TIMEOUT_CHECK_INTERVAL);
        } catch (InterruptedException e) {
          LOG.error("Interrupted when sleep", e);
        }
      }
    }

    private List<TerminalSessionContext> checkIdleSession() {
      final long timeoutInMillis = sessionTimeout * MINUTE_IN_MILLIS;
      synchronized (sessionMapLock) {
        List<TerminalSessionContext> sessionToRelease = Lists.newArrayList();
        for (String sessionId : sessionMap.keySet()) {
          TerminalSessionContext sessionContext = sessionMap.get(sessionId);
          if (sessionContext.isIdleStatus()) {
            long idleTime = System.currentTimeMillis() - sessionContext.lastExecutionTime();
            if (idleTime > timeoutInMillis) {
              sessionToRelease.add(sessionContext);
            }
          }
        }

        sessionToRelease.forEach(s -> sessionMap.remove(s.getSessionId()));
        return sessionToRelease;
      }
    }

    private void releaseSession(TerminalSessionContext sessionContext) {
      try {
        sessionContext.release();
      } catch (Throwable t) {
        LOG.error("error when release session: " + sessionContext.getSessionId(), t);
      }
    }
  }
}
