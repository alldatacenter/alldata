package com.linkedin.feathr.core.configbuilder.typesafe.producer.anchors;

import com.linkedin.feathr.core.config.TimeWindowAggregationType;
import com.linkedin.feathr.core.config.WindowType;
import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.TypedExpr;
import com.linkedin.feathr.core.config.producer.anchors.TimeWindowFeatureConfig;
import com.linkedin.feathr.core.config.producer.anchors.WindowParametersConfig;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.configbuilder.typesafe.producer.common.FeatureTypeConfigBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueType;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.anchors.FeatureConfig.*;


/**
 * Build {@link TimeWindowFeatureConfig} object
 */
class TimeWindowFeatureConfigBuilder {
  private final static Logger logger = LogManager.getLogger(FeatureConfigBuilder.class);

  private TimeWindowFeatureConfigBuilder() {
  }

  public static TimeWindowFeatureConfig build(String featureName, Config featureConfig) {

    // nearline features can use DEF_MVEL to denote def mvel expression
    String defType = featureConfig.hasPath(DEF_MVEL) ? DEF_MVEL : DEF;
    ExprType defExprType = featureConfig.hasPath(DEF_MVEL) ? ExprType.MVEL : ExprType.SQL;
    String columnExpr = featureConfig.getString(defType);

    String aggregationStr = featureConfig.getString(AGGREGATION);
    TimeWindowAggregationType aggregation = TimeWindowAggregationType.valueOf(aggregationStr);

    // if window_parameters exists it represents a nearline feature, else if window exists it is an offline feature.
    WindowParametersConfig windowParameters = null;
    if (featureConfig.hasPath(WINDOW_PARAMETERS)) {
      Config windowsParametersConfig = featureConfig.getConfig(WINDOW_PARAMETERS);
      windowParameters = WindowParametersConfigBuilder.build(windowsParametersConfig);
    } else if (featureConfig.hasPath(WINDOW)) {
      WindowType type = WindowType.SLIDING;
      Duration window = featureConfig.getDuration(WINDOW);
      if (window.getSeconds() <= 0) {
        String errMsg = WINDOW + " field must be in units of seconds, minutes, hours or days, and must be > 0. Refer to "
            + "https://github.com/lightbend/config/blob/master/HOCON.md#duration-format for supported unit strings.";
        throw new ConfigBuilderException(errMsg);
      }

      // Offline case - We take the window and slidingInterval values and convert it to represent a sliding window parameters config.
      // slidingInterval is null for offline.
      windowParameters = new WindowParametersConfig(type, window, null);

    }

    // nearline features can use FILTER_MVEL to denote mvel filter expression
    TypedExpr typedFilter = null;
    if (featureConfig.hasPath(FILTER_MVEL) || featureConfig.hasPath(FILTER)) {
      ExprType filterExprType = featureConfig.hasPath(FILTER_MVEL) ? ExprType.MVEL : ExprType.SQL;
      String filterType = featureConfig.getValue(FILTER).valueType() == ConfigValueType.OBJECT ? FILTER_MVEL : FILTER;
      String filter = featureConfig.getString(filterType);
      typedFilter = new TypedExpr(filter, filterExprType);
    }

    String groupBy = getString(featureConfig, GROUPBY);

    Integer limit = getInt(featureConfig, LIMIT);

    String decay = getString(featureConfig, DECAY);

    String weight = getString(featureConfig, WEIGHT);

    Integer embeddingSize = getInt(featureConfig, EMBEDDING_SIZE);

    FeatureTypeConfig featureTypeConfig = FeatureTypeConfigBuilder.build(featureConfig);

    String defaultValue = featureConfig.hasPath(DEFAULT) ? featureConfig.getValue(DEFAULT).unwrapped().toString() : null;

    TimeWindowFeatureConfig configObj = new TimeWindowFeatureConfig(new TypedExpr(columnExpr, defExprType), aggregation,
        windowParameters, typedFilter, groupBy, limit, decay, weight, embeddingSize, featureTypeConfig, defaultValue);
    logger.trace("Built TimeWindowFeatureConfig object for feature: " + featureName);

    return configObj;
  }

  private static String getString(Config featureConfig, String key) {
    return featureConfig.hasPath(key) ? featureConfig.getString(key) : null;
  }

  private static Integer getInt(Config featureConfig, String key) {
    return featureConfig.hasPath(key) ? featureConfig.getInt(key) : null;
  }
}
