package com.linkedin.feathr.core.config.producer.sources;


/**
 * Represents the supported source types by Frame.
 */
public enum SourceType {
  HDFS("HDFS"),
  ESPRESSO("Espresso"),
  RESTLI("RestLi"),
  VENICE("Venice"),
  KAFKA("Kafka"),
  ROCKSDB("RocksDB"),
  PASSTHROUGH("PASSTHROUGH"),
  COUCHBASE("Couchbase"),
  CUSTOM("Custom"),
  PINOT("Pinot"),
  VECTOR("Vector");

  private final String _sourceType;
  SourceType(String sourceType) {
    _sourceType = sourceType;
  }

  public String getSourceType() {
    return _sourceType;
  }
}
