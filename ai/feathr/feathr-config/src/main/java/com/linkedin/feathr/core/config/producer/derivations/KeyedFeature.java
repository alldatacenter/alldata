package com.linkedin.feathr.core.config.producer.derivations;

import com.linkedin.feathr.core.config.producer.common.KeyListExtractor;
import java.util.List;
import java.util.Objects;

import static com.linkedin.feathr.core.config.producer.derivations.DerivationConfig.*;


/**
 * A tuple that specifies the key (single or composite) associated with a feature
 *
 * @author djaising
 * @author cesun
 */
public class KeyedFeature {
  private final String _rawKeyExpr;
  private final List<String> _key;
  private final String _feature;

  private String _configStr;

  /**
   * Constructor.
   * During construction, the input raw key expression will be extracted to a list of key String.
   * For instance:
   * - "x" will be converted to list ["x"].
   * - "[\"key1\", \"key2\"]" will be converted to list ["key1", "key2"]
   * - "[key1, key2]" will be converted to ["key1", "key2"] also
   *
   * @param rawKeyExpr the raw key expression
   * @param feature The name of the feature
   */
  public KeyedFeature(String rawKeyExpr, String feature) {
    _rawKeyExpr = rawKeyExpr;
    // For now, we only support HOCON String format as the raw key expression
    _key = KeyListExtractor.getInstance().extractFromHocon(rawKeyExpr);
    _feature = feature;

    StringBuilder sb = new StringBuilder();
    sb.append(KEY).append(": ").append(rawKeyExpr).append(", ")
        .append(FEATURE).append(": ").append(feature);
    _configStr = sb.toString();
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
    if (!(o instanceof KeyedFeature)) {
      return false;
    }
    KeyedFeature that = (KeyedFeature) o;
    /*
     * Using the HOCON expression is too strict to check equality. For instance
     * The following three key expressions:
     *
     * key1: [
     *       # String: 3
     *       "key1",
     *       # String: 3
     *       "key2"
     *  ]
     *
     * key2: [key1, key2]
     *
     * key3: ["key1", "key2"]
     *
     * All have the same meaning, it is misleading,
     *  and sometimes impossible (e.g. in unit tests) to distinguish between these.
     * And we should not distinguish them given that we've already parsed them using HOCON API in frame-core.
     *
     * Instead, we use the parsed key list to check the equality.
     */
    return Objects.equals(_key, that._key) && Objects.equals(_feature, that._feature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_rawKeyExpr, _key, _feature);
  }

  public String getRawKeyExpr() {
    return _rawKeyExpr;
  }

  /**
   * Get the list of key String extracted from raw key expression
   */
  public List<String> getKey() {
    return _key;
  }

  public String getFeature() {
    return _feature;
  }
}
