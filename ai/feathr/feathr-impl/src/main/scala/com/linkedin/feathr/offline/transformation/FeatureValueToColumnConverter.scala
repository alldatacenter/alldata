package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException, FeathrFeatureTransformationException}
import com.linkedin.feathr.common.{FeatureTypes, FeatureValue}
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat._
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.typedLit
import org.apache.spark.sql.types._

import scala.collection.JavaConverters._

/**
 * An interface that can be used to convert the user specified default value to a Spark Sql column
 * so that it can be substituted for missing feature values.
 */
private[transformation] trait FeatureValueToColumnConverter {

  /**
   * Convert defaults to Spark SQL column.
   * @param featureName   the feature for which the default value is constructed as a Spark SQL Column.
   * @param defaultValue  default value specified by the user.
   * @param targetType    type of DataFrame column / field.
   * @param featureType   A map of feature name to its FeatureTypes.
   */
  def convert(featureName: String, defaultValue: FeatureValue, targetType: DataType, featureType: FeatureTypes): Column
}

/**
 * A concrete implementation of a converter that converts default parsed as FeatureValue to a Column represented in FDS format.
 */
private[offline] object FeatureValueToFDSColumnConverter extends FeatureValueToColumnConverter {

  override def convert(featureName: String, defaultValue: FeatureValue, targetDataType: DataType, featureType: FeatureTypes): Column = {
    targetDataType match {
      // If the field type is FloatType, convert the default value to rank0 FDS format.
      case _: FloatType =>
        typedLit(defaultValue.getAsNumeric.floatValue())
      // If the field type is StructType, convert the default to rank1 FDS format.
      case _: StructType =>
        val termVectors = defaultValue.getAsTermVector.asScala
        typedLit(FDS1dTensor(termVectors.keys.toSeq, termVectors.values.map(_.toFloat).toSeq))
      case _ =>
        FeatureValueToRawColumnConverter.convert(featureName, defaultValue, targetDataType, featureType)
    }
  }
}

/**
 * A concrete implementation of a converter that converts default parsed as FeatureValue to a Column represented in RAW format.
 */
private[transformation] object FeatureValueToRawColumnConverter extends FeatureValueToColumnConverter {

  override def convert(featureName: String, defaultValue: FeatureValue, targetDataType: DataType, featureType: FeatureTypes): Column = {
    targetDataType match {
      // If the field is StringType, treat it is as CATEGORICAL and get the "term".
      case _: StringType =>
        typedLit(defaultValue.getAsTermVector.keySet().toArray.head)
      case _: IntegerType =>
        // If the field is IntegerType and featureType specified is CATEGORICAL, get FeatureValue as categorical.
        if (featureType == FeatureTypes.CATEGORICAL) {
          typedLit(Integer.valueOf(defaultValue.getAsCategorical))
        } else {
          typedLit(defaultValue.getAsNumeric.intValue())
        }
      // ShortType and LongType are handled similar to IntegerType.
      case _: ShortType =>
        if (featureType == FeatureTypes.CATEGORICAL) {
          typedLit(Integer.valueOf(defaultValue.getAsCategorical).shortValue())
        } else {
          typedLit(defaultValue.getAsNumeric.shortValue())
        }
      case _: LongType =>
        if (featureType == FeatureTypes.CATEGORICAL) {
          typedLit(Integer.valueOf(defaultValue.getAsCategorical).longValue())
        } else {
          typedLit(defaultValue.getAsNumeric.longValue())
        }
      // For DoubleType, extract FeatureValue as numeric.
      case _: DoubleType =>
        typedLit(defaultValue.getAsNumeric.doubleValue())
      // For BooleanType, extract FeatureValue as boolean.
      case _: BooleanType =>
        typedLit(defaultValue.getAsBoolean)
      // For MapType, extract FeatureValue as Term Vector.
      case _: MapType =>
        typedLit(defaultValue.getAsTermVector.asScala)
      case fieldType: ArrayType =>
        fieldType.elementType match {
          // For Array of IntegralType, it is important to differentiate between CATEGORICAL_SET and DENSE_VECTOR.
          // Convert FeatureValue to CATEGORICAL_SET (by extracting "terms") only if specified by user as CATEGORICAL_SET.
          // Else, treat Array as DENSE_VECTOR.
          case _: IntegerType =>
            if (featureType == FeatureTypes.CATEGORICAL_SET) {
              // Numeric array, say [1, 9, 10], when parsed as CATEGORICAL_SET will be represented as
              // [<"1": 1.0>, <"9": 1.0>, <"10: 1.0>]
              typedLit(defaultValue.getAsTermVector.asScala.map(_._1.toInt).toSeq)
            } else {
              // else, is treated as a DENSE_VECTOR.
              typedLit(defaultValue.getAsTermVector.asScala.map(_._2.floatValue()).toSeq)
            }
          case _: LongType =>
            if (featureType == FeatureTypes.CATEGORICAL_SET) {
              typedLit(defaultValue.getAsTermVector.asScala.map(_._1.toLong).toSeq)
            } else {
              typedLit(defaultValue.getAsTermVector.asScala.map(_._2.floatValue()).toSeq)
            }
          case _: ShortType =>
            if (featureType == FeatureTypes.CATEGORICAL_SET) {
              typedLit(defaultValue.getAsTermVector.asScala.map(_._1.toShort).toSeq)
            } else {
              typedLit(defaultValue.getAsTermVector.asScala.map(_._2.floatValue()).toSeq)
            }
          case _: NumericType =>
            typedLit(defaultValue.getAsTermVector.asScala.map(_._2.floatValue()).toSeq)
          // If the elementType is StringType, then the FeatureValue is extracted as CATEGORICAL_SET.
          case _: StringType =>
            typedLit(defaultValue.getAsTermVector.asScala.keys.toSeq)
          case eType =>
            throw new FeathrFeatureTransformationException(
              ErrorLabel.FEATHR_USER_ERROR,
              s"Cannot convert Array of {$eType} to name-term-value FeatureValue," +
                s" only array of float/double/string/int is supported")
        }
      case fType =>
        throw new FeathrFeatureTransformationException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"Cannot apply default value for feature ${featureName} with column type ${fType}")
    }
  }
}

/**
 * Factory that returns the appropriate converter, based on the FeatureColumnFormat.
 */
private[transformation] object DefaultFeatureValueToColumnConverterFactory {

  /**
   * Get converter to convert parsed default feature value to DataFrame column.
   * Assume FeatureColumnFormat = FDS_TENSOR, if not specified.
   * @param featureName           featureName for which the converter is requested.
   * @param targetFeatureFormat   target feature format
   */
  def getConverter(featureName: String, targetFeatureFormat: FeatureColumnFormat = FDS_TENSOR): FeatureValueToColumnConverter = {
    targetFeatureFormat match {
      case FDS_TENSOR => FeatureValueToFDSColumnConverter
      case RAW => FeatureValueToRawColumnConverter
      case _ => throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Cannot convert default value for ${featureName} to ${targetFeatureFormat}")
    }
  }
}
