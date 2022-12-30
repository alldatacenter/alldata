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
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionType;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.DEFAULT_PARTITION;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_BINLOG;
import static com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema.DUMP_ROW_VALUE_INDEX;

/**
 * @class: HivePartitionComputer
 * @desc:
 **/
public class HdfsPartitionComputer<IN extends Row> extends AbstractPartitionComputer<IN> implements PartitionComputer<IN> {
  private static final Logger LOG = LoggerFactory.getLogger(HdfsPartitionComputer.class);
  private static final long serialVersionUID = 4012019767487720040L;

  private Map<String, FieldPathUtils.PathInfo> pathInfoMap;

  public HdfsPartitionComputer(final BitSailConfiguration jobConf) throws Exception {
    super(jobConf);
    checkSupportDynamicPartition(jobConf.getUnNecessaryOption(
        FileSystemSinkOptions.HDFS_DUMP_TYPE, HDFS_DUMP_TYPE_BINLOG));
    pathInfoMap = partitionKeys.stream()
        .collect(Collectors.toMap(PartitionInfo::getName, index -> FieldPathUtils.getPathInfo(index.getName())));
  }

  private void checkSupportDynamicPartition(String dumpType) {
    if (hasDynamicPartition) {
      Preconditions.checkState(StreamingFileSystemValidator
          .HDFS_SUPPORT_DYNAMIC_PARTITIONS.contains(dumpType));
    }
  }

  @Override
  public LinkedHashMap<String, String> generatePartValues(IN in) {
    return generatePartitionAndRowData(in).f0;
  }

  @Override
  public IN projectColumnsToWrite(IN in) {
    return in;
  }

  @Override
  public Tuple2<LinkedHashMap<String, String>, IN> generatePartitionAndRowData(IN in) {
    Object record = parseRowData((byte[]) in.getField(DUMP_ROW_VALUE_INDEX));

    LinkedHashMap<String, String> partSpec = Maps.newLinkedHashMap();
    LocalDateTime localDateTime = generateArchiveEventTime(in, extractEventTimestamp(record));
    for (PartitionInfo partitionInfo : partitionKeys) {

      if (PartitionType.DYNAMIC.equals(partitionInfo.getType())
          || PartitionType.SPLIT.equals(partitionInfo.getType())) {
        String filed = getField(record, pathInfoMap.get(partitionInfo.getName()), partitionInfo.getValue());
        partSpec.put(partitionInfo.getName(), filed);

      } else if (PartitionType.STATIC.equals(partitionInfo.getType())) {
        partSpec.put(partitionInfo.getName(), partitionInfo.getValue());

      } else if (PartitionType.TIME.equals(partitionInfo.getType())) {
        partSpec.put(partitionInfo.getName(), partitionInfo.getFormatter().format(localDateTime));
      }
    }

    return new Tuple2<>(partSpec, projectColumnsToWrite(in));
  }

  private Object parseRowData(byte[] record) {
    if (isEventTime || hasDynamicPartition) {
      try {
        return extractor.parse(record);
      } catch (Exception e) {
        LOG.error("Subtask {} parse row data failed.", taskId);
      }
    }
    return null;
  }

  private String getField(Object record, FieldPathUtils.PathInfo pathInfo, String defaultValue) {
    String columnValue = defaultValue;
    if (Objects.nonNull(record)) {
      try {
        columnValue = extractor.getField(record, pathInfo, defaultValue);
      } catch (Exception e) {
        LOG.error("Subtask {} get field from path {} failed.", taskId, pathInfo.getName());
      }
    }
    if (StringUtils.isEmpty(columnValue)) {
      columnValue = StringUtils.isEmpty(defaultValue) ?
          DEFAULT_PARTITION
          : defaultValue;
    }
    return columnValue;
  }

}
