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

package com.bytedance.bitsail.flink.core.serialization;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.io.Serializable;

/**
 * abstract class of deserializationSchema
 */
@Internal
public class AbstractDeserializationSchema implements Serializable, ResultTypeQueryable<Row> {
  public static final int DEFAULT_SOURCE_INDEX = 0;
  public static final int DUMP_ROW_KEY_INDEX = 0;
  public static final int DUMP_ROW_VALUE_INDEX = 1;
  public static final int DUMP_ROW_PARTITION_INDEX = 2;
  public static final int DUMP_ROW_OFFSET_INDEX = 3;
  public static final int DUMP_ROW_SOURCE_INDEX_INDEX = 4;
  private static final long serialVersionUID = -8936998433692633275L;
  private static final long INVALID_MQ_OFFSET = -1L;

  protected Row deserialize(byte[] key, byte[] value, String partition) throws Exception {
    return deserialize(key, value, partition, INVALID_MQ_OFFSET, DEFAULT_SOURCE_INDEX);
  }

  protected Row deserialize(byte[] key, byte[] value, String partition, long offset) throws Exception {
    return deserialize(key, value, partition, offset, DEFAULT_SOURCE_INDEX);
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  protected Row deserialize(byte[] key, byte[] value, String partition, long offset, int index) throws Exception {
    Row row = new Row(5);
    row.setField(DUMP_ROW_KEY_INDEX, key);
    row.setField(DUMP_ROW_VALUE_INDEX, value);
    row.setField(DUMP_ROW_PARTITION_INDEX, partition);
    row.setField(DUMP_ROW_OFFSET_INDEX, offset);
    row.setField(DUMP_ROW_SOURCE_INDEX_INDEX, index);
    return row;
  }

  public boolean isEndOfStream(Row nextElement) {
    return false;
  }

  @Override
  public TypeInformation<Row> getProducedType() {
    return new RowTypeInfo(
        PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO,
        PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO,
        BasicTypeInfo.STRING_TYPE_INFO,
        BasicTypeInfo.LONG_TYPE_INFO,
        BasicTypeInfo.INT_TYPE_INFO);
  }
}
