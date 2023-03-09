package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.JoinTimeSettingsConfig;
import com.linkedin.feathr.core.config.consumer.TimestampColumnConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.consumer.JoinTimeSettingsConfig.*;


/**
 * Builds the [[JoinTimeSettingsConfig]] class
 * joinTimeSettings: {
 *     timestampColumn: {
 *       def: timestamp
 *       format: yyyyMMdd
 *     }
 *     simulateTimeDelay: 2d
 *   }
 *
 *   (or)
 *
 *   joinTimeSettings: {
 *     useLatestFeatureData: true
 *   }
 * @author rkashyap
 */
class JoinTimeSettingsConfigBuilder {
  private final static Logger logger = LogManager.getLogger(JoinTimeSettingsConfigBuilder.class);

  private JoinTimeSettingsConfigBuilder() {
  }

  public static JoinTimeSettingsConfig build(Config joinTimSettingsConfig) {
    TimestampColumnConfig timestampColumn = joinTimSettingsConfig.hasPath(TIMESTAMP_COLUMN)
        ? TimestampColumnConfigBuilder.build(joinTimSettingsConfig.getConfig(TIMESTAMP_COLUMN))
        : null;

    Duration simulateTimeDelay = joinTimSettingsConfig.hasPath(SIMULATE_TIME_DELAY)
        ? joinTimSettingsConfig.getDuration(SIMULATE_TIME_DELAY)
        : null;

    Boolean useLatestFeatureData = joinTimSettingsConfig.hasPath(USE_LATEST_FEATURE_DATA)
        ? joinTimSettingsConfig.getBoolean(USE_LATEST_FEATURE_DATA)
        : null;

    if (timestampColumn == null && useLatestFeatureData == null) {
      StringBuilder messageBuilder = new StringBuilder();
      messageBuilder.append("One of the fields: ").append(TIMESTAMP_COLUMN).append(" or ")
          .append(USE_LATEST_FEATURE_DATA).append("is required but both are missing");
      throw new ConfigBuilderException(messageBuilder.toString());
    }

    if (useLatestFeatureData != null && useLatestFeatureData) {
      if (timestampColumn != null || simulateTimeDelay != null) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("When ").append(USE_LATEST_FEATURE_DATA).append(" is set to true, ")
            .append("None of the following fields can exist: ").append(TIMESTAMP_COLUMN)
            .append(", ").append(SIMULATE_TIME_DELAY).append(".");
        throw new ConfigBuilderException(messageBuilder.toString());
      }
    }

    JoinTimeSettingsConfig configObj =
        new JoinTimeSettingsConfig(timestampColumn, simulateTimeDelay, useLatestFeatureData);



    logger.debug("Built TimeWindowJoinConfig object");

    return configObj;
  }
}
