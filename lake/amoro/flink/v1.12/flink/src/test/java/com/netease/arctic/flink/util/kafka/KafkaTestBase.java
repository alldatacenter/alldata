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
package com.netease.arctic.flink.util.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.netease.arctic.flink.util.kafka.KafkaConfigGenerate.getPropertiesWithByteArray;

/**
 * The base for the Kafka tests. It brings up:
 *
 * <ul>
 *   <li>A ZooKeeper mini cluster
 *   <li>Three Kafka Brokers (mini clusters)
 * </ul>
 *
 * <p>Code in this test is based on the following GitHub repository: <a
 * href="https://github.com/sakserv/hadoop-mini-clusters">
 * https://github.com/sakserv/hadoop-mini-clusters</a> (ASL licensed), as per commit
 * <i>bc6b2b2d5f6424d5f377aa6c0871e82a956462ef</i>
 */
@SuppressWarnings("serial")
public class KafkaTestBase {

  public final Logger LOG = LoggerFactory.getLogger(KafkaTestBase.class);

  private final int NUMBER_OF_KAFKA_SERVERS = 3;

  public String brokerConnectionStrings;

  public Properties standardProps;

  public KafkaTestEnvironment kafkaServer;

  public Properties secureProps = new Properties();

  // ------------------------------------------------------------------------
  //  Setup and teardown of the mini clusters
  // ------------------------------------------------------------------------

  public void prepare() throws Exception {
    LOG.info("-------------------------------------------------------------------------");
    LOG.info("    Starting KafkaTestBase ");
    LOG.info("-------------------------------------------------------------------------");

    startClusters(false);

    LOG.info("-------------------------------------------------------------------------");
    LOG.info("    KafkaTestBase Started");
    LOG.info("-------------------------------------------------------------------------");
  }

  public void shutDownServices() throws Exception {

    LOG.info("-------------------------------------------------------------------------");
    LOG.info("    Shut down KafkaTestBase ");
    LOG.info("-------------------------------------------------------------------------");

    shutdownClusters();

    LOG.info("-------------------------------------------------------------------------");
    LOG.info("    KafkaTestBase finished");
    LOG.info("-------------------------------------------------------------------------");
  }

  public Properties getProperties() {
    Properties props = new Properties();
    props.putAll(standardProps);
    props.putAll(secureProps);
    return props;
  }

  public void startClusters() throws Exception {
    startClusters(
        KafkaTestEnvironment.createConfig().setKafkaServersNumber(NUMBER_OF_KAFKA_SERVERS));
  }

  public void startClusters(boolean secureMode)
      throws Exception {
    startClusters(
        KafkaTestEnvironment.createConfig()
            .setKafkaServersNumber(NUMBER_OF_KAFKA_SERVERS)
            .setSecureMode(secureMode));
  }

  public void startClusters(KafkaTestEnvironment.Config environmentConfig)
      throws Exception {
    kafkaServer = new KafkaTestEnvironmentImpl();

    LOG.info("Starting KafkaTestBase.prepare() for Kafka " + kafkaServer.getVersion());

    kafkaServer.prepare(environmentConfig);

    standardProps = kafkaServer.getStandardProperties();

    brokerConnectionStrings = kafkaServer.getBrokerConnectionString();

    if (environmentConfig.isSecureMode()) {
      if (!kafkaServer.isSecureRunSupported()) {
        throw new IllegalStateException(
            "Attempting to test in secure mode but secure mode not supported by the KafkaTestEnvironment.");
      }
      secureProps = kafkaServer.getSecureProperties();
    }
  }

  public void shutdownClusters() throws Exception {
    if (secureProps != null) {
      secureProps.clear();
    }

    if (kafkaServer != null) {
      kafkaServer.shutdown();
    }
  }

  public void createTestTopic(
      String topic, int numberOfPartitions, int replicationFactor) {
    kafkaServer.createTestTopic(topic, numberOfPartitions, replicationFactor);
  }

  public void deleteTestTopic(String topic) {
    kafkaServer.deleteTestTopic(topic);
  }

  public void createTopics(int numPartitions, String topic) {
    createTestTopic(topic, numPartitions, 1);
  }

  public ConsumerRecords<String, String> readRecords(String topic) {
    Properties properties = KafkaConfigGenerate.getProperties(getProperties());
    properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
    KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
    consumer.assign(
        consumer.partitionsFor(topic).stream()
            .map(partitionInfo -> new TopicPartition(topic, partitionInfo.partition()))
            .collect(Collectors.toSet()));
    consumer.seekToBeginning(consumer.assignment());
    return consumer.poll(Duration.ofMillis(1000));
  }

  public ConsumerRecords<byte[], byte[]> readRecordsBytes(String topic) {
    Collection<ConsumerRecords<?, ?>> records = readAllRecords(topic, getPropertiesWithByteArray(getProperties()));
    Map<TopicPartition, List<ConsumerRecord<byte[], byte[]>>> data = new HashMap<>();
    records.forEach(consumerRecords -> {
      ConsumerRecord<?, ?> consumerRecord = consumerRecords.iterator().next();
      TopicPartition tp = new TopicPartition(consumerRecord.topic(), consumerRecord.partition());
      List<ConsumerRecord<byte[], byte[]>> list = data.getOrDefault(tp, new ArrayList<>());
      list.add((ConsumerRecord<byte[], byte[]>) consumerRecord);
      data.put(tp, list);
    });
    return new ConsumerRecords<>(data);
  }

  public Collection<ConsumerRecords<?, ?>> readAllRecords(String topic, Properties properties) {
    properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
    KafkaConsumer<?, ?> consumer = new KafkaConsumer<>(properties);
    consumer.assign(
        consumer.partitionsFor(topic).stream()
            .map(partitionInfo -> new TopicPartition(topic, partitionInfo.partition()))
            .collect(Collectors.toSet()));
    consumer.seekToBeginning(consumer.assignment());
    List<ConsumerRecords<?, ?>> records = new ArrayList<>();
    while (true) {
      ConsumerRecords<?, ?> consumerRecords = consumer.poll(Duration.ofMillis(1000));
      if (consumerRecords.isEmpty()) {
        break;
      }
      records.add(consumerRecords);
    }
    return records;
  }

  public Integer countAllRecords(String topic) {
    Properties properties = KafkaConfigGenerate.getProperties(getProperties());
    properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
    return readAllRecords(topic, properties).stream().mapToInt(ConsumerRecords::count).sum();
  }
}
