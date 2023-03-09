package com.linkedin.feathr.core.configvalidator.typesafe;

import com.linkedin.feathr.core.config.ConfigType;
import com.linkedin.feathr.core.configdataprovider.ConfigDataProvider;
import com.linkedin.feathr.core.configdataprovider.StringConfigDataProvider;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Test class for {@link ExtractorClassValidationUtils}
 */
public class ExtractorClassValidationUtilsTest {
  @Test(description = "Test getting classes from FeatureDef conf with Join conf")
  public void testGetClassesWithJoinConf() {
    try (
        ConfigDataProvider featureDefProvider
            = new StringConfigDataProvider(FeatureDefConfFixture.featureDefWithExtractors);
        ConfigDataProvider joinProvider
            = new StringConfigDataProvider(JoinConfFixture.joinConf1)
    ) {
      Map<ConfigType, ConfigDataProvider> map = Stream.of(new Object[][] {
          {ConfigType.FeatureDef, featureDefProvider},
          {ConfigType.Join, joinProvider},
      }).collect(Collectors.toMap(d -> (ConfigType) d[0], d -> (ConfigDataProvider) d[1]));

      Set<String> extractors = ExtractorClassValidationUtils.getExtractorClasses(map);
      Set<String> expectedExtractors = new HashSet<>(FeatureDefConfFixture.expectedExtractors);
      // if Join config provided, won't return extractors that are not used
      expectedExtractors.remove("com.linkedin.frame.online.anchor.test.ExtractorNotUsed");

      Assert.assertEquals(extractors, expectedExtractors);

    } catch (IOException e) {
      fail("Error in building config", e);
    }
  }

  @Test(description = "Test getting classes from FeatureDef conf without Join conf")
  public void testGetClassesWithoutJoinConf() {
    try (ConfigDataProvider featureDefProvider
        = new StringConfigDataProvider(FeatureDefConfFixture.featureDefWithExtractors)) {
      Map<ConfigType, ConfigDataProvider> map =
          Collections.singletonMap(ConfigType.FeatureDef, featureDefProvider);
      Set<String> extractors = ExtractorClassValidationUtils.getExtractorClasses(map);
      Assert.assertEquals(extractors, FeatureDefConfFixture.expectedExtractors);
    } catch (Throwable e) {
      fail("Error in building config", e);
    }
  }
}
