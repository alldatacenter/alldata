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
package org.apache.drill.exec.store.parquet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Category({ParquetTest.class, UnlikelyTest.class})
public class TestVarlenDecimal extends ClusterTest {

  @BeforeClass
  public static void setUp() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
  }

  @BeforeClass
  public static void enableDecimalDataType() {
    client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
  }

  @AfterClass
  public static void disableDecimalDataType() {
    client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
  }

  private static final String DATAFILE = "cp.`parquet/varlenDecimal.parquet`";

  @Test
  public void testNullCount() throws Exception {
    String query = "select count(*) as c from %s where department_id is null";
    testBuilder()
        .sqlQuery(query, DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(1L)
        .go();
  }

  @Test
  public void testNotNullCount() throws Exception {
    String query = "select count(*) as c from %s where department_id is not null";
    testBuilder()
        .sqlQuery(query, DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(106L)
        .go();
  }

  @Test
  public void testSimpleQuery() throws Exception {
    String query = "select cast(department_id as bigint) as c from %s where cast(employee_id as decimal) = 170";

    testBuilder()
        .sqlQuery(query, DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(80L)
        .go();
  }

  @Test
  public void testWriteReadJson() throws Exception {
    // Drill stores decimal values in JSON files correctly, but it can read only double values, but not big decimal.
    // See JsonToken class.

    String tableName = "jsonWithDecimals";
    try {
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "json");

      String bigDecimalValue = "987654321987654321987654321.987654321";

      run(
          "create table dfs.tmp.%s as\n" +
              "select cast('%s' as decimal(36, 9)) dec36", tableName, bigDecimalValue);

      String json = FileUtils.readFileToString(
        Paths.get(dirTestWatcher.getDfsTestTmpDir().getPath(), tableName, "0_0_0.json").toFile(),
        Charset.defaultCharset()
      );

      Assert.assertThat(json, CoreMatchers.containsString(bigDecimalValue));

      // checks that decimal value may be read as a double value
      testBuilder()
          .sqlQuery("select dec36 from dfs.tmp.%s", tableName)
          .unOrdered()
          .baselineColumns("dec36")
          .baselineValues(new BigDecimal(bigDecimalValue).doubleValue())
          .go();
    } finally {
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
      run("drop table if exists dfs.tmp.%s", tableName);
    }
  }

  @Test
  public void testWriteReadCsv() throws Exception {
    String tableName = "csvWithDecimals";
    try {
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csvh");

      String bigDecimalValue = "987654321987654321987654321.987654321";

      run(
          "create table dfs.tmp.%s as\n" +
              "select cast('%s' as decimal(36, 9)) dec36", tableName, bigDecimalValue);

      String csv = FileUtils.readFileToString(
        Paths.get(dirTestWatcher.getDfsTestTmpDir().getPath(), tableName, "0_0_0.csvh").toFile(),
        Charset.defaultCharset()
      );

      Assert.assertThat(csv, CoreMatchers.containsString(bigDecimalValue));

      testBuilder()
          .sqlQuery("select cast(dec36 as decimal(36, 9)) as dec36 from dfs.tmp.%s", tableName)
          .ordered()
          .baselineColumns("dec36")
          .baselineValues(new BigDecimal(bigDecimalValue))
          .go();
    } finally {
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
      run("drop table if exists dfs.tmp.%s", tableName);
    }
  }

  @Test
  public void testUnionAllWithDifferentScales() throws Exception {
    try {
      run("create table dfs.tmp.t as select cast(999999999999999 as decimal(15,0)) as d");

      String query = "select cast(1000 as decimal(10,1)) as d\n" +
          "union all \n" +
          "select 596.000 as d \n" +
          "union all \n" +
          "select d from dfs.tmp.t";

      testBuilder()
          .sqlQuery(query)
          .unOrdered()
          .baselineColumns("d")
          .baselineValues(new BigDecimal("1000.000"))
          .baselineValues(new BigDecimal("596.000"))
          .baselineValues(new BigDecimal("999999999999999.000"))
          .go();

      List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Collections.singletonList(Pair.of(
          SchemaPath.getSimplePath("d"),
          Types.withPrecisionAndScale(MinorType.VARDECIMAL, DataMode.REQUIRED, 18, 3)));

      testBuilder()
          .sqlQuery(query)
          .schemaBaseLine(expectedSchema)
          .go();
    } finally {
      run("drop table if exists dfs.tmp.t");
    }
  }

  @Test // DRILL-7960
  public void testWideningLimit() throws Exception {
    // A union of VARDECIMALs that requires a widening to an unsupported
    // DECIMAL(40, 6). The resulting column should be limited DECIMAL(38, 6)
    // and a precision loss warning logged.
    String query = "SELECT CAST(10 AS DECIMAL(38, 4)) AS `Col1` " +
      "UNION ALL " +
      "SELECT CAST(22 AS DECIMAL(29, 6)) AS `Col1`";

    testBuilder().sqlQuery(query)
      .unOrdered()
      .baselineColumns("Col1")
      .baselineValues(new BigDecimal("10.000000"))
      .baselineValues(new BigDecimal("22.000000"))
      .go();

    List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Collections.singletonList(Pair.of(
        SchemaPath.getSimplePath("Col1"),
        Types.withPrecisionAndScale(MinorType.VARDECIMAL, DataMode.REQUIRED, 38, 6)
    ));

    testBuilder()
        .sqlQuery(query)
        .schemaBaseLine(expectedSchema)
        .go();
  }
}
