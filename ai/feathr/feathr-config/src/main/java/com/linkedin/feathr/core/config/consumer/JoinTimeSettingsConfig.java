package com.linkedin.feathr.core.config.consumer;

import com.linkedin.feathr.core.config.ConfigObj;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the temporal fields for the observationDataTimeSettings used for loading of observation data.
 *
 * @author rkashyap
 */
public class JoinTimeSettingsConfig implements ConfigObj {

  public static final String TIMESTAMP_COLUMN = "timestampColumn";
  public static final String SIMULATE_TIME_DELAY = "simulateTimeDelay";
  public static final String USE_LATEST_FEATURE_DATA = "useLatestFeatureData";

  private final Optional<TimestampColumnConfig> _timestampColumn;
  private final Optional<Duration> _simulateTimeDelay;
  private final Optional<Boolean> _useLatestFeatureData;

  private String _configStr;

  /**
   * Constructor with all parameters
   * @param timestampColumn The timestamp column and format object.
   * @param simulateTimeDelay A Duration value that shifts the observation data to the past thus simulating a delay
   *                          on the observation data.
   * @param useLatestFeatureData Boolean to indicate using of latest feature data
   */
  public JoinTimeSettingsConfig(TimestampColumnConfig timestampColumn, Duration simulateTimeDelay, Boolean useLatestFeatureData) {
    _timestampColumn = Optional.ofNullable(timestampColumn);
    _simulateTimeDelay = Optional.ofNullable(simulateTimeDelay);
    _useLatestFeatureData = Optional.ofNullable(useLatestFeatureData);
    constructConfigStr();
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    _timestampColumn.ifPresent(t -> sb.append(TIMESTAMP_COLUMN).append(": ").append(t).append("\n"));
    _simulateTimeDelay.ifPresent(t -> sb.append(SIMULATE_TIME_DELAY).append(": ").append(t).append("\n"));
    _useLatestFeatureData.ifPresent(t -> sb.append(USE_LATEST_FEATURE_DATA).append(": ").append(t).append("\n"));
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
    if (!(o instanceof JoinTimeSettingsConfig)) {
      return false;
    }
    JoinTimeSettingsConfig that = (JoinTimeSettingsConfig) o;
    return Objects.equals(_timestampColumn, that._timestampColumn) && Objects.equals(_simulateTimeDelay, that._simulateTimeDelay)
        && Objects.equals(_useLatestFeatureData, that._useLatestFeatureData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_timestampColumn.hashCode(), _useLatestFeatureData, _simulateTimeDelay);
  }

  public Optional<TimestampColumnConfig> getTimestampColumn() {
    return _timestampColumn;
  }

  public Optional<Duration> getSimulateTimeDelay() {
    return _simulateTimeDelay;
  }

  public Optional<Boolean> getUseLatestFeatureData() {
    return _useLatestFeatureData;
  }
}
