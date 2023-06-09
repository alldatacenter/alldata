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

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.store.easy.json.loader.JsonLoaderOptions;
import org.apache.drill.exec.store.easy.json.loader.ClosingStreamIterator;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;
import org.apache.drill.exec.store.kafka.KafkaStoragePlugin;
import org.apache.drill.exec.store.kafka.MetaDataField;
import org.apache.drill.exec.store.kafka.ReadOptions;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Properties;
import java.util.StringJoiner;

/**
 * MessageReader class which will convert ConsumerRecord into JSON and writes to
 * VectorContainerWriter of JsonReader
 */
public class JsonMessageReader implements MessageReader {

  private static final Logger logger = LoggerFactory.getLogger(JsonMessageReader.class);

  private final ClosingStreamIterator stream = new ClosingStreamIterator();

  private KafkaJsonLoader kafkaJsonLoader;
  private ResultSetLoader resultSetLoader;
  private SchemaNegotiator negotiator;
  private ReadOptions readOptions;
  private Properties kafkaConsumerProps;

  @Override
  public void init(SchemaNegotiator negotiator, ReadOptions readOptions, KafkaStoragePlugin plugin) {
    this.negotiator = negotiator;
    this.resultSetLoader = negotiator.build();
    this.readOptions = readOptions;
    this.kafkaConsumerProps = plugin.getConfig().getKafkaConsumerProps();
  }

  @Override
  public void readMessage(ConsumerRecord<?, ?> record) {
    byte[] recordArray = (byte[]) record.value();
    try {
      parseAndWrite(record, recordArray);
    } catch (TokenIterator.RecoverableJsonException e) {
      if (!readOptions.isSkipInvalidRecords()) {
        throw UserException.dataReadError(e)
            .message(String.format("Error happened when parsing invalid record. " +
                "Please set `%s` option to 'true' to skip invalid records.", ExecConstants.KAFKA_READER_SKIP_INVALID_RECORDS))
            .addContext(resultSetLoader.errorContext())
            .build(logger);
      }
    }
  }

  private void parseAndWrite(ConsumerRecord<?, ?> record, byte[] recordArray) {
    stream.setValue(new ByteArrayInputStream(recordArray));
    if (kafkaJsonLoader == null) {
      JsonLoaderOptions jsonLoaderOptions = new JsonLoaderOptions();
      jsonLoaderOptions.allTextMode = readOptions.isAllTextMode();
      jsonLoaderOptions.readNumbersAsDouble = readOptions.isReadNumbersAsDouble();
      jsonLoaderOptions.skipMalformedRecords = readOptions.isSkipInvalidRecords();
      jsonLoaderOptions.allowNanInf = readOptions.isAllowNanInf();
      jsonLoaderOptions.enableEscapeAnyChar = readOptions.isAllowEscapeAnyChar();
      jsonLoaderOptions.skipMalformedDocument = readOptions.isSkipInvalidRecords();

      kafkaJsonLoader = (KafkaJsonLoader) new KafkaJsonLoader.KafkaJsonLoaderBuilder()
          .resultSetLoader(resultSetLoader)
          .standardOptions(negotiator.queryOptions())
          .options(jsonLoaderOptions)
          .errorContext(negotiator.parentErrorContext())
          .fromStream(() -> stream)
          .build();
    }

    RowSetLoader rowWriter = resultSetLoader.writer();
    rowWriter.start();
    if (kafkaJsonLoader.parser().next()) {
      writeValue(rowWriter, MetaDataField.KAFKA_TOPIC, record.topic());
      writeValue(rowWriter, MetaDataField.KAFKA_PARTITION_ID, record.partition());
      writeValue(rowWriter, MetaDataField.KAFKA_OFFSET, record.offset());
      writeValue(rowWriter, MetaDataField.KAFKA_TIMESTAMP, record.timestamp());
      writeValue(rowWriter, MetaDataField.KAFKA_MSG_KEY, record.key() != null ? record.key().toString() : null);
      rowWriter.save();
    }
  }

  private <T> void writeValue(RowSetLoader rowWriter, MetaDataField metaDataField, T value) {
    if (rowWriter.tupleSchema().column(metaDataField.getFieldName()) == null) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(metaDataField.getFieldName(), metaDataField.getFieldType(), TypeProtos.DataMode.OPTIONAL);
      rowWriter.addColumn(colSchema);
    }
    rowWriter.column(metaDataField.getFieldName()).setObject(value);
  }

  @Override
  public ResultSetLoader getResultSetLoader() {
    return resultSetLoader;
  }

  @Override
  public KafkaConsumer<byte[], byte[]> getConsumer(KafkaStoragePlugin plugin) {
    return new KafkaConsumer<>(plugin.getConfig().getKafkaConsumerProps(),
      new ByteArrayDeserializer(), new ByteArrayDeserializer());
  }

  @Override
  public boolean endBatch() {
    kafkaJsonLoader.endBatch();
    return resultSetLoader.hasRows();
  }

  @Override
  public void close() {
    try {
      kafkaJsonLoader.close();
      resultSetLoader.close();
    } catch (Exception e) {
      logger.warn("Error while closing JsonMessageReader: {}", e.getMessage());
    }
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", JsonMessageReader.class.getSimpleName() + "[", "]")
        .add("kafkaJsonLoader=" + kafkaJsonLoader)
        .add("resultSetLoader=" + resultSetLoader)
        .toString();
  }
}
