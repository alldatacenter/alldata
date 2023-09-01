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

package org.apache.uniffle.common.util;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;

// copy from org.apache.spark.network.util.JavaUtils
public final class UnitConverter {

  private UnitConverter() {
  }

  private static final Map<String, ByteUnit> byteSuffixes =
      ImmutableMap.<String, ByteUnit>builder()
          .put("b", ByteUnit.BYTE)
          .put("k", ByteUnit.KiB)
          .put("kb", ByteUnit.KiB)
          .put("m", ByteUnit.MiB)
          .put("mb", ByteUnit.MiB)
          .put("g", ByteUnit.GiB)
          .put("gb", ByteUnit.GiB)
          .put("t", ByteUnit.TiB)
          .put("tb", ByteUnit.TiB)
          .put("p", ByteUnit.PiB)
          .put("pb", ByteUnit.PiB)
          .build();

  private static final Map<String, TimeUnit> timeSuffixes =
      ImmutableMap.<String, TimeUnit>builder()
          .put("us", TimeUnit.MICROSECONDS)
          .put("ms", TimeUnit.MILLISECONDS)
          .put("s", TimeUnit.SECONDS)
          .put("m", TimeUnit.MINUTES)
          .put("min", TimeUnit.MINUTES)
          .put("h", TimeUnit.HOURS)
          .put("d", TimeUnit.DAYS)
          .build();

  public static boolean isByteString(String str) {
    String strLower = str.toLowerCase();
    for (String key: byteSuffixes.keySet()) {
      if (strLower.contains(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Convert a passed byte string (e.g. 50b, 100kb, or 250mb) to the given. If no suffix is
   * provided, a direct conversion to the provided unit is attempted.
   */
  public static long byteStringAs(String str, ByteUnit unit) {
    String lower = str.toLowerCase(Locale.ROOT).trim();
    try {
      Matcher m = Pattern.compile("([0-9]+)([a-z]+)?").matcher(lower);
      Matcher fractionMatcher = Pattern.compile("([0-9]+\\.[0-9]+)([a-z]+)?").matcher(lower);
      if (m.matches()) {
        long val = Long.parseLong(m.group(1));
        String suffix = m.group(2);
        // Check for invalid suffixes
        ByteUnit byteUnit = unit;
        if (suffix != null) {
          byteUnit = byteSuffixes.get(suffix);
          if (byteUnit == null) {
            throw new NumberFormatException("Invalid suffix: \"" + suffix + "\"");
          }
        }
        // If suffix is valid use that, otherwise none was provided and use the default passed
        return unit.convertFrom(val, byteUnit);
      } else if (fractionMatcher.matches()) {
        throw new NumberFormatException("Fractional values are not supported. Input was: "
            + fractionMatcher.group(1));
      } else {
        throw new NumberFormatException("Failed to parse byte string: " + str);
      }
    } catch (NumberFormatException e) {
      String byteError = "Size must be specified as bytes (b), "
          + "kibibytes (k), mebibytes (m), gibibytes (g), tebibytes (t), or pebibytes(p). "
          + "E.g. 50b, 100k, or 250m.";
      throw new NumberFormatException(byteError + "\n" + e.getMessage());
    }
  }

  /**
   * Convert a passed byte string (e.g. 50b, 100k, or 250m) to bytes for
   * internal use.
   *
   * If no suffix is provided, the passed number is assumed to be in bytes.
   */
  public static long byteStringAsBytes(String str) {
    return byteStringAs(str, ByteUnit.BYTE);
  }

  /**
   * Convert a passed time string (e.g. 50s, 100ms, or 250us) to a time count in the given unit.
   * The unit is also considered the default if the given string does not specify a unit.
   */
  public static long timeStringAs(String str, TimeUnit unit) {
    String lower = str.toLowerCase(Locale.ROOT).trim();

    try {
      Matcher m = Pattern.compile("(-?[0-9]+)([a-z]+)?").matcher(lower);
      if (!m.matches()) {
        throw new NumberFormatException("Failed to parse time string: " + str);
      }

      long val = Long.parseLong(m.group(1));
      String suffix = m.group(2);

      TimeUnit timeUnit = unit;
      // Check for invalid suffixes
      if (suffix != null) {
        timeUnit = timeSuffixes.get(suffix);
        if (timeUnit == null) {
          throw new NumberFormatException("Invalid suffix: \"" + suffix + "\"");
        }
      }

      // If suffix is valid use that, otherwise none was provided and use the default passed
      return unit.convert(val, timeUnit);
    } catch (NumberFormatException e) {
      String timeError = "Time must be specified as seconds (s), "
          + "milliseconds (ms), microseconds (us), minutes (m or min), hour (h), or day (d). "
          + "E.g. 50s, 100ms, or 250us.";

      throw new NumberFormatException(timeError + "\n" + e.getMessage());
    }
  }
}
