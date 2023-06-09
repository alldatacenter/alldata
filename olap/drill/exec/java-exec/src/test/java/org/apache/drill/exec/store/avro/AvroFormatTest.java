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
package org.apache.drill.exec.store.avro;

import org.apache.commons.io.FileUtils;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.util.JsonStringHashMap;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.TestBuilder;
import org.apache.hadoop.fs.Path;
import org.joda.time.Period;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;
import static org.apache.drill.test.TestBuilder.mapOfObject;

/**
 * Unit tests for Avro record reader.
 */
public class AvroFormatTest extends ClusterTest {

  private static String mapTableName;
  private static AvroDataGenerator dataGenerator;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
    dataGenerator = new AvroDataGenerator(dirTestWatcher);
    // Create temporary table containing map and map array
    mapTableName = dataGenerator.generateMapSchema().getFileName();
  }

  @Test
  public void testSimplePrimitiveSchema_NoNullValues() throws Exception {
    AvroDataGenerator.AvroTestRecordWriter testSetup = dataGenerator.generateSimplePrimitiveSchema_NoNullValues();
    String file = testSetup.getFileName();
    String sql = "select a_string, b_int, c_long, d_float, e_double, f_bytes, h_boolean, g_null from dfs.`%s`";
    testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineRecords(testSetup.getExpectedRecords())
      .go();
  }

  @Test
  public void testSimplePrimitiveSchema_StarQuery() throws Exception {
    simpleAvroTestHelper(dataGenerator.generateSimplePrimitiveSchema_NoNullValues());
  }

  @Test
  public void testSimplePrimitiveSchema_SelectColumnSubset() throws Exception {
    AvroDataGenerator.AvroTestRecordWriter testSetup = dataGenerator.generateSimplePrimitiveSchema_NoNullValues();
    String file = testSetup.getFileName();
    List<String> projectList = Arrays.asList("`h_boolean`", "`e_double`");
    testBuilder()
      .sqlQuery("select h_boolean, e_double from dfs.`%s`", file)
      .unOrdered()
      .baselineRecords(project(testSetup.getExpectedRecords(), projectList))
      .go();
  }

  @Test
  public void testImplicitColumnsWithStar() throws Exception {
    AvroDataGenerator.AvroTestRecordWriter testWriter = dataGenerator.generateSimplePrimitiveSchema_NoNullValues(1);
    // removes "." and ".." from the path
    String tablePathString = new File(testWriter.getFilePath()).getCanonicalPath();
    Path tablePath = new Path(tablePathString);

    List<Map<String, Object>> expectedRecords = testWriter.getExpectedRecords();
    expectedRecords.get(0).put("`filename`", tablePath.getName());
    expectedRecords.get(0).put("`suffix`", "avro");
    expectedRecords.get(0).put("`fqn`", tablePath.toUri().getPath());
    expectedRecords.get(0).put("`filepath`", tablePath.getParent().toUri().getPath());
    try {
      testBuilder()
        .sqlQuery("select filename, *, suffix, fqn, filepath from dfs.`%s`", tablePath.getName())
        .unOrdered()
        .baselineRecords(expectedRecords)
        .go();
    } finally {
      FileUtils.deleteQuietly(new File(tablePath.toUri().getPath()));
    }
  }

  @Test
  public void testImplicitColumnAlone() throws Exception {
    AvroDataGenerator.AvroTestRecordWriter testWriter = dataGenerator.generateSimplePrimitiveSchema_NoNullValues(1);
    String file = testWriter.getFileName();
    // removes "." and ".." from the path
    String tablePath = new File(testWriter.getFilePath()).getCanonicalPath();
    try {
      testBuilder()
        .sqlQuery("select filename from dfs.`%s`", file)
        .unOrdered()
        .baselineColumns("filename")
        .baselineValues(file)
        .go();
    } finally {
      FileUtils.deleteQuietly(new File(tablePath));
    }
  }

  @Test
  public void testImplicitColumnInWhereClause() throws Exception {
    AvroDataGenerator.AvroTestRecordWriter testWriter = dataGenerator.generateSimplePrimitiveSchema_NoNullValues(1);
    String file = testWriter.getFileName();
    // removes "." and ".." from the path
    String tablePath = new File(testWriter.getFilePath()).getCanonicalPath();

    try {
      testBuilder()
        .sqlQuery("select * from dfs.`%1$s` where filename = '%1$s'", file)
        .unOrdered()
        .baselineRecords(testWriter.getExpectedRecords())
        .go();
    } finally {
      FileUtils.deleteQuietly(new File(tablePath));
    }
  }

  @Test
  public void testPartitionColumn() throws Exception {
    client.alterSession(ExecConstants.FILESYSTEM_PARTITION_COLUMN_LABEL, "directory");
    String file = "avroTable";
    String partitionColumn = "2018";
    String tablePath = FileUtils.getFile(file, partitionColumn).getPath();
    AvroDataGenerator.AvroTestRecordWriter testWriter = dataGenerator.generateSimplePrimitiveSchema_NoNullValues(1, tablePath);
    try {
      testBuilder()
        .sqlQuery("select directory0 from dfs.`%s`", file)
        .unOrdered()
        .baselineColumns("directory0")
        .baselineValues(partitionColumn)
        .go();
    } finally {
      client.resetSession(ExecConstants.FILESYSTEM_PARTITION_COLUMN_LABEL);
      FileUtils.deleteQuietly(new File(testWriter.getFilePath()));
    }
  }

  @Test
  public void testSimpleArraySchema_NoNullValues() throws Exception {
    String file = dataGenerator.generateSimpleArraySchema_NoNullValues().getFileName();
    String sql = "select a_string, c_string_array[0] as csa, e_float_array[2] as efa " +
      "from dfs.`%s` where a_string in ('a_0', 'a_15')";

    testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("a_string", "csa", "efa")
      .baselineValues("a_0", "c_string_array_0_0", 0.0F)
      .baselineValues("a_15", "c_string_array_15_0", 30.0F)
      .go();
  }

  @Test
  public void testSimpleArraySchema_StarQuery() throws Exception {
    simpleAvroTestHelper(dataGenerator.generateSimpleArraySchema_NoNullValues());
  }

  @Test
  public void testDoubleNestedSchema_NoNullValues_NotAllColumnsProjected() throws Exception {
    String file = dataGenerator.generateDoubleNestedSchema_NoNullValues().getFileName();
    String sql = "select a_string, t.c_record.nested_1_int as ni, " +
      "t.c_record.nested_1_record.double_nested_1_int as dni from dfs.`%s` t " +
      "where a_string in ('a_3', 'a_11')";

    testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("a_string", "ni", "dni")
      .baselineValues("a_3", 9, 27)
      .baselineValues("a_11", 121, 1331)
      .go();
  }

  @Test
  public void testSimpleNestedSchema_NoNullValues() throws Exception {
    String file = dataGenerator.generateSimpleNestedSchema_NoNullValues().getFileName();
    String sql = "select a_string, b_int, t.c_record.nested_1_string as ns," +
      " t.c_record.nested_1_int as ni from dfs.`%s` t where b_int in (6, 19)";

    testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("a_string", "b_int", "ns", "ni")
      .baselineValues("a_6", 6, "nested_1_string_6", 36)
      .baselineValues("a_19", 19, "nested_1_string_19", 361)
      .go();
  }

  @Test
  public void testSimpleNestedSchema_StarQuery() throws Exception {
    simpleAvroTestHelper(dataGenerator.generateSimpleNestedSchema_NoNullValues());
  }
  @Test
  public void testDoubleNestedSchema_NoNullValues() throws Exception {
    String file = dataGenerator.generateDoubleNestedSchema_NoNullValues().getFileName();
    String sql = "select a_string, b_int, t.c_record.nested_1_string as ns, t.c_record.nested_1_int as ni, " +
      "t.c_record.nested_1_record.double_nested_1_string as dns, t.c_record.nested_1_record.double_nested_1_int as dni " +
      "from dfs.`%s` t where b_int in (2, 14)";

    testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("a_string", "b_int", "ns", "ni", "dns", "dni")
      .baselineValues("a_2", 2, "nested_1_string_2", 4, "double_nested_1_string_2_2", 8)
      .baselineValues("a_14", 14, "nested_1_string_14", 196, "double_nested_1_string_14_14", 2744)
      .go();
  }

  @Test
  public void testDoubleNestedSchema_StarQuery() throws Exception {
    simpleAvroTestHelper(dataGenerator.generateDoubleNestedSchema_NoNullValues());
  }

  @Test
  public void testSimpleEnumSchema_NoNullValues() throws Exception {
    AvroDataGenerator.AvroTestRecordWriter testSetup = dataGenerator.generateSimpleEnumSchema_NoNullValues();
    String file = testSetup.getFileName();
    String sql = "select a_string, b_enum from dfs.`%s`";
    List<String> projectList = Arrays.asList("`a_string`", "`b_enum`");
    testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineRecords(project(testSetup.getExpectedRecords(), projectList))
      .go();
  }

  @Test
  public void testSimpleEnumSchema_StarQuery() throws Exception {
    simpleAvroTestHelper(dataGenerator.generateSimpleEnumSchema_NoNullValues());
  }

  @Test
  public void testSimpleUnionSchema_StarQuery() throws Exception {
    simpleAvroTestHelper(dataGenerator.generateUnionSchema_WithNullValues());
  }

  @Test
  public void testShouldFailSimpleUnionNonNullSchema_StarQuery() throws Exception {
    String file = dataGenerator.generateUnionSchema_WithNonNullValues().getFileName();

    thrown.expect(UserRemoteException.class);
    thrown.expectMessage("UNSUPPORTED_OPERATION ERROR");
    run("select * from dfs.`%s`", file);
  }

  @Test
  public void testNestedUnionSchema_withNullValues() throws Exception {
    String file = dataGenerator.generateUnionNestedSchema_withNullValues().getFileName();
    String sql = "select a_string, t.c_record.nested_1_string as ns, " +
      "t.c_record.nested_1_int as ni from dfs.`%s` t where a_string in ('a_0', 'a_1')";

    testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("a_string", "ns", "ni")
      .baselineValues("a_0", "nested_1_string_0", 0)
      .baselineValues("a_1", null, null)
      .go();
  }

  // DRILL-4574
  @Test
  public void testFlattenPrimitiveArray() throws Exception {
    String file = dataGenerator.generateSimpleArraySchema_NoNullValues().getFileName();
    String sql = "select a_string, flatten(c_string_array) as array_item from dfs.`%s` t";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("a_string", "array_item");

    for (int i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      for (int j = 0; j < AvroDataGenerator.ARRAY_SIZE; j++) {
        testBuilder.baselineValues("a_" + i, "c_string_array_" + i + "_" + j);
      }
    }
    testBuilder.go();
  }

  //DRILL-4574
  @Test
  public void testFlattenComplexArray() throws Exception {
    String file = dataGenerator.generateNestedArraySchema().getFileName();

    TestBuilder testBuilder = nestedArrayQueryTestBuilder(file);
    for (int i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      for (int j = 0; j < AvroDataGenerator.ARRAY_SIZE; j++) {
        testBuilder.baselineValues(i, j);
      }
    }
    testBuilder.go();
  }

  //DRILL-4574
  @Test
  public void testFlattenEmptyComplexArrayMustYieldNoResults() throws Exception {
    String file = dataGenerator.generateNestedArraySchema(AvroDataGenerator.RECORD_COUNT, 0).getFilePath();
    TestBuilder testBuilder = nestedArrayQueryTestBuilder(file);
    testBuilder.expectsEmptyResultSet();
  }

  @Test
  public void testNestedUnionArraySchema_withNullValues() throws Exception {
    String file = dataGenerator.generateUnionNestedArraySchema_withNullValues().getFileName();
    String sql = "select a_string, t.c_array[0].nested_1_string as ns, " +
      "t.c_array[0].nested_1_int as ni from dfs.`%s` t where a_string in ('a_2', 'a_3')";

    testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("a_string", "ns", "ni")
      .baselineValues("a_2", "nested_1_string_2", 4)
      .baselineValues("a_3", null, null)
      .go();
  }

  @Test
  public void testMapSchema_withNullValues() throws Exception {
    String file = dataGenerator.generateMapSchema_withNullValues().getFileName();
    String sql = "select a_string, c_map['key1'] as k1, c_map['key2'] as k2 " +
      "from dfs.`%s` where a_string in ('a_4', 'a_5')";

    testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("a_string", "k1", "k2")
      .baselineValues("a_4", "nested_1_string_4", "nested_1_string_5")
      .baselineValues("a_5", null, null)
      .go();
  }

  @Test
  public void testMapSchemaComplex_withNullValues() throws Exception {
    String file = dataGenerator.generateMapSchemaComplex_withNullValues().getFileName();
    String sql = "select d_map['key1'] nested_key1, d_map['key2'] nested_key2 from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("nested_key1", "nested_key2");

    List<Object> expectedList = new ArrayList<>();
    for (int i = 0; i < AvroDataGenerator.ARRAY_SIZE; i++) {
      expectedList.add((double) i);
    }
    List<Object> emptyList = listOf();
    for (int i = 0; i < AvroDataGenerator.RECORD_COUNT; i += 2) {
      testBuilder.baselineValues(expectedList, expectedList);
      testBuilder.baselineValues(emptyList, emptyList);
    }
    testBuilder.go();
  }

  @Test
  public void testStringAndUtf8Data() throws Exception {
    simpleAvroTestHelper(dataGenerator.generateStringAndUtf8Data());
  }

  @Test
  public void testLinkedList() throws Exception {
    int numRows = 5;
    String file = dataGenerator.generateLinkedList(numRows);

    TestBuilder testBuilder = testBuilder()
      .sqlQuery("select * from dfs.`%s` t", file)
      .unOrdered()
      .baselineColumns("value", "next");

    for (long i = 0; i < numRows; i++) {
      if (i == numRows - 1) { // last row
        testBuilder.baselineValues(i, mapOf("next", new JsonStringHashMap<>()));
        continue;
      }
      testBuilder.baselineValues(i, mapOf("value", i + 1, "next", new JsonStringHashMap<>()));
    }
    testBuilder.go();
  }

  @Test
  public void testCountStar() throws Exception {
    String file = dataGenerator.generateStringAndUtf8Data().getFileName();
    String sql = "select count(*) as row_count from dfs.`%s`";
    testBuilder()
      .sqlQuery(sql, file)
      .ordered()
      .baselineColumns("row_count")
      .baselineValues((long) AvroDataGenerator.RECORD_COUNT)
      .go();
  }

  @Test
  public void testMapSchema() throws Exception {
    String sql = "select map_field from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, mapTableName)
      .unOrdered()
      .baselineColumns("map_field");

    for (long i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      testBuilder.baselineValues(mapOfObject("key1", i, "key2", i + 1));
    }
    testBuilder.go();
  }

  @Test
  public void testMapSchemaGetByKey() throws Exception {
    String sql = "select map_field['key1'] val1, map_field['key2'] val2 from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, mapTableName)
      .unOrdered()
      .baselineColumns("val1", "val2");

    for (long i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      testBuilder.baselineValues(i, i + 1);
    }
    testBuilder.go();
  }

  @Test
  public void testMapSchemaGetByNotExistingKey() throws Exception {
    String sql = "select map_field['notExists'] as map_field from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, mapTableName)
      .unOrdered()
      .baselineColumns("map_field");

    Object[] nullValue = new Object[] {null};
    for (long i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      testBuilder.baselineValues(nullValue);
    }
    testBuilder.go();
  }

  @Test
  public void testMapSchemaGetByKeyUsingDotNotation() throws Exception {
    String sql = "select t.map_field.key1 val1, t.map_field.key2 val2 from dfs.`%s` t";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, mapTableName)
      .unOrdered()
      .baselineColumns("val1", "val2");

    for (long i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      testBuilder.baselineValues(i, i + 1);
    }
    testBuilder.go();
  }

  @Test
  public void testMapArraySchema() throws Exception {
    String sql = "select map_array from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, mapTableName)
      .unOrdered()
      .baselineColumns("map_array");


    for (int i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      List<Object> array = listOf();
      for (int j = 0; j < AvroDataGenerator.ARRAY_SIZE; j++) {
        array.add(mapOfObject(
            "key1", (i + 1) * (j + 50),
            "key2", (i + 1) * (j + 100)
        ));
      }
      testBuilder.baselineValues(array);
    }
    testBuilder.go();
  }

  @Test
  public void testArrayMapSchemaGetElementByIndex() throws Exception {
    int elementIndex = 1;
    String sql = "select map_array[%d] element from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, elementIndex, mapTableName)
      .unOrdered()
      .baselineColumns("element");

    for (int i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      testBuilder.baselineValues(mapOfObject(
          "key1", (i + 1) * (elementIndex + 50),
          "key2", (i + 1) * (elementIndex + 100)
      ));
    }
    testBuilder.go();
  }

  @Test
  public void testArrayMapSchemaElementGetByKey() throws Exception {
    int elementIndex = 1;
    String sql = "select map_array[%d]['key2'] val from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, elementIndex, mapTableName)
      .unOrdered()
      .baselineColumns("val");

    for (int i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      testBuilder.baselineValues((i + 1) * (elementIndex + 100));
    }
    testBuilder.go();
  }

  @Test
  public void testMapSchemaArrayValue() throws Exception {
    String sql = "select map_array_value from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, mapTableName)
      .unOrdered()
      .baselineColumns("map_array_value");

    for (int i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      List<Object> doubleArray = listOf();
      for (double j = 0; j < AvroDataGenerator.ARRAY_SIZE; j++) {
        doubleArray.add((double) (i + 1) * j);
      }
      testBuilder.baselineValues(mapOfObject("key1", doubleArray, "key2", doubleArray));
    }

    testBuilder.go();
  }

  @Test
  public void testMapSchemaArrayValueGetByKey() throws Exception {
    String sql = "select map_array_value['key1'] element from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, dataGenerator.generateMapSchema().getFileName())
      .unOrdered()
      .baselineColumns("element");

    for (int i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      List<Object> doubleArray = listOf();
      for (double j = 0; j < AvroDataGenerator.ARRAY_SIZE; j++) {
        doubleArray.add((double) (i + 1) * j);
      }
      testBuilder.baselineValues(doubleArray);
    }

    testBuilder.go();
  }

  @Test
  public void testMapSchemaArrayValueGetByKeyElementByIndex() throws Exception {
    String sql = "select map_array_value['key1'][3] element from dfs.`%s`";

    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, mapTableName)
      .unOrdered()
      .baselineColumns("element");

    for (int i = 0; i < AvroDataGenerator.RECORD_COUNT; i++) {
      double val = (double) (i + 1) * 3;
      testBuilder.baselineValues(val);
    }

    testBuilder.go();
  }

  @Test
  public void testMapSchemaValueInFilter() throws Exception {
    String sql = "select map_field['key1'] val from dfs.`%s` where map_field['key1'] < %d";

    long filterValue = AvroDataGenerator.RECORD_COUNT / 10;
    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, mapTableName, filterValue)
      .unOrdered()
      .baselineColumns("val");

    for (long i = 0; i < filterValue; i++) {
      testBuilder.baselineValues(i);
    }
    testBuilder.go();
  }

  @Test
  public void testMapSchemaValueInFilter2() throws Exception {
    String sql = "select map_array[%d]['key2'] val from dfs.`%s` where map_array[%d]['key2'] > %d";

    int elementIndex = 1;
    int startRecord = 30;
    int filterValue = startRecord * (elementIndex + 100);
    TestBuilder testBuilder = testBuilder()
      .sqlQuery(sql, elementIndex, mapTableName, elementIndex, filterValue)
      .unOrdered()
      .baselineColumns("val");

    for (int i = startRecord; i < AvroDataGenerator.RECORD_COUNT; i++) {
      testBuilder.baselineValues((i + 1) * (elementIndex + 100));
    }
    testBuilder.go();
  }

  @Test
  public void testDecimal() throws Exception {
    int numRows = 5;
    String fileName = dataGenerator.generateDecimalData(numRows);

    TestBuilder testBuilder = testBuilder()
      .sqlQuery("select * from dfs.`%s`", fileName)
      .unOrdered()
      .baselineColumns("col_dec_pos_bytes", "col_dec_neg_bytes", "col_dec_pos_fixed", "col_dec_neg_fixed");

    for (int i = 0; i < numRows; i++) {
      testBuilder.baselineValues(
        new BigDecimal(BigInteger.valueOf(100 + i), 2),
        new BigDecimal(BigInteger.valueOf(-200 + i), 2),
        new BigDecimal(BigInteger.valueOf(300 + i), 2),
        new BigDecimal(BigInteger.valueOf(-400 + i), 2));
    }
    testBuilder.go();
  }

  @Test
  public void testDateTime() throws Exception {
    LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("UTC")).withNano(0);
    LocalDate localDate = dateTime.toLocalDate();
    LocalTime localTime = dateTime.toLocalTime();

    String fileName = dataGenerator.generateDateTimeData(dateTime);

    testBuilder()
      .sqlQuery("select * from dfs.`%s`", fileName)
      .unOrdered()
      .baselineColumns("col_timestamp_millis", "col_timestamp_micros", "col_date", "col_time_millis", "col_time_micros")
      .baselineValues(dateTime, dateTime, localDate, localTime, localTime)
      .go();
  }

  @Test
  public void testDuration() throws Exception {
    int numRows = 5;
    String fileName = dataGenerator.generateDuration(numRows);

    TestBuilder testBuilder = testBuilder()
      .sqlQuery("select * from dfs.`%s`", fileName)
      .unOrdered()
      .baselineColumns("col_duration");

    for (int i = 0; i < numRows; i++) {
      testBuilder.baselineValues(Period.months(10 + i).withDays(100 + i).withMillis(1000 + i));
    }
    testBuilder.go();
  }

  @Test
  public void testMultiDimensionalArray() throws Exception {
    int numRecords = 5;
    int arraySize = 3;
    String fileName = dataGenerator.generateMultiDimensionalArray(numRecords, arraySize);

    TestBuilder testBuilder = testBuilder()
      .sqlQuery("select * from dfs.`%s`", fileName)
      .unOrdered()
      .baselineColumns("col_array_two_dims");

    for (int i = 0; i < numRecords; i++) {
      JsonStringArrayList<Object> nestedArray = new JsonStringArrayList<>();
      for (int a = 0; a < arraySize; a++) {
        nestedArray.add(listOf(String.format("val_%s_%s_0", i, a), String.format("val_%s_%s_1", i, a)));
      }
      testBuilder.baselineValues(nestedArray);
    }
    testBuilder.go();
  }

  @Test
  public void testWithProvidedSchema() throws Exception {
    testBuilder()
      .sqlQuery("select * from " +
          "table(dfs.`%s`(schema=>'inline=(col_i int not null default `15`, a_string varchar)')) " +
          "where a_string = 'a_0'",
        dataGenerator.generateStringAndUtf8Data().getFileName())
      .unOrdered()
      .baselineColumns("col_i", "a_string", "b_utf8")
      .baselineValues(15, "a_0", "b_0")
      .go();
  }

  private void simpleAvroTestHelper(AvroDataGenerator.AvroTestRecordWriter testSetup) throws Exception {
    testBuilder()
      .sqlQuery("select * from dfs.`%s`", testSetup.getFileName())
      .unOrdered()
      .baselineRecords(testSetup.getExpectedRecords())
      .go();
  }

  private List<Map<String, Object>> project(List<Map<String,Object>> incomingRecords, List<String> projectCols) {
    List<Map<String,Object>> output = new ArrayList<>();
    for (Map<String, Object> incomingRecord : incomingRecords) {
      JsonStringHashMap<String, Object> newRecord = new JsonStringHashMap<>();
      for (String s : incomingRecord.keySet()) {
        if (projectCols.contains(s)) {
          newRecord.put(s, incomingRecord.get(s));
        }
      }
      output.add(newRecord);
    }
    return output;
  }

  private TestBuilder nestedArrayQueryTestBuilder(String file) {
    String sql = "select rec_nr, array_item['nested_1_int'] as array_item_nested_int from "
      + "(select a_int as rec_nr, flatten(t.b_array) as array_item from dfs.`%s` t) a";

    return testBuilder()
      .sqlQuery(sql, file)
      .unOrdered()
      .baselineColumns("rec_nr", "array_item_nested_int");
  }
}
