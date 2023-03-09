package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.FeatureTypeConfig
import com.linkedin.feathr.offline
import com.linkedin.feathr.offline.{FeatureDataFrame, JoinKeys, TestFeathr}
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.job.FeatureGenSpec
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlan}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * Unit test class for PostGenPruner.
 */
class TestPostGenPruner extends TestFeathr with MockitoSugar {

  /**
   * Test postGenPruningStage should:
   * 1. set key columns to non-nullable and feature columns to nullable
   * 2. standardize key columns to key0, key1, key2, etc.
   */
  @Test
  def testStandardizedKeyColumnsAndNonNullableFeatureColumns(): Unit = {
    val mockAnalyzeFeatureInfo = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockFeatureGenSpec = mock[FeatureGenSpec]

    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map.empty[String, DerivedFeature])
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map.empty[String, FeatureAnchorWithSource])
    when(mockAnalyzeFeatureInfo.allRequiredFeatures).thenReturn(Seq(offline.ErasedEntityTaggedFeature(Seq(0), "f")))
    when(mockAnalyzeFeatureInfo.keyTagIntsToStrings).thenReturn(Seq("originalKey"))
    when(mockFeatureGenSpec.getFeatures()).thenReturn(Seq("f"))
    val postGenPruningStage = new PostGenPruner()
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df =
      FeatureDataFrame(Seq((1, 1.0f), (2, 2.0f)).toDF("originalKey", s"${FeathrFeatureNamePrefix}f"), Map("f" -> FeatureTypeConfig.NUMERIC_TYPE_CONFIG))
    val featureDataWithJoinKeys: Map[String, (FeatureDataFrame, JoinKeys)] = Map("f" -> (df, Seq("originalKey")))
    val featureData = postGenPruningStage.prune(featureDataWithJoinKeys, mockFeatureGenSpec.getFeatures(), mockAnalyzeFeatureInfo, mockFeatureGroups)
    val columnFields = featureData.head._2._1.schema.fields
    // key column name should be key0
    assertEquals(columnFields(0).name, "key0")
    // key column should be non-nullable
    assertEquals(columnFields(0).nullable, false)
    // feature column should be nullable
    assertEquals(columnFields(1).nullable, true)
  }
}
