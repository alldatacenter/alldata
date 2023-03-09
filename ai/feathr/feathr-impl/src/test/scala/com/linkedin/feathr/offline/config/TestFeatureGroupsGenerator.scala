package com.linkedin.feathr.offline.config

import com.linkedin.feathr.common.exception.FeathrConfigException
import org.scalatest.testng.TestNGSuite
import org.testng.Assert
import org.testng.annotations.Test

import scala.io.Source

class TestFeatureGroupsGenerator extends TestNGSuite{

  private val _feathrConfigLoader = FeathrConfigLoader()

  @Test(description = "test with only a global feature def config")
  def testGlobalConfig(): Unit = {
    val feathrConfig = _feathrConfigLoader.load(Source.fromURL(getClass.getClassLoader.getResource("feathrConf-default.conf")).getLines().mkString("\n"))
    val featureGroups = FeatureGroupsGenerator(Seq(feathrConfig)).getFeatureGroups()
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_isBanana"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_betaMinusGamma"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f2"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_sum"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f1"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("a_simple_derived_feature"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature2"))
  }

  @Test(description = "test with a global feature def config and local feature def config")
  def testGlobalAndLocalConfigs(): Unit = {
    val localConfig = Some("""
                             |anchors: {
                             |  anchorL: {
                             |    source: "anchor2-source.csv"
                             |    key: "entityId"
                             |    features: {
                             |      local_a_f1: "toNumeric(x)"
                             |      local_a_y: "toNumeric(y)"
                             |    }
                             |  }
                             |
                             |  extra-anchorL: {
                             |    source: "anchor2-source.csv"
                             |    key: "entityId"
                             |    features: {
                             |      local_a_age: "toNumeric(z)"
                             |    }
                             |  }
                             |}
                             |
                             |derivations: {
                             |  local_foobar_dualkey_feature: {
                             |    key: [x, y]
                             |    inputs: {
                             |      a0: { key: [x, y], feature: foobar_dualkey_feature }
                             |      a1: { key: [x], feature: local_a_y }
                             |    }
                             |    definition: "toNumeric(a0) / toNumeric(a1)"
                             |  }
                             |}
      """.stripMargin)

    val featureDefConfig = _feathrConfigLoader.load(Source.fromURL(getClass.getClassLoader.getResource("feathrConf-default.conf")).getLines().mkString("\n"))
    val localDefConfig = _feathrConfigLoader.load(localConfig.get)

    val featureGroups = FeatureGroupsGenerator(Seq(featureDefConfig), Some(Seq(localDefConfig))).getFeatureGroups()
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_isBanana"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_betaMinusGamma"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f2"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_sum"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f1"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("a_simple_derived_feature"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature2"))

    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("local_a_f1"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("local_a_y"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("local_a_age"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("local_foobar_dualkey_feature"))
  }

  @Test(description = "test with a global feature def config and local feature def config having an anchor conflict")
  def testGlobalAndLocalConfigsWithAnchorFeatureConflict(): Unit = {
    val localConfig = Some("""
                             |anchors: {
                             |  anchorL: {
                             |    source: "anchor2-source.csv"
                             |    key: "entityId"
                             |    features: {
                             |      f2: "toNumeric(x)"  ###### this is a duplicated name
                             |      local_a_y: "toNumeric(y)"
                             |    }
                             |  }
                             |}
      """.stripMargin)
    val featureDefConfig = _feathrConfigLoader.load(Source.fromURL(getClass.getClassLoader.getResource("feathrConf-default.conf")).getLines().mkString("\n"))
    val localDefConfig = _feathrConfigLoader.load(localConfig.get)

    val featureGroups = FeatureGroupsGenerator(Seq(featureDefConfig), Some(Seq(localDefConfig))).getFeatureGroups()

    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_isBanana"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_betaMinusGamma"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f2"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_sum"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f1"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("a_simple_derived_feature"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature2"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("local_a_y"))
  }

  @Test(description = "test with a global feature def config and local feature def config having a derived feature conflict")
  def testGlobalAndLocalConfigsWithDerivedFeatureConflict(): Unit = {
    val localConfig = Some("""
                             |anchors: {
                             |}
                             |
                             |derivations: {
                             |  foobar_dualkey_feature: {  ###### this is a duplicated name
                             |    key: [x, y]
                             |    inputs: {
                             |      a0: { key: [x, y], feature: f2 }
                             |      a1: { key: [x], feature: local_a_y }
                             |    }
                             |    definition: "toNumeric(a0) / toNumeric(a1)"
                             |  }
                             |}
      """.stripMargin)
    val featureDefConfig = _feathrConfigLoader.load(Source.fromURL(getClass.getClassLoader.getResource("feathrConf-default.conf")).getLines().mkString("\n"))
    val localDefConfig = _feathrConfigLoader.load(localConfig.get)

    val featureGroups = FeatureGroupsGenerator(Seq(featureDefConfig), Some(Seq(localDefConfig))).getFeatureGroups()
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_isBanana"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_betaMinusGamma"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f2"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_sum"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f1"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("a_simple_derived_feature"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature2"))
  }

  @Test(description = "test with a global config, feature def config and local feature def config having a derived feature conflict")
  def testGlobalAndFeatureAndLocalConfig(): Unit = {
    // feature config may contain anchors from multiple conf files
    val featureConfig = Some("""
                               |anchors: {
                               |  anchorA: {
                               |    source: "anchor2-source.csv"
                               |    key: "entityId"
                               |    features: {
                               |      a_f1A: "toNumeric(x)"
                               |    }
                               |  }
                               |}
                               |
                               |anchors: {
                               |  anchorB: {
                               |    source: "anchor2-source.csv"
                               |    key: "entityId"
                               |    features: {
                               |      a_f1B: "toNumeric(x)"
                               |    }
                               |  }
                               |}
      """.stripMargin)

    val localConfig = Some("""
                             |anchors: {
                             |}
                             |
                             |derivations: {
                             |  foobar_dualkey_feature_test: {
                             |    key: [x, y]
                             |    inputs: {
                             |      a0: { key: [x, y], feature: foobar_dualkey_feature }
                             |      a1: { key: [x], feature: local_a_y }
                             |    }
                             |    definition: "toNumeric(a0) / toNumeric(a1)"
                             |  }
                             |}
      """.stripMargin)
    val defaultFeatureConfig = _feathrConfigLoader.load(Source.fromURL(getClass.getClassLoader.getResource("feathrConf-default.conf")).getLines().mkString("\n"))
    val featureDefConfig = _feathrConfigLoader.load(featureConfig.get)
    val localDefConfig = _feathrConfigLoader.load(localConfig.get)

    val featureGroups = FeatureGroupsGenerator(Seq(defaultFeatureConfig, featureDefConfig), Some(Seq(localDefConfig))).getFeatureGroups()
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_isBanana"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_betaMinusGamma"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f2"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_sum"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f1"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("a_simple_derived_feature"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_f1A"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("a_f1B"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("foobar_dualkey_feature2"))
  }

  @Test(description = "test by setting the localOverride parameter")
  def testLocalConfigOverride(): Unit = {
    val localConfig = """
                             |anchors: {
                             |}
                             |
                             |derivations: {
                             |  derivedF1: "Math.log(toNumeric(a_omega_logA))"
                             |}
      """.stripMargin
    // by default, the local.override.all is set to true
    val featureDefConfig = _feathrConfigLoader.load(Source.fromURL(getClass.getClassLoader.getResource("feathrConf-default.conf")).getLines().mkString("\n"))
    val localDefConfig = _feathrConfigLoader.load(localConfig)
    val featureGroups1 = FeatureGroupsGenerator(Seq(featureDefConfig), Some(Seq(localDefConfig))).getFeatureGroups()
    Assert.assertTrue(featureGroups1.allDerivedFeatures.contains("derivedF1"))
    Assert.assertTrue(featureGroups1.allDerivedFeatures("derivedF1").dependencyFeatures.contains("a_omega_logA"))

    val featureGroups2 = FeatureGroupsGenerator(Seq(featureDefConfig)).getFeatureGroups()
    Assert.assertTrue(featureGroups2.allDerivedFeatures.contains("derivedF1"))
    Assert.assertTrue(featureGroups2.allDerivedFeatures("derivedF1").dependencyFeatures.contains("anchoredF1"))
  }

  @Test(description = "test for all seq join features")
  def testSeqJoinFeatures(): Unit = {
    // feature config may contain anchors from multiple conf files
    val featureConfig = """
                          |anchors: {
                          |  -local: {
                          |   source: "seqJoin/Names.avro.json"
                          |   key.sqlExpr: x
                          |   features: {
                          |    z.def.sqlExpr: Name
                          |   }
                          |  }
                          |  a-to-a-local: {
                          |   source: "seqJoin/a.avro.json"
                          |   key.sqlExpr: x
                          |   features: {
                          |    aaToa.def.sqlExpr: x
                          |   }
                          |  }
                          |  mockdata-a-value-local: {
                          |    source: "seqJoin/a.avro.json"
                          |    key.sqlExpr: "x"
                          |    features: {
                          |      ax : {
                          |      def.sqlExpr: x
                          |      default: 1
                          |      type: NUMERIC
                          |      }
                          |   }
                          |   }
                          |}
                          |
                          |derivations: {
                          |seq_join_a_names1: {
                          |    key: "x"
                          |    join: {
                          |      base: { key: x, feature: ax }
                          |      expansion: { key: x, feature: aaToa }
                          |    }
                          |    aggregation:"UNION"
                          |  }
                          |
                          |seq_join_a_names2: {
                          |    key: "x"
                          |    join: {
                          |      base: { key: x, feature: seq_join_a_names1 }
                          |      expansion: { key: x, feature: z }
                          |    }
                          |    aggregation:"UNION"
                          |  }
                          |}
                          |
                          |
      """.stripMargin

    val featureDefConfig = _feathrConfigLoader.load(featureConfig)

    val featureGroups = FeatureGroupsGenerator(Seq(featureDefConfig))
    val groups = featureGroups.getFeatureGroups()
    Assert.assertTrue(groups.allAnchoredFeatures.contains("aaToa"))
    Assert.assertTrue(groups.allAnchoredFeatures.contains("ax"))
    Assert.assertTrue(groups.allAnchoredFeatures.contains("z"))
    Assert.assertTrue(groups.allDerivedFeatures.contains("seq_join_a_names1"))
    Assert.assertTrue(groups.allDerivedFeatures.contains("seq_join_a_names2"))
    Assert.assertTrue(groups.allSeqJoinFeatures.contains("seq_join_a_names1"))
    Assert.assertTrue(groups.allSeqJoinFeatures.contains("seq_join_a_names2"))
  }

  @Test(description = "test for all window agg features")
  def testWindowAggFeatures(): Unit = {
    val featureConfig = """
                               |sources: {
                               |  swaSource: {
                               |    location: { path: "slidingWindowAgg/localSWAAnchorTestFeatureData/daily" }
                               |    timePartitionPattern: "yyyy/MM/dd"
                               |    timeWindowParameters: {
                               |      timestampColumn: "timestamp"
                               |      timestampColumnFormat: "yyyy-MM-dd"
                               |    }
                               |  }
                               |}
                               |
                               |anchors: {
                               |  swaAnchor: {
                               |    source: "swaSource"
                               |    key: "substring(x, 0)"
                               |    lateralViewParameters: {
                               |      lateralViewDef: explode(features)
                               |      lateralViewItemAlias: feature
                               |    }
                               |    features: {
                               |      f1: {
                               |        def: "feature.col.value"
                               |        filter: "feature.col.name = 'f1'"
                               |        aggregation: SUM
                               |        groupBy: "feature.col.term"
                               |        window: 3d
                               |      }
                               |    }
                               |  }
                               |
                               |  swaAnchor2: {
                               |    source: "swaSource"
                               |    key: "x"
                               |    lateralViewParameters: {
                               |      lateralViewDef: explode(features)
                               |      lateralViewItemAlias: feature
                               |    }
                               |    features: {
                               |      f1Sum: {
                               |        def: "feature.col.value"
                               |        filter: "feature.col.name = 'f1'"
                               |        aggregation: SUM
                               |        groupBy: "feature.col.term"
                               |        window: 3d
                               |      }
                               |    }
                               |  }
                               |  swaAnchorWithKeyExtractor: {
                               |    source: "swaSource"
                               |    keyExtractor: "com.linkedin.feathr.offline.anchored.keyExtractor.SimpleSampleKeyExtractor"
                               |    features: {
                               |      f3: {
                               |        def: "aggregationWindow"
                               |        aggregation: SUM
                               |        window: 3d
                               |      }
                               |    }
                               |   }
                               |  swaAnchorWithKeyExtractor2: {
                               |      source: "swaSource"
                               |      keyExtractor: "com.linkedin.feathr.offline.anchored.keyExtractor.SimpleSampleKeyExtractor"
                               |      features: {
                               |        f4: {
                               |           def: "aggregationWindow"
                               |           aggregation: SUM
                               |           window: 3d
                               |       }
                               |     }
                               |   }
                               |  swaAnchorWithKeyExtractor: {
                               |    source: "swaSource"
                               |    keyExtractor: "com.linkedin.feathr.offline.anchored.keyExtractor.SimpleSampleKeyExtractor2"
                               |    lateralViewParameters: {
                               |      lateralViewDef: explode(features)
                               |      lateralViewItemAlias: feature
                               |    }
                               |    features: {
                               |      f2: {
                               |        def: "feature.col.value"
                               |        filter: "feature.col.name = 'f2'"
                               |        aggregation: SUM
                               |        groupBy: "feature.col.term"
                               |        window: 3d
                               |      }
                               |    }
                               |  }
                               |}
      """.stripMargin

    val featureDefConfig = _feathrConfigLoader.load(featureConfig)

    val featureGroups = FeatureGroupsGenerator(Seq(featureDefConfig)).getFeatureGroups()
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f1Sum"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f1"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f3"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f4"))
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("f2"))
    Assert.assertTrue(featureGroups.allWindowAggFeatures.contains("f1Sum"))
    Assert.assertTrue(featureGroups.allWindowAggFeatures.contains("f1"))
    Assert.assertTrue(featureGroups.allWindowAggFeatures.contains("f3"))
    Assert.assertTrue(featureGroups.allWindowAggFeatures.contains("f4"))
    Assert.assertTrue(featureGroups.allWindowAggFeatures.contains("f2"))
  }

  @Test(description = "test for all passthrough features")
  def testPassthroughFeatures(): Unit = {
    val featureConfig = """
                               sources: {
                               |  ptSource: {
                               |    type: "PASSTHROUGH"
                               |  }
                               |}
                               |anchors: {
                               |  anchor3: {
                               |    source: ptSource
                               |    key: "IdInObservation"
                               |    features: {
                               |      anchoredPf: {
                               |       def: "(foreach(v : passThroughFeatureSet) {if (v.name == \"pf2\") return v.value*2;} return null;)"
                               |       default: 3.14
                               |      }
                               |    }
                               |  }
                               |}
                               |
                               |derivations: {
                               |  C: "anchoredPf*2"      //MVEL derived feature
                               |}
      """.stripMargin

    val featureDefConfig = _feathrConfigLoader.load(featureConfig)

    val featureGroups = FeatureGroupsGenerator(Seq(featureDefConfig)).getFeatureGroups()
    Assert.assertTrue(featureGroups.allAnchoredFeatures.contains("anchoredPf"))
    Assert.assertTrue(featureGroups.allDerivedFeatures.contains("C"))
    Assert.assertTrue(featureGroups.allPassthroughFeatures.contains("anchoredPf"))
  }

  // Feature definitions for test feature configs with features which have been repeated
  val featureAnchorConfig1 = """
                         |  sources: {
                         |  ptSource: {
                         |    type: "PASSTHROUGH"
                         |  }
                         |}
                         |anchors: {
                         |  anchor3: {
                         |    source: ptSource
                         |    key: "IdInObservation"
                         |    features: {
                         |      anchoredPf: {
                         |       def: "(foreach(v : passThroughFeatureSet) {if (v.name == \"pf2\") return v.value*2;} return null;)"
                         |       default: 3.14
                         |      }
                         |    }
                         |  }
                         |}
      """.stripMargin
  val featureAnchorConfig2 = """
                         |  sources: {
                         |  ptSource: {
                         |    type: "PASSTHROUGH"
                         |  }
                         |}
                         |anchors: {
                         |  anchor3: {
                         |    source: ptSource
                         |    key: "IdInObservation"
                         |    features: {
                         |      anchoredPf: {
                         |       def: "(foreach(v : passThroughFeatureSet) {if (v.name == \"pf2\") return v.value*4;} return null;)"
                         |       default: 3.14
                         |      }
                         |    }
                         |  }
                         |}
      """.stripMargin
  val featureDerivedConfig1 = """
                       |anchors: {
                       |}
                       |
                       |derivations: {
                       |  derivedF1: "Math.log(toNumeric(a_omega_logA))"
                       |}
      """.stripMargin
  val featureDerivedConfig2 = """
                       |anchors: {
                       |}
                       |
                       |derivations: {
                       |  derivedF1: "a_omega_logA"
                       |}
      """.stripMargin

  @Test(description = "test for duplicate anchored features in diff feature def configs", expectedExceptions = Array(classOf[FeathrConfigException]))
  def testWithDupAnchoredFeatures(): Unit = {
    val featureDefConfig1 = _feathrConfigLoader.load(featureAnchorConfig1)
    val featureDefConfig2 = _feathrConfigLoader.load(featureAnchorConfig2)

    FeatureGroupsGenerator(Seq(featureDefConfig1, featureDefConfig2)).getFeatureGroups()
  }

  @Test(description = "test for duplicate derived features in diff feature def configs", expectedExceptions = Array(classOf[FeathrConfigException]))
  def testWithDupDerivedFeatures(): Unit = {
    val featureDefConfig1 = _feathrConfigLoader.load(featureDerivedConfig1)
    val featureDefConfig2 = _feathrConfigLoader.load(featureDerivedConfig2)

    FeatureGroupsGenerator(Seq(featureDefConfig1, featureDefConfig2)).getFeatureGroups()
  }

  @Test(description = "test for duplicate anchored features in diff local override def configs", expectedExceptions = Array(classOf[FeathrConfigException]))
  def testLocalConfigOverrideWithDupAnchoredFeatures(): Unit = {
    // by default, the local.override.all is set to true
    val featureDefConfig = _feathrConfigLoader.load(Source.fromURL(getClass.getClassLoader.getResource("feathrConf-default.conf")).getLines().mkString("\n"))
    val localDefConfig1 = _feathrConfigLoader.load(featureAnchorConfig1)
    val localDefConfig2 = _feathrConfigLoader.load(featureAnchorConfig2)
    FeatureGroupsGenerator(Seq(featureDefConfig), Some(Seq(localDefConfig1, localDefConfig2))).getFeatureGroups()
  }

  @Test(description = "test for duplicate derived features in diff local override def configs", expectedExceptions = Array(classOf[FeathrConfigException]))
  def testLocalConfigOverrideWithDuplicateDerivedFeatures(): Unit = {

    // by default, the local.override.all is set to true
    val featureDefConfig = _feathrConfigLoader.load(Source.fromURL(getClass.getClassLoader.getResource("feathrConf-default.conf")).getLines().mkString("\n"))
    val localDefConfig1 = _feathrConfigLoader.load(featureDerivedConfig1)
    val localDefConfig2 = _feathrConfigLoader.load(featureDerivedConfig2)
    FeatureGroupsGenerator(Seq(featureDefConfig), Some(Seq(localDefConfig1, localDefConfig2))).getFeatureGroups()
  }
}
