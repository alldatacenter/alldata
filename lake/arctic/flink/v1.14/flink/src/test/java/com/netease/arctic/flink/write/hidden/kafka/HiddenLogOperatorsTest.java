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

package com.netease.arctic.flink.write.hidden.kafka;

import com.netease.arctic.flink.read.source.log.kafka.LogKafkaSource;
import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.flink.shuffle.ShuffleHelper;
import com.netease.arctic.flink.util.OneInputStreamOperatorInternTest;
import com.netease.arctic.flink.util.TestGlobalAggregateManager;
import com.netease.arctic.flink.write.hidden.HiddenLogWriter;
import com.netease.arctic.log.LogDataJsonDeserialization;
import com.netease.arctic.utils.IdGenerator;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.GenericArrayData;
import org.apache.flink.table.data.GenericMapData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.util.CloseableIterator;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.netease.arctic.flink.kafka.testutils.KafkaContainerTest.KAFKA_CONTAINER;
import static com.netease.arctic.flink.kafka.testutils.KafkaContainerTest.getPropertiesByTopic;
import static com.netease.arctic.flink.kafka.testutils.KafkaContainerTest.readRecordsBytes;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE;
import static com.netease.arctic.flink.write.hidden.kafka.BaseLogTest.createLogDataDeserialization;
import static com.netease.arctic.flink.write.hidden.kafka.BaseLogTest.userSchema;

/**
 * Hidden log operator tests.
 */
public class HiddenLogOperatorsTest {
  private static final Logger LOG = LoggerFactory.getLogger(HiddenLogOperatorsTest.class);
  public static final String topic = "produce-consume-topic";
  public static final TestGlobalAggregateManager globalAggregateManger = new TestGlobalAggregateManager();

  @BeforeClass
  public static void prepare() throws Exception {
    KAFKA_CONTAINER.start();
  }

  @AfterClass
  public static void shutdown() throws Exception {
    KAFKA_CONTAINER.close();
  }

  @Test
  public void testProduceAndConsume() throws Exception {
    String topic = "testProduceAndConsume";
    final int count = 20;

    String[] expect = new String[count];
    try (OneInputStreamOperatorTestHarness<RowData, RowData> harness = createProducer(null, topic)) {
      harness.setup();
      harness.initializeEmptyState();
      harness.open();
      for (int i = 0; i < count; i++) {
        RowData row = createRowData(i);
        expect[i] = row.toString();
        harness.processElement(row, 0);
      }
      harness.snapshot(1, 1);
      harness.notifyOfCompletedCheckpoint(1);
      List<String> output = collect(harness);
      Assertions.assertEquals(count, output.size());
      Assertions.assertArrayEquals(expect, output.toArray(new String[0]));

      createConsumerWithoutRetract(true, count, "test-gid", topic);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  @Test
  public void testProducerFailoverWithoutRetract() throws Exception {
    String topic = "testProducerFailoverWithoutRetract";
    OperatorSubtaskState state;
    try {
      OneInputStreamOperatorTestHarness<RowData, RowData> harness = createProducer(null, topic);
      harness.setup();
      harness.initializeEmptyState();
      harness.open();
      harness.processElement(createRowData(1), 0);
      harness.processElement(createRowData(2), 0);
      harness.processElement(createRowData(3), 0);
      state = harness.snapshot(1, 1);
      harness.processElement(createRowData(4), 0);
      harness.processElement(createRowData(5), 0);
      harness.notifyOfCompletedCheckpoint(1);
      List<String> output = collect(harness);
      Assertions.assertEquals(5, output.size());
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

    // failover happen 1 time
    try {
      OneInputStreamOperatorTestHarness<RowData, RowData> harness = createProducer(1L, topic);
      harness.setup();
      harness.initializeState(state);
      harness.open();
      harness.processElement(createRowData(4), 0);
      harness.processElement(createRowData(5), 0);
      harness.processElement(createRowData(6), 0);
      harness.snapshot(2, 1);
      harness.processElement(createRowData(7), 0);
      harness.processElement(createRowData(8), 0);
      harness.notifyOfCompletedCheckpoint(2);
      List<String> output = collect(harness);
      Assertions.assertEquals(5, output.size());
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

    createConsumerWithoutRetract(true, 10, "test-gid", topic);
  }

  @Test
  public void testMultiParallelismFailoverConsistencyRead() throws Exception {
    String topic = "testMultiParallelismFailoverConsistencyRead";
    OperatorSubtaskState state0;
    OperatorSubtaskState state1;
    OperatorSubtaskState state2;
    byte[] jobId = IdGenerator.generateUpstreamId();
    try (OneInputStreamOperatorInternTest<RowData, RowData> harness0 =
             createProducer(3, 0, jobId, topic);
         OneInputStreamOperatorInternTest<RowData, RowData> harness1 =
             createProducer(3, 1, jobId, topic);
         OneInputStreamOperatorInternTest<RowData, RowData> harness2 =
             createProducer(3, 2, jobId, topic)
    ) {
      harness0.setup();
      harness0.initializeEmptyState();
      harness0.open();
      harness1.setup();
      harness1.initializeEmptyState();
      harness1.open();
      harness2.setup();
      harness2.initializeEmptyState();
      harness2.open();

      harness0.processElement(createRowData(1), 0);

      state0 = harness0.snapshot(1, 1);

      harness1.processElement(createRowData(11), 0);
      harness2.processElement(createRowData(21), 0);

      // chp-1 success.
      state1 = harness1.snapshot(1, 1);
      state2 = harness2.snapshot(1, 1);

      harness0.processElement(createRowData(2), 0);
      harness1.processElement(createRowData(12), 0);
      harness2.processElement(createRowData(22), 0);
      harness0.notifyOfCompletedCheckpoint(1);
      harness1.notifyOfCompletedCheckpoint(1);
      harness2.notifyOfCompletedCheckpoint(1);
      harness0.processElement(createRowData(3), 0);
      // after 3, harness0 happen timeout
      harness1.processElement(createRowData(13), 0);
      harness2.processElement(createRowData(23), 0);

      // harness0 snapshot chp-2 failed.
      harness1.snapshot(2, 1);
      harness2.snapshot(2, 1);

      harness1.processElement(createRowData(14), 0);
      harness2.processElement(createRowData(24), 0);
      // notify chp-2 aborted
      harness1.notifyOfAbortedCheckpoint(2);
      harness2.notifyOfAbortedCheckpoint(2);

      List<String> output = collect(harness0);
      output.addAll(collect(harness1));
      output.addAll(collect(harness2));
      Assertions.assertEquals(11, output.size());
      ConsumerRecords<byte[], byte[]> consumerRecords = readRecordsBytes(topic);
      Assertions.assertEquals(11, consumerRecords.count());
      LogDataJsonDeserialization<RowData> deserialization = createLogDataDeserialization();
      consumerRecords.forEach(consumerRecord -> {
        try {
          System.out.println(deserialization.deserialize(consumerRecord.value()));
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

    // failover restore from chp-1
    try (OneInputStreamOperatorInternTest<RowData, RowData> harness0 =
             createProducer(3, 0, jobId, 1L, topic);
         OneInputStreamOperatorInternTest<RowData, RowData> harness1 =
             createProducer(3, 1, jobId, 1L, topic);
         OneInputStreamOperatorInternTest<RowData, RowData> harness2 =
             createProducer(3, 2, jobId, 1L, topic)
    ) {
      harness0.setup();
      harness0.initializeState(state0);
      harness0.open();
      harness1.setup();
      harness1.initializeState(state1);
      harness1.open();
      harness2.setup();
      harness2.initializeState(state2);
      harness2.open();

      harness0.processElement(createRowData(2), 0);
      harness1.processElement(createRowData(12), 0);
      harness2.processElement(createRowData(22), 0);
      // chp-2
      state1 = harness1.snapshot(3, 1);
      state2 = harness2.snapshot(3, 1);

      harness0.processElement(createRowData(3), 0);
      // after 3, harness0 happen timeout
      harness1.processElement(createRowData(13), 0);
      harness2.processElement(createRowData(23), 0);

      harness1.processElement(createRowData(14), 0);
      harness2.processElement(createRowData(24), 0);

      harness1.notifyOfAbortedCheckpoint(2);
      harness2.notifyOfAbortedCheckpoint(2);

      List<String> output = collect(harness0);
      output.addAll(collect(harness1));
      output.addAll(collect(harness2));
      Assertions.assertEquals(8, output.size());
      ConsumerRecords<byte[], byte[]> consumerRecords = readRecordsBytes(topic);
      LogDataJsonDeserialization<RowData> deserialization = createLogDataDeserialization();
      consumerRecords.forEach(consumerRecord -> {
        try {
          System.out.println(deserialization.deserialize(consumerRecord.value()));
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      Assertions.assertEquals(20, consumerRecords.count());
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
    createConsumerWithoutRetract(true, 19, "test-gid", topic);
    createConsumerWithRetract(true, 27, "test-gid-2", topic);
  }

  public static RowData createRowData(int i) {
    GenericRowData rowData = new GenericRowData(userSchema.columns().size());
    rowData.setField(0, true);
    rowData.setField(1, i);
    rowData.setField(2, 1L);
    GenericRowData sub = new GenericRowData(18);
    sub.setField(0, true);
    sub.setField(1, 1);
    sub.setField(2, 1L);
    sub.setField(3, StringData.fromString("sssss"));
    sub.setField(4, LocalTime.of(13, 23, 23, 98766545).toNanoOfDay());
    sub.setField(5, DecimalData.fromBigDecimal(new BigDecimal("123456789.123456789123456789"), 30, 18));
    sub.setField(6, 123.12345f);
    sub.setField(7, 123.12345d);
    sub.setField(8, (int) LocalDate.of(2022, 5, 5).toEpochDay());
    sub.setField(9, TimestampData.fromLocalDateTime(LocalDateTime.of(2022, 12, 12, 13, 14, 14, 987654234)));
    sub.setField(10, TimestampData.fromInstant(Instant.parse("2022-12-13T13:33:44.98765432Z")));
    sub.setField(11, new byte[]{1});
    sub.setField(12, new byte[]{'1'});
    sub.setField(13, new byte[]{2});

    GenericArrayData fSubList = new GenericArrayData(new long[]{112L, 123L});
    sub.setField(14, fSubList);

    GenericArrayData fSubList2 = new GenericArrayData(new int[]{112, 123});
    sub.setField(15, fSubList2);

    GenericRowData subStruct = new GenericRowData(3);
    subStruct.setField(0, false);
    subStruct.setField(1, 112);
    subStruct.setField(2, 123L);
    GenericArrayData structList = new GenericArrayData(new GenericRowData[]{subStruct});
    sub.setField(16, structList);

    GenericMapData map = new GenericMapData(new HashMap<StringData, StringData>() {{
      put(StringData.fromString("Key_123"), StringData.fromString("Str_123"));
      put(StringData.fromString("Key_124"), StringData.fromString("Str_123"));
      put(StringData.fromString("Key_125"), StringData.fromString("Str_123"));
    }});
    sub.setField(17, map);

    rowData.setField(3, sub);
    return rowData;
  }

  private static List<String> collect(
      OneInputStreamOperatorTestHarness<RowData, RowData> harness) {
    List<String> parts = new ArrayList<>();
    harness.extractOutputValues().forEach(m -> parts.add(m.toString()));
    return parts;
  }

  private void createConsumerWithRetract(
      boolean print, int count, final String groupId, String topic) throws Exception {
    createConsumer(print, count, groupId, true, topic);
  }

  private void createConsumerWithoutRetract(
      boolean print, int count, final String groupId, String topic) throws Exception {
    createConsumer(print, count, groupId, false, topic);
  }

  private void createConsumer(
      boolean print, int count, final String groupId, boolean retract, String topic) throws Exception {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    env.setParallelism(1);
    env.enableCheckpointing(10000);
    env.getConfig().setRestartStrategy(RestartStrategies.noRestart());
    List<String> topics = new ArrayList<>();
    topics.add(topic);
    Properties properties = getPropertiesByTopic(topic);
    properties.put("group.id", groupId);
    properties.put("auto.offset.reset", "earliest");

    Map<String, String> configuration = new HashMap<>();
    configuration.put(ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.key(), String.valueOf(retract));

    DataStream<RowData> streamWithTimestamps =
        env.fromSource(
            LogKafkaSource.builder(userSchema, configuration)
                .setTopics(topics)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setProperties(properties)
                .build(),
            WatermarkStrategy.noWatermarks(),
            "Log Source"
        );
    if (print) {
      streamWithTimestamps.print("log-hidden");
    }

    CloseableIterator<RowData> iterator = streamWithTimestamps.executeAndCollect("testLog");

    List<RowData> actualResult = new ArrayList<>();

    while (iterator.hasNext()) {
      RowData row = iterator.next();
      actualResult.add(row);
      LOG.info("size {}, {}, {}.", actualResult.size(), row.getRowKind(), row.getInt(1));
      if (actualResult.size() == count) {
        break;
      }
    }
  }

  public static OneInputStreamOperatorTestHarness<RowData, RowData> createProducer(
      Long restoredCheckpoint, String topic) throws Exception {
    return createProducer(
        1,
        1,
        0,
        restoredCheckpoint,
        IdGenerator.generateUpstreamId(),
        new TestGlobalAggregateManager(),
        topic);
  }

  public static OneInputStreamOperatorInternTest<RowData, RowData> createProducer(
      int maxParallelism,
      int subTaskId,
      byte[] jobId,
      Long restoredCheckpointId,
      String topic) throws Exception {
    return createProducer(
        maxParallelism,
        maxParallelism,
        subTaskId,
        restoredCheckpointId,
        jobId,
        globalAggregateManger,
        topic);
  }

  public static OneInputStreamOperatorInternTest<RowData, RowData> createProducer(
      int maxParallelism,
      int subTaskId,
      byte[] jobId,
      String topic) throws Exception {
    return createProducer(
        maxParallelism,
        maxParallelism,
        subTaskId,
        null,
        jobId,
        globalAggregateManger,
        topic);
  }

  private static OneInputStreamOperatorInternTest<RowData, RowData> createProducer(
      int maxParallelism,
      int parallelism,
      int subTaskId,
      Long restoredCheckpointId,
      byte[] jobId,
      TestGlobalAggregateManager testGlobalAggregateManager,
      String topic) throws Exception {
    HiddenLogWriter writer =
        new HiddenLogWriter(
            userSchema,
            getPropertiesByTopic(topic),
            topic,
            new HiddenKafkaFactory<>(),
            LogRecordV1.fieldGetterFactory,
            jobId,
            ShuffleHelper.EMPTY
        );

    OneInputStreamOperatorInternTest<RowData, RowData> harness =
        new OneInputStreamOperatorInternTest<>(
            writer,
            maxParallelism,
            parallelism,
            subTaskId,
            restoredCheckpointId,
            testGlobalAggregateManager);
    harness.getStreamConfig().setTimeCharacteristic(TimeCharacteristic.ProcessingTime);
    return harness;
  }
}
