package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.config.WindowType;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a windowparameters object config which is used in
 * @see TimeWindowFeatureConfig
 *    windowParameters:
 *    {
 *      type: <String> //The window type: SlidingWindow (MVP), FixedWindow (MVP), SessionWindow
 *      size: <Duration> length of window time
 *      slidingInterval: <Duration> // (Optional) Used only by sliding window. Specifies the interval of sliding window starts
 *    }
 *  }
 * Details can be referenced in the FeatureDefConfigSchema
 */
public class WindowParametersConfig {
  private final WindowType _windowType;
  private final Duration _size;
  private final Optional<Duration> _slidingInterval;
  private String _configStr;

  /**
   * Constructor with all parameters
   * @param windowType //The window type: SlidingWindow (MVP), FixedWindow (MVP), SessionWindow
   * @param size length of window time
   * @param slidingInterval (Optional) Used only by sliding window. Specifies the interval of sliding window starts
   */
  public WindowParametersConfig(WindowType windowType, Duration size, Duration slidingInterval) {
    _windowType = windowType;
    _size = size;
    _slidingInterval = Optional.ofNullable(slidingInterval);
    constructConfigStr();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WindowParametersConfig)) {
      return false;
    }
    WindowParametersConfig that = (WindowParametersConfig) o;
    return Objects.equals(_windowType, that._windowType) && Objects.equals(_size, that._size)
        && Objects.equals(_slidingInterval, that._slidingInterval);
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();

    sb.append(FeatureConfig.TYPE).append(": ").append(_windowType).append("\n")
        .append(FeatureConfig.SIZE).append(": ").append(_size).append("\n");
    _slidingInterval.ifPresent(d -> sb.append(FeatureConfig.SLIDING_INTERVAL).append(": ").append(d).append("\n"));

    _configStr = sb.toString();
  }

  @Override
  public String toString() {
    return _configStr;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_windowType, _size, _slidingInterval);
  }

  public WindowType getWindowType() {
    return _windowType;
  }

  public Duration getSize() {
    return _size;
  }

  public Optional<Duration>  getSlidingInterval() {
    return _slidingInterval;
  }
}
