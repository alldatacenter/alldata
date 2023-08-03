package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithKeyExtractor;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.LateralViewParams;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import java.util.Map;
import javax.lang.model.SourceVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.anchors.AnchorConfig.*;


/**
 * Builds AnchorConfig objects that have features that are extracted via a udf class (an extractor)
 */
class AnchorConfigWithKeyExtractorBuilder extends BaseAnchorConfigBuilder {
  private final static Logger logger = LogManager.getLogger(AnchorConfigWithKeyExtractorBuilder.class);

  private AnchorConfigWithKeyExtractorBuilder() {
  }

  public static AnchorConfigWithKeyExtractor build(String name, Config config) {
    String source = config.getString(SOURCE);

    String keyExtractor;
    String className = config.getString(KEY_EXTRACTOR);
    if (SourceVersion.isName(className)) {
      keyExtractor = className;
    } else {
      throw new ConfigBuilderException("Invalid class name for keyExtractor: " + className);
    }

    if (config.hasPath(KEY_ALIAS)) {
      throw new ConfigBuilderException("keyAlias and keyExtractor are mutually exclusive fields");
    }

    Map<String, FeatureConfig> features = getFeatures(config);

    /*
     * Build LateralViewParams if the anchor contains time-window features (aka sliding-window features)
     * and if the lateral view parameters have been specified in the anchor config.
     */
    LateralViewParams lateralViewParams = (hasTimeWindowFeatureConfig(features) && config.hasPath(LATERAL_VIEW_PARAMS))
        ? LateralViewParamsBuilder.build(name, config.getConfig(LATERAL_VIEW_PARAMS)) : null;

    AnchorConfigWithKeyExtractor anchorConfig = new AnchorConfigWithKeyExtractor(source, keyExtractor, features, lateralViewParams);
    logger.trace("Built AnchorConfigWithExtractor object for anchor " + name);

    return anchorConfig;
  }
}
