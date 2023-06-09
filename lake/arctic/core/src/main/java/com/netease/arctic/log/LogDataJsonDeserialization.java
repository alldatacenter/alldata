/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netease.arctic.log.data.LogArrayData;
import com.netease.arctic.log.data.LogMapData;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.base.Predicate;
import org.apache.iceberg.relocated.com.google.common.primitives.Longs;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

import static com.netease.arctic.utils.FlipUtil.convertToBoolean;
import static org.apache.iceberg.relocated.com.google.common.base.Preconditions.checkArgument;

/**
 * Deserialization that deserializes a JSON bytes array into an instance of {@link LogData}
 * through {@link LogData.Factory#create(Object, Object...)}
 */
public class LogDataJsonDeserialization<T> implements Serializable {
  private static final long serialVersionUID = -5741370033707067127L;
  private static final Logger LOG = LoggerFactory.getLogger(LogDataJsonDeserialization.class);
  private static final int ROW_BEGINNING_POS = 18;

  private final JsonToLogDataConverters.JsonToLogDataConverter<T> jsonToLogDataConverter;
  private final LogData.Factory<T> factory;

  /**
   * Object mapper for parsing the JSON.
   */
  private final ObjectMapper objectMapper = new ObjectMapper();

  public LogDataJsonDeserialization(
      Schema schema,
      LogData.Factory<T> factory,
      LogArrayData.Factory arrayFactory,
      LogMapData.Factory mapFactory) {
    this.factory = factory;
    this.jsonToLogDataConverter =
        new JsonToLogDataConverters<>(factory, arrayFactory, mapFactory)
            .createConverter(schema.asStruct());
    boolean hasDecimalType = hasDecimalType(schema.asStruct());
    if (hasDecimalType) {
      objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }
  }

  private boolean hasDecimalType(Types.StructType structType) {
    return structType
        .fields()
        .stream()
        .map(Types.NestedField::type)
        .anyMatch((Predicate<Type>) type -> {
          if (type.typeId() == Type.TypeID.STRUCT) {
            boolean hasDecimalType = hasDecimalType((Types.StructType) type);
            if (hasDecimalType) {
              return true;
            }
          }
          return type.typeId() == Type.TypeID.DECIMAL;
        });
  }

  public LogData<T> deserialize(byte[] message) throws IOException {
    try {
      checkArgument(message != null, "message is null.");
      checkArgument(message.length >= ROW_BEGINNING_POS, "message is illegal.");
      byte[] versionBytes = Bytes.subByte(message, 0, 4);
      byte[] upstreamIdBytes = Bytes.subByte(message, 4, 4);
      long epicNo = Longs.fromByteArray(Bytes.subByte(message, 8, 8));
      byte flip = Bytes.subByte(message, 16, 1)[0];
      byte changeActionByte = Bytes.subByte(message, 17, 1)[0];

      boolean flipBoolean = convertToBoolean(flip);

      T actualValue;
      if (flipBoolean) {
        // we can ignore actual value which should be empty, when flip is true.
        return factory.create(null, versionBytes, upstreamIdBytes, epicNo, true, changeActionByte);
      }

      byte[] actualValueBytes = Bytes.subByte(message, 18, message.length - 18);
      final JsonNode root = objectMapper.readTree(actualValueBytes);
      actualValue = (T) jsonToLogDataConverter.convert(root, null);
      return factory.create(actualValue, versionBytes, upstreamIdBytes, epicNo, false, changeActionByte);
    } catch (Throwable t) {
      LOG.error("", t);
      throw t;
    }
  }
}
