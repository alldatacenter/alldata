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

package com.netease.arctic.flink.read.source.log.pulsar;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.synchronization.FutureCompletingBlockingQueue;
import org.apache.flink.connector.pulsar.source.config.SourceConfiguration;
import org.apache.flink.connector.pulsar.source.reader.message.PulsarMessage;
import org.apache.flink.connector.pulsar.source.reader.source.PulsarOrderedSourceReader;
import org.apache.flink.connector.pulsar.source.reader.split.PulsarOrderedPartitionSplitReader;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.PulsarClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * The source reader for log pulsar subscription Exclusive, which consumes the ordered messages.
 * To support LogPulsarRecordEmitter.
 */
@Internal
public class LogPulsarOrderedSourceReader<OUT> extends PulsarOrderedSourceReader<OUT> {
  private static final Logger LOG = LoggerFactory.getLogger(LogPulsarOrderedSourceReader.class);

  public LogPulsarOrderedSourceReader(
      FutureCompletingBlockingQueue<RecordsWithSplitIds<PulsarMessage<OUT>>> elementsQueue,
      Supplier<PulsarOrderedPartitionSplitReader<OUT>> splitReaderSupplier,
      SourceReaderContext context,
      SourceConfiguration sourceConfiguration,
      PulsarClient pulsarClient,
      PulsarAdmin pulsarAdmin) {
    super(
        elementsQueue,
        splitReaderSupplier,
        context,
        sourceConfiguration,
        pulsarClient,
        pulsarAdmin,
        new LogPulsarRecordEmitter<>());
  }
}
