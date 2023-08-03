package com.linkedin.feathr.core.config.consumer;

import com.linkedin.feathr.core.config.ConfigObj;
import java.util.Objects;

/**
 * Represents the temporal fields for the absolute time range object.
 *
 * @author rkashyap
 */
public class AbsoluteTimeRangeConfig implements ConfigObj {
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String TIME_FORMAT = "timeFormat";

  private final String _startTime;
  private final String _endTime;
  private final String _timeFormat;

  private String _configStr;

  /**
   * Constructor with all parameters
   * @param startTime The start time for the observation data
   * @param endTime The end time for the observation data
   * @param timeFormat The time format in which the times are specified
   */
  public AbsoluteTimeRangeConfig(String startTime, String endTime, String timeFormat) {
    _startTime = startTime;
    _endTime = endTime;
    _timeFormat = timeFormat;

    constructConfigStr();
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    sb.append(START_TIME).append(": ").append(_startTime).append("\n")
        .append(END_TIME).append(": ").append(_endTime).append("\n")
        .append(TIME_FORMAT).append(": ").append(_timeFormat).append("\n");
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
    if (!(o instanceof AbsoluteTimeRangeConfig)) {
      return false;
    }
    AbsoluteTimeRangeConfig that = (AbsoluteTimeRangeConfig) o;
    return Objects.equals(_startTime, that._startTime) && Objects.equals(_endTime, that._endTime)
        && Objects.equals(_timeFormat, that._timeFormat);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_startTime, _endTime, _timeFormat);
  }

  public String getStartTime() {
    return _startTime;
  }

  public String  getEndTime() {
    return _endTime;
  }

  public String getTimeFormat() {
    return _timeFormat;
  }
}
