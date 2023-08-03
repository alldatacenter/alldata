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
import com.netease.arctic.spark.test.helper.DataComparator;
import com.netease.arctic.spark.test.helper.TableFiles;
import com.netease.arctic.spark.test.helper.TestTableHelper;
import com.netease.arctic.spark.test.helper.TestTables;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@EnableCatalogSelect
@EnableCatalogSelect.SelectCatalog(byTableFormat = true)
public class TestCreateTableAsSelect extends SparkTableTestBase {


  public static final Schema simpleSourceSchema = TestTables.MixedIceberg.NoPK_PT.schema;
  public static final List<Record> simpleSourceData = TestTables.MixedIceberg.PK_PT.newDateGen().records(10);


  public static Stream<Arguments> testTimestampZoneHandle() {
    return Stream.of(
        Arguments.of(TableFormat.MIXED_ICEBERG,
            "PRIMARY KEY(id, pt)", true, Types.TimestampType.withoutZone()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "", false, Types.TimestampType.withZone()),
        Arguments.of(TableFormat.MIXED_HIVE,
            "PRIMARY KEY(id, pt)", true, Types.TimestampType.withoutZone()),
        Arguments.of(TableFormat.MIXED_HIVE, "", false, Types.TimestampType.withoutZone())
    );
  }

  @ParameterizedTest
  @MethodSource
  public void testTimestampZoneHandle(
      TableFormat format, String primaryKeyDDL, boolean timestampWithoutZone, Types.TimestampType expectType) {
    createViewSource(simpleSourceSchema, simpleSourceData);
    spark().conf().set(
        SparkSQLProperties.USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES, timestampWithoutZone
    );

    String sqlText = "CREATE TABLE " + target() + " " + primaryKeyDDL +
        " USING " + provider(format) + " AS SELECT * FROM " + source();
    sql(sqlText);

    ArcticTable table = loadTable();
    Types.NestedField f = table.schema().findField("ts");
    Asserts.assertType(expectType, f.type());
  }


  private static PartitionSpec.Builder ptBuilder() {
    return PartitionSpec.builderFor(simpleSourceSchema);
  }

  public static Stream<Arguments> testSchemaAndData() {
    PrimaryKeySpec keyIdPtSpec = PrimaryKeySpec.builderFor(simpleSourceSchema)
        .addColumn("id")
        .addColumn("pt")
        .build();
    PrimaryKeySpec keyIdSpec = PrimaryKeySpec.builderFor(simpleSourceSchema)
        .addColumn("id")
        .build();


    return Stream.of(
        Arguments.of(TableFormat.MIXED_HIVE, "PRIMARY KEY(id, pt)", "PARTITIONED BY(pt)",
            keyIdPtSpec, ptBuilder().identity("pt").build()),
        Arguments.of(TableFormat.MIXED_HIVE, "PRIMARY KEY(id, pt)", "",
            keyIdPtSpec, PartitionSpec.unpartitioned()),
        Arguments.of(TableFormat.MIXED_HIVE, "", "PARTITIONED BY(pt)",
            PrimaryKeySpec.noPrimaryKey(),
            ptBuilder().identity("pt").build()),
        Arguments.of(TableFormat.MIXED_HIVE, "", "",
            PrimaryKeySpec.noPrimaryKey(), PartitionSpec.unpartitioned()),

        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id, pt)", "",
            keyIdPtSpec, PartitionSpec.unpartitioned()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "", "PARTITIONED BY(pt,id)",
            PrimaryKeySpec.noPrimaryKey(),
            ptBuilder().identity("pt").identity("id").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id)", "PARTITIONED BY(years(ts))",
            keyIdSpec, ptBuilder().year("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id)", "PARTITIONED BY(months(ts))",
            keyIdSpec, ptBuilder().month("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id)", "PARTITIONED BY(days(ts))",
            keyIdSpec, ptBuilder().day("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id)", "PARTITIONED BY(date(ts))",
            keyIdSpec, ptBuilder().day("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id)", "PARTITIONED BY(hours(ts))",
            keyIdSpec, ptBuilder().hour("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id)", "PARTITIONED BY(date_hour(ts))",
            keyIdSpec, ptBuilder().hour("ts").build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id)", "PARTITIONED BY(bucket(10, id))",
            keyIdSpec, ptBuilder().bucket("id", 10).build()),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id)", "PARTITIONED BY(truncate(10, data))",
            keyIdSpec, ptBuilder().truncate("data", 10).build())
    );
  }


  @ParameterizedTest
  @MethodSource
  public void testSchemaAndData(
      TableFormat format, String primaryKeyDDL, String partitionDDL,
      PrimaryKeySpec keySpec, PartitionSpec ptSpec
  ) {
    spark().conf().set("spark.sql.session.timeZone", "UTC");
    createViewSource(simpleSourceSchema, simpleSourceData);

    spark().conf().set(
        SparkSQLProperties.USE_TIMESTAMP_WITHOUT_TIME_ZONE_IN_NEW_TABLES, true
    );

    String sqlText = "CREATE TABLE " + target() + " " + primaryKeyDDL +
        " USING " + provider(format) + " " + partitionDDL +
        " AS SELECT * FROM " + source();
    sql(sqlText);

    Schema expectSchema = TestTableHelper.toSchemaWithPrimaryKey(simpleSourceSchema, keySpec);
    expectSchema = TestTableHelper.timestampToWithoutZone(expectSchema);

    ArcticTable table = loadTable();
    Asserts.assertPartition(ptSpec, table.spec());
    Assertions.assertEquals(keySpec.primaryKeyExisted(), table.isKeyedTable());
    if (table.isKeyedTable()) {
      Asserts.assertPrimaryKey(keySpec, table.asKeyedTable().primaryKeySpec());
    }
    Asserts.assertType(expectSchema.asStruct(), table.schema().asStruct());
    TableFiles files = TestTableHelper.files(table);
    Asserts.assertAllFilesInBaseStore(files);

    if (TableFormat.MIXED_HIVE == format) {
      Table hiveTable = loadHiveTable();
      Asserts.assertHiveColumns(expectSchema, ptSpec, hiveTable.getSd().getCols());
      Asserts.assertHivePartition(ptSpec, hiveTable.getPartitionKeys());
      Asserts.assertAllFilesInHiveLocation(files, hiveTable.getSd().getLocation());
    }


    List<Record> records = TestTableHelper.tableRecords(table);
    DataComparator.build(simpleSourceData, records)
        .ignoreOrder(Comparator.comparing(r -> (Integer) r.get(0)))
        .assertRecordsEqual();
  }


  public static Stream<Arguments> testSourceDuplicateCheck() {
    List<Record> duplicateSource = Lists.newArrayList(simpleSourceData);
    duplicateSource.add(simpleSourceData.get(0));

    return Stream.of(
        Arguments.of(TableFormat.MIXED_ICEBERG, simpleSourceData, "PRIMARY KEY(id, pt)", false),
        Arguments.of(TableFormat.MIXED_ICEBERG, simpleSourceData, "", false),
        Arguments.of(TableFormat.MIXED_ICEBERG, duplicateSource, "", false),
        Arguments.of(TableFormat.MIXED_ICEBERG, duplicateSource, "PRIMARY KEY(id, pt)", true),

        Arguments.of(TableFormat.MIXED_HIVE, simpleSourceData, "PRIMARY KEY(id, pt)", false),
        Arguments.of(TableFormat.MIXED_HIVE, simpleSourceData, "", false),
        Arguments.of(TableFormat.MIXED_HIVE, duplicateSource, "", false),
        Arguments.of(TableFormat.MIXED_HIVE, duplicateSource, "PRIMARY KEY(id, pt)", true)
    );
  }

  @ParameterizedTest(name = "{index} {0} {2} {3}")
  @MethodSource
  public void testSourceDuplicateCheck(
      TableFormat format, List<Record> sourceData, String primaryKeyDDL, boolean duplicateCheckFailed
  ) {
    spark().conf().set(SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE, "true");
    createViewSource(simpleSourceSchema, sourceData);
    String sqlText = "CREATE TABLE " + target() + " " + primaryKeyDDL +
        " USING " + provider(format) + " " +
        " AS SELECT * FROM " + source();

    boolean exceptionCatched = false;
    try {
      sql(sqlText);
    } catch (Exception e) {
      exceptionCatched = true;
    }

    Assertions.assertEquals(duplicateCheckFailed, exceptionCatched);
  }


  public static Stream<Arguments> testAdditionProperties() {
    String propertiesDDL = "TBLPROPERTIES('k1'='v1', 'k2'='v2')";
    Map<String, String> expectProperties = ImmutableMap.of("k1", "v1", "k2", "v2");
    Map<String, String> emptyProperties = Collections.emptyMap();
    return Stream.of(
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id, pt)", "", emptyProperties),
        Arguments.of(TableFormat.MIXED_ICEBERG, "PRIMARY KEY(id, pt)", propertiesDDL, expectProperties),
        Arguments.of(TableFormat.MIXED_ICEBERG, "", propertiesDDL, expectProperties),

        Arguments.of(TableFormat.MIXED_HIVE, "PRIMARY KEY(id, pt)", "", emptyProperties),
        Arguments.of(TableFormat.MIXED_HIVE, "PRIMARY KEY(id, pt)", propertiesDDL, expectProperties),
        Arguments.of(TableFormat.MIXED_HIVE, "", propertiesDDL, expectProperties)
    );
  }


  @ParameterizedTest
  @MethodSource
  public void testAdditionProperties(
      TableFormat format, String primaryKeyDDL, String propertiesDDL, Map<String, String> expectProperties
  ) {
    createViewSource(simpleSourceSchema, simpleSourceData);
    String sqlText = "CREATE TABLE " + target() + " " + primaryKeyDDL +
        " USING " + provider(format) + " PARTITIONED BY (pt) " + propertiesDDL +
        " AS SELECT * FROM " + source();
    sql(sqlText);
    ArcticTable table = loadTable();
    Map<String, String> tableProperties = table.properties();
    Asserts.assertHashMapContainExpect(expectProperties, tableProperties);
  }


  // TODO: test optimize write for ctas.

}
