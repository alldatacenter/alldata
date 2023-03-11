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
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestKeyedHiveInsertOverwriteDynamic extends SparkTestBase {
  private final String database = "db";
  private final String table = "testA";
  private final String insertTable = "testInsert";

  private String contextOverwriteMode;

  protected String createTableInsert = "create table {0}.{1}( \n" +
          " id int, \n" +
          " data string, \n" +
          " dt string, primary key(id)) \n" +
          " using arctic ";

  @Before
  public void before() throws IOException {
    sql("use " + catalogNameHive);
    sql("create database if not exists {0}", database);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " dt string , \n" +
        " primary key (id) \n" +
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
        "(6, ''ccc'',  ''2021-1-2'') \n ", database, table);

    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 4, 5, 6, 3);

    List<Partition> partitions = hms.getClient().listPartitions(
        database,
        table,
        (short) -1);
    Assert.assertEquals(3, partitions.size());
    sql("use spark_catalog");
    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 4, 5, 6, 3);
  }

  @Test
  public void testInsertOverwriteDuplicateData() {
    sql("create table {0}.{1}( \n" +
            " id int, \n" +
            " name string, \n" +
            " data string, primary key(id, name))\n" +
            " using arctic partitioned by (data) " , database, "testPks");

    // insert overwrite values
    Assert.assertThrows(UnsupportedOperationException.class,
            () -> sql("insert overwrite " + database + "." + "testPks" +
                    " values (1, 1.1, 'abcd' ) , " +
                    "(1, 1.1, 'bbcd'), " +
                    "(3, 1.3, 'cbcd') "));

    sql(createTableInsert, database, insertTable);
    sql("insert into " + database + "." + table +
            " values (1, 'aaa',  '2021-1-1' ) , " +
            "(2, 'bbb',  '2021-1-2'), " +
            "(3, 'ccc',  '2021-1-3') ");

    // insert overwrite select + group by has no duplicated data
    sql("insert overwrite " + database + "." + insertTable + " select * from {0}.{1} group by id, data, dt",
            database, table);
    rows = sql("select * from " + database + "." + insertTable);
    Assert.assertEquals(3, rows.size());

    // insert overwrite select + group by has duplicated data
    sql("insert into " + database + "." + table +
            " values (1, 'aaaa', 'abcd' )");

    Assert.assertThrows(UnsupportedOperationException.class,
            () -> sql("insert overwrite " + database + "." + insertTable +
                            " select * from {0}.{1} group by id, data, dt",
                    database, table));

    sql("drop table " + database + "." + "testPks");
    sql("drop table " + database + "." + insertTable);
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
}
