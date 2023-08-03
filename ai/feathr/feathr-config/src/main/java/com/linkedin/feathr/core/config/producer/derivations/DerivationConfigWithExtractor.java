package com.linkedin.feathr.core.config.producer.derivations;

import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.utils.Utils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents the definition of a derived feature using a user-defined class.
 *
 * @author djaising
 * @author cesun
 */
public final class DerivationConfigWithExtractor implements DerivationConfig {
  private final List<String> _keys;
  private final List<KeyedFeature> _inputs;
  private final String _className;
  private final Optional<FeatureTypeConfig> _featureTypeConfig;

  private String _configStr;

  /**
   * Constructor
   * @param keys   The key of the derived feature; can be single or composite key.
   * @param inputs The parent feature(s) from whom this feature is derived. It is expressed as a list of {@link KeyedFeature}
   * @param className The user-defined class which implements the feature derivation logic.
   *
   */
  public DerivationConfigWithExtractor(List<String> keys, List<KeyedFeature> inputs, String className) {
    _keys = keys;
    _inputs = inputs;
    _className = className;
    _featureTypeConfig = Optional.empty();

    StringBuilder sb = new StringBuilder();
    sb.append(KEY)
        .append(": ")
        .append(Utils.string(keys))
        .append("\n")
        .append(INPUTS)
        .append(": ")
        .append(Utils.string(inputs))
        .append("\n")
        .append(CLASS)
        .append(": ")
        .append(className)
        .append("\n");
    _configStr = sb.toString();
  }

  /**
   * Constructor
   * @param keys   The key of the derived feature; can be single or composite key.
   * @param inputs The parent feature(s) from whom this feature is derived. It is expressed as a list of {@link KeyedFeature}
   * @param className The user-defined class which implements the feature derivation logic.
   *
   */
  public DerivationConfigWithExtractor(List<String> keys, List<KeyedFeature> inputs, String className,
      FeatureTypeConfig featureTypeConfig) {
    _keys = keys;
    _inputs = inputs;
    _className = className;
    _featureTypeConfig = Optional.ofNullable(featureTypeConfig);

    StringBuilder sb = new StringBuilder();
    sb.append(KEY)
        .append(": ")
        .append(Utils.string(keys))
        .append("\n")
        .append(INPUTS)
        .append(": ")
        .append(Utils.string(inputs))
        .append("\n")
        .append(CLASS)
        .append(": ")
        .append(className)
        .append("\n");
    _configStr = sb.toString();
  }

  public List<String> getKeys() {
    return _keys;
  }

  public List<KeyedFeature> getInputs() {
    return _inputs;
  }

  public String getClassName() {
    return _className;
  }

  public Optional<FeatureTypeConfig> getFeatureTypeConfig() {
    return _featureTypeConfig;
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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DerivationConfigWithExtractor that = (DerivationConfigWithExtractor) o;
    return Objects.equals(_keys, that._keys) && Objects.equals(_inputs, that._inputs) && Objects.equals(_className,
        that._className) && Objects.equals(_featureTypeConfig, that._featureTypeConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_keys, _inputs, _className, _featureTypeConfig);
  }
}
