package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configbuilder.typesafe.TypesafeConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.ResourceConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.StringConfigDataProvider;
import com.linkedin.feathr.core.configvalidator.ConfigValidatorFixture;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.ValidationStatus;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Test class for {@link JoinConfSemanticValidator}
 */
public class JoinConfSemanticValidatorTest {
  private TypesafeConfigBuilder _configBuilder = new TypesafeConfigBuilder();
  private JoinConfSemanticValidator _joinConfSemanticValidator = new JoinConfSemanticValidator();

  private Map<FeatureReachType, Set<String>> _featureReachableInfo;

  @BeforeClass
  public void init() {
    try (ConfigDataProvider featureDefProvider =
        new ResourceConfigDataProvider("invalidSemanticsConfig/feature-not-reachable-def.conf")) {
      FeatureDefConfigSemanticValidator featureDefConfSemanticValidator = new FeatureDefConfigSemanticValidator();
      FeatureDefConfig featureDefConfig = _configBuilder.buildFeatureDefConfig(featureDefProvider);

      _featureReachableInfo = featureDefConfSemanticValidator.getFeatureAccessInfo(featureDefConfig);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests requesting unreachable features")
  public void testRequestUnreachableFeatures() {
    try (ConfigDataProvider joinConfProvider = new StringConfigDataProvider(ConfigValidatorFixture.joinConfig1)) {
      JoinConfig joinConfig = _configBuilder.buildJoinConfig(joinConfProvider);

      ValidationResult validationResult = _joinConfSemanticValidator.validate(joinConfig, _featureReachableInfo);
      Assert.assertEquals(validationResult.getValidationType(), ValidationType.SEMANTIC);
      Assert.assertEquals(validationResult.getValidationStatus(), ValidationStatus.INVALID);
      Assert.assertNotNull(validationResult.getDetails());
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests requesting undefined features")
  public void testRequestUndefinedFeatures() {
    try (ConfigDataProvider joinConfProvider = new StringConfigDataProvider(ConfigValidatorFixture.joinConfig2)) {
      JoinConfig joinConfig = _configBuilder.buildJoinConfig(joinConfProvider);

      ValidationResult validationResult = _joinConfSemanticValidator.validate(joinConfig, _featureReachableInfo);
      Assert.assertEquals(validationResult.getValidationType(), ValidationType.SEMANTIC);
      Assert.assertEquals(validationResult.getValidationStatus(), ValidationStatus.INVALID);
      Assert.assertNotNull(validationResult.getDetails());
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Test get requested features")
  public void testGetRequestedFeatures() {
    try (ConfigDataProvider joinConfProvider = new StringConfigDataProvider(JoinConfFixture.joinConf1)) {
      JoinConfig joinConfig = _configBuilder.buildJoinConfig(joinConfProvider);
      Set<String> requestedFeatureNames = JoinConfSemanticValidator.getRequestedFeatureNames(joinConfig);
      Assert.assertEquals(requestedFeatureNames, JoinConfFixture.requestedFeatureNames1);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }
}
