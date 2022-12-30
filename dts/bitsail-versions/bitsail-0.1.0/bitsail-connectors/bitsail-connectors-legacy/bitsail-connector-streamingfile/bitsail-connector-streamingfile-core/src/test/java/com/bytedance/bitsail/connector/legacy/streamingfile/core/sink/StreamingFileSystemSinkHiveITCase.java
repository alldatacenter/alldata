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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.testcontainers.kafka.KafkaCluster;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created 2022/7/26
 */
@Ignore
public class StreamingFileSystemSinkHiveITCase {
  private static final Logger LOG = LoggerFactory.getLogger(StreamingFileSystemSinkHiveITCase.class);

  private static final String HIVE_SITE_CONF_FILE = "hive-site.xml";
  private static final String HIVE_METASTORE_PROPERTIES;

  private static final String[] COLUMNS = {"id", "text", "timestamp"};
  private static final long START_TIME = 1658793600L;
  private static final long SEND_BATCH = 100L;
  private static final long SEND_INTERVAL = 5L;

  static {
    URL hiveConfUrl = StreamingFileSystemSinkHiveITCase.class.getClassLoader().getResource(HIVE_SITE_CONF_FILE);
    HiveConf hiveConf = HiveMetaClientUtil.getHiveConf(hiveConfUrl.getPath());
    HashMap<String, String> properties = Maps.newHashMap();
    properties.put("", "");
    HIVE_METASTORE_PROPERTIES = JsonSerializer.serialize(properties);
    LOG.info("hiveConfUrl: {}", hiveConfUrl);
    LOG.info("HIVE_METASTORE_PROPERTIES: {}", HIVE_METASTORE_PROPERTIES);
  }

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  private final KafkaCluster kafkaDocker = new KafkaCluster();
  private final String topicName = "testTopic";
  private KafkaProducer kafkaProducer;
  private ScheduledExecutorService produceService;

  private static String constructARecord(int index) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(COLUMNS[0], index);
    jsonObject.put(COLUMNS[1], "text_" + index);
    jsonObject.put(COLUMNS[2], "" + (START_TIME + index));
    return jsonObject.toJSONString();
  }

  @Before
  public void before() {
    kafkaDocker.startService();
    kafkaDocker.createTopic(topicName);
    kafkaProducer = kafkaDocker.getProducer(topicName);
    startSendDataToKafka();

    environmentVariables.set("HADOOP_USER_NAME", "root");
  }

  @After
  public void after() {
    produceService.shutdown();
    kafkaProducer.close();
    kafkaDocker.stopService();

    produceService = null;
    kafkaProducer = null;
  }

  protected void updateConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(FileSystemSinkOptions.HIVE_METASTORE_PROPERTIES, HIVE_METASTORE_PROPERTIES);
  }

  private void startSendDataToKafka() {
    produceService = new ScheduledThreadPoolExecutor(1);
    AtomicInteger sendCount = new AtomicInteger(0);
    produceService.scheduleAtFixedRate(() -> {
      LOG.info(">>> send count: {}", sendCount.get());
      try {
        for (int i = 0; i < SEND_BATCH; ++i) {
          String record = constructARecord(sendCount.getAndIncrement());
          kafkaProducer.send(new ProducerRecord(topicName, record));
          if (i % SEND_BATCH == (SEND_BATCH - 1)) {
            LOG.info("batch last record: {}", record);
          }
        }
      } catch (Exception e) {
        LOG.error("failed to send a record");
      } finally {
        LOG.info(">>> kafka produce count: {}", sendCount.get());
      }
    }, 0, SEND_INTERVAL, TimeUnit.SECONDS);
  }

  @Test
  public void testHiveFormatType() throws Exception {
    BitSailConfiguration hdfsConfiguration = BitSailConfiguration.newDefault();
    updateConfiguration(hdfsConfiguration);
    EmbeddedFlinkCluster.submitJob(hdfsConfiguration);
  }
}