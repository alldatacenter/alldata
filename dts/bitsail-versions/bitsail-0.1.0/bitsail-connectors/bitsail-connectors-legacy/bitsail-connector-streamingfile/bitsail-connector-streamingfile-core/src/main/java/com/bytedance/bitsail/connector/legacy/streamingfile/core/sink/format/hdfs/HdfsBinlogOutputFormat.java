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

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.apache.hadoop.io.Text;

/**
 * @class: HdfsBinlogOutputFormat
 * @desc:
 **/
@SuppressWarnings("checkstyle:MagicNumber")
public class HdfsBinlogOutputFormat<IN extends Row> extends HdfsSequenceOutputFormat<IN> {
  private static final long serialVersionUID = -2430092222203400576L;

  private static final Text EMPTY_TEXT_KEY = new Text();

  HdfsBinlogOutputFormat(final BitSailConfiguration jobConf, Path outputPath) {
    super(jobConf, outputPath);
  }

  /**
   * Get the partition and offset details from the record and add it as a key in the format {partition}_{offset}
   *
   * @param record input record
   * @return partition and offset
   */
  @Override
  Object getRecordKey(IN record) {
    Object partition = record.getField(2);
    Object offset = record.getField(4);
    if (partition == null || offset == null) {
      return EMPTY_TEXT_KEY;
    }
    String partString = partition.toString();
    String offsetString = offset.toString();
    if (StringUtils.isEmpty(partString) || StringUtils.isEmpty(offsetString)) {
      return EMPTY_TEXT_KEY;
    }
    return new Text(String.format("%s_%s", partString, offsetString));
  }

  @Override
  Class getKeyClass() {
    return Text.class;
  }

  @Override
  public String toString() {
    return "HdfsBinlogOutputFormat(" + outputPath.toString() + ")";
  }
}
