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
package org.apache.ambari.server.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static Helper methods for datetime conversions
 */
public class DateUtils {

  public static final String ALLOWED_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssz";

  /**
   * Milliseconds to readable format in current server timezone
   * @param timestamp
   * @return
   */
  public static String convertToReadableTime(Long timestamp) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return dateFormat.format(new Date(timestamp));
  }

  /**
   * Convert time in given format to milliseconds
   * @return
   */
  public static Long convertToTimestamp(String time, String format) {
    SimpleDateFormat dateFormat = new SimpleDateFormat(format);
    try {
      Date date = dateFormat.parse(time);
      return date.getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Convert from supported format to Date
   * @param date
   * @return
   * @throws ParseException
   */
  public static Date convertToDate(String date) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat(ALLOWED_DATE_FORMAT);
    return sdf.parse(date);
  }

  /**
   * Convert Date to allowed format
   * @param date
   * @return
   * @throws ParseException
   */
  public static String convertDateToString(Date date) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat(ALLOWED_DATE_FORMAT);
    return sdf.format(date);
  }

  /**
   * Get difference in minutes between old date and now
   * @param oldTime
   * @return
   */
  public static Long getDateDifferenceInMinutes(Date oldTime) {
    long diff = Math.abs(oldTime.getTime() - new Date().getTime());
    return diff / (60 * 1000) % 60;
  }

  /**
   * Check if given time is in the future
   * @param time
   * @return
   */
  public static boolean isFutureTime(Date time) {
    Date now = new Date();
    return time.after(now);
  }

  /**
   * Returns a date given period before now
   *
   * @param periodString is a string indicating a time period. Example '1y2m3w4d5y'
   * means 1 year, 2 months, 3 weeks, 4 days, 5 hours.
   * @return
   */
  public static Date getDateSpecifiedTimeAgo(String periodString) {
    String pattern = "((\\d+)([hdwmy]))";
    Pattern findPattern = Pattern.compile(pattern);
    Pattern matchPattern = Pattern.compile(pattern+"+");

    Map<String, Integer> qualifierToConstant = new HashMap<String, Integer>() {{
        put("h",   Calendar.HOUR);
        put("d",    Calendar.DATE);
        put("w",   Calendar.WEEK_OF_YEAR);
        put("m",  Calendar.MONTH);
        put("y",   Calendar.YEAR);
    }};

    if(!matchPattern.matcher(periodString).matches()) {
      throw new IllegalArgumentException(String.format("Invalid string for indicating period %s", periodString));
    }

    Calendar calendar = Calendar.getInstance();
    Matcher m = findPattern.matcher(periodString);

    while (m.find()) {
      int amount = Integer.parseInt(m.group(2));
      int unit = qualifierToConstant.get(m.group(3));
      calendar.add(unit, -amount);
    }
    return calendar.getTime();
  }
}
