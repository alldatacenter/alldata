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

package com.netease.arctic.server.utils;

import javax.annotation.Nonnull;
import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Utility class for {@link Configurations} related helper functions.
 */
public class ConfigurationUtil {

  private static final String[] EMPTY = new String[0];

  // Make sure that we cannot instantiate this class
  private ConfigurationUtil() {
  }

  /**
   * Creates a new {@link Configurations} from the given {@link Properties}.
   *
   * @param properties to convert into a {@link Configurations}
   * @return {@link Configurations} which has been populated by the values of the given {@link
   * Properties}
   */
  @Nonnull
  public static Configurations createConfiguration(Properties properties) {
    final Configurations configuration = new Configurations();

    final Set<String> propertyNames = properties.stringPropertyNames();

    for (String propertyName : propertyNames) {
      configuration.setString(propertyName, properties.getProperty(propertyName));
    }

    return configuration;
  }

  @Nonnull
  public static String[] splitPaths(@Nonnull String separatedPaths) {
    return separatedPaths.length() > 0 ?
        separatedPaths.split(",|" + File.pathSeparator) : EMPTY;
  }

  // --------------------------------------------------------------------------------------------
  //  Type conversion
  // --------------------------------------------------------------------------------------------

  private static void checkConfigContains(Map<String, String> configs, String key) {
    checkArgument(
        configs.containsKey(key), "Key %s is missing present from dynamic configs.", key);
  }

  /**
   * Tries to convert the raw value into the provided type.
   *
   * @param rawValue rawValue to convert into the provided type clazz
   * @param clazz    clazz specifying the target type
   * @param <T>      type of the result
   * @return the converted value if rawValue is of type clazz
   * @throws IllegalArgumentException if the rawValue cannot be converted in the specified target
   *                                  type clazz
   */
  @SuppressWarnings("unchecked")
  public static <T> T convertValue(Object rawValue, Class<?> clazz) {
    if (Integer.class.equals(clazz)) {
      return (T) convertToInt(rawValue);
    } else if (Long.class.equals(clazz)) {
      return (T) convertToLong(rawValue);
    } else if (Boolean.class.equals(clazz)) {
      return (T) convertToBoolean(rawValue);
    } else if (Float.class.equals(clazz)) {
      return (T) convertToFloat(rawValue);
    } else if (Double.class.equals(clazz)) {
      return (T) convertToDouble(rawValue);
    } else if (String.class.equals(clazz)) {
      return (T) convertToString(rawValue);
    } else if (clazz.isEnum()) {
      return (T) convertToEnum(rawValue, (Class<? extends Enum<?>>) clazz);
    } else if (clazz == Duration.class) {
      return (T) convertToDuration(rawValue);
    } else if (clazz == Map.class) {
      return (T) convertToProperties(rawValue);
    }

    throw new IllegalArgumentException("Unsupported type: " + clazz);
  }

  @SuppressWarnings("unchecked")
  public static <T> T convertToList(Object rawValue, Class<?> atomicClass) {
    if (rawValue instanceof List) {
      return (T) rawValue;
    } else {
      return (T)
          StructuredOptionsSplitter.splitEscaped(rawValue.toString(), ';').stream()
              .map(s -> convertValue(s, atomicClass))
              .collect(Collectors.toList());
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, String> convertToProperties(Object o) {
    if (o instanceof Map) {
      return (Map<String, String>) o;
    } else {
      List<String> listOfRawProperties =
          StructuredOptionsSplitter.splitEscaped(o.toString(), ',');
      return listOfRawProperties.stream()
          .map(s -> StructuredOptionsSplitter.splitEscaped(s, ':'))
          .peek(
              pair -> {
                if (pair.size() != 2) {
                  throw new IllegalArgumentException(
                      "Could not parse pair in the map " + pair);
                }
              })
          .collect(Collectors.toMap(a -> a.get(0), a -> a.get(1)));
    }
  }

  @SuppressWarnings("unchecked")
  public static <E extends Enum<?>> E convertToEnum(Object o, Class<E> clazz) {
    if (o.getClass().equals(clazz)) {
      return (E) o;
    }

    return Arrays.stream(clazz.getEnumConstants())
        .filter(
            e ->
                e.toString()
                    .toUpperCase(Locale.ROOT)
                    .equals(o.toString().toUpperCase(Locale.ROOT)))
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "Could not parse value for enum %s. Expected one of: [%s]",
                        clazz, Arrays.toString(clazz.getEnumConstants()))));
  }

  public static Duration convertToDuration(Object o) {
    if (o.getClass() == Duration.class) {
      return (Duration) o;
    }

    return TimeUtils.parseDuration(o.toString());
  }

  public static String convertToString(Object o) {
    if (o.getClass() == String.class) {
      return (String) o;
    } else if (o.getClass() == Duration.class) {
      Duration duration = (Duration) o;
      return String.format("%d ns", duration.toNanos());
    } else if (o instanceof List) {
      return ((List<?>) o)
          .stream()
          .map(e -> StructuredOptionsSplitter.escapeWithSingleQuote(convertToString(e), ";"))
          .collect(Collectors.joining(";"));
    } else if (o instanceof Map) {
      return ((Map<?, ?>) o)
          .entrySet().stream()
          .map(
              e -> {
                String escapedKey =
                    StructuredOptionsSplitter.escapeWithSingleQuote(e.getKey().toString(), ":");
                String escapedValue =
                    StructuredOptionsSplitter.escapeWithSingleQuote(e.getValue().toString(), ":");

                return StructuredOptionsSplitter.escapeWithSingleQuote(
                    escapedKey + ":" + escapedValue, ",");
              })
          .collect(Collectors.joining(","));
    }

    return o.toString();
  }

  static Integer convertToInt(Object o) {
    if (o.getClass() == Integer.class) {
      return (Integer) o;
    } else if (o.getClass() == Long.class) {
      long value = (Long) o;
      if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
        return (int) value;
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Configuration value %s overflows/underflows the integer type.",
                value));
      }
    }

    return Integer.parseInt(o.toString());
  }

  static Long convertToLong(Object o) {
    if (o.getClass() == Long.class) {
      return (Long) o;
    } else if (o.getClass() == Integer.class) {
      return ((Integer) o).longValue();
    }

    return Long.parseLong(o.toString());
  }

  static Boolean convertToBoolean(Object o) {
    if (o.getClass() == Boolean.class) {
      return (Boolean) o;
    }

    switch (o.toString().toUpperCase()) {
      case "TRUE":
        return true;
      case "FALSE":
        return false;
      default:
        throw new IllegalArgumentException(
            String.format(
                "Unrecognized option for boolean: %s. Expected either true or false(case insensitive)",
                o));
    }
  }

  static Float convertToFloat(Object o) {
    if (o.getClass() == Float.class) {
      return (Float) o;
    } else if (o.getClass() == Double.class) {
      double value = ((Double) o);
      if (value == 0.0 ||
          (value >= Float.MIN_VALUE && value <= Float.MAX_VALUE) ||
          (value >= -Float.MAX_VALUE && value <= -Float.MIN_VALUE)) {
        return (float) value;
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Configuration value %s overflows/underflows the float type.",
                value));
      }
    }

    return Float.parseFloat(o.toString());
  }

  static Double convertToDouble(Object o) {
    if (o.getClass() == Double.class) {
      return (Double) o;
    } else if (o.getClass() == Float.class) {
      return ((Float) o).doubleValue();
    }

    return Double.parseDouble(o.toString());
  }

  /**
   * Collection of utilities about time intervals.
   */
  public static class TimeUtils {

    private static final Map<String, ChronoUnit> LABEL_TO_UNIT_MAP =
        Collections.unmodifiableMap(initMap());

    /**
     * Parse the given string to a java {@link Duration}. The string is in format "{length
     * value}{time unit label}", e.g. "123ms", "321 s". If no time unit label is specified, it will
     * be considered as milliseconds.
     *
     * <p>Supported time unit labels are:
     *
     * <ul>
     *   <li>DAYS： "d", "day"
     *   <li>HOURS： "h", "hour"
     *   <li>MINUTES： "min", "minute"
     *   <li>SECONDS： "s", "sec", "second"
     *   <li>MILLISECONDS： "ms", "milli", "millisecond"
     *   <li>MICROSECONDS： "µs", "micro", "microsecond"
     *   <li>NANOSECONDS： "ns", "nano", "nanosecond"
     * </ul>
     *
     * @param text string to parse.
     */
    public static Duration parseDuration(String text) {
      checkNotNull(text);

      final String trimmed = text.trim();
      checkArgument(!trimmed.isEmpty(), "argument is an empty- or whitespace-only string");

      final int len = trimmed.length();
      int pos = 0;

      char current;
      while (pos < len && (current = trimmed.charAt(pos)) >= '0' && current <= '9') {
        pos++;
      }

      final String number = trimmed.substring(0, pos);
      final String unitLabel = trimmed.substring(pos).trim().toLowerCase(Locale.US);

      if (number.isEmpty()) {
        throw new NumberFormatException("text does not start with a number");
      }

      final long value;
      try {
        value = Long.parseLong(number); // this throws a NumberFormatException on overflow
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "The value '" + number +
                "' cannot be re represented as 64bit number (numeric overflow).");
      }

      if (unitLabel.isEmpty()) {
        return Duration.of(value, ChronoUnit.MILLIS);
      }

      ChronoUnit unit = LABEL_TO_UNIT_MAP.get(unitLabel);
      if (unit != null) {
        return Duration.of(value, unit);
      } else {
        throw new IllegalArgumentException(
            "Time interval unit label '" + unitLabel +
                "' does not match any of the recognized units: " +
                TimeUnit.getAllUnits());
      }
    }

    private static Map<String, ChronoUnit> initMap() {
      Map<String, ChronoUnit> labelToUnit = new HashMap<>();
      for (TimeUnit timeUnit : TimeUnit.values()) {
        for (String label : timeUnit.getLabels()) {
          labelToUnit.put(label, timeUnit.getUnit());
        }
      }
      return labelToUnit;
    }

    /**
     * @param duration to convert to string
     * @return duration string in millis
     */
    public static String getStringInMillis(final Duration duration) {
      return duration.toMillis() + TimeUnit.MILLISECONDS.labels.get(0);
    }

    /**
     * Pretty prints the duration as a lowest granularity unit that does not lose precision.
     *
     * <p>Examples:
     *
     * <pre>{@code
     * Duration.ofMilliseconds(60000) will be printed as 1 min
     * Duration.ofHours(1).plusSeconds(1) will be printed as 3601 s
     * }</pre>
     *
     * <b>NOTE:</b> It supports only durations that fit into long.
     */
    public static String formatWithHighestUnit(Duration duration) {
      long nanos = duration.toNanos();

      List<TimeUnit> orderedUnits =
          Arrays.asList(
              TimeUnit.NANOSECONDS,
              TimeUnit.MICROSECONDS,
              TimeUnit.MILLISECONDS,
              TimeUnit.SECONDS,
              TimeUnit.MINUTES,
              TimeUnit.HOURS,
              TimeUnit.DAYS);

      TimeUnit highestIntegerUnit =
          IntStream.range(0, orderedUnits.size())
              .sequential()
              .filter(
                  idx ->
                      nanos % orderedUnits.get(idx).unit.getDuration().toNanos() != 0)
              .boxed()
              .findFirst()
              .map(
                  idx -> {
                    if (idx == 0) {
                      return orderedUnits.get(0);
                    } else {
                      return orderedUnits.get(idx - 1);
                    }
                  })
              .orElse(TimeUnit.MILLISECONDS);

      return String.format(
          "%d %s",
          nanos / highestIntegerUnit.unit.getDuration().toNanos(),
          highestIntegerUnit.getLabels().get(0));
    }

    private static ChronoUnit toChronoUnit(java.util.concurrent.TimeUnit timeUnit) {
      switch (timeUnit) {
        case NANOSECONDS:
          return ChronoUnit.NANOS;
        case MICROSECONDS:
          return ChronoUnit.MICROS;
        case MILLISECONDS:
          return ChronoUnit.MILLIS;
        case SECONDS:
          return ChronoUnit.SECONDS;
        case MINUTES:
          return ChronoUnit.MINUTES;
        case HOURS:
          return ChronoUnit.HOURS;
        case DAYS:
          return ChronoUnit.DAYS;
        default:
          throw new IllegalArgumentException(
              String.format("Unsupported time unit %s.", timeUnit));
      }
    }

    /**
     * Enum which defines time unit, mostly used to parse value from configuration file.
     */
    private enum TimeUnit {
      DAYS(ChronoUnit.DAYS, singular("d"), plural("day")),
      HOURS(ChronoUnit.HOURS, singular("h"), plural("hour")),
      MINUTES(ChronoUnit.MINUTES, singular("min"), plural("minute")),
      SECONDS(ChronoUnit.SECONDS, singular("s"), plural("sec"), plural("second")),
      MILLISECONDS(ChronoUnit.MILLIS, singular("ms"), plural("milli"), plural("millisecond")),
      MICROSECONDS(ChronoUnit.MICROS, singular("µs"), plural("micro"), plural("microsecond")),
      NANOSECONDS(ChronoUnit.NANOS, singular("ns"), plural("nano"), plural("nanosecond"));

      private static final String PLURAL_SUFFIX = "s";

      private final List<String> labels;

      private final ChronoUnit unit;

      TimeUnit(ChronoUnit unit, String[]... labels) {
        this.unit = unit;
        this.labels =
            Arrays.stream(labels)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
      }

      /**
       * @param label the original label
       * @return the singular format of the original label
       */
      private static String[] singular(String label) {
        return new String[]{label};
      }

      /**
       * @param label the original label
       * @return both the singular format and plural format of the original label
       */
      private static String[] plural(String label) {
        return new String[]{label, label + PLURAL_SUFFIX};
      }

      public static String getAllUnits() {
        return Arrays.stream(TimeUnit.values())
            .map(TimeUnit::createTimeUnitString)
            .collect(Collectors.joining(", "));
      }

      private static String createTimeUnitString(TimeUnit timeUnit) {
        return timeUnit.name() + ": (" + String.join(" | ", timeUnit.getLabels()) + ")";
      }

      public List<String> getLabels() {
        return labels;
      }

      public ChronoUnit getUnit() {
        return unit;
      }
    }
  }
}
