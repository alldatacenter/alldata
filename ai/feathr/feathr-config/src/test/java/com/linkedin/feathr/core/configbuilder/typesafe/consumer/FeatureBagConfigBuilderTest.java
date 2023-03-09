package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.configbuilder.typesafe.AbstractConfigBuilderTest;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.configbuilder.typesafe.consumer.JoinFixture.*;


public class FeatureBagConfigBuilderTest extends AbstractConfigBuilderTest {


  @Test(description = "Tests build of FeatureBag config objects")
  public void testFeatureBagConfigBuilder() {
    testConfigBuilder(featureBagConfigStr, FeatureBagConfigBuilder::build, expFeatureBagConfigObj);
  }

  @Test(description = "Tests build of FeatureBag config objects with special chars")
  public void testFeatureBagConfigBuilderWithSpecialChars() {
    testConfigBuilder(featureBagConfigStrWithSpecialChars, FeatureBagConfigBuilder::build, expFeatureBagConfigObjWithSpecialChars);
  }
}
