package com.linkedin.feathr.offline.util

import com.linkedin.feathr.common.TensorUtils
import com.linkedin.feathr.common.tensor.{TensorType, Tensors}
import com.linkedin.feathr.common.types.PrimitiveType
import com.linkedin.feathr.offline.AssertFeatureUtils
import com.linkedin.feathr.offline.transformation.FDSConversionUtils
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.{GenericRow, GenericRowWithSchema}
import org.apache.spark.sql.types._
import org.scalatest.testng.TestNGSuite
import org.testng.Assert.{assertEquals, assertTrue}
import org.testng.annotations.{DataProvider, Test}

import java.util
import java.util.Collections
import scala.collection.mutable

class TestFDSConversionUtil extends TestNGSuite {

  val EMPTY_STRING = ""
  val EXPECTED_DENSE_VECTOR_FDS_DATA_TYPE = ArrayType(FloatType, false)

  @Test(description = "Verifies that dense tensor does not allow null element")
  def testFDSTensorSchemas(): Unit = {
    assertEquals(EXPECTED_DENSE_VECTOR_FDS_DATA_TYPE, FeaturizedDatasetUtils.DENSE_VECTOR_FDS_DATA_TYPE)
  }

  @DataProvider
  def dataForTestRawToDFSRow(): Array[Array[Any]] = {
    Array(
      // Auto-Tz Scalar
      Array("foo", StringType, "foo"),
      Array(1.5, FloatType, 1.5f),
      Array(5, FloatType, 5),
      Array(true, BooleanType, true),
      // Explicit tz scalar
      Array(123L, LongType, 123L),
      Array(1.77D, DoubleType, 1.77D),
      // auto-Tz 1D dense tensor
      Array(Map("1" -> 11.0f, "0" -> 10.0f), FeaturizedDatasetUtils.DENSE_VECTOR_FDS_DATA_TYPE, Array(10.0f, 11.0f)),
      Array(List(1.0, 2.0), FeaturizedDatasetUtils.DENSE_VECTOR_FDS_DATA_TYPE, Array(1.0f, 2.0f)),
      // explicit tz 1D dense tensor
      Array(List(true, false),
        FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(TensorUtils.parseTensorType("TENSOR<DENSE>[INT]:BOOLEAN")), Array(true, false)),
      Array(List(1, 2),
        FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(TensorUtils.parseTensorType("TENSOR<DENSE>[INT]:INT")), Array(1, 2)),
      Array(List("x", "y"),
        FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(TensorUtils.parseTensorType("TENSOR<DENSE>[INT]:STRING")), Array("x", "y")),
      // Auto-tz supported maps. The result should be sorted accordingly to dimension
      Array(Map(EMPTY_STRING -> 1.0f), FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE, Row.fromSeq(Array(Array(EMPTY_STRING), Array(1.0f)))),
      Array(Map("2" -> 2.0f, "1" -> 1.0f), FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE, Row.fromSeq(Array(Array("1", "2"), Array(1.0f, 2.0f)))),
      // Explicit tz maps. The result should be sorted accordingly to dimension
      Array(
        Map(1 -> true, 5 -> false, 3 -> true),
        FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(TensorUtils.parseTensorType("TENSOR<SPARSE>[INT]:BOOLEAN")),
        Row.fromSeq(Array(Array(1, 3, 5), Array(true, true, false)))),
      Array(
        Map("y" -> true, "x" -> false, "z" -> false),
        FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(TensorUtils.parseTensorType("TENSOR<SPARSE>[STRING]:BOOLEAN")),
        Row.fromSeq(Array(Array("x", "y", "z"), Array(false, true, false)))),
      Array(
        Map(1 -> 1, 5 -> 5, 3 -> 3),
        FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(TensorUtils.parseTensorType("TENSOR<SPARSE>[INT]:INT")),
        Row.fromSeq(Array(Array(1, 3, 5), Array(1, 3, 5)))),
      // TensorData
      Array(
        Tensors.asScalarTensor(new TensorType(PrimitiveType.LONG, Collections.emptyList()), 1L),
        FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(TensorUtils.parseTensorType("TENSOR<SPARSE>[INT]:LONG")),
        1L)
    )
  }

  @Test(description = "Test converting a raw value into FDS", dataProvider = "dataForTestRawToDFSRow")
  def testRawToDFSRow(rawVal: Any, targetType: DataType, expectedRow: Any): Unit = {
    val inputRow = FDSConversionUtils.rawToFDSRow(rawVal, targetType)
    inputRow match {
      case row: Number =>
        assertEquals(row.doubleValue(), expectedRow.asInstanceOf[Number].doubleValue(), 0.000001)
      case row: GenericRow =>
        assertTrue(AssertFeatureUtils.rowApproxEquals(row.asInstanceOf[GenericRow], expectedRow.asInstanceOf[GenericRow]))
      case row =>
        assertEquals(row, expectedRow)
    }
  }

  @DataProvider
  def dataForTestEqualsIgnoreNullability(): Array[Array[Any]] = {
    Array(
      // exactly the same
      Array(FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE, FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE, true),
      // only nullability is different
      Array(
        FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE,
        StructType(
          Seq(
            StructField(FeaturizedDatasetUtils.FDS_1D_TENSOR_DIM, ArrayType(StringType, containsNull = true), nullable = true),
            StructField(FeaturizedDatasetUtils.FDS_1D_TENSOR_VALUE, ArrayType(FloatType, containsNull = true), nullable = true))),
        true),
      // field names are different
      Array(
        FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE,
        StructType(
          Seq(
            StructField("index", ArrayType(StringType, containsNull = true), nullable = true),
            StructField(FeaturizedDatasetUtils.FDS_1D_TENSOR_VALUE, ArrayType(FloatType, containsNull = true), nullable = true))),
        false),
      // field types are different
      Array(
        FeaturizedDatasetUtils.TERM_VECTOR_FDS_DATA_TYPE,
        StructType(
          Seq(
            StructField(FeaturizedDatasetUtils.FDS_1D_TENSOR_DIM, ArrayType(StringType, containsNull = false), nullable = false),
            StructField(FeaturizedDatasetUtils.FDS_1D_TENSOR_VALUE, ArrayType(IntegerType, containsNull = false), nullable = false))),
        false))
  }

  @Test(dataProvider = "dataForTestEqualsIgnoreNullability")
  def testEqualsIgnoreNullability(schema: StructType, otherSchema: StructType, result: Boolean): Unit = {
    assertEquals(FDSConversionUtils.equalsIgnoreNullability(schema, otherSchema), result)
  }

  @DataProvider
  def dataForTestConvertMapToFDS(): Array[Array[Any]] = {
    Array(
      Array(Map("a" -> 1.1), StringType, StringType, util.Arrays.asList(util.Arrays.asList("a").toArray, util.Arrays.asList(1.1).toArray).toArray),
      Array(Map(1 -> 1.1), StringType, IntegerType, util.Arrays.asList(util.Arrays.asList("1").toArray, util.Arrays.asList(1).toArray).toArray),
      Array(Map(1 -> 123), StringType, LongType, util.Arrays.asList(util.Arrays.asList("1").toArray, util.Arrays.asList(123L).toArray).toArray),
      Array(Map(1 -> true), StringType, BooleanType, util.Arrays.asList(util.Arrays.asList("1").toArray, util.Arrays.asList(true).toArray).toArray),
      Array(Map(1 -> 1.1), IntegerType, StringType, util.Arrays.asList(util.Arrays.asList(1).toArray, util.Arrays.asList(1.1).toArray).toArray),
      Array(Map(1 -> 1.1), IntegerType, IntegerType, util.Arrays.asList(util.Arrays.asList(1).toArray, util.Arrays.asList(1).toArray).toArray),
      Array(Map(1 -> 123), IntegerType, LongType, util.Arrays.asList(util.Arrays.asList(1).toArray, util.Arrays.asList(123L).toArray).toArray),
      Array(Map(1 -> true), IntegerType, BooleanType, util.Arrays.asList(util.Arrays.asList(1).toArray, util.Arrays.asList(true).toArray).toArray)
    )
  }
  @Test(dataProvider = "dataForTestConvertMapToFDS")
  def testConvertMapToFDS(input: Map[String, Double], dimType: DataType, valType: DataType, expectedArray: Array[_]): Unit = {
    assertEquals(FDSConversionUtils.convertMapToFDS(input, valType, dimType), expectedArray)
  }

  @DataProvider
  def dataForTestConvertRawValueTo1DFDSDenseTensorRowTz(): Array[Array[Any]] = {
    val eleType = StructType(
        StructField("group", IntegerType, false) ::
        StructField("value", IntegerType, false) :: Nil
      )
    val row1 = new GenericRowWithSchema(Array(1, 3), eleType)
    val row2 = new GenericRowWithSchema(Array(2, 4), eleType)
    Array(
      Array(mutable.WrappedArray.make(Array(2.0f, 6.0f)), util.Arrays.asList(2.0f, 6.0f).toArray),
      Array(Array(1.1).toList, util.Arrays.asList(1.1).toArray),
      Array(Map("a" -> 1.1), util.Arrays.asList(1.1).toArray),
      // Simulate raw value return by SWA feature with groupBy
      Array(mutable.WrappedArray.make(Array(row1, row2)), util.Arrays.asList(3, 4).toArray)
    )
  }
  @Test(dataProvider = "dataForTestConvertRawValueTo1DFDSDenseTensorRowTz")
  def testConvertRawValueTo1DFDSDenseTensorRowTz(input: Any, expected: Array[_]): Unit = {
    assertEquals(FDSConversionUtils.convertRawValueTo1DFDSDenseTensorRowTz(input), expected)
  }
}
