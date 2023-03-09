package com.linkedin.feathr.common.configObj.generation;

import com.linkedin.feathr.common.configObj.ConfigObj;

import java.util.List;
import java.util.Objects;


/**
 * Define the feature generation specification, i.e., list of features to generate and other settings.
 * We introduce env to differentiate between offline features.
 */

public class FeatureGenConfig implements ConfigObj {
  private final OperationalConfig _operationalConfig;
  private final List<String> _features;

  /**
   * Constructor
   * @param operationalConfig
   * @param features
   */
  public FeatureGenConfig(OperationalConfig operationalConfig, List<String> features) {
    _operationalConfig = operationalConfig;
    _features = features;
  }

  public OperationalConfig getOperationalConfig() {
    return _operationalConfig;
  }

  public List<String> getFeatures() {
    return _features;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FeatureGenConfig)) {
      return false;
    }
    FeatureGenConfig that = (FeatureGenConfig) o;
    return Objects.equals(_operationalConfig, that._operationalConfig) && Objects.equals(_features, that._features);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_operationalConfig, _features);
  }

  @Override
  public String toString() {
    return "FeatureGenConfig{" + "_operationalConfig=" + _operationalConfig + ", _features=" + _features + '}';
  }
}
