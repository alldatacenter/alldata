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
package org.apache.drill.exec.metastore.store;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.exception.MetadataException;
import org.apache.drill.exec.metastore.MetastoreMetadataProviderManager;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.record.SchemaUtil;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.schema.SchemaProvider;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.parquet.ParquetTableMetadataUtils;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.metastore.components.tables.BasicTablesRequests;
import org.apache.drill.metastore.components.tables.MetastoreTableInfo;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.NonInterestingColumnsMetadata;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.metadata.TableMetadata;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.drill.metastore.metadata.TableMetadataProviderBuilder;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.util.SchemaPathUtils;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TableMetadataProvider} which uses Drill Metastore for providing table metadata
 * for file-based tables.
 */
public class MetastoreFileTableMetadataProvider implements TableMetadataProvider {
  private static final Logger logger = LoggerFactory.getLogger(MetastoreFileTableMetadataProvider.class);

  protected final BasicTablesRequests basicTablesRequests;
  protected final TableInfo tableInfo;
  protected final MetastoreTableInfo metastoreTableInfo;
  protected final TupleMetadata schema;
  protected final List<String> paths;
  protected final DrillStatsTable statsProvider;
  protected final TableMetadataProviderBuilder fallbackBuilder;

  protected final boolean useSchema;
  protected final boolean useStatistics;
  protected final boolean fallbackToFileMetadata;

  protected BaseTableMetadata tableMetadata;
  protected Map<Path, SegmentMetadata> segmentsMetadata;
  protected List<PartitionMetadata> partitions;
  protected Map<Path, FileMetadata> files;

  private NonInterestingColumnsMetadata nonInterestingColumnsMetadata;

  protected MetastoreFileTableMetadataProvider(Builder<?> builder) {
    SchemaProvider schemaProvider = builder.metadataProviderManager.getSchemaProvider();
    TupleMetadata schema = builder.schema;
    // schema passed into the builder has greater priority
    if (schema == null && schemaProvider != null) {
      try {
        schema = schemaProvider.read().getSchema();
      } catch (IOException e) {
        logger.warn("Unable to read schema from schema provider [{}]: {}.\n" +
                "Query execution will continue without using the schema.",
            builder.metadataProviderManager.getTableInfo().name(), e.getMessage());
        logger.trace("Error when reading the schema", e);
      }
    }

    this.basicTablesRequests = builder.metadataProviderManager.getMetastoreRegistry().get().tables().basicRequests();
    this.tableInfo = builder.metadataProviderManager.getTableInfo();
    this.metastoreTableInfo = basicTablesRequests.metastoreTableInfo(tableInfo);
    this.useSchema = builder.metadataProviderManager.getConfig().useSchema();
    this.useStatistics = builder.metadataProviderManager.getConfig().useStatistics();
    this.fallbackToFileMetadata = builder.metadataProviderManager.getConfig().fallbackToFileMetadata();
    this.schema = schema;
    this.fallbackBuilder = builder.fallback;
    this.statsProvider = builder.metadataProviderManager.getStatsProvider();
    this.paths = builder.paths;

    TableMetadataProvider source = builder.metadataProviderManager.getTableMetadataProvider();
    // store results into FileSystemMetadataProviderManager to be able to use them when creating new instances
    if (source == null || source.getFilesMetadataMap().size() < getFilesMetadataMap().size()) {
      builder.metadataProviderManager.setTableMetadataProvider(this);
    }
  }

  protected void throwIfChanged() {
    if (basicTablesRequests.hasMetastoreTableInfoChanged(metastoreTableInfo)) {
      throw MetadataException.of(MetadataException.MetadataExceptionType.INCONSISTENT_METADATA);
    }
  }

  @Override
  public TableMetadata getTableMetadata() {
    throwIfChanged();
    if (tableMetadata == null) {
      if (schema == null) {
        if (useSchema) {
          tableMetadata = basicTablesRequests.tableMetadata(tableInfo);
        } else {
          throw MetadataException.of(MetadataException.MetadataExceptionType.ABSENT_SCHEMA);
        }
      } else {
        tableMetadata = basicTablesRequests.tableMetadata(tableInfo).toBuilder()
            .schema(schema)
            .build();
      }

      if (!useStatistics) {
        // removes statistics to prevent its usage later
        tableMetadata = tableMetadata.toBuilder()
            .columnsStatistics(Collections.emptyMap())
            .build();
      }

      if (statsProvider != null) {
        if (!statsProvider.isMaterialized()) {
          statsProvider.materialize();
        }
        tableMetadata = tableMetadata.cloneWithStats(
            ParquetTableMetadataUtils.getColumnStatistics(tableMetadata.getSchema(), statsProvider),
            DrillStatsTable.getEstimatedTableStats(statsProvider));
      }
    }
    return tableMetadata;
  }

  @Override
  public List<SchemaPath> getPartitionColumns() {
    throwIfChanged();
    return basicTablesRequests.interestingColumnsAndPartitionKeys(tableInfo).partitionKeys().values().stream()
        .map(SchemaPath::getSimplePath)
        .collect(Collectors.toList());
  }

  @Override
  public List<PartitionMetadata> getPartitionsMetadata() {
    throwIfChanged();
    if (partitions == null) {
      partitions = basicTablesRequests.partitionsMetadata(tableInfo, null, null);
    }
    return partitions;
  }

  @Override
  public List<PartitionMetadata> getPartitionMetadata(SchemaPath columnName) {
    throwIfChanged();
    return basicTablesRequests.partitionsMetadata(tableInfo, null, columnName.getRootSegmentPath());
  }

  @Override
  public Map<Path, FileMetadata> getFilesMetadataMap() {
    throwIfChanged();
    if (files == null) {
      files = basicTablesRequests.filesMetadata(tableInfo, null, paths).stream()
          .collect(Collectors.toMap(FileMetadata::getPath, Function.identity()));
    }
    return files;
  }

  @Override
  public Map<Path, SegmentMetadata> getSegmentsMetadataMap() {
    throwIfChanged();
    if (segmentsMetadata == null) {
      segmentsMetadata = basicTablesRequests.segmentsMetadataByColumn(tableInfo, null, null).stream()
          .collect(Collectors.toMap(SegmentMetadata::getPath, Function.identity()));
    }
    return segmentsMetadata;
  }

  @Override
  public FileMetadata getFileMetadata(Path location) {
    throwIfChanged();
    return basicTablesRequests.fileMetadata(tableInfo, null, location.toUri().getPath());
  }

  @Override
  public List<FileMetadata> getFilesForPartition(PartitionMetadata partition) {
    throwIfChanged();
    List<String> paths = partition.getLocations().stream()
        .map(path -> path.toUri().getPath())
        .collect(Collectors.toList());
    return basicTablesRequests.filesMetadata(tableInfo, null, paths);
  }

  @Override
  public NonInterestingColumnsMetadata getNonInterestingColumnsMetadata() {
    throwIfChanged();
    if (nonInterestingColumnsMetadata == null) {
      TupleMetadata schema = getTableMetadata().getSchema();

      List<StatisticsHolder<?>> statistics = Collections.singletonList(new StatisticsHolder<>(Statistic.NO_COLUMN_STATS, ColumnStatisticsKind.NULLS_COUNT));

      List<SchemaPath> columnPaths = SchemaUtil.getSchemaPaths(schema);
      List<SchemaPath> interestingColumns = getInterestingColumns(columnPaths);
      // populates statistics for non-interesting columns and columns for which statistics wasn't collected
      Map<SchemaPath, ColumnStatistics<?>> columnsStatistics = columnPaths.stream()
          .filter(schemaPath -> !interestingColumns.contains(schemaPath)
              || SchemaPathUtils.getColumnMetadata(schemaPath, schema).isArray())
          .collect(Collectors.toMap(
              Function.identity(),
              schemaPath -> new ColumnStatistics<>(statistics, SchemaPathUtils.getColumnMetadata(schemaPath, schema).type())));
      nonInterestingColumnsMetadata = new NonInterestingColumnsMetadata(columnsStatistics);
    }
    return nonInterestingColumnsMetadata;
  }

  @Override
  public boolean checkMetadataVersion() {
    return true;
  }

  private List<SchemaPath> getInterestingColumns(List<SchemaPath> columnPaths) {
    if (useStatistics) {
      return getTableMetadata().getInterestingColumns() == null
          ? columnPaths
          : getTableMetadata().getInterestingColumns();
    } else {
      // if `metastore.metadata.use_statistics` is false, all columns are treated as non-interesting
      return Collections.emptyList();
    }
  }

  public static class Builder<T extends Builder<T>> implements FileTableMetadataProviderBuilder<T> {
    protected final MetastoreMetadataProviderManager metadataProviderManager;

    // builder for fallback ParquetFileTableMetadataProvider
    // for the case when required metadata is absent in Metastore
    protected final TableMetadataProviderBuilder fallback;

    protected TupleMetadata schema;

    protected List<String> paths;

    private FileSelection selection;

    private DrillFileSystem fs;

    public Builder(MetastoreMetadataProviderManager source) {
      this(source, new SimpleFileTableMetadataProvider.Builder(FileSystemMetadataProviderManager.init()));
    }

    protected Builder(MetastoreMetadataProviderManager source, TableMetadataProviderBuilder fallback) {
      this.metadataProviderManager = source;
      this.fallback = fallback;
    }

    @Override
    public T withSchema(TupleMetadata schema) {
      this.schema = schema;
      return self();
    }

    public T withSelection(FileSelection selection) {
      this.selection = selection;
      return self();
    }

    public T withFileSystem(DrillFileSystem fs) {
      this.fs = fs;
      return self();
    }

    protected T self() {
      return (T) this;
    }

    public MetastoreMetadataProviderManager metadataProviderManager() {
      return metadataProviderManager;
    }

    public FileSelection selection() {
      return selection;
    }

    public DrillFileSystem fs() {
      return fs;
    }

    @Override
    public TableMetadataProvider build() throws IOException {
      if (selection().isExpandedFully()) {
        paths = selection.getFiles().stream()
            .map(path -> Path.getPathWithoutSchemeAndAuthority(path).toUri().getPath())
            .collect(Collectors.toList());
      } else {
        paths = DrillFileSystemUtil.listFiles(fs, selection.getSelectionRoot(), true).stream()
            .map(fileStatus -> Path.getPathWithoutSchemeAndAuthority(fileStatus.getPath()).toUri().getPath())
            .collect(Collectors.toList());
      }
      return new MetastoreFileTableMetadataProvider(this);
    }
  }
}
