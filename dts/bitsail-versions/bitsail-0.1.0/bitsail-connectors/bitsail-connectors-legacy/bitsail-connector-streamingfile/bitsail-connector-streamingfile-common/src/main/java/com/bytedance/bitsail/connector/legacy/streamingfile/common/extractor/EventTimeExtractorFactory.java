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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor.binlog.DebeziumEventTimeExtractor;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor.binlog.MysqlBinlogEventTimeExtractor;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;

import org.apache.commons.lang3.StringUtils;

/**
 * Created 2020-02-11.
 */
public class EventTimeExtractorFactory {

  public static AbstractEventTimeExtractor create(BitSailConfiguration jobConf) throws Exception {
    String dumpType = jobConf.getUnNecessaryOption(FileSystemSinkOptions.HDFS_DUMP_TYPE, StreamingFileSystemValidator.HDFS_DUMP_TYPE_BINLOG);
    if (StringUtils.isEmpty(dumpType)) {
      throw new RuntimeException("Dump type is missing.");
    }
    if (jobConf.get(FileSystemCommonOptions.ArchiveOptions.CUSTOM_EXTRACTOR_CLASSPATH) != null) {
      return new CustomEventTimeExtractor(jobConf);
    }
    switch (dumpType) {
      case StreamingFileSystemValidator.HDFS_DUMP_TYPE_BINLOG:
        return new MysqlBinlogEventTimeExtractor(jobConf);
      case StreamingFileSystemValidator.HDFS_DUMP_TYPE_JSON:
      case StreamingFileSystemValidator.HDFS_DUMP_TYPE_MSGPACK:
        return new TextEventTimeExtractor(jobConf);
      case StreamingFileSystemValidator.HDFS_DUMP_TYPE_DEBEZIUM_JSON:
        return new DebeziumEventTimeExtractor(jobConf);
      default:
        throw new UnsupportedOperationException(String.format("Dump type: %s don't support archive current.", dumpType));
    }
  }
}
