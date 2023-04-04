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

package com.netease.arctic.spark.delegate;

import com.netease.arctic.spark.ArcticSparkSessionCatalog;
import com.netease.arctic.spark.SparkTestContext;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.spark.SparkCatalog;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.catalyst.analysis.TableAlreadyExistsException;
import org.apache.spark.sql.connector.catalog.CatalogManager;
import org.apache.spark.sql.connector.catalog.CatalogPlugin;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestArcticSessionCatalog extends SparkTestContext {

  @Rule
  public TestName testName = new TestName();
  protected long begin ;

  @BeforeClass
  public static void startAll() throws IOException, ClassNotFoundException {
    Map<String, String> configs = Maps.newHashMap();
    Map<String, String> arcticConfigs = setUpTestDirAndArctic();
    Map<String, String> hiveConfigs = setUpHMS();
    configs.putAll(arcticConfigs);
    configs.putAll(hiveConfigs);

    configs.put("spark.sql.catalog.spark_catalog", ArcticSparkSessionCatalog.class.getName());
    configs.put("spark.sql.catalog.spark_catalog.url", amsUrl + "/" + catalogNameHive);
    configs.put("spark.sql.catalog.catalog", SparkCatalog.class.getName());
    configs.put("spark.sql.catalog.catalog.type", "hive");
    configs.put("spark.sql.arctic.delegate.enabled", "true");

    setUpSparkSession(configs);
  }

  @AfterClass
  public static void stopAll() {
    cleanUpAms();
    cleanUpHive();
    cleanUpSparkSession();
  }


  @Before
  public void testBegin(){
    System.out.println("==================================");
    System.out.println("  Test Begin: " + testName.getMethodName());
    System.out.println("==================================");
    begin = System.currentTimeMillis();
  }

  @After
  public void after() {
    long cost = System.currentTimeMillis() - begin;
    System.out.println("==================================");
    System.out.println("  Test End: " + testName.getMethodName() + ", total cost: " + cost + " ms");
    System.out.println("==================================");
  }

  private String database = "default";
  private String table2 = "test2";
  private String table3 = "test3";
  private String table_D = "test4";
  private String table_D2 = "test5";

  @Ignore
  @Test
  public void testHiveDelegate() throws TException {
    System.out.println("spark.sql.arctic.delegate.enabled = " + spark.conf().get("spark.sql.arctic.delegate.enabled"));
    sql("use spark_catalog");
    sql("create table {0}.{1} ( id int, data string) using arctic", database, table_D);
    sql("create table {0}.{1} ( id int, data string) STORED AS parquet", database, table_D2);
    sql("insert overwrite {0}.{1} values \n" +
        "(1, ''aaa''), \n " +
        "(2, ''bbb''), \n " +
        "(3, ''ccc'') \n ", database, table_D);
    sql("insert overwrite {0}.{1} values \n" +
        "(1, ''aaa''), \n " +
        "(2, ''bbb''), \n " +
        "(3, ''ccc'') \n ", database, table_D2);
    Table tableA = hms.getClient().getTable(database, table_D);
    Assert.assertNotNull(tableA);
    Table tableB = hms.getClient().getTable(database, table_D2);
    Assert.assertNotNull(tableB);
    sql("select * from {0}.{1}", database, table_D);
    sql("select * from {0}.{1}", database, table_D2);

    sql("drop table {0}.{1}", database, table_D);
    sql("drop table {0}.{1}", database, table_D2);

  }

  @Test
  public void testTableDDL() throws TableAlreadyExistsException, NoSuchTableException {
    TableIdentifier tableIdent = TableIdentifier.of(catalogNameHive, database, table_D);
    sql("use spark_catalog");
    sql("create table {0}.{1} ( id int, data string) using arctic", database, table_D);
    sql("ALTER TABLE {0}.{1} ADD COLUMN c3 INT", database, table_D);
    Types.StructType expectedSchema = Types.StructType.of(
        Types.NestedField.optional(1, "id", Types.IntegerType.get()),
        Types.NestedField.optional(2, "data", Types.StringType.get()),
        Types.NestedField.optional(3, "c3", Types.IntegerType.get()));

    Assert.assertEquals("Schema should match expected",
        expectedSchema, loadTable(tableIdent).schema().asStruct());

    // test rename
    CatalogManager catalogManager = spark.sessionState().catalogManager();
    CatalogPlugin catalog = catalogManager.currentCatalog();
    Identifier oldName = Identifier.of(new String[]{database}, table_D);
    Identifier newName = Identifier.of(new String[]{database}, table_D2);
    if (catalog instanceof ArcticSparkSessionCatalog) {
      Assert.assertThrows(
          UnsupportedOperationException.class,
          () ->  ((ArcticSparkSessionCatalog<?>) catalog).renameTable(oldName, newName));
      ((ArcticSparkSessionCatalog<?>) catalog).dropTable(oldName);
    }

    sql("create table {0}.{1} ( id int, data string) using iceberg", database, table_D);
    if (catalog instanceof ArcticSparkSessionCatalog) {
      ((ArcticSparkSessionCatalog<?>) catalog).renameTable(oldName, newName);
      ((ArcticSparkSessionCatalog<?>) catalog).dropTable(newName);
      Identifier noTable = Identifier.of(new String[]{database}, "no_table");
      ((ArcticSparkSessionCatalog<?>) catalog).dropTable(noTable);
    }
  }

  @Ignore
  @Test
  public void testCatalogEnable() throws TException {
    sql("set spark.sql.arctic.delegate.enabled=false");
    sql("use spark_catalog");
    System.out.println("spark.sql.arctic.delegate.enabled = " + spark.conf().get("spark.sql.arctic.delegate.enabled"));
    sql("create table {0}.{1} ( id int, data string) STORED AS parquet", database, table2);
    sql("insert overwrite {0}.{1} values \n" +
        "(1, ''aaa''), \n " +
        "(2, ''bbb''), \n " +
        "(3, ''ccc'') \n ", database, table2);
    Table tableB = hms.getClient().getTable(database, table2);
    Assert.assertNotNull(tableB);
    sql("select * from {0}.{1}", database, table2);

    sql("create table {0}.{1} ( id int, data string) STORED AS parquet", database, table3);
    sql("insert overwrite {0}.{1} values \n" +
        "(4, ''aaa''), \n " +
        "(5, ''bbb''), \n " +
        "(6, ''ccc'') \n ", database, table2);
    rows = sql("select * from {0}.{1}", database, table2);
    assertContainIdSet(rows, 0, 4, 5, 6);
    sql("select * from {0}.{1}", database, table3);

    sql("drop table {0}.{1}", database, table2);
    sql("drop table {0}.{1}", database, table3);
  }

  @Test
  public void testCreateTableLikeUsingSparkCatalog() {
    sql("set spark.sql.arctic.delegate.enabled=true");
    sql("use spark_catalog");
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp , \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( ts ) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, table3);

    sql("create table {0}.{1} like {2}.{3} using arctic", database, table2, database, table3);
    sql("desc table {0}.{1}", database, table2);
    assertDescResult(rows, Lists.newArrayList("id"));

    sql("desc table extended {0}.{1}", database, table2);
    assertDescResult(rows, Lists.newArrayList("id"));

    sql("drop table {0}.{1}", database, table2);

    sql("drop table {0}.{1}", database, table3);
  }

  @Test
  public void testCreateTableLikeWithNoProvider() throws TException {
    sql("set spark.sql.arctic.delegate.enabled=true");
    sql("use spark_catalog");
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp,  \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( ts ) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, table3);

    sql("create table {0}.{1} like {2}.{3}", database, table2, database, table3);
    Table hiveTableA = hms.getClient().getTable(database, table2);
    Assert.assertNotNull(hiveTableA);
    sql("drop table {0}.{1}", database, table2);

    sql("drop table {0}.{1}", database, table3);
  }

  @Test
  public void testCreateTableLikeWithoutArcticCatalogWithNoProvider() throws TException {
    sql("use catalog");
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts timestamp  \n" +
        ") stored as parquet \n" +
        " partitioned by ( ts ) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, table3);

    sql("create table {0}.{1} like {2}.{3}", database, table2, database, table3);
    Table hiveTableA = hms.getClient().getTable(database, table2);
    Assert.assertNotNull(hiveTableA);
    sql("use spark_catalog");
    sql("drop table {0}.{1}", database, table3);
    sql("drop table {0}.{1}", database, table2);
  }


  @Test
  public void testCreateTableAsSelect() {
    sql("set spark.sql.arctic.delegate.enabled=true");
    String table = "test_create_table_as_select";
    List<Row> tempRows = com.google.common.collect.Lists.newArrayList(
        RowFactory.create(1L, "a", "2020-01-01"),
        RowFactory.create(2L, "b", "2021-01-01"),
        RowFactory.create(3L, "c", "2022-01-01")
    );
    Schema schema = new Schema(
        com.google.common.collect.Lists.newArrayList(
            Types.NestedField.of(1, false, "id", Types.LongType.get(), ""),
            Types.NestedField.of(2, false, "name", Types.StringType.get(), ""),
            Types.NestedField.of(3, false, "pt", Types.StringType.get(), "")
        )
    );
    TableIdentifier arcticTableId = TableIdentifier.of(catalogNameHive, database, table);
    Dataset<Row> df = spark.createDataFrame(tempRows, SparkSchemaUtil.convert(schema));
    df.registerTempTable("tmp");

    sql("create table {0}.{1} primary key (id) using arctic as select * from tmp", database, table);
    rows = sql("select id, name, pt from {0}.{1}", database, table);
    assertContainIdSet(rows,0, 1L, 2L, 3L);
    assertTableExist(arcticTableId);

    sql("drop table {0}.{1}", database, table);
  }

  @Test
  public void testLoadTable() throws TException {
    sql("use {0}", catalogNameHive);
    sql("create table {0}.{1} ( id int, data string) using arctic", database, table_D);
    sql("insert overwrite {0}.{1} values \n" +
        "(1, ''aaa''), \n " +
        "(2, ''bbb''), \n " +
        "(3, ''ccc'') \n ", database, table_D);
    Table tableA = hms.getClient().getTable(database, table_D);
    Assert.assertNotNull(tableA);
    sql("use spark_catalog");
    rows = sql("select * from {0}.{1}", database, table_D);
    Assert.assertEquals(rows.size(), 3);
    sql("drop table {0}.{1}", database, table_D);
  }

}
