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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.netease.arctic.log.data.LogArrayData;
import com.netease.arctic.log.data.LogMapData;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.netease.arctic.log.TimeFormats.ISO8601_TIMESTAMP_WITH_LOCAL_TIMEZONE_FORMAT;
import static com.netease.arctic.log.TimeFormats.SQL_TIMESTAMP_FORMAT;
import static com.netease.arctic.log.TimeFormats.SQL_TIME_FORMAT;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

/**
 * Tool class used to convert from {@link JsonNode} to {@link LogData}.
 * {@link T} indicate an actual value wrapped within {@link LogData}
 */
public class JsonToLogDataConverters<T> implements Serializable {

  private static final long serialVersionUID = 647419880134661188L;
  private final boolean failOnMissingField = false;
  LogData.Factory<T> factory;
  LogArrayData.Factory arrayFactory;
  LogMapData.Factory mapFactory;

  public JsonToLogDataConverters(
      LogData.Factory<T> factory,
      LogArrayData.Factory arrayFactory,
      LogMapData.Factory mapFactory) {
    this.factory = Preconditions.checkNotNull(factory);
    this.arrayFactory = Preconditions.checkNotNull(arrayFactory);
    this.mapFactory = Preconditions.checkNotNull(mapFactory);
  }

  /**
   * Converter to convert {@link JsonNode} to log data.
   *
   * @param <T> to indicate the log data type
   */
  interface JsonToLogDataConverter<T> extends Converter<JsonNode, Object, Void, T> {
  }

  public JsonToLogDataConverter<T> createConverter(Type type) {
    return wrapIntoNullableConverter(createNotNullConverter(type));
  }

  private JsonToLogDataConverter<T> createNotNullConverter(Type type) {
    switch (type.typeId()) {
      case BOOLEAN:
        return this::convertToBoolean;
      case INTEGER:
        return this::convertToInt;
      case LONG:
        return this::convertToLong;
      case FLOAT:
        return this::convertToFloat;
      case DOUBLE:
        return this::convertToDouble;
      case DATE:
        return this::convertToDate;
      case TIME:
        // For the type: Flink only support TimeType with default precision (second) now. The precision of time is
        // not supported in Flink, so we can think of it as a simple time type directly.
        // For the data: Flink uses int that support mills to represent time data, so it supports mills precision.
        return this::convertToTime;
      case TIMESTAMP:
        Types.TimestampType timestamp = (Types.TimestampType) type;
        if (timestamp.shouldAdjustToUTC()) {
          return this::convertToTimestampWithLocalZone;
        } else {
          return this::convertToTimestamp;
        }
      case STRING:
        return this::convertToString;
      case UUID:
      case FIXED:
      case BINARY:
        return this::convertToBytes;
      case DECIMAL:
        return this::convertDecimal;
      case LIST:
        return createListConverter(type);
      case MAP:
        return createMapConverter(type);
      case STRUCT:
        return createStructConverter(type);
      default:
        throw new UnsupportedOperationException("Not Support to parse type: " + type);
    }
  }

  private JsonToLogDataConverter<T> createStructConverter(Type type) {
    final List<Types.NestedField> fields = type.asNestedType().asStructType().fields();
    final Type[] fieldTypes = fields.stream()
        .map(Types.NestedField::type)
        .toArray(Type[]::new);
    final String[] fieldNames = fields.stream()
        .map(Types.NestedField::name).toArray(String[]::new);
    final List<JsonToLogDataConverter<T>> fieldConverters =
        Arrays.stream(fieldTypes)
            .map(this::createConverter)
            .collect(Collectors.toList());

    return (jsonNode, context) -> {
      ObjectNode node = (ObjectNode) jsonNode;
      int arity = fieldNames.length;
      Object[] struct = new Object[arity];
      for (int i = 0; i < arity; i++) {
        String fieldName = fieldNames[i];
        JsonNode field = node.get(fieldName);
        Object convertedField = convertField(fieldConverters.get(i), fieldName, field, context);
        struct[i] = convertedField;
      }

      return factory.createActualValue(struct, fieldTypes);
    };
  }

  private JsonToLogDataConverter<T> createMapConverter(Type type) {
    Types.MapType map = type.asNestedType().asMapType();
    Types.NestedField keyField = map.field(map.keyId());
    Types.NestedField valueField = map.field(map.valueId());
    final JsonToLogDataConverter<T> keyConverter = createConverter(keyField.type());
    final JsonToLogDataConverter<T> valueConverter = createConverter(valueField.type());
    return (jsonNode, context) -> {
      Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
      Map<Object, Object> result = new HashMap<>();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        Object key = keyConverter.convert(TextNode.valueOf(entry.getKey()), context);
        Object value = valueConverter.convert(entry.getValue(), context);
        key = convertSecondTimeIfNecessary(keyField.type(), key);
        value = convertSecondTimeIfNecessary(valueField.type(), value);
        result.put(key, value);
      }
      return mapFactory.create(result);
    };
  }

  private JsonToLogDataConverter<T> createListConverter(Type type) {
    Types.ListType list = type.asNestedType().asListType();
    Types.NestedField elementField = list.field(list.elementId());
    JsonToLogDataConverter<T> elementConverter = createConverter(elementField.type());
    Type elementType = elementField.type();

    return (jsonNode, context) -> {
      final ArrayNode node = (ArrayNode) jsonNode;
      Object[] array = null;
      for (int i = 0; i < node.size(); i++) {
        final JsonNode innerNode = node.get(i);
        Object value = elementConverter.convert(innerNode, context);
        Object flinkValue = convertSecondTimeIfNecessary(elementType, value);
        if (flinkValue != null) {
          if (array == null) {
            Class<?> flinkValueClass = flinkValue.getClass();
            array = (Object[]) Array.newInstance(flinkValueClass, node.size());
          }
          array[i] = flinkValue;
        }
      }
      array = array == null ? new Object[node.size()] : array;
      return arrayFactory.create(array);
    };
  }

  private Object convertSecondTimeIfNecessary(Type type, Object object) {
    return factory.convertIfNecessary(type, object);
  }

  private Object convertDecimal(JsonNode jsonNode, Void context) {
    if (jsonNode.isBigDecimal()) {
      return jsonNode.decimalValue();
    } else {
      return new BigDecimal(jsonNode.asText());
    }
  }

  private Object convertToBytes(JsonNode jsonNode, Void context) {
    try {
      return jsonNode.binaryValue();
    } catch (IOException e) {
      throw new JsonParseException("Unable to deserialize byte array.", e);
    }
  }

  private Object convertToString(JsonNode jsonNode, Void context) {
    if (jsonNode.isContainerNode()) {
      return jsonNode.toString();
    } else {
      return jsonNode.asText();
    }
  }

  private Object convertToTimestampWithLocalZone(JsonNode jsonNode, Void context) {
    TemporalAccessor parsedTimestampWithLocalZone =
        ISO8601_TIMESTAMP_WITH_LOCAL_TIMEZONE_FORMAT.parse(jsonNode.asText());
    LocalTime localTime = parsedTimestampWithLocalZone.query(TemporalQueries.localTime());
    LocalDate localDate = parsedTimestampWithLocalZone.query(TemporalQueries.localDate());

    return LocalDateTime.of(localDate, localTime).toInstant(ZoneOffset.UTC);
  }

  private Object convertToTimestamp(JsonNode jsonNode, Void context) {
    TemporalAccessor parsedTimestamp;
    parsedTimestamp = SQL_TIMESTAMP_FORMAT.parse(jsonNode.asText());
    LocalTime localTime = parsedTimestamp.query(TemporalQueries.localTime());
    LocalDate localDate = parsedTimestamp.query(TemporalQueries.localDate());

    return LocalDateTime.of(localDate, localTime);
  }

  private boolean convertToBoolean(JsonNode jsonNode, Void context) {
    if (jsonNode.isBoolean()) {
      // avoid redundant toString and parseBoolean, for better performance
      return jsonNode.asBoolean();
    } else {
      return Boolean.parseBoolean(jsonNode.asText().trim());
    }
  }

  private int convertToInt(JsonNode jsonNode, Void context) {
    if (jsonNode.canConvertToInt()) {
      // avoid redundant toString and parseInt, for better performance
      return jsonNode.asInt();
    } else {
      return Integer.parseInt(jsonNode.asText().trim());
    }
  }

  private long convertToLong(JsonNode jsonNode, Void context) {
    if (jsonNode.canConvertToLong()) {
      // avoid redundant toString and parseLong, for better performance
      return jsonNode.asLong();
    } else {
      return Long.parseLong(jsonNode.asText().trim());
    }
  }

  private float convertToFloat(JsonNode jsonNode, Void context) {
    if (jsonNode.isDouble()) {
      // avoid redundant toString and parseDouble, for better performance
      return (float) jsonNode.asDouble();
    } else {
      return Float.parseFloat(jsonNode.asText().trim());
    }
  }

  private double convertToDouble(JsonNode jsonNode, Void context) {
    if (jsonNode.isDouble()) {
      // avoid redundant toString and parseDouble, for better performance
      return jsonNode.asDouble();
    } else {
      return Double.parseDouble(jsonNode.asText().trim());
    }
  }

  private int convertToDate(JsonNode jsonNode, Void context) {
    LocalDate date = ISO_LOCAL_DATE.parse(jsonNode.asText()).query(TemporalQueries.localDate());
    return (int) date.toEpochDay();
  }

  private long convertToTime(JsonNode jsonNode, Void context) {
    TemporalAccessor parsedTime = SQL_TIME_FORMAT.parse(jsonNode.asText());
    LocalTime localTime = parsedTime.query(TemporalQueries.localTime());

    // get number of nanos of the day
    return localTime.toNanoOfDay();
  }

  private Object convertField(
      JsonToLogDataConverter<T> fieldConverter, String fieldName, JsonNode field, Void context) {
    if (field == null) {
      if (failOnMissingField) {
        throw new JsonParseException("Could not find field with name '" + fieldName + "'.");
      } else {
        return null;
      }
    } else {
      return fieldConverter.convert(field, context);
    }
  }

  private static <T> JsonToLogDataConverter<T> wrapIntoNullableConverter(JsonToLogDataConverter<T> converter) {
    return (source, context) -> {
      if (source == null || source.isNull() || source.isMissingNode()) {
        return null;
      }
      return converter.convert(source, context);
    };
  }

  private static final class JsonParseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JsonParseException(String message) {
      super(message);
    }

    public JsonParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
