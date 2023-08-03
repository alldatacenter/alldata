package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;


/**
 * Represents the source config params for a Venice store
 */
public final class VeniceConfig extends SourceConfig {
  private final String _storeName;
  private final String _keyExpr;

  /*
   * Fields used to specify the Venice source configuration
   */
  public static final String STORE_NAME = "storeName";
  public static final String KEY_EXPR = "keyExpr";

  /**
   * Constructor
   *
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param storeName Name of the Venice store
   * @param keyExpr Key expression
   */
  public VeniceConfig(String sourceName, String storeName, String keyExpr) {
    super(sourceName);
    _storeName = storeName;
    _keyExpr = keyExpr;
  }

  public String getStoreName() {
    return _storeName;
  }

  public String getKeyExpr() {
    return _keyExpr;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.VENICE;
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
    VeniceConfig that = (VeniceConfig) o;
    return Objects.equals(_storeName, that._storeName) && Objects.equals(_keyExpr, that._keyExpr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _storeName, _keyExpr);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("VeniceConfig{");
    sb.append("_storeName='").append(_storeName).append('\'');
    sb.append(", _keyExpr='").append(_keyExpr).append('\'');
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
