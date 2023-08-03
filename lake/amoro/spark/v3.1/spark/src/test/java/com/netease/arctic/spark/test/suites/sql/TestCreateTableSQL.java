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
import com.netease.arctic.spark.SparkSQLProperties;
import com.netease.arctic.spark.test.Asserts;
import com.netease.arctic.spark.test.SparkTableTestBase;
import com.netease.arctic.spark.test.extensions.EnableCatalogSelect;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;


@EnableCatalogSelect
@EnableCatalogSelect.SelectCatalog(byTableFormat = true)
public class TestCreateTableSQL extends SparkTableTestBase {


  public static Stream<Arguments> testTimestampHandleInCreateTable() {
    return Stream.of(
        Arguments.arguments(TableFormat.MIXED_HIVE, true, Types.TimestampType.withoutZone()),
        Arguments.arguments(TableFormat.MIXED_HIVE, false, Types.TimestampType.withoutZone()),
        Arguments.arguments(TableFormat.MIXED_ICEBERG, true, Types.TimestampType.withoutZone()),
        Arguments.arguments(TableFormat.MIXED_ICEBERG, false, Types.TimestampType.withZone())
    );
  }

  @DisplayName("Test `use-timestamp-without-zone-in-new-tables`")
  @ParameterizedTest
  @MethodSource
  public void testTimestampHandleInCreateTable(
      TableFormat format, boolean usingTimestampWithoutZone, Types.TimestampType expectType) {
    spark().conf().set(
        SparkSQLProperties.USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES,
        usingTimestampWithoutZone);

    String sqlText = "CREATE TABLE " + target() + "(\n" +
        "id INT, \n" +
        "ts TIMESTAMP \n) using  " + provider(format);
    sql(sqlText);
    ArcticTable actual = loadTable();
    Type actualType = actual.schema().findField("ts").type();
    Assertions.assertEquals(expectType, actualType);
  }


  public static Stream<Arguments> testPrimaryKeyNotNullConstraint() {
    return Stream.of(
        Arguments.of(TableFormat.MIXED_HIVE, "INT", "", false),
        Arguments.of(TableFormat.MIXED_HIVE, "INT NOT NULL", "", true),
        Arguments.of(TableFormat.MIXED_HIVE, "INT", ", PRIMARY KEY(id)", true),
        Arguments.of(TableFormat.MIXED_HIVE, "INT NOT NULL", ", PRIMARY KEY(id)", true),
        Arguments.of(TableFormat.MIXED_ICEBERG, "INT", "", false),
        Arguments.of(TableFormat.MIXED_ICEBERG, "INT NOT NULL", "", true),
        Arguments.of(TableFormat.MIXED_ICEBERG, "INT", ", PRIMARY KEY(id)", true),
        Arguments.of(TableFormat.MIXED_ICEBERG, "INT NOT NULL", ", PRIMARY KEY(id)", true)
    );
  }

  @DisplayName("Test auto add `NOT NULL` for primary key")
  @ParameterizedTest
  @MethodSource
  public void testPrimaryKeyNotNullConstraint(
      TableFormat format, String idFieldTypeDDL, String primaryKeyDDL, boolean expectRequired
  ) {
    String sqlText = "CREATE TABLE " + target() + "(\n" +
        "id " + idFieldTypeDDL + ",\n" +
        "DATA string " + primaryKeyDDL + "\n" +
        ") using " + provider(format);

    sql(sqlText);
    Schema actualSchema = loadTable().schema();
    Types.NestedField idField = actualSchema.findField("id");
    Assertions.assertEquals(idField.isRequired(), expectRequired);
  }


  public static Stream<Arguments> testPrimaryKeySpecExist() {
    return Stream.of(
        Arguments.of(TableFormat.MIXED_HIVE, ", PRIMARY KEY(id)", true),
        Arguments.of(TableFormat.MIXED_HIVE, "", false),
        Arguments.of(TableFormat.MIXED_ICEBERG, ", PRIMARY KEY(id)", true),
        Arguments.of(TableFormat.MIXED_ICEBERG, "", false)
    );
  }

  @DisplayName("Test PRIMARY KEY spec exists.")
  @ParameterizedTest
  @MethodSource()
  public void testPrimaryKeySpecExist(
      TableFormat format, String primaryKeyDDL, boolean expectKeyedTable
  ) {
    String sqlText = "CREATE TABLE " + target() + " ( \n" +
        "id int, data string " + primaryKeyDDL + " ) using " + provider(format);
    sql(sqlText);
    ArcticTable actualTable = loadTable();
    Assertions.assertEquals(actualTable.isKeyedTable(), expectKeyedTable);
    if (expectKeyedTable) {
      PrimaryKeySpec keySpec = actualTable.asKeyedTable().primaryKeySpec();
      Assertions.assertEquals(1, keySpec.fields().size());
      Assertions.assertTrue(keySpec.fieldNames().contains("id"));
    }
  }


  static Schema schema = new Schema(
      Types.NestedField.required(0, "id", Types.IntegerType.get()),
      Types.NestedField.required(1, "data", Types.StringType.get()),
      Types.NestedField.required(2, "ts", Types.TimestampType.withoutZone()),
      Types.NestedField.required(3, "pt", Types.StringType.get())
  );

  public static Stream<Arguments> testPartitionSpec() {

    return Stream.of(
        Arguments.of(TableFormat.MIXED_HIVE, "", PartitionSpec.unpartitioned()),
        Arguments.of(TableFormat.MIXED_HIVE, "PARTITIONED BY (pt)",
            PartitionSpec.builderFor(schema).identity("pt").build()),

        Arguments.of(TableFormat.MIXED_ICEBERG, "PARTITIONED BY (years(ts))",
            PartitionSpec.builderFor(schema).year("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PARTITIONED BY (months(ts))",
            PartitionSpec.builderFor(schema).month("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PARTITIONED BY (days(ts))",
            PartitionSpec.builderFor(schema).day("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PARTITIONED BY (date(ts))",
            PartitionSpec.builderFor(schema).day("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PARTITIONED BY (hours(ts))",
            PartitionSpec.builderFor(schema).hour("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PARTITIONED BY (date_hour(ts))",
            PartitionSpec.builderFor(schema).hour("ts").build()),

        Arguments.of(TableFormat.MIXED_ICEBERG, "PARTITIONED BY (bucket(4, id))",
            PartitionSpec.builderFor(schema).bucket("id", 4).build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PARTITIONED BY (truncate(10, data))",
            PartitionSpec.builderFor(schema).truncate("data", 10).build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PARTITIONED BY (truncate(10, id))",
            PartitionSpec.builderFor(schema).truncate("id", 10).build())
    );
  }

  @DisplayName("Test PartitionSpec is right")
  @ParameterizedTest
  @MethodSource()
  public void testPartitionSpec(
      TableFormat format, String partitionDDL, PartitionSpec expectSpec
  ) {
    String sqlText = "CREATE TABLE " + target() + " ( \n" +
        "id int, " +
        "data string, " +
        "ts timestamp, " +
        "pt string, " +
        " PRIMARY KEY(id) ) using  " + provider(format) + " " + partitionDDL;

    sql(sqlText);

    ArcticTable actualTable = loadTable();
    Asserts.assertPartition(expectSpec, actualTable.spec());
    if (TableFormat.MIXED_HIVE == format) {
      Table hiveTable = loadHiveTable();
      Asserts.assertHivePartition(expectSpec, hiveTable.getPartitionKeys());
    }
  }


  public static Stream<Arguments> testSchemaAndProperties() {
    String structDDL = "id INT,\n" +
        "data string NOT NULL,\n" +
        "point struct<x: double NOT NULL, y: double NOT NULL>,\n" +
        "maps map<string, string>,\n" +
        "arrays array<string>,\n" +
        "pt string ";
    Types.NestedField id = Types.NestedField.optional(1, "id", Types.IntegerType.get());
    Types.NestedField data = Types.NestedField.required(2, "data", Types.StringType.get());
    Types.NestedField point = Types.NestedField.optional(3, "point", Types.StructType.of(
        Types.NestedField.required(4, "x", Types.DoubleType.get()),
        Types.NestedField.required(5, "y", Types.DoubleType.get())
    ));
    Types.NestedField map = Types.NestedField.optional(6, "maps", Types.MapType.ofOptional(
        7, 8, Types.StringType.get(), Types.StringType.get()));
    Types.NestedField array = Types.NestedField.optional(
        9, "arrays", Types.ListType.ofOptional(10, Types.StringType.get()));
    Types.NestedField pt = Types.NestedField.optional(11, "pt", Types.StringType.get());


    return Stream.of(
        Arguments.of(
            TableFormat.MIXED_ICEBERG, structDDL,
            "TBLPROPERTIES('key'='value1', 'catalog'='INTERNAL')",
            new Schema(Lists.newArrayList(id, data, point, map, array, pt)),
            ImmutableMap.of("key", "value1", "catalog", "INTERNAL")),
        Arguments.of(
            TableFormat.MIXED_ICEBERG, structDDL + ", PRIMARY KEY(id)",
            "TBLPROPERTIES('key'='value1', 'catalog'='INTERNAL')",
            new Schema(Lists.newArrayList(id.asRequired(), data, point, map, array, pt)),
            ImmutableMap.of("key", "value1", "catalog", "INTERNAL")),

        Arguments.of(
            TableFormat.MIXED_HIVE, structDDL,
            "tblproperties('key'='value1', 'catalog'='hive')",
            new Schema(Lists.newArrayList(id, data, point, map, array, pt)),
            ImmutableMap.of("key", "value1", "catalog", "hive")),
        Arguments.of(
            TableFormat.MIXED_HIVE, structDDL + ", PRIMARY KEY(id)",
            "tblproperties('key'='value1', 'catalog'='hive')",
            new Schema(Lists.newArrayList(id.asRequired(), data, point, map, array, pt)),
            ImmutableMap.of("key", "value1", "catalog", "hive"))
    );
  }

  @DisplayName("Test primary key, schema and properties")
  @ParameterizedTest
  @MethodSource
  public void testSchemaAndProperties(
      TableFormat format, String structDDL, String propertiesDDL,
      Schema expectSchema, Map<String, String> expectProperties
  ) {
    spark().conf().set(
        SparkSQLProperties.USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES, false
    );
    String sqlText = "CREATE TABLE " + target() + "(" +
        structDDL + ") using  " + provider(format) + " " + propertiesDDL;
    sql(sqlText);

    ArcticTable tbl = loadTable();

    Asserts.assertType(expectSchema.asStruct(), tbl.schema().asStruct());
    Asserts.assertHashMapContainExpect(expectProperties, tbl.properties());
    if (TableFormat.MIXED_HIVE == format) {
      Table hiveTable = loadHiveTable();
      Asserts.assertHiveColumns(expectSchema, PartitionSpec.unpartitioned(), hiveTable.getSd().getCols());
    }
  }

}
