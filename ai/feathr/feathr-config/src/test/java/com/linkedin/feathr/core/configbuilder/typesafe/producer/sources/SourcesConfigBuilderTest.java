package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.configbuilder.typesafe.AbstractConfigBuilderTest;
import org.testng.annotations.Test;


public class SourcesConfigBuilderTest extends AbstractConfigBuilderTest {

  @Test(description = "Tests build of all offline source configs")
  public void offlineSourcesConfigTest() {
    testConfigBuilder(
        SourcesFixture.offlineSourcesConfigStr, SourcesConfigBuilder::build, SourcesFixture.expOfflineSourcesConfigObj);
  }

  @Test(description = "Tests build of all online source configs")
  public void onlineSourcesConfigTest() {
    testConfigBuilder(
        SourcesFixture.onlineSourcesConfigStr, SourcesConfigBuilder::build, SourcesFixture.expOnlineSourcesConfigObj);
  }
}
