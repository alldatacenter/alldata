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
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.connector.legacy.messagequeue.source.option.BaseMessageQueueReaderOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.testcontainers.kafka.KafkaCluster;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bytedance.bitsail.connector.legacy.kafka.constants.KafkaConstants.CONNECTOR_STARTUP_MODE_VALUE_EARLIEST;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_JSON;
import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR;

/**
 * Created 2022/7/26
 */
public class StreamingFileSystemSinkHdfsITCase {
  private static final Logger LOG = LoggerFactory.getLogger(StreamingFileSystemSinkHdfsITCase.class);
  private static final String LOCAL_FS = "file://";
  private static final String OUTPUT_DIR = LOCAL_FS + "/tmp/streaming_file_hdfs/";
  private static final String[] COLUMNS = {"id", "text", "timestamp"};
  private static final long START_TIME = 1658743200L;
  private static final long SEND_BATCH = 2000L;
  private static final long SEND_INTERVAL = 1L;
  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  private final String topicName = "testTopic";
  private final KafkaCluster kafkaDocker = new KafkaCluster();
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
    jobConfiguration.set(CommonOptions.JOB_TYPE, "STREAMING");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_LIB_PATH, "plugin");
    jobConfiguration.set(CommonOptions.JOB_PLUGIN_CONF_PATH, "plugin_conf");
    jobConfiguration.set(CommonOptions.ENABLE_DYNAMIC_LOADER, true);
    jobConfiguration.set(ReaderOptions.READER_CLASS, "com.bytedance.bitsail.connector.legacy.kafka.source" +
        ".KafkaSourceFunctionDAGBuilder");
    jobConfiguration.set(WriterOptions.WRITER_CLASS, "com.bytedance.bitsail.connector.legacy.streamingfile.sink" +
        ".FileSystemSinkFunctionDAGBuilder");
    setConnectorProps(jobConfiguration, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaDocker.getBootstrapServer());
    setConnectorProps(jobConfiguration, ConsumerConfig.GROUP_ID_CONFIG, "test_consumer");
    setConnectorProps(jobConfiguration, ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    setConnectorProps(jobConfiguration, "topic", topicName);
    setConnectorProps(jobConfiguration, "startup-mode", CONNECTOR_STARTUP_MODE_VALUE_EARLIEST);

    jobConfiguration.set(ReaderOptions.READER_METRIC_TAG_NAME, "kafka");
    jobConfiguration.set(BaseMessageQueueReaderOptions.ENABLE_COUNT_MODE, true);
    jobConfiguration.set(BaseMessageQueueReaderOptions.COUNT_MODE_RECORD_THRESHOLD, 2000L);

    jobConfiguration.set(FileSystemSinkOptions.HDFS_DUMP_TYPE, HDFS_DUMP_TYPE_JSON);
    jobConfiguration.set(FileSystemSinkOptions.HDFS_COMPRESSION_CODEC, "none");
    jobConfiguration.set(FileSystemSinkOptions.HDFS_REPLICATION, (short) 1);
    jobConfiguration.set(FileSystemCommonOptions.CONNECTOR_PATH, OUTPUT_DIR);
    jobConfiguration.set(FileSystemCommonOptions.DUMP_FORMAT_TYPE, "hdfs");
    jobConfiguration.set(FileSystemCommonOptions.PartitionOptions.PARTITION_INFOS,
        "[{\"name\":\"date\",\"type\":\"TIME\"},{\"name\":\"hour\",\"type\":\"TIME\"}]");
    jobConfiguration.set(FileSystemCommonOptions.ArchiveOptions.ENABLE_EVENT_TIME, true);
    jobConfiguration.set(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_FIELDS, "timestamp");
  }

  private void setConnectorProps(BitSailConfiguration jobConfiguration, String key, String value) {
    String path = BaseMessageQueueReaderOptions.CONNECTOR_PROPERTIES.key() + "." + CONNECTOR + "." + key;
    if (!jobConfiguration.fieldExists(path)) {
      jobConfiguration.set(path, value);
    }
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
  public void testHdfsFormatType() throws Exception {
    BitSailConfiguration hdfsConfiguration = BitSailConfiguration.newDefault();
    updateConfiguration(hdfsConfiguration);
    EmbeddedFlinkCluster.submitJob(hdfsConfiguration);
  }
}