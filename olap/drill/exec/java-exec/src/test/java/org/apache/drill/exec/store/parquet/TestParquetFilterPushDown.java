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
import org.apache.drill.PlanTestBase;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.expr.stat.RowsMatch;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.store.parquet.columnreaders.ParquetRecordReader;
import org.apache.drill.exec.store.parquet.metadata.Metadata;
import org.apache.drill.exec.store.parquet.metadata.MetadataBase;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.exec.expr.IsPredicate;
import org.apache.drill.exec.expr.StatisticsProvider;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ProfileParser;
import org.apache.drill.test.QueryBuilder;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestParquetFilterPushDown extends PlanTestBase {
  private static final String CTAS_TABLE = "order_ctas";
  private static FragmentContextImpl fragContext;

  private static FileSystem fs;

  @BeforeClass
  public static void initFSAndCreateFragContext() throws Exception {
    fs = getLocalFileSystem();
    fragContext = new FragmentContextImpl(bits[0].getContext(),
      BitControl.PlanFragment.getDefaultInstance(), null, bits[0].getContext().getFunctionImplementationRegistry());

    dirTestWatcher.copyResourceToRoot(Paths.get("parquetFilterPush"));
    dirTestWatcher.copyResourceToRoot(Paths.get("parquet", "multirowgroup.parquet"));
    dirTestWatcher.copyResourceToRoot(Paths.get("parquet", "multirowgroup2.parquet"));
    dirTestWatcher.copyResourceToRoot(Paths.get("parquet", "multirowgroupwithNulls.parquet"));
  }

  @AfterClass
  public static void teardown() throws IOException {
    fragContext.close();
    fs.close();
  }

  @Rule
  public final TestWatcher ctasWatcher = new TestWatcher() {
    @Override
    protected void failed(Throwable e, Description description) {
      deleteCtasTable();
    }

    @Override
    protected void starting(Description description) {
      deleteCtasTable();
    }

    @Override
    protected void finished(Description description) {
      deleteCtasTable();
    }

    private void deleteCtasTable() {
      FileUtils.deleteQuietly(new File(dirTestWatcher.getDfsTestTmpDir(), CTAS_TABLE));
    }
  };

  @Test
  // Test filter evaluation directly without going through SQL queries.
  public void testIntPredicateWithEval() throws Exception {
    // intTbl.parquet has only one int column
    //    intCol : [0, 100].
    final File file = dirTestWatcher.getRootDir()
      .toPath()
      .resolve(Paths.get("parquetFilterPush", "intTbl", "intTbl.parquet"))
      .toFile();
    MetadataBase.ParquetTableMetadataBase footer = getParquetMetaData(file);

    testParquetRowGroupFilterEval(footer, "intCol = 100", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol = 0", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol = 50", RowsMatch.SOME);

    testParquetRowGroupFilterEval(footer, "intCol = -1", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol = 101", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "intCol > 100", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol > 99", RowsMatch.SOME);

    testParquetRowGroupFilterEval(footer, "intCol >= 100", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol >= 101", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "intCol < 100", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol < 1", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol < 0", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "intCol <= 100", RowsMatch.ALL);
    testParquetRowGroupFilterEval(footer, "intCol <= 1", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol <= 0", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol <= -1", RowsMatch.NONE);

    // "and"
    testParquetRowGroupFilterEval(footer, "intCol > 100 and intCol < 200", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol > 50 and intCol < 200", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol > 50 and intCol > 200", RowsMatch.NONE); // essentially, intCol > 200

    // "or"
    testParquetRowGroupFilterEval(footer, "intCol = 150 or intCol = 160", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol = 50 or intCol = 160", RowsMatch.SOME);

    //"nonExistCol" does not exist in the table. "AND" with a filter on exist column
    testParquetRowGroupFilterEval(footer, "intCol > 100 and nonExistCol = 100", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol > 50 and nonExistCol = 100", RowsMatch.NONE); // since nonExistCol = 100 -> Unknown -> could drop.
    testParquetRowGroupFilterEval(footer, "nonExistCol = 100 and intCol > 50", RowsMatch.NONE); // since nonExistCol = 100 -> Unknown -> could drop.
    testParquetRowGroupFilterEval(footer, "intCol > 100 and nonExistCol < 'abc'", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "nonExistCol < 'abc' and intCol > 100", RowsMatch.NONE); // nonExistCol < 'abc' hit NumberException and is ignored, but intCol >100 will

    // say "drop".
    testParquetRowGroupFilterEval(footer, "intCol > 50 and nonExistCol < 'abc'", RowsMatch.SOME); // because nonExistCol < 'abc' hit NumberException and
    // is ignored.

    //"nonExistCol" does not exist in the table. "OR" with a filter on exist column
    testParquetRowGroupFilterEval(footer, "intCol > 100 or nonExistCol = 100", RowsMatch.NONE); // nonExistCol = 100 -> could drop.
    testParquetRowGroupFilterEval(footer, "nonExistCol = 100 or intCol > 100", RowsMatch.NONE); // nonExistCol = 100 -> could drop.

    testParquetRowGroupFilterEval(footer, "intCol > 50 or nonExistCol < 100", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "nonExistCol < 100 or intCol > 50", RowsMatch.SOME);

    // cast function on column side (LHS)
    testParquetRowGroupFilterEval(footer, "cast(intCol as bigint) = 100", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "cast(intCol as bigint) = 0", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "cast(intCol as bigint) = 50", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "cast(intCol as bigint) = 101", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "cast(intCol as bigint) = -1", RowsMatch.NONE);

    // cast function on constant side (RHS)
    testParquetRowGroupFilterEval(footer, "intCol = cast(100 as bigint)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol = cast(0 as bigint)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol = cast(50 as bigint)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "intCol = cast(101 as bigint)", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol = cast(-1 as bigint)", RowsMatch.NONE);

    // cast into float4/float8
    testParquetRowGroupFilterEval(footer, "cast(intCol as float4) = cast(101.0 as float4)", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "cast(intCol as float4) = cast(-1.0 as float4)", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "cast(intCol as float4) = cast(1.0 as float4)", RowsMatch.SOME);

    testParquetRowGroupFilterEval(footer, "cast(intCol as float8) = 101.0", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "cast(intCol as float8) = -1.0", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "cast(intCol as float8) = 1.0", RowsMatch.SOME);
  }

  @Test
  public void testIntPredicateAgainstAllNullColWithEval() throws Exception {
    // intAllNull.parquet has only one int column with all values being NULL.
    // column values statistics: num_nulls: 25, min/max is not defined
    final File file = dirTestWatcher.getRootDir()
      .toPath()
      .resolve(Paths.get("parquetFilterPush", "intTbl", "intAllNull.parquet"))
      .toFile();
    MetadataBase.ParquetTableMetadataBase footer = getParquetMetaData(file);

    testParquetRowGroupFilterEval(footer, "intCol = 100", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol = 0", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol = -100", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "intCol > 10", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol >= 10", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "intCol < 10", RowsMatch.NONE);
    testParquetRowGroupFilterEval(footer, "intCol <= 10", RowsMatch.NONE);
  }

  @Test
  public void testDatePredicateAgainstDrillCTAS1_8WithEval() throws Exception {
    // The parquet file is created on drill 1.8.0 with DRILL CTAS:
    //   create table dfs.tmp.`dateTblCorrupted/t1` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-01' and date '1992-01-03';

    final File file = dirTestWatcher.getRootDir()
      .toPath()
      .resolve(Paths.get("parquetFilterPush", "dateTblCorrupted", "t1", "0_0_0.parquet"))
      .toFile();
    MetadataBase.ParquetTableMetadataBase footer = getParquetMetaData(file);

    testDatePredicateAgainstDrillCTASHelper(footer);
  }

  @Test
  public void testDatePredicateAgainstDrillCTASPost1_8WithEval() throws Exception {
    // The parquet file is created on drill 1.9.0-SNAPSHOT (commit id:03e8f9f3e01c56a9411bb4333e4851c92db6e410) with DRILL CTAS:
    //   create table dfs.tmp.`dateTbl1_9/t1` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-01' and date '1992-01-03';

    final File file = dirTestWatcher.getRootDir()
      .toPath()
      .resolve(Paths.get("parquetFilterPush", "dateTbl1_9", "t1", "0_0_0.parquet"))
      .toFile();
    MetadataBase.ParquetTableMetadataBase footer = getParquetMetaData(file);

    testDatePredicateAgainstDrillCTASHelper(footer);
  }

  private void testDatePredicateAgainstDrillCTASHelper(MetadataBase.ParquetTableMetadataBase footer) {
    testParquetRowGroupFilterEval(footer, "o_orderdate = cast('1992-01-01' as date)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_orderdate = cast('1991-12-31' as date)", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "o_orderdate >= cast('1991-12-31' as date)", RowsMatch.ALL);
    testParquetRowGroupFilterEval(footer, "o_orderdate >= cast('1992-01-03' as date)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_orderdate >= cast('1992-01-04' as date)", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "o_orderdate > cast('1992-01-01' as date)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_orderdate > cast('1992-01-03' as date)", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "o_orderdate <= cast('1992-01-01' as date)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_orderdate <= cast('1991-12-31' as date)", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "o_orderdate < cast('1992-01-02' as date)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_orderdate < cast('1992-01-01' as date)", RowsMatch.NONE);
  }

  @Test
  public void testTimeStampPredicateWithEval() throws Exception {
    // Table dateTblCorrupted is created by CTAS in drill 1.8.0.
    //    create table dfs.tmp.`tsTbl/t1` as select DATE_ADD(cast(o_orderdate as date), INTERVAL '0 10:20:30' DAY TO SECOND) as o_ordertimestamp from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-01' and date '1992-01-03';
    final File file = dirTestWatcher.getRootDir()
      .toPath()
      .resolve(Paths.get("parquetFilterPush", "tsTbl", "t1", "0_0_0.parquet"))
      .toFile();
    MetadataBase.ParquetTableMetadataBase footer = getParquetMetaData(file);

    testParquetRowGroupFilterEval(footer, "o_ordertimestamp = cast('1992-01-01 10:20:30' as timestamp)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_ordertimestamp = cast('1992-01-01 10:20:29' as timestamp)", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "o_ordertimestamp >= cast('1992-01-01 10:20:29' as timestamp)", RowsMatch.ALL);
    testParquetRowGroupFilterEval(footer, "o_ordertimestamp >= cast('1992-01-03 10:20:30' as timestamp)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_ordertimestamp >= cast('1992-01-03 10:20:31' as timestamp)", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "o_ordertimestamp > cast('1992-01-03 10:20:29' as timestamp)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_ordertimestamp > cast('1992-01-03 10:20:30' as timestamp)", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "o_ordertimestamp <= cast('1992-01-01 10:20:30' as timestamp)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_ordertimestamp <= cast('1992-01-01 10:20:29' as timestamp)", RowsMatch.NONE);

    testParquetRowGroupFilterEval(footer, "o_ordertimestamp < cast('1992-01-01 10:20:31' as timestamp)", RowsMatch.SOME);
    testParquetRowGroupFilterEval(footer, "o_ordertimestamp < cast('1992-01-01 10:20:30' as timestamp)", RowsMatch.NONE);

  }

  @Test
  public void testFilterPruning() throws Exception {
    // multirowgroup2 is a parquet file with 3 rowgroups inside. One with a=0, another with a=1 and a=2, and the last with a=3 and a=4;
    // FilterPushDown should be able to prune the filter from the scan operator according to the rowgroup statistics.
    final String sql = "select * from dfs.`parquet/multirowgroup2.parquet` where ";
    PlanTestBase.testPlanMatchingPatterns(sql + "a > 1", new String[]{"numRowGroups=2"}); //No filter pruning
    PlanTestBase.testPlanMatchingPatterns(sql + "a > 2", new String[]{"numRowGroups=1"}, new String[]{"Filter\\("}); // Filter pruning

    PlanTestBase.testPlanMatchingPatterns(sql + "a < 2", new String[]{"numRowGroups=2"}); // No filter pruning
    PlanTestBase.testPlanMatchingPatterns(sql + "a < 1", new String[]{"numRowGroups=1"}, new String[]{"Filter\\("}); // Filter pruning

    PlanTestBase.testPlanMatchingPatterns(sql + "a >= 2", new String[]{"numRowGroups=2"}); // No filter pruning
    PlanTestBase.testPlanMatchingPatterns(sql + "a >= 1", new String[]{"numRowGroups=2"}, new String[]{"Filter\\("}); // Filter pruning

    PlanTestBase.testPlanMatchingPatterns(sql + "a <= 1", new String[]{"numRowGroups=2"}); // No filter pruning
    PlanTestBase.testPlanMatchingPatterns(sql + "a <= 2", new String[]{"numRowGroups=2"}, new String[]{"Filter\\("}); // Filter pruning

    PlanTestBase.testPlanMatchingPatterns(sql + "a > 0 and a < 2", new String[]{"numRowGroups=1"}); // No filter pruning
    PlanTestBase.testPlanMatchingPatterns(sql + "a > 0 and a < 3", new String[]{"numRowGroups=1"}, new String[]{"Filter\\("}); //Filter pruning

    PlanTestBase.testPlanMatchingPatterns(sql + "a < 1 or a > 1", new String[]{"numRowGroups=3"}); // No filter pruning
    PlanTestBase.testPlanMatchingPatterns(sql + "a < 1 or a > 2", new String[]{"numRowGroups=2"}, new String[]{"Filter\\("}); //Filter pruning

    // Partial filter pruning
    testParquetFilterPruning(sql + "a >=1 and cast(a as varchar) like '%3%'", 1, 2, new String[]{">\\($1, 1\\)"});
    testParquetFilterPruning(sql + "a >=1 and a/3>=1", 2, 2, new String[]{">\\($1, 1\\)"});
  }

  @Test
  public void testFilterPruningWithNulls() throws Exception {
    // multirowgroupwithNulls is a parquet file with 4 rowgroups inside and some groups contain null values.
    // RG1 : [min: 20, max: 29, num_nulls: 0]
    // RG2 : [min: 31, max: 39, num_nulls: 1]
    // RG3 : [min: 40, max: 49, num_nulls: 1]
    // RG4 : [min: 50, max: 59, num_nulls: 0]
    final String sql = "select a from dfs.`parquet/multirowgroupwithNulls.parquet` where ";
    // "<" "and" ">" with filter
    testParquetFilterPruning(sql + "30 < a and 40 > a", 9, 1, null);
    testParquetFilterPruning(sql + "30 < a and a < 40", 9, 1, null);
    testParquetFilterPruning(sql + "a > 30 and 40 > a", 9, 1, null);
    testParquetFilterPruning(sql + "a > 30 and a < 40", 9, 1, null);
    // "<" "and" ">" with no filter
    testParquetFilterPruning(sql + "19 < a and 30 > a", 10, 1, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "19 < a and a < 30", 10, 1, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "a > 19 and 30 > a", 10, 1, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "a > 19 and a < 30", 10, 1, new String[]{"Filter\\("});
    // "<=" "and" ">=" with filter
    testParquetFilterPruning(sql + "a >= 30 and 39 >= a", 9, 1, null);
    testParquetFilterPruning(sql + "a >= 30 and a <= 39", 9, 1, null);
    testParquetFilterPruning(sql + "30 <= a and 39 >= a", 9, 1, null);
    testParquetFilterPruning(sql + "30 <= a and a <= 39", 9, 1, null);
    // "<=" "and" ">=" with no filter
    testParquetFilterPruning(sql + "a >= 20 and a <= 29", 10, 1, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "a >= 20 and 29 >= a", 10, 1, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "20 <= a and a <= 29", 10, 1, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "20 <= a and 29 >= a", 10, 1, new String[]{"Filter\\("});
    // "<" "or" ">" with filter
    testParquetFilterPruning(sql + "a < 40 or a > 49", 29, 3, null);
    testParquetFilterPruning(sql + "a < 40 or 49 < a", 29, 3, null);
    testParquetFilterPruning(sql + "40 > a or a > 49", 29, 3, null);
    testParquetFilterPruning(sql + "40 > a or 49 < a", 29, 3, null);
    // "<" "or" ">" with no filter
    testParquetFilterPruning(sql + "a < 30 or a > 49", 20, 2, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "a < 30 or 49 < a", 20, 2, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "30 > a or a > 49", 20, 2, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "30 > a or 49 < a", 20, 2, new String[]{"Filter\\("});
    // "<=" "or" ">=" with filter
    testParquetFilterPruning(sql + "a <= 39 or a >= 50", 29, 3, null);
    testParquetFilterPruning(sql + "a <= 39 or 50 <= a", 29, 3, null);
    testParquetFilterPruning(sql + "39 >= a or a >= 50", 29, 3, null);
    testParquetFilterPruning(sql + "39 >= a or 50 <= a", 29, 3, null);
    // "<=" "or" ">=" with no filter
    testParquetFilterPruning(sql + "a <= 29 or a >= 50", 20, 2, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "a <= 29 or 50 <= a", 20, 2, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "29 >= a or a >= 50", 20, 2, new String[]{"Filter\\("});
    testParquetFilterPruning(sql + "29 >= a or 50 <= a", 20, 2, new String[]{"Filter\\("});
  }

  @Test
  // Test against parquet files from Drill CTAS post 1.8.0 release.
  public void testDatePredicateAgainstDrillCTASPost1_8() throws Exception {
    test("use dfs.tmp");
    test("create table `%s/t1` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-01' and " +
      "date '1992-01-03'", CTAS_TABLE);
    test("create table `%s/t2` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-04' and " +
      "date '1992-01-06'", CTAS_TABLE);
    test("create table `%s/t3` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-07' and " +
      "date '1992-01-09'", CTAS_TABLE);

    final String query1 = "select o_orderdate from dfs.tmp.order_ctas where o_orderdate = date '1992-01-01'";
    testParquetFilterPD(query1, 9, 1, false);

    final String query2 = "select o_orderdate from dfs.tmp.order_ctas where o_orderdate < date '1992-01-01'";
    testParquetFilterPD(query2, 0, 1, false);

    final String query3 = "select o_orderdate from dfs.tmp.order_ctas where o_orderdate between date '1992-01-01' and date '1992-01-03'";
    testParquetFilterPD(query3, 22, 1, false);

    final String query4 = "select o_orderdate from dfs.tmp.order_ctas where o_orderdate between date '1992-01-01' and date '1992-01-04'";
    testParquetFilterPD(query4, 33, 2, false);

    final String query5 = "select o_orderdate from dfs.tmp.order_ctas where o_orderdate between date '1992-01-01' and date '1992-01-06'";
    testParquetFilterPD(query5, 49, 2, false);

    final String query6 = "select o_orderdate from dfs.tmp.order_ctas where o_orderdate > date '1992-01-10'";
    testParquetFilterPD(query6, 0, 1, false);

    // Test parquet files with metadata cache files available.
    // Now, create parquet metadata cache files, and run the above queries again. Flag "usedMetadataFile" should be true.
    test(String.format("refresh table metadata %s", CTAS_TABLE));
    testParquetFilterPD(query1, 9, 1, true);
    testParquetFilterPD(query2, 0, 1, true);
    testParquetFilterPD(query3, 22, 1, true);
    testParquetFilterPD(query4, 33, 2, true);
    testParquetFilterPD(query5, 49, 2, true);
    testParquetFilterPD(query6, 0, 1, true);
  }

  @Test
  public void testParquetFilterPDOptionsDisabled() throws Exception {
    try {
      test("alter session set `%s` = false", PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_KEY);

      test("use dfs.tmp");
      test("create table `%s/t1` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-01' and " +
        "date '1992-01-03'", CTAS_TABLE);
      test("create table `%s/t2` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-04' and " +
        "date '1992-01-06'", CTAS_TABLE);
      test("create table `%s/t3` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-07' and " +
        "date '1992-01-09'", CTAS_TABLE);

      final String query1 = "select o_orderdate from dfs.tmp.order_ctas where o_orderdate = date '1992-01-01'";
      testParquetFilterPD(query1, 9, 3, false);

    } finally {
      resetSessionOption(PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_KEY);
    }
  }

  @Test
  public void testParquetFilterPDOptionsThreshold() throws Exception {
    try {
      test("alter session set `" + PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD_KEY  + "` = 2 ");

      test("use dfs.tmp");
      test("create table `%s/t1` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-01' and " +
        "date '1992-01-03'", CTAS_TABLE);
      test("create table `%s/t2` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-04' and " +
        "date '1992-01-06'", CTAS_TABLE);
      test("create table `%s/t3` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-07' and " +
        "date '1992-01-09'", CTAS_TABLE);

      final String query1 = "select o_orderdate from dfs.tmp.order_ctas where o_orderdate = date '1992-01-01'";
      testParquetFilterPD(query1, 9, 3, false);

    } finally {
      resetSessionOption(PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD_KEY);
    }
  }

  @Test
  public void testParquetFilterPDWithLargeCondition() throws Exception {
    test("SELECT * FROM (SELECT n.n_name AS name, n.n_nationkey AS nationkey, " +
        "cast(n.n_regionkey AS FLOAT) AS regionkey FROM cp.`/tpch/nation.parquet` n) " +
        "WHERE ((name = 'A' AND ((regionkey >= 0.0 AND regionkey <= 120.0 AND nationkey = 0.005))) " +
        "OR (name = 'B' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'C' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'D' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'E' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'F' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'G' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'I' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'J' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'K' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'L' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'M' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'N' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'O' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'P' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))) " +
        "OR (name = 'Q' AND ((regionkey >= 0.0  AND regionkey <= 120.0  AND nationkey = 0.005))))");
  }

  @Test
  public void testDatePredicateAgainstCorruptedDateCol() throws Exception {
    // Table dateTblCorrupted is created by CTAS in drill 1.8.0. Per DRILL-4203, the date column is shifted by some value.
    // The CTAS are the following, then copy to drill test resource directory.
    //    create table dfs.tmp.`dateTblCorrupted/t1` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-01' and date '1992-01-03';
    //    create table dfs.tmp.`dateTblCorrupted/t2` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-04' and date '1992-01-06';
    //    create table dfs.tmp.`dateTblCorrupted/t3` as select cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-07' and date '1992-01-09';

    final String query1 = "select o_orderdate from dfs.`parquetFilterPush/dateTblCorrupted` where o_orderdate = date '1992-01-01'";
    testParquetFilterPD(query1, 9, 1, false);

    final String query2 = "select o_orderdate from dfs.`parquetFilterPush/dateTblCorrupted` where o_orderdate < date '1992-01-01'";
    testParquetFilterPD(query2, 0, 1, false);

    final String query3 = "select o_orderdate from dfs.`parquetFilterPush/dateTblCorrupted` where o_orderdate between date '1992-01-01' and date '1992-01-03'";
    testParquetFilterPD(query3, 22, 1, false);

    final String query4 = "select o_orderdate from dfs.`parquetFilterPush/dateTblCorrupted` where o_orderdate between date '1992-01-01' and date '1992-01-04'";
    testParquetFilterPD(query4, 33, 2, false);

    final String query5 = "select o_orderdate from dfs.`parquetFilterPush/dateTblCorrupted` where o_orderdate between date '1992-01-01' and date '1992-01-06'";
    testParquetFilterPD(query5, 49, 2, false);

    final String query6 = "select o_orderdate from dfs.`parquetFilterPush/dateTblCorrupted` where o_orderdate > date '1992-01-10'";

    testParquetFilterPD(query6, 0, 1, false);
  }

  @Test
  public void testTimeStampPredicate() throws Exception {
    // Table dateTblCorrupted is created by CTAS in drill 1.8.0.
    //    create table dfs.tmp.`tsTbl/t1` as select DATE_ADD(cast(o_orderdate as date), INTERVAL '0 10:20:30' DAY TO SECOND) as o_ordertimestamp from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-01' and date '1992-01-03';
    //    create table dfs.tmp.`tsTbl/t2` as select DATE_ADD(cast(o_orderdate as date), INTERVAL '0 10:20:30' DAY TO SECOND) as o_ordertimestamp from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-04' and date '1992-01-06';
    //    create table dfs.tmp.`tsTbl/t3` as select DATE_ADD(cast(o_orderdate as date), INTERVAL '0 10:20:30' DAY TO SECOND) as o_ordertimestamp from cp.`tpch/orders.parquet` where o_orderdate between date '1992-01-07' and date '1992-01-09';

    final String query1 = "select o_ordertimestamp from dfs.`parquetFilterPush/tsTbl` where o_ordertimestamp = timestamp '1992-01-01 10:20:30'";
    testParquetFilterPD(query1, 9, 1, false);

    final String query2 = "select o_ordertimestamp from dfs.`parquetFilterPush/tsTbl` where o_ordertimestamp < timestamp '1992-01-01 10:20:30'";
    testParquetFilterPD(query2, 0, 1, false);

    final String query3 = "select o_ordertimestamp from dfs.`parquetFilterPush/tsTbl` where o_ordertimestamp between timestamp '1992-01-01 00:00:00' and timestamp '1992-01-06 10:20:30'";
    testParquetFilterPD(query3, 49, 2, false);
  }

  @Test
  public void testBooleanPredicate() throws Exception {
    // Table blnTbl was created by CTAS in drill 1.12.0 and consist of 4 files withe the next data:
    //    File 0_0_0.parquet has col_bln column with the next values: true, true, true.
    //    File 0_0_1.parquet has col_bln column with the next values: false, false, false.
    //    File 0_0_2.parquet has col_bln column with the next values: true, null, false.
    //    File 0_0_3.parquet has col_bln column with the next values: null, null, null.

    final String queryIsNull = "select col_bln from dfs.`parquetFilterPush/blnTbl` where col_bln is null";
    testParquetFilterPD(queryIsNull, 4, 2, false);

    final String queryIsNotNull = "select col_bln from dfs.`parquetFilterPush/blnTbl` where col_bln is not null";
    testParquetFilterPD(queryIsNotNull, 8, 3, false);

    final String queryIsTrue = "select col_bln from dfs.`parquetFilterPush/blnTbl` where col_bln is true";
    testParquetFilterPD(queryIsTrue, 4, 2, false);

    final String queryIsNotTrue = "select col_bln from dfs.`parquetFilterPush/blnTbl` where col_bln is not true";
    testParquetFilterPD(queryIsNotTrue, 8, 3, false);

    final String queryIsFalse = "select col_bln from dfs.`parquetFilterPush/blnTbl` where col_bln is false";
    testParquetFilterPD(queryIsFalse, 4, 2, false);

    final String queryIsNotFalse = "select col_bln from dfs.`parquetFilterPush/blnTbl` where col_bln is not false";
    testParquetFilterPD(queryIsNotFalse, 8, 3, false);

    final String queryEqualTrue = "select col_bln from dfs.`parquetFilterPush/blnTbl` where col_bln = true";
    testParquetFilterPD(queryEqualTrue, 4, 2, false);

    final String queryNotEqualTrue = "select col_bln from dfs.`parquetFilterPush/blnTbl` where not col_bln = true";
    testParquetFilterPD(queryNotEqualTrue, 4, 2, false);

    final String queryEqualFalse = "select col_bln from dfs.`parquetFilterPush/blnTbl` where col_bln = false";
    testParquetFilterPD(queryEqualFalse, 4, 2, false);

    final String queryNotEqualFalse = "select col_bln from dfs.`parquetFilterPush/blnTbl` where not col_bln = false";
    testParquetFilterPD(queryNotEqualFalse, 4, 2, false);

    final String queryEqualTrueWithAnd = "select col_bln from dfs.`parquetFilterPush/blnTbl` where col_bln = true and unk_col = 'a'";
    testParquetFilterPD(queryEqualTrueWithAnd, 0, 2, false);

    // File ff1.parquet has column with the values: false, null, false.
    // File tt1.parquet has column with the values: true, null, true.
    // File ft0.parquet has column with the values: false, true.
    final String query = "select a from dfs.`parquetFilterPush/tfTbl` where ";
    testParquetFilterPD(query + "a is true", 3, 2, false);
    testParquetFilterPD(query + "a is false", 3, 2, false);
    testParquetFilterPD(query + "a is not true", 5, 1, false);
    testParquetFilterPD(query + "a is not false", 5, 1, false);
  }

  @Test // DRILL-5359
  public void testFilterWithItemFlatten() throws Exception {
    final String sql = "select n_regionkey\n"
      + "from (select n_regionkey, \n"
      + "            flatten(nation.cities) as cities \n"
      + "      from cp.`tpch/nation.parquet` nation) as flattenedCities \n"
      + "where flattenedCities.cities.`zip` = '12345'";

    final String[] expectedPlan = {"(?s)Filter.*Flatten"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);

  }

  @Test
  public void testMultiRowGroup() throws Exception {
    // multirowgroup is a parquet file with 2 rowgroups inside. One with a = 1 and the other with a = 2;
    // FilterPushDown should be able to remove the rowgroup with a = 1 from the scan operator.
    final String sql = "select * from dfs.`parquet/multirowgroup.parquet` where a > 1";
    final String[] expectedPlan = {"numRowGroups=1"};
    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan);
  }

  @Test
  public void testWithMissingStatistics() throws Exception {
    /*
      wide_string.parquet

      Schema:
        message root {
          optional binary col_str (UTF8);
        }

      Content:
      first row -> `a` character repeated 2050 times
      second row -> null
     */
    String tableName = "wide_string_table";
    java.nio.file.Path wideStringFilePath = Paths.get("parquet", "wide_string.parquet");
    dirTestWatcher.copyResourceToRoot(wideStringFilePath, Paths.get(tableName, "0_0_0.parquet"));
    dirTestWatcher.copyResourceToRoot(wideStringFilePath, Paths.get(tableName, "0_0_1.parquet"));

    String query = String.format("select count(1) as cnt from dfs.`%s` where col_str is null", tableName);

    String[] expectedPlan = {"numRowGroups=2"};
    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan);

    testBuilder().sqlQuery(query).unOrdered().baselineColumns("cnt").baselineValues(2L).go();
  }

  @Test // testing min=false, max=true, min/max set, no nulls
  @SuppressWarnings("unchecked")
  public void testMinFalseMaxTrue() {
    LogicalExpression le = Mockito.mock(LogicalExpression.class);
    ColumnStatistics<Boolean> booleanStatistics = Mockito.mock(ColumnStatistics.class);
    Mockito.doReturn(booleanStatistics).when(le).accept(ArgumentMatchers.any(), ArgumentMatchers.any());
    StatisticsProvider<Boolean> re = Mockito.mock(StatisticsProvider.class);
    Mockito.when(re.getRowCount()).thenReturn(2L); // 2 rows
    Mockito.when(booleanStatistics.contains(ArgumentMatchers.any())).thenReturn(true); // stat is not empty
    Mockito.when(booleanStatistics.get(ColumnStatisticsKind.NULLS_COUNT)).thenReturn(0L); // no nulls
    Mockito.when(booleanStatistics.get(ColumnStatisticsKind.MIN_VALUE)).thenReturn(false); // min false
    Mockito.when(booleanStatistics.get(ColumnStatisticsKind.MAX_VALUE)).thenReturn(true); // max true
    Mockito.when(booleanStatistics.getValueComparator()).thenReturn(Comparator.nullsFirst(Comparator.naturalOrder())); // comparator
    IsPredicate<Boolean> isTrue = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_TRUE, le);
    assertEquals(RowsMatch.SOME, isTrue.matches(re));
    IsPredicate<Boolean> isFalse = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_FALSE, le);
    assertEquals(RowsMatch.SOME, isFalse.matches(re));
    IsPredicate<Boolean> isNotTrue = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_NOT_TRUE, le);
    assertEquals(RowsMatch.SOME, isNotTrue.matches(re));
    IsPredicate<Boolean> isNotFalse = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_NOT_FALSE, le);
    assertEquals(RowsMatch.SOME, isNotFalse.matches(re));
  }

  @Test // testing min=false, max=false, min/max set, no nulls
  @SuppressWarnings("unchecked")
  public void testMinFalseMaxFalse() {
    LogicalExpression le = Mockito.mock(LogicalExpression.class);
    ColumnStatistics<Boolean> booleanStatistics = Mockito.mock(ColumnStatistics.class);
    Mockito.doReturn(booleanStatistics).when(le).accept(ArgumentMatchers.any(), ArgumentMatchers.any());
    StatisticsProvider<Boolean> re = Mockito.mock(StatisticsProvider.class);
    Mockito.when(re.getRowCount()).thenReturn(2L); // 2 rows
    Mockito.when(booleanStatistics.contains(ArgumentMatchers.any())).thenReturn(true); // stat is not empty
    Mockito.when(booleanStatistics.get(ColumnStatisticsKind.NULLS_COUNT)).thenReturn(0L); // no nulls
    Mockito.when(booleanStatistics.get(ColumnStatisticsKind.MIN_VALUE)).thenReturn(false); // min false
    Mockito.when(booleanStatistics.get(ColumnStatisticsKind.MAX_VALUE)).thenReturn(false); // max false
    Mockito.when(booleanStatistics.getValueComparator()).thenReturn(Comparator.nullsFirst(Comparator.naturalOrder())); // comparator
    IsPredicate<Boolean> isTrue = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_TRUE, le);
    assertEquals(RowsMatch.NONE, isTrue.matches(re));
    IsPredicate<Boolean> isFalse = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_FALSE, le);
    assertEquals(RowsMatch.ALL, isFalse.matches(re));
    IsPredicate<Boolean> isNotTrue = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_NOT_TRUE, le);
    assertEquals(RowsMatch.ALL, isNotTrue.matches(re));
    IsPredicate<Boolean> isNotFalse = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_NOT_FALSE, le);
    assertEquals(RowsMatch.NONE, isNotFalse.matches(re));
  }

  @Test // testing min=true, max=true, min/max set, no nulls
  @SuppressWarnings("unchecked")
  public void testMinTrueMaxTrue() {
    LogicalExpression le = Mockito.mock(LogicalExpression.class);
    ColumnStatistics<Boolean> booleanStatistics = Mockito.mock(ColumnStatistics.class);
    Mockito.doReturn(booleanStatistics).when(le).accept(ArgumentMatchers.any(), ArgumentMatchers.any());
    StatisticsProvider<Boolean> re = Mockito.mock(StatisticsProvider.class);
    Mockito.when(re.getRowCount()).thenReturn(Long.valueOf(2)); // 2 rows
    Mockito.when(booleanStatistics.contains(ArgumentMatchers.any())).thenReturn(true); // stat is not empty
    Mockito.when(booleanStatistics.get(ColumnStatisticsKind.NULLS_COUNT)).thenReturn(0L); // no nulls
    Mockito.when(booleanStatistics.get(ColumnStatisticsKind.MIN_VALUE)).thenReturn(true); // min false
    Mockito.when(booleanStatistics.get(ColumnStatisticsKind.MAX_VALUE)).thenReturn(true); // max false
    IsPredicate<Boolean> isTrue = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_TRUE, le);
    assertEquals(RowsMatch.ALL, isTrue.matches(re));
    IsPredicate<Boolean> isFalse = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_FALSE, le);
    assertEquals(RowsMatch.NONE, isFalse.matches(re));
    IsPredicate<Boolean> isNotTrue = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_NOT_TRUE, le);
    assertEquals(RowsMatch.NONE, isNotTrue.matches(re));
    IsPredicate<Boolean> isNotFalse = (IsPredicate<Boolean>) IsPredicate.createIsPredicate(FunctionGenerationHelper.IS_NOT_FALSE, le);
    assertEquals(RowsMatch.ALL, isNotFalse.matches(re));
  }

  @Test
  public void tesNonDeterministicIsNotNullWithNonExistingColumn() throws Exception {
    String query = "select count(*) as cnt from cp.`tpch/nation.parquet`\n" +
        "where (case when random() = 1 then true else null end * t) is not null";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(0L)
        .go();
  }

  @Test
  public void testParquetSingleRowGroupFilterRemoving() throws Exception {
    test("create table dfs.tmp.`singleRowGroupTable` as select * from cp.`tpch/nation.parquet`");

    String query = "select * from dfs.tmp.`singleRowGroupTable` where n_nationkey > -1";

    testParquetFilterPruning(query, 25, 1, new String[]{"Filter\\("});
  }

  @Test
  public void testPreservingColumnAliasAfterRemovingFilter() throws Exception {
    test("create table dfs.tmp.`testAliasTable` as select * from cp.`tpch/nation.parquet` limit 1");
    String query = "select n_regionkey as x from dfs.tmp.`testAliasTable` where n_nationkey > -100500";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("x")
        .baselineValues(0)
        .go();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Some test helper functions.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  private void testParquetFilterPruning(final String query, int expectedRowCount, int expectedRowgroups, String[] excludedPattern) throws Exception {
    int actualRowCount = testSql(query);
    assertEquals(expectedRowCount, actualRowCount);
    String numRowGroupPattern = "numRowGroups=" + expectedRowgroups;
    testPlanMatchingPatterns(query, new String[]{numRowGroupPattern}, excludedPattern);
  }

  private void testParquetFilterPD(final String query, int expectedRowCount, int expectedNumFiles, boolean usedMetadataFile) throws Exception {
    int actualRowCount = testSql(query);
    assertEquals(expectedRowCount, actualRowCount);
    String numFilesPattern = "numFiles=" + expectedNumFiles;
    String usedMetaPattern = "usedMetadataFile=" + usedMetadataFile;

    testPlanMatchingPatterns(query, new String[]{numFilesPattern, usedMetaPattern});
  }

  private void testParquetRowGroupFilterEval(MetadataBase.ParquetTableMetadataBase footer, final String exprStr, RowsMatch canDropExpected) {
    final LogicalExpression filterExpr = parseExpr(exprStr);
    testParquetRowGroupFilterEval(footer, 0, filterExpr, canDropExpected);
  }

  private void testParquetRowGroupFilterEval(MetadataBase.ParquetTableMetadataBase footer, final int rowGroupIndex, final LogicalExpression filterExpr, RowsMatch canDropExpected) {
    RowsMatch canDrop = FilterEvaluatorUtils.evalFilter(filterExpr, footer, rowGroupIndex, fragContext.getOptions(), fragContext);
    Assert.assertEquals(canDropExpected, canDrop);
  }

  private MetadataBase.ParquetTableMetadataBase getParquetMetaData(File file) throws IOException {
    return Metadata.getParquetTableMetadata(fs, new Path(file.toURI().getPath()), ParquetReaderConfig.getDefaultInstance());
  }

  // =========  runtime pruning  ==========
  @Rule
  public final BaseDirTestWatcher baseDirTestWatcher = new BaseDirTestWatcher();

  /**
   *
   * @throws Exception
   */
  private void genericTestRuntimePruning(int maxParallel, String sql, long expectedRows, int numPartitions, int numPruned) throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(baseDirTestWatcher)
      .sessionOption(ExecConstants.SKIP_RUNTIME_ROWGROUP_PRUNING_KEY,false)
      .sessionOption(PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD_KEY,0)
      .maxParallelization(maxParallel)
      .saveProfiles();

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      runAndCheckResults(client, sql, expectedRows, numPartitions, numPruned);
    }
  }

  /**
   * Test runtime pruning
   *
   * @throws Exception
   */
  @Test
  public void testRuntimePruning() throws Exception {
    // 's' is the partitioning key (values are: 3,4,5,6 ) -- prune out 2 out of 4 rowgroups
    genericTestRuntimePruning( 2, "select a from cp.`parquet/multirowgroupwithNulls.parquet` where s > 4", 20, 4,2 );
    // prune out all rowgroups
    genericTestRuntimePruning( 2, "select a from cp.`parquet/multirowgroupwithNulls.parquet` where s > 8", 0, 4,4 );
  }

  private void runAndCheckResults(ClientFixture client, String sql, long expectedRows, long numPartitions, long numPruned) throws Exception {
    QueryBuilder.QuerySummary summary = client.queryBuilder().sql(sql).run();

    if (expectedRows > 0) {
      assertEquals(expectedRows, summary.recordCount());
    }

    ProfileParser profile = client.parseProfile(summary.queryIdString());
    List<ProfileParser.OperatorProfile> ops = profile.getOpsOfType(ParquetRowGroupScan.OPERATOR_TYPE);

    assertFalse(ops.isEmpty());
    // check for the first op only
    ProfileParser.OperatorProfile parquestScan0 = ops.get(0);
    long resultNumRowgroups = parquestScan0.getMetric(ParquetRecordReader.Metric.NUM_ROWGROUPS.ordinal());
    assertEquals(numPartitions, resultNumRowgroups);
    long resultNumPruned = parquestScan0.getMetric(ParquetRecordReader.Metric.ROWGROUPS_PRUNED.ordinal());
    assertEquals(numPruned,resultNumPruned);
  }
}
