package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.configbuilder.typesafe.TypesafeConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.ResourceConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.StringConfigDataProvider;
import com.linkedin.feathr.core.configvalidator.ConfigValidatorFixture;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.ValidationStatus;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Test class for {@link FeatureConsumerConfValidator}
 */
public class FeatureConsumerConfValidatorTest {
  private FeatureConsumerConfValidator _featureConsumerConfValidator = new FeatureConsumerConfValidator();
  private TypesafeConfigBuilder _configBuilder = new TypesafeConfigBuilder();

  @Test(description = "test validation for Frame feature consumer")
  public void testRequestUnreachableFeatures() {
    try {
      Map<ConfigType, ConfigDataProvider> configs = new HashMap<>();
      configs.put(ConfigType.FeatureDef, new ResourceConfigDataProvider("invalidSemanticsConfig/feature-not-reachable-def.conf"));
      configs.put(ConfigType.Join, new StringConfigDataProvider(ConfigValidatorFixture.joinConfig1));

      // perform syntax validation
      Map<ConfigType, ValidationResult> syntaxResult = _featureConsumerConfValidator.validate(configs, ValidationType.SYNTACTIC);
      ValidationResult featureDefSyntaxResult = syntaxResult.get(ConfigType.FeatureDef);
      Assert.assertEquals(featureDefSyntaxResult.getValidationStatus(), ValidationStatus.VALID);
      ValidationResult joinSyntaxResult = syntaxResult.get(ConfigType.Join);
      Assert.assertEquals(joinSyntaxResult.getValidationStatus(), ValidationStatus.VALID);

      // perform semantic validation
      Map<ConfigType, ValidationResult> semanticResult = _featureConsumerConfValidator.validate(configs, ValidationType.SEMANTIC);
      ValidationResult featureDefSemanticResult = semanticResult.get(ConfigType.FeatureDef);
      Assert.assertEquals(featureDefSemanticResult.getValidationStatus(), ValidationStatus.WARN);
      ValidationResult joinSemanticResult = semanticResult.get(ConfigType.Join);
      Assert.assertEquals(joinSemanticResult.getValidationStatus(), ValidationStatus.INVALID);

    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }
}
