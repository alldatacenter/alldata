package com.linkedin.feathr.offline.util

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.common.tensor._
import com.linkedin.feathr.common.types.PrimitiveType
import com.linkedin.feathr.common.{AutoTensorizableTypes, FeatureTypeConfig, FeatureTypes}
import com.linkedin.feathr.offline.transformation.FDSConversionUtils
import org.apache.logging.log4j.{LogManager, Logger}
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

private[offline] object FeaturizedDatasetUtils {

  val FDS_1D_TENSOR_DIM = "indices0"
  val FDS_1D_TENSOR_VALUE = "values"
  val UNKNOWN_SIZE: Int = -1

  val log: Logger = LogManager.getLogger(getClass)
  // Constant for the Quince-FDS DataType for a literally-converted term-vector (NTV) feature.
  val DENSE_VECTOR_FDS_TENSOR_TYPE: TensorType =
    new TensorType(TensorCategory.DENSE, PrimitiveType.FLOAT, List(PrimitiveDimensionType.INT.withShape(UNKNOWN_SIZE).asInstanceOf[DimensionType]).asJava, null)
  val DENSE_VECTOR_FDS_DATA_TYPE: DataType =
    FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(DENSE_VECTOR_FDS_TENSOR_TYPE)

  val TERM_VECTOR_FDS_DATA_TYPE: StructType = StructType(
    Seq(
      StructField(FDS_1D_TENSOR_DIM, ArrayType(StringType, containsNull = false), nullable = false),
      StructField(FDS_1D_TENSOR_VALUE, ArrayType(FloatType, containsNull = false), nullable = false)))

  /**
   * For a given DataFrame Row that conforms exactly to the com.linkedin.events.common.proml.Tensor Avro schema,
   * parse this Row into a TensorData object.
   *
   * For now, it only supports sparse tensors.
   *
   * @param row DataFrame Row that conforms to com.linkedin.events.common.proml.Tensor Avro schema
   * @return the TensorData object
   */
  def parseAvroTensorToTensorData(row: Row): TensorData = {
    row match {
      case Row(sparse: Row, _@_*) =>
        sparse match {
          case Row(indices: Seq[Row], values: Row) =>
            val (dimensionLists, dimensionTypes) = indices.map {
              // The `_@_*` trick should make these patterns resilient to a situation where more fields have been added
              // to the end of this schema.
              case Row(ints: Seq[Int], _@_*) => (ints.map(int2Integer), PrimitiveType.INT)
              case Row(null, longs: Seq[Long], _@_*) => (longs.map(long2Long), PrimitiveType.LONG)
              case Row(null, null, strings: Seq[String], _@_*) => (strings, PrimitiveType.STRING)
            }.unzip
            // Now really we should always know the TensorType anyway, so shouldn't need to parse out the types for each
            // row. But TensorType's not serializable, so would be tough to pass into this function. So I'll just use
            // this approach for now, inferring what the column type should have been based on which field was set in
            // the sparse tensor Avro record.
            val (valueList, valueType) = values match {
              case Row(null, null, doubles: Seq[Double], _@_*) => (doubles.map(double2Double), PrimitiveType.DOUBLE)
              case Row(null, null, null, floats: Seq[Float], _@_*) => (floats.map(float2Float), PrimitiveType.FLOAT)
              case Row(null, null, null, null, ints: Seq[Int], _@_*) => (ints.map(int2Integer), PrimitiveType.INT)
              case Row(null, null, null, null, null, longs: Seq[Long], _@_*) => (longs.map(long2Long), PrimitiveType.LONG)
              case Row(null, null, null, null, null, null, strings: Seq[String], _@_*) => (strings, PrimitiveType.STRING)
            }
            new LOLTensorData((dimensionTypes :+ valueType).toArray, dimensionLists.asInstanceOf[Seq[Seq[_]]].map(_.asJava).asJava, valueList.asJava, false)
        }
      case Row(null, _: Row, _@_*) =>
        throw new UnsupportedOperationException()
    }
  }

  /**
   * For a given Quince TensorData, converts the tensor into its Quince-FDS representation, which will be either a
   * primitive value or Spark Row (struct) indices and values arrays.
   *
   * @param tensor the Quince TensorData
   * @return the Quince-FDS struct or primitive
   *
   */
  def tensorToDataFrameRow(tensor: TensorData, targetDataType: Option[DataType] = None): Any = {
    tensor match {
      case null => null
      case _ =>
        // FDSDenseTensorWrapper can be dense tensor but is not of DenseTensor type
        // TODO(PROML-19908): FDSDenseTensorWrapper should be a subclass of DenseTensor
        val isDenseTensor = tensor.isInstanceOf[DenseTensor] ||
          (targetDataType.isDefined && targetDataType.get.isInstanceOf[ArrayType])
        if (isDenseTensor) {
          val iter = tensor.iterator
          val arity = tensor.getArity
          val dimensionSize = arity - 1
          if (dimensionSize > 1) {
            throw new FeathrException(ErrorLabel.FEATHR_ERROR,
              "DenseTensor only support 1D but not higher now. The dimension size is: " + dimensionSize)
          }
          val valueColumnIndex = arity - 1
          val valueType = tensor.getTypes()(valueColumnIndex)
          val array = new ArrayBuffer[Any](tensor.cardinality())

          iter.start()
          while (iter.isValid) {
            valueType.getRepresentation match {
              case Primitive.INT => array += iter.getInt(valueColumnIndex)
              case Primitive.LONG => array += iter.getLong(valueColumnIndex)
              case Primitive.FLOAT => array += iter.getFloat(valueColumnIndex)
              case Primitive.DOUBLE => array += iter.getDouble(valueColumnIndex)
              case Primitive.STRING => array += iter.getString(valueColumnIndex)
              case Primitive.BOOLEAN => array += iter.getBoolean(valueColumnIndex)
            }
            iter.next()
          }
          array
        } else {
          val iter = tensor.iterator
          val arity = tensor.getArity
          val colTypes = tensor.getTypes
          val arrays = colTypes.map { _ =>
            new ArrayBuffer[Any](tensor.cardinality())
          }

          iter.start()
          while (iter.isValid) {
            for (i <- 0 until arity) {
              colTypes(i).getRepresentation match {
                case Primitive.INT => arrays(i) += iter.getInt(i)
                case Primitive.LONG => arrays(i) += iter.getLong(i)
                case Primitive.FLOAT => arrays(i) += iter.getFloat(i)
                case Primitive.DOUBLE => arrays(i) += iter.getDouble(i)
                case Primitive.STRING => arrays(i) += iter.getString(i)
                case Primitive.BOOLEAN => arrays(i) += iter.getBoolean(i)
              }
            }
            iter.next()
          }

          if (colTypes.length == 1) {
            // primitive (rank-0 tensor)
            arrays(0)(0)
          } else {
            Row.fromSeq(arrays)
          }
        }
    }
  }



  /**
   * Converts a DataFrame containing feature columns that is in 'raw' format (see

   * into a similar DataFrame with the same column names, but whose feature columns have been converted into the
   * FDS format.
   * This function takes a special map from feature column name to (feature name, simple feature type) as parameter.
   * It is used during the feature transformation where the 'header' is not ready.
   *
   * @param inputRawDF joined 'raw' format dataframe
   * @param featureColNameToFeatureNameAndType feature column name map to (feature name, simple feature type)
   * @return FDS formatted dataframe
   */
  def convertRawDFtoQuinceFDS(inputRawDF: DataFrame, featureColNameToFeatureNameAndType: Map[String, (String, FeatureTypeConfig)]): DataFrame = {
    val (newStructType, featureColumnsToConvert) = getFDSSchema(inputRawDF, featureColNameToFeatureNameAndType)
    val encoder = RowEncoder(newStructType)
    val schema = inputRawDF.schema
    val allIndicesToConvert = featureColNameToFeatureNameAndType.map(pair => schema.fieldIndex(pair._1)).toSet

    val outputDf = inputRawDF.map(row => {
      Row.fromSeq(schema.indices.map { i =>
        {
          if (allIndicesToConvert.contains(i)) {
            val rawFeatureValue = row.get(i)
            FDSConversionUtils.rawToFDSRow(rawFeatureValue, featureColumnsToConvert(i)._2)
          } else {
            row.get(i)
          }
        }
      })
    })(encoder)
    outputDf
  }

  /**
   * Try to guess the feature type when the user does not config it explicitly
   * @param featureType feature type that user specified or UNSPECIFIED
   * @param colDataType dataframe column type
   * @return inferred feature type
   */
  def inferFeatureType(featureType: FeatureTypes, colDataType: Option[DataType]): FeatureTypes = {
    if (featureType == FeatureTypes.UNSPECIFIED && colDataType.isDefined) {
      inferFeatureTypeFromColumnDataType(colDataType.get)
    } else featureType
  }

  /**
   * Infer the feature type from a dataframe column type
   * datatype -> feature type mapping rules:
   *
   * boolean -> FeatureTypes.BOOLEAN
   * numeric -> FeatureTypes.NUMERIC
   * string -> FeatureTypes.CATEGORICAL
   * array[numeric]  -> FeatureTypes.DENSE_VECTOR
   * array[others] -> FeatureTypes.CATEGORICAL_SET
   *
   * default is FeatureTypes.TERM_VECTOR
   * @param dataType datatype that has the "raw" feature value
   * @return inferred feature type according to the "raw" feature value datatype
   */
  def inferFeatureTypeFromColumnDataType(dataType: DataType): FeatureTypes = {
    dataType match {
      case _: BooleanType => FeatureTypes.BOOLEAN
      case _: NumericType => FeatureTypes.NUMERIC
      case _: StringType => FeatureTypes.CATEGORICAL
      case dType: ArrayType =>
        if (dType.elementType.isInstanceOf[NumericType]) FeatureTypes.DENSE_VECTOR
        else FeatureTypes.CATEGORICAL_SET
      case _ => FeatureTypes.TERM_VECTOR
    }
  }

  /**
   * Lookup TensorType for a given feature name.
   * Return the AUTO-Tensorized Tensor Type, which relies on the simple feature types and the data column type
   * @param featureRefStr feature reference string, e.g. feathr-f1-1-0, could also be a simple feature name f1
   * @param featureTypeConfig feature type config of this feature. It can be inferred or provided by the user.
   * @param colDataType the column datatype of the feature. With Spark sql-based feature, we will be able to get the
   *                    datatype of the result column, but in MVEL-based feature, we will not have this information.
   *                    This is used when featureType is unspecified in the feature definition.
   * @return
   */
  def lookupTensorTypeForFeatureRef(
      featureRefStr: String,
      colDataType: Option[DataType],
      featureTypeConfig: FeatureTypeConfig = FeatureTypeConfig.UNDEFINED_TYPE_CONFIG): TensorType = {
    // Used the refined datatype to get getDefaultTensorType, or derived feature A*2 will fail where A is a numeric feature
    val refinedFeatureType = inferFeatureType(featureTypeConfig.getFeatureType, colDataType)
    val resolvedType = lookupTensorTypeForFeatureRef(featureRefStr, refinedFeatureType, featureTypeConfig)
    // Add explicit coercion to DENSE category for scalar shapes to comply with FDS spec
    if (resolvedType.getShape.isEmpty) {
      new TensorType(TensorCategory.DENSE, resolvedType.getValueType, resolvedType.getDimensionTypes, resolvedType.getDimensionNames)
    } else {
      resolvedType
    }
  }

  def lookupTensorTypeForFeatureRef(featureRefStr: String, featureType: FeatureTypes, featureTypeConfig: FeatureTypeConfig): TensorType = {
    // For backward-compatibility, we are using following order to determine the tensor type:
    //   1. Use tensor type specified in the config,
    //   2. If not, then use get auto-tensorized tensor type.
    val simpleType = if (featureTypeConfig.getFeatureType != FeatureTypes.UNSPECIFIED) {
      featureTypeConfig.getFeatureType
    } else {
      featureType
    }
    val autoTzTensorTypeOpt = AutoTensorizableTypes.getDefaultTensorType(simpleType)

    val tensorType = if (simpleType == FeatureTypes.DENSE_VECTOR) {
      DENSE_VECTOR_FDS_TENSOR_TYPE
    } else if (featureTypeConfig.hasTensorType) {
      featureTypeConfig.getTensorType
    } else if (autoTzTensorTypeOpt.isPresent) {
      autoTzTensorTypeOpt.get()
    } else throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Cannot get tensor type for ${featureRefStr} with type ${featureType}")
    tensorType
  }

  def lookupTensorTypeForNonFMLFeatureRef(featureRefStr: String, featureType: FeatureTypes, featureTypeConfig: FeatureTypeConfig): TensorType = {
    // For backward-compatibility, we are using following order to dertermin the tensor type:
    //   1. always use FML metadata for tensor type,
    //   2. then use tensor type specified in the config,
    //   3. then use get auto-tensorized tensor type.
    val autoTzTensorTypeOpt = AutoTensorizableTypes.getDefaultTensorType(featureType)

    val tensorType = if (featureType == FeatureTypes.DENSE_VECTOR) {
      DENSE_VECTOR_FDS_TENSOR_TYPE
    } else if (featureTypeConfig.hasTensorType) {
      featureTypeConfig.getTensorType
    } else if (autoTzTensorTypeOpt.isPresent) {
      autoTzTensorTypeOpt.get()
    } else throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Cannot get tensor type for ${featureRefStr} with type ${featureType}")
    tensorType
  }


  /**
   * For a given Quince TensorData, converts the tensor into its Quince-FDS representation, which will be either a
   * primitive value or Spark Row (struct) indices and values arrays.
   * @param tensor the Quince TensorData
   * @return the Quince-FDS struct or primitive

   */
  def tensorToFDSDataFrameRow(tensor: TensorData, targetDataType: Option[DataType] = None): Any = {
    tensor match {
      case null => null
      case _ =>
        // FDSDenseTensorWrapper can be dense tensor but is not of DenseTensor type

        val isDenseTensor = tensor.isInstanceOf[DenseTensor] ||
          (targetDataType.isDefined && targetDataType.get.isInstanceOf[ArrayType])
        if (isDenseTensor) {
          val iter = tensor.iterator
          val arity = tensor.getArity
          val dimensionSize = arity - 1
          if (dimensionSize > 1) {
            throw new FeathrException(ErrorLabel.FEATHR_ERROR,
              "DenseTensor only support 1D but not higher now. The dimension size is: " + dimensionSize)
          }
          val valueColumnIndex = arity - 1
          val valueType = tensor.getTypes()(valueColumnIndex)
          val array = new ArrayBuffer[Any](tensor.cardinality())

          iter.start()
          while (iter.isValid) {
            valueType.getRepresentation match {
              case Primitive.INT => array += iter.getInt(valueColumnIndex)
              case Primitive.LONG => array += iter.getLong(valueColumnIndex)
              case Primitive.FLOAT => array += iter.getFloat(valueColumnIndex)
              case Primitive.DOUBLE => array += iter.getDouble(valueColumnIndex)
              case Primitive.STRING => array += iter.getString(valueColumnIndex)
              case Primitive.BOOLEAN => array += iter.getBoolean(valueColumnIndex)
            }
            iter.next()
          }
          array
        } else {
          val iter = tensor.iterator
          val arity = tensor.getArity
          val colTypes = tensor.getTypes
          val arrays = colTypes.map { _ =>
            new ArrayBuffer[Any](tensor.cardinality())
          }

          iter.start()
          while (iter.isValid) {
            for (i <- 0 until arity) {
              colTypes(i).getRepresentation match {
                case Primitive.INT => arrays(i) += iter.getInt(i)
                case Primitive.LONG => arrays(i) += iter.getLong(i)
                case Primitive.FLOAT => arrays(i) += iter.getFloat(i)
                case Primitive.DOUBLE => arrays(i) += iter.getDouble(i)
                case Primitive.STRING => arrays(i) += iter.getString(i)
                case Primitive.BOOLEAN => arrays(i) += iter.getBoolean(i)
              }
            }
            iter.next()
          }

          if (colTypes.length == 1) {
            // primitive (rank-0 tensor)
            arrays(0)(0)
          } else {
            Row.fromSeq(arrays)
          }
        }
    }
  }

  /**
   * For a given Quince TensorType, produce a Spark DataType schema following the Quince-FDS convention
   *
   * @param tensorType Quince TensorType
   * @return constructed Spark DataType
   */
  def tensorTypeToDataFrameSchema(tensorType: TensorType): DataType = {
    @tailrec
    def getNestArrays(dataType: DataType, dimensionNum: Int): DataType = {
      if (dimensionNum == 0) {
        dataType
      } else {
        // Dense tensor does not allow null element
        getNestArrays(ArrayType(dataType, containsNull = false), dimensionNum - 1)
      }
    }
    val columnTypes = tensorType.getColumnTypes
    if (columnTypes.size == 1) {
      getNestArrays(columnTypeToPrimitiveSchema(columnTypes.head), 0)
    } else if (TensorCategory.SPARSE == tensorType.getTensorCategory) {
      val dimensionFields = columnTypes.init.zipWithIndex map {
        case (representable, position) =>
          StructField("indices" + position, ArrayType(columnTypeToPrimitiveSchema(representable), containsNull = false), nullable = false)
      }
      val valueField = StructField("values", ArrayType(columnTypeToPrimitiveSchema(columnTypes.last), containsNull = false), nullable = false)
      StructType(dimensionFields :+ valueField)
    } else if (TensorCategory.DENSE == tensorType.getTensorCategory) {
      val dimensionNum = tensorType.getDimensionTypes.size
      getNestArrays(columnTypeToPrimitiveSchema(columnTypes.last), dimensionNum)
    } else {
      throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Tensor type ${tensorType} cannot be converted to DataFrame schema")
    }
  }


  private def columnTypeToPrimitiveSchema(representable: Representable): DataType = representable match {
    case Primitive.INT => IntegerType
    case Primitive.LONG => LongType
    case Primitive.FLOAT => FloatType
    case Primitive.DOUBLE => DoubleType
    case Primitive.STRING => StringType
    case Primitive.BOOLEAN => BooleanType
  }

  /**
   * Get Quince-FDS format schema for the input dataframe with features indicated in featureColNameToFeatureNameAndType
   *
   * @param inputDF                                  input dataframe with features
   * @param featureColNameToFeatureNameAndTypeConfig map feature column name to (feature name, simple feature type config)
   * @return (schema for converted dataframe, map from feature column index to (column name, feature column schema))
   */
  private[feathr] def getFDSSchema(
      inputDF: DataFrame,
      featureColNameToFeatureNameAndTypeConfig: Map[String, (String, FeatureTypeConfig)] = Map()): (StructType, Map[Int, (String, DataType)]) = {
    val featureColumnsToConvert: Map[Int, (String, DataType)] = featureColNameToFeatureNameAndTypeConfig.map {
      case (colName, (featureName, featureTypeConfig)) =>
        val colPos = inputDF.schema.fieldIndex(colName)
        val colType = Some(inputDF.schema.fields(colPos).dataType)
        val tensorType = lookupTensorTypeForFeatureRef(featureName, colType, featureTypeConfig)
        val newSchema = tensorTypeToDataFrameSchema(tensorType)
        (colPos, (colName, newSchema))
    }
    val newDfSchemaFields: Array[StructField] = inputDF.schema.zipWithIndex.map {
      case (structField, pos) =>
        if (featureColumnsToConvert.contains(pos)) {
          val newColumnSchema = featureColumnsToConvert(pos)._2
          StructField(structField.name, newColumnSchema, structField.nullable, structField.metadata)
        } else {
          structField
        }
    }.toArray
    (StructType(newDfSchemaFields), featureColumnsToConvert)
  }
}
