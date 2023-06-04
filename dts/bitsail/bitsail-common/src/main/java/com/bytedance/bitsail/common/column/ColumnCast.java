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

package com.bytedance.bitsail.common.column;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;

public final class ColumnCast {

  private static final Logger LOG = LoggerFactory.getLogger(ColumnCast.class);

  private static String dateTimePattern;
  private static String datePattern;
  private static String timePattern;
  private static String zoneIdContent;
  private static String encoding;

  private static DateTimeFormatter dateTimeFormatter;
  private static DateTimeFormatter dateFormatter;
  private static DateTimeFormatter timeFormatter;
  private static List<DateTimeFormatter> formatters;
  private static ZoneId dateTimeZone;
  private static volatile boolean enabled = false;

  public static void initColumnCast(BitSailConfiguration commonConfiguration) {
    if (enabled) {
      return;
    }
    if (StringUtils.isEmpty(commonConfiguration.get(CommonOptions.DateFormatOptions.TIME_ZONE))) {
      zoneIdContent = ZoneOffset.systemDefault().getId();
    } else {
      zoneIdContent = commonConfiguration.get(CommonOptions.DateFormatOptions.TIME_ZONE);
    }
    dateTimeZone = ZoneId.of(zoneIdContent);
    dateTimePattern = commonConfiguration.get(CommonOptions.DateFormatOptions
        .DATE_TIME_PATTERN);
    datePattern = commonConfiguration.get(CommonOptions.DateFormatOptions
        .DATE_PATTERN);
    timePattern = commonConfiguration.get(CommonOptions.DateFormatOptions
        .TIME_PATTERN);
    encoding = commonConfiguration.get(CommonOptions.DateFormatOptions.COLUMN_ENCODING);

    formatters = Lists.newArrayList();
    dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
    dateFormatter = DateTimeFormatter.ofPattern(datePattern);
    timeFormatter = DateTimeFormatter.ofPattern(timePattern);
    commonConfiguration.get(CommonOptions.DateFormatOptions.EXTRA_FORMATS)
        .forEach(pattern -> formatters.add(DateTimeFormatter.ofPattern(pattern)));
    formatters.add(dateTimeFormatter);
    formatters.add(dateFormatter);
    enabled = true;
  }

  public static Date string2Date(StringColumn column) {
    checkState();
    if (null == column.asString()) {
      return null;
    }
    String dateStr = column.asString();

    for (DateTimeFormatter formatter : formatters) {
      try {
        TemporalAccessor parse = formatter.parse(dateStr);
        LocalDateTime localDateTime = null;
        LocalDate localDate = LocalDate.from(parse);
        if (parse.isSupported(ChronoField.HOUR_OF_DAY)
            || parse.isSupported(ChronoField.HOUR_OF_DAY)
            || parse.isSupported(ChronoField.MINUTE_OF_HOUR)
            || parse.isSupported(ChronoField.SECOND_OF_MINUTE)
            || parse.isSupported(ChronoField.MICRO_OF_SECOND)) {
          localDateTime = LocalDateTime.of(localDate, LocalTime.from(parse));
        } else {
          localDateTime = localDate.atStartOfDay();
        }
        return Date.from(localDateTime.atZone(dateTimeZone).toInstant());
      } catch (Exception e) {
        LOG.debug("Formatter = {} parse string {} failed.", formatter, dateStr, e);
        //ignore
      }
    }
    throw new IllegalArgumentException(String.format("String [%s] can't be parse by all formatter.", dateStr));
  }

  public static String date2String(final DateColumn column) {
    checkState();
    if (null == column.asDate()) {
      return null;
    }
    Date date = column.asDate();
    OffsetDateTime offsetDateTime = Instant.ofEpochMilli(date.toInstant().toEpochMilli())
        .atZone(dateTimeZone).toOffsetDateTime();

    switch (column.getSubType()) {
      case DATE:
        return dateFormatter.format(offsetDateTime);
      case TIME:
        return timeFormatter.format(offsetDateTime);
      case DATETIME:
        return dateTimeFormatter.format(offsetDateTime);
      default:
        throw BitSailException
            .asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, "");
    }
  }

  public static String bytes2String(final BytesColumn column) throws UnsupportedEncodingException {
    checkState();
    if (null == column.asBytes()) {
      return null;
    }

    return new String(column.asBytes(), encoding);

  }

  public static void refresh() {
    enabled = false;
  }

  private static void checkState() {
    Preconditions.checkState(enabled, "Column cast in disabled from now.");
  }
}
