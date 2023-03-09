package com.linkedin.feathr.offline.job

import com.linkedin.feathr.offline.TestFeathr.SPARK_DEFAULT_PARALLELISM
import com.linkedin.feathr.offline._
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Row, SparkSession}
import org.scalatest.testng.TestNGSuite
import org.testng.Assert._
import org.testng.annotations.{AfterClass, BeforeClass, Test}

import scala.collection.mutable

class TestFeatureJoinJob extends TestNGSuite{
  private val generatedDataFolder = "src/test/generated"

  var ss: SparkSession = _
  var conf: Configuration = _

  // generate mock data
  val mockDataFolder = generatedDataFolder + "/mockData"
  val trainingData = mockDataFolder + "/training_data"
  val featureData = mockDataFolder + "/a_feature_data"
  @BeforeClass
  def setup(): Unit = {

    // set up SparkSession
    val sparkConf = new SparkConf()
    sparkConf.registerKryoClasses(Array(classOf[GenericRecord]))
    sparkConf.set("spark.kryo.registrator", "org.apache.spark.serializer.AvroGenericArrayKryoRegistrator")
    sparkConf.set("spark.default.parallelism", SPARK_DEFAULT_PARALLELISM)
    sparkConf.set("spark.kryo.registrator", "org.apache.spark.serializer.AvroGenericArrayKryoRegistrator")
    sparkConf.set("spark.feathr.enable.column.filter", "true")
    sparkConf.set("spark.driver.host", "localhost")
    sparkConf.set("spark.kryoserializer.buffer.max", "512")
    // set numThreads = "*" to run Spark locally with as many worker threads as logical cores on the machine
    ss = TestFeathr.getOrCreateSparkSession
    conf = ss.sparkContext.hadoopConfiguration

    // define anchors/derivations
    val mockConfigFolder = generatedDataFolder + "/config"
    val feathrConfigPath = mockConfigFolder + "/feathr.conf"
    val feathrConf =
      """sources: {
        |  ptSource: {
        |    type: "PASSTHROUGH"
        |  }
        |}
        |
        anchors: {
        |  anchor1: {
        |    source: "%s"
        |    key: "IdInFeatureData"
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
        |      ff: {
        |       def: "c"
        |       default: [6,7]
        |      }
        |    }
        |  }
        |  anchor2: {
        |    source: "%s"
        |    extractor: "com.linkedin.feathr.offline.anchored.anchorExtractor.TestxFeatureDataExtractor"
        |    features: [ z ]
        |  }
        |}
        |
        |derivations: {
        |  multiply_a_b: "toNumeric(aa) * toNumeric(bb)"
        |
        |  categorical_b: {
        |    key: [foo]
        |    inputs: { foo_b: { key: foo, feature: bb } }
        |    definition: "toCategorical(foo_b)"
        |  }
        |}""".stripMargin.format(featureData, featureData)
    TestUtils.writeToFile(feathrConf, feathrConfigPath)

    // define feature join
    val joinConfigPath = mockConfigFolder + "/featureJoin_singleKey.conf"
    val featureJoinConf =
      """features: [
        |  {
        |    key: "IdInObservation"
        |    featureList: ["aa", "bb", "cc", "dd", "ee", "ff", "multiply_a_b", "categorical_b", "z"]
        |  }
        |]""".stripMargin
    TestUtils.writeToFile(featureJoinConf, joinConfigPath)

  }

  @AfterClass
  def cleanUp(): Unit = {
  }

  @Test(description = "test SWA with dense vector feature")
  def testLocalAnchorSWAWithDenseVector(): Unit = {
    val res = LocalFeatureJoinJob.joinWithHoconJoinConfig(
      """
        | settings: {
        |  joinTimeSettings: {
        |    timestampColumn: {
        |       def: "timestamp"
        |       format: "yyyy-MM-dd"
        |      }
        |  }
        |  conflictsAutoCorrectionSettings: {
        |    renameFeatures: "False"
        |    suffix: "suffixTest"
        |  }
        |}
        |
        |features: [
        |
        |  {
        |    key: [mId],
        |    featureList: ["aEmbedding"]
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
        |    }
        |  }
        |}
        """.stripMargin,
      observationDataPath = "simple-obs.csv",
      dataPathHandlers = List())

    val featureList = res.data.collect().sortBy(x => (x.getAs[String]("mId")))

    assertEquals(featureList.size, 2)
    assertEquals(featureList(0).getAs[Row]("aEmbedding"), mutable.WrappedArray.make(Array(1.5f, 1.8f)))
    assertEquals(featureList(1).getAs[Row]("aEmbedding"), mutable.WrappedArray.make(Array(7.5f, 7.7f)))
  }

@Test(description = "test conflicts auto correction on dataset")
def testConflictsAutoCorrectionDataset(): Unit = {
  val res = LocalFeatureJoinJob.joinWithHoconJoinConfig (
  """
    | settings: {
    |  joinTimeSettings: {
    |    timestampColumn: {
    |       def: "timestamp"
    |       format: "yyyy-MM-dd"
    |      }
    |  }
    |  conflictsAutoCorrectionSettings: {
    |    renameFeatures: "False"
    |    suffix: "suffixTest"
    |  }
    |}
    |
    |features: [
    |
    |  {
    |    key: [mId],
    |    featureList: ["aEmbedding", "bEmbedding"]
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
    |      bEmbedding: {
    |        def: "embedding"
    |        aggregation: LATEST
    |        window: 2d
    |      }
    |    }
    |  }
    |}
    """.stripMargin,
  observationDataPath = "simple-obs2.csv",
  dataPathHandlers = List () )

  val featureList = res.data.collect ().sortBy (x => (x.getAs[String] ("mId") ) )

  assertEquals (featureList.size, 2)
  assertEquals (featureList (0).getAs[Row] ("aEmbedding"), mutable.WrappedArray.make (Array (1.5f, 1.8f) ) )
  assertEquals (featureList (1).getAs[Row] ("aEmbedding"), mutable.WrappedArray.make (Array (7.5f, 7.7f) ) )

  assertEquals(res.data.columns(2), "aEmbedding_suffixTest")
  assertEquals(res.data.columns(3), "bEmbedding_suffixTest")
  assertTrue(Seq("aEmbedding", "bEmbedding").contains(res.data.columns(4)) )
  assertTrue(Seq("aEmbedding", "bEmbedding").contains(res.data.columns(5)))
  }

@Test(description = "test conflicts auto correction on features")
def testConflictsAutoCorrectionFeatures(): Unit = {
  val res = LocalFeatureJoinJob.joinWithHoconJoinConfig (
  """
    | settings: {
    |  joinTimeSettings: {
    |    timestampColumn: {
    |       def: "timestamp"
    |       format: "yyyy-MM-dd"
    |      }
    |  }
    |  conflictsAutoCorrectionSettings: {
    |    renameFeatures: "True"
    |    suffix: "10"
    |  }
    |}
    |
    |features: [
    |
    |  {
    |    key: [mId],
    |    featureList: ["aEmbedding", "bEmbedding"]
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
    |      bEmbedding: {
    |        def: "embedding"
    |        aggregation: LATEST
    |        window: 2d
    |      }
    |    }
    |  }
    |}
    """.stripMargin,
  observationDataPath = "simple-obs2.csv",
  dataPathHandlers = List () )

  val featureList = res.data.collect ().sortBy (x => (x.getAs[String] ("mId") ) )

  assertEquals (featureList.size, 2)
  assertEquals (featureList (0).getAs[Row] ("aEmbedding_10"), mutable.WrappedArray.make (Array (1.5f, 1.8f) ) )
  assertEquals (featureList (1).getAs[Row] ("aEmbedding_10"), mutable.WrappedArray.make (Array (7.5f, 7.7f) ) )

  assertEquals(res.data.columns(2), "aEmbedding")
  assertEquals(res.data.columns(3), "bEmbedding")
  assertTrue(Seq("aEmbedding_10", "bEmbedding_10").contains(res.data.columns(4)))
  assertTrue(Seq("aEmbedding_10", "bEmbedding_10").contains(res.data.columns(5)))
  }
}
