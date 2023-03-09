package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.common.tensor.TensorData
import com.linkedin.feathr.common.util.CoercionUtils
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types._

import java.util
import scala.collection.JavaConverters._
import scala.collection.convert.Wrappers.JMapWrapper
import scala.collection.mutable

/**
 * Utilities to convert FDS format to other formats
 */
private[offline] object FDSConversionUtils {
  val trueValueMap = JMapWrapper[String, Float](Map("" -> 1.0f).asJava)
  /**
   * Convert a "raw" value to a FDS row data. Used in auto-tensorizing features.
   * The value could be the result from SQL/MVEL features or SWA features



   * for better performance. Also, research replacing this with the new FeatureValue interface/formatter.

   *
   * @param rawFeatureValue raw value, e.g. integer, float, array, map, list, etc.
   * @param targetDataType FDS expected/target column datatype
   * @return Tensor data in FDS format
   */
  def rawToFDSRow(rawFeatureValue: Any, targetDataType: DataType): Any = {
    if (rawFeatureValue == null) return null
    // convert the "raw" input data into a FDS column a specific dataType
    rawFeatureValue match {
      case tensorData: TensorData =>
        FeaturizedDatasetUtils.tensorToFDSDataFrameRow(tensorData, Some(targetDataType))
      case _ =>
        targetDataType match {
          // Scalar tensor
          case IntegerType => parseIntValue(rawFeatureValue)
          case LongType => parseLongValue(rawFeatureValue)
          case StringType => rawFeatureValue.toString
          case FloatType => parseFloatValue(rawFeatureValue)
          case DoubleType => parseDoubleValue(rawFeatureValue)
          case BooleanType => parseBooleanValue(rawFeatureValue)
          // 1D sparse tensor
          case targetType: StructType if targetType.fields.size == 2 =>
            convertRawValueTo1DFDSSparseTensorRow(rawFeatureValue, targetType)
          // 1D dense tensor
          case targetType: ArrayType if !targetType.elementType.isInstanceOf[ArrayType] =>
            convertRawValueTo1DFDSDenseTensorRow(rawFeatureValue, targetType)
          case otherType =>
            throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Converting ${rawFeatureValue} to FDS Tensor type " +
              s"${otherType} is not supported. Only 0-D and 1-D tensor is supported.")
        }
    }
  }

  /**
   * Parse a scalar value into Long.
   * @throws FeathrException exception if it's not supported scalar data.
   */
  private def parseLongValue(input: Any): Long = {
    input match {
      case value: Number => value.longValue
      case _ => throw new FeathrException(ErrorLabel.FEATHR_ERROR, "Provided data " + input + " can't be converted to Long.")
    }
  }

  /**
   * Parse a scalar value into Double.
   * @throws FeathrException exception if it's not supported scalar data.
   */
  private def parseDoubleValue(input: Any): Double = {
    input match {
      case value: Number => value.doubleValue()
      case _ => throw new FeathrException(ErrorLabel.FEATHR_ERROR, "Provided data " + input + " can't be converted to Double.")
    }
  }


  private def parseIntValue(input: Any): Int = {
    input match {
      case value: Float => value.toInt
      case value: Int => value
      case value: Long => value.toInt
      case value: Double => value.toInt
      case value: Number => value.intValue()
      case value =>
        if (value.isInstanceOf[Map[_, _]]) {
          value.asInstanceOf[Map[_, _]].values.head.asInstanceOf[Float].toInt
        } else {
          value.toString.toInt
        }
    }
  }

  /**
   * parse float value from a input raw value
   * Auto-tensorized datatype uses float to represent numeric feature value,
   * while the actual input raw value type returned by SQL/MVEL expression
   * could be any numeric value type, e.g. int/long/float/double, etc.
   */
  private def parseFloatValue(input: Any): Any = {
    input match {
      case value: Float => value
      case value: Int => value.toFloat
      case value: Long => value.toFloat
      case value: Double => value.toFloat
      case value: Number => value.floatValue()
      case value =>
        if (value.isInstanceOf[Map[_, _]]) {
          value.asInstanceOf[Map[_, _]].values.head.asInstanceOf[Float]
        } else if (value.isInstanceOf[GenericRowWithSchema]) { // convert FDS 1d tensor to num
          // MVEL anchored feature could return List of empty maps, which is then converted to struct of empty arrays
          // in FDS format. There code here will handle the case where user want to cast the empty feature value to a
          // numeric feature. In such cases, they expect to get null in the end.
          val nums = value.asInstanceOf[GenericRowWithSchema].getAs[Seq[Number]](FeaturizedDatasetUtils.FDS_1D_TENSOR_VALUE)
          if (nums.nonEmpty) {
            nums.head.floatValue()
          } else null
        } else if (value.isInstanceOf[JMapWrapper[String, Number]]) {
          // Derived feature could return a map of string to float, which is then converted to JMapWrapper in dataframe.
          val nums = value.asInstanceOf[JMapWrapper[String, Number]]
          if (nums.nonEmpty) {
            nums.head._2.floatValue()
          } else null
        } else {
          value.toString.toFloat
        }
    }
  }

  /**
   * Parse supported data into boolean. We can convert boolean into boolean as-is or convert the NTV representation
   * into boolean. For NTV representation, Map(""->1.0f) is true, and Map() is false.
   */
  private[feathr] def parseBooleanValue(input: Any): Boolean = {
    input match {
      case value: Boolean => value
      case javaMap: JMapWrapper[String, Number] =>
        if (javaMap.isEmpty) {
          false
        } else if (javaMap.equals(trueValueMap)) {
          true
        } else {
          throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, s"Converting Map[String, Float] to boolean. Only empty " +
            s"map(false) or map from empty string to 1.0(true) is supported. But the map is: ${javaMap}")
        }
      case _ =>
        throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, s"Can not convert unsupported data type ${input.getClass} " +
          s"for data ${input} into boolean. Only boolean or Map[String, Float] is supported.")
    }
  }

  // covert a map returned by sql/mvel to FDS Row
  private[feathr] def convertMapToFDS(mapValues: Map[_, _], valType: DataType, dimType: DataType): Array[Array[_]] = {
    val items = mapValues.toSeq
    val dimensions = dimType match {
      case _ : StringType =>
        // For StringType, we can try to do some basic coercion
        items.map(item => item._1.toString).toArray
      case _: IntegerType =>
        items.map(item => item._1.asInstanceOf[Integer]).toArray
      case _: LongType =>
        items.map(item => item._1.asInstanceOf[java.lang.Long]).toArray
      case _: BooleanType =>
        items.map(item => item._1.asInstanceOf[java.lang.Boolean]).toArray
      case _ =>
        throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Converting ${mapValues} to FDS Tensor type with dimension " +
          s"type of ${dimType} is not supported")
    }
    val values = valType match {
      case _: IntegerType =>
        items.map(value => parseIntValue(value._2)).toArray
      case _: LongType =>
        items.map(value => parseLongValue(value._2)).toArray
      case _: StringType =>
        items.map(value => value._2).toArray
      case _: FloatType =>
        items.map(value => parseFloatValue(value._2).asInstanceOf[Float]).toArray
      case _: DoubleType =>
        items.map(value => parseDoubleValue(value._2)).toArray
      case _: BooleanType =>
        items.map(value => value._2).toArray
      case _ =>
        throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Converting ${mapValues} to FDS Tensor type with value type " +
          s"of ${valType} is not supported")
    }
    Array(dimensions, values)
  }

  // covert an array returned by sql/mvel/SWA to FDS Row
  private def convertRawArrayToFDS1dTensor(values: Array[_], dataType: DataType, dimType: DataType): Array[Array[_]] = {
    val size = values.size
    if (size == 0) {
      Array(Array(), Array())
    } else {
      values.head match {
        case _: GenericRowWithSchema =>
          // converting SWA feature with grouping
          val termValues = values.map { ele =>
            val row = ele.asInstanceOf[GenericRowWithSchema]
            (row.get(0).toString, row.getAs[Number](1).floatValue())
          }
          val (terms, vals) = (termValues.map(_._1), termValues.map(_._2))
          Array(terms.asInstanceOf[Array[_]], vals.asInstanceOf[Array[_]])
        case _: Number =>
          // converting an array of number to FDS 1D sparse tensor
          val dims = values.map(num => CoercionUtils.safeToString(num))
          Array(dims.asInstanceOf[Array[_]], Array.fill(size)(1.0f).asInstanceOf[Array[_]])
        case _: util.Map[_, _] =>
          // converting an list of map to FDS 1D sparse tensor
          val flattenMap = values.flatMap(map => map.asInstanceOf[util.Map[Any, Any]].asScala.toMap).toMap
          convertMapToFDS(flattenMap, dataType, dimType)
        case _ =>
          Array(values.map(_.toString), Array.fill(size)(1.0f))
      }
    }
  }

  // covert a rawFeatureValue returned by sql/mvel to FDS Row
  // For autoTz, the value is always Float. So we try our best to convert/coerce into Float
  private def convertRawValueTo1DFDSDenseTensorRowAutoTz(rawFeatureValue: Any): Array[_] = {
    rawFeatureValue match {
      case values: util.ArrayList[Number] =>
        values.asScala.map(_.floatValue()).toArray
      case values: mutable.WrappedArray[Number] =>
        values.toArray.map(_.floatValue())
      case values: List[Number] =>
        values.toArray.map(_.floatValue())
      case mapValues: Map[String, Number] =>
        convertMapTo1dDenseTensor(mapValues)
      case mapValues: JMapWrapper[String, Number] =>
        convertMapTo1dDenseTensor(mapValues.toMap)
      case mapValues: util.Map[String, Number] =>
        convertMapTo1dDenseTensor(mapValues.asScala.toMap)
      case _ =>
        throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Converting ${rawFeatureValue} of type ${rawFeatureValue.getClass} " +
          s"to FDS Tensor is not yet supported")
    }
  }

  // covert a rawFeatureValue returned by sql/mvel to FDS Row
  // For tensorization, the value differs. So we do not coerce. Users need to make sure their data type matches
  // the tensor type.
  private[feathr] def convertRawValueTo1DFDSDenseTensorRowTz(rawFeatureValue: Any): Array[_] = {
    rawFeatureValue match {
      case values: util.ArrayList[Any] =>
        values.asScala.toArray
      case values: mutable.WrappedArray[Any] =>
        if (values.nonEmpty && values(0).isInstanceOf[GenericRowWithSchema]) {
          // Assuming the result is returned by SWA feature with groupBy, hence keeping only the
          // feature value as an array and dropping the index info.
          values.asInstanceOf[mutable.WrappedArray[GenericRowWithSchema]].map(v => v.get(v.size - 1)).toArray
        } else {
          values.toArray
        }
      case values: List[Any] =>
        values.toArray
      case mapValues: Map[Integer, Any] =>
        convertMapTo1dDenseTensorTz(mapValues)
      case mapValues: JMapWrapper[Integer, Any] =>
        convertMapTo1dDenseTensorTz(mapValues.toMap)
      case mapValues: util.Map[Integer, Any] =>
        convertMapTo1dDenseTensorTz(mapValues.asScala.toMap)
      case _ =>
        throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Converting ${rawFeatureValue} of type ${rawFeatureValue.getClass} " +
          s"to FDS Tensor is not yet supported")
    }
  }

  // covert a rawFeatureValue returned by sql/mvel to FDS Row
  private def convertRawValueTo1DFDSDenseTensorRow(rawFeatureValue: Any, targetType: ArrayType): Array[_] = {
    targetType.elementType match {
      case _: FloatType =>
        // If it's FloatType, then we know it's autoTz rules.
        convertRawValueTo1DFDSDenseTensorRowAutoTz(rawFeatureValue)
      case _ =>
        convertRawValueTo1DFDSDenseTensorRowTz(rawFeatureValue)
    }
  }

  // covert a NTV map returned by sql/mvel to FDS Row
  private def convertMapTo1dDenseTensor(mapValues: Map[String, Number]): Array[Float] = {
    val items = mapValues.toSeq
    val sortedValues: Array[Float] = items.sortBy(item => item._1.toString.toInt).map(_._2.floatValue()).toArray
    sortedValues
  }

  // Convert a Map[Integer, Any] to 1-D sparse tensor
  private def convertMapTo1dDenseTensorTz(mapValues: Map[Integer, Any]): Array[Any] = {
    val items = mapValues.toSeq
    val sortedValues: Array[Any] = items.sortBy(item => item._1.toString).map(_._2).toArray
    sortedValues
  }

  /**
   * covert an array returned by sql/mvel to FDS Row
   * @param rawFeatureValue input value
   * @param targetType target FDS datacolumn type
   * @return FDS formatted rawFeatureValue
   */
  private def convertRawValueTo1DFDSSparseTensorRow(rawFeatureValue: Any, targetType: StructType) = {
    if (targetType.fields.size != 2) {
      throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Cannot convert ${rawFeatureValue} to FDS row of type ${targetType}")
    }
    val arrays = if (rawFeatureValue.isInstanceOf[GenericRowWithSchema] &&
        equalsIgnoreNullability(rawFeatureValue.asInstanceOf[GenericRowWithSchema].schema, targetType)) {
      rawFeatureValue.asInstanceOf[GenericRowWithSchema].toSeq.asInstanceOf[Seq[mutable.WrappedArray[_]]].map(_.toArray).toArray
    } else {
      // Converting to 1d-tensor, could be string->float, or int -> float, see AutoTensorTypes.TERM_VECTOR_TENSOR_TYPE
      // and AutoTensorTypes.DENSE_1D_FLOAT_TENSOR_TYPE
      val dimType = targetType.asInstanceOf[StructType].fields(0).dataType.asInstanceOf[ArrayType].elementType
      val valType = targetType.asInstanceOf[StructType].fields(1).dataType.asInstanceOf[ArrayType].elementType

      // handle Map raw input converting to Sparse 1-D tensor. FDS only support STRING and INT dimension
      val arrays = dimType match {
        case StringType =>
          // Converting to AutoTensorTypes.TERM_VECTOR_TENSOR_TYPE
          // Supported raw value type includes: Array of String, Array of Number, Map of String -> Number
          rawFeatureValue match {
            case values: String =>
              // input is a CATEGORICAL feature, has only one value
              Array(Array(values), Array(1.0f))
            case values: util.ArrayList[String] =>
              // input is array of string
              convertRawArrayToFDS1dTensor(values.toArray, valType, dimType)
            case mapValues: Map[_, _] =>
              // input is a map
              convertMapToFDS(mapValues, valType, dimType)
            case mapValues: JMapWrapper[_, _] =>
              convertMapToFDS(mapValues.toMap, valType, dimType)
            case mapValues: util.Map[_, _] =>
              convertMapToFDS(mapValues.asScala.toMap, valType, dimType)
            case values: Number =>
              Array(Array(CoercionUtils.safeToString(values.longValue())), Array(1.0f))
            case values: mutable.WrappedArray[Any] =>
              convertRawArrayToFDS1dTensor(values.toArray, valType, dimType)
            case _ => throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Converting ${rawFeatureValue} to FDS Tensor type ${targetType} is not supported")
          }

        case IntegerType | LongType | BooleanType =>
          // and AutoTensorTypes.DENSE_1D_FLOAT_TENSOR_TYPE
          rawFeatureValue match {
            case values: Seq[Number] =>
              val indices = values.zipWithIndex.map(_._2).toArray
              Array(indices, values.map(_.floatValue()).toArray)
            case values: util.ArrayList[Number] =>
              val indices = values.asScala.zipWithIndex.map(_._2).toArray
              Array(indices, values.asScala.map(_.floatValue()).toArray)
            case mapValues: Map[_, _] =>
              convertMapToFDS(mapValues, valType, dimType)
            case mapValues: JMapWrapper[_, _] =>
              convertMapToFDS(mapValues.toMap, valType, dimType)
            case mapValues: util.Map[_, _] =>
              convertMapToFDS(mapValues.asScala.toMap, valType, dimType)
            case record: GenericRowWithSchema =>
              val indices = record.get(0).asInstanceOf[mutable.WrappedArray[Int]].toArray
              val values = record.get(1).asInstanceOf[mutable.WrappedArray[Float]].toArray
              Array(indices, values)
            case _ =>
              throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Converting ${rawFeatureValue} to FDS Tensor type ${targetType} is not yet supported")
          }
        case _ => throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Converting ${rawFeatureValue} to FDS Tensor type ${targetType} is yet supported")
      }
      arrays.asInstanceOf[Array[Array[_]]]
    }
    // we need to sort arrays according to dimension array of the 1d sparse tensor, i.e. the first array
    val valType = targetType.asInstanceOf[StructType].fields(1).dataType.asInstanceOf[ArrayType].elementType
    val indexArray = arrays(0).toArray
    val sortedArrays = if (indexArray.nonEmpty) {
      val firstElement = indexArray.head
      val sortedArrays = firstElement match {
        case _: String =>
          // dimension is of string type
          // Auto tz case. If user does not explicitly give a valType and the the values are Numbers, auto tz logic sets
          // valType to Float and we will coerce the output to Float.
          if (valType == FloatType) {
            val dimToValArray = arrays(0).zip(arrays(1).map(_.toString.toFloat))
            val sortedArray = try {
              dimToValArray.sortBy(p => java.lang.Float.valueOf(p._1.toString))
            } catch {
              case e: Exception => dimToValArray.sortBy(p => p._1.toString)
            }
            sortedArray.unzip
          } else { // Explicit tz case
            arrays(0).zip(arrays(1)).sortBy(p => p._1.toString).unzip
          }
        case _: Int =>
          // dimension is integer type
          arrays(0).zip(arrays(1)).sortBy(p => Integer.valueOf(p._1.toString)).unzip
        case _: Long =>
          // dimension is Long type
          arrays(0).zip(arrays(1)).sortBy(p => java.lang.Long.valueOf(p._1.toString)).unzip
        case _: Boolean =>
          // dimension is Boolean type
          arrays(0).zip(arrays(1)).sortBy(p => java.lang.Boolean.valueOf(p._1.toString)).unzip
      }
      Array(sortedArrays._1, sortedArrays._2)
    } else {
      arrays
    }
    Row.fromSeq(sortedArrays)
  }

  /**
   * Compares two types, ignoring nullability of ArrayType, MapType, StructType.
   * This function is adopted from Spark core file org/apache/spark/sql/types/DataType.scala
   */
  def equalsIgnoreNullability(left: DataType, right: DataType): Boolean = {
    (left, right) match {
      case (ArrayType(leftElementType, _), ArrayType(rightElementType, _)) =>
        equalsIgnoreNullability(leftElementType, rightElementType)
      case (MapType(leftKeyType, leftValueType, _), MapType(rightKeyType, rightValueType, _)) =>
        equalsIgnoreNullability(leftKeyType, rightKeyType) &&
          equalsIgnoreNullability(leftValueType, rightValueType)
      case (StructType(leftFields), StructType(rightFields)) =>
        leftFields.length == rightFields.length &&
          leftFields.zip(rightFields).forall {
            case (l, r) =>
              l.name == r.name && equalsIgnoreNullability(l.dataType, r.dataType)
          }
      case (l, r) => l == r
    }
  }
}
