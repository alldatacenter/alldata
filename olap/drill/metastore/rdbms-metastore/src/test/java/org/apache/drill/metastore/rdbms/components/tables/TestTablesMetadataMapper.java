/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.rdbms.components.tables;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.TestData;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.rdbms.RdbmsBaseTest;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.generated.Tables;
import org.jooq.impl.DSL;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTablesMetadataMapper extends RdbmsBaseTest {

  @Test
  public void testTable() {
    assertEquals(Tables.TABLES, TablesMetadataMapper.TableMapper.get().table());
    assertEquals(Tables.SEGMENTS, TablesMetadataMapper.SegmentMapper.get().table());
    assertEquals(Tables.FILES, TablesMetadataMapper.FileMapper.get().table());
    assertEquals(Tables.ROW_GROUPS, TablesMetadataMapper.RowGroupMapper.get().table());
    assertEquals(Tables.PARTITIONS, TablesMetadataMapper.PartitionMapper.get().table());
  }

  @Test
  public void testToFields() {
    List<Field<?>> tablesFields = TablesMetadataMapper.TableMapper.get()
      .toFields(Arrays.asList(MetastoreColumn.OWNER,
        MetastoreColumn.TABLE_NAME,
        MetastoreColumn.INTERESTING_COLUMNS));

    assertEquals(
      Arrays.asList(Tables.TABLES.OWNER, Tables.TABLES.TABLE_NAME, Tables.TABLES.INTERESTING_COLUMNS),
      tablesFields);

    List<Field<?>> segmentsFields = TablesMetadataMapper.SegmentMapper.get()
      .toFields(Arrays.asList(MetastoreColumn.COLUMNS_STATISTICS,
        MetastoreColumn.TABLE_NAME,
        MetastoreColumn.LOCATION));

    assertEquals(
      Arrays.asList(Tables.SEGMENTS.COLUMN_STATISTICS, Tables.SEGMENTS.TABLE_NAME, Tables.SEGMENTS.LOCATION),
      segmentsFields);

    List<Field<?>> filesFields = TablesMetadataMapper.FileMapper.get()
      .toFields(Arrays.asList(MetastoreColumn.PATH,
        MetastoreColumn.TABLE_NAME,
        MetastoreColumn.LOCATION));

    assertEquals(
      Arrays.asList(Tables.FILES.PATH, Tables.FILES.TABLE_NAME, Tables.FILES.LOCATION),
      filesFields);

    List<Field<?>> rowGroupsFields = TablesMetadataMapper.RowGroupMapper.get()
      .toFields(Arrays.asList(MetastoreColumn.PATH,
        MetastoreColumn.TABLE_NAME,
        MetastoreColumn.HOST_AFFINITY));

    assertEquals(
      Arrays.asList(Tables.ROW_GROUPS.PATH, Tables.ROW_GROUPS.TABLE_NAME, Tables.ROW_GROUPS.HOST_AFFINITY),
      rowGroupsFields);

    List<Field<?>> partitionFields = TablesMetadataMapper.PartitionMapper.get()
      .toFields(Arrays.asList(MetastoreColumn.PARTITION_VALUES,
        MetastoreColumn.TABLE_NAME,
        MetastoreColumn.LOCATIONS));

    assertEquals(
      Arrays.asList(Tables.PARTITIONS.PARTITION_VALUES, Tables.PARTITIONS.TABLE_NAME, Tables.PARTITIONS.LOCATIONS),
      partitionFields);
  }

  @Test
  public void testToFieldsAbsent() {
    List<Field<?>> tableFields = TablesMetadataMapper.TableMapper.get().toFields(
      Arrays.asList(MetastoreColumn.SCHEMA, MetastoreColumn.COLUMN, MetastoreColumn.HOST_AFFINITY));
    assertEquals(1, tableFields.size());

    List<Field<?>> segmentFields = TablesMetadataMapper.SegmentMapper.get().toFields(
      Arrays.asList(MetastoreColumn.SCHEMA, MetastoreColumn.OWNER, MetastoreColumn.HOST_AFFINITY));
    assertEquals(1, segmentFields.size());

    List<Field<?>> fileFields = TablesMetadataMapper.FileMapper.get().toFields(
      Arrays.asList(MetastoreColumn.SCHEMA, MetastoreColumn.OWNER, MetastoreColumn.HOST_AFFINITY));
    assertEquals(1, fileFields.size());

    List<Field<?>> rowGroupFields = TablesMetadataMapper.RowGroupMapper.get().toFields(
      Arrays.asList(MetastoreColumn.LOCATIONS, MetastoreColumn.OWNER, MetastoreColumn.HOST_AFFINITY));
    assertEquals(1, rowGroupFields.size());

    List<Field<?>> partitionFields = TablesMetadataMapper.PartitionMapper.get().toFields(
      Arrays.asList(MetastoreColumn.LOCATIONS, MetastoreColumn.OWNER, MetastoreColumn.HOST_AFFINITY));
    assertEquals(1, partitionFields.size());
  }

  @Test
  public void testToCondition() {
    FilterExpression filterExpression = FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs");

    Condition tablesCondition = TablesMetadataMapper.TableMapper.get().toCondition(filterExpression);
    assertEquals(Tables.TABLES.STORAGE_PLUGIN.eq("dfs"), tablesCondition);

    Condition segmentsCondition = TablesMetadataMapper.SegmentMapper.get().toCondition(filterExpression);
    assertEquals(Tables.SEGMENTS.STORAGE_PLUGIN.eq("dfs"), segmentsCondition);

    Condition filesCondition = TablesMetadataMapper.FileMapper.get().toCondition(filterExpression);
    assertEquals(Tables.FILES.STORAGE_PLUGIN.eq("dfs"), filesCondition);

    Condition rowGroupsCondition = TablesMetadataMapper.RowGroupMapper.get().toCondition(filterExpression);
    assertEquals(Tables.ROW_GROUPS.STORAGE_PLUGIN.eq("dfs"), rowGroupsCondition);

    Condition partitionsCondition = TablesMetadataMapper.PartitionMapper.get().toCondition(filterExpression);
    assertEquals(Tables.PARTITIONS.STORAGE_PLUGIN.eq("dfs"), partitionsCondition);
  }

  @Test
  public void testToConditionNull() {
    assertEquals(DSL.noCondition().toString(),
      TablesMetadataMapper.TableMapper.get().toCondition(null).toString());

    assertEquals(DSL.noCondition().toString(),
      TablesMetadataMapper.SegmentMapper.get().toCondition(null).toString());

    assertEquals(DSL.noCondition().toString(),
      TablesMetadataMapper.FileMapper.get().toCondition(null).toString());

    assertEquals(DSL.noCondition().toString(),
      TablesMetadataMapper.RowGroupMapper.get().toCondition(null).toString());

    assertEquals(DSL.noCondition().toString(),
      TablesMetadataMapper.PartitionMapper.get().toCondition(null).toString());
  }

  @Test
  public void testToDeleteConditionsTables() {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    List<TableMetadataUnit> units = Arrays.asList(
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("region")
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .build()
    );

    Condition[] expectedConditions = new Condition[] {
      DSL.and(Tables.TABLES.STORAGE_PLUGIN.eq("dfs"),
        Tables.TABLES.WORKSPACE.eq("tmp"),
        Tables.TABLES.TABLE_NAME.eq("region")),

      DSL.and(Tables.TABLES.STORAGE_PLUGIN.eq("dfs"),
        Tables.TABLES.WORKSPACE.eq("tmp"),
        Tables.TABLES.TABLE_NAME.eq("nation"))
    };

    List<Condition> actualConditions = TablesMetadataMapper.TableMapper.get().toDeleteConditions(units);

    assertEquals(expectedConditions.length, actualConditions.size());
    assertThat(actualConditions, hasItems(expectedConditions));
  }

  @Test
  public void testToDeleteConditionsSegments() {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    List<TableMetadataUnit> units = Arrays.asList(
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2008")
        .metadataIdentifier("2008")
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2008")
        .metadataIdentifier("2008/Q1")
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2009")
        .metadataIdentifier("2009")
        .build()
    );

    Condition[] expectedConditions = new Condition[] {
      DSL.and(Tables.SEGMENTS.STORAGE_PLUGIN.eq("dfs"),
        Tables.SEGMENTS.WORKSPACE.eq("tmp"),
        Tables.SEGMENTS.TABLE_NAME.eq("nation"),
        Tables.SEGMENTS.METADATA_KEY.eq("2008")),

      DSL.and(Tables.SEGMENTS.STORAGE_PLUGIN.eq("dfs"),
        Tables.SEGMENTS.WORKSPACE.eq("tmp"),
        Tables.SEGMENTS.TABLE_NAME.eq("nation"),
        Tables.SEGMENTS.METADATA_KEY.eq("2009")),
    };

    List<Condition> actualConditions = TablesMetadataMapper.SegmentMapper.get().toDeleteConditions(units);

    assertEquals(expectedConditions.length, actualConditions.size());
    assertThat(actualConditions, hasItems(expectedConditions));
  }

  @Test
  public void testToDeleteConditionsFiles() {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    List<TableMetadataUnit> units = Arrays.asList(
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2008")
        .metadataIdentifier("2008/0_0_0.parquet")
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2008")
        .metadataIdentifier("2008/0_0_1.parquet")
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2009")
        .metadataIdentifier("2009/0_0_0.parquet")
        .build()
    );

    Condition[] expectedConditions = new Condition[] {
      DSL.and(Tables.FILES.STORAGE_PLUGIN.eq("dfs"),
        Tables.FILES.WORKSPACE.eq("tmp"),
        Tables.FILES.TABLE_NAME.eq("nation"),
        Tables.FILES.METADATA_KEY.eq("2008")),

      DSL.and(Tables.FILES.STORAGE_PLUGIN.eq("dfs"),
        Tables.FILES.WORKSPACE.eq("tmp"),
        Tables.FILES.TABLE_NAME.eq("nation"),
        Tables.FILES.METADATA_KEY.eq("2009")),
    };

    List<Condition> actualConditions = TablesMetadataMapper.FileMapper.get().toDeleteConditions(units);

    assertEquals(expectedConditions.length, actualConditions.size());
    assertThat(actualConditions, hasItems(expectedConditions));
  }

  @Test
  public void testToDeleteConditionsRowGroups() {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    List<TableMetadataUnit> units = Arrays.asList(
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2008")
        .metadataIdentifier("2008/0_0_0.parquet/1")
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2008")
        .metadataIdentifier("2008/0_0_0.parquet/2")
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2009")
        .metadataIdentifier("2009/0_0_0.parquet/1")
        .build()
    );

    Condition[] expectedConditions = new Condition[] {
      DSL.and(Tables.ROW_GROUPS.STORAGE_PLUGIN.eq("dfs"),
        Tables.ROW_GROUPS.WORKSPACE.eq("tmp"),
        Tables.ROW_GROUPS.TABLE_NAME.eq("nation"),
        Tables.ROW_GROUPS.METADATA_KEY.eq("2008")),

      DSL.and(Tables.ROW_GROUPS.STORAGE_PLUGIN.eq("dfs"),
        Tables.ROW_GROUPS.WORKSPACE.eq("tmp"),
        Tables.ROW_GROUPS.TABLE_NAME.eq("nation"),
        Tables.ROW_GROUPS.METADATA_KEY.eq("2009")),
    };

    List<Condition> actualConditions = TablesMetadataMapper.RowGroupMapper.get().toDeleteConditions(units);

    assertEquals(expectedConditions.length, actualConditions.size());
    assertThat(actualConditions, hasItems(expectedConditions));
  }

  @Test
  public void testToDeleteConditionsPartitions() {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    List<TableMetadataUnit> units = Arrays.asList(
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2008")
        .metadataIdentifier("2008/01")
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2008")
        .metadataIdentifier("2008/02")
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("2009")
        .metadataIdentifier("2009/01")
        .build()
    );

    Condition[] expectedConditions = new Condition[] {
      DSL.and(Tables.PARTITIONS.STORAGE_PLUGIN.eq("dfs"),
        Tables.PARTITIONS.WORKSPACE.eq("tmp"),
        Tables.PARTITIONS.TABLE_NAME.eq("nation"),
        Tables.PARTITIONS.METADATA_KEY.eq("2008")),

      DSL.and(Tables.PARTITIONS.STORAGE_PLUGIN.eq("dfs"),
        Tables.PARTITIONS.WORKSPACE.eq("tmp"),
        Tables.PARTITIONS.TABLE_NAME.eq("nation"),
        Tables.PARTITIONS.METADATA_KEY.eq("2009")),
    };

    List<Condition> actualConditions = TablesMetadataMapper.PartitionMapper.get().toDeleteConditions(units);

    assertEquals(expectedConditions.length, actualConditions.size());
    assertThat(actualConditions, hasItems(expectedConditions));
  }
}
