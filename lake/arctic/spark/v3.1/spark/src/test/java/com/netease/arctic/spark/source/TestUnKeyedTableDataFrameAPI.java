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

package com.netease.arctic.spark.source;

import com.netease.arctic.spark.SparkTestBase;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.Column;
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
  final String tablePath = catalogNameArctic + "." + database + "." + table;
  final TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, table);
  final Schema schema = new Schema(
      Types.NestedField.of(1, false, "id", Types.IntegerType.get()),
      Types.NestedField.of(2, false, "data", Types.StringType.get()),
      Types.NestedField.of(3, false, "ts", Types.TimestampType.withZone())
  );

  Dataset<Row> df;

  @Before
  public void setUp() {
    sql("use " + catalogNameArctic);
    sql("create database if not exists {0} ", database);
  }

  @After
  public void cleanUp() {
    sql("use " + catalogNameArctic);
    sql("drop table  if exists {0}.{1}", database, table);
    sql("drop database {0}", database);
  }

  @Test
  public void testV2ApiUnkeyedTable() throws Exception {
    StructType structType = SparkSchemaUtil.convert(schema);

    // create test
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(1, "aaa", quickTs(1)),
            RowFactory.create(2, "bbb", quickTs(2)),
            RowFactory.create(3, "ccc", quickTs(3))
        ), structType
    );
    df.writeTo(tablePath)
        .partitionedBy(new Column("data"))
        .create();
    assertTableExist(identifier);
    df = spark.read()
        .table(tablePath);
    Assert.assertEquals(3, df.count());

    // append test
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(4, "aaa", quickTs(1)),
            RowFactory.create(5, "bbb", quickTs(2)),
            RowFactory.create(6, "ccc", quickTs(3))
        ), structType
    );
    df.writeTo(tablePath)
        .append();
    df = spark.read()
        .table(tablePath);
    Assert.assertEquals(6, df.count());

    // replace test
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(7, "aaa", quickTs(1)),
            RowFactory.create(8, "bbb", quickTs(2)),
            RowFactory.create(9, "ccc", quickTs(3))
        ), structType
    );
    df.writeTo(tablePath)
        .partitionedBy(new Column("data"))
        .replace();
    df = spark.read()
        .table(tablePath);
    Assert.assertEquals(3, df.count());
    df.show();

    // overwritePartition test
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(10, "ccc", quickTs(3)),
            RowFactory.create(11, "ddd", quickTs(4)),
            RowFactory.create(12, "eee", quickTs(5))
        ), structType
    );
    df.writeTo(tablePath)
        .overwritePartitions();
    df = spark.read()
        .table(tablePath);
    Assert.assertEquals(5, df.count());
  }

  @Test
  public void testV1ApiUnkeyedTable() {
    StructType structType = SparkSchemaUtil.convert(schema);

    // test create
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(1, "aaa", quickTs(1)),
            RowFactory.create(2, "bbb", quickTs(2)),
            RowFactory.create(3, "ccc", quickTs(3))
        ), structType
    );
    df.write().format("arctic")
        .partitionBy("data")
        .save(tablePath);
    assertTableExist(identifier);
    df = spark.read().format("arctic").load(tablePath);
    Assert.assertEquals(3, df.count());

    // test overwrite dynamic
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(4, "aaa", quickTs(1)),
            RowFactory.create(5, "aaa", quickTs(2)),
            RowFactory.create(6, "aaa", quickTs(3))
        ), structType
    );
    df.write().format("arctic")
        .partitionBy("data")
        .option("overwrite-mode", "dynamic")
        .mode(SaveMode.Overwrite)
        .save(tablePath);
    df = spark.read().format("arctic").load(tablePath);
    Assert.assertEquals(5, df.count());
  }

  @Test
  public void testV2ApiKeyedTable() throws Exception {
    sql("use " + catalogNameArctic);
    sql("create table {0}.{1} (" +
        " id int, data string, ts timestamp, primary key (id) \n" +
        ") using arctic partitioned by (days(ts)) ", database, table);

    // test overwrite partitions
    StructType structType = SparkSchemaUtil.convert(schema);
    df = spark.createDataFrame(
        Lists.newArrayList(
            RowFactory.create(1, "aaa", quickTs(1)),
            RowFactory.create(2, "bbb", quickTs(2)),
            RowFactory.create(3, "ccc", quickTs(3))
        ), structType
    );
    df.writeTo(tablePath).overwritePartitions();

    df = spark.read()
        .table(tablePath);
    Assert.assertEquals(3, df.count());
  }


}
