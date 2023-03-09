package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.linkedin.feathr.core.configbuilder.typesafe.TypesafeConfigBuilder;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.ResourceConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.StringConfigDataProvider;
import com.linkedin.feathr.core.configvalidator.ValidationResult;
import com.linkedin.feathr.core.configvalidator.ValidationStatus;
import com.linkedin.feathr.core.configvalidator.ValidationType;
import com.linkedin.feathr.exception.FeathrConfigException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Tests for {@link FeatureDefConfigSemanticValidator}
 */
public class FeatureDefConfSemanticValidatorTest {
  private TypesafeConfigBuilder configBuilder = new TypesafeConfigBuilder();
  private FeatureDefConfigSemanticValidator configValidator = new FeatureDefConfigSemanticValidator();
  private MvelValidator mvelValidator = MvelValidator.getInstance();
  private HdfsSourceValidator hdfsSourceValidator = HdfsSourceValidator.getInstance();


  @Test(description = "Tests getting duplicate feature names in FeatureDef config")
  public void testGetDuplicateFeatureNames() {
    try (ConfigDataProvider provider = new ResourceConfigDataProvider("invalidSemanticsConfig/duplicate-feature.conf")) {
      FeatureDefConfigSemanticValidator featureDefConfigSemanticValidator = new FeatureDefConfigSemanticValidator();
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);
      ValidationResult validationResult = featureDefConfigSemanticValidator.validate(featureDefConfig);
      Assert.assertEquals(validationResult.getValidationStatus(), ValidationStatus.WARN);
      Assert.assertEquals(validationResult.getDetails().toString(), "Optional[The following features' definitions are duplicate: \n"
          + "member_lixSegment_isJobSeeker]");
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }


  @Test(description = "Tests config failure when duplicate source names are in several FeatureDef configs")
  public void testMultipleConfigDuplicateSourceNames() {

    List<String> resources = Arrays.asList("invalidSemanticsConfig/duplicate-feature.conf",
        "invalidSemanticsConfig/undefined-source.conf");

    try (ConfigDataProvider featureDefConfigProvider = new ResourceConfigDataProvider(resources)) {
      FeatureConsumerConfValidator validator = new FeatureConsumerConfValidator();
      Map<ConfigType, ConfigDataProvider> configTypeWithDataProvider = new HashMap<>();
      configTypeWithDataProvider.put(ConfigType.FeatureDef, featureDefConfigProvider);
      Map<ConfigType, ValidationResult> validationResultMap =
          validator.validate(configTypeWithDataProvider, ValidationType.SEMANTIC);

      ValidationResult validationResult =  validationResultMap.get(ConfigType.FeatureDef);
      Assert.assertEquals(validationResult.getValidationStatus(), ValidationStatus.WARN);
      String expected = "Optional[The following source name(s) are "
          + "duplicates between two or more feature definition configs: \n"
          + "source name: member_derived_data\n"
          + "File paths of two or more files that have duplicate source names: \n"
          + "Resources: [invalidSemanticsConfig/duplicate-feature.conf, invalidSemanticsConfig/undefined-source.conf] ";
      Assert.assertEquals(validationResult.getDetails().toString().substring(0,307), expected);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests getting undefined sources in anchors from FeatureDef config")
  public void testGetUndefinedAnchorSources() {
    try (ConfigDataProvider provider = new ResourceConfigDataProvider("invalidSemanticsConfig/undefined-source.conf")) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);

      Map<String, String> undefinedAnchorSources =
          configValidator.getUndefinedAnchorSources(featureDefConfig);

      Assert.assertEquals(undefinedAnchorSources.size(), 1);
      Assert.assertTrue(undefinedAnchorSources.containsKey("memberLixSegmentV2"));
      Assert.assertEquals(undefinedAnchorSources.get("memberLixSegmentV2"), "member_derived_date");

    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests approved extractor with parameters won't throw exception.")
  public void testApprovedExtractorWithParams() {
    try (ConfigDataProvider provider = new ResourceConfigDataProvider("extractor-with-params.conf")) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);

      configValidator.validateApprovedExtractorWithParameters(featureDefConfig);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Tests non-approved extractor with parameters will throw exception.", expectedExceptions = FeathrConfigException.class)
  public void testNonApprovedExtractorWithParams() throws Exception {
    try (ConfigDataProvider provider = new ResourceConfigDataProvider(
        "invalidSemanticsConfig/extractor-with-params-not-approved.conf")) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);

      configValidator.validateApprovedExtractorWithParameters(featureDefConfig);
    }
  }

  @Test(description = "Tests getting all reachable and unreachable features in FeatureDef config with an invalid config.")
  public void testGetReachableFeatures() {

    try (ConfigDataProvider provider = new ResourceConfigDataProvider(
        "invalidSemanticsConfig/feature-not-reachable-def.conf")) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);
      Map<FeatureReachType, Set<String>> featureAccessInfo = configValidator.getFeatureAccessInfo(featureDefConfig);

      Set<String> reachableFeatures = featureAccessInfo.get(FeatureReachType.REACHABLE);
      Set<String> expectedReachableFeatures = new HashSet<>();
      expectedReachableFeatures.add("feature1");
      expectedReachableFeatures.add("feature2");
      expectedReachableFeatures.add("derived_feature_1");
      expectedReachableFeatures.add("derived_feature_2");
      Assert.assertEquals(reachableFeatures.size(), 4);
      Assert.assertEquals(reachableFeatures, expectedReachableFeatures);

      Set<String> unreachableFeatures = featureAccessInfo.get(FeatureReachType.UNREACHABLE);
      Set<String> expectedUnreachableFeatures = new HashSet<>();
      expectedUnreachableFeatures.add("feature3");
      expectedUnreachableFeatures.add("derived_feature_3");
      Assert.assertEquals(unreachableFeatures.size(), 2);
      Assert.assertEquals(unreachableFeatures, expectedUnreachableFeatures);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Test MVEL heuristic validation for single MVEL expression")
  public void testSingleMvelHeuristicCheckWithIn() {
    Assert.assertTrue(mvelValidator.heuristicProjectionExprCheck("(parent.name in users)"));
    Assert.assertTrue(mvelValidator.heuristicProjectionExprCheck("(name in (familyMembers in users))"));
    Assert.assertTrue(mvelValidator.heuristicProjectionExprCheck("myFunc(abc)"));
    Assert.assertFalse(mvelValidator.heuristicProjectionExprCheck("parent.name in users"));
    Assert.assertFalse(mvelValidator.heuristicProjectionExprCheck("(name in familyMembers in users)"));
    Assert.assertFalse(mvelValidator.heuristicProjectionExprCheck("(some expression) familyMembers in users"));
  }

  @Test(description = "Test feature MVEL extracting")
  public void testExtractingMvelFromFeatureDef() {
    try (ConfigDataProvider provider = new StringConfigDataProvider(FeatureDefConfFixture.featureDefWithMvel)) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);
      Map<String, String> mvelDef = mvelValidator.getFeatureMvels(featureDefConfig);
      Map<String, String> expectedResult = new HashMap<String, String>() {{
        put("waterloo_member_geoCountry_local", "$.countryCode in geoStdData");
        put("waterloo_member_job_cosineSimilarity", "cosineSimilarity(a, b)");
        put("maxPV12h", "pageView");
        put("waterloo_member_geoCountry_local_alias", "waterloo_member_geoCountry_local");
      }};
      Assert.assertEquals(mvelDef, expectedResult);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Test anchor key MVEL extracting")
  public void testExtractingMvelFromAnchor() {
    try (ConfigDataProvider provider = new StringConfigDataProvider(FeatureDefConfFixture.featureDefWithMvel)) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);
      Map<String, List<String>> mvelDef = mvelValidator.getAnchorKeyMvels(featureDefConfig);
      Map<String, List<String>> expectedResult = new HashMap<String, List<String>>() {{
        put("nearLineFeatureAnchor", Collections.singletonList("a in b")); // the anchor key MVEL expr
      }};
      Assert.assertEquals(mvelDef, expectedResult);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Test MVEL heuristic check")
  public void testMvelHeuristicCheck() {
    try (ConfigDataProvider provider = new StringConfigDataProvider(FeatureDefConfFixture.featureDefWithMvel)) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);
      Map<String, List<String>> invalidMvels = mvelValidator.getPossibleInvalidMvelsUsingIn(featureDefConfig);
      Map<String, List<String>> expectedResult = new HashMap<String, List<String>>() {{
        put("waterloo_member_geoCountry_local", Collections.singletonList("$.countryCode in geoStdData"));
        put("nearLineFeatureAnchor", Collections.singletonList("a in b")); // the anchor key MVEL expr
      }};
      Assert.assertEquals(invalidMvels, expectedResult);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Test MVEL validator")
  public void testMvelValidator() {
    try (ConfigDataProvider provider = new StringConfigDataProvider(FeatureDefConfFixture.featureDefWithMvel)) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);
      ValidationResult result = mvelValidator.validate(featureDefConfig);
      Assert.assertEquals(result.getValidationStatus(), ValidationStatus.WARN);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Test getting invalid Hdfs source")
  public void testGetHdfsInvalidManagedDataSets() {
    try (ConfigDataProvider provider = new StringConfigDataProvider(FeatureDefConfFixture.featureDefWithHdfsSource)) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);
      Map<String, String> invalidDataSets = hdfsSourceValidator.getInvalidManagedDataSets(featureDefConfig);
      Map<String, String> expectedResult = new HashMap<String, String>() {{
        put("hdfsSource1", "/data/tracking_column/test");
        put("hdfsSource2", "/jobs/metrics/ump_v2/metrics/test/test/test/test");
        put("hdfsSource3", "/jobs/metrics/udp/datafiles/test");
        put("testAnchor1", "/jobs/metrics/udp/snapshot/test/#LATEST");
      }};
      Assert.assertEquals(invalidDataSets, expectedResult);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Test HdfsSource validator")
  public void testHdfsSourceValidator() {
    try (ConfigDataProvider provider = new StringConfigDataProvider(FeatureDefConfFixture.featureDefWithHdfsSource)) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);
      ValidationResult result = hdfsSourceValidator.validate(featureDefConfig);
      Assert.assertEquals(result.getValidationStatus(), ValidationStatus.WARN);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Test getting required features")
  public void testGetRequiredFeatures() {
    try (ConfigDataProvider provider = new StringConfigDataProvider(FeatureDefConfFixture.featureDefWithExtractors)) {
      FeatureDefConfig featureDefConfig = configBuilder.buildFeatureDefConfig(provider);
      Set<String> requestedFeatures = Stream.of("offline_feature1_1", "offline_feature2_1", "offline_feature4_1",
          "derived_feature_1", "derived_feature_2", "derived_feature_4").collect(Collectors.toSet());

      Set<String> requiredFeatures =
          FeatureDefConfigSemanticValidator.getRequiredFeatureNames(featureDefConfig, requestedFeatures);

      Set<String> expectedRequiredFeatures = Stream.of("offline_feature1_1", "offline_feature2_1", "offline_feature3_1",
          "offline_feature4_1", "online_feature1_1",  "online_feature2_1", "derived_feature_1",
          "derived_feature_2", "derived_feature_3", "derived_feature_4").collect(Collectors.toSet());

      Assert.assertEquals(requiredFeatures, expectedRequiredFeatures);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }
}
