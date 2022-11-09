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

package com.bytedance.bitsail.connector.legacy.jdbc.converter;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.DBUtilErrorCode;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created 2021/4/2
 */
public class JdbcValueConverter implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(JdbcValueConverter.class);

  private static final byte[] EMPTY_CHAR_ARRAY = new byte[0];

  protected Object extract(ResultSet rs,
                           ResultSetMetaData metaData,
                           int columnIndex,
                           int columnType,
                           String columnTypeName,
                           String columnName,
                           String encoding) throws Exception {
    switch (columnType) {
      case Types.CHAR:
      case Types.NCHAR:
      case Types.VARCHAR:
      case Types.LONGVARCHAR:
      case Types.NVARCHAR:
      case Types.LONGNVARCHAR:
        return extractStringValue(rs, columnIndex, encoding);
      case Types.CLOB:
      case Types.NCLOB:
      case Types.ROWID:
        return extractStringValue(rs, columnIndex, null);
      case Types.BIT:
      case Types.TINYINT:
        return extractShortValue(rs, columnIndex);
      case Types.SMALLINT:
      case Types.INTEGER:
        return extractIntegerValue(rs, columnIndex);
      case Types.BIGINT:
        return extractBigIntegerValue(rs, columnIndex);

      case Types.NUMERIC:
      case Types.DECIMAL:
        return extractBigDecimalValue(rs, columnIndex);

      case Types.FLOAT:
      case Types.REAL:
      case Types.DOUBLE:
        return extractDoubleValue(rs, columnIndex);

      case Types.TIME:
        return extractTimeValue(rs, columnIndex);

      case Types.DATE:
        return extractDateValue(rs, columnIndex, columnTypeName);

      case Types.TIMESTAMP:
        return extractTimestampValue(rs, columnIndex);

      case Types.BINARY:
      case Types.VARBINARY:
      case Types.BLOB:
      case Types.LONGVARBINARY:
        return extractBinaryValue(rs, columnIndex);

      case Types.BOOLEAN:
        return extractBooleanValue(rs, columnIndex);
      case Types.ARRAY:
        return rs.getArray(columnIndex);
      case Types.NULL:
      case Types.OTHER:
      case Types.STRUCT:
        return extractObjectValue(rs, columnType);
      default:
        throw BitSailException
            .asBitSailException(
                DBUtilErrorCode.UNSUPPORTED_TYPE,
                String.format(
                    "JDBC extract: The column data type in your configuration is not support. Column name:[%s], Column type name:[%s]." +
                        " Please try to change the column data type or don't transmit this column.",
                    columnName,
                    columnTypeName));

    }
  }

  public final Object convert(ResultSetMetaData metaData,
                              ResultSet rs,
                              int columnIndex,
                              String encoding) throws Exception {
    int columnType = metaData.getColumnType(columnIndex);
    String columnName = metaData.getColumnName(columnIndex);
    String columnTypeName = metaData.getColumnTypeName(columnIndex);
    Object value = extract(rs, metaData, columnIndex, columnType, columnTypeName, columnName, encoding);
    LOG.debug("value: {}", value);
    return convert(value, columnType, columnName, columnTypeName);
  }

  protected Object convert(Object value,
                           int columnType,
                           String columnName,
                           String columnTypeName) throws Exception {
    switch (columnType) {
      case Types.CHAR:
      case Types.NCHAR:
      case Types.VARCHAR:
      case Types.LONGVARCHAR:
      case Types.NVARCHAR:
      case Types.LONGNVARCHAR:
      case Types.CLOB:
      case Types.NCLOB:
      case Types.ROWID:
        return convertStringValue(value, columnName, columnTypeName);
      case Types.BIT:
      case Types.TINYINT:
      case Types.SMALLINT:
        return convertShortValue(value, columnName, columnTypeName);
      case Types.INTEGER:
        return convertLongValue(value, columnName, columnTypeName);

      case Types.BIGINT:
        return convertBigIntegerValue(value, columnName, columnTypeName);

      case Types.NUMERIC:
      case Types.DECIMAL:
        return convertBigDecimalValue(value, columnName, columnTypeName);

      case Types.FLOAT:
      case Types.REAL:
      case Types.DOUBLE:
        return convertDoubleValue(value, columnName, columnTypeName);

      case Types.TIME:
      case Types.DATE:
      case Types.TIMESTAMP:
        return convertTimeValue(value, columnName, columnTypeName);
      case Types.BINARY:
      case Types.VARBINARY:
      case Types.BLOB:
      case Types.LONGVARBINARY:
        return convertBinaryValue(value, columnName, columnTypeName);

      case Types.BOOLEAN:
        return convertBooleanValue(value, columnName, columnTypeName);

      case Types.ARRAY:
        return convertArrayValue(value, columnName, columnTypeName);
      case Types.NULL:
      case Types.OTHER:
      case Types.STRUCT:
        return convertObjectValue(value);
      default:
        throw BitSailException
            .asBitSailException(
                DBUtilErrorCode.UNSUPPORTED_TYPE,
                String.format(
                    "JDBC convert: The column data type in your configuration is not support. Column name:[%s], Column type name:[%s]." +
                        " Please try to change the column data type or don't transmit this column.",
                    columnName,
                    columnTypeName));

    }
  }

  protected String extractStringValue(ResultSet rs, int columnIndex, String encoding) throws SQLException, UnsupportedEncodingException {
    String value = null;
    if (StringUtils.isBlank(encoding)) {
      value = rs.getString(columnIndex);
    } else {
      byte[] bytes = rs.getBytes(columnIndex);
      bytes = Objects.isNull(bytes) ? EMPTY_CHAR_ARRAY
          : bytes;
      value = new String(bytes, encoding);
    }
    return value;
  }

  private Short extractShortValue(ResultSet rs, int columnIndex) throws SQLException {
    Short shortValue = rs.getShort(columnIndex);
    if (rs.wasNull()) {
      shortValue = null;
    }
    return shortValue;
  }

  private Long extractIntegerValue(ResultSet rs, int columnIndex) throws SQLException {
    Long longValue = rs.getLong(columnIndex);
    if (rs.wasNull()) {
      longValue = null;
    }
    return longValue;
  }

  private BigInteger extractBigIntegerValue(ResultSet rs, int columnIndex) throws SQLException {
    String bigIntegerValue = rs.getString(columnIndex);
    if (StringUtils.isEmpty(bigIntegerValue)) {
      return null;
    }
    return new BigDecimal(bigIntegerValue).toBigInteger();
  }

  private BigDecimal extractBigDecimalValue(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getBigDecimal(columnIndex);
  }

  protected Double extractDoubleValue(ResultSet rs, int columnIndex) throws SQLException {
    String doubleValue = rs.getString(columnIndex);
    if (StringUtils.isEmpty(doubleValue)) {
      return null;
    }
    return new BigDecimal(doubleValue).doubleValue();
  }

  private Time extractTimeValue(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getTime(columnIndex);
  }

  private Object extractDateValue(ResultSet rs, int columnIndex, String columnTypeName) throws SQLException {
    if (StringUtils.equalsIgnoreCase(columnTypeName, "year")) {
      return rs.getInt(columnIndex);
    }
    return rs.getDate(columnIndex);
  }

  protected Timestamp extractTimestampValue(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getTimestamp(columnIndex);
  }

  private byte[] extractBinaryValue(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getBytes(columnIndex);
  }

  private Boolean extractBooleanValue(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getBoolean(columnIndex);
  }

  private Object extractObjectValue(ResultSet rs, int columnIndex) throws SQLException {
    Object objectValue = rs.getObject(columnIndex);
    if (Objects.nonNull(objectValue)) {
      objectValue = objectValue.toString();
    }
    return objectValue;
  }

  protected String convertStringValue(Object value, String columnName, String columnTypeName) {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof String) {
      return (String) value;
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to String. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  private Short convertShortValue(Object value, String columnName, String columnTypeName) {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof Short) {
      return (Short) value;
    }
    if (value instanceof Number) {
      return ((Number) value).shortValue();
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to Short. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  private Long convertLongValue(Object value, String columnName, String columnTypeName) {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof Short) {
      return ((Short) value).longValue();
    }
    if (value instanceof Integer) {
      return ((Integer) value).longValue();
    }
    if (value instanceof Long) {
      return (Long) value;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to Long. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  private BigInteger convertBigIntegerValue(Object value, String columnName, String columnTypeName) {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof Long) {
      return new BigDecimal((Long) value).toBigInteger();
    }
    if (value instanceof BigInteger) {
      return (BigInteger) value;
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to BigInteger. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  private BigDecimal convertBigDecimalValue(Object value, String columnName, String columnTypeName) {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof Float) {
      return new BigDecimal((Float) value);
    }
    if (value instanceof Double) {
      return new BigDecimal((Double) value);
    }
    if (value instanceof BigDecimal) {
      return (BigDecimal) value;
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to BigDecimal. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  protected Double convertDoubleValue(Object value, String columnName, String columnTypeName) {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof Float) {
      return ((Float) value).doubleValue();
    }
    if (value instanceof Double) {
      return (Double) value;
    }
    if (value instanceof BigDecimal) {
      return ((BigDecimal) value).doubleValue();
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to Double. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  protected Object convertTimeValue(Object value, String columnName, String columnTypeName) {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof Timestamp) {
      return (Timestamp) value;
    }
    if (value instanceof Time) {
      return (Time) value;
    }
    if (value instanceof Date) {
      return (Date) value;
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to Time. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  private byte[] convertBinaryValue(Object value, String columnName, String columnTypeName) {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof byte[]) {
      return (byte[]) value;
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to byte[]. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  protected Boolean convertBooleanValue(Object value, String columnName, String columnTypeName) {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to Boolean. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  private List<Object> convertArrayValue(Object value, String columnName, String columnTypeName) throws Exception {
    if (Objects.isNull(value)) {
      return null;
    }
    if (value instanceof Array) {
      Array arrayElements = ((Array) value);
      Object[] arrays = (Object[]) arrayElements.getArray();
      List<Object> tmpArrays = Lists.newArrayList();
      for (Object element : arrays) {
        tmpArrays.add(convert(element, arrayElements.getBaseType(), columnName, arrayElements.getBaseTypeName()));
      }
      return tmpArrays;
    }
    throw new IllegalArgumentException(String
        .format("Unexpected value %s while converting to Array. Column name:[%s], Column type name:[%s]",
                value, columnName, columnTypeName));
  }

  private Object convertObjectValue(Object value) {
    if (Objects.isNull(value)) {
      return null;
    }
    return value.toString();
  }
}
