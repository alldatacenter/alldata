package com.netease.arctic.server.optimizing;

import com.netease.arctic.ams.api.CommitMetaProducer;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.op.OverwriteBaseFiles;
import com.netease.arctic.optimizing.RewriteFilesInput;
import com.netease.arctic.optimizing.RewriteFilesOutput;
import com.netease.arctic.server.exception.OptimizingCommitException;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.trace.SnapshotSummary;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.RewriteFiles;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.exceptions.ValidationException;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.util.StructLikeMap;
import org.glassfish.jersey.internal.guava.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.netease.arctic.hive.op.UpdateHiveFiles.DELETE_UNTRACKED_HIVE_FILE;
import static com.netease.arctic.server.ArcticServiceConstants.INVALID_SNAPSHOT_ID;

public class KeyedTableCommit extends UnKeyedTableCommit {

  private static final Logger LOG = LoggerFactory.getLogger(KeyedTableCommit.class);

  protected ArcticTable table;

  protected Collection<TaskRuntime> tasks;

  protected Long fromSnapshotId;

  protected StructLikeMap<Long> fromSequenceOfPartitions;

  protected StructLikeMap<Long> toSequenceOfPartitions;

  public KeyedTableCommit(
      ArcticTable table, Collection<TaskRuntime> tasks, Long fromSnapshotId,
      StructLikeMap<Long> fromSequenceOfPartitions, StructLikeMap<Long> toSequenceOfPartitions) {
    super(fromSnapshotId, table, tasks);
    this.table = table;
    this.tasks = tasks;
    this.fromSnapshotId = fromSnapshotId == null ? INVALID_SNAPSHOT_ID : fromSnapshotId;
    this.fromSequenceOfPartitions = fromSequenceOfPartitions;
    this.toSequenceOfPartitions = toSequenceOfPartitions;
  }

  @Override
  public void commit() throws OptimizingCommitException {
    if (tasks.isEmpty()) {
      LOG.info("{} getRuntime no tasks to commit", table.id());
    }
    LOG.info("{} getRuntime tasks to commit with from snapshot id = {}", table.id(),
        fromSnapshotId);

    //In the scene of moving files to hive, the files will be renamed
    List<DataFile> hiveNewDataFiles = moveFile2HiveIfNeed();

    Set<DataFile> addedDataFiles = Sets.newHashSet();
    Set<DataFile> removedDataFiles = Sets.newHashSet();
    Set<DeleteFile> addedDeleteFiles = Sets.newHashSet();
    Set<DeleteFile> removedDeleteFiles = Sets.newHashSet();

    StructLikeMap<Long> partitionOptimizedSequence =
        TablePropertyUtil.getPartitionOptimizedSequence(table.asKeyedTable());

    for (TaskRuntime taskRuntime : tasks) {
      RewriteFilesInput input = taskRuntime.getInput();
      StructLike partition = partition(input);

      //Check if the partition version has expired
      if (fileInPartitionNeedSkip(partition, partitionOptimizedSequence, fromSequenceOfPartitions)) {
        toSequenceOfPartitions.remove(partition);
        continue;
      }
      //Only base data file need to remove
      if (input.rewrittenDataFiles() != null) {
        Arrays.stream(input.rewrittenDataFiles())
            .map(s -> (PrimaryKeyedFile) s.asDataFile().internalDataFile())
            .filter(s -> s.type() == DataFileType.BASE_FILE)
            .forEach(removedDataFiles::add);
      }

      //Only position delete need to remove
      if (input.rewrittenDeleteFiles() != null) {
        Arrays.stream(input.rewrittenDeleteFiles())
            .filter(IcebergContentFile::isDeleteFile)
            .map(IcebergContentFile::asDeleteFile)
            .forEach(removedDeleteFiles::add);
      }

      RewriteFilesOutput output = taskRuntime.getOutput();
      if (CollectionUtils.isNotEmpty(hiveNewDataFiles)) {
        addedDataFiles.addAll(hiveNewDataFiles);
      } else if (output.getDataFiles() != null) {
        Collections.addAll(addedDataFiles, output.getDataFiles());
      }

      if (output.getDeleteFiles() != null) {
        Collections.addAll(addedDeleteFiles, output.getDeleteFiles());
      }
    }

    try {
      executeCommit(addedDataFiles, removedDataFiles, addedDeleteFiles, removedDeleteFiles);
    } catch (Exception e) {
      //Only failures to clean files will trigger a retry
      LOG.warn("Optimize commit table {} failed, give up commit.", table.id(), e);

      if (needMoveFile2Hive()) {
        correctHiveData(addedDataFiles, addedDeleteFiles);
      }
      throw new OptimizingCommitException("unexpected commit error ", e);
    }
  }

  private void executeCommit(
      Set<DataFile> addedDataFiles,
      Set<DataFile> removedDataFiles,
      Set<DeleteFile> addedDeleteFiles,
      Set<DeleteFile> removedDeleteFiles) {
    //overwrite files
    OverwriteBaseFiles overwriteBaseFiles = new OverwriteBaseFiles(table.asKeyedTable());
    overwriteBaseFiles.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
    overwriteBaseFiles.validateNoConflictingAppends(Expressions.alwaysFalse());
    overwriteBaseFiles.dynamic(false);
    toSequenceOfPartitions.forEach(overwriteBaseFiles::updateOptimizedSequence);
    addedDataFiles.forEach(overwriteBaseFiles::addFile);
    addedDeleteFiles.forEach(overwriteBaseFiles::addFile);
    removedDataFiles.forEach(overwriteBaseFiles::deleteFile);
    if (TableTypeUtil.isHive(table) && !needMoveFile2Hive()) {
      overwriteBaseFiles.set(DELETE_UNTRACKED_HIVE_FILE, "true");
    }
    overwriteBaseFiles.skipEmptyCommit().commit();

    //remove delete files
    if (CollectionUtils.isNotEmpty(removedDeleteFiles)) {
      RewriteFiles rewriteFiles = table.asKeyedTable().baseTable().newRewrite();
      rewriteFiles.set(SnapshotSummary.SNAPSHOT_PRODUCER, CommitMetaProducer.OPTIMIZE.name());
      rewriteFiles.rewriteFiles(Collections.emptySet(), removedDeleteFiles,
          Collections.emptySet(), Collections.emptySet());
      try {
        rewriteFiles.commit();
      } catch (ValidationException e) {
        LOG.warn("Iceberg RewriteFiles commit failed, but ignore", e);
      }
    }

    LOG.info("{} optimize committed, delete {} files [{} posDelete files], " +
            "add {} new files [{} posDelete files]",
        table.id(), removedDataFiles.size(), removedDeleteFiles.size(), addedDataFiles.size(),
        addedDataFiles.size());
  }

  private boolean fileInPartitionNeedSkip(
      StructLike partitionData,
      StructLikeMap<Long> partitionOptimizedSequence,
      StructLikeMap<Long> fromSequenceOfPartitions) {
    Long optimizedSequence = partitionOptimizedSequence.getOrDefault(partitionData, -1L);
    Long fromSequence = fromSequenceOfPartitions.getOrDefault(partitionData, Long.MAX_VALUE);

    return optimizedSequence >= fromSequence;
  }

  private StructLike partition(RewriteFilesInput input) {
    return input.allFiles()[0].partition();
  }
}
