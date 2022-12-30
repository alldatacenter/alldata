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

import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.compress.CompressionCodec;

import java.io.IOException;

/**
 * HdfsSequenceOutputFormat.
 */
public class HdfsSequenceOutputFormat<IN extends Row> extends AbstractHdfsOutputFormat<IN> {
  private static final long serialVersionUID = 1L;

  private transient SequenceFile.Writer recordWriter;

  HdfsSequenceOutputFormat(final BitSailConfiguration jobConf, Path outputPath) {
    super(jobConf, outputPath);
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    this.recordWriter = null;
    super.open(taskNumber, numTasks);
  }

  @Override
  public void close() throws IOException {
    if (recordWriter != null) {
      recordWriter.hflush();
      recordWriter.close();
      recordWriter = null;
    }
    super.close();
  }

  @Override
  protected void sync() throws IOException {
    recordWriter.hflush();
  }

  @Override
  public void writeRecordInternal(IN record) throws IOException {
    recordWriter.append(getRecordKey(record), new BytesWritable((byte[]) record.getField(1)));
  }

  Object getRecordKey(IN record) {
    byte[] key = (byte[]) record.getField(0);
    return key == null || key.length == 0 ? EMPTY_KEY : new BytesWritable(key);
  }

  Class getKeyClass() {
    return BytesWritable.class;
  }

  @Override
  protected void initRecordWriter() throws IOException {
    super.initFileOutputStream();

    Class keyClass = getKeyClass();
    Class valueClass = BytesWritable.class;
    CompressionCodec compressionCodec = HdfsFileSystemFactory.getCompressionCodec(compressionCodecName, hdfsCompressionConfig);
    SequenceFile.CompressionType compressionType = compressionCodec != null ? SequenceFile.CompressionType.BLOCK : SequenceFile.CompressionType.NONE;

    recordWriter = SequenceFile.createWriter(hadoopFileSystem.getConf(),
        SequenceFile.Writer.stream(this.outputStream),
        SequenceFile.Writer.keyClass(keyClass),
        SequenceFile.Writer.valueClass(valueClass),
        SequenceFile.Writer.compression(compressionType, compressionCodec));
  }

  @Override
  public String toString() {
    return "HdfsSequenceOutputFormat(" + outputPath.toString() + ")";
  }
}
