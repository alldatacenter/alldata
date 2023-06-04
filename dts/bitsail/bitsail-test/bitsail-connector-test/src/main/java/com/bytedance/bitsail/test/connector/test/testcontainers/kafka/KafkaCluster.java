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

package com.bytedance.bitsail.test.connector.test.testcontainers.kafka;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created 2022/7/26
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class KafkaCluster {

  private static final Logger LOG = LoggerFactory.getLogger(KafkaCluster.class);

  private static final DockerImageName KAFKA_DOCKER_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:4.0.1");

  private static final String BOOTSTRAP_SERVER = "PLAINTEXT://localhost:9092";

  private KafkaContainers kafkaContainer;
  private AdminClient kafkaAdminClient;

  public static String getBootstrapServer() {
    return BOOTSTRAP_SERVER;
  }

  public void startService() {
    kafkaContainer = new KafkaContainers(KAFKA_DOCKER_IMAGE);
    kafkaContainer.setPortBindings(Arrays.asList("9092:9092", "9093:9093", "2181:2181"));
    kafkaContainer.start();
    kafkaContainer.waitingFor(Wait.defaultWaitStrategy());
    LOG.info("Successfully start kafka service, bootstrap server: {}", kafkaContainer.getBootstrapServers());
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    kafkaAdminClient = KafkaAdminClient.create(properties);
  }

  @SneakyThrows
  public void createTopic(String topicName) {
    CreateTopicsResult createTopicsResult = kafkaAdminClient
        .createTopics(Lists.newArrayList(new NewTopic(topicName, 1, (short) 1)));
  }

  public void stopService() {
    if (Objects.nonNull(kafkaContainer)) {
      kafkaContainer.close();
    }
    kafkaContainer = null;
  }

  public List<ConsumerRecord<?, ?>> consumerTopic(String topicName, int maxConsumeCount, int maxWaitSeconds) {
    List<ConsumerRecord<?, ?>> recordList = new ArrayList<>();
    try (
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
            ImmutableMap.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER,
                ConsumerConfig.GROUP_ID_CONFIG, "validation-consumer",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
            ),
            new StringDeserializer(),
            new StringDeserializer()
        )
    ) {
      consumer.subscribe(Collections.singletonList(topicName));
      Unreliables.retryUntilTrue(
          maxWaitSeconds,
          TimeUnit.SECONDS,
          () -> {
            int consumeCount = 0;
            while (consumeCount < maxConsumeCount) {
              ConsumerRecords<String, String> records = consumer.poll(Duration.ofMinutes(3).toMillis());
              if (records.isEmpty()) {
                return false;
              }
              records.forEach(recordList::add);
              consumeCount += records.count();
            }
            return true;
          }
      );
      consumer.unsubscribe();
    }

    return recordList.subList(0, maxConsumeCount);
  }

  public KafkaProducer<String, String> getProducer(String topicName) {
    KafkaProducer<String, String> producer = new KafkaProducer<>(
        ImmutableMap.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER,
            ProducerConfig.CLIENT_ID_CONFIG, "producer"
        ),
        new StringSerializer(),
        new StringSerializer()
    );
    return producer;
  }
}
