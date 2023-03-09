package com.linkedin.feathr.common.configObj.generation;

import com.linkedin.feathr.common.configObj.DateTimeConfig;

import java.time.Duration;
import java.util.List;
import java.util.Objects;


/**
 * Operational section in feature generation config
 *
 * Feature generation config contains two major sections, i.e., operational and feature list sections,
 * feature list specify the features to generate,
 * operational section contains all the related settings.
 */
public class OfflineOperationalConfig extends OperationalConfig {
  private final DateTimeConfig _timeSetting;
  private final Duration _retention;
  private final Duration _simulateTimeDelay;
  private final Boolean _enableIncremental;

  public OfflineOperationalConfig(List<OutputProcessorConfig> outputProcessorsListConfig, String name, DateTimeConfig timeSetting,
      Duration retention, Duration simulateTimeDelay, Boolean enableIncremental) {
    super(outputProcessorsListConfig, name);
    _timeSetting = timeSetting;
    _retention = retention;
    _simulateTimeDelay = simulateTimeDelay;
    _enableIncremental = enableIncremental;
  }

  /*
   * The previously used lombok library auto generates getters with underscore, which is used in production.
   * For backward compatibility, we need to keep these getters.
   * However, function name with underscore can not pass LinkedIn's style check, here we need suppress the style check
   *  for the getters only.
   *

   */
  // CHECKSTYLE:OFF
  @Deprecated
  public DateTimeConfig get_timeSetting() {
    return _timeSetting;
  }

  @Deprecated
  public Duration get_retention() {
    return _retention;
  }

  @Deprecated
  public Duration get_simulateTimeDelay() {
    return _simulateTimeDelay;
  }

  @Deprecated
  public Boolean get_enableIncremental() {
    return _enableIncremental;
  }
  // CHECKSTYLE:ON

  public DateTimeConfig getTimeSetting() {
    return _timeSetting;
  }

  public Duration getRetention() {
    return _retention;
  }

  public Duration getSimulateTimeDelay() {
    return _simulateTimeDelay;
  }

  public Boolean getEnableIncremental() {
    return _enableIncremental;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OfflineOperationalConfig)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    OfflineOperationalConfig that = (OfflineOperationalConfig) o;
    return Objects.equals(_timeSetting, that._timeSetting) && Objects.equals(_retention, that._retention)
        && Objects.equals(_simulateTimeDelay, that._simulateTimeDelay) && Objects.equals(_enableIncremental,
        that._enableIncremental);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _timeSetting, _retention, _simulateTimeDelay, _enableIncremental);
  }

  @Override
  public String toString() {
    return "OfflineOperationalConfig{" + "_timeSetting=" + _timeSetting + ", _retention=" + _retention
        + ", _simulateTimeDelay=" + _simulateTimeDelay + ", _enableIncremental=" + _enableIncremental + '}';
  }
}
