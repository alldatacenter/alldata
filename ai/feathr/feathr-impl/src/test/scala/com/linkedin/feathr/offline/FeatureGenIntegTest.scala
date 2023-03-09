package com.linkedin.feathr.offline

import com.linkedin.feathr.common.exception.FeathrException
import com.linkedin.feathr.offline.AssertFeatureUtils._
import com.linkedin.feathr.offline.job.PreprocessedDataFrameManager
import com.linkedin.feathr.offline.source.dataloader.CsvDataLoader
import com.linkedin.feathr.offline.util.FeathrUtils.{SKIP_MISSING_FEATURE, setFeathrJobParam}
import com.linkedin.feathr.offline.util.{FeathrTestUtils, FeatureGenConstants}
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types._
import org.testng.Assert.{assertEquals, assertNotNull, assertTrue}
import org.testng.annotations.Test

import scala.collection.mutable

/**
 * Integration tests to test feature generation APIs in feathr offline.
 */
class FeatureGenIntegTest extends FeathrIntegTest {
  private val defaultSwaSource =
    """
      |  swaSource: {
      |    location: { path: "generation/daily/" }
      |    timePartitionPattern: "yyyy/MM/dd"
      |    timeWindowParameters: {
      |      timestampColumn: "timestamp"
      |      timestampColumnFormat: "yyyy-MM-dd"
      |    }
      |  }
    """.stripMargin

  private val malformedSource =
    """
      |  swaSource11: {
      |    location: { path: "generation/daiy/" }
      |    timePartitionPattern: "yyyy/MM/dd"
      |    timeWindowParameters: {
      |      timestampColumn: "timestamp"
      |      timestampColumnFormat: "yyyy-MM-dd"
      |    }
      |  }
    """.stripMargin
  val aFeaturesGroup = Seq(
    // feature group 1:
    // A feature ref string with "-" delimiters
    "myNS-aDisinterestEmbeddingFromMultiTaskModelT109-1-0",
    // same source, same anchor, mvel features (has key)
    "mockdata_a_ct",
    "mockdata_a_sample",
    // same source, another different anchor, mvel features (has key)
    "mockdata_a_b",
    // feature group 2:
    // customized extractor extends the old AnchorExtractor[GenericRecord], they do not have key field in config
    // or getKeyColumnNames in the extractor implementation, however, in the config above, we added a new
    // filed 'keyAlias', so we can now support it
    "mockdata_a_b_row_generic_extractor",
    // feature group 3:
    // simlar to mockdata_a_b_row_generic_extractor, this customized extractor extends the old
    // AnchorExtractor[SpecificRecord], they do not have key field in config or getKeyColumnNames
    // in the extractor implementation, but with the 'keyAlias' field added, we can support this as well
    "mockdata_a_b_row_specific_extractor",
    // feature group 4:
    // same source, different key, but same alias, mvel features
    "mockdata_a_b_diff_key",
    // feature group 6:
    // same source, same anchor, sql features
    "mockdata_a_sample_map_local",
    "mockdata_a_sample_map_local_default",
    // same source, different anchor, sql features
    "mockdata_a_b_local",
    // feature group 7
    // same source, different anchor, sql features, with key alias
    "mockdata_a_b_local_with_alias",
    // feature group 8:
    // same source, different anchor, dataframe-based key extractor features
    "mockdata_a_b_key_extractor")
  val aFeatureGroup = Seq(
    // feature group 5:
    // different source, mvel anchor
    "az_mvel",
    // feature group 9:
    // different source, sql anchor
    "az_sql")

  val sampleSwaFeatureDefConfig =
    """
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
      |anchors: {
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
      |  }
      |
      |  swaAnchorWithKeyExtractor2: {
      |    source: "swaSource"
      |    keyExtractor: "com.linkedin.feathr.offline.anchored.keyExtractor.SimpleSampleKeyExtractor"
      |    features: {
      |      f4: {
      |        def: "aggregationWindow"
      |        aggregation: SUM
      |        window: 3d
      |      }
      |    }
      |  }
      |
      |  swaAnchorWithKeyExtractor3: {
      |    source: "swaSource"
      |    keyExtractor: "com.linkedin.feathr.offline.anchored.keyExtractor.SimpleSampleKeyExtractorWithOtherKey"
      |    features: {
      |      f5: {
      |        def: "aggregationWindow"
      |        aggregation: SUM
      |        window: 3d
      |      }
      |    }
      |  }
      |}
    """.stripMargin

  /**
   * Test timePartitionPattern in the middle of data sources path
   */
  @Test
  def testTimePartitionPatternMiddlePath(): Unit = {
    val applicationConfig = generateSimpleApplicationConfig(features = "f3, f4")
    val featureDefConfig =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "slidingWindowAgg/localSWAAnchorTestFeatureData/daily" }
        |    timePartitionPattern: "yyyy/MM/dd"
        |    postfixPath: "postfixPath"
        |    timeWindowParameters: {
        |      timestampColumn: "timestamp"
        |      timestampColumnFormat: "yyyy-MM-dd"
        |    }
        |  }
        |}
        |anchors: {
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
        |  }
        |
        |  swaAnchorWithKeyExtractor2: {
        |    source: "swaSource"
        |    keyExtractor: "com.linkedin.feathr.offline.anchored.keyExtractor.SimpleSampleKeyExtractor"
        |    features: {
        |      f4: {
        |        def: "aggregationWindow"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |    }
        |  }
        |}
      """.stripMargin
    val dfs = localFeatureGenerate(applicationConfig, featureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    // we should have 8 dataframes, each one contains a group of feature above
    assertEquals(dfCount, 1)
    // group by dataframe
    val featureList =
      dfs.head._2.data.collect().sortBy(row => (row.getAs[String]("key0"), row.getAs[String]("key1")))
    assertEquals(featureList.size, 4)
    assertEquals(featureList(0).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(0).getAs[Float]("f4"), 1f, 1e-5)
    assertEquals(featureList(1).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(1).getAs[Float]("f4"), 1f, 1e-5)
  }

  /**
   * Test sliding window aggregation feature using key extractor in multiple anchors
   */
  @Test
  def testSWAFeatureGenMultiAnchorKeyExtractor(): Unit = {
    val applicationConfig = generateSimpleApplicationConfig(features = "f3, f4")
    val featureDefConfig =
      """
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
        |anchors: {
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
        |  }
        |
        |  swaAnchorWithKeyExtractor2: {
        |    source: "swaSource"
        |    keyExtractor: "com.linkedin.feathr.offline.anchored.keyExtractor.SimpleSampleKeyExtractor"
        |    features: {
        |      f4: {
        |        def: "aggregationWindow"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |    }
        |  }
        |
        |  swaAnchorWithKeyExtractor3: {
        |    source: "swaSource"
        |    keyExtractor: "com.linkedin.feathr.offline.anchored.keyExtractor.SimpleSampleKeyExtractorWithOtherKey"
        |    features: {
        |      f5: {
        |        def: "aggregationWindow"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |    }
        |  }
        |}
    """.stripMargin
    val dfs = localFeatureGenerate(applicationConfig, featureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    // we should have 8 dataframes, each one contains a group of feature above
    assertEquals(dfCount, 1)
    // group by dataframe
    val featureList =
      dfs.head._2.data.collect().sortBy(row => (row.getAs[String]("key0"), row.getAs[String]("key1")))
    assertEquals(featureList.size, 4)
    assertEquals(featureList(0).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(0).getAs[Float]("f4"), 1f, 1e-5)
    assertEquals(featureList(1).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(1).getAs[Float]("f4"), 1f, 1e-5)
  }

  /**
   * Test with non SWA feature and an application config writing to an output directory.
   */
  @Test(enabled = false)
  def testFeatureGenWithApplicationConfig(): Unit = {
    val applicationConfig =
      s"""
         | operational: {
         |  name: generateWithDefaultParams
         |  endTime: 2021-01-02
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[
         |  {
         |      name: REDIS
         |      params: {
         |        table_name: "nycFeatures"
         |      }
         |   }
         |  ]
         |}
         |features: [f_location_avg_fare, f_location_max_fare]
      """.stripMargin
    val featureDefConfig =
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
        |      f_trip_time_duration: "import java.time.LocalDateTime; import java.time.format.DateTimeFormatter; import java.time.Duration; DateTimeFormatter formatter = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'); LocalDateTime pickupDate = LocalDateTime.parse(lpep_pickup_datetime, formatter); LocalDateTime dropoffDate = LocalDateTime.parse(lpep_dropoff_datetime, formatter); Duration.between(pickupDate, dropoffDate).toMinutes();"
        |
        |      f_day_of_week: "import java.time.LocalDateTime;import java.time.format.DateTimeFormatter; DateTimeFormatter formatter = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss');LocalDateTime localDate = LocalDateTime.parse(lpep_pickup_datetime, formatter);localDate.getDayOfWeek().getValue()"
        |
        |      f_day_of_month: "import java.time.LocalDateTime;import java.time.format.DateTimeFormatter; DateTimeFormatter formatter = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss');LocalDateTime localDate = LocalDateTime.parse(lpep_pickup_datetime, formatter);localDate.getDayOfMonth()"
        |
        |      f_hour_of_day: "import java.time.LocalDateTime;import java.time.format.DateTimeFormatter; DateTimeFormatter formatter = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss');LocalDateTime localDate = LocalDateTime.parse(lpep_pickup_datetime, formatter);localDate.getHour()"
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
        |     definition: "f_trip_distance * f_trip_time_duration"
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
        |""".stripMargin

    val res = localFeatureGenerate(applicationConfig, featureDefConfig)
    res.head._2.data.show(100)
  }

  /**
   * Test with non SWA feature in streaming mode. Disabled for now. Need to set environment variables to enable it.
   */
  @Test(enabled = false)
  def testStreamingFeatureGen(): Unit = {
    val applicationConfig =
      s"""
         | operational: {
         |  name: generateWithDefaultParams
         |  endTime: 2021-01-02
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[
         |  {
         |      name: REDIS
         |      params: {
         |        table_name: "nycFeaturesStreaming"
         |        streaming: true
         |      }
         |   }
         |  ]
         |}
         |features: [f_modified_streaming_count, f_modified_streaming_count2, trips_today]
      """.stripMargin
    val featureDefConfig =
      """
        |anchors: {
        |  kafkaAnchor: {
        |     source: kafkaStreamingSource
        |     key.sqlExpr: [driver_id]
        |     features: {
        |         trips_today: {
        |             def.sqlExpr: "trips_today + 1" //if_else(true, trips_today, 1)"
        |             type: {
        |                type: TENSOR
        |                tensorCategory: DENSE
        |                dimensionType: []
        |                valType: INT
        |             }
        |         }
        |
        |         f_modified_streaming_count: {
        |            def.sqlExpr: "trips_today + 1"
        |            type: {
        |                type: TENSOR
        |                tensorCategory: DENSE
        |                dimensionType: []
        |                valType: INT
        |            }
        |         }
        |      }
        |  }
        |  kafkaAnchor2: {
        |       source: kafkaStreamingSource
        |       key.sqlExpr: [driver_id]
        |       features: {
        |          trips_today2: {
        |             def.sqlExpr: "trips_today + 1"
        |             type: {
        |                type: TENSOR
        |                tensorCategory: DENSE
        |                dimensionType: []
        |                valType: INT
        |              }
        |            }
        |          f_modified_streaming_count2: {
        |             def.sqlExpr: "trips_today + 1"
        |             type: {
        |                type: TENSOR
        |                tensorCategory: DENSE
        |                dimensionType: []
        |                valType: INT
        |              }
        |           }
        |        }
        |    }
        |}
        |
        |sources: {
        |  kafkaStreamingSource: {
        |    type: KAFKA
        |    config: {
        |       brokers: ["feathrazureci.servicebus.windows.net:9093"]
        |       topics: [feathrcieventhub]
        |       schema: {
        |          type = "avro"
        |          avroJson:"\n    {\n        \"type\": \"record\",\n        \"name\": \"DriverTrips\",\n        \"fields\": [\n            {\"name\": \"driver_id\", \"type\": \"long\"},\n            {\"name\": \"trips_today\", \"type\": \"int\"},\n            {\n                \"name\": \"datetime\",\n                \"type\": {\"type\": \"long\", \"logicalType\": \"timestamp-micros\"}\n            }\n        ]\n    }\n    "
        |       }
        |     }
        |   }
        |}
        |
        |""".stripMargin
    localFeatureGenerate(applicationConfig, featureDefConfig)
  }

  /**
   * Test with non SWA feature with preprocessing and an application config writing to an output directory.
   */
  @Test(enabled = false)
  def testFeatureGenWithApplicationConfigPreprocessing(): Unit = {
    val applicationConfig =
      s"""
         | operational: {
         |  name: generateWithDefaultParams
         |  endTime: 2021-01-02
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[
         |  {
         |      name: REDIS
         |      params: {
         |        table_name: "nycFeatures"
         |      }
         |   }
         |  ]
         |}
         |features: [f_trip_distance, f_location_max_fare]
      """.stripMargin
    val featureDefConfig =
      """
        |anchors: {
        |  nonAggFeatures: {
        |    source: nycTaxiBatchSource
        |    key: DOLocationID
        |    features: {
        |
        |      f_trip_distance: "(float)new_fare_amount"
        |
        |      f_is_long_trip_distance: "trip_distance>30"

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
        |     definition: "f_trip_distance * f_trip_time_duration"
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
        |""".stripMargin

    val df1 = new CsvDataLoader(ss, "src/test/resources/mockdata//driver_data/green_tripdata_2021-01.csv")
      .loadDataFrame()
      .withColumn("new_lpep_dropoff_datetime", col("lpep_dropoff_datetime"))
      .withColumn("new_fare_amount", col("fare_amount") + 1000000)
    val df2 = new CsvDataLoader(ss, "src/test/resources/mockdata//driver_data/green_tripdata_2021-01.csv")
      .loadDataFrame()
      .withColumn("new_improvement_surcharge", col("improvement_surcharge") + 1000000)
      .withColumn("new_tip_amount", col("tip_amount") + 1000000)
      .withColumn("new_lpep_pickup_datetime", col("lpep_pickup_datetime"))
    println("df1:")
    df1.show(10)
    println("df2:")
    df2.show(10)
    PreprocessedDataFrameManager.preprocessedDfMap = Map("f_is_long_trip_distance,f_trip_distance" -> df1, "f_location_avg_fare,f_location_max_fare" -> df2)

    val res = localFeatureGenerate(applicationConfig, featureDefConfig)
    res.head._2.data.show(100)
    PreprocessedDataFrameManager.preprocessedDfMap = Map()
  }


  /**
   * test sliding window aggregation feature using key extractor in multiple anchors with different extractors
   */
  @Test
  def testSWAFeatureGenMultiAnchorKeyExtractorWithDifferentExtractors(): Unit = {
    val applicationConfig = generateSimpleApplicationConfig(features = "f3, f5", forceJoin = true)
    val dfs = localFeatureGenerate(applicationConfig, sampleSwaFeatureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    // we should have 8 dataframes, each one contains a group of feature above
    assertEquals(dfCount, 1)
    // group by dataframe
    val featureList =
      dfs.head._2.data.collect().sortBy(row => (row.getAs[String]("key0"), row.getAs[String]("key1")))
    assertEquals(featureList.size, 4)
    assertEquals(featureList(0).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(0).getAs[Float]("f5"), 1f, 1e-5)
    assertEquals(featureList(1).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(1).getAs[Float]("f5"), 1f, 1e-5)
  }

  /**
   * test sliding window aggregation feature using key extractor
   */
  @Test
  def testSWAFeatureGenWithKeyExtractor(): Unit = {
    val applicationConfig = generateSimpleApplicationConfig(features = "f3, f4")
    val dfs = localFeatureGenerate(applicationConfig, sampleSwaFeatureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 1)
    val featureList = dfs.head._2.data
      .collect()
      .sortBy(row => (row.getAs[String]("key0"), row.getAs[String](("key1"))))
    assertEquals(featureList.size, 4)
    assertEquals(featureList(0).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(0).getAs[Float]("f4"), 1f, 1e-5)
    assertEquals(featureList(1).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(1).getAs[Float]("f4"), 1f, 1e-5)
    assertEquals(featureList(2).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(2).getAs[Float]("f4"), 1f, 1e-5)
    assertEquals(featureList(3).getAs[Float]("f3"), 1f, 1e-5)
    assertEquals(featureList(3).getAs[Float]("f4"), 1f, 1e-5)
  }

  @Test(
    description = "Test generating features in FDS format, the test verifies the " +
      "feature columns are ordered")
  def testSWAFeatureGenWithKeyInFDS(): Unit = {
    val featureDefConfig4 = s"""
       |sources: {
       |  swaSource: {
       |    location: { path: "generation/daily/" }
       |    timePartitionPattern: "yyyy/MM/dd"
       |    timeWindowParameters: {
       |      timestampColumn: "timestamp"
       |      timestampColumnFormat: "yyyy-MM-dd"
       |    }
       |  }
       |}
       |anchors: {
       |  swaAnchorWithKeyExtractor: {
       |    source: "swaSource"
       |    key: x
       |    features: {
       |      f3: {
       |        def: count
       |        aggregation: SUM
       |        filter: "Id <= 10"
       |        window: 3d
       |        default: 0.0
       |        type: {
       |          type: TENSOR
       |          tensorCategory: DENSE
       |          valType: FLOAT
       |       }
       |      }
       |      f4: {
       |        def: count
       |        aggregation: SUM
       |        groupBy: Id
       |        filter: "Id <= 10"
       |        window: 3d
       |      }
       |    }
       |  }
       |}
    """.stripMargin
    val applicationConfig4 =
      generateSimpleApplicationConfig(features = "f4, f3", endTime = "2019-05-21")
    val dfs = localFeatureGenerate(applicationConfig4, featureDefConfig4)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 1)
    val expJoinedObsAndFeatures = {
      val expSchema = StructType(
        List(
          StructField("key0", IntegerType, nullable = false),
          StructField("f3", FloatType, nullable = true),
          StructField(
            "f4",
            StructType(List(
              StructField("indices0", ArrayType(StringType, containsNull = false), nullable = false),
              StructField("values", ArrayType(FloatType, containsNull = false), nullable = false))),
            nullable = true)))
      val expData = List(Row(2, 6f, Row(List("10"), List(6.0f))), Row(1, 2f, Row(List("10"), List(2.0f))))

      val rdd = ss.sparkContext.parallelize(expData)
      ss.createDataFrame(rdd, expSchema)
    }
    AssertFeatureUtils.assertDataFrameEquals(dfs.head._2.data, expJoinedObsAndFeatures)
  }

  /**
   * test sliding window aggregation feature with on load part of the dataset(1d out of 2d)
   */
  @Test
  def testSWAFeatureGenWithKey1d(): Unit = {
    val featureDefConfig = generateSimpleFeatureDefConfig(window = "1d")
    val applicationConfig = generateSimpleApplicationConfig(features = "f3", endTime = "2019-05-21")
    val dfs = localFeatureGenerate(applicationConfig, featureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 1)
    val featureList = dfs.head._2.data.collect().sortBy(row => (row.getAs[Int]("key0")))
    assertEquals(featureList.size, 2)
    assertEquals(featureList(0).getAs[Float]("f3"), 3f, 1e-5)
    assertEquals(featureList(1).getAs[Float]("f3"), 3f, 1e-5)
  }

  /**
   * test sliding window aggregation feature with a time delay
   */
  @Test
  def testSWAFeatureGenWithTimeDelay(): Unit = {
    val applicationConfig =
      generateSimpleApplicationConfig(features = "f3, f3Sum", endTime = "2019-05-22", extraParam = "timeDelay: 1d", extraOutputParam = "outputTimestamp: true")
    val featureDefConfig =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    timePartitionPattern: "yyyy/MM/dd"
        |    timeWindowParameters: {
        |      timestampColumn: "timestamp"
        |      timestampColumnFormat: "yyyy-MM-dd"
        |    }
        |  }
        |}
        |anchors: {
        |  swaAnchorWithKeyExtractor: {
        |    source: "swaSource"
        |    key: x
        |    features: {
        |      f3: {
        |        def: "count"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |      f3Sum: {
        |        def: "count"
        |        aggregation: AVG
        |        window: 3d
        |      }
        |    }
        |  }
        |}
    """.stripMargin
    val dfs = localFeatureGenerate(applicationConfig, featureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 1)
    val df = dfs.head._2.data
    val featureList = df
      .collect()
      .sortBy(row => (row.getAs[Int]("key0")))
    assertEquals(featureList.length, 2)
    // end at 2019/05/22 with 1 day deley, so it actually looks for data of 2019/05/19 and 2019/05/20
    assertEquals(featureList(0).getAs[Float]("f3"), 6.0f)
    assertEquals(featureList(0).getAs[Float]("f3Sum"), 6 / 4.0f) // 4 records in total, so 6/4 = 1.5
    assertEquals(featureList(1).getAs[Float]("f3"), 10.0f)
    assertEquals(featureList(1).getAs[Float]("f3Sum"), 10 / 3.0f) // 3 records in total so 10/3 = 3.333333
    // check auto generated timestamps
    val timestamps = df.collect().map(row => row.getAs[String](FeatureGenConstants.FEATHR_AUTO_GEN_TIMESTAMP_FIELD))
    assertEquals(timestamps(0), "20190521")
  }

  /**
   * test feature generation before 3/10/2019, when we switched from PST to PDT, expect a run time error
   */
  @Test(expectedExceptions = Array(classOf[RuntimeException]), expectedExceptionsMessageRegExp = ".*No available date partition.*")
  def testSWAFeatureGenWithSpecialTime(): Unit = {
    /*
   Time window ends on 2019-04-17 and size is 28 days.
   we end up using 2/15/2019 as the start date, which is before 03-10.
   We don't have data on that day, so we will expect an exception”
     */
    val featureDefConfig = generateSimpleFeatureDefConfig(window = "28d")

    val applicationConfig =
      generateSimpleApplicationConfig(features = "f3", endTime = "2019-03-17", extraParam = "timeDelay: 1d")
    val dfs = localFeatureGenerate(applicationConfig, featureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 1)
  }

  /**
   * test sliding window aggregation feature using key field
   */
  @Test
  def testSWAFeatureGenWithKey(): Unit = {
    val featureDefConfig = generateSimpleFeatureDefConfig(window = "3d", extraParam = "filter: \"Id <= 10\"")
    val applicationConfig = generateSimpleApplicationConfig(features = "f3", endTime = "2019-05-21")
    val dfs = localFeatureGenerate(applicationConfig, featureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 1)
    val featureList = dfs.head._2.data.collect().sortBy(row => (row.getAs[Int]("key0")))
    assertEquals(featureList.size, 2)
    val featureColumnName = "f3"
    assertEquals(featureList(0).getAs[Float](featureColumnName), 2f, 1e-5)
    assertEquals(featureList(1).getAs[Float](featureColumnName), 6f, 1e-5)
  }

  /**
   * test sliding window aggregation feature with different windows
   */
  @Test
  def testSWAFeatureGenWithKey1dAnd3d(): Unit = {
    val featureDefConfig =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    timePartitionPattern: "yyyy/MM/dd"
        |    timeWindowParameters: {
        |      timestampColumn: "timestamp"
        |      timestampColumnFormat: "yyyy-MM-dd"
        |    }
        |  }
        |}
        |anchors: {
        |  swaAnchorWithKeyExtractor: {
        |    source: "swaSource"
        |    key: x
        |    features: {
        |      f3: {
        |        def: "count"
        |        aggregation: SUM
        |        window: 1d
        |      }
        |      f4: {
        |        def: "count"
        |        aggregation: SUM
        |        window: 1d
        |      }
        |      f5: {
        |        def: "count"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |    }
        |  }
        |}
    """.stripMargin

    val applicationConfig = generateSimpleApplicationConfig(features = "f3, f4, f5", endTime = "2019-05-21")

    val dfs = localFeatureGenerate(applicationConfig, featureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 2)
    dfs.keySet.foreach(key => {
      val df = dfs(key).data
      val featureList = df.collect().sortBy(row => (row.getAs[Int]("key0")))
      if (key.getFeatureName == "f3") {
        assertEquals(featureList.size, 2)
        assertEquals(featureList(0).getAs[Float]("f3"), 3f, 1e-5)
        assertEquals(featureList(0).getAs[Float]("f4"), 3f, 1e-5)
        assertEquals(featureList(1).getAs[Float]("f3"), 3f, 1e-5)
        assertEquals(featureList(1).getAs[Float]("f4"), 3f, 1e-5)
      } else if (key.getFeatureName == "f5") {
        assertEquals(featureList.size, 2)
        assertEquals(featureList(0).getAs[Float]("f5"), 6f, 1e-5)
        assertEquals(featureList(1).getAs[Float]("f5"), 10f, 1e-5)
      }
    })
  }

  /**
   * test sliding window aggregation feature with malformed source and the skip missing feature flag turned on.
   * The feature with the malformed source should be skipped.
   */
  @Test
  def testSWAFeatureWithMalformedSource(): Unit = {
    setFeathrJobParam(SKIP_MISSING_FEATURE, "true")
    val swaApplicationConfig = generateSimpleApplicationConfig(features = "f3, f4, f5, f6", endTime = "2019-05-21")
    val swaFeatureDefConfig =
      s"""
         |sources: {
         |  ${defaultSwaSource}
         |  ${malformedSource}
         |}
         |anchors: {
         |  swaAnchor1: {
         |    source: "swaSource11"
         |    key: x
         |    features: {
         |      f3: {
         |        def: "count"
         |        aggregation: SUM
         |        window: 1d
         |      }
         |    }
         |  }
         |
         |  swaAnchor2: {
         |    source: "swaSource"
         |    key: x
         |    features: {
         |      f4: {
         |        def: "count"
         |        aggregation: SUM
         |        window: 1d
         |      }
         |      f5: {
         |        def: "count"
         |        aggregation: SUM
         |        window: 3d
         |      }
         |
         |      f6: {
         |        def: "count"
         |        aggregation: LATEST
         |        window: 3d
         |      }
         |    }
         |  }
         |}
    """.stripMargin
    val dfs = localFeatureGenerate(swaApplicationConfig, swaFeatureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 2)
    // Assert that the feature is skipped.
    assertTrue(!dfs.keySet.map(x => x.getFeatureName).contains("f3"))
    setFeathrJobParam(SKIP_MISSING_FEATURE, "false")
  }

  /**
   * test sliding window aggregation feature with different anchors
   */
  @Test
  def testSWAFeatureGenDifferentAnchor(): Unit = {
    val swaApplicationConfig = generateSimpleApplicationConfig(features = "f3, f4, f5, f6", endTime = "2019-05-21")
    val swaFeatureDefConfig =
      s"""
         |sources: {
         |  ${defaultSwaSource}
         |}
         |anchors: {
         |  swaAnchor1: {
         |    source: "swaSource"
         |    key: x
         |    features: {
         |      f3: {
         |        def: "count"
         |        aggregation: SUM
         |        window: 1d
         |      }
         |    }
         |  }
         |
         |  swaAnchor2: {
         |    source: "swaSource"
         |    key: x
         |    features: {
         |      f4: {
         |        def: "count"
         |        aggregation: SUM
         |        window: 1d
         |      }
         |      f5: {
         |        def: "count"
         |        aggregation: SUM
         |        window: 3d
         |      }
         |
         |      f6: {
         |        def: "count"
         |        aggregation: LATEST
         |        window: 3d
         |      }
         |    }
         |  }
         |}
    """.stripMargin
    val dfs = localFeatureGenerate(swaApplicationConfig, swaFeatureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 2)
    dfs.keySet.foreach(key => {
      val df = dfs(key).data
      val featureList = df.collect().sortBy(row => (row.getAs[Int]("key0")))
      if (key.getFeatureName == "f3") {
        assertEquals(featureList.size, 2)
        assertEquals(featureList(0).getAs[Float]("f3"), 3f, 1e-5)
        assertEquals(featureList(0).getAs[Float]("f4"), 3f, 1e-5)
        assertEquals(featureList(1).getAs[Float]("f3"), 3f, 1e-5)
        assertEquals(featureList(1).getAs[Float]("f4"), 3f, 1e-5)
      } else if (key.getFeatureName == "f5") {
        // f5 and f6 are in the same dataframe
        assertEquals(featureList.size, 2)
        assertEquals(featureList(0).getAs[Float]("f5"), 6f, 1e-5)
        assertEquals(featureList(1).getAs[Float]("f5"), 10f, 1e-5)
        assertEquals(featureList(0).getAs[Float]("f6"), 2f, 1e-5)
        assertEquals(featureList(1).getAs[Float]("f6"), 3f, 1e-5)
      }
    })
  }

  /**
   * test sliding window aggregation feature with group by
   */
  @Test
  def testSWAFeatureGenWithGroupBy(): Unit = {
    val groupBy = """CASE WHEN Id2 IS NOT NULL THEN Id2 ELSE \"null\" END"""
    val swaFeatureDefConfig =
      s"""
         |sources: {
         |  ${defaultSwaSource}
         |}
         |anchors: {
         |  swaAnchorWithKeyExtractor: {
         |    source: "swaSource"
         |    key: x
         |    features: {
         |      f3: {
         |        def: "count"
         |        aggregation: SUM
         |        window: 3d
         |        groupBy: "Id"
         |      }
         |
         |      f4: {
         |        def: "1"
         |        aggregation: COUNT
         |        window: 3d
         |        filter: "Id2 is not null"
         |        groupBy: "${groupBy}"
         |      }
         |    }
         |  }
         |}
    """.stripMargin
    val swaApplicationConfig = generateSimpleApplicationConfig(features = "f3, f4", endTime = "2019-05-21")

    val dfs = localFeatureGenerate(swaApplicationConfig, swaFeatureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 2) // Expect 2 dataframes because one feature has filter while the other does not.

    val firstFeatureList = dfs
      .filter(_._1.getFeatureName == "f3")
      .map(_._2.data)
      .head
      .collect()
      .sortBy(row => row.getAs[Int]("key0"))
    assertRowTensorEquals(firstFeatureList(0).getAs[Row]("f3"), List("10", "11"), List(2.0, 4.0))
    assertRowTensorEquals(firstFeatureList(1).getAs[Row]("f3"), List("10", "11"), List(6.0, 4.0))
    val secondFeatureList = dfs
      .filter(_._1.getFeatureName == "f4")
      .map(_._2.data)
      .head
      .collect()
      .sortBy(row => row.getAs[Int]("key0"))
    assertRowTensorEquals(secondFeatureList(0).getAs[Row]("f4"), List("10", "11", "null"), List(2.0, 1.0, 0.0))
    assertRowTensorEquals(secondFeatureList(1).getAs[Row]("f4"), List("10", "11"), List(2.0, 1.0))
  }

  /**
   * Test with SWA feature having lateral view parameter.
   */
  @Test
  def testSWAFeatureGenWithLateralView(): Unit = {
    val swaFeatureDefConfig =
      """
        | sources: {
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
        |    keyAlias: "x"
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
        |}
    """.stripMargin
    val swaApplicationConfig = generateSimpleApplicationConfig(features = "f1", endTime = "2018-05-2")
    val dfs = localFeatureGenerate(swaApplicationConfig, swaFeatureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 1)

    val df = dfs.head._2.data

    val featureList = df.collect().sortBy(row => Option(row.getAs[String]("key0")).getOrElse("null"))
    val row0 = featureList(0)
    assertRowTensorEquals(row0.getAs[Row]("f1"), List("f1t1", "f1t2"), List(2.0, 3.0))
    val row1 = featureList(1)
    assertRowTensorEquals(row1.getAs[Row]("f1"), List("f1t1", "f1t2"), List(5.0, 6.0))
  }

  /**
   * The test verifies that in AFG, when a filter is specified, only rows that pass the filter
   * appear in the output.
   */
  @Test
  def testAFGOutputHasOnlyFilteredRows(): Unit = {
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    timePartitionPattern: "yyyy/MM/dd"
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
        |        def: count   // the column that contains the raw view count
        |        filter: "Id in (10, 11)"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |      g: {
        |        def: count   // the column that contains the raw view count
        |        filter: "Id in (9)"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val features = Seq("f")
    val keyField = "key0"
    val featureGenConfigStr =
      s"""
         |operational: {
         |  name: generateWithDefaultParams
         |  endTime: "2019-05-23"
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[ ]
         |}
         |features: [${features.mkString(",")}]
  """.stripMargin

    /**
     * Expected output:
     * TaggedFeatureName=(x):f
     * +--------+---+
     * |key0|  f|
     * +--------+---+
     * |       1|  6|
     * |       2| 10|
     * +--------+---+
     */
    val expectedSchema = StructType(
      Seq(StructField(keyField, LongType), StructField(features.head, LongType) // f
      ))

    val expectedRows = Array(new GenericRowWithSchema(Array(1, 6), expectedSchema), new GenericRowWithSchema(Array(2, 10), expectedSchema))

    val dfs = localFeatureGenerate(featureGenConfigStr, featureDefAsString)
    dfs.foreach(x => {
      x._2.data.show()
    })

    // Verify that requested features appear in the output
    assertEquals(dfs.size, features.size)

    // Verify number of rows and aggregated data
    val actualRows = dfs.head._2.data.collect().sortBy(row => row.getAs[Int](keyField))
    validateRows(actualRows, expectedRows)
  }

  /**
   * The test verifies that AFG works with hourly data.
   */
  @Test
  def testAFGOutputWithHourlyData(): Unit = {
    val featureDefAsString =
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
        |      g: {
        |        def: count   // the column that contains the raw view count
        |        aggregation: SUM
        |        window: 23h
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val features = Seq("f")
    val keyField = "key0"
    val featureGenConfigStr =
      s"""
         |operational: {
         |  name: generateWithDefaultParams
         |  endTime: "2019-05-20-01"
         |  endTimeFormat: "yyyy-MM-dd-hh"
         |  resolution: HOURLY
         |  output:[ ]
         |}
         |features: [${features.mkString(",")}]
  """.stripMargin

    /**
     * Expected output:
     * TaggedFeatureName=(x):f
     * +--------+---+
     * |key0|  f|
     * +--------+---+
     * |       1| 15|
     * |       2| 35|
     * +--------+---+
     */
    val expectedSchema = StructType(
      Seq(StructField(keyField, LongType), StructField(features.head, LongType) // f
      ))

    val expectedRows = Array(new GenericRowWithSchema(Array(1, 15.0), expectedSchema), new GenericRowWithSchema(Array(2, 35.0), expectedSchema))

    val dfs = localFeatureGenerate(featureGenConfigStr, featureDefAsString)
    dfs.foreach(x => {
      x._2.data.show()
    })

    // Verify that requested features appear in the output
    assertEquals(dfs.size, features.size)

    // Verify number of rows and aggregated data
    val actualRows = dfs.head._2.data.collect().sortBy(row => row.getAs[Int](keyField))
    validateRows(actualRows, expectedRows)
  }

  /**
   * The test verifies that in AFG, when same filter is specified for features in same anchor and with same time window,
   * the features are read into single dataframe.
   * Verifies that [[com.linkedin.feathr.offline.job.FeatureTransformation.FeatureGroupingCriteria]]
   * groups features based on filters as well.
   *
   * Test also verifies that only filtered rows appear in the output.
   */
  @Test
  def testSingleDFWhenSameFilters(): Unit = {
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    timePartitionPattern: "yyyy/MM/dd"
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
        |        def: count   // the column that contains the raw view count
        |        filter: "Id in (9)"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |      g: {
        |        def: count   // the column that contains the raw view count
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
    val featureGenConfigStr =
      s"""
         |operational: {
         |  name: generateWithDefaultParams
         |  endTime: "2019-05-23"
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[ ]
         |}
         |features: [${features.mkString(",")}]
  """.stripMargin

    /**
     * Expected output:
     * TaggedFeatureName=(x):f
     * +--------+---+---+
     * |x|  f|  g|
     * +--------+---+---+
     * |       3|  5|  5|
     * +--------+---+---+
     *
     * TaggedFeatureName=(x):g
     * +--------+---+---+
     * |x|  f|  g|
     * +--------+---+---+
     * |       3|  5|  5|
     * +--------+---+---+
     */
    val expectedSchema = StructType(
      Seq(
        StructField(keyField, LongType),
        StructField(features.head, LongType), // f
        StructField(features.last, LongType) // g
      ))

    val expectedRows = Array(new GenericRowWithSchema(Array(3, 5, 5), expectedSchema))

    val dfs = localFeatureGenerate(featureGenConfigStr, featureDefAsString)
    dfs.foreach(x => {
      x._2.data.show()
    })

    // Verify that requested features appear in the output
    assertEquals(dfs.size, features.size)

    // Verify that there is exactly one Dataframe
    assertEquals(dfs.groupBy(_._2.data).size, 1)

    val actualRows = dfs.head._2.data.collect().sortBy(row => row.getAs[Float](keyField))
    validateRows(actualRows, expectedRows)
  }

  /**
   * The test verifies that in AFG, when NO filters are specified for features in same anchor and with same time window,
   * the features are read into single dataframe.
   * Verifies that [[com.linkedin.feathr.offline.job.FeatureTransformation.FeatureGroupingCriteria]]
   * groups features even when filters are not specified.
   *
   */
  @Test
  def testSingleDFWhenNoFilters(): Unit = {
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    timePartitionPattern: "yyyy/MM/dd"
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
        |        def: count   // the column that contains the raw view count
        |        aggregation: SUM
        |        window: 3d
        |      }
        |      g: {
        |        def: count   // the column that contains the raw view count
        |        aggregation: SUM
        |        window: 3d
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val features = Seq("f", "g")
    val keyField = "key0"
    val featureGenConfigStr =
      s"""
         |operational: {
         |  name: generateWithDefaultParams
         |  endTime: "2019-05-23"
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[ ]
         |}
         |features: [${features.mkString(",")}]
  """.stripMargin

    /**
     * Expected output:
     * TaggedFeatureName=(x):f
     * +--------+---+---+
     * |x|  f|  g|
     * +--------+---+---+
     * |       1|  6|  6|
     * |       2| 10| 10|
     * |       3|  5|  5|
     * +--------+---+---+
     *
     * TaggedFeatureName=(x):g
     * +--------+---+---+
     * |x|  f|  g|
     * +--------+---+---+
     * |       1|  6|  6|
     * |       2| 10| 10|
     * |       3|  5|  5|
     * +--------+---+---+
     */
    val expectedSchema = StructType(
      Seq(
        StructField(keyField, LongType),
        StructField(features.head, LongType), // f
        StructField(features.last, LongType) // g
      ))

    val expectedRows = Array(
      new GenericRowWithSchema(Array(1, 6, 6), expectedSchema),
      new GenericRowWithSchema(Array(2, 10, 10), expectedSchema),
      new GenericRowWithSchema(Array(3, 5, 5), expectedSchema))

    val dfs = localFeatureGenerate(featureGenConfigStr, featureDefAsString)
    dfs.foreach(x => {
      x._2.data.show()
    })

    // Verify that requested features appear in the output
    assertEquals(dfs.size, features.size)

    // Verify that there is exactly one Dataframe
    assertEquals(dfs.groupBy(_._2.data).size, 1)

    val actualRows = dfs.head._2.data.collect().sortBy(row => row.getAs[Int](keyField))
    // Data validation
    validateRows(actualRows, expectedRows)
  }

  /**
   * test for features with old AnchorExtractor[GenericRecord] based extractor
   * since the old extractor interface does not provide key field names
   * and key field does not exist in config, user must provide a keyAlias
   * otherwise, we expect an exception there
   */
  @Test(expectedExceptions = Array(classOf[RuntimeException]), expectedExceptionsMessageRegExp = ".*unsupported.*")
  def testUnsupportedGenericRecordExtractor(): Unit = {
    val featureDefAsString =
      """
        |anchors: {
        |  mockdata-mvel-generic-extractor: {
        |    source: "seqJoin/a.avro.json"
        |    extractor: "com.linkedin.feathr.offline.job.SampleGenericRecordMVELExtractor"
        |    features: [
        |       mockdata_a_b_row_generic_extractor
        |    ]
        |  }
        |}
        |""".stripMargin

    val features = Seq("mockdata_a_b_row_generic_extractor")
    val dfs = localFeatureGenerateForSeqOfFeatures(features, featureDefAsString)
    dfs.map(_._2.data.count())
  }

  /**
   * The test verifies that in AFG, when filters are specified for features in same anchor and with same time window,
   * the features are NOT read into single dataframe.
   * Verifies that [[com.linkedin.feathr.offline.job.FeatureTransformation.FeatureGroupingCriteria]]
   * groups features based on filters as well.
   *
   * Test also verifies that only filtered rows appear in the output.
   */
  @Test
  def testFiltersAppliedOnDifferentDF(): Unit = {
    val featureDefAsString =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    timePartitionPattern: "yyyy/MM/dd"
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
        |        def: count   // the column that contains the raw view count
        |        filter: "Id in (10, 11)"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |      g: {
        |        def: count   // the column that contains the raw view count
        |        filter: "Id in (9)"
        |        aggregation: SUM
        |        window: 3d
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val features = Seq("f", "g")
    val keyField = "key0"
    val featureGenConfigStr =
      s"""
         |operational: {
         |  name: generateWithDefaultParams
         |  endTime: "2019-05-23"
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[ ]
         |}
         |features: [${features.mkString(",")}]
  """.stripMargin

    /**
     * Expected output:
     * TaggedFeatureName=(x):f
     * +--------+---+
     * |x|  f|
     * +--------+---+
     * |       1|  6|
     * |       2| 10|
     * +--------+---+
     *
     * TaggedFeatureName=(x):g
     * +--------+---+
     * |x|  g|
     * +--------+---+
     * |       3|  5|
     * +--------+---+
     */
    def expectedSchemaHelper(featureName: String) =
      StructType(Seq(StructField(keyField, LongType), StructField(featureName, LongType)))

    val expectedRowsF = Array(
      new GenericRowWithSchema(Array(1, 6), expectedSchemaHelper(features.head)),
      new GenericRowWithSchema(Array(2, 10), expectedSchemaHelper(features.head)))
    val expectedRowsG = Array(new GenericRowWithSchema(Array(3, 5), expectedSchemaHelper(features.last)))

    val dfs = localFeatureGenerate(featureGenConfigStr, featureDefAsString)
    dfs.foreach(x => {
      x._2.data.show()
    })
    // Verify that requested features appear in the output
    assertEquals(dfs.size, features.size)

    // Verify number of rows and aggregated data
    val actualRowsF = dfs
      .filter(_._1.getFeatureName.equals(features.head))
      .map(_._2.data)
      .head
      .collect()
      .sortBy(row => row.getAs[Int](keyField))

    validateRows(actualRowsF, expectedRowsF)

    val actualRowsG = dfs
      .filter(_._1.getFeatureName.equals(features.last))
      .map(_._2.data)
      .head
      .collect()
      .sortBy(row => row.getAs[Int](keyField))

    validateRows(actualRowsG, expectedRowsG)
  }

  /**
   * generate a simple application config for common simple test usage
   *
   * @param features         requested feature names, separated by comma
   * @param endTime          end time of feature generation
   * @param resolution       resolution of feature generation
   * @param extraParam       extra parameters for feature generation
   * @param extraOutputParam extra parameters for output
   * @param forceJoin        force to join all dataframe into one
   */
  def generateSimpleApplicationConfig(
      features: String,
      endTime: String = "2018-05-02",
      resolution: String = "DAILY",
      extraParam: String = "",
      extraOutputParam: String = "",
      forceJoin: Boolean = false): String = {
    val featureParams = if (forceJoin) {
      s"""features: [${features}]"""
    } else ""
    s"""
       |operational: {
       |  name: generateWithDefaultParams
       |  endTime: ${endTime}
       |  endTimeFormat: "yyyy-MM-dd"
       |  resolution: ${resolution}
       |  ${extraParam}
       |  output:[ {
       |     name: HDFS
       |     params: {
       |      ${featureParams}
       |      ${extraOutputParam}
       |      path: ""
       |     }
       |   }
       |  ]
       |}
       |features: [${features}]
    """.stripMargin
  }

  /**
   * generate simple FeatureDef config with only single feature
   */
  private def generateSimpleFeatureDefConfig(
      window: String,
      path: String = "generation/daily/",
      key: String = "x",
      feature: String = "f3",
      featureDefinition: String = "count",
      aggregation: String = "SUM",
      extraParam: String = ""): String = {
    s"""
       |sources: {
       |  swaSource: {
       |    location: { path: "${path}" }
       |    timePartitionPattern: "yyyy/MM/dd"
       |    timeWindowParameters: {
       |      timestampColumn: "timestamp"
       |      timestampColumnFormat: "yyyy-MM-dd"
       |    }
       |  }
       |}
       |anchors: {
       |  swaAnchorWithKeyExtractor: {
       |    source: "swaSource"
       |    key: ${key}
       |    features: {
       |      ${feature}: {
       |        def: ${featureDefinition}
       |        aggregation: ${aggregation}
       |        ${extraParam}
       |        window: ${window}
       |      }
       |    }
       |  }
       |}
    """.stripMargin
  }

  /**
   * Test derived feature generation with anchored features that are computed on different DataFrames.
   * The anchored feature DataFrames should be joined before evaluating the derived feature.
   */
  @Test
  def testDerivedFeaturesOnMultipleDataFrames(): Unit = {
    val featureDefConf =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "generation/daily/" }
        |    timePartitionPattern: "yyyy/MM/dd"
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
        |        def: count   // the column that contains the raw view count
        |        filter: "Id in (10, 11)"
        |        aggregation: SUM
        |        window: 3d
        |        default: 0
        |      }
        |      g: {
        |        def: count   // the column that contains the raw view count
        |        filter: "Id in (9)"
        |        aggregation: SUM
        |        window: 3d
        |        default: 0
        |      }
        |    }
        |  }
        |}
        |derivations: {
        |  h: "toNumeric(f) + toNumeric(g)"
        |}
      """.stripMargin

    val features = Seq("f", "g", "h")
    val featureGenConfig =
      s"""
         |operational: {
         |  name: generateWithDefaultParams
         |  endTime: "2019-05-23"
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[ ]
         |}
         |features: [${features.mkString(",")}]
  """.stripMargin

    val output = localFeatureGenerate(featureGenConfig, featureDefConf)

    /**
     * Expected Output:
     * +--------+---+---+----------+
     * |x|  f|  g|         h|
     * +--------+---+---+----------+
     * |       2| 10|  0|[ -> 10.0]|
     * |       1|  6|  0| [ -> 6.0]|
     * |       3|  0|  5| [ -> 5.0]|
     * +--------+---+---+----------+
     */
    val groupedOutput = output.groupBy(_._2.data)
    assertEquals(groupedOutput.size, 1) // features should be generated on a single DF
    val basePath = AssertFeatureUtils.getTestResultBasePath(getClass.getCanonicalName.stripSuffix("$"))
    val df = groupedOutput.head._1

    val expectedDf = ss.createDataFrame(
      ss.sparkContext.parallelize(
        Seq(
          Row(
            // x
            2,
            // f
            10.0f,
            // g
            0.0f,
            // h
            Row(mutable.WrappedArray.make(Array("")), mutable.WrappedArray.make(Array(10.0f)))),
          Row(
            // x
            1,
            // f
            6.0f,
            // g
            0.0f,
            // h
            Row(mutable.WrappedArray.make(Array("")), mutable.WrappedArray.make(Array(6.0f)))),
          Row(
            // x
            3,
            // f
            0.0f,
            // g
            5.0f,
            // h
            Row(mutable.WrappedArray.make(Array("")), mutable.WrappedArray.make(Array(5.0f)))))),
      StructType(
        List(
          StructField("x", IntegerType, true),
          StructField("f", FloatType, true),
          StructField("g", FloatType, true),
          StructField(
            "h",
            StructType(List(StructField("indices0", ArrayType(StringType, false), false), StructField("values", ArrayType(FloatType, false), false)))))))
    def cmpFunc(row: Row): String = row.get(0).toString
    FeathrTestUtils.assertDataFrameApproximatelyEquals(df, expectedDf, cmpFunc)
  }

  /**
   * This test validates that derived features with multiple keys are supported
   * if and only if all dependent features have the same keys.
   */
  @Test
  def testDerivedFeatureGenWithMultipleKeysSupported(): Unit = {
    val featureDefConf =
      """
        |anchors: {
        |  local: {
        |   source: "anchorAndDerivations/derivations/featureGeneration/Data.avro.json"
        |   key: ["x", "y"]
        |   features: {
        |    a_z: "z"
        |   }
        |  }
        |}
        |
        |derivations: {
        |  a_derived_z: {
        |    key: ["x", "Id"]
        |    inputs: {
        |     arg1:  { key: ["x", "Id"], feature: a_z }
        |    }
        |    definition: arg1
        |  }
        |}
        |
    """.stripMargin
    val features = Seq("a_derived_z")
    val output = localFeatureGenerateForSeqOfFeatures(features, featureDefConf)

    /**
     * Expected Output:
     * +----------+--------+---------------------------+
     * |x|x|a_derived_z|
     * +----------+--------+---------------------------+
     * |         2|       1|         [b -> 1.0]|
     * |         5|       2|       [e -> 1.0]|
     * |         1|       1|       [a ->...|
     * |         4|       2|        [d -> 1.0]|
     * |         6|       3|             [f -> 1.0]|
     * |         3|       2|           [c -> 1.0]|
     * +----------+--------+---------------------------+
     */
    val groupedOutput = output.groupBy(_._2.data)
    assertEquals(groupedOutput.size, 1) // features should be generated on a single DF
    val expectedDf = ss.createDataFrame(
      ss.sparkContext.parallelize(
        Seq(
          Row("1", "1", Row(List("a"), List(1.0f))),
          Row("4", "2", Row(List("d"), List(1.0f))),
          Row("5", "2", Row(List("e"), List(1.0f))),
          Row("6", "3", Row(List("f"), List(1.0f))),
          Row("2", "1", Row(List("b"), List(1.0f))),
          Row("3", "2", Row(List("c"), List(1.0f))))),
      StructType(
        List(
          StructField("key0", StringType, true),
          StructField("key1", StringType, true),
          StructField(
            "a_derived_z",
            StructType(List(
              StructField("indices0", ArrayType(StringType, containsNull = false), nullable = false),
              StructField("values", ArrayType(FloatType, containsNull = false), nullable = false))),
            nullable = true))))
    def cmpFunc(row: Row): String = row.get(1).toString
    FeathrTestUtils.assertDataFrameApproximatelyEquals(groupedOutput.head._1, expectedDf, cmpFunc)
  }

  /**
   * This test validates that derived features with multiple keys are supported
   * if and only if all dependent features have the same keys.
   * To enable this test, set the value of FeatureUtils.SKIP_MISSING_FEATURE to True. From
   * Spark 3.1, SparkContext.updateConf() is not supported.
   */
  @Test
  def testDerivedFeatureGenWithSkipFeatureFlagOn(): Unit = {
    setFeathrJobParam(SKIP_MISSING_FEATURE, "true")
    val featureDefConf =
      """
        |anchors: {
        |  local: {
        |   source: "anchorAndDerivations/derivations/featureGeneation/Data.avro.json"
        |   key: ["x", "y"]
        |   features: {
        |    a_z: "z"
        |   }
        |  }
        |  local1: {
        |   source: "anchorAndDerivations/derivations/featureGeneration/Data.avro.json"
        |   key: ["x", "y"]
        |   features: {
        |    a_z1: "z"
        |   }
        |  }
        |}
        |
        |derivations: {
        |  a_derived_z: {
        |    key: ["x", "Id"]
        |    inputs: {
        |     arg1:  { key: ["x", "Id"], feature: a_z }
        |    }
        |    definition: arg1
        |  }
        |}
        |
    """.stripMargin
    val features = Seq("a_derived_z", "a_z", "a_z1")
    val output = localFeatureGenerateForSeqOfFeatures(features, featureDefConf)

    /**
     * Expected Output:
     * +----------+--------+---------------------------+
     * |x|x|a_derived_z|
     * +----------+--------+---------------------------+
     * |         2|       1|         [b -> 1.0]|
     * |         5|       2|       [e -> 1.0]|
     * |         1|       1|       [a ->...|
     * |         4|       2|        [d -> 1.0]|
     * |         6|       3|             [f -> 1.0]|
     * |         3|       2|           [c -> 1.0]|
     * +----------+--------+---------------------------+
     */
    val groupedOutput = output.groupBy(_._2.data)
    assertEquals(groupedOutput.size, 1) // features should be generated on a single DF
    assertEquals(groupedOutput.head._2.size, 1) // Only 1 feature was generated
    assertEquals(groupedOutput.head._2.keySet.head.getFeatureName, "a_z1") // Only 1 feature was generated
    setFeathrJobParam(SKIP_MISSING_FEATURE, "false")
  }

  /**
   * Test derived feature generation does not support cross join.
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*Feature Generation currently does not support cross join.*")
  def testDerivedFeatureGenWithCrossJoinNotSupported(): Unit = {
    val featureDefConf =
      """
        |anchors: {
        |  -local: {
        |   source: "anchorAndDerivations/derivations/featureGeneration/Names.avro.json"
        |   key: "x"
        |   features: {
        |    a_Name: Name
        |   }
        |  }
        |  a-value: {
        |    source: "anchorAndDerivations/derivations/featureGeneration/aData.avro.json"
        |    key: "Id"
        |    features: {
        |      a_ct: "isPresent(t) && isPresent(t.cc) ? t.cc : null"
        |      a_sample: "isPresent(t) && isPresent(t.cc) && isPresent(t.code) ? t.cc + ':' + t.code : null"
        |    }
        |  }
        |}
        |
        |derivations: {
        |  a_derived_ct: "a_ct"
        |  a_derived_ctr: {
        |    key: ["x", "Id"]
        |    inputs: [
        |       { key: "x", feature: a_ct }
        |       { key: "x", feature: a_Name }
        |    ]
        |    class: "com.linkedin.feathr.offline.derived.TestDerivationFunctionExtractor"
        |  }
        |}
        |
    """.stripMargin
    val features = Seq("a_Name", "a_ct", "a_derived_ctr", "a_derived_ct")
    val result = localFeatureGenerateForSeqOfFeatures(features, featureDefConf)
    result.groupBy(_._2.data).foreach(g => g._1.show())
  }

  /**
   * Test derived feature generation does not support sequential join features.
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*does not support.*Sequential Join.*")
  def testDerivedFeatureGenWithSequentialJoin(): Unit = {
    val featureDefConf =
      """
        |anchors: {
        |  -local: {
        |   source: "anchorAndDerivations/derivations/featureGeneration/Names.avro.json"
        |   key.sqlExpr: x
        |   features: {
        |    a_Name.def.sqlExpr: Name
        |   }
        |  }
        |  a-value-local: {
        |    source: "anchorAndDerivations/derivations/featureGeneration/Data.avro.json"
        |    key.sqlExpr: "x"
        |    features: {
        |      a_x : {
        |      def.sqlExpr: "case when x = 5 then null else x end"
        |      type: NUMERIC
        |      }
        |   }
        |   }
        |}
        |
        |derivations: {
        |
        |  a_seqJoin: {
        |    key: "x"
        |    join: {
        |      base: { key: x, feature: a_x, outputKey: x }
        |      expansion: { key: x, feature: a_Name }
        |    }
        |    aggregation:"UNION"
        |  }
        |}
    """.stripMargin
    val features = Seq("a_Name", "a_x", "a_seqJoin")
    val result = localFeatureGenerateForSeqOfFeatures(features, featureDefConf)
    result.groupBy(_._2.data).foreach(g => g._1.show())
  }

  /**
   * Test derived feature generation does not support cross join.
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*Feature generation does not support Passthrough features.*")
  def testFeatureGenDoesNotSupportPassthroughFeatures(): Unit = {
    val featureDefConf =
      """
        |sources: {
        |  ptSource: {
        |    type: "PASSTHROUGH"
        |  }
        |}
        |anchors: {
        |  a-value: {
        |    source: ptSource
        |    key: "x"
        |    features: {
        |      a_ct: "isPresent(t) && isPresent(t.cc) ? t.cc : null"
        |      a_sample: "isPresent(t) && isPresent(t.cc) && isPresent(t.code) ? t.cc + ':' + t.code : null"
        |    }
        |  }
        |}
        |
        |derivations: {
        |  a_derived_ct: "a_ct"
        |}
        |
    """.stripMargin
    val features = Seq("a_Name", "a_ct", "a_derived_ctr", "a_derived_ct")
    val result = localFeatureGenerateForSeqOfFeatures(features, featureDefConf)
    result.groupBy(_._2.data).foreach(g => g._1.show())
  }

  @Test(description = "Test derived feature generation in FDS format")
  def testDerivedFeatureWithFDSFormat(): Unit = {
    val featureDefConfig4 =
      s"""
         |sources: {
         |  swaSource: {
         |    location: { path: "generation/daily/" }
         |    timePartitionPattern: "yyyy/MM/dd"
         |    timeWindowParameters: {
         |      timestampColumn: "timestamp"
         |      timestampColumnFormat: "yyyy-MM-dd"
         |    }
         |  }
         |}
         |anchors: {
         |  swaAnchorWithKeyExtractor: {
         |    source: "swaSource"
         |    key: x
         |    features: {
         |      f3: {
         |        def: count
         |        aggregation: SUM
         |        filter: "Id <= 10"
         |        window: 3d
         |      }
         |      f4: {
         |        def: count
         |        aggregation: SUM
         |        groupBy: Id
         |        filter: "Id <= 10"
         |        window: 3d
         |      }
         |    }
         |  }
         |}
         |derivations: {
         |  h: "f3 * 2"
         |}
         """.stripMargin
    val applicationConfig4 =
      generateSimpleApplicationConfig(features = "f3, f4, h", endTime = "2019-05-21")
    val dfs = localFeatureGenerate(applicationConfig4, featureDefConfig4)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    assertEquals(dfCount, 1)
    val rows = dfs.head._2.data.collect().sortBy(_.get(0).toString)
    assertEquals(rows(0).getAs[Int]("key0"), 1)
    assertEquals(rows(0).getAs[Float]("f3"), 2.0f)
    assertRowTensorEquals(rows(0).getAs[Row]("f4"), List("10"), List(2.0f))
    assertRowTensorEquals(rows(0).getAs[Row]("h"), List(""), List(4.0f))
    assertEquals(rows(1).getAs[Int]("key0"), 2)
    assertEquals(rows(1).getAs[Float]("f3"), 6.0f)
    assertRowTensorEquals(rows(1).getAs[Row]("f4"), List("10"), List(6.0f))
    assertRowTensorEquals(rows(1).getAs[Row]("h"), List(""), List(12.0f))
  }

  /**
   * Test sliding window aggregation feature with time stamp column inferred from the source path.
   */
  @Test
  def testSWAFeatureWithInferredTimestampColumn(): Unit = {
    val applicationConfig = generateSimpleApplicationConfig(features = "f3")
    val featureDefConfig =
      """
        |sources: {
        |  swaSource: {
        |    location: { path: "slidingWindowAgg/localSWAAnchorTestFeatureData/daily" }
        |    timePartitionPattern: "yyyy/MM/dd"
        |  }
        |}
        |anchors: {
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
        |  }
        |}
    """.stripMargin
    val dfs = localFeatureGenerate(applicationConfig, featureDefConfig)
    // group by dataframe
    val dfCount = dfs.groupBy(_._2.data).size
    // we should have 8 dataframes, each one contains a group of feature above
    assertEquals(dfCount, 1)
    // group by dataframe
    val featureList =
      dfs.head._2.data.collect().sortBy(row => (row.getAs[String]("key0"), row.getAs[String]("key1")))
    assertEquals(featureList.size, 4)
    assertEquals(featureList(0).getAs[Float]("f3"), 1f, 1e-5)
  }

  /**
   * Naive validator method that validates the actual rows returned match the expectation.
   *
   * Following validations are done -
   * 1. actualRows passed to the method is not null
   * 2. Number of rows in actual result match the expected number.
   * 3. The data in each row is the same as the corresponding row in expected.
   *
   * @param actualRows
   * @param expectedRows
   */
  private def validateRows(actualRows: Array[Row], expectedRows: Array[GenericRowWithSchema]): Unit = {
    assertNotNull(actualRows)
    assertEquals(actualRows.length, expectedRows.length)

    for ((actual, expected) <- actualRows zip expectedRows) {
      assertEquals(actual, expected)
    }
  }
}
