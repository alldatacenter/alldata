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

package com.bytedance.bitsail.connector.legacy.kafka.source;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.testcontainers.kafka.KafkaCluster;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created 2022/8/31
 */
public class KafkaSourceITCase {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaSourceITCase.class);
  private static final int TOTAL_SEND_COUNT = 300;
  private final String topicName = "testTopic";
  private final KafkaCluster kafkaCluster = new KafkaCluster();

  private static String constructARecord(int index) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("ID", index);
    jsonObject.put("NAME", "text_" + index);
    jsonObject.put("DATE", System.currentTimeMillis());
    return jsonObject.toJSONString();
  }

  @Before
  public void before() {
    kafkaCluster.startService();
    kafkaCluster.createTopic(topicName);
    startSendDataToKafka();
  }

  private void startSendDataToKafka() {
    KafkaProducer<String, String> producer = kafkaCluster.getProducer(topicName);
    ScheduledThreadPoolExecutor produceService = new ScheduledThreadPoolExecutor(1);
    AtomicInteger sendCount = new AtomicInteger(0);
    produceService.scheduleAtFixedRate(() -> {
      try {
        for (int i = 0; i < 5000; ++i) {
          String record = constructARecord(sendCount.getAndIncrement());
          producer.send(new ProducerRecord(topicName, record));
        }
      } catch (Exception e) {
        LOG.error("failed to send a record");
      } finally {
        LOG.info(">>> kafka produce count: {}", sendCount.get());
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  @Test
  public void testKafkaSource() throws Exception {
    BitSailConfiguration configuration = JobConfUtils.fromClasspath("kafka_to_print.json");
    updateConfiguration(configuration);
    EmbeddedFlinkCluster.submitJob(configuration);
  }

  protected void updateConfiguration(BitSailConfiguration jobConfiguration) {
    //  jobConfiguration.set(FakeReaderOptions.TOTAL_COUNT, TOTAL_SEND_COUNT);

    Map<String, String> properties = Maps.newHashMap();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaCluster.getBootstrapServer());
    properties.put("topic", topicName);
    jobConfiguration.set("job.reader.connector.connector", properties);
  }

  @After
  public void after() {
    kafkaCluster.stopService();
  }

}