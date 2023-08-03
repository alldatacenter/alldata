package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;
import java.util.Optional;


/**
 * Abstract class for all HDFS config classes
 */
public abstract class HdfsConfig extends SourceConfig {
  private final String _path;
  private final Optional<String> _timePartitionPattern;

  /* Represents the fields in a HDFS source config */
  public static final String PATH = "location.path";
  public static final String HAS_TIME_SNAPSHOT = "hasTimeSnapshot";
  public static final String TIME_PARTITION_PATTERN = "timePartitionPattern";

  /**
   * Constructor
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param path HDFS path or Dali URI used to access HDFS
   * @param timePartitionPattern  format of the time partitioned feature
   */
  protected HdfsConfig(String sourceName, String path, String timePartitionPattern) {
    super(sourceName);
    _path = path;
    _timePartitionPattern = Optional.ofNullable(timePartitionPattern);
  }

  /**
   * Constructor
   * @param path HDFS path or Dali URI used to access HDFS
   */
  protected HdfsConfig(String sourceName, String path) {
    this(sourceName, path, null);
  }

  public String getPath() {
    return _path;
  }

  public Optional<String> getTimePartitionPattern() {
    return _timePartitionPattern;
  }

  @Override
  public SourceType getSourceType() {
    return SourceType.HDFS;
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
    HdfsConfig that = (HdfsConfig) o;
    return Objects.equals(_path, that._path) && Objects.equals(_timePartitionPattern, that._timePartitionPattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _path, _timePartitionPattern);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("HdfsConfig{");
    sb.append("_path='").append(_path).append('\'');
    sb.append(", _timePartitionPattern=").append(_timePartitionPattern);
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
