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

package com.bytedance.bitsail.connector.legacy.hudi.format;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.hudi.common.HudiWriteOptions;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.formats.json.JsonRowDataDeserializationSchema;
import org.apache.flink.formats.json.TimestampFormat;
import org.apache.flink.table.runtime.typeutils.RowDataTypeInfo;
import org.apache.flink.table.types.logical.RowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserFormatFactory {
  private static final Logger LOG = LoggerFactory.getLogger(ParserFormatFactory.class);

  public static DeserializationSchema getDeserializationSchema(BitSailConfiguration jobConf, RowType outputType) {
    String formatType = jobConf.get(HudiWriteOptions.FORMAT_TYPE);
    if (formatType == null || formatType.isEmpty()) {
      throw new RuntimeException("Job writer format type is missing.");
    }
    DeserializationSchema deserializationSchema;
    switch (formatType) {
      case "json":
        deserializationSchema = getJsonDeserializationSchema(jobConf, outputType);
        break;
      default:
        throw new RuntimeException("Unsupported format type: " + formatType);
    }
    return deserializationSchema;
  }

  private static JsonRowDataDeserializationSchema getJsonDeserializationSchema(BitSailConfiguration jobConf, RowType rowType) {
    final boolean ignoreParseErrors = jobConf.get(HudiWriteOptions.FORMAT_IGNORE_PARSE_ERRORS);

    final String timestampType = jobConf.get(HudiWriteOptions.FORMAT_TIMESTAMP_FORMAT).toLowerCase();
    LOG.info("Timestamp Type is : " + timestampType);

    TimestampFormat tsFormat;
    switch (timestampType) {
      case "iso_8601":
        tsFormat = TimestampFormat.ISO_8601;
        break;
      case "sql":
        tsFormat = TimestampFormat.SQL;
        break;
      default:
        throw new RuntimeException("Unsupported timestamp type: " + timestampType);
    }
    return new JsonRowDataDeserializationSchema(
        rowType,
        new RowDataTypeInfo(rowType),
        false,
        ignoreParseErrors,
        tsFormat
    );
  }
}
