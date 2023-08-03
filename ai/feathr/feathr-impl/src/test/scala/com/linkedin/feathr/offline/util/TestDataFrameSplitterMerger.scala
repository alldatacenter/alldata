package com.linkedin.feathr.offline.util

import com.linkedin.feathr.offline.TestFeathr
import org.apache.spark.sql.functions.{col, concat, expr, lit}
import org.testng.Assert.{assertEquals, assertNotNull, assertNull, assertTrue}
import org.testng.annotations.Test
import com.linkedin.feathr.offline.util.DataFrameSplitterMerger._
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import com.linkedin.feathr.offline.util.TestSplitMergeDataProvider._
import org.scalatest.testng.TestNGSuite

class TestDataFrameSplitterMerger extends TestNGSuite {

  /**
   * Test partitioning when NO nulls exist in column.
   */
  @Test
  def testSplitOnNullDataWhenNoNulls(): Unit = {
    val df = getDataWithNoNulls
    val (dfWithNoNull, dfWithNull) = splitOnNull(df, Version)
    assertEquals(dfWithNoNull.count, df.count)
    assertEquals(dfWithNull.count, 0)
  }

  /**
   * Test partitioning when ONLY nulls exist in column.
   */
  @Test
  def testSplitOnNullDataWhenAllNulls(): Unit = {
    val df = getDataWithNulls
    val (dfWithNoNull, dfWithNull) = splitOnNull(df, Version)
    assertEquals(dfWithNoNull.count, 0)
    assertEquals(dfWithNull.count, df.count)
  }

  /**
   * Test partitioning on Empty DataFrame.
   */
  @Test
  def testSplitOnNullEmptyDataFrame(): Unit = {
    val df = getEmptyData
    val (dfWithNoNull, dfWithNull) = splitOnNull(df, Version)
    assertEquals(dfWithNoNull.count, 0)
    assertEquals(dfWithNull.count, 0)
  }

  /**
   * Basic partition test. With nulls and non-nulls.
   */
  @Test
  def testSplitOnNull(): Unit = {
    val df = getDefaultData
    val (dfWithNoNull, dfWithNull) = splitOnNull(df, Version)
    assertEquals(dfWithNoNull.count, df.count / 2)
    assertEquals(dfWithNull.count, df.count / 2)
    dfWithNoNull.collect().foreach(row => assertNotNull(row.getAs[String](Version)))
    dfWithNull.collect().foreach(row => assertNull(row.getAs[String](Version)))
  }

  /**
   * Test split on condition.
   */
  @Test
  def testSplitOnCondition(): Unit = {
    val df = getDataWithNoNulls
    val (df1, df2) = split(df, Version, s"Version < 1")
    df1.collect().foreach(row => assertTrue(row.getAs[String](Version) < "1"))
    df2.collect().foreach(row => assertTrue(row.getAs[String](Version) >= "1"))
  }

  /**
   * Test union when "other" DataFrame is empty.
   */
  @Test
  def testUnionWithEmptyOther(): Unit = {
    val df1 = getDataWithJoinedColumn
    val df2 = getEmptyData
    val result = merge(df1, df2)
    assertEquals(result.count, df1.count)
    assertEquals(result.columns.mkString(","), df1.columns.mkString(","))
  }

  /**
   * Test union when base DataFrame is empty.
   */
  @Test
  def testUnionWithEmptyBase(): Unit = {
    val df1 = getEmptyData
    val df2 = getDefaultData
    val result = merge(df1, df2)
    assertEquals(result.count, df2.count)
    assertEquals(result.columns.mkString(","), df1.columns.mkString(","))
  }

  /**
   * Test union when base DataFrame has less number of columns.
   */
  @Test
  def testUnionWithLessColumnsInBase(): Unit = {
    val df1 = getEmptyData
    val df2 = getDataWithJoinedColumn
    val result = merge(df1, df2)
    assertEquals(result.count, df2.count)
    assertEquals(result.columns.mkString(","), df1.columns.mkString(","))
  }

  /**
   * Test union when base DataFrame has more number of columns.
   */
  @Test
  def testUnionWithMoreColumnsInBase(): Unit = {
    val df1 = getDataWithJoinedColumn
    val df2 = getEmptyData
    val result = merge(df1, df2)
    assertEquals(result.count, df1.count)
    assertEquals(result.columns.mkString(","), df1.columns.mkString(","))
  }

  /**
   * End-to-end partitioning + union test.
   */
  @Test
  def testSplitAndUnion(): Unit = {
    val df = getDefaultData
    val (df1, df2) = splitOnNull(df, Version)
    val df1WithCol = df1.withColumn(s"$Version", concat(col(Id), col(Version)))
    val result = merge(df1WithCol, df2)
    assertEquals(result.count, df.count)
    assertEquals(result.columns.mkString(","), df1WithCol.columns.mkString(","))
  }
}

object TestSplitMergeDataProvider {
  val Id = "Id"
  val Version = "version"
  val schema = {
    val colField1 = StructField(Id, StringType, nullable = true)
    val colField2 = StructField(Version, StringType, nullable = true)
    StructType(List(colField1, colField2))
  }

  val sparkSession = TestFeathr.getOrCreateSparkSession

  def getDefaultData: DataFrame = {
    val data = List(Row("a:1", "1"), Row("a:2", null), Row("z:1", null), Row("z:2", "2"))
    val rdd = sparkSession.sparkContext.parallelize(data)
    sparkSession.createDataFrame(rdd, schema)
  }

  def getDataWithNoNulls: DataFrame = getDefaultData.withColumn(Version, expr(s"case when $Version is null then 0 else $Version end"))
  def getDataWithNulls: DataFrame = getDefaultData.withColumn(Version, lit(null))
  def getEmptyData: DataFrame = sparkSession.createDataFrame(sparkSession.sparkContext.emptyRDD[Row], schema)
  def getDataWithJoinedColumn: DataFrame = getDefaultData.withColumn(s"new_$Version", concat(col(Id), col(Version)))
}
