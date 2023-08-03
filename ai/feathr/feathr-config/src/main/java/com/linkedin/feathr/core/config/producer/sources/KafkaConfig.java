package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;
import java.util.Optional;


/**
 * Represents Kafka source config
 */
public final class KafkaConfig extends SourceConfig {
  private final String _stream;
  private final Optional<SlidingWindowAggrConfig> _swaConfig;

  /*
   * Field used in Kafka source config fragment
   */
  public static final String STREAM = "stream";

  /**
   * Constructor with full parameters
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param stream Name of Kafka stream
   * @param swaConfig {@link SlidingWindowAggrConfig} object
   */
  public KafkaConfig(String sourceName, String stream, SlidingWindowAggrConfig swaConfig) {
    super(sourceName);
    _stream = stream;
    _swaConfig = Optional.ofNullable(swaConfig);
  }

  public String getStream() {
    return _stream;
  }

  public Optional<SlidingWindowAggrConfig> getSwaConfig() {
    return _swaConfig;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.KAFKA;
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
    KafkaConfig that = (KafkaConfig) o;
    return Objects.equals(_stream, that._stream) && Objects.equals(_swaConfig, that._swaConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _stream, _swaConfig);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("KafkaConfig{");
    sb.append("_stream='").append(_stream).append('\'');
    sb.append(", _swaConfig=").append(_swaConfig);
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
