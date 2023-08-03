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

package org.apache.iceberg;

import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.FileNameRules;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.scan.BasicArcticFileScanTask;
import com.netease.arctic.scan.ChangeTableIncrementalScan;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.util.StructLikeMap;

import java.util.List;

public class ArcticChangeTableScan extends DataTableScan implements ChangeTableIncrementalScan {
  private StructLikeMap<Long> fromPartitionSequence;
  private StructLikeMap<Long> fromPartitionLegacyTransactionId;
  private Long toSequence;

  public ArcticChangeTableScan(TableOperations ops, Table table) {
    super(ops, table);
  }

  protected ArcticChangeTableScan(
      TableOperations ops, Table table, Schema schema, TableScanContext context) {
    super(ops, table, schema, context);
  }

  @Override
  public ArcticChangeTableScan useSnapshot(long scanSnapshotId) {
    TableScan scan = super.useSnapshot(scanSnapshotId);
    return newRefinedScan(
        tableOps(), table(), scan.schema(), context().useSnapshotId(scanSnapshotId));
  }

  @Override
  protected ArcticChangeTableScan newRefinedScan(
      TableOperations ops, Table table, Schema schema, TableScanContext context) {
    ArcticChangeTableScan scan = new ArcticChangeTableScan(ops, table, schema, context);
    scan.fromPartitionSequence = this.fromPartitionSequence;
    scan.fromPartitionLegacyTransactionId = this.fromPartitionLegacyTransactionId;
    scan.toSequence = this.toSequence;
    return scan;
  }

  @Override
  public ChangeTableIncrementalScan fromSequence(StructLikeMap<Long> partitionSequence) {
    ArcticChangeTableScan scan = newRefinedScan(tableOps(), table(), schema(), context());
    scan.fromPartitionSequence = partitionSequence;
    return scan;
  }

  @Override
  public ChangeTableIncrementalScan toSequence(long sequence) {
    ArcticChangeTableScan scan = newRefinedScan(tableOps(), table(), schema(), context());
    scan.toSequence = sequence;
    return scan;
  }

  @Override
  public ChangeTableIncrementalScan fromLegacyTransaction(StructLikeMap<Long> partitionTransactionId) {
    ArcticChangeTableScan scan = newRefinedScan(tableOps(), table(), schema(), context());
    scan.fromPartitionLegacyTransactionId = partitionTransactionId;
    return scan;
  }

  @Override
  public CloseableIterable<IcebergContentFile<?>> planFilesWithSequence() {
    Snapshot snapshot = snapshot();

    FileIO io = table().io();
    List<ManifestFile> dataManifests = snapshot.dataManifests(io);
    List<ManifestFile> deleteManifests = snapshot.deleteManifests(io);
    scanMetrics().totalDataManifests().increment((long) dataManifests.size());
    scanMetrics().totalDeleteManifests().increment((long) deleteManifests.size());
    ArcticChangeManifestGroup manifestGroup =
        new ArcticChangeManifestGroup(io, dataManifests, deleteManifests)
            .caseSensitive(isCaseSensitive())
            .select(scanColumns())
            .filterData(filter())
            .specsById(table().specs())
            .scanMetrics(scanMetrics())
            .ignoreDeleted();

    if (shouldIgnoreResiduals()) {
      manifestGroup.ignoreResiduals();
    }

    if (dataManifests.size() > 1 && shouldPlanWithExecutor()) {
      manifestGroup.planWith(planExecutor());
    }
    CloseableIterable<ArcticChangeManifestGroup.ChangeFileScanTask>
        files = manifestGroup.planFilesWithSequence();

    files = CloseableIterable.filter(files, f -> {
      StructLike partition = f.file().partition();
      long sequenceNumber = f.getDataSequenceNumber();
      Boolean shouldKeep = shouldKeepFile(partition, sequenceNumber);
      if (shouldKeep == null) {
        String filePath = f.file().path().toString();
        return shouldKeepFileWithLegacyTxId(partition,
            FileNameRules.parseChange(filePath, sequenceNumber).transactionId());
      } else {
        return shouldKeep;
      }
    });
    return CloseableIterable
        .transform(files, f -> IcebergContentFile.wrap(f.file(), f.getDataSequenceNumber()));
  }

  @Override
  public CloseableIterable<FileScanTask> doPlanFiles() {
    return CloseableIterable.transform(planFilesWithSequence(), fileWithSequence ->
        new BasicArcticFileScanTask(DefaultKeyedFile.parseChange(
            ((DataFile) fileWithSequence),
            fileWithSequence.getSequenceNumber()), null, table().spec(), null)
    );
  }


  private Boolean shouldKeepFile(StructLike partition, long sequence) {
    if (biggerThanToSequence(sequence)) {
      return false;
    }
    if (fromPartitionSequence == null || fromPartitionSequence.isEmpty()) {
      // if fromPartitionSequence is not set or is empty, return null to check legacy transactionId
      return null;
    }
    if (table().spec().isUnpartitioned()) {
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
    if (table().spec().isUnpartitioned()) {
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
}
