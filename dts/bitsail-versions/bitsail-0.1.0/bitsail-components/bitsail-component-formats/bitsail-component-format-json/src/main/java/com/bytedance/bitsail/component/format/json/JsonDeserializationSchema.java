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

package com.bytedance.bitsail.component.format.json;

import com.bytedance.bitsail.base.format.DeserializationSchema;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.common.typeinfo.BasicArrayTypeInfo;
import com.bytedance.bitsail.common.typeinfo.ListTypeInfo;
import com.bytedance.bitsail.common.typeinfo.MapTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfos;
import com.bytedance.bitsail.component.format.json.error.JsonFormatErrorCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonDeserializationSchema implements DeserializationSchema<byte[], Row> {

  private final BitSailConfiguration deserializationConfiguration;

  private final TypeInfo<?>[] typeInfos;

  private final String[] fieldNames;

  private final transient ObjectMapper objectMapper;

  private final transient DateTimeFormatter localDateTimeFormatter;
  private final transient DateTimeFormatter localDateFormatter;
  private final transient DateTimeFormatter localTimeFormatter;
  private final transient DeserializationConverter typeInfoConverter;

  public JsonDeserializationSchema(BitSailConfiguration deserializationConfiguration,
                                   TypeInfo<?>[] typeInfos,
                                   String[] fieldNames) {
    this.deserializationConfiguration = deserializationConfiguration;
    this.typeInfos = typeInfos;
    this.fieldNames = fieldNames;
    this.objectMapper = new ObjectMapper();
    this.localDateTimeFormatter = DateTimeFormatter.ofPattern(
        deserializationConfiguration.get(CommonOptions.DateFormatOptions.DATE_TIME_PATTERN));
    this.localDateFormatter = DateTimeFormatter
        .ofPattern(deserializationConfiguration.get(CommonOptions.DateFormatOptions.DATE_PATTERN));
    this.localTimeFormatter = DateTimeFormatter
        .ofPattern(deserializationConfiguration.get(CommonOptions.DateFormatOptions.TIME_PATTERN));

    this.typeInfoConverter = createConverters(typeInfos, fieldNames);
  }

  @Override
  public Row deserialize(byte[] message) {
    JsonNode jsonNode;
    try {
      jsonNode = objectMapper.readTree(message);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(JsonFormatErrorCode.JSON_FORMAT_SCHEMA_PARSE_FAILED, e);
    }

    return (Row) typeInfoConverter.convert(jsonNode);
  }

  @Override
  public boolean isEndOfStream(Row nextElement) {
    return false;
  }

  private interface DeserializationConverter extends Serializable {
    Object convert(JsonNode jsonNode);
  }

  private DeserializationConverter createConverters(TypeInfo<?>[] typeInfos,
                                                    String[] fieldNames) {
    final DeserializationConverter[] fieldConverters = Arrays.stream(typeInfos)
        .map(this::createConverter)
        .toArray(DeserializationConverter[]::new);

    return jsonNode -> {
      ObjectNode node = (ObjectNode) jsonNode;
      int arity = fieldNames.length;
      Row row = new Row(arity);
      for (int i = 0; i < arity; i++) {
        String fieldName = fieldNames[i];
        JsonNode field = node.get(fieldName);
        Object converted;
        try {
          converted = fieldConverters[i].convert(field);
        } catch (Exception e) {
          throw BitSailException.asBitSailException(JsonFormatErrorCode.JSON_FORMAT_COVERT_FAILED,
              String.format("Field %s can't convert into type %s, value = %s.",
                  fieldName,
                  typeInfos[i],
                  field));
        }
        row.setField(i, converted);
      }
      return row;
    };
  }

  private DeserializationConverter createConverter(TypeInfo<?> typeInfo) {
    return wrapperCreateConverter(typeInfo);
  }

  private DeserializationConverter wrapperCreateConverter(TypeInfo<?> typeInfo) {
    DeserializationConverter typeInfoConverter =
        createTypeInfoConverter(typeInfo);
    return (jsonNode) -> {
      if (jsonNode == null) {
        return null;
      }
      if (jsonNode.isNull()) {
        return null;
      }
      try {
        return typeInfoConverter.convert(jsonNode);
      } catch (Throwable t) {
        throw t;
      }
    };
  }

  private DeserializationConverter createTypeInfoConverter(TypeInfo<?> typeInfo) {
    Class<?> typeClass = typeInfo.getTypeClass();

    if (typeClass == TypeInfos.VOID_TYPE_INFO.getTypeClass()) {
      return jsonNode -> null;
    }
    if (typeClass == TypeInfos.BOOLEAN_TYPE_INFO.getTypeClass()) {
      return this::convertToBoolean;
    }
    if (typeClass == TypeInfos.SHORT_TYPE_INFO.getTypeClass()) {
      return this::convertToShort;
    }
    if (typeClass == TypeInfos.INT_TYPE_INFO.getTypeClass()) {
      return this::convertToInt;
    }
    if (typeClass == TypeInfos.LONG_TYPE_INFO.getTypeClass()) {
      return this::convertToLong;
    }
    if (typeClass == TypeInfos.BIG_INTEGER_TYPE_INFO.getTypeClass()) {
      return this::convertToBigInteger;
    }
    if (typeClass == TypeInfos.FLOAT_TYPE_INFO.getTypeClass()) {
      return this::convertToFloat;
    }
    if (typeClass == TypeInfos.DOUBLE_TYPE_INFO.getTypeClass()) {
      return this::convertToDouble;
    }
    if (typeClass == TypeInfos.BIG_DECIMAL_TYPE_INFO.getTypeClass()) {
      return this::createDecimalConverter;
    }
    if (typeClass == TypeInfos.STRING_TYPE_INFO.getTypeClass()) {
      return this::convertToString;
    }
    if (typeClass == TypeInfos.SQL_TIMESTAMP_TYPE_INFO.getTypeClass()) {
      return this::convertToSqlTimestamp;
    }
    if (typeClass == TypeInfos.SQL_DATE_TYPE_INFO.getTypeClass()) {
      return this::convertToSqlTime;
    }
    if (typeClass == TypeInfos.SQL_TIME_TYPE_INFO.getTypeClass()) {
      return this::convertToSqlDate;
    }
    if (typeClass == TypeInfos.LOCAL_DATE_TIME_TYPE_INFO.getTypeClass()) {
      return this::convertToTimestamp;
    }
    if (typeClass == TypeInfos.LOCAL_DATE_TYPE_INFO.getTypeClass()) {
      return this::convertToDate;
    }
    if (typeClass == TypeInfos.LOCAL_TIME_TYPE_INFO.getTypeClass()) {
      return this::convertToTime;
    }
    if (typeClass == BasicArrayTypeInfo.BINARY_TYPE_INFO.getTypeClass()) {
      return this::convertToBytes;
    }
    if (typeInfo instanceof ListTypeInfo) {
      return createListConverter((ListTypeInfo<?>) typeInfo);
    }

    if (typeInfo instanceof MapTypeInfo) {
      return createMapConverter((MapTypeInfo<?, ?>) typeInfo);
    }
    throw BitSailException.asBitSailException(JsonFormatErrorCode.JSON_FORMAT_COVERT_FAILED,
        String.format("Json format converter not support type info: %s.", typeInfo));
  }

  private Short convertToShort(JsonNode jsonNode) {
    if (jsonNode.isShort()) {
      return jsonNode.shortValue();
    }
    return Short.parseShort(jsonNode.asText().trim());
  }

  private Date convertToSqlDate(JsonNode jsonNode) {
    LocalDate localDate = convertToDate(jsonNode);
    return new Date(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
  }

  private Time convertToSqlTime(JsonNode jsonNode) {
    throw new UnsupportedOperationException();
  }

  private Timestamp convertToSqlTimestamp(JsonNode jsonNode) {
    LocalDateTime localDateTime = convertToTimestamp(jsonNode);
    return new Timestamp(localDateTime
        .atZone(ZoneOffset.systemDefault())
        .toInstant().toEpochMilli());
  }

  private boolean convertToBoolean(JsonNode jsonNode) {
    if (jsonNode.isBoolean()) {
      return jsonNode.asBoolean();
    } else {
      return Boolean.parseBoolean(jsonNode.asText().trim());
    }
  }

  private int convertToInt(JsonNode jsonNode) {
    if (jsonNode.canConvertToInt()) {
      return jsonNode.asInt();
    } else {
      return Integer.parseInt(jsonNode.asText().trim());
    }
  }

  private long convertToLong(JsonNode jsonNode) {
    if (jsonNode.canConvertToLong()) {
      return jsonNode.asLong();
    } else {
      return Long.parseLong(jsonNode.asText().trim());
    }
  }

  private BigInteger convertToBigInteger(JsonNode jsonNode) {
    if (jsonNode.isBigInteger()) {
      return jsonNode.bigIntegerValue();
    } else {
      return new BigInteger(jsonNode.asText().trim());
    }
  }

  private double convertToDouble(JsonNode jsonNode) {
    if (jsonNode.isDouble()) {
      return jsonNode.asDouble();
    } else {
      return Double.parseDouble(jsonNode.asText().trim());
    }
  }

  private float convertToFloat(JsonNode jsonNode) {
    if (jsonNode.isDouble()) {
      return (float) jsonNode.asDouble();
    } else {
      return Float.parseFloat(jsonNode.asText().trim());
    }
  }

  private LocalDate convertToDate(JsonNode jsonNode) {
    if (jsonNode.canConvertToLong()) {
      return Instant.ofEpochMilli(jsonNode.asLong())
          .atZone(ZoneOffset.systemDefault())
          .toLocalDateTime()
          .toLocalDate();
    }
    return localDateFormatter.parse(jsonNode.asText()).query(TemporalQueries.localDate());
  }

  private LocalTime convertToTime(JsonNode jsonNode) {
    if (jsonNode.canConvertToLong()) {
      return LocalTime.ofSecondOfDay(jsonNode.asLong());
    }
    return localTimeFormatter.parse(jsonNode.asText()).query(TemporalQueries.localTime());
  }

  private LocalDateTime convertToTimestamp(JsonNode jsonNode) {
    if (jsonNode.canConvertToLong()) {
      return Instant.ofEpochMilli(jsonNode.asLong())
          .atZone(ZoneOffset.systemDefault())
          .toLocalDateTime();
    }

    TemporalAccessor parse = localDateTimeFormatter.parse(jsonNode.asText());
    LocalTime localTime = parse.query(TemporalQueries.localTime());
    LocalDate localDate = parse.query(TemporalQueries.localDate());

    return LocalDateTime.of(localDate, localTime);
  }

  private String convertToString(JsonNode jsonNode) {
    if (jsonNode instanceof ContainerNode) {
      return jsonNode.toString();
    } else {
      return jsonNode.asText();
    }
  }

  private byte[] convertToBytes(JsonNode jsonNode) {
    try {
      return jsonNode.binaryValue();
    } catch (IOException e) {
      throw BitSailException.asBitSailException(JsonFormatErrorCode.JSON_FORMAT_COVERT_FAILED, e);
    }
  }

  private BigDecimal createDecimalConverter(JsonNode jsonNode) {
    BigDecimal bigDecimal;
    if (jsonNode.isBigDecimal()) {
      bigDecimal = jsonNode.decimalValue();
    } else {
      bigDecimal = new BigDecimal(jsonNode.asText());
    }
    return bigDecimal;
  }

  private DeserializationConverter createListConverter(ListTypeInfo<?> listTypeInfo) {
    DeserializationConverter elementConverter = createConverter(listTypeInfo.getElementTypeInfo());
    return jsonNode -> {
      if (!jsonNode.isArray()) {
        throw BitSailException.asBitSailException(JsonFormatErrorCode.JSON_FORMAT_COVERT_FAILED,
            "Json node is not array node.");
      }
      ArrayNode arrayNode = (ArrayNode) jsonNode;
      List<Object> converted = Lists.newArrayList();
      for (int i = 0; i < arrayNode.size(); i++) {
        final JsonNode innerNode = arrayNode.get(i);
        converted.add(elementConverter.convert(innerNode));
      }
      return converted;
    };
  }

  private DeserializationConverter createMapConverter(MapTypeInfo<?, ?> mapTypeInfo) {

    TypeInfo<?> keyTypeInfo = mapTypeInfo.getKeyTypeInfo();
    TypeInfo<?> valueTypeInfo = mapTypeInfo.getValueTypeInfo();
    final DeserializationConverter keyTypeInfoConverter = createConverter(keyTypeInfo);
    final DeserializationConverter valueTypeInfoConverter = createConverter(valueTypeInfo);

    return jsonNode -> {
      if (!jsonNode.isObject()) {
        throw BitSailException.asBitSailException(JsonFormatErrorCode.JSON_FORMAT_COVERT_FAILED,
            "Json node is not object node.");
      }
      Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
      Map<Object, Object> result = new HashMap<>();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        Object key = keyTypeInfoConverter.convert(TextNode.valueOf(entry.getKey()));
        Object value = valueTypeInfoConverter.convert(entry.getValue());
        result.put(key, value);
      }
      return result;
    };
  }
}
