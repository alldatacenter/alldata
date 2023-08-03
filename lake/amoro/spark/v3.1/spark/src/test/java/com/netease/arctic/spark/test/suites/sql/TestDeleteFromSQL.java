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

import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.spark.test.SparkTableTestBase;
import com.netease.arctic.spark.test.extensions.EnableCatalogSelect;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@EnableCatalogSelect
@EnableCatalogSelect.SelectCatalog(byTableFormat = true)
public class TestDeleteFromSQL extends SparkTableTestBase {
  public static Stream<Arguments> testDelete() {
    return Stream.of(
        Arguments.of(TableFormat.MIXED_HIVE, ", PRIMARY KEY(id)", " where id = 3"),
        Arguments.of(TableFormat.MIXED_HIVE, ", PRIMARY KEY(id)", ""),
        Arguments.of(TableFormat.MIXED_HIVE, "", ""),
        Arguments.of(TableFormat.MIXED_HIVE, "", " where id = 3"),
        Arguments.of(TableFormat.MIXED_ICEBERG, ", PRIMARY KEY(id)", " where id = 3"),
        Arguments.of(TableFormat.MIXED_ICEBERG, ", PRIMARY KEY(id)", ""),
        Arguments.of(TableFormat.MIXED_ICEBERG, "", ""),
        Arguments.of(TableFormat.MIXED_ICEBERG, "", " where id = 3")
    );
  }

  @DisplayName("Test `test delete`")
  @ParameterizedTest
  @MethodSource
  public void testDelete(TableFormat format, String primaryKeyDDL, String filter) {
    String sqlText = "CREATE TABLE " + target() + " ( \n" +
        "id int, data string " + primaryKeyDDL + " ) using " + provider(format);
    sql(sqlText);
    sql("insert into " +
        target().database + "." + target().table +
        " values (1, 'a'), (2, 'b'), (3, 'c')");
    sql("delete from " + target().database + "." + target().table + filter);
    Dataset<Row> sql = sql("select * from " +
        target().database + "." + target().table);
    if (filter.isEmpty()) {
      Assertions.assertEquals(0, sql.collectAsList().size());
    } else {
      Assertions.assertEquals(2, sql.collectAsList().size());
    }
  }
}
