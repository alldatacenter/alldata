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
import java.util.stream.IntStream;

import org.apache.drill.categories.HiveStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.hive.HiveClusterTest;
import org.apache.drill.exec.hive.HiveTestFixture;
import org.apache.drill.exec.hive.HiveTestUtilities;
import org.apache.drill.test.ClusterFixture;
import org.apache.hadoop.hive.ql.Driver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;
import static org.apache.drill.test.TestBuilder.mapOfObject;

@Category({SlowTest.class, HiveStorageTest.class})
public class TestHiveUnions extends HiveClusterTest {

  private static HiveTestFixture hiveTestFixture;

  @BeforeClass
  public static void setUp() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher)
        .sessionOption(ExecConstants.HIVE_OPTIMIZE_PARQUET_SCAN_WITH_NATIVE_READER, true)
    );
    hiveTestFixture = HiveTestFixture.builder(dirTestWatcher).build();
    hiveTestFixture.getDriverManager().runWithinSession(TestHiveUnions::generateData);
    hiveTestFixture.getPluginManager().addHivePluginTo(cluster.drillbit());
  }

  @AfterClass
  public static void tearDown() {
    if (hiveTestFixture != null) {
      hiveTestFixture.getPluginManager().removeHivePluginFrom(cluster.drillbit());
    }
  }

  private static void generateData(Driver d) {
    HiveTestUtilities.executeQuery(d, "CREATE TABLE dummy(d INT) STORED AS TEXTFILE");
    HiveTestUtilities.executeQuery(d, "INSERT INTO TABLE dummy VALUES (1)");

    String unionDdl = "CREATE TABLE union_tbl(" +
        "tag INT, " +
        "ut UNIONTYPE<INT, DOUBLE, ARRAY<STRING>, STRUCT<a:INT,b:STRING>, DATE, BOOLEAN," +
        "DECIMAL(9,3), TIMESTAMP, BIGINT, FLOAT, MAP<INT, BOOLEAN>, ARRAY<INT>>) " +
        "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' COLLECTION ITEMS TERMINATED BY '&' " +
        "MAP KEYS TERMINATED BY '#' LINES TERMINATED BY '\\n' STORED AS TEXTFILE";
    HiveTestUtilities.executeQuery(d, unionDdl);

    String insert = "INSERT INTO TABLE union_tbl " +
        "SELECT %1$d, " +
        "create_union(%1$d, " +
        "1, " +
        "CAST(17.55 AS DOUBLE), " +
        "array('x','yy','zzz'), " +
        "named_struct('a',1,'b','x'), " +
        "CAST('2019-09-09' AS DATE), " +
        "true, " +
        "CAST(12356.123 AS DECIMAL(9,3)), " +
        "CAST('2018-10-21 04:51:36' AS TIMESTAMP), " +
        "CAST(9223372036854775807 AS BIGINT), " +
        "CAST(-32.058 AS FLOAT), " +
        "map(1,true,2,false,3,false,4,true), " +
        "array(7,-9,2,-5,22)" +
        ")" +
        " FROM dummy";

    IntStream.of(1, 5, 0, 2, 4, 3, 11, 8, 7, 9, 10, 6)
        .forEach(v -> HiveTestUtilities.executeQuery(d, String.format(insert, v)));
  }

  @Test
  public void checkUnion() throws Exception {
    testBuilder()
        .sqlQuery("SELECT tag, ut FROM hive.union_tbl")
        .unOrdered()
        .baselineColumns("tag", "ut")
        .baselineValues(1, 17.55)
        .baselineValues(5, true)
        .baselineValues(0, 1)
        .baselineValues(2, listOf("x", "yy", "zzz"))
        .baselineValues(4, DateUtility.parseLocalDate("2019-09-09"))
        .baselineValues(3, mapOf("a", 1, "b", "x"))
        .baselineValues(11, listOf(7, -9, 2, -5, 22))
        .baselineValues(8, 9223372036854775807L)
        .baselineValues(7, DateUtility.parseBest("2018-10-21 04:51:36"))
        .baselineValues(9, -32.058f)
        .baselineValues(10, mapOfObject(1, true, 2, false, 3, false, 4, true))
        .baselineValues(6, new BigDecimal("12356.123"))
        .go();
  }
}
