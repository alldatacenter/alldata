package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.FeatureBagConfig;
import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.config.consumer.SettingsConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.consumer.JoinConfig.*;
import static com.linkedin.feathr.core.utils.Utils.*;


/**
 * Builds a JoinConfig object. It does so by delegating to child builders.
 */
public class JoinConfigBuilder {
  private final static Logger logger = LogManager.getLogger(JoinConfigBuilder.class);

  private JoinConfigBuilder() {
  }

  public static JoinConfig build(Config fullConfig) {
    SettingsConfig settings = null;
    if (fullConfig.hasPath(SETTINGS)) {
      Config config = fullConfig.getConfig(SETTINGS);
      settings = SettingsConfigBuilder.build(config);
    }

    Map<String, FeatureBagConfig> featureBags = new HashMap<>();
    ConfigObject rootConfigObj = fullConfig.root();

    // Extract all feature bag names by excluding the 'settings' field name
    Set<String> featureBagNameSet = rootConfigObj.keySet().stream().filter(fbn -> !fbn.equals(SETTINGS)).collect(
        Collectors.toSet());

    // Iterate over each feature bag name to build feature bag config objects, and insert them into a map
    for (String featureBagName : featureBagNameSet) {
      List<? extends Config> featuresConfigList = fullConfig.getConfigList(quote(featureBagName));
      FeatureBagConfig featureBagConfig = FeatureBagConfigBuilder.build(featuresConfigList);
      featureBags.put(featureBagName, featureBagConfig);
    }

    /*
     * TODO: Semantic validation
     * validate that the feature names refer to valid feature names in the FeatureDef config.
     */

    JoinConfig configObj = new JoinConfig(settings, featureBags);
    logger.debug("Built JoinConfig object");

    return configObj;
  }
}
