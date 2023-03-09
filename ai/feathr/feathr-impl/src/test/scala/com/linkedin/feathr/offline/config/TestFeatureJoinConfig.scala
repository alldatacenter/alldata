package com.linkedin.feathr.offline.config

import com.linkedin.feathr.common.{DateParam, JoiningFeatureParams}
import com.linkedin.feathr.offline.anchored.WindowTimeUnit
import org.scalatest.testng.TestNGSuite
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Test suite to test parsing of join configs
 */
class TestFeatureJoinConfig extends TestNGSuite{

  /**
   * Settings config with observationDataTimeSettings and joinTimeSettings.
   */
  @Test
  def testParseJoinConfigWithFullSettingsConf(): Unit = {
    val joinConfigAsString = """
      | settings: {
      |  observationDataTimeSettings: {
      |     absoluteTimeRange: {
      |         startTime: "2018-05-01"
      |         endTime: "2018-05-03"
      |         timeFormat: "yyyy-MM-dd"
      |     }
      |  }
      |  joinTimeSettings: {
      |     timestampColumn: {
      |       def: timestamp
      |       format: "yyyy-MM-dd"
      |     }
      |  }
      |}
      |
      |features: [
      |   {
      |       key: [x],
      |       featureList: ["f1"]
      |   }
      |]
      |
      |
      |observationPath: "some/path"
      |outputPath: "some/path"
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)

    Assert.assertEquals(joinConfig.settings.get.observationDataTimeSetting.get.dateParam, DateParam(Some("2018-05-01"), Some("2018-05-03")))
    Assert.assertEquals(joinConfig.settings.get.joinTimeSetting.get.timestampColumn, TimestampColumn("timestamp", "yyyy-MM-dd"))
    Assert.assertEquals(joinConfig.joinFeatures.toList, List(JoiningFeatureParams(Seq("x"), "f1")))
  }

  /**
   * Settings config with only observationDataTimeSettings.
   */
  @Test
  def testParseJoinConfigWithOnlyObsSetting(): Unit = {
    val joinConfigAsString =
      """
    | settings: {
    |  observationDataTimeSettings: {
    |     relativeTimeRange: {
    |         offset: 1d
    |         window: 3d
    |     }
    |  }
    |}
    |
    |features: [
    |   {
    |       key: [x],
    |       featureList: ["f1"]
    |   }
    |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)

    Assert.assertEquals(joinConfig.settings.get.observationDataTimeSetting.get.dateParam, DateParam(None, None, Some("1d"), Some("3d")))
    Assert.assertEquals(joinConfig.settings.get.joinTimeSetting, None)
    Assert.assertEquals(joinConfig.joinFeatures.toList, List(JoiningFeatureParams(Seq("x"), "f1")))
  }

  /**
   * Settings config with useLatestFeatureData.
   */
  @Test
  def testParseJoinConfigWithUseLatestFeatureData(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |   joinTimeSettings: {
        |     useLatestFeatureData: true
        |   }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f1"]
        |   }
        |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)

    Assert.assertEquals(joinConfig.settings.get.observationDataTimeSetting, None)
    Assert.assertEquals(joinConfig.settings.get.joinTimeSetting.get.useLatestFeatureData, true)
  }

  /**
   * Settings config with override time delays.
   */
  @Test
  def testParseJoinConfigWithOverrideTimeDelay(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |   joinTimeSettings: {
        |     simulateTimeDelay: 1d
        |   }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f1"]
        |       overrideTimeDelay: 2d
        |   },
        |   {
        |       key: [x],
        |       featureList: ["f2"]
        |   }
        |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)

    Assert.assertEquals(joinConfig.featuresToTimeDelayMap.get("f1"), Some("2d"))
    Assert.assertEquals(joinConfig.settings.get.joinTimeSetting.get.simulateTimeDelay, Some(WindowTimeUnit.parseWindowTime("1d")))
  }

  /**
   * Settings config with useLatestFeatureData set to true and timestamp column also set to true.
   */
  @Test(
    expectedExceptions = Array(classOf[RuntimeException]),
    expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] When useLatestFeatureData flag is set to true(.*)")
  def testParseJoinConfigWithUseLatestFeatureDataAndTimestamp(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |   joinTimeSettings: {
        |     timestampColumn: {
        |       def: timestamp
        |       format: "yyyy/MM/dd"
        |     }
        |     useLatestFeatureData: true
        |   }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f2"]
        |   }
        |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)
  }

  /**
   * Settings config with invalid name in settings.
   */
  @Test(expectedExceptions = Array(classOf[RuntimeException]), expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] unrecognized(.*)")
  def testParseJoinConfigWithInvalidNameInSettings(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |   joinTimeSettings: {
        |     timestampColumn: {
        |       def: timestamp
        |       format: "yyyy/MM/dd"
        |     }
        |   }
        |   randomString: {
        |   }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f2"]
        |   }
        |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)
  }

  /**
   * Settings config with no timestamp format in timestamp column format.
   */
  @Test(expectedExceptions = Array(classOf[RuntimeException]), expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] format field is not correctly set(.*)")
  def testParseJoinConfigWithNoTimestampFormat(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |   joinTimeSettings: {
        |     timestampColumn: {
        |       def: timestamp
        |     }
        |   }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f2"]
        |   }
        |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)
  }

  /**
   * Settings config with no window format in relativeTimeRange format.
   */
  @Test(expectedExceptions = Array(classOf[RuntimeException]), expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] window field is a required field(.*)")
  def testParseJoinConfigWithNoWindow(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |   observationDataTimeSettings: {
        |     relativeTimeRange: {
        |       offset: 1d
        |     }
        |   }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f2"]
        |   }
        |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)
  }

  /**
   * Settings config with no endTime in absoluteTimeRange format.
   */
  @Test(expectedExceptions = Array(classOf[RuntimeException]), expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] endTime field is a required field(.*)")
  def testParseJoinConfigWithNoStartTime(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |   observationDataTimeSettings: {
        |     absoluteTimeRange: {
        |       startTime: "2018-05-01"
        |       timeFormat: "yyyy-MM-dd"
        |     }
        |   }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f2"]
        |   }
        |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)
  }

  /**
   * Settings config with both absoluteTimeRange and relativeTimeRange.
   */
  @Test(
    expectedExceptions = Array(classOf[RuntimeException]),
    expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] Both relativeTimeRange and absoluteTimeRange(.*)")
  def testParseJoinConfigWithBothAbsoluteTimeAndRelativeTimeRange(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |   observationDataTimeSettings: {
        |     absoluteTimeRange: {
        |       startTime: "2018-05-01"
        |       endTime: "2018-05-10"
        |       timeFormat: "yyyy-MM-dd"
        |     }
        |     relativeTimeRange: {
        |       window: 3d
        |       offset: 1d
        |     }
        |   }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f2"]
        |   }
        |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)
  }

  /**
   * Settings config with empty observationDataTimeSettings.
   */
  @Test(
    expectedExceptions = Array(classOf[RuntimeException]),
    expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] relativeTimeRange and absoluteTimeRange are not set(.*)")
  def testParseJoinConfigWithNoAbsoluteTimeAndRelativeTimeRange(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |   observationDataTimeSettings: {
        |   }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f2"]
        |   }
        |]
    """.stripMargin
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsString)
  }
}
