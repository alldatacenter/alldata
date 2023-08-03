package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.AbsoluteTimeRangeConfig;
import com.linkedin.feathr.core.config.consumer.ObservationDataTimeSettingsConfig;
import com.linkedin.feathr.core.config.consumer.RelativeTimeRangeConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.consumer.ObservationDataTimeSettingsConfig.*;


/**
 * Builds the [[ObservationDataTimeSettingsConfig]] object
 *
 * observationDataTimeSettings: {
 *      absoluteTimeRange: {
 *        startTime: 20200809
 *        endTime: 20200810
 *        timeFormat: yyyyMMdd
 *      }
 *      (or)
 *      relativeTimeRange: {
 *       offset: 1d
 *        window: 1d
 *      }
 *    }
 * @author rkashyap
 */
public class ObservationDataTimeSettingsConfigBuilder {
  private final static Logger logger = LogManager.getLogger(ObservationDataTimeSettingsConfigBuilder.class);

  private ObservationDataTimeSettingsConfigBuilder() {
  }

  public static ObservationDataTimeSettingsConfig build(Config observationDataTimeSettings) {

    AbsoluteTimeRangeConfig absoluteTimeRangeConfig = observationDataTimeSettings.hasPath(ABSOLUTE_TIME_RANGE)
        ? AbsoluteTimeRangeConfigBuilder.build(observationDataTimeSettings.getConfig(ABSOLUTE_TIME_RANGE))
        : null;

    RelativeTimeRangeConfig relativeTimeRangeConfig = observationDataTimeSettings.hasPath(RELATIVE_TIME_RANGE)
        ? RelativeTimeRangeConfigBuilder.build(observationDataTimeSettings.getConfig(RELATIVE_TIME_RANGE))
        : null;

    if (absoluteTimeRangeConfig != null && relativeTimeRangeConfig != null) {
      throw new ConfigBuilderException(String.format("Please provide only one of the absoluteTimeRange or RelativeTimeRange. Currently, you"
          + "have provided both the configs:- AbsoluteTimeRange: %s , RelativeTimeRange: %s", absoluteTimeRangeConfig.toString(),
          relativeTimeRangeConfig.toString()));
    }

    if (absoluteTimeRangeConfig == null && relativeTimeRangeConfig == null) {
      throw new ConfigBuilderException(String.format("Please provide atleast one of absoluteTimeRange or RelativeTimeRange. If you do not"
              + "intend to filter the observation data, please remove the section observationDataTimeSettings from the settings section.",
          absoluteTimeRangeConfig.toString(), relativeTimeRangeConfig.toString()));
    }

    ObservationDataTimeSettingsConfig configObj =
        new ObservationDataTimeSettingsConfig(absoluteTimeRangeConfig, relativeTimeRangeConfig);
    logger.debug("Built Observation data time settings object");

    return configObj;
  }
}