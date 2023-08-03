package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;
import java.util.Optional;


/**
 * Represents the RocksDB source config
 */
// TODO: verify if both encoder and decoder are required. Frame will support 'Use Mode 3' where both of these are required.
public final class RocksDbConfig extends SourceConfig {

  /*
   * Fields used to specify config params in RocksDB source config
   */
  public static final String REFERENCE_SOURCE = "referenceSource";
  public static final String EXTRACT_FEATURES = "extractFeatures";
  public static final String ENCODER = "encoder";
  public static final String DECODER = "decoder";
  public static final String KEYEXPR = "keyExpr";

  private final String _referenceSource;
  private final Boolean _extractFeatures;
  private final Optional<String> _encoder;
  private final Optional<String> _decoder;
  private final Optional<String> _keyExpr;

  /**
   * Constructor with full parameters
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   */
  public RocksDbConfig(String sourceName, String referenceSource, Boolean extractFeatures, String encoder, String decoder,
      String keyExpr) {
    super(sourceName);

    _referenceSource = referenceSource;
    _extractFeatures = extractFeatures;
    _encoder = Optional.ofNullable(encoder);
    _decoder = Optional.ofNullable(decoder);
    _keyExpr = Optional.ofNullable(keyExpr);
  }

  @Deprecated
  /**
   * Deprecated Constructor without full parameters for backwards compatibility
   * @param referenceSource
   * @param extractFeatures
   * @param encoder encoder
   * @param decoder decoder
   */
  public RocksDbConfig(String sourceName, String referenceSource, Boolean extractFeatures, String encoder, String decoder) {
    super(sourceName);

    _referenceSource = referenceSource;
    _extractFeatures = extractFeatures;
    _encoder = Optional.ofNullable(encoder);
    _decoder = Optional.ofNullable(decoder);
    _keyExpr = Optional.empty();
  }

  public String getReferenceSource() {
    return _referenceSource;
  }

  public Boolean getExtractFeatures() {
    return _extractFeatures;
  }

  public Optional<String> getEncoder() {
    return _encoder;
  }

  public Optional<String> getDecoder() {
    return _decoder;
  }

  public Optional<String> getKeyExpr() {
    return _keyExpr;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.ROCKSDB;
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
    RocksDbConfig that = (RocksDbConfig) o;
    return Objects.equals(_referenceSource, that._referenceSource) && Objects.equals(_extractFeatures,
        that._extractFeatures) && Objects.equals(_encoder, that._encoder) && Objects.equals(_decoder, that._decoder)
        && Objects.equals(_keyExpr, that._keyExpr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _referenceSource, _extractFeatures, _encoder, _decoder, _keyExpr);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RocksDbConfig{");
    sb.append("_referenceSource='").append(_referenceSource).append('\'');
    sb.append(", _extractFeatures=").append(_extractFeatures);
    sb.append(", _encoder=").append(_encoder);
    sb.append(", _decoder=").append(_decoder);
    sb.append(", _keyExpr=").append(_keyExpr);
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
