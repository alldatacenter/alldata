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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_MSGPACK;
import static com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema.DUMP_ROW_VALUE_INDEX;

/**
 * HdfsTextOutputFormat.
 */
public class HdfsTextOutputFormat<IN extends Row> extends AbstractHdfsOutputFormat<IN> {
  private static final Logger LOG = LoggerFactory.getLogger(HdfsTextOutputFormat.class);

  private static final long serialVersionUID = 1L;

  private transient ObjectMapper messagePackDer;
  private transient ObjectMapper jsonObjectSer;

  public HdfsTextOutputFormat(final BitSailConfiguration jobConf, Path outputPath) {
    super(jobConf, outputPath);
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    this.messagePackDer = new ObjectMapper(new MessagePackFactory());
    this.jsonObjectSer = new ObjectMapper();
    super.open(taskNumber, numTasks);
  }

  @Override
  public void close() throws IOException {
    if (compressionOutputStream != null) {
      compressionOutputStream.flush();
      outputStream.hflush();
      compressionOutputStream.close();

      compressionOutputStream = null;
      outputStream = null;
    }
    super.close();
  }

  @Override
  protected void sync() throws IOException {
    if (compressionOutputStream != null) {
      compressionOutputStream.flush();
    }
    outputStream.hflush();
  }

  @Override
  public void writeRecordInternal(IN record) throws IOException {
    byte[] value = getRecordValue(record);
    if (value == null) {
      return;
    }

    if (compressionOutputStream != null) {
      this.compressionOutputStream.write(value);
      this.compressionOutputStream.write(NEWLINE);
    } else {
      this.outputStream.write(value);
      this.outputStream.write(NEWLINE);
    }
  }

  private byte[] getRecordValue(IN record) {
    byte[] value = (byte[]) record.getField(DUMP_ROW_VALUE_INDEX);
    // convert msgpack to json
    if (value != null && dumpType.equalsIgnoreCase(HDFS_DUMP_TYPE_MSGPACK)) {
      return messagePackToJson(value);
    }
    return value;
  }

  private byte[] messagePackToJson(byte[] bytes) {
    try {
      return jsonObjectSer.writeValueAsBytes(messagePackDer.readValue(bytes, Object.class));
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  protected void initRecordWriter() throws IOException {
    initCompressFileOutputStream();
  }

  @Override
  public String toString() {
    return "HdfsTextOutputFormat(" + outputPath.toString() + ")";
  }
}
