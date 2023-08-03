package com.linkedin.feathr.core.configbuilder.typesafe.producer;

import com.linkedin.feathr.core.config.producer.FeatureDefConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.configbuilder.typesafe.producer.FeatureDefFixture.*;
import static org.testng.Assert.*;


public class FeatureDefConfigBuilderTest {

  @Test(description = "Tests building of FeatureDef config object")
  public void test() {
    Config fullConfig = ConfigFactory.parseString(featureDefConfigStr1);
    FeatureDefConfig obsFeatureDefConfigObj = FeatureDefConfigBuilder.build(fullConfig);

    assertEquals(obsFeatureDefConfigObj, expFeatureDefConfigObj1);
  }

  @Test(description = "Tests building of FeatureDef config object with only AnchorConfig")
  public void testWithOnlyAnchorConfig() {
    Config fullConfig = ConfigFactory.parseString(featureDefConfigStr2);
    FeatureDefConfig obsFeatureDefConfigObj = FeatureDefConfigBuilder.build(fullConfig);

    assertEquals(obsFeatureDefConfigObj, expFeatureDefConfigObj2);
  }

  @Test(description = "Tests building of FeatureDef config object with feature and dimension sections")
  public void testWithFeatureAndDimensionSections() {
    Config fullConfig = ConfigFactory.parseString(featureDefConfigStr3);
    FeatureDefConfig obsFeatureDefConfigObj = FeatureDefConfigBuilder.build(fullConfig);

    assertEquals(obsFeatureDefConfigObj, expFeatureDefConfigObj3);
  }
}
