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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.netease.arctic.log.data.LogArrayData;
import com.netease.arctic.log.data.LogMapData;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.netease.arctic.log.TimeFormats.ISO8601_TIMESTAMP_WITH_LOCAL_TIMEZONE_FORMAT;
import static com.netease.arctic.log.TimeFormats.SQL_TIMESTAMP_FORMAT;
import static com.netease.arctic.log.TimeFormats.SQL_TIME_FORMAT;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

/**
 * Tool class used to convert from {@link LogData} to {@link JsonNode}
 */
public class LogDataToJsonConverters implements Serializable {
  private static final long serialVersionUID = 6851339012836496637L;

  /**
   * Runtime converter that converts {@link LogData} into {@link JsonNode}
   *
   * @param <T> indicate an actual value wrapped within {@link LogData}
   */
  interface LogDataToJsonConverter<T> extends Converter<
      Object, JsonNode, LogDataToJsonConverter.FormatConverterContext, T> {

    class FormatConverterContext implements Serializable {
      private static final long serialVersionUID = 109329249465478511L;
      private transient JsonNode node;
      private final ObjectMapper mapper;

      public FormatConverterContext(
          ObjectMapper jsonMapper, JsonNode node) {
        this.mapper = jsonMapper;
        this.node = node;
      }
    }
  }

  public static <T> LogDataToJsonConverter<T> createConverter(
      Type type,
      LogData.FieldGetterFactory<T> fieldGetterFactory) {

    return wrapIntoNullableConverter(createNotNullConverter(type, fieldGetterFactory));
  }

  private static <T> LogDataToJsonConverter<T> createNotNullConverter(
      Type type,
      LogData.FieldGetterFactory<T> fieldGetterFactory) {
    // 以下 switch case 来源于 org.apache.iceberg.types.TypeUtil
    // and org.apache.iceberg.flink.TypeToFlinkType
    switch (type.typeId()) {
      case BOOLEAN:
        return (source, context) -> context.mapper.getNodeFactory().booleanNode((Boolean) source);
      case INTEGER:
        return ((source, context) -> context.mapper.getNodeFactory().numberNode((int) source));
      case LONG:
        return ((source, context) -> context.mapper.getNodeFactory().numberNode((long) source));
      case FLOAT:
        return ((source, context) -> context.mapper.getNodeFactory().numberNode((float) source));
      case DOUBLE:
        return ((source, context) -> context.mapper.getNodeFactory().numberNode((double) source));
      case DATE:
        return createDateConverter();
      case TIME:
        // For the type: Flink only support TimeType with default precision (second) now. The precision of time is
        // not supported in Flink, so we can think of it as a simple time type directly.
        // For the data: Flink uses int that support mills to represent time data, so it supports mills precision.
        return createTimeConverter();
      case TIMESTAMP:
        Types.TimestampType timestamp = (Types.TimestampType) type;
        if (timestamp.shouldAdjustToUTC()) {
          return createTimestampWithLocalZone();
        } else {
          return createTimestampConverter();
        }
      case STRING:
        return ((source, context) -> context.mapper.getNodeFactory().textNode(source.toString()));
      case UUID:
      case FIXED:
      case BINARY:
        return ((source, context) -> context.mapper.getNodeFactory().binaryNode((byte[]) source));
      case DECIMAL:
        return ((source, context) -> context.mapper.getNodeFactory().numberNode((BigDecimal) source));
      case LIST:
        return createListConverter(type, fieldGetterFactory);
      case MAP:
        return createMapConverter(type, fieldGetterFactory);
      case STRUCT:
        return createStructConverter(type, fieldGetterFactory);
      default:
        throw new UnsupportedOperationException("Not Support to parse type: " + type);
    }
  }

  private static <T> LogDataToJsonConverter<T> createMapConverter(
      Type type,
      LogData.FieldGetterFactory<T> fieldGetterFactory) {
    Types.MapType map = type.asNestedType().asMapType();
    Types.NestedField keyField = map.field(map.keyId());
    Types.NestedField valueField = map.field(map.valueId());
    final LogDataToJsonConverter<T> valueConverter =
        createConverter(valueField.type(), fieldGetterFactory);
    final LogArrayData.ElementGetter valueGetter = LogArrayData.createElementGetter(valueField);
    final LogArrayData.ElementGetter keyGetter = LogArrayData.createElementGetter(keyField);
    return ((source, context) -> {
      ObjectNode node;
      // reuse could be a NullNode if last record is null.
      if (context.node == null || context.node.isNull()) {
        node = context.mapper.createObjectNode();
      } else {
        node = (ObjectNode) context.node;
        node.removeAll();
      }
      LogMapData mapData = (LogMapData) source;
      LogArrayData keyArray = mapData.keyArray();
      LogArrayData valueArray = mapData.valueArray();
      int numElements = mapData.size();
      for (int i = 0; i < numElements; i++) {
        String fieldName;
        if (keyArray.isNullAt(i)) {
          // when map key is null
          throw new RuntimeException(
              "JSON format doesn't support to serialize map data with null keys. ");
        } else {
          Object obj = keyGetter.getElementOrNull(keyArray, i);
          fieldName = null != obj ? obj.toString() : null;
        }

        Object value = valueGetter.getElementOrNull(valueArray, i);

        LogDataToJsonConverter.FormatConverterContext subContext =
            new LogDataToJsonConverter.FormatConverterContext(
                context.mapper,
                node.get(fieldName));
        node.set(fieldName, valueConverter.convert(value, subContext));
      }

      return node;
    });
  }

  private static <T> LogDataToJsonConverter<T> createListConverter(
      Type type,
      LogData.FieldGetterFactory<T> fieldGetterFactory) {
    Types.ListType list = type.asNestedType().asListType();
    Types.NestedField elementField = list.field(list.elementId());
    LogDataToJsonConverter<T> elementConverter =
        createConverter(elementField.type(), fieldGetterFactory);
    LogArrayData.ElementGetter elementGetter =
        LogArrayData.createElementGetter(elementField);

    return (source, context) -> {
      ArrayNode node;

      // reuse could be a NullNode if last record is null.
      if (context.node == null || context.node.isNull()) {
        node = context.mapper.createArrayNode();
      } else {
        node = (ArrayNode) context.node;
        node.removeAll();
      }
      LogArrayData array = (LogArrayData) source;
      int numElements = array.size();
      LogDataToJsonConverter.FormatConverterContext subContext =
          new LogDataToJsonConverter.FormatConverterContext(
              context.mapper,
              null);
      for (int i = 0; i < numElements; i++) {
        Object element = elementGetter.getElementOrNull(array, i);

        node.add(elementConverter.convert(element, subContext));
      }
      return node;
    };
  }

  private static <T> LogDataToJsonConverter<T> createTimestampWithLocalZone() {
    return (source, context) -> {
      Instant instant = (Instant) source;
      return context.mapper.getNodeFactory()
          .textNode(
              ISO8601_TIMESTAMP_WITH_LOCAL_TIMEZONE_FORMAT.format(
                  instant
                      .atOffset(ZoneOffset.UTC)));
    };
  }

  private static <T> LogDataToJsonConverter<T> createTimestampConverter() {
    return (source, context) -> {
      LocalDateTime localDateTime = (LocalDateTime) source;
      return context.mapper.getNodeFactory()
          .textNode(SQL_TIMESTAMP_FORMAT.format(localDateTime));
    };
  }

  private static <T> LogDataToJsonConverter<T> createDateConverter() {
    return (source, context) -> {
      int days = (int) source;
      LocalDate date = LocalDate.ofEpochDay(days);
      return context.mapper.getNodeFactory().textNode(ISO_LOCAL_DATE.format(date));
    };
  }

  private static <T> LogDataToJsonConverter<T> createTimeConverter() {
    return (source, context) -> {
      long nanosSecond = (long) source;
      LocalTime time = LocalTime.ofNanoOfDay(nanosSecond);
      return context.mapper.getNodeFactory().textNode(SQL_TIME_FORMAT.format(time));
    };
  }

  private static <T> LogDataToJsonConverter<T> createStructConverter(
      Type type,
      LogData.FieldGetterFactory<T> fieldGetterFactory) {
    final Types.StructType structType = type.asNestedType().asStructType();
    final List<Types.NestedField> fields = structType.fields();
    final Type[] fieldTypes = fields.stream()
        .map(Types.NestedField::type)
        .toArray(Type[]::new);
    final String[] fieldNames = fields.stream()
        .map(Types.NestedField::name).toArray(String[]::new);
    final List<LogDataToJsonConverter<T>> fieldConverterList =
        Arrays.stream(fieldTypes)
            .map(type1 -> createConverter(type1, fieldGetterFactory))
            .collect(Collectors.toList());
    final int fieldCount = fields.size();
    List<LogData.FieldGetter<T>> fieldGetterList = new ArrayList<>();
    for (int i = 0; i < fieldCount; i++) {
      fieldGetterList.add(fieldGetterFactory.createFieldGetter(fieldTypes[i], i));
    }

    return (source, context) -> {
      ObjectNode node;
      // reuse could be a NullNode if last record is null.
      if (context.node == null || context.node.isNull()) {
        node = context.mapper.createObjectNode();
      } else {
        node = (ObjectNode) context.node;
      }
      T actualValue = (T) source;
      for (int i = 0; i < fieldCount; i++) {
        String fieldName = fieldNames[i];
        try {
          LogDataToJsonConverter.FormatConverterContext subContext =
              new LogDataToJsonConverter.FormatConverterContext(
                  context.mapper,
                  node.get(fieldName));

          Object field = fieldGetterList.get(i).getFieldOrNull(actualValue, i);
          node.set(
              fieldName,
              fieldConverterList.get(i).convert(
                  field, subContext));
        } catch (Throwable t) {
          throw new RuntimeException(
              String.format("Fail to serialize at field: %s.", fieldName), t);
        }
      }
      return node;
    };
  }

  private static <T> LogDataToJsonConverter<T> wrapIntoNullableConverter(LogDataToJsonConverter<T> converter) {
    return (source, context) -> {
      if (source == null) {
        return context.mapper.getNodeFactory().nullNode();
      }
      return converter.convert(source, context);
    };
  }
}
