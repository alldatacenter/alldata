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
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * test for arctic keyed table
 */
public class TestCreateTableDDL extends SparkTestBase {

  private final String database = "db_def";
  private final String tableA = "testa";
  private final String tableB = "testb";

  @Before
  public void prepare() {
    sql("create database if not exists " + database);
  }

  @After
  public void clean() {
    sql("drop database if exists " + database + " cascade");
  }


  @Test
  public void testCreateKeyedTableWithPartitioned() throws TException {
    // hive style
    TableIdentifier identifierA = TableIdentifier.of(catalogName, database, tableA);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by (ts string, dt string) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA);
    assertTableExist(identifierA);
    ArcticTable keyedTableA = loadTable(identifierA);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchema, keyedTableA.schema().asStruct());
    sql("desc table {0}.{1}", database, tableA);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    Assert.assertArrayEquals("Primary should match expected",
        new List[]{Collections.singletonList("id")},
        new List[]{keyedTableA.asKeyedTable().primaryKeySpec().fieldNames()});
    Assert.assertTrue(keyedTableA.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", keyedTableA.properties().get("props.test1"));
    Assert.assertTrue(keyedTableA.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", keyedTableA.properties().get("props.test2"));

    Table hiveTableA = hms.getClient().getTable(database, tableA);
    Assert.assertNotNull(hiveTableA);
    rows = sql("desc table {0}.{1}", database, tableA);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("drop table {0}.{1}", database, tableA);
    assertTableNotExist(identifierA);

    // column reference style
    TableIdentifier identifierB = TableIdentifier.of(catalogName, database, tableB);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string ,\n " +
        " dt string ,\n " +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by (ts, dt) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    ArcticTable keyedTableB = loadTable(identifierB);
    Types.StructType expectedSchemaB = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchemaB, keyedTableB.schema().asStruct());

    sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    Assert.assertArrayEquals("Primary should match expected",
        new List[]{Collections.singletonList("id")},
        new List[]{keyedTableB.asKeyedTable().primaryKeySpec().fieldNames()});

    Assert.assertTrue(keyedTableB.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", keyedTableB.properties().get("props.test1"));
    Assert.assertTrue(keyedTableB.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", keyedTableB.properties().get("props.test2"));

    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifierB);
  }

  @Test
  public void testCreateKeyedTableUnPartitioned() throws TException {
    TableIdentifier identifierA = TableIdentifier.of(catalogName, database, tableA);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp , \n " +
        " primary key (id) \n" +
        ") using arctic \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA);

    assertTableExist(identifierA);
    ArcticTable keyedTable = loadTable(identifierA);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone())
    );
    Assert.assertEquals("Schema should match expected",
        expectedSchema, keyedTable.schema().asStruct());

    Assert.assertArrayEquals("Primary should match expected",
        new List[]{Collections.singletonList("id")},
        new List[]{keyedTable.asKeyedTable().primaryKeySpec().fieldNames()});
    Assert.assertTrue(keyedTable.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", keyedTable.properties().get("props.test1"));
    Assert.assertTrue(keyedTable.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", keyedTable.properties().get("props.test2"));

    Table hiveTableA = hms.getClient().getTable(database, tableA);
    Assert.assertNotNull(hiveTableA);
    rows = sql("desc table {0}.{1}", database, tableA);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts"),
        Lists.newArrayList());

    sql("drop table {0}.{1}", database, tableA);
    assertTableNotExist(identifierA);
  }


  @Test
  public void testCreateUnKeyedTableWithPartitioned() throws TException {
    // hive style
    TableIdentifier identifierA = TableIdentifier.of(catalogName, database, tableA);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string \n " +
        ") using arctic \n" +
        " partitioned by (ts string, dt string) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA);

    assertTableExist(identifierA);
    ArcticTable unKeyedTable = loadTable(identifierA);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchema, unKeyedTable.schema().asStruct());

    Assert.assertTrue(unKeyedTable.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", unKeyedTable.properties().get("props.test1"));
    Assert.assertTrue(unKeyedTable.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", unKeyedTable.properties().get("props.test2"));

    Table hiveTableA = hms.getClient().getTable(database, tableA);
    Assert.assertNotNull(hiveTableA);
    rows = sql("desc table {0}.{1}", database, tableA);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("drop table {0}.{1}", database, tableA);
    assertTableNotExist(identifierA);

    // column reference style
    TableIdentifier identifierB = TableIdentifier.of(catalogName, database, tableB);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string, \n " +
        " dt string \n " +
        ") using arctic \n" +
        " partitioned by (ts, dt) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    ArcticTable unKeyedTableB = loadTable(identifierB);
    Types.StructType expectedSchemaB = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()),
        Types.NestedField.optional(4, "dt", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchemaB, unKeyedTableB.schema().asStruct());

    sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    Assert.assertTrue(unKeyedTableB.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", unKeyedTableB.properties().get("props.test1"));
    Assert.assertTrue(unKeyedTableB.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", unKeyedTableB.properties().get("props.test2"));

    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));


    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifierB);
  }

  @Test
  public void testCreateUnKeyedTableUnPartitioned() throws TException {
    TableIdentifier identifier = TableIdentifier.of(catalogName, database, tableB);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string \n " +
        ") using arctic \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    ArcticTable unKeyedTableB = loadTable(identifier);
    Types.StructType expectedSchemaB = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.StringType.get()));
    Assert.assertEquals("Schema should match expected",
        expectedSchemaB, unKeyedTableB.schema().asStruct());

    Assert.assertTrue(unKeyedTableB.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", unKeyedTableB.properties().get("props.test1"));
    Assert.assertTrue(unKeyedTableB.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", unKeyedTableB.properties().get("props.test2"));

    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts"),
        Lists.newArrayList());

    sql("drop table {0}.{1}", database, tableB);
    assertTableNotExist(identifier);
  }

  @Test
  public void testCreateHiveTableUnPartitioned() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string \n " +
        ") STORED AS parquet ", database, tableB);

    sql("use {0}", database);
    rows = sql("show tables");
    Assert.assertEquals(1, rows.size());
    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts"),
        Lists.newArrayList());

    sql("drop table {0}.{1}", database, tableB);
    rows = sql("show tables");
    Assert.assertEquals(0, rows.size());
  }


  @Test
  public void testCreateHiveTableWithPartitioned() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string \n" +
        ") partitioned by (ts string, dt string) " +
        "STORED AS parquet ", database, tableB);

    sql("use {0}", database);
    rows = sql("show tables");
    Assert.assertEquals(1, rows.size());
    sql("desc {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("drop table {0}.{1}", database, tableB);
    rows = sql("show tables");
    Assert.assertEquals(0, rows.size());
  }

  @Test
  public void testCreateSourceTableWithPartitioned() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string, \n " +
        " dt string \n " +
        ") using parquet \n" +
        " partitioned by (ts, dt) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    sql("use {0}", database);
    rows = sql("show tables");
    Assert.assertEquals(1, rows.size());
    sql("desc {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts", "dt"),
        Lists.newArrayList("ts", "dt"));

    sql("drop table {0}.{1}", database, tableB);
    rows = sql("show tables");
    Assert.assertEquals(0, rows.size());
  }


  @Test
  public void testCreateSourceTableUnPartitioned() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n" +
        " ts string \n " +
        ") using parquet \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableB);

    sql("use {0}", database);
    rows = sql("show tables");
    Assert.assertEquals(1, rows.size());

    Table hiveTableB = hms.getClient().getTable(database, tableB);
    Assert.assertNotNull(hiveTableB);
    rows = sql("desc table {0}.{1}", database, tableB);
    assertHiveDesc(rows,
        Lists.newArrayList("id", "name", "ts"),
        Lists.newArrayList());

    sql("drop table {0}.{1}", database, tableB);
    rows = sql("show tables");
    Assert.assertEquals(0, rows.size());
  }
}
