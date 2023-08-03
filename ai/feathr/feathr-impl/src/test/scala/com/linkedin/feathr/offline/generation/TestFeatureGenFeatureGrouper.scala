package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.FeatureTypeConfig
import com.linkedin.feathr.offline.join.algorithms.SparkJoinWithNoJoinCondition
import com.linkedin.feathr.offline.{AssertFeatureUtils, FeatureDataFrame, JoinKeys, TestFeathr}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types._
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert._
import org.testng.annotations.Test

/**
 * Unit test class for FeatureGenFeatureGrouper.
 */
class TestFeatureGenFeatureGrouper extends TestFeathr with MockitoSugar {

  /**
   * Test joinGroupedFeatures does not fail and returns empty featureData
   * when input is empty.
   */
  @Test
  def testJoinGroupedFeaturesWhenFeatureDataIsEmpty(): Unit = {
    val featureDataWithJoinKeys = Map.empty[String, (FeatureDataFrame, JoinKeys)]

    val featureGenFeatureGrouper = FeatureGenFeatureGrouper()
    assertTrue(featureGenFeatureGrouper.joinGroupedFeatures(featureDataWithJoinKeys, SparkJoinWithNoJoinCondition()).isEmpty)

  }

  /**
   * Test joinGroupedFeatures output. The test validates that the output contains exactly one DF.
   * Test also does data verification.
   */
  @Test
  def testJoinGroupedFeaturesOutput(): Unit = {
    val keyColumnName = "originalKey"
    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df1 =
      FeatureDataFrame(
        Seq((1, 1.0f, 5.0f), (2, 2.0f, 6.0f)).toDF(keyColumnName, s"${FeathrFeatureNamePrefix}f1", s"${FeathrFeatureNamePrefix}f2"),
        Map("f1" -> FeatureTypeConfig.NUMERIC_TYPE_CONFIG, "f2" -> FeatureTypeConfig.NUMERIC_TYPE_CONFIG))

    val df2 =
      FeatureDataFrame(
        Seq((3, "val1", 10.0f), (4, "val2", 11.0f)).toDF(keyColumnName, s"${FeathrFeatureNamePrefix}g1", s"${FeathrFeatureNamePrefix}g2"),
        Map("g1" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG, "g2" -> FeatureTypeConfig.NUMERIC_TYPE_CONFIG))
    val featureDataWithJoinKeys: Map[String, (FeatureDataFrame, JoinKeys)] =
      Map("f1" -> (df1, Seq(keyColumnName)), "f2" -> (df1, Seq(keyColumnName)), "g1" -> (df2, Seq(keyColumnName)), "g2" -> (df2, Seq(keyColumnName)))

    /**
     * Expected output:
     * +------------------+------------------+------------------+------------------+-----------+
     * |__feathr_feature_f1|__feathr_feature_f2|__feathr_feature_g1|__feathr_feature_g2|originalKey|
     * +------------------+------------------+------------------+------------------+-----------+
     * |               1.0|               5.0|              null|              null|          1|
     * |               2.0|               6.0|              null|              null|          2|
     * |              null|              null|              val1|              10.0|          3|
     * |              null|              null|              val2|              11.0|          4|
     * +------------------+------------------+------------------+------------------+-----------+
     */
    val expectedSchema = StructType(
      Seq(
        StructField(s"${FeathrFeatureNamePrefix}f1", FloatType), // f1
        StructField(s"${FeathrFeatureNamePrefix}f2", FloatType), // f2
        StructField(s"${FeathrFeatureNamePrefix}g1", StringType), // g1
        StructField(s"${FeathrFeatureNamePrefix}g2", FloatType), // g2
        StructField(keyColumnName, LongType)))

    val expectedRows =
      Array(
        new GenericRowWithSchema(Array(1.0f, 5.0f, null, null, 1), expectedSchema),
        new GenericRowWithSchema(Array(2.0f, 6.0f, null, null, 2), expectedSchema),
        new GenericRowWithSchema(Array(null, null, "val1", 10.0f, 3), expectedSchema),
        new GenericRowWithSchema(Array(null, null, "val2", 11.0f, 4), expectedSchema))

    val featureGenFeatureGrouper = FeatureGenFeatureGrouper()
    val result = featureGenFeatureGrouper.joinGroupedFeatures(featureDataWithJoinKeys, SparkJoinWithNoJoinCondition())
    assertEquals(result.groupBy(_._2._1).size, 1)

    val resultDF = result.head._2._1.df
    AssertFeatureUtils.validateRows(
      resultDF.select(resultDF.columns.sorted.map(str => col(str)): _*).collect().sortBy(row => row.getAs[Int](keyColumnName)),
      expectedRows)
  }

  /**
   * Test verifies that the feature types of grouped features are aggregated by joinGroupedFeatures.
   */
  @Test
  def testFeatureTypeConfigsAreMergedAfterJoin(): Unit = {
    val keyColumnName = "originalKey"
    val featureNameFeatureTypeConfigMap = Map(
      "f1" -> FeatureTypeConfig.NUMERIC_TYPE_CONFIG,
      "f2" -> FeatureTypeConfig.NUMERIC_TYPE_CONFIG,
      "g1" -> FeatureTypeConfig.CATEGORICAL_TYPE_CONFIG,
      "g2" -> FeatureTypeConfig.NUMERIC_TYPE_CONFIG)

    val sqlContext = ss.sqlContext
    import sqlContext.implicits._
    val df1 =
      FeatureDataFrame(
        Seq((1, 1.0f, 5.0f), (2, 2.0f, 6.0f)).toDF(keyColumnName, s"${FeathrFeatureNamePrefix}f1", s"${FeathrFeatureNamePrefix}f2"),
        featureNameFeatureTypeConfigMap.filterKeys(Seq("f1", "f2").contains))

    val df2 =
      FeatureDataFrame(
        Seq((3, "val1", 10.0f), (4, "val2", 11.0f)).toDF(keyColumnName, s"${FeathrFeatureNamePrefix}g1", s"${FeathrFeatureNamePrefix}g2"),
        featureNameFeatureTypeConfigMap.filterKeys(Seq("g1", "g2").contains))
    val featureDataWithJoinKeys: Map[String, (FeatureDataFrame, JoinKeys)] =
      Map("f1" -> (df1, Seq(keyColumnName)), "f2" -> (df1, Seq(keyColumnName)), "g1" -> (df2, Seq(keyColumnName)), "g2" -> (df2, Seq(keyColumnName)))

    val featureGenFeatureGrouper = FeatureGenFeatureGrouper()
    val result = featureGenFeatureGrouper.joinGroupedFeatures(featureDataWithJoinKeys, SparkJoinWithNoJoinCondition())
    assertEquals(result.groupBy(_._2._1).size, 1)
    assertEquals(result.head._2._1.inferredFeatureType, featureNameFeatureTypeConfigMap)
  }
}
