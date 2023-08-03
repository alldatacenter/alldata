package com.linkedin.feathr.core.config.producer.derivations;

import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.TypedExpr;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents a derived feature whose derivation can be expressed as a user-defined expression with type
 *
 * @author djaising
 * @author cesun
 */
public final class SimpleDerivationConfig implements DerivationConfig {
  private final TypedExpr _featureTypedExpr;
  private final Optional<FeatureTypeConfig> _featureTypeConfig;

  /**
   * Constructor
   * @param featureExpr A user-defined MVEL expression
   * @deprecated please use {@link #SimpleDerivationConfig(TypedExpr)}
   */
  @Deprecated
  public SimpleDerivationConfig(String featureExpr) {
    _featureTypedExpr = new TypedExpr(featureExpr, ExprType.MVEL);
    _featureTypeConfig = Optional.empty();
  }

  /**
   * Constructor
   * @param typedExpr A user-defined expression with type
   */
  public SimpleDerivationConfig(TypedExpr typedExpr) {
    _featureTypedExpr = typedExpr;
    _featureTypeConfig = Optional.empty();
  }


  /**
   * Constructor
   * @param typedExpr A user-defined expression with type
   */
  public SimpleDerivationConfig(TypedExpr typedExpr, FeatureTypeConfig featureTypeConfig) {
    _featureTypedExpr = typedExpr;
    _featureTypeConfig = Optional.ofNullable(featureTypeConfig);
  }

  /**
   * get the expression string
   * @deprecated please use {@link #getFeatureTypedExpr()}
   */
  @Deprecated
  public String getFeatureExpr() {
    return _featureTypedExpr.getExpr();
  }

  public TypedExpr getFeatureTypedExpr() {
    return _featureTypedExpr;
  }

  public Optional<FeatureTypeConfig> getFeatureTypeConfig() {
    return _featureTypeConfig;
  }

  @Override
  public String toString() {
    return _featureTypedExpr.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimpleDerivationConfig that = (SimpleDerivationConfig) o;
    return Objects.equals(_featureTypedExpr, that._featureTypedExpr) && Objects.equals(_featureTypeConfig,
        that._featureTypeConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_featureTypedExpr, _featureTypeConfig);
  }
}
