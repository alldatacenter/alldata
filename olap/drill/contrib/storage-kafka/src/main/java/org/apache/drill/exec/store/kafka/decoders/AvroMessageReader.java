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

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.ColumnConverter;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.store.avro.AvroColumnConverterFactory;
import org.apache.drill.exec.store.kafka.KafkaStoragePlugin;
import org.apache.drill.exec.store.kafka.MetaDataField;
import org.apache.drill.exec.store.kafka.ReadOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class AvroMessageReader implements MessageReader {
  private static final Logger logger = LoggerFactory.getLogger(AvroMessageReader.class);

  private KafkaAvroDeserializer deserializer;
  private ColumnConverter converter;
  private ResultSetLoader loader;
  private boolean deserializeKey;

  @Override
  public void init(SchemaNegotiator negotiator, ReadOptions readOptions, KafkaStoragePlugin plugin) {
    Properties kafkaConsumerProps = plugin.getConfig().getKafkaConsumerProps();
    Map<String, Object> propertiesMap = kafkaConsumerProps.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
    deserializer = new KafkaAvroDeserializer(null, propertiesMap);
    TupleMetadata providedSchema = negotiator.providedSchema();
    loader = negotiator.build();
    AvroColumnConverterFactory factory = new AvroColumnConverterFactory(providedSchema);
    converter = factory.getRootConverter(providedSchema, new TupleSchema(), loader.writer());

    String keyDeserializer = kafkaConsumerProps.getProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
    deserializeKey = keyDeserializer != null && keyDeserializer.equals(KafkaAvroDeserializer.class.getName());
  }

  @Override
  public void readMessage(ConsumerRecord<?, ?> record) {
    RowSetLoader rowWriter = loader.writer();
    byte[] recordArray = (byte[]) record.value();
    GenericRecord genericRecord = (GenericRecord) deserializer.deserialize(null, recordArray);

    Schema schema = genericRecord.getSchema();

    if (Schema.Type.RECORD != schema.getType()) {
      throw UserException.dataReadError()
          .message(String.format("Root object must be record type. Found: %s", schema.getType()))
          .addContext("Reader", this)
          .build(logger);
    }

    rowWriter.start();
    converter.convert(genericRecord);
    writeValue(rowWriter, MetaDataField.KAFKA_TOPIC, record.topic());
    writeValue(rowWriter, MetaDataField.KAFKA_PARTITION_ID, record.partition());
    writeValue(rowWriter, MetaDataField.KAFKA_OFFSET, record.offset());
    writeValue(rowWriter, MetaDataField.KAFKA_TIMESTAMP, record.timestamp());
    writeValue(rowWriter, MetaDataField.KAFKA_MSG_KEY, record.key() != null ? getKeyValue((byte[]) record.key()) : null);
    rowWriter.save();
  }

  private Object getKeyValue(byte[] keyValue) {
    if (deserializeKey) {
      return deserializer.deserialize(null, keyValue).toString();
    } else {
      return new String(keyValue);
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
  public KafkaConsumer<byte[], byte[]> getConsumer(KafkaStoragePlugin plugin) {
    return new KafkaConsumer<>(plugin.getConfig().getKafkaConsumerProps(),
        new ByteArrayDeserializer(), new ByteArrayDeserializer());
  }

  @Override
  public ResultSetLoader getResultSetLoader() {
    return loader;
  }

  @Override
  public boolean endBatch() {
    return loader.hasRows();
  }

  @Override
  public void close() throws IOException {
    try {
      deserializer.close();
      loader.close();
    } catch (Exception e) {
      logger.warn("Error while closing AvroMessageReader: {}", e.getMessage());
    }
  }
}
