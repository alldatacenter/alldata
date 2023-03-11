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

import kafka.server.KafkaServer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Abstract class providing a Kafka test environment.
 */
public abstract class KafkaTestEnvironment {
  /**
   * Configuration class for {@link KafkaTestEnvironment}.
   */
  public static class Config {
    private int kafkaServersNumber = 1;
    private Properties kafkaServerProperties = null;
    private boolean secureMode = false;
    private boolean hideKafkaBehindProxy = false;

    /**
     * Please use {@link KafkaTestEnvironment#createConfig()} method.
     */
    private Config() {
    }

    public int getKafkaServersNumber() {
      return kafkaServersNumber;
    }

    public Config setKafkaServersNumber(int kafkaServersNumber) {
      this.kafkaServersNumber = kafkaServersNumber;
      return this;
    }

    public Properties getKafkaServerProperties() {
      return kafkaServerProperties;
    }

    public Config setKafkaServerProperties(Properties kafkaServerProperties) {
      this.kafkaServerProperties = kafkaServerProperties;
      return this;
    }

    public boolean isSecureMode() {
      return secureMode;
    }

    public Config setSecureMode(boolean secureMode) {
      this.secureMode = secureMode;
      return this;
    }
  }

  protected static final String KAFKA_HOST = "localhost";

  public static Config createConfig() {
    return new Config();
  }

  public abstract void prepare(Config config) throws Exception;

  public abstract void shutdown() throws Exception;

  public abstract void deleteTestTopic(String topic);

  public abstract void createTestTopic(
      String topic, int numberOfPartitions, int replicationFactor, Properties properties);

  public void createTestTopic(String topic, int numberOfPartitions, int replicationFactor) {
    this.createTestTopic(topic, numberOfPartitions, replicationFactor, new Properties());
  }

  public abstract Properties getStandardProperties();

  public abstract Properties getSecureProperties();

  public abstract String getBrokerConnectionString();

  public abstract String getVersion();

  public abstract List<KafkaServer> getBrokers();

  public Properties getIdempotentProducerConfig() {
    Properties props = new Properties();
    props.put("enable.idempotence", "true");
    props.put("acks", "all");
    props.put("retries", "3");
    return props;
  }

  // -- consumer / producer instances:
  public abstract <K, V> Collection<ConsumerRecord<K, V>> getAllRecordsFromTopic(
      Properties properties, String topic, int partition, long timeout);

  // -- offset handlers

  /**
   * Simple interface to commit and retrieve offsets.
   */
  public interface KafkaOffsetHandler {
    Long getCommittedOffset(String topicName, int partition);

    void setCommittedOffset(String topicName, int partition, long offset);

    void close();
  }

  public abstract KafkaOffsetHandler createOffsetHandler();

  // -- leader failure simulation

  public abstract void restartBroker(int leaderId) throws Exception;

  public abstract int getLeaderToShutDown(String topic) throws Exception;

  public abstract int getBrokerId(KafkaServer server);

  public abstract boolean isSecureRunSupported();

  protected void maybePrintDanglingThreadStacktrace(String threadNameKeyword) {
    for (Map.Entry<Thread, StackTraceElement[]> threadEntry :
        Thread.getAllStackTraces().entrySet()) {
      if (threadEntry.getKey().getName().contains(threadNameKeyword)) {
        System.out.println("Dangling thread found:");
        for (StackTraceElement ste : threadEntry.getValue()) {
          System.out.println(ste);
        }
      }
    }
  }
}
