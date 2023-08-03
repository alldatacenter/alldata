package com.linkedin.feathr.core.config.consumer;

import com.linkedin.feathr.core.config.ConfigObj;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents temporal parameters used in observationDataTimeSettings.
 *
 * @author rkashyap
 */
public class ObservationDataTimeSettingsConfig implements ConfigObj {

  public static final String ABSOLUTE_TIME_RANGE = "absoluteTimeRange";
  public static final String RELATIVE_TIME_RANGE = "relativeTimeRange";

  private final Optional<AbsoluteTimeRangeConfig> _absoluteTimeRangeConfig;
  private final Optional<RelativeTimeRangeConfig> _relativeTimeRangeConfig;

  private String _configStr;

  /**
   * Constructor with all parameters
   * @param absoluteTimeRangeConfig The observation data's absolute time range
   * @param relativeTimeRangeConfig The observation data's relative time range
   */
  public ObservationDataTimeSettingsConfig(AbsoluteTimeRangeConfig absoluteTimeRangeConfig,
      RelativeTimeRangeConfig relativeTimeRangeConfig) {
    _absoluteTimeRangeConfig = Optional.ofNullable(absoluteTimeRangeConfig);
    _relativeTimeRangeConfig = Optional.ofNullable(relativeTimeRangeConfig);

    constructConfigStr();
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    _absoluteTimeRangeConfig.ifPresent(t -> sb.append(t).append(": ").append(t).append("\n"));
    _relativeTimeRangeConfig.ifPresent(t -> sb.append(t).append(": ").append(t).append("\n"));

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
    if (!(o instanceof ObservationDataTimeSettingsConfig)) {
      return false;
    }
    ObservationDataTimeSettingsConfig that = (ObservationDataTimeSettingsConfig) o;
    return Objects.equals(_absoluteTimeRangeConfig, that._absoluteTimeRangeConfig)
        && Objects.equals(_relativeTimeRangeConfig, that._relativeTimeRangeConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_absoluteTimeRangeConfig, _relativeTimeRangeConfig);
  }

  public Optional<AbsoluteTimeRangeConfig>  getAbsoluteTimeRange() {
    return _absoluteTimeRangeConfig;
  }

  public Optional<RelativeTimeRangeConfig> getRelativeTimeRange() {
    return _relativeTimeRangeConfig;
  }

}
