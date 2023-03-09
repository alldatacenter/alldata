package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;


/**
 * Represents the source config params for a Couchbase store
 */
public final class CouchbaseConfig extends SourceConfig {
  // Couchbase bucket name
  private final String _bucketName;

  // Expression used to produce Couchbase key input
  private final String _keyExpr;

  // Fully qualified class name of the stored document in bucket
  private final String _documentModel;

  /*
   * Fields used to specify the Couchbase source configuration
   */
  public static final String BUCKET_NAME = "bucketName";
  public static final String KEY_EXPR = "keyExpr";
  public static final String BOOTSTRAP_URIS = "bootstrapUris";
  public static final String DOCUMENT_MODEL = "documentModel";

  /**
   * Constructor
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param bucketName Name of the Couchbase bucket
   * @param keyExpr Key expression
   * @param documentModel Document model stored in bucket
   */
  public CouchbaseConfig(String sourceName, String bucketName, String keyExpr, String documentModel) {
    super(sourceName);
    _bucketName = bucketName;
    _keyExpr = keyExpr;
    _documentModel = documentModel;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.COUCHBASE;
  }

  public String getBucketName() {
    return _bucketName;
  }

  public String getKeyExpr() {
    return _keyExpr;
  }

  public String getDocumentModel() {
    return _documentModel;
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
    CouchbaseConfig that = (CouchbaseConfig) o;
    return Objects.equals(_bucketName, that._bucketName) && Objects.equals(_keyExpr, that._keyExpr)
        && Objects.equals(_documentModel, that._documentModel);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), _bucketName, _keyExpr, _documentModel);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CouchbaseConfig{");
    sb.append("_bucketName='").append(_bucketName).append('\'');
    sb.append(", _keyExpr='").append(_keyExpr).append('\'');
    sb.append(", _documentModel='").append(_documentModel).append('\'');
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
