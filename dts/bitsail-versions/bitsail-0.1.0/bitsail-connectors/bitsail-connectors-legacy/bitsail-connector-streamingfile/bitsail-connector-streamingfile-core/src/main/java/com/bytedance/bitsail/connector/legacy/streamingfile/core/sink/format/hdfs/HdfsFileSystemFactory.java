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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.OutputFormatFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.directory.PartitionDirectoryManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.FileSystemCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.AbstractFileSystemFactory;

import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_COMPRESSION_CODEC_NONE;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_COMPRESSION_CODEC_ZSTD;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_BINARY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_BINLOG;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_DEBEZIUM_JSON;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_PB;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_TEXT;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_TFRECORD;

/**
 * {@link HdfsFileSystemFactory} hdfs sink factory for hdfs.
 */
public class HdfsFileSystemFactory<IN extends Row> extends AbstractFileSystemFactory<IN> {
  private static final Logger LOG = LoggerFactory.getLogger(HdfsFileSystemFactory.class);

  private final String dumpType;
  private final Boolean hdfsCompressionConfig;

  public HdfsFileSystemFactory(BitSailConfiguration jobConf) {
    super(jobConf);
    this.dumpType = jobConf.getUnNecessaryOption(FileSystemSinkOptions.HDFS_DUMP_TYPE, HDFS_DUMP_TYPE_TEXT);
    this.hdfsCompressionConfig = jobConf.get(FileSystemSinkOptions.HDFS_COMPRESSION_CONFIG);
  }

  static CompressionCodec getCompressionCodec(String compressionCodecName, Boolean hdfsCompressionConfig) {
    Configuration conf = new Configuration();
    LOG.info("default hadoop conf: {}", conf);

    if (compressionCodecName.equalsIgnoreCase(HDFS_COMPRESSION_CODEC_NONE)) {
      return null;
    }
    LOG.info("support io.compression.codecs: " + conf.get("io.compression.codecs"));

    CompressionCodecFactory codecFactory = new CompressionCodecFactory(conf);
    LOG.info("CompressionCodecFactory: {}", codecFactory);
    LOG.info("compressionCodecName: {}", compressionCodecName);
    CompressionCodec codec = codecFactory.getCodecByName(compressionCodecName);
    if (codec == null) {
      throw new IllegalArgumentException("Codec " + compressionCodecName + " is unsupported.");
    }
    LOG.info("CompressionCodec class name: {}", codec.getClass().getName());
    if (codec instanceof org.apache.hadoop.conf.Configurable) {
      ((org.apache.hadoop.conf.Configurable) codec).setConf(conf);
    }

    return codec;
  }

  @Override
  public OutputFormatFactory<IN> createOutputFormatFactory() {
    return new HdfsOutputFormatFactory<>(jobConf);
  }

  @Override
  public String getOutputDir() {
    return jobConf.get(FileSystemCommonOptions.CONNECTOR_PATH);
  }

  @Override
  public FileSystemCommitter createFileSystemCommitter() throws Exception {
    String outputDir = getOutputDir();
    String stagingDir = toStagingDir(outputDir);
    LOG.info("HDFS OutputDir={}", outputDir);
    LOG.info("HDFS StagingDir={}", stagingDir);

    if (hdfsOverwrite) {
      Path outputPath = new Path(outputDir);
      if (FileSystem.get(outputPath.toUri()).exists(outputPath)) {
        FileSystem.get(outputPath.toUri()).delete(outputPath, true);
        LOG.info("Delete HDFS OutputDir={}", outputDir);
      }

      Path stagingPath = new Path(stagingDir);
      if (FileSystem.get(stagingPath.toUri()).exists(stagingPath)) {
        FileSystem.get(stagingPath.toUri()).delete(stagingPath, true);
        LOG.info("Delete HDFS StagingDir={}", stagingDir);
      }
    }

    String compressionCodecName = jobConf.getUnNecessaryOption(
        FileSystemSinkOptions.HDFS_COMPRESSION_CODEC, HDFS_COMPRESSION_CODEC_ZSTD);
    CompressionCodec compressionCodec = getCompressionCodec(compressionCodecName, hdfsCompressionConfig);
    String compressionExtension = compressionCodec != null ? compressionCodec.getDefaultExtension() : null;
    if (dumpType.equalsIgnoreCase(HDFS_DUMP_TYPE_PB)) {
      compressionExtension = compressionExtension != null ? ".pb" + compressionExtension : ".pb";
    }
    LOG.info("HDFS Compress Codec: " + compressionExtension);

    // move hour partition to file name like: /{date}/{hour}_task_dorado_{job_id}_{task_id}.{timestamp}
    PartitionDirectoryManager.DirectoryType directoryType = PartitionDirectoryManager.getDirectoryType(jobConf);
    return new FileSystemCommitter(
        FileSystem::get,
        createMetaStoreFactory(),
        false,
        new Path(stagingDir),
        new LinkedHashMap<>(),
        directoryType.getPartitionSize(partitionKeys),
        compressionExtension,
        jobConf);
  }

  @Override
  public HdfsPartitionComputer<IN> createPartitionComputer() throws Exception {
    return new HdfsPartitionComputer<>(jobConf);
  }

  @Override
  public TableMetaStoreFactory createMetaStoreFactory() {
    String outputPath = jobConf.get(FileSystemCommonOptions.CONNECTOR_PATH);
    HdfsMetaStoreFactory.HdfsMetaStoreFactoryBuilder builder = HdfsMetaStoreFactory.builder()
        .locationPath(outputPath)
        .partitionInfoMap(PartitionUtils.getPartitionInfoMap(jobConf))
        .partSpecMapping(PartitionUtils.getPartitionMapping(jobConf));

    return builder.build();
  }

  public static class HdfsOutputFormatFactory<IN extends Row> implements OutputFormatFactory<IN> {
    private final BitSailConfiguration jobConf;

    HdfsOutputFormatFactory(final BitSailConfiguration jobConf) {
      this.jobConf = jobConf;
    }

    @Override
    public AbstractHdfsOutputFormat<IN> createOutputFormat(Path path, FileSystemMetaManager manager) {
      String dumpType = jobConf.getUnNecessaryOption(FileSystemSinkOptions.HDFS_DUMP_TYPE, HDFS_DUMP_TYPE_TEXT);
      switch (dumpType.toLowerCase()) {
        case HDFS_DUMP_TYPE_BINARY:
          return new HdfsSequenceOutputFormat<>(jobConf, path);
        case HDFS_DUMP_TYPE_BINLOG:
        case HDFS_DUMP_TYPE_DEBEZIUM_JSON:
          return new HdfsBinlogOutputFormat<>(jobConf, path);
        case HDFS_DUMP_TYPE_PB:
          return new HdfsPBOutputFormat<>(jobConf, path);
        case HDFS_DUMP_TYPE_TFRECORD:
          return new HdfsTFRecordOutputFormat<>(jobConf, path);
        default:
          return new HdfsTextOutputFormat<>(jobConf, path);
      }
    }
  }
}
