package com.linkedin.feathr.core.config.consumer;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.utils.Utils;
import java.util.List;
import java.util.Objects;


/**
 * Represents list of configs for features
 */
public final class FeatureBagConfig implements ConfigObj {
  private final List<KeyedFeatures> _keyedFeatures;

  private String _configStr;

  /**
   * Constructor
   * @param keyedFeatures
   */
  public FeatureBagConfig(List<KeyedFeatures> keyedFeatures) {
    Utils.require(!keyedFeatures.isEmpty(), "List of features to be joined can't be empty");
    _keyedFeatures = keyedFeatures;

    StringBuilder sb = new StringBuilder();
    sb.append(Utils.string(keyedFeatures, "\n")).append("\n");
    _configStr = sb.toString();
  }

  @Override
  public String toString() {
    return _configStr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FeatureBagConfig)) {
      return false;
    }
    FeatureBagConfig that = (FeatureBagConfig) o;
    return Objects.equals(_keyedFeatures, that._keyedFeatures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_keyedFeatures);
  }

  public List<KeyedFeatures> getKeyedFeatures() {
    return _keyedFeatures;
  }
}
