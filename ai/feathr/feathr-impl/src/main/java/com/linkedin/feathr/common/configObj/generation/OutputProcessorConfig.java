package com.linkedin.feathr.common.configObj.generation;

import com.linkedin.feathr.common.configObj.ConfigObj;
import com.typesafe.config.Config;
import java.util.Objects;


/**
 * Output processor config, e.g., write to HDFS processor or push to Redis processor
 */
public class OutputProcessorConfig implements ConfigObj {
  private final String _name;
  // other params, e.g, redis params or hdfs specific parameters
  private final Config _params;

  /**
   * Constructor
   * @param name
   * @param params
   */
  public OutputProcessorConfig(String name, Config params) {
    _name = name;
    _params = params;
  }

  public String getName() {
    return _name;
  }

  public Config getParams() {
    return _params;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OutputProcessorConfig)) {
      return false;
    }
    OutputProcessorConfig that = (OutputProcessorConfig) o;
    return Objects.equals(_name, that._name)  && Objects.equals(_params,
        that._params);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _params);
  }

  @Override
  public String toString() {
    return "OutputProcessorConfig{" + "_name='" + _name + '\'' + ", _params="
        + _params + '}';
  }
}