package com.linkedin.feathr.offline.source.dataloader.hdfs

import com.linkedin.feathr.offline.TestFeathr
import org.apache.spark.sql.Row
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * unit tests for [[FileFormat]]
 */
class TestFileFormat extends TestFeathr {

  @Test(description = "test loading dataframe with FileFormat")
  def testLoadDataFrame() : Unit = {
    val path = "anchor1-source.csv"
    val absolutePath = getClass.getClassLoader.getResource(path).getPath
    val df = FileFormat.loadDataFrame(ss, absolutePath, "CSV")

    val expectedRows = Array(
      Row("1", "apple", "10", "10", "0.1"),
      Row("2", "orange", "10", "3", "0.1"),
      Row("3", "banana", "10", "2", "0.9"),
      Row("4", "apple", "10", "1", "0.7"),
      Row("5", "apple", "11", "11", "1.0"),
      Row("7", "banana", "2", "10", "81.27"),
      Row("9", "banana", "4", "4", "0.4")
    )
    assertEquals(df.collect(), expectedRows)
  }

  @Test(description = "test loading dataframe with FileFormat by specifying delimiter")
  def testLoadDataFrameWithCsvDelimiterOption() : Unit = {
    val path = "anchor1-source.tsv"
    val absolutePath = getClass.getClassLoader.getResource(path).getPath
    val sqlContext = ss.sqlContext
    sqlContext.setConf("spark.feathr.inputFormat.csvOptions.sep", "\t")
    val df = FileFormat.loadDataFrame(ss, absolutePath, "CSV")

    val expectedRows = Array(
      Row("1", "apple", "10", "10", "0.1"),
      Row("2", "orange", "10", "3", "0.1"),
      Row("3", "banana", "10", "2", "0.9"),
      Row("4", "apple", "10", "1", "0.7"),
      Row("5", "apple", "11", "11", "1.0"),
      Row("7", "banana", "2", "10", "81.27"),
      Row("9", "banana", "4", "4", "0.4")
    )
    assertEquals(df.collect(), expectedRows)
    sqlContext.setConf("spark.feathr.inputFormat.csvOptions.sep", "")
  }

  @Test(description = "test loading dataframe with FileFormat by specifying delimiter for HDFS")
  def testLoadDataFrameWithCsvDelimiterOptionHDFS() : Unit = {
    val path = "anchor1-source.tsv"
    val absolutePath = getClass.getClassLoader.getResource(path).getPath
    val sqlContext = ss.sqlContext
    sqlContext.setConf("spark.feathr.inputFormat.csvOptions.sep", "\t")
    val df = FileFormat.loadHdfsDataFrame("CSV", Seq(absolutePath))

    val expectedRows = Array(
      Row("1", "apple", "10", "10", "0.1"),
      Row("2", "orange", "10", "3", "0.1"),
      Row("3", "banana", "10", "2", "0.9"),
      Row("4", "apple", "10", "1", "0.7"),
      Row("5", "apple", "11", "11", "1.0"),
      Row("7", "banana", "2", "10", "81.27"),
      Row("9", "banana", "4", "4", "0.4")
    )
    assertEquals(df.collect(), expectedRows)
    sqlContext.setConf("spark.feathr.inputFormat.csvOptions.sep", "")
  }

}