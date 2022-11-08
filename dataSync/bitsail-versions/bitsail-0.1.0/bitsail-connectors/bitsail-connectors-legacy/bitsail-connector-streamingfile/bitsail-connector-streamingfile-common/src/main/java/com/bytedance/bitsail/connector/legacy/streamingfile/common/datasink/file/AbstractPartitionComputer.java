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

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionType;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor.AbstractEventTimeExtractor;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor.EventTimeExtractorFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.MetricsFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;

import com.google.common.collect.Maps;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bytedance.bitsail.common.option.CommonOptions.CheckPointOptions.CHECKPOINT_INTERVAL;
import static com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema.DUMP_ROW_PARTITION_INDEX;

/**
 * @class: AbstractPartitionComputer
 * @desc:
 **/
public abstract class AbstractPartitionComputer<IN extends Row> implements PartitionComputer<IN> {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractPartitionComputer.class);
  private static final long serialVersionUID = 6003203824501966327L;
  private static final long DEFAULT_TIMESTAMP_OFFSET = 300000L;

  protected final BitSailConfiguration jobConf;
  protected final String formatType;
  protected final List<PartitionInfo> partitionKeys;
  protected final List<PartitionInfo> timePartitionKeys;
  protected final boolean isEventTime;
  protected final boolean isStrictArchive;
  protected final boolean hasDynamicPartition;
  protected final long eventTimeThreshold;
  protected final String eventTimeFields;
  private final long checkpointInterval;
  protected AbstractEventTimeExtractor extractor;
  protected Integer taskId;
  private Map<String, Long> partitionLatestTimestamp;
  private long latestJobTimestamp;
  private long defaultTimestamp;
  private transient MetricManager metrics;
  private transient boolean safeMode;
  private transient long safeModeTimestamp;
  private transient long safeModeMinTimestamp;

  public AbstractPartitionComputer(final BitSailConfiguration jobConf) throws Exception {
    this.jobConf = jobConf;
    this.taskId = StreamingFileSystemMetricsNames.INVALID_TASK_ID;

    this.formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);
    this.partitionKeys = PartitionUtils.getPartitionInfo(jobConf);
    this.timePartitionKeys = partitionKeys.stream()
        .filter(partitionKey -> PartitionType.TIME.equals(partitionKey.getType()))
        .collect(Collectors.toList());
    hasDynamicPartition = partitionKeys.stream()
        .anyMatch(partitionKey -> PartitionType.DYNAMIC.equals(partitionKey.getType()));

    isEventTime = jobConf.get(FileSystemCommonOptions.ArchiveOptions.ENABLE_EVENT_TIME);
    isStrictArchive = jobConf.get(FileSystemCommonOptions.ArchiveOptions.STRICT_ARCHIVE_MODE);
    eventTimeThreshold = jobConf.get(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_THRESHOLD);
    eventTimeFields = jobConf.get(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_FIELDS);
    extractor = getExtractor(jobConf);

    partitionLatestTimestamp = Maps.newHashMap();
    defaultTimestamp = System.currentTimeMillis();
    latestJobTimestamp = INVALID_TIMESTAMPS;
    checkpointInterval = jobConf.get(CHECKPOINT_INTERVAL);
    safeMode = false;
  }

  private AbstractEventTimeExtractor getExtractor(BitSailConfiguration jobConf) throws Exception {
    if (isEventTime || hasDynamicPartition) {
      return EventTimeExtractorFactory.create(jobConf);
    }
    return null;
  }

  protected List<FieldPathUtils.PathInfo> getEventTimeFields() {
    if (isEventTime && extractor != null) {
      return extractor.getEventTimeFields();
    }
    return Collections.emptyList();
  }

  /**
   * Execute construct logic in flink runtime.
   * if invoking
   * {@link org.apache.flink.api.common.functions.RichFunction#open(Configuration)} this method will be invoked
   */
  public void open(RuntimeContext context, FileSystemMetaManager fileSystemMetaManager) {
    timePartitionKeys.forEach(PartitionInfo::createFormatter);
    taskId = context.getIndexOfThisSubtask();
    metrics = MetricsFactory.getInstanceMetricsManager(jobConf, taskId);
    if (Objects.nonNull(extractor)) {
      extractor.startContext(taskId);
    }
  }

  protected LinkedHashMap<String, String> generateTimePartSpecs(IN row, long timestamp) {
    LinkedHashMap<String, String> partSpec = new LinkedHashMap<>();
    LocalDateTime localDateTime = generateArchiveEventTime(row, timestamp);

    for (PartitionInfo partitionInfo : timePartitionKeys) {
      partSpec.put(partitionInfo.getName(), partitionInfo.getFormatter().format(localDateTime));
    }

    return partSpec;
  }

  protected LocalDateTime generateArchiveEventTime(IN row, long timestamp) {
    long recordTimestamp = defaultTimestamp;
    if (isEventTime) {
      recordTimestamp = computeArchiveTimestamp(row, timestamp);
    }
    return Instant.ofEpochMilli(recordTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  /**
   * extract event timestamp for hdfs dump
   */
  public long extractEventTimestamp(Object record) {
    if (isEventTime && extractor != null && Objects.nonNull(record)) {
      try {
        return extractor.extractEventTimestamp(record, INVALID_TIMESTAMPS);
      } catch (Exception e) {
        LOG.error("extractor event time failed.", e);
        return INVALID_TIMESTAMPS;
      }
    }
    return INVALID_TIMESTAMPS;
  }

  /**
   * convert event timestamp for hive dump
   */
  public long convertEventTimestamp(Object recordTimestamp) {
    if (isEventTime && extractor != null && recordTimestamp != null) {
      try {
        return extractor.convertEventTimestamp(recordTimestamp, INVALID_TIMESTAMPS);
      } catch (Exception e) {
        LOG.error("convert event time failed.", e);
        return INVALID_TIMESTAMPS;
      }
    }
    return INVALID_TIMESTAMPS;
  }

  /**
   * Compute row archive timestamp
   *
   * @param row Record value
   * @return The archive time for the record
   */
  private long computeArchiveTimestamp(IN row, long eventTime) {
    String partition = (String) row.getField(DUMP_ROW_PARTITION_INDEX);
    long preEventTime = partitionLatestTimestamp.getOrDefault(partition, INVALID_TIMESTAMPS);
    boolean validEventTime = (eventTime <= (System.currentTimeMillis() + DEFAULT_TIMESTAMP_OFFSET));

    if (eventTime == INVALID_TIMESTAMPS) {
      metrics.recordCounter(StreamingFileSystemMetricsNames.ARCHIVE_INVALID_EVENT_TIME);
      return defaultTimestamp;
    } else if (!validEventTime && preEventTime == INVALID_TIMESTAMPS) {
      metrics.recordCounter(StreamingFileSystemMetricsNames.ARCHIVE_FASTER_THAN_SYSTEM_TIME);
      return defaultTimestamp;
    } else if (eventTime > preEventTime && validEventTime && eventTime >= latestJobTimestamp) {
      partitionLatestTimestamp.put(partition, eventTime);
    }

    long minArchiveTimestamp = preEventTime - eventTimeThreshold;
    if (!extractor.isBinlog() && !isStrictArchive && eventTime < minArchiveTimestamp) {
      metrics.recordCounter(StreamingFileSystemMetricsNames.ARCHIVE_PRE_EVENT_TIME);
      eventTime = preEventTime;
    }

    if (!validEventTime) {
      metrics.recordCounter(StreamingFileSystemMetricsNames.ARCHIVE_FASTER_THAN_SYSTEM_TIME);
      eventTime = preEventTime;
    }

    if (eventTime < latestJobTimestamp) {
      metrics.recordCounter(StreamingFileSystemMetricsNames.ARCHIVE_LESS_THAN_COMMIT_TIME);
      eventTime = isStrictArchive ? eventTime : defaultTimestamp;
    } else {
      metrics.recordCounter(StreamingFileSystemMetricsNames.ARCHIVE_SUCCESS);
    }

    return eventTime;
  }

  /**
   * @param defaultTimestamp system timestamp
   */
  public void updateDefaultTimestamp(Long defaultTimestamp) {
    this.defaultTimestamp = defaultTimestamp;
  }

  public void updatePartitionLatestTimestamp(Map<String, Long> partitionLatestTimestamp) {
    this.partitionLatestTimestamp = partitionLatestTimestamp;
  }

  public void updateLatestJobTimestamp(Long latestJobTimestamp) {
    this.latestJobTimestamp = latestJobTimestamp;
  }

  private long getTaskMinTimestamp() {
    final HashMap<String, Long> immutable = Maps.newHashMap(partitionLatestTimestamp);
    return immutable.values().stream().min(Long::compareTo).orElse(INVALID_TIMESTAMPS);
  }

  public void configureSafeMode(boolean safeMode, long timestamp, long minTaskTimestamp) {
    this.safeMode = safeMode;
    if (this.safeMode) {
      this.safeModeTimestamp = timestamp;
      this.safeModeMinTimestamp = minTaskTimestamp;
    }
  }

  public long getPartitionMinTimestamp() {
    if (safeMode) {
      long duration = System.currentTimeMillis() - safeModeTimestamp;
      if (duration < checkpointInterval / 2) {
        LOG.info("Subtask {} will use safe mode for duration {} is smaller than one half checkpoint interval {}.", taskId, duration, checkpointInterval);
        return this.safeModeMinTimestamp;
      }
    }
    return getTaskMinTimestamp();
  }
}
