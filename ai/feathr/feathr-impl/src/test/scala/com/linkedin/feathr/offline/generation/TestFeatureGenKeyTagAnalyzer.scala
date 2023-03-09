package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.exception.FeathrException
import com.linkedin.feathr.common.{ErasedEntityTaggedFeature, JoiningFeatureParams}
import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.job.FeatureGenSpec
import com.linkedin.feathr.offline.logical.FeatureGroups
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert._
import org.testng.annotations.Test

import java.util

/**
 * Unit test class for [[FeatureGenKeyTagAnalyzer]]
 */
class TestFeatureGenKeyTagAnalyzer extends TestFeathr with MockitoSugar {

  /**
   * Test [[FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures()]] when inferred anchor keyTags is empty.
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*Could not find inferred keyTags for anchored feature.*")
  def testComputeDerivedKeyTagsWhenInferredIsEmpty(): Unit = {
    val mockFeatureGenSpec = mock[FeatureGenSpec]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockFeatureAnchorWithSource = mock[FeatureAnchorWithSource]
    val mockDerivedFeature = mock[DerivedFeature]
    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))
    when(mockFeatureGenSpec.getFeatures()).thenReturn(Seq("f", "g", "h"))
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map("f" -> mockFeatureAnchorWithSource, "g" -> mockFeatureAnchorWithSource))
    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures(mockFeatureGenSpec, mockFeatureGroups, Seq.empty[JoiningFeatureParams])
  }

  /**
   * Test [[FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures()]] when a keyTag for dependent feature is missing.
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*Could not find inferred keyTags for anchored feature.*")
  def testComputeDerivedKeyTagsWhenInferredIsKeyTagIsMissing(): Unit = {
    val mockFeatureGenSpec = mock[FeatureGenSpec]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockFeatureAnchorWithSource = mock[FeatureAnchorWithSource]
    val mockDerivedFeature = mock[DerivedFeature]
    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))
    when(mockFeatureGenSpec.getFeatures()).thenReturn(Seq("f", "g", "h"))
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map("f" -> mockFeatureAnchorWithSource, "g" -> mockFeatureAnchorWithSource))
    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures(
      mockFeatureGenSpec,
      mockFeatureGroups,
      Seq(new JoiningFeatureParams(Seq("x"), "f")))
  }

  /**
   * Basic test [[FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures()]].
   */
  @Test
  def testComputeDerivedKeyTags(): Unit = {
    val mockFeatureGenSpec = mock[FeatureGenSpec]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockFeatureAnchorWithSource = mock[FeatureAnchorWithSource]
    val mockDerivedFeature = mock[DerivedFeature]
    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))
    when(mockFeatureGenSpec.getFeatures()).thenReturn(Seq("f", "g", "h"))
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map("f" -> mockFeatureAnchorWithSource, "g" -> mockFeatureAnchorWithSource))
    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    val keyTaggedDerivedFeature = FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures(
      mockFeatureGenSpec,
      mockFeatureGroups,
      Seq(new JoiningFeatureParams(Seq("x"), "f"), new JoiningFeatureParams(Seq("x"), "g")))
    assertEquals(keyTaggedDerivedFeature.size, 1)
    assertEquals(keyTaggedDerivedFeature.head.featureName, "h")
    assertEquals(s"[${keyTaggedDerivedFeature.head.keyTags.mkString(",")}]", "[x]")
  }

  /**
   * Test [[FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures()]] fails for cross join scenarios.
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*does not support cross join.*")
  def testComputeDerivedKeyTagsFailsForCrossJoin(): Unit = {
    val mockFeatureGenSpec = mock[FeatureGenSpec]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockFeatureAnchorWithSource = mock[FeatureAnchorWithSource]
    val mockDerivedFeature = mock[DerivedFeature]
    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))
    when(mockFeatureGenSpec.getFeatures()).thenReturn(Seq("f", "g", "h"))
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map("f" -> mockFeatureAnchorWithSource, "g" -> mockFeatureAnchorWithSource))
    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    val keyTagAnalyzer = FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures(
      mockFeatureGenSpec,
      mockFeatureGroups,
      Seq(new JoiningFeatureParams(Seq("x"), "f"), new JoiningFeatureParams(Seq("y"), "g")))
  }

  /**
   * Test [[FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures()]] does not wrongly categorize
   * multi-key dependent features as cross join scenario.
   */
  @Test
  def testComputeDerivedKeyTagsSucceedsForValidMultiKeyCase(): Unit = {
    val mockFeatureGenSpec = mock[FeatureGenSpec]
    val mockFeatureGroups = mock[FeatureGroups]
    val mockFeatureAnchorWithSource = mock[FeatureAnchorWithSource]
    val mockDerivedFeature = mock[DerivedFeature]
    val erasedEntityTaggedAnchored =
      Seq(new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "f"), new ErasedEntityTaggedFeature(new util.ArrayList[Integer](0), "g"))
    when(mockFeatureGenSpec.getFeatures()).thenReturn(Seq("f", "g", "h"))
    when(mockFeatureGroups.allAnchoredFeatures).thenReturn(Map("f" -> mockFeatureAnchorWithSource, "g" -> mockFeatureAnchorWithSource))
    when(mockFeatureGroups.allDerivedFeatures).thenReturn(Map("h" -> mockDerivedFeature))
    when(mockDerivedFeature.consumedFeatureNames).thenReturn(erasedEntityTaggedAnchored)

    val keyTaggedDerivedFeature = FeatureGenKeyTagAnalyzer.inferKeyTagsForDerivedFeatures(
      mockFeatureGenSpec,
      mockFeatureGroups,
      Seq(
        new JoiningFeatureParams(Seq("x", "y"), "f"),
        new JoiningFeatureParams(Seq("x", "y"), "g")))
    assertEquals(keyTaggedDerivedFeature.size, 1)
    assertEquals(keyTaggedDerivedFeature.head.featureName, "h")
    assertEquals(s"[${keyTaggedDerivedFeature.head.keyTags.mkString(",")}]", "[x,y]")
  }
}
