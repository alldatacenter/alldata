package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.utils.Utils;
import java.util.Map;
import java.util.Objects;


/**
 * Container class for the Anchors.
 *
 * @author djaising
 * @author cesun
 */
public class AnchorsConfig implements ConfigObj {
  private final Map<String, AnchorConfig> _anchors;
  private String _anchorStr;

  /**
   * Constructor
   * @param anchors map of anchor name to {@link AnchorConfig}
   */
  public AnchorsConfig(Map<String, AnchorConfig> anchors) {
    _anchors = anchors;
    _anchorStr = Utils.string(anchors, "\n");
  }

  @Override
  public String toString() {
    return _anchorStr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AnchorsConfig)) {
      return false;
    }
    AnchorsConfig that = (AnchorsConfig) o;
    return Objects.equals(_anchors, that._anchors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_anchors);
  }

  public Map<String, AnchorConfig> getAnchors() {
    return _anchors;
  }
}
