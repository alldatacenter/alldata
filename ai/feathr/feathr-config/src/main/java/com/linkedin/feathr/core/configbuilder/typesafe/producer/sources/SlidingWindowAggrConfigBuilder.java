package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.SlidingWindowAggrConfig;
import com.linkedin.feathr.core.config.producer.sources.TimeWindowParams;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.SlidingWindowAggrConfig.*;
import static com.linkedin.feathr.core.config.producer.sources.TimeWindowParams.*;


/**
 * Build {@link SlidingWindowAggrConfig} object
 */
class SlidingWindowAggrConfigBuilder {
  private final static Logger logger = LogManager.getLogger(SlidingWindowAggrConfigBuilder.class);

  private final static String LEGACY_TIMESTAMP_FIELD = "timestamp";
  private final static String LEGACY_TIMESTAMP_FORMAT = "timestamp_format";

  private SlidingWindowAggrConfigBuilder() {
  }

  public static SlidingWindowAggrConfig build(Config sourceConfig) {
    Boolean isTimeSeries = sourceConfig.hasPath(IS_TIME_SERIES) && sourceConfig.getBoolean(IS_TIME_SERIES);
    Config timeWindowConfig = sourceConfig.getConfig(TIMEWINDOW_PARAMS);
    String timestampField;
    String timestampFormat;
    if (timeWindowConfig.hasPath(LEGACY_TIMESTAMP_FIELD)) {
      // TODO - 12604) we should remove the legacy fields after the users migrate to new syntax
      timestampField = timeWindowConfig.getString(LEGACY_TIMESTAMP_FIELD);
      timestampFormat = timeWindowConfig.getString(LEGACY_TIMESTAMP_FORMAT);
    } else {
      timestampField = timeWindowConfig.getString(TIMESTAMP_FIELD);
      timestampFormat = timeWindowConfig.getString(TIMESTAMP_FORMAT);
    }

    TimeWindowParams timeWindowParams = new TimeWindowParams(timestampField, timestampFormat);

    SlidingWindowAggrConfig configObj = new SlidingWindowAggrConfig(isTimeSeries, timeWindowParams);
    logger.trace("Built SlidingWindowAggrConfig object");

    return configObj;
  }
}
