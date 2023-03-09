package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.utils.Utils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;


/**
 * Represents the anchor definition (the object part) for the anchors that have the extractor specified (in lieu of the
 * key).
 * The features may be specified in two ways as shown below,
 *   where the keyExtractor and (keyAlias and/or key) fields are mutually exclusive.
 * If using keyAlias or keys, the extractor can only be of AnchorExtractor type.
 * If using keyExtractor, the extractor can only be of SimpleAnchorExtractorSpark or GenericAnchorExtractorSpark.
 * <pre>
 *{@code
 * <anchor name 1>: {
 *   source: <source name>
 *   keyExtractor: <full extractor class name>
 *   extractor: <full extractor class name>
 *   features: {
 *     <feature name 1> : {
 *       default: <default value>
 *     },
 *     <feature name 2>: {
 *       default: <default value>
 *     },
 *     ...
 *   }
 * }
 *}
 * </pre>
 *
 * A concise format when there is no default value defined for each feature on this anchor
 * <pre>
 * {@code
 * <anchor name 2>: {
 *   source: <source name 2>
 *   keyExtractor: <full extractor class name>
 *   extractor: <full extractor class name 2>
 *   features: [
 *     <feature name 1>,
 *     <feature name 2>,
 *     ...
 *   ]
 * }
 *}
 *</pre>
 *
 * One example of using keyAlias
 * <pre>
 * {@code
 * <anchor name 2>: {
 *   source: <source name 2>
 *   key: <key list>
 *   keyAlias: <key alias list>
 *   extractor: <full extractor class name 2>
 *   features: [
 *     <feature name 1>,
 *     <feature name 2>,
 *     ...
 *   ]
 * }
 *}
 *</pre>
 *
 * @author djaising
 * @author cesun
 */
public class AnchorConfigWithExtractor extends AnchorConfig {
  private final Optional<String> _keyExtractor;
  private final Optional<List<String>> _keyAlias;
  private final Optional<TypedKey> _typedKey;
  private final String _extractor;
  private String _configStr;

  /**
   * Constructor
   * @param source Source name (defined in sources section) or HDFS/Dali path
   * @param keyExtractor name of Java class that is used to extract the key(s)
   * @param typedKey the {@link TypedKey} object
   * @param keyAlias list of key alias
   * @param extractor Name of Java class that is used to extract the feature(s)
   * @param features Map of feature names to {@link FeatureConfig} object
   */
  public AnchorConfigWithExtractor(String source, String keyExtractor, TypedKey typedKey,
                                   List<String> keyAlias, String extractor, Map<String, FeatureConfig> features) {
    super(source, features);
    _keyExtractor = Optional.ofNullable(keyExtractor);
    _keyAlias = Optional.ofNullable(keyAlias);
    _typedKey = Optional.ofNullable(typedKey);
    _extractor = extractor;
  }

  /**
   * Constructor
   * @param source Source name (defined in sources section) or HDFS/Dali path
   * @param keyExtractor name of Java class that is used to extract the key(s)
   * @param extractor Name of Java class that is used to extract the feature(s)
   * @param features Map of feature names to {@link FeatureConfig} object
   */
  public AnchorConfigWithExtractor(String source, String keyExtractor, String extractor,
                                   Map<String, FeatureConfig> features) {
    this(source, keyExtractor, null, null, extractor, features);
  }
  /**
   * Constructor
   * @param source Source name (defined in sources section) or HDFS/Dali path
   * @param extractor Name of Java class that is used to extract the feature(s)
   * @param features Map of feature names to {@link FeatureConfig} object
   */
  public AnchorConfigWithExtractor(String source, String extractor, Map<String, FeatureConfig> features) {
    this(source, null, null, null, extractor, features);
  }

  public Optional<String> getKeyExtractor() {
    return _keyExtractor;
  }

  public Optional<List<String>> getKeyAlias() {
    return _keyAlias;
  }

  public Optional<TypedKey> getTypedKey() {
    return _typedKey;
  }

  public String getExtractor() {
    return _extractor;
  }

  @Override
  public String toString() {
    if (_configStr == null) {
      StringJoiner stringJoiner = new StringJoiner("\n");

      stringJoiner.add(String.join(": ", SOURCE, getSource()))
          .add(String.join(": ", EXTRACTOR, getExtractor()))
          .add(FEATURES + ":{\n" + Utils.string(getFeatures()) + "\n}");

      _keyExtractor.ifPresent(ke -> stringJoiner.add(String.join(": ", KEY_EXTRACTOR, ke)));
      _keyAlias.ifPresent(ka -> stringJoiner.add(String.join(": ", KEY_ALIAS, Utils.string(ka))));
      _typedKey.ifPresent(tk -> stringJoiner.add(_typedKey.toString()));

      _configStr = stringJoiner.toString();
    }

    return _configStr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AnchorConfigWithExtractor)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    AnchorConfigWithExtractor that = (AnchorConfigWithExtractor) o;
    return Objects.equals(_extractor, that._extractor)
        && Objects.equals(_keyAlias, that._keyAlias)
        && Objects.equals(_typedKey, that._typedKey)
        && Objects.equals(_keyExtractor, that._keyExtractor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _extractor, _keyAlias, _typedKey, _keyExtractor);
  }
}
