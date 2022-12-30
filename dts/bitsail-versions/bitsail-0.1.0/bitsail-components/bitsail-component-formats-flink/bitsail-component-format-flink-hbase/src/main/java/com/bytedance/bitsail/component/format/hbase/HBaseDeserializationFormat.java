/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.component.format.hbase;

import com.bytedance.bitsail.base.format.DeserializationFormat;
import com.bytedance.bitsail.base.format.DeserializationSchema;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.BooleanColumn;
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.column.DoubleColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.typeinfo.BasicArrayTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfos;

import org.apache.flink.types.Row;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.charset.Charset;

public class HBaseDeserializationFormat implements DeserializationFormat<byte[][], Row> {

  private static final Integer BIT_LENGTH = 1;

  private BitSailConfiguration configuration;

  public HBaseDeserializationFormat(BitSailConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public DeserializationSchema<byte[][], Row> createRuntimeDeserializationSchema(TypeInfo<?>[] typeInfos) {
    return new DeserializationSchema<byte[][], Row>() {

      @Override
      public boolean isEndOfStream(Row nextElement) {
        return false;
      }

      @Override
      public Row deserialize(byte[][] rowCell) {
        Row row = new Row(typeInfos.length);
        for (int i = 0; i < row.getArity(); i++) {
          TypeInfo<?> typeInfo = typeInfos[i];
          Column column = deserializeValue(typeInfo, rowCell[i]);
          row.setField(i, column);
        }
        return row;
      }

      private Column deserializeValue(TypeInfo<?> typeInfo, byte[] cell) throws BitSailException {
        Class<?> columnTypeClass = typeInfo.getTypeClass();

        if (TypeInfos.STRING_TYPE_INFO.getTypeClass() == columnTypeClass) {
          return new StringColumn(cell == null ? null : new String(cell, Charset.defaultCharset()));

        } else if (TypeInfos.BOOLEAN_TYPE_INFO.getTypeClass() == columnTypeClass) {
          if (cell != null && cell.length > BIT_LENGTH) {
            return new BooleanColumn(Boolean.valueOf(Bytes.toString(cell)));
          }
          return new BooleanColumn(cell == null ? null : Bytes.toBoolean(cell));

        } else if (TypeInfos.INT_TYPE_INFO.getTypeClass() == columnTypeClass ||
            TypeInfos.SHORT_TYPE_INFO.getTypeClass() == columnTypeClass) {
          return new LongColumn(cell == null ? null : Integer.valueOf(Bytes.toString(cell)));

        } else if (columnTypeClass == TypeInfos.LONG_TYPE_INFO.getTypeClass()
            || columnTypeClass == TypeInfos.BIG_INTEGER_TYPE_INFO.getTypeClass()) {
          return new LongColumn(cell == null ? null : Long.valueOf(Bytes.toString(cell)));

        } else if (columnTypeClass == TypeInfos.DOUBLE_TYPE_INFO.getTypeClass()
            || columnTypeClass == TypeInfos.FLOAT_TYPE_INFO.getTypeClass()) {
          return new DoubleColumn(cell == null ? null : Double.valueOf(Bytes.toString(cell)));

        } else if (columnTypeClass == TypeInfos.SQL_DATE_TYPE_INFO.getTypeClass() ||
            columnTypeClass == TypeInfos.SQL_TIME_TYPE_INFO.getTypeClass() ||
            columnTypeClass == TypeInfos.SQL_TIMESTAMP_TYPE_INFO.getTypeClass()) {
          return new DateColumn(cell == null ? null : Long.valueOf(Bytes.toString(cell)));

        } else if (columnTypeClass == BasicArrayTypeInfo.BINARY_TYPE_INFO.getTypeClass()) {
          return new BytesColumn(cell);

        } else {
          throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_COLUMN_TYPE,
              columnTypeClass + " is not supported yet!");
        }

      }
    };
  }
}
