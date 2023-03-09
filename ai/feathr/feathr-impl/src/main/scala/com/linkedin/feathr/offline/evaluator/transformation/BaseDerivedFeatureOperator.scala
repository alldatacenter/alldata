package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.common
import com.linkedin.feathr.common.{FeatureDerivationFunction, FeatureTypeConfig, FeatureTypes}
import com.linkedin.feathr.compute.{NodeReference, Transformation}
import com.linkedin.feathr.exception.{ErrorLabel, FrameFeatureTransformationException}
import com.linkedin.feathr.offline.derived.functions.{MvelFeatureDerivationFunction, SimpleMvelDerivationFunction}
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.graph.NodeUtils.{getFeatureTypeConfigsMap, getFeatureTypeConfigsMapForTransformationNodes}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.transformation.{FDSConversionUtils, FeatureColumnFormat}
import com.linkedin.feathr.offline.util.{CoercionUtilsScala, FeaturizedDatasetUtils}
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils.tensorTypeToDataFrameSchema
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types.{StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable

/**
 * BaseDerivedFeatureOperator contains the function applyDerivationFunction is used by the 4 different derived operators we support
 * (OPERATOR_ID_DERIVED_MVEL, OPERATOR_ID_DERIVED_JAVA_UDF_FEATURE_EXTRACTOR, OPERATOR_ID_DERIVED_SPARK_SQL_FEATURE_EXTRACTOR,
 * and OPERATOR_ID_EXTRACT_FROM_TUPLE)
 * to apply their respective derivation functions to the context dataframe. Note that this function expects the columns which
 * the derivation function requires as inputs to be joined to the contextDf
 */
object BaseDerivedFeatureOperator {
  def applyDerivationFunction(node: Transformation,
    derivationFunction: FeatureDerivationFunction,
    graphTraverser: FCMGraphTraverser,
    contextDf: DataFrame): DataFrame = {
    val featureName = if (node.getFeatureName == null) graphTraverser.nodeIdToFeatureName(node.getId) else node.getFeatureName
    // If the feature name is already in the contextDf, drop that column
    val inputDf = if (contextDf.columns.contains(featureName)) {
      contextDf.drop(featureName)
    } else {
      contextDf
    }

    // Gather inputs from node
    val inputs = node.getInputs
    val inputFeatureNames = inputs.toArray.map(input => {
      val inp = input.asInstanceOf[NodeReference]
      graphTraverser.nodeIdToFeatureName(inp.getId)
    }).sorted
    val inputNodes = inputs.toArray.map(input => {
      val inp = input.asInstanceOf[NodeReference]
      graphTraverser.nodes(inp.getId)
    }).toSeq
    val inputFeatureTypeConfigs = getFeatureTypeConfigsMap(inputNodes)

    // Prepare schema values needed for computation.
    val featureTypeConfigs = getFeatureTypeConfigsMapForTransformationNodes(Seq(node))
    val featureTypeConfig = featureTypeConfigs.getOrElse(featureName, new FeatureTypeConfig(FeatureTypes.UNSPECIFIED))
    val tensorType = FeaturizedDatasetUtils.lookupTensorTypeForNonFMLFeatureRef(featureName, FeatureTypes.UNSPECIFIED, featureTypeConfig)
    val newSchema = tensorTypeToDataFrameSchema(tensorType)
    val inputSchema = inputDf.schema
    val mvelContext: Option[FeathrExpressionExecutionContext] = graphTraverser.mvelExpressionContext
    val outputSchema = StructType(inputSchema.union(StructType(Seq(StructField(featureName, newSchema, nullable = true)))))
    val encoder = RowEncoder(outputSchema)
    val outputDf = inputDf.map(row => {
      try {
        val contextFeatureValues = mutable.Map.empty[String, common.FeatureValue]
        inputFeatureNames.map(inputFeatureName => {
          val featureTypeConfig = inputFeatureTypeConfigs.getOrElse(inputFeatureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
          val featureValue = CoercionUtilsScala.coerceFieldToFeatureValue(row, inputSchema, inputFeatureName, featureTypeConfig)
          contextFeatureValues.put(inputFeatureName, featureValue)
        }
        )
        // Sort by input feature name to be consistent with how the derivation function is created.
        val featureValues = contextFeatureValues.toSeq.sortBy(_._1).map(fv => Option(fv._2))
        val derivedFunc = derivationFunction match {
          case mvelDerivedFunc: MvelFeatureDerivationFunction =>
            mvelDerivedFunc.mvelContext = mvelContext
            mvelDerivedFunc
          case simpleMvelDerivedFunc: SimpleMvelDerivationFunction =>
            simpleMvelDerivedFunc.mvelContext = mvelContext
            simpleMvelDerivedFunc
          case func => func
        }
        val unlinkedOutput = derivedFunc.getFeatures(featureValues)
        val featureType = featureTypeConfigs
          .getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG).getFeatureType
        val fdFeatureValue = unlinkedOutput.map(fv => {
          if (fv.isDefined) {
            if (featureType == FeatureTypes.TENSOR && !derivationFunction.isInstanceOf[SimpleMvelDerivationFunction]) {
              // Convert to FDS directly when tensor type is specified
              FDSConversionUtils.rawToFDSRow(fv.get.getAsTensorData, newSchema)
            } else {
              FDSConversionUtils.rawToFDSRow(fv.get.getAsTermVector.asScala, newSchema)
            }
          } else {
            null
          }
        })
        Row.fromSeq(outputSchema.indices.map { i => {
          if (i >= inputSchema.size) {
            fdFeatureValue(i - inputSchema.size)
          } else {
            row.get(i)
          }
        }
        })
      } catch {
        case e: Exception =>
          throw new FrameFeatureTransformationException(
            ErrorLabel.FEATHR_USER_ERROR,
            s"Fail to calculate derived feature " + featureName,
            e)
      }
    })(encoder)

    // Apply feature alias if there is one defined.
    if (graphTraverser.nodeIdToFeatureName(node.getId) != node.getFeatureName) {
      val featureAlias = graphTraverser.nodeIdToFeatureName(node.getId)
      graphTraverser.featureColumnFormatsMap(featureAlias) = FeatureColumnFormat.RAW
      outputDf.withColumnRenamed(featureName, featureAlias)
    } else outputDf
  }
}