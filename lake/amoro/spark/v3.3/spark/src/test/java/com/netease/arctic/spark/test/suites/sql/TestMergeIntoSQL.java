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
import com.netease.arctic.spark.test.SparkTableTestBase;
import com.netease.arctic.spark.test.extensions.EnableCatalogSelect;
import com.netease.arctic.spark.test.helper.DataComparator;
import com.netease.arctic.spark.test.helper.ExpectResultHelper;
import com.netease.arctic.spark.test.helper.RecordGenerator;
import com.netease.arctic.spark.test.helper.TestTableHelper;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Types;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@EnableCatalogSelect
@EnableCatalogSelect.SelectCatalog(byTableFormat = true)
public class TestMergeIntoSQL extends SparkTableTestBase {

  private static final Schema schema = new Schema(
      Types.NestedField.required(1, "id", Types.IntegerType.get()),
      Types.NestedField.required(2, "data", Types.StringType.get()),
      Types.NestedField.required(3, "pt", Types.StringType.get())
  );
  private static final PrimaryKeySpec pk = PrimaryKeySpec.builderFor(schema).addColumn("id").build();

  private static final List<Record> base = Lists.newArrayList(
      RecordGenerator.newRecord(schema, 1, "a", "001"),
      RecordGenerator.newRecord(schema, 2, "b", "002")
  );
  private static final List<Record> change = Lists.newArrayList(
      RecordGenerator.newRecord(schema, 3, "c", "001"),
      RecordGenerator.newRecord(schema, 4, "d", "002")
  );

  private static final List<Record> source = Lists.newArrayList(
      RecordGenerator.newRecord(schema, 1, "s1", "001"),
      RecordGenerator.newRecord(schema, 2, "s2", "002"),
      RecordGenerator.newRecord(schema, 5, "s5", "001"),
      RecordGenerator.newRecord(schema, 6, "s6", "003")
  );

  private List<Record> target = Lists.newArrayList();

  public void setupTest(PrimaryKeySpec keySpec) {
    ArcticTable table = createTarget(schema, builder -> builder.withPrimaryKeySpec(keySpec));
    target.addAll(base);
    target.addAll(change);

    if (table.isKeyedTable()) {
      TestTableHelper.writeToBase(table, base);
      TestTableHelper.writeToChange(table.asKeyedTable(), change, ChangeAction.INSERT);
    } else {
      TestTableHelper.writeToBase(table, target);
    }
  }

  public static Stream<Arguments> args() {
    return Stream.of(
        Arguments.arguments(MIXED_ICEBERG, pk),
        Arguments.arguments(MIXED_ICEBERG, noPrimaryKey),

        Arguments.arguments(MIXED_HIVE, pk),
        Arguments.arguments(MIXED_HIVE, noPrimaryKey)
    );
  }

  @DisplayName("SQL: MERGE INTO for all actions with condition")
  @ParameterizedTest
  @MethodSource("args")
  public void testAllAction(TableFormat format, PrimaryKeySpec keySpec) {
    setupTest(keySpec);
    createViewSource(schema, source);

    sql("MERGE INTO " + target() + " AS t USING " + source() + " AS s ON t.id == s.id " +
        "WHEN MATCHED AND t.id = 1 THEN DELETE " +
        "WHEN MATCHED AND t.id = 2 THEN UPDATE SET * " +
        "WHEN NOT MATCHED AND s.id != 5 THEN INSERT *");

    List<Record> expects = ExpectResultHelper.expectMergeResult(
            target, source, r -> r.getField("id")
        ).whenMatched((t, s) -> t.getField("id").equals(1), (t, s) -> null)
        .whenMatched((t, s) -> t.getField("id").equals(2), (t, s) -> s)
        .whenNotMatched(s -> !s.getField("id").equals(5), Function.identity())
        .results();

    ArcticTable table = loadTable();
    List<Record> actual = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, actual)
        .ignoreOrder("id")
        .assertRecordsEqual();
  }

  @DisplayName("SQL: MERGE INTO for all actions with condition")
  @ParameterizedTest
  @MethodSource("args")
  public void testSetExactValue(TableFormat format, PrimaryKeySpec keySpec) {
    setupTest(keySpec);
    createViewSource(schema, source);

    sql("MERGE INTO " + target() + " AS t USING " + source() + " AS s ON t.id == s.id " +
        "WHEN MATCHED AND t.id = 2 THEN UPDATE SET t.data = 'ccc' ");

    List<Record> expects = ExpectResultHelper.expectMergeResult(
            target, source, r -> r.getField("id")
        ).whenMatched((t, s) -> t.getField("id").equals(2), (t, s) -> {
          t.setField("data", "ccc");
          return t;
        })
        .results();

    ArcticTable table = loadTable();
    List<Record> actual = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, actual)
        .ignoreOrder("id")
        .assertRecordsEqual();
  }

  @DisplayName("SQL: MERGE INTO for all actions with target no data")
  @ParameterizedTest
  @MethodSource("args")
  public void testEmptyTarget(TableFormat format, PrimaryKeySpec keySpec) {
    ArcticTable table = createTarget(schema, builder -> builder.withPrimaryKeySpec(keySpec));
    createViewSource(schema, source);

    sql("MERGE INTO " + target() + " AS t USING " + source() + " AS s ON t.id == s.id " +
        "WHEN MATCHED AND t.id = 1 THEN DELETE " +
        "WHEN MATCHED AND t.id = 2 THEN UPDATE SET * " +
        "WHEN NOT MATCHED THEN INSERT *");

    table.refresh();
    List<Record> expects = Lists.newArrayList(source);
    List<Record> actual = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, actual)
        .ignoreOrder("id")
        .assertRecordsEqual();
  }

  @DisplayName("SQL: MERGE INTO for all actions without condition")
  @ParameterizedTest
  @MethodSource("args")
  public void testActionWithoutCondition(TableFormat format, PrimaryKeySpec keySpec) {
    setupTest(keySpec);
    createViewSource(schema, source);

    sql("MERGE INTO " + target() + " AS t USING " + source() + " AS s ON t.id == s.id " +
        "WHEN MATCHED THEN UPDATE SET * " +
        "WHEN NOT MATCHED THEN INSERT *");

    List<Record> expects = ExpectResultHelper.expectMergeResult(
            target, source, r -> r.getField("id")
        ).whenMatched((t, s) -> true, (t, s) -> s)
        .whenNotMatched(s -> true, Function.identity())
        .results();

    ArcticTable table = loadTable();
    List<Record> actual = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, actual)
        .ignoreOrder("id")
        .assertRecordsEqual();
  }

  @DisplayName("SQL: MERGE INTO for only delete actions")
  @ParameterizedTest
  @MethodSource("args")
  public void testOnlyDeletes(TableFormat format, PrimaryKeySpec keySpec) {
    setupTest(keySpec);
    createViewSource(schema, source);
    sql("MERGE INTO " + target() + " AS t USING " + source() + " AS s ON t.id == s.id " +
        "WHEN MATCHED THEN DELETE ");

    List<Record> expects = ExpectResultHelper.expectMergeResult(
            target, source, r -> r.getField("id")
        ).whenMatched((t, s) -> true, (t, s) -> null)
        .results();

    ArcticTable table = loadTable();
    List<Record> actual = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, actual)
        .ignoreOrder("id")
        .assertRecordsEqual();
  }

  @DisplayName("SQL: MERGE INTO for explicit column ")
  @ParameterizedTest
  @MethodSource("args")
  public void testExplicitColumn(TableFormat format, PrimaryKeySpec keySpec) {
    setupTest(keySpec);
    createViewSource(schema, source);

    sql("MERGE INTO " + target() + " AS t USING " + source() + " AS s ON t.id == s.id " +
        "WHEN MATCHED THEN UPDATE SET t.id = s.id, t.data = s.pt, t.pt = s.pt " +
        "WHEN NOT MATCHED THEN INSERT (t.data, t.pt, t.id) values ( s.pt, s.pt, s.id) ");

    Function<Record, Record> dataAsPt = s -> {
      Record r = s.copy();
      r.setField("data", s.getField("pt"));
      return r;
    };
    List<Record> expects = ExpectResultHelper.expectMergeResult(
            target, source, r -> r.getField("id")
        ).whenMatched((t, s) -> true, (t, s) -> dataAsPt.apply(s))
        .whenNotMatched(s -> true, dataAsPt)
        .results();

    ArcticTable table = loadTable();
    List<Record> actual = TestTableHelper.tableRecords(table);
    DataComparator.build(expects, actual)
        .ignoreOrder("id")
        .assertRecordsEqual();
  }

  public static Stream<TableFormat> formatArgs() {
    return Stream.of(MIXED_HIVE, MIXED_ICEBERG);
  }

  @DisplayName("SQL: MERGE INTO failed if join on non primary key")
  @ParameterizedTest
  @MethodSource("formatArgs")
  public void testFailedForNonPrimaryKeyMerge(TableFormat format) {
    setupTest(pk);
    createViewSource(schema, source);

    boolean catched = false;
    try {
      sql("MERGE INTO " + target() + " AS t USING " + source() + " AS s ON t.pt == s.id " +
          "WHEN MATCHED THEN UPDATE SET t.id = s.id, t.data = s.pt, t.pt = s.pt " +
          "WHEN NOT MATCHED THEN INSERT (t.data, t.pt, t.id) values ( s.pt, s.pt, s.id) ");
    } catch (Exception e) {
      catched = true;
    }
    Assertions.assertTrue(catched);
  }

  @DisplayName("SQL: MERGE INTO failed if source has duplicate join key")
  @ParameterizedTest
  @MethodSource("formatArgs")
  public void testFailedWhenDuplicateJoinKey(TableFormat format) {
    setupTest(pk);
    List<Record> source = Lists.newArrayList(
        RecordGenerator.newRecord(schema, 1, "s1", "001"),
        RecordGenerator.newRecord(schema, 1, "s2", "001")
    );
    createViewSource(schema, source);

    boolean catched = false;
    try {
      sql("MERGE INTO " + target() + " AS t USING " + source() + " AS s ON t.id == s.id " +
          "WHEN MATCHED THEN UPDATE SET t.id = s.id, t.data = s.pt, t.pt = s.pt " +
          "WHEN NOT MATCHED THEN INSERT (t.data, t.pt, t.id) values ( s.pt, s.pt, s.id) ");
    } catch (Exception e) {
      catched = true;
    }
    Assertions.assertTrue(catched);
  }
}
