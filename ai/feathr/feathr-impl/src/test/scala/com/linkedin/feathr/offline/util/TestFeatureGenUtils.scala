package com.linkedin.feathr.offline.util

import java.time.LocalDateTime

import com.linkedin.feathr.offline.{AssertFeatureUtils, TestFeathr}
import com.linkedin.feathr.offline.job.FeatureTransformation
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.hdfs.HdfsConfiguration
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class TestFeatureGenUtils extends TestFeathr {
  private val currentDirectory = new java.io.File(".").getCanonicalPath
  private val generatedDataFolder = currentDirectory + "/src/test/generated/"
  private val mockDataFolder = generatedDataFolder + "/mockData/"

  @Test(description = "test incremental join unitility for feature generation")
  def testDataFrameMerge(): Unit = {
    val sparkSession = ss
    import sparkSession.implicits._
    val preAggDF = Seq((1, 1.0), (2, 5.0)).toDF("key", "f1")
    val oldDeltaDF = Seq((1, 1.0), (2, 2.0)).toDF("key", "f1")
    val newDeltaDF = Seq((1, 2.0), (2, 10.0)).toDF("key", "f1")
    val joinKeys = Seq("key")
    val features = Seq("f1")
    val minusOldDF = FeatureTransformation.mergeDeltaDF(preAggDF, oldDeltaDF, joinKeys, joinKeys, features, false)
    val withNewDeltaDF = FeatureTransformation.mergeDeltaDF(minusOldDF, newDeltaDF, joinKeys, joinKeys, features)
    val expectedDF = Seq((1, 2.0), (2, 13.0)).toDF("key", "f1")
    AssertFeatureUtils.assertRowsSortedEquals(withNewDeltaDF.collect().toList, expectedDF.collect().toList, "DataFrames contents aren't equal:")
  }

  @Test(description = "Get the latest date from the historical aggregation daily snapshot")
  def testGetLatestHistoricalAggregationDate(): Unit = {
    val testLatestPathName = mockDataFolder + "/test_multi_latest_path"
    val pathString1 = testLatestPathName + "/" + "2018/01/17/"
    val pathString2 = testLatestPathName + "/" + "2018/11/15/"
    val pathString3 = testLatestPathName + "/" + "2018/11/16/"
    val pathString4 = testLatestPathName + "/" + "2018/08/"

    val conf = new HdfsConfiguration()
    val superFs = FileSystem.get(conf)

    superFs.create(new Path(pathString1 + "test.avro"))
    superFs.create(new Path(pathString1 + "test1.avro"))
    superFs.create(new Path(pathString1 + "test2.avro"))
    superFs.create(new Path(pathString2 + "test.avro"))
    superFs.create(new Path(pathString3 + "test.avro"))
    superFs.create(new Path(pathString3 + "test1.avro"))
    superFs.create(new Path(pathString4))

    {
      val latestDate = IncrementalAggUtils.getLatestHistoricalAggregationDate(testLatestPathName, LocalDateTime.now())
      assertEquals(IncrementalAggUtils.formatDateAsString(latestDate.get), "20181116")
    }

    {
      val cutOffTime = LocalDateTime.of(2018, 11, 16, 0, 0)
      val latestDateWithCutoff = IncrementalAggUtils.getLatestHistoricalAggregationDate(testLatestPathName, cutOffTime)
      assertEquals(IncrementalAggUtils.formatDateAsString(latestDateWithCutoff.get), "20181115")
    }

    {
      val cutOffTime = LocalDateTime.of(2018, 5, 16, 0, 0)
      val latestDateWithCutoff = IncrementalAggUtils.getLatestHistoricalAggregationDate(testLatestPathName, cutOffTime)
      assertEquals(IncrementalAggUtils.formatDateAsString(latestDateWithCutoff.get), "20180117")
    }

    {
      val cutOffTime = LocalDateTime.of(2016, 5, 16, 0, 0)
      val latestDateWithCutoff = IncrementalAggUtils.getLatestHistoricalAggregationDate(testLatestPathName, cutOffTime)
      assertEquals(false, latestDateWithCutoff.isDefined)
    }

    {
      val cutOffTime = LocalDateTime.of(2999, 5, 16, 0, 0)
      val latestDateWithCutoff = IncrementalAggUtils.getLatestHistoricalAggregationDate(testLatestPathName, cutOffTime)
      assertEquals(IncrementalAggUtils.formatDateAsString(latestDateWithCutoff.get), "20181116")
    }
  }

  @Test(description = "Get the latest date from the historical aggregation daily snapshot")
  def testGetDaysGapBetweenLatestAggSnapshotAndEndTime(): Unit = {
    val testDaysGapPath = mockDataFolder + "/test_daysgap"
    val pathString1 = testDaysGapPath + "/" + "2019/09/29/"

    val conf = new HdfsConfiguration()
    val superFs = FileSystem.get(conf)

    superFs.create(new Path(pathString1 + "test.avro"))

    {
      val actual = IncrementalAggUtils.getDaysGapBetweenLatestAggSnapshotAndEndTime(testDaysGapPath, "2019/10/03", "yyyy/MM/dd")
      assertEquals(actual, Some(4))
    }

    {
      val actual = IncrementalAggUtils.getDaysGapBetweenLatestAggSnapshotAndEndTime(testDaysGapPath, "2019/10/01", "yyyy/MM/dd")
      assertEquals(actual, Some(2))
    }
  }
}
