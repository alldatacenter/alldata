/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark;

import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.types.Types;
import org.apache.spark.SparkException;
import org.apache.spark.sql.AnalysisException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * test for alter arctic keyed table
 */
public class TestAlterKeyedTable extends SparkTestBase {
  private final String database = "db_def";
  private final String tableName = "testA";
  private final TableIdentifier tableIdent = TableIdentifier.of(catalogNameArctic, database, tableName);

  @Before
  public void prepare() {
    sql("use " + catalogNameArctic);
    sql("create database if not exists " + database);
    sql("CREATE TABLE {0}.{1} \n" +
            "(id bigint, data string, ts timestamp, primary key (id)) \n" +
            "using arctic partitioned by ( days(ts) ) \n" +
            "tblproperties ( 'test.props1' = 'val1' , 'test.props2' = 'val2' )",
        database,
        tableName);
  }

  @After
  public void removeTables() {
    sql("DROP TABLE IF EXISTS {0}.{1}", database, tableName);
  }

  @Test
  public void testKeyedColumnNotNull() {
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.LongType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()));
    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(tableIdent).schema().asStruct());
  }

  @Test
  public void testAddColumnNotNull() {
    Assert.assertThrows(
        SparkException.class,
        () -> sql("ALTER TABLE {0}.{1} ADD COLUMN c3 INT NOT NULL", database, tableName));
  }

  @Test
  public void testAddColumn() {
    sql(
        "ALTER TABLE {0}.{1} ADD COLUMN point struct<x: double NOT NULL, y: double NOT NULL> AFTER id",
        database,
        tableName);

    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.LongType.get()),
        Types.NestedField.optional(4, "point", Types.StructType.of(
            Types.NestedField.required(5, "x", Types.DoubleType.get()),
            Types.NestedField.required(6, "y", Types.DoubleType.get())
        )),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()));

    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(tableIdent).schema().asStruct());

    sql("ALTER TABLE {0}.{1} ADD COLUMN point.z double COMMENT ''May be null'' FIRST", database, tableName);

    Types.StructType expectedSchema2 = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.LongType.get()),
        Types.NestedField.optional(4, "point", Types.StructType.of(
            Types.NestedField.optional(7, "z", Types.DoubleType.get(), "May be null"),
            Types.NestedField.required(5, "x", Types.DoubleType.get()),
            Types.NestedField.required(6, "y", Types.DoubleType.get())
        )),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()));

    Assert.assertEquals("Schema should match expected",
        expectedSchema2, loadTable(tableIdent).schema().asStruct());
  }

  @Test
  public void testDropColumn() {
    sql("ALTER TABLE {0}.{1} DROP COLUMN data", database, tableName);

    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.LongType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()));

    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(tableIdent).schema().asStruct());
  }

  @Test
  public void testRenameColumn() {
    sql("ALTER TABLE {0}.{1} RENAME COLUMN data TO row_data", database, tableName);

    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.LongType.get()),
        Types.NestedField.optional(2, "row_data", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()));

    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(tableIdent).schema().asStruct());
  }

  @Test
  public void testAlterColumnComment() {
    sql("ALTER TABLE {0}.{1} ALTER COLUMN id COMMENT ''Record id''", database, tableName);

    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.LongType.get(), "Record id"),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()));

    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(tableIdent).schema().asStruct());
  }

  @Test
  public void testAlterColumnType() {
    sql("ALTER TABLE {0}.{1} ADD COLUMN count int", database, tableName);
    sql("ALTER TABLE {0}.{1} ALTER COLUMN count TYPE bigint", database, tableName);

    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.LongType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()),
        Types.NestedField.optional(4, "count", Types.LongType.get()));

    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(tableIdent).schema().asStruct());
  }

  @Test
  public void testAlterColumnDropNotNull() {
    sql("ALTER TABLE {0}.{1} ALTER COLUMN data DROP NOT NULL", database, tableName);

    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.LongType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()));

    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(tableIdent).schema().asStruct());
  }

  @Test
  public void testAlterColumnSetNotNull() {
    Assert.assertThrows(
        AnalysisException.class,
        () -> sql("ALTER TABLE {0}.{1} ALTER COLUMN data SET NOT NULL", database, tableName));
  }

  @Test
  public void testAlterColumnPositionAfter() {
    sql("ALTER TABLE {0}.{1} ADD COLUMN count int", database, tableName);
    sql("ALTER TABLE {0}.{1} ALTER COLUMN count AFTER id", database, tableName);

    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.LongType.get()),
        Types.NestedField.optional(4, "count", Types.IntegerType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()));

    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(tableIdent).schema().asStruct());
  }

  @Test
  public void testAlterTableProperties() {
    sql("ALTER TABLE {0}.{1} SET TBLPROPERTIES " +
        "( ''test.props2'' = ''new-value'', ''test.props3'' = ''val3'' )", database, tableName);

    ArcticTable keyedTable = loadTable(catalogNameArctic, database, tableName);
    //Assert.assertEquals(3, keyedTable.properties().size());

    Assert.assertEquals("val1", keyedTable.properties().get("test.props1"));
    Assert.assertEquals("new-value", keyedTable.properties().get("test.props2"));
    Assert.assertEquals("val3", keyedTable.properties().get("test.props3"));
  }

  @Test
  public void testAlterTableUnsetProperties() {
    sql("ALTER TABLE {0}.{1} SET TBLPROPERTIES " +
        "( ''test.props2'' = ''new-value'', ''test.props3'' = ''val3'' )", database, tableName);
    sql("ALTER TABLE {0}.{1} UNSET TBLPROPERTIES " +
        "( ''test.props2'' )", database, tableName);

    ArcticTable keyedTable = loadTable(catalogNameArctic, database, tableName);

    Assert.assertEquals("val1", keyedTable.properties().get("test.props1"));
    Assert.assertNull(keyedTable.properties().get("test.props2"));
    Assert.assertEquals("val3", keyedTable.properties().get("test.props3"));
  }
}
