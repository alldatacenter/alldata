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

package com.bytedance.bitsail.dump.datasink.file.parser;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.parser.error.ParserErrorCode;

import org.apache.commons.lang.ArrayUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @class: RowBytesParser
 * @desc:
 **/
public class RowBytesParser extends BytesParser {

  private BitSailConfiguration jobConf;

  public RowBytesParser(BitSailConfiguration jobConf) throws Exception {
    this.jobConf = jobConf;
  }

  @Override
  public Row parse(Row row, byte[] line, RowTypeInfo rowTypeInfo) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public Tuple2<Row, Object> parse(Row row, byte[] bytes, RowTypeInfo rowTypeInfo, List<FieldPathUtils.PathInfo> pathInfos) throws Exception {
    throw new UnsupportedOperationException();
  }

  public void parseBitSailRow(Row flinkRow, Row bitSailRow, RowTypeInfo flinkRowTypeInfo) throws BitSailException {
    try {
      for (int index = 0; index < bitSailRow.getArity(); index++) {
        TypeInformation typeInfo = flinkRowTypeInfo.getTypeAt(index);
        Column bitSailValue = (Column) bitSailRow.getField(index);

        Object flinkValue = createFlinkValue(typeInfo, bitSailValue);
        flinkRow.setField(index, flinkValue);
      }
    } catch (Exception e) {
      throw BitSailException.asBitSailException(ParserErrorCode.ILLEGAL_TEXT, "value: " + bitSailRow.toString(), e);
    }
  }

  private Object createFlinkValue(TypeInformation typeInfo, Column fieldVal) {
    Class columnTypeClass = typeInfo.getTypeClass();
    if (columnTypeClass == List.class) {
      return createListValue((org.apache.flink.api.java.typeutils.ListTypeInfo) typeInfo, (List) fieldVal);
    } else if (columnTypeClass == Map.class) {
      return createMapValue((org.apache.flink.api.java.typeutils.MapTypeInfo) typeInfo, (Map) fieldVal);
    } else {
      return createBasicValue(typeInfo, fieldVal);
    }
  }

  private List<?> createListValue(org.apache.flink.api.java.typeutils.ListTypeInfo typeInfo, List<Column> fieldVal) {
    TypeInformation<?> valueTypeInfo = typeInfo.getElementTypeInfo();
    List<Object> lc = new ArrayList<>();
    if (fieldVal != null) {
      for (int j = 0; j < fieldVal.size(); j++) {
        Object listValue = createFlinkValue(valueTypeInfo, fieldVal.get(j));
        lc.add(listValue);
      }
    }
    return lc;
  }

  private Map<?, ?> createMapValue(org.apache.flink.api.java.typeutils.MapTypeInfo typeInfo, Map<Column, Column> fieldVal) {
    TypeInformation<?> keyTypeInfo = typeInfo.getKeyTypeInfo();
    TypeInformation<?> valueTypeInfo = typeInfo.getValueTypeInfo();

    Map<Object, Object> mc = new HashMap<>();
    if (fieldVal != null) {
      fieldVal.forEach((key, value) -> {
        Object mapKey = createBasicValue(keyTypeInfo, key);
        Object mapValue = createFlinkValue(valueTypeInfo, value);
        mc.put(mapKey, mapValue);
      });
    }
    return mc;
  }

  private Object createBasicValue(TypeInformation typeInfo, Column column) {
    Class typeClass = typeInfo.getTypeClass();

    if (null == column.getRawData()) {
      return null;
    }

    if (typeClass == String.class) {
      return column.asString();
    } else if (typeClass == Boolean.class) {
      return column.asBoolean();
    } else if (typeClass == byte[].class) {
      return column.asBytes();
    } else if (typeClass == Byte[].class) {
      return ArrayUtils.toObject(column.asBytes());
    } else if (typeClass == Short.class) {
      return column.asLong().shortValue();
    } else if (typeClass == Integer.class) {
      return column.asLong().intValue();
    } else if (typeClass == Long.class) {
      return column.asLong();
    } else if (typeClass == BigInteger.class) {
      return column.asBigInteger();
    } else if (typeClass == Byte.class) {
      return column.asLong().byteValue();
    } else if (typeClass == Date.class) {
      return column.asDate();
    } else if (typeClass == java.sql.Date.class) {
      return new java.sql.Date(column.asDate().getTime());
    } else if (typeClass == java.sql.Time.class) {
      return new java.sql.Time(column.asDate().getTime());
    } else if (typeClass == java.sql.Timestamp.class) {
      return new java.sql.Timestamp(column.asDate().getTime());
    } else if (typeClass == Float.class) {
      return column.asDouble().floatValue();
    } else if (typeClass == Double.class) {
      return column.asDouble();
    } else if (typeClass == BigDecimal.class) {
      return column.asBigDecimal();
    } else {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
          "Flink basic data type " + typeClass + " is not supported!");
    }
  }
}
