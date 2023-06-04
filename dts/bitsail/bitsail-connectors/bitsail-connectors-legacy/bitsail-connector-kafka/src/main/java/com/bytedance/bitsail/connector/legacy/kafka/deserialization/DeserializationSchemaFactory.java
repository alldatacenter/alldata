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

package com.bytedance.bitsail.connector.legacy.kafka.deserialization;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.connector.legacy.kafka.constants.KafkaConstants;
import com.bytedance.bitsail.connector.legacy.messagequeue.source.option.BaseMessageQueueReaderOptions;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.formats.json.JsonRowDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.types.Row;

import java.util.List;
import java.util.Map;

/**
 * Created 2022/8/30
 */
public class DeserializationSchemaFactory {
  private static final String STREAMING_FILE_DESERIALIZATION_SCHEMA_KEY = "streaming_file";
  private static final String JSON_DESERIALIZATION_SCHEMA_KEY = "json";

  public static KafkaDeserializationSchema<Row> getDeserializationSchema(BitSailConfiguration configuration) {
    String formatType = configuration.get(BaseMessageQueueReaderOptions.FORMAT_TYPE);
    if (StringUtils.equalsIgnoreCase(STREAMING_FILE_DESERIALIZATION_SCHEMA_KEY, formatType)) {
      boolean countMode = configuration.get(BaseMessageQueueReaderOptions.ENABLE_COUNT_MODE);
      boolean multiSource = configuration.get(CommonOptions.MULTI_SOURCE_ENABLED);
      if (countMode) {
        return new CountKafkaDeserializationSchema(configuration);
      } else {
        Map<String, String> connectorConf = configuration.getUnNecessaryMap(BaseMessageQueueReaderOptions.CONNECTOR_PROPERTIES);
        int sourceIndex = multiSource ?
            Integer.parseInt(connectorConf.get(KafkaConstants.CONNECTOR_SOURCE_INDEX)) :
            RowKafkaDeserializationSchema.DEFAULT_SOURCE_INDEX;
        return new RowKafkaDeserializationSchema(sourceIndex);
      }
    }
    if (StringUtils.equalsIgnoreCase(JSON_DESERIALIZATION_SCHEMA_KEY, formatType)) {
      List<ColumnInfo> columnInfos = configuration.get(ReaderOptions.BaseReaderOptions.COLUMNS);
      RowTypeInfo rowTypeInfo = NativeFlinkTypeInfoUtil.getRowTypeInformation(columnInfos);
      return new CountKafkaDeserializationSchemaWrapper<>(configuration,
          new JsonRowDeserializationSchema.Builder(rowTypeInfo)
              .build());
    }

    throw new IllegalArgumentException(String.format("Unsupported %s format type.", formatType));
  }
}
