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
import com.netease.arctic.spark.SparkSQLProperties;
import com.netease.arctic.spark.test.SparkTableTestBase;
import com.netease.arctic.spark.test.extensions.EnableCatalogSelect;
import com.netease.arctic.spark.test.helper.DataComparator;
import com.netease.arctic.spark.test.helper.ExpectResultHelper;
import com.netease.arctic.spark.test.helper.RecordGenerator;
import com.netease.arctic.spark.test.helper.TestTableHelper;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.MetadataColumns;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * test 1. upsert not enabled
 * test 2. upsert enabled
 * test 3. upsert source duplicate check
 * test 4. upsert optimize write
 */
@EnableCatalogSelect
@EnableCatalogSelect.SelectCatalog(byTableFormat = true)
public class TestInsertIntoSQL extends SparkTableTestBase {


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
      RecordGenerator.newRecord(schema, 3, "ccc", "AAA"),
      RecordGenerator.newRecord(schema, 4, "ddd", "AAA"),
      RecordGenerator.newRecord(schema, 5, "eee", "BBB"),
      RecordGenerator.newRecord(schema, 6, "fff", "BBB"),
      RecordGenerator.newRecord(schema, 7, "ggg", "BBB"),
      RecordGenerator.newRecord(schema, 8, "hhh", "BBB")
  );
  List<Record> source = Lists.newArrayList(
      RecordGenerator.newRecord(schema, 1, "xxx", "AAA"),
      RecordGenerator.newRecord(schema, 2, "xxx", "AAA"),
      RecordGenerator.newRecord(schema, 7, "xxx", "BBB"),
      RecordGenerator.newRecord(schema, 8, "xxx", "BBB"),
      RecordGenerator.newRecord(schema, 9, "xxx", "CCC"),
      RecordGenerator.newRecord(schema, 10, "xxx", "CCC")
  );
  List<Record> duplicateSource = Lists.newArrayList(
      RecordGenerator.newRecord(schema, 1, "xxx", "AAA"),
      RecordGenerator.newRecord(schema, 2, "xxx", "AAA"),
      RecordGenerator.newRecord(schema, 2, "xxx", "BBB")
  );

  Comparator<Record> pkComparator = Comparator.comparing(r -> r.get(0, Integer.class));

  Comparator<Record> dataComparator = Comparator.comparing(r -> r.get(1, String.class));

  Comparator<Record> changeActionComparator = Comparator.comparing(
      r -> (String) r.getField(MetadataColumns.CHANGE_ACTION_NAME));
  Comparator<Record> comparator = pkComparator.thenComparing(dataComparator);


  public static Stream<Arguments> testNoUpsert() {
    return Stream.of(
        Arguments.of(MIXED_HIVE, schema, idPrimaryKeySpec, ptSpec),
        Arguments.of(MIXED_HIVE, schema, noPrimaryKey, ptSpec),
        Arguments.of(MIXED_HIVE, schema, idPrimaryKeySpec, unpartitioned),
        Arguments.of(MIXED_HIVE, schema, noPrimaryKey, unpartitioned),

        Arguments.of(MIXED_ICEBERG, schema, idPrimaryKeySpec, ptSpec),
        Arguments.of(MIXED_ICEBERG, schema, noPrimaryKey, ptSpec),
        Arguments.of(MIXED_ICEBERG, schema, idPrimaryKeySpec, unpartitioned),
        Arguments.of(MIXED_ICEBERG, schema, noPrimaryKey, unpartitioned)
    );
  }

  @DisplayName("TestSQL: INSERT INTO table without upsert")
  @ParameterizedTest
  @MethodSource
  public void testNoUpsert(
      TableFormat format, Schema schema, PrimaryKeySpec keySpec, PartitionSpec ptSpec
  ) {
    ArcticTable table = createTarget(schema, tableBuilder ->
        tableBuilder.withPrimaryKeySpec(keySpec)
            .withProperty(TableProperties.UPSERT_ENABLED, "false")
            .withProperty(TableProperties.CHANGE_FILE_FORMAT, "AVRO")
            .withPartitionSpec(ptSpec));

    createViewSource(schema, source);

    TestTableHelper.writeToBase(table, base);
    sql("INSERT INTO " + target() + " SELECT * FROM " + source());
    table.refresh();

    // mor result
    List<Record> results = TestTableHelper.tableRecords(table);
    List<Record> expects = Lists.newArrayList();
    expects.addAll(base);
    expects.addAll(source);

    DataComparator.build(expects, results)
        .ignoreOrder(comparator)
        .assertRecordsEqual();
  }


  public static Stream<Arguments> testUpsert() {
    return Stream.of(
        Arguments.of(MIXED_HIVE, schema, idPrimaryKeySpec, ptSpec),
        Arguments.of(MIXED_HIVE, schema, noPrimaryKey, ptSpec),
        Arguments.of(MIXED_HIVE, schema, idPrimaryKeySpec, unpartitioned),
        Arguments.of(MIXED_HIVE, schema, noPrimaryKey, unpartitioned),

        Arguments.of(MIXED_ICEBERG, schema, idPrimaryKeySpec, ptSpec),
        Arguments.of(MIXED_ICEBERG, schema, noPrimaryKey, ptSpec),
        Arguments.of(MIXED_ICEBERG, schema, idPrimaryKeySpec, unpartitioned),
        Arguments.of(MIXED_ICEBERG, schema, noPrimaryKey, unpartitioned)
    );
  }

  @DisplayName("TestSQL: INSERT INTO table with upsert enabled")
  @ParameterizedTest
  @MethodSource
  public void testUpsert(
      TableFormat format, Schema schema, PrimaryKeySpec keySpec, PartitionSpec ptSpec
  ) {
    ArcticTable table = createTarget(schema, tableBuilder ->
        tableBuilder.withPrimaryKeySpec(keySpec)
            .withProperty(TableProperties.UPSERT_ENABLED, "true")
            .withPartitionSpec(ptSpec));
    createViewSource(schema, source);

    TestTableHelper.writeToBase(table, base);
    sql("INSERT INTO " + target() + " SELECT * FROM " + source());


    List<Record> expects;
    if (keySpec.primaryKeyExisted()) {
      expects = ExpectResultHelper.upsertResult(base, source, r -> r.get(0, Integer.class));
    } else {
      expects = Lists.newArrayList(base);
      expects.addAll(source);
    }

    table.refresh();
    List<Record> results = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, results)
        .ignoreOrder(comparator)
        .assertRecordsEqual();

    if (table.isKeyedTable()) {
      List<Record> deletes = ExpectResultHelper.upsertDeletes(base, source, r -> r.get(0, Integer.class));

      List<Record> expectChanges = deletes.stream()
          .map(
              r -> TestTableHelper.extendMetadataValue(
                  r, MetadataColumns.CHANGE_ACTION_FIELD, ChangeAction.DELETE.name())
          ).collect(Collectors.toList());

      source.stream().map(
          r -> TestTableHelper.extendMetadataValue(
              r, MetadataColumns.CHANGE_ACTION_FIELD, ChangeAction.INSERT.name()
          )
      ).forEach(expectChanges::add);

      List<Record> changes = TestTableHelper.changeRecordsWithAction(table.asKeyedTable());

      DataComparator.build(expectChanges, changes)
          .ignoreOrder(pkComparator.thenComparing(changeActionComparator))
          .assertRecordsEqual();
    }
  }


  public static Stream<Arguments> testDuplicateSourceCheck() {
    return Stream.of(
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, true, true),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey, true, false),
        Arguments.arguments(MIXED_HIVE, idPrimaryKeySpec, false, false),

        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, true, true),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey, true, false),
        Arguments.arguments(MIXED_ICEBERG, idPrimaryKeySpec, false, false)
    );
  }

  @DisplayName("TestSQL: INSERT INTO duplicate source check")
  @ParameterizedTest(name = "{index} {0} {1} source-is-duplicate: {2} expect-exception: {3}")
  @MethodSource
  public void testDuplicateSourceCheck(
      TableFormat format, PrimaryKeySpec keySpec, boolean duplicateSource, boolean expectException
  ) {
    spark().conf().set(SparkSQLProperties.CHECK_SOURCE_DUPLICATES_ENABLE, "true");
    createTarget(schema, tableBuilder ->
        tableBuilder.withPrimaryKeySpec(keySpec));

    List<Record> source = duplicateSource ? this.duplicateSource : this.source;
    createViewSource(schema, source);

    boolean getException = false;

    try {
      sql("INSERT INTO " + target() + " SELECT * FROM " + source());
    } catch (Exception e) {
      getException = true;
    }
    Assertions.assertEquals(expectException, getException, "expect exception assert failed.");
  }
}
