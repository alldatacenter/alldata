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
 */

package com.bytedance.bitsail.entry.flink.savepoint;

import com.bytedance.bitsail.base.execution.Mode;
import com.bytedance.bitsail.client.api.command.BaseCommandArgs;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.entry.flink.command.FlinkRunCommandArgs;
import com.bytedance.bitsail.entry.flink.configuration.FlinkRunnerConfigOptions;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created 2022/8/5
 */
public class FlinkRunnerSavepointLoader {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkRunnerSavepointLoader.class);

  private static final String EXTERNALIZED_CHECKPOINT_RETENTION_KEY = "execution.checkpointing.externalized-checkpoint-retention";
  private static final String EXTERNALIZED_CHECKPOINT_RETENTION_VAL = "RETAIN_ON_CANCELLATION";

  private static final String CHECKPOINT_CONF_KEY = "state.checkpoints.dir";
  private static final String CHECKPOINT_DIR_PREFIX = "chk-";
  private static final String CHECKPOINT_METADATA_NAME = "_metadata";
  private static final long CHECKPOINT_DEFAULT_VALUE = -1;

  public static void loadSavepointPath(BitSailConfiguration sysConfiguration,
                                       BitSailConfiguration jobConfiguration,
                                       BaseCommandArgs baseCommandArgs,
                                       FlinkRunCommandArgs flinkCommandArgs,
                                       List<String> flinkCommands) {
    Mode jobRunMode = Mode.getJobRunMode(jobConfiguration.get(CommonOptions.JOB_TYPE));
    if (Mode.BATCH.equals(jobRunMode)) {
      LOG.info("Skip load savepoint in batch mode.");
      return;
    }
    if (flinkCommandArgs.isSkipSavepoint()) {
      LOG.info("Skip load savepoint in cause by parameter.");
      return;
    }
    String name = jobConfiguration.getNecessaryOption(CommonOptions.JOB_NAME, CommonErrorCode.CONFIG_ERROR);
    Optional<Path> checkpointWorkDirOptional = getCheckpointWorkDir(sysConfiguration, name);
    if (!checkpointWorkDirOptional.isPresent()) {
      LOG.info("Checkpoint work dir is not configure, skip load checkpoint.");
      return;
    }
    Path checkpointWorkDir = checkpointWorkDirOptional.get();
    LOG.info("Flink checkpoint work dir = {}.", checkpointWorkDir);

    baseCommandArgs.getProperties()
        .put(EXTERNALIZED_CHECKPOINT_RETENTION_KEY, EXTERNALIZED_CHECKPOINT_RETENTION_VAL);
    baseCommandArgs.getProperties()
        .put(CHECKPOINT_CONF_KEY, checkpointWorkDir.toString());

    if (StringUtils.isNotEmpty(flinkCommandArgs.getFromSavepoint())) {
      LOG.info("Flink runner load savepoint path from args: {}.", flinkCommandArgs.getFromSavepoint());
      flinkCommands.add("-s");
      flinkCommands.add(flinkCommandArgs.getFromSavepoint());
      return;
    }

    try {
      String checkpointPath = loadLatestCheckpointPath(checkpointWorkDir);
      if (StringUtils.isEmpty(checkpointPath)) {
        LOG.info("No checkpoint found in checkpoint work dir = {}.", checkpointWorkDir);
        return;
      }
      LOG.info("Found latest checkpoint = {}.", checkpointPath);
      flinkCommands.add("-s");
      flinkCommands.add(checkpointPath);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static Optional<Path> getCheckpointWorkDir(BitSailConfiguration sysConfiguration, String name) {
    String checkpointBaseDir = sysConfiguration.get(FlinkRunnerConfigOptions.FLINK_CHECKPOINT_DIR);
    if (StringUtils.isEmpty(checkpointBaseDir)) {
      return Optional.empty();
    }
    return Optional.of(new Path(checkpointBaseDir, name));
  }

  private static String loadLatestCheckpointPath(Path checkpointWorkDir) throws IOException {
    Configuration hdfsConfiguration = new Configuration();
    FileSystem fileSystem = FileSystem.get(hdfsConfiguration);
    LOG.info("Starting load checkpoint path for work dir = {}.", checkpointWorkDir);
    org.apache.hadoop.fs.Path hdfsCheckpointWorkDir = new org.apache.hadoop.fs.Path(checkpointWorkDir.toString());

    if (!fileSystem.exists(hdfsCheckpointWorkDir)) {
      return null;
    }

    FileStatus[] fileStatuses = fileSystem.listStatus(hdfsCheckpointWorkDir);
    List<FileStatus> fileStatusList =
        Arrays.stream(fileStatuses)
            .filter(FileStatus::isDirectory)
            .sorted(Comparator.comparingLong(FileStatus::getModificationTime).reversed())
            .collect(Collectors.toList());

    for (FileStatus fileStatus : fileStatusList) {
      long maxCheckpointId = CHECKPOINT_DEFAULT_VALUE;
      String latestCheckpointPath = null;
      for (FileStatus chkFileStatus : fileSystem.listStatus(fileStatus.getPath())) {
        long checkpointId = CHECKPOINT_DEFAULT_VALUE;
        org.apache.hadoop.fs.Path checkpointDir = chkFileStatus.getPath();
        String fileName = checkpointDir.getName();
        if (fileName.startsWith(CHECKPOINT_DIR_PREFIX) && chkFileStatus.isDirectory()) {
          try {
            checkpointId = Long.parseLong(fileName.substring(CHECKPOINT_DIR_PREFIX.length()));
          } catch (NumberFormatException e) {
            //Ignore
          }
        }
        if (checkpointId <= 0) {
          continue;
        }

        if (fileSystem.exists(new org.apache.hadoop.fs.Path(checkpointDir.toString(), CHECKPOINT_METADATA_NAME))) {
          if (checkpointId > maxCheckpointId) {
            maxCheckpointId = checkpointId;
            latestCheckpointPath = Paths.get(checkpointDir.toUri()).toString();
          }
        }
      }

      if (latestCheckpointPath != null) {
        return latestCheckpointPath;
      }
    }
    return null;
  }
}
