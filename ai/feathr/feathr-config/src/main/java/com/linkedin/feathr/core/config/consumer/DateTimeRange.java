package com.linkedin.feathr.core.config.consumer;

import java.time.LocalDateTime;
import java.util.Objects;


/**
 * Represents the start and end local date-times without regards to timezone in the ISO-8601 calendar system.
 *
 * @author djaising
 * @author cesun
 */
public final class DateTimeRange {
  public static final String START_TIME = "start_time";
  public static final String END_TIME = "end_time";

  private final LocalDateTime _start;
  private final LocalDateTime _end;

  private String _configStr;

  /**
   * Constructor
   * @param start The start date-time
   * @param end The end date-time
   */
  public DateTimeRange(LocalDateTime start, LocalDateTime end) {
    _start = start;
    _end = end;

    constructConfigStr();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DateTimeRange)) {
      return false;
    }
    DateTimeRange that = (DateTimeRange) o;
    return Objects.equals(_start, that._start) && Objects.equals(_end, that._end);
  }


  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    sb.append(START_TIME).append(": ").append(_start).append("\n")
        .append(END_TIME).append(": ").append(_end).append("\n");
    _configStr = sb.toString();
  }

  @Override
  public String toString() {
    return _configStr;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_start, _end);
  }

  public LocalDateTime getStart() {
    return _start;
  }

  public LocalDateTime getEnd() {
    return _end;
  }
}
