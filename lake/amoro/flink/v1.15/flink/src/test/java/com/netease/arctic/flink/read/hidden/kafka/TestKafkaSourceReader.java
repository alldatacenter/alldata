/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.read.hidden.kafka;

import com.netease.arctic.flink.kafka.testutils.KafkaContainerTest;
import com.netease.arctic.flink.read.source.log.kafka.LogKafkaPartitionSplit;
import com.netease.arctic.flink.read.source.log.kafka.LogKafkaPartitionSplitState;
import com.netease.arctic.flink.read.source.log.kafka.LogKafkaSource;
import com.netease.arctic.flink.read.source.log.kafka.LogKafkaSourceReader;
import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.flink.util.TestUtil;
import com.netease.arctic.log.FormatVersion;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonDeserialization;
import com.netease.arctic.log.LogDataJsonSerialization;
import com.netease.arctic.utils.IdGenerator;
import org.apache.flink.api.common.eventtime.Watermark;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.connector.testutils.source.reader.TestingReaderContext;
import org.apache.flink.table.data.RowData;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.netease.arctic.flink.kafka.testutils.KafkaContainerTest.KAFKA_CONTAINER;
import static com.netease.arctic.flink.kafka.testutils.KafkaContainerTest.getPropertiesByTopic;
import static com.netease.arctic.flink.kafka.testutils.KafkaContainerTest.readRecordsBytes;
import static com.netease.arctic.flink.shuffle.RowKindUtil.transformFromFlinkRowKind;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE;
import static com.netease.arctic.flink.write.hidden.kafka.TestBaseLog.createLogDataDeserialization;
import static com.netease.arctic.flink.write.hidden.kafka.TestBaseLog.userSchema;
import static com.netease.arctic.flink.write.hidden.kafka.TestHiddenLogOperators.createRowData;
import static org.junit.Assert.assertEquals;

public class TestKafkaSourceReader {
  private static final Logger LOG = LoggerFactory.getLogger(TestKafkaSourceReader.class);
  private static String topic;
  private static final int KAFKA_PARTITION_NUMS = 1;
  private static final int NUM_SPLITS = 1;
  private static final int NUM_RECORDS_PER_SPLIT = 10;
  private static final int TOTAL_NUM_RECORDS = NUM_RECORDS_PER_SPLIT * NUM_SPLITS;

  @Rule
  public TestName testName = new TestName();

  private static final byte[] JOB_ID = IdGenerator.generateUpstreamId();

  @BeforeClass
  public static void prepare() throws Exception {
    KAFKA_CONTAINER.start();
  }

  @AfterClass
  public static void shutdown() throws Exception {
    KAFKA_CONTAINER.close();
  }

  @Before
  public void initData() throws Exception {
    topic = TestUtil.getUtMethodName(testName);
    KafkaContainerTest.createTopics(KAFKA_PARTITION_NUMS, topic);
    write(topic, TOTAL_NUM_RECORDS);
  }

  @Test
  public void testSourceReaderFailover() throws Exception {
    final String groupId = "testSourceReaderFailover";
    LogKafkaSourceReader reader = (LogKafkaSourceReader) createReader(groupId);
    reader.addSplits(getSplits(NUM_SPLITS));
    ValidatingSourceOutput output = new ValidatingSourceOutput();
    List<KafkaPartitionSplit> splitList;
    long checkpointId = 0;
    do {
      checkpointId++;
      reader.pollNext(output);
      // Create a checkpoint for each message consumption, but not complete them.
      splitList = reader.snapshotState(checkpointId);
    } while (output.count() < TOTAL_NUM_RECORDS);

    // The completion of the last checkpoint should subsume all the previous checkpoints.
    assertEquals(checkpointId, reader.getOffsetsToCommit().size());
    reader.notifyCheckpointComplete(checkpointId);

    // re-create and restore
    reader = (LogKafkaSourceReader) createReader(groupId);
    reader.addSplits(splitList);
    List<KafkaPartitionSplit> currentSplitList = reader.snapshotState(checkpointId);
    currentSplitList.forEach(s -> assertEquals(TOTAL_NUM_RECORDS, s.getStartingOffset()));
  }

  private ProducerRecord<byte[], byte[]> createLogData(String topic, int i, int epicNo, boolean flip,
                                                       LogDataJsonSerialization<RowData> serialization) {
    RowData rowData = createRowData(i);
    LogData<RowData> logData = new LogRecordV1(
      FormatVersion.FORMAT_VERSION_V1,
      JOB_ID,
      epicNo,
      flip,
      transformFromFlinkRowKind(rowData.getRowKind()),
      rowData
    );
    byte[] message = serialization.serialize(logData);
    int partition = 0;
    ProducerRecord<byte[], byte[]> producerRecord =
      new ProducerRecord<>(topic, partition, null, null, message);
    return producerRecord;
  }

  private void write(String topic, int numRecords) throws Exception {
    KafkaProducer producer = KafkaContainerTest.getProducer();
    LogDataJsonSerialization<RowData> serialization =
      new LogDataJsonSerialization<>(userSchema, LogRecordV1.fieldGetterFactory);
    for (int i = 0; i < numRecords; i++) {
      producer.send(createLogData(topic, 0, 1, false, serialization));
    }
    printDataInTopic(topic);
  }

  public static void printDataInTopic(String topic) {
    ConsumerRecords<byte[], byte[]> consumerRecords = readRecordsBytes(topic);
    LogDataJsonDeserialization<RowData> deserialization = createLogDataDeserialization();
    consumerRecords.forEach(consumerRecord -> {
      try {
        LOG.info("data in kafka: {}", deserialization.deserialize(consumerRecord.value()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private SourceReader<RowData, KafkaPartitionSplit> createReader(String groupId) {
    List<String> topics = new ArrayList<>();
    topics.add(topic);
    LogKafkaSource kafkaSource = createKafkaSource(groupId, false, topics);
    return kafkaSource.createReader(new TestingReaderContext());
  }

  private LogKafkaSource createKafkaSource(String groupId, boolean retract, List<String> topics) {
    Properties properties = getPropertiesByTopic(topic);
    properties.put("group.id", groupId);
    properties.put("auto.offset.reset", "earliest");

    Map<String, String> configuration = new HashMap<>();
    configuration.put(ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.key(), String.valueOf(retract));

    return LogKafkaSource.builder(userSchema, configuration)
      .setTopics(topics)
      .setStartingOffsets(OffsetsInitializer.earliest())
      .setProperties(properties)
      .build();
  }

  protected List<LogKafkaPartitionSplit> getSplits(int numRecordsPerSplit) {
    List<LogKafkaPartitionSplit> splits = new ArrayList<>();
    for (int i = 0; i < numRecordsPerSplit; i++) {
      splits.add(getSplit(i, numRecordsPerSplit));
    }
    return splits;
  }

  protected LogKafkaPartitionSplit getSplit(int splitId, int numRecords) {
    long stoppingOffset = KafkaPartitionSplit.NO_STOPPING_OFFSET;
    KafkaPartitionSplit kafkaPartitionSplit = new KafkaPartitionSplit(new TopicPartition(topic, splitId), 0L, stoppingOffset);
    return new LogKafkaPartitionSplit(new LogKafkaPartitionSplitState(kafkaPartitionSplit));
  }

  // ---------------- helper classes -----------------
  /** A source output that validates the output. */
  public static class ValidatingSourceOutput implements ReaderOutput<RowData> {
    private final Set<RowData> consumedValues = new HashSet<>();
    private int max = Integer.MIN_VALUE;
    private int min = Integer.MAX_VALUE;

    private int count = 0;

    @Override
    public void collect(RowData rowData) {
      count++;
      consumedValues.add(rowData);
    }

    @Override
    public void collect(RowData rowData, long timestamp) {
      collect(rowData);
    }

    @Override
    public void emitWatermark(Watermark watermark) {
    }

    public void validate() {
      assertEquals(
        String.format("Should be %d distinct elements in total", TOTAL_NUM_RECORDS),
        TOTAL_NUM_RECORDS,
        consumedValues.size());
      assertEquals(
        String.format("Should be %d elements in total", TOTAL_NUM_RECORDS),
        TOTAL_NUM_RECORDS,
        count);
      assertEquals("The min value should be 0", 0, min);
      assertEquals(
        "The max value should be " + (TOTAL_NUM_RECORDS - 1),
        TOTAL_NUM_RECORDS - 1,
        max);
    }

    public int count() {
      return count;
    }

    @Override
    public void markIdle() {}

    @Override
    public void markActive() {
    }

    @Override
    public SourceOutput<RowData> createOutputForSplit(String splitId) {
      return this;
    }

    @Override
    public void releaseOutputForSplit(String splitId) {}
  }
}
