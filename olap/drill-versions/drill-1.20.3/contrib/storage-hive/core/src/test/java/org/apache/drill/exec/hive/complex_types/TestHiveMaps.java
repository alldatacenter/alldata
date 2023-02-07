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
package org.apache.drill.exec.hive.complex_types;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Paths;

import org.apache.drill.categories.HiveStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.hive.HiveClusterTest;
import org.apache.drill.exec.hive.HiveTestFixture;
import org.apache.drill.exec.hive.HiveTestUtilities;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.apache.drill.test.ClusterFixture;
import org.apache.hadoop.hive.ql.Driver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.drill.exec.expr.fn.impl.DateUtility.parseBest;
import static org.apache.drill.exec.expr.fn.impl.DateUtility.parseLocalDate;
import static org.apache.drill.exec.hive.HiveTestUtilities.assertNativeScanUsed;
import static org.apache.drill.test.TestBuilder.mapOf;
import static org.apache.drill.test.TestBuilder.mapOfObject;

@Category({SlowTest.class, HiveStorageTest.class})
public class TestHiveMaps extends HiveClusterTest {

  private static HiveTestFixture hiveTestFixture;

  @BeforeClass
  public static void setUp() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher)
        .sessionOption(ExecConstants.HIVE_OPTIMIZE_PARQUET_SCAN_WITH_NATIVE_READER, true));
    hiveTestFixture = HiveTestFixture.builder(dirTestWatcher).build();
    hiveTestFixture.getDriverManager().runWithinSession(TestHiveMaps::generateData);
    hiveTestFixture.getPluginManager().addHivePluginTo(cluster.drillbit());
  }

  @AfterClass
  public static void tearDown() {
    if (hiveTestFixture != null) {
      hiveTestFixture.getPluginManager().removeHivePluginFrom(cluster.drillbit());
    }
  }

  private static void generateData(Driver d) {
    HiveTestUtilities.executeQuery(d, "CREATE TABLE map_tbl(" +
        "rid INT, " +
        "int_string MAP<INT, STRING>," +
        "timestamp_decimal MAP<TIMESTAMP, DECIMAL(9,3)>," +
        "char_tinyint MAP<CHAR(2), TINYINT>," +
        "date_boolean MAP<DATE, BOOLEAN>," +
        "double_float MAP<DOUBLE, FLOAT>," +
        "varchar_bigint MAP<VARCHAR(5), BIGINT>," +
        "boolean_smallint MAP<BOOLEAN, SMALLINT>," +
        "decimal_char MAP<DECIMAL(9,3), CHAR(1)>) " +
        "ROW FORMAT DELIMITED " +
        "FIELDS TERMINATED BY ',' " +
        "COLLECTION ITEMS TERMINATED BY '#' " +
        "MAP KEYS TERMINATED BY '@' " +
        "STORED AS TEXTFILE");
    HiveTestUtilities.loadData(d, "map_tbl", Paths.get("complex_types/map/map_tbl.txt"));

    HiveTestUtilities.executeQuery(d, "CREATE TABLE map_complex_tbl(" +
        "rid INT, " +
        "map_n_1 MAP<INT,MAP<STRING, INT>>, " +
        "map_n_2 MAP<INT,MAP<BOOLEAN, MAP<STRING, INT>>>, " +
        "map_arr MAP<STRING, ARRAY<INT>>, " +
        "map_arr_2 MAP<STRING, ARRAY<ARRAY<INT>>>, " +
        "map_arr_map MAP<STRING, ARRAY<MAP<STRING, INT>>>, " +
        "map_struct MAP<STRING, STRUCT<fs:STRING, fi:INT>>, " +
        "map_struct_map MAP<STRING, STRUCT<i:INT, m:MAP<INT,INT>>>" +
        ") ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe' STORED AS TEXTFILE"
    );
    HiveTestUtilities.loadData(d, "map_complex_tbl", Paths.get("complex_types/map/map_complex_tbl.json"));

    HiveTestUtilities.executeQuery(d, "CREATE TABLE map_tbl_p(" +
        "rid INT, " +
        "int_string MAP<INT, STRING>," +
        "timestamp_decimal MAP<TIMESTAMP, DECIMAL(9,3)>," +
        "char_tinyint MAP<CHAR(2), TINYINT>," +
        "date_boolean MAP<DATE, BOOLEAN>," +
        "double_float MAP<DOUBLE, FLOAT>," +
        "varchar_bigint MAP<VARCHAR(5), BIGINT>," +
        "boolean_smallint MAP<BOOLEAN, SMALLINT>," +
        "decimal_char MAP<DECIMAL(9,3), CHAR(1)>) " +
        "STORED AS PARQUET");
    HiveTestUtilities.insertData(d, "map_tbl", "map_tbl_p");

    HiveTestUtilities.executeQuery(d, "CREATE VIEW map_tbl_vw AS SELECT int_string FROM map_tbl WHERE rid=1");


    HiveTestUtilities.executeQuery(d, "CREATE TABLE dummy(d INT) STORED AS TEXTFILE");
    HiveTestUtilities.executeQuery(d, "INSERT INTO TABLE dummy VALUES (1)");


    File copy = dirTestWatcher.copyResourceToRoot(Paths.get("complex_types/map/map_union_tbl.avro"));
    String location = copy.getParentFile().toURI().getPath();

    String mapUnionDdl = String.format("CREATE EXTERNAL TABLE " +
        "map_union_tbl(rid INT, map_u MAP<STRING,UNIONTYPE<INT,STRING,BOOLEAN>>) " +
        " STORED AS AVRO LOCATION '%s'", location);
    HiveTestUtilities.executeQuery(d, mapUnionDdl);
  }

  @Test
  public void mapIntToString() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, int_string FROM hive.map_tbl")
        .unOrdered()
        .baselineColumns("rid", "int_string")
        .baselineValues(1, mapOfObject(1, "First", 2, "Second", 3, "Third"))
        .baselineValues(2, mapOfObject(4, "Fourth", 5, "Fifth"))
        .baselineValues(3, mapOfObject(6, "Sixth", 2, "!!"))
        .go();
  }

  @Test
  public void mapIntToStringInHiveView() throws Exception {
    testBuilder()
        .sqlQuery("SELECT * FROM hive.map_tbl_vw")
        .unOrdered()
        .baselineColumns("int_string")
        .baselineValues(mapOfObject(1, "First", 2, "Second", 3, "Third"))
        .go();
  }

  @Test
  public void mapIntToStringInDrillView() throws Exception {
    queryBuilder().sql(
        "CREATE VIEW %s.`map_vw` AS SELECT int_string FROM hive.map_tbl WHERE rid=1",
        StoragePluginTestUtils.DFS_TMP_SCHEMA
    ).run();
    testBuilder()
        .sqlQuery("SELECT * FROM %s.map_vw", StoragePluginTestUtils.DFS_TMP_SCHEMA)
        .unOrdered()
        .baselineColumns("int_string")
        .baselineValues(mapOfObject(1, "First", 2, "Second", 3, "Third"))
        .go();
  }

  @Test
  public void mapTimestampToDecimal() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, timestamp_decimal FROM hive.map_tbl")
        .unOrdered()
        .baselineColumns("rid", "timestamp_decimal")
        .baselineValues(1, mapOfObject(
            parseBest("2018-10-21 04:51:36"), new BigDecimal("-100000.000"),
            parseBest("2017-07-11 09:26:48"), new BigDecimal("102030.001")
        ))
        .baselineValues(2, mapOfObject(
            parseBest("1913-03-03 18:47:14"), new BigDecimal("84.509")
        ))
        .baselineValues(3, mapOfObject(
            parseBest("2016-01-21 12:39:30"), new BigDecimal("906668.849")
        ))
        .go();
  }

  @Test
  public void mapCharToTinyint() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, char_tinyint FROM hive.map_tbl")
        .unOrdered()
        .baselineColumns("rid", "char_tinyint")
        .baselineValues(1, mapOfObject("MN", -128, "MX", 127, "ZR", 0))
        .baselineValues(2, mapOfObject("ls", 1, "ks", 2))
        .baselineValues(3, mapOfObject("fx", 20, "fy", 30, "fz", 40, "fk", -31))
        .go();
  }

  @Test
  public void mapDateToBoolean() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, date_boolean FROM hive.map_tbl")
        .unOrdered()
        .baselineColumns("rid", "date_boolean")
        .baselineValues(1, mapOfObject(
            parseLocalDate("1965-12-15"), true, parseLocalDate("1970-09-02"), false,
            parseLocalDate("2025-05-25"), true, parseLocalDate("2919-01-17"), false
        ))
        .baselineValues(2, mapOfObject(
            parseLocalDate("1944-05-09"), false, parseLocalDate("2002-02-11"), true
        ))
        .baselineValues(3, mapOfObject(
            parseLocalDate("2068-10-05"), false, parseLocalDate("2051-07-27"), false,
            parseLocalDate("2052-08-28"), true
        ))
        .go();
  }

  @Test
  public void mapDoubleToFloat() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, double_float FROM hive.map_tbl")
        .unOrdered()
        .baselineColumns("rid", "double_float")
        .baselineValues(1, mapOfObject(
            0.47745359256854, -5.3763375f
        ))
        .baselineValues(2, mapOfObject(
            -0.47745359256854, -0.6549191f,
            -13.241563769628, -82.399826f,
            0.3436367772981237, 12.633938f,
            9.73366, 86.19402f
        ))
        .baselineValues(3, mapOfObject(
            170000000.00, 9867.5623f
        ))
        .go();
  }

  @Test
  public void mapVarcharToBigint() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, varchar_bigint FROM hive.map_tbl")
        .unOrdered()
        .baselineColumns("rid", "varchar_bigint")
        .baselineValues(1, mapOfObject("m", -3226305034926780974L))
        .baselineValues(2, mapOfObject("MBAv", 0L))
        .baselineValues(3, mapOfObject("7R9F", -2077124879355227614L, "12AAa", -6787493227661516341L))
        .go();
  }

  @Test
  public void mapBooleanToSmallint() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, boolean_smallint FROM hive.map_tbl")
        .unOrdered()
        .baselineColumns("rid", "boolean_smallint")
        .baselineValues(1, mapOfObject(true, -19088))
        .baselineValues(2, mapOfObject(false, -4774))
        .baselineValues(3, mapOfObject(false, 32767, true, 25185))
        .go();
  }

  @Test
  public void mapDecimalToChar() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, decimal_char FROM hive.map_tbl")
        .unOrdered()
        .baselineColumns("rid", "decimal_char")
        .baselineValues(1, mapOfObject(
            new BigDecimal("-3.930"), "L"))
        .baselineValues(2, mapOfObject(
            new BigDecimal("-0.600"), "P", new BigDecimal("21.555"), "C", new BigDecimal("99.999"), "X"))
        .baselineValues(3, mapOfObject(
            new BigDecimal("-444023.971"), "L", new BigDecimal("827746.528"), "A"))
        .go();
  }

  @Test
  public void mapIntToStringParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "map_tbl_p");
    testBuilder()
        .sqlQuery("SELECT rid, int_string FROM hive.map_tbl_p")
        .unOrdered()
        .baselineColumns("rid", "int_string")
        .baselineValues(1, mapOfObject(1, "First", 2, "Second", 3, "Third"))
        .baselineValues(2, mapOfObject(4, "Fourth", 5, "Fifth"))
        .baselineValues(3, mapOfObject(6, "Sixth", 2, "!!"))
        .go();
  }

  @Test
  public void mapTimestampToDecimalParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "map_tbl_p");
    testBuilder()
        .sqlQuery("SELECT rid, timestamp_decimal FROM hive.map_tbl_p")
        .optionSettingQueriesForTestQuery("alter session set `" + ExecConstants.PARQUET_READER_INT96_AS_TIMESTAMP + "` = true")
        .unOrdered()
        .baselineColumns("rid", "timestamp_decimal")
        .baselineValues(1, mapOfObject(
            parseBest("2018-10-21 04:51:36"), new BigDecimal("-100000.000"),
            parseBest("2017-07-11 09:26:48"), new BigDecimal("102030.001")
        ))
        .baselineValues(2, mapOfObject(
            parseBest("1913-03-03 18:47:14"), new BigDecimal("84.509")
        ))
        .baselineValues(3, mapOfObject(
            parseBest("2016-01-21 12:39:30"), new BigDecimal("906668.849")
        ))
        .go();
  }

  @Test
  public void mapCharToTinyintParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "map_tbl_p");
    testBuilder()
        .sqlQuery("SELECT rid, char_tinyint FROM hive.map_tbl_p")
        .unOrdered()
        .baselineColumns("rid", "char_tinyint")
        .baselineValues(1, mapOfObject("MN", -128, "MX", 127, "ZR", 0))
        .baselineValues(2, mapOfObject("ls", 1, "ks", 2))
        .baselineValues(3, mapOfObject("fx", 20, "fy", 30, "fz", 40, "fk", -31))
        .go();
  }

  @Test
  public void mapDateToBooleanParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "map_tbl_p");
    testBuilder()
        .sqlQuery("SELECT rid, date_boolean FROM hive.map_tbl_p")
        .unOrdered()
        .baselineColumns("rid", "date_boolean")
        .baselineValues(1, mapOfObject(
            parseLocalDate("1965-12-15"), true, parseLocalDate("1970-09-02"), false,
            parseLocalDate("2025-05-25"), true, parseLocalDate("2919-01-17"), false
        ))
        .baselineValues(2, mapOfObject(
            parseLocalDate("1944-05-09"), false, parseLocalDate("2002-02-11"), true
        ))
        .baselineValues(3, mapOfObject(
            parseLocalDate("2068-10-05"), false, parseLocalDate("2051-07-27"), false,
            parseLocalDate("2052-08-28"), true
        ))
        .go();
  }

  @Test
  public void mapDoubleToFloatParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "map_tbl_p");
    testBuilder()
        .sqlQuery("SELECT rid, double_float FROM hive.map_tbl_p")
        .unOrdered()
        .baselineColumns("rid", "double_float")
        .baselineValues(1, mapOfObject(
            0.47745359256854, -5.3763375f
        ))
        .baselineValues(2, mapOfObject(
            -0.47745359256854, -0.6549191f,
            -13.241563769628, -82.399826f,
            0.3436367772981237, 12.633938f,
            9.73366, 86.19402f
        ))
        .baselineValues(3, mapOfObject(
            170000000.00, 9867.5623f
        ))
        .go();
  }

  @Test
  public void mapVarcharToBigintParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "map_tbl_p");
    testBuilder()
        .sqlQuery("SELECT rid, varchar_bigint FROM hive.map_tbl_p")
        .unOrdered()
        .baselineColumns("rid", "varchar_bigint")
        .baselineValues(1, mapOfObject("m", -3226305034926780974L))
        .baselineValues(2, mapOfObject("MBAv", 0L))
        .baselineValues(3, mapOfObject("7R9F", -2077124879355227614L, "12AAa", -6787493227661516341L))
        .go();
  }

  @Test
  public void mapBooleanToSmallintParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "map_tbl_p");
    testBuilder()
        .sqlQuery("SELECT rid, boolean_smallint FROM hive.map_tbl_p")
        .unOrdered()
        .baselineColumns("rid", "boolean_smallint")
        .baselineValues(1, mapOfObject(true, -19088))
        .baselineValues(2, mapOfObject(false, -4774))
        .baselineValues(3, mapOfObject(false, 32767, true, 25185))
        .go();
  }

  @Test
  public void mapDecimalToCharParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "map_tbl_p");
    testBuilder()
        .sqlQuery("SELECT rid, decimal_char FROM hive.map_tbl_p")
        .unOrdered()
        .baselineColumns("rid", "decimal_char")
        .baselineValues(1, mapOfObject(
            new BigDecimal("-3.930"), "L"))
        .baselineValues(2, mapOfObject(
            new BigDecimal("-0.600"), "P", new BigDecimal("21.555"), "C", new BigDecimal("99.999"), "X"))
        .baselineValues(3, mapOfObject(
            new BigDecimal("-444023.971"), "L", new BigDecimal("827746.528"), "A"))
        .go();
  }

  @Test
  public void nestedMap() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, map_n_1 FROM hive.map_complex_tbl")
        .unOrdered()
        .baselineColumns("rid", "map_n_1")
        .baselineValues(1, mapOfObject(1, mapOfObject("A-0", 21, "A-1", 22)))
        .baselineValues(2, mapOfObject(1, mapOfObject("A+0", 12, "A-1", 22)))
        .baselineValues(3, mapOfObject(1, mapOfObject("A-0", 11, "A+1", 11)))
        .go();
  }

  @Test
  public void doublyNestedMap() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, map_n_2 FROM hive.map_complex_tbl")
        .unOrdered()
        .baselineColumns("rid", "map_n_2")
        .baselineValues(1, mapOfObject(
            3, mapOfObject(true, mapOfObject("k1", 1, "k2", 2), false, mapOfObject("k3", 1, "k4", 2))
        ))
        .baselineValues(2, mapOfObject(
            3, mapOfObject(true, mapOfObject("k1", 1, "k2", 2), false, mapOfObject("k3", 1, "k4", 2)),
            4, mapOfObject(true, mapOfObject("k1", 1, "k2", 2))
        ))
        .baselineValues(3, mapOfObject(
            3, mapOfObject(false, mapOfObject("k1", 1, "k2", 2))
        ))
        .go();
  }

  @Test
  public void mapWithArrayValue() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, map_arr FROM hive.map_complex_tbl")
        .unOrdered()
        .baselineColumns("rid", "map_arr")
        .baselineValues(1, mapOfObject("a1", asList(0, 9, 8), "a2", asList(-9, 0, 1)))
        .baselineValues(2, mapOfObject("a1", asList(7, 7, 7)))
        .baselineValues(3, mapOfObject("x", asList(5, 6, 7, 8, 9, 10, 100), "y", asList(0, 0, 0, 1, 0, 1, 0, 1)))
        .go();
  }

  @Test
  public void mapWithNestedArrayValue() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, map_arr_2 FROM hive.map_complex_tbl")
        .unOrdered()
        .baselineColumns("rid", "map_arr_2")
        .baselineValues(1, mapOfObject("aa1", asList(asList(-7, 3, 1), asList(0), asList(-2, -22))))
        .baselineValues(2, mapOfObject("1a1", asList(asList(-7, 3, 10, -2, -22), asList(0, -1, 0))))
        .baselineValues(3, mapOfObject("aa1", asList(asList(0))))
        .go();
  }

  @Test
  public void mapWithArrayOfMapsValue() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, map_arr_map FROM hive.map_complex_tbl")
        .unOrdered()
        .baselineColumns("rid", "map_arr_map")
        .baselineValues(1, mapOfObject(
            "key01", asList(mapOfObject("key01.0", 0), mapOfObject("key01.1", 1), mapOfObject("key01.2", 2), mapOfObject("key01.3", 3))
        ))
        .baselineValues(2, mapOfObject(
            "key01", asList(mapOfObject("key01.0", 0), mapOfObject("key01.1", 1)), "key02", asList(mapOfObject("key02.0", 0))
        ))
        .baselineValues(3, mapOfObject(
            "key01", asList(mapOfObject("key01.0", 0))
        ))
        .go();
  }

  @Test
  public void mapWithStructValue() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, map_struct FROM hive.map_complex_tbl")
        .unOrdered()
        .baselineColumns("rid", "map_struct")
        .baselineValues(1, mapOfObject(
            "a", mapOf("fs", "(0-0)", "fi", 101),
            "b", mapOf("fs", "=-=", "fi", 202)
        ))
        .baselineValues(2, mapOfObject(
            "a", mapOf("fs", "|>-<|", "fi", 888),
            "c", mapOf("fs", "//*?//;..*/", "fi", 1021)
        ))
        .baselineValues(3, mapOfObject(
            "c", mapOf("fs", "<<`~`~`~`>>", "fi", 9889)
        ))
        .go();
  }

  @Test
  public void mapWithStructMapValue() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, map_struct_map FROM hive.map_complex_tbl")
        .unOrdered()
        .baselineColumns("rid", "map_struct_map")
        .baselineValues(1, mapOfObject(
            "z", mapOf("i", 1, "m", mapOfObject(1, 1, 3, 2, 7, 0)),
            "zz", mapOf("i", 2, "m", mapOfObject(0, 0)),
            "zzz", mapOf("i", 3, "m", mapOfObject(2, 2))
        ))
        .baselineValues(2, mapOfObject(
            "x", mapOf("i", 2, "m", mapOfObject(0, 2, 3, 1))
        ))
        .baselineValues(3, mapOfObject(
            "x", mapOf("i", 3, "m", mapOfObject(0, 0, 1, 1)),
            "z", mapOf("i", 4, "m", mapOfObject(3, 3))
        ))
        .go();
  }

  @Test
  public void getByKeyP0() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mp.int_string[2] p0 FROM hive.map_tbl mp")
        .unOrdered()
        .baselineColumns("rid", "p0")
        .baselineValues(1, "Second")
        .baselineValues(2, null)
        .baselineValues(3, "!!")
        .go();
  }

  @Test
  public void getByKeyP1() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mp.timestamp_decimal[CAST('2018-10-21 04:51:36' as TIMESTAMP)] p1 FROM hive.map_tbl mp")
        .unOrdered()
        .baselineColumns("rid", "p1")
        .baselineValues(1, new BigDecimal("-100000.000"))
        .baselineValues(2, null)
        .baselineValues(3, null)
        .go();
  }

  @Test
  public void getByKeyP2() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mp.char_tinyint.fk p2 FROM hive.map_tbl mp")
        .unOrdered()
        .baselineColumns("rid", "p2")
        .baselineValues(1, null)
        .baselineValues(2, null)
        .baselineValues(3, -31)
        .go();
  }

  @Test
  public void getByKeyP3() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mp.date_boolean[CAST('2025-05-25' as DATE)] p3 FROM hive.map_tbl mp")
        .unOrdered()
        .baselineColumns("rid", "p3")
        .baselineValues(1, true)
        .baselineValues(2, null)
        .baselineValues(3, null)
        .go();
  }

  @Test
  public void getByKeyP4() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mp.varchar_bigint['12AAa'] p4 FROM hive.map_tbl mp")
        .unOrdered()
        .baselineColumns("rid", "p4")
        .baselineValues(1, null)
        .baselineValues(2, null)
        .baselineValues(3, -6787493227661516341L)
        .go();
  }

  @Test
  public void getByKeyP5() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mp.boolean_smallint[true] p5 FROM hive.map_tbl mp")
        .unOrdered()
        .baselineColumns("rid", "p5")
        .baselineValues(1, -19088)
        .baselineValues(2, null)
        .baselineValues(3, 25185)
        .go();
  }

  @Test
  public void getByKeyP6() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mp.decimal_char[99.999] p6 FROM hive.map_tbl mp")
        .unOrdered()
        .baselineColumns("rid", "p6")
        .baselineValues(1, null)
        .baselineValues(2, "X")
        .baselineValues(3, null)
        .go();
  }

  @Test
  public void getByKeyP7() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_n_1[1] p7 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p7")
        .baselineValues(1, mapOfObject("A-0", 21, "A-1", 22))
        .baselineValues(2, mapOfObject("A+0", 12, "A-1", 22))
        .baselineValues(3, mapOfObject("A-0", 11, "A+1", 11))
        .go();
  }

  @Test
  public void getByKeyP8() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_n_1[1]['A-0'] p8 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p8")
        .baselineValues(1, 21)
        .baselineValues(2, null)
        .baselineValues(3, 11)
        .go();
  }

  @Test
  public void getByKeyP9() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_n_2[4] p9 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p9")
        .baselineValues(1, emptyMap())
        .baselineValues(2, mapOfObject(true, mapOfObject("k1", 1, "k2", 2)))
        .baselineValues(3, emptyMap())
        .go();
  }

  @Test
  public void getByKeyP10() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_n_2[3][true] p10 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p10")
        .baselineValues(1, mapOfObject("k1", 1, "k2", 2))
        .baselineValues(2, mapOfObject("k1", 1, "k2", 2))
        .baselineValues(3, mapOfObject())
        .go();
  }

  @Test
  public void getByKeyP11() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_n_2[3][true]['k2'] p11 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p11")
        .baselineValues(1, 2)
        .baselineValues(2, 2)
        .baselineValues(3, null)
        .go();
  }

  @Test
  public void getByKeyP12() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_arr['a1'] p12 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p12")
        .baselineValues(1, asList(0, 9, 8))
        .baselineValues(2, asList(7, 7, 7))
        .baselineValues(3, emptyList())
        .go();
  }

  @Test
  public void getByKeyP13() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_arr['a1'][2] p13 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p13")
        .baselineValues(1, 8)
        .baselineValues(2, 7)
        .baselineValues(3, null)
        .go();
  }

  @Test
  public void getByKeyP14() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_arr_2['aa1'][0] p14 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p14")
        .baselineValues(1, asList(-7, 3, 1))
        .baselineValues(2, emptyList())
        .baselineValues(3, asList(0))
        .go();
  }

  @Test
  public void getByKeyP15() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_arr_map['key01'][1] p15 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p15")
        .baselineValues(1, mapOfObject("key01.1", 1))
        .baselineValues(2, mapOfObject("key01.1", 1))
        .baselineValues(3, emptyMap())
        .go();
  }

  @Test
  public void getByKeyP16() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_arr_map['key01'][1]['key01.1'] p16 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p16")
        .baselineValues(1, 1)
        .baselineValues(2, 1)
        .baselineValues(3, null)
        .go();
  }

  @Test
  public void getByKeyP17() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_struct['a'] p17 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p17")
        .baselineValues(1, mapOf("fs", "(0-0)", "fi", 101))
        .baselineValues(2, mapOf("fs", "|>-<|", "fi", 888))
        .baselineValues(3, mapOf())
        .go();
  }

  @Test
  public void getByKeyP18() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_struct['c']['fs'] p18 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p18")
        .baselineValues(1, null)
        .baselineValues(2, "//*?//;..*/")
        .baselineValues(3, "<<`~`~`~`>>")
        .go();
  }

  @Test
  public void getByKeyP19() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_struct_map['z']['i'] p19 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p19")
        .baselineValues(1, 1)
        .baselineValues(2, null)
        .baselineValues(3, 4)
        .go();
  }

  @Test
  public void getByKeyP20() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_struct_map['z']['m'] p20 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p20")
        .baselineValues(1, mapOfObject(1, 1, 3, 2, 7, 0))
        .baselineValues(2, emptyMap())
        .baselineValues(3, mapOfObject(3, 3))
        .go();
  }

  @Test
  public void getByKeyP21() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_struct_map['z']['m'][3] p21 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p21")
        .baselineValues(1, 2)
        .baselineValues(2, null)
        .baselineValues(3, 3)
        .go();
  }

  @Test
  public void getByKeyP22() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, mc.map_struct_map.z.m[3] p22 FROM hive.map_complex_tbl mc")
        .unOrdered()
        .baselineColumns("rid", "p22")
        .baselineValues(1, 2)
        .baselineValues(2, null)
        .baselineValues(3, 3)
        .go();
  }

  @Test
  public void countMapColumn() throws Exception {
    testBuilder()
        .sqlQuery("SELECT COUNT(int_string) AS cnt FROM hive.map_tbl")
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(3L)
        .go();
  }

  @Test
  public void typeOfFunctions() throws Exception {
    testBuilder()
        .sqlQuery("SELECT sqlTypeOf(%1$s) sto, typeOf(%1$s) to, modeOf(%1$s) mo, drillTypeOf(%1$s) dto " +
            "FROM hive.map_tbl LIMIT 1", "int_string")
        .unOrdered()
        .baselineColumns("sto", "to",                "mo",       "dto")
        .baselineValues( "MAP", "DICT<INT,VARCHAR>", "NOT NULL", "DICT")
        .go();
  }

  @Test
  public void mapStringToUnion() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, map_u FROM hive.map_union_tbl")
        .unOrdered()
        .baselineColumns("rid", "map_u")
        .baselineValues(1, mapOfObject("10", "TextTextText", "15", true, "20", 100100))
        .baselineValues(2, mapOfObject("20", false, "25", "TextTextText", "30", true))
        .baselineValues(3, mapOfObject("30", "TextTextText", "35", 200200, "10", true))
        .go();
  }
}
