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
package org.apache.drill;

import org.apache.commons.io.FileUtils;
import org.apache.drill.categories.SqlTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.schema.PathSchemaProvider;
import org.apache.drill.exec.record.metadata.schema.SchemaContainer;
import org.apache.drill.exec.record.metadata.schema.SchemaProvider;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.hadoop.fs.Path;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Category({SqlTest.class, UnlikelyTest.class})
public class TestSchemaCommands extends ClusterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testCreateWithoutSchema() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("PARSE ERROR: Lexical error");

    run("create schema for");
  }

  @Test
  public void testCreateWithForAndPath() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("PARSE ERROR: Encountered \"path\"");

    run("create schema ( col1 int, col2 int) for table tbl path '/tmp/schema.file'");
  }

  @Test
  public void testCreateWithPathAndOrReplace() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("PARSE ERROR: <OR REPLACE> cannot be used with <PATH> property");

    run("create or replace schema (col1 int, col2 int) path '/tmp/schema.file'");
  }

  @Test
  public void testCreateForMissingTable() throws Exception {
    String table = "dfs.tmp.tbl";
    thrown.expect(UserException.class);
    thrown.expectMessage("VALIDATION ERROR: Table [tbl] was not found");

    run("create schema (col1 int, col2 int) for table %s", table);
  }

  @Test
  public void testCreateForTemporaryTable() throws Exception {
    String table = "temp_create";
    try {
      run("create temporary table %s as select 'a' as c from (values(1))", table);
      thrown.expect(UserException.class);
      thrown.expectMessage(String.format("VALIDATION ERROR: Indicated table [%s] is temporary table", table));

      run("create schema (col1 int, col2 int) for table %s", table);
    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testCreateForImmutableSchema() throws Exception {
    String table = "sys.version";
    thrown.expect(UserException.class);
    thrown.expectMessage("VALIDATION ERROR: Unable to create or drop objects. Schema [sys] is immutable");

    run("create schema (col1 int, col2 int) for table %s", table);
  }

  @Test
  public void testMissingDirectory() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    Path schema = new Path(Paths.get(tmpDir.getPath(), "missing_parent_directory", "file.schema").toFile().getPath());

    thrown.expect(UserException.class);
    thrown.expectMessage(String.format("RESOURCE ERROR: Parent path for schema file [%s] does not exist", schema.toUri().getPath()));

    run("create schema (col1 int, col2 int) path '%s'", schema.toUri().getPath());
  }

  @Test
  public void testTableAsFile() throws Exception {
    File tmpDir = dirTestWatcher.getDfsTestTmpDir();
    String table = "test_table_as_file.json";
    File tablePath = new File(tmpDir, table);
    assertTrue(tablePath.createNewFile());

    thrown.expect(UserException.class);
    thrown.expectMessage(String.format("RESOURCE ERROR: Indicated table [%s] must be a directory",
      String.format("dfs.tmp.%s", table)));

    try {
      run("create schema (col1 int, col2 int) for table %s.`%s`", "dfs.tmp", table);
    } finally {
      FileUtils.deleteQuietly(tablePath);
    }
  }

  @Test
  public void testCreateSimpleForPathWithExistingSchema() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schema = new File(tmpDir, "simple_for_path.schema");
    assertTrue(schema.createNewFile());

    thrown.expect(UserException.class);
    thrown.expectMessage(String.format("VALIDATION ERROR: Schema already exists for [%s]", schema.getPath()));

    try {
      run("create schema (col1 int, col2 int) path '%s'", schema.getPath());
    } finally {
      FileUtils.deleteQuietly(schema);
    }
  }

  @Test
  public void testCreateSimpleForTableWithExistingSchema() throws Exception {
    String table = "dfs.tmp.table_for_simple_existing_schema";
    try {
      run("create table %s as select 'a' as c from (values(1))", table);
      testBuilder()
        .sqlQuery("create schema (c varchar not null) for table %s", table)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", table))
        .go();

      thrown.expect(UserRemoteException.class);
      thrown.expectMessage(String.format("VALIDATION ERROR: Schema already exists for [%s]", table));
      run("create schema (c varchar not null) for table %s", table);
    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testSuccessfulCreateForPath() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "schema_for_successful_create_for_path.schema");
    assertFalse(schemaFile.exists());
    try {
      testBuilder()
        .sqlQuery("create schema (i int not null, v varchar) path '%s'", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      assertTrue(schemaProvider.exists());

      SchemaContainer schemaContainer = schemaProvider.read();

      assertNull(schemaContainer.getTable());
      assertNotNull(schemaContainer.getSchema());

      TupleMetadata schema = schemaContainer.getSchema();
      ColumnMetadata intColumn = schema.metadata("i");
      assertFalse(intColumn.isNullable());
      assertEquals(TypeProtos.MinorType.INT, intColumn.type());

      ColumnMetadata varcharColumn = schema.metadata("v");
      assertTrue(varcharColumn.isNullable());
      assertEquals(TypeProtos.MinorType.VARCHAR, varcharColumn.type());
    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testSuccessfulCreateOrReplaceForTable() throws Exception {
    String tableName = "table_for_successful_create_or_replace_for_table";
    String table = String.format("dfs.tmp.%s", tableName);
    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      File schemaPath = Paths.get(dirTestWatcher.getDfsTestTmpDir().getPath(),
        tableName, SchemaProvider.DEFAULT_SCHEMA_NAME).toFile();

      assertFalse(schemaPath.exists());

      testBuilder()
        .sqlQuery("create schema (c varchar not null) for table %s", table)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", table))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaPath.getPath()));
      assertTrue(schemaProvider.exists());

      SchemaContainer schemaContainer = schemaProvider.read();
      assertNotNull(schemaContainer.getTable());
      assertEquals(String.format("dfs.tmp.`%s`", tableName), schemaContainer.getTable());

      assertNotNull(schemaContainer.getSchema());
      ColumnMetadata column = schemaContainer.getSchema().metadata("c");
      assertFalse(column.isNullable());
      assertEquals(TypeProtos.MinorType.VARCHAR, column.type());

      testBuilder()
        .sqlQuery("create or replace schema (c varchar) for table %s", table)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", table))
        .go();

      assertTrue(schemaProvider.exists());

      SchemaContainer updatedSchemaContainer = schemaProvider.read();
      assertNotNull(updatedSchemaContainer.getTable());
      assertEquals(String.format("dfs.tmp.`%s`", tableName), updatedSchemaContainer.getTable());

      assertNotNull(updatedSchemaContainer.getSchema());
      ColumnMetadata updatedColumn = updatedSchemaContainer.getSchema().metadata("c");
      assertTrue(updatedColumn.isNullable());
      assertEquals(TypeProtos.MinorType.VARCHAR, updatedColumn.type());
    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testCreateWithSchemaProperties() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "schema_for_create_with_properties.schema");
    assertFalse(schemaFile.exists());
    try {
      testBuilder()
        .sqlQuery("create schema (i int not null) path '%s' " +
            "properties ('k1' = 'v1', 'k2' = 'v2', 'k3' = 'v3')", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      assertTrue(schemaProvider.exists());

      SchemaContainer schemaContainer = schemaProvider.read();

      assertNull(schemaContainer.getTable());
      TupleMetadata schema = schemaContainer.getSchema();
      assertNotNull(schema);

      Map<String, String> properties = new LinkedHashMap<>();
      properties.put("k1", "v1");
      properties.put("k2", "v2");
      properties.put("k3", "v3");

      assertEquals(properties.size(), schema.properties().size());
      assertEquals(properties, schema.properties());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testCreateWithoutSchemaProperties() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "schema_for_create_without_properties.schema");
    assertFalse(schemaFile.exists());
    try {
      testBuilder()
        .sqlQuery("create schema (i int not null) path '%s'", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      assertTrue(schemaProvider.exists());

      SchemaContainer schemaContainer = schemaProvider.read();

      assertNull(schemaContainer.getTable());
      TupleMetadata schema = schemaContainer.getSchema();
      assertNotNull(schema);
      assertNotNull(schema.properties());
      assertEquals(0, schema.properties().size());
    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testCreateWithVariousColumnProperties() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "schema_for_create_with_various_column_properties.schema");
    assertFalse(schemaFile.exists());
    try {
      testBuilder()
        .sqlQuery("create schema ( " +
            "a int not null default '10', " +
            "b date format 'yyyy-MM-dd' default '2017-01-31', " +
            "c varchar properties {'k1' = 'v1', 'k2' = 'v2'}) " +
            "path '%s'",
          schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      assertTrue(schemaProvider.exists());

      SchemaContainer schemaContainer = schemaProvider.read();

      assertNull(schemaContainer.getTable());
      TupleMetadata schema = schemaContainer.getSchema();
      assertNotNull(schema);

      assertEquals(3, schema.size());

      ColumnMetadata a = schema.metadata("a");
      assertTrue(a.decodeDefaultValue() instanceof Integer);
      assertEquals(10, a.decodeDefaultValue());
      assertEquals("10", a.defaultValue());

      ColumnMetadata b = schema.metadata("b");
      assertTrue(b.decodeDefaultValue() instanceof LocalDate);
      assertEquals("yyyy-MM-dd", b.format());
      assertEquals(LocalDate.parse("2017-01-31"), b.decodeDefaultValue());
      assertEquals("2017-01-31", b.defaultValue());

      ColumnMetadata c = schema.metadata("c");
      Map<String, String> properties = new LinkedHashMap<>();
      properties.put("k1", "v1");
      properties.put("k2", "v2");
      assertEquals(properties, c.properties());

      assertEquals(0, schema.properties().size());
    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testCreateWithoutColumns() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "schema_for_create_without_columns.schema");
    assertFalse(schemaFile.exists());
    try {
      testBuilder()
        .sqlQuery("create schema () " +
            "path '%s' " +
            "properties ('prop' = 'val')",
          schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      assertTrue(schemaProvider.exists());

      SchemaContainer schemaContainer = schemaProvider.read();

      assertNull(schemaContainer.getTable());
      TupleMetadata schema = schemaContainer.getSchema();
      assertNotNull(schema);

      assertTrue(schema.isEmpty());
      assertEquals("val", schema.property("prop"));
    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testCreateUsingLoadFromMissingFile() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("RESOURCE ERROR: File with raw schema [path/to/file] does not exist");

    run("create schema load 'path/to/file' for table dfs.tmp.t");
  }

  @Test
  public void testCreateUsingLoad() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File rawSchema = new File(tmpDir, "raw.schema");
    File schemaFile = new File(tmpDir, "schema_for_create_using_load.schema");
    try {
      Files.write(rawSchema.toPath(), Arrays.asList(
        "i int,",
        "v varchar"
      ));

      assertTrue(rawSchema.exists());

      testBuilder()
        .sqlQuery("create schema load '%s' path '%s' properties ('k1'='v1', 'k2' = 'v2')",
          rawSchema.getPath(), schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      assertTrue(schemaFile.exists());

      SchemaContainer schemaContainer = schemaProvider.read();

      assertNull(schemaContainer.getTable());

      TupleMetadata schema = schemaContainer.getSchema();
      assertNotNull(schema);

      assertEquals(2, schema.size());
      assertEquals(TypeProtos.MinorType.INT, schema.metadata("i").type());
      assertEquals(TypeProtos.MinorType.VARCHAR, schema.metadata("v").type());

      assertEquals(2, schema.properties().size());
    } finally {
      FileUtils.deleteQuietly(rawSchema);
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testCreateUsingLoadEmptyFile() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File rawSchema = new File(tmpDir, "raw_empty.schema");
    File schemaFile = new File(tmpDir, "schema_for_create_using_load_empty_file.schema");

    try {
      assertTrue(rawSchema.createNewFile());

      testBuilder()
        .sqlQuery("create schema load '%s' path '%s' properties ('k1'='v1', 'k2' = 'v2')",
          rawSchema.getPath(), schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      assertTrue(schemaFile.exists());

      SchemaContainer schemaContainer = schemaProvider.read();

      assertNull(schemaContainer.getTable());

      TupleMetadata schema = schemaContainer.getSchema();
      assertNotNull(schema);

      assertEquals(0, schema.size());
      assertEquals(2, schema.properties().size());
    } finally {
      FileUtils.deleteQuietly(rawSchema);
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testDropWithoutTable() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("PARSE ERROR: Encountered \"<EOF>\"");

    run("drop schema");
  }

  @Test
  public void testDropForMissingTable() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("VALIDATION ERROR: Table [t] was not found");

    run("drop schema for table dfs.t");
  }

  @Test
  public void testDropForTemporaryTable() throws Exception {
    String table = "temp_drop";
    try {
      run("create temporary table %s as select 'a' as c from (values(1))", table);
      thrown.expect(UserException.class);
      thrown.expectMessage(String.format("VALIDATION ERROR: Indicated table [%s] is temporary table", table));

      run("drop schema for table %s", table);
    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testDropForImmutableSchema() throws Exception {
    String table = "sys.version";
    thrown.expect(UserException.class);
    thrown.expectMessage("VALIDATION ERROR: Unable to create or drop objects. Schema [sys] is immutable");

    run("drop schema for table %s", table);
  }

  @Test
  public void testDropForMissingSchema() throws Exception {
    String table = "dfs.tmp.table_with_missing_schema";
    try {
      run("create table %s as select 'a' as c from (values(1))", table);
      thrown.expect(UserException.class);
      thrown.expectMessage(String.format("VALIDATION ERROR: Schema [%s] " +
        "does not exist in table [%s] root directory", SchemaProvider.DEFAULT_SCHEMA_NAME, table));

      run("drop schema for table %s", table);
    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testDropForMissingSchemaIfExists() throws Exception {
    String table = "dfs.tmp.table_with_missing_schema_if_exists";
    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      testBuilder()
        .sqlQuery("drop schema if exists for table %s", table)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format("Schema [%s] does not exist in table [%s] root directory",
          SchemaProvider.DEFAULT_SCHEMA_NAME, table))
        .go();
    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testSuccessfulDrop() throws Exception {
    String tableName = "table_for_successful_drop";
    String table = String.format("dfs.tmp.%s", tableName);

    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      File schemaPath = Paths.get(dirTestWatcher.getDfsTestTmpDir().getPath(),
        tableName, SchemaProvider.DEFAULT_SCHEMA_NAME).toFile();

      assertFalse(schemaPath.exists());

      testBuilder()
        .sqlQuery("create schema (c varchar not null) for table %s", table)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", table))
        .go();

      assertTrue(schemaPath.exists());

      testBuilder()
        .sqlQuery("drop schema for table %s", table)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Dropped schema for table [%s]", table))
        .go();

      assertFalse(schemaPath.exists());
    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testDescribeForMissingTable() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("VALIDATION ERROR: Table [t] was not found");

    run("describe schema for table dfs.t");
  }

  @Test
  public void testDescribeForMissingSchema() throws Exception {
    String table = "dfs.tmp.table_describe_with_missing_schema";
    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      testBuilder()
        .sqlQuery("describe schema for table %s", table)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format("Schema for table [%s] is absent", table))
        .go();

    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testDescribeDefault() throws Exception {
    String tableName = "table_describe_default";
    String table = String.format("dfs.tmp.%s", tableName);
    try {
      run("create table %s as select 'a' as c from (values(1))", table);
      run("create schema (col int) for table %s", table);

      File schemaFile = Paths.get(dirTestWatcher.getDfsTestTmpDir().getPath(),
        tableName, SchemaProvider.DEFAULT_SCHEMA_NAME).toFile();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      SchemaContainer schemaContainer = schemaProvider.read();
      String schema = PathSchemaProvider.WRITER.writeValueAsString(schemaContainer);

      testBuilder()
        .sqlQuery("describe schema for table %s", table)
        .unOrdered()
        .baselineColumns("schema")
        .baselineValues(schema)
        .go();

      testBuilder()
        .sqlQuery("describe schema for table %s", table)
        .unOrdered()
        .sqlBaselineQuery("desc schema for table %s", table)
        .go();

    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testDescribeJson() throws Exception {
    String tableName = "table_describe_json";
    String table = String.format("dfs.tmp.%s", tableName);
    try {
      run("create table %s as select 'a' as c from (values(1))", table);
      run("create schema (col int) for table %s", table);

      File schemaFile = Paths.get(dirTestWatcher.getDfsTestTmpDir().getPath(),
        tableName, SchemaProvider.DEFAULT_SCHEMA_NAME).toFile();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      SchemaContainer schemaContainer = schemaProvider.read();
      String schema = PathSchemaProvider.WRITER.writeValueAsString(schemaContainer);

      testBuilder()
        .sqlQuery("describe schema for table %s as json", table)
        .unOrdered()
        .baselineColumns("schema")
        .baselineValues(schema)
        .go();

    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testDescribeStatement() throws Exception {
    String tableName = "table_describe_statement";
    String table = String.format("dfs.tmp.%s", tableName);
    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      String statement = "CREATE OR REPLACE SCHEMA \n"
        + "(\n"
        + "`col_date` DATE FORMAT 'yyyy-MM-dd' DEFAULT '-1', \n"
        + "`col_int` INT NOT NULL FORMAT 'yyyy-MM-dd' PROPERTIES { 'drill.strict' = 'true', 'some_column_prop' = 'some_column_val' }, \n"
        + "`col_array_int` ARRAY<INT>, \n"
        + "`col_nested_array_int` ARRAY<ARRAY<INT>>, \n"
        + "`col_map_required` MAP<INT, VARCHAR NOT NULL>, \n"
        + "`col_map_optional` MAP<INT, VARCHAR>, \n"
        + "`col_map_array` ARRAY<MAP<INT, VARCHAR>>, \n"
        + "`col_struct` STRUCT<`s1` INT, `s2` VARCHAR NOT NULL>, \n"
        + "`col_struct_array` ARRAY<STRUCT<`s1` INT, `s2` VARCHAR NOT NULL>>\n"
        + ") \n"
        + "FOR TABLE dfs.tmp.`table_describe_statement` \n"
        + "PROPERTIES (\n"
        + "'drill.strict' = 'false', \n"
        + "'some_schema_prop' = 'some_schema_val'\n"
        + ")";

      run(statement);

      testBuilder()
        .sqlQuery("describe schema for table %s as statement", table)
        .unOrdered()
        .baselineColumns("schema")
        .baselineValues(statement)
        .go();

    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testDescribeWithoutColumns() throws Exception {
    String tableName = "table_describe_statement_without_columns";
    String table = String.format("dfs.tmp.%s", tableName);
    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      String statement = "CREATE OR REPLACE SCHEMA \n"
        + "() \n"
        + "FOR TABLE dfs.tmp.`table_describe_statement_without_columns` \n"
        + "PROPERTIES (\n"
        + "'drill.strict' = 'false', \n"
        + "'some_schema_prop' = 'some_schema_val'\n"
        + ")";

      run(statement);

      testBuilder()
        .sqlQuery("describe schema for table %s as statement", table)
        .unOrdered()
        .baselineColumns("schema")
        .baselineValues(statement)
        .go();

    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testAlterAddAbsentKeywords() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("PARSE ERROR");
    run("alter schema for table abc add");
  }

  @Test
  public void testAlterAddAbsentSchemaForTable() throws Exception {
    String tableName = "table_alter_schema_add_absent_schema";
    String table = String.format("dfs.tmp.%s", tableName);
    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      thrown.expect(UserException.class);
      thrown.expectMessage("RESOURCE ERROR: Schema does not exist");

      run("alter schema for table %s add columns (col int)", table);
    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testAlterAddAbsentSchemaPath() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("RESOURCE ERROR: Schema does not exist");

    run("alter schema path '%s' add columns (col int)",
      new File(dirTestWatcher.getTmpDir(), "absent.schema").getPath());
  }

  @Test
  public void testAlterAddDuplicateColumn() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "schema_for_duplicate_column.schema");
    assertFalse(schemaFile.exists());
    try {
      run("create schema (col int) path '%s'", schemaFile.getPath());

      thrown.expect(UserException.class);
      thrown.expectMessage("VALIDATION ERROR");

      run("alter schema path '%s' add columns (col varchar)", schemaFile.getPath());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testAlterAddDuplicateProperty() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "schema_for_duplicate_property.schema");
    assertFalse(schemaFile.exists());
    try {
      run("create schema (col int) path '%s' properties ('prop' = 'a')", schemaFile.getPath());

      thrown.expect(UserException.class);
      thrown.expectMessage("VALIDATION ERROR");

      run("alter schema path '%s' add properties ('prop' = 'b')", schemaFile.getPath());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testAlterAddColumns() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "alter_schema_add_columns.schema");
    assertFalse(schemaFile.exists());
    try {
      run("create schema (col1 int) path '%s' properties ('prop1' = 'a')", schemaFile.getPath());

      testBuilder()
        .sqlQuery("alter schema path '%s' add " +
          "columns (col2 varchar) ", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Schema for [%s] was updated", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      TupleMetadata schema = schemaProvider.read().getSchema();

      assertEquals(2, schema.size());

      assertEquals("col1", schema.fullName(0));
      assertEquals("col2", schema.fullName(1));

      assertEquals(1, schema.properties().size());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testAlterAddProperties() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "alter_schema_add_properties.schema");
    assertFalse(schemaFile.exists());
    try {
      run("create schema (col1 int) path '%s' properties ('prop1' = 'a')", schemaFile.getPath());

      testBuilder()
        .sqlQuery("alter schema path '%s' add " +
          "properties ('prop2' = 'b', 'prop3' = 'c')", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Schema for [%s] was updated", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      TupleMetadata schema = schemaProvider.read().getSchema();

      assertEquals(1, schema.size());

      Map<String, String> expectedProperties = new HashMap<>();
      expectedProperties.put("prop1", "a");
      expectedProperties.put("prop2", "b");
      expectedProperties.put("prop3", "c");

      assertEquals(expectedProperties, schema.properties());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testAlterAddSuccess() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "alter_schema_add_success.schema");
    assertFalse(schemaFile.exists());
    try {
      run("create schema (col1 int) path '%s' properties ('prop1' = 'a')", schemaFile.getPath());

      testBuilder()
        .sqlQuery("alter schema path '%s' add " +
          "columns (col2 varchar, col3 boolean) " +
          "properties ('prop2' = 'b', 'prop3' = 'c')", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Schema for [%s] was updated", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      TupleMetadata schema = schemaProvider.read().getSchema();

      assertEquals(3, schema.size());

      assertEquals("col1", schema.fullName(0));
      assertEquals("col2", schema.fullName(1));
      assertEquals("col3", schema.fullName(2));

      Map<String, String> expectedProperties = new HashMap<>();
      expectedProperties.put("prop1", "a");
      expectedProperties.put("prop2", "b");
      expectedProperties.put("prop3", "c");

      assertEquals(expectedProperties, schema.properties());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testAlterAddForTable() throws Exception {
    String tableName = "table_for_alter_add";
    String table = String.format("dfs.tmp.%s", tableName);
    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      File schemaPath = Paths.get(dirTestWatcher.getDfsTestTmpDir().getPath(),
        tableName, SchemaProvider.DEFAULT_SCHEMA_NAME).toFile();

      run("create schema (col int) for table %s properties ('prop1' = 'a')", table);

      testBuilder()
        .sqlQuery("alter schema for table %s add or replace " +
          "columns (col2 varchar, col3 boolean) " +
          "properties ('prop2' = 'd', 'prop3' = 'c')", table)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Schema for [%s] was updated", table))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaPath.getPath()));
      assertTrue(schemaProvider.exists());

      TupleMetadata schema = schemaProvider.read().getSchema();
      assertEquals(3, schema.size());
      assertEquals(3, schema.properties().size());

    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testAlterReplace() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "alter_schema_replace_success.schema");
    assertFalse(schemaFile.exists());
    try {
      run("create schema (col1 int, col2 int) path '%s' " +
        "properties ('prop1' = 'a', 'prop2' = 'b')", schemaFile.getPath());

      testBuilder()
        .sqlQuery("alter schema path '%s' add or replace " +
          "columns (col2 varchar, col3 boolean) " +
          "properties ('prop2' = 'd', 'prop3' = 'c')", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Schema for [%s] was updated", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      TupleMetadata schema = schemaProvider.read().getSchema();

      assertEquals(3, schema.size());

      assertEquals("col1", schema.fullName(0));
      assertEquals("col2", schema.fullName(1));
      assertEquals(TypeProtos.MinorType.VARCHAR, schema.metadata("col2").type());
      assertEquals("col3", schema.fullName(2));

      Map<String, String> expectedProperties = new HashMap<>();
      expectedProperties.put("prop1", "a");
      expectedProperties.put("prop2", "d");
      expectedProperties.put("prop3", "c");

      assertEquals(expectedProperties, schema.properties());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testAlterRemoveAbsentKeywords() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("PARSE ERROR");
    run("alter schema for table abc remove");
  }

  @Test
  public void testAlterRemoveAbsentSchemaForTable() throws Exception {
    String tableName = "table_alter_schema_remove_absent_schema";
    String table = String.format("dfs.tmp.%s", tableName);
    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      thrown.expect(UserException.class);
      thrown.expectMessage("RESOURCE ERROR: Schema does not exist");

      run("alter schema for table %s remove columns (col)", table);
    } finally {
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testAlterRemoveAbsentSchemaPath() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("RESOURCE ERROR: Schema does not exist");

    run("alter schema path '%s' remove columns (col)",
      new File(dirTestWatcher.getTmpDir(), "absent.schema").getPath());
  }

  @Test
  public void testAlterRemoveColumns() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "alter_schema_remove_columns.schema");
    assertFalse(schemaFile.exists());
    try {
      run("create schema (col1 int, col2 varchar, col3 boolean, col4 int) path '%s' " +
        "properties ('prop1' = 'a', 'prop2' = 'b', 'prop3' = 'c', 'prop4' = 'd')", schemaFile.getPath());

      testBuilder()
        .sqlQuery("alter schema path '%s' remove " +
          "columns (col2, col4) ", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Schema for [%s] was updated", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      TupleMetadata schema = schemaProvider.read().getSchema();

      assertEquals(2, schema.size());

      assertEquals("col1", schema.fullName(0));
      assertEquals("col3", schema.fullName(1));

      assertEquals(4, schema.properties().size());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testAlterRemoveProperties() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "alter_schema_remove_success.schema");
    assertFalse(schemaFile.exists());
    try {
      run("create schema (col1 int, col2 varchar, col3 boolean, col4 int) path '%s' " +
        "properties ('prop1' = 'a', 'prop2' = 'b', 'prop3' = 'c', 'prop4' = 'd')", schemaFile.getPath());

      testBuilder()
        .sqlQuery("alter schema path '%s' remove " +
          "properties ('prop2', 'prop4')", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Schema for [%s] was updated", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      TupleMetadata schema = schemaProvider.read().getSchema();

      assertEquals(4, schema.size());

      Map<String, String> expectedProperties = new HashMap<>();
      expectedProperties.put("prop1", "a");
      expectedProperties.put("prop3", "c");

      assertEquals(expectedProperties, schema.properties());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testAlterRemoveSuccess() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "alter_schema_remove_success.schema");
    assertFalse(schemaFile.exists());
    try {
      run("create schema (col1 int, col2 varchar, col3 boolean, col4 int) path '%s' " +
        "properties ('prop1' = 'a', 'prop2' = 'b', 'prop3' = 'c', 'prop4' = 'd')", schemaFile.getPath());

      testBuilder()
        .sqlQuery("alter schema path '%s' remove " +
          "columns (col2, col4) " +
          "properties ('prop2', 'prop4')", schemaFile.getPath())
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Schema for [%s] was updated", schemaFile.getPath()))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaFile.getPath()));
      TupleMetadata schema = schemaProvider.read().getSchema();

      assertEquals(2, schema.size());

      assertEquals("col1", schema.fullName(0));
      assertEquals("col3", schema.fullName(1));

      Map<String, String> expectedProperties = new HashMap<>();
      expectedProperties.put("prop1", "a");
      expectedProperties.put("prop3", "c");

      assertEquals(expectedProperties, schema.properties());

    } finally {
      FileUtils.deleteQuietly(schemaFile);
    }
  }

  @Test
  public void testAlterRemoveForTable() throws Exception {
    String tableName = "table_for_alter_add";
    String table = String.format("dfs.tmp.%s", tableName);
    try {
      run("create table %s as select 'a' as c from (values(1))", table);

      File schemaPath = Paths.get(dirTestWatcher.getDfsTestTmpDir().getPath(),
        tableName, SchemaProvider.DEFAULT_SCHEMA_NAME).toFile();

      run("create schema (col1 int, col2 varchar, col3 boolean, col4 int) for table %s " +
        "properties ('prop1' = 'a', 'prop2' = 'b', 'prop3' = 'c', 'prop4' = 'd')", table);

      testBuilder()
        .sqlQuery("alter schema for table %s remove " +
          "columns (col2, col4) " +
          "properties ('prop2', 'prop4')", table)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Schema for [%s] was updated", table))
        .go();

      SchemaProvider schemaProvider = new PathSchemaProvider(new Path(schemaPath.getPath()));
      assertTrue(schemaProvider.exists());

      TupleMetadata schema = schemaProvider.read().getSchema();
      assertEquals(2, schema.size());
      assertEquals(2, schema.properties().size());

    } finally {
      run("drop table if exists %s", table);
    }
  }
}
