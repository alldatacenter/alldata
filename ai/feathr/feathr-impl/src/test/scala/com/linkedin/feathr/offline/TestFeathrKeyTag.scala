package com.linkedin.feathr.offline

import com.linkedin.feathr.offline.client.FeathrClient
import com.linkedin.feathr.offline.config.{FeatureJoinConfig, KeyedFeatureList}
import com.linkedin.feathr.offline.job.LocalFeatureJoinJob
import org.apache.spark.sql.Row
import org.testng.Assert.assertEquals
import org.testng.annotations.{BeforeClass, Test}

class TestFeathrKeyTag extends TestFeathr {

  @BeforeClass
  override def setFeathrConfig(): Unit = {
    val feathrConfs = List(feathrConfigLoader.load(getClass.getClassLoader.getResource("feathrConf-default.conf")))
    feathr = FeathrClient.builder(ss).addFeatureDefConfs(Some(feathrConfs)).build()
  }

  /*
    Tests a list of single-keyed anchored features, not all of which have the same key (y and x)
   */
  @Test
  def testWithDifferentKeys(): Unit = {
    val trainingData = "test1-observations.csv"
    val keyedFeatureLists = Seq(
      KeyedFeatureList(Seq("a"), Seq("a_isBanana")),
      KeyedFeatureList(Seq("a"), Seq("a_sum")),
      KeyedFeatureList(Seq("a"), Seq("a_y")),
      KeyedFeatureList(Seq("b"), Seq("a_abc")),
      KeyedFeatureList(Seq("b"), Seq("a_num")))
    val joinConfig = FeatureJoinConfig(Map("testWithDifferentKeys" -> keyedFeatureLists))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())

    val joinOuput = feathr.joinFeatures(joinConfig, obsData)
    val joined = joinOuput.data.collect().sortBy(row => (row.getAs[String]("a"), row.getAs[String]("b")))

    assertEquals(joined(0).getAs[Float]("a_sum"), 40.5f)
    assertEquals(joined(0).getAs[Float]("a_num"), 10.0f)
    assertEquals(joined(0).getAs[Boolean]("a_isBanana"), false)
    assertEquals(joined(0).getAs[Boolean]("a_abc"), true)
    assertEquals(joined(0).getAs[Float]("a_y"), 65.2f)

    assertEquals(joined(1).getAs[Float]("a_sum"), 18.1f)
    assertEquals(joined(1).getAs[Float]("a_num"), 9.0f)
    assertEquals(joined(1).getAs[Boolean]("a_isBanana"), false)
    assertEquals(joined(1).getAs[Boolean]("a_abc"), true)
    assertEquals(joined(1).getAs[Float]("a_y"), 92.2f)

    assertEquals(joined(2).getAs[Float]("a_sum"), 31.1f)
    assertEquals(joined(2).getAs[Float]("a_num"), 9.0f)
    assertEquals(joined(2).getAs[Boolean]("a_isBanana"), true)
    assertEquals(joined(2).getAs[Boolean]("a_abc"), true)
    assertEquals(joined(2).getAs[Float]("a_y"), 77.5f)

    assertEquals(joined(3).getAs[Float]("a_sum"), 31.1f)
    assertEquals(joined(3).getAs[Float]("a_num"), null)
    assertEquals(joined(3).getAs[Boolean]("a_isBanana"), true)
    assertEquals(joined(3).getAs[Boolean]("a_abc"), false)
    assertEquals(joined(3).getAs[Float]("a_y"), 77.5f)

    assertEquals(joined(4).getAs[Float]("a_sum"), 28.9f)
    assertEquals(joined(4).getAs[Float]("a_num"), 15.0f)
    assertEquals(joined(4).getAs[Boolean]("a_isBanana"), false)
    assertEquals(joined(4).getAs[Boolean]("a_abc"), false)
    assertEquals(joined(4).getAs[Float]("a_y"), 34.0f)
  }

  /*
    Tests multi-key derived feature, depending on features that need to be joined in two separate stages (x and y)
    The feature depends on another derived feature, and transitively depends on two a features and one a feature.
    Note that the dependencies aren't provided by the caller here, Feathr must figure out the dependencies, pull from the right
    anchors, and compute the dependency features. This behavior is new in Feathr 0.2.*
   */
  @Test
  def testWithMultiKeyDerivedFeatures(): Unit = {

    val trainingData = "test1-observations.csv"
    val keyedFeatureLists = Seq(KeyedFeatureList(Seq("a", "b"), Seq("foobar_dualkey_feature2")))
    val joinConfig = FeatureJoinConfig(Map("testWithMultiKeyDerivedFeatures" -> keyedFeatureLists))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())

    val joinOuput = feathr.joinFeatures(joinConfig, obsData)
    val joined = joinOuput.data.collect().sortBy(row => (row.getAs[String]("a"), row.getAs[String]("b")))

    AssertFeatureUtils.rowApproxEquals(joined(0).getAs[Row]("foobar_dualkey_feature2"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(1.5199387)))
    AssertFeatureUtils.rowApproxEquals(joined(1).getAs[Row]("foobar_dualkey_feature2"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(1.015217f)))
    AssertFeatureUtils.rowApproxEquals(joined(2).getAs[Row]("foobar_dualkey_feature2"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(4.9194965f)))
    AssertFeatureUtils.rowApproxEquals(joined(3).getAs[Row]("foobar_dualkey_feature2"), null)
    AssertFeatureUtils.rowApproxEquals(joined(4).getAs[Row]("foobar_dualkey_feature2"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(295.6356f)))
  }

  /*
    Tests getting a particular a feature for viewer and viewee. Also get a derived-feature for viewee only.
   */
  @Test
  def testFeatureWithDifferentKeyTag(): Unit = {
    val trainingData = "test2-observations.csv"
    val keyedFeatureLists = Seq(
      KeyedFeatureList(Seq("viewer"), Seq("f2")),
      KeyedFeatureList(Seq("viewee"), Seq("f2")),
      KeyedFeatureList(Seq("viewee"), Seq("a_simple_derived_feature")))
    val joinConfig = FeatureJoinConfig(Map("testFeatureWithDifferentKeyTag" -> keyedFeatureLists))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())

    val joinOuput = feathr.joinFeatures(joinConfig, obsData)
    val joined = joinOuput.data.collect().sortBy(row => (row.getAs[String]("viewer"), row.getAs[String]("viewee")))

    assertEquals(joined(0).getAs[Float]("viewer__f2"), 100.0f)
    assertEquals(joined(0).getAs[Float]("viewee__f2"), 501.0f)
    AssertFeatureUtils.rowApproxEquals(joined(0).getAs[Row]("a_simple_derived_feature"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(1.0f)))

    assertEquals(joined(1).getAs[Float]("viewer__f2"), 123.0f)
    assertEquals(joined(1).getAs[Float]("viewee__f2"), 100.0f)
    AssertFeatureUtils.rowApproxEquals(joined(1).getAs[Row]("a_simple_derived_feature"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(1.0f)))

    assertEquals(joined(2).getAs[Float]("viewer__f2"), 501.0f)
    assertEquals(joined(2).getAs[Float]("viewee__f2"), 21341.0f)
    AssertFeatureUtils.rowApproxEquals(joined(2).getAs[Row]("a_simple_derived_feature"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(1.0f)))

    assertEquals(joined(3).getAs[Float]("viewer__f2"), 501.0f)
    assertEquals(joined(3).getAs[Float]("viewee__f2"), 34.0f)
    AssertFeatureUtils.rowApproxEquals(joined(3).getAs[Row]("a_simple_derived_feature"), TestUtils.build1dSparseTensorFDSRow(Array(), Array()))

    assertEquals(joined(4).getAs[Float]("viewer__f2"), 21341.0f)
    assertEquals(joined(4).getAs[Float]("viewee__f2"), 452.0f)
    AssertFeatureUtils.rowApproxEquals(joined(4).getAs[Row]("a_simple_derived_feature"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(1.0f)))
  }

  /*
   * Test with different types of feature values
   */
  @Test
  def testWithDifferentTypeOfFV(): Unit = {
    val trainingData = "test1-observations.csv"
    val keyedFeatureLists = Seq(
      KeyedFeatureList(Seq("a"), Seq("a_isBanana")),
      KeyedFeatureList(Seq("a"), Seq("a_sum")),
      KeyedFeatureList(Seq("a"), Seq("a_y")))
    val joinConfig = FeatureJoinConfig(Map("testWithDifferentTypeOfFV" -> keyedFeatureLists))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())

    val joinOuput = feathr.joinFeatures(joinConfig, obsData)
    val joined = joinOuput.data.collect().sortBy(row => (row.getAs[String]("a"), row.getAs[String]("b")))

    assertEquals(joined(0).getAs[Boolean]("a_isBanana"), false)
    assertEquals(joined(0).getAs[Float]("a_sum"), 40.5f)
    assertEquals(joined(0).getAs[Float]("a_y"), 65.2f)

    assertEquals(joined(1).getAs[Boolean]("a_isBanana"), false)
    assertEquals(joined(1).getAs[Float]("a_sum"), 18.1f)
    assertEquals(joined(1).getAs[Float]("a_y"), 92.2f)

    assertEquals(joined(2).getAs[Boolean]("a_isBanana"), true)
    assertEquals(joined(2).getAs[Float]("a_sum"), 31.1f)
    assertEquals(joined(2).getAs[Float]("a_y"), 77.5f)

    assertEquals(joined(3).getAs[Boolean]("a_isBanana"), true)
    assertEquals(joined(3).getAs[Float]("a_sum"), 31.1f)
    assertEquals(joined(3).getAs[Float]("a_y"), 77.5f)

    assertEquals(joined(4).getAs[Boolean]("a_isBanana"), false)
    assertEquals(joined(4).getAs[Float]("a_sum"), 28.9f)
    assertEquals(joined(4).getAs[Float]("a_y"), 34.0f)
  }

  /*
   * Test with numeric features
   */
  @Test
  def testMathLog(): Unit = {
    val trainingData = "test4-observations.csv"
    val keyedFeatureList = Seq(
      KeyedFeatureList(Seq("a"), Seq("anchoredF1")),
      KeyedFeatureList(Seq("a"), Seq("a_omega_logA")),
      KeyedFeatureList(Seq("a"), Seq("derivedF1")))
    val joinConfig = FeatureJoinConfig(Map("testMathLog" -> keyedFeatureList))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())

    val joinOuput = feathr.joinFeatures(joinConfig, obsData)
    val joined = joinOuput.data.collect().sortBy(row => (row.getAs[String]("a")))

    assertEquals(joined(0).getAs[Float]("anchoredF1"), 0.1f)
    assertEquals(joined(0).getAs[Float]("a_omega_logA"), -2.3025851, 0.0001)
    AssertFeatureUtils.rowApproxEquals(joined(0).getAs[Row]("derivedF1"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(-2.3025851)))

    assertEquals(joined(1).getAs[Float]("anchoredF1"), 0.1f)
    assertEquals(joined(1).getAs[Float]("a_omega_logA"), -2.3025851, 0.0001)
    AssertFeatureUtils.rowApproxEquals(joined(1).getAs[Row]("derivedF1"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(-2.3025851)))

    assertEquals(joined(2).getAs[Float]("anchoredF1"), 0.9f)
    assertEquals(joined(2).getAs[Float]("a_omega_logA"), -0.105360545, 0.0001)
    AssertFeatureUtils.rowApproxEquals(joined(2).getAs[Row]("derivedF1"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(-0.105360545)))

    assertEquals(joined(3).getAs[Float]("anchoredF1"), 0.7f)
    assertEquals(joined(3).getAs[Float]("a_omega_logA"), -0.35667497, 0.0001)
    AssertFeatureUtils.rowApproxEquals(joined(3).getAs[Row]("derivedF1"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(-0.35667497)))

    assertEquals(joined(4).getAs[Float]("anchoredF1"), 1.0f)
    assertEquals(joined(4).getAs[Float]("a_omega_logA"), 0, 0.0001)
    AssertFeatureUtils.rowApproxEquals(joined(4).getAs[Row]("derivedF1"), TestUtils.build1dSparseTensorFDSRow(Array(), Array(0.0)))

    assertEquals(joined(5).getAs[Float]("anchoredF1"), null)
    assertEquals(joined(5).getAs[Float]("a_omega_logA"), null)
    AssertFeatureUtils.rowApproxEquals(joined(5).getAs[Row]("derivedF1"), null)
  }

  @Test(enabled = true)
  def testMVELLoopExpFeature(): Unit = {
    val trainingData = "testMVELLoopExpFeature-observations.csv"
    val keyedFeatureList = Seq(KeyedFeatureList(Seq("a_id"), Seq("aMap")))
    val joinConfig = FeatureJoinConfig(Map("testMVELLoopExpFeature" -> keyedFeatureList))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())

    val feathrConfs = List(feathrConfigLoader.load(getClass.getClassLoader.getResource("testMVELLoopExpFeature.conf")))
    val feathrClient = FeathrClient.builder(ss).addFeatureDefConfs(Some(feathrConfs)).build()
    val joinOuput = feathrClient.joinFeatures(joinConfig, obsData)
    val joined = joinOuput.data.collect().sortBy(row => (row.getAs[String]("a_id")))

    assertEquals(joined(0).getAs[Row]("aMap"), TestUtils.build1dSparseTensorFDSRow(Array("1"), Array(0.5)))
    AssertFeatureUtils.rowApproxEquals(joined(1).getAs[Row]("aMap"), TestUtils.build1dSparseTensorFDSRow(Array("456"), Array(0.7)))
    AssertFeatureUtils.rowApproxEquals(joined(2).getAs[Row]("aMap"), TestUtils.build1dSparseTensorFDSRow(Array("6", "5"), Array(0.78, 0.1)))
  }
}
