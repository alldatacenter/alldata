package com.linkedin.feathr.core.utils;

import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utils to read typesafe configs
 */
public class ConfigUtils {
  public static final String TIMESTAMP_FORMAT_EPOCH = "epoch";
  public static final String TIMESTAMP_FORMAT_EPOCH_MILLIS = "epoch_millis";

  private ConfigUtils() {

  }

  /**
   * return string config value with default
   * @param config typesafe config to read value from
   * @param path path of the config value
   * @return config value
   */
  public static String getStringWithDefault(Config config, String path, String defaultValue) {
    return config.hasPath(path) ? config.getString(path) : defaultValue;
  }

  /**
   * return int config value with default
   * @param config typesafe config to read value from
   * @param path path of the config value
   * @return config value
   */
  public static int getIntWithDefault(Config config, String path, int defaultValue) {
    return config.hasPath(path) ? config.getInt(path) : defaultValue;
  }

  /**
   * return numeric config value with default
   * @param config typesafe config to read value from
   * @param path path of the config value
   * @return config value
   */
  public static Number getNumberWithDefault(Config config, String path, Number defaultValue) {
    return config.hasPath(path) ? config.getNumber(path) : defaultValue;
  }

  /**
   * return numeric config value with default
   * @param config typesafe config to read value from
   * @param path path of the config value
   * @return config value
   */
  public static Duration getDurationWithDefault(Config config, String path, Duration defaultValue) {
    return config.hasPath(path) ? config.getDuration(path) : defaultValue;
  }


  /**
   * return long config value with default
   * @param config typesafe config to read value from
   * @param path path of the config value
   * @return config value
   */
  public static long getLongWithDefault(Config config, String path, long defaultValue) {
    return config.hasPath(path) ? config.getLong(path) : defaultValue;
  }

  /**
   * return boolean config value with default
   * @param config typesafe config to read value from
   * @param path path of the config value
   * @return config value
   */
  public static boolean getBooleanWithDefault(Config config, String path, Boolean defaultValue) {
    return config.hasPath(path) ? config.getBoolean(path) : defaultValue;
  }

  /**
   * return a String map config value where the key and value are both simple {@link String}
   * @param config the typesafe config containing the String map
   * @return the map value
   */
  public static Map<String, String> getStringMap(Config config) {
    return config.root().keySet().stream().collect(Collectors.toMap(k -> k, config::getString));
  }

  /**
   * convert ChronoUnit String to ChronoUnit enum
   * @param timeResolutionStr the timeResolution String
   * @return
   */
  public static ChronoUnit getChronoUnit(String timeResolutionStr) {
    ChronoUnit timeResolution;
    switch (timeResolutionStr.toUpperCase()) {
      case "DAILY":
        timeResolution = ChronoUnit.DAYS;
        break;
      case "HOURLY":
        timeResolution = ChronoUnit.HOURS;
        break;
      default:
        throw new RuntimeException("Unsupported time resolution unit " + timeResolutionStr);
    }
    return timeResolution;
  }

  /**
   * Check if the input timestamp pattern is valid by checking for epoch/epoch_millis and then invoking the DateTimeFormatter.
   * @param fieldName Field name where present to throw a meaningful error message
   * @param timestampPattern  The timestamp pattern string
   * @return  true if valid string, else will throw an exception
   */
  public static void validateTimestampPatternWithEpoch(String fieldName, String fieldValue, String timestampPattern) {
    if (timestampPattern.equalsIgnoreCase(TIMESTAMP_FORMAT_EPOCH) || timestampPattern.equalsIgnoreCase(TIMESTAMP_FORMAT_EPOCH_MILLIS)) {
      return;
    } else { // try
      validateTimestampPattern(fieldName, fieldValue, timestampPattern);
    }
  }

  /**
   * Check if the input timestamp pattern is valid by invoking the DateTimeFormatter.
   * @param fieldName Field name where present to throw a meaningful error message
   * @param timestampPattern  The timestamp pattern string
   * @return  true if valid string, else will throw an exception
   */
  public static void validateTimestampPattern(String fieldName, String fieldValue, String timestampPattern) {
    try {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(timestampPattern);
      LocalDate.parse(fieldValue, dateTimeFormatter);
    } catch (Throwable e) {
      throw new ConfigBuilderException(String.format("Parsing settings configuration failed for "
          + "timestamp_format=%s for field name %s.", timestampPattern, fieldName), e);
    }
  }

  /**
   * return a String list config value where the value can be either a single String or String list
   * @param config the typesafe config to read value from
   * @param path path of the config value
   * @return config value
   */
  public static List<String> getStringList(Config config, String path) {
    if (!config.hasPath(path)) {
      return null;
    }

    ConfigValueType valueType = config.getValue(path).valueType();
    List<String> valueList;
    switch (valueType) {
      case STRING:
        valueList = Collections.singletonList(config.getString(path));
        break;

      case LIST:
        valueList = config.getStringList(path);
        break;

      default:
        throw new ConfigBuilderException("Expected value type String or List, got " + valueType);
    }
    return valueList;
  }

  /**
   * Get the typesafe {@link ConfigValue#render()} with given path
   * @param config the typesafe {@Config} object to read value from
   * @param path the path
   * @return {@link String} representation for the {@link ConfigValue}, and null if the path does not exist
   */
  public static String getHoconString(Config config, String path) {
    ConfigRenderOptions renderOptions = ConfigRenderOptions.concise();
    if (!config.hasPath(path)) {
      return null;
    }
    ConfigValue configValue = config.getValue(path);

    // Warning: HOCON might automatically add comments or quote, which won't influence HOCON parser
    return configValue.render(renderOptions);
  }

}
