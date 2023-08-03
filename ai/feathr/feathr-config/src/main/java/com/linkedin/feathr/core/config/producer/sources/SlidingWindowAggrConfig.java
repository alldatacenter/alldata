package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;

/**
 * Represents sliding time-window aggregation config
 */
public final class SlidingWindowAggrConfig {
  public static final String IS_TIME_SERIES = "isTimeSeries";
  public static final String TIMEWINDOW_PARAMS = "timeWindowParameters";

  // this is a deprecated field. It is replaced by timePartitionPattern. We keep it for backward compatibility.
  private final Boolean _isTimeSeries;

  private final TimeWindowParams _timeWindowParams;

  private String _configStr;

  /**
   * Constructor
   * @param isTimeSeries Always true
   * @param timeWindowParams Sliding time-window parameters
   */
  public SlidingWindowAggrConfig(Boolean isTimeSeries, TimeWindowParams timeWindowParams) {
    _isTimeSeries = isTimeSeries;
    _timeWindowParams = timeWindowParams;

    StringBuilder sb = new StringBuilder();
    sb.append(IS_TIME_SERIES).append(": ").append(isTimeSeries).append("\n")
        .append(TIMEWINDOW_PARAMS).append(": ").append(timeWindowParams).append("\n");
    _configStr = sb.toString();
  }

  @Override
  public String toString() {
    return _configStr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SlidingWindowAggrConfig)) {
      return false;
    }
    SlidingWindowAggrConfig that = (SlidingWindowAggrConfig) o;
    return Objects.equals(_isTimeSeries, that._isTimeSeries) && Objects.equals(_timeWindowParams, that._timeWindowParams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_isTimeSeries, _timeWindowParams);
  }

  public Boolean getTimeSeries() {
    return _isTimeSeries;
  }

  public TimeWindowParams getTimeWindowParams() {
    return _timeWindowParams;
  }
}
