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

package com.bytedance.bitsail.connector.legacy.rocketmq.sink.format;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.connector.legacy.rocketmq.error.RocketMQPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.rocketmq.sink.format.json.JsonSerializationSchema;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.json.JsonRowSerializationSchema;
import org.apache.flink.types.Row;

import java.util.List;

public class RocketMQSerializationFactory {

  TypeInformation<Row> rowTypeInfo;
  List<Integer> partitionIndices;
  List<Integer> keyIndices;

  public RocketMQSerializationFactory(TypeInformation<Row> typeInfo, List<Integer> partitionIndices, List<Integer> keyIndices) {
    this.rowTypeInfo = typeInfo;
    this.partitionIndices = partitionIndices;
    this.keyIndices = keyIndices;
  }

  public RocketMQSerializationSchema getSerializationSchemaByFormat(RocketMQSinkFormat format) {
    if (format == RocketMQSinkFormat.JSON) {
      SerializationSchema<Row> jsonSerialSchema = JsonRowSerializationSchema.builder().withTypeInfo(this.rowTypeInfo).build();
      return new JsonSerializationSchema(jsonSerialSchema, partitionIndices, keyIndices);
    }
    throw BitSailException.asBitSailException(RocketMQPluginErrorCode.UNSUPPORTED_FORMAT,
        "unsupported sink format: " + format);
  }
}
