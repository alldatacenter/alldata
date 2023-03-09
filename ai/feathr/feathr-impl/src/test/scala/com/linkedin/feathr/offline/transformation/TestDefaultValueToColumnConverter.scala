package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.common.FeatureTypes._
import com.linkedin.feathr.common.FeatureValue
import com.linkedin.feathr.common.exception.{FeathrException, FeathrFeatureTransformationException}
import com.linkedin.feathr.offline.TestFeathr
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.testng.annotations.{DataProvider, Test}

import java.{lang => jl, util => ju}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{ArrayType, BooleanType, DataType, DateType, DoubleType, FloatType, IntegerType, LongType, MapType, ShortType, StringType, StructField, StructType}
import org.testng.Assert.assertEquals

import scala.collection.mutable

/**
 * Unit test class for DefaultValueToColumnConverter.
 */
class TestDefaultValueToColumnConverter extends TestFeathr with MockitoSugar {

  /**
   * A data provider that provides IntegralType i.e., a subset of NumericType but are not FractionalType.
   * These include IntegerType, ShortType and LongType.
   */
  @DataProvider(name = "IntegralDataTypeProvider")
  def integralDataTypeProvider(): Array[Array[Object]] = Array(Array(IntegerType), Array(ShortType), Array(LongType))

  /**
   * A data provider that provides FeatureValueToColumnConverter.
   */
  @DataProvider(name = "FeatureValueToColumnConverterProvider")
  def featureValueToColumnConverterProvider(): Array[Array[Object]] = Array(Array(FeatureValueToFDSColumnConverter), Array(FeatureValueToRawColumnConverter))

  /**
   * A data provider that provides combination of IntegralTypes and  FeatureValueToColumnConverter.
   */
  @DataProvider(name = "DataTypeAndConverterProvider")
  def dataTypeAndConverterProvider(): Array[Array[Object]] = {
    val integralTypes = Array(IntegerType, ShortType, LongType)
    val converterTypes = Array(FeatureValueToFDSColumnConverter, FeatureValueToRawColumnConverter)
    for {
      x <- integralTypes
      y <- converterTypes
    } yield Array(x, y)
  }

  /**
   * Test converting CATEGORICAL FeatureValue to StringType column in RAW format.
   */
  @Test
  def testConvertStringFieldToRawColumn(): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    mockTermVector.put("term1", 1.0f)
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector)

    val retCol = FeatureValueToRawColumnConverter.convert("f1", mockFeatureValue, StringType, CATEGORICAL)
    assertEquals(retCol.expr.dataType, StringType)
    verify(mockFeatureValue).getAsTermVector
  }

  /**
   * Test converting CATEGORICAL FeatureValue to IntegerType, ShortType and LongType columns in RAW & FDS format.
   * A data provider is used to run through all combinations.
   */
  @Test(dataProvider = "DataTypeAndConverterProvider")
  def testConvertIntegralFieldToRawColumnForCategoricalType(dataType: DataType, converter: FeatureValueToColumnConverter): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    when(mockFeatureValue.getAsCategorical).thenReturn("1")
    val retCol = converter.convert("f1", mockFeatureValue, dataType, CATEGORICAL)
    assertEquals(retCol.expr.dataType, dataType)
    verify(mockFeatureValue).getAsCategorical
  }

  /**
   * Test converting FeatureValue with unspecified FeatureType, to IntegerType, ShortType and LongType columns in RAW & FDS format.
   * A data provider is used to run through all combinations.
   */
  @Test(dataProvider = "DataTypeAndConverterProvider")
  def testConvertIntegralFieldToRawColumnForUnspecifiedType(dataType: DataType, converter: FeatureValueToColumnConverter): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    when(mockFeatureValue.getAsNumeric).thenReturn(1.0f)
    val retCol = converter.convert("f1", mockFeatureValue, dataType, UNSPECIFIED)
    assertEquals(retCol.expr.dataType, dataType)
    verify(mockFeatureValue).getAsNumeric
  }

  /**
   * Test converting FeatureValue with unspecified FeatureType, to LongType column in RAW format.
   */
  @Test
  def testConvertDoubleFieldToRawColumn(): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    when(mockFeatureValue.getAsNumeric).thenReturn(1.0f)
    val retCol = FeatureValueToRawColumnConverter.convert("f1", mockFeatureValue, DoubleType, UNSPECIFIED)
    assertEquals(retCol.expr.dataType, DoubleType)
    verify(mockFeatureValue).getAsNumeric
  }

  /**
   * Test converting BOOLEAN type FeatureValue to a BooleanType column in RAW format.
   */
  @Test
  def testConvertBooleanFieldToRawColumn(): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    when(mockFeatureValue.getAsBoolean).thenReturn(true)
    val retCol = FeatureValueToRawColumnConverter.convert("f1", mockFeatureValue, BooleanType, BOOLEAN)
    assertEquals(retCol.expr.dataType, BooleanType)
    verify(mockFeatureValue).getAsBoolean
  }

  /**
   * Test converting TERM_VECTOR type FeatureValue to a MapType column in RAW format.
   */
  @Test
  def testConvertMapFieldToRawColumn(): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    mockTermVector.put("term1", 1.0f)
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector)

    val retCol = FeatureValueToRawColumnConverter.convert("f1", mockFeatureValue, MapType(StringType, IntegerType, true), TERM_VECTOR)
    assertEquals(retCol.expr.dataType, MapType(StringType, FloatType, true)) // Note: We expect StringType -> FloatType to be returned.
    verify(mockFeatureValue).getAsTermVector
  }

  /**
   * Test converting CATEGORICAL_SET type FeatureValue to a ArrayType[IntegerType] column in RAW format.
   */
  @Test
  def testConvertCategoricalSetOfIntegerElementTypeToRawColumn(): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    mockTermVector.put("1", 1.0f)
    mockTermVector.put("2", 1.0f)
    mockTermVector.put("3", 1.0f)
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector)

    val retCol = FeatureValueToRawColumnConverter.convert("f1", mockFeatureValue, ArrayType(IntegerType), CATEGORICAL_SET)
    assertEquals(retCol.expr.dataType, ArrayType(IntegerType, false))
    verify(mockFeatureValue).getAsTermVector

    // Validate by applying the column on a DataFrame
    val keyField = StructField("key", StringType, nullable = false)
    val valueField = StructField("value", ArrayType(IntegerType))
    val validationDF = ss.createDataFrame(ss.sparkContext.parallelize(Seq(Row("key1", null))), StructType(List(keyField, valueField)))
    val retData = validationDF.withColumn("defaults", retCol).collect().head.getAs[Seq[Integer]]("defaults")
    assertEquals(retData.sorted, mutable.WrappedArray.make(Array(1, 2, 3).map(_.intValue())))
  }

  /**
   * Test converting CATEGORICAL_SET type FeatureValue to a ArrayType[ShortType] column in RAW format.
   */
  @Test
  def testConvertCategoricalSetOfShortElementTypeToRawColumn(): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    mockTermVector.put("1", 1.0f)
    mockTermVector.put("2", 1.0f)
    mockTermVector.put("3", 1.0f)
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector)

    val retCol = FeatureValueToRawColumnConverter.convert("f1", mockFeatureValue, ArrayType(ShortType), CATEGORICAL_SET)
    assertEquals(retCol.expr.dataType, ArrayType(ShortType, false))
    verify(mockFeatureValue).getAsTermVector

    // Validate by applying the column on a DataFrame
    val keyField = StructField("key", StringType, nullable = false)
    val valueField = StructField("value", ArrayType(ShortType))
    val validationDF = ss.createDataFrame(ss.sparkContext.parallelize(Seq(Row("key1", null))), StructType(List(keyField, valueField)))
    val retData = validationDF.withColumn("defaults", retCol).collect().head.getAs[Seq[Short]]("defaults")
    assertEquals(retData.sorted, mutable.WrappedArray.make(Array(1, 2, 3).map(_.shortValue())))
  }

  /**
   * Test converting CATEGORICAL_SET type FeatureValue to a ArrayType[LongType] column in RAW format.
   */
  @Test
  def testConvertCategoricalSetOfLongElementTypeToRawColumn(): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    mockTermVector.put("1", 1.0f)
    mockTermVector.put("2", 1.0f)
    mockTermVector.put("3", 1.0f)
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector)

    val retCol = FeatureValueToRawColumnConverter.convert("f1", mockFeatureValue, ArrayType(LongType), CATEGORICAL_SET)
    assertEquals(retCol.expr.dataType, ArrayType(LongType, false))
    verify(mockFeatureValue).getAsTermVector

    // Validate by applying the column on a DataFrame
    val keyField = StructField("key", StringType, nullable = false)
    val valueField = StructField("value", ArrayType(LongType))
    val validationDF = ss.createDataFrame(ss.sparkContext.parallelize(Seq(Row("key1", null))), StructType(List(keyField, valueField)))
    val retData = validationDF.withColumn("defaults", retCol).collect().head.getAs[Seq[Long]]("defaults")
    assertEquals(retData.sorted, mutable.WrappedArray.make(Array(1, 2, 3).map(_.longValue())))
  }

  /**
   * Test converting FeatureValue with type unspecified, to a ArrayType[Float] column in RAW & FDS format.
   * A data provider is used to run through all combinations.
   */
  @Test(dataProvider = "DataTypeAndConverterProvider")
  def testConvertArrayOfIntegralTypeToRawColumnAsDenseVector(dataType: DataType, converter: FeatureValueToColumnConverter): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    mockTermVector.put("term1", 1.0f)
    mockTermVector.put("term2", 2.0f)
    mockTermVector.put("term3", 3.0f)
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector)

    val retCol = converter.convert("f1", mockFeatureValue, ArrayType(dataType), UNSPECIFIED)
    assertEquals(retCol.expr.dataType, ArrayType(FloatType, false))
    verify(mockFeatureValue).getAsTermVector

    // Validate by applying the column on a DataFrame
    val keyField = StructField("key", StringType, nullable = false)
    val valueField = StructField("value", ArrayType(FloatType, false))
    val validationDF = ss.createDataFrame(ss.sparkContext.parallelize(Seq(Row("key1", null))), StructType(List(keyField, valueField)))
    val retData = validationDF.withColumn("defaults", retCol).collect().head.getAs[Seq[Float]]("defaults")
    assertEquals(retData.sorted, mutable.WrappedArray.make(Array(1.0f, 2.0f, 3.0f)))
  }

  /**
   * Test converting CATEGORICAL_SET type FeatureValue to a ArrayType[StringType] column in RAW format.
   */
  @Test
  def testConvertCategoricalSetFieldWithStringElementTypeToRawColumn(): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    mockTermVector.put("term1", 1.0f)
    mockTermVector.put("term2", 1.0f)
    mockTermVector.put("term3", 1.0f)
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector)

    val retCol = FeatureValueToRawColumnConverter.convert("f1", mockFeatureValue, ArrayType(StringType), UNSPECIFIED)
    assertEquals(retCol.expr.dataType, ArrayType(StringType))
    verify(mockFeatureValue).getAsTermVector

    // Validate by applying the column on a DataFrame
    val keyField = StructField("key", StringType, nullable = false)
    val valueField = StructField("value", ArrayType(StringType))
    val validationDF = ss.createDataFrame(ss.sparkContext.parallelize(Seq(Row("key1", null))), StructType(List(keyField, valueField)))
    val retData = validationDF.withColumn("defaults", retCol).collect().head.getAs[Seq[String]]("defaults")
    assertEquals(retData.sorted, mutable.WrappedArray.make(Array("term1", "term2", "term3")))
  }

  /**
   * Test converting CATEGORICAL_SET type FeatureValue to a ArrayType[DateType] column throws an error.
   */
  @Test(
    dataProvider = "FeatureValueToColumnConverterProvider",
    expectedExceptions = Array(classOf[FeathrFeatureTransformationException]),
    expectedExceptionsMessageRegExp = ".*only array of float/double/string/int is supported.*")
  def testConvertCategoricalSetToUnsupportedElementTypeThrowsError(converter: FeatureValueToColumnConverter): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    mockTermVector.put("term1", 1.0f)
    mockTermVector.put("term2", 1.0f)
    mockTermVector.put("term3", 1.0f)
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector)

    converter.convert("f1", mockFeatureValue, ArrayType(DateType), CATEGORICAL_SET)
  }

  /**
   * Test converting FeatureValue to DateType column throws exception because this is unsupported.
   */
  @Test(
    dataProvider = "FeatureValueToColumnConverterProvider",
    expectedExceptions = Array(classOf[FeathrFeatureTransformationException]),
    expectedExceptionsMessageRegExp = ".*Cannot apply default value for feature.*")
  def testConvertDefaultToUnsupportedDataType(converter: FeatureValueToColumnConverter): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    mockTermVector.put("term1", 1.0f)
    mockTermVector.put("term2", 1.0f)
    mockTermVector.put("term3", 1.0f)
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector)

    converter.convert("f1", mockFeatureValue, DateType, UNSPECIFIED)
  }

  /**
   * Test converting BOOLEAN FeatureValue to Rank1 FDS Tensor format.
   */
  @Test
  def testConvertBooleanFeatureValueToFDS1dTensorFormat(): Unit = {
    val mockFeatureValue = mock[FeatureValue]
    val mockTermVector = new ju.HashMap[String, jl.Float]()
    when(mockFeatureValue.getAsTermVector).thenReturn(mockTermVector) // Return empty map for Boolean
    val fds1dTensorType: StructType = StructType(
      Seq(StructField("indices0", ArrayType(StringType, true), true), StructField("values", ArrayType(FloatType, false), true)))

    val retCol = FeatureValueToFDSColumnConverter.convert("f1", mockFeatureValue, fds1dTensorType, UNSPECIFIED)
    assertEquals(retCol.expr.dataType, fds1dTensorType)
    verify(mockFeatureValue).getAsTermVector
  }

  /**
   * Test DefaultFeatureValueToColumnConverterFactory returns FeatureValueToFDSColumnConverter by default.
   */
  @Test
  def testFactoryReturnsFDSConverterByDefault(): Unit = {
    assertEquals(DefaultFeatureValueToColumnConverterFactory.getConverter("someFeature"), FeatureValueToFDSColumnConverter)
  }

  /**
   * Test DefaultFeatureValueToColumnConverterFactory returns FeatureValueToRawColumnConverter when feature column format is RAW.
   */
  @Test
  def testFactoryReturnsRawConverterWhenSpecified(): Unit = {
    assertEquals(DefaultFeatureValueToColumnConverterFactory.getConverter("someFeature", FeatureColumnFormat.RAW), FeatureValueToRawColumnConverter)
  }

  /**
   * Test factory throws exception for unsupported feature column format. Currently, only RAW and FDS are supported.
   */
  @Test(expectedExceptions = Array(classOf[FeathrException]), expectedExceptionsMessageRegExp = ".*Cannot convert default value.*")
  def testNTVFormatIsUnsupported(): Unit = {
    DefaultFeatureValueToColumnConverterFactory.getConverter("someFeature", null)
  }
}
