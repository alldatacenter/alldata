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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * HdfsTFRecordOutputFormat.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class HdfsTFRecordOutputFormat<IN extends Row> extends AbstractHdfsOutputFormat<IN> {
  private static final long serialVersionUID = 1L;

  HdfsTFRecordOutputFormat(final BitSailConfiguration jobConf, Path outputPath) {
    super(jobConf, outputPath);
  }

  @Override
  protected void sync() throws IOException {
    outputStream.hflush();
  }

  @Override
  public void writeRecordInternal(IN record) throws IOException {
    byte[] value = (byte[]) record.getField(1);
    if (value == null) {
      return;
    }

    byte[] lenBytes = toInt64LE(value.length);
    outputStream.write(lenBytes);
    outputStream.write(toInt32LE(Crc32C.maskedCrc32c(lenBytes)));
    outputStream.write(value);
    outputStream.write(toInt32LE(Crc32C.maskedCrc32c(value)));
  }

  private byte[] toInt64LE(long data) {
    byte[] buff = new byte[8];
    ByteBuffer bb = ByteBuffer.wrap(buff);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putLong(data);
    return buff;
  }

  private byte[] toInt32LE(int data) {
    byte[] buff = new byte[4];
    ByteBuffer bb = ByteBuffer.wrap(buff);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(data);
    return buff;
  }

  @Override
  protected void initRecordWriter() throws IOException {
    initFileOutputStream();
  }

  @Override
  public String toString() {
    return "HdfsTFRecordOutputFormat(" + outputPath.toString() + ")";
  }
}
