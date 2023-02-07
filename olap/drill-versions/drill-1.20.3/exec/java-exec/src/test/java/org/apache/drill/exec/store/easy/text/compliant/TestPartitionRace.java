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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Demonstrates a race condition inherent in the way that partition
 * columns are currently implemented. Two files: one at the root directory,
 * one down one level. Parallelization is forced to two. (Most tests use
 * small files and both files end up being read in the same scanner, which
 * masks the problem shown here.)
 * <p>
 * Depending on which file is read first, the output row may start with
 * or without the partition column. Once the column occurs, it will
 * persist.
 * <p>
 * The solution is to figure out the max partition depth in the
 * EasySubScan rather than in each scan operator, which is done in the
 * current "V3" version. The tests here verify this behavior.
 */

@Category(EvfTest.class)
public class TestPartitionRace extends BaseCsvTest {

  @BeforeClass
  public static void setup() throws Exception {
    BaseCsvTest.setup(false,  true, 2);

    // Two-level partitioned table

    File rootDir = new File(testDir, PART_DIR);
    rootDir.mkdir();
    buildFile(new File(rootDir, "first.csv"), validHeaders);
    File nestedDir = new File(rootDir, NESTED_DIR);
    nestedDir.mkdir();
    buildFile(new File(nestedDir, "second.csv"), secondFile);
  }

  /**
   * Oddly, when run in a single fragment, the files occur in a
   * stable order, the partition always appears, and it appears in
   * the first column position.
   */
  @Test
  public void testSingleScan() throws IOException {
    String sql = "SELECT * FROM `dfs.data`.`%s`";

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .add("b", MinorType.VARCHAR)
        .add("c", MinorType.VARCHAR)
        .addNullable("dir0", MinorType.VARCHAR)
        .buildSchema();

    Iterator<DirectRowSet> iter = client.queryBuilder().sql(sql, PART_DIR).rowSetIterator();
    RowSet rowSet;
    if (SCHEMA_BATCH_ENABLED) {
      // First batch is empty; just carries the schema.

      assertTrue(iter.hasNext());
      rowSet = iter.next();
      assertEquals(0, rowSet.rowCount());
      rowSet.clear();
    }

    // Read the two batches.

    for (int j = 0; j < 2; j++) {
      assertTrue(iter.hasNext());
      rowSet = iter.next();

      // Figure out which record this is and test accordingly.

      RowSetReader reader = rowSet.reader();
      assertTrue(reader.next());
      String col1 = reader.scalar("a").getString();
      if (col1.equals("10")) {
        RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
            .addRow("10", "foo", "bar", null)
            .build();
        RowSetUtilities.verify(expected, rowSet);
      } else {
        RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
            .addRow("20", "fred", "wilma", NESTED_DIR)
            .build();
        RowSetUtilities.verify(expected, rowSet);
      }
    }
    assertFalse(iter.hasNext());
  }

  /**
   * V3 computes partition depth in the group scan (which sees all files), and
   * so the partition column count does not vary across scans. Also, V3 puts
   * partition columns at the end of the row so that data columns don't
   * "jump around" when files are shifted to a new partition depth.
   */
  @Test
  public void testNoRace() throws IOException {
    String sql = "SELECT * FROM `dfs.data`.`%s`";

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .add("b", MinorType.VARCHAR)
        .add("c", MinorType.VARCHAR)
        .addNullable("dir0", MinorType.VARCHAR)
        .buildSchema();

    try {
      enableMultiScan();

      // Loop to run the query 10 times or until we see both files
      // in the first position.

      boolean sawRootFirst = false;
      boolean sawNestedFirst = false;
      for (int i = 0; i < 10; i++) {

        Iterator<DirectRowSet> iter = client.queryBuilder().sql(sql, PART_DIR).rowSetIterator();
        RowSet rowSet;
        if (SCHEMA_BATCH_ENABLED) {
          // First batch is empty; just carries the schema.

          assertTrue(iter.hasNext());
          rowSet = iter.next();
          assertEquals(0, rowSet.rowCount());
          rowSet.clear();
        }

        // Read the two batches.

        for (int j = 0; j < 2; j++) {
          assertTrue(iter.hasNext());
          rowSet = iter.next();

          // Figure out which record this is and test accordingly.

          RowSetReader reader = rowSet.reader();
          assertTrue(reader.next());
          String col1 = reader.scalar("a").getString();
          if (col1.equals("10")) {
            if (i == 0) {
              sawRootFirst = true;
            }
            RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
                .addRow("10", "foo", "bar", null)
                .build();
            RowSetUtilities.verify(expected, rowSet);
          } else {
            if (i == 0) {
              sawNestedFirst = true;
            }
            RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
                .addRow("20", "fred", "wilma", NESTED_DIR)
                .build();
            RowSetUtilities.verify(expected, rowSet);
          }
        }
        assertFalse(iter.hasNext());
        if (sawRootFirst &&
            sawNestedFirst) {
          // The following should appear most of the time.
          System.out.println("Both variations occurred");
          return;
        }
      }

      // If you see this, maybe something got fixed. Or, maybe the
      // min parallelization hack above stopped working.
      // Or, you were just unlucky and can try the test again.
      // We print messages, rather than using assertTrue, to avoid
      // introducing a flaky test.

      System.out.println("Some variations did not occur");
      System.out.println(String.format("Outer first: %s", sawRootFirst));
      System.out.println(String.format("Nested first: %s", sawNestedFirst));
    } finally {
      resetMultiScan();
    }
  }
}
