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
package org.apache.drill.exec.store.phoenix;

import static org.junit.Assert.assertEquals;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
@Category({ SlowTest.class, RowSetTests.class })
public class PhoenixCommandTest extends PhoenixBaseTest {

  @Test
  public void testShowTablesLike() throws Exception {
    runAndPrint("SHOW SCHEMAS");
    run("USE phoenix123.v1");
    assertEquals(1, queryBuilder().sql("SHOW TABLES LIKE '%REGION%'").run().recordCount());
  }

  @Test
  public void testShowTables() throws Exception {
    String sql = "SHOW TABLES FROM phoenix123.v1";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("TABLE_SCHEMA", MinorType.VARCHAR)
        .addNullable("TABLE_NAME", MinorType.VARCHAR)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("phoenix123.v1", "ARRAYTYPE")
        .addRow("phoenix123.v1", "DATATYPE")
        .addRow("phoenix123.v1", "NATION")
        .addRow("phoenix123.v1", "REGION")
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testDescribe() throws Exception {
    assertEquals(4, queryBuilder().sql("DESCRIBE phoenix123.v1.NATION").run().recordCount());
  }

  @Test
  public void testDescribeCaseInsensitive() throws Exception {
    String sql = "DESCRIBE phoenix123.v1.nation"; // use lowercase
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("COLUMN_NAME", MinorType.VARCHAR)
        .addNullable("DATA_TYPE", MinorType.VARCHAR)
        .addNullable("IS_NULLABLE", MinorType.VARCHAR)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("N_NATIONKEY", "BIGINT", "NO")
        .addRow("N_NAME", "CHARACTER VARYING", "YES")
        .addRow("N_REGIONKEY", "BIGINT", "YES")
        .addRow("N_COMMENT", "CHARACTER VARYING", "YES")
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }
}
