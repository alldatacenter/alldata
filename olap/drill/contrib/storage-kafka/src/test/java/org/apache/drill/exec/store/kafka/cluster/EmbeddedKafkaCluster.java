/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.kafka.cluster;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import kafka.server.KafkaServer;
import org.apache.drill.exec.ZookeeperHelper;
import org.apache.drill.exec.store.kafka.KafkaAsyncCloser;
import org.apache.drill.exec.store.kafka.TestQueryConstants;
import org.apache.kafka.common.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.server.KafkaConfig;
import scala.Option;

public class EmbeddedKafkaCluster implements TestQueryConstants {

  private static final Logger logger = LoggerFactory.getLogger(EmbeddedKafkaCluster.class);
  private List<KafkaServer> brokers;
  private ZookeeperHelper zkHelper;
  private KafkaAsyncCloser closer;
  private final Properties props;

  public EmbeddedKafkaCluster() throws IOException {
    this(new Properties());
  }

  public EmbeddedKafkaCluster(Properties props) throws IOException {
    this(props, 1);
  }

  public EmbeddedKafkaCluster(Properties baseProps, int numberOfBrokers) throws IOException {
    this.props = new Properties();
    props.putAll(baseProps);
    this.zkHelper = new ZookeeperHelper();
    zkHelper.startZookeeper(1);
    this.brokers = new ArrayList<>(numberOfBrokers);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < numberOfBrokers; ++i) {
      if (i != 0) {
        sb.append(BROKER_DELIM);
      }
      int ephemeralBrokerPort = getEphemeralPort();
      sb.append(LOCAL_HOST).append(":").append(ephemeralBrokerPort);
      addBroker(props, i, ephemeralBrokerPort);
    }

    this.props.put("metadata.broker.list", sb.toString());
    this.props.put(KafkaConfig.ZkConnectProp(), this.zkHelper.getConnectionString());
    logger.info("Initialized Kafka Server");
    this.closer = new KafkaAsyncCloser();
  }

  private void addBroker(Properties props, int brokerID, int ephemeralBrokerPort) {
    Properties properties = new Properties();
    properties.putAll(props);
    properties.put(KafkaConfig.LeaderImbalanceCheckIntervalSecondsProp(), String.valueOf(1));
    properties.put(KafkaConfig.OffsetsTopicPartitionsProp(), String.valueOf(1));
    properties.put(KafkaConfig.OffsetsTopicReplicationFactorProp(), String.valueOf(1));
    properties.put(KafkaConfig.DefaultReplicationFactorProp(), String.valueOf(1));
    properties.put(KafkaConfig.GroupMinSessionTimeoutMsProp(), String.valueOf(100));
    properties.put(KafkaConfig.AutoCreateTopicsEnableProp(), Boolean.FALSE);
    properties.put(KafkaConfig.ZkConnectProp(), zkHelper.getConnectionString());
    properties.put(KafkaConfig.BrokerIdProp(), String.valueOf(brokerID + 1));
    properties.put(KafkaConfig.HostNameProp(), LOCAL_HOST);
    properties.put(KafkaConfig.AdvertisedHostNameProp(), LOCAL_HOST);
    properties.put(KafkaConfig.PortProp(), String.valueOf(ephemeralBrokerPort));
    properties.put(KafkaConfig.AdvertisedPortProp(), String.valueOf(ephemeralBrokerPort));
    properties.put(KafkaConfig.DeleteTopicEnableProp(), Boolean.TRUE);
    properties.put(KafkaConfig.LogDirsProp(), getTemporaryDir().getAbsolutePath());
    properties.put(KafkaConfig.LogFlushIntervalMessagesProp(), String.valueOf(1));
    brokers.add(getBroker(properties));
  }

  private static KafkaServer getBroker(Properties properties) {
    KafkaServer broker = new KafkaServer(new KafkaConfig(properties),
        Time.SYSTEM, Option.<String>apply("kafka"), false);
    broker.startup();
    return broker;
  }

  public void shutDownCluster() {
    closer.close();
    closer = null;

    if (brokers != null) {
      brokers.forEach(KafkaServer::shutdown);
      brokers = null;
    }
    if (zkHelper != null) {
      zkHelper.stopZookeeper();
      zkHelper = null;
    }
  }

  public void shutDownBroker(int brokerId) {
    brokers.stream()
        .filter(broker -> Integer.parseInt(broker.config().getString(KafkaConfig.BrokerIdProp())) == brokerId)
        .findAny()
        .ifPresent(KafkaServer::shutdown);
  }

  public Properties getProps() {
    Properties tmpProps = new Properties();
    tmpProps.putAll(this.props);
    return tmpProps;
  }

  public List<KafkaServer> getBrokers() {
    return brokers;
  }

  public void setBrokers(List<KafkaServer> brokers) {
    this.brokers = brokers;
  }

  public ZookeeperHelper getZkServer() {
    return zkHelper;
  }

  public String getKafkaBrokerList() {
    return brokers.stream()
        .map(KafkaServer::config)
        .map(serverConfig -> serverConfig.hostName() + ":" + serverConfig.port())
        .collect(Collectors.joining(","));
  }

  public void registerToClose(AutoCloseable autoCloseable) {
    closer.close(autoCloseable);
  }

  private int getEphemeralPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private File getTemporaryDir() {
    File file = new File(System.getProperty("java.io.tmpdir"), ZK_TMP + System.nanoTime());
    if (!file.mkdir()) {
      logger.error("Failed to create temp Dir");
      throw new RuntimeException("Failed to create temp Dir");
    }
    return file;
  }
}
