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

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * HdfsPBOutputFormat.
 */
public class HdfsPBOutputFormat<IN extends Row> extends AbstractHdfsOutputFormat<IN> {
  private static final long serialVersionUID = -1276541181228015472L;

  private transient DataOutputStream dataOutputStream;

  HdfsPBOutputFormat(final BitSailConfiguration jobConf, Path outputPath) {
    super(jobConf, outputPath);
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);
    dataOutputStream = null;
    if (compressionOutputStream != null) {
      dataOutputStream = new DataOutputStream(compressionOutputStream);
    } else {
      dataOutputStream = new DataOutputStream(outputStream);
    }
  }

  @Override
  public void close() throws IOException {
    if (dataOutputStream != null) {
      dataOutputStream.flush();
      outputStream.hflush();
      dataOutputStream.close();

      dataOutputStream = null;
      compressionOutputStream = null;
      outputStream = null;
    }
    super.close();
  }

  @Override
  protected void sync() throws IOException {
    dataOutputStream.flush();
    outputStream.hflush();
  }

  @Override
  public void writeRecordInternal(IN record) throws IOException {
    byte[] value = (byte[]) record.getField(1);

    dataOutputStream.writeLong(EMPTY_KEY_LENGTH);
    dataOutputStream.write(EMPTY_KEY.getBytes());
    dataOutputStream.writeLong(Long.reverseBytes(value.length));
    dataOutputStream.write(value);
  }

  @Override
  protected void initRecordWriter() throws IOException {
    initCompressFileOutputStream();
  }

  @Override
  public String toString() {
    return "HdfsPBOutputFormat(" + outputPath.toString() + ")";
  }
}
