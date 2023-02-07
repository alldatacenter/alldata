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
package org.apache.drill.exec.store.parquet2;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.util.Text;
import org.apache.drill.test.BaseTestQuery;
import org.joda.time.Period;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.drill.exec.expr.fn.impl.DateUtility.parseLocalDate;
import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;
import static org.apache.drill.test.TestBuilder.mapOfObject;

@Category({ParquetTest.class, UnlikelyTest.class})
public class TestDrillParquetReader extends BaseTestQuery {
  // enable decimal data type and make sure DrillParquetReader is used to handle test queries
  @BeforeClass
  public static void setup() throws Exception {
    alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
    alterSession(ExecConstants.PARQUET_NEW_RECORD_READER, true);
  }

  @AfterClass
  public static void cleanup() throws Exception {
    resetSessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    resetSessionOption(ExecConstants.PARQUET_NEW_RECORD_READER);
  }

  private void testColumn(String columnName) throws Exception {
    BigDecimal result = new BigDecimal("1.20000000");

    testBuilder()
      .ordered()
      .sqlQuery("select %s from cp.`parquet2/decimal28_38.parquet`", columnName)
      .baselineColumns(columnName)
      .baselineValues(result)
      .go();
  }

  @Test
  public void testRequiredDecimal28() throws Exception {
    testColumn("d28_req");
  }

  @Test
  public void testRequiredDecimal38() throws Exception {
    testColumn("d38_req");
  }

  @Test
  public void testOptionalDecimal28() throws Exception {
    testColumn("d28_opt");
  }

  @Test
  public void testOptionalDecimal38() throws Exception {
    testColumn("d38_opt");
  }

  @Test
  public void test4349() throws Exception {
    // start by creating a parquet file from the input csv file
    runSQL("CREATE TABLE dfs.tmp.`4349` AS SELECT columns[0] id, CAST(NULLIF(columns[1], '') AS DOUBLE) val FROM cp.`parquet2/4349.csv.gz`");

    // querying the parquet file should return the same results found in the csv file
    testBuilder()
      .unOrdered()
      .sqlQuery("SELECT * FROM dfs.tmp.`4349` WHERE id = 'b'")
      .sqlBaselineQuery("SELECT columns[0] id, CAST(NULLIF(columns[1], '') AS DOUBLE) val FROM cp.`parquet2/4349.csv.gz` WHERE columns[0] = 'b'")
      .go();
  }

  @Test
  public void testUnsignedAndSignedIntTypes() throws Exception {
    testBuilder()
      .unOrdered()
      .sqlQuery("select * from cp.`parquet/uint_types.parquet`")
      .baselineColumns("uint8_field", "uint16_field", "uint32_field", "uint64_field", "int8_field", "int16_field",
        "required_uint8_field", "required_uint16_field", "required_uint32_field", "required_uint64_field",
        "required_int8_field", "required_int16_field")
      .baselineValues(255, 65535, 2147483647, 9223372036854775807L, 255, 65535, -1, -1, -1, -1L, -2147483648, -2147483648)
      .baselineValues(-1, -1, -1, -1L, -2147483648, -2147483648, 255, 65535, 2147483647, 9223372036854775807L, 255, 65535)
      .baselineValues(null, null, null, null, null, null, 0, 0, 0, 0L, 0, 0)
      .go();
  }

  @Test
  public void testLogicalIntTypes() throws Exception {
    String query = String.format("select " +
        "t.uint_64 as uint_64, t.uint_32 as uint_32, t.uint_16 as uint_16, t.uint_8 as uint_8,  " +
        "t.int_64 as int_64, t.int_32 as int_32, t.int_16 as int_16, t.int_8 as int_8  " +
        "from cp.`parquet/logical_int.parquet` t" );
    String[] columns = {"uint_64", "uint_32", "uint_16", "uint_8", "int_64", "int_32", "int_16", "int_8" };
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns(columns)
        .baselineValues(0L, 0, 0, 0, 0L, 0, 0, 0)
        .baselineValues(-1L, -1, -1, -1, -1L, -1, -1, -1)
        .baselineValues(1L, 1, 1, 1, -9223372036854775808L, 1, 1, 1)
        .baselineValues(9223372036854775807L, 2147483647, 65535, 255, 9223372036854775807L, -2147483648, -32768, -128)
        .build()
        .run();
  }

  @Test //DRILL-5971
  public void testLogicalIntTypes2() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    Arrays.fill(bytesOnes, (byte)1);
    byte[] bytesZeros = new byte[12];
    String query = String.format(
        " select " +
            " t.rowKey as rowKey, " +
            " t._UTF8 as _UTF8, " +
            " t._Enum as _Enum, " +
            " t._INT32_RAW as _INT32_RAW, " +
            " t._INT_8 as _INT_8, " +
            " t._INT_16 as _INT_16, " +
            " t._INT_32 as _INT_32, " +
            " t._UINT_8 as _UINT_8, " +
            " t._UINT_16 as _UINT_16, " +
            " t._UINT_32 as _UINT_32, " +
            " t._INT64_RAW as _INT64_RAW, " +
            " t._INT_64 as _INT_64, " +
            " t._UINT_64 as _UINT_64, " +
            " t._DATE_int32 as _DATE_int32, " +
            " t._TIME_MILLIS_int32 as _TIME_MILLIS_int32, " +
            " t._TIMESTAMP_MILLIS_int64 as _TIMESTAMP_MILLIS_int64, " +
            " t._TIMESTAMP_MICROS_int64 as _TIMESTAMP_MICROS_int64, " +
            " t._INTERVAL_fixed_len_byte_array_12 as _INTERVAL_fixed_len_byte_array_12, " +
            " t._INT96_RAW as _INT96_RAW " +
            " from " +
            " cp.`parquet/parquet_logical_types_simple.parquet` t " +
            " order by t.rowKey "
    );
    String[] columns = {
        "rowKey ",
        "_UTF8",
        "_Enum",
        "_INT32_RAW",
        "_INT_8",
        "_INT_16",
        "_INT_32",
        "_UINT_8",
        "_UINT_16",
        "_UINT_32",
        "_INT64_RAW",
        "_INT_64",
        "_UINT_64",
        "_DATE_int32",
        "_TIME_MILLIS_int32",
        "_TIMESTAMP_MILLIS_int64",
        "_TIMESTAMP_MICROS_int64",
        "_INTERVAL_fixed_len_byte_array_12",
        "_INT96_RAW"

    };
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns(columns)
        .baselineValues(1, "UTF8 string1", "RANDOM_VALUE", 1234567, 123, 12345, 1234567, 123, 1234, 1234567,
            1234567890123456L, 1234567890123456L, 1234567890123456L, LocalDate.parse("5350-02-17"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"),
            LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"),
            LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"),
            LocalDateTime.of(1970, 1, 1, 0, 0, 0),new Period("PT0S"), bytesZeros)
        .build()
        .run();
  }

  @Test //DRILL-5971
  public void testLogicalIntTypes3() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    Arrays.fill(bytesOnes, (byte)1);
    byte[] bytesZeros = new byte[12];
    String query = String.format(
        " select " +
            " t.rowKey as rowKey, " +
            " t._UTF8 as _UTF8, " +
            " t._Enum as _Enum, " +
            " t._INT32_RAW as _INT32_RAW, " +
            " t._INT_8 as _INT_8, " +
            " t._INT_16 as _INT_16, " +
            " t._INT_32 as _INT_32, " +
            " t._UINT_8 as _UINT_8, " +
            " t._UINT_16 as _UINT_16, " +
            " t._UINT_32 as _UINT_32, " +
            " t._INT64_RAW as _INT64_RAW, " +
            " t._INT_64 as _INT_64, " +
            " t._UINT_64 as _UINT_64, " +
            " t._DATE_int32 as _DATE_int32, " +
            " t._TIME_MILLIS_int32 as _TIME_MILLIS_int32, " +
            " t._TIMESTAMP_MILLIS_int64 as _TIMESTAMP_MILLIS_int64, " +
            " t._TIMESTAMP_MICROS_int64 as _TIMESTAMP_MICROS_int64, " +
            " t._INTERVAL_fixed_len_byte_array_12 as _INTERVAL_fixed_len_byte_array_12, " +
            " t._INT96_RAW as _INT96_RAW " +
            " from " +
            " cp.`parquet/parquet_logical_types_simple_nullable.parquet` t " +
            " order by t.rowKey "
    );
    String[] columns = {
        "rowKey ",
        "_UTF8",
        "_Enum",
        "_INT32_RAW",
        "_INT_8",
        "_INT_16",
        "_INT_32",
        "_UINT_8",
        "_UINT_16",
        "_UINT_32",
        "_INT64_RAW",
        "_INT_64",
        "_UINT_64",
        "_DATE_int32",
        "_TIME_MILLIS_int32",
        "_TIMESTAMP_MILLIS_int64",
        "_TIMESTAMP_MICROS_int64",
        "_INTERVAL_fixed_len_byte_array_12",
        "_INT96_RAW"

    };
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns(columns)
        .baselineValues(1, "UTF8 string1", "RANDOM_VALUE", 1234567, 123, 12345, 1234567, 123, 1234, 1234567,
            1234567890123456L, 1234567890123456L, 1234567890123456L, LocalDate.parse("5350-02-17"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0),
            new Period("PT0S"), bytesZeros)
        .baselineValues(4, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null)
        .build().run();
  }

  @Test //DRILL-6670: include tests on data with dictionary encoding disabled
  public void testLogicalIntTypes4() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    Arrays.fill(bytesOnes, (byte)1);
    byte[] bytesZeros = new byte[12];
    String query = String.format(
        " select " +
            " t.rowKey as rowKey, " +
            " t._UTF8 as _UTF8, " +
            " t._Enum as _Enum, " +
            " t._INT32_RAW as _INT32_RAW, " +
            " t._INT_8 as _INT_8, " +
            " t._INT_16 as _INT_16, " +
            " t._INT_32 as _INT_32, " +
            " t._UINT_8 as _UINT_8, " +
            " t._UINT_16 as _UINT_16, " +
            " t._UINT_32 as _UINT_32, " +
            " t._INT64_RAW as _INT64_RAW, " +
            " t._INT_64 as _INT_64, " +
            " t._UINT_64 as _UINT_64, " +
            " t._DATE_int32 as _DATE_int32, " +
            " t._TIME_MILLIS_int32 as _TIME_MILLIS_int32, " +
            " t._TIMESTAMP_MILLIS_int64 as _TIMESTAMP_MILLIS_int64, " +
            " t._TIMESTAMP_MICROS_int64 as _TIMESTAMP_MICROS_int64, " +
            " t._INTERVAL_fixed_len_byte_array_12 as _INTERVAL_fixed_len_byte_array_12, " +
            " t._INT96_RAW as _INT96_RAW " +
            " from " +
            " cp.`parquet/parquet_logical_types_simple_nodict.parquet` t " +
            " order by t.rowKey "
    );
    String[] columns = {
        "rowKey ",
        "_UTF8",
        "_Enum",
        "_INT32_RAW",
        "_INT_8",
        "_INT_16",
        "_INT_32",
        "_UINT_8",
        "_UINT_16",
        "_UINT_32",
        "_INT64_RAW",
        "_INT_64",
        "_UINT_64",
        "_DATE_int32",
        "_TIME_MILLIS_int32",
        "_TIMESTAMP_MILLIS_int64",
        "_TIMESTAMP_MICROS_int64",
        "_INTERVAL_fixed_len_byte_array_12",
        "_INT96_RAW"

    };
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns(columns)
        .baselineValues(1, "UTF8 string1", "RANDOM_VALUE", 1234567, 123, 12345, 1234567, 123, 1234, 1234567,
            1234567890123456L, 1234567890123456L, 1234567890123456L, LocalDate.parse("5350-02-17"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .build()
        .run();
  }

  @Test //DRILL-6670: include tests on data with dictionary encoding disabled
  public void testLogicalIntTypes5() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    Arrays.fill(bytesOnes, (byte)1);
    byte[] bytesZeros = new byte[12];
    String query = String.format(
        " select " +
            " t.rowKey as rowKey, " +
            " t._UTF8 as _UTF8, " +
            " t._Enum as _Enum, " +
            " t._INT32_RAW as _INT32_RAW, " +
            " t._INT_8 as _INT_8, " +
            " t._INT_16 as _INT_16, " +
            " t._INT_32 as _INT_32, " +
            " t._UINT_8 as _UINT_8, " +
            " t._UINT_16 as _UINT_16, " +
            " t._UINT_32 as _UINT_32, " +
            " t._INT64_RAW as _INT64_RAW, " +
            " t._INT_64 as _INT_64, " +
            " t._UINT_64 as _UINT_64, " +
            " t._DATE_int32 as _DATE_int32, " +
            " t._TIME_MILLIS_int32 as _TIME_MILLIS_int32, " +
            " t._TIMESTAMP_MILLIS_int64 as _TIMESTAMP_MILLIS_int64, " +
            " t._TIMESTAMP_MICROS_int64 as _TIMESTAMP_MICROS_int64, " +
            " t._INTERVAL_fixed_len_byte_array_12 as _INTERVAL_fixed_len_byte_array_12, " +
            " t._INT96_RAW as _INT96_RAW " +
            " from " +
            " cp.`parquet/parquet_logical_types_simple_nullable_nodict.parquet` t " +
            " order by t.rowKey "
    );
    String[] columns = {
        "rowKey ",
        "_UTF8",
        "_Enum",
        "_INT32_RAW",
        "_INT_8",
        "_INT_16",
        "_INT_32",
        "_UINT_8",
        "_UINT_16",
        "_UINT_32",
        "_INT64_RAW",
        "_INT_64",
        "_UINT_64",
        "_DATE_int32",
        "_TIME_MILLIS_int32",
        "_TIMESTAMP_MILLIS_int64",
        "_TIMESTAMP_MICROS_int64",
        "_INTERVAL_fixed_len_byte_array_12",
        "_INT96_RAW"

    };
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns(columns)
        .baselineValues(1, "UTF8 string1", "RANDOM_VALUE", 1234567, 123, 12345, 1234567, 123, 1234, 1234567,
            1234567890123456L, 1234567890123456L, 1234567890123456L, LocalDate.parse("5350-02-17"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .baselineValues(4, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null)
        .build().run();
  }

  @Test // DRILL-6856
  public void testIsTrueOrNullCondition() throws Exception {
    testBuilder()
        .sqlQuery("SELECT col_bln " +
            "FROM cp.`parquetFilterPush/blnTbl/0_0_2.parquet` " +
            "WHERE col_bln IS true OR col_bln IS null " +
            "ORDER BY col_bln")
        .ordered()
        .baselineColumns("col_bln")
        .baselineValuesForSingleColumn(true, null)
        .go();
  }

  @Test
  public void hiveIntArray() throws Exception {
    // Nesting 0: reading ARRAY<INT>
    testBuilder()
        .sqlQuery("SELECT int_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("int_arr_n_0")
        .baselineValuesForSingleColumn(asList(-1, 0, 1))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(Collections.singletonList(100500))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<INT>>
    testBuilder()
        .sqlQuery("SELECT int_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("int_arr_n_1")
        .baselineValuesForSingleColumn(asList(asList(-1, 0, 1), asList(-2, 1)))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(100500, 500100)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<INT>>>
    testBuilder()
        .sqlQuery("SELECT int_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("int_arr_n_2")
        .baselineValues(asList(
            asList(asList(7, 81), asList(-92, 54, -83), asList(-10, -59)),
            asList(asList(-43, -80)),
            asList(asList(-70, -62))
        ))
        .baselineValues(asList(
            asList(asList(34, -18)),
            asList(asList(-87, 87), asList(52, 58), asList(58, 20, -81), asList(-94, -93))
        ))
        .baselineValues(asList(
            asList(asList(-56, 9), asList(39, 5)),
            asList(asList(28, 88, -28))
        ))
        .go();
  }

  @Test
  public void hiveBooleanArray() throws Exception {
    // Nesting 0: reading ARRAY<BOOLEAN>
    testBuilder()
        .sqlQuery("SELECT boolean_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("boolean_arr_n_0")
        .baselineValuesForSingleColumn(asList(false, true, false, true, false))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(Collections.singletonList(true))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<BOOLEAN>>
    testBuilder()
        .sqlQuery("SELECT boolean_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("boolean_arr_n_1")
        .baselineValuesForSingleColumn(asList(asList(true, false, true), asList(false, false)))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(false, true)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<BOOLEAN>>>
    testBuilder()
        .sqlQuery("SELECT boolean_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("boolean_arr_n_2")
        .baselineValues(asList(
            asList(asList(false, true)),
            asList(asList(true), asList(false, true), asList(true), asList(true)),
            asList(asList(false), asList(true, false, false), asList(true, true), asList(false, true, false)),
            asList(asList(false, true), asList(true, false), asList(true, false, true)),
            asList(asList(false), asList(false), asList(false))
        ))
        .baselineValues(asList(
            asList(asList(false, true), asList(false), asList(false, false), asList(true, true, true), asList(false)),
            asList(asList(false, false, true)),
            asList(asList(false, true), asList(true, false))
        ))
        .baselineValues(asList(
            asList(asList(true, true), asList(false, true, false), asList(true), asList(true, true, false)),
            asList(asList(false), asList(false, true), asList(false), asList(false)),
            asList(asList(true, true, true), asList(true, true, true), asList(false), asList(false)),
            asList(asList(false, false))
        ))
        .go();
  }

  @Test
  public void hiveCharArray() throws Exception {
    // Nesting 0: reading ARRAY<CHAR(2)>
    testBuilder()
        .sqlQuery("SELECT char_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("char_arr_n_0")
        .baselineValuesForSingleColumn(asList(new Text("aa"), new Text("cc"), new Text("ot")))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(asList(new Text("+a"), new Text("-c"), new Text("*t")))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<CHAR(2)>>
    testBuilder()
        .sqlQuery("SELECT char_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("char_arr_n_1")
        .baselineValuesForSingleColumn(asList(
            asList(new Text("aa")),
            asList(new Text("cc"), new Text("ot"))))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(new Text("*t"))))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<CHAR(2)>>>
    testBuilder()
        .sqlQuery("SELECT char_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("char_arr_n_2")
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(new Text("eT"))//[0][0]
                ),
                asList( // [1]
                    asList(new Text("w9"), new Text("fC"), new Text("ww")),//[1][0]
                    asList(new Text("3o"), new Text("f7"), new Text("Za")),//[1][1]
                    asList(new Text("lX"), new Text("iv"), new Text("jI"))//[1][2]
                ),
                asList( // [2]
                    asList(new Text("S3"), new Text("Qa"), new Text("aG")),//[2][0]
                    asList(new Text("bj"), new Text("gc"), new Text("NO"))//[2][1]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(new Text("PV"), new Text("tH"), new Text("B7")),//[0][0]
                    asList(new Text("uL")),//[0][1]
                    asList(new Text("7b"), new Text("uf")),//[0][2]
                    asList(new Text("zj")),//[0][3]
                    asList(new Text("sA"), new Text("hf"), new Text("hR"))//[0][4]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(new Text("W1"), new Text("FS")),//[0][0]
                    asList(new Text("le"), new Text("c0")),//[0][1]
                    asList(new Text(""), new Text("0v"))//[0][2]
                ),
                asList( // [1]
                    asList(new Text("gj"))//[1][0]
                )
            )
        )
        .go();
  }

  @Test
  public void hiveBigintArray() throws Exception {
    // Nesting 0: reading ARRAY<BIGINT>
    testBuilder()
        .sqlQuery("SELECT bigint_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("bigint_arr_n_0")
        .baselineValuesForSingleColumn(asList(-9223372036854775808L, 0L, 10000000010L, 9223372036854775807L))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(Collections.singletonList(10005000L))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<BIGINT>>
    testBuilder()
        .sqlQuery("SELECT bigint_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("bigint_arr_n_1")
        .baselineValuesForSingleColumn(asList(
            asList(-9223372036854775808L, 0L, 10000000010L),
            asList(9223372036854775807L, 9223372036854775807L)))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(10005000L, 100050010L)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<BIGINT>>>
    testBuilder()
        .sqlQuery("SELECT bigint_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("bigint_arr_n_2")
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(7345032157033769004L),//[0][0]
                    asList(-2306607274383855051L, 3656249581579032003L)//[0][1]
                ),
                asList( // [1]
                    asList(6044100897358387146L, 4737705104728607904L)//[1][0]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(4833583793282587107L, -8917877693351417844L, -3226305034926780974L)//[0][0]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(8679405200896733338L, 8581721713860760451L, 1150622751848016114L),//[0][0]
                    asList(-6672104994192826124L, 4807952216371616134L),//[0][1]
                    asList(-7874492057876324257L)//[0][2]
                ),
                asList( // [1]
                    asList(8197656735200560038L),//[1][0]
                    asList(7643173300425098029L, -3186442699228156213L, -8370345321491335247L),//[1][1]
                    asList(8781633305391982544L, -7187468334864189662L)//[1][2]
                ),
                asList( // [2]
                    asList(6685428436181310098L),//[2][0]
                    asList(1358587806266610826L),//[2][1]
                    asList(-2077124879355227614L, -6787493227661516341L),//[2][2]
                    asList(3713296190482954025L, -3890396613053404789L),//[2][3]
                    asList(4636761050236625699L, 5268453104977816600L)//[2][4]
                )
            )
        )
        .go();
  }

  @Test
  public void hiveDateArray() throws Exception {

    // Nesting 0: reading ARRAY<DATE>
    testBuilder()
        .sqlQuery("SELECT date_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("date_arr_n_0")
        .baselineValuesForSingleColumn(asList(
            parseLocalDate("2018-10-21"),
            parseLocalDate("2017-07-11"),
            parseLocalDate("2018-09-23")))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(asList(parseLocalDate("2018-07-14")))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<DATE>>
    testBuilder()
        .sqlQuery("SELECT date_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("date_arr_n_1")
        .baselineValuesForSingleColumn(asList(
            asList(parseLocalDate("2017-03-21"), parseLocalDate("2017-09-10"), parseLocalDate("2018-01-17")),
            asList(parseLocalDate("2017-03-24"), parseLocalDate("2018-09-22"))))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(parseLocalDate("2017-08-09"), parseLocalDate("2017-08-28"))))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<DATE>>>
    testBuilder()
        .sqlQuery("SELECT date_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("date_arr_n_2")
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(parseLocalDate("1952-08-24")),//[0][0]
                    asList(parseLocalDate("1968-10-05"), parseLocalDate("1951-07-27")),//[0][1]
                    asList(parseLocalDate("1943-11-18"), parseLocalDate("1991-04-27"))//[0][2]
                ),
                asList( // [1]
                    asList(parseLocalDate("1981-12-27"), parseLocalDate("1984-02-03")),//[1][0]
                    asList(parseLocalDate("1953-04-15"), parseLocalDate("2002-08-15"), parseLocalDate("1926-12-10")),//[1][1]
                    asList(parseLocalDate("2009-08-09"), parseLocalDate("1919-08-30"), parseLocalDate("1906-04-10")),//[1][2]
                    asList(parseLocalDate("1995-10-28"), parseLocalDate("1989-09-07")),//[1][3]
                    asList(parseLocalDate("2002-01-03"), parseLocalDate("1929-03-17"), parseLocalDate("1939-10-23"))//[1][4]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(parseLocalDate("1936-05-05"), parseLocalDate("1941-04-12"), parseLocalDate("1914-04-15"))//[0][0]
                ),
                asList( // [1]
                    asList(parseLocalDate("1944-05-09"), parseLocalDate("2002-02-11"))//[1][0]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(parseLocalDate("1965-04-18"), parseLocalDate("2012-11-07"), parseLocalDate("1961-03-15")),//[0][0]
                    asList(parseLocalDate("1922-05-22"), parseLocalDate("1978-03-25")),//[0][1]
                    asList(parseLocalDate("1935-05-29"))//[0][2]
                ),
                asList( // [1]
                    asList(parseLocalDate("1904-07-08"), parseLocalDate("1968-05-23"), parseLocalDate("1946-03-31")),//[1][0]
                    asList(parseLocalDate("2014-01-28")),//[1][1]
                    asList(parseLocalDate("1938-09-20"), parseLocalDate("1920-07-09"), parseLocalDate("1990-12-31")),//[1][2]
                    asList(parseLocalDate("1984-07-20"), parseLocalDate("1988-11-25")),//[1][3]
                    asList(parseLocalDate("1941-12-21"), parseLocalDate("1939-01-16"), parseLocalDate("2012-09-19"))//[1][4]
                ),
                asList( // [2]
                    asList(parseLocalDate("2020-12-28")),//[2][0]
                    asList(parseLocalDate("1930-11-13")),//[2][1]
                    asList(parseLocalDate("2014-05-02"), parseLocalDate("1935-02-16"), parseLocalDate("1919-01-17")),//[2][2]
                    asList(parseLocalDate("1972-04-20"), parseLocalDate("1951-05-30"), parseLocalDate("1963-01-11"))//[2][3]
                ),
                asList( // [3]
                    asList(parseLocalDate("1993-03-20"), parseLocalDate("1978-12-31")),//[3][0]
                    asList(parseLocalDate("1965-12-15"), parseLocalDate("1970-09-02"), parseLocalDate("2010-05-25"))//[3][1]
                )
            )
        )
        .go();
  }

  @Test
  public void hiveDecimalArray() throws Exception {
    // Nesting 0: reading ARRAY<DECIMAL(9,3)>
    testBuilder()
        .sqlQuery("SELECT decimal_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("decimal_arr_n_0")
        .baselineValuesForSingleColumn(asList(new BigDecimal("-100000.000"), new BigDecimal("102030.001"), new BigDecimal("0.001")))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(Collections.singletonList(new BigDecimal("-10.500")))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<DECIMAL(9,3)>>
    testBuilder()
        .sqlQuery("SELECT decimal_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("decimal_arr_n_1")
        .baselineValuesForSingleColumn(asList(
            asList(new BigDecimal("-100000.000"), new BigDecimal("102030.001")),
            asList(new BigDecimal("0.101"), new BigDecimal("0.102")),
            asList(new BigDecimal("0.001"), new BigDecimal("327670.001"))))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(new BigDecimal("10.500"), new BigDecimal("5.010"))))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<DECIMAL(9,3)>>>
    testBuilder()
        .sqlQuery("SELECT decimal_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("decimal_arr_n_2")
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(new BigDecimal("9.453")),//[0][0]
                    asList(new BigDecimal("8.233"), new BigDecimal("-146577.465")),//[0][1]
                    asList(new BigDecimal("-911144.423"), new BigDecimal("-862766.866"), new BigDecimal("-129948.784"))//[0][2]
                ),
                asList( // [1]
                    asList(new BigDecimal("931346.867"))//[1][0]
                ),
                asList( // [2]
                    asList(new BigDecimal("81.750")),//[2][0]
                    asList(new BigDecimal("587225.077"), new BigDecimal("-3.930")),//[2][1]
                    asList(new BigDecimal("0.042")),//[2][2]
                    asList(new BigDecimal("-342346.511"))//[2][3]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(new BigDecimal("375098.406"), new BigDecimal("84.509")),//[0][0]
                    asList(new BigDecimal("-446325.287"), new BigDecimal("3.671")),//[0][1]
                    asList(new BigDecimal("286958.380"), new BigDecimal("314821.890"), new BigDecimal("18513.303")),//[0][2]
                    asList(new BigDecimal("-444023.971"), new BigDecimal("827746.528"), new BigDecimal("-54.986")),//[0][3]
                    asList(new BigDecimal("-44520.406"))//[0][4]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(new BigDecimal("906668.849"), new BigDecimal("1.406")),//[0][0]
                    asList(new BigDecimal("-494177.333"), new BigDecimal("952997.058"))//[0][1]
                ),
                asList( // [1]
                    asList(new BigDecimal("642385.159"), new BigDecimal("369753.830"), new BigDecimal("634889.981")),//[1][0]
                    asList(new BigDecimal("83970.515"), new BigDecimal("-847315.758"), new BigDecimal("-0.600")),//[1][1]
                    asList(new BigDecimal("73013.870")),//[1][2]
                    asList(new BigDecimal("337872.675"), new BigDecimal("375940.114"), new BigDecimal("-2.670")),//[1][3]
                    asList(new BigDecimal("-7.899"), new BigDecimal("755611.538"))//[1][4]
                )
            )
        )
        .go();
  }

  @Test
  public void hiveDoubleArray() throws Exception {
    // Nesting 0: reading ARRAY<DOUBLE>
    testBuilder()
        .sqlQuery("SELECT double_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("double_arr_n_0")
        .baselineValuesForSingleColumn(asList(-13.241563769628, 0.3436367772981237, 9.73366))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(asList(15.581409176959358))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<DOUBLE>>
    testBuilder()
        .sqlQuery("SELECT double_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("double_arr_n_1")
        .baselineValuesForSingleColumn(asList(asList(-24.049666910012498, 14.975034200, 1.19975056092457), asList(-2.293376758961259, 80.783)))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(0.47745359256854, -0.47745359256854)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<DOUBLE>>>
    testBuilder()
        .sqlQuery("SELECT double_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("double_arr_n_2")
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(-9.269519394436928),//[0][0]
                    asList(0.7319990286742192, 55.53357952933713, -4.450389221972496)//[0][1]
                ),
                asList( // [1]
                    asList(0.8453724066773386)//[1][0]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(-7966.1700155142025, 2519.664646202656),//[0][0]
                    asList(-0.4584683555041169),//[0][1]
                    asList(-860.4673046946417, 6.371900064750405, 0.4722917366204724)//[0][2]
                ),
                asList( // [1]
                    asList(-62.76596817199298),//[1][0]
                    asList(712.7880069076203, -5.14172156610055),//[1][1]
                    asList(3891.128276893486, -0.5008908018575201)//[1][2]
                ),
                asList( // [2]
                    asList(246.42074787345825, -0.7252828610111548),//[2][0]
                    asList(-845.6633966327038, -436.5267842528363)//[2][1]
                ),
                asList( // [3]
                    asList(5.177407969462521),//[3][0]
                    asList(0.10545048230228471, 0.7364424942282094),//[3][1]
                    asList(-373.3798205258425, -79.65616885610245)//[3][2]
                ),
                asList( // [4]
                    asList(-744.3464669962211, 3.8376055596419754),//[4][0]
                    asList(5784.252615154324, -4792.10612059247, -2535.4093308546435)//[4][1]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(0.054727088545119096, 0.3289046600776335, -183.0613955159468)//[0][0]
                ),
                asList( // [1]
                    asList(-1653.1119499932845, 5132.117249049659),//[1][0]
                    asList(735.8474815185632, -5.4205625353286795),//[1][1]
                    asList(2.9513430741605107, -7513.09536433704),//[1][2]
                    asList(1660.4238619967039),//[1][3]
                    asList(472.7475322920831)//[1][4]
                )
            )
        )
        .go();
  }

  @Test
  public void hiveFloatArray() throws Exception {
    // Nesting 0: reading ARRAY<FLOAT>
    testBuilder()
        .sqlQuery("SELECT float_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("float_arr_n_0")
        .baselineValuesForSingleColumn(asList(-32.058f, 94.47389f, 16.107912f))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(Collections.singletonList(25.96484f))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<FLOAT>>
    testBuilder()
        .sqlQuery("SELECT float_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("float_arr_n_1")
        .baselineValuesForSingleColumn(asList(asList(-82.399826f, 12.633938f, 86.19402f), asList(-13.03544f, 64.65487f)))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(15.259451f, -15.259451f)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<FLOAT>>>
    testBuilder()
        .sqlQuery("SELECT float_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("float_arr_n_2")
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(-5.6506114f),//[0][0]
                    asList(26.546333f, 3724.8389f),//[0][1]
                    asList(-53.65775f, 686.8335f, -0.99032f)//[0][2]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(29.042528f),//[0][0]
                    asList(3524.3398f, -8856.58f, 6.8508215f)//[0][1]
                ),
                asList( // [1]
                    asList(-0.73994386f, -2.0008986f),//[1][0]
                    asList(-9.903006f, -271.26172f),//[1][1]
                    asList(-131.80347f),//[1][2]
                    asList(39.721367f, -4870.5444f),//[1][3]
                    asList(-1.4830998f, -766.3066f, -0.1659732f)//[1][4]
                ),
                asList( // [2]
                    asList(3467.0298f, -240.64255f),//[2][0]
                    asList(2.4072556f, -85.89145f)//[2][1]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(-888.68243f, -38.09065f),//[0][0]
                    asList(-6948.154f, -185.64319f, 0.7401936f),//[0][1]
                    asList(-705.2718f, -932.4041f)//[0][2]
                ),
                asList( // [1]
                    asList(-2.581712f, 0.28686252f, -0.98652786f),//[1][0]
                    asList(-57.448563f, -0.0057083773f, -0.21712556f),//[1][1]
                    asList(-8.076653f, -8149.519f, -7.5968184f),//[1][2]
                    asList(8.823492f),//[1][3]
                    asList(-9134.323f, 467.53275f, -59.763447f)//[1][4]
                ),
                asList( // [2]
                    asList(0.33596575f, 6805.2256f, -3087.9531f),//[2][0]
                    asList(9816.865f, -164.90712f, -1.9071647f)//[2][1]
                ),
                asList( // [3]
                    asList(-0.23883149f),//[3][0]
                    asList(-5.3763375f, -4.7661624f)//[3][1]
                ),
                asList( // [4]
                    asList(-52.42167f, 247.91452f),//[4][0]
                    asList(9499.771f),//[4][1]
                    asList(-0.6549191f, 4340.83f)//[4][2]
                )
            )
        )
        .go();
  }

  @Test
  public void hiveSmallintArray() throws Exception {
    // Nesting 0: reading ARRAY<SMALLINT>
    testBuilder()
        .sqlQuery("SELECT smallint_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("smallint_arr_n_0")
        .baselineValuesForSingleColumn(asList(-32768, 0, 32767))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(asList(10500))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<SMALLINT>>
    testBuilder()
        .sqlQuery("SELECT smallint_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("smallint_arr_n_1")
        .baselineValuesForSingleColumn(asList(asList(-32768, -32768), asList(0, 0), asList(32767, 32767)))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(10500, 5010)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<SMALLINT>>>
    testBuilder()
        .sqlQuery("SELECT smallint_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("smallint_arr_n_2")
        .baselineValues(asList(
            asList(asList(-28752)),
            asList(asList(17243, 15652), asList(-9684), asList(10176, 18123), asList(-15404, 15420), asList(11136, -19435)),
            asList(asList(-29634, -12695), asList(4350, -24289, -10889)),
            asList(asList(13731), asList(27661, -15794, 21784), asList(14341, -4635), asList(1601, -29973), asList(2750, 30373, -11630)),
            asList(asList(-11383))
        ))
        .baselineValues(asList(
            asList(asList(23860), asList(-27345, 19068), asList(-7174, 286, 14673)),
            asList(asList(14844, -9087), asList(-25185, 219), asList(26875), asList(-4699), asList(-3853, -15729, 11472)),
            asList(asList(-29142), asList(-13859), asList(-23073, 31368, -26542)),
            asList(asList(14914, 14656), asList(4636, 6289))
        ))
        .baselineValues(asList(
            asList(asList(10426, 31865), asList(-19088), asList(-4774), asList(17988)),
            asList(asList(-6214, -26836, 30715)),
            asList(asList(-4231), asList(31742, -661), asList(-22842, 4203), asList(18278))
        ))
        .go();
  }

  @Test
  public void hiveStringArray() throws Exception {
    // Nesting 0: reading ARRAY<STRING>
    testBuilder()
        .sqlQuery("SELECT string_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("string_arr_n_0")
        .baselineValuesForSingleColumn(asList(new Text("First Value Of Array"), new Text("komlnp"), new Text("The Last Value")))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(Collections.singletonList(new Text("ABCaBcA-1-2-3")))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<STRING>>
    testBuilder()
        .sqlQuery("SELECT string_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("string_arr_n_1")
        .baselineValuesForSingleColumn(asList(asList(new Text("Array 0, Value 0"), new Text("Array 0, Value 1")), asList(new Text("Array 1"))))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(new Text("One"))))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<STRING>>>
    testBuilder()
        .sqlQuery("SELECT string_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("string_arr_n_2")
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(new Text("dhMGOr1QVO"), new Text("NZpzBl"), new Text("LC8mjYyOJ7l8dHUpk"))//[0][0]
                ),
                asList( // [1]
                    asList(new Text("JH")),//[1][0]
                    asList(new Text("aVxgfxAu")),//[1][1]
                    asList(new Text("fF amN8z8"))//[1][2]
                ),
                asList( // [2]
                    asList(new Text("denwte5R39dSb2PeG"), new Text("Gbosj97RXTvBK1w"), new Text("S3whFvN")),//[2][0]
                    asList(new Text("2sNbYGQhkt303Gnu"), new Text("rwG"), new Text("SQH766A8XwHg2pTA6a"))//[2][1]
                ),
                asList( // [3]
                    asList(new Text("L"), new Text("khGFDtDluFNoo5hT")),//[3][0]
                    asList(new Text("b8")),//[3][1]
                    asList(new Text("Z"))//[3][2]
                ),
                asList( // [4]
                    asList(new Text("DTEuW"), new Text("b0Wt84hIl"), new Text("A1H")),//[4][0]
                    asList(new Text("h2zXh3Qc"), new Text("NOcgU8"), new Text("RGfVgv2rvDG")),//[4][1]
                    asList(new Text("Hfn1ov9hB7fZN"), new Text("0ZgCD3"))//[4][2]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(new Text("nk"), new Text("HA"), new Text("CgAZCxTbTrFWJL3yM")),//[0][0]
                    asList(new Text("T7fGXYwtBb"), new Text("G6vc")),//[0][1]
                    asList(new Text("GrwB5j3LBy9")),//[0][2]
                    asList(new Text("g7UreegD1H97"), new Text("dniQ5Ehhps7c1pBuM"), new Text("S wSNMGj7c")),//[0][3]
                    asList(new Text("iWTEJS0"), new Text("4F"))//[0][4]
                ),
                asList( // [1]
                    asList(new Text("YpRcC01u6i6KO"), new Text("ujpMrvEfUWfKm"), new Text("2d")),//[1][0]
                    asList(new Text("2"), new Text("HVDH"), new Text("5Qx Q6W112"))//[1][1]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(new Text("S8d2vjNu680hSim6iJ")),//[0][0]
                    asList(new Text("lRLaT9RvvgzhZ3C"), new Text("igSX1CP"), new Text("FFZMwMvAOod8")),//[0][1]
                    asList(new Text("iBX"), new Text("sG")),//[0][2]
                    asList(new Text("ChRjuDPz99WeU9"), new Text("2gBBmMUXV9E5E"), new Text(" VkEARI2upO"))//[0][3]
                ),
                asList( // [1]
                    asList(new Text("UgMok3Q5wmd")),//[1][0]
                    asList(new Text("8Zf9CLfUSWK"), new Text(""), new Text("NZ7v")),//[1][1]
                    asList(new Text("vQE3I5t26"), new Text("251BeQJue"))//[1][2]
                ),
                asList( // [2]
                    asList(new Text("Rpo8"))//[2][0]
                ),
                asList( // [3]
                    asList(new Text("jj3njyupewOM Ej0pu"), new Text("aePLtGgtyu4aJ5"), new Text("cKHSvNbImH1MkQmw0Cs")),//[3][0]
                    asList(new Text("VSO5JgI2x7TnK31L5"), new Text("hIub"), new Text("eoBSa0zUFlwroSucU")),//[3][1]
                    asList(new Text("V8Gny91lT"), new Text("5hBncDZ"))//[3][2]
                ),
                asList( // [4]
                    asList(new Text("Y3"), new Text("StcgywfU"), new Text("BFTDChc")),//[4][0]
                    asList(new Text("5JNwXc2UHLld7"), new Text("v")),//[4][1]
                    asList(new Text("9UwBhJMSDftPKuGC")),//[4][2]
                    asList(new Text("E hQ9NJkc0GcMlB"), new Text("IVND1Xp1Nnw26DrL9"))//[4][3]
                )
            )
        ).go();
  }

  @Test
  public void hiveTimestampArray() throws Exception {
    mockUtcDateTimeZone();

    // Nesting 0: reading ARRAY<TIMESTAMP>
    testBuilder()
        .optionSettingQueriesForTestQuery("alter session set `" + ExecConstants.PARQUET_READER_INT96_AS_TIMESTAMP + "` = true")
        .sqlQuery("SELECT timestamp_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("timestamp_arr_n_0")
        .baselineValuesForSingleColumn(asList(
            LocalDateTime.of(2018, 10, 21, 1, 51, 36),
            LocalDateTime.of(2017, 7, 11, 6, 26, 48),
            LocalDateTime.of(2018, 9, 23, 0, 2, 33)))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(asList(LocalDateTime.of(2018, 7, 14, 2, 20, 34)))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<TIMESTAMP>>
    testBuilder()
        .optionSettingQueriesForTestQuery("alter session set `" + ExecConstants.PARQUET_READER_INT96_AS_TIMESTAMP + "` = true")
        .sqlQuery("SELECT timestamp_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("timestamp_arr_n_1")
        .baselineValuesForSingleColumn(asList(
            asList(LocalDateTime.of(2017, 3, 21, 10, 52, 33), LocalDateTime.of(2017, 9, 9, 22, 29, 24), LocalDateTime.of(2018, 1, 17, 2, 45, 23)),
            asList(LocalDateTime.of(2017, 3, 23, 23, 3, 23), LocalDateTime.of(2018, 9, 22, 2, 0, 26))))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(LocalDateTime.of(2017, 8, 9, 5, 26, 8), LocalDateTime.of(2017, 8, 28, 6, 47, 23))))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<TIMESTAMP>>>
    testBuilder()
        .optionSettingQueriesForTestQuery("alter session set `" + ExecConstants.PARQUET_READER_INT96_AS_TIMESTAMP + "` = true")
        .sqlQuery("SELECT timestamp_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("timestamp_arr_n_2")
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(LocalDateTime.of(1929, 1, 8, 17, 31, 47)),//[0][0]
                    asList(LocalDateTime.of(1968, 7, 2, 12, 13, 55), LocalDateTime.of(1990, 1, 25, 18, 5, 51), LocalDateTime.of(1950, 10, 26, 16, 16, 10)),//[0][1]
                    asList(LocalDateTime.of(1946, 9, 3, 0, 3, 50), LocalDateTime.of(1987, 3, 29, 7, 27, 5)),//[0][2]
                    asList(LocalDateTime.of(1979, 11, 29, 6, 1, 14))//[0][3]
                ),
                asList( // [1]
                    asList(LocalDateTime.of(2010, 8, 26, 9, 8, 51), LocalDateTime.of(2012, 2, 5, 0, 34, 22)),//[1][0]
                    asList(LocalDateTime.of(1955, 2, 24, 16, 45, 33)),//[1][1]
                    asList(LocalDateTime.of(1994, 6, 19, 6, 33, 56), LocalDateTime.of(1971, 11, 5, 3, 27, 55), LocalDateTime.of(1925, 4, 11, 11, 55, 48)),//[1][2]
                    asList(LocalDateTime.of(1916, 10, 2, 3, 7, 14), LocalDateTime.of(1995, 4, 11, 15, 5, 51), LocalDateTime.of(1973, 11, 17, 3, 6, 53))//[1][3]
                ),
                asList( // [2]
                    asList(LocalDateTime.of(1929, 12, 19, 14, 49, 8), LocalDateTime.of(1942, 10, 28, 2, 55, 13), LocalDateTime.of(1936, 12, 1, 10, 1, 37)),//[2][0]
                    asList(LocalDateTime.of(1926, 12, 9, 5, 34, 14), LocalDateTime.of(1971, 7, 23, 12, 1, 0), LocalDateTime.of(2014, 1, 7, 4, 29, 3)),//[2][1]
                    asList(LocalDateTime.of(2012, 8, 25, 20, 26, 10)),//[2][2]
                    asList(LocalDateTime.of(2010, 3, 4, 6, 31, 54), LocalDateTime.of(1950, 7, 20, 16, 26, 8), LocalDateTime.of(1953, 3, 16, 13, 13, 24))//[2][3]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(LocalDateTime.of(1904, 12, 9, 22, 37, 10)),//[0][0]
                    asList(LocalDateTime.of(1994, 4, 12, 20, 6, 7)),//[0][1]
                    asList(LocalDateTime.of(1954, 7, 5, 20, 48, 9), LocalDateTime.of(1913, 3, 3, 16, 45, 10), LocalDateTime.of(1960, 4, 30, 19, 35, 28)),//[0][2]
                    asList(LocalDateTime.of(1962, 9, 26, 14, 11, 12), LocalDateTime.of(1906, 6, 18, 2, 3, 17), LocalDateTime.of(2003, 6, 19, 2, 15, 24))//[0][3]
                ),
                asList( // [1]
                    asList(LocalDateTime.of(1929, 3, 20, 4, 33, 40), LocalDateTime.of(1939, 2, 12, 4, 3, 7), LocalDateTime.of(1945, 2, 16, 18, 18, 16))//[1][0]
                ),
                asList( // [2]
                    asList(LocalDateTime.of(1969, 8, 11, 19, 25, 31), LocalDateTime.of(1944, 8, 10, 23, 57, 58)),//[2][0]
                    asList(LocalDateTime.of(1989, 3, 18, 10, 33, 56), LocalDateTime.of(1961, 6, 6, 1, 44, 50))//[2][1]
                )
            )
        )
        .baselineValuesForSingleColumn(
            asList( // row
                asList( // [0]
                    asList(LocalDateTime.of(1999, 12, 6, 23, 16, 45)),//[0][0]
                    asList(LocalDateTime.of(1903, 12, 11, 2, 26, 16), LocalDateTime.of(2007, 1, 3, 17, 27, 28)),//[0][1]
                    asList(LocalDateTime.of(2018, 3, 16, 13, 43, 19), LocalDateTime.of(2002, 9, 16, 5, 58, 40), LocalDateTime.of(1956, 5, 16, 14, 47, 44)),//[0][2]
                    asList(LocalDateTime.of(2006, 9, 19, 15, 38, 19), LocalDateTime.of(2016, 1, 21, 10, 39, 30))//[0][3]
                )
            )
        )
        .go();
  }

  @Test
  public void hiveTinyintArray() throws Exception {
    // Nesting 0: reading ARRAY<TINYINT>
    testBuilder()
        .sqlQuery("SELECT tinyint_arr_n_0 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("tinyint_arr_n_0")
        .baselineValuesForSingleColumn(asList(-128, 0, 127))
        .baselineValuesForSingleColumn(emptyList())
        .baselineValuesForSingleColumn(asList(-101))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<TINYINT>>
    testBuilder()
        .sqlQuery("SELECT tinyint_arr_n_1 FROM cp.`parquet2/hive_arrays_p.parquet`")
        .unOrdered()
        .baselineColumns("tinyint_arr_n_1")
        .baselineValuesForSingleColumn(asList(asList(-128, -127), asList(0, 1), asList(127, 126)))
        .baselineValuesForSingleColumn(asList(emptyList(), emptyList()))
        .baselineValuesForSingleColumn(asList(asList(-102)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<TINYINT>>>
    testBuilder()
        .sqlQuery("SELECT tinyint_arr_n_2 FROM cp.`parquet2/hive_arrays_p.parquet` order by rid")
        .ordered()
        .baselineColumns("tinyint_arr_n_2")
        .baselineValues(asList(
            asList(asList(31, 65, 54), asList(66), asList(22), asList(-33, -125, 116)),
            asList(asList(-5, -10)),
            asList(asList(78), asList(86), asList(90, 34), asList(32)),
            asList(asList(103, -49, -33), asList(-30), asList(107, 24, 74), asList(16, -58)),
            asList(asList(-119, -8), asList(50, -99, 26), asList(-119))
        ))
        .baselineValues(asList(
            asList(asList(-90, -113), asList(71, -65)),
            asList(asList(88, -83)),
            asList(asList(11), asList(121, -57)),
            asList(asList(-79), asList(16, -111, -111), asList(90, 106), asList(33, 29, 42), asList(74))
        ))
        .baselineValues(asList(
            asList(asList(74, -115), asList(19, 85, 3))
        ))
        .go();
  }

  @Test
  public void testTimeMicros() throws Exception {
    testBuilder()
      .unOrdered()
      .sqlQuery("select * from cp.`parquet2/allTypes.parquet`")
      .baselineColumns("int_field", "long_field", "float_field", "double_field", "string_field",
        "boolean_field", "time_field", "timestamp_field", "date_field", "decimal_field", "uuid_field",
        "fixed_field", "binary_field", "list_field", "map_field", "struct_field", "repeated_struct_field",
        "repeated_list_field", "repeated_map_field")
      .baselineValues(1, 100L, 0.5F, 1.5D, "abc", true, LocalTime.of(2, 42, 42),
        LocalDateTime.of(1994, 4, 18, 11, 0, 0), LocalDate.of(1994, 4, 18),
        new BigDecimal("12.34"), new byte[16], new byte[10], "hello".getBytes(StandardCharsets.UTF_8),
        listOf("a", "b", "c"),
        mapOfObject(
          new Text("a"), 0.1F,
          new Text("b"), 0.2F),
        mapOf(
          "struct_int_field", 123,
          "struct_string_field", "abc"),
        listOf(
          mapOf(
            "struct_int_field", 123,
            "struct_string_field", "abc"),
          mapOf(
            "struct_int_field", 123,
            "struct_string_field", "abc")),
        listOf(listOf("a", "b", "c"), listOf("a", "b", "c")),
        listOf(
          mapOfObject(
            new Text("a"), 0.1F,
            new Text("b"), 0.2F),
          mapOfObject(
            new Text("a"), 0.1F,
            new Text("b"), 0.2F))
      )
      .baselineValues(null, null, null, null, null, null, null, null, null, null, null, null, null,
        listOf(), mapOfObject(), mapOf(), listOf(), listOf(), listOf())
      .go();

    testRunAndPrint(UserBitShared.QueryType.SQL, "select * from cp.`parquet2/allTypes.parquet`");
  }

}
