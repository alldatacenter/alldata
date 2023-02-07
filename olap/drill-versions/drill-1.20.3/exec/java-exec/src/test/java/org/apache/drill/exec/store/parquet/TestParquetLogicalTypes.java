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

import static org.apache.drill.test.TestBuilder.mapOf;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.BaseTestQuery;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test Parquet logical type handling with default Parquet reader.
 * Tests are executed on data files with dictionary encoding enabled and disabled.
 * parquet_logical_types* files are generated with ParquetSimpleTestFileGenerator class.
 */
@Category({ParquetTest.class, UnlikelyTest.class})
public class TestParquetLogicalTypes extends BaseTestQuery {

  @Test //DRILL-5971
  public void testComplexLogicalIntTypes() throws Exception {
    String query = "select t.complextype as complextype,  " +
            "t.uint_64 as uint_64, t.uint_32 as uint_32, t.uint_16 as uint_16, t.uint_8 as uint_8,  " +
            "t.int_64 as int_64, t.int_32 as int_32, t.int_16 as int_16, t.int_8 as int_8  " +
            "from cp.`store/parquet/complex/logical_int_complex.parquet` t";
    String[] columns = {"complextype", "uint_64", "uint_32", "uint_16", "uint_8", "int_64", "int_32", "int_16", "int_8" };
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns(columns)
        .baselineValues(mapOf("a","a","b","b"), 0L, 0, 0, 0, 0L, 0, 0, 0)
        .baselineValues(mapOf("a","a","b","b"), -1L, -1, -1, -1, -1L, -1, -1, -1)
        .baselineValues(mapOf("a","a","b","b"), 1L, 1, 1, 1, -9223372036854775808L, 1, 1, 1)
        .baselineValues(mapOf("a","a","b","b"), 9223372036854775807L, 2147483647, 65535, 255, 9223372036854775807L, -2147483648, -32768, -128)
        .build()
        .run();
  }

  @Test //DRILL-5971
  public void testComplexLogicalIntTypes2() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
        " select " +
        " t.rowKey as rowKey, " +
        " t.StringTypes._UTF8 as _UTF8, " +
        " t.StringTypes._Enum as _Enum, " +
        " t.NumericTypes.Int32._INT32_RAW as _INT32_RAW, " +
        " t.NumericTypes.Int32._INT_8 as _INT_8, " +
        " t.NumericTypes.Int32._INT_16 as _INT_16, " +
        " t.NumericTypes.Int32._INT_32 as _INT_32, " +
        " t.NumericTypes.Int32._UINT_8 as _UINT_8, " +
        " t.NumericTypes.Int32._UINT_16 as _UINT_16, " +
        " t.NumericTypes.Int32._UINT_32 as _UINT_32, " +
        " t.NumericTypes.Int64._INT64_RAW as _INT64_RAW, " +
        " t.NumericTypes.Int64._INT_64 as _INT_64, " +
        " t.NumericTypes.Int64._UINT_64 as _UINT_64, " +
        " t.NumericTypes.DateTimeTypes._DATE_int32 as _DATE_int32, " +
        " t.NumericTypes.DateTimeTypes._TIME_MILLIS_int32 as _TIME_MILLIS_int32, " +
        " t.NumericTypes.DateTimeTypes._TIMESTAMP_MILLIS_int64 as _TIMESTAMP_MILLIS_int64, " +
        " t.NumericTypes.DateTimeTypes._TIMESTAMP_MICROS_int64 as _TIMESTAMP_MICROS_int64, " +
        " t.NumericTypes.DateTimeTypes._INTERVAL_fixed_len_byte_array_12 as _INTERVAL_fixed_len_byte_array_12, " +
        " t.NumericTypes.Int96._INT96_RAW as _INT96_RAW " +
        " from " +
        " cp.`store/parquet/complex/parquet_logical_types_complex.parquet` t " +
        " order by t.rowKey ";
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
            LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .build()
        .run();
  }

  @Test //DRILL-5971
  public void testComplexLogicalIntTypes3() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
        " select " +
            " t.rowKey as rowKey, " +
            " t.StringTypes._UTF8 as _UTF8, " +
            " t.StringTypes._Enum as _Enum, " +
            " t.NumericTypes.Int32._INT32_RAW as _INT32_RAW, " +
            " t.NumericTypes.Int32._INT_8 as _INT_8, " +
            " t.NumericTypes.Int32._INT_16 as _INT_16, " +
            " t.NumericTypes.Int32._INT_32 as _INT_32, " +
            " t.NumericTypes.Int32._UINT_8 as _UINT_8, " +
            " t.NumericTypes.Int32._UINT_16 as _UINT_16, " +
            " t.NumericTypes.Int32._UINT_32 as _UINT_32, " +
            " t.NumericTypes.Int64._INT64_RAW as _INT64_RAW, " +
            " t.NumericTypes.Int64._INT_64 as _INT_64, " +
            " t.NumericTypes.Int64._UINT_64 as _UINT_64, " +
            " t.NumericTypes.DateTimeTypes._DATE_int32 as _DATE_int32, " +
            " t.NumericTypes.DateTimeTypes._TIME_MILLIS_int32 as _TIME_MILLIS_int32, " +
            " t.NumericTypes.DateTimeTypes._TIMESTAMP_MILLIS_int64 as _TIMESTAMP_MILLIS_int64, " +
            " t.NumericTypes.DateTimeTypes._TIMESTAMP_MICROS_int64 as _TIMESTAMP_MICROS_int64, " +
            " t.NumericTypes.DateTimeTypes._INTERVAL_fixed_len_byte_array_12 as _INTERVAL_fixed_len_byte_array_12, " +
            " t.NumericTypes.Int96._INT96_RAW as _INT96_RAW " +
            " from " +
            " cp.`store/parquet/complex/parquet_logical_types_complex_nullable.parquet` t " +
            " order by t.rowKey ";
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
            LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .baselineValues(4, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null)
        .build().run();
  }

  @Test //DRILL-6670: include tests on data with dictionary encoding disabled
  public void testComplexLogicalIntTypes4() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
        " select " +
        " t.rowKey as rowKey, " +
        " t.StringTypes._UTF8 as _UTF8, " +
        " t.StringTypes._Enum as _Enum, " +
        " t.NumericTypes.Int32._INT32_RAW as _INT32_RAW, " +
        " t.NumericTypes.Int32._INT_8 as _INT_8, " +
        " t.NumericTypes.Int32._INT_16 as _INT_16, " +
        " t.NumericTypes.Int32._INT_32 as _INT_32, " +
        " t.NumericTypes.Int32._UINT_8 as _UINT_8, " +
        " t.NumericTypes.Int32._UINT_16 as _UINT_16, " +
        " t.NumericTypes.Int32._UINT_32 as _UINT_32, " +
        " t.NumericTypes.Int64._INT64_RAW as _INT64_RAW, " +
        " t.NumericTypes.Int64._INT_64 as _INT_64, " +
        " t.NumericTypes.Int64._UINT_64 as _UINT_64, " +
        " t.NumericTypes.DateTimeTypes._DATE_int32 as _DATE_int32, " +
        " t.NumericTypes.DateTimeTypes._TIME_MILLIS_int32 as _TIME_MILLIS_int32, " +
        " t.NumericTypes.DateTimeTypes._TIMESTAMP_MILLIS_int64 as _TIMESTAMP_MILLIS_int64, " +
        " t.NumericTypes.DateTimeTypes._TIMESTAMP_MICROS_int64 as _TIMESTAMP_MICROS_int64, " +
        " t.NumericTypes.DateTimeTypes._INTERVAL_fixed_len_byte_array_12 as _INTERVAL_fixed_len_byte_array_12, " +
        " t.NumericTypes.Int96._INT96_RAW as _INT96_RAW " +
        " from " +
        " cp.`store/parquet/complex/parquet_logical_types_complex_nodict.parquet` t " +
        " order by t.rowKey ";
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
            LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .build()
        .run();
  }

  @Test //DRILL-6670: include tests on data with dictionary encoding disabled
  public void testComplexLogicalIntTypes5() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
        " select " +
            " t.rowKey as rowKey, " +
            " t.StringTypes._UTF8 as _UTF8, " +
            " t.StringTypes._Enum as _Enum, " +
            " t.NumericTypes.Int32._INT32_RAW as _INT32_RAW, " +
            " t.NumericTypes.Int32._INT_8 as _INT_8, " +
            " t.NumericTypes.Int32._INT_16 as _INT_16, " +
            " t.NumericTypes.Int32._INT_32 as _INT_32, " +
            " t.NumericTypes.Int32._UINT_8 as _UINT_8, " +
            " t.NumericTypes.Int32._UINT_16 as _UINT_16, " +
            " t.NumericTypes.Int32._UINT_32 as _UINT_32, " +
            " t.NumericTypes.Int64._INT64_RAW as _INT64_RAW, " +
            " t.NumericTypes.Int64._INT_64 as _INT_64, " +
            " t.NumericTypes.Int64._UINT_64 as _UINT_64, " +
            " t.NumericTypes.DateTimeTypes._DATE_int32 as _DATE_int32, " +
            " t.NumericTypes.DateTimeTypes._TIME_MILLIS_int32 as _TIME_MILLIS_int32, " +
            " t.NumericTypes.DateTimeTypes._TIMESTAMP_MILLIS_int64 as _TIMESTAMP_MILLIS_int64, " +
            " t.NumericTypes.DateTimeTypes._TIMESTAMP_MICROS_int64 as _TIMESTAMP_MICROS_int64, " +
            " t.NumericTypes.DateTimeTypes._INTERVAL_fixed_len_byte_array_12 as _INTERVAL_fixed_len_byte_array_12, " +
            " t.NumericTypes.Int96._INT96_RAW as _INT96_RAW " +
            " from " +
            " cp.`store/parquet/complex/parquet_logical_types_complex_nullable.parquet` t " +
            " order by t.rowKey ";
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
            LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .baselineValues(4, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null)
        .build().run();
  }

  @Test //DRILL-6670
  public void testSimpleLogicalIntTypes1() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
      " select " +
        " rowKey, " +
        " _UTF8, " +
        " _Enum, " +
        " _INT32_RAW, " +
        " _INT_8, " +
        " _INT_16, " +
        " _INT_32, " +
        " _UINT_8, " +
        " _UINT_16, " +
        " _UINT_32, " +
        " _INT64_RAW, " +
        " _INT_64, " +
        " _UINT_64, " +
        " _DATE_int32, " +
        " _TIME_MILLIS_int32, " +
        " _TIMESTAMP_MILLIS_int64, " +
        " _TIMESTAMP_MICROS_int64, " +
        " _INTERVAL_fixed_len_byte_array_12, " +
        " _INT96_RAW " +
        " from " +
        " cp.`parquet/parquet_logical_types_simple.parquet` t " +
        " order by t.rowKey ";
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
        LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
        new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
        bytes12)
      .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
        9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
        LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
        new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
        bytesOnes)
      .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
        -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
        LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
      .build()
      .run();
  }

  @Test //DRILL-6670
  public void testSimpleLogicalIntTypes2() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
      " select " +
        " rowKey, " +
        " _UTF8, " +
        " _Enum, " +
        " _INT32_RAW, " +
        " _INT_8, " +
        " _INT_16, " +
        " _INT_32, " +
        " _UINT_8, " +
        " _UINT_16, " +
        " _UINT_32, " +
        " _INT64_RAW, " +
        " _INT_64, " +
        " _UINT_64, " +
        " _DATE_int32, " +
        " _TIME_MILLIS_int32, " +
        " _TIMESTAMP_MILLIS_int64, " +
        " _TIMESTAMP_MICROS_int64, " +
        " _INTERVAL_fixed_len_byte_array_12, " +
        " _INT96_RAW " +
        " from " +
        " cp.`parquet/parquet_logical_types_simple_nullable.parquet` t " +
        " order by t.rowKey ";
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
        LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
        new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
        bytes12)
      .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
        9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
        LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
        new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
        bytesOnes)
      .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
        -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
        LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
      .baselineValues(4, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null)
      .build().run();
  }

  @Test //DRILL-6670
  public void testSimpleLogicalIntTypes3() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
      " select " +
        " rowKey, " +
        " _UTF8, " +
        " _Enum, " +
        " _INT32_RAW, " +
        " _INT_8, " +
        " _INT_16, " +
        " _INT_32, " +
        " _UINT_8, " +
        " _UINT_16, " +
        " _UINT_32, " +
        " _INT64_RAW, " +
        " _INT_64, " +
        " _UINT_64, " +
        " _DATE_int32, " +
        " _TIME_MILLIS_int32, " +
        " _TIMESTAMP_MILLIS_int64, " +
        " _TIMESTAMP_MICROS_int64, " +
        " _INTERVAL_fixed_len_byte_array_12, " +
        " _INT96_RAW " +
        " from " +
        " cp.`parquet/parquet_logical_types_simple_nodict.parquet` t " +
        " order by t.rowKey ";
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
        LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
        new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
        bytes12)
      .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
        9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
        LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
        new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
        bytesOnes)
      .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
        -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
        LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
      .build()
      .run();
  }

  @Test //DRILL-6670
  public void testSimpleLogicalIntTypes4() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
      " select " +
        " rowKey, " +
        " _UTF8, " +
        " _Enum, " +
        " _INT32_RAW, " +
        " _INT_8, " +
        " _INT_16, " +
        " _INT_32, " +
        " _UINT_8, " +
        " _UINT_16, " +
        " _UINT_32, " +
        " _INT64_RAW, " +
        " _INT_64, " +
        " _UINT_64, " +
        " _DATE_int32, " +
        " _TIME_MILLIS_int32, " +
        " _TIMESTAMP_MILLIS_int64, " +
        " _TIMESTAMP_MICROS_int64, " +
        " _INTERVAL_fixed_len_byte_array_12, " +
        " _INT96_RAW " +
        " from " +
        " cp.`parquet/parquet_logical_types_simple_nullable_nodict.parquet` t " +
        " order by t.rowKey ";
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
        LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
        new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
        bytes12)
      .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
        9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
        LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
        new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
        bytesOnes)
      .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
        -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
        LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
        LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
      .baselineValues(4, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null)
      .build().run();
  }

  @Test
  public void testV2SimpleLogicalIntTypes1() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
        " select " +
        " rowKey, " +
        " _UTF8, " +
        " _Enum, " +
        " _INT32_RAW, " +
        " _INT_8, " +
        " _INT_16, " +
        " _INT_32, " +
        " _UINT_8, " +
        " _UINT_16, " +
        " _UINT_32, " +
        " _INT64_RAW, " +
        " _INT_64, " +
        " _UINT_64, " +
        " _DATE_int32, " +
        " _TIME_MILLIS_int32, " +
        " _TIMESTAMP_MILLIS_int64, " +
        " _TIMESTAMP_MICROS_int64, " +
        " _INTERVAL_fixed_len_byte_array_12, " +
        " _INT96_RAW " +
        " from " +
        " cp.`parquet/parquet_v2_logical_types_simple.parquet` t " +
        " order by t.rowKey ";
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
            LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
       .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .build()
        .run();
  }

  @Test
  public void testV2SimpleLogicalIntTypes2() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
        " select " +
        " rowKey, " +
        " _UTF8, " +
        " _Enum, " +
        " _INT32_RAW, " +
        " _INT_8, " +
        " _INT_16, " +
        " _INT_32, " +
        " _UINT_8, " +
        " _UINT_16, " +
        " _UINT_32, " +
        " _INT64_RAW, " +
        " _INT_64, " +
        " _UINT_64, " +
        " _DATE_int32, " +
        " _TIME_MILLIS_int32, " +
        " _TIMESTAMP_MILLIS_int64, " +
        " _TIMESTAMP_MICROS_int64, " +
        " _INTERVAL_fixed_len_byte_array_12, " +
        " _INT96_RAW " +
        " from " +
        " cp.`parquet/parquet_v2_logical_types_simple_nullable.parquet` t " +
        " order by t.rowKey ";
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
            LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
           bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .baselineValues(4, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null)
        .build().run();
  }

  @Test
  public void testV2SimpleLogicalIntTypes3() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
        " select " +
        " rowKey, " +
        " _UTF8, " +
        " _Enum, " +
        " _INT32_RAW, " +
        " _INT_8, " +
        " _INT_16, " +
        " _INT_32, " +
        " _UINT_8, " +
        " _UINT_16, " +
        " _UINT_32, " +
        " _INT64_RAW, " +
        " _INT_64, " +
        " _UINT_64, " +
        " _DATE_int32, " +
        " _TIME_MILLIS_int32, " +
        " _TIMESTAMP_MILLIS_int64, " +
        " _TIMESTAMP_MICROS_int64, " +
        " _INTERVAL_fixed_len_byte_array_12, " +
        " _INT96_RAW " +
        " from " +
        " cp.`parquet/parquet_v2_logical_types_simple_nodict.parquet` t " +
        " order by t.rowKey ";
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
            LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
            bytes12)
       .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .build()
        .run();
  }

  @Test
  public void testV2SimpleLogicalIntTypes4() throws Exception {
    byte[] bytes12 = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b' };
    byte[] bytesOnes = new byte[12];
    byte[] bytesZeros = new byte[12];
    Arrays.fill(bytesOnes, (byte) 1);
    String query =
        " select " +
        " rowKey, " +
        " _UTF8, " +
        " _Enum, " +
        " _INT32_RAW, " +
        " _INT_8, " +
        " _INT_16, " +
        " _INT_32, " +
        " _UINT_8, " +
        " _UINT_16, " +
        " _UINT_32, " +
        " _INT64_RAW, " +
        " _INT_64, " +
        " _UINT_64, " +
        " _DATE_int32, " +
        " _TIME_MILLIS_int32, " +
        " _TIMESTAMP_MILLIS_int64, " +
        " _TIMESTAMP_MICROS_int64, " +
        " _INTERVAL_fixed_len_byte_array_12, " +
        " _INT96_RAW " +
        " from " +
        " cp.`parquet/parquet_v2_logical_types_simple_nullable_nodict.parquet` t " +
        " order by t.rowKey ";
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
            LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1973-11-29T21:33:09.012"), LocalDateTime.of(1970, 1, 2, 10, 17, 36, 789_000_000),
            new Period().plusMonths(875770417).plusDays(943142453).plusMillis(1650536505),
           bytes12)
        .baselineValues(2, "UTF8 string2", "MAX_VALUE", 2147483647, 127, 32767, 2147483647, 255, 65535, -1,
            9223372036854775807L, 9223372036854775807L, -1L, LocalDate.parse("1969-12-31"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0xFFFFFFFF), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("2038-01-19T03:14:07.999"), LocalDateTime.of(294247, 1, 10, 4, 0, 54, 775_000_000),
            new Period().plusMonths(16843009).plusDays(16843009).plusMillis(16843009),
            bytesOnes)
        .baselineValues(3, "UTF8 string3", "MIN_VALUE", -2147483648, -128, -32768, -2147483648, 0, 0, 0,
            -9223372036854775808L, -9223372036854775808L, 0L, LocalDate.parse("1970-01-01"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC).toLocalTime(),
            LocalDateTime.parse("1970-01-01T00:00:00.0"), LocalDateTime.of(1970, 1, 1, 0, 0, 0), new Period("PT0S"), bytesZeros)
        .baselineValues(4, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null)
        .build().run();
  }

  @Test
  public void testUUID() throws Exception {
    String query = "select `_UUID` from cp.`parquet/parquet_test_file_simple.parquet`";
    byte[] bytes = new byte[16];
    Arrays.fill(bytes, (byte) 1);
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("_UUID")
            .baselineValues(bytes)
            .baselineValues(bytes)
            .baselineValues(bytes)
            .go();
  }

  @Test
  public void testNullableVarBinaryUUID() throws Exception {
    String query = "select `opt1_p1` from cp.`parquet/uuid-simple-fixed-length-array.parquet` order by `opt1_p1` limit 4";
    byte[] firstValue = {60, -19, 30, -38, -49, 8, 79, 38, -103, -105, -6, -36, 65, -27, -60, -91};
    byte[] secondValue = {-13, 5, 4, -97, 62, -78, 73, 69, -115, -25, -62, 88, 116, 37, 86, -41};

    testBuilder()
            .sqlQuery(query)
            .ordered()
            .baselineColumns("opt1_p1")
            .baselineValues(firstValue)
            .baselineValues(secondValue)
            .baselineValues(new Object[]{null})
            .baselineValues(new Object[]{null})
            .go();
  }

  @Test
  public void testNullableDecimalDictionaryEncoding() throws Exception {
    testBuilder()
        .sqlQuery("select RegHrs from cp.`parquet/dict_dec.parquet`")
        .ordered()
        .baselineColumns("RegHrs")
        .baselineValues(new BigDecimal("8.000000"))
        .go();
  }

  @Test
  public void testRequiredDecimalDictionaryEncoding() throws Exception {
    testBuilder()
        .sqlQuery("select * from cp.`parquet/SingleRow_RequiredFixedLength_Decimal.parquet`")
        .ordered()
        .baselineColumns("Cost", "Sale")
        .baselineValues(new BigDecimal("550.000000"), new BigDecimal("1050.000000"))
        .go();
  }

  @Test
  public void testRequiredIntervalDictionaryEncoding() throws Exception {
    testBuilder()
        .sqlQuery("select * from cp.`parquet/interval_dictionary.parquet`")
        .unOrdered()
        .baselineColumns("_INTERVAL_fixed_len_byte_array_12")
        .baselineValues(Period.months(875770417).plusDays(943142453).plusMillis(1650536505))
        .baselineValues(Period.months(16843009).plusDays(16843009).plusMillis(16843009))
        .baselineValues(Period.seconds(0))
        .go();
  }

  @Test
  public void testNullableIntervalDictionaryEncoding() throws Exception {
    alterSession(ExecConstants.PARQUET_NEW_RECORD_READER, true);
    testBuilder()
        .sqlQuery("select * from cp.`parquet/nullable_interval_dictionary.parquet`")
        .unOrdered()
        .baselineColumns("_INTERVAL_fixed_len_byte_array_12")
        .baselineValues(Period.months(875770417).plusDays(943142453).plusMillis(1650536505))
        .baselineValues(Period.months(16843009).plusDays(16843009).plusMillis(16843009))
        .baselineValues(Period.seconds(0))
        .baselineValues((Object) null)
        .go();
  }
}
