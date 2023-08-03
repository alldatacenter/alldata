package com.linkedin.feathr.offline

import com.linkedin.feathr.offline.client.FeathrClient
import com.linkedin.feathr.offline.config.{FeatureJoinConfig, KeyedFeatureList}
import com.linkedin.feathr.offline.job.LocalFeatureJoinJob
import org.apache.spark.sql.Row
import org.testng.Assert.assertEquals
import org.testng.annotations.{BeforeClass, Test}

/**
 * Test default value in feathr with quince-fds format
 */
class TestFeathrDefaultValue extends TestFeathr {
  val trainingData = "testMVELFeatureWithNullValue-observations.csv"

  @BeforeClass
  override def setFeathrConfig(): Unit = {
    val feathrConfigs = List(feathrConfigLoader.load(getClass.getClassLoader.getResource("feathrConf-default.conf")))
    feathr = FeathrClient.builder(ss).addFeatureDefConfs(Some(feathrConfigs)).build()
  }

  // test the quince-fds default value
  // the default value can be set as a wrappedArray("term"), wrappedArray("value"
  @Test
  def testDefaultValueWithNameTerm(): Unit = {
    val keyedFeatureList = KeyedFeatureList(Seq("mId"), Seq("featuresWithNullNameTerm"))
    val joinConfig = FeatureJoinConfig(Map("featuresWithNullNameTerm" -> Seq(keyedFeatureList)))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())

    val joined = feathr.joinFeatures(joinConfig, obsData).data.collect()

    assertEquals(joined.size, 3)
    assertEquals(joined(0).getAs[Row]("featuresWithNullNameTerm"), TestUtils.build1dSparseTensorFDSRow(Array("term1_1", "term1_2"), Array(1.0f, 2.0f)))
    assertEquals(joined(1).getAs[Row]("featuresWithNullNameTerm"), TestUtils.build1dSparseTensorFDSRow(Array("term_default"), Array(0.0f)))
    assertEquals(joined(2).getAs[Row]("featuresWithNullNameTerm"), TestUtils.build1dSparseTensorFDSRow(Array("term_default"), Array(0.0f)))
  }

  // test single term value default value
  // set as one category with the value 1
  @Test
  def testDefaultValueWithSingleTerm(): Unit = {
    val keyedFeatureList = KeyedFeatureList(Seq("mId"), Seq("featuresWithNullSingleTerm"))
    val joinConfig = FeatureJoinConfig(Map("featuresWithNullSingleTerm" -> Seq(keyedFeatureList)))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())

    val joined = feathr.joinFeatures(joinConfig, obsData).data.collect()

    assertEquals(joined.size, 3)
    assertEquals(joined(0).getAs[Row]("featuresWithNullSingleTerm"), TestUtils.build1dSparseTensorFDSRow(Array("term1_1", "term1_2"), Array(1.0f, 2.0f)))
    assertEquals(joined(1).getAs[Row]("featuresWithNullSingleTerm"), TestUtils.build1dSparseTensorFDSRow(Array("term_default"), Array(1.0f)))
    assertEquals(joined(2).getAs[Row]("featuresWithNullSingleTerm"), TestUtils.build1dSparseTensorFDSRow(Array("term_default"), Array(1.0f)))
  }

  // test feature with empty map, it should not be handled as missing value
  // the behavior of Feathr-offline handling default value is, when the feature value is null, the default value will be applied
  // when the feature value is an empty map, default value won't be applied.
  @Test
  def testDefaultValueWithEmptyMap(): Unit = {
    val keyedFeatureList = KeyedFeatureList(Seq("mId"), Seq("featuresWithEmptyMap"))
    val joinConfig = FeatureJoinConfig(Map("featuresWithEmptyMap" -> Seq(keyedFeatureList)))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())

    val joined = feathr.joinFeatures(joinConfig, obsData).data.collect()

    assertEquals(joined.size, 3)

    assertEquals(joined(0).getAs[Row]("featuresWithEmptyMap"), TestUtils.build1dSparseTensorFDSRow(Array(), Array()))
    assertEquals(joined(1).getAs[Row]("featuresWithEmptyMap"), TestUtils.build1dSparseTensorFDSRow(Array("term_default"), Array(0.0f)))
    assertEquals(joined(2).getAs[Row]("featuresWithEmptyMap"), TestUtils.build1dSparseTensorFDSRow(Array("term_default"), Array(0.0f)))
  }

  // Allow empty feature definition configs
  @Test
  def testEmptyConfigFile(): Unit = {
    val feathrConfigs = List(feathrConfigLoader.load(""))
    feathr = FeathrClient.builder(ss).addFeatureDefConfs(Some(feathrConfigs)).build()
  }
}
