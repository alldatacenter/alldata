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
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestKeyedTableDMLInsertOverwriteDynamic extends SparkTestBase {

  private final String database = "db";
  private final String table = "testA";
  private KeyedTable keyedTable;
  private final TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, table);

  private String contextOverwriteMode;

  @Before
  public void before() {
    sql("use " + catalogNameArctic);
    sql("create database if not exists {0}", database);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " ts timestamp , \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( days(ts) ) \n", database, table);

    sql("insert overwrite {0}.{1} values \n" +
        "(1, ''aaa'',  timestamp('' 2022-1-1 09:00:00 '')), \n " +
        "(2, ''bbb'',  timestamp('' 2022-1-2 09:00:00 '')), \n " +
        "(3, ''ccc'',  timestamp('' 2022-1-3 09:00:00 '')) \n ", database, table);
    keyedTable = loadTable(identifier).asKeyedTable();

    writeChange(identifier, ChangeAction.INSERT, Lists.newArrayList(
        newRecord(keyedTable, 4, "ddd", quickDateWithZone(1)),
        newRecord(keyedTable, 5, "eee", quickDateWithZone(2)),
        newRecord(keyedTable, 6, "666", quickDateWithZone(3)),
        newRecord(keyedTable, 1024, "1024", quickDateWithZone(4))
    ));

    sql("select * from {0}.{1} order by id", database, table);

    contextOverwriteMode = spark.conf().get("spark.sql.sources.partitionOverwriteMode");
    System.out.println("spark.sql.sources.partitionOverwriteMode = " + contextOverwriteMode);
    sql("set spark.sql.sources.partitionOverwriteMode = {0}", "DYNAMIC");
  }

  @After
  public void after() {
    sql("drop table {0}.{1}", database, table);
    sql("set spark.sql.sources.partitionOverwriteMode = {0}", contextOverwriteMode);
  }

  @Test
  public void testInsertOverwrite() {
    // insert overwrite by values
    // before 4 partition, [1-3] partition has base && change file, [4] partition has change file
    // expect [1-2] partition replaced, [3-4] partition keep:
    // P[1]=> [7  ]
    // P[2]=> [8,9]
    // P[3]=> [3,6]
    // P[4]=> [1024]
    //
    sql("insert overwrite {0}.{1} values \n" +
        "(7, ''aaa'',  timestamp('' 2022-1-1 09:00:00 '')), \n " +
        "(8, ''bbb'',  timestamp('' 2022-1-2 09:00:00 '')), \n " +
        "(9, ''ccc'',  timestamp('' 2022-1-2 09:00:00 '')) \n ", database, table);

    rows = sql("select id, data, ts from {0}.{1} order by id", database, table);
    Assert.assertEquals(6, rows.size());

    assertContainIdSet(rows, 0, 7, 8, 9, 3, 6, 1024);

    writeChange(identifier, ChangeAction.INSERT, Lists.newArrayList(
        newRecord(keyedTable, 1, "ddd", quickDateWithZone(1)),
        newRecord(keyedTable, 3, "eee", quickDateWithZone(2)),
        newRecord(keyedTable, 5, "666", quickDateWithZone(3)),
        newRecord(keyedTable, 2004, "1024", quickDateWithZone(4))
    ));

    rows = sql("select id, data, ts from {0}.{1} order by id", database, table);
    Assert.assertEquals(10, rows.size());

    assertContainIdSet(rows, 0, 1, 3, 5, 7, 8, 9, 3, 6, 1024, 2004);
  }

  @Test
  public void testInsertOverwriteHasPartitionFunc() {
    sql("create table {0}.{1} ( \n" +
            " id int , \n" +
            " data string , \n " +
            " ts timestamp , \n" +
            " primary key (id) \n" +
            ") using arctic \n" +
            " partitioned by ( months(ts) ) \n", database, "table_months");
    sql("insert overwrite {0}.{1} values \n" +
            "(1, ''aaa'',  timestamp('' 2022-1-1 09:00:00 '')), \n " +
            "(2, ''bbb'',  timestamp('' 2022-2-2 09:00:00 '')), \n " +
            "(3, ''ccc'',  timestamp('' 2022-3-2 09:00:00 '')) \n ", database, "table_months");

    sql("insert overwrite {0}.{1} values \n" +
            "(4, ''aaa'',  timestamp('' 2022-1-1 09:00:00 '')), \n " +
            "(5, ''bbb'',  timestamp('' 2022-2-2 09:00:00 '')), \n " +
            "(6, ''ccc'',  timestamp('' 2022-3-2 09:00:00 '')) \n ", database, "table_months");

    rows = sql("select id, data, ts from {0}.{1} order by id", database, "table_months");
    Assert.assertEquals(3, rows.size());

    assertContainIdSet(rows, 0, 4, 5, 6);

    sql("drop table {0}.{1}", database, "table_months");
  }

  @Test
  public void testInsertOverwriteNoBasePartition() {
    // insert overwrite by values
    // before 4 partition, [1-3] partition has base && change file, [4] partition has change file
    // expect
    // P[1]=> [1,4]
    // P[2]=> [2,5]
    // P[3]=> [3,6]
    // P[4]=> [7，8，9]
    //
    sql("insert overwrite {0}.{1} values \n" +
        "(7, ''aaa'',  timestamp('' 2022-1-4 09:00:00 '')), \n " +
        "(8, ''bbb'',  timestamp('' 2022-1-4 09:00:00 '')), \n " +
        "(9, ''ccc'',  timestamp('' 2022-1-4 09:00:00 '')) \n ", database, table);

    rows = sql("select id, data, ts from {0}.{1} order by id", database, table);
    Assert.assertEquals(9, rows.size());

    assertContainIdSet(rows, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
  }
}
