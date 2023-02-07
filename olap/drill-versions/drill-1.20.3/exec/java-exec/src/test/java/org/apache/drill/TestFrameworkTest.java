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
package org.apache.drill;

import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.test.TestBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

// TODO - update framework to remove any dependency on the Drill engine
// for reading baseline result sets currently using it with the assumption
// that the csv and json readers are well tested, and handling diverse
// types in the test framework would require doing some redundant work
// to enable casting outside of Drill or some better tooling to generate
// parquet files that have all of the parquet types

public class TestFrameworkTest extends BaseTestQuery {

  private static String CSV_COLS = " cast(columns[0] as bigint) employee_id, columns[1] as first_name, columns[2] as last_name ";

  @Test(expected = AssertionError.class)
  public void testSchemaTestBuilderSetInvalidBaselineValues() throws Exception {
    final String query = "SELECT ltrim('drill') as col FROM (VALUES(1)) limit 0";

    List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList();
    TypeProtos.MajorType majorType = TypeProtos.MajorType.newBuilder()
            .setMinorType(TypeProtos.MinorType.VARCHAR)
            .setMode(TypeProtos.DataMode.REQUIRED)
            .build();
    expectedSchema.add(Pair.of(SchemaPath.getSimplePath("col"), majorType));

    testBuilder()
            .sqlQuery(query)
            .schemaBaseLine(expectedSchema)
            .baselineValues(new Object[0])
            .build()
            .run();
  }

  @Test(expected = AssertionError.class)
  public void testSchemaTestBuilderSetInvalidBaselineRecords() throws Exception {
    final String query = "SELECT ltrim('drill') as col FROM (VALUES(1)) limit 0";

    List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList();
    TypeProtos.MajorType majorType = TypeProtos.MajorType.newBuilder()
        .setMinorType(TypeProtos.MinorType.VARCHAR)
        .setMode(TypeProtos.DataMode.REQUIRED)
        .build();
    expectedSchema.add(Pair.of(SchemaPath.getSimplePath("col"), majorType));

    testBuilder()
        .sqlQuery(query)
        .schemaBaseLine(expectedSchema)
        .baselineRecords(Collections.<Map<String, Object>>emptyList())
        .build()
        .run();
  }

  @Test(expected = AssertionError.class)
  public void testSchemaTestBuilderSetInvalidBaselineColumns() throws Exception {
    final String query = "SELECT ltrim('drill') as col FROM (VALUES(1)) limit 0";

    List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList();
    TypeProtos.MajorType majorType = TypeProtos.MajorType.newBuilder()
            .setMinorType(TypeProtos.MinorType.VARCHAR)
            .setMode(TypeProtos.DataMode.REQUIRED)
            .build();
    expectedSchema.add(Pair.of(SchemaPath.getSimplePath("col"), majorType));

    testBuilder()
        .sqlQuery(query)
        .baselineColumns("col")
        .schemaBaseLine(expectedSchema)
        .build()
        .run();
  }

  @Test
  public void testCSVVerification() throws Exception {
    testBuilder()
        .sqlQuery("select employee_id, first_name, last_name from cp.`testframework/small_test_data.json`")
        .ordered()
        .csvBaselineFile("testframework/small_test_data.tsv")
        .baselineTypes(TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("employee_id", "first_name", "last_name")
        .build().run();
  }

  @Test
  public void testBaselineValsVerification() throws Exception {
    testBuilder()
        .sqlQuery("select employee_id, first_name, last_name from cp.`testframework/small_test_data.json` limit 1")
        .ordered()
        .baselineColumns("employee_id", "first_name", "last_name")
        .baselineValues(12l, "Jewel", "Creek")
        .build().run();

    testBuilder()
        .sqlQuery("select employee_id, first_name, last_name from cp.`testframework/small_test_data.json` limit 1")
        .unOrdered()
        .baselineColumns("employee_id", "first_name", "last_name")
        .baselineValues(12l, "Jewel", "Creek")
        .build().run();
  }

  @Test
  public void testDecimalBaseline() throws  Exception {
    try {
      test(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));

      // type information can be provided explicitly
      testBuilder()
          .sqlQuery("select cast(dec_col as decimal(38,2)) dec_col from cp.`testframework/decimal_test.json`")
          .unOrdered()
          .csvBaselineFile("testframework/decimal_test.tsv")
          .baselineTypes(Types.withPrecisionAndScale(TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.REQUIRED, 38, 2))
          .baselineColumns("dec_col")
          .build().run();

      // type information can also be left out, this will prompt the result types of the test query to drive the
      // interpretation of the test file
      testBuilder()
          .sqlQuery("select cast(dec_col as decimal(38,2)) dec_col from cp.`testframework/decimal_test.json`")
          .unOrdered()
          .csvBaselineFile("testframework/decimal_test.tsv")
          .baselineColumns("dec_col")
          .build().run();

      // Or you can provide explicit values to the builder itself to avoid going through the drill engine at all to
      // populate the baseline results
      testBuilder()
          .sqlQuery("select cast(dec_col as decimal(38,2)) dec_col from cp.`testframework/decimal_test.json`")
          .unOrdered()
          .baselineColumns("dec_col")
          .baselineValues(new BigDecimal("3.70"))
          .build().run();
    } finally {
      test(String.format("alter session set `%s` = false", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
    }
  }

  @Test
  public void testMapOrdering() throws Exception {
    testBuilder()
        .sqlQuery("select * from cp.`testframework/map_reordering.json`")
        .unOrdered()
        .jsonBaselineFile("testframework/map_reordering2.json")
        .build().run();
  }

  @Test
  public void testBaselineValsVerificationWithNulls() throws Exception {
    testBuilder()
        .sqlQuery("select * from cp.`store/json/json_simple_with_null.json`")
        .ordered()
        .baselineColumns("a", "b")
        .baselineValues(5l, 10l)
        .baselineValues(7l, null)
        .baselineValues(null, null)
        .baselineValues(9l, 11l)
        .build().run();

    testBuilder()
        .sqlQuery("select * from cp.`store/json/json_simple_with_null.json`")
        .unOrdered()
        .baselineColumns("a", "b")
        .baselineValues(5l, 10l)
        .baselineValues(9l, 11l)
        .baselineValues(7l, null)
        .baselineValues(null, null)
        .build().run();
  }

  @Test
  public void testBaselineValsVerificationWithComplexAndNulls() throws Exception {
    LocalDateTime localDT = LocalDateTime.of(2019, 9, 30, 20, 47, 43, 123);
    Instant instant = localDT.atZone(ZoneId.systemDefault()).toInstant();
    long ts = instant.toEpochMilli() + instant.getNano();
    ts = ts + ZoneId.systemDefault().getRules().getOffset(instant).getTotalSeconds() * 1000;
    testBuilder()
        .sqlQuery("select * from cp.`jsoninput/input2.json` limit 1")
        .ordered()
        .baselineColumns("integer", "float", "x", "z", "l", "rl", "`date`")
        .baselineValues(2010l,
                        17.4,
                        mapOf("y", "kevin",
                            "z", "paul"),
                        listOf(mapOf("orange", "yellow",
                                "pink", "red"),
                            mapOf("pink", "purple")),
                        listOf(4l, 2l),
                        listOf(listOf(2l, 1l),
                            listOf(4l, 6l)),
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()))
        .build().run();
  }

  @Test
  public void testCSVVerification_missing_records_fails() throws Exception {
    try {
    testBuilder()
        .sqlQuery("select employee_id, first_name, last_name from cp.`testframework/small_test_data.json`")
        .ordered()
        .csvBaselineFile("testframework/small_test_data_extra.tsv")
        .baselineTypes(TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("employee_id", "first_name", "last_name")
        .build().run();
    } catch (AssertionError ex) {
      assertEquals("Incorrect number of rows returned by query. expected:<7> but was:<5>", ex.getMessage());
      // this indicates successful completion of the test
      return;
    }
    throw new Exception("Test framework verification failed, expected failure on missing records.");
  }

  @Test
  public void testCSVVerification_extra_records_fails() throws Exception {
    try {
      testBuilder()
          .sqlQuery("select %s from cp.`testframework/small_test_data_extra.tsv`", CSV_COLS)
          .ordered()
          .csvBaselineFile("testframework/small_test_data.tsv")
          .baselineTypes(TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR)
          .baselineColumns("employee_id", "first_name", "last_name")
          .build().run();
    } catch (AssertionError ex) {
      assertEquals("Incorrect number of rows returned by query. expected:<5> but was:<7>", ex.getMessage());
      // this indicates successful completion of the test
      return;
    }
    throw new Exception("Test framework verification failed, expected failure for extra records.");
  }

  @Test
  public void testCSVVerification_extra_column_fails() throws Exception {
    try {
      testBuilder()
          .sqlQuery("select " + CSV_COLS + ", columns[3] as address from cp.`testframework/small_test_data_extra_col.tsv`")
          .ordered()
          .csvBaselineFile("testframework/small_test_data.tsv")
          .baselineTypes(TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR)
          .baselineColumns("employee_id", "first_name", "last_name")
          .build().run();
    } catch (AssertionError ex) {
      assertEquals("Unexpected extra column `address` returned by query.", ex.getMessage());
      // this indicates successful completion of the test
      return;
    }
    throw new Exception("Test framework verification failed, expected failure on extra column.");
  }

  @Test
  public void testCSVVerification_missing_column_fails() throws Exception {
    try {
      testBuilder()
          .sqlQuery("select employee_id, first_name, last_name from cp.`testframework/small_test_data.json`")
          .ordered()
          .csvBaselineFile("testframework/small_test_data_extra_col.tsv")
          .baselineTypes(TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR)
          .baselineColumns("employee_id", "first_name", "last_name", "address")
          .build().run();
    } catch (Exception ex) {
      assertTrue(ex.getMessage(), ex.getMessage().startsWith("Expected column(s) `address`,  not found in result set"));
      // this indicates successful completion of the test
      return;
    }
    throw new Exception("Test framework verification failed, expected failure on missing column.");
  }

  @Test
  public void testCSVVerificationOfTypes() throws Throwable {
    try {
    testBuilder()
        .sqlQuery("select employee_id, first_name, last_name from cp.`testframework/small_test_data.json`")
        .ordered()
        .csvBaselineFile("testframework/small_test_data.tsv")
        .baselineTypes(TypeProtos.MinorType.INT, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("employee_id", "first_name", "last_name")
        .build().run();
    } catch (Exception ex) {
      assertThat(ex.getMessage(), CoreMatchers.containsString(
          "at position 0 column '`employee_id`' mismatched values, expected: 12(Integer) but received 12(Long)"));
      // this indicates successful completion of the test
      return;
    }
    throw new Exception("Test framework verification failed, expected failure on type check.");
  }

  @Test
  public void testCSVVerificationOfOrder_checkFailure() throws Throwable {
    try {
      testBuilder()
          .sqlQuery("select columns[0] as employee_id, columns[1] as first_name, columns[2] as last_name from cp.`testframework/small_test_data_reordered.tsv`")
          .ordered()
          .csvBaselineFile("testframework/small_test_data.tsv")
          .baselineColumns("employee_id", "first_name", "last_name")
          .build().run();
    } catch (Exception ex) {
      assertThat(ex.getMessage(), CoreMatchers.containsString(
          "at position 0 column '`employee_id`' mismatched values, expected: 12(String) but received 16(String)"));
      // this indicates successful completion of the test
      return;
    }
    throw new Exception("Test framework verification failed, expected failure on order check.");
  }

  @Test
  public void testCSVVerificationOfUnorderedComparison() throws Throwable {
    testBuilder()
        .sqlQuery("select columns[0] as employee_id, columns[1] as first_name, columns[2] as last_name from cp.`testframework/small_test_data_reordered.tsv`")
        .unOrdered()
        .csvBaselineFile("testframework/small_test_data.tsv")
        .baselineColumns("employee_id", "first_name", "last_name")
        .build().run();
  }

  // TODO - enable more advanced type handling for JSON, currently basic support works
  // add support for type information taken from test query, or explicit type expectations
  @Test
  public void testBasicJSON() throws Exception {
    testBuilder()
        .sqlQuery("select * from cp.`scan_json_test_3.json`")
        .ordered()
        .jsonBaselineFile("/scan_json_test_3.json")
        .build().run();

    testBuilder()
        .sqlQuery("select * from cp.`scan_json_test_3.json`")
        .unOrdered() // Check other verification method with same files
        .jsonBaselineFile("/scan_json_test_3.json")
        .build().run();
  }

  @Test
  public void testComplexJSON_all_text() throws Exception {
    testBuilder()
        .sqlQuery("select * from cp.`store/json/schema_change_int_to_string.json`")
        .optionSettingQueriesForTestQuery("alter system set `store.json.all_text_mode` = true")
        .ordered()
        .jsonBaselineFile("store/json/schema_change_int_to_string.json")
        .optionSettingQueriesForBaseline("alter system set `store.json.all_text_mode` = true")
        .build().run();

    testBuilder()
        .sqlQuery("select * from cp.`store/json/schema_change_int_to_string.json`")
        .optionSettingQueriesForTestQuery("alter system set `store.json.all_text_mode` = true")
        .unOrdered() // Check other verification method with same files
        .jsonBaselineFile("store/json/schema_change_int_to_string.json")
        .optionSettingQueriesForBaseline("alter system set `store.json.all_text_mode` = true")
        .build().run();
    test("alter system set `store.json.all_text_mode` = false");
  }

  @Test
  public void testRepeatedColumnMatching() throws Exception {
    try {
      testBuilder()
          .sqlQuery("select * from cp.`store/json/schema_change_int_to_string.json`")
          .optionSettingQueriesForTestQuery("alter system set `store.json.all_text_mode` = true")
          .ordered()
          .jsonBaselineFile("testframework/schema_change_int_to_string_non-matching.json")
          .optionSettingQueriesForBaseline("alter system set `store.json.all_text_mode` = true")
          .build().run();
    } catch (Exception ex) {
      assertThat(ex.getMessage(), CoreMatchers.containsString(
          "at position 1 column '`field_1`' mismatched values, " +
          "expected: [\"5\",\"2\",\"3\",\"4\",\"1\",\"2\"](JsonStringArrayList) but received [\"5\"](JsonStringArrayList)"));
      // this indicates successful completion of the test
      return;
    }
    throw new Exception("Test framework verification failed, expected failure on order check.");
  }

  @Test
  public void testEmptyResultSet() throws Exception {
    testBuilder()
        .sqlQuery("select * from cp.`store/json/json_simple_with_null.json` where 1=0")
        .expectsEmptyResultSet()
        .build().run();
    try {
      testBuilder()
          .sqlQuery("select * from cp.`store/json/json_simple_with_null.json`")
          .expectsEmptyResultSet()
          .build().run();
    } catch (AssertionError ex) {
      assertEquals("Different number of records returned expected:<0> but was:<4>", ex.getMessage());
      // this indicates successful completion of the test
      return;
    }
    throw new Exception("Test framework verification failed, expected failure on unexpected records.");
  }

  @Test
  public void testCSVVerificationTypeMap() throws Throwable {
    Map<SchemaPath, TypeProtos.MajorType> typeMap = new HashMap<>();
    typeMap.put(TestBuilder.parsePath("first_name"), Types.optional(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("employee_id"), Types.optional(TypeProtos.MinorType.INT));
    typeMap.put(TestBuilder.parsePath("last_name"), Types.optional(TypeProtos.MinorType.VARCHAR));
    testBuilder()
        .sqlQuery("select cast(columns[0] as int) employee_id, columns[1] as first_name, columns[2] as last_name from cp.`testframework/small_test_data_reordered.tsv`")
        .unOrdered()
        .csvBaselineFile("testframework/small_test_data.tsv")
        .baselineColumns("employee_id", "first_name", "last_name")
        // This should work without this line because of the default type casts added based on the types that come out of the test query.
        // To write a test that enforces strict typing you must pass type information using a CSV with a list of types,
        // or any format with a Map of types like is constructed above and include the call to pass it into the test, which is commented out below
        //.baselineTypes(typeMap)
        .build().run();

    typeMap.clear();
    typeMap.put(TestBuilder.parsePath("first_name"), Types.optional(TypeProtos.MinorType.VARCHAR));
    // This is the wrong type intentionally to ensure failures happen when expected
    typeMap.put(TestBuilder.parsePath("employee_id"), Types.optional(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("last_name"), Types.optional(TypeProtos.MinorType.VARCHAR));

    try {
    testBuilder()
        .sqlQuery("select cast(columns[0] as int) employee_id, columns[1] as first_name, columns[2] as last_name from cp.`testframework/small_test_data_reordered.tsv`")
        .unOrdered()
        .csvBaselineFile("testframework/small_test_data.tsv")
        .baselineColumns("employee_id", "first_name", "last_name")
        .baselineTypes(typeMap)
        .build().run();
    } catch (Exception ex) {
      // this indicates successful completion of the test
      return;
    }
    throw new Exception("Test framework verification failed, expected failure on type check.");
  }

}
