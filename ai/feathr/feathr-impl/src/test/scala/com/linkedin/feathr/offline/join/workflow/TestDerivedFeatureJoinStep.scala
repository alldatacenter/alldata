package com.linkedin.feathr.offline.join.workflow

import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureTypes}
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.derived.{DerivedFeature, DerivedFeatureEvaluator}
import com.linkedin.feathr.offline.join.JoinExecutionContext
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlan}
import com.linkedin.feathr.offline.{ErasedEntityTaggedFeature, FeatureDataFrame, TestFeathr}
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert._
import org.testng.annotations.Test

/**
 * Unit test class for [[DerivedFeatureJoinStep]]
 */
class TestDerivedFeatureJoinStep extends TestFeathr with MockitoSugar {

  /**
   * Test when join stages are empty. Should return input DF and empty map for inferred types.
   */
  @Test
  def testJoinStepWhenJoinStagesIsEmpty(): Unit = {
    // mock execution context
    val mockExecutionContext = mock[JoinExecutionContext]
    val mockLogicalPlan = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockDerivedFeatureUtils = mock[DerivedFeatureEvaluator]
    when(mockExecutionContext.logicalPlan).thenReturn(mockLogicalPlan)
    when(mockExecutionContext.featureGroups).thenReturn(mockFeatureGroups)
    when(mockLogicalPlan.joinStages).thenReturn(Seq.empty) // stage derived is empty
    when(mockLogicalPlan.convertErasedEntityTaggedToJoinStage(any())).thenReturn(Seq.empty) // post join derived is empty
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map.empty[String, FeatureAnchorWithSource])
    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map.empty[String, DerivedFeature])
    when(mockExecutionContext.sparkSession).thenReturn(ss)

    // mock derived join step input
    val mockDerivedStepInput = mock[DataFrameJoinStepInput]
    when(mockDerivedStepInput.observation).thenReturn(ss.emptyDataFrame)
    val derivedFeatureJoinStep = DerivedFeatureJoinStep(mockDerivedFeatureUtils)
    val FeatureDataFrameOutput(FeatureDataFrame(outputDF, inferredFeatureType)) =
      derivedFeatureJoinStep.joinFeatures(Seq(ErasedEntityTaggedFeature(Seq(0), "featureName1")), mockDerivedStepInput)(mockExecutionContext)

    // validate output
    assertTrue(outputDF.collect().isEmpty)
    assertTrue(inferredFeatureType.isEmpty)

    // verify mocks
    verify(mockExecutionContext, atLeast(1)).logicalPlan
    verify(mockLogicalPlan).joinStages
    verify(mockLogicalPlan).convertErasedEntityTaggedToJoinStage(any())
    verifyNoInteractions(mockDerivedFeatureUtils)
  }

  /**
   * Test that derivation is applied for derived features within a join stage
   * and when there are no post join derived features.
   */
  @Test
  def testJoinStepForStageDerivedFeatures(): Unit = {
    // mock execution context
    val mockExecutionContext = mock[JoinExecutionContext]
    val mockLogicalPlan = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivedFeatureUtils = mock[DerivedFeatureEvaluator]

    // mock execution context invocations
    when(mockExecutionContext.logicalPlan).thenReturn(mockLogicalPlan)
    when(mockExecutionContext.featureGroups).thenReturn(mockFeatureGroups)
    when(mockExecutionContext.sparkSession).thenReturn(ss)
    // mock logical plan invocations
    // stage derived features is non empty while post join derived is empty
    when(mockLogicalPlan.joinStages).thenReturn(Seq((Seq(0), Seq("feature1"))))
    when(mockLogicalPlan.convertErasedEntityTaggedToJoinStage(any())).thenReturn(Seq.empty) // post join derived is empty
    when(mockLogicalPlan.keyTagIntsToStrings).thenReturn(Seq("key1"))
    // mock feature group invocations
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map.empty[String, FeatureAnchorWithSource])
    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("feature1" -> mockDerivedFeature))
    // Note: The data returned here is not the accurate representation of feature data. Here, it does not have to be.
    when(mockDerivedFeatureUtils.evaluate(any(classOf[Seq[Int]]), any(classOf[Seq[String]]), any(classOf[DataFrame]), any(classOf[DerivedFeature])))
      .thenReturn(FeatureDataFrame(
        ss.createDataFrame(ss.sparkContext.emptyRDD[Row], StructType(Seq(StructField("key1", StringType), StructField("feature1", StringType)))),
        Map("feature1" -> new FeatureTypeConfig(FeatureTypes.CATEGORICAL))))

    // mock derived join step input
    val mockDerivedStepInput = mock[DataFrameJoinStepInput]
    when(mockDerivedStepInput.observation).thenReturn(ss.emptyDataFrame)
    val derivedFeatureJoinStep = DerivedFeatureJoinStep(mockDerivedFeatureUtils)
    val FeatureDataFrameOutput(FeatureDataFrame(outputDF, inferredFeatureType)) =
      derivedFeatureJoinStep.joinFeatures(Seq(ErasedEntityTaggedFeature(Seq(0), "featureName1")), mockDerivedStepInput)(mockExecutionContext)

    // validate output
    assertTrue(outputDF.collect().isEmpty)
    assertEquals(outputDF.columns.mkString(", "), "key1, feature1")
    assertTrue(inferredFeatureType.nonEmpty)

    // verify mocks
    verify(mockExecutionContext, atLeast(1)).logicalPlan
    verify(mockLogicalPlan).joinStages
    verify(mockLogicalPlan).convertErasedEntityTaggedToJoinStage(any())
    verify(mockDerivedFeatureUtils, times(1)).evaluate(any(classOf[Seq[Int]]), any(classOf[Seq[String]]), any(classOf[DataFrame]), any(classOf[DerivedFeature]))
  }

  /**
   * Test that both in stage derived features and post join derived features are evaluated
   * in this step.
   */
  @Test
  def testJoinStepForAllTypesOfDerivedFeatures(): Unit = {
    // mock execution context
    val mockExecutionContext = mock[JoinExecutionContext]
    val mockLogicalPlan = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockDerivedFeature = mock[DerivedFeature]
    val mockDerivedFeatureUtils = mock[DerivedFeatureEvaluator]

    // mock execution context invocations
    when(mockExecutionContext.logicalPlan).thenReturn(mockLogicalPlan)
    when(mockExecutionContext.featureGroups).thenReturn(mockFeatureGroups)
    when(mockExecutionContext.sparkSession).thenReturn(ss)
    // mock get derived features
    when(mockLogicalPlan.joinStages).thenReturn(Seq((Seq(0), Seq("feature1", "feature3"))))
    when(mockLogicalPlan.convertErasedEntityTaggedToJoinStage(any())).thenReturn(Seq((Seq(0), Seq("feature2"))))
    when(mockLogicalPlan.keyTagIntsToStrings).thenReturn(Seq("key1"))
    // mock feature group invocations
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map.empty[String, FeatureAnchorWithSource])
    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("feature1" -> mockDerivedFeature, "feature2" -> mockDerivedFeature))
    // Note: The data returned here is not the accurate representation of feature data. Here, it does not have to be.
    when(mockDerivedFeatureUtils.evaluate(any(classOf[Seq[Int]]), any(classOf[Seq[String]]), any(classOf[DataFrame]), any(classOf[DerivedFeature])))
      .thenReturn(FeatureDataFrame(
        ss.createDataFrame(
          ss.sparkContext.emptyRDD[Row],
          StructType(Seq(StructField("key1", StringType), StructField("feature1", StringType), StructField("feature2", StringType)))),
        Map("feature1" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG, "feature2" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG)))

    // mock derived join step input
    val mockDerivedStepInput = mock[DataFrameJoinStepInput]
    when(mockDerivedStepInput.observation).thenReturn(ss.emptyDataFrame)
    val derivedFeatureJoinStep = DerivedFeatureJoinStep(mockDerivedFeatureUtils)
    val FeatureDataFrameOutput(FeatureDataFrame(outputDF, inferredFeatureType)) =
      derivedFeatureJoinStep.joinFeatures(
        Seq(ErasedEntityTaggedFeature(Seq(0), "featureName1"), ErasedEntityTaggedFeature(Seq(0), "featureName2")),
        mockDerivedStepInput)(mockExecutionContext)

    // validate output
    assertTrue(outputDF.collect().isEmpty)
    assertEquals(outputDF.columns.mkString(", "), "key1, feature1, feature2")
    assertTrue(inferredFeatureType.nonEmpty)

    // verify mocks
    verify(mockExecutionContext, atLeast(1)).logicalPlan
    verify(mockLogicalPlan).joinStages
    verify(mockLogicalPlan).convertErasedEntityTaggedToJoinStage(any())
    // verify applied derived feature is called exactly twice
    verify(mockDerivedFeatureUtils, times(2)).evaluate(any(classOf[Seq[Int]]), any(classOf[Seq[String]]), any(classOf[DataFrame]), any(classOf[DerivedFeature]))
  }
}
