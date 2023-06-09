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
package org.apache.drill.metastore.components.tables;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides handy methods to retrieve Metastore Tables data for analysis.
 * Contains list of most frequent requests to the Metastore Tables without a need
 * to write filters and transformers from {@link TableMetadataUnit} class.
 */
public class BasicTablesRequests {

  private final Tables tables;

  public BasicTablesRequests(Tables tables) {
    this.tables = tables;
  }

  /**
   * Returns metastore table information, including metastore version and table last modified time.
   *
   * Schematic SQL request:
   * <pre>
   *   select lastModifiedTime from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey = 'GENERAL_INFO'
   *   and metadataType = 'TABLE'
   * </pre>
   *
   * @param tableInfo table information
   * @return {@link MetastoreTableInfo} instance
   */
  public MetastoreTableInfo metastoreTableInfo(TableInfo tableInfo) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE)
      .requestColumns(MetastoreColumn.LAST_MODIFIED_TIME)
      .build();

    long version = tables.metadata().version();
    TableMetadataUnit unit = retrieveSingleElement(request(requestMetadata));
    return MetastoreTableInfo.of(tableInfo, unit, version);
  }

  /**
   * Checks if given metastore table information is the same with current one.
   * If Metastore supports versioning, first checks metastore versions,
   * if metastore version did not change, it is assumed table metadata did not change as well.
   * If Metastore version has changed or Metastore does not support versioning,
   * retrieves current metastore table info and checks against given one.
   *
   * @param metastoreTableInfo metastore table information
   * @return true is metastore table information has changed, false otherwise
   */
  public boolean hasMetastoreTableInfoChanged(MetastoreTableInfo metastoreTableInfo) {
    if (tables.metadata().supportsVersioning()
      && metastoreTableInfo.metastoreVersion() == tables.metadata().version()) {
      return false;
    }

    MetastoreTableInfo current = metastoreTableInfo(metastoreTableInfo.tableInfo());
    return metastoreTableInfo.hasChanged(current.isExists(), current.lastModifiedTime());
  }

  /**
   * Returns tables general information metadata based on given filter.
   * For example, can return list of tables that belong to particular storage plugin or
   * storage plugin and workspace combination.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$TABLE_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp'
   *   and metadataKey = 'GENERAL_INFO'
   *   and metadataType = 'TABLE'
   * </pre>
   *
   * @param filter filter expression
   * @return list of table metadata
   */
  public List<BaseTableMetadata> tablesMetadata(FilterExpression filter) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .customFilter(filter)
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE)
      .requestColumns(TableMetadataUnit.SCHEMA.tableColumns())
      .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.tables(units);
  }

  /**
   * Returns table general information metadata based on given table information.
   * Expects only one qualified result, otherwise will fail.
   * If no data is returned, will return null.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$TABLE_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey = 'GENERAL_INFO'
   *   and metadataType = 'TABLE'
   * </pre>
   *
   * @param tableInfo table information
   * @return table metadata
   */
  public BaseTableMetadata tableMetadata(TableInfo tableInfo) {
    List<BaseTableMetadata> tables = tablesMetadata(tableInfo.toFilter());
    return retrieveSingleElement(tables);
  }

  /**
   * Returns segments metadata based on given table information, locations and column name.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$SEGMENT_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and location in ('.../part_int=3/d3', '.../part_int=3/d4')
   *   and metadataKey = 'part_int=3'
   *   and metadataType = 'SEGMENT'
   * </pre>
   *
   * @param tableInfo table information
   * @param locations segments locations
   * @param metadataKey metadata key
   * @return list of segment metadata
   */
  public List<SegmentMetadata> segmentsMetadataByMetadataKey(TableInfo tableInfo, List<String> locations, String metadataKey) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .locations(locations)
      .metadataKey(metadataKey)
      .metadataType(MetadataType.SEGMENT)
      .requestColumns(TableMetadataUnit.SCHEMA.segmentColumns())
      .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.segments(units);
  }

  /**
   * Returns segments metadata based on given table information, locations and column name.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$SEGMENT_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and location in ('.../dir0', '.../dir1')
   *   and column = 'n_nation'
   *   and metadataType = 'SEGMENT'
   * </pre>
   *
   * @param tableInfo table information
   * @param locations segments locations
   * @param column column name
   * @return list of segment metadata
   */
  public List<SegmentMetadata> segmentsMetadataByColumn(TableInfo tableInfo, List<String> locations, String column) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .locations(locations)
      .column(column)
      .metadataType(MetadataType.SEGMENT)
      .requestColumns(TableMetadataUnit.SCHEMA.segmentColumns())
      .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.segments(units);
  }

  /**
   * Returns segments metadata based on the given table information and metadata identifiers.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$SEGMENT_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and identifier in ('part_int=3', …)
   *   and metadataType = 'SEGMENT'
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataInfos list of MetadataInfo for required segments to obtain
   * @return list of segment metadata
   */
  public List<SegmentMetadata> segmentsMetadata(TableInfo tableInfo, List<MetadataInfo> metadataInfos) {
    List<String> keys = metadataInfos.stream()
        .map(MetadataInfo::key)
        .collect(Collectors.toList());
    List<String> identifiers = metadataInfos.stream()
        .map(MetadataInfo::identifier)
        .collect(Collectors.toList());

    RequestMetadata requestMetadata = RequestMetadata.builder()
        .tableInfo(tableInfo)
        .metadataKeys(keys)
        .identifiers(identifiers)
        .metadataType(MetadataType.SEGMENT)
        .requestColumns(TableMetadataUnit.SCHEMA.segmentColumns())
        .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.segments(units);
  }

  /**
   * Returns list of {@link TableMetadataUnit} metadata based on the given table information, and metadata identifiers.
   *
   * Schematic SQL request:
   * <pre>
   *   select * from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and identifier in ('part_int=3', …)
   *   and metadataType in ('SEGMENT', …)
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataInfos list of MetadataInfo for required metadata to obtain
   * @return list of metadata
   */
  public List<TableMetadataUnit> metadata(TableInfo tableInfo, Collection<MetadataInfo> metadataInfos) {
    List<String> keys = metadataInfos.stream()
        .map(MetadataInfo::key)
        .collect(Collectors.toList());
    List<String> identifiers = metadataInfos.stream()
        .map(MetadataInfo::identifier)
        .collect(Collectors.toList());

    Set<MetadataType> metadataTypes = metadataInfos.stream()
        .map(MetadataInfo::type)
        .collect(Collectors.toSet());

    RequestMetadata requestMetadata = RequestMetadata.builder()
        .tableInfo(tableInfo)
        .metadataKeys(keys)
        .identifiers(identifiers)
        .metadataTypes(metadataTypes)
        .build();

    return request(requestMetadata);
  }

  /**
   * Returns partitions metadata based on given table information, metadata keys and column name.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$PARTITION_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey in ('part_int=3', 'part_int=4')
   *   and column = 'n_nation'
   *   and metadataType = 'PARTITION'
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataKeys list of metadata keys
   * @param column partition column
   * @return list of partition metadata
   */
  public List<PartitionMetadata> partitionsMetadata(TableInfo tableInfo, List<String> metadataKeys, String column) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .metadataKeys(metadataKeys)
      .column(column)
      .metadataType(MetadataType.PARTITION)
      .requestColumns(TableMetadataUnit.SCHEMA.partitionColumns())
      .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.partitions(units);
  }

  /**
   * Returns files metadata based on given table information, metadata key and files paths.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$FILE_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey = 'part_int=3'
   *   and path in ('/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet', …)
   *   and metadataType = 'FILE'
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataKey metadata key
   * @param paths list of full file paths
   * @return list of files metadata
   */
  public List<FileMetadata> filesMetadata(TableInfo tableInfo, String metadataKey, List<String> paths) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .metadataKey(metadataKey)
      .paths(paths)
      .metadataType(MetadataType.FILE)
      .requestColumns(TableMetadataUnit.SCHEMA.fileColumns())
      .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.files(units);
  }

  /**
   * Returns files metadata based on the given table information and metadata keys.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$FILE_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and identifier = in ('part_int=3', …)
   *   and metadataType = 'FILE'
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataInfos list of MetadataInfo for required row groups to obtain
   * @return list of row group metadata
   */
  public List<FileMetadata> filesMetadata(TableInfo tableInfo, List<MetadataInfo> metadataInfos) {
    List<String> keys = metadataInfos.stream()
        .map(MetadataInfo::key)
        .collect(Collectors.toList());
    List<String> identifiers = metadataInfos.stream()
        .map(MetadataInfo::identifier)
        .collect(Collectors.toList());

    RequestMetadata requestMetadata = RequestMetadata.builder()
        .tableInfo(tableInfo)
        .metadataKeys(keys)
        .identifiers(identifiers)
        .metadataType(MetadataType.FILE)
        .requestColumns(TableMetadataUnit.SCHEMA.fileColumns())
        .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.files(units);
  }

  /**
   * Returns file metadata based on given table information, metadata key and full path.
   * Expects only one qualified result, otherwise will fail.
   * If no data is returned, will return null.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$FILE_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey = 'part_int=3'
   *   and path = '/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet'
   *   and metadataType = 'FILE'
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataKey metadata key
   * @param path full file path
   * @return list of files metadata
   */
  public FileMetadata fileMetadata(TableInfo tableInfo, String metadataKey, String path) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .metadataKey(metadataKey)
      .path(path)
      .metadataType(MetadataType.FILE)
      .requestColumns(TableMetadataUnit.SCHEMA.fileColumns())
      .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return retrieveSingleElement(BasicTablesTransformer.files(units));
  }

  /**
   * Returns row groups metadata based on given table information, metadata key and location.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$ROW_GROUP_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey = 'part_int=3'
   *   and path = '/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet'
   *   and metadataType = 'ROW_GROUP'
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataKey metadata key
   * @param path full path to the file of the row group
   * @return list of row group metadata
   */
  public List<RowGroupMetadata> rowGroupsMetadata(TableInfo tableInfo, String metadataKey, String path) {
   RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .metadataKey(metadataKey)
      .path(path)
      .metadataType(MetadataType.ROW_GROUP)
      .requestColumns(TableMetadataUnit.SCHEMA.rowGroupColumns())
      .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.rowGroups(units);
  }

  /**
   * Returns row groups metadata based on the given table information, metadata keys and locations.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$ROW_GROUP_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey in ('part_int=3', …)
   *   and path in ('/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet', …)
   *   and metadataType = 'ROW_GROUP'
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataKeys metadata key
   * @param paths list of full paths to the file of the row group
   * @return list of row group metadata
   */
  public List<RowGroupMetadata> rowGroupsMetadata(TableInfo tableInfo, List<String> metadataKeys, List<String> paths) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
        .tableInfo(tableInfo)
        .metadataKeys(metadataKeys)
        .paths(paths)
        .metadataType(MetadataType.ROW_GROUP)
        .requestColumns(TableMetadataUnit.SCHEMA.rowGroupColumns())
        .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.rowGroups(units);
  }

  /**
   * Returns row groups metadata based on given table information and metadata identifiers.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$ROW_GROUP_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and identifier in ('part_int=3', …)
   *   and metadataType = 'ROW_GROUP'
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataInfos list of MetadataInfo for required row groups to obtain
   * @return list of row group metadata
   */
  public List<RowGroupMetadata> rowGroupsMetadata(TableInfo tableInfo, List<MetadataInfo> metadataInfos) {
    List<String> keys = metadataInfos.stream()
        .map(MetadataInfo::key)
        .collect(Collectors.toList());
    List<String> identifiers = metadataInfos.stream()
        .map(MetadataInfo::identifier)
        .collect(Collectors.toList());

    RequestMetadata requestMetadata = RequestMetadata.builder()
        .tableInfo(tableInfo)
        .metadataKeys(keys)
        .identifiers(identifiers)
        .metadataType(MetadataType.ROW_GROUP)
        .requestColumns(TableMetadataUnit.SCHEMA.rowGroupColumns())
        .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.rowGroups(units);
  }

  /**
   * Returns metadata for segments, files and row groups based on given metadata keys and locations.
   *
   * Schematic SQL request:
   * <pre>
   *   select [$SEGMENT_METADATA$] / [$FILE_METADATA$] / [$ROW_GROUP_METADATA$] from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey in ('part_int=1', 'part_int=2', 'part_int=5')
   *   and location in ('.../dir0/d3', '.../dir0/d4', '.../part_int=3/d3', '.../part_int=4/d4')
   *   and metadataType in ('SEGMENT', 'FILE', 'ROW_GROUP')
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataKeys metadata keys
   * @param locations locations
   * @return list of segments / files / rows groups metadata in {@link BasicTablesTransformer.MetadataHolder} instance
   */
  public BasicTablesTransformer.MetadataHolder fullSegmentsMetadataWithoutPartitions(TableInfo tableInfo, List<String> metadataKeys, List<String> locations) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .metadataKeys(metadataKeys)
      .locations(locations)
      .metadataTypes(MetadataType.SEGMENT, MetadataType.FILE, MetadataType.ROW_GROUP)
      .build();

    List<TableMetadataUnit> units = request(requestMetadata);
    return BasicTablesTransformer.all(units);
  }

  /**
   * Returns map of file full paths and their last modified time.
   *
   * Schematic SQL request:
   * <pre>
   *   select path, lastModifiedTime from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey = 'part_int=3'
   *   and location in ('/tmp/nation/part_int=3/part_varchar=g', ...)
   *   and metadataType = 'FILE'
   * </pre>
   *
   * @param tableInfo table information
   * @param metadataKey metadata key
   * @param locations files locations
   * @return result map where key is file full path and value is file last modification time
   */
  public Map<String, Long> filesLastModifiedTime(TableInfo tableInfo, String metadataKey, List<String> locations) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .metadataKey(metadataKey)
      .locations(locations)
      .metadataType(MetadataType.FILE)
      .requestColumns(MetastoreColumn.PATH, MetastoreColumn.LAST_MODIFIED_TIME)
      .build();

    return request(requestMetadata).stream()
      .collect(Collectors.toMap(
        TableMetadataUnit::path,
        TableMetadataUnit::lastModifiedTime,
        (o, n) -> n));
  }

  /**
   * Returns map of segments metadata keys and their last modified time.
   *
   * Schematic SQL request:
   * <pre>
   *   select metadataKey, lastModifiedTime from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and location in ('.../dir0', '.../dir1')
   *   and metadataType = 'SEGMENT'
   * </pre>
   *
   * @param tableInfo table information
   * @param locations segments locations
   * @return result map where key is metadata key and value is its last modification time
   */
  public Map<String, Long> segmentsLastModifiedTime(TableInfo tableInfo, List<String> locations) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .locations(locations)
      .metadataType(MetadataType.SEGMENT)
      .requestColumns(MetastoreColumn.METADATA_KEY, MetastoreColumn.LAST_MODIFIED_TIME)
      .build();

    return request(requestMetadata).stream()
      .collect(Collectors.toMap(
        TableMetadataUnit::metadataKey,
        TableMetadataUnit::lastModifiedTime,
        (o, n) -> n));
  }

  /**
   * Returns tables interesting columns and partition keys based on given table information.
   * Expects only one qualified result, otherwise will fail.
   * If no data is returned, will return null.
   *
   * Schematic SQL request:
   * <pre>
   *   select interestingColumns, partitionKeys from METASTORE
   *   where storage = 'dfs' and workspace = 'tmp' and tableName = 'nation'
   *   and metadataKey = 'GENERAL_INFO'
   *   and metadataType = 'TABLE'
   * </pre>
   *
   * @param tableInfo table information
   * @return {@link TableMetadataUnit} instance with set interesting columns and partition keys if present
   */
  public TableMetadataUnit interestingColumnsAndPartitionKeys(TableInfo tableInfo) {
    RequestMetadata requestMetadata = RequestMetadata.builder()
      .tableInfo(tableInfo)
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE)
      .requestColumns(MetastoreColumn.INTERESTING_COLUMNS, MetastoreColumn.PARTITION_KEYS)
      .build();

    return retrieveSingleElement(request(requestMetadata));
  }

  /**
   * Executes Metastore Tables read request based on given information in {@link RequestMetadata}.
   *
   * @param requestMetadata request metadata
   * @return list of metadata units
   */
  public List<TableMetadataUnit> request(RequestMetadata requestMetadata) {
    return tables.read()
      .metadataTypes(requestMetadata.metadataTypes())
      .filter(requestMetadata.filter())
      .columns(requestMetadata.columns())
      .execute();
  }

  /**
   * Retrieves one element from the given list of elements.
   * If given list of elements is null or empty, returns null.
   * Will fail, if given list of elements contains more than one element.
   *
   * @param elements list of elements
   * @param <T> elements type
   * @return single element
   * @throws IllegalArgumentException if given more than one element
   */
  private <T> T retrieveSingleElement(List<T> elements) {
    if (elements == null || elements.isEmpty()) {
      return null;
    }
    if (elements.size() > 1) {
      throw new IllegalArgumentException(String.format("Expected one element but received [%s]", elements.size()));
    }
    return elements.get(0);
  }

  /**
   * Request metadata holder that provides request metadata types, filters and columns.
   * Combines given filters using {@link FilterExpression.Operator#AND} operator.
   * Supports only {@link FilterExpression.Operator#EQUAL} and {@link FilterExpression.Operator#IN}
   * operators for predefined filter references, for other cases custom filter can be used.
   */
  public static class RequestMetadata {

    private Set<MetadataType> metadataTypes;
    private final FilterExpression filter;
    private final List<MetastoreColumn> columns;

    private RequestMetadata(Set<MetadataType> metadataTypes, FilterExpression filter, List<MetastoreColumn> columns) {
      this.metadataTypes = metadataTypes;
      this.filter = filter;
      this.columns = columns;
    }

    public Set<MetadataType> metadataTypes() {
      return metadataTypes;
    }

    public FilterExpression filter() {
      return filter;
    }

    public List<MetastoreColumn> columns() {
      return columns;
    }

    public static RequestMetadata.Builder builder() {
      return new RequestMetadata.Builder();
    }

    public static class Builder {

      private TableInfo tableInfo;
      private String location;
      private List<String> locations;
      private String column;
      private String metadataKey;
      private List<String> metadataKeys;
      private String path;
      private List<String> paths;
      private List<String> identifiers;
      private FilterExpression customFilter;
      private Set<MetadataType> metadataTypes = new HashSet<>();
      private final List<MetastoreColumn> requestColumns = new ArrayList<>();

      public RequestMetadata.Builder metadataType(MetadataType metadataType) {
        this.metadataTypes.add(metadataType);
        return this;
      }

      public RequestMetadata.Builder metadataTypes(MetadataType... metadataTypes) {
        this.metadataTypes.addAll(Sets.newHashSet(metadataTypes));
        return this;
      }

      public RequestMetadata.Builder metadataTypes(Set<MetadataType> metadataTypes) {
        this.metadataTypes.addAll(metadataTypes);
        return this;
      }

      public RequestMetadata.Builder tableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
        return this;
      }

      public RequestMetadata.Builder location(String location) {
        this.location = location;
        return this;
      }

      public RequestMetadata.Builder locations(List<String> locations) {
        this.locations = locations;
        return this;
      }

      public RequestMetadata.Builder column(String column) {
        this.column = column;
        return this;
      }

      public RequestMetadata.Builder metadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
        return this;
      }

      public RequestMetadata.Builder metadataKeys(List<String> metadataKeys) {
        this.metadataKeys = metadataKeys;
        return this;
      }

      public RequestMetadata.Builder path(String path) {
        this.path = path;
        return this;
      }

      public RequestMetadata.Builder paths(List<String> paths) {
        this.paths = paths;
        return this;
      }

      public RequestMetadata.Builder identifiers(List<String> identifiers) {
        this.identifiers = identifiers;
        return this;
      }

      public RequestMetadata.Builder customFilter(FilterExpression customFilter) {
        this.customFilter = customFilter;
        return this;
      }

      public RequestMetadata.Builder requestColumns(List<MetastoreColumn> requestColumns) {
        this.requestColumns.addAll(requestColumns);
        return this;
      }

      public RequestMetadata.Builder requestColumns(MetastoreColumn... requestColumns) {
        return requestColumns(Arrays.asList(requestColumns));
      }

      public RequestMetadata build() {
        return new RequestMetadata(metadataTypes, createFilter(), requestColumns);
      }

      private FilterExpression createFilter() {
        List<FilterExpression> filters = new ArrayList<>();
        if (tableInfo != null) {
          filters.add(tableInfo.toFilter());
        }
        addFilter(MetastoreColumn.LOCATION, location, filters);
        addFilter(MetastoreColumn.LOCATION, locations, filters);
        addFilter(MetastoreColumn.COLUMN, column, filters);
        addFilter(MetastoreColumn.METADATA_KEY, metadataKey, filters);
        addFilter(MetastoreColumn.METADATA_KEY, metadataKeys, filters);
        addFilter(MetastoreColumn.PATH, path, filters);
        addFilter(MetastoreColumn.PATH, paths, filters);
        addFilter(MetastoreColumn.METADATA_IDENTIFIER, identifiers, filters);
        if (customFilter != null) {
          filters.add(customFilter);
        }

        if (filters.isEmpty()) {
          return null;
        }

        if (filters.size() == 1) {
          return filters.get(0);
        }

        return FilterExpression.and(filters.get(0), filters.get(1),
          filters.subList(2, filters.size()).toArray(new FilterExpression[0]));
      }

      /**
       * Creates filter based on given parameters and adds to the given list of filters.
       * If given filter value is null, does nothing. If given filter value is List and is not empty,
       * creates {@link FilterExpression.Operator#IN} filter, if List is empty, does nothing.
       * For all other cases, creates {@link FilterExpression.Operator#EQUAL} filter.
       *
       * @param column Metastore column to which filter will be applied
       * @param value filter value
       * @param filters current list of filters
       */
      private <T> void addFilter(MetastoreColumn column, T value, List<FilterExpression> filters) {
        if (value == null) {
          return;
        }

        if (value instanceof List) {
          List<?> list = (List<?>) value;
          if (list.isEmpty()) {
            return;
          }
          filters.add(FilterExpression.in(column, list));
          return;
        }

        filters.add(FilterExpression.equal(column, value));
      }
    }
  }
}
