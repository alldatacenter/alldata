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

import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.TestBuilder;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ParquetInternalsTest extends ClusterTest {

  @ClassRule
  public static BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @BeforeClass
  public static void setup( ) throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
      // Set options, etc.

    startCluster(builder);
  }

  @Test
  public void testFixedWidth() throws Exception {
    String sql = "SELECT l_orderkey, l_partkey, l_suppkey, l_linenumber, l_quantity\n" +
                 "FROM `cp`.`tpch/lineitem.parquet` LIMIT 20";

    Map<SchemaPath, TypeProtos.MajorType> typeMap = new HashMap<>();
    typeMap.put(TestBuilder.parsePath("l_orderkey"), Types.required(TypeProtos.MinorType.INT));
    typeMap.put(TestBuilder.parsePath("l_partkey"), Types.required(TypeProtos.MinorType.INT));
    typeMap.put(TestBuilder.parsePath("l_suppkey"), Types.required(TypeProtos.MinorType.INT));
    typeMap.put(TestBuilder.parsePath("l_linenumber"), Types.required(TypeProtos.MinorType.INT));
    typeMap.put(TestBuilder.parsePath("l_quantity"), Types.required(TypeProtos.MinorType.FLOAT8));
    client.testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .csvBaselineFile("parquet/expected/fixedWidth.csv")
      .baselineColumns("l_orderkey", "l_partkey", "l_suppkey", "l_linenumber", "l_quantity")
      .baselineTypes(typeMap)
      .build()
      .run();
  }

  @Test
  public void testVariableWidth() throws Exception {
    String sql = "SELECT s_name, s_address, s_phone, s_comment\n" +
                 "FROM `cp`.`tpch/supplier.parquet` LIMIT 20";

    Map<SchemaPath, TypeProtos.MajorType> typeMap = new HashMap<>();
    typeMap.put(TestBuilder.parsePath("s_name"), Types.required(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("s_address"), Types.required(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("s_phone"), Types.required(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("s_comment"), Types.required(TypeProtos.MinorType.VARCHAR));
    client.testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .csvBaselineFile("parquet/expected/variableWidth.csv")
      .baselineColumns("s_name", "s_address", "s_phone", "s_comment")
      .baselineTypes(typeMap)
      .build()
      .run();
  }

  @Test
  public void testMixedWidth() throws Exception {
    String sql = "SELECT s_suppkey, s_name, s_address, s_phone, s_acctbal\n" +
                 "FROM `cp`.`tpch/supplier.parquet` LIMIT 20";

    Map<SchemaPath, TypeProtos.MajorType> typeMap = new HashMap<>();
    typeMap.put(TestBuilder.parsePath("s_suppkey"), Types.required(TypeProtos.MinorType.INT));
    typeMap.put(TestBuilder.parsePath("s_name"), Types.required(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("s_address"), Types.required(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("s_phone"), Types.required(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("s_acctbal"), Types.required(TypeProtos.MinorType.FLOAT8));
    client.testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .csvBaselineFile("parquet/expected/mixedWidth.csv")
      .baselineColumns("s_suppkey", "s_name", "s_address", "s_phone", "s_acctbal")
      .baselineTypes(typeMap)
      .build()
      .run();
  }

  @Test
  public void testStar() throws Exception {
    String sql = "SELECT *\n" +
                 "FROM `cp`.`tpch/supplier.parquet` LIMIT 20";

    Map<SchemaPath, TypeProtos.MajorType> typeMap = new HashMap<>();
    typeMap.put(TestBuilder.parsePath("s_suppkey"), Types.required(TypeProtos.MinorType.INT));
    typeMap.put(TestBuilder.parsePath("s_name"), Types.required(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("s_address"), Types.required(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("s_nationkey"), Types.required(TypeProtos.MinorType.INT));
    typeMap.put(TestBuilder.parsePath("s_phone"), Types.required(TypeProtos.MinorType.VARCHAR));
    typeMap.put(TestBuilder.parsePath("s_acctbal"), Types.required(TypeProtos.MinorType.FLOAT8));
    typeMap.put(TestBuilder.parsePath("s_comment"), Types.required(TypeProtos.MinorType.VARCHAR));
    client.testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .csvBaselineFile("parquet/expected/star.csv")
      .baselineColumns("s_suppkey", "s_name", "s_address", "s_nationkey", "s_phone", "s_acctbal", "s_comment")
      .baselineTypes(typeMap)
      .build()
      .run();
  }

  @Test
  public void testMissing() throws Exception {
    String sql = "SELECT s_suppkey, bogus\n" +
                 "FROM `cp`.`tpch/supplier.parquet` LIMIT 20";

    // This test should return nothing but nulls. At present, the test
    // framework can't check this case. Temporarily dumping the query
    // to a CSV file to the console.
    // TODO: Once the "row set" fixture is available, use that to verify
    // that all rows are null.

    // Can't handle nulls this way...
    //    Map<SchemaPath, TypeProtos.MajorType> typeMap = new HashMap<>();
    //    typeMap.put(TestBuilder.parsePath("s_suppkey"), Types.required(TypeProtos.MinorType.INT));
    //    typeMap.put(TestBuilder.parsePath("bogus"), Types.optional(TypeProtos.MinorType.INT));
    //    client.testBuilder()
    //      .sqlQuery(sql)
    //      .unOrdered()
    //      .csvBaselineFile("parquet/expected/bogus.csv")
    //      .baselineColumns("s_suppkey", "bogus")
    //      .baselineTypes(typeMap)
    //      .build()
    //      .run();
  }
}
