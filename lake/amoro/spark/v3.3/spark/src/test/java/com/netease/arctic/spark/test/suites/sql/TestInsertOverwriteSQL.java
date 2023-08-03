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
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.spark.SparkSQLProperties;
import com.netease.arctic.spark.test.Asserts;
import com.netease.arctic.spark.test.SparkTableTestBase;
import com.netease.arctic.spark.test.extensions.EnableCatalogSelect;
import com.netease.arctic.spark.test.helper.DataComparator;
import com.netease.arctic.spark.test.helper.ExpectResultHelper;
import com.netease.arctic.spark.test.helper.RecordGenerator;
import com.netease.arctic.spark.test.helper.TableFiles;
import com.netease.arctic.spark.test.helper.TestTableHelper;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 1. dynamic insert overwrite
 * 2. static insert overwrite
 * 3. for un-partitioned table
 * 3. duplicate check for insert overwrite
 * 4. optimize write is work for insert overwrite
 */
@EnableCatalogSelect
@EnableCatalogSelect.SelectCatalog(byTableFormat = true)
public class TestInsertOverwriteSQL extends SparkTableTestBase {

  static final String OVERWRITE_MODE_KEY = "spark.sql.sources.partitionOverwriteMode";
  static final String DYNAMIC = "DYNAMIC";
  static final String STATIC = "STATIC";

  static final Schema schema = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "data", Types.StringType.get()),
      Types.NestedField.required(3, "pt", Types.StringType.get())
  );

  static final PrimaryKeySpec idPrimaryKeySpec = PrimaryKeySpec.builderFor(schema)
      .addColumn("id").build();

  static final PartitionSpec ptSpec = PartitionSpec.builderFor(schema)
      .identity("pt").build();

  List<Record> base = Lists.newArrayList(
      RecordGenerator.newRecord(schema, 1, "aaa", "AAA"),
      RecordGenerator.newRecord(schema, 2, "bbb", "AAA"),
      RecordGenerator.newRecord(schema, 3, "ccc", "BBB"),
      RecordGenerator.newRecord(schema, 4, "ddd", "BBB"),
      RecordGenerator.newRecord(schema, 5, "eee", "CCC"),
      RecordGenerator.newRecord(schema, 6, "fff", "CCC")
  );

  List<Record> change = Lists.newArrayList(
      RecordGenerator.newRecord(schema, 7, "ggg", "DDD"),
      RecordGenerator.newRecord(schema, 8, "hhh", "DDD"),
      RecordGenerator.newRecord(schema, 9, "jjj", "AAA"),
      RecordGenerator.newRecord(schema, 10, "kkk", "AAA")
  );

  List<Record> source = Lists.newArrayList(
      RecordGenerator.newRecord(schema, 1, "xxx", "AAA"),
      RecordGenerator.newRecord(schema, 2, "xxx", "AAA"),
      RecordGenerator.newRecord(schema, 11, "xxx", "DDD"),
      RecordGenerator.newRecord(schema, 12, "xxx", "DDD"),
      RecordGenerator.newRecord(schema, 13, "xxx", "EEE"),
      RecordGenerator.newRecord(schema, 14, "xxx", "EEE")
  );

  private ArcticTable table;
  private List<Record> target;
  private List<DataFile> initFiles;

  private void initTargetTable(PrimaryKeySpec keySpec, PartitionSpec ptSpec) {
    table = createTarget(schema, builder ->
        builder.withPartitionSpec(ptSpec)
            .withPrimaryKeySpec(keySpec));
    initFiles = TestTableHelper.writeToBase(table, base);
    target = Lists.newArrayList(base);

    if (keySpec.primaryKeyExisted()) {
      List<DataFile> changeFiles = TestTableHelper.writeToChange(
          table.asKeyedTable(), change, ChangeAction.INSERT);
      initFiles.addAll(changeFiles);
      target.addAll(change);
    }

    createViewSource(schema, source);
  }

  private void assertFileLayout(TableFormat format) {
    TableFiles files = TestTableHelper.files(table);
    Set<String> initFileSet = initFiles.stream().map(f -> f.path().toString()).collect(Collectors.toSet());
    files = files.removeFiles(initFileSet);

    Asserts.assertAllFilesInBaseStore(files);
    if (MIXED_HIVE == format) {
      String hiveLocation = ((SupportHive) table).hiveLocation();
      Asserts.assertAllFilesInHiveLocation(files, hiveLocation);
    }
  }

  @BeforeEach
  void cleanVars() {
    this.table = null;
    this.target = Lists.newArrayList();
    this.initFiles = Lists.newArrayList();
  }

  public static Stream<Arguments> testDynamic() {
    return Stream.of(
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey)
    );
  }

  @DisplayName("TestSQL: INSERT OVERWRITE dynamic mode")
  @ParameterizedTest()
  @MethodSource
  public void testDynamic(
      TableFormat format, PrimaryKeySpec keySpec
  ) {
    spark().conf().set(OVERWRITE_MODE_KEY, DYNAMIC);

    initTargetTable(keySpec, ptSpec);

    sql("INSERT OVERWRITE " + target() + " SELECT * FROM " + source());

    table.refresh();
    List<Record> expects = ExpectResultHelper.dynamicOverwriteResult(target, source, r -> r.getField("pt"));
    List<Record> actual = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, actual)
        .ignoreOrder("id")
        .assertRecordsEqual();

    assertFileLayout(format);
  }

  private static Record setPtValue(Record r, String value) {
    Record record = r.copy();
    record.setField("pt", value);
    return record;
  }

  public static Stream<Arguments> testStatic() {
    Function<Record, Boolean> alwaysTrue = r -> true;
    Function<Record, Boolean> deleteAAA = r -> "AAA".equals(r.getField("pt"));
    Function<Record, Boolean> deleteDDD = r -> "DDD".equals(r.getField("pt"));

    Function<Record, Record> noTrans = Function.identity();
    Function<Record, Record> ptAAA = r -> setPtValue(r, "AAA");
    Function<Record, Record> ptDDD = r -> setPtValue(r, "DDD");

    return Stream.of(
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, "", "*", alwaysTrue, noTrans),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, "PARTITION(pt = 'AAA')", "id, data",
            deleteAAA, ptAAA),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, "", "*", alwaysTrue, noTrans),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, "PARTITION(pt = 'DDD')", "id, data",
            deleteDDD, ptDDD),

        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, "", "*", alwaysTrue, noTrans),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, "PARTITION(pt = 'AAA')", "id, data",
            deleteAAA, ptAAA),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, "", "*", alwaysTrue, noTrans),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, "PARTITION(pt = 'DDD')", "id, data",
            deleteDDD, ptDDD)
    );
  }

  @DisplayName("TestSQL: INSERT OVERWRITE static mode")
  @ParameterizedTest(name = "{index} {0} {1} {2} SELECT {3}")
  @MethodSource
  public void testStatic(
      TableFormat format, PrimaryKeySpec keySpec, String ptFilter, String sourceProject,
      Function<Record, Boolean> deleteFilter, Function<Record, Record> sourceTrans
  ) {
    spark().conf().set(OVERWRITE_MODE_KEY, STATIC);
    initTargetTable(keySpec, ptSpec);

    sql("INSERT OVERWRITE " + target() + " " + ptFilter +
        " SELECT " + sourceProject + " FROM " + source());
    table.refresh();

    List<Record> expects = target.stream()
        .filter(r -> !deleteFilter.apply(r))
        .collect(Collectors.toList());

    source.stream().map(sourceTrans).forEach(expects::add);

    List<Record> actual = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, actual)
        .ignoreOrder("pt", "id")
        .assertRecordsEqual();

    assertFileLayout(format);
  }

  public static Stream<Arguments> testUnPartitioned() {

    return Stream.of(
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, DYNAMIC),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, STATIC),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, DYNAMIC),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, DYNAMIC),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, DYNAMIC),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, STATIC),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, DYNAMIC),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, DYNAMIC)
    );
  }

  @DisplayName("TestSQL: INSERT OVERWRITE un-partitioned")
  @ParameterizedTest(name = "{index} {0} {1} partitionOverwriteMode={2}")
  @MethodSource
  public void testUnPartitioned(
      TableFormat format, PrimaryKeySpec keySpec, String mode
  ) {
    spark().conf().set(OVERWRITE_MODE_KEY, mode);
    initTargetTable(keySpec, PartitionSpec.unpartitioned());

    sql("INSERT OVERWRITE " + target() + " SELECT * FROM " + source());

    table.refresh();
    List<Record> expects = Lists.newArrayList(source);
    List<Record> actual = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, actual)
        .ignoreOrder("pt", "id")
        .assertRecordsEqual();

    assertFileLayout(format);
  }

  private static final Schema hiddenPartitionSchema = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "ts", Types.TimestampType.withZone())
  );
  private static final OffsetDateTime EPOCH = LocalDateTime.of(
          2000, 1, 1, 0, 0, 0)
      .atOffset(ZoneOffset.UTC);
  private static final List<Record> hiddenPartitionSource = IntStream.range(0, 10)
      .boxed()
      .map(i -> RecordGenerator.newRecord(hiddenPartitionSchema, i, EPOCH.plusDays(i)))
      .collect(Collectors.toList());

  private static PartitionSpec.Builder ptBuilder() {
    return PartitionSpec.builderFor(hiddenPartitionSchema);
  }

  public static Stream<Arguments> testHiddenPartitions() {
    return Stream.of(
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptBuilder().year("ts").build()),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptBuilder().month("ts").build()),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptBuilder().day("ts").build()),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptBuilder().hour("ts").build()),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec,
            ptBuilder().bucket("id", 8).build()),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec,
            ptBuilder().truncate("id", 10).build()),

        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, ptBuilder().year("ts").build()),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, ptBuilder().month("ts").build()),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, ptBuilder().day("ts").build()),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, ptBuilder().hour("ts").build()),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey,
            ptBuilder().bucket("id", 8).build()),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey,
            ptBuilder().truncate("id", 10).build())
    );
  }

  @DisplayName("TestSQL: INSERT OVERWRITE hidden partition optimize write")
  @ParameterizedTest()
  @MethodSource
  public void testHiddenPartitions(
      TableFormat format, PrimaryKeySpec keySpec, PartitionSpec ptSpec
  ) {
    spark().conf().set(OVERWRITE_MODE_KEY, DYNAMIC);
    spark().conf().set(SparkSQLProperties.OPTIMIZE_WRITE_ENABLED, "true");

    this.table = createTarget(hiddenPartitionSchema, builder ->
        builder.withPrimaryKeySpec(keySpec)
            .withPartitionSpec(ptSpec));
    createViewSource(hiddenPartitionSchema, hiddenPartitionSource);
    this.initFiles = Lists.newArrayList();

    sql("INSERT OVERWRITE " + target() + " SELECT * FROM " + source());

    table.refresh();
    assertFileLayout(format);
  }

  public static Stream<Arguments> testOptimizeWrite() {
    return Stream.of(
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptSpec, STATIC, 4, true),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, unpartitioned, STATIC, 4, true),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, ptSpec, STATIC, 4, true),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, unpartitioned, STATIC, 4, true),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptSpec, STATIC, 1, true),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptSpec, STATIC, 4, false),

        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptSpec, DYNAMIC, 4, true),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, unpartitioned, DYNAMIC, 4, true),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, ptSpec, DYNAMIC, 4, true),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, unpartitioned, DYNAMIC, 4, true),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptSpec, DYNAMIC, 1, true),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptSpec, DYNAMIC, 4, false),

        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, ptSpec, STATIC, 4, true),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, unpartitioned, STATIC, 4, true),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, ptSpec, STATIC, 4, true),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, unpartitioned, STATIC, 4, true),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, ptSpec, STATIC, 1, true),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, ptSpec, STATIC, 4, false),

        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, ptSpec, DYNAMIC, 4, true),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, unpartitioned, DYNAMIC, 4, true),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, ptSpec, DYNAMIC, 4, true),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, unpartitioned, DYNAMIC, 4, true),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, ptSpec, DYNAMIC, 1, true),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, ptSpec, DYNAMIC, 4, false)
    );
  }

  @DisplayName("TestSQL: INSERT OVERWRITE optimize write works")
  @ParameterizedTest()
  @MethodSource
  public void testOptimizeWrite(
      TableFormat format, PrimaryKeySpec keySpec, PartitionSpec ptSpec,
      String mode, int bucket, boolean optimizeWriteEnable
  ) {
    spark().conf().set(SparkSQLProperties.OPTIMIZE_WRITE_ENABLED, optimizeWriteEnable);
    spark().conf().set(OVERWRITE_MODE_KEY, mode);

    this.table = createTarget(schema, builder -> builder.withPrimaryKeySpec(keySpec)
        .withProperty(TableProperties.BASE_FILE_INDEX_HASH_BUCKET, String.valueOf(bucket))
        .withPartitionSpec(ptSpec));

    String[] ptValues = {"AAA", "BBB", "CCC", "DDD"};
    List<Record> source = IntStream.range(1, 100)
        .boxed()
        .map(i -> RecordGenerator.newRecord(schema, i, "index" + i, ptValues[i % ptValues.length]))
        .collect(Collectors.toList());
    createViewSource(schema, source);

    sql("INSERT OVERWRITE " + target() + " SELECT * FROM " + source());

    boolean shouldOptimized = optimizeWriteEnable && (keySpec.primaryKeyExisted() || ptSpec.isPartitioned());

    if (shouldOptimized) {
      table.refresh();
      TableFiles files = TestTableHelper.files(table);
      int expectFiles = ExpectResultHelper.expectOptimizeWriteFileCount(source, table, bucket);

      Assertions.assertEquals(expectFiles, files.baseDataFiles.size());
    }
  }

  public static Arguments[] testSourceDuplicateCheck() {
    return new Arguments[]{
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptSpec, STATIC, true, true),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, ptSpec, STATIC, true, false),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptSpec, DYNAMIC, true, true),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, ptSpec, DYNAMIC, true, false),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, ptSpec, DYNAMIC, false, false),

        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, ptSpec, STATIC, true, true),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, ptSpec, STATIC, true, false),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, ptSpec, DYNAMIC, true, true),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, ptSpec, DYNAMIC, true, false),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, ptSpec, DYNAMIC, false, false),
    };
  }

  @DisplayName("TestSQL: INSERT OVERWRITE duplicate check source")
  @ParameterizedTest()
  @MethodSource
  public void testSourceDuplicateCheck(
      TableFormat format, PrimaryKeySpec keySpec, PartitionSpec ptSpec, String mode,
      boolean duplicateSource, boolean expectChecked
  ) {
    spark().conf().set(OVERWRITE_MODE_KEY, mode);
    spark().conf().set(SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE, true);

    table = createTarget(schema, builder -> builder.withPartitionSpec(ptSpec)
        .withPrimaryKeySpec(keySpec));
    List<Record> sourceData = Lists.newArrayList(this.base);
    if (duplicateSource) {
      sourceData.addAll(this.source);
    }
    createViewSource(schema, sourceData);

    boolean failed = false;
    try {
      sql("INSERT OVERWRITE " + target() + " SELECT * FROM " + source());
    } catch (Exception e) {
      failed = true;
    }
    Assertions.assertEquals(expectChecked, failed);
  }
}
