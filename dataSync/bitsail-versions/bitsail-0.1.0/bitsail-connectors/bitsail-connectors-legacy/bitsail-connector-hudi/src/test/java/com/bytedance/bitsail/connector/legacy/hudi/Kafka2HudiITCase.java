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

package com.bytedance.bitsail.connector.legacy.hudi;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.hudi.configuration.FlinkOptions;
import com.bytedance.bitsail.connector.legacy.messagequeue.source.option.BaseMessageQueueReaderOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.testcontainers.kafka.KafkaCluster;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bytedance.bitsail.connector.legacy.kafka.constants.KafkaConstants.CONNECTOR_STARTUP_MODE_VALUE_EARLIEST;
import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR;

public class Kafka2HudiITCase {
  private static final Logger LOG = LoggerFactory.getLogger(Kafka2HudiITCase.class);
  private static final String[] COLUMNS = {"id", "text", "timestamp"};
  private static final String TEST_SCHEMA =
      "[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"text\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"string\"}]";
  private static final long START_TIME = 1658743200L;
  private static final long SEND_BATCH = 2000L;
  private static final long SEND_INTERVAL = 10L;
  private static final String WRITER_PREFIX = "job.writer.";
  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  private final String topicName = "testTopic";
  private final KafkaCluster kafkaDocker = new KafkaCluster();
  @TempDir
  File tempFile;
  private KafkaProducer kafkaProducer;
  private ScheduledExecutorService produceService;

  private static String constructARecord(int index) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(COLUMNS[0], index);
    jsonObject.put(COLUMNS[1], "text_" + index);
    jsonObject.put(COLUMNS[2], "" + (START_TIME + index));
    return jsonObject.toJSONString();
  }

  @BeforeEach
  public void before() {
    kafkaDocker.startService();
    kafkaDocker.createTopic(topicName);
    kafkaProducer = kafkaDocker.getProducer(topicName);
    startSendDataToKafka();

    environmentVariables.set("HADOOP_USER_NAME", "haoke");
  }

  @AfterEach
  public void after() {
    produceService.shutdown();
    kafkaProducer.close();
    kafkaDocker.stopService();

    produceService = null;
    kafkaProducer = null;
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

  protected void setStreamingConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(CommonOptions.JOB_TYPE, "STREAMING");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_LIB_PATH, "plugin");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_CONF_PATH, "plugin_conf");
    jobConfiguration.set(CommonOptions.ENABLE_DYNAMIC_LOADER, true);
  }

  protected void setReaderConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(ReaderOptions.READER_CLASS, "com.bytedance.bitsail.connector.legacy.kafka.source" +
        ".KafkaSourceFunctionDAGBuilder");
    setConnectorProps(jobConfiguration, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaDocker.getBootstrapServer());
    setConnectorProps(jobConfiguration, ConsumerConfig.GROUP_ID_CONFIG, "test_consumer");
    setConnectorProps(jobConfiguration, ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    setConnectorProps(jobConfiguration, "topic", topicName);
    setConnectorProps(jobConfiguration, "startup-mode", CONNECTOR_STARTUP_MODE_VALUE_EARLIEST);

    jobConfiguration.set(ReaderOptions.READER_METRIC_TAG_NAME, "kafka");
    jobConfiguration.set(BaseMessageQueueReaderOptions.ENABLE_COUNT_MODE, true);
    jobConfiguration.set(BaseMessageQueueReaderOptions.COUNT_MODE_RECORD_THRESHOLD, 2000L);
    jobConfiguration.set(BaseMessageQueueReaderOptions.FORMAT_TYPE.key(), "json");
    jobConfiguration.set(ReaderOptions.BaseReaderOptions.COLUMNS, JsonSerializer.parseToList(TEST_SCHEMA, ColumnInfo.class));
  }

  protected void setWriterConfiguration(BitSailConfiguration jobConfiguration) {
    jobConfiguration.set(WriterOptions.WRITER_CLASS, "com.bytedance.bitsail.connector.legacy.hudi.dag" +
        ".HudiSinkFunctionDAGBuilder");

    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.RECORD_KEY_FIELD.key(), "id");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.INDEX_KEY_FIELD.key(), "id");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.PRECOMBINE_FIELD.key(), "timestamp");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.PATH.key(), tempFile.getAbsolutePath());
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.TABLE_NAME.key(), "test_table");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.TABLE_TYPE.key(), "MERGE_ON_READ");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.OPERATION.key(), "upsert");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.INDEX_TYPE.key(), "BUCKET");
    jobConfiguration.set(WRITER_PREFIX + FlinkOptions.BUCKET_INDEX_NUM_BUCKETS.key(), "4");
    jobConfiguration.set(WriterOptions.BaseWriterOptions.COLUMNS, JsonSerializer.parseToList(TEST_SCHEMA, ColumnInfo.class));
  }

  private void setConnectorProps(BitSailConfiguration jobConfiguration, String key, String value) {
    String path = BaseMessageQueueReaderOptions.CONNECTOR_PROPERTIES.key() + "." + CONNECTOR + "." + key;
    if (!jobConfiguration.fieldExists(path)) {
      jobConfiguration.set(path, value);
    }
  }

  @Test
  public void testKafka2Hudi() throws Exception {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    setStreamingConfiguration(jobConf);
    setReaderConfiguration(jobConf);
    setWriterConfiguration(jobConf);
    EmbeddedFlinkCluster.submitJob(jobConf);
  }
}
