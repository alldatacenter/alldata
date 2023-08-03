package com.linkedin.feathr.core.config.producer.derivations;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.utils.Utils;
import java.util.Map;
import java.util.Objects;


/**
 * Container class for all derived feature configurations.
 *
 * @author djaising
 * @author cesun
 */
public final class DerivationsConfig implements ConfigObj {

  private final Map<String, DerivationConfig> _derivations;

  private String _configStr;

  /**
   * Constructor
   * @param derivations map of derivation name to {@link DerivationConfig}
   */
  public DerivationsConfig(Map<String, DerivationConfig> derivations) {
    _derivations = derivations;
    _configStr = Utils.string(derivations, "\n");
  }

  public Map<String, DerivationConfig> getDerivations() {
    return _derivations;
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
    if (!(o instanceof DerivationsConfig)) {
      return false;
    }
    DerivationsConfig that = (DerivationsConfig) o;
    return Objects.equals(_derivations, that._derivations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_derivations);
  }
}
