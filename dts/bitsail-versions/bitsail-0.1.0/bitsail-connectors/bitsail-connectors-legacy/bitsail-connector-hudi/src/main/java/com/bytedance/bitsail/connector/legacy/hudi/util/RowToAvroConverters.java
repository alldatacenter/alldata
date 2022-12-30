/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Files: apache/hudi (https://github.com/apache/hudi)
 * Copyright: Copyright 2019-2020 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.connector.legacy.hudi.util;

import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.SqlTimeTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ListTypeInfo;
import org.apache.flink.api.java.typeutils.MapTypeInfo;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool class used to convert from {@link Row} to Avro {@link GenericRecord}.
 *
 * <p>NOTE: reference from Flink release 1.12.0, should remove when Flink version upgrade to that.
 */
@Internal
@SuppressWarnings("checkstyle:MagicNumber")
public class RowToAvroConverters {

  private static Conversions.DecimalConversion decimalConversion = new Conversions.DecimalConversion();

  // --------------------------------------------------------------------------------
  // Runtime Converters
  // --------------------------------------------------------------------------------

  /**
   * Creates a runtime converter according to the given logical type that converts objects of
   * Flink Table & SQL internal data structures to corresponding Avro data structures.
   */
  public static RowToAvroConverter createConverter(TypeInformation type) {
    final RowToAvroConverter converter;
    Class typeClass = type.getTypeClass();
    if (BasicTypeInfo.SHORT_TYPE_INFO.getTypeClass().equals(typeClass)) {
      converter =
          new RowToAvroConverter() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Schema schema, Object object) {
              return ((Short) object).intValue();
            }
          };
    } else if (BasicTypeInfo.BOOLEAN_TYPE_INFO.getTypeClass().equals(typeClass) || BasicTypeInfo.INT_TYPE_INFO.getTypeClass().equals(typeClass) ||
        BasicTypeInfo.LONG_TYPE_INFO.getTypeClass().equals(typeClass) || BasicTypeInfo.FLOAT_TYPE_INFO.getTypeClass().equals(typeClass) ||
        BasicTypeInfo.DOUBLE_TYPE_INFO.getTypeClass().equals(typeClass) || SqlTimeTypeInfo.TIME.getTypeClass().equals(typeClass) ||
        SqlTimeTypeInfo.DATE.getTypeClass().equals(typeClass) || SqlTimeTypeInfo.TIMESTAMP.getTypeClass().equals(typeClass)) {
      converter =
          new RowToAvroConverter() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Schema schema, Object object) {
              return object;
            }
          };
    } else if (BasicTypeInfo.BIG_INT_TYPE_INFO.getTypeClass().equals(typeClass)) {
      converter =
          new RowToAvroConverter() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Schema schema, Object object) {
              return ((BigInteger) object).longValue();
            }
          };
    } else if (BasicTypeInfo.STRING_TYPE_INFO.getTypeClass().equals(typeClass)) {
      converter =
          new RowToAvroConverter() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Schema schema, Object object) {
              return new Utf8(object.toString());
            }
          };
    } else if (PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO.getTypeClass().equals(typeClass)) {
      converter =
          new RowToAvroConverter() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Schema schema, Object object) {
              return ByteBuffer.wrap((byte[]) object);
            }
          };
    } else if (BasicTypeInfo.BIG_DEC_TYPE_INFO.getTypeClass().equals(typeClass)) {
      converter =
          new RowToAvroConverter() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object convert(Schema schema, Object object) {
              BigDecimal javaDecimal = (BigDecimal) object;
              return decimalConversion.toFixed(javaDecimal, schema, schema.getLogicalType());
            }
          };
    } else if (type instanceof ListTypeInfo) {
      converter = createListConverter((ListTypeInfo) type);
    } else if (type instanceof MapTypeInfo) {
      converter = createMapConverter((MapTypeInfo) type);
    } else if (type instanceof RowTypeInfo) {
      converter = createRowConverter((RowTypeInfo) type);
    } else {
      throw new UnsupportedOperationException("Unsupported type: " + type);
    }

    // wrap into nullable converter
    return new RowToAvroConverter() {
      private static final long serialVersionUID = 1L;

      @Override
      public Object convert(Schema schema, Object object) {
        if (object == null) {
          return null;
        }

        // get actual schema if it is a nullable schema
        Schema actualSchema;
        if (schema.getType() == Schema.Type.UNION) {
          List<Schema> types = schema.getTypes();
          int size = types.size();
          if (size == 2 && types.get(1).getType() == Schema.Type.NULL) {
            actualSchema = types.get(0);
          } else if (size == 2 && types.get(0).getType() == Schema.Type.NULL) {
            actualSchema = types.get(1);
          } else {
            throw new IllegalArgumentException(
                "The Avro schema is not a nullable type: " + schema.toString());
          }
        } else {
          actualSchema = schema;
        }
        return converter.convert(actualSchema, object);
      }
    };
  }

  // --------------------------------------------------------------------------------
  // IMPORTANT! We use anonymous classes instead of lambdas for a reason here. It is
  // necessary because the maven shade plugin cannot relocate classes in
  // SerializedLambdas (MSHADE-260). On the other hand we want to relocate Avro for
  // sql-client uber jars.
  // --------------------------------------------------------------------------------

  private static RowToAvroConverter createRowConverter(RowTypeInfo rowType) {
    final RowToAvroConverter[] fieldConverters = Arrays.stream(rowType.getFieldTypes())
        .map(RowToAvroConverters::createConverter).toArray(RowToAvroConverter[]::new);
    final int length = rowType.getArity();

    return new RowToAvroConverter() {
      private static final long serialVersionUID = 1L;

      @Override
      public Object convert(Schema schema, Object object) {
        final Row row = (Row) object;
        final List<Schema.Field> fields = schema.getFields();
        final GenericRecord record = new GenericData.Record(schema);
        for (int i = 0; i < length; ++i) {
          final Schema.Field schemaField = fields.get(i);
          Object avroObject =
              fieldConverters[i].convert(
                  schemaField.schema(), row.getField(i));
          record.put(i, avroObject);
        }
        return record;
      }
    };
  }

  private static RowToAvroConverter createListConverter(ListTypeInfo listTypeInfo) {
    final RowToAvroConverter elementConverter = createConverter(listTypeInfo.getElementTypeInfo());

    return new RowToAvroConverter() {
      private static final long serialVersionUID = 1L;

      @Override
      public Object convert(Schema schema, Object object) {
        final Schema elementSchema = schema.getElementType();
        List inputList = (List) object;
        List<Object> convertedList = new ArrayList<>();
        for (int i = 0; i < inputList.size(); ++i) {
          convertedList.add(
              elementConverter.convert(
                  elementSchema, inputList.get(i)));
        }
        return convertedList;
      }
    };
  }

  private static RowToAvroConverter createMapConverter(MapTypeInfo mapTypeInfo) {
    TypeInformation valueType = mapTypeInfo.getKeyTypeInfo();
    final RowToAvroConverter valueConverter = createConverter(valueType);

    return new RowToAvroConverter() {
      private static final long serialVersionUID = 1L;

      @Override
      public Object convert(Schema schema, Object object) {
        final Schema valueSchema = schema.getValueType();
        final HashMap inputMap = (HashMap) object;
        final Object[] keyArray = inputMap.keySet().toArray();
        final Map<Object, Object> resultMap = new HashMap<>(inputMap.size());
        for (Object key : keyArray) {
          // avro only support use string as key
          final String keyInString = key.toString();
          final Object value = valueConverter.convert(
              valueSchema, inputMap.get(key));
          resultMap.put(keyInString, value);
        }
        return resultMap;
      }
    };
  }

  /**
   * Runtime converter that converts objects of Flink Table & SQL internal data structures to
   * corresponding Avro data structures.
   */
  @FunctionalInterface
  public interface RowToAvroConverter extends Serializable {
    Object convert(Schema schema, Object object);
  }
}

