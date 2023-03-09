package com.linkedin.feathr.core.config.producer;

import java.util.Objects;


/**
 * expression with {@link ExprType} type
 */
public class TypedExpr {
  private final String _expr;
  private final ExprType _exprType;
  private String _configStr;

  public TypedExpr(String expr, ExprType exprType) {
    _expr = expr;
    _exprType = exprType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TypedExpr)) {
      return false;
    }
    TypedExpr typedExpr = (TypedExpr) o;
    return Objects.equals(_expr, typedExpr._expr) && _exprType == typedExpr._exprType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_expr, _exprType);
  }

  public String getExpr() {
    return _expr;
  }

  public ExprType getExprType() {
    return _exprType;
  }

  @Override
  public String toString() {
    if (_configStr == null) {
      _configStr = String.join("\n",
          String.join(": ", "expression", _expr),
          String.join(": ", "expression type", _exprType.toString()));
    }
    return _configStr;
  }
}
