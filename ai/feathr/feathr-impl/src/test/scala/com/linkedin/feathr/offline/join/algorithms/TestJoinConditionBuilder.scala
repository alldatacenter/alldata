package com.linkedin.feathr.offline.join.algorithms

import com.linkedin.feathr.common.exception.FeathrFeatureJoinException
import com.linkedin.feathr.offline.TestFeathr
import org.apache.spark.sql.functions.{expr, lit}
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.types.{DataTypes, FloatType, StringType, StructField, StructType}
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert._
import org.testng.annotations.Test

/**
 * Unit test class for various join condition builders.
 */
class TestJoinConditionBuilder extends TestFeathr with MockitoSugar {

  /**
   * Test Numeric type as left join key and right join key.
   */
  @Test
  def testNumericTypeAsLeftJoinKeyForBasicSparkJoin(): Unit = {
    val leftJoinKey = Seq("floatKey1")
    val rightJoinKey = Seq("floatKey2")
    val leftDF = getDefaultLeftDataFrame()
    val rightDF = getDefaultRightDataFrame()
    val joinCondition = EqualityJoinConditionBuilder.buildJoinCondition(leftJoinKey, leftDF, rightJoinKey, rightDF)
    assertEquals(joinCondition, leftDF("floatKey1") === rightDF("floatKey2"))
  }

  /**
   * Test String type as left join key
   */
  @Test
  def testStringTypeAsLeftJoinKeyForBasicSparkJoin(): Unit = {
    val leftJoinKey = Seq("stringKey1")
    val rightJoinKey = Seq("floatKey2") // should this be an error?
    val leftDF = getDefaultLeftDataFrame()
    val rightDF = getDefaultRightDataFrame()
    val joinCondition = EqualityJoinConditionBuilder.buildJoinCondition(leftJoinKey, leftDF, rightJoinKey, rightDF)
    assertEquals(joinCondition, leftDF("stringKey1") === rightDF("floatKey2"))
  }

  /**
   * Test Numeric type as left join key and right join key.
   */
  @Test
  def testNumericTypeAsLeftJoinKeyForSequentialJoin(): Unit = {
    val leftJoinKey = Seq("floatKey1")
    val rightJoinKey = Seq("floatKey2")
    val leftDF = getDefaultLeftDataFrame()
    val rightDF = getDefaultRightDataFrame()
    val joinCondition = SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, leftDF, rightJoinKey, rightDF)
    assertEquals(joinCondition, leftDF("floatKey1") === rightDF("floatKey2"))
  }

  /**
   * Test String type as left join key.
   */
  @Test
  def testStringTypeAsLeftJoinKeyForSequentialJoin(): Unit = {
    val leftJoinKey = Seq("stringKey1")
    val rightJoinKey = Seq("floatKey2") // should this be an error?
    val leftDF = getDefaultLeftDataFrame()
    val rightDF = getDefaultRightDataFrame()
    val joinCondition = SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, leftDF, rightJoinKey, rightDF)
    assertEquals(joinCondition, leftDF("stringKey1") === rightDF("floatKey2"))
  }

  /**
   * Test array type as left join key.
   */
  @Test
  def testArrayTypeAsLeftJoinKeyForSequentialJoin(): Unit = {
    val leftJoinKey = Seq("arrayKey1")
    val rightJoinKey = Seq("floatKey2")
    val leftDF = getDefaultLeftDataFrame()
    val rightDF = getDefaultRightDataFrame()
    val joinCondition = SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, leftDF, rightJoinKey, rightDF)
    assertEquals(joinCondition, expr("array_contains(arrayKey1, floatKey2)"))
  }

  /**
   * Test nested field such as a.b.c as left join column.
   */
  @Test
  def testNestedFieldAsLeftJoinKeyForSequentialJoin(): Unit = {
    val leftJoinKey = Seq("mapKey1.stringKey1")
    val rightJoinKey = Seq("stringKey2")
    val leftDF = getDefaultLeftDataFrame()
    val rightDF = getDefaultRightDataFrame()
    val joinCondition = SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, leftDF, rightJoinKey, rightDF)
    assertEquals(joinCondition.toString(), (leftDF("mapKey1.stringKey1") === rightDF("stringKey2")).toString())
  }

  /**
   * Test nested field such as a.b.c as left join column.
   */
  @Test(
    expectedExceptions = Array(classOf[FeathrFeatureJoinException]),
    expectedExceptionsMessageRegExp = ".*StringType, NumericType and ArrayType columns as left join columns.*")
  def testUnsupportedLeftDataTypeForSequentialJoin(): Unit = {
    val leftJoinKey = Seq("booleanKey1")
    val rightJoinKey = Seq("stringKey2")
    val leftDF = getDefaultLeftDataFrame().withColumn("booleanKey1", lit(true))
    val rightDF = getDefaultRightDataFrame()
    SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, leftDF, rightJoinKey, rightDF)
  }

  /**
   * Test multiple join keys.
   */
  @Test
  def testMulitpleKeysInLeftJoinKeyForSequentialJoin(): Unit = {
    val leftJoinKey = Seq("floatKey1", "stringKey1", "arrayKey1")
    val rightJoinKey = Seq("floatKey2", "stringKey2", "floatKey2")
    val leftDF = getDefaultLeftDataFrame()
    val rightDF = getDefaultRightDataFrame()
    val joinCondition = SequentialJoinConditionBuilder.buildJoinCondition(leftJoinKey, leftDF, rightJoinKey, rightDF)
    assertEquals(
      joinCondition,
      leftDF("floatKey1") === rightDF("floatKey2") && leftDF("stringKey1") === rightDF("stringKey2") && expr("array_contains(arrayKey1, floatKey2)"))
  }

  /**
   * Get default left DataFrame for tests.
   */
  private def getDefaultLeftDataFrame(): DataFrame = {
    val data = List(Row(1.0f, "abc", List(1.0f, 2.0f)), Row(2.0f, "def", List(3.0f, 4.0f), List("abc", "xyz")))
    val rdd = ss.sparkContext.parallelize(data)
    ss.createDataFrame(rdd, getDefaultLeftSchema())
  }

  /**
   * Get default right DataFrame for tests.
   */
  private def getDefaultRightDataFrame(): DataFrame = {
    val data = List(Row(1.0f, "abc", "L"), Row(2.0f, "def", "A"))
    val rdd = ss.sparkContext.parallelize(data)
    ss.createDataFrame(rdd, getDefaultRightSchema())
  }

  /**
   * Default schema for the build side of the join.
   */
  private def getDefaultLeftSchema(): StructType = {
    val colField1 = StructField("floatKey1", FloatType, nullable = false)
    val colField2 = StructField("stringKey1", StringType, nullable = false)
    val colField3 = StructField("arrayKey1", DataTypes.createArrayType(FloatType), nullable = false)
    val colField4 = StructField("mapKey1", DataTypes.createStructType(Array(colField2)))
    StructType(List(colField1, colField2, colField3, colField4))
  }

  /**
   * Default schema for the probe side of the join.
   */
  private def getDefaultRightSchema(): StructType = {
    val colField1 = StructField("floatKey2", FloatType, nullable = false)
    val colField2 = StructField("stringKey2", StringType, nullable = false)
    val colField3 = StructField("value", StringType, nullable = false)
    StructType(List(colField1, colField2, colField3))
  }
}
