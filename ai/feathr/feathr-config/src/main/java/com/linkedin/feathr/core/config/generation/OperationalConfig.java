package com.linkedin.feathr.core.config.generation;

import com.linkedin.feathr.core.config.ConfigObj;
import java.util.List;
import java.util.Objects;


/**
 * Operational section in feature generation config
 *
 * This abstract class is extended by offline and nearline Operational Config.
 */
public abstract class OperationalConfig implements ConfigObj {
  private final List<OutputProcessorConfig> _outputProcessorsListConfig;
  private final String _name;

  public OperationalConfig(List<OutputProcessorConfig> outputProcessorsListConfig, String name) {
    _outputProcessorsListConfig = outputProcessorsListConfig;
    _name = name;
  }

  /*
   * The previously used lombok library auto generates getters with underscore, which is used in production.
   * For backward compatibility, we need to keep these getters.
   * However, function name with underscore can not pass LinkedIn's style check, here we need suppress the style check
   *  for the getters only.
   *
   * For more detail, please refer to the style check wiki:
   * https://iwww.corp.linkedin.com/wiki/cf/display/TOOLS/Checking+Java+Coding+Style+with+Gradle+Checkstyle+Plugin
   *
   * TODO - 7493) remove the ill-named getters
   */
  // CHECKSTYLE:OFF
  @Deprecated
  public List<OutputProcessorConfig> get_outputProcessorsListConfig() {
    return _outputProcessorsListConfig;
  }

  @Deprecated
  public String get_name() {
    return _name;
  }
  // CHECKSTYLE:ON

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
