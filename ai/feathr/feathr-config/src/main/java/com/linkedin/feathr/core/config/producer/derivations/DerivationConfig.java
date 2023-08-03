package com.linkedin.feathr.core.config.producer.derivations;

import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.config.producer.common.FeatureTypeConfig;

import java.util.Optional;


/**
 * Represents the fields used for specifying the configuration parameters for feature derivations in the derivations
 * section of the FeatureDef config file.
 */
public interface DerivationConfig extends ConfigObj {
  String KEY = "key";
  String INPUTS = "inputs";
  String FEATURE = "feature";
  String DEFINITION = "definition";
  String CLASS = "class";
  String JOIN = "join"; // join field for sequential join config
  String BASE = "base"; // base feature for sequential join config
  String EXPANSION = "expansion"; // expansion feature for sequential join config
  String AGGREGATION = "aggregation"; // aggregation field for sequential join config
  String OUTPUT_KEY = "outputKey"; // outputKey field for base feature in sequential join config
  String TRANSFORMATION = "transformation"; // transformation field for base feature in sequential join config
  String TRANSFORMATION_CLASS = "transformationClass"; // transformationClass field for base feature in sequential join config
  String SQL_EXPR = "sqlExpr"; // sqlExpr field for simple derivation config with SQL expression
  String SQL_DEFINITION = "definition.sqlExpr"; // sqlExpr field for derivation config with SQL definition\
  String TYPE = "type";

  Optional<FeatureTypeConfig> getFeatureTypeConfig();
}
