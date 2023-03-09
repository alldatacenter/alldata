package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Represents the Vector source config. For example
 *
 * "vectorImageStoreForPNG": {
 *   type: "VECTOR"
 *   keyExpr: "key[0]"
 *   featureSourceName: "png_200_200"
 * }
 *
 * Note here that the featureSourceName is a Vector query parameter which is decided between the team that will use the
 * media data and Vector. This is a string but will be created via a process detailed by the Vector team.
 */
public class VectorConfig extends SourceConfig {
  private final String _keyExpr;
  private final String _featureSourceName;

  /*
   * Fields to specify the Vector source configuration
   */
  public static final String KEY_EXPR = "keyExpr";
  public static final String FEATURE_SOURCE_NAME = "featureSourceName";

  /**
   * Constructor
   * @param sourceName the name of the source referenced by anchors in the feature definition
   * @param keyExpr the key expression used to extract assetUrn to access asset from Vector endpoint
   * @param featureSourceName the vector query parameter needed in addition the assetUrn to fetch the asset
   */
  public VectorConfig(@Nonnull String sourceName, @Nonnull String keyExpr, @Nonnull String featureSourceName) {
    super(sourceName);
    _keyExpr = keyExpr;
    _featureSourceName = featureSourceName;
  }

  public String getKeyExpr() {
    return _keyExpr; }

  public String getFeatureSourceName() {
    return _featureSourceName; }

  @Override
  public SourceType getSourceType() {
    return SourceType.VECTOR;
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
    VectorConfig that = (VectorConfig) o;
    return Objects.equals(_keyExpr, that._keyExpr) && Objects.equals(_featureSourceName, that._featureSourceName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _keyExpr, _featureSourceName);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("VectorConfig{");
    sb.append("_keyExpr=").append(_keyExpr);
    sb.append(", _featureSourceName=").append(_featureSourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
