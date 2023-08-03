package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.TimeWindowFeatureConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.anchors.AnchorConfig.*;


abstract class BaseAnchorConfigBuilder {
  private final static Logger logger = LogManager.getLogger(BaseAnchorConfigBuilder.class);

  // Gets feature config objects by invoking the FeatureConfigBuilder appropriately
  public static Map<String, FeatureConfig> getFeatures(Config anchorConfig) {
    logger.debug("Building FeatureConfig objects in anchor " + anchorConfig);

    ConfigValue value = anchorConfig.getValue(FEATURES);
    ConfigValueType valueType = value.valueType();

    Map<String, FeatureConfig> features;
    switch (valueType) {                // Note that features can be expressed as a list or as an object
      case LIST:
        List<String> featureNames = anchorConfig.getStringList(FEATURES);
        features = FeatureConfigBuilder.build(featureNames);
        break;

      case OBJECT:
        Config featuresConfig = anchorConfig.getConfig(FEATURES);
        features = FeatureConfigBuilder.build(featuresConfig);
        break;

      default:
        throw new ConfigBuilderException("Expected " + FEATURES + " value type List or Object, got " + valueType);
    }

    return features;
  }

  /*
   * Check if the feature configs have TimeWindowFeatureConfig objects. An anchor can contain
   * time-window features or regular features but never a mix of both.
   */
  static boolean hasTimeWindowFeatureConfig(Map<String, FeatureConfig> featureConfigMap) {
    FeatureConfig featureConfig = featureConfigMap.values().iterator().next();
    return featureConfig instanceof TimeWindowFeatureConfig;
  }
}
