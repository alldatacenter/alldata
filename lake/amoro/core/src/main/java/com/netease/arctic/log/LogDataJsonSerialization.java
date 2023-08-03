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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.netease.arctic.log.LogData.FieldGetterFactory;
import org.apache.iceberg.Schema;

import java.io.Serializable;

/**
 * Serialization that serializes an instance of {@link LogData} into a JSON bytes.
 */
public class LogDataJsonSerialization<T> implements Serializable {
  private static final long serialVersionUID = 66420071549145794L;
  private transient LogDataToJsonConverters.LogDataToJsonConverter<T> logDataToJsonConverter;

  private Schema schema;
  private LogData.FieldGetterFactory<T> fieldGetterFactory;

  /**
   * Reusable object node.
   */
  private transient ObjectNode node;
  /**
   * Object mapper that is used to create output JSON objects.
   */
  private final ObjectMapper mapper = new ObjectMapper();

  private transient LogDataToJsonConverters.LogDataToJsonConverter.FormatConverterContext converterContext;

  public LogDataJsonSerialization(Schema schema, FieldGetterFactory<T> fieldGetterFactory) {
    this.schema = schema;
    this.fieldGetterFactory = fieldGetterFactory;
  }

  public void init() {
    if (this.logDataToJsonConverter == null) {
      this.logDataToJsonConverter = LogDataToJsonConverters.createConverter(schema.asStruct(), fieldGetterFactory);
    }
  }

  public byte[] serialize(LogData<T> element) {
    // 4 bytes version + 4 bytes upstreamId + 8 bytes EpicNo + 1 byte flip + 1 byte rowKind + n bytes object data
    MessageBytes messageBytes = new MessageBytes();

    messageBytes
        .append(element.getVersionBytes())
        .append(element.getUpstreamIdBytes())
        .append(element.getEpicNoBytes())
        .append(element.getFlipByte())
        .append(element.getChangeActionByte());

    if (element.getFlip()) {
      // would ignore serializing actual value if flip is true.
      return messageBytes.toBytes();
    }

    // append n bytes data bytes
    if (node == null) {
      node = mapper.createObjectNode();
      converterContext =
          new LogDataToJsonConverters.LogDataToJsonConverter.FormatConverterContext(
              mapper, node
          );
    }

    try {
      convertRow(element);
      byte[] actualDataBytes = mapper.writeValueAsBytes(node);
      messageBytes.append(actualDataBytes);
    } catch (Throwable t) {
      throw new RuntimeException("Could not serialize row '" + element + "'. ", t);
    }
    return messageBytes.toBytes();
  }

  void convertRow(LogData<T> element) {
    init();
    logDataToJsonConverter.convert(element.getActualValue(), converterContext);
  }
}
