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

package com.bytedance.bitsail.connector.legacy.rocketmq.sink.format.json;

import com.bytedance.bitsail.connector.legacy.rocketmq.sink.format.RocketMQSerializationSchema;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class JsonSerializationSchema implements RocketMQSerializationSchema {
  private static final Logger LOG = LoggerFactory.getLogger(JsonSerializationSchema.class);

  private static final long serialVersionUID = 3L;

  private final SerializationSchema<Row> serializationSchema;
  private final List<Integer> partitionKeyIndices;
  private final List<Integer> keyIndices;

  public JsonSerializationSchema(SerializationSchema<Row> serializationSchema, List<Integer> partitionKeyIndices, List<Integer> keyIndices) {
    this.serializationSchema = serializationSchema;
    this.partitionKeyIndices = partitionKeyIndices;
    this.keyIndices = keyIndices;
  }

  @Override
  public byte[] serializeKey(Row row) {
    if (keyIndices != null) {
      String key = this.keyIndices.stream()
          .map(i -> {
            Object keyField = row.getField(i);
            if (keyField != null) {
              return keyField.toString();
            }
            LOG.warn("Found null key in row: [{}]", row);
            return null;
          })
          .filter(StringUtils::isNotEmpty)
          .collect(Collectors.joining());
      return key.getBytes(StandardCharsets.UTF_8);
    } else {
      return null;
    }
  }

  @Override
  public byte[] serializeValue(Row row) {
    return serializationSchema.serialize(row);
  }

  @Override
  public String getPartitionKey(Row row) {
    if (partitionKeyIndices != null) {
      return this.partitionKeyIndices.stream()
          .map(i -> {
            Object partitionField = row.getField(i);
            if (partitionField != null) {
              return partitionField.toString();
            }
            LOG.warn("Found null key in row: [{}]", row);
            return null;
          })
          .filter(StringUtils::isNotEmpty)
          .collect(Collectors.joining());
    } else {
      return null;
    }
  }
}

