package com.linkedin.feathr.offline.util

import com.linkedin.feathr.common
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException, FeathrFeatureTransformationException}
import com.linkedin.feathr.common.featurizeddataset.SparkDeserializerFactory
import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureTypes, FeatureValue, GenericTypedTensor}
import com.linkedin.feathr.common.util.CoercionUtils
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{ArrayType, BooleanType, IntegerType, LongType, MapType, NumericType, ShortType, StringType, StructType}

import scala.collection.JavaConverters._
import scala.collection.convert.wrapAll._
import scala.collection.mutable

/**
 * Scala wrapper for CoercionUtils
 */
private[offline] object CoercionUtilsScala {

  def safeToString(item: Any): String = CoercionUtils.safeToString(item)

  def coerceToFeatureValue(item: Any): common.FeatureValue = {
    item match {
      case value: common.FeatureValue => value
      case value =>
        val featureType = CoercionUtils.getCoercedFeatureType(value)
        new common.FeatureValue(value, featureType)
    }

  }

  def coerceToFeatureValue(item: Any, featureType: FeatureTypes): common.FeatureValue = {
    item match {
      case x: common.FeatureValue => x
      case x => new common.FeatureValue(item, featureType)
    }
  }

  /**
   * Coerce a feature value into a string key for sequential join.
   *
   * Since the type is lost when data is coerced into FeatureValue, we need to make the following assumptions:
   * Any numerical value in sequential join must represent an id, and hence be coerced back as Long numbers.
   *
   * @param featureValue  a FeatureValue.
   * @return  a list of ids (as string) from the feature Value
   */
  def coerceFeatureValueToStringKey(featureValue: common.FeatureValue): Seq[String] = {
    if (featureValue.size() == 1) {
      val (term, value) = featureValue.getValue.iterator.next
      if (term.equals("") && value.isInstanceOf[Number]) { // a numeric feature
        Seq(value.toLong.toString)
      } else if (value.equals(1.0f)) { // a categorical feature
        Seq(term)
      } else if (term.equals("0")) { // a dense vector with length 1
        Seq(value.toLong.toString)
      } else {
        throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR, s"Cannot coerce FeatureValue to String keys for join: $featureValue")
      }
    } else {
      val valueSet = featureValue.getValue.values().asScala.toSet
      if (valueSet.size == 1 && valueSet.contains(1.0f)) { // a categorical set feature
        featureValue.getValue.keySet().toSeq
      } else {
        val isDenseVector = featureValue.getValue.keys.toSeq.map(_.toInt).sorted.zipWithIndex.filter(pr => pr._1 != pr._2).size == 0
        val isCategoricalSet = featureValue.getValue.values.filter(!_.equals(1.0f)).size == 0
        if (isDenseVector) {
          featureValue.getValue.toSeq.sortBy(_._1.toInt).map(_._2.toLong.toString)
        } else if (isCategoricalSet) {
          featureValue.getValue.keys.toSeq
        } else {
          throw new FeathrFeatureTransformationException(ErrorLabel.FEATHR_USER_ERROR, s"Cannot coerce FeatureValue to String keys for join: $featureValue")
        }
      }
    }
  }

  def coerceFieldToFeatureValue(row: Row, schema: StructType, fieldName: String, featureTypeConfig: FeatureTypeConfig): FeatureValue = {
    print("ROW IS " + row + " and featureTypeConfig is " + featureTypeConfig + " and feature name is " + fieldName)
    val fieldIndex = schema.fieldIndex(fieldName)
    val fieldType = schema.toList(fieldIndex)
    val valueMap = if (row.get(fieldIndex) == null) {
      null
    } else {
      // If the FeatureValue type is tensor, then just use Quince's FeatureDeserializer to convert it to TensorData
      // and then FeatureValue
      if (featureTypeConfig.getFeatureType.equals(FeatureTypes.TENSOR)) {
        val tensorType = featureTypeConfig.getTensorType
        val featureDeserializer = SparkDeserializerFactory.getFeatureDeserializer(tensorType)
        val tensorData = featureDeserializer.deserialize(row.getAs[mutable.WrappedArray[_]](fieldIndex))
        val genericTypedTensor = new GenericTypedTensor(tensorData, tensorType)
        new FeatureValue(genericTypedTensor)
      } else {
        fieldType.dataType match {
          case _: StringType =>
            val value = row.getAs[String](fieldIndex)
            common.FeatureValue.createCategorical(value)
          case _: BooleanType =>
            common.FeatureValue.createBoolean(row.getAs[Boolean](fieldIndex))
          case _: NumericType =>
            val value = row.getAs[Number](fieldIndex).floatValue()
            common.FeatureValue.createNumeric(value)
          case _: MapType =>
            // getJavaMap cannot handle null value, will throw exception
            Option(row.getJavaMap(fieldIndex)) match {
              case None => null
              case Some(value) =>
                new common.FeatureValue(value)
            }
          case arrType: ArrayType =>
            arrType.elementType match {
              case _: ShortType =>
                throw new FeathrFeatureTransformationException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"Cannot convert Array of Short type in ${fieldName} to name-term-value FeatureValue," +
                    s" as it is ambiguous, could be CATEGORICAL_SET or DENSE_VECTOR. Please use Spark-dataframe based derivation expression.")
              case _: IntegerType =>
                throw new FeathrFeatureTransformationException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"Cannot convert Array of Integer type in ${fieldName} to name-term-value FeatureValue," +
                    s" as it is ambiguous, could be CATEGORICAL_SET or DENSE_VECTOR. Please use Spark-dataframe based derivation expression.")
              case _: LongType =>
                throw new FeathrFeatureTransformationException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"Cannot convert Array of Long type in ${fieldName} to name-term-value FeatureValue," +
                    s" as it is ambiguous, could be CATEGORICAL_SET or DENSE_VECTOR. Please use Spark-dataframe based derivation expression.")
              case _: NumericType =>
                val elements = row.getAs[mutable.WrappedArray[_]](fieldIndex).asJava
                new FeatureValue(elements, FeatureTypes.DENSE_VECTOR)
              case _: StringType =>
                val elements = row.getAs[mutable.WrappedArray[_]](fieldIndex).asJava
                new FeatureValue(elements, FeatureTypes.CATEGORICAL_SET)
              case eType =>
                throw new FeathrConfigException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"Cannot convert Array of {$eType} to name-term-value FeatureValue for column ${fieldName}," +
                    s" only array of float/double/string is supported")
            }
          case _: StructType =>
            val fdsFeature = row.getAs[Row](fieldIndex)
            val terms = fdsFeature.get(0) match {
              case keys: Array[_] => keys.map(_.toString).toArray
              case keys: Seq[_] => keys.map(_.toString).toArray
              case keys: mutable.WrappedArray[_] => keys.map(_.toString).toArray
            }
            val values = fdsFeature.get(1) match {
              case values: Array[Float] => values
              case values: Seq[Float] => values.toArray
              case values: mutable.WrappedArray[Float] => values.toArray
            }
            val termValues = terms.zip(values).toMap.asJava
            new FeatureValue(termValues, FeatureTypes.TERM_VECTOR)
        }
      }
    }
    valueMap
  }
}
