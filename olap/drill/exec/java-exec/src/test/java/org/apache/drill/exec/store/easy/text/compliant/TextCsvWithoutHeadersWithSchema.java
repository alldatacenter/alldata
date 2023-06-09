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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the combination of a CSV file without headers, but with a
 * provided schema. The provided schema <b>must</b> list columns
 * in the order they appear in the file, with no holes. The column
 * list need not be exhaustive, however, there can be unlabeled columns
 * at the end which are ignored. No `columns` array is created in
 * this case.
 * <p>
 * Not exhaustive: once we get past building the schema, operation
 * is identical to the with-headers, with-schema case.
 */
public class TextCsvWithoutHeadersWithSchema extends BaseCsvTest {

  private static String basicFileContents[] = {
      "10,2019-03-20,it works!,1234.5,ignore me"
  };

  private static String fileContents[] = {
      "10,2019-03-20,it works!,1234.5"
  };

  @BeforeClass
  public static void setup() throws Exception {
    BaseCsvTest.setup(false,  false);
  }

  /**
   * Test the simplest possible case: a table with one file:
   * <ul>
   * <li>Column in projection, table, and schema</li>
   * <li>Column in projection, but not in schema or table.</li>
   * <li>Column in schema and table, but not in projection.</li>
   * <li>Column in table (at end of row), but not in schema</li>
   * </ul>
   * Also tests type conversion, including "empty" (no) conversion.
   */
  @Test
  public void testBasicSchema() throws Exception {
    String tablePath = buildTable("basic", basicFileContents);

    try {
      enableSchemaSupport();
      String schemaSql =
          "create schema (intcol int not null, datecol date not null, " +
          "`str` varchar not null, `dub` double not null) " +
          "for table %s";
      run(schemaSql, tablePath);
      String sql = "SELECT `intcol`, `datecol`, `str`, `dub`, `missing` FROM %s";
      RowSet actual = client.queryBuilder().sql(sql, tablePath).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("intcol", MinorType.INT)      // Has a schema
          .add("datecol", MinorType.DATE)    // Has a schema
          .add("str", MinorType.VARCHAR)     // Has a schema, original type
          .add("dub", MinorType.FLOAT8)      // Has a schema
          .add("missing", MinorType.VARCHAR) // No data, no schema, default type
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(10, new LocalDate(2019, 3, 20), "it works!", 1234.5D, "")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Test column in schema, not in file.
   */
  @Test
  public void testMissingColumn() throws Exception {
    String tablePath = buildTable("missingCol", fileContents);

    try {
      enableSchemaSupport();
      String schemaSql =
          "create schema (intcol int not null, datecol date not null, " +
          "`str` varchar not null, `dub` double not null, " +
          "`extra` bigint not null default '20') " +
          "for table %s";
      run(schemaSql, tablePath);
      String sql = "SELECT `intcol`, `datecol`, `str`, `dub`, `extra`, `missing` FROM %s";
      RowSet actual = client.queryBuilder().sql(sql, tablePath).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("intcol", MinorType.INT)      // Has a schema
          .add("datecol", MinorType.DATE)    // Has a schema
          .add("str", MinorType.VARCHAR)     // Has a schema, original type
          .add("dub", MinorType.FLOAT8)      // Has a schema
          .add("extra", MinorType.BIGINT)    // No data, has default value
          .add("missing", MinorType.VARCHAR) // No data, no schema, default type
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(10, new LocalDate(2019, 3, 20), "it works!", 1234.5D, 20L, "")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Test with an implicit column.
   */
  @Test
  public void filenameColumn() throws Exception {
    String tablePath = buildTable("test3", fileContents);

    try {
      enableSchemaSupport();
      String schemaSql =
          "create schema (intcol int not null, datecol date not null, " +
          "`str` varchar not null, `dub` double not null) " +
          "for table %s";
      run(schemaSql, tablePath);
      String sql = "SELECT `intcol`, `datecol`, `str`, `dub`, `filename` FROM %s";
      RowSet actual = client.queryBuilder().sql(sql, tablePath).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("intcol", MinorType.INT)
          .add("datecol", MinorType.DATE)
          .add("str", MinorType.VARCHAR)
          .add("dub", MinorType.FLOAT8)
          .add("filename", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(10, new LocalDate(2019, 3, 20), "it works!", 1234.5D, "file0.csv")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }
}
