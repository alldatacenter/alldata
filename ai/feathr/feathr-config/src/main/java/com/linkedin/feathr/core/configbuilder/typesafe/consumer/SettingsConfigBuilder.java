package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.JoinTimeSettingsConfig;
import com.linkedin.feathr.core.config.consumer.ObservationDataTimeSettingsConfig;
import com.linkedin.feathr.core.config.consumer.SettingsConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.consumer.SettingsConfig.*;


/**
 * Builds a {@link SettingsConfig} object
 */
class SettingsConfigBuilder {
  private final static Logger logger = LogManager.getLogger(SettingsConfigBuilder.class);

  private SettingsConfigBuilder() {
  }

  public static SettingsConfig build(Config settingsConfig) {
    SettingsConfig configObj;
    ObservationDataTimeSettingsConfig observationDataTimeSettingsConfig = settingsConfig.hasPath(OBSERVATION_DATA_TIME_SETTINGS)
        ? ObservationDataTimeSettingsConfigBuilder.build(settingsConfig.getConfig(OBSERVATION_DATA_TIME_SETTINGS))
        : null;

    JoinTimeSettingsConfig joinTimeSettingsConfig = settingsConfig.hasPath(JOIN_TIME_SETTINGS)
        ? JoinTimeSettingsConfigBuilder.build(settingsConfig.getConfig(JOIN_TIME_SETTINGS))
        : null;

      configObj = new SettingsConfig(observationDataTimeSettingsConfig, joinTimeSettingsConfig);

    return configObj;
  }
}
