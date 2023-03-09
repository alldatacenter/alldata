package com.linkedin.feathr.core.config.producer.sources;

import java.util.Objects;


/**
 * Time-window parameters used in {@link SlidingWindowAggrConfig}
 */
public final class TimeWindowParams {
  public static final String TIMESTAMP_FIELD = "timestampColumn";
  public static final String TIMESTAMP_FORMAT = "timestampColumnFormat";
  public static final String TIMESTAMP_EPOCH_SECOND_FORMAT = "epoch";
  public static final String TIMESTAMP_EPOCH_MILLISECOND_FORMAT = "epoch_millis";
  private final String _timestampField;
  private final String _timestampFormat;

  private String _configStr;

  /**
   * Constructor
   * @param timestampField Name of the timestamp column/field in fact data
   * @param timestampFormat Format pattern of the timestamp value, specified in {@link java.time.format.DateTimeFormatter} pattern
   */
  public TimeWindowParams(String timestampField, String timestampFormat) {
    _timestampField = timestampField;
    _timestampFormat = timestampFormat;

    StringBuilder sb = new StringBuilder();
    sb.append(TIMESTAMP_FIELD).append(": ").append(timestampField).append("\n")
        .append(TIMESTAMP_FORMAT).append(": ").append(timestampFormat).append("\n");
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
    if (!(o instanceof TimeWindowParams)) {
      return false;
    }
    TimeWindowParams that = (TimeWindowParams) o;
    return Objects.equals(_timestampField, that._timestampField) && Objects.equals(_timestampFormat, that._timestampFormat);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_timestampField, _timestampFormat);
  }

  public String getTimestampField() {
    return _timestampField;
  }

  public String getTimestampFormat() {
    return _timestampFormat;
  }
}
