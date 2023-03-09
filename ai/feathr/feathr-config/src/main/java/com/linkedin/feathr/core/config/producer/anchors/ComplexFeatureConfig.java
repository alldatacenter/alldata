package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.config.producer.definitions.FeatureType;
import java.util.Objects;
import java.util.Optional;


/**
 *
 * Represents an expression based feature configuration by specifying the object part in the following fragment:
 * <pre>
 * {@code
 *   <feature name>: {
 *     def: <feature expression>
 *     type: <feature type>
 *     default: <default value>
 *   }
 * }
 * </pre>
 *
 * <pre>
 * {@code
 *   <feature name>: {
 *     def.sqlExpr: <feature expression>
 *     type: <feature type>
 *     default: <default value>
 *   }
 * }
 * </pre>
 */
// TODO - 17615): Rename this to ExpressionBasedFeatureConfigs
// This class is still used by Galene. We should renamed it in next major version bump.
public final class ComplexFeatureConfig extends FeatureConfig {
  private final String _featureExpr;
  private final ExprType _exprType;
  private final Optional<String> _defaultValue;
  private final Optional<FeatureTypeConfig> _featureTypeConfig;

  private String _configStr;

  /**
   * Constructor with full parameters
   * @param featureExpr An expression for the feature
   * @param exprType expression type of {@link ExprType}
   * @param defaultValue A default value for the feature
   * @param featureTypeConfig A detailed feature type information for the feature
   */
  public ComplexFeatureConfig(String featureExpr, ExprType exprType, String defaultValue,
      FeatureTypeConfig featureTypeConfig) {
    _featureExpr = featureExpr;
    _exprType = exprType;
    _defaultValue = Optional.ofNullable(defaultValue);
    _featureTypeConfig = Optional.ofNullable(featureTypeConfig);

    constructConfigStr();
  }

  /**
   * Constructor
   * @deprecated use {@link #ComplexFeatureConfig(String, ExprType, String, FeatureTypeConfig)} instead
   * @param featureExpr An MVEL expression for the feature
   * @param featureType The type of the feature
   * @param defaultValue A default value for the feature
   */
  @Deprecated
  public ComplexFeatureConfig(String featureExpr, String featureType, String defaultValue) {
    this(featureExpr, defaultValue, new FeatureTypeConfig(FeatureType.valueOf(featureType)));
  }

  /**
   * Constructor
   * @deprecated use {@link #ComplexFeatureConfig(String, ExprType, String, FeatureTypeConfig)} instead
   * @param featureExpr An MVEL expression for the feature
   * @param featureTypeConfig A detailed feature type information for the feature
   */
  @Deprecated
  public ComplexFeatureConfig(String featureExpr, FeatureTypeConfig featureTypeConfig) {
    this(featureExpr, null, featureTypeConfig);
  }

  /**
   * Constructor
   * @deprecated use {@link #ComplexFeatureConfig(String, ExprType, String, FeatureTypeConfig)} instead
   * @param featureExpr An MVEL expression for the feature
   * @param defaultValue A default value for the feature
   * @param featureTypeConfig A detailed feature type information for the feature
   */
  @Deprecated
  public ComplexFeatureConfig(String featureExpr, String defaultValue, FeatureTypeConfig featureTypeConfig) {
    this(featureExpr, ExprType.MVEL, defaultValue, featureTypeConfig);
  }

  /**
   * Constructor
   * @deprecated use {@link #ComplexFeatureConfig(String, ExprType, String, FeatureTypeConfig)} instead
   * @param featureExpr An MVEL expression for the feature
   * @param exprType expression type of {@link ExprType}
   * @param featureType The type of the feature
   * @param defaultValue A default value for the feature
   */
  @Deprecated
  public ComplexFeatureConfig(String featureExpr, ExprType exprType, FeatureType featureType, String defaultValue) {
    this(featureExpr, exprType, defaultValue, featureType == null ? null : new FeatureTypeConfig(featureType));
  }

  public String getFeatureExpr() {
    return _featureExpr;
  }

  public ExprType getExprType() {
    return _exprType;
  }

  /**
   * @deprecated Please use {@link #getFeatureTypeConfig()}
   */
  // TODO - 10369) Remove getFeatureType API in favor of getFeatureTypeConfig()
  @Deprecated
  public Optional<String> getFeatureType() {
    return getFeatureTypeConfig().map(featureTypeConfig -> featureTypeConfig.getFeatureType().name());
  }

  @Override
  public Optional<String> getDefaultValue() {
    return _defaultValue;
  }

  @Override
  public Optional<FeatureTypeConfig> getFeatureTypeConfig() {
    return _featureTypeConfig;
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    sb.append(DEF).append(": ").append(_featureExpr).append("\n");
    _defaultValue.ifPresent(v -> sb.append(DEFAULT).append(": ").append(v).append("\n"));
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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComplexFeatureConfig that = (ComplexFeatureConfig) o;
    return Objects.equals(_featureExpr, that._featureExpr) && _exprType == that._exprType && Objects.equals(
        _defaultValue, that._defaultValue) && Objects.equals(_featureTypeConfig, that._featureTypeConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_featureExpr, _exprType, _defaultValue, _featureTypeConfig);
  }
}
