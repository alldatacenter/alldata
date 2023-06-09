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
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.rdbms.transform.AbstractMetadataMapper;
import org.apache.drill.metastore.rdbms.transform.RdbmsFilterExpressionVisitor;
import org.apache.drill.metastore.rdbms.util.ConverterUtil;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.generated.Tables;
import org.jooq.generated.tables.records.FilesRecord;
import org.jooq.generated.tables.records.PartitionsRecord;
import org.jooq.generated.tables.records.RowGroupsRecord;
import org.jooq.generated.tables.records.SegmentsRecord;
import org.jooq.generated.tables.records.TablesRecord;
import org.jooq.impl.DSL;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract implementation of {@link AbstractMetadataMapper} for RDBMS Metastore tables component.
 * Contains common code for specific RDBMS Metastore tables component tables.
 *
 * @param <R> RDBMS table record type
 */
public abstract class TablesMetadataMapper<R extends Record> extends AbstractMetadataMapper<TableMetadataUnit, R> {

  protected static final Function<TableMetadataUnit, List<String>> TABLE_PARTITION_KEY = unit ->
    Arrays.asList(unit.storagePlugin(), unit.workspace(), unit.tableName());

  protected static final Function<TableMetadataUnit, List<String>> COMPONENT_PARTITION_KEY = unit ->
    Arrays.asList(unit.storagePlugin(), unit.workspace(), unit.tableName(), unit.metadataKey());

  @Override
  public TableMetadataUnit emptyUnit() {
    return TableMetadataUnit.EMPTY_UNIT;
  }

  @Override
  public List<Condition> toDeleteConditions(List<TableMetadataUnit> units) {
    Set<List<String>> partitionValues = units.stream()
      .collect(Collectors.groupingBy(partitionKey(), Collectors.toList()))
      .keySet();

    return partitionValues.stream()
      .map(values -> DSL.and(toConditions(values)))
      .collect(Collectors.toList());
  }

  /**
   * @return function to determine partition key for specific table
   */
  protected abstract Function<TableMetadataUnit, List<String>> partitionKey();

  /**
   * Creates JOOQ conditions based on given list of partition values.
   * Matching is order based.
   *
   * @param values partition values
   * @return list of JOOQ conditions
   */
  protected abstract List<Condition> toConditions(List<String> values);

  /**
   * {@link TablesMetadataMapper} implementation for {@link Tables#TABLES} table.
   */
  public static class TableMapper extends TablesMetadataMapper<TablesRecord> {

    private static final TableMapper INSTANCE = new TableMapper();

    private static final Map<MetastoreColumn, Field<?>> COLUMNS_MAP = ImmutableMap.<MetastoreColumn, Field<?>>builder()
      .put(MetastoreColumn.STORAGE_PLUGIN, Tables.TABLES.STORAGE_PLUGIN)
      .put(MetastoreColumn.WORKSPACE, Tables.TABLES.WORKSPACE)
      .put(MetastoreColumn.TABLE_NAME, Tables.TABLES.TABLE_NAME)
      .put(MetastoreColumn.OWNER, Tables.TABLES.OWNER)
      .put(MetastoreColumn.TABLE_TYPE, Tables.TABLES.TABLE_TYPE)
      .put(MetastoreColumn.METADATA_KEY, Tables.TABLES.METADATA_KEY)
      .put(MetastoreColumn.METADATA_TYPE, Tables.TABLES.METADATA_TYPE)
      .put(MetastoreColumn.LOCATION, Tables.TABLES.LOCATION)
      .put(MetastoreColumn.INTERESTING_COLUMNS, Tables.TABLES.INTERESTING_COLUMNS)
      .put(MetastoreColumn.SCHEMA, Tables.TABLES.SCHEMA)
      .put(MetastoreColumn.COLUMNS_STATISTICS, Tables.TABLES.COLUMN_STATISTICS)
      .put(MetastoreColumn.METADATA_STATISTICS, Tables.TABLES.METADATA_STATISTICS)
      .put(MetastoreColumn.PARTITION_KEYS, Tables.TABLES.PARTITION_KEYS)
      .put(MetastoreColumn.LAST_MODIFIED_TIME, Tables.TABLES.LAST_MODIFIED_TIME)
      .put(MetastoreColumn.ADDITIONAL_METADATA, Tables.TABLES.ADDITIONAL_METADATA)
      .build();

    private static final RdbmsFilterExpressionVisitor FILTER_VISITOR = new RdbmsFilterExpressionVisitor(COLUMNS_MAP);

    public static TableMapper get() {
      return INSTANCE;
    }

    @Override
    public Table<TablesRecord> table() {
      return Tables.TABLES;
    }

    @Override
    public TableMetadataUnit toUnit(Record record) {
      TablesRecord tablesRecord = (TablesRecord) record;
      return TableMetadataUnit.builder()
        .storagePlugin(tablesRecord.getStoragePlugin())
        .workspace(tablesRecord.getWorkspace())
        .tableName(tablesRecord.getTableName())
        .owner(tablesRecord.getOwner())
        .tableType(tablesRecord.getTableType())
        .metadataKey(tablesRecord.getMetadataKey())
        .metadataType(tablesRecord.getMetadataType())
        .location(tablesRecord.getLocation())
        .interestingColumns(ConverterUtil.convertToListString(tablesRecord.getInterestingColumns()))
        .schema(tablesRecord.getSchema())
        .columnsStatistics(ConverterUtil.convertToMapStringString(tablesRecord.getColumnStatistics()))
        .metadataStatistics(ConverterUtil.convertToListString(tablesRecord.getMetadataStatistics()))
        .partitionKeys(ConverterUtil.convertToMapStringString(tablesRecord.getPartitionKeys()))
        .lastModifiedTime(tablesRecord.getLastModifiedTime())
        .additionalMetadata(tablesRecord.getAdditionalMetadata())
        .build();
    }

    @Override
    public TablesRecord toRecord(TableMetadataUnit unit) {
      TablesRecord record = new TablesRecord();
      record.setStoragePlugin(unit.storagePlugin());
      record.setWorkspace(unit.workspace());
      record.setTableName(unit.tableName());
      record.setOwner(unit.owner());
      record.setTableType(unit.tableType());
      record.setMetadataKey(unit.metadataKey());
      record.setMetadataType(unit.metadataType());
      record.setLocation(unit.location());
      record.setInterestingColumns(ConverterUtil.convertToString(unit.interestingColumns()));
      record.setSchema(unit.schema());
      record.setColumnStatistics(ConverterUtil.convertToString(unit.columnsStatistics()));
      record.setMetadataStatistics(ConverterUtil.convertToString(unit.metadataStatistics()));
      record.setPartitionKeys(ConverterUtil.convertToString(unit.partitionKeys()));
      record.setLastModifiedTime(unit.lastModifiedTime());
      record.setAdditionalMetadata(unit.additionalMetadata());
      return record;
    }

    @Override
    protected Map<MetastoreColumn, Field<?>> fieldMapper() {
      return COLUMNS_MAP;
    }

    @Override
    protected RdbmsFilterExpressionVisitor filterVisitor() {
      return FILTER_VISITOR;
    }

    @Override
    protected Function<TableMetadataUnit, List<String>> partitionKey() {
      return TABLE_PARTITION_KEY;
    }

    @Override
    protected List<Condition> toConditions(List<String> values) {
      assert values.size() == 3;
      return Arrays.asList(
        Tables.TABLES.STORAGE_PLUGIN.eq(values.get(0)),
        Tables.TABLES.WORKSPACE.eq(values.get(1)),
        Tables.TABLES.TABLE_NAME.eq(values.get(2))
      );
    }
  }

  /**
   * {@link TablesMetadataMapper} implementation for {@link Tables#SEGMENTS} table.
   */
  public static class SegmentMapper extends TablesMetadataMapper<SegmentsRecord> {

    private static final SegmentMapper INSTANCE = new SegmentMapper();

    private static final Map<MetastoreColumn, Field<?>> COLUMNS_MAP = ImmutableMap.<MetastoreColumn, Field<?>>builder()
      .put(MetastoreColumn.STORAGE_PLUGIN, Tables.SEGMENTS.STORAGE_PLUGIN)
      .put(MetastoreColumn.WORKSPACE, Tables.SEGMENTS.WORKSPACE)
      .put(MetastoreColumn.TABLE_NAME, Tables.SEGMENTS.TABLE_NAME)
      .put(MetastoreColumn.METADATA_KEY, Tables.SEGMENTS.METADATA_KEY)
      .put(MetastoreColumn.METADATA_IDENTIFIER, Tables.SEGMENTS.METADATA_IDENTIFIER)
      .put(MetastoreColumn.METADATA_TYPE, Tables.SEGMENTS.METADATA_TYPE)
      .put(MetastoreColumn.LOCATION, Tables.SEGMENTS.LOCATION)
      .put(MetastoreColumn.SCHEMA, Tables.SEGMENTS.SCHEMA)
      .put(MetastoreColumn.COLUMNS_STATISTICS, Tables.SEGMENTS.COLUMN_STATISTICS)
      .put(MetastoreColumn.METADATA_STATISTICS, Tables.SEGMENTS.METADATA_STATISTICS)
      .put(MetastoreColumn.COLUMN, Tables.SEGMENTS.COLUMN)
      .put(MetastoreColumn.LOCATIONS, Tables.SEGMENTS.LOCATIONS)
      .put(MetastoreColumn.PARTITION_VALUES, Tables.SEGMENTS.PARTITION_VALUES)
      .put(MetastoreColumn.PATH, Tables.SEGMENTS.PATH)
      .put(MetastoreColumn.LAST_MODIFIED_TIME, Tables.SEGMENTS.LAST_MODIFIED_TIME)
      .put(MetastoreColumn.ADDITIONAL_METADATA, Tables.SEGMENTS.ADDITIONAL_METADATA)
      .build();

    private static final RdbmsFilterExpressionVisitor FILTER_VISITOR = new RdbmsFilterExpressionVisitor(COLUMNS_MAP);

    public static SegmentMapper get() {
      return INSTANCE;
    }

    @Override
    public Table<SegmentsRecord> table() {
      return Tables.SEGMENTS;
    }

    @Override
    public TableMetadataUnit toUnit(Record record) {
      SegmentsRecord segmentsRecord = (SegmentsRecord) record;
      return TableMetadataUnit.builder()
        .storagePlugin(segmentsRecord.getStoragePlugin())
        .workspace(segmentsRecord.getWorkspace())
        .tableName(segmentsRecord.getTableName())
        .metadataKey(segmentsRecord.getMetadataKey())
        .metadataIdentifier(segmentsRecord.getMetadataIdentifier())
        .metadataType(segmentsRecord.getMetadataType())
        .location(segmentsRecord.getLocation())
        .schema(segmentsRecord.getSchema())
        .columnsStatistics(ConverterUtil.convertToMapStringString(segmentsRecord.getColumnStatistics()))
        .metadataStatistics(ConverterUtil.convertToListString(segmentsRecord.getMetadataStatistics()))
        .column(segmentsRecord.getColumn())
        .locations(ConverterUtil.convertToListString(segmentsRecord.getLocations()))
        .partitionValues(ConverterUtil.convertToListString(segmentsRecord.getPartitionValues()))
        .path(segmentsRecord.getPath())
        .lastModifiedTime(segmentsRecord.getLastModifiedTime())
        .additionalMetadata(segmentsRecord.getAdditionalMetadata())
        .build();
    }

    @Override
    public SegmentsRecord toRecord(TableMetadataUnit unit) {
      SegmentsRecord record = new SegmentsRecord();
      record.setStoragePlugin(unit.storagePlugin());
      record.setWorkspace(unit.workspace());
      record.setTableName(unit.tableName());
      record.setMetadataKey(unit.metadataKey());
      record.setMetadataIdentifier(unit.metadataIdentifier());
      record.setMetadataType(unit.metadataType());
      record.setLocation(unit.location());
      record.setSchema(unit.schema());
      record.setColumnStatistics(ConverterUtil.convertToString(unit.columnsStatistics()));
      record.setMetadataStatistics(ConverterUtil.convertToString(unit.metadataStatistics()));
      record.setColumn(unit.column());
      record.setLocations(ConverterUtil.convertToString(unit.locations()));
      record.setPartitionValues(ConverterUtil.convertToString(unit.partitionValues()));
      record.setPath(unit.path());
      record.setLastModifiedTime(unit.lastModifiedTime());
      record.setAdditionalMetadata(unit.additionalMetadata());
      return record;
    }

    @Override
    protected Map<MetastoreColumn, Field<?>> fieldMapper() {
      return COLUMNS_MAP;
    }

    @Override
    protected RdbmsFilterExpressionVisitor filterVisitor() {
      return FILTER_VISITOR;
    }

    @Override
    protected Function<TableMetadataUnit, List<String>> partitionKey() {
      return COMPONENT_PARTITION_KEY;
    }

    @Override
    protected List<Condition> toConditions(List<String> values) {
      assert values.size() == 4;
      return Arrays.asList(Tables.SEGMENTS.STORAGE_PLUGIN.eq(values.get(0)),
        Tables.SEGMENTS.WORKSPACE.eq(values.get(1)),
        Tables.SEGMENTS.TABLE_NAME.eq(values.get(2)),
        Tables.SEGMENTS.METADATA_KEY.eq(values.get(3)));
    }
  }

  /**
   * {@link TablesMetadataMapper} implementation for {@link Tables#FILES} table.
   */
  public static class FileMapper extends TablesMetadataMapper<FilesRecord> {

    private static final FileMapper INSTANCE = new FileMapper();

    private static final Map<MetastoreColumn, Field<?>> COLUMNS_MAP = ImmutableMap.<MetastoreColumn, Field<?>>builder()
      .put(MetastoreColumn.STORAGE_PLUGIN, Tables.FILES.STORAGE_PLUGIN)
      .put(MetastoreColumn.WORKSPACE, Tables.FILES.WORKSPACE)
      .put(MetastoreColumn.TABLE_NAME, Tables.FILES.TABLE_NAME)
      .put(MetastoreColumn.METADATA_KEY, Tables.FILES.METADATA_KEY)
      .put(MetastoreColumn.METADATA_IDENTIFIER, Tables.FILES.METADATA_IDENTIFIER)
      .put(MetastoreColumn.METADATA_TYPE, Tables.FILES.METADATA_TYPE)
      .put(MetastoreColumn.LOCATION, Tables.FILES.LOCATION)
      .put(MetastoreColumn.SCHEMA, Tables.FILES.SCHEMA)
      .put(MetastoreColumn.COLUMNS_STATISTICS, Tables.FILES.COLUMN_STATISTICS)
      .put(MetastoreColumn.METADATA_STATISTICS, Tables.FILES.METADATA_STATISTICS)
      .put(MetastoreColumn.PATH, Tables.FILES.PATH)
      .put(MetastoreColumn.LAST_MODIFIED_TIME, Tables.FILES.LAST_MODIFIED_TIME)
      .put(MetastoreColumn.ADDITIONAL_METADATA, Tables.FILES.ADDITIONAL_METADATA)
      .build();

    private static final RdbmsFilterExpressionVisitor FILTER_VISITOR = new RdbmsFilterExpressionVisitor(COLUMNS_MAP);

    public static FileMapper get() {
      return INSTANCE;
    }

    @Override
    public Table<FilesRecord> table() {
      return Tables.FILES;
    }

    @Override
    public TableMetadataUnit toUnit(Record record) {
      FilesRecord filesRecord = (FilesRecord) record;
      return TableMetadataUnit.builder()
        .storagePlugin(filesRecord.getStoragePlugin())
        .workspace(filesRecord.getWorkspace())
        .tableName(filesRecord.getTableName())
        .metadataKey(filesRecord.getMetadataKey())
        .metadataIdentifier(filesRecord.getMetadataIdentifier())
        .metadataType(filesRecord.getMetadataType())
        .location(filesRecord.getLocation())
        .schema(filesRecord.getSchema())
        .columnsStatistics(ConverterUtil.convertToMapStringString(filesRecord.getColumnStatistics()))
        .metadataStatistics(ConverterUtil.convertToListString(filesRecord.getMetadataStatistics()))
        .path(filesRecord.getPath())
        .lastModifiedTime(filesRecord.getLastModifiedTime())
        .additionalMetadata(filesRecord.getAdditionalMetadata())
        .build();
    }

    @Override
    public FilesRecord toRecord(TableMetadataUnit unit) {
      FilesRecord record = new FilesRecord();
      record.setStoragePlugin(unit.storagePlugin());
      record.setWorkspace(unit.workspace());
      record.setTableName(unit.tableName());
      record.setMetadataKey(unit.metadataKey());
      record.setMetadataIdentifier(unit.metadataIdentifier());
      record.setMetadataType(unit.metadataType());
      record.setLocation(unit.location());
      record.setSchema(unit.schema());
      record.setColumnStatistics(ConverterUtil.convertToString(unit.columnsStatistics()));
      record.setMetadataStatistics(ConverterUtil.convertToString(unit.metadataStatistics()));
      record.setPath(unit.path());
      record.setLastModifiedTime(unit.lastModifiedTime());
      record.setAdditionalMetadata(unit.additionalMetadata());
      return record;
    }

    @Override
    protected Map<MetastoreColumn, Field<?>> fieldMapper() {
      return COLUMNS_MAP;
    }

    @Override
    protected RdbmsFilterExpressionVisitor filterVisitor() {
      return FILTER_VISITOR;
    }

    @Override
    protected Function<TableMetadataUnit, List<String>> partitionKey() {
      return COMPONENT_PARTITION_KEY;
    }

    @Override
    protected List<Condition> toConditions(List<String> values) {
      assert values.size() == 4;
      return Arrays.asList(
        Tables.FILES.STORAGE_PLUGIN.eq(values.get(0)),
        Tables.FILES.WORKSPACE.eq(values.get(1)),
        Tables.FILES.TABLE_NAME.eq(values.get(2)),
        Tables.FILES.METADATA_KEY.eq(values.get(3)));
    }
  }

  /**
   * {@link TablesMetadataMapper} implementation for {@link Tables#ROW_GROUPS} table.
   */
  public static class RowGroupMapper extends TablesMetadataMapper<RowGroupsRecord> {

    private static final RowGroupMapper INSTANCE = new RowGroupMapper();

    private static final Map<MetastoreColumn, Field<?>> COLUMNS_MAP = ImmutableMap.<MetastoreColumn, Field<?>>builder()
      .put(MetastoreColumn.STORAGE_PLUGIN, Tables.ROW_GROUPS.STORAGE_PLUGIN)
      .put(MetastoreColumn.WORKSPACE, Tables.ROW_GROUPS.WORKSPACE)
      .put(MetastoreColumn.TABLE_NAME, Tables.ROW_GROUPS.TABLE_NAME)
      .put(MetastoreColumn.METADATA_KEY, Tables.ROW_GROUPS.METADATA_KEY)
      .put(MetastoreColumn.METADATA_IDENTIFIER, Tables.ROW_GROUPS.METADATA_IDENTIFIER)
      .put(MetastoreColumn.METADATA_TYPE, Tables.ROW_GROUPS.METADATA_TYPE)
      .put(MetastoreColumn.LOCATION, Tables.ROW_GROUPS.LOCATION)
      .put(MetastoreColumn.SCHEMA, Tables.ROW_GROUPS.SCHEMA)
      .put(MetastoreColumn.COLUMNS_STATISTICS, Tables.ROW_GROUPS.COLUMN_STATISTICS)
      .put(MetastoreColumn.METADATA_STATISTICS, Tables.ROW_GROUPS.METADATA_STATISTICS)
      .put(MetastoreColumn.PATH, Tables.ROW_GROUPS.PATH)
      .put(MetastoreColumn.ROW_GROUP_INDEX, Tables.ROW_GROUPS.ROW_GROUP_INDEX)
      .put(MetastoreColumn.HOST_AFFINITY, Tables.ROW_GROUPS.HOST_AFFINITY)
      .put(MetastoreColumn.LAST_MODIFIED_TIME, Tables.ROW_GROUPS.LAST_MODIFIED_TIME)
      .put(MetastoreColumn.ADDITIONAL_METADATA, Tables.ROW_GROUPS.ADDITIONAL_METADATA)
      .build();

    private static final RdbmsFilterExpressionVisitor FILTER_VISITOR = new RdbmsFilterExpressionVisitor(COLUMNS_MAP);

    public static RowGroupMapper get() {
      return INSTANCE;
    }

    @Override
    public Table<RowGroupsRecord> table() {
      return Tables.ROW_GROUPS;
    }

    @Override
    public TableMetadataUnit toUnit(Record record) {
      RowGroupsRecord rowGroupsRecord = (RowGroupsRecord) record;
      return TableMetadataUnit.builder()
        .storagePlugin(rowGroupsRecord.getStoragePlugin())
        .workspace(rowGroupsRecord.getWorkspace())
        .tableName(rowGroupsRecord.getTableName())
        .metadataKey(rowGroupsRecord.getMetadataKey())
        .metadataIdentifier(rowGroupsRecord.getMetadataIdentifier())
        .metadataType(rowGroupsRecord.getMetadataType())
        .location(rowGroupsRecord.getLocation())
        .schema(rowGroupsRecord.getSchema())
        .columnsStatistics(ConverterUtil.convertToMapStringString(rowGroupsRecord.getColumnStatistics()))
        .metadataStatistics(ConverterUtil.convertToListString(rowGroupsRecord.getMetadataStatistics()))
        .path(rowGroupsRecord.getPath())
        .rowGroupIndex(rowGroupsRecord.getRowGroupIndex())
        .hostAffinity(ConverterUtil.convertToMapStringFloat(rowGroupsRecord.getHostAffinity()))
        .lastModifiedTime(rowGroupsRecord.getLastModifiedTime())
        .additionalMetadata(rowGroupsRecord.getAdditionalMetadata())
        .build();
    }

    @Override
    public RowGroupsRecord toRecord(TableMetadataUnit unit) {
      RowGroupsRecord record = new RowGroupsRecord();
      record.setStoragePlugin(unit.storagePlugin());
      record.setWorkspace(unit.workspace());
      record.setTableName(unit.tableName());
      record.setMetadataKey(unit.metadataKey());
      record.setMetadataIdentifier(unit.metadataIdentifier());
      record.setMetadataType(unit.metadataType());
      record.setLocation(unit.location());
      record.setSchema(unit.schema());
      record.setColumnStatistics(ConverterUtil.convertToString(unit.columnsStatistics()));
      record.setMetadataStatistics(ConverterUtil.convertToString(unit.metadataStatistics()));
      record.setPath(unit.path());
      record.setRowGroupIndex(unit.rowGroupIndex());
      record.setHostAffinity(ConverterUtil.convertToString(unit.hostAffinity()));
      record.setLastModifiedTime(unit.lastModifiedTime());
      record.setAdditionalMetadata(unit.additionalMetadata());
      return record;
    }

    @Override
    protected Map<MetastoreColumn, Field<?>> fieldMapper() {
      return COLUMNS_MAP;
    }

    @Override
    protected RdbmsFilterExpressionVisitor filterVisitor() {
      return FILTER_VISITOR;
    }

    @Override
    protected Function<TableMetadataUnit, List<String>> partitionKey() {
      return COMPONENT_PARTITION_KEY;
    }

    @Override
    protected List<Condition> toConditions(List<String> values) {
      assert values.size() == 4;
      return Arrays.asList(
        Tables.ROW_GROUPS.STORAGE_PLUGIN.eq(values.get(0)),
        Tables.ROW_GROUPS.WORKSPACE.eq(values.get(1)),
        Tables.ROW_GROUPS.TABLE_NAME.eq(values.get(2)),
        Tables.ROW_GROUPS.METADATA_KEY.eq(values.get(3)));
    }
  }

  /**
   * {@link TablesMetadataMapper} implementation for {@link Tables#PARTITIONS} table.
   */
  public static class PartitionMapper extends TablesMetadataMapper<PartitionsRecord> {

    private static final PartitionMapper INSTANCE = new PartitionMapper();

    private static final Map<MetastoreColumn, Field<?>> COLUMNS_MAP = ImmutableMap.<MetastoreColumn, Field<?>>builder()
      .put(MetastoreColumn.STORAGE_PLUGIN, Tables.PARTITIONS.STORAGE_PLUGIN)
      .put(MetastoreColumn.WORKSPACE, Tables.PARTITIONS.WORKSPACE)
      .put(MetastoreColumn.TABLE_NAME, Tables.PARTITIONS.TABLE_NAME)
      .put(MetastoreColumn.METADATA_KEY, Tables.PARTITIONS.METADATA_KEY)
      .put(MetastoreColumn.METADATA_IDENTIFIER, Tables.PARTITIONS.METADATA_IDENTIFIER)
      .put(MetastoreColumn.METADATA_TYPE, Tables.PARTITIONS.METADATA_TYPE)
      .put(MetastoreColumn.SCHEMA, Tables.PARTITIONS.SCHEMA)
      .put(MetastoreColumn.COLUMNS_STATISTICS, Tables.PARTITIONS.COLUMN_STATISTICS)
      .put(MetastoreColumn.METADATA_STATISTICS, Tables.PARTITIONS.METADATA_STATISTICS)
      .put(MetastoreColumn.COLUMN, Tables.PARTITIONS.COLUMN)
      .put(MetastoreColumn.LOCATIONS, Tables.PARTITIONS.LOCATIONS)
      .put(MetastoreColumn.PARTITION_VALUES, Tables.PARTITIONS.PARTITION_VALUES)
      .put(MetastoreColumn.LAST_MODIFIED_TIME, Tables.PARTITIONS.LAST_MODIFIED_TIME)
      .put(MetastoreColumn.ADDITIONAL_METADATA, Tables.PARTITIONS.ADDITIONAL_METADATA)
      .build();

    private static final RdbmsFilterExpressionVisitor FILTER_VISITOR = new RdbmsFilterExpressionVisitor(COLUMNS_MAP);

    public static PartitionMapper get() {
      return INSTANCE;
    }

    @Override
    public Table<PartitionsRecord> table() {
      return Tables.PARTITIONS;
    }

    @Override
    public TableMetadataUnit toUnit(Record record) {
      PartitionsRecord partitionsRecord = (PartitionsRecord) record;
      return TableMetadataUnit.builder()
        .storagePlugin(partitionsRecord.getStoragePlugin())
        .workspace(partitionsRecord.getWorkspace())
        .tableName(partitionsRecord.getTableName())
        .metadataKey(partitionsRecord.getMetadataKey())
        .metadataIdentifier(partitionsRecord.getMetadataIdentifier())
        .metadataType(partitionsRecord.getMetadataType())
        .schema(partitionsRecord.getSchema())
        .columnsStatistics(ConverterUtil.convertToMapStringString(partitionsRecord.getColumnStatistics()))
        .metadataStatistics(ConverterUtil.convertToListString(partitionsRecord.getMetadataStatistics()))
        .column(partitionsRecord.getColumn())
        .locations(ConverterUtil.convertToListString(partitionsRecord.getLocations()))
        .partitionValues(ConverterUtil.convertToListString(partitionsRecord.getPartitionValues()))
        .lastModifiedTime(partitionsRecord.getLastModifiedTime())
        .additionalMetadata(partitionsRecord.getAdditionalMetadata())
        .build();
    }

    @Override
    public PartitionsRecord toRecord(TableMetadataUnit unit) {
      PartitionsRecord record = new PartitionsRecord();
      record.setStoragePlugin(unit.storagePlugin());
      record.setWorkspace(unit.workspace());
      record.setTableName(unit.tableName());
      record.setMetadataKey(unit.metadataKey());
      record.setMetadataIdentifier(unit.metadataIdentifier());
      record.setMetadataType(unit.metadataType());
      record.setSchema(unit.schema());
      record.setColumnStatistics(ConverterUtil.convertToString(unit.columnsStatistics()));
      record.setMetadataStatistics(ConverterUtil.convertToString(unit.metadataStatistics()));
      record.setColumn(unit.column());
      record.setLocations(ConverterUtil.convertToString(unit.locations()));
      record.setPartitionValues(ConverterUtil.convertToString(unit.partitionValues()));
      record.setLastModifiedTime(unit.lastModifiedTime());
      record.setAdditionalMetadata(unit.additionalMetadata());
      return record;
    }

    @Override
    protected Map<MetastoreColumn, Field<?>> fieldMapper() {
      return COLUMNS_MAP;
    }

    @Override
    protected RdbmsFilterExpressionVisitor filterVisitor() {
      return FILTER_VISITOR;
    }

    @Override
    protected Function<TableMetadataUnit, List<String>> partitionKey() {
      return COMPONENT_PARTITION_KEY;
    }

    @Override
    protected List<Condition> toConditions(List<String> values) {
      assert values.size() == 4;
      return Arrays.asList(
        Tables.PARTITIONS.STORAGE_PLUGIN.eq(values.get(0)),
        Tables.PARTITIONS.WORKSPACE.eq(values.get(1)),
        Tables.PARTITIONS.TABLE_NAME.eq(values.get(2)),
        Tables.PARTITIONS.METADATA_KEY.eq(values.get(3)));
    }
  }
}
