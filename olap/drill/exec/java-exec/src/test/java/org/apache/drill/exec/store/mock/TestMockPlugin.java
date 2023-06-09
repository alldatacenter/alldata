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
package org.apache.drill.exec.store.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryRowSetIterator;
import org.apache.drill.test.QueryBuilder.QuerySummary;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the mock data source. See the package info file in the
 * mock data source for details.
 * <p>
 * This is mostly just a sanity tests: details are added, and
 * tested, where needed in unit tests.
 */

@Category({RowSetTests.class, UnlikelyTest.class})
public class TestMockPlugin extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    startCluster(
        ClusterFixture.builder(dirTestWatcher));
  }

  @Test
  public void testRowLimit() throws RpcException {
    String sql = "SELECT dept_i FROM `mock`.`employee_100`";
    RowSet result = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata schema = result.schema();
    assertEquals(1, schema.size());
    ColumnMetadata col = schema.metadata(0);
    assertEquals("dept_i", col.name());
    assertEquals(MinorType.INT, col.type());
    assertEquals(DataMode.REQUIRED, col.mode());
    assertEquals(100, result.rowCount());
    result.clear();
  }

  @Test
  public void testVarChar() throws RpcException {
    String sql = "SELECT name_s17 FROM `mock`.`employee_100`";
    RowSet result = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata schema = result.schema();
    assertEquals(1, schema.size());
    ColumnMetadata col = schema.metadata(0);
    assertEquals("name_s17", col.name());
    assertEquals(MinorType.VARCHAR, col.type());
    assertEquals(DataMode.REQUIRED, col.mode());
    assertEquals(100, result.rowCount());

    RowSetReader reader = result.reader();
    while (reader.next()) {
      assertEquals(17, reader.scalar(0).getString().length());
    }
    result.clear();
  }

  @Test
  public void testDouble() throws RpcException {
    String sql = "SELECT balance_d FROM `mock`.`employee_100`";
    RowSet result = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata schema = result.schema();
    assertEquals(1, schema.size());
    ColumnMetadata col = schema.metadata(0);
    assertEquals("balance_d", col.name());
    assertEquals(MinorType.FLOAT8, col.type());
    assertEquals(DataMode.REQUIRED, col.mode());
    assertEquals(100, result.rowCount());
    result.clear();
  }

  @Test
  public void testBoolean() throws RpcException {
    String sql = "SELECT active_b FROM `mock`.`employee_100`";
    RowSet result = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata schema = result.schema();
    assertEquals(1, schema.size());
    ColumnMetadata col = schema.metadata(0);
    assertEquals("active_b", col.name());
    assertEquals(MinorType.BIT, col.type());
    assertEquals(DataMode.REQUIRED, col.mode());
    assertEquals(100, result.rowCount());
    result.clear();
  }

  /**
   * By default, the mock reader limits batch size to 10 MB.
   */
  @Test
  public void testSizeLimit() throws RpcException {
    String sql = "SELECT comments_s20000 FROM `mock`.`employee_1K`";
    QueryRowSetIterator iter = client.queryBuilder().sql(sql).rowSetIterator();

    assertTrue(iter.hasNext());
    RowSet result = iter.next();
    TupleMetadata schema = result.schema();
    assertEquals(1, schema.size());
    ColumnMetadata col = schema.metadata(0);
    assertEquals("comments_s20000", col.name());
    assertEquals(MinorType.VARCHAR, col.type());
    assertEquals(DataMode.REQUIRED, col.mode());
    assertTrue(result.rowCount() <= 10 * 1024 * 1024 / 20_000);
    result.clear();
    while (iter.hasNext()) {
      iter.next().clear();
    }
  }


  @Test
  public void testExtendedSqlMultiBatch() throws Exception {
    String sql = "SELECT id_i, name_s10 FROM `mock`.`employees_10K`";

    QuerySummary results = client.queryBuilder().sql(sql).run();
    assertEquals(10_000, results.recordCount());
  }
}
