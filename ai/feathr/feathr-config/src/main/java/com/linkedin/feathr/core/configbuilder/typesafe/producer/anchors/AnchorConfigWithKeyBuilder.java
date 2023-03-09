package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.anchors.AnchorConfigWithKey;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.LateralViewParams;
import com.linkedin.feathr.core.config.producer.anchors.TypedKey;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.utils.ConfigUtils;
import com.typesafe.config.Config;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.anchors.AnchorConfig.*;


/**
 * Builds AnchorConfig objects that have features with keys
 */
class AnchorConfigWithKeyBuilder extends BaseAnchorConfigBuilder {
  private final static Logger logger = LogManager.getLogger(BaseAnchorConfigBuilder.class);

  private AnchorConfigWithKeyBuilder() {
  }

  public static AnchorConfigWithKey build(String name, Config config) {
    String source = config.getString(SOURCE);

    // key field is guaranteed to exist for AnchorConfigWithKeyBuilder
    TypedKey typedKey = TypedKeyBuilder.getInstance().build(config);

    Map<String, FeatureConfig> features = getFeatures(config);

    List<String> keyAlias = ConfigUtils.getStringList(config, KEY_ALIAS);
    if (keyAlias != null && keyAlias.size() != typedKey.getKey().size()) {
      throw new ConfigBuilderException("The size of key and keyAlias does not match");
    }
    /*
     * Build LateralViewParams if the anchor contains time-window features (aka sliding-window features)
     * and if the lateral view parameters have been specified in the anchor config.
     */
    LateralViewParams lateralViewParams = (hasTimeWindowFeatureConfig(features) && config.hasPath(LATERAL_VIEW_PARAMS))
        ? LateralViewParamsBuilder.build(name, config.getConfig(LATERAL_VIEW_PARAMS)) : null;

    AnchorConfigWithKey anchorConfig =
        new AnchorConfigWithKey(source, typedKey, keyAlias, lateralViewParams, features);
    logger.trace("Built AnchorConfigWithKey object for anchor " + name);

    return anchorConfig;
  }
}
