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

package com.netease.arctic.flink.write;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.flink.kafka.testutils.KafkaTestBase;
import com.netease.arctic.flink.metric.MetricsGenerator;
import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.flink.shuffle.ShuffleHelper;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.util.ArcticUtils;
import com.netease.arctic.flink.util.DataUtil;
import com.netease.arctic.flink.util.TestGlobalAggregateManager;
import com.netease.arctic.flink.util.TestOneInputStreamOperatorIntern;
import com.netease.arctic.flink.write.hidden.kafka.HiddenKafkaFactory;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.log.LogDataJsonDeserialization;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.utils.IdGenerator;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.iceberg.Schema;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.types.TypeUtil;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static com.netease.arctic.flink.kafka.testutils.KafkaConfigGenerate.getPropertiesWithByteArray;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.LOG_STORE_CATCH_UP;
import static com.netease.arctic.table.TableProperties.ENABLE_LOG_STORE;
import static com.netease.arctic.table.TableProperties.LOG_STORE_ADDRESS;
import static com.netease.arctic.table.TableProperties.LOG_STORE_MESSAGE_TOPIC;

@RunWith(Parameterized.class)
public class TestAutomaticLogWriter extends FlinkTestBase {
  private static final Logger LOG = LoggerFactory.getLogger(TestAutomaticLogWriter.class);
  public ArcticTableLoader tableLoader;
  public static final TestGlobalAggregateManager globalAggregateManger = new TestGlobalAggregateManager();
  private static final KafkaTestBase kafkaTestBase = new KafkaTestBase();

  private final boolean isGapNone;
  private final boolean logstoreEnabled;

  public TestAutomaticLogWriter(boolean isGapNone, boolean logstoreEnabled) {
    super(new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
      new BasicTableTestHelper(true, true));
    this.isGapNone = isGapNone;
    this.logstoreEnabled = logstoreEnabled;
  }

  @Parameterized.Parameters(name = "isGapNone={0}, logstoreEnabled={1}")
  public static Object[][] parameters() {
    return new Object[][]{
        {true, true},
        {false, false},
        {false, true},
        {true, false}
    };
  }

  @BeforeClass
  public static void prepare() throws Exception {
    kafkaTestBase.prepare();
  }

  @AfterClass
  public static void shutdown() throws Exception {
    kafkaTestBase.shutDownServices();
  }

  @Before
  public void init() {
    tableLoader = ArcticTableLoader.of(TableTestHelper.TEST_TABLE_ID, catalogBuilder);
    tableLoader.open();
  }

  @Test
  public void testHasCaughtUp() throws Exception {
    String topic = Thread.currentThread().getStackTrace()[1].getMethodName() + isGapNone + logstoreEnabled;

    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
    env.getCheckpointConfig()
        .enableExternalizedCheckpoints(
            CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
    env.getConfig().setAutoWatermarkInterval(10);

    List<Object[]> expects = new LinkedList<>();
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    expects.add(new Object[]{1000004, "a", LocalDateTime.parse("2022-06-17 10:10:11", dtf).toEpochSecond(ZoneOffset.UTC), LocalDateTime.parse("2022-06-17 10:10:11", dtf)});
    expects.add(new Object[]{1000015, "b", LocalDateTime.parse("2022-06-17 10:08:11", dtf).toEpochSecond(ZoneOffset.UTC), LocalDateTime.parse("2022-06-17 10:08:11", dtf)});
    expects.add(new Object[]{1000011, "c", LocalDateTime.parse("2022-06-18 10:10:11", dtf).toEpochSecond(ZoneOffset.UTC), LocalDateTime.parse("2022-06-18 10:10:11", dtf)});
    List<Object[]> catchUpExpects = new LinkedList<>();
    catchUpExpects.add(new Object[]{1000014, "d", LocalDateTime.now().minusSeconds(3).toEpochSecond(ZoneOffset.UTC), LocalDateTime.now().minusSeconds(3)});
    catchUpExpects.add(new Object[]{1000021, "d", LocalDateTime.now().minusSeconds(2).toEpochSecond(ZoneOffset.UTC), LocalDateTime.now().minusSeconds(2)});
    catchUpExpects.add(new Object[]{1000015, "e", LocalDateTime.now().minusSeconds(1).toEpochSecond(ZoneOffset.UTC), LocalDateTime.now().minusSeconds(1)});
    expects.addAll(catchUpExpects);

    DataStream<RowData> input = env.fromElements(expects.stream().map(DataUtil::toRowData).toArray(RowData[]::new));

    KeyedTable testKeyedTable = getArcticTable().asKeyedTable();
    UpdateProperties up = testKeyedTable.updateProperties();
    up.set(LOG_STORE_ADDRESS, kafkaTestBase.brokerConnectionStrings);
    up.set(LOG_STORE_MESSAGE_TOPIC, topic);
    if (logstoreEnabled) {
      up.set(ENABLE_LOG_STORE, "true");
    } else {
      up.set(ENABLE_LOG_STORE, "false");
    }
    up.set(LOG_STORE_CATCH_UP.key(), "true");
    up.commit();

    FlinkSink
        .forRowData(input)
        .context(Optional::of)
        .table(testKeyedTable)
        .tableLoader(ArcticTableLoader.of(TableTestHelper.TEST_TABLE_ID, catalogBuilder))
        .flinkSchema(FLINK_SCHEMA)
        .producerConfig(getPropertiesByTopic(topic))
        .topic(topic)
        .build();

    env.execute();

    testKeyedTable.changeTable().refresh();
    List<Record> actual = DataTestHelpers.readKeyedTable(testKeyedTable, null);

    Set<Record> expected = toRecords(DataUtil.toRowSet(expects));
    Assert.assertEquals(expected, new HashSet<>(actual));
    if (logstoreEnabled) {
      checkLogstoreDataAccuracy(topic, expects);
    } else {
      checkLogstoreDataAccuracy(topic, new ArrayList<>());
    }
  }

  @Test
  public void testHasNotCaughtUp() throws Exception {
    String topic = Thread.currentThread().getStackTrace()[1].getMethodName() + isGapNone + logstoreEnabled;
    byte[] jobId = IdGenerator.generateUpstreamId();
    Duration gap;
    KeyedTable testKeyedTable = getArcticTable().asKeyedTable();
    UpdateProperties up = testKeyedTable.updateProperties();
    up.set(LOG_STORE_ADDRESS, kafkaTestBase.brokerConnectionStrings);
    up.set(LOG_STORE_MESSAGE_TOPIC, topic);
    up.set(ENABLE_LOG_STORE, "true");
    up.commit();

    if (isGapNone) {
      gap = null;
    } else {
      gap = Duration.ofSeconds(20);
    }

    List<Object[]> expects = new LinkedList<>();
    List<WriteResult> results;
    testKeyedTable.refresh();
    Assert.assertFalse(Boolean.parseBoolean(
        testKeyedTable.properties().getOrDefault(LOG_STORE_CATCH_UP.key(), "false")));
    try (TestOneInputStreamOperatorIntern<RowData, WriteResult> harness =
             createSingleProducer(1, jobId, topic, gap)) {
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      expects.add(new Object[]{1000004, "a", LocalDateTime.parse("2022-06-17 10:10:11", dtf).toEpochSecond(ZoneOffset.UTC), LocalDateTime.parse("2022-06-17 10:10:11", dtf)});
      expects.add(new Object[]{1000015, "b", LocalDateTime.parse("2022-06-17 10:18:11", dtf).toEpochSecond(ZoneOffset.UTC), LocalDateTime.parse("2022-06-17 10:18:11", dtf)});
      expects.add(new Object[]{1000011, "c", LocalDateTime.parse("2022-06-18 10:10:11", dtf).toEpochSecond(ZoneOffset.UTC), LocalDateTime.parse("2022-06-18 10:10:11", dtf)});
      long checkpoint = 0;

      harness.setup();
      harness.initializeEmptyState();
      harness.open();
      harness.processElement(new StreamRecord<>(createRowData(RowKind.INSERT, expects.get(0))));
      harness.processWatermark(1);
      harness.prepareSnapshotPreBarrier(++checkpoint);
      harness.snapshot(1, 1);
      harness.notifyOfCompletedCheckpoint(checkpoint);
      harness.processElement(new StreamRecord<>(createRowData(RowKind.INSERT, expects.get(1))));
      harness.processWatermark(System.currentTimeMillis() - 1000);
      harness.prepareSnapshotPreBarrier(++checkpoint);
      harness.snapshot(2, 1);
      harness.notifyOfCompletedCheckpoint(checkpoint);
      harness.processElement(new StreamRecord<>(createRowData(RowKind.INSERT, expects.get(2))));
      harness.processWatermark(System.currentTimeMillis());
      harness.prepareSnapshotPreBarrier(++checkpoint);
      harness.snapshot(3, 1);
      harness.notifyOfCompletedCheckpoint(checkpoint);

      results = harness.extractOutputValues();
    } catch (Throwable e) {
      LOG.error("", e);
      throw e;
    }

    // check expects accuracy.
    Assert.assertEquals(3, results.size());
    results.forEach(result -> Assert.assertEquals(1, result.dataFiles().length));
    List<Object[]> expected = isGapNone ? expects.subList(1, expects.size()) : expects.subList(2, expects.size());
    checkLogstoreDataAccuracy(topic, expected);
    testKeyedTable.refresh();
    Assert.assertTrue(Boolean.parseBoolean(testKeyedTable.properties().get(LOG_STORE_CATCH_UP.key())));
  }

  private void checkLogstoreDataAccuracy(String topic, List<Object[]> expects) {
    LogDataJsonDeserialization<RowData> logDataJsonDeserialization =
        new LogDataJsonDeserialization<>(
            TABLE_SCHEMA,
            LogRecordV1.factory,
            LogRecordV1.arrayFactory,
            LogRecordV1.mapFactory
        );
    ConsumerRecords<byte[], byte[]> consumerRecords = kafkaTestBase.readRecordsBytes(topic);
    Assertions.assertEquals(expects.size(), consumerRecords.count());
    List<RowData> actual = new ArrayList<>();
    consumerRecords.forEach(consumerRecord -> {
      try {
        actual.add(logDataJsonDeserialization.deserialize(consumerRecord.value()).getActualValue());
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    Collection<RowData> expected = DataUtil.toRowData(expects);
    Assertions.assertEquals(
        expected.stream().sorted(
            Comparator
                .comparing(
                    RowData::toString)).collect(Collectors.toList()),
        actual.stream().sorted(
            Comparator
                .comparing(
                    RowData::toString)).collect(Collectors.toList()));
  }

  public TestOneInputStreamOperatorIntern<RowData, WriteResult> createSingleProducer(
      int maxParallelism,
      byte[] jobId,
      String topic,
      Duration writeLogstoreWatermarkGap
  ) throws Exception {
    return createProducer(
        maxParallelism, maxParallelism, 0, null, jobId, globalAggregateManger, topic,
        writeLogstoreWatermarkGap);
  }

  private TestOneInputStreamOperatorIntern<RowData, WriteResult> createProducer(
      int maxParallelism,
      int parallelism,
      int subTaskId,
      Long restoredCheckpointId,
      byte[] jobId,
      TestGlobalAggregateManager testGlobalAggregateManager,
      String topic,
      Duration writeLogstoreWatermarkGap) throws Exception {
    AutomaticLogWriter automaticLogWriter =
        new AutomaticLogWriter(
            TABLE_SCHEMA,
            getPropertiesByTopic(topic),
            topic,
            new HiddenKafkaFactory<>(),
            LogRecordV1.fieldGetterFactory,
            jobId,
            ShuffleHelper.EMPTY,
            tableLoader,
            writeLogstoreWatermarkGap);

    KeyedTable testKeyedTable = getArcticTable().asKeyedTable();
    RowType flinkSchemaRowType = (RowType) FLINK_SCHEMA.toRowDataType().getLogicalType();
    Schema writeSchema = TypeUtil.reassignIds(FlinkSchemaUtil.convert(FLINK_SCHEMA), testKeyedTable.schema());
    MetricsGenerator metricsGenerator = ArcticUtils.getMetricsGenerator(false,
        false, testKeyedTable, flinkSchemaRowType, writeSchema);

    ArcticFileWriter streamWriter = FlinkSink.createFileWriter(testKeyedTable,
        null,
        false,
        (RowType) FLINK_SCHEMA.toRowDataType().getLogicalType(),
        tableLoader);

    ArcticWriter<WriteResult> arcticWriter = new ArcticWriter<>(automaticLogWriter, streamWriter, metricsGenerator);

    TestOneInputStreamOperatorIntern<RowData, WriteResult> harness =
        new TestOneInputStreamOperatorIntern<>(
            arcticWriter,
            maxParallelism,
            parallelism,
            subTaskId,
            restoredCheckpointId,
            testGlobalAggregateManager);
    harness.getStreamConfig().setTimeCharacteristic(TimeCharacteristic.ProcessingTime);
    return harness;
  }

  private static Properties getPropertiesByTopic(String topic) {
    Properties properties = getPropertiesWithByteArray(kafkaTestBase.getProperties());
    properties.put(LOG_STORE_MESSAGE_TOPIC, topic);
    properties.put(ProducerConfig.ACKS_CONFIG, "all");
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, "0");
    return properties;
  }
}