package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithOnlyMvel;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.typesafe.config.Config;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.anchors.AnchorConfig.*;


/**
 * Builds AnchorConfig objects that have features directly expressed as an MVEL expression without any
 * key or extractor
 */
class AnchorConfigWithOnlyMvelBuilder extends BaseAnchorConfigBuilder {
  private final static Logger logger = LogManager.getLogger(AnchorConfigWithOnlyMvelBuilder.class);

  private AnchorConfigWithOnlyMvelBuilder() {
  }

  public static AnchorConfigWithOnlyMvel build(String name, Config config) {
    String source = config.getString(SOURCE);

    Map<String, FeatureConfig> features = getFeatures(config);

    AnchorConfigWithOnlyMvel anchorConfig = new AnchorConfigWithOnlyMvel(source, features);
    logger.trace("Build AnchorConfigWithOnlyMvel object for anchor " + name);

    return anchorConfig;
  }
}
