package com.linkedin.feathr.core.configbuilder.typesafe.generation;

import com.linkedin.feathr.core.config.generation.FeatureGenConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * test of Frame feature generation config object
 */
public class FeatureGenConfigBuilderTest {

  @Test(description = "Tests building of generation config for the case with all supported fields")
  public void testWithFullFieldsCase() {
    testFeatureGenConfigBuilder(GenerationFixture.generationConfigStr1, GenerationFixture.expGenerationConfigObj1);
  }

  @Test(description = "Tests building of generation config for cases with minimal supported fields")
  public void testWithDefaultFieldsCase() {
    testFeatureGenConfigBuilder(GenerationFixture.generationConfigStr2, GenerationFixture.expGenerationConfigObj2);
  }

  @Test(description = "Tests building of nearline generation config for all possible cases")
  public void testWithNealineFieldsCase() {
    testFeatureGenConfigBuilder(
        GenerationFixture.nearlineGenerationConfigStr, GenerationFixture.nearlineGenerationConfigObj);
  }

  private void testFeatureGenConfigBuilder(String configStr, FeatureGenConfig expFeatureGenConfigObj) {
    Config withDefaultConfig = ConfigFactory.parseString(configStr);
    FeatureGenConfig generationConfigObj = FeatureGenConfigBuilder.build(withDefaultConfig);
    assertEquals(generationConfigObj, expFeatureGenConfigObj);
  }
}
