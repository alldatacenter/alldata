package com.linkedin.feathr.core.config.generation;

import com.linkedin.feathr.core.config.ConfigObj;
import java.util.List;
import java.util.Objects;


/**
 * Define the feature generation specification, i.e., list of features to generate and other settings.
 * We introduce env to differentiate between offline and nearline features. If env is not mentioned,
 * it defaults to the offline case, and if we have parameter called env: NEARLINE, it represents a nearline feature.
 * env can also be specified as env: OFFLINE.
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

  /*
   * The previously used lombok library auto generates getters with underscore, which is used in production.
   * For backward compatibility, we need to keep these getters.
   * However, function name with underscore can not pass LinkedIn's style check, here we need suppress the style check
   *  for the getters only.
   *
   * For more detail, please refer to the style check wiki:
   * https://iwww.corp.linkedin.com/wiki/cf/display/TOOLS/Checking+Java+Coding+Style+with+Gradle+Checkstyle+Plugin
   *
   * TODO - 7493) remove the ill-named getters
   */
  // CHECKSTYLE:OFF
  @Deprecated
  public OperationalConfig get_operationalConfig() {
    return _operationalConfig;
  }

  @Deprecated
  public List<String> get_features() {
    return _features;
  }
  // CHECKSTYLE:ON

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
