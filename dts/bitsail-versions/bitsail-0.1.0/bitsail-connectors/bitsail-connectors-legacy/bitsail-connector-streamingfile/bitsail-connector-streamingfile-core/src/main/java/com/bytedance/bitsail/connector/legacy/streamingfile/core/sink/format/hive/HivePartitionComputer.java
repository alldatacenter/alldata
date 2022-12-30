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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive;

import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionType;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema.HiveFileSystemMetaManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.types.Row;

import java.util.LinkedHashMap;
import java.util.Objects;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.DEFAULT_PARTITION;
import static com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema.DUMP_ROW_VALUE_INDEX;

/**
 * @class: HivePartitionComputer
 * @desc:
 **/
public class HivePartitionComputer<IN extends Row> extends AbstractPartitionComputer<IN> implements PartitionComputer<IN> {

  private static final long serialVersionUID = 522520746018395427L;
  private transient HiveFileSystemMetaManager fileSystemMetaManager;

  public HivePartitionComputer(final BitSailConfiguration jobConf) throws Exception {
    super(jobConf);
  }

  @Override
  public void open(RuntimeContext context, FileSystemMetaManager fileSystemMetaManager) {
    super.open(context, fileSystemMetaManager);
    this.fileSystemMetaManager = (HiveFileSystemMetaManager) fileSystemMetaManager;
  }

  @Override
  public LinkedHashMap<String, String> generatePartValues(IN row) {
    return generatePartitionAndRowData(row).f0;
  }

  @Override
  public Tuple2<LinkedHashMap<String, String>, IN> generatePartitionAndRowData(IN in) {
    Tuple2<Row, Object> rowAndTime = fileSystemMetaManager.getRowBuilder().parseBitSailRowAndEventTime(
        (byte[]) in.getField(DUMP_ROW_VALUE_INDEX), getEventTimeFields());
    Row rowData = rowAndTime.f0;
    long timestamp = convertEventTimestamp(rowAndTime.f1);

    LinkedHashMap<String, String> partSpec = generateTimePartSpecs(in, timestamp);
    for (int i = partSpec.size(); i < partitionKeys.size(); i++) {
      String partitionValue;

      PartitionInfo partitionInfo = partitionKeys.get(i);
      if (PartitionType.STATIC.equals(partitionInfo.getType())) {
        partitionValue = partitionInfo.getValue();
      } else {
        Column field = (Column) rowData.getField(fileSystemMetaManager.getPartitionColumnMap().get(partitionInfo));
        if (Objects.nonNull(field) && StringUtils.isNotEmpty(field.asString())) {
          partitionValue = field.asString();
        } else {
          partitionValue = StringUtils.isEmpty(partitionInfo.getValue()) ?
              DEFAULT_PARTITION
              : partitionInfo.getValue();
        }
      }
      partSpec.put(partitionInfo.getName(), partitionValue);
    }
    return new Tuple2<>(partSpec, (IN) Row.project(rowData, fileSystemMetaManager.getNormalColumnInfoIndices()));
  }

  @Override
  public IN projectColumnsToWrite(IN in) {
    return generatePartitionAndRowData(in).f1;
  }
}
