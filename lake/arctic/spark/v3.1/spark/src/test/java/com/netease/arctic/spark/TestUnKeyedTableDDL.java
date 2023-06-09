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

import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.types.Types;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TestUnKeyedTableDDL extends SparkTestBase {
  private static final Logger LOG = LoggerFactory.getLogger(TestUnKeyedTableDDL.class);

  @Before
  public void prepare() {
    sql("use " + catalogNameArctic);
    sql("drop database if exists db_def");
    sql("drop database if exists db_test");
    sql("drop database if exists db");
    sql("drop database if exists arctic_db");
  }

  @Test
  public void testDatabaseDDL() throws Exception {
    sql("use " + catalogNameArctic);
    sql("show databases");
    Assert.assertEquals(0, rows.size());

    sql("create database db1");
    sql("show databases");
    Assert.assertEquals(1, rows.size());
    Assert.assertEquals("db1", rows.get(0)[0]);

    sql("drop database db1");
    Assert.assertEquals(0, rows.size());
  }

  @Test
  public void testArcticCatalogTableDDL() throws Exception {
    TableIdentifier ident = TableIdentifier.of(catalogNameArctic, "def", "t1");
    doTableCreateTest(ident);
    doTablePropertiesAlterTest(ident);
    doTableColumnAlterTest(ident);
    doTableDropTest(ident);
  }


  private void doTableCreateTest(TableIdentifier ident)   {
    sql("use " + ident.getCatalog());
    if (catalogNameArctic.equals(ident.getCatalog())){
      sql("create database " + ident.getDatabase());
    }

    List<Object[]> rows;

    Assert.assertThrows(
        NoSuchObjectException.class,
        () -> ams.handler().getTable(ident.buildTableIdentifier()));

    sql("create table " + ident.getDatabase() + "." + ident.getTableName() + " ( \n" +
        " id int , \n" +
        " name string, \n" +
        " birthday date ) \n" +
        " using arctic \n " +
        " partitioned by ( days(birthday) )");
    assertTableExist(ident);
    rows = sql("desc table {0}.{1}", ident.getDatabase(), ident.getTableName());
    Assert.assertEquals(6, rows.size());

    rows = sql("show tables");
    Assert.assertEquals(1, rows.size());
    // result: |namespace|tableName|
    Assert.assertEquals(ident.getDatabase(), rows.get(0)[0]);
    Assert.assertEquals(ident.getTableName(), rows.get(0)[1]);
  }

  protected void doTablePropertiesAlterTest(TableIdentifier ident) {
    sql("use " + ident.getCatalog());

    assertTableExist(ident);
    sql("alter table " + ident.getDatabase() + "." + ident.getTableName()
        + " set tblproperties ('test-props' = 'val')");

    ArcticTable table = loadTable(ident);
    Map<String, String> props = table.properties();
    Assert.assertEquals("val",props.get("test-props"));
    props = loadTablePropertiesFromAms(ident);
    Assert.assertTrue(props.containsKey("test-props"));

    sql("alter table " + ident.getDatabase() + "." + ident.getTableName()
      + " unset tblproperties ('test-props') ");
    table = loadTable(ident);
    Assert.assertFalse(table.properties().containsKey("test-props"));
    props = loadTablePropertiesFromAms(ident);
    Assert.assertFalse(props.containsKey("test-props"));

  }

  protected void doTableColumnAlterTest(TableIdentifier ident){
    sql("use " + ident.getCatalog());

    assertTableExist(ident);
    sql("alter table " + ident.getDatabase() + "." + ident.getTableName()
        + " add column col double ");

    ArcticTable table = loadTable(ident);
    Types.NestedField field = table.schema().findField("col");
    Assert.assertNotNull(field);
    Assert.assertEquals(Types.DoubleType.get(), field.type());

    sql("alter table " + ident.getDatabase() + "." + ident.getTableName()
        + " drop column col");
    table = loadTable(ident);
    field = table.schema().findField("col");
    Assert.assertNull(field);
  }

  protected void doTableDropTest(TableIdentifier ident)  {
    sql("use " + ident.getCatalog());
    assertTableExist(ident);

    sql("drop table " + ident.getDatabase()  + "." + ident.getTableName());
    rows = sql("show tables");
    Assert.assertEquals(0, rows.size());

    Assert.assertThrows(
        NoSuchObjectException.class,
        () -> ams.handler().getTable(ident.buildTableIdentifier()));
  }
}
