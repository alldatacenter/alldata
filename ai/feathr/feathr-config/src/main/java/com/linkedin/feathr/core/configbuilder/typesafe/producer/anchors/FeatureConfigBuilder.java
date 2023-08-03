package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.producer.anchors.ExtractorBasedFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.FeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.SimpleFeatureConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.utils.Utils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.feathr.core.config.producer.anchors.FeatureConfig.*;
import static com.linkedin.feathr.core.utils.Utils.*;


/**
 * Builds FeatureConfig objects, specifically a Map of feature names to FeatureConfig objects in a
 * single anchor
 */
class FeatureConfigBuilder {
  private final static Logger logger = LoggerFactory.getLogger(FeatureConfigBuilder.class);

  private FeatureConfigBuilder() {
  }

  public static Map<String, FeatureConfig> build(Config featuresConfig) {
    logger.debug("Building FeatureConfig object for featuresConfig " + featuresConfig);

    ConfigObject featuresConfigObj = featuresConfig.root();
    Set<String> featureNames = featuresConfigObj.keySet();
    logger.trace("Found feature names:" + Utils.string(featureNames));

    Map<String, FeatureConfig> configObjMap = featureNames.stream()
        .collect(Collectors.toMap(Function.identity(), fName -> FeatureConfigBuilder.build(featuresConfig, fName)));

    logger.debug("Built all FeatureConfig objects");

    return configObjMap;
  }

  public static Map<String, FeatureConfig> build(List<String> featureNames) {
    logger.debug("Building FeatureConfig objects for features " + Utils.string(featureNames));

    Map<String, FeatureConfig> configObjMap = featureNames.stream().
        collect(Collectors.toMap(Function.identity(), ExtractorBasedFeatureConfig::new));

    logger.debug("Built all FeatureConfig objects");

    return configObjMap;
  }

  /**
   * Builds a single FeatureConfig object from the enclosing featuresConfig object. The actual build is delegated
   * to a child builder depending on the type of the feature - simple (built in this method), complex, or
   * time-window feature.
   *
   * featuresConfig refers to the object part of:
   *
   * {@code features : { <feature definition> ...} }
   *
   * The features may be specified in three ways as shown below:
   * <pre>
   * {@code
   *   features: {
   *     <feature name>: {
   *       def: <feature expression>
   *       type: <feature type>
   *       default: <default value>
   *     }
   *     ...
   *   }
   *
   *   features: {
   *     <feature name>: <feature expression>,
   *     ...
   *   }
   *
   *   features: {
   *     <feature name>: {
   *       def: <column name>                // the column/field on which the aggregation will be computed.
   *                                         // Could be specified as a Spark column expression.
   *                                         // for TIMESINCE feature, it should be left as an empty string.
   *       aggregation: <aggregation type>   // one of 5 aggregation types: SUM, COUNT, MAX, TIMESINCE, AVG
   *       window: <length of window time>   // support 4 type of units: d(day), h(hour), m(minute), s(second).
   *                                         // The example value are "7d' or "5h" or "3m" or "1s"
   *       filter: <string>                  // (Optional) a Spark SQL expression for filtering the fact data before aggregation.
   *       groupBy: <column name>            // (Optional) the column/field on which the data will be grouped by before aggregation.
   *       limit: <int>                      // (Optional) a number specifying for each group, taking the records with the TOP k aggregation value.
   *     }
   *     ...
   *   }
   * }
   * </pre>
   */

  private static FeatureConfig build(Config featuresConfig, String featureName) {
    String quotedFeatureName = quote(featureName);
    ConfigValue configValue = featuresConfig.getValue(quotedFeatureName);
    ConfigValueType configValueType = configValue.valueType();
    FeatureConfig configObj;

    switch (configValueType) {
      case STRING:
        String featureExpr = featuresConfig.getString(quotedFeatureName);
        configObj = new ExtractorBasedFeatureConfig(featureExpr);
        logger.trace("Built ExtractorBasedFeatureConfig object for feature " + featureName);
        break;

      case OBJECT:
        Config featureCfg = featuresConfig.getConfig(quotedFeatureName);
        if (featuresConfig.hasPath(quotedFeatureName + "." + WINDOW) || featuresConfig.hasPath(quotedFeatureName + "." + WINDOW_PARAMETERS)) {
          configObj = TimeWindowFeatureConfigBuilder.build(featureName, featureCfg);
        } else if (featureCfg.hasPath(DEF_SQL_EXPR) || featureCfg.hasPath(DEF)) {
          configObj = ExpressionBasedFeatureConfigBuilder.build(featureName, featureCfg);
        } else {
          // An ExtractorBased feature config with type, default value information, and optional parameters
          configObj = ExtractorBasedFeatureConfigBuilder.build(featureName, featureCfg);
        }
        break;

      default:
        throw new ConfigBuilderException("Expected " + featureName + " value type String or Object, got " + configValueType);
    }

    logger.debug("Built FeatureConfig object for feature " + featureName);

    return configObj;
  }
}
