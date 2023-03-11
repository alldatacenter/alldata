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

import com.netease.arctic.flink.read.source.log.LogSourceHelper;
import com.netease.arctic.flink.read.source.log.kafka.LogKafkaPartitionSplitReader;
import com.netease.arctic.flink.read.source.log.kafka.LogRecordKafkaWithRetractInfo;
import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.flink.util.kafka.KafkaConfigGenerate;
import com.netease.arctic.flink.util.kafka.KafkaContainerTest;
import com.netease.arctic.log.FormatVersion;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonDeserialization;
import com.netease.arctic.log.LogDataJsonSerialization;
import com.netease.arctic.utils.IdGenerator;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.splitreader.SplitsAddition;
import org.apache.flink.connector.base.source.reader.splitreader.SplitsChange;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.table.data.RowData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.netease.arctic.flink.shuffle.RowKindUtil.transformFromFlinkRowKind;
import static com.netease.arctic.flink.util.kafka.KafkaContainerTest.KAFKA_CONTAINER;
import static com.netease.arctic.flink.util.kafka.KafkaContainerTest.readRecordsBytes;
import static com.netease.arctic.flink.write.hidden.BaseLogTest.createLogDataDeserialization;
import static com.netease.arctic.flink.write.hidden.BaseLogTest.userSchema;
import static com.netease.arctic.flink.write.hidden.HiddenLogOperatorsTest.createRowData;
import static org.junit.Assert.assertEquals;

public class LogKafkaPartitionSplitReaderTest {

  private static final Logger LOG = LoggerFactory.getLogger(LogKafkaPartitionSplitReaderTest.class);

  public static final int TOPIC1_STOP_OFFSET = 16;
  public static final int TOPIC2_STOP_OFFSET = 21;
  public static final String TOPIC1 = "topic1";
  public static final String TOPIC2 = "topic2";
  private static Map<Integer, Map<String, KafkaPartitionSplit>> splitsByOwners;
  private static final byte[] JOB_ID = IdGenerator.generateUpstreamId();

  @BeforeClass
  public static void prepare() throws Exception {
    KAFKA_CONTAINER.start();

    Map<TopicPartition, Long> earliestOffsets = new HashMap<>();
    earliestOffsets.put(new TopicPartition(TOPIC1, 0), 0L);
    earliestOffsets.put(new TopicPartition(TOPIC2, 0), 5L);
    splitsByOwners = getSplitsByOwners(earliestOffsets);
  }

  @AfterClass
  public static void shutdown() throws Exception {
    KAFKA_CONTAINER.close();
  }

  @Before
  public void initData() throws Exception {
    // |0 1 2 3 4 5 6 7 8 9 Flip 10 11 12 13 14| 15 16 17 18 19
    write(TOPIC1, 0);
    // 0 0 0 0 0 |5 6 7 8 9 10 11 12 13 14 Flip 15 16 17 18 19| 20 21 22 23 24
    write(TOPIC2, 5);
  }

  @Test
  public void testHandleSplitChangesAndFetch() throws IOException {
    LogKafkaPartitionSplitReader reader = createReader(new Properties());
    assignSplitsAndFetchUntilFinish(reader, 0, 20);
    assignSplitsAndFetchUntilFinish(reader, 1, 20);
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
  private void write(String topic, int offset) throws Exception {
    KafkaProducer producer = KafkaContainerTest.getProducer();
    LogDataJsonSerialization<RowData> serialization =
        new LogDataJsonSerialization<>(userSchema, LogRecordV1.fieldGetterFactory);
    for (int j = 0; j < offset; j++) {
      producer.send(createLogData(topic, 0, 1, false, serialization));
    }

    int i = offset;
    // 0-4 + offset success
    for (; i < offset + 5; i++) {
      producer.send(createLogData(topic, i, 1, false, serialization));
    }

    // 5-9 + offset fail
    for (; i < offset + 10; i++) {
      producer.send(createLogData(topic, i, 2, false, serialization));
    }

    producer.send(createLogData(topic, i, 1, true, serialization));

    // 10-14 + offset success
    for (; i < offset + 15; i++) {
      producer.send(createLogData(topic, i, 2, false, serialization));
    }

    for (; i < offset + 20; i++) {
      producer.send(createLogData(topic, i, 3, false, serialization));
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

  private void assignSplitsAndFetchUntilFinish(
      LogKafkaPartitionSplitReader reader, int readerId, int expectedRecordCount) throws IOException {
    Map<String, KafkaPartitionSplit> splits =
        assignSplits(reader, splitsByOwners.get(readerId));

    Map<String, Integer> numConsumedRecords = new HashMap<>();
    Set<String> finishedSplits = new HashSet<>();
    int flipCount = 0;
    while (finishedSplits.size() < splits.size()) {
      RecordsWithSplitIds<ConsumerRecord<byte[], byte[]>> recordsBySplitIds = reader.fetch();
      String splitId = recordsBySplitIds.nextSplit();
      while (splitId != null) {
        // Collect the records in this split.
        List<LogRecordKafkaWithRetractInfo<RowData>> splitFetch = new ArrayList<>();
        ConsumerRecord<byte[], byte[]> record;
        boolean hasFlip = false;
        while ((record = recordsBySplitIds.nextRecordFromSplit()) != null) {
          LOG.info("read: {}, offset: {}", ((LogRecordKafkaWithRetractInfo) record).getLogData().getActualValue(),
              record.offset());
          if (((LogRecordKafkaWithRetractInfo<?>) record).isRetracting()) {
            hasFlip = true;
          }
          splitFetch.add((LogRecordKafkaWithRetractInfo<RowData>) record);
        }
        if (hasFlip) {
          flipCount++;
        }
        // verify the consumed records.
        if (verifyConsumed(splits.get(splitId), splitFetch, flipCount)) {
          finishedSplits.add(splitId);
        }
        numConsumedRecords.compute(
            splitId,
            (ignored, recordCount) ->
                recordCount == null
                    ? splitFetch.size()
                    : recordCount + splitFetch.size());
        splitId = recordsBySplitIds.nextSplit();
      }
    }

    // Verify the number of records consumed from each split.
    numConsumedRecords.forEach(
        (splitId, recordCount) -> {
          assertEquals(
              String.format(
                  "%s should have %d records.",
                  splits.get(splitId), expectedRecordCount),
              expectedRecordCount,
              (long) recordCount);
        });
  }

  public static Map<Integer, Map<String, KafkaPartitionSplit>> getSplitsByOwners(
      Map<TopicPartition, Long> earliestOffsets) {
    final Map<Integer, Map<String, KafkaPartitionSplit>> splitsByOwners = new HashMap<>();
    splitsByOwners.put(0, new HashMap<String, KafkaPartitionSplit>() {{
      TopicPartition tp = new TopicPartition(TOPIC1, 0);
      put(KafkaPartitionSplit.toSplitId(tp), new KafkaPartitionSplit(tp, earliestOffsets.get(tp), TOPIC1_STOP_OFFSET));
    }});
    splitsByOwners.put(1, new HashMap<String, KafkaPartitionSplit>() {{
      TopicPartition tp = new TopicPartition(TOPIC2, 0);
      put(KafkaPartitionSplit.toSplitId(tp), new KafkaPartitionSplit(tp, earliestOffsets.get(tp), TOPIC2_STOP_OFFSET));
    }});
    return splitsByOwners;
  }

  private Map<String, KafkaPartitionSplit> assignSplits(
      LogKafkaPartitionSplitReader reader, Map<String, KafkaPartitionSplit> splits) {
    SplitsChange<KafkaPartitionSplit> splitsChange =
        new SplitsAddition<>(new ArrayList<>(splits.values()));
    reader.handleSplitsChanges(splitsChange);
    return splits;
  }

  private LogKafkaPartitionSplitReader createReader(
      Properties additionalProperties) {
    Properties props = KafkaConfigGenerate.getPropertiesWithByteArray();
    props.put("group.id", "test");
    props.put("auto.offset.reset", "earliest");
    if (!additionalProperties.isEmpty()) {
      props.putAll(additionalProperties);
    }
    return new LogKafkaPartitionSplitReader(
        props,
        null,
        0,
        userSchema,
        true,
        new LogSourceHelper(),
        "all-kinds"
    );
  }

  private boolean verifyConsumed(
      final KafkaPartitionSplit split,
      final Collection<LogRecordKafkaWithRetractInfo<RowData>> consumed,
      final int valueOffsetDiffInOrderedRead) {
    long currentOffset = -1;

    for (LogRecordKafkaWithRetractInfo<RowData> record : consumed) {
      if (record.isRetracting()) {
        assertEquals(record.offset(), record.getActualValue().getInt(1));
      } else {
        assertEquals(record.offset(),
            record.getActualValue().getInt(1) + valueOffsetDiffInOrderedRead);
      }

      currentOffset = Math.max(currentOffset, record.offset());
    }
    if (split.getStoppingOffset().isPresent()) {
      return currentOffset == split.getStoppingOffset().get() - 1;
    } else {
      return false;
    }
  }

}
