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
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.trace.SnapshotSummary;
import com.netease.arctic.utils.SerializationUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class IcebergOptimizeCommit extends BasicOptimizeCommit {
  private static final Logger LOG = LoggerFactory.getLogger(IcebergOptimizeCommit.class);

  public IcebergOptimizeCommit(ArcticTable arcticTable,
                               Map<String, List<OptimizeTaskItem>> optimizeTasksToCommit) {
    super(arcticTable, optimizeTasksToCommit);
  }

  public boolean commit(long baseSnapshotId) throws Exception {
    try {
      if (optimizeTasksToCommit.isEmpty()) {
        LOG.info("{} get no tasks to commit", arcticTable.id());
        return true;
      }
      LOG.info("{} get tasks to commit for partitions {}", arcticTable.id(),
          optimizeTasksToCommit.keySet());

      // collect files
      Set<ContentFile<?>> minorAddFiles = new HashSet<>();
      Set<ContentFile<?>> minorDeleteFiles = new HashSet<>();
      Set<ContentFile<?>> majorAddFiles = new HashSet<>();
      Set<ContentFile<?>> majorDeleteFiles = new HashSet<>();
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

            minorDeleteFiles.addAll(selectDeletedFiles(task));
            partitionOptimizeType.put(entry.getKey(), OptimizeType.Minor);
          } else {
            task.getOptimizeRuntime().getTargetFiles().stream()
                .map(SerializationUtils::toContentFile)
                .forEach(majorAddFiles::add);
            majorDeleteFiles.addAll(selectDeletedFiles(task));
            partitionOptimizeType.put(entry.getKey(), task.getOptimizeTask().getTaskId().getType());
          }
        }
      }

      // commit minor optimize content
      minorCommit(arcticTable, minorAddFiles, minorDeleteFiles, baseSnapshotId);

      // commit major optimize content
      majorCommit(arcticTable, majorAddFiles, majorDeleteFiles, baseSnapshotId);

      return true;
    } catch (Exception e) {
      LOG.warn("Optimize commit table {} failed, give up commit.", arcticTable.id(), e);
      return false;
    }
  }

  public Map<String, OptimizeType> getPartitionOptimizeType() {
    return partitionOptimizeType;
  }

  private void minorCommit(ArcticTable arcticTable,
                           Set<ContentFile<?>> minorAddFiles,
                           Set<ContentFile<?>> minorDeleteFiles,
                           long baseSnapshotId) {
    UnkeyedTable baseArcticTable = arcticTable.asUnkeyedTable();

    if (CollectionUtils.isNotEmpty(minorAddFiles) || CollectionUtils.isNotEmpty(minorDeleteFiles)) {
      LOG.info("{} use RewriteFiles to commit.", arcticTable.id());
      Set<DataFile> addDataFiles = new HashSet<>();
      Set<DataFile> deleteDataFiles = new HashSet<>();
      Set<DeleteFile> addDeleteFiles = new HashSet<>();
      Set<DeleteFile> deletedDeleteFiles = new HashSet<>();
      minorAddFiles.forEach(contentFile -> {
        if (contentFile.content() == FileContent.DATA) {
          addDataFiles.add((DataFile) contentFile);
        } else {
          addDeleteFiles.add((DeleteFile) contentFile);
        }
      });
      minorDeleteFiles.forEach(contentFile -> {
        if (contentFile.content() == FileContent.DATA) {
          deleteDataFiles.add((DataFile) contentFile);
        } else {
          deletedDeleteFiles.add((DeleteFile) contentFile);
        }
      });


      Transaction minorTransaction = baseArcticTable.newTransaction();

      if (CollectionUtils.isNotEmpty(deleteDataFiles) || CollectionUtils.isNotEmpty(addDataFiles)) {
        RewriteFiles dataFileRewrite = minorTransaction.newRewrite();
        if (baseSnapshotId != TableOptimizeRuntime.INVALID_SNAPSHOT_ID) {
          dataFileRewrite.validateFromSnapshot(baseSnapshotId);
          long sequenceNumber = arcticTable.asUnkeyedTable().snapshot(baseSnapshotId).sequenceNumber();
          dataFileRewrite.rewriteFiles(deleteDataFiles, addDataFiles, sequenceNumber);
        } else {
          dataFileRewrite.rewriteFiles(deleteDataFiles, addDataFiles);
        }
        dataFileRewrite.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
        dataFileRewrite.commit();
      }

      if (CollectionUtils.isNotEmpty(deletedDeleteFiles) || CollectionUtils.isNotEmpty(addDeleteFiles)) {
        RewriteFiles deleteFileRewrite = minorTransaction.newRewrite();
        deleteFileRewrite.rewriteFiles(Collections.emptySet(),
            deletedDeleteFiles, Collections.emptySet(), addDeleteFiles);
        deleteFileRewrite.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
        deleteFileRewrite.commit();
      }

      minorTransaction.commitTransaction();

      LOG.info("{} iceberg minor optimize committed, delete {} data files and {} delete files, " +
          "add {} new data files and {} new delete files",
          arcticTable.id(), deleteDataFiles.size(), deletedDeleteFiles, addDataFiles.size(), addDeleteFiles);
    } else {
      LOG.info("{} skip iceberg minor optimize commit", arcticTable.id());
    }
  }

  private void majorCommit(ArcticTable arcticTable,
                           Set<ContentFile<?>> majorAddFiles,
                           Set<ContentFile<?>> majorDeleteFiles,
                           long baseSnapshotId) {
    UnkeyedTable baseArcticTable = arcticTable.asUnkeyedTable();

    if (CollectionUtils.isNotEmpty(majorAddFiles) || CollectionUtils.isNotEmpty(majorDeleteFiles)) {
      Set<DataFile> addDataFiles = majorAddFiles.stream().map(contentFile -> {
        if (contentFile.content() == FileContent.DATA) {
          return (DataFile) contentFile;
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
        if (contentFile.content() != FileContent.DATA) {
          return (DeleteFile) contentFile;
        }

        return null;
      }).filter(Objects::nonNull).collect(Collectors.toSet());

      // rewrite DataFiles
      RewriteFiles rewriteDataFiles = baseArcticTable.newRewrite();
      if (baseSnapshotId != TableOptimizeRuntime.INVALID_SNAPSHOT_ID) {
        rewriteDataFiles.validateFromSnapshot(baseSnapshotId);
        long sequenceNumber = arcticTable.asUnkeyedTable().snapshot(baseSnapshotId).sequenceNumber();
        rewriteDataFiles.rewriteFiles(deleteDataFiles, addDataFiles, sequenceNumber);
      } else {
        rewriteDataFiles.rewriteFiles(deleteDataFiles, addDataFiles);
      }
      rewriteDataFiles.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
      rewriteDataFiles.commit();

      // remove DeleteFiles additional
      if (CollectionUtils.isNotEmpty(deleteDeleteFiles)) {
        RewriteFiles rewriteDeleteFiles = baseArcticTable.newRewrite();
        rewriteDeleteFiles.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
        rewriteDeleteFiles.rewriteFiles(Collections.emptySet(), deleteDeleteFiles,
            Collections.emptySet(), Collections.emptySet());
        try {
          rewriteDeleteFiles.commit();
        } catch (ValidationException e) {
          // Iceberg will drop DeleteFiles that are older than the min Data sequence number. So some DeleteFiles
          // maybe already dropped in the last commit, the exception can be ignored.
          LOG.warn("Iceberg RewriteFiles commit failed, but ignore", e);
        }
      }

      LOG.info("{} major optimize committed, delete {} files [{} Delete files], " +
              "add {} new files",
          arcticTable.id(), majorDeleteFiles.size(), deleteDeleteFiles.size(), majorAddFiles.size());
    } else {
      LOG.info("{} skip major optimize commit", arcticTable.id());
    }
  }

  private static Set<ContentFile<?>> selectDeletedFiles(OptimizeTaskItem taskItem) {
    BasicOptimizeTask optimizeTask = taskItem.getOptimizeTask();
    switch (optimizeTask.getTaskId().getType()) {
      case FullMajor:
        return selectMajorOptimizeDeletedFiles(optimizeTask);
      case Minor:
        return selectMinorOptimizeDeletedFiles(optimizeTask);
    }

    return new HashSet<>();
  }

  private static Set<ContentFile<?>> selectMinorOptimizeDeletedFiles(BasicOptimizeTask optimizeTask) {
    Set<ContentFile<?>> deletedFiles = new HashSet<>();

    if (CollectionUtils.isNotEmpty(optimizeTask.getInsertFiles())) {
      // small data files
      for (ByteBuffer insertFile : optimizeTask.getInsertFiles()) {
        deletedFiles.add(SerializationUtils.toIcebergContentFile(insertFile));
      }
    } else {
      // delete files
      for (ByteBuffer eqDeleteFile : optimizeTask.getDeleteFiles()) {
        deletedFiles.add(SerializationUtils.toIcebergContentFile(eqDeleteFile));
      }
      for (ByteBuffer posDeleteFile : optimizeTask.getPosDeleteFiles()) {
        deletedFiles.add(SerializationUtils.toIcebergContentFile(posDeleteFile));
      }
    }

    return deletedFiles;
  }

  private static Set<ContentFile<?>> selectMajorOptimizeDeletedFiles(BasicOptimizeTask optimizeTask) {
    Set<ContentFile<?>> deletedFiles = new HashSet<>();
    // data files
    for (ByteBuffer insertFile : optimizeTask.getInsertFiles()) {
      deletedFiles.add(SerializationUtils.toIcebergContentFile(insertFile));
    }
    for (ByteBuffer baseFile : optimizeTask.getBaseFiles()) {
      deletedFiles.add(SerializationUtils.toIcebergContentFile(baseFile));
    }

    // delete files
    for (ByteBuffer eqDeleteFile : optimizeTask.getDeleteFiles()) {
      deletedFiles.add(SerializationUtils.toIcebergContentFile(eqDeleteFile));
    }
    for (ByteBuffer posDeleteFile : optimizeTask.getPosDeleteFiles()) {
      deletedFiles.add(SerializationUtils.toIcebergContentFile(posDeleteFile));
    }

    return deletedFiles;
  }
}