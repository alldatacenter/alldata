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

package com.netease.arctic.spark.test.suites.sql;

import com.netease.arctic.spark.test.SparkTableTestBase;
import com.netease.arctic.spark.test.extensions.EnableCatalogSelect;
import com.netease.arctic.spark.test.helper.TestTable;
import org.apache.iceberg.types.Types;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@EnableCatalogSelect
@EnableCatalogSelect.SelectCatalog(byTableFormat = true)
public class TestArcticCommandSQL extends SparkTableTestBase {

  public static Stream<Arguments> testMigrate() {
    Types.NestedField[] fields = {
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.required(2, "data", Types.StringType.get()),
        Types.NestedField.required(3, "pt", Types.StringType.get())
    };

    return Stream.of(
        Arguments.arguments(fields, new String[0], SESSION_CATALOG),
        Arguments.arguments(fields, new String[] {"pt"}, SESSION_CATALOG),
        Arguments.arguments(fields, new String[0], INTERNAL_CATALOG),
        Arguments.arguments(fields, new String[] {"pt"}, INTERNAL_CATALOG)
    );
  }

  @EnableCatalogSelect.SelectCatalog(use = SESSION_CATALOG)
  @ParameterizedTest
  @MethodSource
  public void testMigrate(Types.NestedField[] fields, String[] pt, String targetCatalog) {

    TestTable source = TestTable.format(MIXED_HIVE, fields).pt(pt).build();
    createHiveSource(source.hiveSchema, source.hivePartitions);
    sql("insert overwrite " + source() + " values " +
        " ( 1, 'aaa', '0001' ),  " +
        " ( 2, 'bbb', '0002' ) ");

    sql("CREATE DATABASE IF NOT EXISTS " + targetCatalog + "." + target().database);
    sql("migrate " + source() + " to arctic " + targetCatalog + "." + target());
    sql("DROP TABLE IF EXISTS " + targetCatalog + "." + target());
  }
}
