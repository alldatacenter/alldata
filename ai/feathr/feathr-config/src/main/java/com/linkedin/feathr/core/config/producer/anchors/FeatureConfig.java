package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;


/**
 *  Abstract class for the configuration of a feature in an anchor
 */
public abstract class FeatureConfig implements ConfigObj {
  public static final String DEF = "def";
  public static final String DEF_MVEL = "def.mvel";
  public static final String DEF_SQL_EXPR = "def.sqlExpr";
  public static final String TYPE = "type";
  public static final String DEFAULT = "default";
  public static final String AGGREGATION = "aggregation";
  public static final String WINDOW = "window";
  public static final String SLIDING_INTERVAL = "slidingInterval";
  public static final String FILTER = "filter";
  public static final String FILTER_MVEL = "filter.mvel";
  public static final String GROUPBY = "groupBy";
  public static final String LIMIT = "limit";
  public static final String DECAY = "decay";
  public static final String WEIGHT = "weight";
  public static final String WINDOW_PARAMETERS = "windowParameters";
  public static final String SIZE = "size";
  public static final String EMBEDDING_SIZE = "embeddingSize";
  /**
   * Parameters for the extractor
  */
  public static final String PARAMETERS = "parameters";

  public abstract Optional<String> getDefaultValue();
  public abstract Optional<FeatureTypeConfig> getFeatureTypeConfig();

  /**
   * Return parameters for the extractor.
   */
  public Map<String, String> getParameters() {
    return Collections.emptyMap();
  }
  // Note: feature definition and feature config must be "linked" together in the model layer, not here.
}
