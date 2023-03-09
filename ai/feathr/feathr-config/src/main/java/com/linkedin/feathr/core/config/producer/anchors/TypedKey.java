package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.common.KeyListExtractor;
import com.linkedin.feathr.core.utils.Utils;
import java.util.List;
import java.util.Objects;


/**
 * Key expressions with the corresponding {@link ExprType}
 */
public class TypedKey {
  private final String _rawKeyExpr;
  private final List<String> _key;
  private final ExprType _keyExprType;
  private String _configStr;

  /**
   * Constructor
   * @param rawKeyExpr the raw key expression
   * @param keyExprType key type
   */
  public TypedKey(String rawKeyExpr, ExprType keyExprType) {
    _rawKeyExpr = rawKeyExpr;
    // For now, we only support HOCON String format as the raw key expression
    _key = KeyListExtractor.getInstance().extractFromHocon(rawKeyExpr);
    _keyExprType = keyExprType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TypedKey)) {
      return false;
    }
    TypedKey typedKey = (TypedKey) o;
    /*
     * Using the HOCON expression is too strict to check equality. For instance
     * The following three key expressions:
     *
     * key1: [
     *       # String: 3
     *       "key1",
     *       # String: 3
     *       "key2"
     *  ]
     *
     * key2: [key1, key2]
     *
     * key3: ["key1", "key2"]
     *
     * All have the same meaning, it is misleading,
     *  and sometimes impossible (e.g. in unit tests) to distinguish between these.
     * And we should not distinguish them given that we've already parsed them using HOCON API in frame-core.
     *
     * Instead, we use the parsed key list to check the equality.
     */
    return Objects.equals(_key, typedKey._key) && _keyExprType == typedKey._keyExprType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_rawKeyExpr, _key, _keyExprType);
  }

  @Override
  public String toString() {
    if (_configStr == null) {
      _configStr = String.join("\n",
          String.join(": ", "raw key expression", _rawKeyExpr),
          String.join(": ", "key", (_key.size() == 1 ? _key.get(0) : Utils.string(_key))),
          String.join(": ", "key expression type", _keyExprType.toString()));
    }
    return _configStr;
  }

  /**
   * Get the list of key String extracted from raw key expression
   */
  public List<String> getKey() {
    return _key;
  }

  public ExprType getKeyExprType() {
    return _keyExprType;
  }

  public String getRawKeyExpr() {
    return _rawKeyExpr;
  }
}
