package com.linkedin.feathr.core.configvalidator;

import com.linkedin.feathr.core.configvalidator.typesafe.FeatureConsumerConfValidator;
import com.linkedin.feathr.core.configvalidator.typesafe.FeatureProducerConfValidator;


/**
 * Factory class for {@link ConfigValidator} to replace the usage of the static method of
 * {@link ConfigValidator#getInstance(ClientType clientType)}
 * Since the above getInstance method is used in li-frame-plugin, which is written in Groovy.
 * And Groovy has a known bug to not fully support calling static method with parameters (introduced in Java 8).
 * One discussion can be found here:
 * https://community.smartbear.com/t5/SoapUI-Pro/ERROR-groovy-lang-MissingMethodException-No-signature-of-method/td-p/187960
 */
public class ConfigValidatorFactory {

  private static ConfigValidatorFactory _instance = new ConfigValidatorFactory();

  // Singleton with static factory
  private ConfigValidatorFactory() {

  }

  /**
   * get singleton instance
   */
  public static ConfigValidatorFactory getFactoryInstance() {
    return _instance;
  }

  /**
   * to get an instance of ConfigValidator
   * @param clientType the Frame client type {@link ClientType}
   * @return an instance of ConfigValidator
   */
  public ConfigValidator getValidatorInstance(ClientType clientType) {
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
