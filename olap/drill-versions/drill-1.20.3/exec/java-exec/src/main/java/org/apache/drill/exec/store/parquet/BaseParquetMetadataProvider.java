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
package org.apache.drill.exec.store.parquet;

import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.metastore.store.parquet.ParquetMetadataProvider;
import org.apache.drill.exec.metastore.store.parquet.ParquetMetadataProviderBuilder;
import org.apache.drill.exec.record.metadata.schema.SchemaProvider;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.metastore.metadata.LocationProvider;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.metastore.metadata.BaseMetadata;
import org.apache.drill.metastore.metadata.NonInterestingColumnsMetadata;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.metadata.TableMetadata;
import org.apache.drill.metastore.util.TableMetadataUtils;
import org.apache.drill.shaded.guava.com.google.common.collect.HashBasedTable;
import org.apache.drill.shaded.guava.com.google.common.collect.HashMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.LinkedListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Table;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.metastore.util.SchemaPathUtils;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.store.dfs.ReadEntryWithPath;
import org.apache.drill.exec.store.parquet.metadata.MetadataBase;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.drill.exec.store.parquet.ParquetTableMetadataUtils.PARQUET_COLUMN_STATISTICS;

/**
 * Implementation of {@link ParquetMetadataProvider} which contains base methods for obtaining metadata from
 * parquet statistics.
 */
public abstract class BaseParquetMetadataProvider implements ParquetMetadataProvider {
  private static final Logger logger = LoggerFactory.getLogger(BaseParquetMetadataProvider.class);

  /**
   * {@link HashBasedTable} cannot contain nulls, used this object to represent null values.
   */
  public static final Object NULL_VALUE = new Object();

  protected final List<ReadEntryWithPath> entries;
  protected final ParquetReaderConfig readerConfig;

  protected final String tableName;
  protected final Path tableLocation;

  private ParquetGroupScanStatistics<? extends BaseMetadata> parquetGroupScanStatistics;
  protected MetadataBase.ParquetTableMetadataBase parquetTableMetadata;
  protected Set<Path> fileSet;
  protected TupleMetadata schema;
  protected DrillStatsTable statsTable;

  private List<SchemaPath> partitionColumns;

  private Multimap<Path, RowGroupMetadata> rowGroups;
  private TableMetadata tableMetadata;
  private List<PartitionMetadata> partitions;
  private Map<Path, FileMetadata> files;
  private Map<Path, SegmentMetadata> segments;
  private NonInterestingColumnsMetadata nonInterestingColumnsMetadata;

  // whether metadata for row groups should be collected to create files, partitions and table metadata
  private final boolean collectMetadata = false;

  protected BaseParquetMetadataProvider(Builder<?> builder) {
    if (builder.entries != null) {
      // reuse previously stored metadata
      this.entries = builder.entries;
      this.tableName = builder.selectionRoot != null ? builder.selectionRoot.toUri().getPath() : "";
      this.tableLocation = builder.selectionRoot;
    } else if (builder.selection != null) {
      this.entries = new ArrayList<>();
      this.tableName = builder.selection.getSelectionRoot() != null ? builder.selection.getSelectionRoot().toUri().getPath() : "";
      this.tableLocation = builder.selection.getSelectionRoot();
    } else {
      // case of hive parquet table
      this.entries = new ArrayList<>();
      this.tableName = null;
      this.tableLocation = null;
    }

    SchemaProvider schemaProvider = builder.metadataProviderManager.getSchemaProvider();
    TupleMetadata schema = builder.schema;
    // schema passed into the builder has greater priority
    if (schema == null && schemaProvider != null) {
      try {
        schema = schemaProvider.read().getSchema();
      } catch (IOException e) {
        logger.warn("Unable to read schema from schema provider [{}]: {}.\n" +
                "Query execution will continue without using the schema.",
            tableLocation, e.getMessage());
        logger.trace("Error when reading the schema", e);
      }
    }

    this.readerConfig = builder.readerConfig == null ? ParquetReaderConfig.getDefaultInstance() : builder.readerConfig;
    this.schema = schema;
    this.statsTable = builder.metadataProviderManager.getStatsProvider();
  }

  protected void init(BaseParquetMetadataProvider metadataProvider) throws IOException {
    // Once deserialization for metadata is provided, initInternal() call should be removed
    // and only files list is deserialized based on specified locations
    initInternal();

    assert parquetTableMetadata != null;

    if (fileSet == null) {
      fileSet = new HashSet<>();
      fileSet.addAll(parquetTableMetadata.getFiles().stream()
          .map(MetadataBase.ParquetFileMetadata::getPath)
          .collect(Collectors.toSet()));
    }

    List<Path> fileLocations = getLocations();

    // if metadata provider wasn't specified, or required metadata is absent,
    // obtains metadata from cache files or table footers
    if (metadataProvider == null
        || (metadataProvider.rowGroups != null && !metadataProvider.rowGroups.keySet().containsAll(fileLocations))
        || (metadataProvider.files != null && !metadataProvider.files.keySet().containsAll(fileLocations))) {
      initializeMetadata();
    } else {
      // reuse metadata from existing TableMetadataProvider
      if (metadataProvider.files != null && metadataProvider.files.size() != files.size()) {
        files = metadataProvider.files.entrySet().stream()
            .filter(entry -> fileLocations.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      }
      if (metadataProvider.rowGroups != null) {
        rowGroups = LinkedListMultimap.create();
        metadataProvider.rowGroups.entries().stream()
            .filter(entry -> fileLocations.contains(entry.getKey()))
            .forEach(entry -> rowGroups.put(entry.getKey(), entry.getValue()));
      }
      TableMetadata tableMetadata = getTableMetadata();
      getSegmentsMetadataMap();
      getPartitionsMetadata();
      getRowGroupsMeta();
      getNonInterestingColumnsMetadata();
      this.tableMetadata = TableMetadataUtils.updateRowCount(tableMetadata, getRowGroupsMeta());
      parquetTableMetadata = null;
    }
  }

  /**
   * Method which initializes all metadata kinds to get rid of parquetTableMetadata.
   * Once deserialization and serialization from/into metastore classes is done, this method should be removed
   * to allow lazy initialization.
   */
  public void initializeMetadata() {
    if (statsTable != null && !statsTable.isMaterialized()) {
      statsTable.materialize();
    }
    getTableMetadata();
    getSegmentsMetadataMap();
    getFilesMetadataMap();
    getPartitionsMetadata();
    getRowGroupsMeta();
    getNonInterestingColumnsMetadata();
    parquetTableMetadata = null;
  }

  @Override
  public NonInterestingColumnsMetadata getNonInterestingColumnsMetadata() {
    if (nonInterestingColumnsMetadata == null) {
      nonInterestingColumnsMetadata = ParquetTableMetadataUtils.getNonInterestingColumnsMeta(parquetTableMetadata);
    }
    return nonInterestingColumnsMetadata;
  }

  @Override
  public TableMetadata getTableMetadata() {
    if (tableMetadata == null) {
      List<StatisticsHolder<?>> tableStatistics = new ArrayList<>(DrillStatsTable.getEstimatedTableStats(statsTable));
      Map<SchemaPath, TypeProtos.MajorType> fields = ParquetTableMetadataUtils.resolveFields(parquetTableMetadata);
      Map<SchemaPath, TypeProtos.MajorType> intermediateFields = ParquetTableMetadataUtils.resolveIntermediateFields(parquetTableMetadata);

      if (this.schema == null) {
        schema = new TupleSchema();
        fields.forEach((schemaPath, majorType) -> SchemaPathUtils.addColumnMetadata(schema, schemaPath, majorType, intermediateFields));
      } else {
        // merges specified schema with schema from table
        fields.forEach((schemaPath, majorType) -> {
          if (SchemaPathUtils.getColumnMetadata(schemaPath, schema) == null) {
            SchemaPathUtils.addColumnMetadata(schema, schemaPath, majorType, intermediateFields);
          }
        });
      }

      Map<SchemaPath, ColumnStatistics<?>> columnsStatistics;
      if (collectMetadata) {
        Collection<? extends BaseMetadata> metadata = getFilesMetadataMap().values();
        if (metadata.isEmpty()) {
          metadata = getRowGroupsMeta();
        }
        tableStatistics.add(new StatisticsHolder<>(TableStatisticsKind.ROW_COUNT.mergeStatistics(metadata), TableStatisticsKind.ROW_COUNT));
        columnsStatistics = TableMetadataUtils.mergeColumnsStatistics(metadata, fields.keySet(), PARQUET_COLUMN_STATISTICS);
      } else {
        columnsStatistics = new HashMap<>();
        tableStatistics.add(new StatisticsHolder<>(getParquetGroupScanStatistics().getRowCount(), TableStatisticsKind.ROW_COUNT));

        Set<SchemaPath> unhandledColumns = new HashSet<>();
        if (statsTable != null && statsTable.isMaterialized()) {
          unhandledColumns.addAll(statsTable.getColumns());
        }

        fields.forEach((columnPath, value) -> {
          long columnValueCount = getParquetGroupScanStatistics().getColumnValueCount(columnPath);
          // Adds statistics values itself if statistics is available
          List<StatisticsHolder<?>> stats = new ArrayList<>(DrillStatsTable.getEstimatedColumnStats(statsTable, columnPath));
          unhandledColumns.remove(columnPath);

          // adds statistics for partition columns
          stats.add(new StatisticsHolder<>(columnValueCount, TableStatisticsKind.ROW_COUNT));
          stats.add(new StatisticsHolder<>(getParquetGroupScanStatistics().getRowCount() - columnValueCount, ColumnStatisticsKind.NULLS_COUNT));
          columnsStatistics.put(columnPath, new ColumnStatistics<>(stats, value.getMinorType()));
        });

        for (SchemaPath column : unhandledColumns) {
          columnsStatistics.put(column,
              new ColumnStatistics<>(DrillStatsTable.getEstimatedColumnStats(statsTable, column)));
        }
      }
      MetadataInfo metadataInfo = MetadataInfo.builder().type(MetadataType.TABLE).build();
      tableMetadata = BaseTableMetadata.builder()
          .tableInfo(TableInfo.UNKNOWN_TABLE_INFO)
          .metadataInfo(metadataInfo)
          .location(tableLocation)
          .schema(schema)
          .columnsStatistics(columnsStatistics)
          .metadataStatistics(tableStatistics)
          .partitionKeys(Collections.emptyMap())
          .build();
    }

    return tableMetadata;
  }

  private ParquetGroupScanStatistics<? extends BaseMetadata> getParquetGroupScanStatistics() {
    if (parquetGroupScanStatistics == null) {
      if (collectMetadata) {
        parquetGroupScanStatistics = new ParquetGroupScanStatistics<>(getFilesMetadataMap().values());
      } else {
        parquetGroupScanStatistics = new ParquetGroupScanStatistics<>(getRowGroupsMeta());
      }
    }
    return parquetGroupScanStatistics;
  }

  @Override
  public List<SchemaPath> getPartitionColumns() {
    if (partitionColumns == null) {
      partitionColumns = getParquetGroupScanStatistics().getPartitionColumns();
    }
    return partitionColumns;
  }

  @Override
  public List<PartitionMetadata> getPartitionsMetadata() {
    if (partitions == null) {
      partitions = new ArrayList<>();
      if (collectMetadata) {
        Table<SchemaPath, Object, List<FileMetadata>> colValFile = HashBasedTable.create();

        Collection<FileMetadata> filesMetadata = getFilesMetadataMap().values();
        partitionColumns = getParquetGroupScanStatistics().getPartitionColumns();
        for (FileMetadata fileMetadata : filesMetadata) {
          for (SchemaPath partitionColumn : partitionColumns) {
            Object partitionValue = getParquetGroupScanStatistics().getPartitionValue(fileMetadata.getPath(), partitionColumn);
            // Table cannot contain nulls
            partitionValue = partitionValue == null ? NULL_VALUE : partitionValue;
            List<FileMetadata> partitionFiles = colValFile.get(partitionColumn, partitionValue);
            if (partitionFiles == null) {
              partitionFiles = new ArrayList<>();
              colValFile.put(partitionColumn, partitionValue, partitionFiles);
            }
            partitionFiles.add(fileMetadata);
          }
        }

        for (SchemaPath logicalExpressions : colValFile.rowKeySet()) {
          for (List<FileMetadata> partValues : colValFile.row(logicalExpressions).values()) {
            partitions.add(ParquetTableMetadataUtils.getPartitionMetadata(logicalExpressions, partValues));
          }
        }
      } else {
        for (SchemaPath partitionColumn : getParquetGroupScanStatistics().getPartitionColumns()) {
          Map<Path, Object> partitionPaths = getParquetGroupScanStatistics().getPartitionPaths(partitionColumn);
          Multimap<Object, Path> partitionsForValue = HashMultimap.create();

          partitionPaths.forEach((path, value) -> partitionsForValue.put(value, path));

          partitionsForValue.asMap().forEach((partitionKey, value) -> {
            Map<SchemaPath, ColumnStatistics<?>> columnsStatistics = new HashMap<>();

            List<StatisticsHolder<?>> statistics = new ArrayList<>();
            partitionKey = partitionKey == NULL_VALUE ? null : partitionKey;
            statistics.add(new StatisticsHolder<>(partitionKey, ColumnStatisticsKind.MIN_VALUE));
            statistics.add(new StatisticsHolder<>(partitionKey, ColumnStatisticsKind.MAX_VALUE));

            statistics.add(new StatisticsHolder<>(Statistic.NO_COLUMN_STATS, ColumnStatisticsKind.NULLS_COUNT));
            statistics.add(new StatisticsHolder<>(Statistic.NO_COLUMN_STATS, TableStatisticsKind.ROW_COUNT));
            columnsStatistics.put(partitionColumn,
                new ColumnStatistics<>(statistics,
                    getParquetGroupScanStatistics().getTypeForColumn(partitionColumn).getMinorType()));
            MetadataInfo metadataInfo = MetadataInfo.builder().type(MetadataType.PARTITION).build();
            TableMetadata tableMetadata = getTableMetadata();
            PartitionMetadata partitionMetadata = PartitionMetadata.builder()
                .tableInfo(tableMetadata.getTableInfo())
                .metadataInfo(metadataInfo)
                .column(partitionColumn)
                .schema(tableMetadata.getSchema())
                .columnsStatistics(columnsStatistics)
                .metadataStatistics(statistics)
                .partitionValues(Collections.emptyList())
                .locations(new HashSet<>(value))
                .build();

            partitions.add(partitionMetadata);
          });
        }
      }
    }
    return partitions;
  }

  @Override
  public List<PartitionMetadata> getPartitionMetadata(SchemaPath columnName) {
    return getPartitionsMetadata().stream()
        .filter(Objects::nonNull)
        .filter(partitionMetadata -> partitionMetadata.getColumn().equals(columnName))
        .collect(Collectors.toList());
  }

  @Override
  public FileMetadata getFileMetadata(Path location) {
    return getFilesMetadataMap().get(location);
  }

  @Override
  public List<FileMetadata> getFilesForPartition(PartitionMetadata partition) {
    return partition.getLocations().stream()
        .map(location -> getFilesMetadataMap().get(location))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unused")
  @Override
  public Map<Path, SegmentMetadata> getSegmentsMetadataMap() {
    if (segments == null) {
      if (entries.isEmpty() || !collectMetadata) {
        return Collections.emptyMap();
      }

      segments = new LinkedHashMap<>();

      Path fileLocation = getFilesMetadataMap().values().iterator().next().getPath();
      int levelsCount = fileLocation.depth() - tableLocation.depth();

      Map<Path, FileMetadata> filesMetadata = getFilesMetadataMap();
      int segmentsIndex = levelsCount - 1;
      Map<Path, SegmentMetadata> segmentMetadata = getSegmentsForMetadata(filesMetadata,
          SchemaPath.getSimplePath(MetadataInfo.DEFAULT_COLUMN_PREFIX + segmentsIndex));
      segments.putAll(segmentMetadata);
      for (int i = segmentsIndex - 1; i >= 0; i--) {
        String segmentColumn = MetadataInfo.DEFAULT_COLUMN_PREFIX + i;
        segmentMetadata = getMetadataForSegments(segmentMetadata,
            SchemaPath.getSimplePath(segmentColumn));
        segments.putAll(segmentMetadata);
      }

    }
    return segments;
  }

  private static <T extends BaseMetadata & LocationProvider> Map<Path, SegmentMetadata> getSegmentsForMetadata(
      Map<Path, T> metadata, SchemaPath column) {
    Multimap<Path, T> metadataMultimap = LinkedListMultimap.create();
    metadata.forEach((key, value) -> metadataMultimap.put(key.getParent(), value));

    Map<Path, SegmentMetadata> result = new HashMap<>();
    metadataMultimap.asMap().forEach((key, value) -> result.put(key, combineToSegmentMetadata(value, column)));

    return result;
  }

  private static Map<Path, SegmentMetadata> getMetadataForSegments(Map<Path, SegmentMetadata> metadata, SchemaPath column) {
    Multimap<Path, SegmentMetadata> metadataMultimap = LinkedListMultimap.create();
    metadata.forEach((key, value) -> metadataMultimap.put(key.getParent(), value));

    Map<Path, SegmentMetadata> result = new HashMap<>();
    metadataMultimap.asMap().forEach((key, value) -> result.put(key, combineSegmentMetadata(value, column)));

    return result;
  }

  private static <T extends BaseMetadata & LocationProvider> SegmentMetadata combineToSegmentMetadata(Collection<T> metadataList, SchemaPath column) {
    Set<Path> metadataLocations = metadataList.stream()
        .map(metadata -> metadata.getPath()) // used lambda instead of method reference due to JDK-8141508
        .collect(Collectors.toSet());
    return combineToSegmentMetadata(metadataList, column, metadataLocations);
  }

  private static SegmentMetadata combineSegmentMetadata(Collection<SegmentMetadata> metadataList, SchemaPath column) {
    Set<Path> metadataLocations = metadataList.stream()
        .flatMap(metadata -> metadata.getLocations().stream())
        .collect(Collectors.toSet());

    return combineToSegmentMetadata(metadataList, column, metadataLocations);
  }

  /**
   * Returns {@link SegmentMetadata} which is combined metadata of list of specified metadata
   *
   * @param metadataList      metadata to combine
   * @param column            segment column
   * @param metadataLocations locations of metadata combined in resulting segment
   * @param <T>               type of metadata to combine
   * @return {@link SegmentMetadata} from combined metadata
   */
  private static <T extends BaseMetadata & LocationProvider> SegmentMetadata combineToSegmentMetadata(Collection<T> metadataList,
      SchemaPath column, Set<Path> metadataLocations) {
    List<StatisticsHolder<?>> segmentStatistics =
        Collections.singletonList(
            new StatisticsHolder<>(
                TableStatisticsKind.ROW_COUNT.mergeStatistics(metadataList),
                TableStatisticsKind.ROW_COUNT));
    // this code is used only to collect segment metadata to be used only during filtering,
    // so metadata identifier is not required here and in other places in this class
    MetadataInfo metadataInfo = MetadataInfo.builder().type(MetadataType.SEGMENT).build();
    T firstMetadata = metadataList.iterator().next();

    return SegmentMetadata.builder()
        .tableInfo(firstMetadata.getTableInfo())
        .metadataInfo(metadataInfo)
        .column(column)
        .schema(firstMetadata.getSchema())
        .path(firstMetadata.getPath().getParent())
        .columnsStatistics(TableMetadataUtils.mergeColumnsStatistics(metadataList, firstMetadata.getColumnsStatistics().keySet(), PARQUET_COLUMN_STATISTICS))
        .metadataStatistics(segmentStatistics)
        .partitionValues(Collections.emptyList())
        .locations(metadataLocations)
        .build();
  }

  @Override
  public Map<Path, FileMetadata> getFilesMetadataMap() {
    if (files == null) {
      if (entries.isEmpty() || !collectMetadata) {
        return Collections.emptyMap();
      }
      boolean addRowGroups = false;
      if (rowGroups == null) {
        rowGroups = LinkedListMultimap.create();
        addRowGroups = true;
      }

      Multimap<Path, RowGroupMetadata> rowGroupsMetadata = ParquetTableMetadataUtils.getRowGroupsMetadata(parquetTableMetadata);

      if (addRowGroups) {
        rowGroups = rowGroupsMetadata;
      }

      files = rowGroupsMetadata.asMap().values().stream()
        .map(ParquetTableMetadataUtils::getFileMetadata)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
          FileMetadata::getPath,
          Function.identity(),
          (o, n) -> n,
          LinkedHashMap::new));
    }
    return files;
  }

  @Override
  public List<ReadEntryWithPath> getEntries() {
    return entries;
  }

  @Override
  public Set<Path> getFileSet() {
    return fileSet;
  }

  @Override
  public List<RowGroupMetadata> getRowGroupsMeta() {
    return new ArrayList<>(getRowGroupsMetadataMap().values());
  }

  @Override
  public List<Path> getLocations() {
    return parquetTableMetadata.getFiles().stream()
        .map(MetadataBase.ParquetFileMetadata::getPath)
        .collect(Collectors.toList());
  }

  @Override
  public Multimap<Path, RowGroupMetadata> getRowGroupsMetadataMap() {
    if (rowGroups == null) {
      rowGroups = ParquetTableMetadataUtils.getRowGroupsMetadata(parquetTableMetadata);
    }
    return rowGroups;
  }

  @Override
  public boolean checkMetadataVersion() {
    return false;
  }

  protected abstract void initInternal() throws IOException;

  public static abstract class Builder<T extends Builder<T>> implements ParquetMetadataProviderBuilder<T> {
    private final MetadataProviderManager metadataProviderManager;

    private List<ReadEntryWithPath> entries;
    private Path selectionRoot;
    private ParquetReaderConfig readerConfig;
    private TupleMetadata schema;

    private FileSelection selection;

    public Builder(MetadataProviderManager source) {
      this.metadataProviderManager = source;
    }

    @Override
    public T withEntries(List<ReadEntryWithPath> entries) {
      this.entries = entries;
      return self();
    }

    @Override
    public T withSelectionRoot(Path selectionRoot) {
      this.selectionRoot = selectionRoot;
      return self();
    }

    @Override
    public T withReaderConfig(ParquetReaderConfig readerConfig) {
      this.readerConfig = readerConfig;
      return self();
    }

    @Override
    public T withSelection(FileSelection selection) {
      this.selection = selection;
      return self();
    }

    @Override
    public T withSchema(TupleMetadata schema) {
      this.schema = schema;
      return self();
    }

    protected abstract T self();

    public Path selectionRoot() {
      return selectionRoot;
    }

    public FileSelection selection() {
      return selection;
    }

    public MetadataProviderManager metadataProviderManager() {
      return metadataProviderManager;
    }

    public List<ReadEntryWithPath> entries() {
      return entries;
    }
  }
}
