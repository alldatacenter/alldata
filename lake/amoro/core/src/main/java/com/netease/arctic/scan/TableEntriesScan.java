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

package com.netease.arctic.scan;

import com.netease.arctic.IcebergFileEntry;
import com.netease.arctic.utils.ManifestEntryFields;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.DataTask;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.FileMetadata;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.HasTableOperations;
import org.apache.iceberg.MetadataTableType;
import org.apache.iceberg.MetadataTableUtils;
import org.apache.iceberg.Metrics;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.expressions.InclusiveMetricsEvaluator;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.types.Types;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * API for configuring a scan to get the {@link IcebergFileEntry} of an Iceberg Table.
 */
public class TableEntriesScan {
  private final Table table;
  private final Long snapshotId;
  private final Expression dataFilter;
  private final boolean aliveEntry;
  private final boolean allFileContent;
  private final boolean includeColumnStats;
  private final Set<FileContent> validFileContent;
  private final MetadataTableType metadataTableType;
  private final Schema schema;
  private final Long fromSequence;

  private Table entriesTable;
  private InclusiveMetricsEvaluator lazyMetricsEvaluator = null;
  private Map<String, Integer> lazyIndexOfDataFileType;
  private Map<String, Integer> lazyIndexOfEntryType;

  public static Builder builder(Table table) {
    return new Builder(table);
  }

  public static class Builder {
    private final Table table;
    private Long snapshotId;
    private Expression dataFilter;
    private boolean aliveEntry = true;
    private boolean includeColumnStats = false;
    private final Set<FileContent> fileContents = Sets.newHashSet();
    private Schema schema;
    private MetadataTableType metadataTableType = MetadataTableType.ENTRIES;
    private Long fromSequence;

    public Builder(Table table) {
      this.table = table;
    }

    /**
     * Set the filter
     *
     * @param dataFilter default is always true
     * @return this for chain
     */
    public Builder withDataFilter(Expression dataFilter) {
      this.dataFilter = dataFilter;
      return this;
    }

    /**
     * If only return the Existing, Add entries
     *
     * @param aliveEntry true for only Existing, Add
     * @return this for chain
     */
    public Builder withAliveEntry(boolean aliveEntry) {
      this.aliveEntry = aliveEntry;
      return this;
    }

    /**
     * Set the fileContent
     *
     * @param fileContent default is nothing
     * @return this for chain
     */
    public Builder includeFileContent(FileContent... fileContent) {
      this.fileContents.addAll(Arrays.asList(fileContent));
      return this;
    }

    /**
     * Set the snapshotId
     *
     * @param snapshotId snapshotId
     * @return this for chain
     */
    public Builder useSnapshot(long snapshotId) {
      this.snapshotId = snapshotId;
      return this;
    }

    /**
     * Set if loads the column stats with each file.
     * <p>Column stats include: value count, null value count, lower bounds, and upper bounds.
     *
     * @return this for chain
     */
    public Builder includeColumnStats() {
      this.includeColumnStats = true;
      return this;
    }

    /**
     * Set the sequence number, fetch all entries greater than or equal to this sequence number.
     * @param fromSequence nullable.
     * @return this for chains
     */
    public Builder fromSequence(Long fromSequence) {
      this.fromSequence = fromSequence;
      return this;
    }

    public Builder project(Schema schema) {
      this.schema = schema;
      return this;
    }

    public Builder allEntries() {
      this.metadataTableType = MetadataTableType.ALL_ENTRIES;
      return this;
    }

    public TableEntriesScan build() {
      return new TableEntriesScan(table, snapshotId, dataFilter, aliveEntry,
          fileContents, includeColumnStats, schema, metadataTableType, fromSequence);
    }
  }

  private TableEntriesScan(
      Table table, Long snapshotId, Expression dataFilter, boolean aliveEntry,
      Set<FileContent> validFileContent, boolean includeColumnStats,
      Schema schema, MetadataTableType metadataTableType, Long fromSequence) {
    this.table = table;
    this.dataFilter = dataFilter;
    this.aliveEntry = aliveEntry;
    this.allFileContent = validFileContent.containsAll(Arrays.asList(FileContent.values()));
    this.validFileContent = validFileContent;
    this.snapshotId = snapshotId;
    this.includeColumnStats = includeColumnStats;
    this.schema = schema;
    this.metadataTableType = metadataTableType;
    this.fromSequence = fromSequence;
  }

  public CloseableIterable<IcebergFileEntry> entries() {
    TableScan tableScan = getMetadataTable().newScan();
    if (snapshotId != null) {
      tableScan = tableScan.useSnapshot(snapshotId);
    }
    if (schema != null) {
      tableScan = tableScan.project(schema);
    }
    CloseableIterable<FileScanTask> manifestFileScanTasks = tableScan.planFiles();

    CloseableIterable<StructLike> entries = CloseableIterable.concat(entriesOfManifest(manifestFileScanTasks));

    CloseableIterable<IcebergFileEntry> allEntries =
        CloseableIterable.transform(entries, (entry -> {
          Long sequence = entry.get(entryFieldIndex(ManifestEntryFields.SEQUENCE_NUMBER.name()), Long.class);
          if (fromSequence != null && fromSequence > sequence) {
            return null;
          }
          ManifestEntryFields.Status status =
              ManifestEntryFields.Status.of(
                  entry.get(entryFieldIndex(ManifestEntryFields.STATUS.name()), Integer.class));
          StructLike fileRecord =
              entry.get(entryFieldIndex(ManifestEntryFields.DATA_FILE_FIELD_NAME), StructLike.class);
          FileContent fileContent =
              getFileContent(fileRecord.get(dataFileFieldIndex(DataFile.CONTENT.name()), Integer.class));
          if (shouldKeep(status, fileContent)) {
            Long snapshotId = entry.get(entryFieldIndex(ManifestEntryFields.SNAPSHOT_ID.name()), Long.class);
            ContentFile<?> contentFile = buildContentFile(fileContent, fileRecord);
            if (metricsEvaluator().eval(contentFile)) {
              if (needMetrics() && !includeColumnStats) {
                contentFile = (ContentFile<?>) contentFile.copyWithoutStats();
              }
              return new IcebergFileEntry(snapshotId, sequence, status, contentFile);
            }
          }
          return null;
        }));
    return CloseableIterable.filter(allEntries, Objects::nonNull);
  }

  private Table getMetadataTable() {
    if (this.entriesTable == null) {
      this.entriesTable = MetadataTableUtils.createMetadataTableInstance(((HasTableOperations) table).operations(),
          table.name(), table.name() + "#" + this.metadataTableType.name(),
          this.metadataTableType);
    }
    return this.entriesTable;
  }

  private FileContent getFileContent(int contentId) {
    for (FileContent content : FileContent.values()) {
      if (content.id() == contentId) {
        return content;
      }
    }
    throw new IllegalArgumentException("not support content id " + contentId);
  }

  private boolean needMetrics() {
    return dataFilter != null || includeColumnStats;
  }

  private boolean shouldKeep(ManifestEntryFields.Status status, FileContent fileContent) {
    if (aliveEntry && status == ManifestEntryFields.Status.DELETED) {
      return false;
    }
    if (allFileContent) {
      return true;
    }
    return validFileContent != null && validFileContent.contains(fileContent);
  }

  private Iterable<CloseableIterable<StructLike>> entriesOfManifest(CloseableIterable<FileScanTask> fileScanTasks) {
    return Iterables.transform(fileScanTasks, task -> {
      assert task != null;
      return ((DataTask) task).rows();
    });
  }

  private ContentFile<?> buildContentFile(FileContent fileContent, StructLike fileRecord) {
    ContentFile<?> file;
    if (fileContent == FileContent.DATA) {
      file = buildDataFile(fileRecord);
    } else {
      file = buildDeleteFile(fileRecord, fileContent);
    }
    return file;
  }

  private DataFile buildDataFile(StructLike fileRecord) {
    String filePath = fileRecord.get(dataFileFieldIndex(DataFile.FILE_PATH.name()), String.class);
    Long fileSize = fileRecord.get(dataFileFieldIndex(DataFile.FILE_SIZE.name()), Long.class);
    Long recordCount = fileRecord.get(dataFileFieldIndex(DataFile.RECORD_COUNT.name()), Long.class);
    DataFiles.Builder builder = DataFiles.builder(table.spec())
        .withPath(filePath)
        .withFileSizeInBytes(fileSize)
        .withRecordCount(recordCount);
    if (needMetrics()) {
      builder.withMetrics(buildMetrics(fileRecord));
    }
    if (table.spec().isPartitioned()) {
      StructLike partition = fileRecord.get(dataFileFieldIndex(DataFile.PARTITION_NAME), StructLike.class);
      builder.withPartition(partition);
    }
    return builder.build();
  }

  private DeleteFile buildDeleteFile(StructLike fileRecord, FileContent fileContent) {
    String filePath = fileRecord.get(dataFileFieldIndex(DataFile.FILE_PATH.name()), String.class);
    Long fileSize = fileRecord.get(dataFileFieldIndex(DataFile.FILE_SIZE.name()), Long.class);
    Long recordCount = fileRecord.get(dataFileFieldIndex(DataFile.RECORD_COUNT.name()), Long.class);
    FileMetadata.Builder builder = FileMetadata.deleteFileBuilder(table.spec())
        .withPath(filePath)
        .withFileSizeInBytes(fileSize)
        .withRecordCount(recordCount);
    if (needMetrics()) {
      builder.withMetrics(buildMetrics(fileRecord));
    }
    if (table.spec().isPartitioned()) {
      StructLike partition = fileRecord.get(dataFileFieldIndex(DataFile.PARTITION_NAME), StructLike.class);
      builder.withPartition(partition);
    }
    if (fileContent == FileContent.EQUALITY_DELETES) {
      builder.ofEqualityDeletes();
    } else {
      builder.ofPositionDeletes();
    }
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private Metrics buildMetrics(StructLike dataFile) {
    return new Metrics(
        dataFile.get(dataFileFieldIndex(DataFile.RECORD_COUNT.name()), Long.class),
        (Map<Integer, Long>) dataFile.get(dataFileFieldIndex(DataFile.COLUMN_SIZES.name()), Map.class),
        (Map<Integer, Long>) dataFile.get(dataFileFieldIndex(DataFile.VALUE_COUNTS.name()), Map.class),
        (Map<Integer, Long>) dataFile.get(dataFileFieldIndex(DataFile.NULL_VALUE_COUNTS.name()), Map.class),
        (Map<Integer, Long>) dataFile.get(dataFileFieldIndex(DataFile.NAN_VALUE_COUNTS.name()), Map.class),
        (Map<Integer, ByteBuffer>) dataFile.get(dataFileFieldIndex(DataFile.LOWER_BOUNDS.name()), Map.class),
        (Map<Integer, ByteBuffer>) dataFile.get(dataFileFieldIndex(DataFile.UPPER_BOUNDS.name()), Map.class));
  }

  private int entryFieldIndex(String fieldName) {
    if (lazyIndexOfEntryType == null) {
      List<Types.NestedField> fields = getMetadataTable().schema().columns();
      Map<String, Integer> map = Maps.newHashMap();
      for (int i = 0; i < fields.size(); i++) {
        map.put(fields.get(i).name(), i);
      }
      lazyIndexOfEntryType = map;
    }
    return lazyIndexOfEntryType.get(fieldName);
  }

  private int dataFileFieldIndex(String fieldName) {
    if (lazyIndexOfDataFileType == null) {
      List<Types.NestedField> fields =
          getMetadataTable().schema().findType(ManifestEntryFields.DATA_FILE_FIELD_NAME).asStructType().fields();
      Map<String, Integer> map = Maps.newHashMap();
      for (int i = 0; i < fields.size(); i++) {
        map.put(fields.get(i).name(), i);
      }
      lazyIndexOfDataFileType = map;
    }
    return lazyIndexOfDataFileType.get(fieldName);
  }

  private InclusiveMetricsEvaluator metricsEvaluator() {
    if (lazyMetricsEvaluator == null) {
      if (dataFilter != null) {
        this.lazyMetricsEvaluator =
            new InclusiveMetricsEvaluator(table.spec().schema(), dataFilter);
      } else {
        this.lazyMetricsEvaluator = new AlwaysTrueEvaluator(table.spec().schema());
      }
    }
    return lazyMetricsEvaluator;
  }

  private static class AlwaysTrueEvaluator extends InclusiveMetricsEvaluator {
    public AlwaysTrueEvaluator(Schema schema) {
      super(schema, Expressions.alwaysTrue());
    }

    @Override
    public boolean eval(ContentFile<?> file) {
      return true;
    }
  }
}
