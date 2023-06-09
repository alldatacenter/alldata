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

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import kafka.zk.KafkaZkClient;
import org.apache.drill.categories.KafkaStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.ZookeeperTestUtil;
import org.apache.drill.exec.store.kafka.cluster.EmbeddedKafkaCluster;
import org.apache.drill.exec.store.kafka.decoders.MessageReaderFactoryTest;
import org.apache.drill.test.BaseTest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.security.JaasUtils;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Time;
import org.apache.zookeeper.client.ZKClientConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Category({KafkaStorageTest.class, SlowTest.class})
@RunWith(Suite.class)
@SuiteClasses({KafkaQueriesTest.class, MessageIteratorTest.class, MessageReaderFactoryTest.class, KafkaFilterPushdownTest.class})
public class TestKafkaSuit extends BaseTest {

  private static final Logger logger = LoggerFactory.getLogger(TestKafkaSuit.class);

  private static final String LOGIN_CONF_RESOURCE_PATHNAME = "login.conf";

  public static EmbeddedKafkaCluster embeddedKafkaCluster;

  private static KafkaZkClient zkClient;

  private static final AtomicInteger initCount = new AtomicInteger(0);

  static final int NUM_JSON_MSG = 10;

  private static final int CONN_TIMEOUT = 8 * 1000;

  private static final int SESSION_TIMEOUT = 10 * 1000;

  private static volatile boolean runningSuite = true;

  @BeforeClass
  public static void initKafka() throws Exception {
    synchronized (TestKafkaSuit.class) {
      if (initCount.get() == 0) {
        ZookeeperTestUtil.setZookeeperSaslTestConfigProps();
        System.setProperty(JaasUtils.JAVA_LOGIN_CONFIG_PARAM, ClassLoader.getSystemResource(LOGIN_CONF_RESOURCE_PATHNAME).getFile());
        embeddedKafkaCluster = new EmbeddedKafkaCluster();
        zkClient = KafkaZkClient.apply(embeddedKafkaCluster.getZkServer().getConnectionString(),
            false, SESSION_TIMEOUT, CONN_TIMEOUT, 0, Time.SYSTEM,
            "kafka.server", "SessionExpireListener",
            Option.<String>empty(), Option.<ZKClientConfig>empty());
        createTopicHelper(TestQueryConstants.JSON_TOPIC, 1);
        createTopicHelper(TestQueryConstants.AVRO_TOPIC, 1);
        KafkaMessageGenerator generator = new KafkaMessageGenerator(embeddedKafkaCluster.getKafkaBrokerList(), StringSerializer.class);
        KafkaMessageGenerator avroGenerator = new KafkaMessageGenerator(embeddedKafkaCluster.getKafkaBrokerList(), KafkaAvroSerializer.class);
        generator.populateJsonMsgIntoKafka(TestQueryConstants.JSON_TOPIC, NUM_JSON_MSG);
        avroGenerator.populateAvroMsgIntoKafka(TestQueryConstants.AVRO_TOPIC, NUM_JSON_MSG);
      }
      initCount.incrementAndGet();
      runningSuite = true;
    }
    logger.info("Initialized Embedded Zookeeper and Kafka");
  }

  public static boolean isRunningSuite() {
    return runningSuite;
  }

  @AfterClass
  public static void tearDownCluster() {
    synchronized (TestKafkaSuit.class) {
      if (initCount.decrementAndGet() == 0) {
        if (zkClient != null) {
          zkClient.close();
          zkClient = null;
        }
        if (embeddedKafkaCluster != null && !embeddedKafkaCluster.getBrokers().isEmpty()) {
          embeddedKafkaCluster.shutDownCluster();
          embeddedKafkaCluster = null;
        }
      }
    }
  }

  public static void createTopicHelper(String topicName, int partitions) throws ExecutionException, InterruptedException {
    try (AdminClient adminClient = initAdminClient()) {
      NewTopic newTopic = new NewTopic(topicName, partitions, (short) 1);
      Map<String, String> topicConfigs = new HashMap<>();
      topicConfigs.put(TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG, "CreateTime");
      topicConfigs.put(TopicConfig.RETENTION_MS_CONFIG, "-1");
      newTopic.configs(topicConfigs);
      CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(newTopic));
      result.all().get();
    }
  }

  private static AdminClient initAdminClient() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaCluster.getKafkaBrokerList());
    return AdminClient.create(props);
  }
}
