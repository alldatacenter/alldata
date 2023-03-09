package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.common.tensor.TensorData
import com.linkedin.feathr.common.{AnchorExtractor, FeatureTypeConfig, FeatureTypes, SparkRowExtractor}
import com.linkedin.feathr.offline
import com.linkedin.feathr.offline.FeatureDataFrame
import com.linkedin.feathr.offline.anchored.anchorExtractor.SimpleConfigurableAnchorExtractor
import com.linkedin.feathr.offline.job.{FeatureTransformation, FeatureTypeInferenceContext, TransformedResult}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.util.AccumulatorV2

/**
 * Evaluator that transforms features using MVEL based on DataFrame
 * It evaluates the extractor class against the input data to get the feature value
 * This is used in sequential join's MVEL based expand feature evaluation, and anchored passthrough feature evaluation.
 * It is essentially a batch version of getFeatures() for dataframe
 */
private[offline] object DataFrameBasedRowEvaluator {

  /**
   * Apply a transformer to an input Dataframe to produce features
   * @param transformer transformer to be applied
   * @param inputDf input dataframe
   * @param requestedFeatureNameAndPrefix feature name and the prefix map
   * @param featureTypeConfigs feature name to feature type definition
   * @return Transformed result, e.g. input dataframe with features
   */
  def transform(transformer: AnchorExtractor[_],
                inputDf: DataFrame,
                requestedFeatureNameAndPrefix: Seq[(String, String)],
                featureTypeConfigs: Map[String, FeatureTypeConfig],
                mvelContext: Option[FeathrExpressionExecutionContext]): TransformedResult = {
    if (!transformer.isInstanceOf[SparkRowExtractor]) {
      throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, s"${transformer} must extend SparkRowExtractor.")
    }
    val extractor = transformer.asInstanceOf[SparkRowExtractor]
    val requestedFeatureRefString = requestedFeatureNameAndPrefix.map(_._1)
    val featureNamePrefix = requestedFeatureNameAndPrefix.head._2
    val featureFormat = FeatureColumnFormat.FDS_TENSOR
    // features to calculate, if empty, will calculate all features defined in the extractor
    val selectedFeatureNames = if (requestedFeatureRefString.nonEmpty) requestedFeatureRefString else transformer.getProvidedFeatureNames
    val FeatureDataFrame(transformedDF, transformedFeatureTypes) = transformToFDSTensor(extractor, inputDf, selectedFeatureNames, featureTypeConfigs, mvelContext)
    TransformedResult(
      // Re-compute the featureNamePrefixPairs because feature names can be coming from the extractor.
      selectedFeatureNames.map((_, featureNamePrefix)),
      transformedDF,
      selectedFeatureNames.map(c => (c, featureFormat)).toMap,
      transformedFeatureTypes)
  }

  /**
   * Transform and add feature column to input dataframe using mvelExtractor, output feature format is FDS Tensor
   * @param inputDF      input dataframe
   * @param featureRefStrs features to transform
   * @return inputDF with feature columns added, and the inferred feature types for features.
   *         The inferred type is based on looking at the raw feature value returned by MVEL expression. We used to
   *         infer the feature type and convert them into term-vector in the getFeatures() API. Now in this function,
   *         we still use the same rules to infer the feature type, but storing them in FDS tensor format instead of
   *         term-vector.
   */
  private def transformToFDSTensor(rowExtractor:  SparkRowExtractor,
                                   inputDF: DataFrame,
                                   featureRefStrs: Seq[String],
                                   featureTypeConfigs: Map[String, FeatureTypeConfig],
                                   mvelContext: Option[FeathrExpressionExecutionContext]): FeatureDataFrame = {
    val inputSchema = inputDF.schema
    val spark = SparkSession.builder().getOrCreate()
    val featureTypes = featureTypeConfigs.mapValues(_.getFeatureType)
    val FeatureTypeInferenceContext(featureTypeAccumulators) =
      FeatureTransformation.getTypeInferenceContext(spark, featureTypes, featureRefStrs)

    val transformedRdd = inputDF.rdd.map(row => {
        // in some cases, the input dataframe row here only have Row and does not have schema attached,
        // while MVEL only works with GenericRowWithSchema, create it manually
        val rowWithSchema = if (row.isInstanceOf[GenericRowWithSchema]) {
          row.asInstanceOf[GenericRowWithSchema]
        } else {
          new GenericRowWithSchema(row.toSeq.toArray, inputSchema)
        }
        if (rowExtractor.isInstanceOf[SimpleConfigurableAnchorExtractor]) {
          rowExtractor.asInstanceOf[SimpleConfigurableAnchorExtractor].mvelContext = mvelContext
        }
        val result = rowExtractor.getFeaturesFromRow(rowWithSchema)
        val featureValues = featureRefStrs map {
          featureRef =>
            if (result.contains(featureRef)) {
              val featureValue = result(featureRef)
              if (featureTypeAccumulators(featureRef).isZero && featureValue != null) {
                val rowFeatureType = featureValue.getFeatureType.getBasicType
                featureTypeAccumulators(featureRef).add(FeatureTypes.valueOf(rowFeatureType.toString))
              }
              val tensorData: TensorData = featureValue.getAsTensorData()
              FeaturizedDatasetUtils.tensorToFDSDataFrameRow(tensorData)
            } else null
        }
        Row.merge(row, Row.fromSeq(featureValues))
    })
    val inferredFeatureTypes = FeatureTransformation.inferFeatureTypes(featureTypeAccumulators, transformedRdd, featureRefStrs)
    val inferredFeatureTypeConfigs = inferredFeatureTypes.map(featureTypeEntry => featureTypeEntry._1 -> new FeatureTypeConfig(featureTypeEntry._2))
    // Merge inferred and provided feature type configs
    val (unspecifiedTypeConfigs, providedTypeConfigs) = featureTypeConfigs.partition(x => x._2.getFeatureType == FeatureTypes.UNSPECIFIED)
    // providedTypeConfigs should take precedence over inferredFeatureTypeConfigs; inferredFeatureTypeConfigs should
    // take precedence over unspecifiedTypeConfigs
    val mergedTypeConfigs = unspecifiedTypeConfigs ++ inferredFeatureTypeConfigs ++ providedTypeConfigs
    val featureDF: DataFrame = createFDSFeatureDF(inputDF, featureRefStrs, spark, transformedRdd, mergedTypeConfigs)
    offline.FeatureDataFrame(featureDF, mergedTypeConfigs)
  }

  /**
   * Create a FDS feature dataframe from the MVEL transformed RDD.
   * This functions is used to remove the context fields that have the same names as the feature names,
   * so that we can support feature with the same name as the context field name, e.g.
   * features: {
   *   foo: foo
   * }
   * @param inputDF source dataframe
   * @param featureRefStrs feature ref strings
   * @param ss spark session
   * @param transformedRdd transformed RDD[Row] (in FDS format) from inputDF
   * @param featureTypesConfigs feature types
   * @return FDS feature dataframe
   */
  private def createFDSFeatureDF(
                                  inputDF: DataFrame,
                                  featureRefStrs: Seq[String],
                                  ss: SparkSession,
                                  transformedRdd: RDD[Row],
                                  featureTypesConfigs: Map[String, FeatureTypeConfig]): DataFrame = {
    // first add a prefix to the feature column name in the schema
    val featureColumnNamePrefix = "_feathr_mvel_feature_prefix_"
    val featureTensorTypeInfo = FeatureTransformation.getFDSSchemaFields(featureRefStrs, featureTypesConfigs,
      featureColumnNamePrefix)

    val outputSchema = StructType(inputDF.schema.union(StructType(featureTensorTypeInfo)))

    val transformedDF = ss.createDataFrame(transformedRdd, outputSchema)
    // drop the context column that have the same name as feature names
    val withoutDupContextFieldDF = transformedDF.drop(featureRefStrs: _*)
    // remove the prefix we just added, so that we have a dataframe with feature names as their column names
    val featureDF = featureRefStrs
      .zip(featureRefStrs)
      .foldLeft(withoutDupContextFieldDF)((baseDF, namePair) => {
        baseDF.withColumnRenamed(featureColumnNamePrefix + namePair._1, namePair._2)
      })
    featureDF
  }
}

// Used to infer the feature type for a feature while transforming the feature for each row in the dataset
// See updateFeatureType() for rules being used.
private[offline] class FeatureTypeAccumulator(var featureType: FeatureTypes) extends AccumulatorV2[FeatureTypes, FeatureTypes] {

  def reset(): Unit = {
    featureType = FeatureTypes.UNSPECIFIED
  }

  def add(input: FeatureTypes): Unit = {
    featureType = updateFeatureType(featureType, input)
  }

  def value: FeatureTypes = {
    featureType
  }

  def isZero: Boolean = {
    featureType == FeatureTypes.UNSPECIFIED
  }

  def copy(): FeatureTypeAccumulator = {
    new FeatureTypeAccumulator(featureType)
  }

  def merge(other: AccumulatorV2[FeatureTypes, FeatureTypes]): Unit = {
    featureType = updateFeatureType(featureType, other.value)
  }

  /**
   * Update existing feature type (inferred by previous rows) with the new feature type inferred from a new row
   * Rules:
   * type + type => type
   * Unspecified + type => type
   * CATEGORICAL + CATEGORICAL_SET => CATEGORICAL_SET
   * DENSE_VECTOR + VECTOR => DENSE_VECTOR
   *
   * @param existingType existing feature type
   * @param currentType new feature type
   * @return new feature type
   */
  private def updateFeatureType(existingType: FeatureTypes, currentType: FeatureTypes): FeatureTypes = {
    if (existingType != currentType) {
      (existingType, currentType) match {
        case (eType, FeatureTypes.UNSPECIFIED) => eType
        case (FeatureTypes.UNSPECIFIED, tType) => tType
        case (eType, tType) if (Set(eType, tType).subsetOf(Set(FeatureTypes.CATEGORICAL_SET, FeatureTypes.CATEGORICAL))) =>
          FeatureTypes.CATEGORICAL_SET
        case (eType, tType) if (Set(eType, tType).subsetOf(Set(FeatureTypes.DENSE_VECTOR))) =>
          FeatureTypes.DENSE_VECTOR
        case (eType, tType) =>
          throw new FeathrException(ErrorLabel.FEATHR_USER_ERROR, s"A feature should have only one feature type, but found ${eType} and ${tType}")
      }
    } else currentType
  }
}
