package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.utils.Utils;
import java.util.Map;


/**
 * Represents the anchor definition (the object part) for the anchors that have neither the key nor the extractor
 * specified.
 *
 * @author djaising
 * @author cesun
 */
// TODO: This seems to be valid only for online anchors. Verify.
public class AnchorConfigWithOnlyMvel extends AnchorConfig {

  private String _configStr;

  /**
   * Constructor
   * @param source Source name as defined in the sources section
   * @param features Map of feature names to {@link FeatureConfig}
   */
  public AnchorConfigWithOnlyMvel(String source, Map<String, FeatureConfig> features) {
    super(source, features);

    StringBuilder sb = new StringBuilder();
    sb.append(SOURCE).append(": ").append(source).append("\n")
        .append(FEATURES).append(": ").append(Utils.string(features)).append("\n");
    _configStr = sb.toString();
  }

  @Override
  public String toString() {
    return _configStr;
  }
}
