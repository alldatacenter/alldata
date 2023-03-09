package com.linkedin.feathr.offline.join.algorithms

import com.linkedin.feathr.common.exception.FeathrFeatureJoinException
import com.linkedin.feathr.offline.{AssertFeatureUtils, TestFeathr}
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types._
import org.mockito.Mockito.verifyNoInteractions
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * Unit test class for SparkJoin.
 */
class TestSparkJoin extends TestFeathr with MockitoSugar {

  /**
   * Test that the join throws an exception when the join column sizes do not match.
   */
  @Test
  def testLeftJoinColumnSizeNotEqualToRightJoinColumnSize(): Unit = {
    val mockJoinConditionBuilder = mock[SparkJoinConditionBuilder]
    val defaultJoin = SparkJoinWithJoinCondition(mockJoinConditionBuilder)
    try {
      defaultJoin.join(Seq("col1"), ss.emptyDataFrame, Seq("col1", "col2"), ss.emptyDataFrame, JoinType.left_outer)
    } catch {
      case _: FeathrFeatureJoinException => verifyNoInteractions(mockJoinConditionBuilder)
    }
  }

  /**
   * Test that an exception is thrown when join is called with unsupported join type.
   */
  @Test
  def testUnsupportedJoinType(): Unit = {
    val mockJoinConditionBuilder = mock[SparkJoinConditionBuilder]
    val defaultJoin = SparkJoinWithJoinCondition(mockJoinConditionBuilder)
    try {
      defaultJoin.join(Seq("col1"), ss.emptyDataFrame, Seq("col1"), ss.emptyDataFrame, JoinType.full_outer)
    } catch {
      case _: FeathrFeatureJoinException => verifyNoInteractions(mockJoinConditionBuilder)
    }
  }

  /**
   * Test Spark Join with EqualityConditionBuilder. Tests the success path for SparkJoin.
   */
  @Test
  def testSparkJoinWithEqualityConditionBuilder(): Unit = {
    val leftDF =
      ss.createDataFrame(
        ss.sparkContext.parallelize(Seq(Row("a1", "b1"), Row("a1", "b2"), Row("a2", "b3"))),
        StructType(Seq(StructField("col1", StringType, true), StructField("col2", StringType, true))))
    val rightDF =
      ss.createDataFrame(
        ss.sparkContext.parallelize(Seq(Row("a1", "a1"), Row("a2", "a2"), Row("a3", "a3"))),
        StructType(Seq(StructField("col3", StringType, true), StructField("col4", StringType, true))))
    // expectations
    val expectedRows =
      Seq(Row("a1", "b1", "a1", "a1"), Row("a1", "b2", "a1", "a1"), Row("a2", "b3", "a2", "a2"))
    val expectedCols = Array("col1", "col2", "col3", "col4")
    val defaultJoin = SparkJoinWithJoinCondition(EqualityJoinConditionBuilder)
    val actualDF = defaultJoin.join(Seq("col1"), leftDF, Seq("col3"), rightDF, JoinType.left_outer)

    // validations
    assertEquals(actualDF.count(), expectedRows.size)
    assertEquals(actualDF.columns.sorted.mkString(", "), expectedCols.mkString(", "))
    val actualRows = actualDF.collect().sortBy(row => row.getAs[String]("col1"))
    actualRows.zip(expectedRows).foreach { case (actual, expected) => assertEquals(actual, expected) }

  }

  /**
   * Test that an exception is thrown if the number of columns on the build and probe side of join do not match.
   */
  @Test(
    expectedExceptions = Array(classOf[FeathrFeatureJoinException]),
    expectedExceptionsMessageRegExp = ".*left join key column size is not equal to right join key column size.*")
  def testSparkWithNoJoinConditionThrowsErrorWhenJoinColumnsAreNotEqual(): Unit = {
    val defaultJoin = SparkJoinWithNoJoinCondition()
    defaultJoin.join(Seq("col1"), ss.emptyDataFrame, Seq("col2", "col3"), ss.emptyDataFrame, JoinType.full_outer)
  }

  /**
   * Test SparkWithNoJoinConditions keeps the build side join column after join
   * (and drops the probe side join column).
   */
  @Test
  def testSparkJoinWithNoJoinConditionKeepsBuildSideJoinColumns(): Unit = {
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val leftDF =
      Seq((1, "a1"), (2, "a2"), (3, "a3")).toDF("keyCol1", "featureCol1")
    val rightDF =
      Seq((1, "a1"), (2, "a2"), (4, "a4")).toDF("keyCol2", "featureCol2")

    val expectedSchema = StructType(Seq(StructField("featureCol1", StringType), StructField("featureCol2", StringType), StructField("keyCol", LongType)))
    val expectedRows =
      Array(
        new GenericRowWithSchema(Array("a1", "a1", 1), expectedSchema),
        new GenericRowWithSchema(Array("a2", "a2", 2), expectedSchema),
        new GenericRowWithSchema(Array("a3", null, 3), expectedSchema),
        new GenericRowWithSchema(Array(null, "a4", 4), expectedSchema))

    val joiner = SparkJoinWithNoJoinCondition()
    val resultDF = joiner.join(Seq("keyCol1"), leftDF, Seq("keyCol2"), rightDF, JoinType.full_outer)

    AssertFeatureUtils.validateRows(
      resultDF.select(resultDF.columns.sorted.map(str => col(str)): _*).collect().sortBy(row => row.getAs[Int]("keyCol1")),
      expectedRows)
  }

}
