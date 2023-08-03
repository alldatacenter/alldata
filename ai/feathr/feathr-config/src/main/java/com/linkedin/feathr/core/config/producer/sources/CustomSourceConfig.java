package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;

/**
 * Represents Custom source config
 */
public final class CustomSourceConfig extends SourceConfig {

  private final String _keyExpr;

  // the model of the data being fetched from the custom source
  private final String _dataModel;

  /**
   * Field used in CUSTOM source config fragment
   */
  public static final String DATA_MODEL = "dataModel";
  public static final String KEY_EXPR = "keyExpr";

  /**
   * Constructor with parameters
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param keyExpr the key expression used to compute the key against the custom source
   * @param dataModel Class name of the data returned from the custom source
   */
  public CustomSourceConfig(String sourceName, String keyExpr, String dataModel) {
    super(sourceName);
    _keyExpr = keyExpr;
    _dataModel = dataModel;
  }

  public String getDataModel() {
    return _dataModel;
  }

  public String getKeyExpr() {
    return _keyExpr;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.CUSTOM;
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
    CustomSourceConfig that = (CustomSourceConfig) o;
    return Objects.equals(_keyExpr, that._keyExpr) && Objects.equals(_dataModel, that._dataModel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _keyExpr, _dataModel);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CustomSourceConfig{");
    sb.append("_keyExpr='").append(_keyExpr).append('\'');
    sb.append(", _dataModel='").append(_dataModel).append('\'');
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
