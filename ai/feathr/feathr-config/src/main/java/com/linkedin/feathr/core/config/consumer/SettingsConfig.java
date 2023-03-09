package com.linkedin.feathr.core.config.consumer;

import com.linkedin.feathr.core.config.ConfigObj;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents some 'settings' on the observation data.
 *
 * @author djaising
 * @author cesun
 */
public final class SettingsConfig implements ConfigObj {
  /*
   * Represents the field used to specify the temporal parameter for sliding window aggregation or time aware join
   * in the Join Config file.
   */
  public static final String OBSERVATION_DATA_TIME_SETTINGS = "observationDataTimeSettings";
  public static final String JOIN_TIME_SETTINGS = "joinTimeSettings";

  private final Optional<ObservationDataTimeSettingsConfig> _observationDataTimeSettings;
  private final Optional<JoinTimeSettingsConfig> _joinTimeSettings;

  private String _configStr;

  /**
   * Constructor with parameter timeWindowJoin and observationTimeInfo
   * @param observationDataTimeSettings temporal parameters used to load the observation.
   * @param joinTimeSettings   temporal parameters used for joining the observation with the feature data.
   */
  public SettingsConfig(ObservationDataTimeSettingsConfig observationDataTimeSettings, JoinTimeSettingsConfig joinTimeSettings) {
    _observationDataTimeSettings = Optional.ofNullable(observationDataTimeSettings);
    _joinTimeSettings = Optional.ofNullable(joinTimeSettings);
    constructConfigStr();
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    _observationDataTimeSettings.ifPresent(t -> sb.append(OBSERVATION_DATA_TIME_SETTINGS).append(": ").append(t).append("\n"));
    _joinTimeSettings.ifPresent(t -> sb.append(JOIN_TIME_SETTINGS).append(": ").append(t).append("\n"));
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
    if (!(o instanceof SettingsConfig)) {
      return false;
    }
    SettingsConfig that = (SettingsConfig) o;
    return Objects.equals(_observationDataTimeSettings, that._observationDataTimeSettings) && Objects.equals(_joinTimeSettings, that._joinTimeSettings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_observationDataTimeSettings, _joinTimeSettings);
  }

  public Optional<ObservationDataTimeSettingsConfig> getTimeWindowJoin() {
    return _observationDataTimeSettings;
  }

  public Optional<JoinTimeSettingsConfig> getObservationTimeInfo() {
    return _joinTimeSettings;
  }
}
