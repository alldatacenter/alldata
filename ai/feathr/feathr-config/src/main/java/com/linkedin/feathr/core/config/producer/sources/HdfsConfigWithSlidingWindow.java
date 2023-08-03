package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;


/**
 * Represents HDFS config with sliding window parameters
 */
public final class HdfsConfigWithSlidingWindow extends HdfsConfig {
  private final SlidingWindowAggrConfig _swaConfig;

  /**
   * Constructor
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param path HDFS path
   * @param timePartitionPattern  format of the time partitioned feature
   * @param swaConfig sliding window config
   */
  public HdfsConfigWithSlidingWindow(String sourceName, String path, String timePartitionPattern, SlidingWindowAggrConfig swaConfig) {
    super(sourceName, path, timePartitionPattern);
    _swaConfig = swaConfig;
  }

  /**
   * Constructor
   * @param sourceName the name of the source and it is referenced by the anchor in the feature definition
   * @param path HDFS path
   * @param swaConfig sliding window config
   */
  public HdfsConfigWithSlidingWindow(String sourceName, String path, SlidingWindowAggrConfig swaConfig) {
    this(sourceName, path, null, swaConfig);
  }

  public SlidingWindowAggrConfig getSwaConfig() {
    return _swaConfig;
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
    HdfsConfigWithSlidingWindow that = (HdfsConfigWithSlidingWindow) o;
    return Objects.equals(_swaConfig, that._swaConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _swaConfig);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("HdfsConfigWithSlidingWindow{");
    sb.append("_swaConfig=").append(_swaConfig);
    sb.append(", _sourceName='").append(_sourceName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
