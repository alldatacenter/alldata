package com.linkedin.feathr.offline.join.algorithms

import com.linkedin.feathr.common.exception.FeathrFeatureJoinException
import com.linkedin.feathr.offline.AssertFeatureUtils._
import com.linkedin.feathr.offline.join.util.{FrequentItemEstimatorFactory, FrequentItemEstimatorType, PreComputedFrequentItemEstimator, SparkFrequentItemEstimator}
import com.linkedin.feathr.offline.util.HdfsUtils
import com.linkedin.feathr.offline.{TestFeathr, TestUtils}
import org.apache.commons.math3.distribution.ZipfDistribution
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.{assertEquals, assertTrue}
import org.testng.annotations._

import scala.util.Try

/**
 * Unit tests for Salted Join
 */
class TestSparkSaltedJoin extends TestFeathr with MockitoSugar {
  @BeforeClass
  def setUp(): Unit = {}

  @AfterClass
  def cleanUp(): Unit = {}

  @Test(expectedExceptions = Array(classOf[FeathrFeatureJoinException]))
  def testSaltedJoinInvalidJoinType(): Unit = {
    val mockFrequentItemEstimator = mock[SparkFrequentItemEstimator]
    val saltedJoin = new SaltedSparkJoin(ss, mockFrequentItemEstimator)
    val left = mock[DataFrame]
    val right = mock[DataFrame]
    val outDf = saltedJoin.join(Seq("key"), left, Seq("key"), right, JoinType.full_outer)
    outDf.count()
  }

  @Test
  def testSaltedJoinCorrectness(): Unit = {
    val mockFrequentItemEstimator = mock[SparkFrequentItemEstimator]
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val left = Seq((1, "l1"), (2, "l2"), (2, "l3")).toDF("key", "label")
    val right = Seq((1, "1"), (3, "2")).toDF("key", "value")

    val mockFrequentItemsResult = Seq((2)).toDF("key")
    when(mockFrequentItemEstimator.estimateFrequentItems(left, "key", 0.0002f))
      .thenReturn(mockFrequentItemsResult)

    val saltedJoin = new SaltedSparkJoin(ss, mockFrequentItemEstimator)
    val outDf = saltedJoin.join(Seq("key"), left, Seq("key"), right, JoinType.left_outer)
    val expectedDf = Seq((1, "l1", "1"), (2, "l2", null), (2, "l3", null)).toDF("key", "label", "value")
    assertDataFrameEquals(outDf, expectedDf)

    // test salted join with cached frequent items
    val saltedJoinCached = new SaltedSparkJoin(ss, new PreComputedFrequentItemEstimator(mockFrequentItemsResult))
    val outDfCached = saltedJoin.join(Seq("key"), left, Seq("key"), right, JoinType.left_outer)
    assertDataFrameEquals(outDfCached, expectedDf)

  }

  @Test
  def testEstimators(): Unit = {
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val rows = Seq.tabulate(100) { i =>
      if (i % 2 == 0) (1, -1.0) else (i, i * -1.0)
    }
    val input = ss.createDataFrame(rows).toDF("a", "b")
    val expectedDf = Seq(1).toDF("a")

    import FrequentItemEstimatorType._

    for (estimatorType <- Array(spark, countMinSketch, groupAndCount)) {
      val estimator = FrequentItemEstimatorFactory.create(estimatorType)
      assertEquals(estimator.estimateFrequentItems(input, "a", 0.4f).filter("a==1").count(), 1)
    }

    assertTrue(Try { FrequentItemEstimatorFactory.create(preComputed) }.isFailure)
    assertTrue(Try { FrequentItemEstimatorFactory.create(FrequentItemEstimatorType.withName("wrongType")) }.isFailure)

    val preComputedEstimator = FrequentItemEstimatorFactory.createFromCache(expectedDf)
    assertEquals(preComputedEstimator.estimateFrequentItems(input, "a", 0.4f).filter("a==1").count(), 1)
  }

  @Test(enabled = false)
  def testJoinPerformance(): Unit = {
    val sqlContext = ss.sqlContext
    sqlContext.sql("SET spark.sql.autoBroadcastJoinThreshold = -1")
    // approximate zipf distribution to mimic skewed data
    val cardinality = 200
    val zipfGenerator = new ZipfDistribution(cardinality, 2.07)
    val zipfUDF = udf(() => {
      zipfGenerator.sample().toLong
    })

    val N = cardinality

    val left = sqlContext
      .range(1, N)
      .withColumn("key", zipfUDF())
      // .withColumn("heavyContextPayload", lit("b"*100)) // 0.1kb each record
      .repartition(expr("key"))
      .drop("id")
    val right = sqlContext
      .range(1, N)
      .withColumn("key", (rand(seed = 5) * cardinality).cast("long"))
      .repartition(expr("key"))
      .dropDuplicates("key")
    //  .withColumn("heavyValuePayload", lit("a"*100)) // 0.1kb each record

    val outputDf = TestUtils.collectTime {
      val df = new SaltedSparkJoin(ss, new SparkFrequentItemEstimator).join(Seq("key"), left, Seq("key"), right, JoinType.left_outer)
      HdfsUtils.deletePath("saltedJoinOut", true)
      df.write.format("com.databricks.spark.csv").save("saltedJoinOut")
      df
    }
    val expectOutputDf = TestUtils.collectTime {
      val df = left.join(right, Seq("key"), "left_outer")
      HdfsUtils.deletePath("normalJoinOut", true)
      df.write.format("com.databricks.spark.csv").save("normalJoinOut")
      df
    }
    // Add this so we can keep the Spark UI running to assist debugging
    Thread.sleep(100000000)
  }
}
