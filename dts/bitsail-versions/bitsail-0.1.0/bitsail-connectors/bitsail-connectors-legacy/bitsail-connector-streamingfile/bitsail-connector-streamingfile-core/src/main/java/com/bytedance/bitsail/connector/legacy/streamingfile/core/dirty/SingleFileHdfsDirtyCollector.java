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

import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.base.dirty.DirtyRecord;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs.AbstractHdfsOutputFormat;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs.HdfsTextOutputFormat;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.DUMP_DIRTY_DIR;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_COMPRESSION_CODEC_NONE;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_TEXT;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_REPLICATION_DEFAULT;

/**
 * Created 2020/4/23.
 */
public class SingleFileHdfsDirtyCollector extends AbstractDirtyCollector {
  private static final Logger LOG =
      LoggerFactory.getLogger(SingleFileHdfsDirtyCollector.class);
  protected final transient Configuration configuration;
  protected BitSailConfiguration hdfsDirtyJobConf;
  protected Path outputDir;
  protected Path dirtyParentDir;
  private AbstractHdfsOutputFormat<Row> outputFormat;
  private PartFileInfo partFileInfo;

  public SingleFileHdfsDirtyCollector(BitSailConfiguration jobConf,
                                      Path path,
                                      int taskId,
                                      Configuration configuration) {
    super(jobConf, taskId);
    this.outputDir = path;
    this.configuration = configuration;
    constructStreamingMode();
  }

  protected static DirtyRecord buildDirtyRecord(Row row, Throwable e) {
    return DirtyRecord.builder()
        .dirty(new String((byte[]) row.getField(1)))
        .base64Dirty(Base64.getEncoder().encodeToString((byte[]) row.getField(1)))
        .exception(ExceptionUtils.getMessage(e))
        .build();
  }

  protected static Row wrapper(byte[] dirtyArray) {
    Row row = new Row(2);
    row.setField(0, null);
    row.setField(1, dirtyArray);
    return row;
  }

  private static Path toDirtyParentDir(Path outputDir) {
    Preconditions.checkNotNull(outputDir, "Output dir is empty.");
    return new Path(outputDir, DUMP_DIRTY_DIR);
  }

  protected void constructStreamingMode() {
    jobId = jobConf.get(CommonOptions.JOB_ID);
    dirtyParentDir = toDirtyParentDir(outputDir);
    initHdfsDirtyJobConf();
  }

  private void initHdfsDirtyJobConf() {
    hdfsDirtyJobConf = BitSailConfiguration.newDefault();
    hdfsDirtyJobConf.set(FileSystemSinkOptions.HDFS_REPLICATION, Short.valueOf(HDFS_REPLICATION_DEFAULT));
    hdfsDirtyJobConf.set(FileSystemSinkOptions.HDFS_DUMP_TYPE, HDFS_DUMP_TYPE_TEXT);
    hdfsDirtyJobConf.set(FileSystemSinkOptions.HDFS_COMPRESSION_CODEC, HDFS_COMPRESSION_CODEC_NONE);
  }

  @Override
  protected void collect(Object dirtyObj, Throwable e, long processingTime) throws IOException {
    if (dirtyObj instanceof Row) {
      DirtyRecord dirtyRecord = buildDirtyRecord((Row) dirtyObj, e);
      if (Objects.nonNull(dirtyRecord)) {
        getOrCreateDirtyOutputFormat(processingTime);
        byte[] dirtyArray = JsonSerializer.serializeAsBytes(dirtyRecord);
        partFileInfo.update(dirtyArray.length, processingTime);
        outputFormat.writeRecord(wrapper(dirtyArray));
      }
    }
  }

  @Override
  public void onProcessingTime(long processingTime) throws IOException {
  }

  private void getOrCreateDirtyOutputFormat(long processingTime) throws IOException {

    if (Objects.isNull(partFileInfo)) {
      //split by partition file info.

      partFileInfo = new PartFileInfo(processingTime, 0);
      Path dirtyFilePath = getDirtyFilePath(processingTime);

      partFileInfo.setPath(dirtyFilePath);
      outputFormat = createNewOutputFormat(dirtyFilePath);
    }
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

  private Path getDirtyFilePath(long processingTime) {
    StringBuilder builder = new StringBuilder();
    builder.append("dirty_dorado")
        .append("_")
        .append(jobId)
        .append("_")
        .append(taskId);

    return new Path(dirtyParentDir, builder.toString());
  }

  @Override
  public void clear() throws IOException {
    if (isRunning) {
      FileSystem fileSystem = FileSystem.get(dirtyParentDir.toUri());
      fileSystem.delete(dirtyParentDir, true);
      LOG.error("Hdfs dirty collector overwrite path {}.", dirtyParentDir);
    }
  }

}
