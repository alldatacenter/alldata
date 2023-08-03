package com.netease.arctic.flink.table;
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

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.flink.util.ArcticUtils;
import com.netease.arctic.flink.util.DataUtil;
import com.netease.arctic.flink.util.TestUtil;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.testutils.CommonTestUtils;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.CloseableIterator;
import org.apache.iceberg.io.TaskWriter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.netease.arctic.ams.api.MockArcticMetastoreServer.TEST_CATALOG_NAME;

public class TestWatermark extends FlinkTestBase {
  public static final Logger LOG = LoggerFactory.getLogger(TestWatermark.class);

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private static final String DB = TableTestHelper.TEST_TABLE_ID.getDatabase();
  private static final String TABLE = "test_keyed";

  public TestWatermark() {
    super(new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
      new BasicTableTestHelper(true, true));
  }

  @Before
  public void before() throws Exception {
    super.before();
    super.config();
  }

  @After
  public void after() {
    sql("DROP TABLE IF EXISTS arcticCatalog." + DB + "." + TABLE);
  }

  @Test(timeout = 30000)
  public void testWatermark() throws Exception {
    sql(String.format("CREATE CATALOG arcticCatalog WITH %s", toWithClause(props)));
    Map<String, String> tableProperties = new HashMap<>();
    String table = String.format("arcticCatalog.%s.%s", DB, TABLE);

    sql("CREATE TABLE IF NOT EXISTS %s (" +
            " id bigint, user_id int, name STRING, category string, op_time timestamp, is_true boolean" +
            ", PRIMARY KEY (id, user_id) NOT ENFORCED) PARTITIONED BY(category, name) WITH %s",
        table, toWithClause(tableProperties));

    TableSchema flinkSchema = TableSchema.builder()
        .field("id", DataTypes.BIGINT())
        .field("user_id", DataTypes.INT())
        .field("name", DataTypes.STRING())
        .field("category", DataTypes.STRING())
        .field("op_time", DataTypes.TIMESTAMP(3))
        .field("is_true", DataTypes.BOOLEAN())
        .build();
    RowType rowType = (RowType) flinkSchema.toRowDataType().getLogicalType();
    KeyedTable keyedTable = (KeyedTable) ArcticUtils.loadArcticTable(
        ArcticTableLoader.of(TableIdentifier.of(TEST_CATALOG_NAME, DB, TABLE), catalogBuilder));
    TaskWriter<RowData> taskWriter = createKeyedTaskWriter(keyedTable, rowType, true);
    List<RowData> baseData = new ArrayList<RowData>() {{
      add(GenericRowData.ofKind(
          RowKind.INSERT, 2L, 123, StringData.fromString("a"), StringData.fromString("a"),
          TimestampData.fromLocalDateTime(LocalDateTime.now().minusMinutes(1)), true));
    }};
    for (RowData record : baseData) {
      taskWriter.write(record);
    }
    commit(keyedTable, taskWriter.complete(), true);

    sql("create table d (tt as cast(op_time as timestamp(3)), watermark for tt as tt) like %s", table);

    Table source = getTableEnv().sqlQuery("select is_true from d");

    WatermarkTestOperator op = new WatermarkTestOperator();
    getTableEnv().toRetractStream(source, RowData.class)
        .transform("test watermark", TypeInformation.of(RowData.class), op);
    getEnv().executeAsync("test watermark");

    op.waitWatermark();

    Assert.assertTrue(op.watermark > Long.MIN_VALUE);
  }

  @Test
  public void testSelectWatermarkField() throws Exception {
    sql(String.format("CREATE CATALOG arcticCatalog WITH %s", toWithClause(props)));
    Map<String, String> tableProperties = new HashMap<>();
    String table = String.format("arcticCatalog.%s.%s", DB, TABLE);

    sql("CREATE TABLE IF NOT EXISTS %s (" +
            " id bigint, user_id int, name STRING, category string, op_time timestamp, is_true boolean" +
            ", PRIMARY KEY (id, user_id) NOT ENFORCED) PARTITIONED BY(category, name) WITH %s",
        table, toWithClause(tableProperties));

    TableSchema flinkSchema = TableSchema.builder()
        .field("id", DataTypes.BIGINT())
        .field("user_id", DataTypes.INT())
        .field("name", DataTypes.STRING())
        .field("category", DataTypes.STRING())
        .field("op_time", DataTypes.TIMESTAMP(3))
        .field("is_true", DataTypes.BOOLEAN())
        .build();
    RowType rowType = (RowType) flinkSchema.toRowDataType().getLogicalType();
    KeyedTable keyedTable = (KeyedTable) ArcticUtils.loadArcticTable(
        ArcticTableLoader.of(TableIdentifier.of(TEST_CATALOG_NAME, DB, TABLE), catalogBuilder));
    TaskWriter<RowData> taskWriter = createKeyedTaskWriter(keyedTable, rowType, true);
    List<RowData> baseData = new ArrayList<RowData>() {{
      add(GenericRowData.ofKind(
          RowKind.INSERT, 2L, 123, StringData.fromString("a"), StringData.fromString("a"),
          TimestampData.fromLocalDateTime(LocalDateTime.parse("2022-06-17T10:08:11.0")), true));
    }};
    for (RowData record : baseData) {
      taskWriter.write(record);
    }
    commit(keyedTable, taskWriter.complete(), true);

    sql("create table d (tt as cast(op_time as timestamp(3)), watermark for tt as tt) like %s", table);

    TableResult result = exec("select is_true, tt from d");

    CommonTestUtils.waitUntilJobManagerIsInitialized(() -> result.getJobClient().get().getJobStatus().get());
    Set<Row> actual = new HashSet<>();
    try (CloseableIterator<Row> iterator = result.collect()) {
      Row row = iterator.next();
      actual.add(row);
    }
    result.getJobClient().ifPresent(TestUtil::cancelJob);

    List<Object[]> expected = new LinkedList<>();
    expected.add(new Object[]{true, LocalDateTime.parse("2022-06-17T10:08:11")});
    Assert.assertEquals(DataUtil.toRowSet(expected), actual);
  }

  public static class WatermarkTestOperator extends AbstractStreamOperator<RowData>
      implements OneInputStreamOperator<Tuple2<Boolean, RowData>, RowData> {

    private static final long serialVersionUID = 1L;
    public long watermark;
    private static CompletableFuture<Void> waitWatermark = new CompletableFuture<>();

    public WatermarkTestOperator() {
      super();
      chainingStrategy = ChainingStrategy.ALWAYS;
    }

    private void waitWatermark() throws InterruptedException, ExecutionException {
      waitWatermark.get();
    }

    @Override
    public void processElement(StreamRecord<Tuple2<Boolean, RowData>> element) throws Exception {
      output.collect(element.asRecord());
    }

    @Override
    public void processWatermark(Watermark mark) throws Exception {
      LOG.info("processWatermark: {}", mark);
      watermark = mark.getTimestamp();
      waitWatermark.complete(null);
      super.processWatermark(mark);
    }
  }

}
