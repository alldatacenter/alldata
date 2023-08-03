package com.linkedin.feathr.offline.logical

import com.linkedin.feathr.common.JoiningFeatureParams
import com.linkedin.feathr.common.exception.FeathrConfigException
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.config.{FeatureGroupsGenerator, FeatureJoinConfig}
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.{ErasedEntityTaggedFeature, TestFeathr}
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.{assertEquals, assertTrue}
import org.testng.annotations.Test

/**
 * Unit test class for [[MultiStageJoinPlanner]].
 */
class TestMultiStageJoinPlan extends TestFeathr with MockitoSugar {

  /**
   * Test that [[MultiStageJoinPlanner.getDependenciesForFeature()]] returns an empty sequence when input feature is anchored.
   */
  @Test
  def testGetDependenciesForFeatureWithAnchoredFeature(): Unit = {
    val mockFeatureAnchorWithSource = mock[FeatureAnchorWithSource]
    val allAnchoredFeatures = Map("feature1" -> mockFeatureAnchorWithSource)
    val emptyDerivedFeatures = Map.empty[String, DerivedFeature]
    // validate
    val planner = MultiStageJoinPlanner()
    val result = planner.getDependenciesForFeature(allAnchoredFeatures, emptyDerivedFeatures, "feature1")
    assertTrue(result.isEmpty)
  }

  /**
   * Tests the scenario where the input feature is not found in the FeatureGroups.
   * In such a case, getDependenciesForFeature should throw an exception.
   */
  @Test(expectedExceptions = Array(classOf[FeathrConfigException]), expectedExceptionsMessageRegExp = ".*not defined in the config.*")
  def testGetDependenciesForFeatureWhenFeatureNotFound(): Unit = {
    val emptyAnchoredFeatures = Map.empty[String, FeatureAnchorWithSource]
    val emptyDerivedFeatures = Map.empty[String, DerivedFeature]
    val planner = MultiStageJoinPlanner()
    planner.getDependenciesForFeature(emptyAnchoredFeatures, emptyDerivedFeatures, "not_exist")
  }

  /**
   * Unit test for [[MultiStageJoinPlanner.convertKeyTagsToIntegerIndexes()]] which validates
   * the converted integer tags for string key tags match the expectation.
   */
  @Test
  def testConvertKeyTagsToIntegerIndexesWhenKeyTagsRepeat(): Unit = {
    val features = Seq(
      JoiningFeatureParams(Seq("tagA", "tagB"), "feature1"),
      JoiningFeatureParams(Seq("tagA"), "feature2"),
      JoiningFeatureParams(Seq("tagA", "tagC"), "feature3"),
      JoiningFeatureParams(Seq("tagC", "tagB"), "feature4"))

    val planner = MultiStageJoinPlanner()
    val (erasedEntityFeatures, keyTagStringToInt) = planner.convertKeyTagsToIntegerIndexes(features)
    // Verify the erased tags and that the order is preserved
    assertEquals(s"[${keyTagStringToInt.mkString(", ")}]", "[tagA, tagB, tagC]")
    assertEquals(s"[${erasedEntityFeatures.mkString(", ")}]", "[(0,1):feature1, (0):feature2, (0,2):feature3, (2,1):feature4]")
  }

  /**
   * Unit test for [[MultiStageJoinPlanner.convertKeyTagsToIntegerIndexes()]] which tests empty strings as key tags.
   */
  @Test
  def testConvertKeyTagsToIntegerIndexesWhenKeyTagsAreEmpty(): Unit = {
    val features = Seq(JoiningFeatureParams(Seq("", "tagA"), "feature1"), JoiningFeatureParams(Seq(""), "feature2"))

    val planner = MultiStageJoinPlanner()
    val (erasedEntityFeatures, keyTagStringToInt) = planner.convertKeyTagsToIntegerIndexes(features)
    // Verify the erased tags and that the order is preserved
    assertEquals(keyTagStringToInt.size, 2) // Expect the empty string to still have a mapping.
    assertEquals(s"[${erasedEntityFeatures.mkString(", ")}]", "[(0,1):feature1, (0):feature2]")
  }

  import com.linkedin.feathr.offline.logical.SeqJoinFeatureConfigProvider._

  /**
   * Test required features returned contains dependent anchored features as well.
   */
  @Test
  def testGetDependencyOrderingForDerivedFeature(): Unit = {
    // Get FeathrConfig
    val featureConf = feathrConfigLoader.load(featureDefConf)
    val erasedEntityFeature = ErasedEntityTaggedFeature(Seq(0, 1), "Derivedx2")
    val featureGroups = FeatureGroupsGenerator(Seq(featureConf)).getFeatureGroups()
    val planner = MultiStageJoinPlanner()
    val orderedDependencies = planner.getDependencyOrdering(featureGroups.allAnchoredFeatures, featureGroups.allDerivedFeatures, Seq(erasedEntityFeature))
    val expectedOrderedDependencies = Set(
      ErasedEntityTaggedFeature(Seq(0), "aa"),
      ErasedEntityTaggedFeature(Seq(1), "z"),
      ErasedEntityTaggedFeature(Seq(0, 1), "Derivedx"),
      ErasedEntityTaggedFeature(Seq(0, 1), "Derivedx2"))
    assertEquals(orderedDependencies.size, expectedOrderedDependencies.size)
    orderedDependencies.foreach(f => assertTrue(expectedOrderedDependencies.contains(f)))
  }

  /**
   * Test dependency ordering when only anchored features are requested.
   */
  @Test
  def testGetDependencyOrderingForAnchoredFeatures(): Unit = {
    // Get FeathrConfig
    val featureConf = feathrConfigLoader.load(featureDefConf)
    val erasedEntityFeature = Seq(ErasedEntityTaggedFeature(Seq(0), "aa"), ErasedEntityTaggedFeature(Seq(1), "z"))
    val featureGroups = FeatureGroupsGenerator(Seq(featureConf)).getFeatureGroups()
    val planner = MultiStageJoinPlanner()
    val orderedDependencies = planner.getDependencyOrdering(featureGroups.allAnchoredFeatures, featureGroups.allDerivedFeatures, erasedEntityFeature)
    val expectedOrderedDependencies =
      Set(ErasedEntityTaggedFeature(Seq(0), "aa"), ErasedEntityTaggedFeature(Seq(1), "z"))
    assertEquals(orderedDependencies.size, expectedOrderedDependencies.size)
    orderedDependencies.foreach(f => assertTrue(expectedOrderedDependencies.contains(f)))
  }

  /**
   * Validate the join stages and the post derived stages for derived feature.
   */
  @Test
  def testGetJoinPlanForDerivedFeature(): Unit = {
    // Get FeathrConfig
    val featureConf = feathrConfigLoader.load(featureDefConf)
    val erasedEntityFeature = Seq(ErasedEntityTaggedFeature(Seq(0, 1), "Derivedx2"))
    val featureGroups = FeatureGroupsGenerator(Seq(featureConf)).getFeatureGroups()
    val planner = MultiStageJoinPlanner()
    val orderedDependencies = planner.getDependencyOrdering(featureGroups.allAnchoredFeatures, featureGroups.allDerivedFeatures, erasedEntityFeature)
    val (windowAggJoinStages, basicAnchorJoinStages, postDerivedStages) = planner.getJoinStages(featureGroups, orderedDependencies)
    assertTrue(windowAggJoinStages.isEmpty) // No SWA features
    // validate basic anchored feature stages
    val expectedAnchorJoinStages =
      Set((Seq(0), Seq("aa")), (Seq(1), Seq("z")))
    assertEquals(basicAnchorJoinStages.size, 2) // for keyTag 0 and 1
    basicAnchorJoinStages.foreach(f => assertTrue(expectedAnchorJoinStages.contains(f)))
    // validate post derived features
    val expectedPostDerivedStages =
      Set(ErasedEntityTaggedFeature(Seq(0, 1), "Derivedx"), ErasedEntityTaggedFeature(Seq(0, 1), "Derivedx2"))
    assertEquals(postDerivedStages.size, expectedPostDerivedStages.size)
    postDerivedStages.foreach(f => assertTrue(expectedPostDerivedStages.contains(f)))
  }

  /**
   * Test join plan has multiple stages for same feature with different tags.
   */
  @Test
  def testJoinStagesForSameFeatureDifferentTags(): Unit = {
    // Get FeathrConfig
    val featureConf = feathrConfigLoader.load(featureDefConf)
    // Get FeathrJoinConfig
    val features = Seq("ax")
    val featureJoinConf = getFeatureJoinConfForFeatures(features, Seq("x"), "features") +
      getFeatureJoinConfForFeatures(features, Seq("aUn"), "features2")
    val keyTaggedFeatures = FeatureJoinConfig.parseJoinConfig(featureJoinConf).joinFeatures
    // Get FeatureGroups
    val featureGroups = FeatureGroupsGenerator(Seq(featureConf)).getFeatureGroups()
    val logicalPlanner = MultiStageJoinPlanner()
    val logicalPlan = logicalPlanner.getLogicalPlan(featureGroups, keyTaggedFeatures)
    assertEquals(logicalPlan.joinStages.length, 2)
    assertEquals(logicalPlan.joinStages.head._2.head, "ax")
    assertEquals(logicalPlan.joinStages.tail.head._2.head, "ax")
  }

  /**
   * Test Join Stage contains base feature but no expansion feature
   * when only sequential join feature is requested.
   */
  @Test
  def testSeqJoinBaseFeatureInJoinStage(): Unit = {
    // Get FeathrConfig
    val featureConf = feathrConfigLoader.load(featureDefConf)
    // Get FeathrJoinConfig
    val features = Seq("SeqJoinzs")
    val featureJoinConf = getFeatureJoinConfForFeatures(features, Seq("x"), "features")
    val keyTaggedFeatures = FeatureJoinConfig.parseJoinConfig(featureJoinConf).joinFeatures
    // Get FeatureGroups
    val featureGroups = FeatureGroupsGenerator(Seq(featureConf)).getFeatureGroups()
    val logicalPlanner = MultiStageJoinPlanner()
    val logicalPlan = logicalPlanner.getLogicalPlan(featureGroups, keyTaggedFeatures)
    assertEquals(logicalPlan.joinStages.length, 1)
    assertEquals(logicalPlan.joinStages.head._2.head, "ax")
    assertEquals(logicalPlan.seqJoinFeatures.head.getFeatureName.toString, features.head)
  }

  /**
   * Tests a scenario when base feature is also requested.
   */
  @Test
  def testSeqJoinWhenBaseFeatureIsAlsoRequested(): Unit = {
    // Get FeathrConfig
    val featureConf = feathrConfigLoader.load(featureDefConf)
    // Get FeathrJoinConfig
    val features = Seq("ax", "SeqJoinzs")
    val featureJoinConf = getFeatureJoinConfForFeatures(features = features, key = Seq("x"), featureGroup = "features")
    val keyTaggedFeatures = FeatureJoinConfig.parseJoinConfig(featureJoinConf).joinFeatures
    // Get FeatureGroups
    val featureGroups = FeatureGroupsGenerator(Seq(featureConf)).getFeatureGroups()
    // Verify base feature appears in join stage when explicitly requested
    val logicalPlanner = MultiStageJoinPlanner()
    val logicalPlan = logicalPlanner.getLogicalPlan(featureGroups, keyTaggedFeatures)
    assertEquals(logicalPlan.joinStages.length, 1)
    assertEquals(logicalPlan.joinStages.head._2.head, features.head)
    assertEquals(logicalPlan.seqJoinFeatures.head.getFeatureName.toString, features.tail.head)
  }

  /**
   * This test verifies that expansion feature is added to join stage when explicitly requested.
   */
  @Test
  def testSeqJoinExpansionInJoinStageWhenRequested(): Unit = {
    // Get FeathrConfig
    val featureConf = feathrConfigLoader.load(featureDefConf)
    val featureJoinConf = getFeatureJoinConfForFeatures(features = Seq("SeqJoinzs"), key = Seq("x"), featureGroup = "features") +
      getFeatureJoinConfForFeatures(features = Seq("z"), key = Seq("y"), featureGroup = "features2")
    val keyTaggedFeatures = FeatureJoinConfig.parseJoinConfig(featureJoinConf).joinFeatures
    // Get FeatureGroups
    val featureGroups = FeatureGroupsGenerator(Seq(featureConf)).getFeatureGroups()
    // Verify expansion feature appears in join stage when explicitly requested
    val logicalPlanner = MultiStageJoinPlanner()
    val logicalPlan = logicalPlanner.getLogicalPlan(featureGroups, keyTaggedFeatures)
    assertEquals(logicalPlan.joinStages.length, 2) // base feature + expansion feature (explicitly requested)
    assertEquals(s"[${logicalPlan.joinStages.flatMap(x => x._2).sorted.mkString(", ")}]", "[ax, z]")
    assertEquals(logicalPlan.seqJoinFeatures.head.getFeatureName.toString, "SeqJoinzs")
  }

  /**
   * This test validates that when the base and the expansion features of Sequential Join feature appear in the join plan when
   * they are also the dependent features for another (non-Sequential Join) derived feature,.
   */
  @Test
  def testSeqJoinDependentInJoinStageWhenRequiredByDerived(): Unit = {
    // Get FeathrConfig
    val featureConf = feathrConfigLoader.load(featureDefConf)
    val featureJoinConf = getFeatureJoinConfForFeatures(features = Seq("SeqJoinzs"), key = Seq("x"), featureGroup = "features") +
      getFeatureJoinConfForFeatures(features = Seq("Derivedx"), key = Seq("x", "y"), featureGroup = "features2")
    val keyTaggedFeatures = FeatureJoinConfig.parseJoinConfig(featureJoinConf).joinFeatures
    // Get FeatureGroups
    val featureGroups = FeatureGroupsGenerator(Seq(featureConf)).getFeatureGroups()
    // Verify the dependent features are in the join plan
    val logicalPlanner = MultiStageJoinPlanner()
    val logicalPlan = logicalPlanner.getLogicalPlan(featureGroups, keyTaggedFeatures)
    assertEquals(logicalPlan.joinStages.length, 2) // One for keyTag = 0 and one for keyTag = 1
    assertEquals(s"[${logicalPlan.joinStages.flatMap(s => s._2).mkString(", ")}]", "[z, aa, ax]")
    assertEquals(logicalPlan.seqJoinFeatures.head.getFeatureName.toString, "SeqJoinzs")
    assertTrue(logicalPlan.allRequiredFeatures.map(_.getFeatureName.toString).contains("Derivedx"))
  }
}

/**
 * Feature configuration data provider for tests.
 */
private sealed trait ConfigProvider {
  val featureDefConf: String

  def getFeatureJoinConfForFeatures(features: Seq[String], key: Seq[String], featureGroup: String): String = {
    s"""
       | $featureGroup: [
       |   {
       |     key: [${key.mkString(", ")}]
       |     featureList:[${features.mkString(", ")}]
       |   }
       | ]
      """.stripMargin
  }
}

/**
 * Sequential Join feature config provider.
 */
private object SeqJoinFeatureConfigProvider extends ConfigProvider {
  val featureDefConf =
    """
      |anchors: {
      |  -local: {
      |    source: "seqJoin/Names.avro.json"
      |    key.sqlExpr: x
      |    features: {
      |      z.def.sqlExpr: Name
      |      ax.def.sqlExpr: x
      |    }
      |  }
      |  mockdata-a-value-local: {
      |    source: "seqJoin/a.avro.json"
      |    key.sqlExpr: "x"
      |    features: {
      |      ay : {
      |        def.sqlExpr: "case when x = -1  then null else x end"
      |        type: NUMERIC
      |      }
      |      aa.def.sqlExpr: a
      |    }
      |  }
      |}
      |
      |derivations: {
      |  SeqJoinzs: {
      |    key: "x"
      |    join: {
      |      base: { key: x, feature: ax, outputKey: x }
      |      expansion: { key: x, feature: z }
      |    }
      |    aggregation:"UNION"
      |  }
      |  Derivedx: {
      |    key: ["x", "y"]
      |    inputs: {
      |      arg1: { key: y, feature: z },
      |      arg2: { key: x, feature: aa }
      |    }
      |    definition: {
      |      sqlExpr:"case when arg1 = 'Boeing' then ':aviation' else arg2 end"
      |    }
      |  }
      |  Derivedx2: {
      |    key: ["x", "y"]
      |    inputs: {
      |      arg1: { key: [x, y], feature: Derivedx },
      |      arg2: { key: x, feature: aa }
      |    }
      |    definition: {
      |      sqlExpr: "concat(arg1, ' ', arg2)"
      |    }
      |  }
      |}
      |
      |
      """.stripMargin

  val defaultFeatureJoinConf = getFeatureJoinConfForFeatures(
    features = Seq("ax", "SeqJoinzs"),
    key = Seq("x"),
    featureGroup = "features") + getFeatureJoinConfForFeatures(features = Seq("z"), key = Seq("x"), featureGroup = "features2")

}
