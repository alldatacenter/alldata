/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.delegate;

import com.google.common.collect.Lists;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.spark.ArcticSparkExtensions;
import com.netease.arctic.spark.ArcticSparkSessionCatalog;
import com.netease.arctic.spark.MultiDelegateSessionCatalog;
import com.netease.arctic.spark.SparkTestContext;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.spark.SparkSessionCatalog;
import org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestMultiDelegateSessionCatalog extends SparkTestContext {

  @Rule
  public TestName testName = new TestName();
  protected long begin;

  @BeforeClass
  public static void startAll() throws IOException, ClassNotFoundException {
    Map<String, String> configs = Maps.newHashMap();
    Map<String, String> arcticConfigs = setUpTestDirAndArctic();
    Map<String, String> hiveConfigs = setUpHMS();
    configs.putAll(arcticConfigs);
    configs.putAll(hiveConfigs);
    configs.put("hive.exec.dynamic.partition.mode", "nonstrict");

    configs.put(
        "spark.sql.extensions",
        IcebergSparkSessionExtensions.class.getName() + "," + ArcticSparkExtensions.class.getName());

    configs.put("spark.sql.catalog.spark_catalog", MultiDelegateSessionCatalog.class.getName());
    configs.put("spark.sql.catalog.spark_catalog.delegates", "iceberg,arctic");

    configs.put("spark.sql.catalog.spark_catalog.arctic", ArcticSparkSessionCatalog.class.getName());
    configs.put("spark.sql.catalog.spark_catalog.arctic.url", amsUrl + "/" + catalogNameHive);

    configs.put("spark.sql.catalog.spark_catalog.iceberg", SparkSessionCatalog.class.getName());
    configs.put("spark.sql.catalog.spark_catalog.iceberg.type", "hive");

    setUpSparkSession(configs);
  }

  @AfterClass
  public static void stopAll() {
    cleanUpAms();
    cleanUpHive();
    cleanUpSparkSession();
  }

  @Before
  public void testBegin() {
    System.out.println("==================================");
    System.out.println("  Test Begin: " + testName.getMethodName());
    System.out.println("==================================");
    begin = System.currentTimeMillis();
  }

  @After
  public void testEnd() {
    long cost = System.currentTimeMillis() - begin;
    System.out.println("==================================");
    System.out.println("  Test End: " + testName.getMethodName() + ", total cost: " + cost + " ms");
    System.out.println("==================================");
  }

  private final String database = "test";
  private final String icebergTable = "iceberg_table";
  private final String arcticTable = "arctic_table";
  private final String hiveTable = "hive_table";

  List<Row> tempRows = Lists.newArrayList(
      RowFactory.create(1L, "a", "2020-01-01"),
      RowFactory.create(2L, "b", "2021-01-01"),
      RowFactory.create(3L, "c", "2022-01-01")
  );
  Schema schema = new Schema(
      Lists.newArrayList(
          Types.NestedField.of(1, false, "id", Types.LongType.get(), ""),
          Types.NestedField.of(2, false, "name", Types.StringType.get(), ""),
          Types.NestedField.of(3, false, "pt", Types.StringType.get(), "")
      )
  );
  TableIdentifier arcticTableId = TableIdentifier.of(catalogNameHive, database, arcticTable);

  @Before
  public void before() {
    sql("create database if not exists test");
    Dataset<Row> df = spark.createDataFrame(tempRows, SparkSchemaUtil.convert(schema));
    df.registerTempTable("tmp");
  }

  @After
  public void after(){
    sql("drop table if exists {0}.{1}", database, icebergTable);
    sql("drop table if exists {0}.{1}", database, arcticTable);
    sql("drop table if exists {0}.{1}", database, hiveTable);
    sql("drop database if exists {0} cascade", database);
  }

  @Test
  public void testDelegateManageSessionCatalog() throws TException {
    sql("create table {0}.{1} (id bigint, name string, pt string) " +
            "using iceberg partitioned by (truncate(pt, 4))",
        database, icebergTable);
    sql("create table {0}.{1} (id bigint, name string, pt string, primary key(pt)) " +
            "using arctic partitioned by (pt)",
        database, arcticTable);
    sql("create table {0}.{1} (id bigint, name string) partitioned by (pt string) ",
        database, hiveTable);


    assertTableExist(arcticTableId);
    Table tbl = hms.getClient().getTable(database, icebergTable);
    Assert.assertNotNull(tbl);
    Assert.assertEquals("iceberg", tbl.getParameters().get("table_type").toLowerCase());

    tbl = hms.getClient().getTable(database, hiveTable);
    Assert.assertNotNull(tbl);
    Assert.assertEquals("MANAGED_TABLE", tbl.getTableType());

    sql("insert overwrite {0}.{1} select * from tmp", database, icebergTable);
    sql("insert overwrite {0}.{1} select * from tmp", database, arcticTable);
    sql("insert overwrite {0}.{1} select * from tmp", database, hiveTable);

    writeChange(arcticTableId, ChangeAction.INSERT, Lists.newArrayList(
        newRecord(schema, 4L, "d", "2020-01-01"),
        newRecord(schema, 5L, "e", "2021-01-01"),
        newRecord(schema, 6L, "f", "2022-01-01")
    ));

    rows = sql("select id, name, pt from {0}.{1}", database, icebergTable);
    assertContainIdSet(rows,0, 1L, 2L, 3L);

    rows = sql("select id, name, pt from {0}.{1}", database, arcticTable);
    assertContainIdSet(rows,0, 1L, 2L, 3L, 4L, 5L, 6L);

    rows = sql("select id, name, pt from {0}.{1}", database, hiveTable);
    assertContainIdSet(rows,0, 1L, 2L, 3L);


    sql("set spark.sql.arctic.delegate.enabled = false");
    rows = sql("select id, name, pt from {0}.{1}", database, arcticTable);
    assertContainIdSet(rows,0, 1L, 2L, 3L);
    sql("set spark.sql.arctic.delegate.enabled = true");
  }


  @Test
  public void testIcebergCallStatement() {
    sql("create table {0}.{1} (id bigint, name string, pt string) " +
            "using iceberg partitioned by (truncate(pt, 4))",
        database, icebergTable);
    sql("insert overwrite {0}.{1} select * from tmp", database, icebergTable);
    sql("insert into {0}.{1} select * from tmp", database, icebergTable);
    rows = sql("select id, name, pt from {0}.{1}", database, icebergTable);
    Assert.assertEquals(6, rows.size());

    Dataset<Row> history = spark.read()
        .format("iceberg")
        .load(database + "." + icebergTable + ".history");
    Row first = history.filter("parent_id is null ")
        .limit(1).collectAsList().get(0);
    long snapshotId = first.getAs("snapshot_id");
    System.out.println("snapshotId: " + snapshotId);

    sql("call system.rollback_to_snapshot(''{0}.{1}'', {2})", database, icebergTable, snapshotId + "" );
    rows = sql("select id, name, pt from {0}.{1}", database, icebergTable);
    assertContainIdSet(rows,0, 1L, 2L, 3L);
  }

  @Test
  public void testCTAS() {
    sql("create table {0}.{1} using iceberg as select * from tmp", database, icebergTable);
    rows = sql("select id, name, pt from {0}.{1}", database, icebergTable);
    assertContainIdSet(rows,0, 1L, 2L, 3L);


    sql("create table {0}.{1} primary key (id) using arctic as select * from tmp", database, arcticTable);
    rows = sql("select id, name, pt from {0}.{1}", database, arcticTable);
    assertContainIdSet(rows,0, 1L, 2L, 3L);
    assertTableExist(arcticTableId);
  }

}
