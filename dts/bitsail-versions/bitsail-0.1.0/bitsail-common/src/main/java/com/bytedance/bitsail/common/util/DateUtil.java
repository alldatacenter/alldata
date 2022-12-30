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

package com.bytedance.bitsail.common.util;

import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.DateColumn;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class DateUtil {
  public static final int LENGTH_SECOND = 10;
  public static final int LENGTH_MILLISECOND = 13;
  public static final int LENGTH_MICROSECOND = 16;
  public static final int LENGTH_NANOSECOND = 19;
  private static final Logger LOG = LoggerFactory.getLogger(DateUtil.class);
  private static final String TIME_ZONE = "GMT+8";
  private static final String STANDARD_DATETIME_FORMAT = "standardDatetimeFormatter";
  private static final String STANDARD_DATETIME_FORMAT_FOR_MILLISECOND = "standardDatetimeFormatterForMillisecond";
  private static final String UN_STANDARD_DATETIME_FORMAT = "unStandardDatetimeFormatter";
  private static final String DATE_FORMAT = "dateFormatter";
  private static final String TIME_FORMAT = "timeFormatter";
  private static final String YEAR_FORMAT = "yearFormatter";
  private static final String START_TIME = "1970-01-01";
  public static Map<String, SimpleDateFormat> datetimeFormatter;

  static {
    TimeZone timeZone = TimeZone.getTimeZone(TIME_ZONE);

    Map<String, SimpleDateFormat> formatterMap = new HashMap<>();
    SimpleDateFormat standardDatetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    standardDatetimeFormatter.setTimeZone(timeZone);
    formatterMap.put(STANDARD_DATETIME_FORMAT, standardDatetimeFormatter);

    SimpleDateFormat unStandardDatetimeFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
    unStandardDatetimeFormatter.setTimeZone(timeZone);
    formatterMap.put(UN_STANDARD_DATETIME_FORMAT, unStandardDatetimeFormatter);

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
    dateFormatter.setTimeZone(timeZone);
    formatterMap.put(DATE_FORMAT, dateFormatter);

    SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
    timeFormatter.setTimeZone(timeZone);
    formatterMap.put(TIME_FORMAT, timeFormatter);

    SimpleDateFormat yearFormatter = new SimpleDateFormat("yyyy");
    yearFormatter.setTimeZone(timeZone);
    formatterMap.put(YEAR_FORMAT, yearFormatter);

    SimpleDateFormat standardDatetimeFormatterOfMillisecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    standardDatetimeFormatterOfMillisecond.setTimeZone(timeZone);
    formatterMap.put(STANDARD_DATETIME_FORMAT_FOR_MILLISECOND, standardDatetimeFormatterOfMillisecond);

    datetimeFormatter = formatterMap;
  }

  public static java.sql.Date columnToDate(Object column, SimpleDateFormat customTimeFormat, String timeZone) {
    if (column == null) {
      return null;
    } else if (column instanceof String) {
      if (((String) column).length() == 0) {
        return null;
      }

      Date date = stringToDate((String) column, customTimeFormat, timeZone);
      if (null == date) {
        return null;
      }
      return new java.sql.Date(date.getTime());
    } else if (column instanceof Integer) {
      Integer rawData = (Integer) column;
      return new java.sql.Date(getMillSecond(rawData.toString()));
    } else if (column instanceof Long) {
      Long rawData = (Long) column;
      return new java.sql.Date(getMillSecond(rawData.toString()));
    } else if (column instanceof java.sql.Date) {
      return (java.sql.Date) column;
    } else if (column instanceof Timestamp) {
      Timestamp ts = (Timestamp) column;
      return new java.sql.Date(ts.getTime());
    } else if (column instanceof Date) {
      Date d = (Date) column;
      return new java.sql.Date(d.getTime());
    }

    throw new IllegalArgumentException("Can't convert " + column.getClass().getName() + " to Date");
  }

  public static java.sql.Timestamp columnToTimestamp(Object column, SimpleDateFormat customTimeFormat,
                                                     String timeZone) {
    if (column == null) {
      return null;
    } else if (column instanceof String) {
      if (((String) column).length() == 0) {
        return null;
      }

      Date date = stringToDate((String) column, customTimeFormat, timeZone);
      if (null == date) {
        return null;
      }
      return new java.sql.Timestamp(date.getTime());
    } else if (column instanceof Integer) {
      Integer rawData = (Integer) column;
      return new java.sql.Timestamp(getMillSecond(rawData.toString()));
    } else if (column instanceof Long) {
      Long rawData = (Long) column;
      return new java.sql.Timestamp(getMillSecond(rawData.toString()));
    } else if (column instanceof java.sql.Date) {
      return new java.sql.Timestamp(((java.sql.Date) column).getTime());
    } else if (column instanceof Timestamp) {
      return (Timestamp) column;
    } else if (column instanceof Date) {
      Date d = (Date) column;
      return new java.sql.Timestamp(d.getTime());
    }

    throw new IllegalArgumentException("Can't convert " + column.getClass().getName() + " to Date");
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  public static long getMillSecond(String data) {
    long time  = Long.parseLong(data);
    if (data.length() == LENGTH_SECOND) {
      time = Long.parseLong(data) * 1000;
    } else if (data.length() == LENGTH_MILLISECOND) {
      time = Long.parseLong(data);
    } else if (data.length() == LENGTH_MICROSECOND) {
      time = Long.parseLong(data) / 1000;
    } else if (data.length() == LENGTH_NANOSECOND) {
      time = Long.parseLong(data) / 1000000;
    } else if (data.length() < LENGTH_SECOND) {
      try {
        long day = Long.parseLong(data);
        Date date = datetimeFormatter.get(DATE_FORMAT).parse(START_TIME);
        Calendar cal = Calendar.getInstance();
        long addMill = date.getTime() + day * 24 * 3600 * 1000;
        cal.setTimeInMillis(addMill);
        time = cal.getTimeInMillis();
      } catch (Exception e) {
        throw new IllegalArgumentException("Can't convert " + data + " to MillSecond, Exception: " + e.toString());
      }
    }
    return time;
  }

  /**
   * Parse date from string as following format (in order):
   * 1. yyyy-MM-dd HH:mm:ss
   * 2. yyyyMMddHHmmss
   * 3. yyyy-MM-dd
   * 4. yyyy-MM-dd
   * 5. HH:mm:ss
   * 6. yyyy
   */
  public static Date stringToDate(String strDate, SimpleDateFormat customTimeFormat, String timeZone)  {
    if (strDate == null || strDate.trim().length() == 0) {
      return null;
    }

    if (customTimeFormat != null) {
      try {
        timeZone = Strings.isNullOrEmpty(timeZone) ? TIME_ZONE : timeZone;
        LOG.debug("timeZone : " + timeZone);
        LOG.debug("Trying parsing date str by customDatePatternFormat");
        customTimeFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        return customTimeFormat.parse(strDate);
      } catch (ParseException ignored) {
        // ignored
      }
    }

    try {
      LOG.debug("Trying parsing date str by standard_datetime_format: yyyy-MM-dd HH:mm:ss");
      return datetimeFormatter.get(STANDARD_DATETIME_FORMAT).parse(strDate);
    } catch (ParseException ignored) {
      // ignored
    }

    try {
      LOG.debug("Trying parsing date str by un_standard_datetime_format: yyyyMMddHHmmss");
      return datetimeFormatter.get(UN_STANDARD_DATETIME_FORMAT).parse(strDate);
    } catch (ParseException ignored) {
      // ignored
    }

    try {
      LOG.debug("Trying parsing date str by date_format: yyyyMMdd");
      return datetimeFormatter.get(DATE_FORMAT).parse(strDate);
    } catch (ParseException ignored) {
      // ignored
    }

    try {
      LOG.debug("Trying parsing date str by time_format HH:mm:ss");
      return datetimeFormatter.get(TIME_FORMAT).parse(strDate);
    } catch (ParseException ignored) {
      // ignored
    }

    try {
      LOG.debug("Trying parsing date str by year_format: yyyy");
      return datetimeFormatter.get(YEAR_FORMAT).parse(strDate);
    } catch (ParseException ignored) {
      // ignored
    }

    throw new RuntimeException("can't parse date");
  }

  /**
   * dateString maybe timestamp, maybe date string, maybe datetime string, process all of them
   */
  @SuppressWarnings("checkstyle:MagicNumber")
  public static long convertStringToSeconds(Column column) {
    if (column instanceof DateColumn) {
      return column.asDate().getTime() / 1000;
    }

    try {
      // try parse as integer
      return getMillSecond(column.asString()) / 1000;
    } catch (Exception e) {
      return stringToDate(column.asString(), null, null).toInstant().getEpochSecond();
    }
  }
}
