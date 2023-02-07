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
package org.apache.drill.exec.store.kafka;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.exceptions.ChildErrorContext;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.kafka.decoders.MessageReader;
import org.apache.drill.exec.store.kafka.decoders.MessageReaderFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class KafkaRecordReader implements ManagedReader<SchemaNegotiator> {
  private static final Logger logger = LoggerFactory.getLogger(KafkaRecordReader.class);

  private final ReadOptions readOptions;
  private final KafkaStoragePlugin plugin;
  private final KafkaPartitionScanSpec subScanSpec;
  private final int maxRecords;

  private MessageReader messageReader;
  private long currentOffset;
  private MessageIterator msgItr;

  public KafkaRecordReader(KafkaPartitionScanSpec subScanSpec, OptionManager options, KafkaStoragePlugin plugin, int maxRecords) {
    this.readOptions = new ReadOptions(options);
    this.plugin = plugin;
    this.subScanSpec = subScanSpec;
    this.maxRecords = maxRecords;
  }

  @Override
  public boolean open(SchemaNegotiator negotiator) {
    CustomErrorContext errorContext = new ChildErrorContext(negotiator.parentErrorContext()) {
      @Override
      public void addContext(UserException.Builder builder) {
        super.addContext(builder);
        builder.addContext("topic_name", subScanSpec.getTopicName());
      }
    };
    negotiator.setErrorContext(errorContext);

    messageReader = MessageReaderFactory.getMessageReader(readOptions.getMessageReader());
    messageReader.init(negotiator, readOptions, plugin);
    msgItr = new MessageIterator(messageReader.getConsumer(plugin), subScanSpec, readOptions.getPollTimeOut());

    return true;
  }

  /**
   * KafkaConsumer.poll will fetch 500 messages per poll call. So hasNext will
   * take care of polling multiple times for this given batch next invocation
   */
  @Override
  public boolean next() {
    RowSetLoader rowWriter = messageReader.getResultSetLoader().writer();
    while (!rowWriter.isFull()) {
      if (!nextLine(rowWriter)) {
        return false;
      }
    }
    return messageReader.endBatch();
  }

  private boolean nextLine(RowSetLoader rowWriter) {
    if (rowWriter.limitReached(maxRecords)) {
      return false;
    }

    if (currentOffset >= subScanSpec.getEndOffset() || !msgItr.hasNext()) {
      return false;
    }
    ConsumerRecord<byte[], byte[]> consumerRecord = msgItr.next();
    currentOffset = consumerRecord.offset();
    messageReader.readMessage(consumerRecord);
    return true;
  }

  @Override
  public void close() {
    logger.debug("Last offset processed for {}:{} is - {}", subScanSpec.getTopicName(), subScanSpec.getPartitionId(),
        currentOffset);
    logger.debug("Total time to fetch messages from {}:{} is - {} milliseconds", subScanSpec.getTopicName(),
        subScanSpec.getPartitionId(), msgItr.getTotalFetchTime());
    plugin.registerToClose(msgItr);
    try {
      messageReader.close();
    } catch (IOException e) {
      logger.warn("Error closing Kafka message reader: {}", e.getMessage(), e);
    }
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("readOptions", readOptions)
        .field("currentOffset", currentOffset)
        .toString();
  }
}
