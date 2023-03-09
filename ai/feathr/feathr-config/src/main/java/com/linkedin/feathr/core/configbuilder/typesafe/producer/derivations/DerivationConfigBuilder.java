package com.linkedin.feathr.core.configbuilder.typesafe.producer.derivations;

import com.linkedin.feathr.core.configbuilder.typesafe.producer.common.FeatureTypeConfigBuilder;
import com.linkedin.feathr.core.config.producer.ExprType;
import com.linkedin.feathr.core.config.producer.TypedExpr;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;
import com.linkedin.feathr.core.config.producer.derivations.BaseFeatureConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfig;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExpr;
import com.linkedin.feathr.core.config.producer.derivations.DerivationConfigWithExtractor;
import com.linkedin.feathr.core.config.producer.derivations.KeyedFeature;
import com.linkedin.feathr.core.config.producer.derivations.SequentialJoinConfig;
import com.linkedin.feathr.core.config.producer.derivations.SimpleDerivationConfig;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.utils.ConfigUtils;
import com.linkedin.feathr.core.utils.Utils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.linkedin.feathr.core.config.producer.derivations.DerivationConfig.*;
import static com.linkedin.feathr.core.utils.Utils.*;


/**
 * Builds a feature derivation config object. It delegates the actual build task to its children
 * depending on the type of the feature derivation.
 */
class DerivationConfigBuilder {
  private final static Logger logger = LogManager.getLogger(DerivationConfigBuilder.class);

  private DerivationConfigBuilder() {
  }

  public static DerivationConfig build(String derivedFeatureName, Config derivationsConfig) {
    String quotedDerivedFeatureName = quote(derivedFeatureName);
    DerivationConfig configObj;
    ConfigValue value = derivationsConfig.getValue(quotedDerivedFeatureName);

    switch (value.valueType()) {
      case STRING:
        String expr = derivationsConfig.getString(quotedDerivedFeatureName);
        configObj = new SimpleDerivationConfig(new TypedExpr(expr, ExprType.MVEL));
        break;

      case OBJECT:
        Config derivCfg = derivationsConfig.getConfig(quotedDerivedFeatureName);

        if (derivCfg.hasPath(JOIN)) {
          configObj = buildWithJoin(derivedFeatureName, derivCfg);
        } else if (derivCfg.hasPath(CLASS)) {
          configObj = buildWithExtractor(derivedFeatureName, derivCfg);
        } else if (derivCfg.hasPath(INPUTS)) {
          configObj = buildWithExpr(derivedFeatureName, derivCfg);
        } else if (derivCfg.hasPath(SQL_EXPR)) {
          String sqlExpr = derivCfg.getString(SQL_EXPR);
          FeatureTypeConfig featureTypeConfig = FeatureTypeConfigBuilder.build(derivCfg);
          return new SimpleDerivationConfig(new TypedExpr(sqlExpr, ExprType.SQL), featureTypeConfig);
        } else if (derivCfg.hasPath(DEFINITION)) {
          String mvelExpr = derivCfg.getString(DEFINITION);
          FeatureTypeConfig featureTypeConfig = FeatureTypeConfigBuilder.build(derivCfg);
          return new SimpleDerivationConfig(new TypedExpr(mvelExpr, ExprType.MVEL), featureTypeConfig);
        } else {
          throw new ConfigBuilderException("Expected one of 'definition' or 'class' field in: " + value.render());
        }
        break;

      default:
        throw new ConfigBuilderException("Expected " + derivedFeatureName + " value type String or Object, got "
            + value.valueType());
    }

    logger.debug("Built DerivationConfig object for derived feature " + derivedFeatureName);

    return configObj;
  }

  /**
   * Builds a derived feature config object for derivations expressed with key and MVEL expression
   */
  private static DerivationConfigWithExpr buildWithExpr(String derivedFeatureName, Config derivationConfig) {
    List<String> key = getKey(derivationConfig);

    Config inputsConfig = derivationConfig.getConfig(INPUTS);
    ConfigObject inputsConfigObj = inputsConfig.root();
    Set<String> inputArgs = inputsConfigObj.keySet();

    Map<String, KeyedFeature> inputs = inputArgs.stream().collect(HashMap::new,
        (map, arg) -> {
          Config cfg = inputsConfig.getConfig(arg);
          String keyExprOfCfg = getKeyExpr(cfg);
          String inputFeature = cfg.getString(FEATURE);
          KeyedFeature keyedFeature = new KeyedFeature(keyExprOfCfg, inputFeature);
          map.put(arg, keyedFeature);
        }, HashMap::putAll);

    String defType = derivationConfig.hasPath(SQL_DEFINITION) ? SQL_DEFINITION : DEFINITION;
    ExprType defExprType = derivationConfig.hasPath(SQL_DEFINITION) ? ExprType.SQL : ExprType.MVEL;

    String definition = derivationConfig.getString(defType);

    FeatureTypeConfig featureTypeConfig = FeatureTypeConfigBuilder.build(derivationConfig);

    DerivationConfigWithExpr configObj = new DerivationConfigWithExpr(key, inputs, new TypedExpr(definition, defExprType), featureTypeConfig);
    logger.trace("Built DerivationConfigWithExpr object for derived feature " + derivedFeatureName);

    return configObj;
  }

  /**
   * Builds a derived feature config object for derivations expressed with a udf (extractor class)
   */
  private static DerivationConfigWithExtractor buildWithExtractor(String derivedFeatureName, Config derivationConfig) {
    List<String> key = getKey(derivationConfig);

    List<? extends Config> inputsConfigList = derivationConfig.getConfigList(INPUTS);

    List<KeyedFeature> inputs = inputsConfigList.stream().map(c -> new KeyedFeature(getKeyExpr(c), c.getString(FEATURE)))
        .collect(Collectors.toList());

    String name = derivationConfig.getString(CLASS);
    String className;
    if (SourceVersion.isName(name)) {
      className = name;
    } else {
      throw new ConfigBuilderException("Invalid name for extractor class: " + name);
    }

    FeatureTypeConfig featureTypeConfig = FeatureTypeConfigBuilder.build(derivationConfig);

    DerivationConfigWithExtractor configObj = new DerivationConfigWithExtractor(key, inputs, className, featureTypeConfig);
    logger.trace("Built DerivationConfigWithExtractor object for derived feature" + derivedFeatureName);

    return configObj;
  }

  /**
   * Builds a sequential join config, which is a special form of derived feature config
   */
  private static SequentialJoinConfig buildWithJoin(String sequentialJoinFeatureName, Config derivationConfig) {
    List<String> key = getKey(derivationConfig);

    Config joinConfig = derivationConfig.getConfig(JOIN);
    // there is only two configs in joinConfigList, one is base, the other is expansion
    ConfigObject joinConfigObj = joinConfig.root();
    Set<String> joinArgs = joinConfigObj.keySet();

    if (!joinArgs.contains(BASE) || !joinArgs.contains(EXPANSION) || joinArgs.size() != 2) {
      throw new ConfigBuilderException("Sequential join config should contains both base and expansion feature config, got"
          + Utils.string(joinArgs));
    }

    BaseFeatureConfig base = buildBaseFeatureConfig(joinConfig.getConfig(BASE));

    Config expansionCfg = joinConfig.getConfig(EXPANSION);
    String keyExprOfCfg = getKeyExpr(expansionCfg);
    String inputFeature = expansionCfg.getString(FEATURE);
    KeyedFeature expansion = new KeyedFeature(keyExprOfCfg, inputFeature);

    String aggregation = derivationConfig.getString(AGGREGATION);

    FeatureTypeConfig featureTypeConfig = FeatureTypeConfigBuilder.build(derivationConfig);

    SequentialJoinConfig configObj = new SequentialJoinConfig(key, base, expansion, aggregation, featureTypeConfig);
    logger.trace("Built SequentialJoinConfig object for sequential join feature" + sequentialJoinFeatureName);

    return configObj;
  }

  /**
   * Build the base feature config for sequential join feature
   */
  private static BaseFeatureConfig buildBaseFeatureConfig(Config baseConfig) {
    String keyExpr = getKeyExpr(baseConfig);
    String feature = baseConfig.getString(FEATURE);
    List<String> outputKey = baseConfig.hasPath(OUTPUT_KEY) ? getKey(baseConfig, OUTPUT_KEY) : null;
    String transformation = baseConfig.hasPath(TRANSFORMATION) ? baseConfig.getString(TRANSFORMATION) : null;
    String transformationClass = baseConfig.hasPath(TRANSFORMATION_CLASS) ? baseConfig.getString(TRANSFORMATION_CLASS) : null;
    if (transformation != null && transformationClass != null) {
      throw new ConfigBuilderException("Sequential join base feature config cannot have both transformation \""
          + transformation + "\" and transformationClass \"" + transformationClass + "\".");
    }
    return new BaseFeatureConfig(keyExpr, feature, outputKey, transformation, transformationClass);
  }

  /**
   * get list of keys from Config object
   * @param config the config
   * @param keyField the key field name, in derivation config, it can be either "key" or "outputKey"
   * @return the list of keys
   */
  private static List<String> getKey(Config config, String keyField) {
    ConfigValueType keyValueType = config.getValue(keyField).valueType();
    List<String> key;
    switch (keyValueType) {
      case STRING:
        key = Collections.singletonList(config.getString(keyField));
        break;
      case LIST:
        key = config.getStringList(keyField);
        break;
      default:
        throw new ConfigBuilderException("Expected key type String or List[String], got " + keyValueType);
    }
    return key;
  }

  /**
   * Get list of keys from Config object, by default(in most cases), the key field name is "key"
   */
  private static List<String> getKey(Config config) {
    return getKey(config, KEY);
  }

  private static String getKeyExpr(Config config) {
    return ConfigUtils.getHoconString(config, KEY);
  }
}
