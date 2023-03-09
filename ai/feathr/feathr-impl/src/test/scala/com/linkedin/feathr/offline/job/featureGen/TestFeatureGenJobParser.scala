package com.linkedin.feathr.offline.job.featureGen

import com.linkedin.feathr.offline.job.FeatureGenJobContext
import org.scalatest.testng.TestNGSuite
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * Test the [[FeatureGenJobContext]] class' parser
 */
class TestFeatureGenJobParser extends TestNGSuite{

  @Test(description = "test params-override parameter parsing for feature-gen job")
  def featureGenJobParamsTest(): Unit = {
    val params = Array(
      "--work-dir",
      "/user/feathr-starter-kit/feathr-config",
      "--params-override",
      "[endTime:2019-05-01, output(0).params.path:featureGen/overrideLocalFeatureGenerate/]")
    val jobContext = FeatureGenJobContext.parse(params)
    assertEquals(jobContext.paramsOverride.get, "[endTime:2019-05-01, output(0).params.path:featureGen/overrideLocalFeatureGenerate/]")
    assertEquals(jobContext.workDir, "/user/feathr-starter-kit/feathr-config")
  }

  @Test(description = "test params-override parameter parsing for feature-gen job")
  def featureGenJobFeatureConfOverrideTest(): Unit = {
    val overwrittenPath = "featureGen/overrideLocalFeatureConf/"
    val params = Array(
      "--work-dir",
      "/user/feathr-starter-kit/feathr-config",
      "--feature-conf-override",
      s"[sources.swaSource.location.path: ${overwrittenPath}]")
    val jobContext = FeatureGenJobContext.parse(params)
    assertEquals(jobContext.featureConfOverride.get, s"sources.swaSource.location.path: ${overwrittenPath}")
  }
}
