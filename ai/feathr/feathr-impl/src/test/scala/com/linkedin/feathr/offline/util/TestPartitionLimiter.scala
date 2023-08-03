package com.linkedin.feathr.offline.util

import com.linkedin.feathr.offline.TestFeathr
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.assertEquals
import org.testng.annotations.{BeforeClass, DataProvider, Test}

/**
 * Unit test for PartitionLimiter
 */
class TestPartitionLimiter extends TestFeathr with MockitoSugar{
  @DataProvider
  def testCases: Array[Array[Any]] =
    // (originalPartition, maxPartition, minPartition, expectedPartition)
    Array(
      // minSparkJoinParallelismBeforeJoin is -1, do not repartition
      Array(5, 1, -1, 5),
      // original partition is within the range, do not repartition
      Array(5, 10, 4, 5),
      Array(5, 5, 5, 5),
      // when max and min are the same, it need repartition
      Array(5, 6, 6, 6),
      // original partition is larger than max, but not large enough for repartition
      Array(15, 10, 4, 15),
      Array(20, 10, 4, 20),
      // original partition is large enough for repartition
      Array(21, 10, 4, 10),
      // original partition is smaller than min, but not smaller enough for repartition
      Array(3, 10, 4, 3),
      Array(2, 10, 4, 2)
    )

  val mockSparkSession = mock[SparkSession]
  val mockSparkContext = mock[SparkContext]
  when(mockSparkSession.sparkContext).thenReturn(mockSparkContext)
  when(mockSparkContext.isLocal).thenReturn(false)
  val partitionLimiter = new PartitionLimiter(mockSparkSession)

  val mockLocalSparkSession = mock[SparkSession]
  val mockLocalSparkContext = mock[SparkContext]
  when(mockLocalSparkSession.sparkContext).thenReturn(mockLocalSparkContext)
  when(mockLocalSparkContext.isLocal).thenReturn(true)
  var localPartitionLimiter : PartitionLimiter = new PartitionLimiter(mockLocalSparkSession)

  var testDataFrame : DataFrame = _

  @BeforeClass
  override def setup(): Unit = {
    super.setup()
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    testDataFrame = Range(1, 2).map(k => (k, k)).toDF("id", "val")
  }

  @Test(description = "test limitPartition with DataFrame", dataProvider = "testCases")
  def testLimitPartitionWithDataFrame(originalPartition: Int, maxPartition: Int, minPartition: Int, expectedPartition: Int): Unit = {
    val df = testDataFrame.repartition(originalPartition)
    assertEquals(partitionLimiter.limitPartition(df, maxPartition, minPartition).rdd.getNumPartitions, expectedPartition)
  }

  @Test(description = "test limitPartition with RDD", dataProvider = "testCases")
  def testLimitPartitionWithRdd(originalPartition: Int, maxPartition: Int, minPartition: Int, expectedPartition: Int): Unit = {
    val df = testDataFrame.repartition(originalPartition)
    assertEquals(partitionLimiter.limitPartition(df.rdd, maxPartition, minPartition).getNumPartitions, expectedPartition)
  }

  @Test(description = "test limitPartition that needs to reduce the partitions. Notice DataFrame and RDD has different behaviors in this case.")
  def testReducePartition() : Unit = {
    val df = testDataFrame.repartition(1)
    assertEquals(partitionLimiter.limitPartition(df, 10, 4).rdd.getNumPartitions, 5000)
    assertEquals(partitionLimiter.limitPartition(df.rdd, 10, 4).getNumPartitions, 4)
  }

  @Test(description = "The partition won't be changed for local Spark session.", dataProvider = "testCases")
  def testLocalPartitionLimiterWithDataFrame(originalPartition: Int, maxPartition: Int, minPartition: Int, expectedPartition: Int) : Unit = {
    val df = testDataFrame.repartition(originalPartition)
    assertEquals(localPartitionLimiter.limitPartition(df, maxPartition, minPartition).rdd.getNumPartitions, originalPartition)
  }

  @Test(description = "The partition won't be changed for local Spark session.", dataProvider = "testCases")
  def testLocalPartitionLimiterWithRdd(originalPartition: Int, maxPartition: Int, minPartition: Int, expectedPartition: Int) : Unit = {
    val df = testDataFrame.repartition(originalPartition)
    assertEquals(localPartitionLimiter.limitPartition(df.rdd, maxPartition, minPartition).getNumPartitions, originalPartition)
  }
}
