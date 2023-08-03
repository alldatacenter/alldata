package com.linkedin.feathr.common.configObj.generation;

import com.linkedin.feathr.common.configObj.ConfigObj;

import java.util.List;
import java.util.Objects;


/**
 * Operational section in feature generation config
 *
 * This abstract class is extended by offline Operational Config.
 */
public abstract class OperationalConfig implements ConfigObj {
  private final List<OutputProcessorConfig> _outputProcessorsListConfig;
  private final String _name;

  public OperationalConfig(List<OutputProcessorConfig> outputProcessorsListConfig, String name) {
    _outputProcessorsListConfig = outputProcessorsListConfig;
    _name = name;
  }

  public List<OutputProcessorConfig> getOutputProcessorsListConfig() {
    return _outputProcessorsListConfig;
  }

  public String getName() {
    return _name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OperationalConfig)) {
      return false;
    }
    OperationalConfig that = (OperationalConfig) o;
    return Objects.equals(_outputProcessorsListConfig, that._outputProcessorsListConfig) && Objects.equals(_name,
        that._name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_outputProcessorsListConfig, _name);
  }

  @Override
  public String toString() {
    return "OperationalConfig{" + "_outputProcessorsListConfig=" + _outputProcessorsListConfig + ", _name='" + _name
        + '\'' + '}';
  }
}
