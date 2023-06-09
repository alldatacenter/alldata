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

import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import static org.apache.drill.test.rowSet.RowSetUtilities.longArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(ParquetTest.class)
public class TestEmptyParquet extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    dirTestWatcher.copyResourceToRoot(Paths.get("parquet", "empty"));
    startCluster(builder);
  }

  @After
  public void reset() {
    client.resetSession(ExecConstants.PARQUET_NEW_RECORD_READER);
  }

  @Test
  public void testEmptySimpleDefaultReader() throws Exception {
    client.alterSession(ExecConstants.PARQUET_NEW_RECORD_READER, false);
    String sql = "select * from dfs.`parquet/empty/simple/empty_simple.parquet`";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testEmptySimpleNewReader() throws Exception {
    client.alterSession(ExecConstants.PARQUET_NEW_RECORD_READER, true);
    String sql = "select * from dfs.`parquet/empty/simple/empty_simple.parquet`";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testSimpleDefaultReader() throws Exception {
    client.alterSession(ExecConstants.PARQUET_NEW_RECORD_READER, false);
    String sql = "select * from dfs.`parquet/empty/simple`";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(2, "Tom")
      .build();

    RowSetUtilities.verify(expected, actual);

    String plan = queryBuilder().sql(sql).explainText();
    assertTrue(plan.contains("numRowGroups=2"));
  }

  @Test
  public void testSimpleNewReader() throws Exception {
    client.alterSession(ExecConstants.PARQUET_NEW_RECORD_READER, true);
    String sql = "select * from dfs.`parquet/empty/simple`";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(2, "Tom")
      .build();

    RowSetUtilities.verify(expected, actual);

    String plan = queryBuilder().sql(sql).explainText();
    assertTrue(plan.contains("numRowGroups=2"));
  }

  @Test
  public void testEmptyComplex() throws Exception {
    String sql = "select * from dfs.`parquet/empty/complex/empty_complex.parquet`";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .addArray("orders", TypeProtos.MinorType.BIGINT)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testComplex() throws Exception {
    String sql = "select * from dfs.`parquet/empty/complex`";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .addArray("orders", TypeProtos.MinorType.BIGINT)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(1, "Bob", longArray(1L, 2L, 3L))
      .build();

    RowSetUtilities.verify(expected, actual);

    String plan = queryBuilder().sql(sql).explainText();
    assertTrue(plan.contains("numRowGroups=2"));
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testUnion() throws Exception {
    String sql = "select id from dfs.`parquet/empty/simple/empty_simple.parquet` union " +
      "select id from dfs.`parquet/empty/simple/non_empty_simple.parquet`";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(2)
      .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testJoin() throws Exception {
    String sql = "select * from dfs.`parquet/empty/simple/non_empty_simple.parquet` n left join " +
      "dfs.`parquet/empty/simple/empty_simple.parquet` e on n.id = e.id";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .addNullable("id0", TypeProtos.MinorType.BIGINT)
      .addNullable("name0", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(2, "Tom", null, null)
      .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testCountDirect() throws Exception {
    String sql = "select count(1) from dfs.`parquet/empty/simple/empty_simple.parquet`";

    long count = queryBuilder().sql(sql).singletonLong();
    assertEquals(0L, count);

    String plan = queryBuilder().sql(sql).explainText();
    assertTrue(plan.contains("DynamicPojoRecordReader"));
  }

  @Test
  public void testCountInDirectDefaultReader() throws Exception {
    client.alterSession(ExecConstants.PARQUET_NEW_RECORD_READER, false);
    String sql = "select count(1) from dfs.`parquet/empty/simple/empty_simple.parquet` where random = 1";

    long count = queryBuilder().sql(sql).singletonLong();
    assertEquals(0L, count);

    String plan = queryBuilder().sql(sql).explainText();
    assertFalse(plan.contains("DynamicPojoRecordReader"));
  }

  @Test
  public void testCountInDirectNewReader() throws Exception {
    client.alterSession(ExecConstants.PARQUET_NEW_RECORD_READER, true);
    String sql = "select count(1) from dfs.`parquet/empty/simple/empty_simple.parquet` where random = 1";

    long count = queryBuilder().sql(sql).singletonLong();
    assertEquals(0L, count);

    String plan = queryBuilder().sql(sql).explainText();
    assertFalse(plan.contains("DynamicPojoRecordReader"));
  }

  @Test
  public void testEmptySimpleFilterExistentColumn() throws Exception {
    String sql = "select name from dfs.`parquet/empty/simple/empty_simple.parquet` where id = 1";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testSimpleFilterExistentColumn() throws Exception {
    String sql = "select name from dfs.`parquet/empty/simple` where id = 2";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addSingleCol("Tom")
      .build();

    RowSetUtilities.verify(expected, actual);

    String plan = queryBuilder().sql(sql).explainText();
    assertTrue(plan.contains("numRowGroups=1"));
  }

  @Test
  public void testEmptyComplexFilterExistentColumn() throws Exception {
    String sql = "select orders from dfs.`parquet/empty/complex/empty_complex.parquet` where id = 1";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addArray("orders", TypeProtos.MinorType.BIGINT)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testComplexFilterExistentColumn() throws Exception {
    String sql = "select orders from dfs.`parquet/empty/complex` where id = 1";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addArray("orders", TypeProtos.MinorType.BIGINT)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addSingleCol(longArray(1L, 2L, 3L))
      .build();

    RowSetUtilities.verify(expected, actual);

    String plan = queryBuilder().sql(sql).explainText();
    assertTrue(plan.contains("numRowGroups=1"));
  }

  @Test
  public void testSimpleFilterNonExistentColumn() throws Exception {
    String sql = "select name from dfs.`parquet/empty/simple/empty_simple.parquet` where random = 1";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testComplexFilterNonExistentColumn() throws Exception {
    String sql = "select orders from dfs.`parquet/empty/complex/empty_complex.parquet` where random = 1";

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addArray("orders", TypeProtos.MinorType.BIGINT)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .build();

    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testEmptySimpleWithMetadataFile() throws Exception {
    String tableName = "empty_simple_metadata";
    String fileName = "empty_simple.parquet";
    File tableFileLocation = new File(dirTestWatcher.makeRootSubDir(Paths.get(tableName)), tableName);
    URL sourceFile = Resources.getResource(Paths.get("parquet", "empty", "simple", fileName).toFile().getPath());
    dirTestWatcher.copyResourceToRoot(Paths.get(sourceFile.toURI()), Paths.get(tableFileLocation.toURI()));

    queryBuilder().sql("refresh table metadata dfs.`%s`", tableName).run();

    String sql = String.format("select * from dfs.`%s`", tableName);

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .build();

    RowSetUtilities.verify(expected, actual);

    String plan = queryBuilder().sql(sql).explainText();
    assertTrue(plan.contains("usedMetadataFile=true"));
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testSimpleWithMetadataFile() throws Exception {
    String tableName = "simple_metadata";
    File tableLocation = dirTestWatcher.makeRootSubDir(Paths.get(tableName));
    URL sourceFolder = Resources.getResource(Paths.get("parquet", "empty", "simple").toFile().getPath());
    dirTestWatcher.copyResourceToRoot(Paths.get(sourceFolder.toURI()), Paths.get(tableLocation.toURI()));

    queryBuilder().sql("refresh table metadata dfs.`%s`", tableName).run();

    String sql = String.format("select * from dfs.`%s`", tableName);

    RowSet actual = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(2, "Tom")
      .build();

    RowSetUtilities.verify(expected, actual);

    String plan = queryBuilder().sql(sql).explainText();
    assertTrue(plan.contains("usedMetadataFile=true"));
  }

  /**
   * Test a Parquet file containing a zero-byte dictionary page, c.f.
   * DRILL-8023.
   */
  @Test
  public void testEmptyDictPage() throws Exception {
    try {
      client.alterSession(ExecConstants.PARQUET_NEW_RECORD_READER, false);
      // Only column 'C' in empty_dict_page.parquet has an empty dictionary page.
      String sql = "select A,B,C from dfs.`parquet/empty/empty_dict_page.parquet`";
      RowSet actual = queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("A", TypeProtos.MinorType.BIGINT)
        .addNullable("B", TypeProtos.MinorType.VARCHAR)
        .addNullable("C", TypeProtos.MinorType.INT)
        .buildSchema();

      RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow(1, "a", null)
        .addRow(2, "b", null)
        .addRow(3, "c", null)
        .build();

      RowSetUtilities.verify(expected, actual);
    } finally {
      client.resetSession(ExecConstants.PARQUET_NEW_RECORD_READER);
    }
  }
}
