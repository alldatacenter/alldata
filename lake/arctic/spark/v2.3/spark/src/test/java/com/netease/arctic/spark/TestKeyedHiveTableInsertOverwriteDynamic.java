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

package com.netease.arctic.spark;


import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestKeyedHiveTableInsertOverwriteDynamic extends SparkTestBase {
  private final String database = "db";
  private final String table = "testa";

  private String contextOverwriteMode;

  @Before
  public void before() throws IOException {
    sql("create database if not exists {0}", database);
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " dt string , \n" +
        " primary key (id) \n" +
        ") using arctic \n" +
        " partitioned by ( dt ) \n", database, table);

    sql("insert overwrite table {0}.{1} values \n" +
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
    sql("drop table {0}.{1}", database, table);
    sql("drop database if exists {0} cascade", database);
    sql("set spark.sql.sources.partitionOverwriteMode = {0}", contextOverwriteMode);
  }

  @Test
  public void testInsertOverwrite() throws TException {
    sql("insert overwrite table {0}.{1} values \n" +
        "(4, ''aaa'',  ''2021-1-1''), \n " +
        "(5, ''bbb'',  ''2021-1-2''), \n " +
        "(6, ''ccc'',  ''2021-1-1'') \n ", database, table);
    //read by arctic
    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 4, 5, 6, 3);
    //read by hive
    sql("set spark.sql.arctic.delegate.enabled = false");
    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 4, 5, 6, 3);
    sql("set spark.sql.arctic.delegate.enabled = true");

    List<Partition> partitions = hms.getClient().listPartitions(
        database,
        table,
        (short) -1);
    Assert.assertEquals(3, partitions.size());
  }

  @Test
  public void testInsertOverwriteNoBasePartition() throws TException {
    sql("insert overwrite table {0}.{1} values \n" +
        "(4, ''aaa'',  ''2021-1-4''), \n " +
        "(5, ''bbb'',  ''2021-1-4''), \n " +
        "(6, ''ccc'',  ''2021-1-4'') \n ", database, table);
    //read by arctic
    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(6, rows.size());
    assertContainIdSet(rows, 0, 1, 2, 3, 4, 5, 6);
    //read by hive
    sql("set spark.sql.arctic.delegate.enabled = false");
    rows = sql("select id, data, dt from {0}.{1} order by id", database, table);
    Assert.assertEquals(6, rows.size());
    assertContainIdSet(rows, 0, 1, 2, 3, 4, 5, 6);
    sql("set spark.sql.arctic.delegate.enabled = true");

    List<Partition> partitions = hms.getClient().listPartitions(
        database,
        table,
        (short) -1);
    Assert.assertEquals(4, partitions.size());
  }
}
