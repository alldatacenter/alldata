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

import com.bytedance.bitsail.connector.legacy.jdbc.model.Duration;

import com.google.common.annotations.VisibleForTesting;
import lombok.NonNull;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleTypes;
import oracle.sql.INTERVALDS;
import oracle.sql.INTERVALYM;
import org.apache.commons.dbcp.DelegatingResultSet;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created 2022/10/18
 */
public class OracleValueConverter extends JdbcValueConverter {

  private static final Pattern INTERVAL_DAY_SECOND_PATTERN = Pattern.compile("([+\\-])?(\\d+) (\\d+):(\\d+):(\\d+).(\\d+)");
  private static final Pattern INTERVAL_YEAR_MONTH_PATTERN = Pattern.compile("([+\\-])?(\\d+)-(\\d+)");
  private static final int MICROS_LENGTH = 6;
  private IntervalHandlingMode intervalMode;

  public OracleValueConverter(IntervalHandlingMode mode) {
    this.intervalMode = mode;
  }

  private static OracleResultSet unwrap(ResultSet rs) {
    OracleResultSet oracleResultSet = null;
    if (rs instanceof OracleResultSet) {
      oracleResultSet = (OracleResultSet) rs;
    } else if (rs instanceof DelegatingResultSet) {
      oracleResultSet = unwrap(((DelegatingResultSet) rs).getInnermostDelegate());
    }
    return oracleResultSet;
  }

  @Override
  protected Object extract(ResultSet rs,
                           ResultSetMetaData metaData,
                           int columnIndex,
                           int columnType,
                           String columnTypeName,
                           String columnName,
                           String encoding) throws Exception {
    OracleResultSet oracleResultSet = unwrap(rs);
    if (Objects.isNull(oracleResultSet)) {
      return super.extract(rs, metaData, columnIndex, columnType, columnTypeName, columnName, encoding);
    }
    int oracleColumnType = metaData.getColumnType(columnIndex);
    switch (oracleColumnType) {
      case OracleTypes.TIMESTAMPTZ:
      case OracleTypes.TIMESTAMPLTZ:
        return oracleResultSet.getTIMESTAMP(columnIndex).timestampValue();
      case OracleTypes.INTERVALDS:
        return oracleResultSet.getINTERVALDS(columnIndex);
      case OracleTypes.INTERVALYM:
        return oracleResultSet.getINTERVALYM(columnIndex);
      case OracleTypes.BINARY_FLOAT:
      case OracleTypes.BINARY_DOUBLE:
        return extractDoubleValue(oracleResultSet, columnIndex);
      default:
        return super.extract(rs, metaData, columnIndex, columnType, columnTypeName, columnName, encoding);
    }
  }

  @Override
  protected Object convert(Object value, int columnType, String columnName, String columnTypeName) throws Exception {
    switch (columnType) {
      case OracleTypes.TIMESTAMPTZ:
      case OracleTypes.TIMESTAMPLTZ:
        return convertTimeValue(value, columnName, columnTypeName);
      case OracleTypes.INTERVALDS:
      case OracleTypes.INTERVALYM:
        return convertInterval(value);
      case OracleTypes.BINARY_FLOAT:
      case OracleTypes.BINARY_DOUBLE:
        return value;
      default:
        return super.convert(value, columnType, columnName, columnTypeName);
    }
  }

  @VisibleForTesting
  @SuppressWarnings("checkstyle:MagicNumber")
  Object convertInterval(final Object interval) {
    if (this.intervalMode == null) {
      throw new IllegalArgumentException("Fail to convert interval for oracle with null mode. value: " + interval);
    }
    final String intervalStr = interval.toString();
    if (this.intervalMode.equals(IntervalHandlingMode.STRING)) {
      return intervalStr;
    }
    // Handle IntervalHandlingMode.NUMERIC
    if (interval instanceof INTERVALDS) {
      final Matcher m = INTERVAL_DAY_SECOND_PATTERN.matcher(intervalStr);
      if (m.matches()) {
        final int sign = "-".equals(m.group(1)) ? -1 : 1;
        final Duration duration = Duration.builder()
                .days(sign * Integer.parseInt(m.group(2)))
                .hours(sign * Integer.parseInt(m.group(3)))
                .minutes(sign * Integer.parseInt(m.group(4)))
                .seconds(sign * Integer.parseInt(m.group(5)))
                .microseconds(sign * Integer.parseInt(convertNumRightToFloatingPointToMicro(m.group(6))))
                .build();
        return duration.toMicros();
      }
    } else if (interval instanceof INTERVALYM) {
      final Matcher m = INTERVAL_YEAR_MONTH_PATTERN.matcher(intervalStr);
      if (m.matches()) {
        final int sign = "-".equals(m.group(1)) ? -1 : 1;
        final Duration duration = Duration.builder()
                .years(sign * Integer.parseInt(m.group(2)))
                .months(sign * Integer.parseInt(m.group(3)))
                .build();
        return duration.toMicros();
      }
    }
    throw new IllegalArgumentException("Fail to convert interval for oracle, mode: " + this.intervalMode + " value: " + interval);
  }

  /**
   * Oracle interval convert mode:
   *   NUMERIC: Convert to numeric. Unit: ms
   *   STRING: Convert to String
   */
  public enum IntervalHandlingMode {
    NUMERIC("numeric"),
    STRING("string");
    private final String value;

    IntervalHandlingMode(String value) {
      this.value = value;
    }

    /**
     * convert mode name into logical name
     * @param value mode value, may be null
     * @return the matchinig options
     */
    public static IntervalHandlingMode parse(String value) {
      if (value == null) {
        return null;
      }
      value = value.trim();
      for (IntervalHandlingMode option : IntervalHandlingMode.values()) {
        if (option.getValue().equalsIgnoreCase(value)) {
          return option;
        }
      }
      return null;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Convert number right to floating point to micro number
   *
   * @param numRightToFloatingPoint the string to be converted into micro number
   * @return micro number string
   */
  @VisibleForTesting
  String convertNumRightToFloatingPointToMicro(@NonNull final String numRightToFloatingPoint) {
    if (numRightToFloatingPoint.length() > MICROS_LENGTH) {
      return numRightToFloatingPoint.substring(0, MICROS_LENGTH);
    }
    final StringBuilder sb = new StringBuilder(numRightToFloatingPoint);
    while (sb.length() < MICROS_LENGTH) {
      sb.append('0');
    }
    return sb.toString();
  }
}
