package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import java.util.Map;


/**
 * validator specific for Frame feature producer clients
 */
public class FeatureProducerConfValidator extends TypesafeConfigValidator {

  /**
   * validate each config in Frame feature producer MPs
   *
   * @see ConfigValidator#validate(Map, ValidationType)
   */
  @Override
  public Map<ConfigType, ValidationResult> validate(Map<ConfigType, ConfigDataProvider> configTypeWithDataProvider,
      ValidationType validationType) {

    // feature producer MP should not have join config
    if (configTypeWithDataProvider.containsKey(ConfigType.Join)) {
      String errMsg = "Found Join config provided for config validation in feature producer MP.";
      throw new RuntimeException(errMsg);
    }

    return super.validate(configTypeWithDataProvider, validationType);
  }

  /**
   * Validates FeatureDef config semantically
   * @param featureDefConfig {@link FeatureDefConfig}
   * @return {@link ValidationResult}
   */
  @Override
  public ValidationResult validateSemantics(FeatureDefConfig featureDefConfig) {
    return new FeatureDefConfigSemanticValidator().validate(featureDefConfig);
  }

}
