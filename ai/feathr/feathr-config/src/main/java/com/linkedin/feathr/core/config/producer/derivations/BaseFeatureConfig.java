package com.linkedin.feathr.core.config.producer.derivations;

import com.linkedin.feathr.core.utils.Utils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents the definition of a base feature for sequential join config
 */
public final class BaseFeatureConfig extends KeyedFeature {
  private final Optional<List<String>> _outputKeys; // output keys after transformation
  private final Optional<String> _transformation; // logic to transform the keys of base feature to output keys
  private final Optional<String> _transformationClass; // custom base feature to output keys transformation.

  private String _configStr;

  /**
   * Constructor
   * @param rawkeyExpr the raw Key expression of the base feature
   * @param feature The feature name of the base feature
   * @param outputKeys the output keys of base feature
   * @param transformation the logic to generate outputKeys values
   */
  public BaseFeatureConfig(String rawkeyExpr, String feature, List<String> outputKeys, String transformation, String transformationClass) {
    super(rawkeyExpr, feature);
    _outputKeys = Optional.ofNullable(outputKeys);
    _transformation = Optional.ofNullable(transformation);
    _transformationClass = Optional.ofNullable(transformationClass);
  }

  @Override
  public String toString() {
    if (_configStr == null) {
      _configStr = super.toString();

      _outputKeys.ifPresent(k -> _configStr = String.join("\n",
          _configStr, String.join(": ", DerivationConfig.OUTPUT_KEY, Utils.string(k))));

      _transformation.ifPresent(t -> _configStr = String.join("\n",
          _configStr, String.join(": ", DerivationConfig.TRANSFORMATION, t)));

      _transformationClass.ifPresent(t -> _configStr = String.join("\n",
          _configStr, String.join(": ", DerivationConfig.TRANSFORMATION_CLASS, t)));
    }

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
    if (!super.equals(o)) {
      return false;
    }
    BaseFeatureConfig that = (BaseFeatureConfig) o;
    return Objects.equals(_outputKeys, that._outputKeys) && Objects.equals(_transformation, that._transformation)
        && Objects.equals(_transformationClass, that._transformationClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _outputKeys, _transformation, _transformationClass);
  }

  public Optional<List<String>> getOutputKeys() {
    return _outputKeys;
  }

  public Optional<String> getTransformation() {
    return _transformation;
  }

  public Optional<String> getTransformationClass() {
    return _transformationClass;
  }
}
