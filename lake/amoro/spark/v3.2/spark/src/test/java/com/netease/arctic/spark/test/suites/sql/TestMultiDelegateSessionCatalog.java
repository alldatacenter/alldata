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

package com.netease.arctic.spark.test.suites.sql;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.spark.ArcticSparkExtensions;
import com.netease.arctic.spark.ArcticSparkSessionCatalog;
import com.netease.arctic.spark.MultiDelegateSessionCatalog;
import com.netease.arctic.spark.test.SparkTestBase;
import com.netease.arctic.spark.test.SparkTestContext;
import com.netease.arctic.spark.test.helper.RecordGenerator;
import com.netease.arctic.spark.test.helper.TestTableHelper;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.Schema;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.spark.SparkSessionCatalog;
import org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.thrift.TException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TestMultiDelegateSessionCatalog extends SparkTestBase {

  private String ident(String tableName) {
    return database + "." + tableName;
  }


  @Override
  protected Map<String, String> sparkSessionConfig() {
    Map<String, String> configs = Maps.newHashMap();
    configs.put("hive.exec.dynamic.partition.mode", "nonstrict");

    configs.put("spark.sql.catalog.spark_catalog", MultiDelegateSessionCatalog.class.getName());
    configs.put("spark.sql.catalog.spark_catalog.delegates", "iceberg,arctic");

    configs.put("spark.sql.catalog.spark_catalog.arctic", ArcticSparkSessionCatalog.class.getName());
    configs.put("spark.sql.catalog.spark_catalog.arctic.url",
        context.catalogUrl(SparkTestContext.EXTERNAL_HIVE_CATALOG_NAME));

    configs.put("spark.sql.catalog.spark_catalog.iceberg", SparkSessionCatalog.class.getName());
    configs.put("spark.sql.catalog.spark_catalog.iceberg.type", "hive");
    configs.put("spark.sql.extensions",
        IcebergSparkSessionExtensions.class.getName() + "," + ArcticSparkExtensions.class.getName());

    return configs;
  }

  ArcticCatalog arcticCatalog;

  @Override
  protected ArcticCatalog catalog() {
    if (arcticCatalog == null) {
      String catalogUrl = context.catalogUrl(SparkTestContext.EXTERNAL_HIVE_CATALOG_NAME);
      arcticCatalog = CatalogLoader.load(catalogUrl);
    }
    return arcticCatalog;
  }

  private final String database = "test";
  private final String icebergTable = "iceberg_table";
  private final String arcticTable = "arctic_table";
  private final String hiveTable = "hive_table";
  private final String tempView = "tmp";
  private final TableIdentifier arcticTableIdentifier = TableIdentifier.of(
      SparkTestContext.EXTERNAL_HIVE_CATALOG_NAME, database, arcticTable);

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

  @BeforeEach
  public void before() {
    sql("create database if not exists test");
    Dataset<Row> df = spark().createDataFrame(tempRows, SparkSchemaUtil.convert(schema));
    df.createOrReplaceTempView("tmp");
    try {
      catalog().dropTable(arcticTableIdentifier, true);
    } catch (Exception e) {
      //pass
    }
  }

  @AfterEach
  public void after() {
    sql("drop table if exists " + ident(icebergTable));
    sql("drop table if exists " + ident(arcticTable));
    sql("drop table if exists " + ident(hiveTable));
    sql("drop database if exists " + database + " cascade");
    spark().sessionState().catalog().dropTempView(tempView);
  }

  @Test
  public void testDelegateManageSessionCatalog() throws TException {
    sql("create table " + ident(icebergTable) +
        " (id bigint, name string, pt string) " +
        "using iceberg partitioned by (truncate(pt, 4))");

    sql("create table " + ident(arcticTable) +
        " (id bigint, name string, pt string, primary key(pt)) " +
        "using arctic partitioned by (pt)");


    sql("create table " + ident(hiveTable) +
        " (id bigint, name string) partitioned by (pt string) ");

    final TableIdentifier arcticTableId = TableIdentifier.of(catalog().name(), database, arcticTable);

    catalog().tableExists(arcticTableId);

    Table tbl = context.getHiveClient().getTable(database, icebergTable);
    Assertions.assertNotNull(tbl);
    Assertions.assertEquals("iceberg", tbl.getParameters().get("table_type").toLowerCase());

    tbl = context.getHiveClient().getTable(database, hiveTable);
    Assertions.assertNotNull(tbl);
    Assertions.assertEquals("MANAGED_TABLE", tbl.getTableType());

    sql("insert overwrite " + ident(icebergTable) + " select * from tmp");
    sql("insert overwrite " + ident(arcticTable) + " select * from tmp");
    sql("insert overwrite " + ident(hiveTable) + " select * from tmp");

    ArcticTable table = catalog().loadTable(arcticTableId);
    Assertions.assertTrue(table.isKeyedTable());
    TestTableHelper.writeToChange(table.asKeyedTable(), Lists.newArrayList(
        RecordGenerator.newRecord(schema, 4L, "d", "2020-01-01"),
        RecordGenerator.newRecord(schema, 5L, "e", "2021-01-01"),
        RecordGenerator.newRecord(schema, 6L, "f", "2022-01-01")
    ), ChangeAction.INSERT);

    Dataset<Row> rows = sql("select id, name, pt from " + ident(icebergTable));
    assertContainIdSet(rows, 1L, 2L, 3L);

    rows = sql("select id, name, pt from " + ident(arcticTable));
    assertContainIdSet(rows, 1L, 2L, 3L, 4L, 5L, 6L);

    rows = sql("select id, name, pt from " + ident(hiveTable));
    assertContainIdSet(rows, 1L, 2L, 3L);


    sql("set spark.sql.arctic.delegate.enabled = false");
    rows = sql("select id, name, pt from " + ident(arcticTable));
    assertContainIdSet(rows, 1L, 2L, 3L);
    sql("set spark.sql.arctic.delegate.enabled = true");
  }


  @Test
  public void testIcebergCallStatement() {
    sql("create table " + ident(icebergTable) + " (id bigint, name string, pt string) " +
        "using iceberg partitioned by (truncate(pt, 4))");
    sql("insert overwrite " + ident(icebergTable) + " select * from tmp");
    sql("insert into " + ident(icebergTable) + " select * from tmp");
    Dataset<Row> ds = sql("select id, name, pt from " + ident(icebergTable));
    Assertions.assertEquals(6L, ds.count());

    Dataset<Row> history = spark().read()
        .format("iceberg")
        .load(database + "." + icebergTable + ".history");
    Row first = history.filter("parent_id is null ")
        .limit(1).collectAsList().get(0);
    long snapshotId = first.getAs("snapshot_id");
    System.out.println("snapshotId: " + snapshotId);

    sql("call system.rollback_to_snapshot( '" + ident(icebergTable) + "' , " + snapshotId + ")");
    ds = sql("select id, name, pt from " + ident(icebergTable));
    assertContainIdSet(ds, 1L, 2L, 3L);
  }

  @Test
  public void testCTAS() {
    sql("create table " + ident(icebergTable) + " using iceberg as select * from tmp");
    Dataset<Row> ds = sql("select id, name, pt from tmp");
    assertContainIdSet(ds, 1L, 2L, 3L);


    sql("create table " + ident(arcticTable) + " primary key (id) using arctic as select * from tmp");
    ds = sql("select id, name, pt from " + ident(arcticTable));
    assertContainIdSet(ds, 1L, 2L, 3L);
    catalog().tableExists(TableIdentifier.of(catalog().name(), database, arcticTable));
  }


  private void assertContainIdSet(Dataset<Row> rs, Object... expects) {
    Set<Object> actual = rs.collectAsList()
        .stream()
        .map(r -> r.get(0))
        .collect(Collectors.toSet());
    for (Object id : expects) {
      if (!actual.contains(id)) {
        throw new AssertionError("assert id contain " + id + ", but not found");
      }
    }
  }
}
