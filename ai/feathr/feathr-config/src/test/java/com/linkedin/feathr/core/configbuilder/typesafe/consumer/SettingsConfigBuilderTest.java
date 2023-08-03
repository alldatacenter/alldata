package com.linkedin.feathr.core.configbuilder.typesafe.consumer;

import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.linkedin.feathr.core.configbuilder.typesafe.AbstractConfigBuilderTest;
import org.testng.annotations.Test;

import static com.linkedin.feathr.core.configbuilder.typesafe.consumer.JoinFixture.*;


public class SettingsConfigBuilderTest extends AbstractConfigBuilderTest {

  @Test(description = "Tests an empty settings config")
  public void testEmptySettings() {
    testConfigBuilder(emptySettingsConfigStr, SettingsConfigBuilder::build, expEmptySettingsConfigObj);
  }

  @Test(description = "Tests a settings config with absoluteTimeRange set, normal case")
  public void testSettingsWithAbsoluteTimeRange() {
    testConfigBuilder(settingsWithAbsoluteTimeRange,
        SettingsConfigBuilder::build, expSettingsWithAbsoluteTimeRange);
  }

  @Test(description = "Tests a settings config with only useLatestFeatureData set to true")
  public void testSettingsWithOnlyLatestFeatureData() {
    testConfigBuilder(settingsWithLatestFeatureData,
        SettingsConfigBuilder::build, expSettingsWithLatestFeatureData);
  }

  @Test(description = "Tests a settings config with relativeTimeRange set")
  public void testSettingsWithRelativeTimeRange() {
    testConfigBuilder(settingsWithRelativeTimeRange,
        SettingsConfigBuilder::build, expSettingsWithRelativeTimeRange);
  }

  @Test(description = "Tests a settings config with only window field set")
  public void testSettingsWithOnlyWindow() {
    testConfigBuilder(settingsWithOnlyWindow,
        SettingsConfigBuilder::build, expSettingsWithOnlyWindow);
  }

  @Test(description = "Tests a settings config with only start time",
      expectedExceptions = ConfigBuilderException.class)
  public void testSettingsWithOnlyStartTime() {
    testConfigBuilder(invalidWithOnlyStartTime,
        SettingsConfigBuilder::build, expEmptySettingsConfigObj);
  }

  @Test(description = "Tests a settings config with both absolute time range and relative time range",
      expectedExceptions = ConfigBuilderException.class)
  public void testSettingsWithAbsTimeRangeAndRelTimeRange() {
    testConfigBuilder(invalidWithBothAbsoluteTimeRangeAndRelativeTimeRange,
        SettingsConfigBuilder::build, expEmptySettingsConfigObj);
  }

  @Test(description = "Tests a settings config with both use latest feature data set to true and timestamp column field defined",
      expectedExceptions = ConfigBuilderException.class)
  public void testSettingsWithUseLatestFeatureDataAndTimestampCol() {
    testConfigBuilder(invalidWithUseLatestFeatureDataAndTimestampCol,
        SettingsConfigBuilder::build, expEmptySettingsConfigObj);
  }

  @Test(description = "Tests a settings config with both use latest feature data set to true and time delay field defined",
      expectedExceptions = ConfigBuilderException.class)
  public void testSettingsWithUseLatestFeatureDataAndTimeDelay() {
    testConfigBuilder(invalidWithUseLatestFeatureDataAndTimeDelay,
        SettingsConfigBuilder::build, expEmptySettingsConfigObj);
  }
}
