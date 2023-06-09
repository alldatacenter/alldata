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

import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class KafkaMessageGenerator {

  private static final Logger logger = LoggerFactory.getLogger(KafkaMessageGenerator.class);

  public static final String SCHEMA_REGISTRY_URL = "mock://testurl";

  private final Properties producerProperties = new Properties();

  public KafkaMessageGenerator (final String broker, Class<?> valueSerializer) {
    producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
    producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
    producerProperties.put(ProducerConfig.RETRIES_CONFIG, 3);
    producerProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    producerProperties.put(ProducerConfig.LINGER_MS_CONFIG, 0);
    producerProperties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    producerProperties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
    producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "drill-test-kafka-client");
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
    producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); //So that retries do not cause duplicates
    producerProperties.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
  }

  public void populateAvroMsgIntoKafka(String topic, int numMsg) {
    producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    try (KafkaProducer<Object, GenericRecord> producer = new KafkaProducer<>(producerProperties)) {
      Schema.Parser parser = new Schema.Parser();
      String userSchema = "{\"type\":\"record\"," +
          "\"name\":\"myrecord\"," +
          "\"fields\":[" +
          "{\"name\":\"key1\",\"type\":\"string\"}," +
          "{\"name\":\"key2\",\"type\":\"int\"}," +
          "{\"name\":\"key3\",\"type\":\"boolean\"}," +
          "{\"name\":\"key5\",\"type\":{\"type\":\"array\",\"items\":\"int\"}}," +
          "{\"name\":\"key6\",\"type\":{\"type\":\"record\",\"name\":\"myrecord6\",\"fields\":[" +
          "{\"name\":\"key61\",\"type\":\"double\"}," +
          "{\"name\":\"key62\",\"type\":\"double\"}]}}]}";
      Schema valueSchema = parser.parse(userSchema);
      GenericRecordBuilder valueBuilder = new GenericRecordBuilder(valueSchema);

      String key1Schema = "{\"type\":\"record\"," +
              "\"name\":\"key1record\"," +
              "\"fields\":[" +
              "{\"name\":\"key1\",\"type\":\"string\"}]}\"";
      Schema keySchema = parser.parse(key1Schema);
      GenericRecordBuilder keyBuilder = new GenericRecordBuilder(keySchema);

      Random rand = new Random();
      for (int i = 0; i < numMsg; ++i) {
        // value record
        String key1 = UUID.randomUUID().toString();
        valueBuilder.set("key1", key1);
        valueBuilder.set("key2", rand.nextInt());
        valueBuilder.set("key3", rand.nextBoolean());

        List<Integer> list = Lists.newArrayList();
        list.add(rand.nextInt(100));
        list.add(rand.nextInt(100));
        list.add(rand.nextInt(100));
        valueBuilder.set("key5", list);

        GenericRecordBuilder innerBuilder = new GenericRecordBuilder(valueSchema.getField("key6").schema());
        innerBuilder.set("key61", rand.nextDouble());
        innerBuilder.set("key62", rand.nextDouble());
        valueBuilder.set("key6", innerBuilder.build());

        Record producerRecord = valueBuilder.build();

        // key record
        keyBuilder.set("key1", key1);
        Record keyRecord = keyBuilder.build();

        ProducerRecord<Object, GenericRecord> record = new ProducerRecord<>(topic, keyRecord, producerRecord);
        producer.send(record);
      }
    }
  }

  public void populateJsonMsgIntoKafka(String topic, int numMsg) throws ExecutionException, InterruptedException {
    try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties)) {
      Random rand = new Random();
      for (int i = 0; i < numMsg; ++i) {
        JsonObject object = new JsonObject();
        object.addProperty("key1", UUID.randomUUID().toString());
        object.addProperty("key2", rand.nextInt());
        object.addProperty("key3", rand.nextBoolean());

        JsonArray element2 = new JsonArray();
        element2.add(new JsonPrimitive(rand.nextInt(100)));
        element2.add(new JsonPrimitive(rand.nextInt(100)));
        element2.add(new JsonPrimitive(rand.nextInt(100)));

        object.add("key5", element2);

        JsonObject element3 = new JsonObject();
        element3.addProperty("key61", rand.nextDouble());
        element3.addProperty("key62", rand.nextDouble());
        object.add("key6", element3);

        ProducerRecord<String, String> message = new ProducerRecord<>(topic, object.toString());
        logger.info("Publishing message : {}", message);
        Future<RecordMetadata> future = producer.send(message);
        logger.info("Committed offset of the message : {}", future.get().offset());
      }
    }
  }

  public void populateJsonMsgWithTimestamps(String topic, int numMsg) throws ExecutionException, InterruptedException {
    try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties)) {
      int halfCount = numMsg / 2;

      for (PartitionInfo tpInfo : producer.partitionsFor(topic)) {
        for (int i = 1; i <= numMsg; ++i) {
          JsonObject object = new JsonObject();
          object.addProperty("stringKey", UUID.randomUUID().toString());
          object.addProperty("intKey", numMsg - i);
          object.addProperty("boolKey", i % 2 == 0);

          long timestamp = i < halfCount ? (halfCount - i) : i;
          ProducerRecord<String, String> message = new ProducerRecord<>(tpInfo.topic(), tpInfo.partition(), timestamp, "key" + i, object.toString());
          logger.info("Publishing message : {}", message);
          Future<RecordMetadata> future = producer.send(message);
          logger.info("Committed offset of the message : {}", future.get().offset());
        }
      }
    }
  }

  public void populateMessages(String topic, String... messages) throws ExecutionException, InterruptedException {
    try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties)) {
      for (String content : messages) {
        ProducerRecord<String, String> message = new ProducerRecord<>(topic, content);
        logger.info("Publishing message : {}", message);
        Future<RecordMetadata> future = producer.send(message);
        logger.info("Committed offset of the message : {}", future.get().offset());
      }
    }
  }
}
