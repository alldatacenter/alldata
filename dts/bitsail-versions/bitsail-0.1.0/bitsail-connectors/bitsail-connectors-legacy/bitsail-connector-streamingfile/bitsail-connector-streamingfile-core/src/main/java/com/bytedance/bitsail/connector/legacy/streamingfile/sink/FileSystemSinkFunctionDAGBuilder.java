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

package com.bytedance.bitsail.connector.legacy.streamingfile.sink;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.OutputFormatFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionWriterFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.FileSystemCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.StreamingFileSystemSink;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.AbstractFileSystemFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs.HdfsFileSystemFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive.HiveFileSystemFactory;
import com.bytedance.bitsail.flink.core.writer.FlinkDataWriterDAGBuilder;

import lombok.NoArgsConstructor;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_FORMAT_TYPE_VALUE;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HIVE_FORMAT_TYPE_VALUE;

/**
 * @class: FileSystemSinkFunctionDAGBuilder
 * @desc:
 **/
@NoArgsConstructor
public class FileSystemSinkFunctionDAGBuilder<OUT extends Row> extends FlinkDataWriterDAGBuilder<OUT> {
  private static final Logger LOG = LoggerFactory.getLogger(FileSystemSinkFunctionDAGBuilder.class);

  protected BitSailConfiguration jobConf;
  protected DescriptorProperties descriptorProperties;

  private StreamingFileSystemSink<OUT> fileSystemSink;

  @Override
  public void configure(ExecutionEnviron execution,
                        BitSailConfiguration writerConfiguration) throws Exception {
    jobConf = execution.getGlobalConfiguration();
    descriptorProperties = new DescriptorProperties(true);
    addSinkConf(jobConf);
    this.fileSystemSink = getStreamingFileSystemSink(jobConf);
    this.sinkFunction = fileSystemSink;
  }

  @Override
  public String getWriterName() {
    return "filesystem";
  }

  private StreamingFileSystemSink<OUT> getStreamingFileSystemSink(final BitSailConfiguration jobConf) throws Exception {
    AbstractFileSystemFactory<OUT> fileSystemFactory = getFileSystemFactory(jobConf);
    OutputFormatFactory<OUT> outputFormatFactory = fileSystemFactory.createOutputFormatFactory();
    PartitionComputer<OUT> partitionComputer = fileSystemFactory.createPartitionComputer();
    PartitionWriterFactory<OUT> partitionWriterFactory = fileSystemFactory.createPartitionWriterFactory();
    FileSystemCommitter committer = fileSystemFactory.createFileSystemCommitter();
    committer.overwriteDirtyCollector();

    return new StreamingFileSystemSink<>(
        outputFormatFactory,
        partitionComputer,
        partitionWriterFactory,
        committer,
        jobConf);
  }

  private AbstractFileSystemFactory<OUT> getFileSystemFactory(BitSailConfiguration jobConf) {
    String formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);
    if (formatType == null || formatType.isEmpty()) {
      throw new RuntimeException("Format type is missing.");
    }

    AbstractFileSystemFactory<OUT> abstractFileSystemFactory;
    switch (formatType) {
      case HDFS_FORMAT_TYPE_VALUE:
        abstractFileSystemFactory = new HdfsFileSystemFactory<>(jobConf);
        break;
      case HIVE_FORMAT_TYPE_VALUE:
        abstractFileSystemFactory = new HiveFileSystemFactory<>(jobConf);
        break;
      default:
        throw new RuntimeException("Unsupported format type: " + formatType);
    }
    return abstractFileSystemFactory;
  }

  private void addSinkConf(BitSailConfiguration jobConf) {
    String formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);
    if (formatType == null || formatType.isEmpty()) {
      LOG.info("Format type is missing.");
      return;
    }

    boolean skipConf = jobConf.get(CommonOptions.JOB_CONFIG_SKIP);
    if (skipConf) {
      return;
    }
    if (HIVE_FORMAT_TYPE_VALUE.equalsIgnoreCase(formatType)) {
      Preconditions.checkState(jobConf.fieldExists(FileSystemSinkOptions.HIVE_METASTORE_PROPERTIES));
    }
  }
}
