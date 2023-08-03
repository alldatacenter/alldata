package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.anchors.TimeWindowFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.LateralViewParams;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.anchors.LateralViewParams.LATERAL_VIEW_DEF;
import static com.linkedin.feathr.core.config.producer.anchors.LateralViewParams.LATERAL_VIEW_ITEM_ALIAS;
import static com.linkedin.feathr.core.config.producer.anchors.LateralViewParams.LATERAL_VIEW_FILTER;


/**
 * Builds {@link LateralViewParams} object that are (optionally) used with
 * {@link TimeWindowFeatureConfig} (aka sliding-window features)
 */
class LateralViewParamsBuilder {
  private final static Logger logger = LogManager.getLogger(LateralViewParamsBuilder.class);

  private LateralViewParamsBuilder() {
  }

  public static LateralViewParams build(String anchorName, Config lateralViewParamsConfig) {
    String def = lateralViewParamsConfig.getString(LATERAL_VIEW_DEF);
    String itemAlias = lateralViewParamsConfig.getString(LATERAL_VIEW_ITEM_ALIAS);
    String filter = lateralViewParamsConfig.hasPath(LATERAL_VIEW_FILTER)
        ? lateralViewParamsConfig.getString(LATERAL_VIEW_FILTER) : null;

    LateralViewParams lateralViewParams = new LateralViewParams(def, itemAlias, filter);
    logger.trace("Built LateralViewParams config object for anchor " + anchorName);

    return lateralViewParams;
  }
}
