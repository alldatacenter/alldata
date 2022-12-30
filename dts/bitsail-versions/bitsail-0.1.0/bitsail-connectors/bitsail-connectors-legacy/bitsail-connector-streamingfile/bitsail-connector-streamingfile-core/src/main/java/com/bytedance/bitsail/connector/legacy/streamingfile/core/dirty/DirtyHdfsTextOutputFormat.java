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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs.HdfsTextOutputFormat;

import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.DIRTY_HDFS_FILE_FLUSH_LATENCY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.DIRTY_HDFS_FILE_WRITE_LATENCY;

/**
 * Created 2020/11/4.
 */
public class DirtyHdfsTextOutputFormat<IN extends Row> extends HdfsTextOutputFormat<IN> {
  public DirtyHdfsTextOutputFormat(BitSailConfiguration jobConf, Path outputPath) {
    super(jobConf, outputPath);
  }

  @Override
  public String getFlushBatchMetric() {
    return DIRTY_HDFS_FILE_FLUSH_LATENCY;
  }

  @Override
  public String getWriteRecordMetric() {
    return DIRTY_HDFS_FILE_WRITE_LATENCY;
  }
}
