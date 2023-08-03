package com.linkedin.feathr.offline.job.featureGen

import com.linkedin.feathr.offline.job.{FeatureGenJobContext, FeatureGenSpec}
import org.scalatest.testng.TestNGSuite
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class TestFeatureGenSpecParser extends TestNGSuite{

  @Test(description = "test feature gen spec parser")
  def featureGenSpecParserTest(): Unit = {
    val outputDir = "featureGen/generateWithDefaultParams/"
    val features = Seq("f1", "f2")
    val params = Array("--work-dir", "/user/feathr/feathr-starter-kit/feathr-config")
    val jobContext = FeatureGenJobContext.parse(params)
    val featureGenConfigStr =
      s"""
         |operational: {
         |  name: generateWithDefaultParams
         |  endTime: NOW
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[ {
         |     name: HDFS
         |     params: {
         |      path: ${outputDir}
         |     }
         |   }
         |  ]
         |}
         |features: [${features.mkString(",")}]
      """.stripMargin
    val res = FeatureGenSpec.parse(featureGenConfigStr, jobContext, List())
    assertEquals(res.getFeatures, Seq("f1", "f2"))
    assertEquals(res.getOutputProcessorConfigs.head.getName, "HDFS")
    assertEquals(res.offlineOperationalConfig.getName, "generateWithDefaultParams")
    assertEquals(res.offlineOperationalConfig.getTimeSetting.getReferenceEndTime, "NOW")
    assertEquals(res.offlineOperationalConfig.getTimeSetting.getReferenceEndTimeFormat, "yyyy-MM-dd")
  }
}
