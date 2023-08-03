package com.linkedin.feathr.offline.join.workflow

import com.linkedin.feathr.common.exception.FeathrFeatureJoinException
import com.linkedin.feathr.offline.{ErasedEntityTaggedFeature, FeatureDataFrame, TestFeathr}
import com.linkedin.feathr.offline.job.{KeyedTransformedResult, TransformedResult}
import com.linkedin.feathr.offline.join.algorithms.JoinType.JoinType
import com.linkedin.feathr.offline.join.JoinExecutionContext
import com.linkedin.feathr.offline.join.algorithms.{EqualityJoinConditionBuilder, IdentityJoinKeyColumnAppender, SparkJoinWithJoinCondition, SqlTransformedLeftJoinKeyColumnAppender}
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlan}
import com.linkedin.feathr.offline.util.FeathrUtils
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types.{DataTypes, FloatType, StringType, StructField, StructType}
import org.apache.spark.sql.functions.{col, concat_ws, lit}
import org.testng.annotations.Test
import org.testng.Assert._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.mockito.MockitoSugar

/**
 * Unit test class for [[AnchoredFeatureJoinStep]].
 */
class TestAnchoredFeatureJoinStep extends TestFeathr with MockitoSugar {

  /**
   * Test when join stages are empty. Should return input DF and empty map for inferred types.
   */
  @Test
  def testJoinStepWhenJoinStagesIsEmpty(): Unit = {
    // mock execution context
    val mockExecutionContext = mock[JoinExecutionContext]
    val mockLogicalPlan = mock[MultiStageJoinPlan]
    val mockFeatureGroups = mock[FeatureGroups]
    when(mockExecutionContext.logicalPlan).thenReturn(mockLogicalPlan)
    when(mockExecutionContext.featureGroups).thenReturn(mockFeatureGroups)
    val mockSparkContext = mock[SparkContext]
    val mockSparkSession = mock[SparkSession]
    val mockSparkConf = mock[SparkConf]
    when(mockSparkConf.get(s"${FeathrUtils.FEATHR_PARAMS_PREFIX}${FeathrUtils.ENABLE_CHECKPOINT}", "true"))
      .thenReturn("true")
    when(mockSparkConf.get(s"${FeathrUtils.FEATHR_PARAMS_PREFIX}${FeathrUtils.ENABLE_CHECKPOINT}", "false"))
      .thenReturn("false")
    when(mockSparkContext.getConf).thenReturn(mockSparkConf)
    when(mockSparkSession.sparkContext).thenReturn(mockSparkContext)
    when(mockExecutionContext.sparkSession).thenReturn(mockSparkSession)
    when(mockLogicalPlan.joinStages).thenReturn(Seq.empty)

    // mock anchor join step input
    val mockAnchorStepInput = mock[AnchorJoinStepInput]
    when(mockAnchorStepInput.observation).thenReturn(ss.emptyDataFrame)
    val basicAnchoredFeatureJoinStep =
      AnchoredFeatureJoinStep(SqlTransformedLeftJoinKeyColumnAppender, IdentityJoinKeyColumnAppender, SparkJoinWithJoinCondition(EqualityJoinConditionBuilder), None)
    val FeatureDataFrameOutput(FeatureDataFrame(outputDF, inferredFeatureType)) =
      basicAnchoredFeatureJoinStep.joinFeatures(Seq(ErasedEntityTaggedFeature(Seq(0), "featureName1")), mockAnchorStepInput)(mockExecutionContext)

    // validate output
    assertTrue(outputDF.collect().isEmpty)
    assertTrue(inferredFeatureType.isEmpty)
    // verify mocks
    verify(mockExecutionContext).logicalPlan
    verify(mockExecutionContext).featureGroups
    verify(mockLogicalPlan).joinStages
  }

  /**
   * Test join on single DF when features do not share the same key.
   * This method does not support different keys for input features.
   * So we expect an exception to be thrown.
   */
  @Test(expectedExceptions = Array(classOf[FeathrFeatureJoinException]))
  def testJoinOnSingleDFWhenFeaturesDoNotHaveSameKeys(): Unit = {
    val mockExecutionContext = mock[JoinExecutionContext] // mock execution context
    val mockTransformedResult = mock[TransformedResult]
    // mock KeyTransformedResult with different joinKeys
    val keyedTransformedResults = Seq(
      KeyedTransformedResult(Seq("joinKey1", "joinKey2"), mockTransformedResult),
      KeyedTransformedResult(Seq("joinKey2", "joinKey3"), mockTransformedResult))
    val basicAnchoredFeatureJoinStep =
      AnchoredFeatureJoinStep(SqlTransformedLeftJoinKeyColumnAppender, IdentityJoinKeyColumnAppender, SparkJoinWithJoinCondition(EqualityJoinConditionBuilder), None)
    basicAnchoredFeatureJoinStep.joinFeaturesOnSingleDF(
      Seq(0),
      Seq("leftJoinKeyColumn"),
      ss.emptyDataFrame,
      (Seq("feature1", "feature2"), keyedTransformedResults))(mockExecutionContext)
  }

  /**
   * Test join on single DataFrame when right join DF is empty.
   * The test validates that the columns from the feature DataFrame are joined to observation.
   */
  @Test
  def testJoinOnSingleDFWhenRightJoinColumnSizeIsZero(): Unit = {
    val mockExecutionContext = mock[JoinExecutionContext] // mock execution context
    // mock feature DF
    val mockTransformedResult = mock[TransformedResult]
    when(mockTransformedResult.df).thenReturn(
      getEmptyDataFrameWithSchema()
        .withColumnRenamed("x", "featurex")
        .withColumnRenamed("as", "featureas")
        .withColumnRenamed("v2", "featureScore"))
    val keyedTransformedResults = Seq(KeyedTransformedResult(Seq.empty[String], mockTransformedResult)) // empty join column
    // observation DF
    val leftDF = getDefaultDataFrame()
    val basicAnchoredFeatureJoinStep =
      AnchoredFeatureJoinStep(SqlTransformedLeftJoinKeyColumnAppender, IdentityJoinKeyColumnAppender, SparkJoinWithJoinCondition(EqualityJoinConditionBuilder), None)
    val resultDF = basicAnchoredFeatureJoinStep.joinFeaturesOnSingleDF(Seq(0), Seq("x"), leftDF, (Seq("feature1", "feature2"), keyedTransformedResults))(
      mockExecutionContext)
    resultDF.show()
    val sortedColumns = resultDF.columns.sorted

    // verify output columns
    assertEquals(s"[${sortedColumns.mkString(", ")}]", "[as, featureScore, featureas, featurex, v2, x]")
    // verify mocks
    verify(mockTransformedResult).df
    verifyNoMoreInteractions(mockExecutionContext)
  }

  /**
   * Unit test, verifies that Salted Join algorithm is selected when
   * salted join is required for key tags.
   */
  @Test
  def testJoinOnSingleDFWhenSaltedJoin(): Unit = {
    val mockExecutionContext = mock[JoinExecutionContext] // mock execution context
    when(mockExecutionContext.frequentItemEstimatedDFMap).thenReturn(None)
    when(mockExecutionContext.sparkSession).thenReturn(ss)

    // mock feature DF
    val mockTransformedResult = mock[TransformedResult]
    when(mockTransformedResult.df).thenReturn(
      getEmptyDataFrameWithSchema()
        .withColumn("featurex", concat_ws("-", lit("feature"), col("x")))
        .withColumn("featureas", concat_ws("-", lit("as"), col("x")))
        .withColumn("featureScore", concat_ws("-", lit("v2"), col("x"))))
    val keyedTransformedResults = Seq(KeyedTransformedResult(Seq("featurex"), mockTransformedResult))

    // Inject a mock joiner to return empty DataFrame
    val mockJoiner = mock[SparkJoinWithJoinCondition]
    when(mockJoiner.join(any(classOf[Seq[String]]), any(classOf[DataFrame]), any(classOf[Seq[String]]), any(classOf[DataFrame]), any(classOf[JoinType])))
      .thenReturn(getEmptyDataFrameWithSchema())

    // observation DF
    val leftDF = getDefaultDataFrame()
    val basicAnchoredFeatureJoinStep = AnchoredFeatureJoinStep(SqlTransformedLeftJoinKeyColumnAppender, IdentityJoinKeyColumnAppender, mockJoiner, None)
    val resultDF = basicAnchoredFeatureJoinStep.joinFeaturesOnSingleDF(Seq(0), Seq("x"), leftDF, (Seq("feature1", "feature2"), keyedTransformedResults))(
      mockExecutionContext)
    // Verify that the joiner was called by validating an empty DataFrame was indeed returned
    assertTrue(resultDF.collect().isEmpty)
    // verify mocks
    verify(mockExecutionContext).frequentItemEstimatedDFMap
    verify(mockTransformedResult).df
    verify(mockJoiner).join(any(classOf[Seq[String]]), any(classOf[DataFrame]), any(classOf[Seq[String]]), any(classOf[DataFrame]), any(classOf[JoinType]))
  }

  /**
   * Get default DataFrame for tests.
   */
  private def getDefaultDataFrame(): DataFrame = {
    val data = List(Row("a1", List("python", "java"), 56.0f), Row("a2", List("scala"), 80.0f))
    val rdd = ss.sparkContext.parallelize(data)
    ss.createDataFrame(rdd, getDefaultDataFrameSchema())
  }

  /**
   * Get empty DataFrame with default schema.
   */
  private def getEmptyDataFrameWithSchema(): DataFrame = {
    val rdd = ss.sparkContext.emptyRDD[Row]
    ss.createDataFrame(rdd, getDefaultDataFrameSchema())
  }

  /**
   * Get default schema for tests.
   */
  private def getDefaultDataFrameSchema(): StructType = {
    val column1 = StructField("x", StringType, nullable = false)
    val column2 = StructField("as", DataTypes.createArrayType(StringType), nullable = false)
    val column3 = StructField("v2", FloatType, nullable = false)
    StructType(List(column1, column2, column3))
  }
}
