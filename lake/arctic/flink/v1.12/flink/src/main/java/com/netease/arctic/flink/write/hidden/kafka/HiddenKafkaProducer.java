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

package com.netease.arctic.flink.write.hidden.kafka;

import com.netease.arctic.flink.write.hidden.ArcticLogPartitioner;
import com.netease.arctic.flink.write.hidden.LogMsgFactory;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonSerialization;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaErrorCode;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaException;
import org.apache.flink.streaming.connectors.kafka.internals.FlinkKafkaInternalProducer;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG;

/**
 * This is hidden log queue kafka producer that serializes {@link LogData<T>} and emits to the kafka topic.
 */
public class HiddenKafkaProducer<T> implements LogMsgFactory.Producer<T> {
  private static final Logger LOG = LoggerFactory.getLogger(HiddenKafkaProducer.class);
  /**
   * User defined properties for the Kafka Producer.
   */
  protected final Properties producerConfig;

  private final String topic;

  private final LogDataJsonSerialization<T> logDataJsonSerialization;

  /**
   * The callback than handles error propagation or logging callbacks.
   */
  @Nullable
  protected transient Callback callback;
  /**
   * Errors encountered in the async producer are stored here.
   */
  @Nullable
  protected transient volatile Exception asyncException;

  private transient FlinkKafkaInternalProducer<byte[], byte[]> producer;
  private transient FlinkKafkaInternalProducer<byte[], byte[]> transactionalProducer;

  private ArcticLogPartitioner<T> arcticLogPartitioner;
  private List<Integer> partitions;

  public HiddenKafkaProducer(
      Properties producerConfig,
      String topic,
      LogDataJsonSerialization<T> logDataJsonSerialization,
      ArcticLogPartitioner<T> arcticLogPartitioner) {
    this.producerConfig = producerConfig;
    this.topic = topic;
    this.logDataJsonSerialization = logDataJsonSerialization;
    this.arcticLogPartitioner = arcticLogPartitioner;
  }

  @Override
  public void open() throws Exception {
    callback = (metadata, exception) -> {
      if (exception != null && asyncException == null) {
        asyncException = exception;
      }
      acknowledgeMessage();
    };
    producer = createProducer();
    transactionalProducer = createTransactionalProducer();
    transactionalProducer.initTransactions();
    partitions = getPartitionsByTopic(topic, producer);
    LOG.info("HiddenKafkaPartition topic:{}, partitions:{}.", topic, partitions);
  }

  @Override
  public void send(LogData<T> logData) throws Exception {
    checkErroneous();
    byte[] message = logDataJsonSerialization.serialize(logData);
    Integer partition = arcticLogPartitioner.partition(logData, partitions);
    ProducerRecord<byte[], byte[]> producerRecord =
        new ProducerRecord<>(topic, partition, null, null, message);
    producer.send(producerRecord, callback);
  }

  @Override
  public void sendToAllPartitions(LogData<T> logData) throws Exception {
    checkErroneous();
    byte[] message = logDataJsonSerialization.serialize(logData);
    List<ProducerRecord<byte[], byte[]>> recordList =
        partitions.stream()
            .map(i -> new ProducerRecord<byte[], byte[]>(topic, i, null, null, message))
            .collect(Collectors.toList());
    LOG.info("sending {} partitions with flip message={}.", recordList.size(), logData);
    long start = System.currentTimeMillis();
    try {
      transactionalProducer.beginTransaction();
      for (ProducerRecord<byte[], byte[]> producerRecord : recordList) {
        checkErroneous();
        transactionalProducer.send(producerRecord, callback);
      }
      transactionalProducer.commitTransaction();
      LOG.info("finished flips sending, cost {}ms.", System.currentTimeMillis() - start);
    } catch (Throwable e) {
      LOG.error("", e);
      transactionalProducer.abortTransaction();
      throw new FlinkRuntimeException(e);
    }
  }

  @Override
  public void flush() {
    producer.flush();
  }

  @Override
  public void close() throws Exception {
    try {
      producer.close(Duration.ofSeconds(0));
      transactionalProducer.close(Duration.ofSeconds(0));
    } catch (Exception e) {
      asyncException = ExceptionUtils.firstOrSuppressed(e, asyncException);
    } finally {
      checkErroneous();
    }
  }

  protected FlinkKafkaInternalProducer<byte[], byte[]> createTransactionalProducer() {
    Properties transactionalProperties = new Properties();
    transactionalProperties.putAll(producerConfig);
    transactionalProperties.computeIfAbsent(TRANSACTIONAL_ID_CONFIG, o -> UUID.randomUUID().toString());
    return new FlinkKafkaInternalProducer<>(transactionalProperties);
  }

  protected FlinkKafkaInternalProducer<byte[], byte[]> createProducer() {
    return new FlinkKafkaInternalProducer<>(producerConfig);
  }

  public static List<Integer> getPartitionsByTopic(String topic, org.apache.kafka.clients.producer.Producer producer) {
    // the fetched list is immutable, so we're creating a mutable copy in order to sort it
    List<PartitionInfo> partitionsList = new ArrayList<>(producer.partitionsFor(topic));

    // sort the partitions by partition id to make sure the fetched partition list is the same across subtasks
    partitionsList.sort(Comparator.comparingInt(PartitionInfo::partition));

    return partitionsList.stream().map(PartitionInfo::partition).collect(Collectors.toList());
  }

  protected void checkErroneous() throws FlinkKafkaException {
    Exception e = asyncException;
    if (e != null) {
      // prevent double throwing
      asyncException = null;
      throw new FlinkKafkaException(
          FlinkKafkaErrorCode.EXTERNAL_ERROR,
          "Failed to send data to Kafka: " + e.getMessage(),
          e);
    }
  }

  /**
   * <b>ATTENTION to subclass implementors:</b> When overriding this method, please always call
   * {@code super.acknowledgeMessage()} to keep the invariants of the internal bookkeeping of the producer.
   * If not, be sure to know what you are doing.
   */
  protected void acknowledgeMessage() {
  }
}
