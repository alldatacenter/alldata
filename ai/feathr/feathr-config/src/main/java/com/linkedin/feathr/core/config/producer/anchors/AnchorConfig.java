package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.config.ConfigObj;
import java.util.Map;
import java.util.Objects;


/**
 * Represents the general anchor definition
 */
public abstract class AnchorConfig implements ConfigObj {

  private final String _source;
  private final Map<String, FeatureConfig> _features;

  public static final String SOURCE = "source";
  public static final String KEY = "key";
  public static final String KEY_ALIAS = "keyAlias";
  public static final String KEY_MVEL = "key.mvel";
  public static final String KEY_SQL_EXPR = "key.sqlExpr";
  public static final String KEY_EXTRACTOR = "keyExtractor";
  public static final String EXTRACTOR = "extractor";
  public static final String TRANSFORMER = "transformer";   // TODO: field is deprecated. Remove once client featureDef configs modified.
  public static final String LATERAL_VIEW_PARAMS = "lateralViewParameters";
  public static final String FEATURES = "features";

  /**
   * Constructor
   * @param source source definition
   * @param features map of feature name to {@link FeatureConfig} object
   */
  protected AnchorConfig(String source, Map<String, FeatureConfig> features) {
    _source = source;
    _features = features;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AnchorConfig)) {
      return false;
    }
    AnchorConfig that = (AnchorConfig) o;
    return Objects.equals(_source, that._source) && Objects.equals(_features, that._features);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_source, _features);
  }

  public String getSource() {
    return _source;
  }

  public Map<String, FeatureConfig> getFeatures() {
    return _features;
  }
}

