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
package org.apache.drill.common.expression.fn;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.Map;

import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_ABR_NAME_OF_MONTH;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_DAY_OF_MONTH;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_DAY_OF_WEEK;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_DAY_OF_YEAR;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_FULL_ERA_NAME;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_FULL_NAME_OF_DAY;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_HALFDAY_AM;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_HALFDAY_PM;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_HOUR_12_NAME;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_HOUR_12_OTHER_NAME;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_HOUR_24_NAME;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_ISO_1YEAR;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_ISO_2YEAR;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_ISO_3YEAR;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_ISO_4YEAR;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_ISO_WEEK_OF_YEAR;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_MILLISECOND_OF_MINUTE_NAME;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_MINUTE_OF_HOUR_NAME;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_MONTH;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_NAME_OF_DAY;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_NAME_OF_MONTH;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_SECOND_OF_MINUTE_NAME;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_WEEK_OF_YEAR;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.POSTGRES_YEAR;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.PREFIX_FM;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.PREFIX_FX;
import static org.apache.drill.common.expression.fn.JodaDateValidator.PostgresDateTimeConstant.PREFIX_TM;

public class JodaDateValidator {

  public enum PostgresDateTimeConstant {

    // patterns for replacing
    POSTGRES_FULL_NAME_OF_DAY(true, "day"),
    POSTGRES_DAY_OF_YEAR(false, "ddd"),
    POSTGRES_DAY_OF_MONTH(false, "dd"),
    POSTGRES_DAY_OF_WEEK(false, "d"),
    POSTGRES_NAME_OF_MONTH(true, "month"),
    POSTGRES_ABR_NAME_OF_MONTH(true, "mon"),
    POSTGRES_YEAR(false, "y"),
    POSTGRES_ISO_4YEAR(false, "iyyy"),
    POSTGRES_ISO_3YEAR(false, "iyy"),
    POSTGRES_ISO_2YEAR(false, "iy"),
    POSTGRES_ISO_1YEAR(false, "i"),
    POSTGRES_FULL_ERA_NAME(false, "ee"),
    POSTGRES_NAME_OF_DAY(true, "dy"),
    POSTGRES_HOUR_12_NAME(false, "hh"),
    POSTGRES_HOUR_12_OTHER_NAME(false, "hh12"),
    POSTGRES_HOUR_24_NAME(false, "hh24"),
    POSTGRES_MINUTE_OF_HOUR_NAME(false, "mi"),
    POSTGRES_SECOND_OF_MINUTE_NAME(false, "ss"),
    POSTGRES_MILLISECOND_OF_MINUTE_NAME(false, "ms"),
    POSTGRES_WEEK_OF_YEAR(false, "ww"),
    POSTGRES_ISO_WEEK_OF_YEAR(false, "iw"),
    POSTGRES_MONTH(false, "mm"),
    POSTGRES_HALFDAY_AM(false, "am"),
    POSTGRES_HALFDAY_PM(false, "pm"),

    // pattern modifiers for deleting
    PREFIX_FM(false, "fm"),
    PREFIX_FX(false, "fx"),
    PREFIX_TM(false, "tm");

    private final boolean hasCamelCasing;
    private final String name;

    PostgresDateTimeConstant(boolean hasCamelCasing, String name) {
      this.hasCamelCasing = hasCamelCasing;
      this.name = name;
    }

    public boolean hasCamelCasing() {
      return hasCamelCasing;
    }

    public String getName() {
      return name;
    }
  }

  private static final Map<PostgresDateTimeConstant, String> postgresToJodaMap = Maps.newTreeMap(new LengthDescComparator());

  public static final String POSTGRES_ESCAPE_CHARACTER = "\"";

  // jodaTime patterns
  public static final String JODA_FULL_NAME_OF_DAY = "EEEE";
  public static final String JODA_DAY_OF_YEAR = "D";
  public static final String JODA_DAY_OF_MONTH = "d";
  public static final String JODA_DAY_OF_WEEK = "e";
  public static final String JODA_NAME_OF_MONTH = "MMMM";
  public static final String JODA_ABR_NAME_OF_MONTH = "MMM";
  public static final String JODA_YEAR = "y";
  public static final String JODA_ISO_4YEAR = "xxxx";
  public static final String JODA_ISO_3YEAR = "xxx";
  public static final String JODA_ISO_2YEAR = "xx";
  public static final String JODA_ISO_1YEAR = "x";
  public static final String JODA_FULL_ERA_NAME = "G";
  public static final String JODA_NAME_OF_DAY = "E";
  public static final String JODA_HOUR_12_NAME = "h";
  public static final String JODA_HOUR_24_NAME = "H";
  public static final String JODA_MINUTE_OF_HOUR_NAME = "m";
  public static final String JODA_SECOND_OF_MINUTE_NAME = "ss";
  public static final String JODA_MILLISECOND_OF_MINUTE_NAME = "SSS";
  public static final String JODA_WEEK_OF_YEAR = "w";
  public static final String JODA_MONTH = "MM";
  public static final String JODA_HALFDAY = "aa";
  public static final String JODA_ESCAPE_CHARACTER = "'";
  public static final String EMPTY_STRING = "";

  static {
    postgresToJodaMap.put(POSTGRES_FULL_NAME_OF_DAY, JODA_FULL_NAME_OF_DAY);
    postgresToJodaMap.put(POSTGRES_DAY_OF_YEAR, JODA_DAY_OF_YEAR);
    postgresToJodaMap.put(POSTGRES_DAY_OF_MONTH, JODA_DAY_OF_MONTH);
    postgresToJodaMap.put(POSTGRES_DAY_OF_WEEK, JODA_DAY_OF_WEEK);
    postgresToJodaMap.put(POSTGRES_NAME_OF_MONTH, JODA_NAME_OF_MONTH);
    postgresToJodaMap.put(POSTGRES_ABR_NAME_OF_MONTH, JODA_ABR_NAME_OF_MONTH);
    postgresToJodaMap.put(POSTGRES_FULL_ERA_NAME, JODA_FULL_ERA_NAME);
    postgresToJodaMap.put(POSTGRES_NAME_OF_DAY, JODA_NAME_OF_DAY);
    postgresToJodaMap.put(POSTGRES_HOUR_12_NAME, JODA_HOUR_12_NAME);
    postgresToJodaMap.put(POSTGRES_HOUR_12_OTHER_NAME, JODA_HOUR_12_NAME);
    postgresToJodaMap.put(POSTGRES_HOUR_24_NAME, JODA_HOUR_24_NAME);
    postgresToJodaMap.put(POSTGRES_MINUTE_OF_HOUR_NAME, JODA_MINUTE_OF_HOUR_NAME);
    postgresToJodaMap.put(POSTGRES_SECOND_OF_MINUTE_NAME, JODA_SECOND_OF_MINUTE_NAME);
    postgresToJodaMap.put(POSTGRES_MILLISECOND_OF_MINUTE_NAME, JODA_MILLISECOND_OF_MINUTE_NAME);
    postgresToJodaMap.put(POSTGRES_WEEK_OF_YEAR, JODA_WEEK_OF_YEAR);
    postgresToJodaMap.put(POSTGRES_MONTH, JODA_MONTH);
    postgresToJodaMap.put(POSTGRES_HALFDAY_AM, JODA_HALFDAY);
    postgresToJodaMap.put(POSTGRES_HALFDAY_PM, JODA_HALFDAY);
    postgresToJodaMap.put(POSTGRES_ISO_WEEK_OF_YEAR, JODA_WEEK_OF_YEAR);
    postgresToJodaMap.put(POSTGRES_YEAR, JODA_YEAR);
    postgresToJodaMap.put(POSTGRES_ISO_1YEAR, JODA_ISO_1YEAR);
    postgresToJodaMap.put(POSTGRES_ISO_2YEAR, JODA_ISO_2YEAR);
    postgresToJodaMap.put(POSTGRES_ISO_3YEAR, JODA_ISO_3YEAR);
    postgresToJodaMap.put(POSTGRES_ISO_4YEAR, JODA_ISO_4YEAR);
    postgresToJodaMap.put(PREFIX_FM, EMPTY_STRING);
    postgresToJodaMap.put(PREFIX_FX, EMPTY_STRING);
    postgresToJodaMap.put(PREFIX_TM, EMPTY_STRING);
  }

  /**
   * Replaces all postgres patterns from {@param pattern},
   * available in postgresToJodaMap keys to jodaTime equivalents.
   *
   * @param pattern date pattern in postgres format
   * @return date pattern with replaced patterns in joda format
   */
  public static String toJodaFormat(String pattern) {
    // replaces escape character for text delimiter
    StringBuilder builder = new StringBuilder(pattern.replaceAll(POSTGRES_ESCAPE_CHARACTER, JODA_ESCAPE_CHARACTER));

    int start = 0;    // every time search of postgres token in pattern will start from this index.
    int minPos;       // min position of the longest postgres token
    do {
      // finds first value with max length
      minPos = builder.length();
      PostgresDateTimeConstant firstMatch = null;
      for (PostgresDateTimeConstant postgresPattern : postgresToJodaMap.keySet()) {
        // keys sorted in length decreasing
        // at first search longer tokens to consider situation where some tokens are the parts of large tokens
        // example: if pattern contains a token "DDD", token "DD" would be skipped, as a part of "DDD".
        int pos;
        // some tokens can't be in upper camel casing, so we ignore them here.
        // example: DD, DDD, MM, etc.
        if (postgresPattern.hasCamelCasing()) {
          // finds postgres tokens in upper camel casing
          // example: Month, Mon, Day, Dy, etc.
          pos = builder.indexOf(StringUtils.capitalize(postgresPattern.getName()), start);
          if (pos >= 0 && pos < minPos) {
            firstMatch = postgresPattern;
            minPos = pos;
            if (minPos == start) {
              break;
            }
          }
        }
        // finds postgres tokens in lower casing
        pos = builder.indexOf(postgresPattern.getName().toLowerCase(), start);
        if (pos >= 0 && pos < minPos) {
          firstMatch = postgresPattern;
          minPos = pos;
          if (minPos == start) {
            break;
          }
        }
        // finds postgres tokens in upper casing
        pos = builder.indexOf(postgresPattern.getName().toUpperCase(), start);
        if (pos >= 0 && pos < minPos) {
          firstMatch = postgresPattern;
          minPos = pos;
          if (minPos == start) {
            break;
          }
        }
      }
      // replaces postgres token, if found and it does not escape character
      if (minPos < builder.length() && firstMatch != null) {
        String jodaToken = postgresToJodaMap.get(firstMatch);
        // checks that token is not a part of escape sequence
        if (StringUtils.countMatches(builder.subSequence(0, minPos), JODA_ESCAPE_CHARACTER) % 2 == 0) {
          int offset = minPos + firstMatch.getName().length();
          builder.replace(minPos, offset, jodaToken);
          start = minPos + jodaToken.length();
        } else {
          int endEscapeCharacter = builder.indexOf(JODA_ESCAPE_CHARACTER, minPos);
          if (endEscapeCharacter >= 0) {
            start = endEscapeCharacter;
          } else {
            break;
          }
        }
      }
    } while (minPos < builder.length());
    return builder.toString();
  }

  /**
   * Length decreasing comparator.
   * Compares PostgresDateTimeConstant names by length, if they have the same length, compares them lexicographically.
   */
  private static class LengthDescComparator implements Comparator<PostgresDateTimeConstant> {

    public int compare(PostgresDateTimeConstant o1, PostgresDateTimeConstant o2) {
      int result = o2.getName().length() - o1.getName().length();
      if (result == 0) {
        return o1.getName().compareTo(o2.getName());
      }
      return result;
    }
  }

}
