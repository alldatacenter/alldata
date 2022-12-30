/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink;

import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.base.dirty.impl.NoOpDirtyCollector;
import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filestate.FileStateCollector;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.FileSystemFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionTempFileManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.directory.PartitionDirectoryManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.MetricsFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.dirty.MultiFileHdfsDirtyCollector;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.dirty.SingleFileHdfsDirtyCollector;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.filestate.AbstractFileState;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs.HdfsPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive.HivePartitionCommitter;

import lombok.Getter;
import org.apache.curator.utils.CloseableUtils;
import org.apache.flink.annotation.Internal;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.HDFS_CREATE_FILE_COUNT;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.HDFS_RENAME_FAILED_COUNT;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.HDFS_RENAME_SKIP_COUNT;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.HDFS_RENAME_SUCCESS_COUNT;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionTempFileManager.collectPartSpecToPaths;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionTempFileManager.deleteCheckpoint;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionTempFileManager.deleteTaskCheckpoint;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionTempFileManager.headCheckpoints;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionTempFileManager.listInvalidTaskTemporaryPaths;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionTempFileManager.listTaskTemporaryPaths;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.DIRTY_COLLECTOR_TYPE_MULTI_FILE_HDFS;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.DIRTY_COLLECTOR_TYPE_SINGLE_FILE_HDFS;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_FORMAT_TYPE_VALUE;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HIVE_FORMAT_TYPE_VALUE;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.JOB_ID_VALUE_DEFAULT;

/**
 * File system file committer implementation. It move all files to output path from temporary path.
 *
 * <p>In a checkpoint:
 * 1.Every task will invoke {@link #createTempFileManager} to initialization, it returns
 * a path generator to generate path for task writing. And clean the temporary path of task.
 * 2.After writing done for this checkpoint, need invoke {@link #commitUpToCheckpoint(long)},
 * will move the temporary files to real output path.
 *
 * <p>Batch is a special case of Streaming, which has only one checkpoint.
 *
 * <p>Data consistency:
 * 1.For task failure: will launch a new task and invoke {@link #createTempFileManager},
 * this will clean previous temporary files (This simple design can make it easy to delete the
 * invalid temporary directory of the task, but it also causes that our directory does not
 * support the same task to start multiple backups to run).
 * 2.For job master commit failure when overwrite: this may result in unfinished intermediate
 * results, but if we try to run job again, the final result must be correct (because the
 * intermediate result will be overwritten).
 * 3.For job master commit failure when append: This can lead to inconsistent data. But,
 * considering that the commit action is a single point of execution, and only moves files and
 * updates metadata, it will be faster, so the probability of inconsistency is relatively small.
 *
 * <p>See:
 * {@link PartitionTempFileManager}.
 * {@link PartitionLoader}.
 */
@Internal
@Getter
public class FileSystemCommitter implements Serializable, Closeable {
  private static final Logger LOG =
      LoggerFactory.getLogger(FileSystemCommitter.class);

  private static final long serialVersionUID = 1L;
  private final FileSystemFactory factory;
  private final TableMetaStoreFactory metaStoreFactory;
  private final boolean overwrite;
  private final Path tmpPath;
  private final LinkedHashMap<String, String> staticPartitions;
  private final int partitionColumnSize;
  @Getter
  private final String compressionExtension;
  private final BitSailConfiguration jobConf;
  private final String formatType;
  private final Long jobId;
  private final PartitionDirectoryManager.DirectoryType directoryManager;
  private final List<PartitionInfo> partitionInfos;
  private final int hourIndex;
  private final Path locationPath;
  private int subTaskId = Integer.MAX_VALUE;
  private transient FileSystem fileSystem;
  private transient PartitionLoader loader;
  private transient MetricManager metrics;

  public FileSystemCommitter(
      FileSystemFactory factory,
      TableMetaStoreFactory metaStoreFactory,
      boolean overwrite,
      Path tmpPath,
      LinkedHashMap<String, String> staticPartitions,
      int partitionColumnSize) {
    this(factory, metaStoreFactory, overwrite, tmpPath, staticPartitions, partitionColumnSize,
        null, BitSailConfiguration.newDefault());
  }

  public FileSystemCommitter(
      FileSystemFactory factory,
      TableMetaStoreFactory metaStoreFactory,
      boolean overwrite,
      Path tmpPath,
      LinkedHashMap<String, String> staticPartitions,
      int partitionColumnSize,
      String compressionExtension,
      BitSailConfiguration jobConf) {
    this.directoryManager = PartitionDirectoryManager.getDirectoryType(jobConf);
    this.factory = factory;
    this.metaStoreFactory = metaStoreFactory;
    this.overwrite = overwrite;
    this.tmpPath = tmpPath;
    this.staticPartitions = staticPartitions;
    this.partitionColumnSize = partitionColumnSize;
    this.compressionExtension = compressionExtension;
    this.jobConf = jobConf;
    this.formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);
    this.jobId = jobConf.getUnNecessaryOption(CommonOptions.JOB_ID, JOB_ID_VALUE_DEFAULT);
    this.partitionInfos = PartitionUtils.getPartitionInfo(jobConf);
    try (TableMetaStoreFactory.TableMetaStore metaStore = metaStoreFactory.createTableMetaStore()) {
      this.locationPath = metaStore.getLocationPath();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    this.hourIndex = PartitionUtils.getHourPartitionIndex(partitionInfos);

  }

  /**
   * For committing job's output after successful batch job completion or one checkpoint finish
   * for streaming job. Should move all files to final output paths.
   *
   * <p>NOTE: According to checkpoint notify mechanism of Flink, checkpoint may fail and be
   * abandoned, so this method should commit all checkpoint ids that less than current
   * checkpoint id (Includes failure checkpoints).
   */
  public void commitUpToCheckpoint(long toCpId) throws Exception {
    FileSystem fs = getOrCreateFileSystem();
    if (!fs.exists(tmpPath)) {
      return;
    }

    for (long cp : headCheckpoints(fs, tmpPath, toCpId, true)) {
      List<Path> taskPaths = listTaskTemporaryPaths(fs, tmpPath, cp, Integer.MAX_VALUE);
      commitPendingTaskCheckpoint(fs, getOrCreatePartitionLoader(), taskPaths);
    }
  }

  public void commitTaskCheckpoint(AbstractFileState fileState) throws Exception {
    FileSystem fs = getOrCreateFileSystem();
    if (!fs.exists(tmpPath)) {
      return;
    }
    Map<LinkedHashMap<String, String>, List<Path>> partSpecToFilePathMap = fileState.getPartSpecToFilePathMap(this);
    int totalCount = 0;
    for (List<Path> paths : partSpecToFilePathMap.values()) {
      totalCount += paths.size();
    }
    CommitFileStatus commitFileStatus = new CommitFileStatus(totalCount);
    try {
      for (Map.Entry<LinkedHashMap<String, String>, List<Path>> fileNameEntry : partSpecToFilePathMap.entrySet()) {
        LinkedHashMap<String, String> partSpec = fileNameEntry.getKey();
        List<Path> srcPathWithFilename = fileNameEntry.getValue();
        commitPendingTaskFiles(getOrCreatePartitionLoader(), partSpec, srcPathWithFilename, commitFileStatus);
      }
    } finally {
      MetricManager metrics = getOrCreateMetrics();
      metricCommitStatus(commitFileStatus, fileState, metrics);
    }
  }

  public void commitTaskCheckpoint(long toCpId, Integer assignTaskId) throws Exception {
    FileSystem fs = getOrCreateFileSystem();
    if (!fs.exists(tmpPath)) {
      return;
    }
    LOG.info("Subtask {} commit checkpoint {} with assign task id {}.", subTaskId, toCpId, assignTaskId);
    List<Path> taskPaths = listTaskTemporaryPaths(fs, tmpPath, toCpId, assignTaskId);
    commitPendingTaskCheckpoint(fs, getOrCreatePartitionLoader(), taskPaths);
  }

  private void metricCommitStatus(CommitFileStatus commitFileStatus,
                                  AbstractFileState fileState,
                                  MetricManager metrics) {
    LOG.info("Subtask {} with total files {}, "
            + "skip commit {} files, "
            + "success commit {} files, "
            + "fail commit {} files for task id {} checkpoint {}.",
        subTaskId,
        commitFileStatus.getTotalCount(),
        commitFileStatus.getSkipCount(),
        commitFileStatus.getSuccessCount(),
        commitFileStatus.getFailCount(),
        fileState.getTaskId(), fileState.getCheckpointId());
    metrics.recordCounter(HDFS_CREATE_FILE_COUNT, commitFileStatus.getTotalCount());
    metrics.recordCounter(HDFS_RENAME_SUCCESS_COUNT, commitFileStatus.getSuccessCount());
    if (commitFileStatus.getFailCount() > 0) {
      metrics.recordCounter(HDFS_RENAME_FAILED_COUNT, commitFileStatus.getFailCount());
    }
    if (commitFileStatus.getSkipCount() > 0) {
      metrics.recordCounter(HDFS_RENAME_SKIP_COUNT, commitFileStatus.getSkipCount());
    }
  }

  private void commitPendingTaskCheckpoint(
      FileSystem fs, PartitionLoader loader, List<Path> taskPaths) throws Exception {
    try {
      if (partitionColumnSize > 0) {
        for (Map.Entry<LinkedHashMap<String, String>, List<Path>> entry :
            collectPartSpecToPaths(fs, taskPaths, partitionColumnSize).entrySet()) {
          loader.commitPartitionFile(entry.getKey(), entry.getValue());
        }
      } else {
        loader.commitNonPartitionFile(taskPaths);
      }
    } catch (Exception e) {
      LOG.error("Subtask {} commit checkpoint failed.", subTaskId, e);
      throw e;
    }
  }

  private void commitPendingTaskFiles(PartitionLoader loader,
                                      LinkedHashMap<String, String> partSpec,
                                      List<Path> filePaths,
                                      CommitFileStatus commitFileStatus) throws Exception {
    try {
      if (partitionColumnSize > 0) {
        loader.commitPartitionFiles(partSpec, filePaths, commitFileStatus);
      } else {
        loader.commitNonPartitionSrcFiles(filePaths, commitFileStatus);
      }
    } catch (Exception e) {
      LOG.error("Subtask {} commit checkpoint failed.", subTaskId, e);
      throw e;
    }
  }

  public void cleanCheckpointDir(long toCpId) throws Exception {
    FileSystem fs = getOrCreateFileSystem();
    if (!fs.exists(tmpPath)) {
      return;
    }
    if (subTaskId == 0) {
      for (long cp : headCheckpoints(fs, tmpPath, toCpId, true)) {
        deleteCheckpoint(fs, tmpPath, cp, subTaskId);
      }
    }
  }

  public void cleanTaskCheckpointDir(int taskId, long cpId) throws Exception {
    FileSystem fs = getOrCreateFileSystem();
    if (!fs.exists(tmpPath)) {
      return;
    }
    deleteTaskCheckpoint(fs, tmpPath, cpId, taskId);
  }

  public void cleanInvalidTaskCheckpointDir(int parallelTasks, int taskId, long maxCpId) throws Exception {
    FileSystem fs = getOrCreateFileSystem();
    if (!fs.exists(tmpPath)) {
      return;
    }

    for (long cp : headCheckpoints(fs, tmpPath, maxCpId, false)) {
      List<Path> taskPaths = listInvalidTaskTemporaryPaths(fs, tmpPath, cp, parallelTasks);
      deleteTaskCheckpoint(fs, taskPaths, taskId);
    }
  }

  /**
   * Create a new temporary file manager from task and checkpoint id.
   * It will create a new temporary directory for task and clean it.
   */
  public PartitionTempFileManager createTempFileManager(
      int taskNumber, long checkpointId) throws Exception {
    return new PartitionTempFileManager(factory, tmpPath, taskNumber, checkpointId,
        staticPartitions, compressionExtension, jobId, directoryManager, partitionInfos);
  }

  public AbstractDirtyCollector createDirtyCollector(int taskId, Configuration configuration) {
    AbstractDirtyCollector collector = getJobConfDirtyCollector(taskId, locationPath, configuration);
    if (Objects.isNull(collector)) {
      String dirtyCollectorType = jobConf.get(CommonOptions.DirtyRecordOptions.DIRTY_COLLECTOR_TYPE);
      throw new UnsupportedOperationException(String.format("dirty collect type %s not support.",
          dirtyCollectorType));
    }
    return collector;
  }

  /**
   * Overwrite dirty collector before job start.
   */
  public void overwriteDirtyCollector() throws IOException {
    AbstractDirtyCollector collector = getJobConfDirtyCollector(-1, locationPath, new Configuration());
    if (Objects.nonNull(collector)) {
      collector.clear();
    }
  }

  private AbstractDirtyCollector getJobConfDirtyCollector(int taskId, Path locationPath, Configuration configuration) {
    String dirtyCollectorType = jobConf.getUnNecessaryOption(
        CommonOptions.DirtyRecordOptions.DIRTY_COLLECTOR_TYPE, DIRTY_COLLECTOR_TYPE_SINGLE_FILE_HDFS);
    LOG.info("Using {} dirty collect type.", dirtyCollectorType);

    AbstractDirtyCollector collector;
    if (DIRTY_COLLECTOR_TYPE_SINGLE_FILE_HDFS.equalsIgnoreCase(dirtyCollectorType)) {
      collector = new SingleFileHdfsDirtyCollector(jobConf,
          locationPath,
          taskId,
          configuration);
    } else if (DIRTY_COLLECTOR_TYPE_MULTI_FILE_HDFS.equalsIgnoreCase(dirtyCollectorType)) {
      collector = new MultiFileHdfsDirtyCollector(jobConf,
          locationPath,
          taskId,
          configuration);
    } else {
      collector = new NoOpDirtyCollector(jobConf, taskId);
    }
    return collector;
  }

  /**
   * Create partition committer base on formatType.
   * It will commit partition when checkpoint complete.
   */
  public AbstractPartitionCommitter createPartitionCommitter(FileStateCollector fileStateCollector) {
    if (formatType == null) {
      return null;
    }

    AbstractPartitionCommitter partitionCommitter;
    switch (formatType) {
      case HDFS_FORMAT_TYPE_VALUE:
        partitionCommitter = new HdfsPartitionCommitter(jobConf, metaStoreFactory, fileStateCollector);
        break;
      case HIVE_FORMAT_TYPE_VALUE:
        partitionCommitter = new HivePartitionCommitter(jobConf, metaStoreFactory, fileStateCollector);
        break;
      default:
        throw new RuntimeException("Unsupported format type: " + formatType);
    }
    return partitionCommitter;
  }

  public FileSystem getOrCreateFileSystem() throws IOException {
    if (Objects.isNull(fileSystem)) {
      fileSystem = factory.create(tmpPath.toUri());
    }
    return fileSystem;
  }

  public PartitionLoader getOrCreatePartitionLoader() throws Exception {
    if (Objects.isNull(loader)) {
      FileSystem fileSystem = getOrCreateFileSystem();
      loader = PartitionLoader.builder()
          .overwrite(overwrite)
          .factory(metaStoreFactory)
          .fs(fileSystem)
          .locationPath(locationPath)
          .build();
    }
    return loader;
  }

  public MetricManager getOrCreateMetrics() {
    if (Objects.isNull(metrics)) {
      metrics = MetricsFactory.getInstanceMetricsManager(jobConf, subTaskId);
    }
    return metrics;
  }

  public void setSubTaskId(int subTaskId) {
    this.subTaskId = subTaskId;
  }

  @Override
  public void close() throws IOException {
    CloseableUtils.closeQuietly(loader);
  }
}
