package com.linkedin.feathr.core.config.producer.derivations;

import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.TypedExpr;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.utils.Utils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents the definition of a derived feature using keys and MVEL/SQL expression.
 *
 * @author djaising
 * @author cesun
 */
public final class DerivationConfigWithExpr implements DerivationConfig {
  private final List<String> _keys;
  private final Map<String, KeyedFeature> _inputs;
  private final TypedExpr _typedDefinition;
  private final Optional<FeatureTypeConfig> _featureTypeConfig;

  private String _configStr;

  /**
   * Constructor
   * @param keys The key of the derived feature; can be single or composite key.
   * @param inputs The parent feature(s) from whom this feature is derived. It is expressed as a java.util.Map of
   *               argument name to {@link KeyedFeature}
   * @param typedDefinition A user-defined expression which defines the derived feature using the argument names from the
   *                   inputs, together with the {@link ExprType}
   */
  public DerivationConfigWithExpr(List<String> keys, Map<String, KeyedFeature> inputs, TypedExpr typedDefinition) {
    _keys = keys;
    _inputs = inputs;
    _typedDefinition = typedDefinition;
    _featureTypeConfig = Optional.empty();
  }

  /**
   * Constructor
   * @param keys The key of the derived feature; can be single or composite key.
   * @param inputs The parent feature(s) from whom this feature is derived. It is expressed as a java.util.Map of
   *               argument name to {@link KeyedFeature}
   * @param typedDefinition A user-defined expression which defines the derived feature using the argument names from the
   *                   inputs, together with the {@link ExprType}
   */
  public DerivationConfigWithExpr(List<String> keys, Map<String, KeyedFeature> inputs, TypedExpr typedDefinition,
      FeatureTypeConfig featureTypeConfig) {
    _keys = keys;
    _inputs = inputs;
    _typedDefinition = typedDefinition;
    _featureTypeConfig = Optional.ofNullable(featureTypeConfig);
  }

  /**
   * Constructor
   * @param keys The key of the derived feature; can be single or composite key.
   * @param inputs The parent feature(s) from whom this feature is derived. It is expressed as a java.util.Map of
   *               argument name to {@link KeyedFeature}
   * @param definition A user-defined MVEL expression which defines the derived feature using the argument names from the
   *                   inputs
   * @deprecated please use {@link #DerivationConfigWithExpr(List, Map, TypedExpr)}
   */
  @Deprecated
  public DerivationConfigWithExpr(List<String> keys, Map<String, KeyedFeature> inputs, String definition) {
    _keys = keys;
    _inputs = inputs;
    _typedDefinition = new TypedExpr(definition, ExprType.MVEL);
    _featureTypeConfig = Optional.empty();
  }

  public List<String> getKeys() {
    return _keys;
  }

  public Map<String, KeyedFeature> getInputs() {
    return _inputs;
  }

  @Deprecated
  public String getDefinition() {
    return _typedDefinition.getExpr();
  }

  public TypedExpr getTypedDefinition() {
    return _typedDefinition;
  }

  public Optional<FeatureTypeConfig> getFeatureTypeConfig() {
    return _featureTypeConfig;
  }

  @Override
  public String toString() {
    if (_configStr == null) {
      StringBuilder sb = new StringBuilder();
      sb.append(KEY)
          .append(": ")
          .append(Utils.string(_keys))
          .append("\n")
          .append(INPUTS)
          .append(": ")
          .append(Utils.string(_inputs, "\n"))
          .append("\n")
          .append(DEFINITION)
          .append(": \n")
          .append(_typedDefinition.toString())
          .append("\n");
      _configStr = sb.toString();
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
    DerivationConfigWithExpr that = (DerivationConfigWithExpr) o;
    return Objects.equals(_keys, that._keys) && Objects.equals(_inputs, that._inputs) && Objects.equals(
        _typedDefinition, that._typedDefinition) && Objects.equals(_featureTypeConfig, that._featureTypeConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_keys, _inputs, _typedDefinition, _featureTypeConfig);
  }
}
