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

import static org.apache.drill.test.rowSet.RowSetUtilities.dec;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Iterator;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.joda.time.Period;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the schema mechanism in the context of a simple CSV tables.
 * The focus here is on the schema mechanism, which can be explored
 * with simple tables of just a few rows.
 */
@Category(EvfTest.class)
public class TestCsvWithSchema extends BaseCsvTest {

  protected static final String FILE1_NAME = "file1.csv";

  private static String basicFileContents[] = {
    "intcol,datecol,str,dub",
    "10,2019-03-20,it works!,1234.5"
  };

  private static final String raggedMulti1Contents[] = {
    "id,name,date,gender",
    "1,wilma,2019-01-18,female",
    "2,fred,2019-01-19,male",
    "4,betty,2019-05-04"
  };

  private static final String multi1Contents[] = {
    "id,name,date,gender",
    "1,wilma,2019-01-18,female",
    "2,fred,2019-01-19,male",
    "4,betty,2019-05-04,NA"
  };

  private static final String reordered2Contents[] = {
    "name,id,date",
    "barney,3,2001-01-16"
  };

  private static final String multi3Contents[] = {
    "name,date",
    "dino,2018-09-01"
  };

  private static final String nameOnlyContents[] = {
    "name",
    "dino"
  };

  private static final String SCHEMA_SQL = "create or replace schema (" +
    "id int not null, " +
    "`date` date format 'yyyy-MM-dd', " +
    "gender varchar not null default 'NA', " +
    "comment varchar not null default 'ABC') " +
    "for table %s";

  @BeforeClass
  public static void setup() throws Exception {
    BaseCsvTest.setup(false,  true);
  }

  /**
   * Test the simplest possible case: a table with one file:
   * <ul>
   * <li>Column in projection, table, and schema</li>
   * <li>Column in projection and table but not in schema.</li>
   * <li>Column in projection and schema, but not in table.</li>
   * <li>Column in projection, but not in schema or table.</li>
   * </ul>
   */
  @Test
  public void testBasicSchema() throws Exception {
    String tablePath = buildTable("basic", basicFileContents);

    try {
      enableSchemaSupport();
      String schemaSql = "create schema (intcol int not null, datecol date not null, " +
          "`dub` double not null, `extra` bigint not null default '20') " +
          "for table " + tablePath;
      run(schemaSql);
      String sql = "SELECT `intcol`, `datecol`, `str`, `dub`, `extra`, `missing` FROM " + tablePath;
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("intcol", MinorType.INT)      // Has a schema
          .add("datecol", MinorType.DATE)    // Has a schema
          .add("str", MinorType.VARCHAR)     // No schema, retains type
          .add("dub", MinorType.FLOAT8)      // Has a schema
          .add("extra", MinorType.BIGINT)    // No data, has default value
          .add("missing", MinorType.VARCHAR) // No data, no schema, default type
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(10, LocalDate.of(2019, 3, 20), "it works!", 1234.5D, 20L, "")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Show that the projection framework generates a reasonable default value
   * if told to create a required column that does not exist. In this case,
   * the default default [sic] value for an INT column is 0. There is no
   * "default" value set in the schema, so we use a "default default" instead.)
   */
  @Test
  public void testMissingRequiredCol() throws Exception {
    String tableName = "missingReq";
    String tablePath = buildTable(tableName, multi3Contents);

    try {
      enableSchemaSupport();
      run(SCHEMA_SQL, tablePath);
      String sql = "SELECT id, `name` FROM " + tablePath;
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("name", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(0, "dino")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Use a user-provided default value for a missing required column.
   */
  @Test
  public void testRequiredColDefault() throws Exception {
    String tableName = "missingReq";
    String tablePath = buildTable(tableName, multi3Contents);

    try {
      enableSchemaSupport();
      String schemaSql = SCHEMA_SQL.replace("id int not null", "id int not null default '-1'");
      run(schemaSql, tablePath);
      String sql = "SELECT id, `name`, `date` FROM " + tablePath;
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("name", MinorType.VARCHAR)
          .addNullable("date", MinorType.DATE)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(-1, "dino", LocalDate.of(2018, 9, 1))
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Use a user-provided default value for a missing required column.
   */
  @Test
  public void testDateColDefault() throws Exception {
    String tableName = "missingDate";
    String tablePath = buildTable(tableName, nameOnlyContents);

    try {
      enableSchemaSupport();
      String schemaSql = SCHEMA_SQL.replace("`date` date format 'yyyy-MM-dd'",
          "`date` date not null format 'yyyy-MM-dd' default '2001-02-03'");
      run(schemaSql, tablePath);
      String sql = "SELECT id, `name`, `date` FROM " + tablePath;
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("name", MinorType.VARCHAR)
          .add("date", MinorType.DATE)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(0, "dino", LocalDate.of(2001, 2, 3))
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Use a schema with explicit projection to get a consistent view
   * of the table schema, even if columns are missing, rows are ragged,
   * and column order changes.
   * <p>
   * Force the scans to occur in distinct fragments so the order of the
   * file batches is random.
   */
  @Test
  public void testMultiFileSchema() throws Exception {
    RowSet expected1 = null;
    RowSet expected2 = null;
    try {
      enableSchemaSupport();
      enableMultiScan();
      String tablePath = buildTable("multiFileSchema", raggedMulti1Contents, reordered2Contents);
      run(SCHEMA_SQL, tablePath);

      // Wildcard expands to union of schema + table. In this case
      // all table columns appear in the schema (though not all schema
      // columns appear in the table.)

      String sql = "SELECT id, `name`, `date`, gender, comment FROM " + tablePath;
      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("name", MinorType.VARCHAR)
          .addNullable("date", MinorType.DATE)
          .add("gender", MinorType.VARCHAR)
          .add("comment", MinorType.VARCHAR)
          .buildSchema();
      expected1 = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, "wilma", LocalDate.of(2019, 1, 18), "female", "ABC")
          .addRow(2, "fred", LocalDate.of(2019, 1, 19), "male", "ABC")
          .addRow(4, "betty", LocalDate.of(2019, 5, 4), "NA", "ABC")
          .build();
      expected2 = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(3, "barney", LocalDate.of(2001, 1, 16), "NA", "ABC")
          .build();

      // Loop 10 times so that, as the two reader fragments read the two
      // files, we end up with (acceptable) races that read the files in
      // random order.

      for (int i = 0; i < 10; i++) {
        boolean sawSchema = false;
        boolean sawFile1 = false;
        boolean sawFile2 = false;
        Iterator<DirectRowSet> iter = client.queryBuilder().sql(sql).rowSetIterator();
        while (iter.hasNext()) {
          RowSet result = iter.next();
          if (result.rowCount() == 3) {
            sawFile1 = true;
            new RowSetComparison(expected1).verifyAndClear(result);
          } else if (result.rowCount() == 1) {
            sawFile2 = true;
            new RowSetComparison(expected2).verifyAndClear(result);
          } else {
            assertEquals(0, result.rowCount());
            sawSchema = true;
          }
        }
        assertTrue(!SCHEMA_BATCH_ENABLED || sawSchema);
        assertTrue(sawFile1);
        assertTrue(sawFile2);
      }
    } finally {
      expected1.clear();
      expected2.clear();
      resetSchemaSupport();
      resetMultiScan();
    }
  }

  /**
   * Show that, without schema, the hard schema change for the "missing"
   * gender column causes an error in the sort operator when presented with
   * one batch in which gender is VARCHAR, another in which it is
   * Nullable INT. This is a consequence of using SELECT * on a distributed
   * scan.
   */
  @Test
  public void testWildcardSortFailure() throws Exception {
    try {
      enableSchema(false);
      enableMultiScan();
      String tablePath = buildTable("wildcardSortV2", multi1Contents, reordered2Contents);
      String sql = "SELECT * FROM " + tablePath + " ORDER BY id";
      boolean sawError = false;
      for (int i = 0; i < 10; i++) {
        try {
          // When this fails it will print a nasty stack trace.
          RowSet result = client.queryBuilder().sql(sql).rowSet();
          assertEquals(4, result.rowCount());
          result.clear();
        } catch (UserRemoteException e) {
          sawError = true;
          break;
        }
      }
      assertTrue(sawError);
    } finally {
      resetSchema();
      resetMultiScan();
    }
  }

  /**
   * Because V3 uses VARCHAR for missing columns, and handles ragged rows, there
   * is no vector corruption, and the sort operator sees a uniform schema, even
   * without a schema.
   * <p>
   * This and other tests enable multiple scan fragments, even for small files,
   * then run the test multiple times to generate the result set in different
   * orders (file1 first sometimes, file2 other times.)
   */
  @Test
  public void testExplicitSort() throws Exception {
    try {
      enableSchema(false);
      enableMultiScan();
      String tablePath = buildTable("explictSort1", raggedMulti1Contents, reordered2Contents);
      String sql = "SELECT id, name, gender FROM " + tablePath + " ORDER BY id";
      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.VARCHAR)
          .add("name", MinorType.VARCHAR)
          .add("gender", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow("1", "wilma", "female")
          .addRow("2", "fred", "male")
          .addRow("3", "barney", "")
          .addRow("4", "betty", "")
          .build();
      for (int i = 0; i < 10; i++) {
        RowSet result = client.queryBuilder().sql(sql).rowSet();
        new RowSetComparison(expected).verifyAndClear(result);
      }
      expected.clear();
    } finally {
      resetSchema();
      resetMultiScan();
    }
  }

  /**
   * Adding a schema makes the data types more uniform, and fills in defaults
   * for missing columns. Note that the default is not applied (at least at
   * present) if a column is missing within a file that says it has the
   * column.
   */
  @Test
  public void testSchemaExplicitSort() throws Exception {
    try {
      enableSchemaSupport();
      enableMultiScan();
      // V3 handles ragged columns
      String tablePath = buildTable("explictSort2", raggedMulti1Contents, reordered2Contents);
      run(SCHEMA_SQL, tablePath);
      String sql = "SELECT id, name, gender FROM " + tablePath + " ORDER BY id";
      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("name", MinorType.VARCHAR)
          .add("gender", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, "wilma", "female")
          .addRow(2, "fred", "male")
          .addRow(3, "barney", "NA")
          .addRow(4, "betty", "NA")
          .build();
      for (int i = 0; i < 10; i++) {
        RowSet result = client.queryBuilder().sql(sql).rowSet();
        new RowSetComparison(expected).verifyAndClear(result);
      }
      expected.clear();
    } finally {
      resetSchemaSupport();
      resetMultiScan();
    }
  }

  /**
   * Test the case that a file does not contain a required column (in this case,
   * id in the third file.) There are two choices. 1) we could fail the query,
   * 2) we can muddle through as best we can. The scan framework chooses to
   * muddle through by assuming a default value of 0 for the missing int
   * column.
   * <p>
   * Inserts an ORDER BY to force a single batch in a known order. Assumes
   * the other ORDER BY tests pass.
   * <p>
   * This test shows that having consistent types is sufficient for the sort
   * operator to work; the DAG will include a project operator that reorders
   * the columns when produced by readers in different orders. (Column ordering
   * is more an abstract concept anyway in a columnar system such as Drill.)
   */
  @Test
  public void testMultiFileSchemaMissingCol() throws Exception {
    RowSet expected = null;
    try {
      enableSchemaSupport();
      enableMultiScan();
      String tablePath = buildTable("schemaMissingCols", raggedMulti1Contents,
          reordered2Contents, multi3Contents);
      run(SCHEMA_SQL, tablePath);

      // Wildcard expands to union of schema + table. In this case
      // all table columns appear in the schema (though not all schema
      // columns appear in the table.)

      String sql = "SELECT id, `name`, `date`, gender, comment FROM " +
          tablePath + " ORDER BY id";
      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("name", MinorType.VARCHAR)
          .addNullable("date", MinorType.DATE)
          .add("gender", MinorType.VARCHAR)
          .add("comment", MinorType.VARCHAR)
          .buildSchema();
      expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(0, "dino", LocalDate.of(2018, 9, 1), "NA", "ABC")
          .addRow(1, "wilma", LocalDate.of(2019, 1, 18), "female", "ABC")
          .addRow(2, "fred", LocalDate.of(2019, 1, 19), "male", "ABC")
          .addRow(3, "barney", LocalDate.of(2001, 1, 16), "NA", "ABC")
          .addRow(4, "betty", LocalDate.of(2019, 5, 4), "NA", "ABC")
          .build();

      // Loop 10 times so that, as the two reader fragments read the two
      // files, we end up with (acceptable) races that read the files in
      // random order.

      for (int i = 0; i < 10; i++) {
        RowSet results = client.queryBuilder().sql(sql).rowSet();
        new RowSetComparison(expected).verifyAndClear(results);
      }
    } finally {
      expected.clear();
      resetSchemaSupport();
      resetMultiScan();
    }
  }

  /**
   * Test lenient wildcard projection.
   * The schema contains all columns in the table; the schema ensures
   * a consistent schema regardless of file shape or read order. The sort
   * operator works because it sees the consistent schema, despite great
   * variation in inputs.
   */
  @Test
  public void testWildcardLenientSchema() throws Exception {
    String tableName = "wildcardLenient";
    String tablePath = buildTable(tableName, multi1Contents,
        reordered2Contents, nameOnlyContents);

    try {
      enableSchemaSupport();
      run(SCHEMA_SQL, tablePath);
      String sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .addNullable("date", MinorType.DATE)
          .add("gender", MinorType.VARCHAR)
          .add("comment", MinorType.VARCHAR)
          .add("name", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(0, null, "NA", "ABC", "dino")
          .addRow(1, LocalDate.of(2019, 1, 18), "female", "ABC", "wilma")
          .addRow(2, LocalDate.of(2019, 1, 19), "male", "ABC", "fred")
          .addRow(3, LocalDate.of(2001, 1, 16), "NA", "ABC", "barney")
          .addRow(4, LocalDate.of(2019, 5, 4), "NA", "ABC", "betty")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Test wildcard projection with a strict schema: only schema columns are
   * projected.
   */
  @Test
  public void testWildcardStrictSchema() throws Exception {
    String tableName = "wildcardStrict";
    String tablePath = buildTable(tableName, multi1Contents,
        reordered2Contents, nameOnlyContents);

    try {
      enableSchemaSupport();
      String sql = SCHEMA_SQL +
          " PROPERTIES ('" + TupleMetadata.IS_STRICT_SCHEMA_PROP + "'='true')";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .addNullable("date", MinorType.DATE)
          .add("gender", MinorType.VARCHAR)
          .add("comment", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(0, null, "NA", "ABC")
          .addRow(1, LocalDate.of(2019, 1, 18), "female", "ABC")
          .addRow(2, LocalDate.of(2019, 1, 19), "male", "ABC")
          .addRow(3, LocalDate.of(2001, 1, 16), "NA", "ABC")
          .addRow(4, LocalDate.of(2019, 5, 4), "NA", "ABC")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Test a strict schema where it is needed most: in a scan with multiple
   * fragments, each of which sees a different reader schema. The output schema
   * ensures that each scan independently reports the same schema, so that the
   * downstream sort operator gets a single consistent batch schema.
   */
  @Test
  public void testMultiFragmentStrictSchema() throws Exception {
    String tableName = "wildcardStrict2";
    String tablePath = buildTable(tableName, multi1Contents,
        reordered2Contents, nameOnlyContents);

    try {
      enableSchemaSupport();
      enableMultiScan();
      String sql = SCHEMA_SQL +
          " PROPERTIES ('" + TupleMetadata.IS_STRICT_SCHEMA_PROP + "'='true')";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .addNullable("date", MinorType.DATE)
          .add("gender", MinorType.VARCHAR)
          .add("comment", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(0, null, "NA", "ABC")
          .addRow(1, LocalDate.of(2019, 1, 18), "female", "ABC")
          .addRow(2, LocalDate.of(2019, 1, 19), "male", "ABC")
          .addRow(3, LocalDate.of(2001, 1, 16), "NA", "ABC")
          .addRow(4, LocalDate.of(2019, 5, 4), "NA", "ABC")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
      resetMultiScan();
    }
  }

  private static final String boolContents[] = {
    "id,bool_col",
    "1,true",
    "2,false",
    "3,TRUE",
    "4,FALSE",
    "5,t",
    "6,T",
    "7,1",
    "8,0",
    "9",
    "10,y",
    "11,Y",
    "12,yes",
    "13,yEs",
    "14,on",
    "15,ON",
    "16,foo"
  };

  /**
   * Test the many ways to specify True for boolean columns. Anything that
   * is not true is false.
   */
  @Test
  public void testBool() throws Exception {
    String tableName = "bool";
    String tablePath = buildTable(tableName, boolContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, " +
          "bool_col boolean not null default `true` " +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("bool_col", MinorType.BIT)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow( 1, true)
          .addRow( 2, false)
          .addRow( 3, true)
          .addRow( 4, false)
          .addRow( 5, true)
          .addRow( 6, true)
          .addRow( 7, true)
          .addRow( 8, false)
          .addRow( 9, true)
          .addRow(10, true)
          .addRow(11, true)
          .addRow(12, true)
          .addRow(13, true)
          .addRow(14, true)
          .addRow(15, true)
          .addRow(16, false)
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String decimalContents[] = {
    "id,decimal_col",
    "1,12.34",
    "2,-56.789",
    "3,0",
    "4,8",
    "5",
    "6,0.00",
    "7,-0.00",
  };

  /**
   * Basic decimal sanity test showing rounding, using default values,
   * and so on.
   */
  @Test
  public void testDecimal() throws Exception {
    String tableName = "decimal";
    String tablePath = buildTable(tableName, decimalContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, " +
          "decimal_col decimal(5,2) not null default `100.00` " +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("decimal_col", MinorType.VARDECIMAL, 5, 2)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, dec("12.34"))
          .addRow(2, dec("-56.79"))
          .addRow(3, dec("0"))
          .addRow(4, dec("8"))
          .addRow(5, dec("100.00"))
          .addRow(6, dec("0.00"))
          .addRow(7, dec("0.00"))
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Verify that a decimal type without precision or scale defaults
   * to precision of 38, scale of 0.
   */
  @Test
  public void testDecimalNoPrecOrScale() throws Exception {
    String tableName = "noPrecOrScale";
    String tablePath = buildTable(tableName, decimalContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, " +
          "decimal_col decimal not null default `100.00` " +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("decimal_col", MinorType.VARDECIMAL, 38, 0)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, dec("12"))
          .addRow(2, dec("-57"))
          .addRow(3, dec("0"))
          .addRow(4, dec("8"))
          .addRow(5, dec("100"))
          .addRow(6, dec("0"))
          .addRow(7, dec("0"))
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Verify that a decimal type with no scale defaults to a scale of 0.
   */
  @Test
  public void testDecimalNoScale() throws Exception {
    String tableName = "noScale";
    String tablePath = buildTable(tableName, decimalContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, " +
          "decimal_col decimal(5) not null default `100.00` " +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("decimal_col", MinorType.VARDECIMAL, 5, 0)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, dec("12"))
          .addRow(2, dec("-57"))
          .addRow(3, dec("0"))
          .addRow(4, dec("8"))
          .addRow(5, dec("100"))
          .addRow(6, dec("0"))
          .addRow(7, dec("0"))
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String raggedDecimalContents[] = {
    "id,decimal_col",
    "1,1234.5678",
    "2",
    "3,-12.345"
  };

  /**
   * Verify that the decimal default value is rounded according
   * to the scale specified in the decimal type.
   */
  @Test
  public void testDecimalDefaultRound() throws Exception {
    String tableName = "defaultRound";
    String tablePath = buildTable(tableName, raggedDecimalContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, " +
          "decimal_col decimal(5) not null default `1111.56789` " +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("decimal_col", MinorType.VARDECIMAL, 5, 0)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, dec("1235"))
          .addRow(2, dec("1112"))
          .addRow(3, dec("-12"))
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String decimalOverflowContents[] = {
    "id,decimal_col",
    "1,99999.9",
  };

  /**
   * Test decimal overflow during data reads. The overflow occurs after
   * rounding the data value of 99999.9 to 100000.
  */
  @Test
  public void testDecimalOverflow() throws Exception {
    String tableName = "decimalOverflow";
    String tablePath = buildTable(tableName, decimalOverflowContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, " +
          "decimal_col decimal(5) not null" +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      try {
        client.queryBuilder().sql(sql).run();
        fail();
      } catch (UserRemoteException e) {
        assertTrue(e.getMessage().contains("VALIDATION ERROR"));
        assertTrue(e.getMessage().contains("Value 100000 overflows specified precision 5 with scale 0"));
      }
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Test proper handling of overflow for a default value. In this case,
   * the overflow occurs after rounding.
   */
  @Test
  public void testDecimalDefaultOverflow() throws Exception {
    String tableName = "decimalDefaultOverflow";
    String tablePath = buildTable(tableName, raggedDecimalContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, " +
          "decimal_col decimal(5) not null default `99999.9` " +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      try {
        client.queryBuilder().sql(sql).run();
        fail();
      } catch (UserRemoteException e) {
        assertTrue(e.getMessage().contains("VALIDATION ERROR"));
        assertTrue(e.getMessage().contains("Value 100000 overflows specified precision 5 with scale 0"));
      }
    } finally {
      resetSchemaSupport();
    }
  }

  @Test
  public void testInvalidDecimalSchema() throws Exception {
    String tableName = "invalidDecimal";
    String tablePath = buildTable(tableName, raggedDecimalContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, " +
          "decimal_col decimal(39) not null" +
          ") for table %s";
      try {
        run(sql, tablePath);
        fail();
      } catch (UserRemoteException e) {
        assertTrue(e.getMessage().contains("VALIDATION ERROR"));
        assertTrue(e.getMessage().contains("VARDECIMAL(39, 0) exceeds maximum suppored precision of 38"));
      }
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String trivalContents[] = {
    "id",
    "1"
  };

  @Test
  public void testMissingCols() throws Exception {
    String tableName = "missingCols";
    String tablePath = buildTable(tableName, trivalContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "col_int integer, " +
          "col_bigint bigint, " +
          "col_double double, " +
          "col_float float, " +
          "col_var varchar, " +
          "col_boolean boolean, " +
          "col_interval interval, " +
          "col_time time, " +
          "col_date date, " +
          "col_timestamp timestamp" +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .addNullable("col_int", MinorType.INT)
          .addNullable("col_bigint", MinorType.BIGINT)
          .addNullable("col_double", MinorType.FLOAT8)
          .addNullable("col_float", MinorType.FLOAT4)
          .addNullable("col_var", MinorType.VARCHAR)
          .addNullable("col_boolean", MinorType.BIT)
          .addNullable("col_interval", MinorType.INTERVAL)
          .addNullable("col_time", MinorType.TIME)
          .addNullable("col_date", MinorType.DATE)
          .addNullable("col_timestamp", MinorType.TIMESTAMP)
          .add("id", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(null, null, null, null, null, null, null, null, null, null, "1")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchema();
    }
  }

  /**
   * Verify that, if a schema is provided, a column is missing,
   * and there is no default, that the mode is left at required and
   * the column is filled with zeros. Note that this behavior is
   * specific to the text readers: if have no schema, even an missing
   * VARCHAR column will be REQUIRED and set to an empty string
   * (reason: if the column does appear it will be a required VARCHAR,
   * so, to be consistent, missing columns are also required.)
   */
  @Test
  public void testMissingColsReq() throws Exception {
    String tableName = "missingColsStrict";
    String tablePath = buildTable(tableName, trivalContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "col_int integer not null, " +
          "col_bigint bigint not null, " +
          "col_double double not null, " +
          "col_float float not null, " +
          "col_var varchar not null, " +
          "col_boolean boolean not null, " +
          "col_interval interval not null, " +
          "col_time time not null, " +
          "col_date date not null, " +
          "col_timestamp timestamp not null" +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("col_int", MinorType.INT)
          .add("col_bigint", MinorType.BIGINT)
          .add("col_double", MinorType.FLOAT8)
          .add("col_float", MinorType.FLOAT4)
          .add("col_var", MinorType.VARCHAR)
          .add("col_boolean", MinorType.BIT)
          .add("col_interval", MinorType.INTERVAL)
          .add("col_time", MinorType.TIME)
          .add("col_date", MinorType.DATE)
          .add("col_timestamp", MinorType.TIMESTAMP)
          .add("id", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(0, 0L, 0.0, 0D, "", false, new Period(0), 0, 0L, 0L, "1")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Verify the behavior of missing columns, not null mode, with
   * a default value.
   */
  @Test
  public void testMissingColsReqDefault() throws Exception {
    String tableName = "missingColsDefault";
    String tablePath = buildTable(tableName, trivalContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "col_int integer not null default '10', " +
          "col_bigint bigint not null default '10', " +
          "col_double double not null default '10.5', " +
          "col_float float not null default '10.5f', " +
          "col_var varchar not null default 'foo', " +
          "col_boolean boolean not null default '1', " +
          "col_interval interval not null default 'P10D', " +
          "col_time time not null default '12:34:56', " +
          "col_date date not null default '2019-03-28', " +
          "col_timestamp timestamp not null format 'yyyy-MM-dd HH:mm:ss' default '2019-03-28 12:34:56'" +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("col_int", MinorType.INT)
          .add("col_bigint", MinorType.BIGINT)
          .add("col_double", MinorType.FLOAT8)
          .add("col_float", MinorType.FLOAT4)
          .add("col_var", MinorType.VARCHAR)
          .add("col_boolean", MinorType.BIT)
          .add("col_interval", MinorType.INTERVAL)
          .add("col_time", MinorType.TIME)
          .add("col_date", MinorType.DATE)
          .add("col_timestamp", MinorType.TIMESTAMP)
          .add("id", MinorType.VARCHAR)
          .buildSchema();
      LocalTime lt = LocalTime.of(12, 34, 56);
      LocalDate ld = LocalDate.of(2019, 3, 28);
      Instant ts = LocalDateTime.of(ld, lt).toInstant(ZoneOffset.UTC);
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(10, 10L, 10.5, 10.5f, "foo", true, new Period(0).plusDays(10),
              lt, ld, ts, "1")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String missingColContents[] = {
    "id,amount,start_date",
    "1,20,2019-01-01",
    "2",
    "3,30"
  };

  /**
   * Demonstrate that CSV works for a schema with nullable types when columns
   * are missing (there is no comma to introduce an empty field in the data.)
   */
  @Test
  public void testMissingColsNullable() throws Exception {
    String tableName = "missingColsNullable";
    String tablePath = buildTable(tableName, missingColContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, amount int, start_date date" +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .addNullable("amount", MinorType.INT)
          .addNullable("start_date", MinorType.DATE)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, 20, LocalDate.of(2019, 1, 1))
          .addRow(2, null, null)
          .addRow(3, 30, null)
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String blankColContents[] = {
    "id,amount,start_date",
    "1,20,2019-01-01",
    "2, ,",    // Spaces intentional
    "3, 30 ,"  // Spaces intentional
  };

  /**
   * Demonstrate that CSV uses a comma to introduce a column,
   * even if that column has no data. In this case, CSV assumes the
   * value of the column is a blank string.
   * <p>
   * Such a schema cannot be converted to a number or date column,
   * even nullable, because a blank string is neither a valid number nor
   * a valid date.
   */
  @Test
  public void testBlankCols() throws Exception {
    String tableName = "blankCols";
    String tablePath = buildTable(tableName, blankColContents);

    try {
      enableSchemaSupport();
      String sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.VARCHAR)
          .add("amount", MinorType.VARCHAR)
          .add("start_date", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow("1", "20", "2019-01-01")
          .addRow("2", " ", "")
          .addRow("3", " 30 ", "")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Use the same data set as above tests, but use a schema to do type
   * conversion. Blank columns become 0 for numeric non-nullable, nulls for
   * nullable non-numeric.
   */
  @Test
  public void testBlankColsWithSchema() throws Exception {
    String tableName = "blankColsSchema";
    String tablePath = buildTable(tableName, blankColContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, amount int not null, start_date date" +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("amount", MinorType.INT)
          .addNullable("start_date", MinorType.DATE)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, 20, LocalDate.of(2019, 1, 1))
          .addRow(2, 0, null)
          .addRow(3, 30, null)
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * As above, but with a nullable numeric column. Here, by default,
   * blank values become nulls.
   */
  @Test
  public void testBlankColsWithNullableSchema() throws Exception {
    String tableName = "blankColsNullableSchema";
    String tablePath = buildTable(tableName, blankColContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, amount int, start_date date" +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .addNullable("amount", MinorType.INT)
          .addNullable("start_date", MinorType.DATE)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, 20, LocalDate.of(2019, 1, 1))
          .addRow(2, null, null)
          .addRow(3, 30, null)
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * As above, but with a non-nullable numeric column with a default
   * value.
   */
  @Test
  public void testBlankColsWithNoDefaultValue() throws Exception {
    String tableName = "blankColsNullableSchema";
    String tablePath = buildTable(tableName, blankColContents);

    try {
      enableSchemaSupport();
      String sql = "create or replace schema (" +
          "id int not null, amount int not null default '-1', start_date date" +
          ") for table %s";
      run(sql, tablePath);
      sql = "SELECT * FROM " + tablePath + "ORDER BY id";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("amount", MinorType.INT)
          .addNullable("start_date", MinorType.DATE)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(1, 20, LocalDate.of(2019, 1, 1))
          .addRow(2, -1, null)
          .addRow(3, 30, null)
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }
}
