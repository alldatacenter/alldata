package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.CustomSourceConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.CustomSourceConfig.*;

/**
 * Builds {@link CustomSourceConfig} objects
 */
class CustomSourceConfigBuilder {
  private final static Logger logger = LogManager.getLogger(CustomSourceConfigBuilder.class);

  private CustomSourceConfigBuilder() {
  }

  public static CustomSourceConfig build(String sourceName, Config sourceConfig) {
    String keyExpr = sourceConfig.getString(KEY_EXPR);
    String dataModel = sourceConfig.getString(DATA_MODEL);

    CustomSourceConfig configObj = new CustomSourceConfig(sourceName, keyExpr, dataModel);
    logger.debug("Built CustomSourceConfig object for source " + sourceName);

    return configObj;
  }
}
