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
package org.apache.drill.exec.fn.impl;

import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.util.Text;
import org.apache.drill.categories.EasyOutOfMemory;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.PlannerTest;
import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({ SqlFunctionTest.class, OperatorTest.class, PlannerTest.class, EasyOutOfMemory.class })
public class TestAggregateFunctions extends ClusterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setUp() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    dirTestWatcher.copyResourceToRoot(Paths.get("agg"));
  }

  /*
   * Test checks the count of a nullable column within a map
   * and verifies count is equal only to the number of times the
   * column appears and doesn't include the null count
   */
  @Test
  public void testCountOnNullableColumn() throws Exception {
    testBuilder()
        .sqlQuery("select count(t.x.y)  as cnt1, count(`integer`) as cnt2 from cp.`jsoninput/input2.json` t")
        .ordered()
        .baselineColumns("cnt1", "cnt2")
        .baselineValues(3L, 4L)
        .go();
  }

  @Test
  public void testCountDistinctOnBoolColumn() throws Exception {
    testBuilder()
        .sqlQuery("select count(distinct `bool_val`) as cnt from `sys`.`options_old`")
        .ordered()
        .baselineColumns("cnt")
        .baselineValues(2L)
        .go();
  }

  @Test
  public void testMaxWithZeroInput() throws Exception {
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, false);
      testBuilder()
          .sqlQuery("select max(employee_id * 0.0) as max_val from cp.`employee.json`")
          .unOrdered()
          .baselineColumns("max_val")
          .baselineValues(0.0)
          .go();
    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  @Ignore
  @Test // DRILL-2092: count distinct, non distinct aggregate with group-by
  public void testDrill2092() throws Exception {
    String query = "select a1, b1, count(distinct c1) as dist1, \n"
        + "sum(c1) as sum1, count(c1) as cnt1, count(*) as cnt \n"
        + "from cp.`agg/bugs/drill2092/input.json` \n"
        + "group by a1, b1 order by a1, b1";

    String baselineQuery =
        "select case when columns[0]='null' then cast(null as bigint) else cast(columns[0] as bigint) end as a1, \n"
        + "case when columns[1]='null' then cast(null as bigint) else cast(columns[1] as bigint) end as b1, \n"
        + "case when columns[2]='null' then cast(null as bigint) else cast(columns[2] as bigint) end as dist1, \n"
        + "case when columns[3]='null' then cast(null as bigint) else cast(columns[3] as bigint) end as sum1, \n"
        + "case when columns[4]='null' then cast(null as bigint) else cast(columns[4] as bigint) end as cnt1, \n"
        + "case when columns[5]='null' then cast(null as bigint) else cast(columns[5] as bigint) end as cnt \n"
        + "from cp.`agg/bugs/drill2092/result.tsv`";


    // NOTE: this type of query gets rewritten by Calcite into an inner join of subqueries, so
    // we need to test with both hash join and merge join

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .optionSettingQueriesForTestQuery("alter system set `planner.enable_hashjoin` = true")
        .sqlBaselineQuery(baselineQuery)
        .build().run();

    testBuilder()
    .sqlQuery(query)
    .ordered()
    .optionSettingQueriesForTestQuery("alter system set `planner.enable_hashjoin` = false")
    .sqlBaselineQuery(baselineQuery)
    .build().run();

  }

  @Test // DRILL-2170: Subquery has group-by, order-by on aggregate function and limit
  @Category(UnlikelyTest.class)
  public void testDrill2170() throws Exception {
    String query =
        "select count(*) as cnt from "
        + "cp.`tpch/orders.parquet` o inner join\n"
        + "(select l_orderkey, sum(l_quantity), sum(l_extendedprice) \n"
        + "from cp.`tpch/lineitem.parquet` \n"
        + "group by l_orderkey order by 3 limit 100) sq \n"
        + "on sq.l_orderkey = o.o_orderkey";

    testBuilder()
    .sqlQuery(query)
    .ordered()
    .optionSettingQueriesForTestQuery("alter system set `planner.slice_target` = 1000")
    .baselineColumns("cnt")
    .baselineValues(100L)
    .go();
  }

  @Test // DRILL-2168
  @Category(UnlikelyTest.class)
  public void testGBExprWithDrillFunc() throws Exception {
    testBuilder()
        .ordered()
        .sqlQuery("select concat(n_name, cast(n_nationkey as varchar(10))) as name, count(*) as cnt " +
            "from cp.`tpch/nation.parquet` " +
            "group by concat(n_name, cast(n_nationkey as varchar(10))) " +
            "having concat(n_name, cast(n_nationkey as varchar(10))) > 'UNITED'" +
            "order by concat(n_name, cast(n_nationkey as varchar(10)))")
        .baselineColumns("name", "cnt")
        .baselineValues("UNITED KINGDOM23", 1L)
        .baselineValues("UNITED STATES24", 1L)
        .baselineValues("VIETNAM21", 1L)
        .build().run();
  }

  @Test //DRILL-2242
  @Category(UnlikelyTest.class)
  public void testDRILLNestedGBWithSubsetKeys() throws Exception {
    String sql = " select count(*) as cnt from (select l_partkey from\n" +
        "   (select l_partkey, l_suppkey from cp.`tpch/lineitem.parquet`\n" +
        "      group by l_partkey, l_suppkey) \n" +
        "   group by l_partkey )";

    client.alterSession(ExecConstants.SLICE_TARGET, 1);
    client.alterSession(PlannerSettings.MULTIPHASE.getOptionName(), false);

    testBuilder()
        .ordered()
        .sqlQuery(sql)
        .baselineColumns("cnt")
        .baselineValues(2000L)
        .build().run();

    client.alterSession(ExecConstants.SLICE_TARGET, 1);
    client.alterSession(PlannerSettings.MULTIPHASE.getOptionName(), true);

    testBuilder()
        .ordered()
        .sqlQuery(sql)
        .baselineColumns("cnt")
        .baselineValues(2000L)
        .build().run();

    client.alterSession(ExecConstants.SLICE_TARGET, 1000);
  }

  @Test
  public void testAvgWithNullableScalarFunction() throws Exception {
    String query = " select avg(length(b1)) as col from cp.`jsoninput/nullable1.json`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(3.0d)
        .go();
  }

  @Test
  public void testCountWithAvg() throws Exception {
    testBuilder()
        .sqlQuery("select count(a) col1, avg(b) col2 from cp.`jsoninput/nullable3.json`")
        .unOrdered()
        .baselineColumns("col1", "col2")
        .baselineValues(2L, 3.0d)
        .go();

    testBuilder()
        .sqlQuery("select count(a) col1, avg(a) col2 from cp.`jsoninput/nullable3.json`")
        .unOrdered()
        .baselineColumns("col1", "col2")
        .baselineValues(2L, 1.0d)
        .go();
  }

  @Test
  public void testAvgOnKnownType() throws Exception {
    testBuilder()
        .sqlQuery("select avg(cast(employee_id as bigint)) as col from cp.`employee.json`")
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(578.9982683982684d)
        .go();
  }

  @Test
  public void testStddevOnKnownType() throws Exception {
    testBuilder()
        .sqlQuery("select stddev_samp(cast(employee_id as int)) as col from cp.`employee.json`")
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(333.56708470261117d)
        .go();
  }

  @Test
  public void testVarSampDecimal() throws Exception {
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      testBuilder()
          .sqlQuery("select var_samp(cast(employee_id as decimal(28, 20))) as dec20,\n" +
                "var_samp(cast(employee_id as decimal(28, 0))) as dec6,\n" +
                "var_samp(cast(employee_id as integer)) as d\n" +
                "from cp.`employee.json`")
          .unOrdered()
          .baselineColumns("dec20", "dec6", "d")
          .baselineValues(new BigDecimal("111266.99999699895713760532"),
              new BigDecimal("111266.999997"),
              111266.99999699896)
          .go();
    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  @Test
  public void testVarPopDecimal() throws Exception {
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      testBuilder()
          .sqlQuery("select var_pop(cast(employee_id as decimal(28, 20))) as dec20,\n" +
              "var_pop(cast(employee_id as decimal(28, 0))) as dec6,\n" +
              "var_pop(cast(employee_id as integer)) as d\n" +
              "from cp.`employee.json`")
          .unOrdered()
          .baselineColumns("dec20", "dec6", "d")
          .baselineValues(new BigDecimal("111170.66493206649050804895"),
              new BigDecimal("111170.664932"),
              111170.66493206649)
          .go();
    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  @Test
  public void testStddevSampDecimal() throws Exception {
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      testBuilder()
          .sqlQuery("select stddev_samp(cast(employee_id as decimal(28, 20))) as dec20,\n" +
              "stddev_samp(cast(employee_id as decimal(28, 0))) as dec6,\n" +
              "stddev_samp(cast(employee_id as integer)) as d\n" +
              "from cp.`employee.json`")
          .unOrdered()
          .baselineColumns("dec20", "dec6", "d")
          .baselineValues(new BigDecimal("333.56708470261114349632"),
              new BigDecimal("333.567085"),
              333.56708470261117) // last number differs because of double precision.
          // Was taken sqrt of 111266.99999699895713760531784795216338 and decimal result is correct
          .go();
    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  @Test
  public void testStddevPopDecimal() throws Exception {
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      testBuilder()
          .sqlQuery("select stddev_pop(cast(employee_id as decimal(28, 20))) as dec20,\n" +
              "stddev_pop(cast(employee_id as decimal(28, 0))) as dec6,\n" +
              "stddev_pop(cast(employee_id as integer)) as d\n" +
              "from cp.`employee.json`")
          .unOrdered()
          .baselineColumns("dec20", "dec6", "d")
          .baselineValues(new BigDecimal("333.42265209800381903633"),
              new BigDecimal("333.422652"),
              333.4226520980038)
          .go();
    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  @Test
  public void testSumDecimal() throws Exception {
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      testBuilder()
          .sqlQuery("select sum(cast(employee_id as decimal(9, 0))) as colDecS0,\n" +
              "sum(cast(employee_id as decimal(12, 3))) as colDecS3,\n" +
              "sum(cast(employee_id as integer)) as colInt\n" +
              "from cp.`employee.json`")
          .unOrdered()
          .baselineColumns("colDecS0", "colDecS3", "colInt")
          .baselineValues(BigDecimal.valueOf(668743), new BigDecimal("668743.000"), 668743L)
          .go();
    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  @Test
  public void testAvgDecimal() throws Exception {
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      testBuilder()
          .sqlQuery("select avg(cast(employee_id as decimal(28, 20))) as colDec20,\n" +
              "avg(cast(employee_id as decimal(28, 0))) as colDec6,\n" +
              "avg(cast(employee_id as integer)) as colInt\n" +
              "from cp.`employee.json`")
          .unOrdered()
          .baselineColumns("colDec20", "colDec6", "colInt")
          .baselineValues(new BigDecimal("578.99826839826839826840"),
              new BigDecimal("578.998268"),
              578.9982683982684)
          .go();
    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  @Test
  public void testSumAvgDecimalLimit0() throws Exception {
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema =
        ImmutableList.of(
            Pair.of(SchemaPath.getSimplePath("sum_col"),
                Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 38, 3)),
            Pair.of(SchemaPath.getSimplePath("avg_col"),
                Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 38, 6)),
            Pair.of(SchemaPath.getSimplePath("stddev_pop_col"),
                Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 38, 6)),
            Pair.of(SchemaPath.getSimplePath("stddev_samp_col"),
                Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 38, 6)),
            Pair.of(SchemaPath.getSimplePath("var_pop_col"),
                Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 38, 6)),
            Pair.of(SchemaPath.getSimplePath("var_samp_col"),
                Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 38, 6)),
            Pair.of(SchemaPath.getSimplePath("max_col"),
                Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 9, 3)),
            Pair.of(SchemaPath.getSimplePath("min_col"),
                Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL, 9, 3)));

    String query =
        "select\n" +
            "sum(cast(employee_id as decimal(9, 3))) sum_col,\n" +
            "avg(cast(employee_id as decimal(9, 3))) avg_col,\n" +
            "stddev_pop(cast(employee_id as decimal(9, 3))) stddev_pop_col,\n" +
            "stddev_samp(cast(employee_id as decimal(9, 3))) stddev_samp_col,\n" +
            "var_pop(cast(employee_id as decimal(9, 3))) var_pop_col,\n" +
            "var_samp(cast(employee_id as decimal(9, 3))) var_samp_col,\n" +
            "max(cast(employee_id as decimal(9, 3))) max_col,\n" +
            "min(cast(employee_id as decimal(9, 3))) min_col\n" +
            "from cp.`employee.json` limit 0";
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      client.alterSession(ExecConstants.EARLY_LIMIT0_OPT_KEY, true);

      testBuilder()
          .sqlQuery(query)
          .schemaBaseLine(expectedSchema)
          .go();

      client.alterSession(ExecConstants.EARLY_LIMIT0_OPT_KEY, false);

      testBuilder()
        .sqlQuery(query)
        .schemaBaseLine(expectedSchema)
        .go();

    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
      client.resetSession(ExecConstants.EARLY_LIMIT0_OPT_KEY);
    }
  }

  @Test // DRILL-6221
  public void testAggGroupByWithNullDecimal() throws Exception {
    String fileName = "table.json";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      writer.write("{\"a\": 1, \"b\": 0}");
      writer.write("{\"b\": 2}");
    }

    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      testBuilder()
          .sqlQuery("select sum(cast(a as decimal(9,0))) as s,\n" +
              "avg(cast(a as decimal(9,0))) as av,\n" +
              "var_samp(cast(a as decimal(9,0))) as varSamp,\n" +
              "var_pop(cast(a as decimal(9,0))) as varPop,\n" +
              "stddev_pop(cast(a as decimal(9,0))) as stddevPop,\n" +
              "stddev_samp(cast(a as decimal(9,0))) as stddevSamp," +
              "max(cast(a as decimal(9,0))) as mx," +
            "min(cast(a as decimal(9,0))) as mn from dfs.`%s` t group by a", fileName)
          .unOrdered()
          .baselineColumns("s", "av", "varSamp", "varPop", "stddevPop", "stddevSamp", "mx", "mn")
          .baselineValues(BigDecimal.valueOf(1), new BigDecimal("1.000000"), new BigDecimal("0.000000"),
              new BigDecimal("0.000000"), new BigDecimal("0.000000"), new BigDecimal("0.000000"),
              BigDecimal.valueOf(1), BigDecimal.valueOf(1))
          .baselineValues(null, null, null, null, null, null, null, null)
          .go();

    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  @Test
  // test aggregates when input is empty and data type is optional
  public void countEmptyNullableInput() throws Exception {
    String query = "select " +
        "count(employee_id) col1, avg(employee_id) col2, sum(employee_id) col3 " +
        "from cp.`employee.json` where 1 = 0";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3")
        .baselineValues(0L, null, null)
        .go();
  }

  @Test
  @Ignore("DRILL-4473")
  public void sumEmptyNonexistentNullableInput() throws Exception {
    final String query = "select "
        +
        "sum(int_col) col1, sum(bigint_col) col2, sum(float4_col) col3, sum(float8_col) col4, sum(interval_year_col) col5 "
        +
        "from cp.`employee.json` where 1 = 0";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3", "col4", "col5")
        .baselineValues(null, null, null, null, null)
        .go();
  }

  @Test
  @Ignore("DRILL-4473")
  public void avgEmptyNonexistentNullableInput() throws Exception {
    // test avg function
    final String query = "select "
        +
        "avg(int_col) col1, avg(bigint_col) col2, avg(float4_col) col3, avg(float8_col) col4, avg(interval_year_col) col5 "
        +
        "from cp.`employee.json` where 1 = 0";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3", "col4", "col5")
        .baselineValues(null, null, null, null, null)
        .go();
  }

  @Test
  public void stddevEmptyNonexistentNullableInput() throws Exception {
    // test stddev function
    final String query = "select " +
        "stddev_pop(int_col) col1, stddev_pop(bigint_col) col2, stddev_pop(float4_col) col3, " +
        "stddev_pop(float8_col) col4, stddev_pop(interval_year_col) col5 " +
        "from cp.`employee.json` where 1 = 0";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3", "col4", "col5")
        .baselineValues(null, null, null, null, null)
        .go();

  }

  @Test
  public void minMaxEmptyNonNullableInput() throws Exception {
    // test min and max functions on required type

    final QueryDataBatch result = queryBuilder().sql("select * from cp.`parquet/alltypes_required.parquet` limit 0").results()
        .get(0);

    final Map<String, StringBuilder> functions = Maps.newHashMap();
    functions.put("min", new StringBuilder());
    functions.put("max", new StringBuilder());

    final Map<String, Object> resultingValues = Maps.newHashMap();
    for (UserBitShared.SerializedField field : result.getHeader().getDef().getFieldList()) {
      final String fieldName = field.getNamePart().getName();
      // Only COUNT aggregate function supported for Boolean type
      if (fieldName.equals("col_bln")) {
        continue;
      }
      resultingValues.put(String.format("`%s`", fieldName), null);
      for (Map.Entry<String, StringBuilder> function : functions.entrySet()) {
        function.getValue()
            .append(function.getKey())
            .append("(")
            .append(fieldName)
            .append(") ")
            .append(fieldName)
            .append(",");
      }
    }
    result.release();

    final String query = "select %s from cp.`parquet/alltypes_required.parquet` where 1 = 0";
    final List<Map<String, Object>> baselineRecords = Lists.newArrayList();
    baselineRecords.add(resultingValues);

    for (StringBuilder selectBody : functions.values()) {
      selectBody.setLength(selectBody.length() - 1);

      testBuilder()
          .sqlQuery(query, selectBody.toString())
          .unOrdered()
          .baselineRecords(baselineRecords)
          .go();
    }
  }

  @Test
  public void testSingleValueFunction() throws Exception {
    List<String> tableNames = Arrays.asList(
        "cp.`parquet/alltypes_required.parquet`",
        "cp.`parquet/alltypes_optional.parquet`");
    for (String tableName : tableNames) {
      final QueryDataBatch result =
          queryBuilder().sql("select * from %s limit 1", tableName).results().get(0);

      final Map<String, StringBuilder> functions = new HashMap<>();
      functions.put("single_value", new StringBuilder());

      final Map<String, Object> resultingValues = new HashMap<>();
      final List<String> columns = new ArrayList<>();

      final RecordBatchLoader loader = new RecordBatchLoader(cluster.allocator());
      loader.load(result.getHeader().getDef(), result.getData());

      for (VectorWrapper<?> vectorWrapper : loader.getContainer()) {
        final String fieldName = vectorWrapper.getField().getName();
        Object object = vectorWrapper.getValueVector().getAccessor().getObject(0);
        // VarCharVector returns Text instance, but baseline values should contain String value
        if (object instanceof Text) {
          object = object.toString();
        }
        resultingValues.put(String.format("`%s`", fieldName), object);
        for (Map.Entry<String, StringBuilder> function : functions.entrySet()) {
          function.getValue()
              .append(function.getKey())
              .append("(")
              .append(fieldName)
              .append(") ")
              .append(fieldName)
              .append(",");
        }
        columns.add(fieldName);
      }
      loader.clear();
      result.release();

      String columnsList = String.join(", ", columns);

      final List<Map<String, Object>> baselineRecords = new ArrayList<>();
      baselineRecords.add(resultingValues);

      for (StringBuilder selectBody : functions.values()) {
        selectBody.setLength(selectBody.length() - 1);

        testBuilder()
            .sqlQuery("select %s from (select %s from %s limit 1)",
                selectBody.toString(), columnsList, tableName)
            .unOrdered()
            .baselineRecords(baselineRecords)
            .go();
      }
    }
  }

  @Test
  public void testHashAggSingleValueFunction() throws Exception {
    List<String> tableNames = Arrays.asList(
        "cp.`parquet/alltypes_required.parquet`",
        "cp.`parquet/alltypes_optional.parquet`");
    for (String tableName : tableNames) {
      Map<String, Object> resultingValues = getBaselineRecords(tableName);

      List<Boolean> optionValues = Arrays.asList(true, false);

      try {
        for (Boolean optionValue : optionValues) {
          for (Map.Entry<String, Object> entry : resultingValues.entrySet()) {
            String columnName = String.format("`%s`", entry.getKey());

            // disable interval types when stream agg is disabled due to DRILL-7241
            if (optionValue || !columnName.startsWith("`col_intrvl")) {
              client.alterSession(PlannerSettings.STREAMAGG.getOptionName(), optionValue);
              testBuilder()
                  .sqlQuery("select single_value(t.%1$s) as %1$s\n" +
                      "from (select %1$s from %2$s limit 1) t group by t.%1$s", columnName, tableName)
                  .ordered()
                  .baselineRecords(Collections.singletonList(ImmutableMap.of(columnName, entry.getValue())))
                  .go();
            }
          }
        }
      } finally {
        client.resetSession(PlannerSettings.STREAMAGG.getOptionName());
      }
    }
  }

  private Map<String, Object> getBaselineRecords(String tableName) throws Exception {
    QueryDataBatch result =
        queryBuilder().sql("select * from %s limit 1", tableName).results().get(0);

    Map<String, Object> resultingValues = new HashMap<>();

    RecordBatchLoader loader = new RecordBatchLoader(cluster.allocator());
    loader.load(result.getHeader().getDef(), result.getData());

    for (VectorWrapper<?> vectorWrapper : loader.getContainer()) {
      String fieldName = vectorWrapper.getField().getName();
      Object object = vectorWrapper.getValueVector().getAccessor().getObject(0);
      // VarCharVector returns Text instance, but baseline values should contain String value
      if (object instanceof Text) {
        object = object.toString();
      }
      resultingValues.put(fieldName, object);
    }
    loader.clear();
    result.release();
    return resultingValues;
  }

  @Test
  public void testSingleValueWithComplexInput() throws Exception {
    String query = "select single_value(a) as any_a, single_value(f) as any_f, single_value(m) as any_m," +
        "single_value(p) as any_p from (select * from cp.`store/json/test_anyvalue.json` limit 1)";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("any_a", "any_f", "any_m", "any_p")
        .baselineValues(listOf(mapOf("b", 10L, "c", 15L),
            mapOf("b", 20L, "c", 45L)),
            listOf(mapOf("g", mapOf("h",
                listOf(mapOf("k", 10L), mapOf("k", 20L))))),
            listOf(mapOf("n", listOf(1L, 2L, 3L))),
            mapOf("q", listOf(27L, 28L, 29L)))
        .go();
  }

  @Test
  public void testSingleValueWithMultipleValuesInputsAllTypes() throws Exception {
    List<String> tableNames = Arrays.asList(
        "cp.`parquet/alltypes_required.parquet`",
        "cp.`parquet/alltypes_optional.parquet`");
    for (String tableName : tableNames) {
      QueryDataBatch result =
          queryBuilder().sql("select * from %s limit 1", tableName).results().get(0);

      RecordBatchLoader loader = new RecordBatchLoader(cluster.allocator());
      loader.load(result.getHeader().getDef(), result.getData());

      List<String> columns = StreamSupport.stream(loader.getContainer().spliterator(), false)
          .map(vectorWrapper -> vectorWrapper.getField().getName())
          .collect(Collectors.toList());
      loader.clear();
      result.release();
      for (String columnName : columns) {
        try {
          run("select single_value(t.%1$s) as %1$s from %2$s t", columnName, tableName);
        } catch (UserRemoteException e) {
          assertTrue("No expected current \"FUNCTION ERROR\" and/or \"Input for single_value function has more than one row\"",
              e.getMessage().matches("^FUNCTION ERROR(.|\\n)*Input for single_value function has more than one row(.|\\n)*"));
        }
      }
    }
  }

  @Test
  public void testSingleValueWithMultipleComplexInputs() throws Exception {
    thrown.expect(UserRemoteException.class);
    thrown.expectMessage(containsString("FUNCTION ERROR"));
    thrown.expectMessage(containsString("Input for single_value function has more than one row"));
    run("select single_value(t1.a) from cp.`store/json/test_anyvalue.json` t1");
  }

  /*
   * Streaming agg on top of a filter produces wrong results if the first two batches are filtered out.
   * In the below test we have three files in the input directory and since the ordering of reading
   * of these files may not be deterministic, we have three tests to make sure we test the case where
   * streaming agg gets two empty batches.
   */
  @Test
  public void drill3069() throws Exception {
    final String query = "select max(foo) col1 from dfs.`agg/bugs/drill3069` where foo = %d";
    testBuilder()
        .sqlQuery(query, 2)
        .unOrdered()
        .baselineColumns("col1")
        .baselineValues(2L)
        .go();

    testBuilder()
        .sqlQuery(query, 4)
        .unOrdered()
        .baselineColumns("col1")
        .baselineValues(4L)
        .go();

    testBuilder()
        .sqlQuery(query, 6)
        .unOrdered()
        .baselineColumns("col1")
        .baselineValues(6L)
        .go();
  }

  @Test //DRILL-2748
  @Category(UnlikelyTest.class)
  public void testPushFilterPastAgg() throws Exception {
    final String query =
        " select cnt " +
        " from (select n_regionkey, count(*) cnt from cp.`tpch/nation.parquet` group by n_regionkey) " +
        " where n_regionkey = 2 ";

    // Validate the plan
    String expectedPlan = "(?s)(StreamAgg|HashAgg).*Filter";
    String excludedPatterns = "(?s)Filter.*(StreamAgg|HashAgg)";
    queryBuilder().sql(query)
        .planMatcher()
        .include(expectedPlan)
        .exclude(excludedPatterns)
        .match();

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(5L)
        .build().run();

    // having clause
    String query2 =
        " select count(*) cnt from cp.`tpch/nation.parquet` group by n_regionkey " +
        " having n_regionkey = 2 ";
    queryBuilder().sql(query2)
        .planMatcher()
        .include(expectedPlan)
        .exclude(excludedPatterns)
        .match();

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(5L)
        .build().run();
  }

  @Test
  public void testPushFilterInExprPastAgg() throws Exception {
    final String query =
        " select cnt " +
            " from (select n_regionkey, count(*) cnt from cp.`tpch/nation.parquet` group by n_regionkey) " +
            " where n_regionkey + 100 - 100 = 2 ";

    // Validate the plan
    String expectedPlan = "(?s)(StreamAgg|HashAgg).*Filter";
    String excludedPatterns = "(?s)Filter.*(StreamAgg|HashAgg)";
    queryBuilder().sql(query)
        .planMatcher()
        .include(expectedPlan)
        .exclude(excludedPatterns)
        .match();

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(5L)
        .build().run();
  }

  @Test
  public void testNegPushFilterInExprPastAgg() throws Exception {
    // negative case: should not push filter, since it involves the aggregate result
    String query =
        " select cnt " +
            " from (select n_regionkey, count(*) cnt from cp.`tpch/nation.parquet` group by n_regionkey) " +
            " where cnt + 100 - 100 = 5 ";

    // Validate the plan
    String expectedPlan = "(?s)Filter(?!StreamAgg|!HashAgg)";
    String excludedPatterns = "(?s)(StreamAgg|HashAgg).*Filter";
    queryBuilder().sql(query)
        .planMatcher()
        .include(expectedPlan)
        .exclude(excludedPatterns)
        .match();

    // negative case: should not push filter, since it is expression of group key + agg result.
    String query2 =
        " select cnt " +
            " from (select n_regionkey, count(*) cnt from cp.`tpch/nation.parquet` group by n_regionkey) " +
            " where cnt + n_regionkey = 5 ";
    queryBuilder().sql(query2)
        .planMatcher()
        .include(expectedPlan)
        .exclude(excludedPatterns)
        .match();
  }

  @Test // DRILL-3781
  @Category(UnlikelyTest.class)
  // GROUP BY System functions in schema table.
  public void testGroupBySystemFuncSchemaTable() throws Exception {
    String query = "select count(*) as cnt from sys.version group by CURRENT_DATE";
    String expectedPlan = "(?s)(StreamAgg|HashAgg)";

    queryBuilder().sql(query)
        .planMatcher()
        .include(expectedPlan)
        .match();
  }

  @Test //DRILL-3781
  @Category(UnlikelyTest.class)
  // GROUP BY System functions in csv, parquet, json table.
  public void testGroupBySystemFuncFileSystemTable() throws Exception {
    testBuilder()
        .sqlQuery("select count(*) as cnt from cp.`nation/nation.tbl` group by CURRENT_DATE")
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(25L)
        .build().run();

    testBuilder()
        .sqlQuery("select count(*) as cnt from cp.`tpch/nation.parquet` group by CURRENT_DATE")
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(25L)
        .build().run();

    testBuilder()
        .sqlQuery("select count(*) as cnt from cp.`employee.json` group by CURRENT_DATE")
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(1155L)
        .build().run();
  }

  @Test
  public void test4443() throws Exception {
    run("SELECT MIN(columns[1]) FROM cp.`agg/4443.csv` GROUP BY columns[0]");
  }

  @Test
  public void testCountStarRequired() throws Exception {
    final String query = "select count(*) as col from cp.`tpch/region.parquet`";
    List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList();
    TypeProtos.MajorType majorType = TypeProtos.MajorType.newBuilder()
        .setMinorType(TypeProtos.MinorType.BIGINT)
        .setMode(TypeProtos.DataMode.REQUIRED)
        .build();
    expectedSchema.add(Pair.of(SchemaPath.getSimplePath("col"), majorType));

    testBuilder()
        .sqlQuery(query)
        .schemaBaseLine(expectedSchema)
        .build()
        .run();

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(5L)
        .build()
        .run();
  }


  @Test // DRILL-4531
  @Category(UnlikelyTest.class)
  public void testPushFilterDown() throws Exception {
    final String sql =
        "SELECT  cust.custAddress, \n"
            + "       lineitem.provider \n"
            + "FROM ( \n"
            + "      SELECT cast(c_custkey AS bigint) AS custkey, \n"
            + "             c_address                 AS custAddress \n"
            + "      FROM   cp.`tpch/customer.parquet` ) cust \n"
            + "LEFT JOIN \n"
            + "  ( \n"
            + "    SELECT DISTINCT l_linenumber, \n"
            + "           CASE \n"
            + "             WHEN l_partkey IN (1, 2) THEN 'Store1'\n"
            + "             WHEN l_partkey IN (5, 6) THEN 'Store2'\n"
            + "           END AS provider \n"
            + "    FROM  cp.`tpch/lineitem.parquet` \n"
            + "    WHERE ( l_orderkey >=20160101 AND l_partkey <=20160301) \n"
            + "      AND   l_partkey IN (1,2, 5, 6) ) lineitem\n"
            + "ON        cust.custkey = lineitem.l_linenumber \n"
            + "WHERE     provider IS NOT NULL \n"
            + "GROUP BY  cust.custAddress, \n"
            + "          lineitem.provider \n"
            + "ORDER BY  cust.custAddress, \n"
            + "          lineitem.provider";

    // Validate the plan
    String expectedPlan = "(?s)(Join).*inner"; // With filter pushdown, left join will be converted into inner join
    String excludedPatterns = "(?s)(Join).*(left)";
    queryBuilder().sql(sql)
        .planMatcher()
        .include(expectedPlan)
        .exclude(excludedPatterns)
        .match();
  }

  @Test // DRILL-2385: count on complex objects failed with missing function implementation
  @Category(UnlikelyTest.class)
  public void testCountComplexObjects() throws Exception {
    final String query = "select count(t.%s) %s from cp.`complex/json/complex.json` t";
    Map<String, String> objectsMap = Maps.newHashMap();
    objectsMap.put("COUNT_BIG_INT_REPEATED", "sia");
    objectsMap.put("COUNT_FLOAT_REPEATED", "sfa");
    objectsMap.put("COUNT_MAP_REPEATED", "soa");
    objectsMap.put("COUNT_MAP_REQUIRED", "oooi");
    objectsMap.put("COUNT_LIST_REPEATED", "odd");
    objectsMap.put("COUNT_LIST_OPTIONAL", "sia");

    for (String object: objectsMap.keySet()) {
      String optionSetting = "";
      if (object.equals("COUNT_LIST_OPTIONAL")) {
        // if `exec.enable_union_type` parameter is true then BIGINT<REPEATED> object is converted to LIST<OPTIONAL> one
        optionSetting = "alter session set `exec.enable_union_type`=true";
      }
      try {
        testBuilder()
            .sqlQuery(query, objectsMap.get(object), object)
            .optionSettingQueriesForTestQuery(optionSetting)
            .unOrdered()
            .baselineColumns(object)
            .baselineValues(3L)
            .go();
      } finally {
        client.resetSession(ExecConstants.ENABLE_UNION_TYPE_KEY);
      }
    }
  }

  @Test // DRILL-4264
  @Category(UnlikelyTest.class)
  public void testCountOnFieldWithDots() throws Exception {
    String fileName = "table.json";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      writer.write("{\"rk.q\": \"a\", \"m\": {\"a.b\":\"1\", \"a\":{\"b\":\"2\"}, \"c\":\"3\"}}");
    }

    testBuilder()
      .sqlQuery("select count(t.m.`a.b`) as a,\n" +
        "count(t.m.a.b) as b,\n" +
        "count(t.m['a.b']) as c,\n" +
        "count(t.rk.q) as d,\n" +
        "count(t.`rk.q`) as e\n" +
        "from dfs.`%s` t", fileName)
      .unOrdered()
      .baselineColumns("a", "b", "c", "d", "e")
      .baselineValues(1L, 1L, 1L, 0L, 1L)
      .go();
  }

  @Test // DRILL-5768
  public void testGroupByWithoutAggregate() throws Exception {
    try {
      run("select * from cp.`tpch/nation.parquet` group by n_regionkey");
      fail("Exception was not thrown");
    } catch (UserRemoteException e) {
      assertTrue("No expected current \"Expression 'tpch/nation.parquet.**' is not being grouped\"",
          e.getMessage().matches(".*Expression 'tpch/nation\\.parquet\\.\\*\\*' is not being grouped(.*\\n*.*)"));
    }
  }

  @Test
  public void testCollectListStreamAgg() throws Exception {
    try {
      client.alterSession(PlannerSettings.HASHAGG.getOptionName(), false);
      testBuilder()
          .sqlQuery("select collect_list('n_nationkey', n_nationkey, " +
              "'n_name', n_name, 'n_regionkey', n_regionkey, 'n_comment', n_comment) as l " +
              "from (select * from cp.`tpch/nation.parquet` limit 2)")
          .unOrdered()
          .baselineColumns("l")
          .baselineValues(listOf(
              mapOf("n_nationkey", 0, "n_name", "ALGERIA",
                  "n_regionkey", 0, "n_comment", " haggle. carefully final deposits detect slyly agai"),
              mapOf("n_nationkey", 1, "n_name", "ARGENTINA", "n_regionkey", 1,
                  "n_comment", "al foxes promise slyly according to the regular accounts. bold requests alon")))
          .go();
    } finally {
      client.resetSession(PlannerSettings.HASHAGG.getOptionName());
    }
  }

  @Test
  public void testCollectListHashAgg() throws Exception {
    try {
      client.alterSession(PlannerSettings.STREAMAGG.getOptionName(), false);
      testBuilder()
          .sqlQuery("select collect_list('n_nationkey', n_nationkey, " +
              "'n_name', n_name, 'n_regionkey', n_regionkey, 'n_comment', n_comment) as l " +
              "from (select * from cp.`tpch/nation.parquet` limit 2) group by 'a'")
          .unOrdered()
          .baselineColumns("l")
          .baselineValues(listOf(
              mapOf("n_nationkey", 0, "n_name", "ALGERIA",
                  "n_regionkey", 0, "n_comment", " haggle. carefully final deposits detect slyly agai"),
              mapOf("n_nationkey", 1, "n_name", "ARGENTINA", "n_regionkey", 1,
                  "n_comment", "al foxes promise slyly according to the regular accounts. bold requests alon")))
          .go();
    } finally {
      client.resetSession(PlannerSettings.STREAMAGG.getOptionName());
    }
  }

  @Test
  public void testCollectToListVarcharStreamAgg() throws Exception {
    try {
      client.alterSession(PlannerSettings.HASHAGG.getOptionName(), false);
      testBuilder()
          .sqlQuery("select collect_to_list_varchar(`date`) as l from " +
              "(select * from cp.`store/json/clicks.json` limit 2)")
          .unOrdered()
          .baselineColumns("l")
          .baselineValues(listOf("2014-04-26", "2014-04-20"))
          .go();
    } finally {
      client.resetSession(PlannerSettings.HASHAGG.getOptionName());
    }
  }

  @Test
  public void testCollectToListVarcharHashAgg() throws Exception {
    try {
      client.alterSession(PlannerSettings.STREAMAGG.getOptionName(), false);
      testBuilder()
          .sqlQuery("select collect_to_list_varchar(`date`) as l from " +
              "(select * from cp.`store/json/clicks.json` limit 2) group by 'a'")
          .unOrdered()
          .baselineColumns("l")
          .baselineValues(listOf("2014-04-26", "2014-04-20"))
          .go();
    } finally {
      client.resetSession(PlannerSettings.STREAMAGG.getOptionName());
    }
  }

  @Test
  public void testSchemaFunctionStreamAgg() throws Exception {
    try {
      client.alterSession(PlannerSettings.HASHAGG.getOptionName(), false);
      TupleMetadata schema = new SchemaBuilder()
          .add("n_nationkey", TypeProtos.MinorType.INT)
          .add("n_name", TypeProtos.MinorType.VARCHAR)
          .add("n_regionkey", TypeProtos.MinorType.INT)
          .add("n_comment", TypeProtos.MinorType.VARCHAR)
          .build();

      testBuilder()
          .sqlQuery("select schema('n_nationkey', n_nationkey, " +
              "'n_name', n_name, 'n_regionkey', n_regionkey, 'n_comment', n_comment) as l from " +
              "(select * from cp.`tpch/nation.parquet` limit 2)")
          .unOrdered()
          .baselineColumns("l")
          .baselineValues(schema.jsonString())
          .go();
    } finally {
      client.resetSession(PlannerSettings.HASHAGG.getOptionName());
    }
  }

  @Test
  public void testSchemaFunctionHashAgg() throws Exception {
    try {
      client.alterSession(PlannerSettings.STREAMAGG.getOptionName(), false);
      TupleMetadata schema = new SchemaBuilder()
          .add("n_nationkey", TypeProtos.MinorType.INT)
          .add("n_name", TypeProtos.MinorType.VARCHAR)
          .add("n_regionkey", TypeProtos.MinorType.INT)
          .add("n_comment", TypeProtos.MinorType.VARCHAR)
          .build();

      testBuilder()
          .sqlQuery("select schema('n_nationkey', n_nationkey, " +
              "'n_name', n_name, 'n_regionkey', n_regionkey, 'n_comment', n_comment) as l from " +
              "(select * from cp.`tpch/nation.parquet` limit 2) group by 'a'")
          .unOrdered()
          .baselineColumns("l")
          .baselineValues(schema.jsonString())
          .go();
    } finally {
      client.resetSession(PlannerSettings.STREAMAGG.getOptionName());
    }
  }

  @Test
  public void testMergeSchemaFunctionStreamAgg() throws Exception {
    try {
      client.alterSession(PlannerSettings.HASHAGG.getOptionName(), false);
      String schema = new SchemaBuilder()
          .add("n_nationkey", TypeProtos.MinorType.INT)
          .add("n_name", TypeProtos.MinorType.VARCHAR)
          .add("n_regionkey", TypeProtos.MinorType.INT)
          .add("n_comment", TypeProtos.MinorType.VARCHAR)
          .build()
          .jsonString();

      testBuilder()
          .sqlQuery("select merge_schema('%s') as l from " +
              "(select * from cp.`tpch/nation.parquet` limit 2)", schema)
          .unOrdered()
          .baselineColumns("l")
          .baselineValues(schema)
          .go();
    } finally {
      client.resetSession(PlannerSettings.HASHAGG.getOptionName());
    }
  }

  @Test
  public void testMergeSchemaFunctionHashAgg() throws Exception {
    try {
      client.alterSession(PlannerSettings.STREAMAGG.getOptionName(), false);
      String schema = new SchemaBuilder()
          .add("n_nationkey", TypeProtos.MinorType.INT)
          .add("n_name", TypeProtos.MinorType.VARCHAR)
          .add("n_regionkey", TypeProtos.MinorType.INT)
          .add("n_comment", TypeProtos.MinorType.VARCHAR)
          .build()
          .jsonString();

      testBuilder()
          .sqlQuery("select merge_schema('%s') as l from " +
              "(select * from cp.`tpch/nation.parquet` limit 2) group by 'a'", schema)
          .unOrdered()
          .baselineColumns("l")
          .baselineValues(schema)
          .go();
    } finally {
      client.resetSession(PlannerSettings.STREAMAGG.getOptionName());
    }
  }

  @Test
  public void testInjectVariablesHashAgg() throws Exception {
    try {
      client.alterSession(PlannerSettings.STREAMAGG.getOptionName(), false);
      testBuilder()
          .sqlQuery("select tdigest(p.col_int) from " +
              "cp.`parquet/alltypes_required.parquet` p group by p.col_flt")
          .unOrdered()
          .expectsNumRecords(4)
          .go();
    } finally {
      client.resetSession(PlannerSettings.STREAMAGG.getOptionName());
    }
  }

  @Test //DRILL-7931
  public void testRowTypeMissMatch() throws Exception {
    testBuilder()
      .sqlQuery("select col1, stddev(col2) as g1, SUM(col2) as g2 FROM " +
        "(values ('UA', 3), ('USA', 2), ('UA', 3), ('USA', 5), ('USA', 1), " +
        "('UA', 9)) t(col1, col2) GROUP BY col1 order by col1")
      .unOrdered()
      .approximateEquality(0.000001)
      .baselineColumns("col1", "g1", "g2")
      .baselineValues("UA", 3.4641016151377544, 15L)
      .baselineValues("USA", 2.0816659994661326, 8L)
      .go();
  }
}
