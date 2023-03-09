package com.linkedin.feathr.core.config.producer.derivations;

import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.utils.Utils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents the definition of a sequential join config as derivation feature
 */
public final class SequentialJoinConfig implements DerivationConfig {
  private final List<String> _keys;
  private final BaseFeatureConfig _base;
  private final KeyedFeature _expansion;
  private final String _aggregation;
  private final Optional<FeatureTypeConfig> _featureTypeConfig;

  private String _configStr;

  /**
   * Constructor
   * @param keys The key of the derived feature; can be single or composite key.
   * @param base The base feature for sequential join
   * @param expansion The expansion feature for sequential join
   * @param aggregation The aggregation type
   * @param featureTypeConfig The {@link FeatureTypeConfig} for this feature config
   */
  public SequentialJoinConfig(List<String> keys, BaseFeatureConfig base, KeyedFeature expansion, String aggregation,
      FeatureTypeConfig featureTypeConfig) {
    _keys = keys;
    _base = base;
    _expansion = expansion;
    _aggregation = aggregation;
    _featureTypeConfig = Optional.ofNullable(featureTypeConfig);
  }

  /**
   * Constructor
   * @param keys The key of the derived feature; can be single or composite key.
   * @param base The base feature for sequential join
   * @param expansion The expansion feature for sequential join
   * @param aggregation The aggregation type
   */
  public SequentialJoinConfig(List<String> keys, BaseFeatureConfig base, KeyedFeature expansion, String aggregation) {
    _keys = keys;
    _base = base;
    _expansion = expansion;
    _aggregation = aggregation;
    _featureTypeConfig = Optional.empty();
  }

  @Override
  public String toString() {
    if (_configStr == null) {
      _configStr =
          String.join("\n", String.join(": ", KEY, Utils.string(_keys)), String.join(":\n", BASE, _base.toString()),
              String.join(":\n", EXPANSION, _expansion.toString()), String.join(": ", AGGREGATION, _aggregation));
    }

    return _configStr;
  }

  public List<String> getKeys() {
    return _keys;
  }

  public BaseFeatureConfig getBase() {
    return _base;
  }

  public KeyedFeature getExpansion() {
    return _expansion;
  }

  public String getAggregation() {
    return _aggregation;
  }

  public Optional<FeatureTypeConfig> getFeatureTypeConfig() {
    return _featureTypeConfig;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SequentialJoinConfig that = (SequentialJoinConfig) o;
    return Objects.equals(_keys, that._keys) && Objects.equals(_base, that._base) && Objects.equals(_expansion,
        that._expansion) && Objects.equals(_aggregation, that._aggregation) && Objects.equals(_featureTypeConfig,
        that._featureTypeConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_keys, _base, _expansion, _aggregation, _featureTypeConfig);
  }
}
