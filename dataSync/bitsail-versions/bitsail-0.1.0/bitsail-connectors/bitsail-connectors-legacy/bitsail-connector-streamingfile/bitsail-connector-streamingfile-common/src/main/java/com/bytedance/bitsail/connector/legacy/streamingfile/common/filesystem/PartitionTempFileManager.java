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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.directory.PartitionDirectoryManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.FileNameUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.apache.flink.annotation.Internal;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * Manage temporary files for writing files. Use special rules to organize directories
 * for temporary files.
 *
 * <p>Temporary file directory contains the following directory parts:
 * 1.temporary base path directory.
 * 2.checkpoint id directory.
 * 3.task id directory.
 * 4.directories to specify partitioning.
 * 5.data files.
 * eg: /tmp/cp-1/task-0/p0=1/p1=2/fileName.
 */
@Internal
public class PartitionTempFileManager {
  private static final Logger LOG =
      LoggerFactory.getLogger(PartitionTempFileManager.class);

  private static final String CHECKPOINT_DIR_PREFIX = "cp-";
  private static final String TASK_DIR_PREFIX = "task-";
  private static final int CHECKPOINT_INDEX = 3;

  @Getter
  private final int taskNumber;
  private final Path temporaryPath;
  private final FileSystemFactory fileSystemFactory;
  private final LinkedHashMap<String, String> staticParts;
  private final String compressionExtension;
  private final Long jobId;
  private final PartitionDirectoryManager.DirectoryType directoryType;
  private final int hourIndex;
  private long checkpointId;
  private Path taskTmpDir;

  public PartitionTempFileManager(
      FileSystemFactory factory,
      Path tmpPath,
      int taskNumber,
      long checkpointId,
      LinkedHashMap<String, String> staticParts,
      String compressionExtension,
      long jobId,
      PartitionDirectoryManager.DirectoryType directoryType,
      List<PartitionInfo> partitionInfos) throws Exception {
    checkArgument(checkpointId != -1, "checkpoint id start with 0.");
    this.temporaryPath = tmpPath;
    this.fileSystemFactory = factory;
    this.taskNumber = taskNumber;
    this.checkpointId = checkpointId;
    this.staticParts = staticParts;
    this.compressionExtension = compressionExtension;
    this.jobId = jobId;
    this.directoryType = directoryType;
    this.hourIndex = PartitionUtils.getHourPartitionIndex(partitionInfos);

    // generate and clean task temp dir.
    generateTaskTmpPath(checkpointId);
  }

  private static boolean isTaskDir(String fileName) {
    return fileName.startsWith(TASK_DIR_PREFIX);
  }

  private static boolean isCheckpointDir(String fileName) {
    return fileName.startsWith(CHECKPOINT_DIR_PREFIX);
  }

  private static long getCheckpointId(String fileName) {
    return Long.parseLong(fileName.substring(CHECKPOINT_INDEX));
  }

  private static int getTaskId(String fileName) {
    return Integer.parseInt(fileName.substring(TASK_DIR_PREFIX.length()));
  }

  private static String checkpointName(long checkpointId) {
    return CHECKPOINT_DIR_PREFIX + checkpointId;
  }

  private static String taskName(int task) {
    return TASK_DIR_PREFIX + task;
  }

  /**
   * Delete checkpoint path.
   */
  public static void deleteCheckpoint(
      FileSystem fs, Path basePath, long checkpointId, int taskId) throws IOException {
    Path checkpointPath = new Path(basePath, checkpointName(checkpointId));
    fs.delete(checkpointPath, true);
    LOG.info("Subtask {} delete checkpoint dir: {}).", taskId, checkpointPath.getPath());
  }

  /**
   * Delete task checkpoint path.
   */
  public static void deleteTaskCheckpoint(FileSystem fs, Path basePath, long checkpointId, int taskId) throws IOException {
    Path taskPath = new Path(new Path(basePath, checkpointName(checkpointId)), taskName(taskId));
    fs.delete(taskPath, true);
    LOG.info("Subtask {} delete task checkpoint dir: {}).", taskId, taskPath.getPath());
  }

  public static void deleteTaskCheckpoint(FileSystem fs, List<Path> taskCheckpointPaths, int taskId) throws IOException {
    for (Path taskPath : taskCheckpointPaths) {
      fs.delete(taskPath, true);
      LOG.info("Subtask {} delete task checkpoint dir: {}).", taskId, taskPath.getPath());
    }
  }

  /**
   * Returns checkpoints whose keys are less than or equal to {@code toCpId}
   * in temporary base path.
   */
  public static long[] headCheckpoints(FileSystem fs, Path basePath, long toCpId, boolean less) throws IOException {
    List<Long> cps = new ArrayList<>();

    for (FileStatus taskStatus : fs.listStatus(basePath)) {
      String name = taskStatus.getPath().getName();
      if (isCheckpointDir(name)) {
        long currentCp = getCheckpointId(name);
        if (currentCp <= toCpId && less) {
          // checkpoint paths that less than current checkpoint id.
          cps.add(currentCp);
        } else if (currentCp > toCpId && !less) {
          // checkpoint paths that greater than current checkpoint id.
          cps.add(currentCp);
        }
      }
    }
    return cps.stream().mapToLong(v -> v).toArray();
  }

  /**
   * Returns task temporary paths in this checkpoint.
   */
  public static List<Path> listTaskTemporaryPaths(
      FileSystem fs, Path basePath, long checkpointId, int subTaskId) throws Exception {
    List<Path> taskTmpPaths = new ArrayList<>();
    Path checkpointPath = new Path(basePath, checkpointName(checkpointId));
    List<FileStatus> fileStatusList = Lists.newArrayListWithCapacity(1);

    if (Integer.MAX_VALUE != subTaskId) {
      Path subTaskPath = new Path(checkpointPath, taskName(subTaskId));
      if (fs.exists(subTaskPath)) {
        fileStatusList.add(fs.getFileStatus(subTaskPath));
      }
    } else {
      fileStatusList = Lists.newArrayList(fs.listStatus(checkpointPath));
    }

    for (FileStatus taskStatus : fileStatusList) {
      String name = taskStatus.getPath().getName();
      if (isTaskDir(name)) {
        long taskId = getTaskId(name);
        if (taskId <= subTaskId) {
          taskTmpPaths.add(taskStatus.getPath());
        }
      }
    }
    return taskTmpPaths;
  }

  public static Path getTaskTemporaryPath(Path basePath, long checkpointId, int subTaskId) {
    return new Path(new Path(basePath, checkpointName(checkpointId)), taskName(subTaskId));
  }

  /**
   * Returns invalid task temporary paths in this checkpoint.
   */
  public static List<Path> listInvalidTaskTemporaryPaths(
      FileSystem fs, Path basePath, long checkpointId, int parallelTasks) throws Exception {
    List<Path> taskTmpPaths = new ArrayList<>();
    Path checkpointPath = new Path(basePath, checkpointName(checkpointId));
    List<FileStatus> fileStatusList = Lists.newArrayList(fs.listStatus(checkpointPath));

    for (FileStatus taskStatus : fileStatusList) {
      String name = taskStatus.getPath().getName();
      if (isTaskDir(name)) {
        long taskId = getTaskId(name);
        if (taskId >= parallelTasks) {
          taskTmpPaths.add(taskStatus.getPath());
        }
      }
    }
    return taskTmpPaths;
  }

  /**
   * Collect all partitioned paths, aggregate according to partition spec.
   */
  public static Map<LinkedHashMap<String, String>, List<Path>> collectPartSpecToPaths(
      FileSystem fs, List<Path> taskPaths, int partColSize) {
    Map<LinkedHashMap<String, String>, List<Path>> specToPaths = new HashMap<>();
    for (Path taskPath : taskPaths) {
      PartitionPathUtils.searchPartSpecAndPaths(fs, taskPath, partColSize).forEach(
          tuple2 -> specToPaths.compute(tuple2.f0, (spec, paths) -> {
            paths = paths == null ? new ArrayList<>() : paths;
            paths.add(tuple2.f1);
            return paths;
          }));
    }
    return specToPaths;
  }

  public LinkedHashMap<String, String> getStaticPartSpecs() {
    return staticParts;
  }

  /**
   * Start a new task tmp path, remember the checkpoint id and delete task temporary directory to write.
   */
  public void generateTaskTmpPath(long checkpointId) throws Exception {
    checkArgument(checkpointId != -1);
    this.checkpointId = checkpointId;

    this.taskTmpDir = new Path(
        new Path(temporaryPath, checkpointName(checkpointId)),
        taskName(taskNumber));
  }

  /**
   * Generate a new partition directory with partitions.
   */
  public Path createPartitionFile(long timestamp, String... partitions) {
    return directoryType.createPartitionFilePath(this::newFileNameFromTimestamp, timestamp, taskTmpDir, hourIndex, partitions);
  }

  public String newFileNameFromTimestamp(long timestamp) {
    return FileNameUtils.newFileName(FileNameUtils.SupportedVersion.version1.getVersionNum(),
        jobId, taskNumber, checkpointId, timestamp, compressionExtension);
  }
}
