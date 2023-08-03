package com.linkedin.feathr.core.config.producer;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.config.producer.derivations.DerivationsConfig;
import com.linkedin.feathr.core.config.producer.anchors.AnchorsConfig;
import com.linkedin.feathr.core.config.producer.sources.SourcesConfig;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents the FeatureDef configuration
 *
 * @author djaising
 * @author cesun
 */
public final class FeatureDefConfig implements ConfigObj {
  /*
   * Fields used to specify each of the six sections in a FeatureDef config
   */
  public static final String SOURCES = "sources";
  public static final String ANCHORS = "anchors";
  public static final String DERIVATIONS = "derivations";
  public static final String FEATURES = "features";

  private final Optional<SourcesConfig> _sourcesConfig;
  private final Optional<AnchorsConfig> _anchorsConfig;
  private final Optional<DerivationsConfig> _derivationsConfig;

  private String _configStr;

  /**
   * Constructor with full parameters
   * @param sourcesConfig {@link SourcesConfig}
   * @param anchorsConfig {@link AnchorsConfig}
   * @param derivationsConfig {@link DerivationsConfig}
   */
  public FeatureDefConfig(SourcesConfig sourcesConfig,
      AnchorsConfig anchorsConfig, DerivationsConfig derivationsConfig) {
    _sourcesConfig = Optional.ofNullable(sourcesConfig);
    _anchorsConfig = Optional.ofNullable(anchorsConfig);
    _derivationsConfig = Optional.ofNullable(derivationsConfig);

    constructConfigStr();
  }

  private void constructConfigStr() {
    StringBuilder strBldr = new StringBuilder();
    _sourcesConfig.ifPresent(cfg -> strBldr.append(SOURCES).append(": ").append(cfg).append("\n"));
    _anchorsConfig.ifPresent(cfg -> strBldr.append(ANCHORS).append(": ").append(cfg).append("\n"));
    _derivationsConfig.ifPresent(cfg -> strBldr.append(DERIVATIONS).append(": ").append(cfg).append("\n"));
    _configStr = strBldr.toString();
  }

  public Optional<SourcesConfig> getSourcesConfig() {
    return _sourcesConfig;
  }

  public Optional<AnchorsConfig> getAnchorsConfig() {
    return _anchorsConfig;
  }

  public Optional<DerivationsConfig> getDerivationsConfig() {
    return _derivationsConfig;
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
    FeatureDefConfig that = (FeatureDefConfig) o;
    return Objects.equals(_sourcesConfig, that._sourcesConfig)
        && Objects.equals(_anchorsConfig, that._anchorsConfig) && Objects.equals(_derivationsConfig,
        that._derivationsConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_sourcesConfig, _anchorsConfig, _derivationsConfig);
  }
}
