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
package org.apache.drill.exec.store.kafka.decoders;

import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.store.kafka.KafkaStoragePlugin;
import org.apache.drill.exec.store.kafka.ReadOptions;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.Closeable;

/**
 * MessageReader interface provides mechanism to handle various Kafka Message
 * Formats like JSON, AVRO or custom message formats.
 */
public interface MessageReader extends Closeable {

  void init(SchemaNegotiator negotiator, ReadOptions readOptions, KafkaStoragePlugin plugin);

  void readMessage(ConsumerRecord<?, ?> message);

  KafkaConsumer<byte[], byte[]> getConsumer(KafkaStoragePlugin plugin);

  ResultSetLoader getResultSetLoader();

  boolean endBatch();
}
