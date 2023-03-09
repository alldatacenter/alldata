package com.linkedin.feathr.offline.derived.strategies

import com.linkedin.feathr.common
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException, FeathrFeatureTransformationException}
import com.linkedin.feathr.common.{FeatureDerivationFunction, FeatureTypeConfig, FeatureTypes}
import com.linkedin.feathr.offline.ErasedEntityTaggedFeature
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.derived.{DerivedFeature, DerivedFeatureEvaluator}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.testfwk.TestFwkUtils
import com.linkedin.feathr.offline.transformation.FDSConversionUtils
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils.tensorTypeToDataFrameSchema
import com.linkedin.feathr.offline.util.{CoercionUtilsScala, FeaturizedDatasetUtils}
import com.linkedin.feathr.sparkcommon.FeatureDerivationFunctionSpark
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * This class executes custom derivation logic defined in an implementation of FeatureDerivationFunction.
 */
class RowBasedDerivation(dependentFeatureTypeConfigs: Map[String, FeatureTypeConfig],
                         val mvelContext: Option[FeathrExpressionExecutionContext],
                        ) extends RowBasedDerivationStrategy with Serializable {

  /**
   * Calculate a Row-based derived features such as Mvel based derivations or UDFs.
   * High-level approach:
   * - DerivedFeature metadata is used to walkthrough the dependent features for a derived feature.
   * - DataFrameColName utility is used to generate the column names for the dependent features.
   * - CoercionUtils utility is used to convert the column values to FeatureValue.
   * - DerivedFeatureEvaluator.evaluateFromFeatureValues utility method is used to apply the derivation function.
   * - Use Spark Row library to build a DataFrame row and merge different rows.
   *
   * @param keyTags            key tags of the current join stage
   * @param keyTagList         all key tags of the current feature join job
   * @param df                 input context dataframe
   * @param derivedFeature     derived feature to calculate
   * @param derivationFunction derivation function to be applied
   * @return context dataframe with the derived feature column appended
   */
  override def apply(
      keyTags: Seq[Int],
      keyTagList: Seq[String],
      df: DataFrame,
      derivedFeature: DerivedFeature,
      derivationFunction: FeatureDerivationFunction,
      mvelContext: Option[FeathrExpressionExecutionContext]): DataFrame = {
    if (derivationFunction.isInstanceOf[FeatureDerivationFunctionSpark]) {
      throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, s"Unsupported user customized derived feature ${derivedFeature.producedFeatureNames}")
    }
    // 1. prepare feature column name, schema
    val tagsThisStage = Some(keyTags.map(keyTagList).toList)
    val featureNames = derivedFeature.producedFeatureNames
    val derivedFeatureSchema = featureNames.map(featureName => {
      val featureTypeConfig = derivedFeature.featureTypeConfigs.getOrElse(featureName, new FeatureTypeConfig(FeatureTypes.UNSPECIFIED))
      val tensorType = FeaturizedDatasetUtils.lookupTensorTypeForFeatureRef(featureName, FeatureTypes.UNSPECIFIED, featureTypeConfig)
      val newSchema = tensorTypeToDataFrameSchema(tensorType)
      (featureName, newSchema)
    }).toMap

    val outputSchema = StructType(df.schema.union(StructType(featureNames.map(featureName => {
      val standardizedName = DataFrameColName.genFeatureColumnName(featureName, tagsThisStage)
      // feature column should be nullable
      StructField(standardizedName, derivedFeatureSchema(featureName), nullable = true)
    }))))
    val encoder = RowEncoder(outputSchema)

    val contextDFSchema = df.schema
    // 2. calculate derived feature

    if (TestFwkUtils.IS_DEBUGGER_ENABLED) {
      println(f"${Console.GREEN}Your input table to the derived feature is: ${Console.RESET}")
      df.show(10)
    }
    val outputDF = df.map(row => {
      try {
        // prepare context values
        val contextFeatureValues = mutable.Map.empty[common.ErasedEntityTaggedFeature, common.FeatureValue]
        val linkedInputParams = derivedFeature.consumedFeatureNames.map {
          case ErasedEntityTaggedFeature(calleeTag, featureName) =>
            ErasedEntityTaggedFeature(calleeTag.map(keyTags), featureName)
        }
        linkedInputParams.foreach(dependFeature => {
          val tagInfo = dependFeature.getBinding.asScala.map(keyTagList(_))
          val standardizedName = DataFrameColName.genFeatureColumnName(dependFeature.getFeatureName, Some(tagInfo))
          val featureColumn = if (contextDFSchema.fields.map(_.name).contains(standardizedName)) {
            standardizedName
          } else {
            // for anchored passthrough feature, there is no key tag for them
            DataFrameColName.genFeatureColumnName(dependFeature.getFeatureName)
          }
          val dependentFeatureName = dependFeature.getFeatureName
          val featureTypeConfig = dependentFeatureTypeConfigs.getOrElse(dependentFeatureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)

          val featureValue = CoercionUtilsScala.coerceFieldToFeatureValue(row, contextDFSchema, featureColumn, featureTypeConfig)
          contextFeatureValues.put(ErasedEntityTaggedFeature(dependFeature.getBinding, dependFeature.getFeatureName), featureValue)
        })
        // calculate using original function
        val features = DerivedFeatureEvaluator.evaluateFromFeatureValues(keyTags, derivedFeature, contextFeatureValues.toMap, mvelContext)
        val taggFeatures = features.map(kv => (kv._1.getErasedTagFeatureName, kv._2))
        val featureValues = featureNames.map(featureName => {
          taggFeatures.get(ErasedEntityTaggedFeature(keyTags, featureName).getErasedTagFeatureName).map { featureValue =>
            val featureType = derivedFeature.featureTypeConfigs
              .getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG).getFeatureType

            val schemaType = derivedFeatureSchema(featureName)

            if (featureType == FeatureTypes.TENSOR) {
              // Convert to FDS directly when tensor type is specified
              FDSConversionUtils.rawToFDSRow(featureValue.getAsTensorData, schemaType)
            } else {
              FDSConversionUtils.rawToFDSRow(featureValue.getAsTermVector.asScala, schemaType)
            }
          }.getOrElse(null)
        })

        Row.fromSeq(outputSchema.indices.map { i =>
        {
          if (i >= contextDFSchema.size) {
            featureValues(i - contextDFSchema.size)
          } else {
            row.get(i)
          }
        }
        })
      } catch {
        case e: Exception =>
          throw new FeathrFeatureTransformationException(
            ErrorLabel.FEATHR_USER_ERROR,
            s"Fail to calculate derived feature ${derivedFeature.producedFeatureNames}",
            e)
      }
    })(encoder)
    outputDF
  }
}
