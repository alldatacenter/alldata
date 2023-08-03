package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.anchors.AnchorConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.anchors.AnchorConfig.*;


/**
 * Build a {@link AnchorConfig} object
 */
class AnchorConfigBuilder {
  private final static Logger logger = LogManager.getLogger(AnchorConfigBuilder.class);

  private AnchorConfigBuilder() {
  }

  /*
   * config represents the object part in:
   * <anchor name> : { ... }
   */
  public static AnchorConfig build(String name, Config config) {
    logger.debug("Building AnchorConfig object for anchor " + name);


    AnchorConfig anchorConfig;
    // Delegates the actual build to a child config builder
    if (config.hasPath(EXTRACTOR) || config.hasPath(TRANSFORMER)) {
      /*
       * This check should always go before config.hasPath(KEY_EXTRACTOR), or config.hasPath(KEY),
       *   as the config might contain keyExtractor field or key field
       */
      anchorConfig = AnchorConfigWithExtractorBuilder.build(name, config);
    } else if (config.hasPath(KEY_EXTRACTOR)) {
      /*
       * AnchorConfigWithKeyExtractor contains ONLY keyExtractor, without extractor,
       *   it is mutually exclusive with AnchorConfigWithExtractor
       */
      anchorConfig = AnchorConfigWithKeyExtractorBuilder.build(name, config);
    } else if (config.hasPath(KEY)) {
      /*
       *  AnchorConfigWithKey can not contain extractor field,
       *   it is mutually exclusive with AnchorConfigWithExtractor
       */
      anchorConfig = AnchorConfigWithKeyBuilder.build(name, config);
    } else {
      anchorConfig = AnchorConfigWithOnlyMvelBuilder.build(name, config);
    }

    logger.debug("Built AnchorConfig object for anchor " + name);
    return anchorConfig;
  }
}
