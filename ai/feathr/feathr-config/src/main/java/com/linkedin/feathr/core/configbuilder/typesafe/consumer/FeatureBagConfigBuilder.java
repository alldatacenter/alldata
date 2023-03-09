package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.FeatureBagConfig;
import com.linkedin.feathr.core.config.consumer.KeyedFeatures;
import com.typesafe.config.Config;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Builds FeatureBagConfig objects. These objects specify the features to be fetched.
 */
class FeatureBagConfigBuilder {
  private final static Logger logger = LogManager.getLogger(FeatureBagConfigBuilder.class);

  private FeatureBagConfigBuilder() {
  }

  public static FeatureBagConfig build(List<? extends Config> featuresConfigList) {
    List<KeyedFeatures> keyedFeatures = featuresConfigList.stream().
        map(KeyedFeaturesConfigBuilder::build).collect(Collectors.toList());

    FeatureBagConfig configObj = new FeatureBagConfig(keyedFeatures);
    logger.debug("Built FeatureBagConfig object");

    return configObj;
  }
}
