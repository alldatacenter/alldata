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

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.CallTracer;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.MetricsFactory;

import org.apache.flink.api.java.hadoop.common.HadoopOutputFormatCommonBase;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.compress.CodecPool;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.io.compress.Compressor;
import org.apache.hadoop.mapred.JobConf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.HDFS_FILE_FLUSH_LATENCY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.HDFS_FILE_WRITE_LATENCY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.INVALID_TASK_ID;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.SUB_TASK_ID;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_COMPRESSION_CODEC_ZSTD;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_TEXT;
import static com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs.HdfsFileSystemFactory.getCompressionCodec;
import static com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema.DUMP_ROW_VALUE_INDEX;

/**
 * AbstractHdfsOutputFormat.
 */
public abstract class AbstractHdfsOutputFormat<IN extends Row> extends HadoopOutputFormatCommonBase<IN> {
  static final Long FLUSH_BATCH_COUNT = 3000L;
  static final Long FLUSH_BATCH_SIZE = 3 * 1024 * 1024L;
  static final BytesWritable EMPTY_KEY = new BytesWritable(new byte[] {});
  static final long EMPTY_KEY_LENGTH = 0L;
  static final String CHARSET_NAME = "UTF-8";
  static final byte[] NEWLINE;
  private static final long serialVersionUID = -2457589149266232172L;

  static {
    try {
      NEWLINE = "\n".getBytes(CHARSET_NAME);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("can't find " + CHARSET_NAME + " encoding");
    }
  }

  Path outputPath;
  String dumpType;
  String compressionCodecName;
  Boolean hdfsCompressionConfig;
  transient org.apache.hadoop.fs.FileSystem hadoopFileSystem;
  transient org.apache.hadoop.fs.FSDataOutputStream outputStream;
  transient CompressionOutputStream compressionOutputStream;
  private Short hdfsReplication;
  private BitSailConfiguration jobConf;
  private Map<String, String> hdfsConf;
  private transient int taskId;
  private transient int recordCount;
  private transient int recordSize;
  private transient MetricManager metrics;
  private transient Compressor compressor;

  AbstractHdfsOutputFormat(final BitSailConfiguration jobConf, Path outputPath) {
    super(new JobConf().getCredentials());
    this.jobConf = jobConf;
    this.outputPath = outputPath;
    this.dumpType = jobConf.getUnNecessaryOption(FileSystemSinkOptions.HDFS_DUMP_TYPE, HDFS_DUMP_TYPE_TEXT);
    this.hdfsCompressionConfig = jobConf.get(FileSystemSinkOptions.HDFS_COMPRESSION_CONFIG);
    this.compressionCodecName = jobConf.getUnNecessaryOption(FileSystemSinkOptions.HDFS_COMPRESSION_CODEC, HDFS_COMPRESSION_CODEC_ZSTD);
    this.hdfsReplication = jobConf.get(FileSystemSinkOptions.HDFS_REPLICATION);
    this.hdfsConf = jobConf.getUnNecessaryMap(FileSystemCommonOptions.HDFS_ADVANCED_ARGS);
  }

  @Override
  public void configure(Configuration parameters) {
    taskId = parameters.getInteger(SUB_TASK_ID, INVALID_TASK_ID);
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    recordCount = 0;
    recordSize = 0;
    compressor = null;
    outputStream = null;
    compressionOutputStream = null;
    hadoopFileSystem = null;
    initHadoopFileSystem(hdfsConf);
    initRecordWriter();
    metrics = MetricsFactory.getInstanceMetricsManager(jobConf, taskId);
  }

  @Override
  public void close() throws IOException {
    if (outputStream != null) {
      outputStream.hflush();
      outputStream.close();
      outputStream = null;
    }

    if (compressor != null) {
      try {
        CodecPool.returnCompressor(compressor);
      } catch (NullPointerException e) {
        // For some compressors like 4mc/4mz, it has been reset in compressionOutputStream.close()
      }
      compressor = null;
    }
  }

  @Override
  public void writeRecord(IN record) throws IOException {
    byte[] value = (byte[]) record.getField(DUMP_ROW_VALUE_INDEX);
    if (value == null) {
      return;
    }

    try (CallTracer ignored = metrics.recordTimer(getWriteRecordMetric()).get()) {
      writeRecordInternal(record);
    }

    flush(value);
  }

  private void flush(byte[] value) throws IOException {
    recordCount++;
    recordSize += value.length;

    if (recordCount >= FLUSH_BATCH_COUNT || recordSize >= FLUSH_BATCH_SIZE) {
      try (CallTracer ignored = metrics.recordTimer(getFlushBatchMetric()).get()) {
        sync();
      }
      recordCount = 0;
      recordSize = 0;
    }
  }

  public String getFlushBatchMetric() {
    return HDFS_FILE_FLUSH_LATENCY;
  }

  public String getWriteRecordMetric() {
    return HDFS_FILE_WRITE_LATENCY;
  }

  protected abstract void writeRecordInternal(IN record) throws IOException;

  protected abstract void initRecordWriter() throws IOException;

  protected abstract void sync() throws IOException;

  @SuppressWarnings("checkstyle:MagicNumber")
  private void initHadoopFileSystem(Map<String, String> userDefinedArgs) throws IOException {
    org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
    if (jobConf.fieldExists(FileSystemSinkOptions.CUSTOMIZED_HADOOP_CONF)) {
      Map<String, String> hadoopConf = jobConf.getUnNecessaryMap(FileSystemSinkOptions.CUSTOMIZED_HADOOP_CONF);
      hadoopConf.forEach(conf::set);
    }
    conf.setBoolean("fs.automatic.close", false);
    conf.setBoolean("dfs.ignore.local.node", true);
    conf.setBoolean("dfs.client.retry.policy.enabled", false);
    conf.setInt("dfs.client.block.write.retries", 512);
    conf.setInt("dfs.client.failover.max.attempts", 50);
    conf.setBoolean(CommonConfigurationKeys.IPC_CLIENT_PING_KEY, false);

    userDefinedArgs.forEach(conf::set);

    org.apache.hadoop.fs.Path hadoopOutputPath = new org.apache.hadoop.fs.Path(outputPath.toString());
    hadoopFileSystem = hadoopOutputPath.getFileSystem(conf);
  }

  void initFileOutputStream() throws IOException {
    org.apache.hadoop.fs.Path hadoopOutputPath = new org.apache.hadoop.fs.Path(outputPath.toString());
    this.outputStream = hadoopFileSystem.create(hadoopOutputPath, false, hadoopFileSystem.getConf().getInt(
            CommonConfigurationKeysPublic.IO_FILE_BUFFER_SIZE_KEY,
            CommonConfigurationKeysPublic.IO_FILE_BUFFER_SIZE_DEFAULT),
        hdfsReplication,
        hadoopFileSystem.getDefaultBlockSize(hadoopOutputPath));
  }

  void initCompressFileOutputStream() throws IOException {
    initFileOutputStream();
    CompressionCodec compressionCodec = getCompressionCodec(compressionCodecName, hdfsCompressionConfig);
    if (compressionCodec != null) {
      compressor = CodecPool.getCompressor(compressionCodec, hadoopFileSystem.getConf());
      compressionOutputStream = compressionCodec.createOutputStream(outputStream, compressor);
    }
  }

  @Override
  public String toString() {
    return "AbstractHdfsOutputFormat(" + outputPath.toString() + ") - " + CHARSET_NAME;
  }
}
