package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.utils.Utils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents the anchor definition (the object part) for the anchors that have the key specified.
 * The anchors may be specified in the following ways:
 *
 * In the following, the fields {@code type} and {@code default} are optional.
 *
 * <pre>
 * {@code
 * <anchor name 1>: {
 *   source: <source name>
 *   key: <key expression>
 *   keyAlias: <key alias expression>
 *   features: {
 *     <feature name>: {
 *       def: <feature expression>,
 *       type: <feature type>,
 *       default: <default value>
 *     }
 *     ...
 *   }
 * }
 *
 * <anchor name 2>: {
 *   source: <source name 2>
 *   key: <key expression 2>
 *   keyAlias: <key alias expression>
 *   features: {
 *     <feature name>: <feature expression>,
 *     ...
 *   }
 * }
 * }
 *</pre>
 *
 *
 * In the following, the fields {@code key.sqlExpr} and {@code def.sqlExpr} should be used simultaneously.
 * The fields {@code type} and {@code default} are optional.
 *
 * <pre>
 * {@code
 * <anchor name 3>: {
 *   source: <source name>
 *   key.sqlExpr: <key expression>
 *   keyAlias: <key alias expression>
 *   features: {
 *     <feature name>: {
 *       def.sqlExpr: <feature expression>,
 *       type: <feature type>,
 *       default: <default value>
 *     }
 *     ...
 *   }
 * }
 * }
 *</pre>
 *
 * In the following, the fields 'lateralViewParameters', 'filter', 'groupBy' and 'limit' are optional.
 * Further, within 'lateralViewParameters', 'lateralViewFilter' is optional as well.
 * <pre>
 * {@code
 * <anchor name 4>: {
 *    source: <source name>
 *    key: <key field>
 *    keyAlias: <key alias expression>
 *    lateralViewParameters: {
 *      lateralViewDef: <view def expr>
 *      lateralViewItemAlias: <item alias>
 *      lateralViewFilter: <filter expr>
 *    }
 *    features: {
 *      <feature name>: {
 *        def: <column name>
 *        aggregation: <aggregation type>
 *        window: <length of window time>
 *        filter: <string>
 *        groupBy: <column name>
 *        limit: <int>
 *      }
 *    }
 * }
 * }
 *</pre>
 */
public final class AnchorConfigWithKey extends AnchorConfig {
  private final TypedKey _typedKey;
  private final Optional<List<String>> _keyAlias;
  private final Optional<LateralViewParams> _lateralViewParams;
  private String _configStr;

  /**
   * Constructor
   * @param source source name (defined in sources section) or HDFS/Dali path
   * @param typedKey the {@link TypedKey} object
   * @param keyAlias the list of key alias
   * @param lateralViewParams {@link LateralViewParams} object
   * @param features Map of feature names to {@link FeatureConfig}
   */
  public AnchorConfigWithKey(String source, TypedKey typedKey, List<String> keyAlias,
                             LateralViewParams lateralViewParams, Map<String, FeatureConfig> features) {
    super(source, features);
    _typedKey = typedKey;
    _keyAlias = Optional.ofNullable(keyAlias);
    _lateralViewParams = Optional.ofNullable(lateralViewParams);
  }

  /**
   * Constructor
   * @param source source name (defined in sources section) or HDFS/Dali path
   * @param typedKey the {@link TypedKey} object
   * @param lateralViewParams {@link LateralViewParams} object
   * @param features Map of feature names to {@link FeatureConfig}
   */
  public AnchorConfigWithKey(String source, TypedKey typedKey, LateralViewParams lateralViewParams,
      Map<String, FeatureConfig> features) {
    this(source, typedKey, null, lateralViewParams, features);
  }

  public List<String> getKey() {
    return _typedKey.getKey();
  }

  public TypedKey getTypedKey() {
    return _typedKey;
  }

  public Optional<List<String>> getKeyAlias() {
    return _keyAlias;
  }

  public Optional<LateralViewParams> getLateralViewParams() {
    return _lateralViewParams;
  }

  @Override
  public String toString() {
    if (_configStr == null) {
      _configStr = String.join("\n",
          String.join(": ", SOURCE, getSource()),
          _typedKey.toString(),
          FEATURES + ":{\n" + Utils.string(getFeatures()) + "\n}");

      _keyAlias.ifPresent(ka -> _configStr = String.join("\n", _configStr,
          String.join(": ", KEY_ALIAS, Utils.string(ka))));

      _lateralViewParams.ifPresent(lvp -> _configStr = String.join("\n", _configStr,
          LATERAL_VIEW_PARAMS + ": {\n" + lvp + "\n}"));
    }

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
    if (!super.equals(o)) {
      return false;
    }
    AnchorConfigWithKey that = (AnchorConfigWithKey) o;

    return Objects.equals(_typedKey, that._typedKey)
        && Objects.equals(_keyAlias, that._keyAlias)
        && Objects.equals(_lateralViewParams, that._lateralViewParams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _typedKey, _keyAlias, _lateralViewParams);
  }
}
