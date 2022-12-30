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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.TimeUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionType;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filestate.FileState;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filestate.FileStateCollector;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionPathUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.StreamingJobCommitStatus;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.annotation.Internal;
import org.apache.flink.core.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Commit partition base on timestamp.
 * <p>See {@link AbstractPartitionCommitter}.
 */
@Internal
public abstract class AbstractPartitionCommitter implements Serializable {
  public static final DateTimeFormatter DATE_HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd/HH");
  protected static final Long HOUR_MILLIS = 3600 * 1000L;
  protected static final Long DAY_MILLIS = 24 * HOUR_MILLIS;
  private static final Logger LOG = LoggerFactory.getLogger(AbstractPartitionCommitter.class);
  protected final BitSailConfiguration jobConf;
  protected final String formatType;
  protected final Long jobId;
  protected final Long eventTimeThreshold;
  protected final Long checkpointInterval;
  protected final Long lookbackThreshold;
  protected final Long lookbackUnit;
  protected final Boolean isEventTime;
  protected final Boolean hasHourPartition;
  protected final Boolean ignoreMidnightFast;
  protected final Path locationPath;
  protected final boolean hasDynamicPartition;
  protected final List<PartitionInfo> partitionKeys;
  private final Long eventTimeTagDuration;
  private final Long eventTimeMidNightTagDuration;
  protected TableMetaStoreFactory factory;
  protected FileStateCollector fileStateCollector;

  public AbstractPartitionCommitter(BitSailConfiguration jobConf, TableMetaStoreFactory factory, FileStateCollector fileStateCollector) {
    this.jobConf = jobConf;

    this.jobId = jobConf.getUnNecessaryOption(CommonOptions.JOB_ID, StreamingFileSystemValidator.JOB_ID_VALUE_DEFAULT);
    this.isEventTime = jobConf.get(FileSystemCommonOptions.ArchiveOptions.ENABLE_EVENT_TIME);
    this.eventTimeThreshold = jobConf.get(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_THRESHOLD);
    this.ignoreMidnightFast = jobConf.get(FileSystemCommonOptions.ArchiveOptions.IGNORE_MID_NIGHT_FAST);
    this.checkpointInterval = jobConf.get(CommonOptions.CheckPointOptions.CHECKPOINT_INTERVAL);
    this.factory = factory;
    this.fileStateCollector = fileStateCollector;
    this.eventTimeTagDuration = calculateTagDuration();
    this.eventTimeMidNightTagDuration = jobConf.get(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_TAG_MID_NIGHT_DURATION);

    this.formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);

    // In split situation, split part spec must not exists in target path structure, so will filter
    // it when the partitionKeys init.
    this.partitionKeys = PartitionUtils.filterPartSpec(PartitionUtils.getPartitionInfo(jobConf),
        partSpec -> !PartitionType.SPLIT.equals(partSpec.getType())
    );
    this.hasHourPartition = PartitionUtils.hasHourPartition(partitionKeys);
    this.hasDynamicPartition = PartitionUtils.hasDynamicPartition(partitionKeys);

    this.lookbackThreshold = hasHourPartition ?
        jobConf.get(FileSystemCommonOptions.PartitionOptions.PARTITION_LOOKBACK_HOUR) :
        jobConf.get(FileSystemCommonOptions.PartitionOptions.PARTITION_LOOKBACK_DAY);
    this.lookbackUnit = hasHourPartition ? HOUR_MILLIS : DAY_MILLIS;

    try (TableMetaStoreFactory.TableMetaStore metaStore = factory.createTableMetaStore()) {
      this.locationPath = metaStore.getLocationPath();
    } catch (Exception e) {
      throw new RuntimeException("Error while calling getLocationPath." + e.getMessage(), e);
    }
  }

  private long calculateTagDuration() {
    if (jobConf.fieldExists(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_TAG_DURATION)) {
      return jobConf.get(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_TAG_DURATION);
    } else {
      return Math.max(StreamingFileSystemValidator.EVENT_TIME_TAG_DURATION_DEFAULT, checkpointInterval);
    }
  }

  /**
   * commit partition before commit timestamp and update job commit time
   *
   * @param jobCommitStatus status to commit
   * @param commitTimestamp timestamp of commit moment
   */
  public List<Long> commitAndUpdateJobTime(StreamingJobCommitStatus jobCommitStatus, long commitTimestamp) {
    Long nextPartitionTime = jobCommitStatus.getJobCommitTime();

    if (nextPartitionTime < 0) {
      nextPartitionTime = hasHourPartition ? TimeUtils.roundDownTimeStampHours(commitTimestamp) : TimeUtils.roundDownTimeStampDate(commitTimestamp);
      nextPartitionTime -= lookbackThreshold * lookbackUnit;
    }

    List<Long> commitPartitionTimes = Lists.newArrayList();

    while (nextPartitionTime + lookbackUnit <= commitTimestamp) {
      if (isEventTime) {
        boolean archiveFinishFast = true;
        if (commitTimestamp < nextPartitionTime + lookbackUnit + eventTimeThreshold) {
          long fastTagDuration = eventTimeTagDuration;

          // accelerate archive at midnight
          Calendar calendar = Calendar.getInstance();
          if (calendar.get(Calendar.HOUR_OF_DAY) < 2 && !ignoreMidnightFast) {
            fastTagDuration = eventTimeMidNightTagDuration;
          }

          // wait another duration check if no partition was written for the current duration
          long lastModifyTime = getFileLastModifyTime(nextPartitionTime);
          LOG.info("collect last partition time: {} last modify time: {}.", nextPartitionTime, lastModifyTime);
          if (commitTimestamp < nextPartitionTime + lookbackUnit + fastTagDuration) {
            LOG.info("commit timestamp: {} less than next partition time: {} plus duration: {}.", commitTimestamp, nextPartitionTime + lookbackUnit, fastTagDuration);
            archiveFinishFast = false;
          } else if (commitTimestamp < lastModifyTime + fastTagDuration) {
            LOG.info("commit timestamp: {} less than last modify time: {} plus duration: {}.", commitTimestamp, lastModifyTime, fastTagDuration);
            archiveFinishFast = false;
          }

          if (!archiveFinishFast) {
            break;
          }
        }
      }

      commitPartitionTimes.add(nextPartitionTime);
      nextPartitionTime += lookbackUnit;
    }
    jobCommitStatus.setJobCommitTime(nextPartitionTime);
    return commitPartitionTimes;
  }

  public void commitPartition(List<Long> pendingCommitPartitionTimes) throws Exception {
    if (CollectionUtils.isEmpty(pendingCommitPartitionTimes)) {
      return;
    }
    pendingCommitPartitionTimes.sort(Long::compare);
    long beginCommitPartitionTime = pendingCommitPartitionTimes.get(0);
    long endCommitPartitionTime = pendingCommitPartitionTimes.get(pendingCommitPartitionTimes.size() - 1) +
        HOUR_MILLIS;

    commitMultiPartitions(pendingCommitPartitionTimes);
    commitSinglePartition(beginCommitPartitionTime, endCommitPartitionTime);
  }

  public void commitPartition(String partition) throws Exception {
    Path partitionPath = new Path(locationPath, partition);
    LinkedHashMap<String, String> partSpec = PartitionPathUtils.extractPartitionSpecFromPath(partitionPath);
    commitPartition(partSpec);
  }

  public abstract void commitPartition(LinkedHashMap<String, String> partSpec) throws Exception;

  protected abstract void commitMultiPartitions(List<Long> pendingCommitPartitions) throws Exception;

  protected void commitSinglePartition(Long beginCommitPartitionTime, Long endCommitPartitionTime) throws Exception {
  }

  public long getFileLastModifyTime(long partitionTime) {
    try {
      fileStateCollector.open();
      FileState fileState = fileStateCollector.getFileState(partitionTime);
      if (Objects.isNull(fileState)) {
        return Long.MIN_VALUE;
      }
      return fileState.getModifyTime();
    } catch (Exception e) {
      LOG.error("Collector file latest modify time for {} failed.", partitionTime, e);
      return Long.MIN_VALUE;
    } finally {
      try {
        fileStateCollector.close();
      } catch (Exception e) {
        // ignore
      }
    }
  }

  protected DateTimeFormatter getOrCreateTimeFormatter(PartitionInfo partitionInfo) {
    if (Objects.isNull(partitionInfo.getFormatter())) {
      partitionInfo.createFormatter();
    }
    return partitionInfo.getFormatter();
  }
}
