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

package com.netease.arctic.op;

import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.util.StructLikeMap;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Overwrite {@link com.netease.arctic.table.BaseTable} and change max transaction id map
 */
public class OverwriteBaseFiles extends PartitionTransactionOperation {

  public static final String PROPERTIES_TRANSACTION_ID = "txId";

  private final List<DataFile> deleteFiles;
  private final List<DataFile> addFiles;
  private final List<DeleteFile> deleteDeleteFiles;
  private final List<DeleteFile> addDeleteFiles;
  private Expression deleteExpression = Expressions.alwaysFalse();
  private boolean deleteExpressionApplied = false;
  private final StructLikeMap<Long> partitionOptimizedSequence;

  private Long optimizedSequence;
  // dynamic indicate that the optimized sequence should be applied to the changed partitions
  private Boolean dynamic;
  private Expression conflictDetectionFilter = null;

  public OverwriteBaseFiles(KeyedTable table) {
    super(table);
    this.deleteFiles = Lists.newArrayList();
    this.addFiles = Lists.newArrayList();
    this.deleteDeleteFiles = Lists.newArrayList();
    this.addDeleteFiles = Lists.newArrayList();
    this.partitionOptimizedSequence = StructLikeMap.create(table.spec().partitionType());
  }

  public OverwriteBaseFiles overwriteByRowFilter(Expression expr) {
    if (expr != null) {
      deleteExpression = Expressions.or(deleteExpression, expr);
    }
    return this;
  }

  public OverwriteBaseFiles addFile(DataFile file) {
    addFiles.add(file);
    return this;
  }

  public OverwriteBaseFiles addFile(DeleteFile file) {
    addDeleteFiles.add(file);
    return this;
  }

  public OverwriteBaseFiles deleteFile(DataFile file) {
    deleteFiles.add(file);
    return this;
  }

  public OverwriteBaseFiles deleteFile(DeleteFile file) {
    deleteDeleteFiles.add(file);
    return this;
  }

  public OverwriteBaseFiles dynamic(boolean enable) {
    this.dynamic = enable;
    return this;
  }

  /**
   * Update optimized sequence for partition.
   * The files of ChangeStore whose sequence is bigger than optimized sequence should migrate to BaseStore later.
   *
   * @param partitionData - partition
   * @param sequence - optimized sequence
   * @return this for chain
   */
  public OverwriteBaseFiles updateOptimizedSequence(StructLike partitionData, long sequence) {
    Preconditions.checkArgument(this.dynamic == null || !this.dynamic,
        "updateOptimizedSequenceDynamically() and updateOptimizedSequence() can't be used simultaneously");
    this.partitionOptimizedSequence.put(partitionData, sequence);
    this.dynamic = false;
    return this;
  }

  /**
   * Update optimized sequence for changed partitions.
   * The files of ChangeStore whose sequence is bigger than optimized sequence should migrate to BaseStore later.
   *
   * @param sequence - optimized sequence
   * @return this for chain
   */
  public OverwriteBaseFiles updateOptimizedSequenceDynamically(long sequence) {
    Preconditions.checkArgument(this.dynamic == null || this.dynamic,
        "updateOptimizedSequenceDynamically() and updateOptimizedSequence() can't be used simultaneously");
    this.optimizedSequence = sequence;
    this.dynamic = true;
    return this;
  }

  public OverwriteBaseFiles validateNoConflictingAppends(Expression newConflictDetectionFilter) {
    Preconditions.checkArgument(newConflictDetectionFilter != null, "Conflict detection filter cannot be null");
    this.conflictDetectionFilter = newConflictDetectionFilter;
    return this;
  }

  @Override
  protected boolean isEmptyCommit() {
    applyDeleteExpression();
    return deleteFiles.isEmpty() && addFiles.isEmpty() && deleteDeleteFiles.isEmpty() && addDeleteFiles.isEmpty() &&
        partitionOptimizedSequence.isEmpty();
  }

  @Override
  protected StructLikeMap<Map<String, String>> apply(Transaction transaction) {
    Preconditions.checkState(this.dynamic != null,
        "updateOptimizedSequence() or updateOptimizedSequenceDynamically() must be invoked");
    applyDeleteExpression();

    StructLikeMap<Long> sequenceForChangedPartitions = null;
    if (this.dynamic) {
      sequenceForChangedPartitions = StructLikeMap.create(transaction.table().spec().partitionType());
    }

    UnkeyedTable baseTable = keyedTable.baseTable();

    // step1: overwrite data files
    if (!this.addFiles.isEmpty() || !this.deleteFiles.isEmpty()) {
      OverwriteFiles overwriteFiles = transaction.newOverwrite();

      if (conflictDetectionFilter != null && baseTable.currentSnapshot() != null) {
        overwriteFiles.conflictDetectionFilter(conflictDetectionFilter).validateNoConflictingData();
        overwriteFiles.validateFromSnapshot(baseTable.currentSnapshot().snapshotId());
      }
      if (this.dynamic) {
        for (DataFile d : this.addFiles) {
          sequenceForChangedPartitions.put(d.partition(), this.optimizedSequence);
        }
        for (DataFile d : this.deleteFiles) {
          sequenceForChangedPartitions.put(d.partition(), this.optimizedSequence);
        }
      }
      this.addFiles.forEach(overwriteFiles::addFile);
      this.deleteFiles.forEach(overwriteFiles::deleteFile);
      if (optimizedSequence != null && optimizedSequence > 0) {
        overwriteFiles.set(PROPERTIES_TRANSACTION_ID, optimizedSequence + "");
      }

      if (MapUtils.isNotEmpty(properties)) {
        properties.forEach(overwriteFiles::set);
      }
      overwriteFiles.commit();
    }

    // step2: RowDelta/Rewrite pos-delete files
    if (CollectionUtils.isNotEmpty(addDeleteFiles) || CollectionUtils.isNotEmpty(deleteDeleteFiles)) {
      if (CollectionUtils.isEmpty(deleteDeleteFiles)) {
        RowDelta rowDelta = transaction.newRowDelta();
        if (baseTable.currentSnapshot() != null) {
          rowDelta.validateFromSnapshot(baseTable.currentSnapshot().snapshotId());
        }

        if (this.dynamic) {
          for (DeleteFile d : this.addDeleteFiles) {
            sequenceForChangedPartitions.put(d.partition(), this.optimizedSequence);
          }
        }

        addDeleteFiles.forEach(rowDelta::addDeletes);
        if (MapUtils.isNotEmpty(properties)) {
          properties.forEach(rowDelta::set);
        }
        rowDelta.commit();
      } else {
        RewriteFiles rewriteFiles = transaction.newRewrite();
        if (baseTable.currentSnapshot() != null) {
          rewriteFiles.validateFromSnapshot(baseTable.currentSnapshot().snapshotId());
        }

        if (this.dynamic) {
          for (DeleteFile d : this.addDeleteFiles) {
            sequenceForChangedPartitions.put(d.partition(), this.optimizedSequence);
          }
          for (DeleteFile d : this.deleteDeleteFiles) {
            sequenceForChangedPartitions.put(d.partition(), this.optimizedSequence);
          }
        }
        rewriteFiles.rewriteFiles(Collections.emptySet(), new HashSet<>(deleteDeleteFiles),
            Collections.emptySet(), new HashSet<>(addDeleteFiles));
        if (MapUtils.isNotEmpty(properties)) {
          properties.forEach(rewriteFiles::set);
        }
        rewriteFiles.commit();
      }
    }

    // step3: set optimized sequence id, optimized time
    String commitTime = String.valueOf(System.currentTimeMillis());
    PartitionSpec spec = transaction.table().spec();
    StructLikeMap<Map<String, String>> partitionProperties = StructLikeMap.create(spec.partitionType());
    StructLikeMap<Long> toChangePartitionSequence;
    if (this.dynamic) {
      toChangePartitionSequence = sequenceForChangedPartitions;
    } else {
      toChangePartitionSequence = this.partitionOptimizedSequence;
    }
    toChangePartitionSequence.forEach((partition, sequence) -> {
      Map<String, String> properties = partitionProperties.computeIfAbsent(partition, k -> Maps.newHashMap());
      properties.put(TableProperties.PARTITION_OPTIMIZED_SEQUENCE, String.valueOf(sequence));
      properties.put(TableProperties.PARTITION_BASE_OPTIMIZED_TIME, commitTime);
    });

    return partitionProperties;
  }

  private void applyDeleteExpression() {
    if (this.deleteExpressionApplied) {
      return;
    }
    if (this.deleteExpression == null) {
      return;
    }
    try (CloseableIterable<CombinedScanTask> combinedScanTasks
             = keyedTable.newScan().filter(deleteExpression).planTasks()) {
      combinedScanTasks.forEach(combinedTask -> combinedTask.tasks().forEach(
          t -> {
            t.dataTasks().forEach(ft -> deleteFiles.add(ft.file()));
            t.arcticEquityDeletes().forEach(ft -> deleteFiles.add(ft.file()));
          }
      ));
      this.deleteExpressionApplied = true;
    } catch (IOException e) {
      throw new IllegalStateException("failed when apply delete expression when overwrite files", e);
    }
  }
}
