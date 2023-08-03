package com.linkedin.feathr.core.config.generation;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.config.common.OutputFormat;
import com.typesafe.config.Config;
import java.util.Objects;


/**
 * Output processor config, e.g., write to HDFS processor or push to Venice processor
 */
public class OutputProcessorConfig implements ConfigObj {
  private final String _name;
  private final OutputFormat _outputFormat;
  // other params, e.g, venice params or hdfs specific parameters
  private final Config _params;

  /**
   * Constructor
   * @param name
   * @param outputFormat
   * @param params
   */
  public OutputProcessorConfig(String name, OutputFormat outputFormat, Config params) {
    _name = name;
    _outputFormat = outputFormat;
    _params = params;
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
  public String get_name() {
    return _name;
  }

  @Deprecated
  public OutputFormat get_outputFormat() {
    return _outputFormat;
  }

  @Deprecated
  public Config get_params() {
    return _params;
  }
  // CHECKSTYLE:ON

  public String getName() {
    return _name;
  }

  public OutputFormat getOutputFormat() {
    return _outputFormat;
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
    return Objects.equals(_name, that._name) && _outputFormat == that._outputFormat && Objects.equals(_params,
        that._params);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name, _outputFormat, _params);
  }

  @Override
  public String toString() {
    return "OutputProcessorConfig{" + "_name='" + _name + '\'' + ", _outputFormat=" + _outputFormat + ", _params="
        + _params + '}';
  }
}