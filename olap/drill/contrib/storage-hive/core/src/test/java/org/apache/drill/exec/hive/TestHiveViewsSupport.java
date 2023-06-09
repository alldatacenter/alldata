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
package org.apache.drill.exec.hive;

import java.math.BigDecimal;

import org.apache.drill.categories.HiveStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

@Category({SlowTest.class, HiveStorageTest.class})
public class TestHiveViewsSupport extends HiveTestBase {

  @Test
  public void selectStarFromView() throws Exception {
    testBuilder()
        .sqlQuery("SELECT * FROM hive.hive_view")
        .unOrdered()
        .baselineColumns("key", "value")
        .baselineValues(1, " key_1")
        .baselineValues(2, " key_2")
        .baselineValues(3, " key_3")
        .baselineValues(4, " key_4")
        .baselineValues(5, " key_5")
        .go();
  }

  @Test
  public void useHiveAndSelectStarFromView() throws Exception {
    test("USE hive");
    testBuilder()
        .sqlQuery("SELECT * FROM hive_view")
        .unOrdered()
        .baselineColumns("key", "value")
        .baselineValues(1, " key_1")
        .baselineValues(2, " key_2")
        .baselineValues(3, " key_3")
        .baselineValues(4, " key_4")
        .baselineValues(5, " key_5")
        .go();
  }

  @Test
  public void joinViewAndTable() throws Exception {
    testBuilder()
        .sqlQuery("SELECT v.key AS key, t.`value` AS val " +
            "FROM hive.kv t " +
            "INNER JOIN hive.hive_view v " +
            "ON v.key = t.key AND t.key=1")
        .unOrdered()
        .baselineColumns("key", "val")
        .baselineValues(1, " key_1")
        .go();
  }

  @Test
  public void nativeParquetScanForView() throws Exception {
    try {
      setSessionOption(ExecConstants.HIVE_OPTIMIZE_PARQUET_SCAN_WITH_NATIVE_READER, true);
      setSessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);

      String query = "select * from hive.kv_native_view where key > 1";

      int actualRowCount = testSql(query);
      assertEquals("Expected and actual row count should match", 2, actualRowCount);

      testPlanMatchingPatterns(query,
          new String[]{"HiveDrillNativeParquetScan", "numFiles=1"}, null);

    } finally {
      resetSessionOption(ExecConstants.HIVE_OPTIMIZE_PARQUET_SCAN_WITH_NATIVE_READER);
      resetSessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }

  }

  @Test
  public void viewWithAllSupportedDataTypes() throws Exception {
    testBuilder().sqlQuery("SELECT * FROM hive.readtest_view")
        .unOrdered()
        .baselineColumns(
            "binary_field",
            "boolean_field",
            "tinyint_field",
            "decimal0_field",
            "decimal9_field",
            "decimal18_field",
            "decimal28_field",
            "decimal38_field",
            "double_field",
            "float_field",
            "int_field",
            "bigint_field",
            "smallint_field",
            "string_field",
            "varchar_field",
            "timestamp_field",
            "date_field",
            "char_field",
            // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
            //"binary_part",
            "boolean_part",
            "tinyint_part",
            "decimal0_part",
            "decimal9_part",
            "decimal18_part",
            "decimal28_part",
            "decimal38_part",
            "double_part",
            "float_part",
            "int_part",
            "bigint_part",
            "smallint_part",
            "string_part",
            "varchar_part",
            "timestamp_part",
            "date_part",
            "char_part")
        .baselineValues(
            "binaryfield".getBytes(),
            false,
            34,
            new BigDecimal("66"),
            new BigDecimal("2347.92"),
            new BigDecimal("2758725827.99990"),
            new BigDecimal("29375892739852.8"),
            new BigDecimal("89853749534593985.783"),
            8.345d,
            4.67f,
            123456,
            234235L,
            3455,
            "stringfield",
            "varcharfield",
            DateUtility.parseBest("2013-07-05 17:01:00"),
            DateUtility.parseLocalDate("2013-07-05"),
            "charfield",
            // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
            //"binary",
            true,
            64,
            new BigDecimal("37"),
            new BigDecimal("36.90"),
            new BigDecimal("3289379872.94565"),
            new BigDecimal("39579334534534.4"),
            new BigDecimal("363945093845093890.900"),
            8.345d,
            4.67f,
            123456,
            234235L,
            3455,
            "string",
            "varchar",
            DateUtility.parseBest("2013-07-05 17:01:00"),
            DateUtility.parseLocalDate("2013-07-05"),
            "char")
        .baselineValues( // All fields are null, but partition fields have non-null values
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            // There is a regression in Hive 1.2.1 in binary type partition columns. Disable for now.
            //"binary",
            true,
            64,
            new BigDecimal("37"),
            new BigDecimal("36.90"),
            new BigDecimal("3289379872.94565"),
            new BigDecimal("39579334534534.4"),
            new BigDecimal("363945093845093890.900"),
            8.345d,
            4.67f,
            123456,
            234235L,
            3455,
            "string",
            "varchar",
            DateUtility.parseBest("2013-07-05 17:01:00"),
            DateUtility.parseLocalDate("2013-07-05"),
            "char")
        .go();
  }

  @Test
  public void viewOverView() throws Exception {
    testBuilder()
        .sqlQuery("SELECT * FROM hive.view_over_hive_view")
        .unOrdered()
        .baselineColumns("key", "value")
        .baselineValues(2, " key_2")
        .baselineValues(3, " key_3")
        .go();
  }

  @Test
  public void materializedViews() throws Exception {
    testBuilder()
        .sqlQuery("SELECT * FROM hive.hive_view_m")
        .unOrdered()
        .baselineColumns("key", "value")
        .baselineValues(1, " key_1")
        .go();
  }

  @Test
  public void viewOverTablesInDifferentSchema() throws Exception {
    testBuilder()
        .sqlQuery("SELECT dk_key_count FROM hive.db1.two_table_view")
        .unOrdered()
        .baselineColumns("dk_key_count")
        .baselineValues(5L)
        .go();
  }

}
