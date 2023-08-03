package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;


/**
 * Represents HDFS config for non-time-series, that is, regular data
 */
public final class HdfsConfigWithRegularData extends HdfsConfig {
  // this is a deprecated field. It is replaced by timePartitionPattern. We keep it for backward compatibility.
  private final Boolean _hasTimeSnapshot;

  /**
   * Constructor with full parameters
   *
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param path HDFS path or Dali URI used to access HDFS
   * @param timePartitionPattern  format of the time partitioned feature
   * @param hasTimeSnapshot True if the HDFS source supports time-based access
   */
  public HdfsConfigWithRegularData(String sourceName, String path, String timePartitionPattern, Boolean hasTimeSnapshot) {
    super(sourceName, path, timePartitionPattern);
    _hasTimeSnapshot = hasTimeSnapshot;
  }

  /**
   * Constructor
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param path HDFS path or Dali URI used to access HDFS
   * @param hasTimeSnapshot True if the HDFS source supports time-based access
   */
  public HdfsConfigWithRegularData(String sourceName, String path, Boolean hasTimeSnapshot) {
    this(sourceName, path, null, hasTimeSnapshot);
  }

  public Boolean getHasTimeSnapshot() {
    return _hasTimeSnapshot;
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
    HdfsConfigWithRegularData that = (HdfsConfigWithRegularData) o;
    return Objects.equals(_hasTimeSnapshot, that._hasTimeSnapshot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _hasTimeSnapshot);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("HdfsConfigWithRegularData{");
    sb.append("_hasTimeSnapshot=").append(_hasTimeSnapshot);
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
