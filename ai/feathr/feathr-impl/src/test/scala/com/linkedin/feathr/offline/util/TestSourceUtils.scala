package com.linkedin.feathr.offline.util

import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.client.InputData
import com.linkedin.feathr.offline.source.SourceFormatType
import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.Row
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * unit tests for [[SourceUtils]]
 */
class TestSourceUtils extends TestFeathr {

  @Test(description = "test loading dataframe with SourceUtils")
  def testSourceUtils() : Unit = {
    val conf: Configuration = new Configuration()
    val path = "anchor1-source.csv"
    val absolutePath = getClass.getClassLoader.getResource(path).getPath
    val inputData = InputData(absolutePath, SourceFormatType.FIXED_PATH)
    val df = SourceUtils.loadObservationAsDF(ss, conf, inputData, List())

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

  @Test(description = "test loading dataframe with SourceUtils by specifying delimiter")
  def testSourceUtilsWithCsvDelimiterOption() : Unit = {
    val conf: Configuration = new Configuration()
    val path = "anchor1-source.tsv"
    val sqlContext = ss.sqlContext
    sqlContext.setConf("spark.feathr.inputFormat.csvOptions.sep", "\t")
    val absolutePath = getClass.getClassLoader.getResource(path).getPath
    val inputData = InputData(absolutePath, SourceFormatType.FIXED_PATH)
    val df = SourceUtils.loadObservationAsDF(ss, conf, inputData, List())

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