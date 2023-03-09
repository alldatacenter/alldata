package com.linkedin.feathr.core.config.producer.anchors;

import com.linkedin.feathr.core.utils.Utils;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


/**
 * Represents the anchor definition (the object part) for the anchors that have ONLY keyExtractor specified.
 * It is mutually exclusive with {@link AnchorConfigWithExtractor}
 * The anchors may be specified in the following ways:
 *
 * In the following, the fields {@code lateralViewParameters}, {@code type}, and {@code default} are optional.
 *
 * <pre>
 * {@code
 * <anchor name 1>: {
 *   source: <source name>
 *   keyExtractor: <full extractor class name>
 *    lateralViewParameters: {
 *      lateralViewDef: <view def expr>
 *      lateralViewItemAlias: <item alias>
 *      lateralViewFilter: <filter expr>
 *    }
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
 *   keyExtractor: <full extractor class name>
 *   features: {
 *     <feature name>: <feature expression>,
 *     ...
 *   }
 * }
 * }
 *</pre>
 *
 *
 * <pre>
 * {@code
 * <anchor name 3>: {
 *   source: <source name>
 *   keyExtractor: <full extractor class name>
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
 */
public final class AnchorConfigWithKeyExtractor extends AnchorConfig {
  private final String _keyExtractor;
  private final Optional<LateralViewParams> _lateralViewParams;
  private String _configStr;

  /**
   * Constructor
   * @param source source name (defined in sources section) or HDFS/Dali path
   * @param keyExtractor entity id
   * @param features Map of feature names to {@link FeatureConfig}
   * @param lateralViewParams {@link LateralViewParams} object
   */
  public AnchorConfigWithKeyExtractor(String source, String keyExtractor, Map<String, FeatureConfig> features, LateralViewParams lateralViewParams) {
    super(source, features);
    _keyExtractor = keyExtractor;
    _lateralViewParams = Optional.ofNullable(lateralViewParams);
  }

  /**
   * Constructor
   * @param source source name (defined in sources section) or HDFS/Dali path
   * @param keyExtractor entity id
   * @param features Map of feature names to {@link FeatureConfig}
   */
  public AnchorConfigWithKeyExtractor(String source, String keyExtractor, Map<String, FeatureConfig> features) {
    this(source, keyExtractor, features, null);
  }

  public String getKeyExtractor() {
    return _keyExtractor;
  }

  public Optional<LateralViewParams> getLateralViewParams() {
    return _lateralViewParams;
  }

  @Override
  public String toString() {
    if (_configStr == null) {
      _configStr = String.join("\n",
          String.join(": ", SOURCE, getSource()),
          String.join(": ", KEY_EXTRACTOR, getKeyExtractor()),
          FEATURES + ":{\n" + Utils.string(getFeatures()) + "\n}");

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
    AnchorConfigWithKeyExtractor that = (AnchorConfigWithKeyExtractor) o;
    return Objects.equals(_keyExtractor, that._keyExtractor) && Objects.equals(_lateralViewParams, that._lateralViewParams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), _keyExtractor, _lateralViewParams);
  }
}
