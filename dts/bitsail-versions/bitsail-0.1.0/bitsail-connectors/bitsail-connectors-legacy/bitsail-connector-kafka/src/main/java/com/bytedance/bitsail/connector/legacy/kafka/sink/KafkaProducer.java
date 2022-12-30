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

package com.bytedance.bitsail.connector.legacy.kafka.sink;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Simple KafkaProducer wrapper
 */
@Slf4j
public class KafkaProducer {
  private org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;

  private String topic;
  private int[] partitionList;

  public KafkaProducer(String bootstrapServerAddress, String topic) {
    this(bootstrapServerAddress, topic, null);
  }

  /**
   * Add some default properties for KafkaProducer:
   * - acks=all (to get best consistency)
   * - retries=0
   * - batch.size=16384
   * - linger.ms=1000 (send every 1 second for better throughput)
   * - buffer.memory=33554432 (32MB)
   * - key.serializer={@link org.apache.kafka.common.serialization.StringSerializer}
   * - value.serializer={@link org.apache.kafka.common.serialization.StringSerializer}
   *
   * @param bootstrapServerAddress kafka cluster (bootstrap server address)
   * @param topic                  kafka topic
   * @param userConfigs            user defined properties
   */
  @SuppressWarnings("checkstyle:MagicNumber")
  public KafkaProducer(String bootstrapServerAddress, String topic, Map<String, Object> userConfigs) {
    log.info("Initializing Kafka bootstrapServerAddress [{}], topic [{}].", bootstrapServerAddress, topic);
    this.topic = topic;

    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerAddress);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 0);
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    props.put(ProducerConfig.LINGER_MS_CONFIG, 1000);
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

    if (MapUtils.isNotEmpty(userConfigs)) {
      userConfigs.forEach(props::put);
    }

    producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);
    partitionList = getPartitionsByTopic(topic);
  }

  public Future<RecordMetadata> send(String value) {
    return producer.send(new ProducerRecord<>(topic, value));
  }

  public Future<RecordMetadata> send(String value, Callback callback) {
    return producer.send(new ProducerRecord<>(topic, value), callback);
  }

  public Future<RecordMetadata> send(String value, int partitionId) {
    return producer.send(new ProducerRecord<>(topic, partitionId, null, value));
  }

  public Future<RecordMetadata> send(String value, int partitionId, Callback callback) {
    return producer.send(new ProducerRecord<>(topic, partitionId, null, value), callback);
  }

  public Future<RecordMetadata> send(String key, String value) {
    return producer.send(new ProducerRecord<>(topic, key, value));
  }

  public Future<RecordMetadata> send(String key, String value, Callback callback) {
    return producer.send(new ProducerRecord<>(topic, key, value), callback);
  }

  /**
   * Get partition information of target topic.
   * The partition ids will be sorted so that all subtasks get the same id list.
   *
   * @return the partition id array
   */
  public int[] getPartitionsByTopic(String topic) {
    List<PartitionInfo> partitionsList = new ArrayList<>(producer.partitionsFor(topic));
    partitionsList.sort(Comparator.comparingInt(PartitionInfo::partition));

    int[] partitions = new int[partitionsList.size()];
    for (int i = 0; i < partitions.length; i++) {
      partitions[i] = partitionsList.get(i).partition();
    }
    return partitions;
  }

  /**
   * deciding which partition to insert based on the sum of hashcode of {@code fields}
   */
  public int choosePartitionIdByFields(String[] fields) {
    int totalFieldsHash = 0;
    for (int i = 0; i < fields.length; i++) {
      totalFieldsHash += fields[i].hashCode();
    }

    int absTotalFieldsHash = Math.max(Math.abs(totalFieldsHash), 0);
    return partitionList[absTotalFieldsHash % partitionList.length];
  }

  public void close() {
    if (producer != null) {
      producer.flush();
      producer.close();
    }
  }
}
