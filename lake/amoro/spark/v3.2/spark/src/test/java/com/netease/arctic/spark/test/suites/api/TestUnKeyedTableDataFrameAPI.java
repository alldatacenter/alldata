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

package com.netease.arctic.spark.test.suites.api;

import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.spark.test.SparkTableTestBase;
import com.netease.arctic.spark.test.extensions.EnableCatalogSelect;
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
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;


@EnableCatalogSelect
@EnableCatalogSelect.SelectCatalog(byTableFormat = true)
public class TestUnKeyedTableDataFrameAPI extends SparkTableTestBase {

  final Schema schema = new Schema(
      Types.NestedField.of(1, false, "id", Types.IntegerType.get()),
      Types.NestedField.of(2, false, "data", Types.StringType.get()),
      Types.NestedField.of(3, false, "day", Types.StringType.get())
  );
  Dataset<Row> df;

  public static Stream<Arguments> testV2ApiUnkeyedTable() {
    return Stream.of(
        Arguments.of(TableFormat.MIXED_HIVE),
        Arguments.of(TableFormat.MIXED_ICEBERG)
    );
  }

  @DisplayName("Test `test V2 Api for UnkeyedTable`")
  @ParameterizedTest
  @MethodSource
  public void testV2ApiUnkeyedTable(TableFormat format) throws Exception {
    String tablePath = target().catalog + "." + target().database + "." + target().table;
    StructType structType = SparkSchemaUtil.convert(schema);

    // create test
    df = spark().createDataFrame(
        Lists.newArrayList(
            RowFactory.create(1, "aaa", "aaa"),
            RowFactory.create(2, "bbb", "bbb"),
            RowFactory.create(3, "ccc", "ccc")
        ), structType
    );
    df.writeTo(tablePath)
        .partitionedBy(new Column("day"))
        .create();
    df = spark().read()
        .table(tablePath);
    Assert.assertEquals(3, df.count());

    // append test
    df = spark().createDataFrame(
        Lists.newArrayList(
            RowFactory.create(4, "aaa", "aaa"),
            RowFactory.create(5, "bbb", "bbb"),
            RowFactory.create(6, "ccc", "ccc")
        ), structType
    );
    df.writeTo(tablePath)
        .append();
    df = spark().read()
        .table(tablePath);
    Assertions.assertEquals(6, df.count());

    // replace test
    df = spark().createDataFrame(
        Lists.newArrayList(
            RowFactory.create(7, "aaa", "aaa"),
            RowFactory.create(8, "bbb", "bbb"),
            RowFactory.create(9, "ccc", "ccc")
        ), structType
    );
    df.writeTo(tablePath)
        .partitionedBy(new Column("day"))
        .replace();
    df = spark().read()
        .table(tablePath);
    Assertions.assertEquals(3, df.count());
    df.show();

    // overwritePartition test
    df = spark().createDataFrame(
        Lists.newArrayList(
            RowFactory.create(10, "ccc", "ccc"),
            RowFactory.create(11, "ddd", "ddd"),
            RowFactory.create(12, "eee", "eee")
        ), structType
    );
    df.writeTo(tablePath)
        .overwritePartitions();
    df = spark().read()
        .table(tablePath);
    Assertions.assertEquals(5, df.count());
  }

  public static Stream<Arguments> testV1ApiUnkeyedTable() {
    return Stream.of(
        Arguments.of(TableFormat.MIXED_HIVE),
        Arguments.of(TableFormat.MIXED_ICEBERG)
    );
  }

  @DisplayName("Test `test V1 Api for UnkeyedTable`")
  @ParameterizedTest
  @MethodSource
  public void testV1ApiUnkeyedTable(TableFormat format) {
    String tablePath = target().catalog + "." + target().database + "." + target().table;
    StructType structType = SparkSchemaUtil.convert(schema);

    // test create
    df = spark().createDataFrame(
        Lists.newArrayList(
            RowFactory.create(1, "aaa", "aaa"),
            RowFactory.create(2, "bbb", "bbb"),
            RowFactory.create(3, "ccc", "ccc")
        ), structType
    );
    df.write().format("arctic")
        .partitionBy("day")
        .save(tablePath);
    df = spark().read().format("arctic").load(tablePath);
    Assertions.assertEquals(3, df.count());

    // test overwrite dynamic
    df = spark().createDataFrame(
        Lists.newArrayList(
            RowFactory.create(4, "aaa", "ccc"),
            RowFactory.create(5, "bbb", "ccc"),
            RowFactory.create(6, "ccc", "ccc")
        ), structType
    );
    df.write().format("arctic")
        .partitionBy("day")
        .option("overwrite-mode", "dynamic")
        .mode(SaveMode.Overwrite)
        .save(tablePath);
    df = spark().read().format("arctic").load(tablePath);
    Assertions.assertEquals(5, df.count());
  }

}
