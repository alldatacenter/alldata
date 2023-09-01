/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.server;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.prometheus.client.CollectorRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import org.apache.uniffle.common.Arguments;
import org.apache.uniffle.common.metrics.GRPCMetrics;
import org.apache.uniffle.common.metrics.JvmMetrics;
import org.apache.uniffle.common.metrics.MetricReporter;
import org.apache.uniffle.common.metrics.MetricReporterFactory;
import org.apache.uniffle.common.rpc.ServerInterface;
import org.apache.uniffle.common.security.SecurityConfig;
import org.apache.uniffle.common.security.SecurityContextFactory;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.common.web.CommonMetricsServlet;
import org.apache.uniffle.common.web.JettyServer;
import org.apache.uniffle.server.buffer.ShuffleBufferManager;
import org.apache.uniffle.server.storage.StorageManager;
import org.apache.uniffle.server.storage.StorageManagerFactory;
import org.apache.uniffle.storage.util.StorageType;

import static org.apache.uniffle.common.config.RssBaseConf.RSS_SECURITY_HADOOP_KERBEROS_ENABLE;
import static org.apache.uniffle.common.config.RssBaseConf.RSS_SECURITY_HADOOP_KERBEROS_KEYTAB_FILE;
import static org.apache.uniffle.common.config.RssBaseConf.RSS_SECURITY_HADOOP_KERBEROS_PRINCIPAL;
import static org.apache.uniffle.common.config.RssBaseConf.RSS_SECURITY_HADOOP_KERBEROS_RELOGIN_INTERVAL_SEC;
import static org.apache.uniffle.common.config.RssBaseConf.RSS_SECURITY_HADOOP_KRB5_CONF_FILE;
import static org.apache.uniffle.common.config.RssBaseConf.RSS_STORAGE_TYPE;
import static org.apache.uniffle.common.config.RssBaseConf.RSS_TEST_MODE_ENABLE;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class ShuffleServer {

  private static final Logger LOG = LoggerFactory.getLogger(ShuffleServer.class);
  private RegisterHeartBeat registerHeartBeat;
  private String id;
  private String ip;
  private int port;
  private ShuffleServerConf shuffleServerConf;
  private JettyServer jettyServer;
  private ShuffleTaskManager shuffleTaskManager;
  private ServerInterface server;
  private ShuffleFlushManager shuffleFlushManager;
  private ShuffleBufferManager shuffleBufferManager;
  private StorageManager storageManager;
  private HealthCheck healthCheck;
  private Set<String> tags = Sets.newHashSet();
  private AtomicBoolean isHealthy = new AtomicBoolean(true);
  private GRPCMetrics grpcMetrics;
  private MetricReporter metricReporter;

  public ShuffleServer(ShuffleServerConf shuffleServerConf) throws Exception {
    this.shuffleServerConf = shuffleServerConf;
    try {
      initialization();
    } catch (Exception e) {
      LOG.error("Errors on initializing shuffle server.", e);
      throw e;
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws Exception {
    Arguments arguments = new Arguments();
    CommandLine commandLine = new CommandLine(arguments);
    commandLine.parseArgs(args);
    String configFile = arguments.getConfigFile();
    LOG.info("Start to init shuffle server using config {}", configFile);

    ShuffleServerConf shuffleServerConf = new ShuffleServerConf(configFile);
    final ShuffleServer shuffleServer = new ShuffleServer(shuffleServerConf);
    shuffleServer.start();

    shuffleServer.blockUntilShutdown();
  }

  public void start() throws Exception {
    registerHeartBeat.startHeartBeat();
    jettyServer.start();
    server.start();
    if (metricReporter != null) {
      metricReporter.start();
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        LOG.info("*** shutting down gRPC server since JVM is shutting down");
        try {
          stopServer();
        } catch (Exception e) {
          LOG.error(e.getMessage());
        }
        LOG.info("*** server shut down");
      }
    });
    LOG.info("Shuffle server start successfully!");
  }

  public void stopServer() throws Exception {
    if (jettyServer != null) {
      jettyServer.stop();
      LOG.info("Jetty Server Stopped!");
    }
    if (registerHeartBeat != null) {
      registerHeartBeat.shutdown();
      LOG.info("HeartBeat Stopped!");
    }
    if (storageManager != null) {
      storageManager.stop();
      LOG.info("MultiStorage Stopped!");
    }
    if (healthCheck != null) {
      healthCheck.stop();
      LOG.info("HealthCheck stopped!");
    }
    if (metricReporter != null) {
      metricReporter.stop();
      LOG.info("Metric Reporter Stopped!");
    }
    SecurityContextFactory.get().getSecurityContext().close();
    server.stop();
    LOG.info("RPC Server Stopped!");
  }

  private void initialization() throws Exception {
    boolean testMode = shuffleServerConf.getBoolean(RSS_TEST_MODE_ENABLE);
    String storageType = shuffleServerConf.getString(RSS_STORAGE_TYPE);
    if (!testMode && (StorageType.LOCALFILE.name().equals(storageType)
            || (StorageType.HDFS.name()).equals(storageType))) {
      throw new IllegalArgumentException("RSS storage type about LOCALFILE and HDFS should be used in test mode, "
              + "because of the poor performance of these two types.");
    }
    ip = RssUtils.getHostIp();
    if (ip == null) {
      throw new RuntimeException("Couldn't acquire host Ip");
    }
    port = shuffleServerConf.getInteger(ShuffleServerConf.RPC_SERVER_PORT);
    id = ip + "-" + port;
    LOG.info("Start to initialize server {}", id);
    jettyServer = new JettyServer(shuffleServerConf);
    registerMetrics();

    SecurityConfig securityConfig = null;
    if (shuffleServerConf.getBoolean(RSS_SECURITY_HADOOP_KERBEROS_ENABLE)) {
      securityConfig = SecurityConfig.newBuilder()
          .krb5ConfPath(shuffleServerConf.getString(RSS_SECURITY_HADOOP_KRB5_CONF_FILE))
          .keytabFilePath(shuffleServerConf.getString(RSS_SECURITY_HADOOP_KERBEROS_KEYTAB_FILE))
          .principal(shuffleServerConf.getString(RSS_SECURITY_HADOOP_KERBEROS_PRINCIPAL))
          .reloginIntervalSec(shuffleServerConf.getLong(RSS_SECURITY_HADOOP_KERBEROS_RELOGIN_INTERVAL_SEC))
          .build();
    }
    SecurityContextFactory.get().init(securityConfig);

    storageManager = StorageManagerFactory.getInstance().createStorageManager(shuffleServerConf);
    storageManager.start();

    boolean healthCheckEnable = shuffleServerConf.getBoolean(ShuffleServerConf.HEALTH_CHECK_ENABLE);
    if (healthCheckEnable) {
      List<Checker> builtInCheckers = Lists.newArrayList();
      builtInCheckers.add(storageManager.getStorageChecker());
      healthCheck = new HealthCheck(isHealthy, shuffleServerConf, builtInCheckers);
      healthCheck.start();
    }

    registerHeartBeat = new RegisterHeartBeat(this);
    shuffleFlushManager = new ShuffleFlushManager(shuffleServerConf, id, this, storageManager);
    shuffleBufferManager = new ShuffleBufferManager(shuffleServerConf, shuffleFlushManager);
    shuffleTaskManager = new ShuffleTaskManager(shuffleServerConf, shuffleFlushManager,
        shuffleBufferManager, storageManager);

    setServer();

    initServerTags();
  }

  private void initServerTags() {
    // it's the system tag for server's version
    tags.add(Constants.SHUFFLE_SERVER_VERSION);

    List<String> configuredTags = shuffleServerConf.get(ShuffleServerConf.TAGS);
    if (CollectionUtils.isNotEmpty(configuredTags)) {
      tags.addAll(configuredTags);
    }
    LOG.info("Server tags: {}", tags);
  }

  private void registerMetrics() throws Exception {
    LOG.info("Register metrics");
    CollectorRegistry shuffleServerCollectorRegistry = new CollectorRegistry(true);
    ShuffleServerMetrics.register(shuffleServerCollectorRegistry);
    grpcMetrics = new ShuffleServerGrpcMetrics();
    grpcMetrics.register(new CollectorRegistry(true));
    CollectorRegistry jvmCollectorRegistry = new CollectorRegistry(true);
    boolean verbose = shuffleServerConf.getBoolean(ShuffleServerConf.RSS_JVM_METRICS_VERBOSE_ENABLE);
    JvmMetrics.register(jvmCollectorRegistry, verbose);

    LOG.info("Add metrics servlet");
    jettyServer.addServlet(
        new CommonMetricsServlet(ShuffleServerMetrics.getCollectorRegistry()),
        "/metrics/server");
    jettyServer.addServlet(
        new CommonMetricsServlet(grpcMetrics.getCollectorRegistry()),
        "/metrics/grpc");
    jettyServer.addServlet(
        new CommonMetricsServlet(JvmMetrics.getCollectorRegistry()),
        "/metrics/jvm");
    jettyServer.addServlet(
        new CommonMetricsServlet(ShuffleServerMetrics.getCollectorRegistry(), true),
        "/prometheus/metrics/server");
    jettyServer.addServlet(
        new CommonMetricsServlet(grpcMetrics.getCollectorRegistry(), true),
        "/prometheus/metrics/grpc");
    jettyServer.addServlet(
        new CommonMetricsServlet(JvmMetrics.getCollectorRegistry(), true),
        "/prometheus/metrics/jvm");

    metricReporter = MetricReporterFactory.getMetricReporter(shuffleServerConf, id);
    if (metricReporter != null) {
      metricReporter.addCollectorRegistry(ShuffleServerMetrics.getCollectorRegistry());
      metricReporter.addCollectorRegistry(grpcMetrics.getCollectorRegistry());
      metricReporter.addCollectorRegistry(JvmMetrics.getCollectorRegistry());
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    server.blockUntilShutdown();
  }

  public String getIp() {
    return this.ip;
  }

  public String getId() {
    return this.id;
  }

  public int getPort() {
    return this.port;
  }

  public ShuffleServerConf getShuffleServerConf() {
    return this.shuffleServerConf;
  }

  public ServerInterface getServer() {
    return server;
  }

  @VisibleForTesting
  public void setServer() {
    ShuffleServerFactory shuffleServerFactory = new ShuffleServerFactory(this);
    server = shuffleServerFactory.getServer();
  }

  public void setServer(ServerInterface server) {
    this.server = server;
  }

  public ShuffleTaskManager getShuffleTaskManager() {
    return shuffleTaskManager;
  }

  public ShuffleFlushManager getShuffleFlushManager() {
    return shuffleFlushManager;
  }

  public long getUsedMemory() {
    return shuffleBufferManager.getUsedMemory();
  }

  public long getPreAllocatedMemory() {
    return shuffleBufferManager.getPreAllocatedSize();
  }

  public long getAvailableMemory() {
    return shuffleBufferManager.getCapacity() - shuffleBufferManager.getUsedMemory();
  }

  public int getEventNumInFlush() {
    return shuffleFlushManager.getEventNumInFlush();
  }

  public ShuffleBufferManager getShuffleBufferManager() {
    return shuffleBufferManager;
  }

  public StorageManager getStorageManager() {
    return storageManager;
  }

  public Set<String> getTags() {
    return Collections.unmodifiableSet(tags);
  }

  public boolean isHealthy() {
    return isHealthy.get();
  }

  public GRPCMetrics getGrpcMetrics() {
    return grpcMetrics;
  }
}
