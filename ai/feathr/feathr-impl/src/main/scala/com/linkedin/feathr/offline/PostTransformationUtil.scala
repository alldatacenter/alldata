package com.linkedin.feathr.offline

import java.io.Serializable
import com.linkedin.feathr.common
import com.linkedin.feathr.common.{FeatureTypes, FeatureValue}
import com.linkedin.feathr.offline.exception.FeatureTransformationException
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.mvel.{FeatureVariableResolverFactory, MvelContext}
import com.linkedin.feathr.offline.transformation.MvelDefinition
import com.linkedin.feathr.offline.util.{CoercionUtilsScala, FeaturizedDatasetUtils}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Column, DataFrame}
import org.mvel2.MVEL

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/*
 * Utilities methods to apply transformations on Feathr features
 */
private[offline] object PostTransformationUtil {
  val NTV_FORMAT_BOOLEAN_TRUE = Map("" -> 1.0f)
  val NTV_FORMAT_BOOLEAN_FALSE = Map.empty[String, Float]

  /**
   * transform feature using mvel expression, input and output are both boolean
   * @param featureName feature name
   * @param mvelExpression mvel expr
   * @param compiledExpression compiled mvel
   * @param input input feature value
   * @return transformed feature value
   */
  def booleanTransformer(featureName: String, mvelExpression: MvelDefinition, compiledExpression: Serializable, input: Boolean, mvelContext: Option[FeathrExpressionExecutionContext]): Boolean = {
    val toFeatureValue = common.FeatureValue.createBoolean(input)
    val transformedFeatureValue = transformFeatureValues(featureName, toFeatureValue, compiledExpression, FeatureTypes.TERM_VECTOR, mvelContext)
    transformedFeatureValue match {
      case Success(fVal) => fVal.getAsTermVector.containsKey("true")
      case Failure(ex) =>
        throw new FeatureTransformationException(
          s"booleanTransformer failed for feature: $featureName MVEL expression: $mvelExpression and input data " +
            s"$input with exception: ",
          ex)
    }
  }

  /**
   * transform feature using mvel expression, input is FDS format 1-d tensor output is a map
   * @param featureName feature name
   * @param mvelExpression mvel expr
   * @param compiledExpression compiled mvel
   * @param input input feature value
   * @return transformed feature value
   */
  def fds1dTensorTransformer(
    featureName: String,
    mvelExpression: MvelDefinition,
    compiledExpression: Serializable,
    input: GenericRowWithSchema, mvelContext: Option[FeathrExpressionExecutionContext]): Map[String, Float] = {
    if (input != null) {
      val inputMapKey = input.getAs[Seq[String]](FeaturizedDatasetUtils.FDS_1D_TENSOR_DIM)
      val inputMapVal = input.getAs[Seq[Float]](FeaturizedDatasetUtils.FDS_1D_TENSOR_VALUE)
      val inputMap = inputMapKey.zip(inputMapVal).toMap
      mapTransformer(featureName, mvelExpression, compiledExpression, inputMap, mvelContext)
    } else Map()
  }

  /**
   * Apply the transformation on a list of columns in a dataframe.
   * @param featureNameColumnTuples Feature name to feature column name mapping
   * @param contextDF Initial contextDF
   * @param transformationDef Feature name to transformation logic mapping
   * @param defaultTransformation When transformationDef is not provided for a feature, defaultTransformation will be
   *                              used
   * @return [[FeatureValue]] Feature value after transformation using the mvel expression
   */
  def transformFeatures(
    featureNameColumnTuples: Seq[(String, String)],
    contextDF: DataFrame,
    transformationDef: Map[String, MvelDefinition],
    defaultTransformation: (DataType, String) => Column,
    mvelContext: Option[FeathrExpressionExecutionContext]): DataFrame = {
    val featureColumnNames = featureNameColumnTuples.map(_._2)

    // Transform the features with the provided transformations
    val featureValueColumn = featureNameColumnTuples.map {
      case (featureName, columnName) =>
        val fieldIndex = contextDF.schema.fieldIndex(columnName)
        val fieldType = contextDF.schema.toList(fieldIndex)

        transformationDef.get(featureName) map {
          case mvelExpressionDef =>
            val parserContext = MvelContext.newParserContext()
            val compiledExpression = MVEL.compileExpression(mvelExpressionDef.mvelDef, parserContext)
            val featureType = mvelExpressionDef.featureType
            val convertToString = udf(stringTransformer(featureName, mvelExpressionDef, compiledExpression, _: String, mvelContext))
            val convertToBoolean = udf(booleanTransformer(featureName, mvelExpressionDef, compiledExpression, _: Boolean, mvelContext))
            val convertToFloat = udf(floatTransformer(featureName, mvelExpressionDef, compiledExpression, _: Float, mvelContext))
            val convertToMap = udf(mapTransformer(featureName, mvelExpressionDef, compiledExpression, _: Map[String, Float], mvelContext))
            val convertFDS1dTensorToMap = udf(fds1dTensorTransformer(featureName, mvelExpressionDef, compiledExpression, _: GenericRowWithSchema, mvelContext))
            fieldType.dataType match {
              case _: StringType => convertToString(contextDF(columnName))
              case _: NumericType => convertToFloat(contextDF(columnName))
              case _: MapType => convertToMap(contextDF(columnName))

              case _: StructType => convertFDS1dTensorToMap(contextDF(columnName))
              case _: BooleanType => convertToBoolean(contextDF(columnName))
              case fType => throw new RuntimeException(s"Type $fType is not supported in feature post transformation.")
            }
        } getOrElse defaultTransformation(fieldType.dataType, columnName)
    }

    val featureValueToJoinKeyColumnName = featureValueColumn zip featureColumnNames
    featureValueToJoinKeyColumnName.foldLeft(contextDF)((s, x) => s.withColumn(x._2, x._1))
  }


  /**
   * Transform a feature value using the MVEL expression. Currently used in SeqJoin where we need to transform only
   * a single feature.
   * @param featureName Name of the feature we are looking to transform
   * @param featureValue Feature value of the given feature
   * @return [[FeatureValue]] Feature value after transformation using the MVEL expression
   */
  private def transformFeatureValues(
      featureName: String,
      featureValue: FeatureValue,
      compiledExpression: Serializable,
      featureType: FeatureTypes,
      mvelContext: Option[FeathrExpressionExecutionContext]): Try[FeatureValue] = Try {
    val args = Map(featureName -> Some(featureValue))
    val variableResolverFactory = new FeatureVariableResolverFactory(args)
    val transformedValue = MvelContext.executeExpressionWithPluginSupportWithFactory(compiledExpression, featureValue, variableResolverFactory, mvelContext.orNull)
    CoercionUtilsScala.coerceToFeatureValue(transformedValue, featureType)
  }

  private def floatTransformer(featureName: String, mvelExpression: MvelDefinition, compiledExpression: Serializable, input: Float, mvelContext: Option[FeathrExpressionExecutionContext]): Float = {
    val toFeatureValue = common.FeatureValue.createNumeric(input)
    val transformedFeatureValue = transformFeatureValues(featureName, toFeatureValue, compiledExpression, FeatureTypes.NUMERIC, mvelContext)
    transformedFeatureValue match {
      case Success(fVal) => fVal.getAsNumeric
      case Failure(ex) =>
        throw new FeatureTransformationException(
          s"floatTransformer failed for feature: $featureName MVEL expression: $mvelExpression and input data " +
            s"$input with exception: ",
          ex)
    }
  }

  private def stringTransformer(featureName: String, mvelExpression: MvelDefinition, compiledExpression: Serializable, input: String, mvelContext: Option[FeathrExpressionExecutionContext]): String = {
    val toFeatureValue = common.FeatureValue.createCategorical(input)
    val transformedFeatureValue = transformFeatureValues(featureName, toFeatureValue, compiledExpression, FeatureTypes.CATEGORICAL, mvelContext)
    transformedFeatureValue match {
      case Success(fVal) => fVal.getAsString
      case Failure(ex) =>
        throw new FeatureTransformationException(
          s"floatTransformer failed for feature: $featureName MVEL expression: $mvelExpression and input data " +
            s"$input with exception: ",
          ex)
    }
  }

  private def mapTransformer(
      featureName: String,
      mvelExpression: MvelDefinition,
      compiledExpression: Serializable,
      input: Map[String, Float],
      mvelContext: Option[FeathrExpressionExecutionContext]): Map[String, Float] = {
    if (input == null) {
      return Map()
    }
    val toFeatureValue = new common.FeatureValue(input.asJava)
    val transformedFeatureValue = transformFeatureValues(featureName, toFeatureValue, compiledExpression, FeatureTypes.TERM_VECTOR, mvelContext)
    transformedFeatureValue match {
      case Success(fVal) => fVal.getAsTermVector.asScala.map(kv => (kv._1.asInstanceOf[String], kv._2.asInstanceOf[Float])).toMap
      case Failure(ex) =>
        throw new FeatureTransformationException(
          s"floatTransformer failed for feature: $featureName MVEL expression: $mvelExpression and input data " +
            s"$input with exception: ",
          ex)
    }
  }
}
