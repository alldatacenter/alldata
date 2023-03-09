package com.linkedin.feathr.core.config.producer.sources;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.utils.Utils;
import java.util.Map;
import java.util.Objects;


/**
 * Container class for the source configurations specified in the sources section of the FeatureDef config file.
 */
public final class SourcesConfig implements ConfigObj {
  private final Map<String, SourceConfig> _sources;

  private String _configStr;

  public SourcesConfig(Map<String, SourceConfig> sources) {
    _sources = sources;
    _configStr = Utils.string(sources);
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
    if (!(o instanceof SourcesConfig)) {
      return false;
    }
    SourcesConfig that = (SourcesConfig) o;
    return Objects.equals(_sources, that._sources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_sources);
  }

  public Map<String, SourceConfig> getSources() {
    return _sources;
  }
}

