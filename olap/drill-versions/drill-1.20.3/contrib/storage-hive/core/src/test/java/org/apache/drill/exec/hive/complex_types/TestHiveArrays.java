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

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.drill.categories.HiveStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.hive.HiveClusterTest;
import org.apache.drill.exec.hive.HiveTestFixture;
import org.apache.drill.exec.hive.HiveTestUtilities;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.apache.drill.exec.util.Text;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.TestBuilder;
import org.apache.hadoop.hive.ql.Driver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.drill.exec.expr.fn.impl.DateUtility.parseBest;
import static org.apache.drill.exec.expr.fn.impl.DateUtility.parseLocalDate;
import static org.apache.drill.exec.hive.HiveTestUtilities.assertNativeScanUsed;
import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOfObject;

@Category({SlowTest.class, HiveStorageTest.class})
public class TestHiveArrays extends HiveClusterTest {

  private static HiveTestFixture hiveTestFixture;

  private static final String[] TYPES = {"int", "string", "varchar(5)", "char(2)", "tinyint",
      "smallint", "decimal(9,3)", "boolean", "bigint", "float", "double", "date", "timestamp"};

  @BeforeClass
  public static void setUp() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher)
        .sessionOption(ExecConstants.HIVE_OPTIMIZE_PARQUET_SCAN_WITH_NATIVE_READER, true));
    hiveTestFixture = HiveTestFixture.builder(dirTestWatcher).build();
    hiveTestFixture.getDriverManager().runWithinSession(TestHiveArrays::generateData);
    hiveTestFixture.getPluginManager().addHivePluginTo(cluster.drillbit());
  }

  @AfterClass
  public static void tearDown() {
    if (hiveTestFixture != null) {
      hiveTestFixture.getPluginManager().removeHivePluginFrom(cluster.drillbit());
    }
  }

  private static void generateData(Driver d) {
    Stream.of(TYPES).forEach(type -> {
      createJsonTable(d, type);
      createParquetTable(d, type);
    });

    // binary_array
    HiveTestUtilities.executeQuery(d, "CREATE TABLE binary_array(arr_n_0 ARRAY<BINARY>) STORED AS TEXTFILE");
    HiveTestUtilities.executeQuery(d, "insert into binary_array select array(binary('First'),binary('Second'),binary('Third'))");
    HiveTestUtilities.executeQuery(d, "insert into binary_array select array(binary('First'))");

    // arr_hive_view
    HiveTestUtilities.executeQuery(d, "CREATE VIEW arr_view AS " +
        "SELECT " +
        "   int_array.rid as vwrid," +
        "   int_array.arr_n_0 as int_n0," +
        "   int_array.arr_n_1 as int_n1," +
        "   string_array.arr_n_0 as string_n0," +
        "   string_array.arr_n_1 as string_n1," +
        "   varchar_array.arr_n_0 as varchar_n0," +
        "   varchar_array.arr_n_1 as varchar_n1," +
        "   char_array.arr_n_0 as char_n0," +
        "   char_array.arr_n_1 as char_n1," +
        "   tinyint_array.arr_n_0 as tinyint_n0," +
        "   tinyint_array.arr_n_1 as tinyint_n1," +
        "   smallint_array.arr_n_0 as smallint_n0," +
        "   smallint_array.arr_n_1 as smallint_n1," +
        "   decimal_array.arr_n_0 as decimal_n0," +
        "   decimal_array.arr_n_1 as decimal_n1," +
        "   boolean_array.arr_n_0 as boolean_n0," +
        "   boolean_array.arr_n_1 as boolean_n1," +
        "   bigint_array.arr_n_0 as bigint_n0," +
        "   bigint_array.arr_n_1 as bigint_n1," +
        "   float_array.arr_n_0 as float_n0," +
        "   float_array.arr_n_1 as float_n1," +
        "   double_array.arr_n_0 as double_n0," +
        "   double_array.arr_n_1 as double_n1," +
        "   date_array.arr_n_0 as date_n0," +
        "   date_array.arr_n_1 as date_n1," +
        "   timestamp_array.arr_n_0 as timestamp_n0," +
        "   timestamp_array.arr_n_1 as timestamp_n1 " +
        "FROM " +
        "   int_array," +
        "   string_array," +
        "   varchar_array," +
        "   char_array," +
        "   tinyint_array," +
        "   smallint_array," +
        "   decimal_array," +
        "   boolean_array," +
        "   bigint_array," +
        "   float_array," +
        "   double_array," +
        "   date_array," +
        "   timestamp_array " +
        "WHERE " +
        "   int_array.rid=string_array.rid AND" +
        "   int_array.rid=varchar_array.rid AND" +
        "   int_array.rid=char_array.rid AND" +
        "   int_array.rid=tinyint_array.rid AND" +
        "   int_array.rid=smallint_array.rid AND" +
        "   int_array.rid=decimal_array.rid AND" +
        "   int_array.rid=boolean_array.rid AND" +
        "   int_array.rid=bigint_array.rid AND" +
        "   int_array.rid=float_array.rid AND" +
        "   int_array.rid=double_array.rid AND" +
        "   int_array.rid=date_array.rid AND" +
        "   int_array.rid=timestamp_array.rid "
    );

    HiveTestUtilities.executeQuery(d,
        "CREATE TABLE struct_array(rid INT, " +
            "arr_n_0 ARRAY<STRUCT<a:INT,b:BOOLEAN,c:STRING>>," +
            "arr_n_1 ARRAY<ARRAY<STRUCT<x:DOUBLE,y:DOUBLE>>>, " +
            "arr_n_2 ARRAY<ARRAY<ARRAY<STRUCT<t:INT,d:DATE>>>>" +
            ") " +
            "ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe' STORED AS TEXTFILE"
    );
    HiveTestUtilities.loadData(d, "struct_array", Paths.get("complex_types/array/struct_array.json"));

    HiveTestUtilities.executeQuery(d,
        "CREATE TABLE struct_array_p(rid INT, " +
            "arr_n_0 ARRAY<STRUCT<a:INT,b:BOOLEAN,c:STRING>>," +
            "arr_n_1 ARRAY<ARRAY<STRUCT<x:DOUBLE,y:DOUBLE>>>, " +
            "arr_n_2 ARRAY<ARRAY<ARRAY<STRUCT<t:INT,d:DATE>>>>" +
            ") " +
            "STORED AS PARQUET");
    HiveTestUtilities.insertData(d, "struct_array", "struct_array_p");

    HiveTestUtilities.executeQuery(d,
        "CREATE TABLE map_array(rid INT, " +
            "arr_n_0 ARRAY<MAP<INT,BOOLEAN>>," +
            "arr_n_1 ARRAY<ARRAY<MAP<CHAR(2),INT>>>, " +
            "arr_n_2 ARRAY<ARRAY<ARRAY<MAP<INT,DATE>>>>" +
            ") " +
            "ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe' STORED AS TEXTFILE");
    HiveTestUtilities.loadData(d, "map_array", Paths.get("complex_types/array/map_array.json"));

    String arrayUnionDdl = "CREATE TABLE " +
        "union_array(rid INT, un_arr ARRAY<UNIONTYPE<INT, STRING, BOOLEAN, FLOAT>>) " +
        "ROW FORMAT DELIMITED" +
        " FIELDS TERMINATED BY ','" +
        " COLLECTION ITEMS TERMINATED BY '&'" +
        " MAP KEYS TERMINATED BY '#'" +
        " LINES TERMINATED BY '\\n'" +
        " STORED AS TEXTFILE";
    HiveTestUtilities.executeQuery(d, arrayUnionDdl);
    HiveTestUtilities.loadData(d,"union_array", Paths.get("complex_types/array/union_array.txt"));

  }

  private static void createJsonTable(Driver d, String type) {
    String tableName = getTableNameFromType(type);
    String ddl = String.format(
        "CREATE TABLE %s(rid INT, arr_n_0 ARRAY<%2$s>, arr_n_1 ARRAY<ARRAY<%2$s>>, arr_n_2 ARRAY<ARRAY<ARRAY<%2$s>>>) " +
            "ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe' STORED AS TEXTFILE",
        tableName, type.toUpperCase());

    HiveTestUtilities.executeQuery(d, ddl);
    HiveTestUtilities.loadData(d, tableName, Paths.get(String.format("complex_types/array/%s.json", tableName)));
  }

  private static void createParquetTable(Driver d, String type) {
    String from = getTableNameFromType(type);
    String to = from.concat("_p");
    String ddl = String.format(
        "CREATE TABLE %s(rid INT, arr_n_0 ARRAY<%2$s>, arr_n_1 ARRAY<ARRAY<%2$s>>, arr_n_2 ARRAY<ARRAY<ARRAY<%2$s>>>) STORED AS PARQUET",
        to, type.toUpperCase());
    HiveTestUtilities.executeQuery(d, ddl);
    HiveTestUtilities.insertData(d, from, to);
  }

  private static String getTableNameFromType(String type) {
    String tblType = type.split("\\(")[0];
    return tblType.toLowerCase() + "_array";
  }

  @Test
  public void intArray() throws Exception {
    checkIntArrayInTable("int_array");
  }

  @Test
  public void intArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "int_array_p");
    checkIntArrayInTable("int_array_p");
  }

  private void checkIntArrayInTable(String tableName) throws Exception {
    // Nesting 0: reading ARRAY<INT>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", tableName)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(-1, 0, 1))
        .baselineValues(emptyList())
        .baselineValues(asList(100500))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<INT>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", tableName)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asList(-1, 0, 1), asList(-2, 1)))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(100500, 500100)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<INT>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", tableName)
        .ordered()
        .baselineColumns("arr_n_2")
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
  public void intArrayInJoin() throws Exception {
    testBuilder()
        .sqlQuery("SELECT a.rid as gid, a.arr_n_0 as an0, b.arr_n_0 as bn0 " +
            "FROM hive.int_array a " +
            "INNER JOIN hive.int_array b " +
            "ON a.rid=b.rid WHERE a.rid=1")
        .unOrdered()
        .baselineColumns("gid", "an0", "bn0")
        .baselineValues(1, asList(-1, 0, 1), asList(-1, 0, 1))
        .go();
    testBuilder()
        .sqlQuery("SELECT * FROM (SELECT a.rid as gid, a.arr_n_0 as an0, b.arr_n_0 as bn0,c.arr_n_0 as cn0 " +
            "FROM hive.int_array a,hive.int_array b, hive.int_array c " +
            "WHERE a.rid=b.rid AND a.rid=c.rid) WHERE gid=1")
        .unOrdered()
        .baselineColumns("gid", "an0", "bn0", "cn0")
        .baselineValues(1, asList(-1, 0, 1), asList(-1, 0, 1), asList(-1, 0, 1))
        .go();
  }

  @Test
  public void intArrayByIndex() throws Exception {
    // arr_n_0 array<int>, arr_n_1 array<array<int>>
    testBuilder()
        .sqlQuery("SELECT arr_n_0[0], arr_n_0[1], arr_n_1[0], arr_n_1[1], arr_n_0[3], arr_n_1[3] FROM hive.`int_array`")
        .unOrdered()
        .baselineColumns("EXPR$0", "EXPR$1", "EXPR$2", "EXPR$3", "EXPR$4", "EXPR$5")
        .baselineValues(-1, 0, asList(-1, 0, 1), asList(-2, 1), null, emptyList())
        .baselineValues(null, null, emptyList(), emptyList(), null, emptyList())
        .baselineValues(100500, null, asList(100500, 500100), emptyList(), null, emptyList())
        .go();
  }

  @Test
  public void intArrayFlatten() throws Exception {
    // arr_n_0 array<int>, arr_n_1 array<array<int>>
    testBuilder()
        .sqlQuery("SELECT rid, FLATTEN(arr_n_0) FROM hive.`int_array`")
        .unOrdered()
        .baselineColumns("rid", "EXPR$1")
        .baselineValues(1, -1)
        .baselineValues(1, 0)
        .baselineValues(1, 1)
        .baselineValues(3, 100500)
        .go();

    testBuilder()
        .sqlQuery("SELECT rid, FLATTEN(arr_n_1) FROM hive.`int_array`")
        .unOrdered()
        .baselineColumns("rid", "EXPR$1")
        .baselineValues(1, asList(-1, 0, 1))
        .baselineValues(1, asList(-2, 1))
        .baselineValues(2, emptyList())
        .baselineValues(2, emptyList())
        .baselineValues(3, asList(100500, 500100))
        .go();

    testBuilder()
        .sqlQuery("SELECT rid, FLATTEN(FLATTEN(arr_n_1)) FROM hive.`int_array`")
        .unOrdered()
        .baselineColumns("rid", "EXPR$1")
        .baselineValues(1, -1)
        .baselineValues(1, 0)
        .baselineValues(1, 1)
        .baselineValues(1, -2)
        .baselineValues(1, 1)
        .baselineValues(3, 100500)
        .baselineValues(3, 500100)
        .go();
  }

  @Test
  public void intArrayRepeatedCount() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, REPEATED_COUNT(arr_n_0), REPEATED_COUNT(arr_n_1) FROM hive.`int_array`")
        .unOrdered()
        .baselineColumns("rid", "EXPR$1", "EXPR$2")
        .baselineValues(1, 3, 2)
        .baselineValues(2, 0, 2)
        .baselineValues(3, 1, 1)
        .go();
  }

  @Test
  public void intArrayRepeatedContains() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid FROM hive.`int_array` WHERE REPEATED_CONTAINS(arr_n_0, 100500)")
        .unOrdered()
        .baselineColumns("rid")
        .baselineValues(3)
        .go();
  }

  @Test
  public void intArrayDescribe() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE hive.`int_array` arr_n_0")
        .unOrdered()
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("arr_n_0", "ARRAY", "YES")//todo: fix to ARRAY<INTEGER>
        .go();
    testBuilder()
        .sqlQuery("DESCRIBE hive.`int_array` arr_n_1")
        .unOrdered()
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("arr_n_1", "ARRAY", "YES") // todo: ARRAY<ARRAY<INTEGER>>
        .go();
  }

  @Test
  public void intArrayTypeOfKindFunctions() throws Exception {
    testBuilder()
        .sqlQuery("select " +
            "sqlTypeOf(arr_n_0), sqlTypeOf(arr_n_1),  " +
            "typeOf(arr_n_0), typeOf(arr_n_1), " +
            "modeOf(arr_n_0), modeOf(arr_n_1), " +
            "drillTypeOf(arr_n_0), drillTypeOf(arr_n_1) " +
            "from hive.`int_array` limit 1")
        .unOrdered()
        .baselineColumns(
            "EXPR$0", "EXPR$1",
            "EXPR$2", "EXPR$3",
            "EXPR$4", "EXPR$5",
            "EXPR$6", "EXPR$7"
        )
        .baselineValues(
            "INTEGER", "ARRAY", // why not ARRAY<INTEGER> | ARRAY<ARRAY<INTEGER>> ?
            "INT", "LIST",    // todo: is it ok ?
            "ARRAY", "ARRAY",
            "INT", "LIST"    // todo: is it ok ?
        )
        .go();
  }

  @Test
  public void stringArray() throws Exception {
    checkStringArrayInTable("string_array");
  }

  @Test
  public void stringArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "string_array_p");
    checkStringArrayInTable("string_array_p");
  }

  private void checkStringArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<STRING>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asTextList("First Value Of Array", "komlnp", "The Last Value"))
        .baselineValues(emptyList())
        .baselineValues(asTextList("ABCaBcA-1-2-3"))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<STRING>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asTextList("Array 0, Value 0", "Array 0, Value 1"), asTextList("Array 1")))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asTextList("One")))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<STRING>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(asList(
            asList(asTextList("dhMGOr1QVO", "NZpzBl", "LC8mjYyOJ7l8dHUpk")),
            asList(asTextList("JH"), asTextList("aVxgfxAu"), asTextList("fF amN8z8")),
            asList(asTextList("denwte5R39dSb2PeG", "Gbosj97RXTvBK1w", "S3whFvN"), asTextList("2sNbYGQhkt303Gnu", "rwG", "SQH766A8XwHg2pTA6a")),
            asList(asTextList("L", "khGFDtDluFNoo5hT"), asTextList("b8"), asTextList("Z")),
            asList(asTextList("DTEuW", "b0Wt84hIl", "A1H"), asTextList("h2zXh3Qc", "NOcgU8", "RGfVgv2rvDG"), asTextList("Hfn1ov9hB7fZN", "0ZgCD3"))
        ))
        .baselineValues(asList(
            asList(asTextList("nk", "HA", "CgAZCxTbTrFWJL3yM"), asTextList("T7fGXYwtBb", "G6vc"), asTextList("GrwB5j3LBy9"),
                asTextList("g7UreegD1H97", "dniQ5Ehhps7c1pBuM", "S wSNMGj7c"), asTextList("iWTEJS0", "4F")),
            asList(asTextList("YpRcC01u6i6KO", "ujpMrvEfUWfKm", "2d"), asTextList("2", "HVDH", "5Qx Q6W112"))
        ))
        .baselineValues(asList(
            asList(asTextList("S8d2vjNu680hSim6iJ"), asTextList("lRLaT9RvvgzhZ3C", "igSX1CP", "FFZMwMvAOod8"),
                asTextList("iBX", "sG"), asTextList("ChRjuDPz99WeU9", "2gBBmMUXV9E5E", " VkEARI2upO")),
            asList(asTextList("UgMok3Q5wmd"), asTextList("8Zf9CLfUSWK", "", "NZ7v"), asTextList("vQE3I5t26", "251BeQJue")),
            asList(asTextList("Rpo8")),
            asList(asTextList("jj3njyupewOM Ej0pu", "aePLtGgtyu4aJ5", "cKHSvNbImH1MkQmw0Cs"), asTextList("VSO5JgI2x7TnK31L5", "hIub", "eoBSa0zUFlwroSucU"),
                asTextList("V8Gny91lT", "5hBncDZ")),
            asList(asTextList("Y3", "StcgywfU", "BFTDChc"), asTextList("5JNwXc2UHLld7", "v"), asTextList("9UwBhJMSDftPKuGC"),
                asTextList("E hQ9NJkc0GcMlB", "IVND1Xp1Nnw26DrL9"))
        ))
        .go();
  }

  @Test
  public void stringArrayByIndex() throws Exception {
    // arr_n_0 array<string>, arr_n_1 array<array<string>>
    testBuilder()
        .sqlQuery("SELECT arr_n_0[0], arr_n_0[1], arr_n_1[0], arr_n_1[1], arr_n_0[3], arr_n_1[3] FROM hive.`string_array`")
        .unOrdered()
        .baselineColumns("EXPR$0", "EXPR$1", "EXPR$2", "EXPR$3", "EXPR$4", "EXPR$5")
        .baselineValues("First Value Of Array", "komlnp", asTextList("Array 0, Value 0", "Array 0, Value 1"), asTextList("Array 1"), null, emptyList())
        .baselineValues(null, null, emptyList(), emptyList(), null, emptyList())
        .baselineValues("ABCaBcA-1-2-3", null, asTextList("One"), emptyList(), null, emptyList())
        .go();
  }

  @Test
  public void varcharArray() throws Exception {
    // Nesting 0: reading ARRAY<VARCHAR(5)>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`varchar_array`")
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asTextList("Five", "One", "T"))
        .baselineValues(emptyList())
        .baselineValues(asTextList("ZZ0", "-c54g", "ooo", "k22k"))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<VARCHAR(5)>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`varchar_array`")
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asTextList("Five", "One", "$42"), asTextList("T", "K", "O")))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asTextList("-c54g")))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<VARCHAR(5)>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`varchar_array` order by rid")
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(asList(
            asList(asTextList(""), asTextList("Gt", "", ""), asTextList("9R3y"), asTextList("X3a4")),
            asList(asTextList("o", "6T", "QKAZ"), asTextList("", "xf8r", "As"), asTextList("5kS3")),
            asList(asTextList("", "S7Gx"), asTextList("ml", "27pL", "VPxr"), asTextList(""), asTextList("e", "Dj")),
            asList(asTextList("", "XYO", "fEWz"), asTextList("", "oU"), asTextList("o 8", "", ""),
                asTextList("giML", "H7g"), asTextList("SWX9", "H", "emwt")),
            asList(asTextList("Sp"))
        ))
        .baselineValues(asList(
            asList(asTextList("GCx"), asTextList("", "V"), asTextList("pF", "R7", ""), asTextList("", "AKal"))
        ))
        .baselineValues(asList(
            asList(asTextList("m", "MBAv", "7R9F"), asTextList("ovv"), asTextList("p 7l"))
        ))
        .go();
  }

  @Test
  public void varcharArrayByIndex() throws Exception {
    // arr_n_0 array<varchar>, arr_n_1 array<array<varchar>>
    testBuilder()
        .sqlQuery("SELECT arr_n_0[0], arr_n_0[1], arr_n_1[0], arr_n_1[1], arr_n_0[3], arr_n_1[3] FROM hive.`varchar_array`")
        .unOrdered()
        .baselineColumns("EXPR$0", "EXPR$1", "EXPR$2", "EXPR$3", "EXPR$4", "EXPR$5")
        .baselineValues("Five", "One", asTextList("Five", "One", "$42"), asTextList("T", "K", "O"), null, emptyList())
        .baselineValues(null, null, emptyList(), emptyList(), null, emptyList())
        .baselineValues("ZZ0", "-c54g", asTextList("-c54g"), emptyList(), "k22k", emptyList())
        .go();
  }

  @Test
  public void charArray() throws Exception {
    checkCharArrayInTable("char_array");
  }

  @Test
  public void charArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "char_array_p");
    checkCharArrayInTable("char_array_p");
  }

  private void checkCharArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<CHAR(2)>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asTextList("aa", "cc", "ot"))
        .baselineValues(emptyList())
        .baselineValues(asTextList("+a", "-c", "*t"))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<CHAR(2)>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asTextList("aa"), asTextList("cc", "ot")))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asTextList("*t")))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<CHAR(2)>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(asList(
            asList(asTextList("eT")),
            asList(asTextList("w9", "fC", "ww"), asTextList("3o", "f7", "Za"), asTextList("lX", "iv", "jI")),
            asList(asTextList("S3", "Qa", "aG"), asTextList("bj", "gc", "NO"))
        ))
        .baselineValues(asList(
            asList(asTextList("PV", "tH", "B7"), asTextList("uL"), asTextList("7b", "uf"), asTextList("zj"), asTextList("sA", "hf", "hR"))
        ))
        .baselineValues(asList(
            asList(asTextList("W1", "FS"), asTextList("le", "c0"), asTextList("", "0v")),
            asList(asTextList("gj"))
        ))
        .go();
  }

  @Test
  public void charArrayByIndex() throws Exception {
    // arr_n_0 array<char>, arr_n_1 array<array<char>>
    testBuilder()
        .sqlQuery("SELECT arr_n_0[0], arr_n_0[1], arr_n_1[0], arr_n_1[1], arr_n_0[3], arr_n_1[3] FROM hive.`char_array`")
        .unOrdered()
        .baselineColumns("EXPR$0", "EXPR$1", "EXPR$2", "EXPR$3", "EXPR$4", "EXPR$5")
        .baselineValues("aa", "cc", asTextList("aa"), asTextList("cc", "ot"), null, emptyList())
        .baselineValues(null, null, emptyList(), emptyList(), null, emptyList())
        .baselineValues("+a", "-c", asTextList("*t"), emptyList(), null, emptyList())
        .go();
  }

  @Test
  public void tinyintArray() throws Exception {
    checkTinyintArrayInTable("tinyint_array");
  }

  @Test
  public void tinyintArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "tinyint_array_p");
    checkTinyintArrayInTable("tinyint_array_p");
  }

  private void checkTinyintArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<TINYINT>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(-128, 0, 127))
        .baselineValues(emptyList())
        .baselineValues(asList(-101))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<TINYINT>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asList(-128, -127), asList(0, 1), asList(127, 126)))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(-102)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<TINYINT>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
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
  public void tinyintArrayByIndex() throws Exception {
    // arr_n_0 array<tinyint>, arr_n_1 array<array<tinyint>>
    testBuilder()
        .sqlQuery("SELECT arr_n_0[0], arr_n_0[1], arr_n_1[0], arr_n_1[1], arr_n_0[3], arr_n_1[3] FROM hive.`tinyint_array`")
        .unOrdered()
        .baselineColumns("EXPR$0", "EXPR$1", "EXPR$2", "EXPR$3", "EXPR$4", "EXPR$5")
        .baselineValues(-128, 0, asList(-128, -127), asList(0, 1), null, emptyList())
        .baselineValues(null, null, emptyList(), emptyList(), null, emptyList())
        .baselineValues(-101, null, asList(-102), emptyList(), null, emptyList())
        .go();
  }

  @Test
  public void smallintArray() throws Exception {
    checkSmallintArrayInTable("smallint_array");
  }

  @Test
  public void smallintArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "smallint_array_p");
    checkSmallintArrayInTable("smallint_array_p");
  }

  private void checkSmallintArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<SMALLINT>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(-32768, 0, 32767))
        .baselineValues(emptyList())
        .baselineValues(asList(10500))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<SMALLINT>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asList(-32768, -32768), asList(0, 0), asList(32767, 32767)))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(10500, 5010)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<SMALLINT>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
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
  public void decimalArray() throws Exception {
    checkDecimalArrayInTable("decimal_array");
  }

  @Test
  public void decimalArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "decimal_array_p");
    checkDecimalArrayInTable("decimal_array_p");
  }

  private void checkDecimalArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<DECIMAL(9,3)>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(new BigDecimal("-100000.000"), new BigDecimal("102030.001"), new BigDecimal("0.001")))
        .baselineValues(emptyList())
        .baselineValues(asList(new BigDecimal("-10.500")))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<DECIMAL(9,3)>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(
            asList(new BigDecimal("-100000.000"), new BigDecimal("102030.001")),
            asList(new BigDecimal("0.101"), new BigDecimal("0.102")),
            asList(new BigDecimal("0.001"), new BigDecimal("327670.001"))))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(new BigDecimal("10.500"), new BigDecimal("5.010"))))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<DECIMAL(9,3)>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(asList( // row
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
        ))
        .baselineValues(asList( // row
            asList( // [0]
                asList(new BigDecimal("375098.406"), new BigDecimal("84.509")),//[0][0]
                asList(new BigDecimal("-446325.287"), new BigDecimal("3.671")),//[0][1]
                asList(new BigDecimal("286958.380"), new BigDecimal("314821.890"), new BigDecimal("18513.303")),//[0][2]
                asList(new BigDecimal("-444023.971"), new BigDecimal("827746.528"), new BigDecimal("-54.986")),//[0][3]
                asList(new BigDecimal("-44520.406"))//[0][4]
            )
        ))
        .baselineValues(asList( // row
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
        ))
        .go();
  }

  @Test
  public void booleanArray() throws Exception {
    checkBooleanArrayInTable("boolean_array");
  }

  @Test
  public void booleanArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "boolean_array_p");
    checkBooleanArrayInTable("boolean_array_p");
  }

  private void checkBooleanArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<BOOLEAN>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(false, true, false, true, false))
        .baselineValues(emptyList())
        .baselineValues(Collections.singletonList(true))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<BOOLEAN>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asList(true, false, true), asList(false, false)))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(false, true)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<BOOLEAN>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
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
  public void bigintArray() throws Exception {
    checkBigintArrayInTable("bigint_array");
  }

  @Test
  public void bigintArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "bigint_array_p");
    checkBigintArrayInTable("bigint_array_p");
  }

  private void checkBigintArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<BIGINT>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(-9223372036854775808L, 0L, 10000000010L, 9223372036854775807L))
        .baselineValues(emptyList())
        .baselineValues(asList(10005000L))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<BIGINT>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asList(-9223372036854775808L, 0L, 10000000010L), asList(9223372036854775807L, 9223372036854775807L)))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(10005000L, 100050010L)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<BIGINT>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(asList(
            asList( // [0]
                asList(7345032157033769004L),//[0][0]
                asList(-2306607274383855051L, 3656249581579032003L)//[0][1]
            ),
            asList( // [1]
                asList(6044100897358387146L, 4737705104728607904L)//[1][0]
            )
        ))
        .baselineValues(asList(
            asList( // [0]
                asList(4833583793282587107L, -8917877693351417844L, -3226305034926780974L)//[0][0]
            )
        ))
        .baselineValues(asList(
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
        ))
        .go();
  }

  @Test
  public void floatArray() throws Exception {
    checkFloatArrayInTable("float_array");
  }

  @Test
  public void floatArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "float_array_p");
    checkFloatArrayInTable("float_array_p");
  }

  private void checkFloatArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<FLOAT>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(-32.058f, 94.47389f, 16.107912f))
        .baselineValues(emptyList())
        .baselineValues(Collections.singletonList(25.96484f))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<FLOAT>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asList(-82.399826f, 12.633938f, 86.19402f), asList(-13.03544f, 64.65487f)))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(15.259451f, -15.259451f)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<FLOAT>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(asList(
            asList(asList(-5.6506114f), asList(26.546333f, 3724.8389f), asList(-53.65775f, 686.8335f, -0.99032f))
        ))
        .baselineValues(asList(
            asList(asList(29.042528f), asList(3524.3398f, -8856.58f, 6.8508215f)),
            asList(asList(-0.73994386f, -2.0008986f), asList(-9.903006f, -271.26172f), asList(-131.80347f),
                asList(39.721367f, -4870.5444f), asList(-1.4830998f, -766.3066f, -0.1659732f)),
            asList(asList(3467.0298f, -240.64255f), asList(2.4072556f, -85.89145f))
        ))
        .baselineValues(asList(
            asList(asList(-888.68243f, -38.09065f), asList(-6948.154f, -185.64319f, 0.7401936f), asList(-705.2718f, -932.4041f)),
            asList(asList(-2.581712f, 0.28686252f, -0.98652786f), asList(-57.448563f, -0.0057083773f, -0.21712556f),
                asList(-8.076653f, -8149.519f, -7.5968184f), asList(8.823492f), asList(-9134.323f, 467.53275f, -59.763447f)),
            asList(asList(0.33596575f, 6805.2256f, -3087.9531f), asList(9816.865f, -164.90712f, -1.9071647f)),
            asList(asList(-0.23883149f), asList(-5.3763375f, -4.7661624f)),
            asList(asList(-52.42167f, 247.91452f), asList(9499.771f), asList(-0.6549191f, 4340.83f))
        ))
        .go();
  }

  @Test
  public void doubleArray() throws Exception {
    checkDoubleArrayInTable("double_array");
  }

  @Test
  public void doubleArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "double_array_p");
    checkDoubleArrayInTable("double_array_p");
  }

  private void checkDoubleArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<DOUBLE>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(-13.241563769628, 0.3436367772981237, 9.73366))
        .baselineValues(emptyList())
        .baselineValues(asList(15.581409176959358))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<DOUBLE>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(asList(-24.049666910012498, 14.975034200, 1.19975056092457), asList(-2.293376758961259, 80.783)))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(0.47745359256854, -0.47745359256854)))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<DOUBLE>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(
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
        .baselineValues(
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
        .baselineValues(
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
  public void dateArray() throws Exception {
    checkDateArrayInTable("date_array");
  }

  @Test
  public void dateArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "date_array_p");
    checkDateArrayInTable("date_array_p");
  }

  private void checkDateArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<DATE>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(
            parseLocalDate("2018-10-21"),
            parseLocalDate("2017-07-11"),
            parseLocalDate("2018-09-23")))
        .baselineValues(emptyList())
        .baselineValues(asList(parseLocalDate("2018-07-14")))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<DATE>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(
            asList(parseLocalDate("2017-03-21"), parseLocalDate("2017-09-10"), parseLocalDate("2018-01-17")),
            asList(parseLocalDate("2017-03-24"), parseLocalDate("2018-09-22"))))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(parseLocalDate("2017-08-09"), parseLocalDate("2017-08-28"))))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<DATE>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(asList( // row
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
        ))
        .baselineValues(asList( // row
            asList( // [0]
                asList(parseLocalDate("1936-05-05"), parseLocalDate("1941-04-12"), parseLocalDate("1914-04-15"))//[0][0]
            ),
            asList( // [1]
                asList(parseLocalDate("1944-05-09"), parseLocalDate("2002-02-11"))//[1][0]
            )
        ))
        .baselineValues(asList( // row
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
        ))
        .go();
  }

  @Test
  public void timestampArray() throws Exception {
    checkTimestampArrayInTable("timestamp_array");
  }

  @Test
  public void timestampArrayParquet() throws Exception {
    assertNativeScanUsed(queryBuilder(), "timestamp_array_p");
    checkTimestampArrayInTable("timestamp_array_p");
  }

  private void checkTimestampArrayInTable(String table) throws Exception {
    // Nesting 0: reading ARRAY<TIMESTAMP>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`%s`", table)
        .optionSettingQueriesForTestQuery("alter session set `" + ExecConstants.PARQUET_READER_INT96_AS_TIMESTAMP + "` = true")
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(
            parseBest("2018-10-21 04:51:36"),
            parseBest("2017-07-11 09:26:48"),
            parseBest("2018-09-23 03:02:33")))
        .baselineValues(emptyList())
        .baselineValues(asList(parseBest("2018-07-14 05:20:34")))
        .go();

    // Nesting 1: reading ARRAY<ARRAY<TIMESTAMP>>
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.`%s`", table)
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(
            asList(parseBest("2017-03-21 12:52:33"), parseBest("2017-09-10 01:29:24"), parseBest("2018-01-17 04:45:23")),
            asList(parseBest("2017-03-24 01:03:23"), parseBest("2018-09-22 05:00:26"))))
        .baselineValues(asList(emptyList(), emptyList()))
        .baselineValues(asList(asList(parseBest("2017-08-09 08:26:08"), parseBest("2017-08-28 09:47:23"))))
        .go();

    // Nesting 2: reading ARRAY<ARRAY<ARRAY<TIMESTAMP>>>
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.`%s` order by rid", table)
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(
            asList( // row
                asList( // [0]
                    asList(parseBest("1929-01-08 19:31:47")),//[0][0]
                    asList(parseBest("1968-07-02 15:13:55"), parseBest("1990-01-25 21:05:51"), parseBest("1950-10-26 19:16:10")),//[0][1]
                    asList(parseBest("1946-09-03 03:03:50"), parseBest("1987-03-29 11:27:05")),//[0][2]
                    asList(parseBest("1979-11-29 09:01:14"))//[0][3]
                ),
                asList( // [1]
                    asList(parseBest("2010-08-26 12:08:51"), parseBest("2012-02-05 02:34:22")),//[1][0]
                    asList(parseBest("1955-02-24 19:45:33")),//[1][1]
                    asList(parseBest("1994-06-19 09:33:56"), parseBest("1971-11-05 06:27:55"), parseBest("1925-04-11 13:55:48")),//[1][2]
                    asList(parseBest("1916-10-02 05:09:18"), parseBest("1995-04-11 18:05:51"), parseBest("1973-11-17 06:06:53"))//[1][3]
                ),
                asList( // [2]
                    asList(parseBest("1929-12-19 16:49:08"), parseBest("1942-10-28 04:55:13"), parseBest("1936-12-01 13:01:37")),//[2][0]
                    asList(parseBest("1926-12-09 07:34:14"), parseBest("1971-07-23 15:01:00"), parseBest("2014-01-07 06:29:03")),//[2][1]
                    asList(parseBest("2012-08-25 23:26:10")),//[2][2]
                    asList(parseBest("2010-03-04 08:31:54"), parseBest("1950-07-20 19:26:08"), parseBest("1953-03-16 16:13:24"))//[2][3]
                )
            )
        )
        .baselineValues(
            asList( // row
                asList( // [0]
                    asList(parseBest("1904-12-10 00:39:14")),//[0][0]
                    asList(parseBest("1994-04-12 23:06:07")),//[0][1]
                    asList(parseBest("1954-07-05 23:48:09"), parseBest("1913-03-03 18:47:14"), parseBest("1960-04-30 22:35:28")),//[0][2]
                    asList(parseBest("1962-09-26 17:11:12"), parseBest("1906-06-18 04:05:21"), parseBest("2003-06-19 05:15:24"))//[0][3]
                ),
                asList( // [1]
                    asList(parseBest("1929-03-20 06:33:40"), parseBest("1939-02-12 07:03:07"), parseBest("1945-02-16 21:18:16"))//[1][0]
                ),
                asList( // [2]
                    asList(parseBest("1969-08-11 22:25:31"), parseBest("1944-08-11 02:57:58")),//[2][0]
                    asList(parseBest("1989-03-18 13:33:56"), parseBest("1961-06-06 04:44:50"))//[2][1]
                )
            )
        )
        .baselineValues(
            asList( // row
                asList( // [0]
                    asList(parseBest("1999-12-07 01:16:45")),//[0][0]
                    asList(parseBest("1903-12-11 04:28:20"), parseBest("2007-01-03 19:27:28")),//[0][1]
                    asList(parseBest("2018-03-16 15:43:19"), parseBest("2002-09-16 08:58:40"), parseBest("1956-05-16 17:47:44")),//[0][2]
                    asList(parseBest("2006-09-19 18:38:19"), parseBest("2016-01-21 12:39:30"))//[0][3]
                )
            )
        )
        .go();
  }

  @Test
  public void binaryArray() throws Exception {
    // Nesting 0: reading ARRAY<BINARY>
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.`binary_array`")
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(new StringBytes("First"), new StringBytes("Second"), new StringBytes("Third")))
        .baselineValues(asList(new StringBytes("First")))
        .go();
  }

  @Test
  public void arrayViewDefinedInHive() throws Exception {
    testBuilder()
        .sqlQuery("SELECT * FROM hive.`arr_view` WHERE vwrid=1")
        .unOrdered()
        .baselineColumns("vwrid", "int_n0", "int_n1", "string_n0", "string_n1",
            "varchar_n0", "varchar_n1", "char_n0", "char_n1", "tinyint_n0",
            "tinyint_n1", "smallint_n0", "smallint_n1", "decimal_n0", "decimal_n1",
            "boolean_n0", "boolean_n1", "bigint_n0", "bigint_n1", "float_n0", "float_n1",
            "double_n0", "double_n1", "date_n0", "date_n1", "timestamp_n0", "timestamp_n1")
        .baselineValues(
            1,

            asList(-1, 0, 1),
            asList(asList(-1, 0, 1), asList(-2, 1)),

            asTextList("First Value Of Array", "komlnp", "The Last Value"),
            asList(asTextList("Array 0, Value 0", "Array 0, Value 1"), asTextList("Array 1")),

            asTextList("Five", "One", "T"),
            asList(asTextList("Five", "One", "$42"), asTextList("T", "K", "O")),

            asTextList("aa", "cc", "ot"),
            asList(asTextList("aa"), asTextList("cc", "ot")),

            asList(-128, 0, 127),
            asList(asList(-128, -127), asList(0, 1), asList(127, 126)),

            asList(-32768, 0, 32767),
            asList(asList(-32768, -32768), asList(0, 0), asList(32767, 32767)),

            asList(new BigDecimal("-100000.000"), new BigDecimal("102030.001"), new BigDecimal("0.001")),
            asList(asList(new BigDecimal("-100000.000"), new BigDecimal("102030.001")), asList(new BigDecimal("0.101"), new BigDecimal("0.102")),
                asList(new BigDecimal("0.001"), new BigDecimal("327670.001"))),

            asList(false, true, false, true, false),
            asList(asList(true, false, true), asList(false, false)),

            asList(-9223372036854775808L, 0L, 10000000010L, 9223372036854775807L),
            asList(asList(-9223372036854775808L, 0L, 10000000010L), asList(9223372036854775807L, 9223372036854775807L)),

            asList(-32.058f, 94.47389f, 16.107912f),
            asList(asList(-82.399826f, 12.633938f, 86.19402f), asList(-13.03544f, 64.65487f)),

            asList(-13.241563769628, 0.3436367772981237, 9.73366),
            asList(asList(-24.049666910012498, 14.975034200, 1.19975056092457), asList(-2.293376758961259, 80.783)),

            asList(parseLocalDate("2018-10-21"), parseLocalDate("2017-07-11"), parseLocalDate("2018-09-23")),
            asList(asList(parseLocalDate("2017-03-21"), parseLocalDate("2017-09-10"), parseLocalDate("2018-01-17")),
                asList(parseLocalDate("2017-03-24"), parseLocalDate("2018-09-22"))),

            asList(parseBest("2018-10-21 04:51:36"), parseBest("2017-07-11 09:26:48"), parseBest("2018-09-23 03:02:33")),
            asList(asList(parseBest("2017-03-21 12:52:33"), parseBest("2017-09-10 01:29:24"), parseBest("2018-01-17 04:45:23")),
                asList(parseBest("2017-03-24 01:03:23"), parseBest("2018-09-22 05:00:26")))
        )
        .go();
  }

  @Test
  public void arrayViewDefinedInDrill() throws Exception {
    queryBuilder().sql(
        "CREATE VIEW " + StoragePluginTestUtils.DFS_TMP_SCHEMA + ".`dfs_arr_vw` AS " +
            "SELECT " +
            "   t1.rid as vwrid," +
            "   t1.arr_n_0 as int_n0," +
            "   t1.arr_n_1 as int_n1," +
            "   t2.arr_n_0 as string_n0," +
            "   t2.arr_n_1 as string_n1," +
            "   t3.arr_n_0 as varchar_n0," +
            "   t3.arr_n_1 as varchar_n1," +
            "   t4.arr_n_0 as char_n0," +
            "   t4.arr_n_1 as char_n1," +
            "   t5.arr_n_0 as tinyint_n0," +
            "   t5.arr_n_1 as tinyint_n1," +
            "   t6.arr_n_0 as smallint_n0," +
            "   t6.arr_n_1 as smallint_n1," +
            "   t7.arr_n_0 as decimal_n0," +
            "   t7.arr_n_1 as decimal_n1," +
            "   t8.arr_n_0 as boolean_n0," +
            "   t8.arr_n_1 as boolean_n1," +
            "   t9.arr_n_0 as bigint_n0," +
            "   t9.arr_n_1 as bigint_n1," +
            "   t10.arr_n_0 as float_n0," +
            "   t10.arr_n_1 as float_n1," +
            "   t11.arr_n_0 as double_n0," +
            "   t11.arr_n_1 as double_n1," +
            "   t12.arr_n_0 as date_n0," +
            "   t12.arr_n_1 as date_n1," +
            "   t13.arr_n_0 as timestamp_n0," +
            "   t13.arr_n_1 as timestamp_n1 " +
            "FROM " +
            "   hive.int_array t1," +
            "   hive.string_array t2," +
            "   hive.varchar_array t3," +
            "   hive.char_array t4," +
            "   hive.tinyint_array t5," +
            "   hive.smallint_array t6," +
            "   hive.decimal_array t7," +
            "   hive.boolean_array t8," +
            "   hive.bigint_array t9," +
            "   hive.float_array t10," +
            "   hive.double_array t11," +
            "   hive.date_array t12," +
            "   hive.timestamp_array t13 " +
            "WHERE " +
            "   t1.rid=t2.rid AND" +
            "   t1.rid=t3.rid AND" +
            "   t1.rid=t4.rid AND" +
            "   t1.rid=t5.rid AND" +
            "   t1.rid=t6.rid AND" +
            "   t1.rid=t7.rid AND" +
            "   t1.rid=t8.rid AND" +
            "   t1.rid=t9.rid AND" +
            "   t1.rid=t10.rid AND" +
            "   t1.rid=t11.rid AND" +
            "   t1.rid=t12.rid AND" +
            "   t1.rid=t13.rid "
    ).run();

    testBuilder()
        .sqlQuery("SELECT * FROM " + StoragePluginTestUtils.DFS_TMP_SCHEMA + ".`dfs_arr_vw` WHERE vwrid=1")
        .unOrdered()
        .baselineColumns("vwrid", "int_n0", "int_n1", "string_n0", "string_n1",
            "varchar_n0", "varchar_n1", "char_n0", "char_n1", "tinyint_n0",
            "tinyint_n1", "smallint_n0", "smallint_n1", "decimal_n0", "decimal_n1",
            "boolean_n0", "boolean_n1", "bigint_n0", "bigint_n1", "float_n0", "float_n1",
            "double_n0", "double_n1", "date_n0", "date_n1", "timestamp_n0", "timestamp_n1")
        .baselineValues(
            1,

            asList(-1, 0, 1),
            asList(asList(-1, 0, 1), asList(-2, 1)),

            asTextList("First Value Of Array", "komlnp", "The Last Value"),
            asList(asTextList("Array 0, Value 0", "Array 0, Value 1"), asTextList("Array 1")),

            asTextList("Five", "One", "T"),
            asList(asTextList("Five", "One", "$42"), asTextList("T", "K", "O")),

            asTextList("aa", "cc", "ot"),
            asList(asTextList("aa"), asTextList("cc", "ot")),

            asList(-128, 0, 127),
            asList(asList(-128, -127), asList(0, 1), asList(127, 126)),

            asList(-32768, 0, 32767),
            asList(asList(-32768, -32768), asList(0, 0), asList(32767, 32767)),

            asList(new BigDecimal("-100000.000"), new BigDecimal("102030.001"), new BigDecimal("0.001")),
            asList(asList(new BigDecimal("-100000.000"), new BigDecimal("102030.001")), asList(new BigDecimal("0.101"), new BigDecimal("0.102")),
                asList(new BigDecimal("0.001"), new BigDecimal("327670.001"))),

            asList(false, true, false, true, false),
            asList(asList(true, false, true), asList(false, false)),

            asList(-9223372036854775808L, 0L, 10000000010L, 9223372036854775807L),
            asList(asList(-9223372036854775808L, 0L, 10000000010L), asList(9223372036854775807L, 9223372036854775807L)),

            asList(-32.058f, 94.47389f, 16.107912f),
            asList(asList(-82.399826f, 12.633938f, 86.19402f), asList(-13.03544f, 64.65487f)),

            asList(-13.241563769628, 0.3436367772981237, 9.73366),
            asList(asList(-24.049666910012498, 14.975034200, 1.19975056092457), asList(-2.293376758961259, 80.783)),

            asList(parseLocalDate("2018-10-21"), parseLocalDate("2017-07-11"), parseLocalDate("2018-09-23")),
            asList(asList(parseLocalDate("2017-03-21"), parseLocalDate("2017-09-10"), parseLocalDate("2018-01-17")),
                asList(parseLocalDate("2017-03-24"), parseLocalDate("2018-09-22"))),

            asList(parseBest("2018-10-21 04:51:36"), parseBest("2017-07-11 09:26:48"), parseBest("2018-09-23 03:02:33")),
            asList(asList(parseBest("2017-03-21 12:52:33"), parseBest("2017-09-10 01:29:24"), parseBest("2018-01-17 04:45:23")),
                asList(parseBest("2017-03-24 01:03:23"), parseBest("2018-09-22 05:00:26")))
        )
        .go();
  }

  @Test
  public void structArrayN0() throws Exception {
    testBuilder()
        .sqlQuery("SELECT arr_n_0 FROM hive.struct_array")
        .unOrdered()
        .baselineColumns("arr_n_0")
        .baselineValues(asList(
            TestBuilder.mapOf("a", -1, "b", true, "c", "asdpo daasree"),
            TestBuilder.mapOf("a", 0, "b", false, "c", "xP>vcx _2p3 >.mm,//"),
            TestBuilder.mapOf("a", 902, "b", false, "c", "*-//------*")
        ))
        .baselineValues(asList())
        .go();
  }

  @Test
  public void structArrayN0ByIdxP1() throws Exception {
    HiveTestUtilities.assertNativeScanUsed(queryBuilder(), "struct_array_p");
    testBuilder()
        .sqlQuery("SELECT rid, arr_n_0[1].c p1 FROM hive.struct_array_p")
        .unOrdered()
        .baselineColumns("rid", "p1")
        .baselineValues(1, "xP>vcx _2p3 >.mm,//")
        .baselineValues(2, null)
        .go();
  }

  @Test
  public void structArrayN0ByIdxP2() throws Exception {
    HiveTestUtilities.assertNativeScanUsed(queryBuilder(), "struct_array_p");
    testBuilder()
        .sqlQuery("SELECT rid, arr_n_0[2] p2 FROM hive.struct_array_p")
        .unOrdered()
        .baselineColumns("rid", "p2")
        .baselineValues(1, TestBuilder.mapOf("a", 902, "b", false, "c", "*-//------*"))
        .baselineValues(2, TestBuilder.mapOf())
        .go();
  }

  @Test
  public void structArrayN0ByIdxP3() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid,arr_n_0[2] p3 FROM hive.struct_array")
        .unOrdered()
        .baselineColumns("rid", "p3")
        .baselineValues(1, TestBuilder.mapOf("a", 902, "b", false, "c", "*-//------*"))
        .baselineValues(2, TestBuilder.mapOf())
        .go();
  }

  @Test
  public void structArrayN1() throws Exception {
    testBuilder()
        .sqlQuery("SELECT arr_n_1 FROM hive.struct_array")
        .unOrdered()
        .baselineColumns("arr_n_1")
        .baselineValues(asList(
            asList(
                TestBuilder.mapOf("x", 17.9231, "y", -12.12),
                TestBuilder.mapOf("x", 0.0001, "y", -1.1),
                TestBuilder.mapOf("x", 101.1, "y", -989.11)
            ),
            asList(
                TestBuilder.mapOf("x", 77.32, "y", -11.11),
                TestBuilder.mapOf("x", 13.1, "y", -1.1)
            )
        ))
        .baselineValues(asList(
            asList(),
            asList(TestBuilder.mapOf("x", 21.221, "y", -21.221))
        ))
        .go();
  }

  @Test
  public void structArrayN2() throws Exception {
    testBuilder()
        .sqlQuery("SELECT arr_n_2 FROM hive.struct_array ORDER BY rid")
        .ordered()
        .baselineColumns("arr_n_2")
        .baselineValues(asList(
            asList(
                asList(
                    TestBuilder.mapOf("t", 1, "d", parseLocalDate("2018-10-21")),
                    TestBuilder.mapOf("t", 2, "d", parseLocalDate("2017-07-11"))
                ),
                asList(
                    TestBuilder.mapOf("t", 3, "d", parseLocalDate("2018-09-23")),
                    TestBuilder.mapOf("t", 4, "d", parseLocalDate("1965-04-18")),
                    TestBuilder.mapOf("t", 5, "d", parseLocalDate("1922-05-22"))
                ),
                asList(
                    TestBuilder.mapOf("t", 6, "d", parseLocalDate("1921-05-22")),
                    TestBuilder.mapOf("t", 7, "d", parseLocalDate("1923-05-22"))
                )
            ),
            asList(
                asList(
                    TestBuilder.mapOf("t", 8, "d", parseLocalDate("2002-02-11")),
                    TestBuilder.mapOf("t", 9, "d", parseLocalDate("2017-03-24"))
                )
            ),
            asList(
                asList(
                    TestBuilder.mapOf("t", 10, "d", parseLocalDate("1919-01-17")),
                    TestBuilder.mapOf("t", 11, "d", parseLocalDate("1965-12-15"))
                )
            )
        ))
        .baselineValues(asList(
            asList(
                asList(
                    TestBuilder.mapOf("t", 12, "d", parseLocalDate("2018-09-23")),
                    TestBuilder.mapOf("t", 13, "d", parseLocalDate("1939-10-23")),
                    TestBuilder.mapOf("t", 14, "d", parseLocalDate("1922-05-22"))
                )
            ),
            asList(
                asList(
                    TestBuilder.mapOf("t", 15, "d", parseLocalDate("2018-09-23")),
                    TestBuilder.mapOf("t", 16, "d", parseLocalDate("1965-04-18"))
                )
            )
        ))
        .go();
  }

  @Test
  public void structArrayN2PrimitiveFieldAccess() throws Exception {
    testBuilder()
        .sqlQuery("SELECT sa.arr_n_2[0][0][1].d FROM hive.struct_array sa ORDER BY rid")
        .ordered()
        .baselineColumns("EXPR$0")
        .baselineValues(parseLocalDate("2017-07-11"))
        .baselineValues(parseLocalDate("1939-10-23"))
        .go();
  }

  @Test
  public void mapArrayN0() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, arr_n_0 FROM hive.map_array")
        .unOrdered()
        .baselineColumns("rid", "arr_n_0")
        .baselineValues(1, asList(mapOfObject(0, true, 1, false), mapOfObject(0, false), mapOfObject(1, true)))
        .baselineValues(2, asList(mapOfObject(0, false, 1, true), mapOfObject(0, true)))
        .baselineValues(3, asList(mapOfObject(0, true, 1, false)))
        .go();
  }

  @Test
  public void mapArrayN1() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, arr_n_1 FROM hive.map_array")
        .unOrdered()
        .baselineColumns("rid", "arr_n_1")
        .baselineValues(1, asList(
            asList(mapOfObject(true, "zz", 1, "cx", 2), mapOfObject(true, "oo", 7, "nn", 9), mapOfObject(true, "nb", 3)),
            asList(mapOfObject(true, "is", 12, "ie", 7, "po", 2), mapOfObject(true, "ka", 11)),
            asList(mapOfObject(true, "tr", 3), mapOfObject(true, "xz", 4))
        ))
        .baselineValues(2, asList(
            asList(mapOfObject(true, "vv", 0, "zz", 2), mapOfObject(true, "ui", 8)),
            asList(mapOfObject(true, "iy", 7, "yi", 5), mapOfObject(true, "nb", 4, "nr", 2, "nm", 2), mapOfObject(true, "qw", 12, "qq", 17)),
            asList(mapOfObject(true, "aa", 0, "az", 0), mapOfObject(true, "tt", 25))
        ))
        .baselineValues(3, asList(
            asList(mapOfObject(true, "ix", 40)),
            asList(mapOfObject(true, "cx", 30)),
            asList(mapOfObject(true, "we", 20), mapOfObject(true, "ex", 70))
        ))
        .go();
  }

  @Test
  public void mapArrayN2() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, arr_n_2 FROM hive.map_array")
        .unOrdered()
        .baselineColumns("rid", "arr_n_2")
        .baselineValues(1, asList(
            asList(
                asList(mapOfObject(1, parseLocalDate("2019-09-12"), 2, parseLocalDate("2019-09-13")), mapOfObject(1, parseLocalDate("2019-09-13"))),
                asList(mapOfObject(3, parseLocalDate("2019-09-27")), mapOfObject(5, parseLocalDate("2019-09-17")))
            ),
            asList(
                asList(mapOfObject(7, parseLocalDate("2019-07-07"))),
                asList(mapOfObject(12, parseLocalDate("2019-09-15"))),
                asList(mapOfObject(9, parseLocalDate("2019-09-15")))
            )
        ))
        .baselineValues(2, asList(
            asList(
                asList(mapOfObject(1, parseLocalDate("2020-01-01"), 3, parseLocalDate("2017-03-15"))),
                asList(mapOfObject(5, parseLocalDate("2020-01-05"), 7, parseLocalDate("2017-03-17")), mapOfObject(0, parseLocalDate("2000-12-01")))
            ),
            asList(
                asList(mapOfObject(9, parseLocalDate("2019-05-09")), mapOfObject(0, parseLocalDate("2019-09-01"))),
                asList(mapOfObject(3, parseLocalDate("2019-09-03")), mapOfObject(7, parseLocalDate("2007-08-07")), mapOfObject(4, parseLocalDate("2004-04-04"))),
                asList(mapOfObject(3, parseLocalDate("2003-03-03")), mapOfObject(1, parseLocalDate("2001-01-11")))
            )
        ))
        .baselineValues(3, asList(
            asList(
                asList(mapOfObject(8, parseLocalDate("2019-10-19"))),
                asList(mapOfObject(6, parseLocalDate("2019-11-06")))
            ),
            asList(
                asList(mapOfObject(9, parseLocalDate("2019-11-09"))),
                asList(mapOfObject(6, parseLocalDate("2019-11-06"))),
                asList(mapOfObject(6, parseLocalDate("2019-11-06")))
            )
        ))
        .go();
  }

  @Test
  public void mapArrayRepeatedCount() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, REPEATED_COUNT(arr_n_0) rc FROM hive.map_array")
        .unOrdered()
        .baselineColumns("rid", "rc")
        .baselineValues(1, 3)
        .baselineValues(2, 2)
        .baselineValues(3, 1)
        .go();
  }

  @Test
  public void mapArrayCount() throws Exception {
    testBuilder()
        .sqlQuery("SELECT COUNT(arr_n_0) cnt FROM hive.map_array")
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(3L)
        .go();
  }

  @Test
  public void unionArray() throws Exception {
    testBuilder()
        .sqlQuery("SELECT rid, un_arr FROM hive.union_array")
        .unOrdered()
        .baselineColumns("rid", "un_arr")
        .baselineValues(1, listOf(new Text("S0m3 tExTy 4arZ"), 128, true, 7.7775f))
        .baselineValues(2, listOf(true, 7.7775f))
        .baselineValues(3, listOf(new Text("S0m3 tExTy 4arZ"), 128, 7.7775f))
        .go();
  }

  /**
   * Workaround {@link StringBytes#equals(Object)} implementation
   * used to compare binary array elements.
   * See {@link TestHiveArrays#binaryArray()} for sample usage.
   */
  private static final class StringBytes {

    private final byte[] bytes;

    private StringBytes(String s) {
      bytes = s.getBytes();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof byte[]) {
        return Arrays.equals(bytes, (byte[]) obj);
      }
      return (obj == this) || (obj instanceof StringBytes
          && Arrays.equals(bytes, ((StringBytes) obj).bytes));
    }

    @Override
    public String toString() {
      return new String(bytes);
    }
  }

  private static List<Text> asTextList(String... strings) {
    return Stream.of(strings)
        .map(Text::new)
        .collect(Collectors.toList());
  }

}
