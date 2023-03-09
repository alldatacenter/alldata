package com.linkedin.feathr.core.config.consumer;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.utils.Utils;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the Join Config which specifies the join plan, and is provided by a feature consumer.
 *
 * @author djaising
 * @author cesun
 */
public class JoinConfig implements ConfigObj {
  /*
   * Represents the fields used in the Join Config file
   */
  public static final String SETTINGS = "settings";

  private final Optional<SettingsConfig> _settings;
  private final Map<String, FeatureBagConfig> _featureBagConfigs;

  private String _configStr;

  /**
   * Constructor with all parameters
   * @param settings {@link SettingsConfig} object
   * @param featureBagConfigs The {@link FeatureBagConfig} object that specifies the featureBagConfigs to be fetched and the keys in the observation data
   */
  public JoinConfig(SettingsConfig settings, Map<String, FeatureBagConfig> featureBagConfigs) {
    _settings = Optional.ofNullable(settings);
    _featureBagConfigs = featureBagConfigs;
    constructConfigStr();
  }

  private void constructConfigStr() {
    StringBuilder sb = new StringBuilder();
    _settings.ifPresent(s -> sb.append(SETTINGS).append(": ").append(s).append("\n"));
    sb.append(Utils.string(_featureBagConfigs, "\n")).append("\n");
    _configStr = sb.toString();
  }

  public Optional<SettingsConfig> getSettings() {
    return _settings;
  }

  public Map<String, FeatureBagConfig> getFeatureBagConfigs() {
    return _featureBagConfigs;
  }

  public Optional<FeatureBagConfig> getFeatureBagConfig(String featureBagName) {
    return Optional.ofNullable(_featureBagConfigs.get(featureBagName));
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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JoinConfig that = (JoinConfig) o;
    return Objects.equals(_settings, that._settings) && Objects.equals(_featureBagConfigs, that._featureBagConfigs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_settings, _featureBagConfigs);
  }
}
