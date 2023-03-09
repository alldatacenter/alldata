package com.linkedin.feathr.offline

import com.linkedin.feathr.common.configObj.configbuilder.ConfigBuilderException
import com.linkedin.feathr.common.exception.FeathrConfigException
import com.linkedin.feathr.offline.config.location.SimplePath
import com.linkedin.feathr.offline.generation.SparkIOUtils
import com.linkedin.feathr.offline.job.PreprocessedDataFrameManager
import com.linkedin.feathr.offline.source.dataloader.{AvroJsonDataLoader, CsvDataLoader}
import com.linkedin.feathr.offline.util.FeathrTestUtils
import com.linkedin.feathr.offline.util.FeathrUtils.{SKIP_MISSING_FEATURE, setFeathrJobParam}
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types._
import org.testng.Assert.assertTrue
import org.testng.annotations.{BeforeClass, Test}

import scala.collection.mutable

/**
 * Integration test to test different type of anchored and passthrough features and anchor extractors with default values.
 */
class AnchoredFeaturesIntegTest extends FeathrIntegTest {
  val feathrConf =
    """sources: {
      |  ptSource: {
      |    type: "PASSTHROUGH"
      |  }
      |}
      |
      |anchors: {
      |  anchor1: {
      |    source: "%s"
      |    key: "x"
      |    features: {
      |      aa: {
      |       def: "a"
      |       default: 2
      |       type: "CATEGORICAL"
      |      }
      |
      |      bb: {
      |       def: "b"
      |       default: 3
      |       type: "NUMERIC"
      |      }
      |
      |      cc: {
      |       def: "a"
      |       default: 4
      |       type: "NUMERIC"
      |      }
      |
      |      dd: {
      |       def: "c"
      |       type: "DENSE_VECTOR"
      |      }
      |      ee: {
      |       def: "c"
      |       type: "DENSE_VECTOR"
      |       default: [7,8,9]
      |      }
      |      ee2: {
      |       def: "c"
      |       type: {
      |           type: TENSOR
      |           tensorCategory: DENSE
      |           dimensionType: [INT]
      |           valType: FLOAT
      |           }
      |       default: []
      |      }
      |      ff: {
      |       def: "c"
      |       default: [6,7]
      |      }
      |      fListOfMap: {
      |       def: "[IdInFeatureData:c[1]*c[1]]"
      |       default: {"A5":10,"A8":100}
      |      }
      |      fInt: {
      |       def: "a"
      |       default: 100
      |      }
      |      fFloat: {
      |       def: "b"
      |       default: 101.1
      |      }
      |      fString: {
      |       def: "IdInFeatureData"
      |       default: "defaulta"
      |      }
      |      fNumberArray: {
      |       def: "c"
      |       default: [9,10]
      |      }
      |      fStringToNumericMap: {
      |       def: "e"
      |       default: {"x":5.5}
      |      }
      |      fStringArray: {
      |       def: "f"
      |       default: ["abc", "def"]
      |      }
      |
      |      fIntAsCategorical: {
      |       def: "a"
      |       default: 100
      |       type: CATEGORICAL
      |      }
      |
      |      fNumberArrayAsCategoricalSet: {
      |       def: "c"
      |       default: [9,10]
      |       type: CATEGORICAL_SET
      |      }
      |      fStringArrayAsCategoricalSet: {
      |       def: "f"
      |       default: ["abc", "def"]
      |       type: CATEGORICAL_SET
      |      }
      |    }
      |  }
      |
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
      |derivations: {
      |  multiply_a_b: "toNumeric(aa) * toNumeric(bb)"
      |
      |  categorical_b: {
      |    key: [foo]
      |    inputs: { foo_b: { key: foo, feature: bb } }
      |    definition: "toCategorical(foo_b)"
      |  }
      |}
      """.stripMargin.format(trainingData, featureData)
  @BeforeClass
  def setupAnchorsTest(): Unit = {
    MockAvroData.createMockxFeatureData(featureData)

    // define anchors/derivations
    val mockConfigFolder = generatedDataFolder + "/config"
    val feathrConfigPath = mockConfigFolder + "/feathr.conf"

    TestIOUtils.writeToFile(feathrConf, feathrConfigPath)

    // create a data source from anchorAndDerivations/nullValueSource.avro.json
    val df = new AvroJsonDataLoader(ss, "nullValueSource.avro.json").loadDataFrame()
    SparkIOUtils.writeDataFrame(df, SimplePath(mockDataFolder + "/nullValueSource"), parameters=Map(), dataLoaderHandlers=List())
  }

  /**
   * Test single key join. The joint output
   *
   * - includes float and int features
   * - includes numeric and categorical feature type tags
   * - includes default values for every type.
   */
  @Test
  def testSingleKeyJoinWithDifferentFeatureTypes(): Unit = {
    val selectedColumns = Seq("x", "aa", "bb", "cc", "dd", "ee", "ee2", "ff", "multiply_a_b", "categorical_b") // , "z")
    val featureJoinConf =
      s"""
         |
         |features2: [
         |  {
         |    key: ${selectedColumns.head}
         |    featureList: [${selectedColumns.tail.mkString(",")}]
         |  }
         |]
         |""".stripMargin

    val df = runLocalFeatureJoinForTest(featureJoinConf, feathrConf, trainingData)
    val filteredDf = df.data.select(selectedColumns.head, selectedColumns.tail: _*)

    val expectedDf = ss.createDataFrame(
      ss.sparkContext.parallelize(
        Seq(
          Row(
            // IdInObservation
            "a:3",
            // aa
            Row(mutable.WrappedArray.make(Array("2")), mutable.WrappedArray.make(Array(1.0f))),
            // bb
            3.0f,
            // cc
            4.0f,
            // dd
            null,
            // ee
            mutable.WrappedArray.make(Array(7.0f, 8.0f, 9.0f)),
            // ee2
            mutable.WrappedArray.empty,
            // ff
            mutable.WrappedArray.make(Array(6.0f, 7.0f)),
            // multiply_a_b
            Row(mutable.WrappedArray.make(Array("")), mutable.WrappedArray.make(Array(6.0f))),
            // categorical_b
            Row(mutable.WrappedArray.make(Array("3.0")), mutable.WrappedArray.make(Array(1.0f))),
            // z
            null),
          Row(
            // IdInObservation
            "a:1",
            // aa
            Row(mutable.WrappedArray.make(Array("1")), mutable.WrappedArray.make(Array(1.0f))),
            // bb
            2.6f,
            // cc
            1.0f,
            // dd
            mutable.WrappedArray.make(Array(1.0f, 2.0f, 3.0f)),
            // ee
            mutable.WrappedArray.make(Array(1.0f, 2.0f, 3.0f)),
            // ee2
            mutable.WrappedArray.make(Array(1.0f, 2.0f, 3.0f)),
            // ff
            mutable.WrappedArray.make(Array(1.0f, 2.0f, 3.0f)),
            // multiply_a_b
            Row(mutable.WrappedArray.make(Array("")), mutable.WrappedArray.make(Array(2.6f))),
            // categorical_b
            Row(mutable.WrappedArray.make(Array("2.6")), mutable.WrappedArray.make(Array(1.0f))),
            // z
            Row(mutable.WrappedArray.make(Array("")), mutable.WrappedArray.make(Array(-1.0f)))),
          Row(
            // IdInObservation
            "a:2",
            // aa
            Row(mutable.WrappedArray.make(Array("4")), mutable.WrappedArray.make(Array(1.0f))),
            // bb
            8.0f,
            // cc
            4.0f,
            // dd
            mutable.WrappedArray.make(Array(4.0f, 5.0f, 6.0f)),
            // ee
            mutable.WrappedArray.make(Array(4.0f, 5.0f, 6.0f)),
            // ee2
            mutable.WrappedArray.make(Array(4.0f, 5.0f, 6.0f)),
            // ff
            mutable.WrappedArray.make(Array(4.0f, 5.0f, 6.0f)),
            // multiply_a_b
            Row(mutable.WrappedArray.make(Array("")), mutable.WrappedArray.make(Array(32.0f))),
            // categorical_b
            Row(mutable.WrappedArray.make(Array("8.0")), mutable.WrappedArray.make(Array(1.0f))),
            // z
            Row(mutable.WrappedArray.make(Array("")), mutable.WrappedArray.make(Array(-4.0f)))))),
      StructType(
        List(
          StructField("IdInObservation", StringType, true),
          StructField(
            "aa",
            StructType(List(StructField("indices0", ArrayType(StringType, true), true), StructField("values", ArrayType(FloatType, true), true)))),
          StructField("bb", FloatType, true),
          StructField("cc", FloatType, true),
          StructField("dd", ArrayType(FloatType, true), true),
          StructField("ee", ArrayType(FloatType, false), true),
          StructField("ee2", ArrayType(FloatType, false), true),
          StructField("ff", ArrayType(FloatType, false), true),
          StructField(
            "multiply_a_b",
            StructType(List(StructField("indices0", ArrayType(StringType, false), false), StructField("values", ArrayType(FloatType, false), false)))),
          StructField(
            "categorical_b",
            StructType(List(StructField("indices0", ArrayType(StringType, false), false), StructField("values", ArrayType(FloatType, false), false)))),
          StructField(
            "z",
            StructType(List(StructField("indices0", ArrayType(StringType, false), false), StructField("values", ArrayType(FloatType, false), false)))))))
    def cmpFunc(row: Row): String = row.get(0).toString
    filteredDf.show(10)
    // FeathrTestUtils.assertDataFrameApproximatelyEquals(filteredDf, expectedDf, cmpFunc)
  }

  /*
   * Test skipping combination of anchored, derived and swa features.
   */
  @Test
  def testSkipAnchoredFeatures: Unit = {
    setFeathrJobParam(SKIP_MISSING_FEATURE, "true")
    val df = runLocalFeatureJoinForTest(
      joinConfigAsString =
        """
          |settings: {
          |  joinTimeSettings: {
          |    timestampColumn: {
          |       def: "timestamp"
          |       format: "yyyy-MM-dd"
          |    }
          |    simulateTimeDelay: 1d
          |  }
          |}
          |
          | features: {
          |   key: a_id
          |   featureList: ["featureWithNull", "derived_featureWithNull", "featureWithNull2", "derived_featureWithNull2",
          |    "aEmbedding", "memberEmbeddingAutoTZ"]
          | }
      """.stripMargin,
      featureDefAsString =
        """
          | sources: {
          |  swaSource: {
          |    location: { path: "generaion/daily" }
          |    timePartitionPattern: "yyyy/MM/dd"
          |    timeWindowParameters: {
          |      timestampColumn: "timestamp"
          |      timestampColumnFormat: "yyyy-MM-dd"
          |    }
          |  }
          |  swaSource1: {
          |    location: { path: "generation/daily" }
          |    timePartitionPattern: "yyyy/MM/dd"
          |    timeWindowParameters: {
          |      timestampColumn: "timestamp"
          |      timestampColumnFormat: "yyyy-MM-dd"
          |    }
          |  }
          |}
          |
          | anchors: {
          |  anchor1: {
          |    source: "anchorAndDerivations/nullVaueSource.avro.json"
          |    key: "toUpperCaseExt(mId)"
          |    features: {
          |      featureWithNull: "isPresent(value) ? toNumeric(value) : 0"
          |    }
          |  }
          |  anchor2: {
          |    source: "anchorAndDerivations/nullValueSource.avro.json"
          |    key: "toUpperCaseExt(mId)"
          |    features: {
          |      featureWithNull2: "isPresent(value) ? toNumeric(value) : 0"
          |    }
          |  }
          |  swaAnchor: {
          |    source: "swaSource"
          |    key: "x"
          |    features: {
          |      aEmbedding: {
          |        def: "embedding"
          |        aggregation: LATEST
          |        window: 3d
          |      }
          |    }
          |  }
          |  swaAnchor1: {
          |    source: "swaSource1"
          |    key: "x"
          |    features: {
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
          |derivations: {
          |
          | derived_featureWithNull: "featureWithNull * 2"
          | derived_featureWithNull2: "featureWithNull2 * 2"
          |}
        """.stripMargin,
      observationDataPath = "anchorAndDerivations/testMVELLoopExpFeature-observations.csv")

    assertTrue(!df.data.columns.contains("featureWithNull"))
    assertTrue(!df.data.columns.contains("derived_featureWithNull"))
    assertTrue(df.data.columns.contains("derived_featureWithNull2"))
    assertTrue(df.data.columns.contains("featureWithNull2"))
    assertTrue(!df.data.columns.contains("aEmbedding"))
    assertTrue(df.data.columns.contains("memberEmbeddingAutoTZ"))
    setFeathrJobParam(SKIP_MISSING_FEATURE, "false")
  }

  /*
   * Test features with fdsExtract.
   */
  @Test
  def testFeaturesWithFdsExtract: Unit = {
    val df = runLocalFeatureJoinForTest(
      joinConfigAsString =
        """
          | features: {
          |   key: a_id
          |   featureList: ["featureWithNull"]
          | }
      """.stripMargin,
      featureDefAsString =
        """
          | anchors: {
          |  anchor1: {
          |    source: "anchorAndDerivations/nullValueSource.avro.json"
          |    key.sqlExpr: mId
          |    features: {
          |      featureWithNull.def.sqlExpr: FDSExtract(value)
          |    }
          |  }
          |}
        """.stripMargin,
      observationDataPath = "anchorAndDerivations/testMVELLoopExpFeature-observations.csv")

    val selectedColumns = Seq("a_id", "featureWithNull")
    val filteredDf = df.data.select(selectedColumns.head, selectedColumns.tail: _*)

    val expectedDf = ss.createDataFrame(
      ss.sparkContext.parallelize(
        Seq(
          Row(
            // a_id
            "1",
            // featureWithNull
            1),
          Row(
            // a_id
            "2",
            // featureWithNull
            null),
          Row(
            // a_id
            "3",
            // featureWithNull
            3))),
      StructType(
        List(
          StructField("a_id", StringType, true),
          StructField("featureWithNull", IntegerType, true))))

    def cmpFunc(row: Row): String = row.get(0).toString

    FeathrTestUtils.assertDataFrameApproximatelyEquals(filteredDf, expectedDf, cmpFunc)
  }


  /*
   * Test features with null values.
   */
  @Test
  def testLocalAnchorSimpleTest: Unit = {
    val df = runLocalFeatureJoinForTest(
      joinConfigAsString = """
                             | features: {
                             |   key: a_id
                             |   featureList: ["featureWithNull"]
                             | }
      """.stripMargin,
      featureDefAsString = """
                             | anchors: {
                             |  anchor1: {
                             |    source: "anchorAndDerivations/nullValueSource.avro.json"
                             |    key: "toUpperCaseExt(mId)"
                             |    features: {
                             |      featureWithNull: "isPresent(value) ? toNumeric(value) : 0"
                             |    }
                             |  }
                             |}
        """.stripMargin,
      observationDataPath = "anchorAndDerivations/testMVELLoopExpFeature-observations.csv")

    val selectedColumns = Seq("a_id", "featureWithNull")
    val filteredDf = df.data.select(selectedColumns.head, selectedColumns.tail: _*)

    val expectedDf = ss.createDataFrame(
      ss.sparkContext.parallelize(
        Seq(
          Row(
            // a_id
            "1",
            // featureWithNull
            1.0f),
          Row(
            // a_id
            "2",
            // featureWithNull
            0.0f),
          Row(
            // a_id
            "3",
            // featureWithNull
            3.0f))),
      StructType(
        List(
          StructField("a_id", StringType, true),
          StructField("featureWithNull", FloatType, true))))
    def cmpFunc(row: Row): String = row.get(0).toString
    FeathrTestUtils.assertDataFrameApproximatelyEquals(filteredDf, expectedDf, cmpFunc)
  }

  /**
   * This test validates that Passthrough features specified over multiple anchors
   * do not get dropped silently in the output. TODO: Enable test after FCM can handle new config syntax
   */
  @Test(enabled = false)
  def testPassthroughFeaturesNotDroppedWithMultipleAnchors(): Unit = {
    val featureDefAsString =
      """
        |sources: {
        |  ptSource: {
        |    type: "PASSTHROUGH"
        |  }
        |}
        |
        |anchors: {
        |  anchor1: {
        |    source: ptSource
        |    key: "qid"
        |    features: {
        |        f1: "extract_term_value_from_array(features, 'term', 'value', \"name == 'f1'\")"
        |    }
        |  }
        |}
      """.stripMargin

    val passthroughFeatures = Seq(
      "f1")

    val joinConfigAsString =
      s"""
         |feathrFeatures: [
         |  {
         |    key: qid,
         |    featureList: [${passthroughFeatures.mkString(",")}]
         |  }
         |]
      """.stripMargin

    val df = runLocalFeatureJoinForTest(joinConfigAsString, featureDefAsString, "anchorAndDerivations/passThrough/passthrough.avro.json")
    val fieldNamesSet = df.data.schema.fieldNames.toSet
    assertTrue(
      passthroughFeatures.forall(fieldNamesSet.contains),
      s"Missing features in output: [${passthroughFeatures.toSet.diff(fieldNamesSet).mkString(",")}]")
  }


  /**
   * This test should be enabled once the naming convention has been resolved.

   */
  @Test(enabled = false, expectedExceptions = Array(classOf[ConfigBuilderException]))
  def testInvalidFeatureName: Unit = {
    val df = runLocalFeatureJoinForTest(
      joinConfigAsString = """
                             | features: [
                             | {
                             |   key: [viewee, viewer]
                             |   featureList: [ "foo-feature"]
                             | }
                             | ]
      """.stripMargin,
      featureDefAsString = """
                             | anchors: {
                             |   anchor1: {
                             |     source: anchor6-source.csv
                             |     key.sqlExpr: [sourceId, destId]
                             |     features: {
                             |       foo-feature: {
                             |         def.sqlExpr: cast(source as int)
                             |         type: NUMERIC
                             |       }
                             |     }
                             |   }
                             | }
        """.stripMargin,
      observationDataPath = "anchorAndDerivations/derivations/test2-observations.csv")

    val featureList = df.data.collect().sortBy(row => (row.getAs[String]("viewer"), row.getAs[String]("viewee")))
  }


  // When feature name and observation data field names conflicts, should throw exception
  @Test(expectedExceptions = Array(classOf[FeathrConfigException]), expectedExceptionsMessageRegExp = ".*Please rename feature.*")
  def testFeatureNameConflicts: Unit = {
    val ds = runLocalFeatureJoinForTest(
      joinConfigAsString =
        """
          | features: {
          |   key: viewer
          |   featureList: ["viewee"]
          | }
      """.stripMargin,
      featureDefAsString =
        """
          | anchors: {
          |  anchor1: {
          |    source: "ds:///anchorFeatureIntegTest.nullValueSource"
          |    key: "mId"
          |    features: {
          |      viewee: "isPresent(Value) ? toNumeric(Value) : 0"
          |    }
          |  }
          |}
        """.stripMargin,
      observationDataPath = "anchorAndDerivations/test5-observations.csv")
    ds.data.show()
  }

  // TODO: Enable after FCM can handle new syntax
  @Test(enabled = false)
  def testPassthroughFeaturesWithSWA(): Unit = {
    val featureDefAsString =
      """
        |anchors: {
        |  nonAggFeatures: {
        |    source: PASSTHROUGH
        |    key: NOT_NEEDED
        |    features: {
        |
        |      f_trip_distance: "(float)trip_distance"
        |
        |      f_is_long_trip_distance: "trip_distance>30"
        |
        |      f_trip_time_duration: "time_duration(lpep_pickup_datetime, lpep_dropoff_datetime, 'minutes')"
        |
        |      f_day_of_week: "dayofweek(lpep_dropoff_datetime)"
        |
        |      f_day_of_month: "dayofmonth(lpep_dropoff_datetime)"
        |
        |      f_hour_of_day: "hourofday(lpep_dropoff_datetime)"
        |    }
        |  }
        |
        |  aggregationFeatures: {
        |    source: nycTaxiBatchSource
        |    key: DOLocationID
        |    features: {
        |      f_location_avg_fare: {
        |        def: "float(fare_amount)"
        |        aggregation: AVG
        |        window: 3d
        |      }
        |      f_location_max_fare: {
        |        def: "float(fare_amount)"
        |        aggregation: MAX
        |        window: 3d
        |      }
        |    }
        |  }
        |}
        |
        |derivations: {
        |   f_trip_time_distance: {
        |       definition: "f_trip_distance * f_trip_time_duration"
        |       type: NUMERIC
        |   }
        |   f_trip_time_distance_sql: {
        |    key: [trip]
        |     inputs: {
        |       trip_distance: { key: [trip], feature: f_trip_distance }
        |       trip_time_duration: { key: [trip], feature: f_trip_time_duration }
        |     }
        |     definition.sqlExpr: "trip_distance * trip_time_duration"
        |     type: NUMERIC
        |   }
        |}
        |sources: {
        |  nycTaxiBatchSource: {
        |    location: { path: "/driver_data/green_tripdata_2021-01.csv" }
        |    timeWindowParameters: {
        |      timestampColumn: "lpep_dropoff_datetime"
        |      timestampColumnFormat: "yyyy-MM-dd HH:mm:ss"
        |    }
        |  }
        |}
        |
      """.stripMargin

    val joinConfigAsString =
      s"""
         |settings: {
         | joinTimeSettings: {
         |    timestampColumn: {
         |     def: "lpep_dropoff_datetime"
         |     format: "yyyy-MM-dd HH:mm:ss"
         |    }
         |  }
         |}
         |
         |featureList: [
         |  {
         |    key: DOLocationID
         |    featureList: [f_location_avg_fare, f_trip_time_distance, f_trip_distance,
         |     f_trip_time_duration, f_is_long_trip_distance, f_day_of_week, f_trip_time_distance_sql]
         |  }
         |]
      """.stripMargin

    val df = runLocalFeatureJoinForTest(joinConfigAsString, featureDefAsString, "/driver_data/green_tripdata_2021-01.csv")
    df.data.show()
  }

  // TODO: Enable after FCM can handle new syntax
  @Test(enabled = false)
  def tesSWAWithPreprocessing(): Unit = {
    val featureDefAsString =
      """
        |anchors: {
        |  nonAggFeatures: {
        |    source: PASSTHROUGH
        |    key: NOT_NEEDED
        |    features: {
        |
        |      f_trip_distance: "(float)trip_distance"
        |
        |      f_is_long_trip_distance: "trip_distance>30"
        |
        |      f_trip_time_duration: "time_duration(lpep_pickup_datetime, lpep_dropoff_datetime, 'minutes')"
        |
        |      f_day_of_week: "dayofweek(lpep_dropoff_datetime)"
        |
        |      f_day_of_month: "dayofmonth(lpep_dropoff_datetime)"
        |
        |      f_hour_of_day: "hourofday(lpep_dropoff_datetime)"
        |    }
        |  }
        |
        |  aggregationFeatures: {
        |    source: nycTaxiBatchSource
        |    key: DOLocationID
        |    features: {
        |      f_location_avg_fare: {
        |        def: "float(fare_amount)"
        |        aggregation: AVG
        |        window: 3d
        |      }
        |      f_location_max_fare: {
        |        def: "float(fare_amount)"
        |        aggregation: MAX
        |        window: 3d
        |      }
        |    }
        |  }
        |  aggregationFeatures333: {
        |    source: nycTaxiBatchSource_with_new_dropoff
        |    key: DOLocationID
        |    features: {
        |      f_location_avg_new_tip_amount: {
        |        def: "float(new_tip_amount)"
        |        aggregation: AVG
        |        window: 3d
        |      }
        |      f_location_max_new_improvement_surcharge: {
        |        def: "float(new_improvement_surcharge)"
        |        aggregation: MAX
        |        window: 3d
        |      }
        |    }
        |  }
        |}
        |
        |derivations: {
        |   f_trip_time_distance: {
        |     definition: "f_trip_distance * f_trip_time_duration"
        |     type: NUMERIC
        |   }
        |}
        |sources: {
        |  nycTaxiBatchSource: {
        |    location: { path: "/driver_data/green_tripdata_2021-01.csv" }
        |    timeWindowParameters: {
        |      timestampColumn: "new_lpep_dropoff_datetime"
        |      timestampColumnFormat: "yyyy-MM-dd HH:mm:ss"
        |    }
        |  }
        |  nycTaxiBatchSource_with_new_dropoff: {
        |    location: { path: "/driver_data/green_tripdata_2021-01.csv" }
        |    timeWindowParameters: {
        |      timestampColumn: "new_lpep_pickup_datetime"
        |      timestampColumnFormat: "yyyy-MM-dd HH:mm:ss"
        |    }
        |  }
        |  nycTaxiBatchSource3: {
        |    location: { path: "/driver_data/copy_green_tripdata_2021-01.csv" }
        |    timeWindowParameters: {
        |      timestampColumn: "new_lpep_dropoff_datetime"
        |      timestampColumnFormat: "yyyy-MM-dd HH:mm:ss"
        |    }
        |  }
        |}
        |
      """.stripMargin

    val joinConfigAsString =
      s"""
         |settings: {
         | joinTimeSettings: {
         |    timestampColumn: {
         |     def: "lpep_dropoff_datetime"
         |     format: "yyyy-MM-dd HH:mm:ss"
         |    }
         |  }
         |}
         |
         |featureList: [
         |  {
         |    key: DOLocationID
         |    featureList: [f_location_avg_new_tip_amount, f_location_max_new_improvement_surcharge, f_location_avg_fare, f_location_max_fare, f_trip_time_distance, f_trip_distance, f_trip_time_duration, f_is_long_trip_distance, f_day_of_week]
         |  }
         |]
      """.stripMargin

    val df1 = new CsvDataLoader(ss, "src/test/resources/mockdata//driver_data/green_tripdata_2021-01.csv")
      .loadDataFrame()
      .withColumn("new_lpep_dropoff_datetime", col("lpep_dropoff_datetime"))
      .withColumn("new_fare_amount", col("fare_amount") + 1000000)
    val df2 = new CsvDataLoader(ss, "src/test/resources/mockdata//driver_data/green_tripdata_2021-01.csv")
      .loadDataFrame()
      .withColumn("new_improvement_surcharge", col("improvement_surcharge") + 1000000)
      .withColumn("new_tip_amount", col("tip_amount") + 1000000)
      .withColumn("new_lpep_pickup_datetime", col("lpep_pickup_datetime"))

    PreprocessedDataFrameManager.preprocessedDfMap = Map("f_location_avg_fare,f_location_max_fare" -> df1, "f_location_avg_new_tip_amount,f_location_max_new_improvement_surcharge" -> df2)
    val df = runLocalFeatureJoinForTest(joinConfigAsString, featureDefAsString, "/driver_data/green_tripdata_2021-01.csv")

    val selectedColumns = Seq("DOLocationID", "f_location_avg_new_tip_amount")
    val filteredDf = df.data.select(selectedColumns.head, selectedColumns.tail: _*)

    val expectedDf = ss.createDataFrame(
      ss.sparkContext.parallelize(
        Seq(
          Row(
            // DOLocationID
            "239",
            // f_location_avg_new_tip_amount
            1000002.8f),
          Row(
            // DOLocationID
            "151",
            // f_location_avg_new_tip_amount
            1000000.0f),
          Row(
            // DOLocationID
            "42",
            // f_location_avg_new_tip_amount
            1000001.0f),
          Row(
            // DOLocationID
            "75",
            // f_location_avg_new_tip_amount
            1000000.0f),
        )),
      StructType(
        List(
          StructField("DOLocationID", StringType, true),
          StructField("f_location_avg_new_tip_amount", FloatType, true))))
    def cmpFunc(row: Row): String = row.get(0).toString
    FeathrTestUtils.assertDataFrameApproximatelyEquals(filteredDf, expectedDf, cmpFunc)
    df.data.show()
    PreprocessedDataFrameManager.preprocessedDfMap = Map()
  }
}