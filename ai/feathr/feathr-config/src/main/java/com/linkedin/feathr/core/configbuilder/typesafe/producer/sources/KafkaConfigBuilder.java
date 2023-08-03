package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.KafkaConfig;
import com.linkedin.feathr.core.config.producer.sources.SlidingWindowAggrConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.KafkaConfig.*;
import static com.linkedin.feathr.core.config.producer.sources.SlidingWindowAggrConfig.IS_TIME_SERIES;

/**
 * Builds {@link KafkaConfig} objects
 */
class KafkaConfigBuilder {
  private final static Logger logger = LogManager.getLogger(KafkaConfigBuilder.class);

  private KafkaConfigBuilder() {
  }

  public static KafkaConfig build(String sourceName, Config sourceConfig) {
    String stream = sourceConfig.getString(STREAM);

    // Sliding window aggregation config
    boolean isTimeSeries = sourceConfig.hasPath(IS_TIME_SERIES) && sourceConfig.getBoolean(IS_TIME_SERIES);
    SlidingWindowAggrConfig swaConfig = isTimeSeries ? SlidingWindowAggrConfigBuilder.build(sourceConfig) : null;

    KafkaConfig configObj = new KafkaConfig(sourceName, stream, swaConfig);
    logger.debug("Built KafkaConfig object for source " + sourceName);

    return configObj;
  }
}
