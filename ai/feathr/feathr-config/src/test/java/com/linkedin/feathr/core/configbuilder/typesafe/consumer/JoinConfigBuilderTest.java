package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.config.consumer.JoinConfig;
import com.linkedin.feathr.core.configbuilder.typesafe.AbstractConfigBuilderTest;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.configbuilder.typesafe.consumer.JoinFixture.*;
import static org.testng.Assert.*;


public class JoinConfigBuilderTest extends AbstractConfigBuilderTest {

  @Test(description = "Tests build of JoinConfig config object with single feature bag but no settings")
  public void testWithNoSettings() {
    testJoinConfigBuilder(joinConfigStr1, expJoinConfigObj1);
  }

  @Test(description = "Tests build of JoinConfig config object with single feature bag which has special characters but no settings")
  public void testWithNoSettingsAndWithSpecialChars() {
    testJoinConfigBuilder(joinConfigStr1WithSpecialChars, expJoinConfigObj1WithSpecialChars);
  }

  @Test(description = "Tests build of JoinConfig config object with single feature bag but empty settings")
  public void testWithEmptySettings() {
    testJoinConfigBuilder(joinConfigStr2, expJoinConfigObj2);
  }

  @Test(description = "Tests build of JoinConfig config object with single feature bag and time-window settings")
  public void testWithTimeWindowSettings() {
    testJoinConfigBuilder(joinConfigStr3, expJoinConfigObj3);
  }

  @Test(description = "Tests build of JoinConfig config object with multiple feature bags")
  public void testWithMultiFeatureBags() {
    testJoinConfigBuilder(joinConfigStr4, expJoinConfigObj4);
  }

  private void testJoinConfigBuilder(String configStr, JoinConfig expJoinConfigObj) {
    Config fullConfig = ConfigFactory.parseString(configStr);
    JoinConfig obsJoinConfigObj = JoinConfigBuilder.build(fullConfig);
    assertEquals(obsJoinConfigObj, expJoinConfigObj);
  }
}
