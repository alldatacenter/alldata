package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.EspressoConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.EspressoConfig.*;


/**
 * Builds EspressoConfig objects
 */
class EspressoConfigBuilder {
  private final static Logger logger = LogManager.getLogger(EspressoConfigBuilder.class);

  private EspressoConfigBuilder() {
  }

  public static EspressoConfig build(String sourceName, Config sourceConfig) {
    String database = sourceConfig.getString(DATABASE);
    String table = sourceConfig.getString(TABLE);
    String d2Uri = sourceConfig.getString(D2_URI);
    String keyExpr = sourceConfig.getString(KEY_EXPR);

    EspressoConfig configObj = new EspressoConfig(sourceName, database, table, d2Uri, keyExpr);
    logger.debug("Built EspressoConfig object for source " + sourceName);

    return configObj;
  }
}
