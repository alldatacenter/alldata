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
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.data.file.WrapFileWithSequenceNumberHelper;
import com.netease.arctic.table.ChangeTable;
import org.apache.iceberg.BaseCombinedScanTask;
import org.apache.iceberg.CombinedScanTask;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.util.StructLikeMap;

import java.util.Collection;

public class ChangeTableBasicIncrementalScan implements ChangeTableIncrementalScan {

  private final ChangeTable table;
  private StructLikeMap<Long> fromPartitionSequence;
  private StructLikeMap<Long> fromPartitionLegacyTransactionId;
  private Long toSequence;
  private Expression dataFilter;
  private Long snapshotId;
  private boolean includeColumnStats;

  public ChangeTableBasicIncrementalScan(ChangeTable table) {
    this.table = table;
  }

  @Override
  public Table table() {
    return table;
  }

  @Override
  public ChangeTableIncrementalScan useSnapshot(long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  @Override
  public TableScan asOfTime(long timestampMillis) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TableScan option(String property, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TableScan project(Schema schema) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TableScan caseSensitive(boolean caseSensitive) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TableScan includeColumnStats() {
    this.includeColumnStats = true;
    return this;
  }

  @Override
  public TableScan select(Collection<String> columns) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ChangeTableIncrementalScan filter(Expression expr) {
    if (dataFilter == null) {
      dataFilter = expr;
    } else {
      dataFilter = Expressions.and(expr, dataFilter);
    }
    return this;
  }

  @Override
  public Expression filter() {
    return dataFilter;
  }

  @Override
  public TableScan ignoreResiduals() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TableScan appendsBetween(long fromSnapshotId, long toSnapshotId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TableScan appendsAfter(long fromSnapshotId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ChangeTableIncrementalScan fromSequence(StructLikeMap<Long> partitionSequence) {
    this.fromPartitionSequence = partitionSequence;
    return this;
  }

  @Override
  public ChangeTableIncrementalScan toSequence(long sequence) {
    this.toSequence = sequence;
    return this;
  }

  @Override
  public ChangeTableIncrementalScan fromLegacyTransaction(StructLikeMap<Long> partitionTransactionId) {
    this.fromPartitionLegacyTransactionId = partitionTransactionId;
    return this;
  }

  @Override
  public CloseableIterable<FileScanTask> planFiles() {
    return CloseableIterable.transform(planFilesWithSequence(), fileWithSequence ->
        new BasicArcticFileScanTask(DefaultKeyedFile.parseChange(((DataFile) fileWithSequence),
            fileWithSequence.getSequenceNumber()), null, table.spec(), null)
    );
  }

  @Override
  public CloseableIterable<ContentFileWithSequence<?>> planFilesWithSequence() {
    return planFilesWithSequence(this::shouldKeepFile, this::shouldKeepFileWithLegacyTxId);
  }

  private CloseableIterable<ContentFileWithSequence<?>> planFilesWithSequence(PartitionDataFilter shouldKeepFile,
                                                    PartitionDataFilter shouldKeepFileWithLegacyTxId) {
    Snapshot currentSnapshot = table.currentSnapshot();
    if (currentSnapshot == null) {
      // return no files for table without snapshot
      return CloseableIterable.empty();
    }
    TableEntriesScan.Builder builder = TableEntriesScan.builder(table)
        .withAliveEntry(true)
        .withDataFilter(dataFilter)
        .includeFileContent(FileContent.DATA);
    if (snapshotId != null) {
      builder.useSnapshot(snapshotId);
    }
    if (includeColumnStats) {
      builder.includeColumnStats();
    }
    TableEntriesScan manifestReader = builder.build();

    CloseableIterable<IcebergFileEntry> filteredEntry = CloseableIterable.filter(manifestReader.entries(), entry -> {
      StructLike partition = entry.getFile().partition();
      long sequenceNumber = entry.getSequenceNumber();
      Boolean shouldKeep = shouldKeepFile.shouldKeep(partition, sequenceNumber);
      if (shouldKeep == null) {
        String filePath = entry.getFile().path().toString();
        return shouldKeepFileWithLegacyTxId.shouldKeep(partition, FileNameGenerator.parseChange(filePath,
            sequenceNumber).transactionId());
      } else {
        return shouldKeep;
      }
    });
    return CloseableIterable.transform(filteredEntry,
        e -> WrapFileWithSequenceNumberHelper.wrap(e.getFile(), e.getSequenceNumber()));
  }

  @Override
  public CloseableIterable<CombinedScanTask> planTasks() {
    CloseableIterable<FileScanTask> fileScanTasks = planFiles();
    return CloseableIterable.transform(fileScanTasks, s -> new BaseCombinedScanTask(s));
  }

  @Override
  public Schema schema() {
    return table.schema();
  }

  @Override
  public Snapshot snapshot() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCaseSensitive() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long targetSplitSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int splitLookback() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long splitOpenFileCost() {
    throw new UnsupportedOperationException();
  }

  private Boolean shouldKeepFile(StructLike partition, long sequence) {
    if (biggerThanToSequence(sequence)) {
      return false;
    }
    if (fromPartitionSequence == null || fromPartitionSequence.isEmpty()) {
      // if fromPartitionSequence is not set or is empty, return null to check legacy transactionId
      return null;
    }
    if (table.spec().isUnpartitioned()) {
      Long fromSequence = fromPartitionSequence.entrySet().iterator().next().getValue();
      return sequence > fromSequence;
    } else {
      if (!fromPartitionSequence.containsKey(partition)) {
        // return null to check legacy transactionId
        return null;
      } else {
        Long fromSequence = fromPartitionSequence.get(partition);
        return sequence > fromSequence;
      }
    }
  }

  private boolean biggerThanToSequence(long sequence) {
    return this.toSequence != null && sequence > this.toSequence;
  }

  private boolean shouldKeepFileWithLegacyTxId(StructLike partition, long legacyTxId) {
    if (fromPartitionLegacyTransactionId == null || fromPartitionLegacyTransactionId.isEmpty()) {
      // if fromPartitionLegacyTransactionId is not set or is empty, return all files
      return true;
    }
    if (table.spec().isUnpartitioned()) {
      Long fromTransactionId = fromPartitionLegacyTransactionId.entrySet().iterator().next().getValue();
      return legacyTxId > fromTransactionId;
    } else {
      if (!fromPartitionLegacyTransactionId.containsKey(partition)) {
        // if fromPartitionLegacyTransactionId not contains this partition, return all files of this partition
        return true;
      } else {
        Long partitionTransactionId = fromPartitionLegacyTransactionId.get(partition);
        return legacyTxId > partitionTransactionId;
      }
    }
  }

  interface PartitionDataFilter {
    Boolean shouldKeep(StructLike partition, long sequence);
  }
}
