package com.linkedin.feathr.offline.source.pathutil

import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler

import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.hadoop.mapred.JobConf
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.{assertEquals, assertTrue}
import org.testng.annotations.Test

class TestPathChecker extends TestFeathr with MockitoSugar {


  @Test(description = "test creation of PathChecker")
  def testCreateDataSourcePathChecker(): Unit = {
    val localPathChecker = PathChecker(ss, List())
    assertTrue(localPathChecker.isInstanceOf[LocalPathChecker])
    val mockSparkSession = mock[SparkSession]
    val mockSparkContext = mock[SparkContext]
    when(mockSparkSession.sparkContext).thenReturn(mockSparkContext)
    when(mockSparkContext.isLocal).thenReturn(false)
    val defaultPathChecker = PathChecker(mockSparkSession, List())
    assertTrue(defaultPathChecker.isInstanceOf[HdfsPathChecker])
  }

  @Test(description = "test the APIs for HdfsPathChecker")
  def tesHdfsPathChecker() : Unit = {
    val hdfsPathChecker = new HdfsPathChecker()
    assertEquals(hdfsPathChecker.isMock("anyPath"), false)
    assertEquals(hdfsPathChecker.exists("src/test/resources/anchor1-source.csv"), true)
    assertEquals(hdfsPathChecker.exists("non_existing_path"), false)
  }


  @Test(description = "test exists method for LocalPathChecker")
  def testLocalPathCheckerExists() : Unit = {
    val localPathChecker = new LocalPathChecker(new Configuration(), List())
    assertEquals(localPathChecker.exists("src/test/resources/anchor1-source.csv"), true)
    assertEquals(localPathChecker.exists("anchor1-source.csv"), true)
    assertEquals(localPathChecker.exists("generation/daily/2019/05/19"), true)
    assertEquals(localPathChecker.exists("non-existing_path"), false)
  }

  @Test(description = "test isExternalDataSource method for LocalPathChecker")
  def testLocalPathCheckerIsExternalDataSource() : Unit = {
    val mockCreateDataFrame = (path: String, daliParameters: Map[String, String], jobConf: JobConf) => mock[DataFrame]

    val mockUnionDataFrame = (
      paths: Seq[String],
      parameters: Map[String, String],
      jobConf: JobConf) => mock[DataFrame]

    val mockWriteDataFrame = (
      df: DataFrame,
      path: String,
      parameters: Map[String, String]) => {}

    val mockDataLoaderHandler = DataLoaderHandler(
      validatePath = (path: String) => path == "xyz://",
      createDataFrame = mockCreateDataFrame,
      createUnionDataFrame = mockUnionDataFrame,
      writeDataFrame = mockWriteDataFrame,
    )
    val mockDataLoaderHandlers = List(mockDataLoaderHandler)
    val localPathChecker = new LocalPathChecker(new Configuration(), mockDataLoaderHandlers)
    assertTrue(localPathChecker.isExternalDataSource("xyz://"))
    assertTrue(!localPathChecker.isExternalDataSource("file://"))
  }
}
