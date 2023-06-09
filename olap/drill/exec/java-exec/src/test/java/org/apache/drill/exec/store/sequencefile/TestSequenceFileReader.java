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
package org.apache.drill.exec.store.sequencefile;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Paths;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.easy.sequencefile.SequenceFileBatchReader;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.hadoop.io.BytesWritable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestSequenceFileReader extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));
    dirTestWatcher.copyResourceToRoot(Paths.get("sequencefiles/"));
  }

  @Test
  public void testStarQuery() throws Exception {
    String sql = "select * from cp.`sequencefiles/simple.seq`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable(SequenceFileBatchReader.KEY_SCHEMA, MinorType.VARBINARY)
        .addNullable(SequenceFileBatchReader.VALUE_SCHEMA, MinorType.VARBINARY)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("key1".getBytes(), "value1".getBytes())
        .addRow("key2".getBytes(), "value2".getBytes())
        .build();

    assertEquals(2, sets.rowCount());

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testExplicitQuery() throws Exception {
    String sql = "select convert_from(binary_key, 'UTF8') as binary_key from cp.`sequencefiles/simple.seq`";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable(SequenceFileBatchReader.KEY_SCHEMA, MinorType.VARCHAR)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(byteWritableString("key0"))
        .addRow(byteWritableString("key1"))
        .build();

    assertEquals(2, sets.rowCount());

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testLimitPushdown() throws Exception {
    String sql = "select * from cp.`sequencefiles/simple.seq` limit 1 offset 1";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    assertEquals(1, sets.rowCount());
    sets.clear();
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "select count(*) from cp.`sequencefiles/simple.seq`";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();

    assertEquals("Counts should match", 2, cnt);
  }

  private static String byteWritableString(String input) throws Exception {
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bout);
    final BytesWritable writable = new BytesWritable(input.getBytes("UTF-8"));
    writable.write(out);
    return new String(bout.toByteArray());
  }
}
