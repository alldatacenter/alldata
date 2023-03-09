package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.DateTimeRange;
import com.linkedin.feathr.core.config.consumer.KeyedFeatures;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.utils.Utils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueType;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.consumer.KeyedFeatures.*;


/**
 * Builds the KeyedFeatures config object
 */
class KeyedFeaturesConfigBuilder {
  private final static Logger logger = LogManager.getLogger(KeyedFeaturesConfigBuilder.class);

  private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

  private KeyedFeaturesConfigBuilder() {
  }

  public static KeyedFeatures build(Config featuresConfig) {
    List<String> key = getKey(featuresConfig);

    List<String> features = featuresConfig.getStringList(FEATURE_LIST);

    DateTimeRange dates = getDates(featuresConfig);

    Duration overrideTimeDelay = featuresConfig.hasPath(OVERRIDE_TIME_DELAY)
        ? featuresConfig.getDuration(OVERRIDE_TIME_DELAY)
        : null;

    return new KeyedFeatures(key, features, dates, overrideTimeDelay);
  }

  private static List<String> getKey(Config config) {
    ConfigValueType keyValueType = config.getValue(KEY).valueType();
    switch (keyValueType) {
      case STRING:
        return Collections.singletonList(config.getString(KEY));

      case LIST:
        return config.getStringList(KEY);

      default:
        throw new ConfigBuilderException("Expected key type String or List[String], got " + keyValueType);
    }
  }

  private static DateTimeRange getDates(Config config) {
    DateTimeRange dateTimeParams;

    if (config.hasPath(START_DATE)) {
      String startDateStr = config.getString(START_DATE);
      String endDateStr = config.getString(END_DATE);

      LocalDateTime startDate = LocalDate.parse(startDateStr, dateTimeFormatter).atStartOfDay();
      LocalDateTime endDate = LocalDate.parse(endDateStr, dateTimeFormatter).atStartOfDay();

      dateTimeParams = new DateTimeRange(startDate, endDate);
    } else if (config.hasPath(DATE_OFFSET)) {
      int dateOffset = config.getInt(DATE_OFFSET);
      int numDays = config.getInt(NUM_DAYS);

      // TODO: This will be checked during validation phase; we can remove it when implemented
      String messageStr = String.format("Expected %s > 0 && %s > 0 && %s < %s; got %s = %d, %s = %d",
          DATE_OFFSET, NUM_DAYS, NUM_DAYS, DATE_OFFSET, DATE_OFFSET, dateOffset, NUM_DAYS, numDays);
      Utils.require(numDays > 0 && numDays < dateOffset, messageStr);

      LocalDateTime startDate = LocalDate.now().minusDays(dateOffset).atStartOfDay();
      LocalDateTime endDate = startDate.plusDays(numDays);

      dateTimeParams = new DateTimeRange(startDate, endDate);
    } else {
      dateTimeParams = null;
    }
    return dateTimeParams;
  }
}
