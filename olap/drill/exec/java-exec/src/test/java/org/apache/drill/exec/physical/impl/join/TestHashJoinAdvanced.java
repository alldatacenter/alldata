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
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.test.BaseTestQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

@Category(OperatorTest.class)
public class TestHashJoinAdvanced extends JoinTestBase {

  // Have to disable merge join, if this testcase is to test "HASH-JOIN".
  @BeforeClass
  public static void disableMergeJoin() throws Exception {
    dirTestWatcher.copyResourceToRoot(Paths.get("join", "empty_part"));
    dirTestWatcher.copyFileToRoot(Paths.get("sample-data", "region.parquet"));
    dirTestWatcher.copyFileToRoot(Paths.get("sample-data", "nation.parquet"));
    test(DISABLE_MJ);
  }

  @AfterClass
  public static void enableMergeJoin() throws Exception {
    test(ENABLE_MJ);
  }

  @Test //DRILL-2197 Left Self Join with complex type in projection
  @Category(UnlikelyTest.class)
  public void testLeftSelfHashJoinWithMap() throws Exception {
    final String query = " select a.id, b.oooi.oa.oab.oabc oabc, b.ooof.oa.oab oab from cp.`join/complex_1.json` a left outer join cp.`join/complex_1.json` b on a.id=b.id order by a.id";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .jsonBaselineFile("join/DRILL-2197-result-1.json")
      .build()
      .run();
  }

  @Test //DRILL-2197 Left Join with complex type in projection
  @Category(UnlikelyTest.class)
  public void testLeftHashJoinWithMap() throws Exception {
    final String query = " select a.id, b.oooi.oa.oab.oabc oabc, b.ooof.oa.oab oab from cp.`join/complex_1.json` a left outer join cp.`join/complex_2.json` b on a.id=b.id order by a.id";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .jsonBaselineFile("join/DRILL-2197-result-2.json")
      .build()
      .run();
  }

  @Test
  public void testFOJWithRequiredTypes() throws Exception {
    String query = "select t1.varchar_col from " +
        "cp.`parquet/drill-2707_required_types.parquet` t1 full outer join cp.`parquet/alltypes.json` t2 " +
        "on t1.int_col = t2.INT_col order by t1.varchar_col limit 1";

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("varchar_col")
        .baselineValues("doob")
        .go();
  }

  @Test  // DRILL-2771, similar problem as DRILL-2197 except problem reproduces with right outer join instead of left
  @Category(UnlikelyTest.class)
  public void testRightJoinWithMap() throws Exception {
    final String query = " select a.id, b.oooi.oa.oab.oabc oabc, b.ooof.oa.oab oab from " +
        "cp.`join/complex_1.json` b right outer join cp.`join/complex_1.json` a on a.id = b.id order by a.id";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .jsonBaselineFile("join/DRILL-2197-result-1.json")
        .build()
        .run();
  }

  @Test
  public void testJoinWithDifferentTypesInCondition() throws Exception {
    String query = "select t1.full_name from cp.`employee.json` t1, cp.`department.json` t2 " +
        "where cast(t1.department_id as double) = t2.department_id and t1.employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .optionSettingQueriesForTestQuery(ENABLE_HJ)
        .unOrdered()
        .baselineColumns("full_name")
        .baselineValues("Sheri Nowmer")
        .go();


    query = "select t1.bigint_col from cp.`jsoninput/implicit_cast_join_1.json` t1, cp.`jsoninput/implicit_cast_join_1.json` t2 " +
        " where t1.bigint_col = cast(t2.bigint_col as int) and" + // join condition with bigint and int
        " t1.double_col  = cast(t2.double_col as float) and" + // join condition with double and float
        " t1.bigint_col = cast(t2.bigint_col as double)"; // join condition with bigint and double

    testBuilder()
        .sqlQuery(query)
        .optionSettingQueriesForTestQuery(ENABLE_HJ)
        .unOrdered()
        .baselineColumns("bigint_col")
        .baselineValues(1L)
        .go();

    query = "select count(*) col1 from " +
        "(select t1.date_opt from cp.`parquet/date_dictionary.parquet` t1, cp.`parquet/timestamp_table.parquet` t2 " +
        "where t1.date_opt = t2.timestamp_col)"; // join condition contains date and timestamp

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1")
        .baselineValues(4L)
        .go();
  }

  @Test //DRILL-2197 Left Join with complex type in projection
  @Category(UnlikelyTest.class)
  public void testJoinWithMapAndDotField() throws Exception {
    String fileName = "table.json";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      writer.write("{\"rk.q\": \"a\", \"m\": {\"a.b\":\"1\", \"a\":{\"b\":\"2\"}, \"c\":\"3\"}}");
    }

    testBuilder()
      .sqlQuery("select t1.m.`a.b` as a,\n" +
        "t2.m.a.b as b,\n" +
        "t1.m['a.b'] as c,\n" +
        "t2.rk.q as d,\n" +
        "t1.`rk.q` as e\n" +
        "from dfs.`%1$s` t1,\n" +
        "dfs.`%1$s` t2\n" +
        "where t1.m.`a.b`=t2.m.`a.b` and t1.m.a.b=t2.m.a.b", fileName)
      .unOrdered()
      .baselineColumns("a", "b", "c", "d", "e")
      .baselineValues("1", "2", "1", null, "a")
      .go();
  }

  @Test
  public void testHashLeftJoinWithEmptyTable() throws Exception {
    testJoinWithEmptyFile(dirTestWatcher.getRootDir(), "left outer", new String[] {HJ_PATTERN, LEFT_JOIN_TYPE}, 1155L);
  }

  @Test
  public void testHashInnerJoinWithEmptyTable() throws Exception {
    testJoinWithEmptyFile(dirTestWatcher.getRootDir(), "inner", new String[] {HJ_PATTERN, INNER_JOIN_TYPE}, 0L);
  }

  @Test
  public void testHashRightJoinWithEmptyTable() throws Exception {
    testJoinWithEmptyFile(dirTestWatcher.getRootDir(), "right outer", new String[] {HJ_PATTERN, RIGHT_JOIN_TYPE}, 0L);
  }

  @Test // Test for DRILL-6137 fix
  public void emptyPartTest() throws Exception {
    BaseTestQuery.setSessionOption(ExecConstants.SLICE_TARGET, 1L);

    try {
      testBuilder().sqlQuery("select t.p_partkey, t1.ps_suppkey from " +
        "dfs.`join/empty_part/part` as t RIGHT JOIN dfs.`join/empty_part/partsupp` as t1 ON t.p_partkey = t1.ps_partkey where t1.ps_partkey > 1").unOrdered()
        .baselineColumns("ps_suppkey", "p_partkey")
        .baselineValues(3L, 2L)
        .baselineValues(2503L, 2L)
        .baselineValues(5003L, 2L)
        .baselineValues(7503L, 2L)
        .go();
    } finally {
      BaseTestQuery.resetSessionOption(ExecConstants.SLICE_TARGET);
    }
  }

  @Test // DRILL-6089
  public void testJoinOrdering() throws Exception {
    final String query = "select * from dfs.`sample-data/nation.parquet` nation left outer join " +
      "(select * from dfs.`sample-data/region.parquet`) " +
      "as region on region.r_regionkey = nation.n_nationkey order by nation.n_name desc";

    final Pattern sortHashJoinPattern = Pattern.compile(".*Sort.*HashJoin", Pattern.DOTALL);
    testPlanMatchingPatterns(query, new Pattern[]{sortHashJoinPattern}, null);
  }

  @Test // DRILL-6606
  public void testJoinLimit0Schema() throws Exception {
    String query = "SELECT l.l_quantity, l.l_shipdate, o.o_custkey\n" +
      "FROM (SELECT * FROM cp.`tpch/lineitem.parquet` LIMIT 0) l\n" +
      "    JOIN (SELECT * FROM cp.`tpch/orders.parquet` LIMIT 0) o \n" +
      "    ON l.l_orderkey = o.o_orderkey\n";
    final List<QueryDataBatch> dataBatches = client.runQuery(UserBitShared.QueryType.SQL, query);

    Assert.assertEquals(1, dataBatches.size());

    final QueryDataBatch queryDataBatch = dataBatches.get(0);
    final RecordBatchLoader batchLoader = new RecordBatchLoader(getAllocator());

    try {
      batchLoader.load(queryDataBatch.getHeader().getDef(), queryDataBatch.getData());

      final BatchSchema actualSchema = batchLoader.getSchema();
      SchemaBuilder schemaBuilder = new SchemaBuilder()
        .add("l_quantity", TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.REQUIRED)
        .add("l_shipdate", TypeProtos.MinorType.DATE, TypeProtos.DataMode.REQUIRED)
        .add("o_custkey", TypeProtos.MinorType.INT, TypeProtos.DataMode.REQUIRED);
      final BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

      Assert.assertTrue(expectedSchema.isEquivalent(actualSchema));
    } finally {
      batchLoader.clear();
    }
  }
}
