package com.linkedin.feathr.offline.source.dataloader

import com.linkedin.feathr.common.exception.FeathrInputDataException
import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.config.location.SimplePath
import com.linkedin.feathr.offline.util.FeathrUtils.{MAX_DATA_LOAD_RETRY, setFeathrJobParam}
import org.apache.spark.sql.Row
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * unit tests for [[BatchDataLoader]]
 */
class TestBatchDataLoader extends TestFeathr {

  def escape(raw: String): String = {
    import scala.reflect.runtime.universe._
    Literal(Constant(raw)).toString
  }

  @Test(description = "test loading dataframe with BatchDataLoader")
  def testBatchDataLoader() : Unit = {
    val path = "anchor1-source.csv"
    val absolutePath = getClass.getClassLoader.getResource(path).getPath
    val batchDataLoader = new BatchDataLoader(ss, SimplePath(absolutePath), List())
    val df = batchDataLoader.loadDataFrame()
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

  /**
   * Test the batch loader retries before failing.
   */
  @Test(expectedExceptions = Array(classOf[FeathrInputDataException]),
    expectedExceptionsMessageRegExp = ".* after 3 retries and retry time of 1ms.*")
  def testRetry(): Unit = {
    setFeathrJobParam(MAX_DATA_LOAD_RETRY, "3")
    val path = "anchor11-source.csv"
    val batchDataLoader = new BatchDataLoader(ss, location = SimplePath(path), List())
    val df = batchDataLoader.loadDataFrame()
    df.show()
    setFeathrJobParam(MAX_DATA_LOAD_RETRY, "0")
  }

  @Test(description = "test loading dataframe with BatchDataLoader by specifying delimiter")
  def testBatchDataLoaderWithCsvDelimiterOption() : Unit = {
    val path = "anchor1-source.tsv"
    val absolutePath = getClass.getClassLoader.getResource(path).getPath
    val sqlContext = ss.sqlContext
    sqlContext.setConf("spark.feathr.inputFormat.csvOptions.sep", "\t")
    println(s"Postset config testBatchDataLoaderWithCsvDelimiterOption: ${ss.sqlContext.getConf("spark.feathr.inputFormat.csvOptions.sep")}")
    val batchDataLoader = new BatchDataLoader(ss, SimplePath(absolutePath), List())
    val df = batchDataLoader.loadDataFrame()
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