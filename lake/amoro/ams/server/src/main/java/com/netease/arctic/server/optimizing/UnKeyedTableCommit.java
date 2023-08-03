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

package com.netease.arctic.server.optimizing;

import com.netease.arctic.ams.api.CommitMetaProducer;
import com.netease.arctic.data.FileNameRules;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.HivePartitionUtil;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.optimizing.OptimizingInputProperties;
import com.netease.arctic.optimizing.RewriteFilesOutput;
import com.netease.arctic.server.ArcticServiceConstants;
import com.netease.arctic.server.exception.OptimizingCommitException;
import com.netease.arctic.server.utils.IcebergTableUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.trace.SnapshotSummary;
import com.netease.arctic.utils.TableFileUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.exceptions.ValidationException;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.SnapshotUtil;
import org.glassfish.jersey.internal.guava.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.netease.arctic.hive.op.UpdateHiveFiles.DELETE_UNTRACKED_HIVE_FILE;
import static com.netease.arctic.server.ArcticServiceConstants.INVALID_SNAPSHOT_ID;

public class UnKeyedTableCommit {
  private static final Logger LOG = LoggerFactory.getLogger(UnKeyedTableCommit.class);

  private final Long targetSnapshotId;
  private final ArcticTable table;
  private final Collection<TaskRuntime> tasks;

  public UnKeyedTableCommit(Long targetSnapshotId, ArcticTable table, Collection<TaskRuntime> tasks) {
    this.targetSnapshotId = targetSnapshotId;
    this.table = table;
    this.tasks = tasks;
  }

  protected List<DataFile> moveFile2HiveIfNeed() {
    if (!needMoveFile2Hive()) {
      return null;
    }

    HMSClientPool hiveClient = ((SupportHive) table).getHMSClient();
    Map<String, String> partitionPathMap = new HashMap<>();
    Types.StructType partitionSchema = table.isUnkeyedTable() ?
        table.asUnkeyedTable().spec().partitionType() :
        table.asKeyedTable().baseTable().spec().partitionType();

    List<DataFile> newTargetFiles = new ArrayList<>();
    for (TaskRuntime taskRuntime : tasks) {
      RewriteFilesOutput output = taskRuntime.getOutput();
      DataFile[] dataFiles = output.getDataFiles();
      if (dataFiles == null) {
        continue;
      }

      List<DataFile> targetFiles = Arrays.stream(output.getDataFiles()).collect(Collectors.toList());

      long maxTransactionId = targetFiles.stream()
          .mapToLong(dataFile -> FileNameRules.parseTransactionId(dataFile.path().toString()))
          .max()
          .orElse(0L);

      for (DataFile targetFile : targetFiles) {
        if (partitionPathMap.get(taskRuntime.getPartition()) == null) {
          List<String> partitionValues =
              HivePartitionUtil.partitionValuesAsList(targetFile.partition(), partitionSchema);
          String partitionPath;
          if (table.spec().isUnpartitioned()) {
            try {
              Table hiveTable = ((SupportHive) table).getHMSClient().run(client ->
                  client.getTable(table.id().getDatabase(), table.id().getTableName()));
              partitionPath = hiveTable.getSd().getLocation();
            } catch (Exception e) {
              LOG.error("Get hive table failed", e);
              throw new RuntimeException(e);
            }
          } else {
            String hiveSubdirectory = table.isKeyedTable() ?
                HiveTableUtil.newHiveSubdirectory(maxTransactionId) : HiveTableUtil.newHiveSubdirectory();

            Partition partition = HivePartitionUtil.getPartition(hiveClient, table, partitionValues);
            if (partition == null) {
              partitionPath = HiveTableUtil.newHiveDataLocation(((SupportHive) table).hiveLocation(),
                  table.spec(), targetFile.partition(), hiveSubdirectory);
            } else {
              partitionPath = partition.getSd().getLocation();
            }
          }
          partitionPathMap.put(taskRuntime.getPartition(), partitionPath);
        }

        DataFile finalDataFile = moveTargetFiles(targetFile, partitionPathMap.get(taskRuntime.getPartition()));
        newTargetFiles.add(finalDataFile);
      }
    }
    return newTargetFiles;
  }

  public void commit() throws OptimizingCommitException {
    LOG.info("{} getRuntime tasks to commit {}", table.id(), tasks);

    List<DataFile> hiveNewDataFiles = moveFile2HiveIfNeed();
    // collect files
    Set<DataFile> addedDataFiles = Sets.newHashSet();
    Set<DataFile> removedDataFiles = Sets.newHashSet();
    Set<DeleteFile> addedDeleteFiles = Sets.newHashSet();
    Set<DeleteFile> removedDeleteFiles = Sets.newHashSet();
    for (TaskRuntime task : tasks) {
      if (CollectionUtils.isNotEmpty(hiveNewDataFiles)) {
        addedDataFiles.addAll(hiveNewDataFiles);
      } else if (task.getOutput().getDataFiles() != null) {
        addedDataFiles.addAll(Arrays.asList(task.getOutput().getDataFiles()));
      }
      if (task.getOutput().getDeleteFiles() != null) {
        addedDeleteFiles.addAll(Arrays.asList(task.getOutput().getDeleteFiles()));
      }
      if (task.getInput().rewrittenDataFiles() != null) {
        removedDataFiles.addAll(Arrays.asList(task.getInput().rewrittenDataFiles()));
      }
      if (task.getInput().rewrittenDeleteFiles() != null) {
        removedDeleteFiles.addAll(Arrays.stream(task.getInput().rewrittenDeleteFiles())
            .map(IcebergContentFile::asDeleteFile).collect(Collectors.toSet()));
      }
    }

    UnkeyedTable icebergTable = table.asUnkeyedTable();

    replaceFiles(icebergTable, removedDataFiles, addedDataFiles, addedDeleteFiles);

    removeOldDeleteFiles(icebergTable, removedDeleteFiles);
  }

  protected void replaceFiles(
      UnkeyedTable icebergTable,
      Set<DataFile> removedDataFiles,
      Set<DataFile> addedDataFiles,
      Set<DeleteFile> addDeleteFiles) throws OptimizingCommitException {
    try {
      Transaction transaction = icebergTable.newTransaction();
      if (CollectionUtils.isNotEmpty(removedDataFiles) ||
          CollectionUtils.isNotEmpty(addedDataFiles)) {
        RewriteFiles dataFileRewrite = transaction.newRewrite();
        if (targetSnapshotId != ArcticServiceConstants.INVALID_SNAPSHOT_ID) {
          dataFileRewrite.validateFromSnapshot(targetSnapshotId);
          long sequenceNumber = table.asUnkeyedTable().snapshot(targetSnapshotId).sequenceNumber();
          dataFileRewrite.rewriteFiles(removedDataFiles, addedDataFiles, sequenceNumber);
        } else {
          dataFileRewrite.rewriteFiles(removedDataFiles, addedDataFiles);
        }
        dataFileRewrite.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
        if (TableTypeUtil.isHive(table) && !needMoveFile2Hive()) {
          dataFileRewrite.set(DELETE_UNTRACKED_HIVE_FILE, "true");
        }
        dataFileRewrite.commit();
      }
      if (CollectionUtils.isNotEmpty(addDeleteFiles)) {
        RowDelta addDeleteFileRowDelta = transaction.newRowDelta();
        addDeleteFiles.forEach(addDeleteFileRowDelta::addDeletes);
        addDeleteFileRowDelta.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
        addDeleteFileRowDelta.commit();
      }
      transaction.commitTransaction();
    } catch (Exception e) {
      if (needMoveFile2Hive()) {
        correctHiveData(addedDataFiles, addDeleteFiles);
      }
      LOG.warn("Optimize commit table {} failed, give up commit.", table.id(), e);
      throw new OptimizingCommitException("unexpected commit error ", e);
    }
  }

  protected void removeOldDeleteFiles(
      UnkeyedTable icebergTable,
      Set<DeleteFile> removedDeleteFiles) {
    if (CollectionUtils.isEmpty(removedDeleteFiles)) {
      return;
    }

    RewriteFiles deleteFileRewrite = icebergTable.newRewrite();
    deleteFileRewrite.rewriteFiles(Collections.emptySet(),
        removedDeleteFiles, Collections.emptySet(), Collections.emptySet());
    deleteFileRewrite.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());

    try {
      deleteFileRewrite.commit();
    } catch (ValidationException e) {
      // Iceberg will drop DeleteFiles that are older than the min Data sequence number. So some DeleteFiles
      // maybe already dropped in the last commit, the exception can be ignored.
      LOG.warn("Iceberg RewriteFiles commit failed, but ignore", e);
    }
  }

  protected boolean needMoveFile2Hive() {
    return OptimizingInputProperties.parse(
        tasks.stream().findAny().get().getProperties()).getMoveFile2HiveLocation();
  }

  protected void correctHiveData(Set<DataFile> addedDataFiles, Set<DeleteFile> addedDeleteFiles)
      throws OptimizingCommitException {
    try {
      UnkeyedTable baseArcticTable;
      if (table.isKeyedTable()) {
        baseArcticTable = table.asKeyedTable().baseTable();
      } else {
        baseArcticTable = table.asUnkeyedTable();
      }
      LOG.warn("Optimize commit table {} failed, give up commit and clear files in location.", table.id());
      // only delete data files are produced by major optimize, because the major optimize maybe support hive
      // and produce redundant data files in hive location.(don't produce DeleteFile)
      // minor produced files will be clean by orphan file clean
      Set<String> committedFilePath = getCommittedDataFilesFromSnapshotId(baseArcticTable, targetSnapshotId);
      for (ContentFile<?> addedDataFile : addedDataFiles) {
        deleteUncommittedFile(committedFilePath, addedDataFile);
      }
      for (ContentFile<?> addedDeleteFile : addedDeleteFiles) {
        deleteUncommittedFile(committedFilePath, addedDeleteFile);
      }
    } catch (Exception ex) {
      throw new OptimizingCommitException(
          "An exception was encountered when the commit failed to clear the file",
          true);
    }
  }

  private void deleteUncommittedFile(Set<String> committedFilePath, ContentFile<?> majorAddFile) {
    String filePath = TableFileUtil.getUriPath(majorAddFile.path().toString());
    if (!committedFilePath.contains(filePath) && table.io().exists(filePath)) {
      table.io().deleteFile(filePath);
      LOG.warn("Delete orphan file {} when optimize commit failed", filePath);
    }
  }

  private DataFile moveTargetFiles(DataFile targetFile, String hiveLocation) {
    String oldFilePath = targetFile.path().toString();
    String newFilePath = TableFileUtil.getNewFilePath(hiveLocation, oldFilePath);

    if (!table.io().exists(newFilePath)) {
      if (!table.io().exists(hiveLocation)) {
        LOG.debug("{} hive location {} does not exist and need to mkdir before rename", table.id(), hiveLocation);
        table.io().asFileSystemIO().makeDirectories(hiveLocation);
      }
      table.io().asFileSystemIO().rename(oldFilePath, newFilePath);
      LOG.debug("{} move file from {} to {}", table.id(), oldFilePath, newFilePath);
    }

    // org.apache.iceberg.BaseFile.set
    ((StructLike) targetFile).set(1, newFilePath);
    return targetFile;
  }

  private static Set<String> getCommittedDataFilesFromSnapshotId(UnkeyedTable table, Long snapshotId) {
    long currentSnapshotId = IcebergTableUtil.getSnapshotId(table, true);
    if (currentSnapshotId == INVALID_SNAPSHOT_ID) {
      return Collections.emptySet();
    }

    if (snapshotId == INVALID_SNAPSHOT_ID) {
      snapshotId = null;
    }

    Set<String> committedFilePath = new HashSet<>();
    for (Snapshot snapshot : SnapshotUtil.ancestorsBetween(currentSnapshotId, snapshotId, table::snapshot)) {
      for (DataFile dataFile : snapshot.addedDataFiles(table.io())) {
        committedFilePath.add(TableFileUtil.getUriPath(dataFile.path().toString()));
      }
    }

    return committedFilePath;
  }
}
