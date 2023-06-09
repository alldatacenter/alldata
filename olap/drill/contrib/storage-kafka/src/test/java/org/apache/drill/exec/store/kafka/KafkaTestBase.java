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

import java.util.Map;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.kafka.cluster.EmbeddedKafkaCluster;
import org.apache.drill.exec.store.kafka.decoders.JsonMessageReader;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

import static org.apache.drill.exec.store.kafka.KafkaMessageGenerator.SCHEMA_REGISTRY_URL;

public class KafkaTestBase extends ClusterTest {
  protected static KafkaStoragePluginConfig storagePluginConfig;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // Make sure this test is only running as part of the suit
    Assume.assumeTrue(TestKafkaSuit.isRunningSuite());
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
    TestKafkaSuit.initKafka();
    initKafkaStoragePlugin(TestKafkaSuit.embeddedKafkaCluster);
  }

  public static void initKafkaStoragePlugin(EmbeddedKafkaCluster embeddedKafkaCluster) throws Exception {
    final StoragePluginRegistry pluginRegistry = cluster.drillbit().getContext().getStorage();
    Map<String, String> kafkaConsumerProps = Maps.newHashMap();
    kafkaConsumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaCluster.getKafkaBrokerList());
    kafkaConsumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "drill-test-consumer");
    kafkaConsumerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
    storagePluginConfig = new KafkaStoragePluginConfig(kafkaConsumerProps);
    storagePluginConfig.setEnabled(true);
    pluginRegistry.put(KafkaStoragePluginConfig.NAME, storagePluginConfig);
    client.alterSession(ExecConstants.KAFKA_RECORD_READER, JsonMessageReader.class.getName());
    client.alterSession(ExecConstants.KAFKA_POLL_TIMEOUT, 5000);
  }

  public void runKafkaSQLVerifyCount(String sql, int expectedRowCount) {
    long rowCount = queryBuilder().sql(sql).log();
    if (expectedRowCount != -1) {
      Assert.assertEquals(expectedRowCount, rowCount);
    }
  }

  public static long testSql(String sql) {
    return client.queryBuilder().sql(sql).log();
  }

  @AfterClass
  public static void tearDownKafkaTestBase() {
    if (TestKafkaSuit.isRunningSuite()) {
      TestKafkaSuit.tearDownCluster();
    }
  }
}