package com.linkedin.feathr.core.configvalidator;

import com.linkedin.feathr.core.configvalidator.typesafe.FeatureConsumerConfValidator;
import com.linkedin.feathr.core.configvalidator.typesafe.FeatureProducerConfValidator;
import com.linkedin.feathr.core.configvalidator.typesafe.TypesafeConfigValidator;
import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import java.util.Map;


/**
 * Validates Frame configuration such as FeatureDef config, Join config, etc. Provides capability to perform both
 * syntactic and semantic validations.
 */
public interface ConfigValidator {

  /**
   * Validates the configuration. Configuration type is provided by {@link ConfigType}, the validation to be performed
   * (for example, syntactic) is provided by {@link ValidationType}, and the configuration to be validated is provided
   * by {@link ConfigDataProvider}. Note that the client is responsible for closing the ConfigDataProvider resource.
   * @param configType ConfigType
   * @param validationType ValidationType
   * @param configDataProvider ConfigDataProvider
   * @return {@link ValidationResult}
   * @throws ConfigValidationException if validation can't be performed
   */
  ValidationResult validate(ConfigType configType, ValidationType validationType,
      ConfigDataProvider configDataProvider);

  /**
   * Validates multiple Frame configuration types individually. Note that the client is responsible for closing the
   * ConfigDataProvider resources.
   * @param configTypeWithDataProvider Provides a K-V pair of {@link ConfigType} and {@link ConfigDataProvider}
   * @param validationType The validation to be performed {@link ValidationType}
   * @return Map of ConfigType and the {@link ValidationResult}
   * @throws ConfigValidationException if validation can't be performed
   */
  Map<ConfigType, ValidationResult> validate(Map<ConfigType, ConfigDataProvider> configTypeWithDataProvider,
      ValidationType validationType);

  /**
   * Factory method to get an instance of ConfigValidator
   * @return an instance of ConfigValidator
   * @deprecated please use {{@link #getInstance(ClientType)}} instead
   */
  @Deprecated
  static ConfigValidator getInstance() {
    return new TypesafeConfigValidator();
  }

  /**
   * Factory method to get an instance of ConfigValidator
   * @param clientType the Frame client type {@link ClientType}
   * @return an instance of ConfigValidator
   */
  static ConfigValidator getInstance(ClientType clientType) {
    switch (clientType) {
      case FEATURE_PRODUCER:
        return new FeatureProducerConfValidator();
      case FEATURE_CONSUMER:
        return new FeatureConsumerConfValidator();
      default:
        throw new UnsupportedOperationException("Frame client type not support: " + clientType.toString());
    }
  }
}
