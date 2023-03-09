package com.linkedin.feathr.offline.client

import com.linkedin.feathr.common.{DateParam, FeatureTypeConfig, JoiningFeatureParams, TaggedFeatureName}
import com.linkedin.feathr.offline.TestFeathr
import com.linkedin.feathr.offline.anchored.feature.{FeatureAnchor, FeatureAnchorWithSource}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar.mock
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

import scala.collection.JavaConverters._

class TestDataFrameColName extends TestFeathr {

  private def getSampleFeathrGeneratedDF() = {
    // Create a dataframe where obsCol1 and obsCol2 are part of the observation data, and f1NumericType, f2StringType belong to the feature data.
    /*
     * obsCol1  obsCol2  f1NumericType  f2StringType
     * row1     m1        2.0            abc
     */
    val dfSchema = StructType(
      List(
        StructField("obsCol1", StringType),
        StructField("obsCol2", StringType),
        StructField("__feathr_feature_f1__feathr_tags_OZUWK53FMU", DoubleType),
        StructField("__feathr_feature_f1__feathr_tags_OZUWK53FOI", DoubleType),
        StructField("__feathr_feature_seq_join_a_names__feathr_tags_PA", DoubleType)))

    // Values for the dataframe
    val values = List(
      Row("row1", "m1", 2.0, 3.0))

    ss.createDataFrame(values.asJava, dfSchema)
  }


  @Test
  def testGetFeatureAlias(): Unit = {
    val joiningFeatureParams = Seq(
      JoiningFeatureParams(Seq("mId"), "f1", None, None, Some("f1")),
      JoiningFeatureParams(Seq("mId", "pId"), "f1", None, None, Some("f1_withKeyTags")),
      JoiningFeatureParams(Seq("mId"), "f1", Some(DateParam(Some("12012020"), Some("12062020"), None, None)), None, Some("f1_withDateParam")),
      JoiningFeatureParams(Seq("mId"), "f1", None, Some("1d"), Some("f1_withTimeDelay"))
    )
    assertEquals(DataFrameColName.getFeatureAlias(joiningFeatureParams, "f1", Seq("mId"), None, None), Some("f1"))
    assertEquals(DataFrameColName.getFeatureAlias(joiningFeatureParams, "f1", Seq("mId", "pId"), None, None), Some("f1_withKeyTags"))
    assertEquals(DataFrameColName.getFeatureAlias(joiningFeatureParams, "f1", Seq("mId"),
      Some(DateParam(Some("12012020"), Some("12062020"), None, None)), None), Some("f1_withDateParam"))
    assertEquals(DataFrameColName.getFeatureAlias(joiningFeatureParams, "f1", Seq("mId"), None, Some("1d")), Some("f1_withTimeDelay"))
  }

  @Test
  def testGetTaggedFeatureToNewColumnNameMap(): Unit = {
    val df = getSampleFeathrGeneratedDF()
    val taggedFeatureToNewColumnNameMap = DataFrameColName.getTaggedFeatureToNewColumnName(df)
    val taggedFeature1 = new TaggedFeatureName("viewee", "f1")
    assertEquals(taggedFeatureToNewColumnNameMap(taggedFeature1)._2, "viewee__f1")
    val taggedFeature2 = new TaggedFeatureName("viewer", "f1")
    assertEquals(taggedFeatureToNewColumnNameMap(taggedFeature2)._2, "viewer__f1")
    val taggedFeature3 = new TaggedFeatureName("x", "seq_join_a_names")
    assertEquals(taggedFeatureToNewColumnNameMap(taggedFeature3)._2, "seq_join_a_names")
  }

  @Test(description = "Inferred feature type should be honored when user does not provide feature type")
  def testGenerateHeader(): Unit = {
    val mockFeatureAnchor = mock[FeatureAnchor]
    // Mock if the user does not define feature type
    when(mockFeatureAnchor.featureTypeConfigs).thenReturn(Map.empty[String, FeatureTypeConfig])

    val mockFeatureAnchorWithSource = mock[FeatureAnchorWithSource]
    when(mockFeatureAnchorWithSource.featureAnchor).thenReturn(mockFeatureAnchor)
    val taggedFeatureName = new TaggedFeatureName("id", "f")
    val  featureToColumnNameMap: Map[TaggedFeatureName, String] = Map(taggedFeatureName -> "f")
    val allAnchoredFeatures: Map[String, FeatureAnchorWithSource] = Map("f" -> mockFeatureAnchorWithSource)
    // Mock if the type if inferred to be numeric
    val inferredFeatureTypeConfigs: Map[String, FeatureTypeConfig] = Map("f" -> FeatureTypeConfig.NUMERIC_TYPE_CONFIG)
    val header = DataFrameColName.generateHeader(
      featureToColumnNameMap,
      allAnchoredFeatures,
      Map(),
      inferredFeatureTypeConfigs)
    // output should be using the inferred type, i.e. numeric
    assertEquals(header.featureInfoMap.get(taggedFeatureName).get.featureType, FeatureTypeConfig.NUMERIC_TYPE_CONFIG)
  }
}
