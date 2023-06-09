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

package com.netease.arctic.spark.hive;

import com.google.common.collect.Lists;
import com.netease.arctic.spark.SparkTestBase;
import com.netease.arctic.table.ArcticTable;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.iceberg.Schema;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.StructType;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestUnkeyedHiveInsertOverwriteDynamic extends SparkTestBase {

  private final String database = "db";
  private final String table = "testA";

  private String contextOverwriteMode;

  @Before
  public void before() throws IOException {
    sql("use " + catalogNameHive);
    sql("create database if not exists {0}", database);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " dt string \n" +
        ") using arctic \n" +
        " partitioned by ( dt ) \n", database, table);

    sql("insert overwrite {0}.{1} values \n" +
        "(1, ''aaa'',  ''2021-1-1''), \n " +
        "(2, ''bbb'',  ''2021-1-2''), \n " +
        "(3, ''ccc'',  ''2021-1-3'') \n ", database, table);


    sql("select * from {0}.{1} order by id", database, table);

    contextOverwriteMode = spark.conf().get("spark.sql.sources.partitionOverwriteMode");
    System.out.println("spark.sql.sources.partitionOverwriteMode = " + contextOverwriteMode);
    sql("set spark.sql.sources.partitionOverwriteMode = {0}", "DYNAMIC");
  }

  @After
  public void after() {
    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, table);
    sql("set spark.sql.sources.partitionOverwriteMode = {0}", contextOverwriteMode);
  }

  @Test
  public void testInsertOverwrite() throws TException {
    sql("insert overwrite {0}.{1} values \n" +
        "(4, ''aaa'',  ''2021-1-1''), \n " +
        "(5, ''bbb'',  ''2021-1-2''), \n " +
        "(6, ''ccc'',  ''2021-1-2''), \n " +
        "(7, ''ccc'',  ''2021-1-2''), \n " +
        "(8, ''ccc'',  ''2021-1-2''), \n " +
        "(9, ''ccc'',  ''2021-1-2''), \n " +
        "(10, ''ccc'',  ''2021-1-2''), \n " +
        "(11, ''ccc'',  ''2021-1-2''), \n " +
        "(12, ''ccc'',  ''2021-1-2''), \n " +
        "(13, ''ccc'',  ''2021-1-2'') \n " , database, table);

    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(11, rows.size());
    assertContainIdSet(rows, 0, 3,4, 5, 6, 7, 8, 9, 10, 11, 12, 13);

    List<Partition> partitions = hms.getClient().listPartitions(
        database,
        table,
        (short) -1);
    Assert.assertEquals(3, partitions.size());
    sql("use spark_catalog");
    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(11, rows.size());
    assertContainIdSet(rows, 0, 3,4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
  }

  @Test
  public void testInsertOverwriteNoBasePartition() throws TException {
    sql("insert overwrite {0}.{1} values \n" +
        "(4, ''aaa'',  ''2021-1-4''), \n " +
        "(5, ''bbb'',  ''2021-1-4''), \n " +
        "(6, ''ccc'',  ''2021-1-4'') \n ", database, table);

    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(6, rows.size());
    assertContainIdSet(rows, 0, 1, 2, 3, 4, 5, 6);

    List<Partition> partitions = hms.getClient().listPartitions(
        database,
        table,
        (short) -1);
    Assert.assertEquals(4, partitions.size());
    sql("use spark_catalog");
    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(6, rows.size());
    assertContainIdSet(rows, 0, 1, 2, 3, 4, 5, 6);
  }


  @Test
  public void testInsertOverwriteFromView() {
    ArcticTable t = loadTable(catalogNameHive, database, table);
    List<Row> sources = Lists.newArrayList(
        RowFactory.create(4, "aaa", "2021-1-2"),
        RowFactory.create(5, "aaa", "2021-1-3"),
        RowFactory.create(6, "aaa", "2021-1-2"),
        RowFactory.create(7, "aaa", "2021-1-3"),
        RowFactory.create(8, "aaa", "2021-1-2"),
        RowFactory.create(9, "aaa", "2021-1-3"),
        RowFactory.create(10, "aaa", "2021-1-2"),
        RowFactory.create(11, "aaa", "2021-1-3"),
        RowFactory.create( 12, "aaa", "2021-1-2")
    );
    StructType schema = SparkSchemaUtil.convert(new Schema(
        Types.NestedField.of(1, false, "id", Types.IntegerType.get()),
        Types.NestedField.of(2, false, "data", Types.StringType.get()),
        Types.NestedField.of(3, false, "dt", Types.StringType.get())
    ));
    Dataset<Row> row = spark.createDataFrame(sources, schema);
    row = row.repartition(1);
    row.registerTempTable("view");

    sql("insert overwrite {0}.{1} select * from view", database, table);
    rows = sql("select * from {0}.{1} order by id", database, table);
    Assert.assertEquals(10, rows.size());

  }

}
