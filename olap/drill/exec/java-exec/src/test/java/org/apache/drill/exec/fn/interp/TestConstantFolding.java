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
package org.apache.drill.exec.fn.interp;

import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.PlanTestBase;
import org.apache.drill.categories.SqlTest;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;

@Category(SqlTest.class)
public class TestConstantFolding extends PlanTestBase {

  public static class SmallFileCreator {

    private final File folder;
    private static final List<String> values = Lists.newArrayList("1","2","3");
    private static final String jsonRecord =  "{\"col1\" : 1,\"col2\" : 2, \"col3\" : 3}";
    private String record;

    public SmallFileCreator(File folder) {
      this.folder = folder;
      this.record = null;
    }

    public SmallFileCreator setRecord(String record) {
      this.record = record;
      return this;
    }

    public void createFiles(int smallFileLines, int bigFileLines, String extension, String delimiter) throws Exception{
      if (record == null) {
        if (extension.equals("csv") || extension.equals("tsv")) {
          record = Joiner.on(delimiter).join(values);
        } else if (extension.equals("json") ){
          record = jsonRecord;
        } else {
          throw new UnsupportedOperationException(
              String.format("Extension %s not supported by %s",
                  extension, SmallFileCreator.class.getSimpleName()));
        }
      }
      PrintWriter out;
      for (String fileAndFolderName : new String[]{"bigfile", "BIGFILE_2"}) {
        File bigFolder = new File(folder, fileAndFolderName);
        bigFolder.mkdirs();
        File bigFile = new File (bigFolder, fileAndFolderName + "." + extension);
        out = new PrintWriter(bigFile);
        for (int i = 0; i < bigFileLines; i++ ) {
          out.println(record);
        }
        out.close();
      }

      for (String fileAndFolderName : new String[]{"smallfile", "SMALLFILE_2"}) {
        File smallFolder = new File(folder, fileAndFolderName);
        smallFolder.mkdirs();
        File smallFile = new File (smallFolder, fileAndFolderName + "." + extension);
        out = new PrintWriter(smallFile);
        for (int i = 0; i < smallFileLines; i++ ) {
          out.println(record);
        }
        out.close();
      }
    }

    public void createFiles(int smallFileLines, int bigFileLines, String extension) throws Exception{
      String delimiter;
      if (extension.equals("json")) {
        delimiter = null;
      } else if (extension.equals("csv")) {
        delimiter = ",";
      } else if (extension.equals("tsv")) {
        delimiter = "\t";
      } else {
        throw new UnsupportedOperationException("Extension not recognized, please explicitly provide a delimiter.");
      }
      createFiles(smallFileLines, bigFileLines, extension, delimiter);
    }

    public void createFiles(int smallFileLines, int bigFileLines) throws Exception{
      createFiles(smallFileLines, bigFileLines, "csv", ",");
    }

  }

  @Test
  public void testConstantFolding_allTypes() throws Exception {
    mockUsDateFormatSymbols();

    try {
      test("alter session set `store.json.all_text_mode` = true;");
      test(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));

      String query2 = "SELECT *  " +
          "FROM   cp.`parquet/alltypes.json`  " +
          "WHERE  12 = extract(day from (to_timestamp('2014-02-12 03:18:31:07 AM', 'YYYY-MM-dd HH:mm:ss:SS a'))) " +
          "AND    cast( `int_col` AS             int) = castint('1')  " +
          "AND    cast( `bigint_col` AS          bigint) = castbigint('100000000000')  " +
          "AND    cast( `decimal9_col` AS        decimal(9, 4)) = 1.0 + 0.0  " +
          "AND    cast( `decimal18_col` AS       decimal(18,9)) = 123456789.000000000 + 0.0  " +
          "AND    cast( `decimal28sparse_col` AS decimal(28, 14)) = 123456789.000000000 + 0.0 " +
          "AND    cast( `decimal38sparse_col` AS decimal(38, 19)) = 123456789.000000000 + 0.0 " +
          "AND    cast( `date_col` AS            date) = cast('1995-01-01' as date)  " +
          "AND    cast( `date_col` AS            date) = castdate('1995-01-01')  " +
          "AND    cast( `date_col` AS            date) = DATE '1995-01-01'  " +
          "AND    cast( `timestamp_col` AS timestamp) = casttimestamp('1995-01-01 01:00:10.000')  " +
          "AND    cast( `float4_col` AS float) = castfloat4('1')  " +
          "AND    cast( `float8_col` AS DOUBLE) = castfloat8('1')  " +
          "AND    cast( `varbinary_col` AS varbinary(65000)) = castvarbinary('qwerty', 0)  " +
          "AND    cast( `intervalyear_col` AS interval year) = castintervalyear('P1Y')  " +
          "AND    cast( `intervalday_col` AS interval day) = castintervalday('P1D')" +
          "AND    cast( `bit_col` AS       boolean) = castbit('false')  " +
          "AND    `varchar_col` = concat('qwe','rty')  " +
          "AND    cast( `time_col` AS            time) = casttime('01:00:00')";


      testBuilder()
          .sqlQuery(query2)
          .ordered()
          .baselineColumns("TINYINT_col", "SMALLINT_col", "INT_col", "FLOAT4_col",
              "TIME_col", "DECIMAL9_col", "BIGINT_col", "UINT8_col", "FLOAT8_col",
              "DATE_col", "TIMESTAMP_col", "DECIMAL18_col", "INTERVALYEAR_col",
              "INTERVALDAY_col", "INTERVAL_col", "DECIMAL38SPARSE_col",
              "DECIMAL28SPARSE_col", "VARBINARY_col", "VARCHAR_col",
              "VAR16CHAR_col", "BIT_col")
          // the file is being read in all_text_mode to preserve exact values (as all numbers with decimal points are read
          // as double in the JsonReader), so the baseline values here are all specified as strings
          .baselineValues(
              "1", "1", "1", "1", "01:00:00", "1.0", "100000000000", "1", "1", "1995-01-01", "1995-01-01 01:00:10.000",
              "123456789.000000000", "P1Y", "P1D", "P1Y1M1DT1H1M", "123456789.000000000",
              "123456789.000000000", "qwerty", "qwerty","qwerty", "false")
          .go();
    } finally {
      test("alter session set `store.json.all_text_mode` = false;");
      test(String.format("alter session set `%s` = false", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
    }
  }

  @Ignore("DRILL-2553")
  @Test
  public void testConstExprFolding_withPartitionPrune_verySmallFiles() throws Exception {
    new SmallFileCreator(dirTestWatcher.getRootDir()).createFiles(1, 8);
    testPlanOneExpectedPatternOneExcluded(
        "select * from dfs.`*/*.csv` where dir0 = concat('small','file')",
        "smallfile",
        "bigfile");
  }

  @Test
  public void testConstExprFolding_withPartitionPrune() throws Exception {
    new SmallFileCreator(dirTestWatcher.getRootDir()).createFiles(1, 1000);
    testPlanOneExpectedPatternOneExcluded(
        "select * from dfs.`*/*.csv` where dir0 = concat('small','file')",
        "smallfile",
        "bigfile");
  }

  @Test
  public void testConstExprFolding_nonDirFilter() throws Exception {
    testPlanOneExpectedPatternOneExcluded(
        "select * from cp.`functions/interp/test_input.csv` where columns[0] = 2+2",
        "Filter\\(condition=\\[=\\(ITEM\\(\\$[0-9]+, 0\\), 4\\)",
        "Filter\\(condition=\\[=\\(ITEM\\(\\$[0-9]+, 0\\), \\+\\(2, 2\\)\\)");
  }

  @Test
  public void testConstantFoldingDisableOption() throws Exception {
    try {
      test("alter session set `planner.enable_constant_folding` = false");
      testPlanOneExpectedPatternOneExcluded(
          "select * from cp.`functions/interp/test_input.csv` where columns[0] = 2+2",
          "Filter\\(condition=\\[=\\(ITEM\\(\\$[0-9]+, 0\\), \\+\\(2, 2\\)\\)",
          "Filter\\(condition=\\[=\\(ITEM\\(\\$[0-9]+, 0\\), 4\\)");
    } finally {
      test("alter session set `planner.enable_constant_folding` = true");
    }
  }

  @Test
  public void testConstExprFolding_moreComplicatedNonDirFilter() throws Exception {
    testPlanOneExpectedPatternOneExcluded(
        "select * from cp.`functions/interp/test_input.csv` where columns[1] = ABS((6-18)/(2*3))",
        "Filter\\(condition=\\[=\\(ITEM\\(\\$[0-9]+, 1\\), 2\\)",
        "Filter\\(condition=\\[=\\(ITEM\\(\\$[0-9]+, 1\\), ABS\\(/\\(-\\(6, 18\\), \\*\\(2, 3\\)\\)\\)\\)");
  }

  @Test
  public void testConstExprFolding_dontFoldRandom() throws Exception {
    testPlanOneExpectedPatternOneExcluded(
        "select * from cp.`functions/interp/test_input.csv` where columns[0] = random()",
        "Filter\\(condition=\\[=\\(ITEM\\(\\$[0-9]+, 0\\), RANDOM\\(\\)",
        "Filter\\(condition=\\[=\\(ITEM\\(\\$[0-9]+, 0\\), [0-9\\.]+");
  }

  @Test
  public void testConstExprFolding_ToLimit0() throws Exception {
    testPlanOneExpectedPatternOneExcluded(
        "select * from cp.`functions/interp/test_input.csv` where 1=0",
        "Limit\\(offset=\\[0\\], fetch=\\[0\\]\\)",
        "Filter\\(condition=\\[=\\(1, 0\\)\\]\\)");
  }

  // Despite a comment indicating that the plan generated by the ReduceExpressionRule
  // should be set to be always preferable to the input rel, I cannot get it to
  // produce a plan with the reduced result. I can trace through where the rule is fired
  // and I can see that the expression is being evaluated and the constant is being
  // added to a project, but this is not part of the final plan selected. May
  // need to open a calcite bug.
  // Tried to disable the calc and filter rules, only leave the project one, didn't help.
  @Ignore("DRILL-2218")
  @Test
  public void testConstExprFolding_InSelect() throws Exception {
    testPlanOneExcludedPattern("select columns[0], 3+5 from cp.`functions/interp/test_input.csv`",
        "EXPR\\$[0-9]+=\\[\\+\\(3, 5\\)\\]");
  }
}
