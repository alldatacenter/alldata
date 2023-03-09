package com.linkedin.feathr.core.config.consumer;

import com.linkedin.feathr.core.config.ConfigObj;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents the temporal fields for the relative time range object.
 *
 * @author rkashyap
 */
public class RelativeTimeRangeConfig implements ConfigObj {
  public static final String WINDOW = "window";
  public static final String OFFSET = "offset";

  private final Duration _window;
  private final Optional<Duration> _offset;

  private String _configStr;

  /**
   * Constructor with all parameters
   * @param window number of days/hours from the reference date, reference date = current time - offset
   * @param offset number of days/hours to look back relative to the current timestamp
   */
  public RelativeTimeRangeConfig(Duration window, Duration offset) {
    _window = window;
    _offset = Optional.ofNullable(offset);

    constructConfigStr();
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    sb.append(WINDOW).append(": ").append(_window).append("\n");
    _offset.ifPresent(t -> sb.append(OFFSET).append(": ").append(t).append("\n"));
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
    if (!(o instanceof RelativeTimeRangeConfig)) {
      return false;
    }
    RelativeTimeRangeConfig that = (RelativeTimeRangeConfig) o;
    return Objects.equals(_window, that._window) && Objects.equals(_offset, that._offset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_window, _offset);
  }

  public Duration getWindow() {
    return _window;
  }

  public Optional<Duration>  getOffset() {
    return _offset;
  }
}
