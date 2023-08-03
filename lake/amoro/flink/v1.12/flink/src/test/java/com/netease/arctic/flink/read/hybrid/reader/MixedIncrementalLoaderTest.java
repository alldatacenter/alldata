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

package com.netease.arctic.flink.read.hybrid.reader;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.flink.read.MixedIncrementalLoader;
import com.netease.arctic.flink.read.hybrid.enumerator.ContinuousSplitPlanner;
import com.netease.arctic.flink.read.hybrid.enumerator.MergeOnReadIncrementalPlanner;
import com.netease.arctic.flink.read.source.FlinkArcticMORDataReader;
import com.netease.arctic.flink.util.DataUtil;
import com.netease.arctic.flink.write.FlinkTaskWriterBaseTest;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.flink.data.RowDataUtil;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.TaskWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RunWith(value = Parameterized.class)
public class MixedIncrementalLoaderTest extends TableTestBase implements FlinkTaskWriterBaseTest {

  public MixedIncrementalLoaderTest(boolean partitionedTable) {
    super(
        new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
        new BasicTableTestHelper(true, partitionedTable));
  }

  @Parameterized.Parameters(name = "partitionedTable = {0}")
  public static Object[][] parameters() {
    // todo mix hive test
    return new Object[][]{
        {true},
        {false}};
  }

  @Before
  public void before() throws IOException {
    ArcticTable arcticTable = getArcticTable();
    TableSchema flinkPartialSchema = TableSchema.builder()
        .field("id", DataTypes.INT())
        .field("name", DataTypes.STRING())
        .field("ts", DataTypes.BIGINT())
        .field("op_time", DataTypes.TIMESTAMP())
        .build();
    RowType rowType = (RowType) flinkPartialSchema.toRowDataType().getLogicalType();

    List<RowData> expected = Lists.newArrayList(
        DataUtil.toRowData(1000011, "a", 1010L, LocalDateTime.parse("2022-06-18T10:10:11.0")),
        DataUtil.toRowData(1000012, "b", 1011L, LocalDateTime.parse("2022-06-18T10:10:11.0")),
        DataUtil.toRowData(1000013, "c", 1012L, LocalDateTime.parse("2022-06-18T10:10:11.0")),
        DataUtil.toRowData(1000014, "d", 1013L, LocalDateTime.parse("2022-06-21T10:10:11.0")),
        DataUtil.toRowData(1000015, "e", 1014L, LocalDateTime.parse("2022-06-21T10:10:11.0"))
    );
    for (RowData rowData : expected) {
      try (TaskWriter<RowData> taskWriter = createBaseTaskWriter(arcticTable, rowType)) {
        writeAndCommit(rowData, taskWriter, arcticTable);
      }
    }

    expected = Lists.newArrayList(
        DataUtil.toRowDataWithKind(RowKind.DELETE, 1000015, "e", 1014L, LocalDateTime.parse("2022-06-21T10:10:11.0")),
        DataUtil.toRowData(1000021, "a", 1020L, LocalDateTime.parse("2022-06-28T10:10:11.0")),
        DataUtil.toRowData(1000022, "b", 1021L, LocalDateTime.parse("2022-06-28T10:10:11.0")),
        DataUtil.toRowData(1000023, "c", 1022L, LocalDateTime.parse("2022-06-28T10:10:11.0")),
        DataUtil.toRowData(1000024, "d", 1023L, LocalDateTime.parse("2022-06-28T10:10:11.0")),
        DataUtil.toRowData(1000025, "e", 1024L, LocalDateTime.parse("2022-06-28T10:10:11.0"))
    );
    for (RowData rowData : expected) {
      try (TaskWriter<RowData> taskWriter = createTaskWriter(arcticTable, rowType)) {
        writeAndCommit(rowData, taskWriter, arcticTable);
      }
    }
  }

  @Test
  public void testMOR() {
    KeyedTable keyedTable = getArcticTable().asKeyedTable();
    List<Expression> expressions =
        Lists.newArrayList(
            Expressions.greaterThan("op_time", "2022-06-20T10:10:11.0")
        );
    ContinuousSplitPlanner morPlanner = new MergeOnReadIncrementalPlanner(
        getTableLoader(getCatalogName(), getMetastoreUrl(), keyedTable));

    FlinkArcticMORDataReader flinkArcticMORDataReader = new FlinkArcticMORDataReader(
        keyedTable.io(),
        keyedTable.schema(),
        keyedTable.schema(),
        keyedTable.primaryKeySpec(),
        null,
        true,
        RowDataUtil::convertConstant,
        true
    );

    MixedIncrementalLoader<RowData> incrementalLoader =
        new MixedIncrementalLoader<>(
            morPlanner,
            flinkArcticMORDataReader,
            new RowDataReaderFunction(
                new Configuration(),
                keyedTable.schema(),
                keyedTable.schema(),
                keyedTable.asKeyedTable().primaryKeySpec(),
                null,
                true,
                keyedTable.io(),
                true),
            expressions
        );

    List<RowData> actuals = new ArrayList<>();
    while (incrementalLoader.hasNext()) {
      CloseableIterator<RowData> iterator = incrementalLoader.next();
      while (iterator.hasNext()) {
        RowData rowData = iterator.next();
        System.out.println(rowData);
        actuals.add(rowData);
      }
    }
    if (isPartitionedTable()) {
      Assert.assertEquals(6, actuals.size());
    } else {
      Assert.assertEquals(9, actuals.size());
    }
  }

  @Override
  public String getMetastoreUrl() {
    return getCatalogUrl();
  }

  @Override
  public String getCatalogName() {
    return getCatalog().name();
  }
}