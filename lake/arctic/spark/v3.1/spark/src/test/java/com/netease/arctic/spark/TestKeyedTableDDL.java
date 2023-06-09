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

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * test for arctic keyed table
 */
public class TestKeyedTableDDL extends SparkTestBase {

  private final String database = "db_def";
  private final String table = "testA";
  private final String targetTable = "testB";

  @Before
  public void prepare() {
    sql("use " + catalogNameArctic);
    sql("create database if not exists " + database);
  }

  @After
  public void cleanUp() {
    sql("set `spark.sql.arctic.use-timestamp-without-timezone-in-new-tables` = `false`");
  }

  @Test
  public void testCreateKeyedTable() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, table);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " point struct<x: double NOT NULL, y: double NOT NULL> , \n " +
        " maps map<string, string>, \n " +
        " arrays array<string>, \n " +
        " ts timestamp , \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( days(ts) ) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, table);
    assertTableExist(identifier);
    sql("desc table {0}.{1}", database, table);
    assertDescResult(rows, Lists.newArrayList("id"));

    sql("desc table extended {0}.{1}", database, table);
    assertDescResult(rows, Lists.newArrayList("id"));

    ArcticTable keyedTable = loadTable(identifier);
    Assert.assertTrue(keyedTable.properties().containsKey("props.test1"));
    Assert.assertEquals("val1", keyedTable.properties().get("props.test1"));
    Assert.assertTrue(keyedTable.properties().containsKey("props.test2"));
    Assert.assertEquals("val2", keyedTable.properties().get("props.test2"));

    sql("drop table {0}.{1}", database, table);
    assertTableNotExist(identifier);
  }


  @Test
  public void testCreateKeyedTableLike() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, targetTable);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp , \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( days(ts) ) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, table);

    sql("create table {0}.{1} like {2}.{3} using arctic", database, targetTable, database, table);
    sql("desc table {0}.{1}", database, targetTable);
    assertDescResult(rows, Lists.newArrayList("id"));

    sql("desc table extended {0}.{1}", database, targetTable);
    assertDescResult(rows, Lists.newArrayList("id"));

    sql("drop table {0}.{1}", database, targetTable);

    sql("drop table {0}.{1}", database, table);
    assertTableNotExist(identifier);
  }

  @Test
  public void testCreateUnKeyedTableLike() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, targetTable);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp " +
        ") using arctic \n" +
        " partitioned by ( days(ts) ) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, table);

    sql("create table {0}.{1} like {2}.{3} using arctic", database, targetTable, database, table);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()));
    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(identifier).schema().asStruct());

    sql("drop table {0}.{1}", database, targetTable);

    sql("drop table {0}.{1}", database, table);
    assertTableNotExist(identifier);
  }

  @Test
  public void testCreateTimestampWithZone() {
    TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, targetTable);
    sql("set `spark.sql.arctic.use-timestamp-without-timezone-in-new-tables` = `true`");

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp " +
        ") using arctic \n" +
        " partitioned by ( days(ts) ) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, table);

    sql("create table {0}.{1} like {2}.{3} using arctic", database, targetTable, database, table);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "name", Types.StringType.get()),
        Types.NestedField.optional(3, "ts", Types.TimestampType.withoutZone()));
    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(identifier).schema().asStruct());

    sql("drop table {0}.{1}", database, targetTable);

    sql("drop table {0}.{1}", database, table);
    assertTableNotExist(identifier);
  }

  @Test
  public void testCreateTableRefreshed() throws TException {
    File testArcticDir = new File(testBaseDir, "arctic2");
    sql("set `spark.sql.arctic.refresh-catalog-before-usage` = {0}", "true");
    CatalogMeta catalog = ams.handler().getCatalog(catalogNameArctic);
    ams.handler().updateMeta(catalog, CatalogMetaProperties.KEY_WAREHOUSE, testArcticDir.getPath());
    TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, table);

    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp " +
        ") using arctic \n" +
        " partitioned by ( days(ts) ) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, table);

    ArcticTable arcticTable = loadTable(identifier);
    String expectLocation = testArcticDir.getPath() + "/" +database + "/"+ table + "/base";
    Assert.assertEquals(expectLocation, arcticTable.location());
    sql("drop table {0}.{1}", database, table);
    assertTableNotExist(identifier);
  }
}
