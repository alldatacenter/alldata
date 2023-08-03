package com.linkedin.feathr.offline.job

import com.linkedin.feathr.common
import com.linkedin.feathr.common.{AnchorExtractor, FeatureTypeConfig, FeatureTypes, FeatureValue}
import com.linkedin.feathr.offline.anchored.anchorExtractor.SimpleConfigurableAnchorExtractor
import com.linkedin.feathr.offline.anchored.feature.{FeatureAnchor, FeatureAnchorWithSource}
import com.linkedin.feathr.offline.config.MVELFeatureDefinition
import com.linkedin.feathr.offline.job.FeatureTransformation.{FeatureGroupWithSameTimeWindow, groupFeatures}
import com.linkedin.feathr.offline.source.DataSource
import com.linkedin.feathr.offline.source.accessor.DataSourceAccessor
import com.linkedin.feathr.offline.transformation.{FeatureColumnFormat, FeatureValueToFDSColumnConverter}
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils
import com.linkedin.feathr.offline.{AssertFeatureUtils, TestFeathr}
import com.linkedin.feathr.sparkcommon.{SimpleAnchorExtractorSpark, SourceKeyExtractor}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.testng.Assert.assertEquals
import org.testng.annotations.{DataProvider, Test}

/**
 * Tests for [[FeatureTransformation]]
 */
class TestFeatureTransformation extends TestFeathr with MockitoSugar {
  @Test(expectedExceptions = Array(classOf[RuntimeException]))
  def testGetFeatureDefinitionsFail(): Unit = {
    val extractor = mock[AnchorExtractor[Any]]
    FeatureTransformation.getFeatureDefinitions(extractor)
  }

  @Test(description = "Test the convertTransformedDFToFDS() to function which convert the anchor's TransformedResult into FDS format")
  def testConvertTransformedDFToFDS(): Unit = {
    val anchorsWithSameSource = mock[FeatureAnchorWithSource]
    when(anchorsWithSameSource.selectedFeatures).thenReturn(Seq("f1", "f2"))
    val featureAnchor = mock[FeatureAnchor]
    when(anchorsWithSameSource.featureAnchor).thenReturn(featureAnchor)
    when(featureAnchor.getFeatureTypes).thenReturn(None)
    val anchorFeatureGroup = mock[AnchorFeatureGroups]
    when(anchorFeatureGroup.anchorsWithSameSource).thenReturn(Seq(anchorsWithSameSource))
    val transformedResult = mock[TransformedResult]
    when(transformedResult.inferredFeatureTypes).thenReturn(Map.empty[String, FeatureTypeConfig])
    when(transformedResult.featureColumnFormats).thenReturn(Map("f1" -> FeatureColumnFormat.RAW, "f2" -> FeatureColumnFormat.RAW))
    val withFeatureDF = {
      val expSchema = StructType(
        List(
          StructField("IdInObservation", StringType, true),
          StructField("f1", FloatType, nullable = true),
          StructField("f2", StringType, nullable = true)))
      val expData = List(Row("a:1", 1.0f, "a"), Row("a:2", 4.0f, "b"), Row("a:3", null, null))
      val rdd = ss.sparkContext.parallelize(expData)
      ss.createDataFrame(rdd, expSchema)
    }
    val convertedDF = FeatureTransformation.convertTransformedDFToFDS(Seq("f1", "f2"), transformedResult, withFeatureDF)

    val expJoinedObsAndFeatures = {
      val expSchema = StructType(
        List(
          StructField("IdInObservation", StringType, true),
          StructField("f1", FloatType, nullable = true),
          StructField(
            "f2",
            StructType(List(
              StructField("indices0", ArrayType(StringType, containsNull = false), nullable = false),
              StructField("values", ArrayType(FloatType, containsNull = false), nullable = false))))))

      val expData = List(
        Row("a:1", 1.0f, Row(List("a"), List(1.0f))),
        Row("a:2", 4.0f, Row(List("b"), List(1.0f))),
        Row("a:3", null, null))

      val rdd = ss.sparkContext.parallelize(expData)
      ss.createDataFrame(rdd, expSchema)
    }
    AssertFeatureUtils.assertDataFrameEquals(convertedDF.df, expJoinedObsAndFeatures)
  }


  @DataProvider
  def dataForTestConvertFeatureValueToFDSColumn(): Array[Array[Any]] = {
    val termValue = new java.util.HashMap[String, java.lang.Float]
    termValue.put("a", 1.0f)
    Array(
      // empty term value to FDS1D tensor column
      Array(FeatureValue.createStringTermVector(new java.util.HashMap[String, java.lang.Float]),
        FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE, FeatureTypes.BOOLEAN, "[[],[]]"),
      // non-empty term value to FDS1D tensor column
      Array(FeatureValue.createStringTermVector(termValue),
        FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE, FeatureTypes.BOOLEAN, "[[a],[1.0]]"),
      // float feature value to FDS tensor column
      Array(FeatureValue.createNumeric(2.0f), FloatType, FeatureTypes.NUMERIC, "2.0"),
      // numeric feature value to FDS 1d tensor column (type casting)
      Array(FeatureValue.createNumeric(2.0f), FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE, FeatureTypes.CATEGORICAL, "[[],[2.0]]"),
      // boolean feature value to FDS tensor column
      Array(FeatureValue.createBoolean(true), BooleanType, FeatureTypes.BOOLEAN, "true"),
      // boolean feature value to FDS tensor column
      Array(FeatureValue.createBoolean(false), BooleanType, FeatureTypes.BOOLEAN, "false"),
      // float dense value to FDS tensor column
      Array(FeatureValue.createDenseVector[java.lang.Float](java.util.Arrays.asList[java.lang.Float](10.0f, 20.0f)),
        FeaturizedDatasetUtils.DENSE_VECTOR_FDS_DATA_TYPE, FeatureTypes.DENSE_VECTOR, "[10.0,20.0]"),
      // double dense value to FDS tensor column
      Array(FeatureValue.createDenseVector[java.lang.Double](java.util.Arrays.asList[java.lang.Double](10.0, 20.0)),
        FeaturizedDatasetUtils.DENSE_VECTOR_FDS_DATA_TYPE, FeatureTypes.DENSE_VECTOR, "[10.0,20.0]")
    )
  }

  @Test(dataProvider = "dataForTestConvertFeatureValueToFDSColumn", description = "test conversion from feature value to FDS column")
  def testConvertFeatureValueToFDSColumn(featureValue: FeatureValue, fieldType: DataType, featureType: FeatureTypes, expectedColStr: String): Unit = {
    val result = FeatureValueToFDSColumnConverter.convert("foo", featureValue, fieldType, featureType)
    assertEquals(result.toString(), expectedColStr)
  }

  @Test(description = "Test that groupFeatures groups MVEL anchors under the same grouping criteria and merges them into one anchor.")
  def testGroupFeaturesWithMVEL(): Unit = {
    // Mock values used for all anchors
    val keyForAllFeatures = Seq("x")
    val defaults = Map.empty[String, common.FeatureValue]
    val featureTypeConfigs = Map.empty[String, FeatureTypeConfig]
    val source = "mock_source"
    val mockDataSource = mock[DataSource]
    val mockDataSourceAccessor = mock[DataSourceAccessor]
    val mockSourceKeyExtractor = mock[SourceKeyExtractor]

    // First MVEL anchor
    val f1mockMVEL = MVELFeatureDefinition("f1")
    val f1definition = Map(("f1", f1mockMVEL))
    val f1Extractor = new SimpleConfigurableAnchorExtractor(keyForAllFeatures, f1definition)
    val f1FeatureSet = Set("f1")
    val f1FeatureAnchor = FeatureAnchor(source, f1Extractor, defaults, None, mockSourceKeyExtractor, f1FeatureSet, featureTypeConfigs)
    val f1FeatureAnchorWithSource = FeatureAnchorWithSource(f1FeatureAnchor, mockDataSource, None, Some(f1FeatureSet.toSeq))

    // Second MVEL anchor
    val f2mockMVEL = MVELFeatureDefinition("f2")
    val f2definition = Map(("f2", f2mockMVEL))
    val f2Extractor = new SimpleConfigurableAnchorExtractor(keyForAllFeatures, f2definition)
    val f2FeatureSet = Set("f2")
    val f2FeatureAnchor = FeatureAnchor(source, f2Extractor, defaults, None, mockSourceKeyExtractor, f2FeatureSet, featureTypeConfigs)
    val f2FeatureAnchorWithSource = FeatureAnchorWithSource(f2FeatureAnchor, mockDataSource, None, Some(f2FeatureSet.toSeq))

    // Expected result anchor
    val expectedExtractor = new SimpleConfigurableAnchorExtractor(keyForAllFeatures, f1definition ++ f2definition)
    val expectedFeatureSet = f1FeatureSet ++ f2FeatureSet
    val expectedFeatureGroup = FeatureGroupWithSameTimeWindow(None, expectedFeatureSet.toSeq)

    val inputAnchorMaps = Map((f1FeatureAnchorWithSource, mockDataSourceAccessor), (f2FeatureAnchorWithSource, mockDataSourceAccessor))
    val requestedFeatures = Set("f1", "f2")
    val result = groupFeatures(inputAnchorMaps, requestedFeatures)

    // Should only have 1 grouping criteria
    assert(result.size == 1)
    // Within the criteria we should only have 1 featureAnchorWithSource
    assert(result.head._2.size == 1)
    assertEquals(result.head._2.head._2, expectedFeatureGroup)
    // Assert result extractor is combined
    assertEquals(result.head._2.head._1.featureAnchor.getAsAnchorExtractor.asInstanceOf[SimpleConfigurableAnchorExtractor].getFeaturesDefinitions(),
      expectedExtractor.getFeaturesDefinitions())
    assertEquals(result.head._2.head._1.featureAnchor.getAsAnchorExtractor.asInstanceOf[SimpleConfigurableAnchorExtractor].getKeyExpression(),
      expectedExtractor.getKeyExpression())
    // Assert requested features are combined
    assertEquals(result.head._2.head._1.selectedFeatures, expectedFeatureSet.toSeq)
  }

  @Test(description = "Test that groupFeatures groups spark extractors under same grouping criteria but does not merge the anchors into 1.")
  def testGroupFeaturesWithSparkExtractors(): Unit = {
    // Mock values used for all anchors
    val defaults = Map.empty[String, common.FeatureValue]
    val featureTypeConfigs = Map.empty[String, FeatureTypeConfig]
    val source = "mock_source"
    val mockDataSource = mock[DataSource]
    val mockDataSourceAccessor = mock[DataSourceAccessor]
    val mockSourceKeyExtractor = mock[SourceKeyExtractor]

    // First MVEL anchor
    val f1Extractor = mock[SimpleAnchorExtractorSpark]
    val f1FeatureSet = Set("f1")
    val f1FeatureAnchor = FeatureAnchor(source, f1Extractor, defaults, None, mockSourceKeyExtractor, f1FeatureSet, featureTypeConfigs)
    val f1FeatureAnchorWithSource = FeatureAnchorWithSource(f1FeatureAnchor, mockDataSource, None, Some(f1FeatureSet.toSeq))

    // Second MVEL anchor
    val f2Extractor = mock[SimpleAnchorExtractorSpark]
    val f2FeatureSet = Set("f2")
    val f2FeatureAnchor = FeatureAnchor(source, f2Extractor, defaults, None, mockSourceKeyExtractor, f2FeatureSet, featureTypeConfigs)
    val f2FeatureAnchorWithSource = FeatureAnchorWithSource(f2FeatureAnchor, mockDataSource, None, Some(f2FeatureSet.toSeq))

    val inputAnchorMaps = Map((f1FeatureAnchorWithSource, mockDataSourceAccessor), (f2FeatureAnchorWithSource, mockDataSourceAccessor))
    val requestedFeatures = Set("f1", "f2")
    val result = groupFeatures(inputAnchorMaps, requestedFeatures)

    // Should only have 1 grouping criteria
    assert(result.size == 1)
    // Within the criteria we should have 2 featureAnchorWithSource
    assert(result.head._2.size == 2)
    // The input anchors should not be modified in this case.
    assertEquals(result.head._2.head._1, f1FeatureAnchorWithSource)
  }
}
