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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.TimePartitionGroup;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.RollingPolicy;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;

import com.google.common.collect.Maps;
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Dynamic partition writer to writing multiple partitions at the same time, it maybe consumes more memory.
 */
@Internal
public class DynamicPartitionWriter<T> extends PartitionWriter<T> {
  private static final Logger LOG = LoggerFactory.getLogger(DynamicPartitionWriter.class);

  private final Map<String, OutputFormat<T>> formats;
  private final Map<String, PartFileInfo> partFileInfos;
  private final TimePartitionGroup partitionGroup;
  private final boolean isStrictArchiveMode;

  public DynamicPartitionWriter(
      Context<T> context,
      PartitionTempFileManager manager,
      PartitionComputer<T> computer,
      RollingPolicy<T> rollingPolicy,
      FileSystemMetaManager fileSystemMetaManager) {
    super(context, computer, manager, rollingPolicy, fileSystemMetaManager);

    this.partitionGroup = new TimePartitionGroup(PartitionUtils.getPartitionInfo(context.getJobConf()));
    this.formats = new HashMap<>();
    this.partFileInfos = new HashMap<>();
    this.isStrictArchiveMode = context.getJobConf().get(FileSystemCommonOptions.ArchiveOptions.STRICT_ARCHIVE_MODE);
  }

  @Override
  public Tuple2<OutputFormat<T>, PartFileInfo> getOrCreateFormatForPartition(String partition) throws Exception {
    OutputFormat<T> format = formats.get(partition);
    PartFileInfo partFileInfo = partFileInfos.get(partition);

    if (format == null) {
      long timestamp = System.currentTimeMillis();
      // create a new format to write new partition.
      Tuple2<OutputFormat<T>, PartFileInfo> currentFormatInfo =
          createFormatForPath(manager.createPartitionFile(timestamp, partition), timestamp, partition);

      format = currentFormatInfo.f0;
      partFileInfo = currentFormatInfo.f1;
      formats.put(partition, format);
      partFileInfos.put(partition, partFileInfo);
    }
    return new Tuple2<>(format, partFileInfo);
  }

  @Override
  public void closeFormatForPartition(String partition) throws Exception {
    OutputFormat<T> format = formats.get(partition);
    if (format != null) {
      format.close();
      formats.remove(partition);
      partFileInfos.remove(partition);
    }
  }

  @Override
  public void onProcessingTime(long timestamp) throws Exception {
    Set<String> recyclePartitions = new HashSet<>();
    for (String partition : formats.keySet()) {
      OutputFormat<T> format = formats.get(partition);
      PartFileInfo partFileInfo = partFileInfos.get(partition);
      if (format != null && rollingPolicy.shouldRollOnProcessingTime(partFileInfo, timestamp)) {
        LOG.info("Subtask {} recycle part file which create in {} and close timestamp {}.",
            context.getConf().getInteger(StreamingFileSystemMetricsNames.SUB_TASK_ID, StreamingFileSystemMetricsNames.INVALID_TASK_ID), partFileInfo.getCreationTime(),
            timestamp);
        recyclePartitions.add(partition);
      }
    }

    for (String partition : recyclePartitions) {
      closeFormatForPartition(partition);
    }
  }

  @Override
  public void close(long jobMinTimestamp, long checkpointId, boolean clearPartFileInfo) throws Exception {
    Exception fileCloseException = null;

    Iterator<Map.Entry<String, OutputFormat<T>>> iterator = formats.entrySet().iterator();
    while (iterator.hasNext()) {
      try {
        Map.Entry<String, OutputFormat<T>> entry = iterator.next();
        entry.getValue().close();

        iterator.remove();
        partFileInfos.remove(entry.getKey());
      } catch (Exception e) {
        fileCloseException = e;
      }
    }

    if (fileCloseException != null) {
      throw fileCloseException;
    }
    formats.clear();
    partFileInfos.clear();

    if (!isStrictArchiveMode) {
      replacePartSpecs(jobMinTimestamp);
    }

    super.close(jobMinTimestamp, checkpointId, clearPartFileInfo);
  }

  /**
   * jobMinTimestamp maybe varied after task checkpoint notified, we can get the latest jobMinTimestamp on processing time.
   * So when closing the files, we need to put this files into the folders where timestamp is bigger than jobMinTimestamp.
   */
  private void replacePartSpecs(long jobMinTimestamp) throws Exception {

    LocalDateTime jobMinLocalTime = Instant.ofEpochMilli(jobMinTimestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();

    LinkedHashMap<String, String> replaced = Maps.newLinkedHashMap();

    for (PartFileInfo partFileInfo : getPartFileInfoCreations()) {
      replaced.clear();

      String partition = partFileInfo.getPartition();
      LinkedHashMap<String, String> partSpecs = PartitionPathUtils
          .extractPartitionSpecFromPath(new Path(partition));

      long commitTimeFromPartSpecs = partitionGroup.getCommitTimeFromPartSpecs(partSpecs,
          jobMinTimestamp);

      if (commitTimeFromPartSpecs >= jobMinTimestamp) {
        continue;
      }

      for (Map.Entry<String, String> entry : partSpecs.entrySet()) {
        DateTimeFormatter matchedFormatter = partitionGroup.getMatchedFormatter(entry.getKey());
        if (Objects.isNull(matchedFormatter)) {
          replaced.put(entry.getKey(), entry.getValue());
        } else {
          replaced.put(entry.getKey(), matchedFormatter.format(jobMinLocalTime));
        }
      }

      Path path = partFileInfo.getPath();
      String replacedPartition = generatePartitionFromRecord(replaced);
      long currentTimestamp = System.currentTimeMillis();
      Path replacedPath = manager.createPartitionFile(currentTimestamp, replacedPartition);
      FileSystem fileSystem = FileSystem.get(path.toUri());
      if (fileSystem.rename(path, replacedPath)) {
        LOG.info("Subtask {} renamed temporary file, before: {} after: {}.",
            context.getConf().getInteger(StreamingFileSystemMetricsNames.SUB_TASK_ID, StreamingFileSystemMetricsNames.INVALID_TASK_ID), path, replacedPath);
        partFileInfo.setPath(replacedPath);
        partFileInfo.setPartition(replacedPartition);
        partFileInfo.setCreationTime(currentTimestamp);
        metrics.recordCounter(StreamingFileSystemMetricsNames.HDFS_REPLACED_COUNT);
      } else {
        LOG.info("Subtask {} renamed temporary file failed, before: {} after: {}.",
            context.getConf().getInteger(StreamingFileSystemMetricsNames.SUB_TASK_ID, StreamingFileSystemMetricsNames.INVALID_TASK_ID), path, replacedPath);
        metrics.recordCounter(StreamingFileSystemMetricsNames.HDFS_REPLACED_COUNT_FAILED);
      }
    }
  }

  @Override
  public Map<String, OutputFormat<T>> getOutputFormats() {
    return formats;
  }
}
