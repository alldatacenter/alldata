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

import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.types.StructType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestUnKeyedTableDataFrameAPI extends SparkTestBase {

  final String database = "ddd";
  final String table = "tbl";
  final String tablePath = database + "." + table;
  final TableIdentifier identifier = TableIdentifier.of(catalogName, database, table);
  final Schema schema = new Schema(
      Types.NestedField.of(1, false, "id", Types.IntegerType.get()),
      Types.NestedField.of(2, false, "data", Types.StringType.get()),
      Types.NestedField.of(3, false, "dt", Types.StringType.get())
  );

  Dataset<Row> df;

  @Before
  public void setUp() {
    sql("create database if not exists {0} ", database);
  }

  @After
  public void cleanUp() {
    sql("drop table  if exists {0}.{1}", database, table);
    sql("drop database {0}", database);
  }

  @Test
  public void testDFApiUnkeyedTableOverwrite() throws Exception {
    sql("create table {0}.{1} (" +
        " id int, data string, dt string \n" +
        ") using arctic partitioned by (dt) ", database, table);

    // table exsits
    StructType structType = SparkSchemaUtil.convert(schema);
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(1, "aaa", "2020-1-1"),
            RowFactory.create(2, "bbb", "2020-1-2"),
            RowFactory.create(3, "ccc", "2020-1-3")
        ), structType
    );
    df.write().format("arctic")
        .option("write-mode", "overwrite-dynamic")
        .mode(SaveMode.Overwrite)
        .save(tablePath);

    df = spark.read().format("arctic").load(tablePath);
    df.show();
    Assert.assertEquals(3, df.count());

    // test overwrite dynamic
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(4, "aaa", "2020-1-3"),
            RowFactory.create(5, "aaa", "2020-1-4"),
            RowFactory.create(6, "aaa", "2020-1-5")
        ), structType
    );
    df.write().format("arctic")
        .option("write-mode", "overwrite-dynamic")
        .mode(SaveMode.Overwrite)
        .save(tablePath);
    df = spark.read().format("arctic").load(tablePath);
    df.show();
    Assert.assertEquals(5, df.count());
  }

  @Test
  public void testDFApiUnkeyedTableCreateAndOverwrite() throws Exception {

    // table not exists
    StructType structType = SparkSchemaUtil.convert(schema);
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(1, "aaa", "2020-1-1"),
            RowFactory.create(2, "bbb", "2020-1-2"),
            RowFactory.create(3, "ccc", "2020-1-3")
        ), structType
    );
    // table not exists, create table
    df.write().format("arctic").option("partition.keys", "dt")
        .option("write-mode", "overwrite-dynamic")
        .mode(SaveMode.Overwrite).save(tablePath);

    df = spark.read().format("arctic").load(tablePath);
    df.show();
    Assert.assertEquals(3, df.count());

    // test overwrite dynamic
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(4, "aaa", "2020-1-3"),
            RowFactory.create(5, "aaa", "2020-1-4"),
            RowFactory.create(6, "aaa", "2020-1-5")
        ), structType
    );
    df.write().format("arctic")
        .option("write-mode", "overwrite-dynamic")
        .mode(SaveMode.Overwrite)
        .save(tablePath);
    df = spark.read().format("arctic").load(tablePath);
    df.show();
    Assert.assertEquals(5, df.count());

  }

}
