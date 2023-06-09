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

package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.api.CommitMetaProducer;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.OptimizeTaskRuntime;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.utils.UnKeyedTableUtil;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.op.OverwriteBaseFiles;
import com.netease.arctic.op.UpdatePartitionProperties;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.trace.SnapshotSummary;
import com.netease.arctic.utils.ArcticDataFiles;
import com.netease.arctic.utils.SerializationUtils;
import com.netease.arctic.utils.TableFileUtils;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.exceptions.ValidationException;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.util.SnapshotUtil;
import org.apache.iceberg.util.StructLikeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BasicOptimizeCommit {
  private static final Logger LOG = LoggerFactory.getLogger(BasicOptimizeCommit.class);
  protected final ArcticTable arcticTable;
  protected final Map<String, List<OptimizeTaskItem>> optimizeTasksToCommit;
  protected final Map<String, OptimizeType> partitionOptimizeType = new HashMap<>();

  public BasicOptimizeCommit(ArcticTable arcticTable,
                             Map<String, List<OptimizeTaskItem>> optimizeTasksToCommit) {
    this.arcticTable = arcticTable;
    this.optimizeTasksToCommit = optimizeTasksToCommit;
  }

  public Map<String, List<OptimizeTaskItem>> getCommittedTasks() {
    return optimizeTasksToCommit;
  }

  public boolean commit(long baseSnapshotId) throws Exception {
    Set<ContentFile<?>> minorAddFiles = new HashSet<>();
    Set<ContentFile<?>> minorDeleteFiles = new HashSet<>();
    Set<ContentFile<?>> majorAddFiles = new HashSet<>();
    Set<ContentFile<?>> majorDeleteFiles = new HashSet<>();
    try {
      if (optimizeTasksToCommit.isEmpty()) {
        LOG.info("{} get no tasks to commit", arcticTable.id());
        return true;
      }
      LOG.info("{} get tasks to commit with from snapshot id = {}, for partitions {} ", arcticTable.id(),
          baseSnapshotId, optimizeTasksToCommit.keySet());

      // collect files
      PartitionSpec spec = arcticTable.spec();
      StructLikeMap<Long> toSequenceOfPartitions = StructLikeMap.create(spec.partitionType());
      StructLikeMap<Long> fromSequenceOfPartitions = StructLikeMap.create(spec.partitionType());
      for (Map.Entry<String, List<OptimizeTaskItem>> entry : optimizeTasksToCommit.entrySet()) {
        for (OptimizeTaskItem task : entry.getValue()) {
          if (checkFileCount(task)) {
            LOG.error("table {} file count not match", arcticTable.id());
            throw new IllegalArgumentException("file count not match, can't commit");
          }
          // tasks in partition
          if (task.getOptimizeTask().getTaskId().getType() == OptimizeType.Minor) {
            task.getOptimizeRuntime().getTargetFiles().stream()
                .map(SerializationUtils::toContentFile)
                .forEach(minorAddFiles::add);

            minorDeleteFiles.addAll(selectDeletedFiles(task, minorAddFiles));

            long toSequence = task.getOptimizeTask().getToSequence();
            if (toSequence != BasicOptimizeTask.INVALID_SEQUENCE) {
              if (arcticTable.asKeyedTable().baseTable().spec().isUnpartitioned()) {
                toSequenceOfPartitions.put(TablePropertyUtil.EMPTY_STRUCT, toSequence);
              } else {
                toSequenceOfPartitions.putIfAbsent(ArcticDataFiles.data(spec, entry.getKey()), toSequence);
              }
            }

            long fromSequence = task.getOptimizeTask().getFromSequence();
            if (fromSequence != BasicOptimizeTask.INVALID_SEQUENCE) {
              if (arcticTable.asKeyedTable().baseTable().spec().isUnpartitioned()) {
                fromSequenceOfPartitions.put(TablePropertyUtil.EMPTY_STRUCT, fromSequence);
              } else {
                fromSequenceOfPartitions.putIfAbsent(ArcticDataFiles.data(spec, entry.getKey()), fromSequence);
              }
            }
            
            partitionOptimizeType.put(entry.getKey(), OptimizeType.Minor);
          } else {
            task.getOptimizeRuntime().getTargetFiles().stream()
                .map(SerializationUtils::toContentFile)
                .forEach(majorAddFiles::add);
            majorDeleteFiles.addAll(selectDeletedFiles(task, new HashSet<>()));
            partitionOptimizeType.put(entry.getKey(), task.getOptimizeTask().getTaskId().getType());
          }
        }
      }

      // commit minor optimize content
      minorCommit(arcticTable, minorAddFiles, minorDeleteFiles, toSequenceOfPartitions, fromSequenceOfPartitions);

      // commit major optimize content
      majorCommit(arcticTable, majorAddFiles, majorDeleteFiles, baseSnapshotId);

      return true;
    } catch (Exception e) {
      UnkeyedTable baseArcticTable;
      if (arcticTable.isKeyedTable()) {
        baseArcticTable = arcticTable.asKeyedTable().baseTable();
      } else {
        baseArcticTable = arcticTable.asUnkeyedTable();
      }
      LOG.warn("Optimize commit table {} failed, give up commit and clear files in location.", arcticTable.id(), e);
      // only delete data files are produced by major optimize, because the major optimize maybe support hive
      // and produce redundant data files in hive location.(don't produce DeleteFile)
      // minor produced files will be clean by orphan file clean
      Set<String> committedFilePath = getCommittedDataFilesFromSnapshotId(baseArcticTable, baseSnapshotId);
      for (ContentFile<?> majorAddFile : majorAddFiles) {
        String filePath = TableFileUtils.getUriPath(majorAddFile.path().toString());
        if (!committedFilePath.contains(filePath) && arcticTable.io().exists(filePath)) {
          arcticTable.io().deleteFile(filePath);
          LOG.warn("Delete orphan file {} when optimize commit failed", filePath);
        }
      }
      return false;
    }
  }

  public Map<String, OptimizeType> getPartitionOptimizeType() {
    return partitionOptimizeType;
  }

  protected boolean checkFileCount(OptimizeTaskItem task) {
    int baseFileCount = new HashSet<>(task.getOptimizeTask().getBaseFiles()).size();
    int insertFileCount = new HashSet<>(task.getOptimizeTask().getInsertFiles()).size();
    int deleteFileCount = new HashSet<>(task.getOptimizeTask().getDeleteFiles()).size();
    int posDeleteFileCount = new HashSet<>(task.getOptimizeTask().getPosDeleteFiles()).size();
    int targetFileCount = new HashSet<>(task.getOptimizeRuntime().getTargetFiles()).size();

    boolean result = baseFileCount == task.getOptimizeTask().getBaseFileCnt() &&
        insertFileCount == task.getOptimizeTask().getInsertFileCnt() &&
        deleteFileCount == task.getOptimizeTask().getDeleteFileCnt() &&
        posDeleteFileCount == task.getOptimizeTask().getPosDeleteFileCnt() &&
        targetFileCount == task.getOptimizeRuntime().getNewFileCnt();
    if (!result) {
      LOG.error("file count check failed. baseFileCount/baseFileCnt is {}/{}, " +
              "insertFileCount/insertFileCnt is {}/{}, deleteFileCount/deleteFileCnt is {}/{}, " +
              "posDeleteFileCount/posDeleteFileCnt is {}/{}, targetFileCount/newFileCnt is {}/{}",
          baseFileCount, task.getOptimizeTask().getBaseFileCnt(),
          insertFileCount, task.getOptimizeTask().getInsertFileCnt(),
          deleteFileCount, task.getOptimizeTask().getDeleteFileCnt(),
          posDeleteFileCount, task.getOptimizeTask().getPosDeleteFileCnt(),
          targetFileCount, task.getOptimizeRuntime().getNewFileCnt());
    }
    return !result;
  }

  private void minorCommit(ArcticTable arcticTable,
                           Set<ContentFile<?>> minorAddFiles,
                           Set<ContentFile<?>> minorDeleteFiles,
                           StructLikeMap<Long> toSequenceOfPartitions,
                           StructLikeMap<Long> fromSequenceOfPartitions) {
    UnkeyedTable baseArcticTable;
    if (arcticTable.isKeyedTable()) {
      baseArcticTable = arcticTable.asKeyedTable().baseTable();
    } else {
      baseArcticTable = arcticTable.asUnkeyedTable();
    }

    if (CollectionUtils.isNotEmpty(minorAddFiles) || CollectionUtils.isNotEmpty(minorDeleteFiles)) {
      OverwriteBaseFiles overwriteBaseFiles = new OverwriteBaseFiles(arcticTable.asKeyedTable());
      overwriteBaseFiles.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
      overwriteBaseFiles.validateNoConflictingAppends(Expressions.alwaysFalse());
      AtomicInteger addedPosDeleteFile = new AtomicInteger(0);
      StructLikeMap<Long> partitionOptimizedSequence =
          TablePropertyUtil.getPartitionOptimizedSequence(arcticTable.asKeyedTable());
      minorAddFiles.forEach(contentFile -> {
        // if partition from sequence isn't bigger than optimized sequence in partitionProperty,
        // the partition files is expired
        Long optimizedSequence = partitionOptimizedSequence.getOrDefault(contentFile.partition(), -1L);
        Long fromSequence = fromSequenceOfPartitions.getOrDefault(contentFile.partition(), Long.MAX_VALUE);
        if (optimizedSequence >= fromSequence) {
          toSequenceOfPartitions.remove(contentFile.partition());
          return;
        }

        if (contentFile.content() == FileContent.DATA) {
          overwriteBaseFiles.addFile((DataFile) contentFile);
        } else {
          overwriteBaseFiles.addFile((DeleteFile) contentFile);
          addedPosDeleteFile.incrementAndGet();
        }
      });
      AtomicInteger deletedPosDeleteFile = new AtomicInteger(0);
      Set<DeleteFile> deletedPosDeleteFiles = new HashSet<>();
      minorDeleteFiles.forEach(contentFile -> {
        // if partition from sequence isn't bigger than optimized sequence in partitionProperty,
        // the partition files is expired
        Long optimizedSequence = partitionOptimizedSequence.getOrDefault(contentFile.partition(), -1L);
        Long fromSequence = fromSequenceOfPartitions.getOrDefault(contentFile.partition(), Long.MAX_VALUE);
        if (optimizedSequence >= fromSequence) {
          toSequenceOfPartitions.remove(contentFile.partition());
          return;
        }

        if (contentFile.content() == FileContent.DATA) {
          overwriteBaseFiles.deleteFile((DataFile) contentFile);
        } else {
          deletedPosDeleteFiles.add((DeleteFile) contentFile);
        }
      });

      if (arcticTable.spec().isUnpartitioned()) {
        if (toSequenceOfPartitions.get(TablePropertyUtil.EMPTY_STRUCT) != null) {
          overwriteBaseFiles.updateOptimizedSequence(TablePropertyUtil.EMPTY_STRUCT,
              toSequenceOfPartitions.get(TablePropertyUtil.EMPTY_STRUCT));
        }
      } else {
        if (!toSequenceOfPartitions.isEmpty()) {
          toSequenceOfPartitions.forEach(overwriteBaseFiles::updateOptimizedSequence);
        }
      }
      overwriteBaseFiles.skipEmptyCommit().commit();

      if (CollectionUtils.isNotEmpty(deletedPosDeleteFiles)) {
        RewriteFiles rewriteFiles = baseArcticTable.newRewrite();
        rewriteFiles.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
        rewriteFiles.rewriteFiles(Collections.emptySet(), deletedPosDeleteFiles,
            Collections.emptySet(), Collections.emptySet());
        try {
          rewriteFiles.commit();
        } catch (ValidationException e) {
          LOG.warn("Iceberg RewriteFiles commit failed, but ignore", e);
        }
      }

      LOG.info("{} minor optimize committed, delete {} files [{} posDelete files], " +
              "add {} new files [{} posDelete files]",
          arcticTable.id(), minorDeleteFiles.size(), deletedPosDeleteFile.get(), minorAddFiles.size(),
          addedPosDeleteFile.get());
    } else {
      if (MapUtils.isNotEmpty(toSequenceOfPartitions)) {
        StructLikeMap<Long> partitionOptimizedSequence =
            TablePropertyUtil.getPartitionOptimizedSequence(arcticTable.asKeyedTable());
        UpdatePartitionProperties updatePartitionProperties =
            baseArcticTable.updatePartitionProperties(null);
        toSequenceOfPartitions.forEach((partition, toSequence) -> {
          long optimizedSequence = partitionOptimizedSequence.getOrDefault(partition, -1L);
          long maxSequence = Math.max(toSequence, optimizedSequence);
          updatePartitionProperties.set(partition, TableProperties.PARTITION_OPTIMIZED_SEQUENCE,
              String.valueOf(maxSequence));
        });
        updatePartitionProperties.commit();
      }

      LOG.info("{} skip minor optimize commit, but update partition txId", arcticTable.id());
    }
  }

  private void majorCommit(ArcticTable arcticTable,
                           Set<ContentFile<?>> majorAddFiles,
                           Set<ContentFile<?>> majorDeleteFiles,
                           long baseSnapshotId) {
    UnkeyedTable baseArcticTable;
    if (arcticTable.isKeyedTable()) {
      baseArcticTable = arcticTable.asKeyedTable().baseTable();
    } else {
      baseArcticTable = arcticTable.asUnkeyedTable();
    }

    if (CollectionUtils.isNotEmpty(majorAddFiles) || CollectionUtils.isNotEmpty(majorDeleteFiles)) {
      Set<DataFile> addDataFiles = majorAddFiles.stream().map(contentFile -> {
        if (contentFile.content() == FileContent.DATA) {
          return (DataFile) contentFile;
        }

        return null;
      }).filter(Objects::nonNull).collect(Collectors.toSet());
      Set<DeleteFile> addDeleteFiles = majorAddFiles.stream().map(contentFile -> {
        if (contentFile.content() == FileContent.POSITION_DELETES) {
          return (DeleteFile) contentFile;
        }

        return null;
      }).filter(Objects::nonNull).collect(Collectors.toSet());

      Set<DataFile> deleteDataFiles = majorDeleteFiles.stream().map(contentFile -> {
        if (contentFile.content() == FileContent.DATA) {
          return (DataFile) contentFile;
        }

        return null;
      }).filter(Objects::nonNull).collect(Collectors.toSet());
      Set<DeleteFile> deleteDeleteFiles = majorDeleteFiles.stream().map(contentFile -> {
        if (contentFile.content() == FileContent.POSITION_DELETES) {
          return (DeleteFile) contentFile;
        }

        return null;
      }).filter(Objects::nonNull).collect(Collectors.toSet());

      if (!addDeleteFiles.isEmpty()) {
        throw new IllegalArgumentException("for major optimize, can't add delete files " + addDeleteFiles);
      }

      // rewrite DataFiles
      RewriteFiles dataFilesRewrite = baseArcticTable.newRewrite();
      dataFilesRewrite.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
      if (baseSnapshotId != TableOptimizeRuntime.INVALID_SNAPSHOT_ID) {
        dataFilesRewrite.validateFromSnapshot(baseSnapshotId);
      }

      // if add DataFiles is empty, the DeleteFiles are must exist and apply to old DataFiles
      if (CollectionUtils.isEmpty(addDataFiles)) {
        dataFilesRewrite.rewriteFiles(deleteDataFiles, deleteDeleteFiles, addDataFiles, Collections.emptySet());
      } else {
        dataFilesRewrite.rewriteFiles(deleteDataFiles, Collections.emptySet(), addDataFiles, Collections.emptySet());
      }
      dataFilesRewrite.commit();

      // if add DataFiles is not empty, should remove DeleteFiles additional, because DeleteFiles maybe aren't existed
      if (CollectionUtils.isNotEmpty(addDataFiles) && CollectionUtils.isNotEmpty(deleteDeleteFiles)) {
        RewriteFiles removeDeleteFiles = baseArcticTable.newRewrite()
            .validateFromSnapshot(baseArcticTable.currentSnapshot().snapshotId());
        removeDeleteFiles.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
        removeDeleteFiles
            .rewriteFiles(Collections.emptySet(), deleteDeleteFiles, Collections.emptySet(), addDeleteFiles);
        try {
          removeDeleteFiles.commit();
        } catch (ValidationException e) {
          LOG.warn("Iceberg RewriteFiles commit failed, but ignore", e);
        }
      }

      LOG.info("{} major optimize committed, delete {} files [{} posDelete files], " +
              "add {} new files [{} posDelete files]",
          arcticTable.id(), majorDeleteFiles.size(), deleteDeleteFiles.size(), majorAddFiles.size(),
          addDeleteFiles.size());
    } else {
      LOG.info("{} skip major optimize commit", arcticTable.id());
    }
  }

  private static Set<ContentFile<?>> selectDeletedFiles(OptimizeTaskItem taskItem,
                                                        Set<ContentFile<?>> addPosDeleteFiles) {
    BasicOptimizeTask optimizeTask = taskItem.getOptimizeTask();
    OptimizeTaskRuntime optimizeTaskRuntime = taskItem.getOptimizeRuntime();
    switch (optimizeTask.getTaskId().getType()) {
      case FullMajor:
      case Major:
        return selectMajorOptimizeDeletedFiles(optimizeTask, optimizeTaskRuntime);
      case Minor:
        return selectMinorOptimizeDeletedFiles(optimizeTask, addPosDeleteFiles);
    }

    return new HashSet<>();
  }

  private static Set<ContentFile<?>> selectMinorOptimizeDeletedFiles(BasicOptimizeTask optimizeTask,
                                                                     Set<ContentFile<?>> addPosDeleteFiles) {
    Set<DataTreeNode> newFileNodes = addPosDeleteFiles.stream().map(contentFile -> {
      if (contentFile.content() == FileContent.POSITION_DELETES) {
        return FileNameGenerator.parseFileNodeFromFileName(contentFile.path().toString());
      }

      return null;
    }).filter(Objects::nonNull).collect(Collectors.toSet());

    return optimizeTask.getPosDeleteFiles().stream().map(SerializationUtils::toInternalTableFile)
        .filter(posDeleteFile ->
            newFileNodes.contains(FileNameGenerator.parseFileNodeFromFileName(posDeleteFile.path().toString())))
        .collect(Collectors.toSet());
  }

  private static Set<ContentFile<?>> selectMajorOptimizeDeletedFiles(BasicOptimizeTask optimizeTask,
                                                                     OptimizeTaskRuntime optimizeTaskRuntime) {
    // add base deleted files
    Set<ContentFile<?>> result = optimizeTask.getBaseFiles().stream()
        .map(SerializationUtils::toInternalTableFile).collect(Collectors.toSet());

    // if full optimize or new DataFiles is empty, can delete DeleteFiles
    if (optimizeTask.getTaskId().getType() == OptimizeType.FullMajor ||
        CollectionUtils.isEmpty(optimizeTaskRuntime.getTargetFiles())) {
      result.addAll(optimizeTask.getPosDeleteFiles().stream()
          .map(SerializationUtils::toInternalTableFile).collect(Collectors.toSet()));
    }

    return result;
  }

  private static Set<String> getCommittedDataFilesFromSnapshotId(UnkeyedTable table, Long snapshotId) {
    long currentSnapshotId = UnKeyedTableUtil.getSnapshotId(table);
    if (currentSnapshotId == TableOptimizeRuntime.INVALID_SNAPSHOT_ID) {
      return Collections.emptySet();
    }

    if (snapshotId == TableOptimizeRuntime.INVALID_SNAPSHOT_ID) {
      snapshotId = null;
    }

    Set<String> committedFilePath = new HashSet<>();
    for (Snapshot snapshot : SnapshotUtil.ancestorsBetween(currentSnapshotId, snapshotId, table::snapshot)) {
      for (DataFile dataFile : snapshot.addedFiles()) {
        committedFilePath.add(TableFileUtils.getUriPath(dataFile.path().toString()));
      }
    }

    return committedFilePath;
  }
}
