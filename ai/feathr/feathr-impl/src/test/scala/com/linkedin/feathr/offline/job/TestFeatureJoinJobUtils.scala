package com.linkedin.feathr.offline.job

import com.linkedin.feathr.offline.config.FeatureJoinConfig
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

class TestJoinJobUtils extends TestNGSuite{

  @Test(
    expectedExceptions = Array(classOf[RuntimeException]),
    expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] endTime field is a required field in(.*)")
  def testParseMissingTimeWindowJoinConfig(): Unit = {

    FeatureJoinConfig.parseJoinConfig("""
        |settings: {
        |  observationDataTimeSettings: {
        |     absoluteTimeRange: {
        |       timeFormat: MM/dd/yyyy
        |       startTime: 05/01/2018
        |     }
        |  }
        |  joinTimeSettings: {
        |     timestampColumn: {
        |         def: "my_timestamp_field_name"
        |         format: "MM/dd/yyyy"
        |       }
        |     simulate_time_delay: 1d
        |  }
        |}
        |
        |
        |features: [
        |  {
        |      key: "xId"
        |      featureList: ["x","y"]
        |  },
        |  {
        |      key: "yId"
        |      featureList: ["a","b"]
        |  },
        |  {
        |      featureList: ["c"]
        |  }
        |]
        |
      """.stripMargin)
  }

  @Test
  def testParseEmptySettingsJoinConfig(): Unit = {

    val joinConfigContent =
      FeatureJoinConfig.parseJoinConfig("""
        |settings: {
        |
        |}
        |
        |
        |features: [
        |  {
        |      key: "xId"
        |      featureList: ["x","y"]
        |  },
        |  {
        |      key: "yId"
        |      featureList: ["a","b"]
        |  },
        |  {
        |      featureList: ["c"]
        |  }
        |]
        |
      """.stripMargin)

    val groups = joinConfigContent.groups
    assert(groups.size == 1)

  }


}
