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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.dirty;

import com.bytedance.bitsail.base.dirty.DirtyRecord;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.DefaultRollingPolicy;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs.AbstractHdfsOutputFormat;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs.HdfsTextOutputFormat;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Objects;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.PARTITION_DATE_FORMAT_DEFAULT;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.PARTITION_HOUR_FORMAT_DEFAULT;

/**
 * Created 2020/11/3.
 */
public class MultiFileHdfsDirtyCollector extends SingleFileHdfsDirtyCollector {

  private static final Logger LOG =
      LoggerFactory.getLogger(MultiFileHdfsDirtyCollector.class);

  private transient DateTimeFormatter dateFormatter;

  private transient DateTimeFormatter hourFormatter;

  private transient DateTimeFormatter partitionFormatter;

  private DefaultRollingPolicy<DirtyRecord> rollingPolicy;

  private PartFileInfo partFileInfo;

  private AbstractHdfsOutputFormat<Row> outputFormat;

  private Path dirtyDir;

  public MultiFileHdfsDirtyCollector(BitSailConfiguration jobConf,
                                     Path path,
                                     int taskId,
                                     Configuration configuration) {
    super(jobConf, path, taskId, configuration);
  }

  @Override
  protected void constructStreamingMode() {
    super.constructStreamingMode();
    dateFormatter = DateTimeFormatter.ofPattern(PARTITION_DATE_FORMAT_DEFAULT);
    hourFormatter = DateTimeFormatter.ofPattern(PARTITION_HOUR_FORMAT_DEFAULT);
    partitionFormatter = new DateTimeFormatterBuilder().appendPattern(PARTITION_DATE_FORMAT_DEFAULT)
        .appendPattern(PARTITION_HOUR_FORMAT_DEFAULT)
        .toFormatter();
    initDirtyRollingPolicy();
  }

  private void initDirtyRollingPolicy() {
    Long rollingPolicyInterval = jobConf.get(CommonOptions.DirtyRecordOptions.DIRTY_ROLLING_POLICY_INTERVAL);
    Long rollingPolicySize = jobConf.get(CommonOptions.DirtyRecordOptions.DIRTY_ROLLING_POLICY_SIZE);
    DefaultRollingPolicy.PolicyBuilder builder = new DefaultRollingPolicy
        .PolicyBuilder(rollingPolicySize, rollingPolicyInterval, rollingPolicyInterval);
    rollingPolicy = builder.build();
  }

  @Override
  protected void collect(Object dirtyObj, Throwable e, long processingTime) throws IOException {
    if (dirtyObj instanceof Row) {
      DirtyRecord dirtyRecord = buildDirtyRecord((Row) dirtyObj, e);
      if (Objects.nonNull(dirtyRecord)) {
        getOrCreateDirtyOutputFormat(processingTime);
        if (rollingPolicy.shouldRollOnEvent(partFileInfo, dirtyRecord)) {
          recycleDirtyOutputFormat(processingTime);
        }
        byte[] dirtyArray = JsonSerializer.serializeAsBytes(dirtyRecord);
        partFileInfo.update(dirtyArray.length, processingTime);
        outputFormat.writeRecord(wrapper(dirtyArray));
      }
    }
  }

  @Override
  public void onProcessingTime(long processingTime) throws IOException {
    if (Objects.nonNull(partFileInfo)) {
      if (rollingPolicy.shouldRollOnProcessingTime(partFileInfo, processingTime)) {
        recycleDirtyOutputFormat(processingTime);
      }
    }
  }

  private void getOrCreateDirtyOutputFormat(long processingTime) throws IOException {
    LocalDateTime processingDateTime = Instant.ofEpochMilli(processingTime)
        .atZone(ZoneId.systemDefault()).toLocalDateTime();

    String partition = createPartition(processingDateTime);
    if (Objects.isNull(partFileInfo) || !partition.equalsIgnoreCase(partFileInfo.getPartition())) {
      //split by partition file info.
      if (Objects.nonNull(partFileInfo)) {
        close();
      }
      partFileInfo = new PartFileInfo(processingTime, 0);
      dirtyDir = new Path(dirtyParentDir, dateFormatter.format(processingDateTime));
      Path dirtyFilePath = getDirtyFilePath(processingDateTime, processingTime);

      partFileInfo.setPartition(partition);
      partFileInfo.setPath(dirtyFilePath);
      outputFormat = createNewOutputFormat(dirtyFilePath);
    }
  }

  private String createPartition(LocalDateTime processingDateTime) {
    return partitionFormatter.format(processingDateTime);
  }

  private void recycleDirtyOutputFormat(long processingTime) throws IOException {
    if (outputFormat != null) {
      close();
    }
    getOrCreateDirtyOutputFormat(processingTime);
  }

  private HdfsTextOutputFormat<Row> createNewOutputFormat(Path path) throws IOException {
    LOG.info("Hdfs dirty file path: {}.", path);

    HdfsTextOutputFormat<Row> outputFormat = new DirtyHdfsTextOutputFormat<>(hdfsDirtyJobConf, path);
    outputFormat.configure(configuration);
    outputFormat.open(taskId, 0);
    return outputFormat;
  }

  @Override
  public void close() throws IOException {
    // close all
    if (Objects.nonNull(outputFormat)) {
      outputFormat.close();
      partFileInfo = null;
      outputFormat = null;
    }
  }

  private Path getDirtyFilePath(LocalDateTime dateTime, long processingTime) {
    StringBuilder builder = new StringBuilder();
    builder.append(hourFormatter.format(dateTime))
        .append("_")
        .append("dirty_dorado")
        .append("_")
        .append(jobId)
        .append("_")
        .append(taskId)
        .append("_")
        .append(processingTime);

    return new Path(dirtyDir, builder.toString());
  }

  @Override
  public void clear() throws IOException {

  }

}
