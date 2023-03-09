package com.linkedin.feathr.offline

import com.linkedin.feathr.offline.AssertFeatureUtils.{rowApproxEquals, validateRows}
import com.linkedin.feathr.offline.util.FeathrUtils
import com.linkedin.feathr.offline.util.FeathrUtils.{SKIP_MISSING_FEATURE, setFeathrJobParam}
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{LongType, StructField, StructType}
import org.testng.Assert._
import org.testng.annotations._

import scala.collection.mutable

/**
 * Integ tests for sliding window aggregation functionality
 */
class SlidingWindowAggIntegTest extends FeathrIntegTest {

  /**
   * test SWA with lateralview parameters
   */
  @Test
  def testLocalAnchorSWATest: Unit = {
    val df = runLocalFeatureJoinForTest(
      joinConfigAsString = """
        | settings: {
        |  observationDataTimeSettings: {
        |     absoluteTimeRange: {
        |         startTime: "2018-05-01"
        |         endTime: "2018-05-03"
        |         timeFormat: "yyyy-MM-dd"
        |     }
        |  }
        |  joinTimeSettings: {
        |     timestampColumn: {
        |       def: timestamp
        |       format: "yyyy-MM-dd"
        |     }
        |  }
        |}
        |
        |features: [
        |   {
        |       key: [x],
        |       featureList: ["f1", "f1Sum", "f2", "f1f1"]
        |   },
        |   {
        |        key: [x, y]
        |        featureList: ["f3", "f4"]
        |   }
        |]
    """.stripMargin,
      featureDefAsString = """
          |sources: {
          |  ptSource: {
          |    type: "PASSTHROUGH"
          |  }
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
          |  ptAnchor: {
          |     source: "ptSource"
          |     key: "x"
          |     features: {
          |       f1f1: {
          |         def: "([$.term:$.value] in passthroughFeatures if $.name == 'f1f1')"
          |       }
          |     }
          |  }
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
          |  swaAnchorWithKeyExtractor3: {
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
      """.stripMargin,
      "slidingWindowAgg/localAnchorTestObsData.avro.json").data
    df.show()

    // validate output in name term value format
    val featureList = df.collect().sortBy(row => if (row.get(0) != null) row.getAs[String]("x") else "null")
    val row0 = featureList(0)
    val row0f1 = row0.getAs[Row]("f1")
    assertEquals(row0f1, TestUtils.build1dSparseTensorFDSRow(Array("f1t1", "f1t2"), Array(2.0f, 3.0f)))
    val row0f2 = row0.getAs[Row]("f2")
    assertEquals(row0f2, TestUtils.build1dSparseTensorFDSRow(Array("f2t1"), Array(4.0f)))
    val row0f1f1 = row0.getAs[Row]("f1f1")
    assertEquals(row0f1f1, TestUtils.build1dSparseTensorFDSRow(Array("f1t1"), Array(12.0f)))

    val row1 = featureList(1)
    val row1f1 = row1.getAs[Row]("f1")
    assertEquals(row1f1, TestUtils.build1dSparseTensorFDSRow(Array("f1t1", "f1t2"), Array(5.0f, 6.0f)))
    val row1f2 = row1.getAs[Row]("f2")
    assertEquals(row1f2, TestUtils.build1dSparseTensorFDSRow(Array("f2t1"), Array(7.0f)))
    val row1f1f1 = row1.getAs[Row]("f1f1")
    assertEquals(row1f1f1, TestUtils.build1dSparseTensorFDSRow(Array("f1t1"), Array(12.0f)))
  }

  /**
   * test SWA with dense vector feature
   * The feature dataset generation/daily has different but compatible schema for different partitions,
   * this is supported by fuzzyUnion
   */
  @Test
  def testLocalAnchorSWAWithDenseVector(): Unit = {
    val res = runLocalFeatureJoinForTest(
      """
        | settings: {
        |  joinTimeSettings: {
        |    timestampColumn: {
        |       def: "timestamp"
        |       format: "yyyy-MM-dd"
        |    }
        |    simulateTimeDelay: 1d
        |  }
        |}
        |
        |features: [
        |  {
        |    key: [mId],
        |    featureList: ["aEmbedding", "memberEmbeddingAutoTZ"]
        |  }
        |]
      """.stripMargin,
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily" }
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
        |    key: "x"
        |    features: {
        |      aEmbedding: {
        |        def: "embedding"
        |        aggregation: LATEST
        |        window: 3d
        |      }
        |      memberEmbeddingAutoTZ: {
        |        def: "embedding"
        |        aggregation: LATEST
        |        window: 3d
        |        type: {
        |          type: TENSOR
        |          tensorCategory: SPARSE
        |          dimensionType: [INT]
        |          valType: FLOAT
        |        }
        |      }
        |    }
        |  }
        |}
        """.stripMargin,
      observationDataPath = "slidingWindowAgg/csvTypeTimeFile1.csv").data

    val featureList = res.collect().sortBy(row => if (row.get(0) != null) row.getAs[String]("mId") else "null")

    assertEquals(featureList.size, 2)
    assertEquals(featureList(0).getAs[Row]("aEmbedding"), mutable.WrappedArray.make(Array(5.5f, 5.8f)))
    assertEquals(featureList(0).getAs[Row]("memberEmbeddingAutoTZ"),
      TestUtils.build1dSparseTensorFDSRow(Array(0, 1), Array(5.5f, 5.8f)))
  }

  /**
   * SWA test with default values
   */
  @Test
  def testLocalAnchorSWADefault(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |  observationDataTimeSettings: {
        |   absoluteTimeRange: {
        |     timeFormat: yyyy-MM-dd
        |     startTime: "2018-05-01"
        |     endTime: "2018-05-03"
        |   }
        |  }
        |  joinTimeSettings: {
        |   timestampColumn: {
        |     def: timestamp
        |     format: yyyy-MM-dd
        |   }
        |  }
        |}
        |
        |features: [
        |  {
        |    key: [x],
        |    featureList: ["simplePageViewCount", "simpleFeature"]
        |  }
        |]
      """.stripMargin
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "slidingWindowAgg/localSWADefaultTest/daily" }
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
        |    key: "x"
        |    features: {
        |      simplePageViewCount: {
        |        def: "aggregationWindow"
        |        aggregation: COUNT
        |        window: 3d
        |        default: 10
        |      }
        |      simpleFeature: {
        |        def: "aggregationWindow"
        |        aggregation: COUNT
        |        window: 3d
        |        default: 20
        |      }
        |    }
        |  }
        |}
      """.stripMargin
    val res = runLocalFeatureJoinForTest(joinConfigAsString, featureDefAsString, observationDataPath = "slidingWindowAgg/localAnchorTestObsData.avro.json").data
    res.show()
    val df = res.collect()(0)
    assertEquals(df.getAs[Float]("simplePageViewCount"), 10f)
    assertEquals(df.getAs[Float]("simpleFeature"), 20f)
  }

  /**
   * SWA test when path does not have daily attached to it. It should work as expected.
   */
  @Test
  def testSwaWithMalformedPath(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |  observationDataTimeSettings: {
        |   absoluteTimeRange: {
        |     timeFormat: yyyy-MM-dd
        |     startTime: "2018-05-01"
        |     endTime: "2018-05-03"
        |   }
        |  }
        |  joinTimeSettings: {
        |   timestampColumn: {
        |     def: timestamp
        |     format: yyyy-MM-dd
        |   }
        |  }
        |}
        |
        |features: [
        |  {
        |    key: [x],
        |    featureList: ["simplePageViewCount", "simpleFeature"]
        |  }
        |]
      """.stripMargin
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "slidingWindowAgg/localSWADefaultTest/" }
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
        |    key: "x"
        |    features: {
        |      simplePageViewCount: {
        |        def: "aggregationWindow"
        |        aggregation: COUNT
        |        window: 3d
        |        default: 10
        |      }
        |      simpleFeature: {
        |        def: "aggregationWindow"
        |        aggregation: COUNT
        |        window: 3d
        |        default: 20
        |      }
        |    }
        |  }
        |}
      """.stripMargin
    val res = runLocalFeatureJoinForTest(joinConfigAsString, featureDefAsString, observationDataPath = "slidingWindowAgg/localAnchorTestObsData.avro.json").data
    res.show()
    val df = res.collect()(0)
    assertEquals(df.getAs[Float]("simplePageViewCount"), 10f)
    assertEquals(df.getAs[Float]("simpleFeature"), 20f)
  }

  /**
   * SWA test with missing features. To enable this test, set the value of FeatureUtils.SKIP_MISSING_FEATURE to True. From
   * Spark 3.1, SparkContext.updateConf() is not supported.
   */
  @Test
  def testSWAWithMissingFeatureData(): Unit = {
    setFeathrJobParam(FeathrUtils.SKIP_MISSING_FEATURE, "true")
    val joinConfigAsString =
      """
        | settings: {
        |  observationDataTimeSettings: {
        |   absoluteTimeRange: {
        |     timeFormat: yyyy-MM-dd
        |     startTime: "2018-05-01"
        |     endTime: "2018-05-03"
        |   }
        |  }
        |  joinTimeSettings: {
        |   timestampColumn: {
        |     def: timestamp
        |     format: yyyy-MM-dd
        |   }
        |  }
        |}
        |
        |features: [
        |  {
        |    key: [x],
        |    featureList: ["simplePageViewCount", "simpleFeature"]
        |  }
        |]
      """.stripMargin
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "slidingWindowAgg/localSWADefaultTest/daily" }
        |    timePartitionPattern: "yyyy/MM/dd"
        |    timeWindowParameters: {
        |      timestampColumn: "timestamp"
        |      timestampColumnFormat: "yyyy-MM-dd"
        |    }
        |  }
        |  missingSource: {
        |    location: { path: "slidingWindowAgg/missingFeatureData/daily" }
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
        |    key: "x"
        |    features: {
        |      simplePageViewCount: {
        |        def: "aggregationWindow"
        |        aggregation: COUNT
        |        window: 3d
        |        default: 10
        |      }
        |    }
        |  }
        |  missingAnchor: {
        |  source: "missingSource"
        |  key: "x"
        |  features: {
        |   simpleFeature: {
        |        def: "aggregationWindow"
        |        aggregation: COUNT
        |        window: 3d
        |        default: 20
        |     }
        |    }
        |  }
        |}
      """.stripMargin
    val res = runLocalFeatureJoinForTest(joinConfigAsString, featureDefAsString, observationDataPath = "slidingWindowAgg/localAnchorTestObsData.avro.json").data
    res.show()
    val df = res.collect()(0)
    assertEquals(df.getAs[Float]("simplePageViewCount"), 10f)
    assert(!res.columns.contains("simpleFeature"))
    setFeathrJobParam(SKIP_MISSING_FEATURE, "false")
  }

  /**
   * test SWA with simulate time expressed as days and hours
   */
  @Test
  def testLocalAnchorSWASimulateTimeDelays(): Unit = {
    // aligned by day, 7 days
    testLocalAnchorSWASimulateTimeDelay("7d")
    // not aligned by day, 7 days and 7 hours
    testLocalAnchorSWASimulateTimeDelay("175h")
  }

  def testLocalAnchorSWASimulateTimeDelay(delay: String): Unit = {
    val res = runLocalFeatureJoinForTest(
      joinConfigAsString = s"""
         | settings: {
         | observationDataTimeSettings: {
         |   absoluteTimeRange: {
         |     timeFormat: yyyy-MM-dd
         |     startTime: "2018-05-01"
         |     endTime: "2018-05-03"
         |   }
         |  }
         |  joinTimeSettings: {
         |   timestampColumn: {
         |     def: timestamp
         |     format: yyyy-MM-dd
         |   }
         |   simulateTimeDelay: ${delay}
         |  }
         |}
         |
         |features: [
         |
         |  {
         |    key: [x],
         |    featureList: ["simpleFeature"]
         |  }
         |]
      """.stripMargin,
      featureDefAsString = """
          |sources: {
          |  swaSource: {
          |    location: { path: "slidingWindowAgg/localSWASimulateTimeDelay/daily" }
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
          |    key: "x"
          |    features: {
          |      simpleFeature: {
          |        def: "aggregationWindow"
          |        aggregation: COUNT
          |        window: 3d
          |      }
          |    }
          |  }
          |}
        """.stripMargin,
      observationDataPath = "slidingWindowAgg/localAnchorTestObsData.avro.json").data

    val result = res.collect().filter(x => x.getAs[String]("x") == "a2")
    assertEquals(result.head.getAs[Float]("simpleFeature"), 2.0f, 1.0e-7)
  }

  /**
   * test SWA with time delay override
   */
  @Test
  def testLocalAnchorSWAWithMultipleSettingsConfigTest: Unit = {
    val res = runLocalFeatureJoinForTest(
      joinConfigAsString = s"""
         | settings: {
         |    observationDataTimeSettings: {
         |      absoluteTimeRange: {
         |         timeFormat: yyyy-MM-dd
         |         startTime: 2018-05-01
         |         endTime: 2018-05-03
         |      }
         |    }
         |    joinTimeSettings: {
         |      timestampColumn: {
         |         def: timestamp
         |         format: yyyy-MM-dd
         |      }
         |      simulateTimeDelay: 7d
         |    }
         |}
         |
         |features: [
         |
         |  {
         |    key: [x],
         |    featureList: ["simpleFeature", "likesFeature"]
         |  },
         |  {
         |    key: [x],
         |    featureList: ["commentsFeature"]
         |    overrideTimeDelay: 8d
         |  }
         |]
      """.stripMargin,
      featureDefAsString = """
          |sources: {
          |  swaSource: {
          |    location: { path: "slidingWindowAgg/localSWASimulateTimeDelay/daily" }
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
          |    key: "x"
          |    features: {
          |      simpleFeature: {
          |        def: "aggregationWindow"
          |        aggregation: COUNT
          |        window: 3d
          |      }
          |      likesFeature: {
          |        def: "foo"
          |        aggregation: COUNT
          |        window: 3d
          |      }
          |    }
          |  }
          |    swaAnchor2: {
          |    source: "swaSource"
          |    key: "x"
          |    features: {
          |      commentsFeature: {
          |        def: "bar"
          |        aggregation: COUNT
          |        window: 3d
          |      }
          |    }
          |  }
          |}
        """.stripMargin,
      observationDataPath = "slidingWindowAgg/localAnchorTestObsData.avro.json").data

    res.show

    val result1 = res.collect().filter(x => x.getAs[String]("x") == "a2")
    assertEquals(result1.head.getAs[Float]("simpleFeature"), 2.0f, 1.0e-7)
    assertEquals(result1.head.getAs[Float]("commentsFeature"), 2f)
    assertEquals(result1.head.getAs[Float]("likesFeature"), 2f)
  }

  /**
  * The test verifies that AFG works with hourly data.
  */
  @Test
  def testAFGOutputWithHourlyData(): Unit = {
    val res = runLocalFeatureJoinForTest(
      joinConfigAsString = s"""
                              | settings: {
                              |    joinTimeSettings: {
                              |      timestampColumn: {
                              |         def: timestamp
                              |         format: yyyy-MM-dd-hh
                              |      }
                              |    }
                              |}
                              |
                              |features: [
                              |  {
                              |    key: [x],
                              |    featureList: ["f"]
                              |  }
                              |]
      """.stripMargin,
      featureDefAsString =
        """
          |sources: {
          |  swaSource: {
          |    location: { path: "generation/hourly/" }
          |    timePartitionPattern: "yyyy/MM/dd/hh"
          |  }
          |}
          |anchors: {
          |  swaAnchorWithKeyExtractor: {
          |    source: "swaSource"
          |    key: [x]
          |    features: {
          |      f: {
          |        def: count   // the column that contains the raw view count
          |        aggregation: SUM
          |        window: 24h
          |      }
          |    }
          |  }
          |}
      """.stripMargin,
      observationDataPath = "slidingWindowAgg/hourlyObsData.avro.json").data

    res.show
    val featureList = res.collect().sortBy(row => row.getAs[Integer]("x"))

    val row1 = featureList(0)
    val row1f1 = row1.getAs[Float]("f")
    assertEquals(row1f1, 9.0f)
    val row2 = featureList(1)
    val row2f2 = row2.getAs[Float]("f")
    assertEquals(row2f2, 28.0f)
  }

  /**
   * Test SWA with union in observation
   */
  @Test
  def testObservationWithUnion(): Unit = {
    val df = runLocalFeatureJoinForTest(
      """settings: {
        |  joinTimeSettings: {
        |    timestampColumn: {
        |       def: "timestamp/1000"
        |       format: "epoch"
        |    }
        |  }
        |},
        |  features:[
        |    {
        |      key: x
        |      featureList: [f5, f1, feature_timestamp]
        |    },
        |    {
        |      key: [x, y]
        |      featureList: [f2]
        |    }
        |  ]
        |
        |""".stripMargin,
      """
        |sources: {
        |ptSource: {
        |    type: "PASSTHROUGH"
        |  }
        |fooIntentV1Source: {
        |    type: "HDFS"
        |    location: { path: "slidingWindowAgg/foo/daily" }
        |    timePartitionPattern: "yyyy/MM/dd"
        |    timeWindowParameters: {
        |      // Since fooIntent is daily data, timestamp expresses 00:00 of the day, and is in unit of seconds.
        |      timestampColumn: "metadataMap.timestamp.STRING"
        |      timestampColumnFormat: "epoch"
        |    }
        |  }
        |}
        |anchors: {
        |passthroughAnchor: {
        | source: ptSource
        | key: x
        | features: {
        |   feature_timestamp: {
        |     // We need to check if the array elements are null because the array is a union of null and passthrough features.
        |     def: "(foreach(v: passThroughFeatures) {if (v!=null && v.name == \"feature_timestamp\") return [v.term:v.value];})"
        |   }
        | }
        |}
        |a1: {
        |  source: "slidingWindowAgg/foo/daily/2019/01/05/data.avro.json"
        |  key: uid
        |  features:
        |  {
        |    f1:{
        |      def: active
        |    }
        |  }
        |},
        |a2: {
        |  source: "slidingWindowAgg/featureDataWithUnionNull.avro.json"
        |  key: [uid, y]
        |  features:
        |  {
        |    f2:{
        |      def: "y"
        |    }
        |  }
        |},
        |
        |fooV1Anchor: {
        |    source: "fooIntentV1Source"
        |    key: "uid"
        |    features: {
        |      f5: {
        |        def: "active"
        |        aggregation: LATEST
        |        window: 107d
        |      }
        |    }
        |  }
        |}
       """.stripMargin,
      "slidingWindowAgg/obsWithPassthrough.avro.json").data
    df.show()
    val featureList = df.collect().sortBy(row => row.getAs[Long]("x"))

    // First row is mainly nulls, so we start asserting values from the second row.
    val row1 = featureList(1)
    val row1f1 = row1.getAs[Float]("f1")
    assertTrue(Math.abs(row1f1 - 0.16055842) < 0.001)
    val row1f2 = row1.getAs[Float]("f2")
    assertEquals(row1f2, 123.0f)
    val row1featureTs = row1.getAs[Row]("feature_timestamp")
    rowApproxEquals(row1featureTs, TestUtils.build1dSparseTensorFDSRow(Array(""), Array(1.54711451E12)))
  }

  /**
   * Invalid SWA case with no joinTimeSettings
   */
  @Test(
    expectedExceptions = Array(classOf[RuntimeException]),
    expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] joinTimeSettings section is not defined in join config(.*)")
  def testInvalidCaseWithNoJoinTimeSettings(): Unit = {
    val joinConfigAsString =
      """
        | settings: {
        |  observationDataTimeSettings: {
        |   absoluteTimeRange: {
        |     timeFormat: yyyy-MM-dd
        |     startTime: "2018-05-01"
        |     endTime: "2018-05-03"
        |   }
        |  }
        |}
        |
        |features: [
        |  {
        |    key: [x],
        |    featureList: ["simplePageViewCount"]
        |  }
        |]
      """.stripMargin
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "slidingWindowAgg/localSWADefaultTest/daily" }
        |    isTimeSeries: true
        |    timeWindowParameters: {
        |      timestamp: "timestamp"
        |      timestamp_format: "yyyy-MM-dd"
        |    }
        |  }
        |}
        |
        |anchors: {
        |  swaAnchor: {
        |    source: "swaSource"
        |    key: "x"
        |    features: {
        |      simplePageViewCount: {
        |        def: "aggregationWindow"
        |        aggregation: COUNT
        |        window: 3d
        |        default: 10
        |      }
        |    }
        |  }
        |}
      """.stripMargin
    runLocalFeatureJoinForTest(joinConfigAsString, featureDefAsString, observationDataPath = "slidingWindowAgg/localAnchorTestObsData.avro.json")
  }

  /**
   * test invalid case when there is an overrideTimeDelay with no simulateTimeDelay set.
   * TODO: Enable after adding validation code in FCM.
   */
  @Test(
    enabled = false,
    expectedExceptions = Array(classOf[RuntimeException]),
    expectedExceptionsMessageRegExp = "\\[FEATHR_USER_ERROR\\] overrideTimeDelay cannot be defined without setting a simulateTimeDelay(.*)")
  def testInvalidCaseWithOverrideTimeDelay: Unit = {
    val res = runLocalFeatureJoinForTest(
      joinConfigAsString = s"""
        | settings: {
        |    observationDataTimeSettings: {
        |      absoluteTimeRange: {
        |         timeFormat: yyyy-MM-dd
        |         startTime: 2018-05-01
        |         endTime: 2018-05-03
        |      }
        |    }
        |    joinTimeSettings: {
        |      timestampColumn: {
        |         def: timestamp
        |         format: yyyy-MM-dd
        |      }
        |    }
        |}
        |
        |features: [
        |
        |  {
        |    key: [x],
        |    featureList: ["simpleFeature", "likesFeature"]
        |  },
        |  {
        |    key: [x],
        |    featureList: ["commentsFeature"]
        |    overrideTimeDelay: 8d
        |  }
        |]
      """.stripMargin,
      featureDefAsString = """
       |sources: {
       |  swaSource: {
       |    location: { path: "slidingWindowAgg/localSWASimulateTimeDelay/daily" }
       |    isTimeSeries: true
       |    timeWindowParameters: {
       |      timestamp: "timestamp"
       |      timestamp_format: "yyyy-MM-dd"
       |    }
       |  }
       |}
       |
       |anchors: {
       |  swaAnchor: {
       |    source: "swaSource"
       |    key: "x"
       |    features: {
       |      simpleFeature: {
       |        def: "aggregationWindow"
       |        aggregation: COUNT
       |        window: 3d
       |      }
       |      likesFeature: {
       |        def: "foo"
       |        aggregation: COUNT
       |        window: 3d
       |      }
       |    }
       |  }
       |    swaAnchor2: {
       |    source: "swaSource"
       |    key: "x"
       |    features: {
       |      commentsFeature: {
       |        def: "bar"
       |        aggregation: COUNT
       |        window: 3d
       |      }
       |    }
       |  }
       |}
        """.stripMargin,
      observationDataPath = "slidingWindowAgg/localAnchorTestObsData.avro.json").data

    val result1 = res.collect().filter(x => x.getAs[String]("x") == "a2")
  }

  /**
   * test sliding window aggregation with timestamp column from the date partition.
   */
  @Test
  def testLocalAnchorSWASimulateTimeDelay(): Unit = {
    val res = runLocalFeatureJoinForTest(
      joinConfigAsString = s"""
                              | settings: {
                              | observationDataTimeSettings: {
                              |   absoluteTimeRange: {
                              |     timeFormat: yyyy-MM-dd
                              |     startTime: "2018-05-01"
                              |     endTime: "2018-05-03"
                              |   }
                              |  }
                              |  joinTimeSettings: {
                              |   timestampColumn: {
                              |     def: timestamp
                              |     format: yyyy-MM-dd
                              |   }
                              |   simulateTimeDelay: 7d
                              |  }
                              |}
                              |
                              |features: [
                              |
                              |  {
                              |    key: [x],
                              |    featureList: ["simpleFeature"]
                              |  }
                              |]
      """.stripMargin,
      featureDefAsString = """
                             |sources: {
                             |  swaSource: {
                             |    location: { path: "slidingWindowAgg/localSWASimulateTimeDelay/daily" }
                             |    timePartitionPattern: "yyyy/MM/dd"
                             |  }
                             |}
                             |
                             |anchors: {
                             |  swaAnchor: {
                             |    source: "swaSource"
                             |    key: "x"
                             |    features: {
                             |      simpleFeature: {
                             |        def: "aggregationWindow"
                             |        aggregation: COUNT
                             |        window: 3d
                             |      }
                             |    }
                             |  }
                             |}
        """.stripMargin,
      observationDataPath = "slidingWindowAgg/localAnchorTestObsData.avro.json").data

    val result = res.collect().filter(x => x.getAs[String]("x") == "a2")
    assertEquals(result.head.getAs[Float]("simpleFeature"), 2.0f, 1.0e-7)
  }

  /**
   * The test verifies that in SWA, when a filter is specified, the rows that do not meet filtering condition
   * should have null values.
   */
  @Test
  def testFilteredRowsOfSWAFeaturesHaveNulls(): Unit = {
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    isTimeSeries: true
        |    timeWindowParameters: {
        |      timestampColumn: "timestamp"
        |      timestampColumnFormat: "yyyy-MM-dd"
        |    }
        |  }
        |}
        |anchors: {
        |  swaAnchorWithKeyExtractor: {
        |    source: "swaSource"
        |    key: [x]
        |    features: {
        |      f: {
        |        def: "count"   // the column that contains the raw view count
        |        filter: "Id in (9)"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val features = Seq("f")
    val keyField = "x"
    val featureJoinAsString =
      s"""
         | settings: {
         |  joinTimeSettings: {
         |   timestampColumn: {
         |     def: timestamp
         |     format: yyyy-MM-dd
         |   }
         |  }
         |}
         |features: [
         |  {
         |    key: [$keyField],
         |    featureList: [${features.mkString(",")}]
         |  }
         |]
  """.stripMargin

    /**
     * Expected key and feature:
     * +--------+---+
     * |x|  f|
     * +--------+---+
     * |       1|  null|
     * |       2|  null|
     * |       3|  5   |
     * +--------+---+
     */
    val dfs = runLocalFeatureJoinForTest(featureJoinAsString, featureDefAsString, "featuresWithFilterObs.avro.json").data
    dfs.show()
    assertEquals(dfs.count(), 3) // Expected row count

    val featureValues = dfs.collect().sortBy(row => row.getAs[Int]("x"))
    assertEquals(featureValues(0).getAs[Float]("f"), null)
    assertEquals(featureValues(1).getAs[Float]("f"), null)
    assertEquals(featureValues(2).getAs[Float]("f"), 5.0f)
  }

  /**
   * The test verifies that in SWA, rows are eliminated only
   * when all feature values are nulls.
   */
  @Test
  def testSWAJoinFiltersOnlyWhenAllRowsAreNulls(): Unit = {
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    isTimeSeries: true
        |    timeWindowParameters: {
        |      timestampColumn: "timestamp"
        |      timestampColumnFormat: "yyyy-MM-dd"
        |    }
        |  }
        |}
        |anchors: {
        |  swaAnchorWithKeyExtractor: {
        |    source: "swaSource"
        |    key: [x]
        |    features: {
        |      f: {
        |        def: "count"   // the column that contains the raw view count
        |        filter: "Id in (10, 11)"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |      g: {
        |        def: "count"   // the column that contains the raw view count
        |        filter: "Id in (9)"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val features = Seq("f", "g")
    val keyField = "x"
    val featureJoinAsString =
      s"""
         | settings: {
         |  joinTimeSettings: {
         |   timestampColumn: {
         |     def: timestamp
         |     format: yyyy-MM-dd
         |   }
         |  }
         |}
         |features: [
         |  {
         |    key: [$keyField],
         |    featureList: [${features.mkString(",")}]
         |  }
         |]
      """.stripMargin

    /**
     * Expected output:
     * +--------+----+----+
     * |x|   f|   g|
     * +--------+----+----+
     * |       1|   6|null|
     * |       2|  17|null|
     * |       3|null|   5|
     * +--------+----+----+
     */
    val expectedSchema = StructType(
      Seq(
        StructField(keyField, LongType),
        StructField(features.head, LongType), // f
        StructField(features.last, LongType) // g
      ))

    val expectedRows = Array(
      new GenericRowWithSchema(Array(1, 6, null), expectedSchema),
      new GenericRowWithSchema(Array(2, 10, null), expectedSchema),
      new GenericRowWithSchema(Array(3, null, 5), expectedSchema))
    val dfs = runLocalFeatureJoinForTest(featureJoinAsString, featureDefAsString, "featuresWithFilterObs.avro.json").data
    dfs.show()

    validateRows(dfs.select(keyField, features: _*).collect().sortBy(row => row.getAs[Int](keyField)), expectedRows)
  }


  @Test
  def testSWACountDistinct(): Unit = {
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    isTimeSeries: true
        |    timeWindowParameters: {
        |      timestampColumn: "timestamp"
        |      timestampColumnFormat: "yyyy-MM-dd"
        |    }
        |  }
        |}
        |anchors: {
        |  swaAnchorWithKeyExtractor: {
        |    source: "swaSource"
        |    key: [x]
        |    features: {
        |      f: {
        |        def: "Id"   // the column that contains the raw view count
        |        aggregation: COUNT
        |        window: 10d
        |      }
        |      g: {
        |        def: "Id"   // the column that contains the raw view count
        |        aggregation: COUNT_DISTINCT
        |        window: 10d
        |      }
        |    }
        |  }
        |}
    """.stripMargin

    val features = Seq("f", "g")
    val keyField = "x"
    val featureJoinAsString =
      s"""
         | settings: {
         |  joinTimeSettings: {
         |   timestampColumn: {
         |     def: timestamp
         |     format: yyyy-MM-dd
         |   }
         |  }
         |}
         |features: [
         |  {
         |    key: [$keyField],
         |    featureList: [${features.mkString(",")}]
         |  }
         |]
    """.stripMargin


    /**
     * Expected output:
     * +--------+----+----+
     * |x|   f|   g|
     * +--------+----+----+
     * |       1|   6|   2|
     * |       2|   5|   2|
     * |       3|   1|   1|
     * +--------+----+----+
     */
    val expectedSchema = StructType(
      Seq(
        StructField(keyField, LongType),
        StructField(features.head, LongType), // f
        StructField(features.last, LongType) // g
      ))

    val expectedRows = Array(
      new GenericRowWithSchema(Array(1, 6, 2), expectedSchema),
      new GenericRowWithSchema(Array(2, 5, 2), expectedSchema),
      new GenericRowWithSchema(Array(3, 1, 1), expectedSchema))
    val dfs = runLocalFeatureJoinForTest(featureJoinAsString, featureDefAsString, "featuresWithFilterObs.avro.json").data

    validateRows(dfs.select(keyField, features: _*).collect().sortBy(row => row.getAs[Int](keyField)), expectedRows)
  }
}
