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

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.spark.SparkTestBase;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestHiveTableMergeOnRead extends SparkTestBase {
  private final String database = "db";
  private final String table = "testA";
  private KeyedTable keyedTable;

  private UnkeyedTable unkeyedTable;
  private final TableIdentifier identifier = TableIdentifier.of(catalogNameHive, database, table);

  @Before
  public void before() {
    sql("use " + catalogNameHive);
    sql("create database if not exists {0}", database);
  }

  @After
  public void after() {
    sql("use " + catalogNameHive);
    sql("drop table {0}.{1}", database, table);
    sql("drop database if exists " + database);
  }

  @Test
  public void testMergeOnReadKeyedPartition() throws IOException, TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " dt string , \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( dt ) \n", database, table);
    keyedTable = loadTable(identifier).asKeyedTable();

    writeHive(keyedTable, BaseLocationKind.INSTANT, Lists.newArrayList(
        newRecord(keyedTable, 1, "aaa", "2021-1-1"),
        newRecord(keyedTable, 2, "bbb", "2021-1-1"),
        newRecord(keyedTable, 3, "ccc", "2021-1-1"),
        newRecord(keyedTable, 4, "ddd", "2021-1-2"),
        newRecord(keyedTable, 5, "eee", "2021-1-2"),
        newRecord(keyedTable, 6, "fff", "2021-1-2")
    ));
    writeHive(keyedTable, HiveLocationKind.INSTANT, Lists.newArrayList(
        newRecord(keyedTable, 7, "aaa_hive", "2021-1-1"),
        newRecord(keyedTable, 8, "bbb_hive", "2021-1-1"),
        newRecord(keyedTable, 9, "ccc_hive", "2021-1-2"),
        newRecord(keyedTable, 10, "ddd_hive", "2021-1-2")
    ));
    writeChange(identifier, ChangeAction.DELETE, Lists.newArrayList(
        newRecord(keyedTable, 1, "aaa", "2021-1-1")
    ));

    sql("select * from {0}.{1} order by id", database, table);
    Assert.assertEquals(9, rows.size());
    assertContainIdSet(rows, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    List<Partition> partitions = hms.getClient().listPartitions(
        database,
        table,
        (short) -1);
    Assert.assertEquals(2, partitions.size());
    sql("use spark_catalog");
    sql("select * from {0}.{1}", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 7, 8, 9, 10);
  }

  @Test
  public void testMergeOnReadUnkeyedPartiton() throws IOException, TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " dt string \n" +
        ") using arctic \n" +
        " partitioned by ( dt ) \n", database, table);
    unkeyedTable = loadTable(identifier).asUnkeyedTable();
    writeHive(unkeyedTable, BaseLocationKind.INSTANT, Lists.newArrayList(
        newRecord(unkeyedTable.schema(), 1, "aaa", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 2, "bbb", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 3, "ccc", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 4, "ddd", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 5, "eee", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 6, "fff", "2021-1-2")
    ));
    writeHive(unkeyedTable, HiveLocationKind.INSTANT, Lists.newArrayList(
        newRecord(unkeyedTable.schema(), 7, "aaa_hive", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 8, "bbb_hive", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 9, "ccc_hive", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 10, "ddd_hive", "2021-1-2")
    ));
    sql("select * from {0}.{1} order by id", database, table);
    Assert.assertEquals(10, rows.size());
    assertContainIdSet(rows, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    List<Partition> partitions = hms.getClient().listPartitions(
        database,
        table,
        (short) -1);
    Assert.assertEquals(2, partitions.size());
    sql("use spark_catalog");
    sql("select * from {0}.{1}", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 7, 8, 9, 10);
  }

  @Test
  public void testMergeOnReadKeyedUnPartition() throws IOException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " dt string , \n" +
        " primary key (id) \n" +
        ") using arctic \n", database, table);
    keyedTable = loadTable(identifier).asKeyedTable();

    List<DataFile> dataFiles = writeHive(keyedTable, BaseLocationKind.INSTANT, Lists.newArrayList(
        newRecord(keyedTable, 1, "aaa", "2021-1-1"),
        newRecord(keyedTable, 2, "bbb", "2021-1-1"),
        newRecord(keyedTable, 3, "ccc", "2021-1-1"),
        newRecord(keyedTable, 4, "ddd", "2021-1-2"),
        newRecord(keyedTable, 5, "eee", "2021-1-2"),
        newRecord(keyedTable, 6, "fff", "2021-1-2")
    ));

    writeHive(keyedTable, HiveLocationKind.INSTANT, Lists.newArrayList(
        newRecord(keyedTable, 7, "aaa_hive", "2021-1-1"),
        newRecord(keyedTable, 8, "bbb_hive", "2021-1-1"),
        newRecord(keyedTable, 9, "ccc_hive", "2021-1-2"),
        newRecord(keyedTable, 10, "ddd_hive", "2021-1-2")
    ));
    writeChange(identifier, ChangeAction.DELETE, Lists.newArrayList(
        newRecord(keyedTable, 1, "aaa", "2021-1-1")
    ));
    sql("select * from {0}.{1} order by id", database, table);
    Assert.assertEquals(9, rows.size());
    assertContainIdSet(rows, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    sql("use spark_catalog");
    sql("select * from {0}.{1}", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 7, 8, 9, 10);
  }

  @Test
  public void testMergeOnReadUnkeyedUnpartition() throws IOException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " dt string \n" +
        ") using arctic \n", database, table);
    unkeyedTable = loadTable(identifier).asUnkeyedTable();
    writeHive(unkeyedTable, BaseLocationKind.INSTANT, Lists.newArrayList(
        newRecord(unkeyedTable.schema(), 1, "aaa", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 2, "bbb", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 3, "ccc", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 4, "ddd", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 5, "eee", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 6, "fff", "2021-1-2")
    ));
    writeHive(unkeyedTable, HiveLocationKind.INSTANT, Lists.newArrayList(
        newRecord(unkeyedTable.schema(), 7, "aaa_hive", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 8, "bbb_hive", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 9, "ccc_hive", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 10, "ddd_hive", "2021-1-2")
    ));
    sql("select * from {0}.{1} order by id", database, table);
    Assert.assertEquals(10, rows.size());
    assertContainIdSet(rows, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    sql("use spark_catalog");
    sql("select * from {0}.{1}", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 7, 8, 9, 10);
  }

}
