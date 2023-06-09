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

package org.apache.drill.exec.udfs;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.drill.test.rowSet.RowSetUtilities.mapArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestComplexSchemaFunctions extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testMapSchemaFunction() throws RpcException {
    String sql = "SELECT getMapSchema(record) AS schema FROM cp.`json/nestedSchema.json`";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();
    assertEquals(results.rowCount(), 1);

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addMap("schema")
          .addNullable("int_field", MinorType.VARCHAR)
          .addNullable("double_field", MinorType.VARCHAR)
          .addNullable("string_field", MinorType.VARCHAR)
          .addNullable("boolean_field", MinorType.VARCHAR)
          .addNullable("int_list", MinorType.VARCHAR)
          .addNullable("double_list", MinorType.VARCHAR)
          .addNullable("boolean_list", MinorType.VARCHAR)
          .addNullable("map", MinorType.VARCHAR)
          .addNullable("repeated_map", MinorType.VARCHAR)
        .resumeSchema()
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow((Object)strArray("BIGINT", "FLOAT8", "VARCHAR", "BIT", "REPEATED_BIGINT", "REPEATED_FLOAT8", "REPEATED_BIT", "MAP", "REPEATED_MAP"))
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testMapSchemaFunctionWithInnerMap() throws RpcException {
    String sql = "SELECT getMapSchema(t1.record.map) AS schema FROM cp.`json/nestedSchema.json` AS t1";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();
    assertEquals(results.rowCount(), 1);

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addMap("schema")
          .addNullable("nested_int_field", MinorType.VARCHAR)
          .addNullable("nested_double_field", MinorType.VARCHAR)
          .addNullable("nested_string_field", MinorType.VARCHAR)
        .resumeSchema()
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow((Object)strArray("BIGINT", "FLOAT8", "VARCHAR"))
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testMapSchemaFunctionWithNull() throws RpcException {
    String sql = "SELECT getMapSchema(null) AS schema FROM cp.`json/nestedSchema.json` AS t1";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("schema", MinorType.MAP)
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow((Object) mapArray())
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }
}
