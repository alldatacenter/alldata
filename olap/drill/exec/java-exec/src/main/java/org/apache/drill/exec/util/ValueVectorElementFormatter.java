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
 */
package org.apache.drill.exec.util;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;

/**
 * This class is responsible for formatting ValueVector elements.
 * Specific format templates are taken from the options.
 * It creates and reuses the concrete formatter instances for performance purposes.
 */
public class ValueVectorElementFormatter {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ValueVectorElementFormatter.class);
  private final OptionManager options;

  private DateTimeFormatter timestampFormatter;
  private DateTimeFormatter dateFormatter;
  private DateTimeFormatter timeFormatter;

  public ValueVectorElementFormatter(OptionManager options) {
    this.options = options;
  }

  /**
   * Formats ValueVector elements in accordance with it's minor type.
   *
   * @param value ValueVector element to format
   * @param minorType the minor type of the element
   * @return the formatted value, null if failed
   */
  public String format(Object value, TypeProtos.MinorType minorType) {
    switch (minorType) {
      case TIMESTAMP:
        if (value instanceof LocalDateTime) {
          return format((LocalDateTime) value,
                        options.getString(ExecConstants.WEB_DISPLAY_FORMAT_TIMESTAMP),
                        (v, p) -> v.format(getTimestampFormatter(p)));
        }
      case DATE:
        if (value instanceof LocalDate) {
          return format((LocalDate) value,
                        options.getString(ExecConstants.WEB_DISPLAY_FORMAT_DATE),
                        (v, p) -> v.format(getDateFormatter(p)));
        }
      case TIME:
        if (value instanceof LocalTime) {
          return format((LocalTime) value,
                        options.getString(ExecConstants.WEB_DISPLAY_FORMAT_TIME),
                        (v, p) -> v.format(getTimeFormatter(p)));
        }
      case VARBINARY:
        if (value instanceof byte[]) {
          byte[] bytes = (byte[]) value;
          return org.apache.drill.common.util.DrillStringUtils.toBinaryString(bytes);
        }
      default:
        return value.toString();
    }
  }

  /**
   * Formats ValueVector elements using given function.
   *
   * @param value ValueVector element to format, casted to it's actual type.
   * @param formatPattern pattern used for the formatting. If empty then value.toString() will be returned.
   * @param formatFunction function that takes ValueVector element and format pattern as arguments and returns the formatted string.
   * @param <T> actual type of the ValueVector element.
   * @return the formatted value, null if failed
   */
  private <T> String format(T value, String formatPattern, BiFunction<T, String, String> formatFunction) {
    if (formatPattern.isEmpty()) {
      return value.toString();
    }
    String formattedValue = null;
    try {
      formattedValue = formatFunction.apply(value, formatPattern);
    } catch (Exception e) {
      logger.debug(String.format("Could not format the value '%s' with the pattern '%s': %s", value, formatPattern, e.getMessage()));
    }
    return formattedValue;
  }

  private DateTimeFormatter getTimestampFormatter(String formatPattern) {
    if (timestampFormatter == null) {
      timestampFormatter = DateTimeFormatter.ofPattern(formatPattern);
    }
    return timestampFormatter;
  }

  private DateTimeFormatter getDateFormatter(String formatPattern) {
    if (dateFormatter == null) {
      dateFormatter = DateTimeFormatter.ofPattern(formatPattern);
    }
    return dateFormatter;
  }

  private DateTimeFormatter getTimeFormatter(String formatPattern) {
    if (timeFormatter == null) {
      timeFormatter = DateTimeFormatter.ofPattern(formatPattern);
    }
    return timeFormatter;
  }
}
