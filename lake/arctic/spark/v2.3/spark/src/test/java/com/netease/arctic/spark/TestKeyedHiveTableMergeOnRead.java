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

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestKeyedHiveTableMergeOnRead extends SparkTestBase {
  private final String database = "db";
  private final String table = "test_mora";
  private KeyedTable keyedTable;
  private final TableIdentifier identifier = TableIdentifier.of(catalogName, database, table);

  private final List<Object[]> files = Lists.newArrayList(
      new Object[]{1, "aaa", new Double(12345.123), new Float(12.11), "2021-1-1"},
      new Object[]{2, "bbb", new Double(12345.123), new Float(12.11), "2021-1-1"},
      new Object[]{3, "ccc", new Double(12345.123), new Float(12.11), "2021-1-1"},
      new Object[]{4, "ddd", new Double(12345.123), new Float(12.11), "2021-1-2"},
      new Object[]{5, "eee", new Double(12345.123), new Float(12.11), "2021-1-2"},
      new Object[]{6, "fff", new Double(12345.123), new Float(12.11), "2021-1-2"},
      new Object[]{7, "aaa_hive", new Double(12345.123), new Float(12.11), "2021-1-1"},
      new Object[]{8, "bbb_hive", new Double(12345.123), new Float(12.11), "2021-1-1"},
      new Object[]{9, "ccc_hive", new Double(12345.123), new Float(12.11), "2021-1-2"},
      new Object[]{10, "ddd_hive", new Double(12345.123), new Float(12.11), "2021-1-2"}
  );

  @Before
  public void before() {
    sql("create database if not exists {0}", database);
  }

  @After
  public void after() {
    sql("drop table {0}.{1}", database, table);
    sql("drop database if exists " + database);
  }

  @Test
  public void testMergeOnReadKeyedPartition() throws IOException, TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " salary double , \n" +
        " money float , \n" +
        " dt string , \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( dt ) \n", database, table);
    keyedTable = loadTable(identifier).asKeyedTable();

    writeHive(keyedTable, BaseLocationKind.INSTANT, Lists.newArrayList(
        newRecord(keyedTable, files.get(0)),
        newRecord(keyedTable, files.get(1)),
        newRecord(keyedTable, files.get(2)),
        newRecord(keyedTable, files.get(3)),
        newRecord(keyedTable, files.get(4)),
        newRecord(keyedTable, files.get(5))
    ));
    writeHive(keyedTable, HiveLocationKind.INSTANT, Lists.newArrayList(
        newRecord(keyedTable, files.get(6)),
        newRecord(keyedTable, files.get(7)),
        newRecord(keyedTable, files.get(8)),
        newRecord(keyedTable, files.get(9))
    ));
    writeChange(identifier, ChangeAction.DELETE, Lists.newArrayList(
        newRecord(keyedTable, files.get(0))
    ));

    rows = sql("select * from {0}.{1} order by id", database, table);

    assertEquals("Should have rows matching the expected rows",
        files.subList(1, files.size()),
        rows);
    Assert.assertEquals(9, rows.size());
    assertContainIdSet(rows, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    List<Partition> partitions = hms.getClient().listPartitions(
        database,
        table,
        (short) -1);
    Assert.assertEquals(2, partitions.size());
    //disable arctic
    sql("set spark.sql.arctic.delegate.enabled = false");
    sql("select * from {0}.{1} order by id", database, table);
    assertEquals("Should have rows matching the expected rows",
        files.subList(6, files.size()),
        rows);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 7, 8, 9, 10);
    sql("set spark.sql.arctic.delegate.enabled = true");
  }


  @Test
  public void testMergeOnReadKeyedUnPartition() throws IOException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " salary double , \n" +
        " money float , \n" +
        " dt string , \n" +
        " primary key (id) \n" +
        ") using arctic \n", database, table);
    keyedTable = loadTable(identifier).asKeyedTable();

    writeHive(keyedTable, BaseLocationKind.INSTANT, Lists.newArrayList(
        newRecord(keyedTable, files.get(0)),
        newRecord(keyedTable, files.get(1)),
        newRecord(keyedTable, files.get(2)),
        newRecord(keyedTable, files.get(3)),
        newRecord(keyedTable, files.get(4)),
        newRecord(keyedTable, files.get(5))
    ));
    writeHive(keyedTable, HiveLocationKind.INSTANT, Lists.newArrayList(
        newRecord(keyedTable, files.get(6)),
        newRecord(keyedTable, files.get(7)),
        newRecord(keyedTable, files.get(8)),
        newRecord(keyedTable, files.get(9))
    ));
    writeChange(identifier, ChangeAction.DELETE, Lists.newArrayList(
        newRecord(keyedTable, files.get(0))
    ));
    rows = sql("select * from {0}.{1} order by id", database, table);
    assertEquals("Should have rows matching the expected rows",
        files.subList(1, files.size()),
        rows);
    Assert.assertEquals(9, rows.size());
    assertContainIdSet(rows, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    //disable arctic
    sql("set spark.sql.arctic.delegate.enabled = false");
    sql("select * from {0}.{1} order by id", database, table);
    assertEquals("Should have rows matching the expected rows",
        files.subList(6, files.size()),
        rows);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 7, 8, 9, 10);
    sql("set spark.sql.arctic.delegate.enabled = true");
  }

}
