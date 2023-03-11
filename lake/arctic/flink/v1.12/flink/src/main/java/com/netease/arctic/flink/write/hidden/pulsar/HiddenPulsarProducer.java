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

package com.netease.arctic.flink.write.hidden.pulsar;

import com.google.common.collect.Lists;
import com.netease.arctic.flink.write.hidden.ArcticLogPartitioner;
import com.netease.arctic.flink.write.hidden.LogMsgFactory;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonSerialization;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.pulsar.common.config.PulsarClientFactory;
import org.apache.flink.connector.pulsar.sink.config.SinkConfiguration;
import org.apache.flink.connector.pulsar.sink.writer.toipc.TopicMetadataListener;
import org.apache.flink.connector.pulsar.sink.writer.toipc.TopicProducerRegister;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.flink.util.IOUtils;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.apache.pulsar.client.api.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.connector.pulsar.common.config.PulsarOptions.PULSAR_ENABLE_TRANSACTION;

/**
 * This is hidden log queue pulsar producer that serializes {@link LogData<T>} and emits to the pulsar topic.
 */
public class HiddenPulsarProducer<T> implements LogMsgFactory.Producer<T> {
  private static final Logger LOG = LoggerFactory.getLogger(HiddenPulsarProducer.class);
  /**
   * User defined properties for the Pulsar Producer.
   */
  protected final SinkConfiguration configuration;

  private final LogDataJsonSerialization<T> logDataJsonSerialization;

  private transient TopicProducerRegister producerRegister;
  private transient PulsarClient pulsarClient;

  private ArcticLogPartitioner<T> arcticLogPartitioner;
  private long pendingMessages = 0;
  private final TopicMetadataListener metadataListener;

  public HiddenPulsarProducer(
      SinkConfiguration configuration,
      String topic,
      LogDataJsonSerialization<T> logDataJsonSerialization,
      ArcticLogPartitioner<T> arcticLogPartitioner) {
    this.configuration = configuration;
    this.logDataJsonSerialization = logDataJsonSerialization;
    this.arcticLogPartitioner = arcticLogPartitioner;
    this.metadataListener = new TopicMetadataListener(Lists.newArrayList(topic));
  }

  @Override
  public void open(StreamingRuntimeContext context) throws Exception {
    metadataListener.open(configuration, context == null ? null : context.getProcessingTimeService());
    producerRegister = new TopicProducerRegister(configuration);
    
    // enable transaction forcibly to support send Flip messages in transaction
    Configuration conf = new Configuration(configuration);
    conf.set(PULSAR_ENABLE_TRANSACTION, true);
    pulsarClient = PulsarClientFactory.createClient(new SinkConfiguration(conf));
  }

  @Override
  public void open() throws Exception {
    open(null);
  }

  @Override
  public void send(LogData<T> logData) throws Exception {
    byte[] message = logDataJsonSerialization.serialize(logData);

    List<String> availableTopics = metadataListener.availableTopics();
    String topicPartition = arcticLogPartitioner.partition(logData, availableTopics);

    // Create message builder for sending message.
    TypedMessageBuilder<?> builder = createMessageBuilder(topicPartition, message);
    requirePermits();
    enqueueMessageSending(topicPartition, builder);
  }

  private void enqueueMessageSending(String topic, TypedMessageBuilder<?> builder)
      throws ExecutionException, InterruptedException {
    // Block the mailbox executor for yield method.
    builder.sendAsync()
        .whenComplete(
            (id, ex) -> {
              releasePermits();
              if (ex != null) {
                throw new FlinkRuntimeException(
                    "Failed to send data to Pulsar " + topic, ex);
              } else {
                LOG.debug(
                    "Sent message to Pulsar {} with message id {}", topic, id);
              }
            })
        .get();
  }

  @Override
  public void sendToAllPartitions(LogData<T> logData) throws Exception {
    byte[] message = logDataJsonSerialization.serialize(logData);
    List<String> availableTopics = metadataListener.availableTopics();

    LOG.info("sending {} partitions with flip message={}.", availableTopics.size(), logData);
    long start = System.currentTimeMillis();
    Transaction tx = pulsarClient.newTransaction()
        .withTransactionTimeout(30, TimeUnit.SECONDS).build().get();

    try {
      for (String topic : availableTopics) {
        pulsarClient.newProducer(Schema.BYTES)
            // disable timeout to support transaction
            .sendTimeout(0, TimeUnit.SECONDS)
            .topic(topic).create()
            .newMessage(tx).value(message)
            .send();
      }
      tx.commit();
      LOG.info("finished flips sending, cost {}ms.", System.currentTimeMillis() - start);
    } catch (Throwable e) {
      LOG.error("send flip error", e);
      tx.abort();
      throw new FlinkRuntimeException(e);
    }
  }

  @Override
  public void flush() throws IOException {
    while (pendingMessages != 0) {
      producerRegister.flush();
      LOG.info("Flush the pending messages to Pulsar. Still in pending: {}", pendingMessages);
      Thread.yield();
    }
  }

  @Override
  public void close() throws Exception {
    IOUtils.closeAll(pulsarClient, metadataListener, producerRegister);
  }

  private void requirePermits() throws InterruptedException {
    while (pendingMessages >= configuration.getMaxPendingMessages()) {
      LOG.info("Waiting for the available permits.");
      Thread.yield();
    }
    pendingMessages++;
  }

  private void releasePermits() {
    pendingMessages -= 1;
  }

  private TypedMessageBuilder<?> createMessageBuilder(
      String topic, byte[] logData) {
    TypedMessageBuilder<byte[]> builder = producerRegister.createMessageBuilder(topic, Schema.BYTES);

    // Schema evolution would serialize the message by Pulsar Schema in TypedMessageBuilder.
    // The type has been checked in PulsarMessageBuilder#value.
    builder.value(logData);

    return builder;
  }

}
