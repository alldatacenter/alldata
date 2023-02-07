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
package org.apache.drill.exec.store.easy.text.compliant;

import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.TestEmptyInputSql;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test behavior of the text (CSV) reader for files without headers
 * and without an external schema. Data is represented with the
 * `columns` array column.
 */
@Category(EvfTest.class)
public class TestCsvWithoutHeaders extends BaseCsvTest {

  private static final String TEST_FILE_NAME = "simple.csv";

  private static String sampleData[] = {
      "10,foo,bar",
      "20,fred,wilma"
  };

  private static String raggedRows[] = {
      "10,dino",
      "20,foo,bar",
      "30"
  };

  private static String secondSet[] = {
      "30,barney,betty"
  };

  @BeforeClass
  public static void setup() throws Exception {
    BaseCsvTest.setup(false,  false);

    buildFile(TEST_FILE_NAME, sampleData);
    buildNestedTableWithoutHeaders();
  }

  protected static void buildNestedTableWithoutHeaders() throws IOException {

    // Two-level partitioned table
    File rootDir = new File(testDir, PART_DIR);
    rootDir.mkdir();
    buildFile(new File(rootDir, ROOT_FILE), sampleData);
    File nestedDir = new File(rootDir, NESTED_DIR);
    nestedDir.mkdir();
    buildFile(new File(nestedDir, NESTED_FILE), secondSet);
  }

  /**
   * Verify that the wildcard expands to the `columns` array
   */
  @Test
  public void testWildcard() throws IOException {
    String sql = "SELECT * FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, TEST_FILE_NAME).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addSingleCol(strArray("10", "foo", "bar"))
        .addSingleCol(strArray("20", "fred", "wilma"))
        .build();
    RowSetUtilities.verify(expected, actual);
  }

  /**
   * An empty no-headers file has a valid schema: it will always
   * be `columns`. The scan operator can return a single, empty
   * batch with that schema to represent the empty file.
   *
   * @see {@link TestEmptyInputSql#testQueryEmptyCsv}
   */
  @Test
  public void testEmptyFile() throws IOException {
    buildFile(EMPTY_FILE, new String[] {});
    String sql = "SELECT * FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, EMPTY_FILE).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .build();
    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testColumns() throws IOException {
    String sql = "SELECT columns FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, TEST_FILE_NAME).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addSingleCol(strArray("10", "foo", "bar"))
        .addSingleCol(strArray("20", "fred", "wilma"))
        .build();
    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void doTestWildcardAndMetadata() throws IOException {
    String sql = "SELECT *, filename FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, TEST_FILE_NAME).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .add("filename", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(strArray("10", "foo", "bar"), TEST_FILE_NAME)
        .addRow(strArray("20", "fred", "wilma"), TEST_FILE_NAME)
        .build();
    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testColumnsAndMetadata() throws IOException {
    String sql = "SELECT columns, filename FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, TEST_FILE_NAME).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .add("filename", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(strArray("10", "foo", "bar"), TEST_FILE_NAME)
        .addRow(strArray("20", "fred", "wilma"), TEST_FILE_NAME)
        .build();
    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testSpecificColumns() throws IOException {
    String sql = "SELECT columns[0], columns[2] FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, TEST_FILE_NAME).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("EXPR$0", MinorType.VARCHAR)
        .addNullable("EXPR$1", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("10", "bar")
        .addRow("20", "wilma")
        .build();
    RowSetUtilities.verify(expected, actual);
  }

  @Test
  public void testRaggedRows() throws IOException {
    String fileName = "ragged.csv";
    buildFile(fileName, raggedRows);
    String sql = "SELECT columns FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, fileName).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();
    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addSingleCol(strArray("10", "dino"))
        .addSingleCol(strArray("20", "foo", "bar"))
        .addSingleCol(strArray("30"))
        .build();
    RowSetUtilities.verify(expected, actual);
  }

  /**
   * Test partition expansion.
   * <p>
   * V3, as in V2 before Drill 1.12, puts partition columns after
   * data columns (so that data columns don't shift positions if
   * files are nested to another level.)
   */
  @Test
  public void testPartitionExpansion() throws IOException {
    String sql = "SELECT * FROM `dfs.data`.`%s`";
    Iterator<DirectRowSet> iter = client.queryBuilder().sql(sql, PART_DIR).rowSetIterator();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .addNullable("dir0", MinorType.VARCHAR)
        .buildSchema();

    RowSet rowSet;
    if (SCHEMA_BATCH_ENABLED) {

      // First batch is empty; just carries the schema.
      assertTrue(iter.hasNext());
      rowSet = iter.next();
      assertEquals(0, rowSet.rowCount());
      rowSet.clear();
    }

    // Read the two data batches.
    for (int i = 0; i < 2; i++) {
      assertTrue(iter.hasNext());
      rowSet = iter.next();

      // Figure out which record this is and test accordingly.
      RowSetReader reader = rowSet.reader();
      assertTrue(reader.next());
      ArrayReader ar = reader.array(0);
      assertTrue(ar.next());
      String col1 = ar.scalar().getString();
      if (col1.equals("10")) {
        RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
            .addRow(strArray("10", "foo", "bar"), null)
            .addRow(strArray("20", "fred", "wilma"), null)
            .build();
        RowSetUtilities.verify(expected, rowSet);
      } else {
        RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
            .addRow(strArray("30", "barney", "betty"), NESTED_DIR)
            .build();
        RowSetUtilities.verify(expected, rowSet);
      }
    }
    assertFalse(iter.hasNext());
  }

  /**
   * When the `columns` array is allowed, the projection list cannot
   * implicitly suggest that `columns` is a map.
   * <p>
   * V2 message: DATA_READ ERROR: Selected column 'columns' must be an array index
   * @throws Exception
   */
  @Test
  public void testColumnsAsMap() throws Exception {
    String sql = "SELECT `%s`.columns.foo FROM `dfs.data`.`%s`";
    try {
      client.queryBuilder().sql(sql, TEST_FILE_NAME, TEST_FILE_NAME).run();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains(
          "VALIDATION ERROR: Column `columns` has map elements, but must be an array"));
      assertTrue(e.getMessage().contains("Plugin config name: csv"));
    }
  }
  /**
   * When the `columns` array is allowed, and an index is projected,
   * it must be below the maximum.
   * <p>
   * V2 message: INTERNAL_ERROR ERROR: 70000
   * @throws Exception
   */
  @Test
  public void testColumnsIndexOverflow() throws Exception {
    String sql = "SELECT columns[70000] FROM `dfs.data`.`%s`";
    try {
      client.queryBuilder().sql(sql, TEST_FILE_NAME, TEST_FILE_NAME).run();
    } catch (UserRemoteException e) {
      assertTrue(e.getMessage().contains(
          "VALIDATION ERROR: `columns`[70000] index out of bounds, max supported size is 65536"));
      assertTrue(e.getMessage().contains("Plugin config name: csv"));
    }
  }

  @Test
  public void testHugeColumn() throws IOException {
    String fileName = buildBigColFile(false);
    String sql = "SELECT * FROM `dfs.data`.`%s`";
    RowSet actual = client.queryBuilder().sql(sql, fileName).rowSet();
    assertEquals(10, actual.rowCount());
    RowSetReader reader = actual.reader();
    ArrayReader arrayReader = reader.array(0);
    while (reader.next()) {
      int i = reader.logicalIndex();
      arrayReader.next();
      assertEquals(Integer.toString(i + 1), arrayReader.scalar().getString());
      arrayReader.next();
      String big = arrayReader.scalar().getString();
      assertEquals(BIG_COL_SIZE, big.length());
      for (int j = 0; j < BIG_COL_SIZE; j++) {
        assertEquals((char) ((j + i) % 26 + 'A'), big.charAt(j));
      }
      arrayReader.next();
      assertEquals(Integer.toString((i + 1) * 10), arrayReader.scalar().getString());
    }
    actual.clear();
  }
}
