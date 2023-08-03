package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.VeniceConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.VeniceConfig.*;

/**
 * Builds {@link VeniceConfig} objects
 */
class VeniceConfigBuilder {
  private final static Logger logger = LogManager.getLogger(VeniceConfigBuilder.class);

  private VeniceConfigBuilder() {
  }

  public static VeniceConfig build(String sourceName, Config sourceConfig) {
    String storeName = sourceConfig.getString(STORE_NAME);
    String keyExpr = sourceConfig.getString(KEY_EXPR);

    VeniceConfig configObj = new VeniceConfig(sourceName, storeName, keyExpr);
    logger.debug("Built VeniceConfig object for source " + sourceName);

    return configObj;
  }
}
