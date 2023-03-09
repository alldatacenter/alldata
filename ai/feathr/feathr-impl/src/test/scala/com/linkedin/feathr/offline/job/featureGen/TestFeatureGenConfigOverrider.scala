package com.linkedin.feathr.offline.job.featureGen

import com.linkedin.feathr.offline.config.FeathrConfigLoader
import com.linkedin.feathr.offline.job.{FeatureGenConfigOverrider, FeatureGenJobContext}
import org.scalatest.testng.TestNGSuite
import org.testng.Assert.{assertEquals, assertTrue}
import org.testng.annotations.Test

/**
 * Test suite to test the [[FeatureGenConfigOverrider]] class
 */
class TestFeatureGenConfigOverrider extends TestNGSuite{

  private val feathrConfigLoader = FeathrConfigLoader()
  @Test(description = "test feature definition override parsing for feature-gen job")
  def featureGenJobFeatureConfOverrideParsingTest(): Unit = {
    val overwrittenPath = "featureGen/overrideLocalFeatureConf/"
    val params =
      Array("--work-dir", "/user/feathr-starter-kit/feathr-config", "--feature-conf-override", s"[sources.swaSource.location.path: ${overwrittenPath}]")
    val jobContext = FeatureGenJobContext.parse(params)
    assertEquals(jobContext.featureConfOverride.get, s"sources.swaSource.location.path: ${overwrittenPath}")
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "origSwaSourcePath" }
        |    timePartitionPattern: "yyyy/MM/dd"
        |    timeWindowParameters: {
        |      timestampColumn: "timestamp"
        |      timestampColumnFormat: "yyyy-MM-dd"
        |    }
        |  }
        |}
        |anchors: {
        |  swaAnchor1: {
        |    source: "swaSource"
        |    key: x
        |    features: {
        |      f3: {
        |        def: "count"
        |        aggregation: SUM
        |        window: 1d
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    // test feature definition override
    val configs = feathrConfigLoader.load(feathrConfigLoader.resolveOverride(featureDefAsString, jobContext.featureConfOverride.get))
    assertEquals(configs.sources.get.head._2.path, overwrittenPath)
  }

  @Test(description = "test overrideFeature def config method")
  def overrideFeatureDefConfigTest(): Unit = {
    val featureDefConfig =
      """
        |anchors: {
        |// feathrtest sample data
        |  mockdata-a-value: {
        |    source: "LocalSQLAnchorTest/a.avro.json"
        |    key: "x"
        |    features: {
        |      mockdata_a_ct: "value_ct"
        |    }
        |  }
        |}
        |""".stripMargin
    val overrideFeatureDefConfig =
      """
        |anchors: {
        |// feathrtest sample data
        |  mockdata-a-value: {
        |    source: "LocalSQLAnchorTest/a.avro.json"
        |    key: "x"
        |    keyAlias: "mId"
        |    features: {
        |      // <feature name>: <MVEL expression for how to compute the feature>
        |      mockdata_a_ct: "xyz"
        |    }
        |  }
        |}
        |""".stripMargin
    val res = FeatureGenConfigOverrider.overrideFeatureDefs(Some(featureDefConfig), None, new FeatureGenJobContext("", None, Some(overrideFeatureDefConfig)))
    assertTrue(res._1.get.contains("xyz") && !res._1.get.contains("value_ct"))
  }

  @Test(description = "test local overrideFeature def config method")
  def overrideLocalFeatureDefConfigTest(): Unit = {
    val localFeatureDefConfig =
      """
        |anchors: {
        |// feathrtest sample data
        |  mockdata-a-value: {
        |    source: "LocalSQLAnchorTest/a.avro.json"
        |    key: "x"
        |    features: {
        |      mockdata_a_ct: "value_ct"
        |    }
        |  }
        |}
        |""".stripMargin
    val overrideLocalFeatureDefConfig =
      """
        |anchors: {
        |// feathrtest sample data
        |  mockdata-a-value: {
        |    source: "LocalSQLAnchorTest/a.avro.json"
        |    key: "x"
        |    keyAlias: "mId"
        |    features: {
        |      // <feature name>: <MVEL expression for how to compute the feature>
        |      mockdata_a_ct: "xyz"
        |    }
        |  }
        |}
        |""".stripMargin
    val res =
      FeatureGenConfigOverrider.overrideFeatureDefs(None, Some(localFeatureDefConfig), new FeatureGenJobContext("", None, Some(overrideLocalFeatureDefConfig)))
    assertTrue(res._2.get.contains("xyz") && !res._2.get.contains("value_ct"))
  }

  @Test(description = "test applyOverride method by checking if the overridden value gets inserted")
  def overrideFeatureGenerationTest(): Unit = {
    val overwrittenPath = "featureGen/overrideLocalFeatureConf/"
    val outputDir = "featureGen/generateWithDefaultParams/"
    val features = Seq("f1", "f2")
    val overrideString = Some(s"[sources.swaSource.location.path: ${overwrittenPath}]")
    val featureGenConfigStr =
      s"""
         |operational: {
         |  name: generateWithDefaultParams
         |  endTime: NOW
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[]
         |}
         |features: [${features.mkString(",")}]
      """.stripMargin
    val res = FeatureGenConfigOverrider.applyOverride(featureGenConfigStr, overrideString)
    assertTrue(res.contains(overwrittenPath))
  }
}
