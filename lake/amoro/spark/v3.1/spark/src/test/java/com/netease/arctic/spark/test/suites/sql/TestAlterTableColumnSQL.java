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
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.AnalysisException;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@EnableCatalogSelect
@EnableCatalogSelect.SelectCatalog(byTableFormat = true)
public class TestAlterTableColumnSQL extends SparkTableTestBase {

  public static Stream<Arguments> testAddColumn() {
    return Stream.of(
        Arguments.of(TableFormat.MIXED_HIVE, "",
            Types.StructType.of(
                Types.NestedField.optional(1, "id", Types.LongType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(4, "point", Types.StructType.of(
                    Types.NestedField.required(5, "x", Types.DoubleType.get()),
                    Types.NestedField.required(6, "y", Types.DoubleType.get())
                )),
                Types.NestedField.optional(3, "ts", Types.StringType.get()))),
        Arguments.of(TableFormat.MIXED_ICEBERG, "",
            Types.StructType.of(
                Types.NestedField.optional(1, "id", Types.LongType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.StringType.get()),
                Types.NestedField.optional(4, "point", Types.StructType.of(
                    Types.NestedField.required(5, "x", Types.DoubleType.get()),
                    Types.NestedField.required(6, "y", Types.DoubleType.get())
                )))),
        Arguments.of(TableFormat.MIXED_HIVE, ", PRIMARY KEY(id)",
            Types.StructType.of(
                Types.NestedField.required(1, "id", Types.LongType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(4, "point", Types.StructType.of(
                    Types.NestedField.required(5, "x", Types.DoubleType.get()),
                    Types.NestedField.required(6, "y", Types.DoubleType.get())
                )),
                Types.NestedField.optional(3, "ts", Types.StringType.get()))),
        Arguments.of(TableFormat.MIXED_ICEBERG, ", PRIMARY KEY(id)",
            Types.StructType.of(
                Types.NestedField.required(1, "id", Types.LongType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.StringType.get()),
                Types.NestedField.optional(4, "point", Types.StructType.of(
                    Types.NestedField.required(5, "x", Types.DoubleType.get()),
                    Types.NestedField.required(6, "y", Types.DoubleType.get())
                ))))
    );
  }

  @DisplayName("Test `add column`")
  @ParameterizedTest
  @MethodSource()
  public void testAddColumn(TableFormat format, String primaryKeyDDL, Types.StructType expectedSchema) {
    String sqlText = "CREATE TABLE " + target() + " ( \n" +
        "id bigint, data string, ts string " + primaryKeyDDL + " ) using " +
        provider(format) + " PARTITIONED BY (ts)";
    sql(sqlText);
    sql("ALTER TABLE " +
        target().database + "." + target().table +
        " ADD COLUMN point struct<x: double NOT NULL, y: double NOT NULL>");

    Assertions.assertEquals(expectedSchema, loadTable().schema().asStruct(), "Schema should match expected");
  }

  public static Stream<Arguments> testDropColumn() {
    return Stream.of(
        Arguments.of(TableFormat.MIXED_ICEBERG, ", PRIMARY KEY(id)",
            Types.StructType.of(
                Types.NestedField.required(1, "id", Types.LongType.get()),
                Types.NestedField.optional(3, "ts", Types.StringType.get()))),
        Arguments.of(TableFormat.MIXED_ICEBERG, "",
            Types.StructType.of(
                Types.NestedField.optional(1, "id", Types.LongType.get()),
                Types.NestedField.optional(3, "ts", Types.StringType.get())))
    );
  }

  @DisplayName("Test `drop column`")
  @ParameterizedTest
  @MethodSource()
  public void testDropColumn(TableFormat format, String primaryKeyDDL, Types.StructType expectedSchema) {

    String sqlText = "CREATE TABLE " + target() + " ( \n" +
        "id bigint, data string, ts string " + primaryKeyDDL + " ) using " +
        provider(format) + " PARTITIONED BY (ts)";
    sql(sqlText);
    sql("ALTER TABLE " +
        target().database + "." + target().table +
        " DROP COLUMN data");

    Assertions.assertEquals(expectedSchema, loadTable().schema().asStruct(), "Schema should match expected");
  }

  /**
   * TODO: are arguments could be simplify?
   * TODO:  Test coverage is not enough.
   */
  public static Stream<Arguments> testAlterColumn() {
    return Stream.of(
        Arguments.of(" id COMMENT 'Record id'", "",
            Types.StructType.of(
                Types.NestedField.optional(1, "id", Types.LongType.get(), "Record id"),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()),
                Types.NestedField.optional(4, "count", Types.IntegerType.get()))),
        Arguments.of(" count TYPE bigint", "",
            Types.StructType.of(
                Types.NestedField.optional(1, "id", Types.LongType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()),
                Types.NestedField.optional(4, "count", Types.LongType.get()))),
        Arguments.of(" data DROP NOT NULL", "",
            Types.StructType.of(
                Types.NestedField.optional(1, "id", Types.LongType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()),
                Types.NestedField.optional(4, "count", Types.IntegerType.get()))),
        Arguments.of(" data SET NOT NULL", "", null),
        Arguments.of(" count AFTER id", "",
            Types.StructType.of(
                Types.NestedField.optional(1, "id", Types.LongType.get()),
                Types.NestedField.optional(4, "count", Types.IntegerType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()))),
        Arguments.of(" id COMMENT 'Record id'", ", PRIMARY KEY(id)",
            Types.StructType.of(
                Types.NestedField.required(1, "id", Types.LongType.get(), "Record id"),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()),
                Types.NestedField.optional(4, "count", Types.IntegerType.get()))),
        Arguments.of(" count TYPE bigint", ", PRIMARY KEY(id)",
            Types.StructType.of(
                Types.NestedField.required(1, "id", Types.LongType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()),
                Types.NestedField.optional(4, "count", Types.LongType.get()))),
        Arguments.of(" data DROP NOT NULL", ", PRIMARY KEY(id)",
            Types.StructType.of(
                Types.NestedField.required(1, "id", Types.LongType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.TimestampType.withZone()),
                Types.NestedField.optional(4, "count", Types.IntegerType.get()))),
        Arguments.of(" data SET NOT NULL", ", PRIMARY KEY(id)", null),
        Arguments.of(" count AFTER id", ", PRIMARY KEY(id)",
            Types.StructType.of(
                Types.NestedField.required(1, "id", Types.LongType.get()),
                Types.NestedField.optional(4, "count", Types.IntegerType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "ts", Types.TimestampType.withZone())))
    );
  }

  @DisplayName("Test `alter column`")
  @ParameterizedTest
  @MethodSource()
  @EnableCatalogSelect.SelectCatalog(use = INTERNAL_CATALOG)
  public void testAlterColumn(String alterText, String primaryKeyDDL, Types.StructType expectedSchema) {
    String sqlText = "CREATE TABLE " + target() + " ( \n" +
        "id bigint, data string, ts timestamp, count int " + primaryKeyDDL + " ) using " +
        provider(TableFormat.MIXED_ICEBERG) + " PARTITIONED BY (ts)";
    sql(sqlText);
    if (expectedSchema != null) {
      sql("ALTER TABLE " +
          target().database + "." + target().table +
          " ALTER COLUMN " + alterText);
      Assertions.assertEquals(expectedSchema, loadTable().schema().asStruct(), "Schema should match expected");
    } else {
      Assert.assertThrows(
          AnalysisException.class,
          () -> sql("ALTER TABLE " +
              target().database + "." + target().table +
              " ALTER COLUMN " + alterText));
    }
  }

  public static Stream<Arguments> testAlterTableProperties() {
    return Stream.of(
        Arguments.of(TableFormat.MIXED_ICEBERG, "", " SET TBLPROPERTIES ('test.props' = 'val')", "val"),
        Arguments.of(TableFormat.MIXED_HIVE, "", " SET TBLPROPERTIES ('test.props' = 'val')", "val"),
        Arguments.of(TableFormat.MIXED_ICEBERG, "", " UNSET TBLPROPERTIES ('test.props')", null),
        Arguments.of(TableFormat.MIXED_HIVE, "", " UNSET TBLPROPERTIES ('test.props')", null),
        Arguments.of(TableFormat.MIXED_ICEBERG,
            ", PRIMARY KEY(id)", " SET TBLPROPERTIES ('test.props' = 'val')", "val"),
        Arguments.of(TableFormat.MIXED_HIVE,
            ", PRIMARY KEY(id)", " SET TBLPROPERTIES ('test.props' = 'val')", "val"),
        Arguments.of(TableFormat.MIXED_ICEBERG,
            ", PRIMARY KEY(id)", " UNSET TBLPROPERTIES ('test.props')", null),
        Arguments.of(TableFormat.MIXED_HIVE,
            ", PRIMARY KEY(id)", " UNSET TBLPROPERTIES ('test.props')", null)
    );
  }


  @DisplayName("Test `alter table properties`")
  @ParameterizedTest
  @MethodSource()
  public void testAlterTableProperties(
      TableFormat format, String primaryKeyDDL, String alterText, String expectedProperties) {
    String sqlText = "CREATE TABLE " + target() + " ( \n" +
        "id bigint, data string, ts string " + primaryKeyDDL + " ) using " +
        provider(format) + " PARTITIONED BY (ts)";
    sql(sqlText);
    sql("ALTER TABLE " +
        target().database + "." + target().table + alterText);
    Assertions.assertEquals(expectedProperties, loadTable().properties().get("test.props"));
  }

}
