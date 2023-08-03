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

package com.netease.arctic.ams.api;

import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import org.apache.thrift.TException;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HADOOP;

public class MockArcticMetastoreServer implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(MockArcticMetastoreServer.class);
  public static final String TEST_CATALOG_NAME = "test_catalog";
  public static final String TEST_DB_NAME = "test_db";

  private int port;
  private int retry = 10;
  private boolean started = false;
  private final AmsHandler amsHandler = new AmsHandler();

  private final OptimizerManagerHandler optimizerManagerHandler = new OptimizerManagerHandler();

  private TServer server;

  private static final MockArcticMetastoreServer INSTANCE = new MockArcticMetastoreServer();

  public static MockArcticMetastoreServer getInstance() {
    if (!INSTANCE.isStarted()) {
      INSTANCE.start();
      Map<String, String> storageConfig = new HashMap<>();
      storageConfig.put(
          CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
          CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS);
      storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE, getHadoopSite());
      storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE, getHadoopSite());

      Map<String, String> authConfig = new HashMap<>();
      authConfig.put(
          CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
          CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE);
      authConfig.put(
          CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
          System.getProperty("user.name"));

      Map<String, String> catalogProperties = new HashMap<>();
      catalogProperties.put(CatalogMetaProperties.KEY_WAREHOUSE, "/tmp");

      CatalogMeta catalogMeta = new CatalogMeta(TEST_CATALOG_NAME, CATALOG_TYPE_HADOOP,
          storageConfig, authConfig, catalogProperties);
      INSTANCE.handler().createCatalog(catalogMeta);

      try {
        INSTANCE.handler().createDatabase(TEST_CATALOG_NAME, TEST_DB_NAME);
      } catch (TException e) {
        throw new RuntimeException(e);
      }
    }
    return INSTANCE;
  }

  public void createCatalogIfAbsent(CatalogMeta catalogMeta) {
    MockArcticMetastoreServer server = getInstance();
    if (server.handler().getCatalogs().stream()
        .noneMatch(meta -> meta.getCatalogName().equals(catalogMeta.getCatalogName()))) {
      server.handler().createCatalog(catalogMeta);
    }
  }

  public static String getHadoopSite() {
    return Base64.getEncoder().encodeToString("<configuration></configuration>".getBytes(StandardCharsets.UTF_8));
  }

  public String getServerUrl() {
    return "thrift://127.0.0.1:" + port;
  }

  public String getUrl() {
    return "thrift://127.0.0.1:" + port + "/" + TEST_CATALOG_NAME;
  }

  public String getUrl(String catalogName) {
    return "thrift://127.0.0.1:" + port + "/" + catalogName;
  }

  public MockArcticMetastoreServer() {
    this.port = randomPort();
  }

  int randomPort() {
    // create a random port between 14000 - 18000
    int port = new Random().nextInt(4000);
    return port + 14000;
  }

  public void start() {
    Thread t = new Thread(this);
    t.start();
    started = true;
  }

  public void stopAndCleanUp() {
    if (server != null) {
      server.stop();
    }
    amsHandler.cleanUp();
    started = false;
  }

  public boolean isStarted() {
    return started;
  }

  public AmsHandler handler() {
    return amsHandler;
  }

  public OptimizerManagerHandler optimizerHandler() {
    return optimizerManagerHandler;
  }

  public int port() {
    return port;
  }

  @Override
  public void run() {
    try {
      TServerSocket socket = new TServerSocket(port);
      TMultiplexedProcessor processor = new TMultiplexedProcessor();
      ArcticTableMetastore.Processor<AmsHandler> amsProcessor =
          new ArcticTableMetastore.Processor<>(amsHandler);
      processor.registerProcessor("TableMetastore", amsProcessor);

      OptimizingService.Processor<OptimizerManagerHandler> optimizerManProcessor =
          new OptimizingService.Processor<>(optimizerManagerHandler);
      processor.registerProcessor("OptimizeManager", optimizerManProcessor);

      final long maxMessageSize = 100 * 1024 * 1024L;
      final TProtocolFactory protocolFactory;
      final TProtocolFactory inputProtoFactory;
      protocolFactory = new TBinaryProtocol.Factory();
      inputProtoFactory = new TBinaryProtocol.Factory(true, true, maxMessageSize, maxMessageSize);

      SynchronousQueue<Runnable> executorQueue = new SynchronousQueue<>();
      AtomicInteger threadCount = new AtomicInteger(0);
      ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
          1,
          10,
          60,
          TimeUnit.SECONDS,
          executorQueue,
          r -> {
            Thread thread = new Thread(r);
            String threadName = "AMS-pool-" + threadCount.incrementAndGet();
            thread.setName(threadName);
            LOG.info("Mock AMS create thread: " + threadName);
            return thread;
          }, new ThreadPoolExecutor.AbortPolicy());

      TThreadPoolServer.Args args = new TThreadPoolServer.Args(socket)
          .processor(processor)
          .transportFactory(new TFramedTransport.Factory())
          .protocolFactory(protocolFactory)
          .inputProtocolFactory(inputProtoFactory)
          .executorService(threadPoolExecutor);
      server = new TThreadPoolServer(args);
      server.serve();

      LOG.info("arctic in-memory metastore start");
    } catch (TTransportException e) {
      if (e.getCause() instanceof BindException) {
        if (--retry < 0) {
          throw new IllegalStateException(e);
        } else {
          port = randomPort();
          LOG.info("Address already in use, port {}, and retry a new port.", port);
          run();
        }
      } else {
        throw new IllegalStateException(e);
      }
    }
  }

  public static class AmsHandler implements ArcticTableMetastore.Iface {
    private static final long DEFAULT_BLOCKER_TIMEOUT = 60_000;
    private final ConcurrentLinkedQueue<CatalogMeta> catalogs = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TableMeta> tables = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, List<String>> databases = new ConcurrentHashMap<>();

    private final Map<TableIdentifier, List<TableCommitMeta>> tableCommitMetas = new HashMap<>();

    private final Map<TableIdentifier, Map<String, Blocker>> tableBlockers = new HashMap<>();
    private final AtomicLong blockerId = new AtomicLong(1L);

    public void cleanUp() {
      catalogs.clear();
      tables.clear();
      databases.clear();
      tableCommitMetas.clear();
    }

    public void createCatalog(CatalogMeta catalogMeta) {
      catalogs.add(catalogMeta);
    }

    public void dropCatalog(String catalogName) {
      tables.removeIf(tableMeta -> tableMeta.getTableIdentifier().getCatalog().equals(catalogName));
      databases.remove(catalogName);
      catalogs.removeIf(catalogMeta -> catalogMeta.getCatalogName().equals(catalogName));
    }

    public Map<TableIdentifier, List<TableCommitMeta>> getTableCommitMetas() {
      return tableCommitMetas;
    }

    @Override
    public void ping() {

    }

    @Override
    public List<CatalogMeta> getCatalogs() {
      return new ArrayList<>(catalogs);
    }

    @Override
    public CatalogMeta getCatalog(String name) throws TException {
      return catalogs.stream().filter(c -> name.equals(c.getCatalogName()))
          .findFirst().orElseThrow(() -> new NoSuchObjectException("catalog with name: " + name + " non-exists."));
    }

    @Override
    public List<String> getDatabases(String catalogName) throws TException {
      return databases.get(catalogName) == null ? new ArrayList<>() : databases.get(catalogName);
    }

    @Override
    public void createDatabase(String catalogName, String database) throws TException {
      databases.computeIfAbsent(catalogName, c -> new ArrayList<>());
      if (databases.get(catalogName).contains(database)) {
        throw new AlreadyExistsException("database exist");
      }
      databases.computeIfPresent(catalogName, (c, dbList) -> {
        List<String> newList = new ArrayList<>(dbList);
        newList.add(database);
        return newList;
      });
    }

    @Override
    public void dropDatabase(String catalogName, String database) throws TException {
      List<String> dbList = databases.get(catalogName);
      if (dbList == null || !dbList.contains(database)) {
        throw new NoSuchObjectException();
      }
      databases.computeIfPresent(catalogName, (c, dbs) -> {
        List<String> databaseList = new ArrayList<>(dbs);
        databaseList.remove(database);
        return databaseList;
      });
    }

    @Override
    public void createTableMeta(TableMeta tableMeta)
        throws TException {
      TableIdentifier identifier = tableMeta.getTableIdentifier();
      String catalog = identifier.getCatalog();
      String database = identifier.getDatabase();
      CatalogMeta catalogMeta = getCatalog(catalog);
      if (
          !"hive".equalsIgnoreCase(catalogMeta.getCatalogType()) &&
              (databases.get(catalog) == null || !databases.get(catalog).contains(database))) {
        throw new NoSuchObjectException("database non-exists");
      }
      tables.add(tableMeta);
    }

    @Override
    public List<TableMeta> listTables(String catalogName, String database) throws TException {
      return tables.stream()
          .filter(t -> catalogName.equals(t.getTableIdentifier().getCatalog()))
          .filter(t -> database.equals(t.getTableIdentifier().getDatabase()))
          .collect(Collectors.toList());
    }

    @Override
    public TableMeta getTable(TableIdentifier tableIdentifier) throws TException {
      return tables.stream()
          .filter(t -> tableIdentifier.equals(t.getTableIdentifier()))
          .findFirst()
          .orElseThrow(NoSuchObjectException::new);
    }

    @Override
    public void removeTable(TableIdentifier tableIdentifier, boolean deleteData) {
      tables.removeIf(t -> t.getTableIdentifier().equals(tableIdentifier));
    }

    @Override
    public void tableCommit(TableCommitMeta commit) throws TException {
      tableCommitMetas.putIfAbsent(commit.getTableIdentifier(), new ArrayList<>());
      tableCommitMetas.get(commit.getTableIdentifier()).add(commit);
      if (commit.getProperties() != null) {
        TableMeta meta = getTable(commit.getTableIdentifier());
        meta.setProperties(commit.getProperties());
      }
    }

    @Override
    public long allocateTransactionId(TableIdentifier tableIdentifier, String transactionSignature) {
      throw new UnsupportedOperationException("allocate TransactionId from AMS is not supported now");
    }

    @Override
    public Blocker block(TableIdentifier tableIdentifier, List<BlockableOperation> operations,
        Map<String, String> properties) throws TException {
      Map<String, Blocker> blockers = this.tableBlockers.computeIfAbsent(tableIdentifier, t -> new HashMap<>());
      long now = System.currentTimeMillis();
      properties.put("create.time", now + "");
      properties.put("expiration.time", (now + DEFAULT_BLOCKER_TIMEOUT) + "");
      properties.put("blocker.timeout", DEFAULT_BLOCKER_TIMEOUT + "");
      Blocker blocker = new Blocker(this.blockerId.getAndIncrement() + "", operations, properties);
      blockers.put(blocker.getBlockerId(), blocker);
      return blocker;
    }

    @Override
    public void releaseBlocker(TableIdentifier tableIdentifier, String blockerId) {
      Map<String, Blocker> blockers = this.tableBlockers.get(tableIdentifier);
      if (blockers != null) {
        blockers.remove(blockerId);
      }
    }

    @Override
    public long renewBlocker(TableIdentifier tableIdentifier, String blockerId) throws TException {
      Map<String, Blocker> blockers = this.tableBlockers.get(tableIdentifier);
      if (blockers == null) {
        throw new NoSuchObjectException("illegal blockerId " + blockerId + ", it may be released or expired");
      }
      Blocker blocker = blockers.get(blockerId);
      if (blocker == null) {
        throw new NoSuchObjectException("illegal blockerId " + blockerId + ", it may be released or expired");
      }
      long expirationTime = System.currentTimeMillis() + DEFAULT_BLOCKER_TIMEOUT;
      blocker.getProperties().put("expiration.time", expirationTime + "");
      return expirationTime;
    }

    @Override
    public List<Blocker> getBlockers(TableIdentifier tableIdentifier) throws TException {
      Map<String, Blocker> blockers = this.tableBlockers.get(tableIdentifier);
      if (blockers == null) {
        return Collections.emptyList();
      } else {
        return new ArrayList<>(blockers.values());
      }
    }

    public void updateMeta(CatalogMeta meta, String key, String value) {
      meta.getCatalogProperties().put(key, value);
    }
  }

  public static class OptimizerManagerHandler implements OptimizingService.Iface {

    private final Map<String, OptimizerRegisterInfo> registeredOptimizers = new ConcurrentHashMap<>();
    private final Queue<OptimizingTask> pendingTasks = new ArrayBlockingQueue<>(100);
    private final Map<String, Map<Integer, OptimizingTaskId>> executingTasks = new ConcurrentHashMap<>();
    private final Map<String, List<OptimizingTaskResult>> completedTasks = new ConcurrentHashMap<>();

    public void cleanUp() {
    }

    @Override
    public void ping() {

    }

    @Override
    public void touch(String authToken) throws TException {
      checkToken(authToken);
    }

    @Override
    public OptimizingTask pollTask(String authToken, int threadId) throws TException {
      checkToken(authToken);
      return pendingTasks.poll();
    }

    @Override
    public void ackTask(String authToken, int threadId, OptimizingTaskId taskId) throws TException {
      checkToken(authToken);
      if (!executingTasks.containsKey(authToken)) {
        executingTasks.putIfAbsent(authToken, new ConcurrentHashMap<>());
      }
      Map<Integer, OptimizingTaskId> executingTasksMap = executingTasks.get(authToken);
      if (executingTasksMap.containsKey(threadId)) {
        throw new ArcticException(ErrorCodes.DUPLICATED_TASK_ERROR_CODE, "DuplicateTask", String.format("Optimizer:%s" +
            " thread:%d is executing another task", authToken, threadId));
      }
      executingTasksMap.put(threadId, taskId);
    }

    @Override
    public void completeTask(String authToken, OptimizingTaskResult taskResult) throws TException {
      checkToken(authToken);
      executingTasks.get(authToken).remove(taskResult.getThreadId());
      if (!completedTasks.containsKey(authToken)) {
        completedTasks.putIfAbsent(authToken, new CopyOnWriteArrayList<>());
      }
      List<OptimizingTaskResult> completeTaskList = completedTasks.get(authToken);
      completeTaskList.add(taskResult);
    }

    @Override
    public String authenticate(OptimizerRegisterInfo registerInfo) throws TException {
      String token = UUID.randomUUID().toString();
      registeredOptimizers.put(token, registerInfo);
      return token;
    }

    public Map<String, OptimizerRegisterInfo> getRegisteredOptimizers() {
      return registeredOptimizers;
    }

    public boolean offerTask(OptimizingTask task) {
      return pendingTasks.offer(task);
    }

    public Queue<OptimizingTask> getPendingTasks() {
      return pendingTasks;
    }

    public Map<String, Map<Integer, OptimizingTaskId>> getExecutingTasks() {
      return executingTasks;
    }

    public Map<String, List<OptimizingTaskResult>> getCompletedTasks() {
      return completedTasks;
    }

    private void checkToken(String token) throws ArcticException {
      if (!registeredOptimizers.containsKey(token)) {
        throw new ArcticException(ErrorCodes.PLUGIN_RETRY_AUTH_ERROR_CODE, "unknown token", "unknown token");
      }
    }
  }
}
