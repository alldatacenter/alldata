package com.linkedin.feathr.offline.mvel

import com.linkedin.feathr.offline.anchored.anchorExtractor.SimpleConfigurableAnchorExtractor
import com.linkedin.feathr.offline.client.FeathrClient
import com.linkedin.feathr.offline.config.{FeatureJoinConfig, KeyedFeatureList}
import com.linkedin.feathr.offline.job.LocalFeatureJoinJob
import com.linkedin.feathr.offline.{TestFeathr, TestUtils}
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.spark.sql.Row
import org.testng.Assert._
import org.testng.annotations.Test

class TestFeathrMVEL extends TestFeathr {

  /**
   * Test if errors such as NPE from the MVEL expression would be gracefully handled
   * When test runs successfully, an MVEL PropertyAccessException containing an NPE
   * should be caught from applying SimpleConfigurableAnchorExtractor, because we deliberately
   * used in the feature definition a method that doesn't exist.
   * TODO: org.apache.avro.AvroRuntimeException: Not a valid schema field: foo is thrown and this is not
   * gracefully handled. Modify test to reflect this behavior.
   */
  @Test(enabled = false)
  def testWrongMVELExpressionFeature(): Unit = {

    val feathrClient = FeathrClient.builder(ss).addFeatureDef(Some(FeathrMvelFixture.wrongMVELExpressionFeatureConf)).build()

    val extractor = feathrClient.allAnchoredFeatures.get("wrongExpFeature").get.featureAnchor.extractor.asInstanceOf[SimpleConfigurableAnchorExtractor]

    // Build a fake Avro datum
    val inputSchema = SchemaBuilder
      .record("Input")
      .fields()
      .name("alpha")
      .`type`()
      .floatType()
      .noDefault
      .endRecord()
    val datum = new GenericRecordBuilder(inputSchema).set("alpha", 1.0).build()

    // the NPE should be caught and handled gracefully by returning an empty features map
    assertTrue(extractor.getFeatures(datum).isEmpty)
  }

  /**
   * Test if null values from data would be handled gracefully without causing job failure,
   * when strictMode is disabled.
   *
   * The corresponding feature name is logged
   */
  @Test
  def testMVELFeatureWithNullValue(): Unit = {
    val trainingData = "testMVELFeatureWithNullValue-observations.csv"
    val keyedFeatureList = Seq(KeyedFeatureList(Seq("mId"), Seq("featureWithNull")))
    val joinConfig = FeatureJoinConfig(Map("mvelFeaturesWithNull" -> keyedFeatureList))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())
    val feathrClient = FeathrClient.builder(ss).addFeatureDef(Some(FeathrMvelFixture.mvelFeatureWithNullValueConf)).build()

    val joined = feathrClient.joinFeatures(joinConfig, obsData)

    val featureList = joined.data.collect().toList
    assertEquals(featureList.size, 3)
    assertEquals(featureList(0).getAs[Float]("featureWithNull"), 2.0f)
    assertEquals(featureList(1).getAs[Float]("featureWithNull"), null)
    assertEquals(featureList(2).getAs[Float]("featureWithNull"), 4.0f)
  }

  /**
   * Test on nested records with null.
   */
  @Test
  def testMVELFeatureWithNextedRecordsAndNullValue(): Unit = {
    val trainingData = "testMVELFeatureWithNullValue-observations.csv"
    val keyedFeatureList = Seq(KeyedFeatureList(Seq("mId"), Seq("featureWithNull2")))
    val joinConfig = FeatureJoinConfig(Map("mvelFeaturesWithNull" -> keyedFeatureList))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())
    val feathrClient = FeathrClient.builder(ss).addFeatureDef(Some(FeathrMvelFixture.mvelFeatureWithNullValueConf)).build()

    val joined = feathrClient.joinFeatures(joinConfig, obsData)

    val featureList = joined.data.collect().toList
    assertEquals(featureList.size, 3)
    assertEquals(featureList(0).getAs[Float]("featureWithNull2"), 2.0f)
    assertEquals(featureList(1).getAs[Float]("featureWithNull2"), null)
    assertEquals(featureList(2).getAs[Float]("featureWithNull2"), null)
  }

  /**
   * Test if VECTOR type data contains NULL value, the feature-join job would throw exception as expected
   *
   * If VECTOR type data contains a NULL, e.g., [0.1, null, 0.3], and test runs successfully,
   * an Exception containing an NPE should be caught.
   *
   */
  @Test
  def testMVELFeatureWithNullValueInVector(): Unit = {
    val trainingData = "testMVELFeatureWithNullValue-observations.csv"
    val keyedFeatureList = Seq(KeyedFeatureList(Seq("mId"), Seq("featureWithNull3")))
    val joinConfig = FeatureJoinConfig(Map("mvelFeaturesWithNull" -> keyedFeatureList))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())
    val feathrClient = FeathrClient.builder(ss).addFeatureDef(Some(FeathrMvelFixture.mvelFeatureWithNullValueConf)).build()
    try {
      feathrClient.joinFeatures(joinConfig, obsData)
    } catch {
      case npe: Exception => assertNull(null)
    }
  }

  /**
   * A test to demonstrate how null-checking derived feature should be defined.
   *
   * null-checking derived features are the features that checks if a parent feature contains null value:
   * e.g. "parentFeature == null ? parentFeature : someOtherThing"
   *
   * Since null value in derived feature would result in an NullPointerException {See @FeatureVariableResolverFactory},
   * such null-checking derived feature is not allowed.
   *
   * Instead, user must define a proper default value for parentFeature and check on the default value in the derived
   * feature, e.g.
   * "parentFeature == 0 ? parentFeature : someOtherThing" (assume 0 is the default value for parentFeature).
   */
  @Test
  def testMVELDerivedFeatureCheckingNull(): Unit = {
    val trainingData = "testMVELDerivedFeatureCheckingNull-observations.csv"
    val keyedFeatureList = Seq(KeyedFeatureList(Seq("mId"), Seq("checkingZero")), KeyedFeatureList(Seq("mId"), Seq("checkingEmpty")))
    val joinConfig = FeatureJoinConfig(Map("mvelDerivedFeatureWithNull" -> keyedFeatureList))
    val obsData = LocalFeatureJoinJob.loadObservationAsFDS(ss, trainingData, List())
    val feathrClient = FeathrClient.builder(ss).addFeatureDef(Some(FeathrMvelFixture.mvelDerivedFeatureCheckingNullConf)).build()

    val joined = feathrClient.joinFeatures(joinConfig, obsData)

    val featureList = joined.data.collect().toList
    assertEquals(featureList.size, 3)
    assertEquals(featureList(0).getAs[Row]("checkingZero"), TestUtils.build1dSparseTensorFDSRow(Array(""), Array(1.0f)))
    assertEquals(featureList(0).getAs[Row]("checkingEmpty"), TestUtils.build1dSparseTensorFDSRow(Array(""), Array(1.0f)))
    assertEquals(featureList(0).getAs[Row]("checkingZero"), TestUtils.build1dSparseTensorFDSRow(Array(""), Array(1.0f)))
    assertEquals(featureList(0).getAs[Row]("checkingEmpty"), TestUtils.build1dSparseTensorFDSRow(Array(""), Array(1.0f)))
    assertEquals(featureList(0).getAs[Row]("checkingZero"), TestUtils.build1dSparseTensorFDSRow(Array(""), Array(1.0f)))
    assertEquals(featureList(0).getAs[Row]("checkingEmpty"), TestUtils.build1dSparseTensorFDSRow(Array(""), Array(1.0f)))
  }
}
