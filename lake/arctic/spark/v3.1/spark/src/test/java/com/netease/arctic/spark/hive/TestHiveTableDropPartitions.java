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

import com.netease.arctic.spark.SparkTestBase;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.hadoop.fs.FileStatus;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestHiveTableDropPartitions extends SparkTestBase {
  private final String database = "db_hive";

  private final String dropPartitionTable = "drop_partition_table";

  private final TableIdentifier identifier = TableIdentifier.of(catalogNameHive, database, dropPartitionTable);

  @Before
  public void prepare() {
    sql("use " + catalogNameHive);
    sql("create database if not exists " + database);
  }

  @After
  public void cleanUpTable() {
    sql("drop database " + database);
  }

  @Test
  public void testAlterKeyedTableDropPartitions() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts int, primary key(id) " +
        ") using arctic \n" +
        " partitioned by (ts, name) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, dropPartitionTable);
    sql("insert overwrite {0}.{1} "+
        " values (1, ''aaa'', 1 ) , " +
        "(4, ''bbb'', 2), " +
        "(5, ''ccc'', 3) ", database, dropPartitionTable);

    sql("insert into {0}.{1} "+
        " values (2, ''aaa'', 1 ) , " +
        "(3, ''bbb'', 2), " +
        "(6, ''ccc'', 3) ", database, dropPartitionTable);
    rows = sql("select * from {0}.{1}", database,dropPartitionTable);
    Assert.assertEquals(6, rows.size());
    Assert.assertEquals(3, hms.getClient().listPartitions(
        database,
        dropPartitionTable,
        (short) -1).size());
    KeyedTable keyedTable = loadTable(identifier).asKeyedTable();
    List<FileStatus> exceptList = keyedTable.changeTable().io().list(keyedTable.changeLocation());

    sql("alter table {0}.{1} drop if exists partition (ts=1, name=''aaa'')", database, dropPartitionTable);
    sql("select * from {0}.{1}", database,dropPartitionTable);
    Assert.assertEquals(4, rows.size());
    Assert.assertEquals(2, hms.getClient().listPartitions(
        database,
        dropPartitionTable,
        (short) -1).size());
    Assert.assertEquals(exceptList.size(), keyedTable.changeTable().io().list(keyedTable.changeLocation()).size());
    sql("drop table {0}.{1}", database, dropPartitionTable);
  }

  @Test
  public void testAlterUnKeyedTableDropPartitions() throws TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts int" +
        ") using arctic \n" +
        " partitioned by (ts, name) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, dropPartitionTable);
    sql("insert overwrite {0}.{1} "+
        " values (1, ''aaa'', 1 ) , " +
        "(4, ''bbb'', 2), " +
        "(5, ''ccc'', 3) ", database, dropPartitionTable);

    sql("insert into {0}.{1} "+
        " values (2, ''aaa'', 1 ) , " +
        "(3, ''bbb'', 2), " +
        "(6, ''ccc'', 3) ", database, dropPartitionTable);
    rows = sql("select * from {0}.{1}", database,dropPartitionTable);
    Assert.assertEquals(6, rows.size());
    Assert.assertEquals(3, hms.getClient().listPartitions(
        database,
        dropPartitionTable,
        (short) -1).size());
    UnkeyedTable unkeyedTable = loadTable(identifier).asUnkeyedTable();
    List<FileStatus> exceptList = unkeyedTable.io().list(unkeyedTable.location());

    sql("alter table {0}.{1} drop if exists partition (ts=1, name=''aaa'')", database, dropPartitionTable);
    sql("select * from {0}.{1}", database,dropPartitionTable);
    Assert.assertEquals(4, rows.size());
    Assert.assertEquals(2, hms.getClient().listPartitions(
        database,
        dropPartitionTable,
        (short) -1).size());
    Assert.assertEquals(exceptList.size(), unkeyedTable.io().list(unkeyedTable.location()).size());
    sql("drop table {0}.{1}", database, dropPartitionTable);
  }

  @Test
  public void testAlterNotArcticTableDropPartitions() throws TException {
    sql("use spark_catalog");
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " name string , \n " +
        " ts int" +
        ") STORED AS parquet \n" +
        " partitioned by (ts) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, dropPartitionTable);
    sql("insert overwrite {0}.{1} "+
        " values (1, ''aaa'', 1 ) , " +
        "(4, ''bbb'', 2), " +
        "(5, ''ccc'', 3) ", database, dropPartitionTable);

    sql("insert into {0}.{1} "+
        " values (2, ''aaa'', 1 ) , " +
        "(3, ''bbb'', 2), " +
        "(6, ''ccc'', 3) ", database, dropPartitionTable);
    rows = sql("select * from {0}.{1}", database,dropPartitionTable);
    Assert.assertEquals(6, rows.size());
    Assert.assertEquals(3, hms.getClient().listPartitions(
        database,
        dropPartitionTable,
        (short) -1).size());

    sql("alter table {0}.{1} drop if exists partition (ts=1)", database, dropPartitionTable);
    sql("select * from {0}.{1}", database,dropPartitionTable);
    Assert.assertEquals(4, rows.size());
    Assert.assertEquals(2, hms.getClient().listPartitions(
        database,
        dropPartitionTable,
        (short) -1).size());
    sql("drop table {0}.{1}", database, dropPartitionTable);
  }
}
