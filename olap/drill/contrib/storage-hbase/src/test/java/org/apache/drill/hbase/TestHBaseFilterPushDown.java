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
package org.apache.drill.hbase;

import org.apache.drill.categories.HbaseStorageTest;
import org.apache.drill.PlanTestBase;
import org.apache.drill.categories.SlowTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, HbaseStorageTest.class})
public class TestHBaseFilterPushDown extends BaseHBaseTest {

  @Test
  public void testFilterPushDownRowKeyEqual() throws Exception {
    setColumnWidths(new int[] {8, 38, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key = 'b4'";

    runHBaseSQLVerifyCount(sql, 1);

    final String[] expectedPlan = {".*startRow=\"b4\", stopRow=\"b4\\\\x00\".*"};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownRowKeyNotEqual() throws Exception {
    setColumnWidths(new int[] {8, 38, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key <> 'b4'";

    runHBaseSQLVerifyCount(sql, 7);

    final String[] expectedPlan = {".*startRow=\"\", stopRow=\"\", filter=\"RowFilter \\(NOT_EQUAL, b4\\)\".*"};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownRowKeyEqualWithItem() throws Exception {
    setColumnWidths(new int[] {20, 30});
    final String sql = "SELECT\n"
        + "  cast(tableName.row_key as varchar(20)), cast(tableName.f.c1 as varchar(30))\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key = 'b4'";

    runHBaseSQLVerifyCount(sql, 1);

    final String[] expectedPlan = {".*startRow=\"b4\", stopRow=\"b4\\\\x00\".*"};
    final String[] excludedPlan ={".*startRow=null, stopRow=null.*"};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }


  @Test
  public void testFilterPushDownCompositeDateRowKey1() throws Exception {
    setColumnWidths(new int[] {11, 22, 32});
    runHBaseSQLVerifyCount("SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'date_epoch_be') d\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 9, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeDate` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'date_epoch_be') < DATE '2015-06-18' AND\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'date_epoch_be') > DATE '2015-06-13'",
        12);
  }

  @Test
  public void testFilterPushDownCompositeDateRowKey2() throws Exception {
    setColumnWidths(new int[] {11, 22, 32});
    runHBaseSQLVerifyCount("SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'date_epoch_be') d\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 9, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeDate` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'date_epoch_be') = DATE '2015-08-22'",
        3);
  }

  @Test
  public void testFilterPushDownCompositeDateRowKey3() throws Exception {
    setColumnWidths(new int[] {11, 2000});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'date_epoch_be') d\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 9, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeDate` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'date_epoch_be') < DATE '2015-06-18' AND\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'date_epoch_be') > DATE '2015-06-13'",
        1);
  }

  @Test
  public void testFilterPushDownCompositeDateRowKey4() throws Exception {
    setColumnWidths(new int[] {30, 22, 30, 10});
    runHBaseSQLVerifyCount("SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'timestamp_epoch_be') d\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 9, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'time_epoch_be') t\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeDate` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'timestamp_epoch_be') >= TIMESTAMP '2015-06-18 08:00:00.000' AND\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'timestamp_epoch_be') < TIMESTAMP '2015-06-20 16:00:00.000'",
        7);
  }

  @Test
  public void testFilterPushDownCompositeTimeRowKey1() throws Exception {
    setColumnWidths(new int[] {50, 40, 32});
    runHBaseSQLVerifyCount("SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'time_epoch_be') d\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 9, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeTime` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'time_epoch_be') = TIME '23:57:15.275'",//convert_from(binary_string('\\x00\\x00\\x00\\x00\\x55\\x4D\\xBE\\x80'), 'BIGINT_BE') \n"
        1);
  }

  @Test
  public void testFilterPushDownCompositeTimeRowKey2() throws Exception {
    setColumnWidths(new int[] {30, 2002, 32});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'time_epoch_be') d\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 9, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeTime` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'time_epoch_be') = TIME '23:55:51.250'",//convert_from(binary_string('\\x00\\x00\\x00\\x00\\x55\\x4D\\xBE\\x80'),
        // 'BIGINT_BE') \n"
        1);
  }

  @Test
  public void testFilterPushDownCompositeTimeRowKey3() throws Exception {
    setColumnWidths(new int[] {30, 22, 32});
    runHBaseSQLVerifyCount("SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'time_epoch_be') d\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 9, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeTime` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'time_epoch_be') > TIME '23:57:06' AND"//convert_from(binary_string('\\x00\\x00\\x00\\x00\\x55\\x4D\\xBE\\x80'), 'BIGINT_BE') \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'time_epoch_be') < TIME '23:59:59'",
        8);
  }

  @Test
  public void testFilterPushDownCompositeBigIntRowKey1() throws Exception {
    setColumnWidths(new int[] {15, 40, 32});
    runHBaseSQLVerifyCount("SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'bigint_be') d\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 9, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeDate` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'bigint_be') = cast(1409040000000 as bigint)",//convert_from(binary_string('\\x00\\x00\\x00\\x00\\x55\\x4D\\xBE\\x80'),
        // 'BIGINT_BE') \n"
        1);
  }

  @Test
  public void testFilterPushDownCompositeBigIntRowKey2() throws Exception {
    setColumnWidths(new int[] {16, 22, 32});
    runHBaseSQLVerifyCount("SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'bigint_be') i\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'date_epoch_be') d\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'time_epoch_be') t\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 9, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeDate` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'uint8_be') > cast(1438300800000 as bigint) AND\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 8), 'uint8_be') < cast(1438617600000 as bigint)",
        10);
  }

  @Test
  public void testFilterPushDownCompositeIntRowKey1() throws Exception {
    setColumnWidths(new int[] {16, 22, 32});
    runHBaseSQLVerifyCount("SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 4), 'uint4_be') i\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 5, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeInt` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 4), 'uint4_be') >= cast(423 as int) AND"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 4), 'uint4_be') < cast(940 as int)",
        11);
  }

  @Test
  public void testFilterPushDownCompositeIntRowKey2() throws Exception {
    setColumnWidths(new int[] {16, 2002, 32});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 4), 'uint4_be') i\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 5, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeInt` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 4), 'uint4_be') >= cast(300 as int) AND"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 4), 'uint4_be') < cast(900 as int)",
        1);
  }

  @Test
  public void testFilterPushDownCompositeIntRowKey3() throws Exception {
    setColumnWidths(new int[] {16, 22, 32});
    runHBaseSQLVerifyCount("SELECT \n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 4), 'uint4_be') i\n"
        + ", CONVERT_FROM(BYTE_SUBSTR(row_key, 5, 8), 'bigint_be') id\n"
        + ", CONVERT_FROM(tableName.f.c, 'UTF8') \n"
        + " FROM hbase.`TestTableCompositeInt` tableName\n"
        + " WHERE\n"
        + " CONVERT_FROM(BYTE_SUBSTR(row_key, 1, 4), 'uint4_be') = cast(658 as int)",
        1);
  }

  @Test
  public void testFilterPushDownDoubleOB() throws Exception {
    setColumnWidths(new int[] {8, 25});
    runHBaseSQLVerifyCount("SELECT\n"
        + " convert_from(t.row_key, 'DOUBLE_OB') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableDoubleOB` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'DOUBLE_OB') > cast(95.54 as DOUBLE)",
        6);
  }

  @Test
  public void testFilterPushDownDoubleOBPlan() throws Exception {
    setColumnWidths(new int[] {8, 2000});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR\n"
        + "SELECT\n"
        + " convert_from(t.row_key, 'DOUBLE_OB') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableDoubleOB` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'DOUBLE_OB') > cast(95.54 as DOUBLE)",
        1);
  }

  @Test
  public void testFilterPushDownDoubleOBDesc() throws Exception {
    setColumnWidths(new int[] {8, 25});
    runHBaseSQLVerifyCount("SELECT\n"
        + " convert_from(t.row_key, 'DOUBLE_OBD') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableDoubleOBDesc` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'DOUBLE_OBD') > cast(95.54 as DOUBLE)",
        6);
  }

  @Test
  public void testFilterPushDownDoubleOBDescPlan() throws Exception {
    setColumnWidths(new int[] {8, 2000});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR\n"
        + "SELECT\n"
        + " convert_from(t.row_key, 'DOUBLE_OBD') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableDoubleOBDesc` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'DOUBLE_OBD') > cast(95.54 as DOUBLE)",
        1);
  }

  @Test
  public void testFilterPushDownIntOB() throws Exception {
    setColumnWidths(new int[] {15, 25});
    runHBaseSQLVerifyCount("SELECT\n"
        + " convert_from(t.row_key, 'INT_OB') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableIntOB` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'INT_OB') >= cast(-32 as INT) AND"
        + "  CONVERT_FROM(row_key, 'INT_OB') < cast(59 as INT)",
        91);
  }

  @Test
  public void testFilterPushDownIntOBDesc() throws Exception {
    setColumnWidths(new int[] {15, 25});
    runHBaseSQLVerifyCount("SELECT\n"
        + " convert_from(t.row_key, 'INT_OBD') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableIntOBDesc` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'INT_OBD') >= cast(-32 as INT) AND"
        + "  CONVERT_FROM(row_key, 'INT_OBD') < cast(59 as INT)",
        91);
  }

  @Test
  public void testFilterPushDownIntOBPlan() throws Exception {
    setColumnWidths(new int[] {15, 2000});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR\n"
        + "SELECT\n"
        + " convert_from(t.row_key, 'INT_OB') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableIntOB` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'INT_OB') > cast(-23 as INT) AND"
        + "  CONVERT_FROM(row_key, 'INT_OB') < cast(14 as INT)",
        1);
  }

  @Test
  public void testFilterPushDownIntOBDescPlan() throws Exception {
    setColumnWidths(new int[] {15, 2000});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR\n"
        + "SELECT\n"
        + " convert_from(t.row_key, 'INT_OBD') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableIntOBDesc` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'INT_OBD') > cast(-23 as INT) AND"
        + "  CONVERT_FROM(row_key, 'INT_OBD') < cast(14 as INT)",
        1);
  }

  @Test
  public void testFilterPushDownBigIntOB() throws Exception {
    setColumnWidths(new int[] {15, 25});
    runHBaseSQLVerifyCount("SELECT\n"
        + " convert_from(t.row_key, 'BIGINT_OB') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableBigIntOB` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'BIGINT_OB') > cast(1438034423063 as BIGINT) AND"
        + "  CONVERT_FROM(row_key, 'BIGINT_OB') <= cast(1438034423097 as BIGINT)",
        34);
  }

  @Test
  public void testFilterPushDownBigIntOBPlan() throws Exception {
    setColumnWidths(new int[] {15, 2000});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR\n"
        + "SELECT\n"
        + " convert_from(t.row_key, 'BIGINT_OB') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableBigIntOB` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'BIGINT_OB') > cast(1438034423063 as BIGINT) AND"
        + "  CONVERT_FROM(row_key, 'BIGINT_OB') < cast(1438034423097 as BIGINT)",
        1);
  }

  @Test
  public void testFilterPushDownFloatOB() throws Exception {
    setColumnWidths(new int[] {8, 25});
    runHBaseSQLVerifyCount("SELECT\n"
        + " convert_from(t.row_key, 'FLOAT_OB') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableFloatOB` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'FLOAT_OB') > cast(95.74 as FLOAT) AND"
        + "  CONVERT_FROM(row_key, 'FLOAT_OB') < cast(99.5 as FLOAT)",
        5);
  }

  @Test
  public void testFilterPushDownFloatOBPlan() throws Exception {
    setColumnWidths(new int[] {8, 2000});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR\n"
        + "SELECT\n"
        + " convert_from(t.row_key, 'FLOAT_OB') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableFloatOB` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'FLOAT_OB') > cast(95.54 as FLOAT) AND"
        + "  CONVERT_FROM(row_key, 'FLOAT_OB') < cast(99.77 as FLOAT)",
        1);
  }

  @Test
  public void testFilterPushDownBigIntOBDesc() throws Exception {
    setColumnWidths(new int[] {15, 25});
    runHBaseSQLVerifyCount("SELECT\n"
        + " convert_from(t.row_key, 'BIGINT_OBD') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableBigIntOBDesc` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'BIGINT_OBD') > cast(1438034423063 as BIGINT) AND"
        + "  CONVERT_FROM(row_key, 'BIGINT_OBD') <= cast(1438034423097 as BIGINT)",
        34);
  }

  @Test
  public void testFilterPushDownBigIntOBDescPlan() throws Exception {
    setColumnWidths(new int[] {15, 2000});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR\n"
        + "SELECT\n"
        + " convert_from(t.row_key, 'BIGINT_OBD') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableBigIntOBDesc` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'BIGINT_OBD') > cast(1438034423063 as BIGINT) AND"
        + "  CONVERT_FROM(row_key, 'BIGINT_OBD') < cast(1438034423097 as BIGINT)",
        1);
  }

  @Test
  public void testFilterPushDownFloatOBDesc() throws Exception {
    setColumnWidths(new int[] {8, 25});
    runHBaseSQLVerifyCount("SELECT\n"
        + " convert_from(t.row_key, 'FLOAT_OBD') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableFloatOBDesc` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'FLOAT_OBD') > cast(95.74 as FLOAT) AND"
        + "  CONVERT_FROM(row_key, 'FLOAT_OBD') < cast(99.5 as FLOAT)",
        5);
  }

  @Test
  public void testFilterPushDownFloatOBDescPlan() throws Exception {
    setColumnWidths(new int[] {8, 2000});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR\n"
        + "SELECT\n"
        + " convert_from(t.row_key, 'FLOAT_OBD') rk,\n"
        + " convert_from(t.`f`.`c`, 'UTF8') val\n"
        + "FROM\n"
        + "  hbase.`TestTableFloatOBDesc` t\n"
        + "WHERE\n"
        + "  CONVERT_FROM(row_key, 'FLOAT_OBD') > cast(95.54 as FLOAT) AND"
        + "  CONVERT_FROM(row_key, 'FLOAT_OBD') < cast(99.77 as FLOAT)",
        1);
  }

  @Test
  public void testFilterPushDownRowKeyLike() throws Exception {
    setColumnWidths(new int[] {8, 22});
    final String sql = "SELECT\n"
        + "  row_key, convert_from(tableName.f.c, 'UTF8') `f.c`\n"
        + "FROM\n"
        + "  hbase.`TestTable3` tableName\n"
        + "WHERE\n"
        + "  row_key LIKE '08%0' OR row_key LIKE '%70'";

    runHBaseSQLVerifyCount(sql, 21);

    final String[] expectedPlan = {".*filter=\"FilterList OR.*EQUAL.*EQUAL.*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownRowKeyLikeWithEscape() throws Exception {
    setColumnWidths(new int[] {8, 22});
    final String sql = "SELECT\n"
        + "  row_key, convert_from(tableName.f.c, 'UTF8') `f.c`\n"
        + "FROM\n"
        + "  hbase.`TestTable3` tableName\n"
        + "WHERE\n"
        + "  row_key LIKE '!%!_AS!_PREFIX!_%' ESCAPE '!'";

    runHBaseSQLVerifyCount(sql, 2);

    final String[] expectedPlan = {".*startRow=\"\\%_AS_PREFIX_\", stopRow=\"\\%_AS_PREFIX`\", filter=\"RowFilter.*EQUAL.*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownRowKeyRangeAndColumnValueLike() throws Exception {
    setColumnWidths(new int[] {8, 22});
    final String sql = "SELECT\n"
        + "  row_key, convert_from(tableName.f.c, 'UTF8') `f.c`\n"
        + "FROM\n"
        + "  hbase.`TestTable3` tableName\n"
        + "WHERE\n"
        + " row_key >= '07' AND row_key < '09' AND tableName.f.c LIKE 'value 0%9'";

    runHBaseSQLVerifyCount(sql, 22);

    final String[] expectedPlan = {".*startRow=\"07\", stopRow=\"09\", filter=\"FilterList AND.*RowFilter \\(GREATER_OR_EQUAL, 07\\), RowFilter \\(LESS, 09\\), SingleColumnValueFilter \\(f, c, EQUAL.*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownRowKeyGreaterThan() throws Exception {
    setColumnWidths(new int[] {8, 38, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key > 'b4'";

    runHBaseSQLVerifyCount(sql, 4);

    final String[] expectedPlan = {".*startRow=\"b4\\\\x00\", stopRow=\"\".*"};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownRowKeyGreaterThanWithItem() throws Exception {
    setColumnWidths(new int[] {8, 38});
    final String sql = "SELECT\n"
        + "  row_key, tableName.f2.c3\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key > 'b4'";

    runHBaseSQLVerifyCount(sql, 2);

    final String[] expectedPlan = {".*startRow=\"b4\\\\x00\".*stopRow=.*"};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownRowKeyBetween() throws Exception {
    setColumnWidths(new int[] {8, 74, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key BETWEEN 'a2' AND 'b4'";

    runHBaseSQLVerifyCount(sql, 3);

    final String[] expectedPlan = {".*startRow=\"a2\", stopRow=\"b4\\\\x00\", filter=\"FilterList AND.*GREATER_OR_EQUAL, a2.*LESS_OR_EQUAL, b4.*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownRowKeyBetweenWithItem() throws Exception {
    setColumnWidths(new int[] {8, 12});
    final String sql = "SELECT\n"
        + "  row_key, tableName.f.c1\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key BETWEEN 'a2' AND 'b4'";

    runHBaseSQLVerifyCount(sql, 3);

    final String[] expectedPlan = {".*startRow=\"a2\", stopRow=\"b4\\\\x00\", filter=\"FilterList AND.*GREATER_OR_EQUAL, a2.*LESS_OR_EQUAL, b4.*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownMultiColumns() throws Exception {
    setColumnWidths(new int[] {8, 74, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` t\n"
        + "WHERE\n"
        + "  (row_key >= 'b5' OR row_key <= 'a2') AND (t.f.c1 >= '1' OR t.f.c1 is null)";

    runHBaseSQLVerifyCount(sql, 5);

    final String[] expectedPlan = {".*startRow=\"\", stopRow=\"\", filter=\"FilterList OR.*GREATER_OR_EQUAL, b5.*LESS_OR_EQUAL, a2.*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownMultiColumnsWithItem() throws Exception {
    setColumnWidths(new int[] {8, 8});
    final String sql = "SELECT\n"
        + "  row_key, t.f.c1\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` t\n"
        + "WHERE\n"
        + "  (row_key >= 'b5' OR row_key <= 'a2') AND (t.f.c1 >= '1' OR t.f.c1 is null)";

    final String[] expectedPlan = {".*startRow=\"\", stopRow=\"\", filter=\"FilterList OR.*GREATER_OR_EQUAL, b5.*LESS_OR_EQUAL, a2.*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownConvertExpression() throws Exception {
    setColumnWidths(new int[] {8, 38, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  convert_from(row_key, 'UTF8') > 'b4'";

    runHBaseSQLVerifyCount(sql, 4);

    final String[] expectedPlan = {".*startRow=\"b4\\\\x00\", stopRow=\"\".*"};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownConvertExpressionWithItem() throws Exception {
    setColumnWidths(new int[] {8, 38});
    final String sql = "SELECT\n"
        + "  row_key, tableName.f2.c3\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  convert_from(row_key, 'UTF8') > 'b4'";

    runHBaseSQLVerifyCount(sql, 2);

    final String[] expectedPlan = {".*startRow=\"b4\\\\x00\", stopRow=\"\".*"};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownConvertExpressionWithNumber() throws Exception {
    setColumnWidths(new int[] {8, 1100});
    runHBaseSQLVerifyCount("EXPLAIN PLAN FOR\n"
        + "SELECT\n"
        + "  row_key\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  convert_from(row_key, 'INT_BE') = 75",
        1);
  }

  @Test
  public void testFilterPushDownRowKeyLessThanOrEqualTo() throws Exception {
    setColumnWidths(new int[] {8, 74, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  'b4' >= row_key";

    runHBaseSQLVerifyCount(sql, 4);

    final String[] expectedPlan = {".*startRow=\"\", stopRow=\"b4\\\\x00\".*"};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownRowKeyLessThanOrEqualToWithItem() throws Exception {
    setColumnWidths(new int[] {8, 12});
    final String sql = "SELECT\n"
        + "  row_key, tableName.f.c1\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  'b4' >= row_key";

    runHBaseSQLVerifyCount(sql, 4);

    final String[] expectedPlan = {".*startRow=\"\", stopRow=\"b4\\\\x00\".*"};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);
  }

  @Test
  public void testFilterPushDownOrRowKeyEqual() throws Exception {
    setColumnWidths(new int[] {8, 38, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key = 'b4' or row_key = 'a2'";

    runHBaseSQLVerifyCount(sql, 2);

    final String[] expectedPlan = {".*startRow=\"a2\", stopRow=\"b4\\\\x00\", filter=\"FilterList OR \\(2/2\\): \\[RowFilter \\(EQUAL, b4\\), RowFilter \\(EQUAL, a2\\).*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);

  }

  @Test
  public void testFilterPushDownOrRowKeyInPred() throws Exception {
    setColumnWidths(new int[] {8, 38, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key in ('b4', 'a2')";

    runHBaseSQLVerifyCount(sql, 2);

    final String[] expectedPlan = {".*startRow=\"a2\", stopRow=\"b4\\\\x00\", filter=\"FilterList OR \\(2/2\\): \\[RowFilter \\(EQUAL, b4\\), RowFilter \\(EQUAL, a2\\).*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);

  }

  @Test
  public void testFilterPushDownOrRowKeyEqualRangePred() throws Exception {
    setColumnWidths(new int[] {8, 38, 38});
    final String sql = "SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName\n"
        + "WHERE\n"
        + "  row_key = 'a2' or row_key between 'b5' and 'b6'";

    runHBaseSQLVerifyCount(sql, 3);

    final String[] expectedPlan = {".*startRow=\"a2\", stopRow=\"b6\\\\x00\", filter=\"FilterList OR \\(2/2\\): \\[RowFilter \\(EQUAL, a2\\), FilterList AND \\(2/2\\): \\[RowFilter \\(GREATER_OR_EQUAL, b5\\), RowFilter \\(LESS_OR_EQUAL, b6.*\""};
    final String[] excludedPlan ={};
    final String sqlHBase = canonizeHBaseSQL(sql);
    PlanTestBase.testPlanMatchingPatterns(sqlHBase, expectedPlan, excludedPlan);

  }

  @Test
  public void testDummyColumnsAreAvoided() throws Exception {
    setColumnWidth(10);
    // Key aspects:
    // - HBase columns c2 and c3 are referenced in the query
    // - column c2 appears in rows in one region but not in rows in a second
    //   region, and c3 appears only in the second region
    // - a downstream operation (e.g., sorting) doesn't handle schema changes
    final String sql = "SELECT\n"
        + "  row_key, \n"
        + "  t.f .c2, t.f .c3, \n"
        + "  t.f2.c2, t.f2.c3 \n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` t\n"
        + "WHERE\n"
        + "  row_key = 'a3' OR row_key = 'b7' \n"
        + "ORDER BY row_key";

    runHBaseSQLVerifyCount(sql, 2);
  }

  @Test
  public void testConvertFromPushDownWithView() throws Exception {
    test("create view dfs.tmp.pd_view as\n" +
       "select convert_from(byte_substr(row_key, 1, 8), 'date_epoch_be') as d\n" +
       "from hbase.`TestTableCompositeDate`");

    String query = "select d from dfs.tmp.pd_view where d > date '2015-06-13' and d < DATE '2015-06-18'";
    String[] expectedPlan = {
      "startRow=\"\\\\x00\\\\x00\\\\x01M\\\\xEF\\]\\\\xA0\\\\x00\", " +
      "stopRow=\"\\\\x00\\\\x00\\\\x01N\\\\x03\\\\xF7\\\\x10\\\\x00\""
    };
    String[] excludedPlan ={"Filter\\("};
    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    runHBaseSQLVerifyCount(query, 12);
  }
}

