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

package com.netease.arctic.flink.read.hidden.pulsar;

import com.netease.arctic.TableTestBase;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.flink.InternalCatalogBuilder;
import com.netease.arctic.flink.read.source.log.pulsar.LogPulsarSource;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.table.descriptors.ArcticValidator;
import com.netease.arctic.flink.util.FailoverTestUtil;
import com.netease.arctic.flink.util.TestUtil;
import com.netease.arctic.flink.util.pulsar.LogPulsarHelper;
import com.netease.arctic.flink.util.pulsar.PulsarTestEnvironment;
import com.netease.arctic.flink.util.pulsar.runtime.PulsarRuntime;
import com.netease.arctic.flink.write.FlinkSink;
import com.netease.arctic.flink.write.hidden.BaseLogTest;
import com.netease.arctic.log.LogData;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StartCursor;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.runtime.minicluster.RpcServiceSharing;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;
import org.apache.flink.test.util.MiniClusterWithClientResource;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.netease.arctic.ams.api.MockArcticMetastoreServer.TEST_CATALOG_NAME;
import static com.netease.arctic.ams.api.MockArcticMetastoreServer.TEST_DB_NAME;
import static com.netease.arctic.flink.read.ArcticSourceTest.tableRecords;
import static com.netease.arctic.flink.util.FailoverTestUtil.triggerFailover;
import static com.netease.arctic.flink.write.hidden.BaseLogTest.FLINK_USER_SCHEMA;
import static com.netease.arctic.flink.write.hidden.BaseLogTest.userSchema;
import static com.netease.arctic.flink.write.hidden.HiddenLogOperatorsTest.DATA_INDEX;
import static org.apache.flink.connector.pulsar.common.config.PulsarOptions.PULSAR_ADMIN_URL;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_MAX_FETCH_TIME;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_SUBSCRIPTION_NAME;

public class LogPulsarSourceTest extends TableTestBase {

  private static final Logger LOG = LoggerFactory.getLogger(LogPulsarSourceTest.class);

  @ClassRule
  public static PulsarTestEnvironment environment = new PulsarTestEnvironment(PulsarRuntime.container());
  public String TOPIC = "LogPulsarSourceTest_";
  public static final int PARALLELISM = 3;
  private List<LogData<RowData>> dataInPulsar;
  public LogPulsarHelper logPulsarHelper;

  public static final String RESULT_TABLE = "result";
  private static final TableIdentifier RESULT_TABLE_ID =
      TableIdentifier.of(TEST_CATALOG_NAME, TEST_DB_NAME, RESULT_TABLE);
  private static KeyedTable result;
  public static InternalCatalogBuilder catalogBuilder;
  @Rule
  public TestName testName = new TestName();
  
  @Rule
  public final MiniClusterWithClientResource miniClusterResource =
      new MiniClusterWithClientResource(
          new MiniClusterResourceConfiguration.Builder()
              .setNumberTaskManagers(PARALLELISM)
              .setNumberSlotsPerTaskManager(1)
              .setRpcServiceSharing(RpcServiceSharing.DEDICATED)
              .withHaLeadershipControl()
              .build());

  @Before
  public void initData() throws Exception {
    TOPIC += TestUtil.getUtMethodName(testName);
    TestUtil.cancelAllJobs(miniClusterResource.getMiniCluster());
    logPulsarHelper = new LogPulsarHelper(environment);
    // |0 1 2 3 4 5 6 7 8 9 Flip 10 11 12 13 14| 15 16 17 18 19
    dataInPulsar = logPulsarHelper.write(TOPIC, 0);

    testCatalog = CatalogLoader.load(AMS.getUrl());
    result = testCatalog
        .newTableBuilder(RESULT_TABLE_ID, userSchema)
        .withProperty(TableProperties.LOCATION, tableDir.getPath() + "/" + RESULT_TABLE)
        .withPrimaryKeySpec(BaseLogTest.PRIMARY_KEY_SPEC)
        .create().asKeyedTable();
    catalogBuilder = InternalCatalogBuilder.builder().metastoreUrl(AMS.getUrl());
  }

  @After
  public void after() {
    testCatalog.dropTable(RESULT_TABLE_ID, true);
    logPulsarHelper.op().deleteTopicByForce(TOPIC);
  }

  @Test(timeout = 60000)
  public void testTmFailover() throws Exception {
    testArcticSource(FailoverTestUtil.FailoverType.TM, false);
  }

  @Test(timeout = 60000)
  public void testJmFailover() throws Exception {
    testArcticSource(FailoverTestUtil.FailoverType.JM, false);
  }

  public void testArcticSource(FailoverTestUtil.FailoverType failoverType, boolean logRetractionEnabled) throws Exception {
    LogPulsarSource source = createSource(null, logRetractionEnabled, logPulsarHelper, TOPIC);
    List<RowData> excepted = getExcepted(logRetractionEnabled);

    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    // enable checkpoint
    env.enableCheckpointing(1000);
    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(1, 0));

    DataStream<RowData> input = env.fromSource(
            source,
            WatermarkStrategy.noWatermarks(),
            "LogPulsarSource")
        .setParallelism(PARALLELISM);

    DataStream<RowData> streamFailingInTheMiddleOfReading =
        FailoverTestUtil.RecordCounterToFail.wrapWithFailureAfter(input, excepted.size() / 2);

    FlinkSink
        .forRowData(streamFailingInTheMiddleOfReading)
        .table(result)
        .tableLoader(ArcticTableLoader.of(RESULT_TABLE_ID, catalogBuilder))
        .flinkSchema(FlinkSchemaUtil.toSchema(FLINK_USER_SCHEMA))
        .build();

    JobClient jobClient = env.executeAsync("Bounded Arctic Source Failover Test");
    JobID jobId = jobClient.getJobID();

    FailoverTestUtil.RecordCounterToFail.waitToFail();
    triggerFailover(
        failoverType,
        jobId,
        FailoverTestUtil.RecordCounterToFail::continueProcessing,
        miniClusterResource.getMiniCluster());

    assertRecords(result, excepted, Duration.ofMillis(2000), 50);
  }

  public static void assertRecords(
      KeyedTable testFailoverTable, List<RowData> expected, Duration checkInterval, int maxCheckCount)
      throws InterruptedException {
    for (int i = 0; i < maxCheckCount; ++i) {
      if (equalsRecords(expected, tableRecords(testFailoverTable))) {
        break;
      } else {
        Thread.sleep(checkInterval.toMillis());
      }
    }
    // success or failure, assert on the latest table state
    equalsRecords(expected, tableRecords(testFailoverTable));
  }

  private static boolean equalsRecords(List<RowData> expected, List<RowData> tableRecords) {
    try {
      Integer[] expectedArray = extractAndSortData(expected);
      Integer[] actualArray = extractAndSortData(tableRecords);
      Assert.assertArrayEquals(expectedArray, actualArray);
      return true;
    } catch (Throwable e) {
      return false;
    }
  }

  private static Integer[] extractAndSortData(List<RowData> records) {
    return records.stream().map(rowData -> rowData.getInt(DATA_INDEX)).sorted()
        .collect(Collectors.toList())
        .toArray(new Integer[records.size()]);
  }

  private List<RowData> getExcepted(boolean logRetractionEnable) {
    List<RowData> excepted = new ArrayList<>(dataInPulsar.size());
    if (!logRetractionEnable) {
      for (int i = 0; i < dataInPulsar.size(); i++) {
        if (dataInPulsar.get(i).getFlip()) {
          continue;
        }
        excepted.add(dataInPulsar.get(i).getActualValue());
      }
    }
    excepted.sort(Comparator.comparingInt((r -> r.getInt(DATA_INDEX))));
    return excepted;
  }

  public static LogPulsarSource createSource(Properties conf, boolean logRetractionEnabled,
                                             LogPulsarHelper logPulsarHelper, String topic) {
    Map<String, String> tableProperties = new HashMap<>();
    tableProperties.put(TableProperties.LOG_STORE_ADDRESS, logPulsarHelper.op().serviceUrl());
    tableProperties.put(ArcticValidator.ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.key(),
        String.valueOf(logRetractionEnabled));

    Properties properties = new Properties();
    if (conf != null) {
      properties.putAll(conf);
    }
    properties.put(PULSAR_ADMIN_URL.key(), logPulsarHelper.op().adminUrl());
    properties.put(PULSAR_SUBSCRIPTION_NAME.key(), "source-test");
    properties.put(PULSAR_MAX_FETCH_TIME.key(), Duration.ofSeconds(1).toMillis());

    return (LogPulsarSource) LogPulsarSource.builder(userSchema, tableProperties)
        .setProperties(properties)
        .setTopics(topic)
        .setStartCursor(StartCursor.earliest())
        .build();
  }

}


