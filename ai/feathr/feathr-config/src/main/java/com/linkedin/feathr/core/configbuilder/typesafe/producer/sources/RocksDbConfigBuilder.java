package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.config.producer.sources.RocksDbConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import javax.lang.model.SourceVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.sources.RocksDbConfig.*;

/**
 * Builds {@link RocksDbConfig} objects
 */
class RocksDbConfigBuilder {
  private final static Logger logger = LogManager.getLogger(RocksDbConfigBuilder.class);

  private RocksDbConfigBuilder() {
  }

  public static RocksDbConfig build(String sourceName, Config sourceConfig) {
    String referenceSource = sourceConfig.getString(REFERENCE_SOURCE);
    Boolean extractFeatures = sourceConfig.getBoolean(EXTRACT_FEATURES);

    String encoder = getCodec(sourceConfig, ENCODER);

    String decoder = getCodec(sourceConfig, DECODER);

    String keyExpr = getCodec(sourceConfig, KEYEXPR);

    RocksDbConfig configObj = new RocksDbConfig(sourceName, referenceSource, extractFeatures, encoder, decoder, keyExpr);
    logger.debug("Built RocksDbConfig object for source" + sourceName);

    return configObj;
  }

  private static String getCodec(Config sourceConfig, String codec) {
    if (sourceConfig.hasPath(codec)) {
      String name = sourceConfig.getString(codec);
      if (SourceVersion.isName(name)) {
        return name;
      } else {
        throw new ConfigBuilderException("Invalid name for " + codec + " : " + name);
      }
    } else {
      return null;
    }
  }
}
