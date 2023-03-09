package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.exception.FeathrException
import com.linkedin.feathr.common.{ErasedEntityTaggedFeature, FeatureTypeConfig}
import com.linkedin.feathr.offline.derived.{DerivedFeature, DerivedFeatureEvaluator}
import com.linkedin.feathr.offline.evaluator.{BaseDataFrameMetadata, DerivedFeatureGenStage}
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlan}
import com.linkedin.feathr.offline.{FeatureDataFrame, TestFeathr}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert._
import org.testng.annotations.Test

import java.util

class TestStageEvaluator extends TestFeathr with MockitoSugar {

  /**
   * Test evaluateStage returns the input DataFrame if the derived feature already exists.
   */
  @Test
  def testEvaluateStageSkipsComputationIfFeatureExists(): Unit = {
    val mockAnalyzeFeatureInfo = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivedFeatureUtils = mock[DerivedFeatureEvaluator]

    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))

    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    val derivedFeatureGenStage = new DerivedFeatureGenStage(mockFeatureGroups, mockAnalyzeFeatureInfo, mockDerivedFeatureUtils)
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df =
      Seq((1, "f1", "g1", "h1"), (2, "f2", "g2", "h2")).toDF("key", s"${FeathrFeatureNamePrefix}f", s"${FeathrFeatureNamePrefix}g", s"${FeathrFeatureNamePrefix}h")
    val featureTypeMap =
      Map("f" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG, "g" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG, "h" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG)
    val featureDataFrame = FeatureDataFrame(df, featureTypeMap)
    val evaluatedMap =
      derivedFeatureGenStage.evaluate(Seq("h"), Seq(0), Map("f" -> (featureDataFrame, Seq("key")), "g" -> (featureDataFrame, Seq("key")))).groupBy(_._2._1.df)
    assertEquals(evaluatedMap.size, 1) // exactly one DataFrame
    assertEquals(evaluatedMap.head._2.size, 3)
    assertTrue(evaluatedMap.head._2.contains("h"))
    verifyNoInteractions(mockDerivedFeatureUtils)
  }

  /**
   * Test evaluateBaseDataFrameForDerivation joins DataFrames if the consumed features are on different DataFrames.
   */
  @Test
  def testEvaluateBaseDataFrameWhenFeaturesAreOnDifferentDf(): Unit = {
    val mockAnalyzeFeatureInfo = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivedFeatureUtils = mock[DerivedFeatureEvaluator]

    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))

    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    val derivedFeatureGenStage = new DerivedFeatureGenStage(mockFeatureGroups, mockAnalyzeFeatureInfo, mockDerivedFeatureUtils)
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df1 =
      FeatureDataFrame(Seq((1, "f1"), (2, "f2")).toDF("key", s"${FeathrFeatureNamePrefix}f"), Map("f" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG))
    val df2 =
      FeatureDataFrame(Seq((1, "g1"), (2, "g2")).toDF("key", s"${FeathrFeatureNamePrefix}g"), Map("g" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG))

    val BaseDataFrameMetadata(featureDataFrame, joinKeys, featuresOnBaseDf) =
      derivedFeatureGenStage
        .evaluateBaseDataFrameForDerivation("h", mockDerivedFeature, Map("f" -> (df1, Seq("key")), "g" -> (df2, Seq("key"))))
    assertEquals(featureDataFrame.df.columns.length, 3)
    assertEquals(s"[${joinKeys.mkString(",")}]", "[key]")
    assertEquals(s"[${featuresOnBaseDf.mkString(",")}]", s"[f,g]")
    verifyNoInteractions(mockDerivedFeatureUtils)
  }

  /**
   * Test evaluateBaseDataFrameForDerivation fails if dependent features are not already evaluated.
   */
  @Test(
    expectedExceptions = Array(classOf[FeathrException]),
    expectedExceptionsMessageRegExp = ".*Error when processing derived feature.*Requires following features to be generated.*")
  def testEvaluateBaseDataFrameFailsWhenDependentFeaturesDoesNotExist(): Unit = {
    val mockAnalyzeFeatureInfo = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivedFeatureUtils = mock[DerivedFeatureEvaluator]

    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))

    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    val derivedFeatureGenStage = new DerivedFeatureGenStage(mockFeatureGroups, mockAnalyzeFeatureInfo, mockDerivedFeatureUtils)
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df1 =
      FeatureDataFrame(Seq((1, "f1"), (2, "f2")).toDF("key", s"${FeathrFeatureNamePrefix}f"), Map("f" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG))
    derivedFeatureGenStage
      .evaluateBaseDataFrameForDerivation("h", mockDerivedFeature, Map("f" -> (df1, Seq("key"))))
  }

  /**
   * Test evaluateBaseDataFrameForDerivation fails if dependent features do not share the same join key.
   * This is a cross join scenario.
   */
  @Test(
    expectedExceptions = Array(classOf[FeathrException]),
    expectedExceptionsMessageRegExp = ".*Error when processing derived feature.*Join Keys for dependent feature do not match.*")
  def testEvaluateBaseDataFrameFailsWhenJoinKeysDoNotMatch(): Unit = {
    val mockAnalyzeFeatureInfo = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivedFeatureUtils = mock[DerivedFeatureEvaluator]

    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))

    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    val derivedFeatureGenStage = new DerivedFeatureGenStage(mockFeatureGroups, mockAnalyzeFeatureInfo, mockDerivedFeatureUtils)
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df1 =
      FeatureDataFrame(Seq((1, "f1"), (2, "f2")).toDF("key", s"${FeathrFeatureNamePrefix}f"), Map("f" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG))
    val df2 =
      FeatureDataFrame(Seq((1, "g1"), (2, "g2")).toDF("key", s"${FeathrFeatureNamePrefix}g"), Map("g" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG))

    derivedFeatureGenStage
      .evaluateBaseDataFrameForDerivation("h", mockDerivedFeature, Map("f" -> (df1, Seq("key")), "g" -> (df2, Seq("key", "rogueKey"))))
  }

  /**
   * Test evaluateBaseDataFrameForDerivation merges FeatureTypeMap.
   */
  @Test
  def testEvaluateBaseDataFrameMergesFeatureTypeMap(): Unit = {
    val mockAnalyzeFeatureInfo = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivedFeatureUtils = mock[DerivedFeatureEvaluator]

    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))

    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    val derivedFeatureGenStage = new DerivedFeatureGenStage(mockFeatureGroups, mockAnalyzeFeatureInfo, mockDerivedFeatureUtils)
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df1 =
      FeatureDataFrame(Seq((1, "f1"), (2, "f2")).toDF("key", s"${FeathrFeatureNamePrefix}f"), Map("f" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG))
    val df2 =
      FeatureDataFrame(Seq((1, "g1"), (2, "g2")).toDF("key", s"${FeathrFeatureNamePrefix}g"), Map("g" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG))

    val BaseDataFrameMetadata(featureDataFrame, joinKeys, featuresOnBaseDf) =
      derivedFeatureGenStage
        .evaluateBaseDataFrameForDerivation("h", mockDerivedFeature, Map("f" -> (df1, Seq("key")), "g" -> (df2, Seq("key"))))
    assertEquals(featureDataFrame.df.columns.length, 3)
    assertEquals(featureDataFrame.inferredFeatureType.keySet.size, 2)
    assertTrue(Seq("f", "g").forall(featureDataFrame.inferredFeatureType.keySet.contains))
    verifyNoInteractions(mockDerivedFeatureUtils)
  }

  /**
   * Test evaluateBaseDataFrameForDerivation does not fail FeatureTypeMap is empty.
   */
  @Test
  def testEvaluateBaseDataFrameWithEmptyFeatureTypeMap(): Unit = {
    val mockAnalyzeFeatureInfo = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivedFeatureUtils = mock[DerivedFeatureEvaluator]

    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))

    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    val derivedFeatureGenStage = new DerivedFeatureGenStage(mockFeatureGroups, mockAnalyzeFeatureInfo, mockDerivedFeatureUtils)
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df1 =
      FeatureDataFrame(Seq((1, "f1"), (2, "f2")).toDF("key", s"${FeathrFeatureNamePrefix}f"), Map.empty[String, FeatureTypeConfig])
    val df2 =
      FeatureDataFrame(Seq((1, "g1"), (2, "g2")).toDF("key", s"${FeathrFeatureNamePrefix}g"), Map.empty[String, FeatureTypeConfig])

    val BaseDataFrameMetadata(featureDataFrame, joinKeys, featuresOnBaseDf) =
      derivedFeatureGenStage
        .evaluateBaseDataFrameForDerivation("h", mockDerivedFeature, Map("f" -> (df1, Seq("key")), "g" -> (df2, Seq("key"))))
    assertEquals(featureDataFrame.df.columns.length, 3)
    assertEquals(s"[${joinKeys.mkString(",")}]", "[key]")
    assertEquals(s"[${featuresOnBaseDf.mkString(",")}]", s"[f,g]")
    verifyNoInteractions(mockDerivedFeatureUtils)
  }
}
