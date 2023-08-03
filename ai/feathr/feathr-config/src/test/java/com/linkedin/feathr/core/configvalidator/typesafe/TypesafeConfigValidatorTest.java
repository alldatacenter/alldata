package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.configvalidator.ConfigValidator;
import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.configbuilder.typesafe.TypesafeConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.StringConfigDataProvider;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.ValidationStatus;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import com.typesafe.config.Config;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.config.ConfigType.*;
import static com.linkedin.feathr.core.configvalidator.ConfigValidatorFixture.*;
import static com.linkedin.feathr.core.configvalidator.ValidationStatus.*;
import static com.linkedin.feathr.core.configvalidator.ValidationType.*;
import static org.testng.Assert.*;


/**
 * Unit tests for {@link TypesafeConfigValidator}. Tests are provided for only those methods that are public but not
 * provided as part of {@link ConfigValidator ConfigValidator}.
 */
public class TypesafeConfigValidatorTest {
  private TypesafeConfigValidator _validator;

  @BeforeClass
  public void init() {
    _validator = new TypesafeConfigValidator();
  }

  @Test(description = "Tests validation of FeatureDef config syntax")
  public void testFeatureDefConfigSyntax() {
    ValidationResult expResult = new ValidationResult(SYNTACTIC, VALID);
    runAndValidate(FeatureDef, validFeatureDefConfig, expResult);
  }

  @Test(description = "Tests validation of Join config syntax")
  public void testJoinConfigSyntax() {
    ValidationResult expResult = new ValidationResult(SYNTACTIC, VALID);
    runAndValidate(Join, validJoinConfigWithSingleFeatureBag, expResult);
  }

  @Test(description = "Test validation of FeatureDef naming validation")
  public void testNamingValidation() {
    ConfigDataProvider cdp = new StringConfigDataProvider(invalidFeatureDefConfig2);
    ValidationResult obsResult = _validator.validate(FeatureDef, SYNTACTIC, cdp);

    assertEquals(obsResult.getValidationStatus(), WARN);
    assertNotNull(obsResult.getDetails().orElse(null));
  }

  @Test(description = "Tests validation of Presentation config syntax")
  public void testPresentationConfigSyntax() {
    ValidationResult expResult = new ValidationResult(SYNTACTIC, VALID);
    runAndValidate(Presentation, validPresentationConfig, expResult);
  }

  @Test(description = "Test validation of anchors with parameters")
  public void testValidParameterizedAnchorConfig() {
    ValidationResult expResult = new ValidationResult(SYNTACTIC, VALID);
    runAndValidate(FeatureDef, validFeatureDefConfigWithParameters, expResult);
  }

  private void runAndValidate(ConfigType configType, String configStr, ValidationResult expResult) {
    try (ConfigDataProvider cdp = new StringConfigDataProvider(configStr)) {
      TypesafeConfigBuilder builder = new TypesafeConfigBuilder();
      Config config = builder.buildTypesafeConfig(configType, cdp);
      ValidationResult obsResult = _validator.validateSyntax(configType, config);

      assertEquals(obsResult, expResult);
    } catch (Exception e) {
      fail("Caught exception: " + e.getMessage(), e);
    }
  }

  private void runAndValidate(ConfigType configType, String configStr, ValidationType validationType, ValidationStatus validationStatus) {
    try (ConfigDataProvider cdp = new StringConfigDataProvider(configStr)) {
      TypesafeConfigBuilder builder = new TypesafeConfigBuilder();
      Config config = builder.buildTypesafeConfig(configType, cdp);
      ValidationResult obsResult = _validator.validateSyntax(configType, config);

      assertEquals(obsResult.getValidationType(), validationType);
      assertEquals(obsResult.getValidationStatus(), validationStatus);
    } catch (Exception e) {
      fail("Caught exception: " + e.getMessage(), e);
    }
  }
}
