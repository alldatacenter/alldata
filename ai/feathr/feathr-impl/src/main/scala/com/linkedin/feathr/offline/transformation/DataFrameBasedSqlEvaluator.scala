package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureTypes, SELECTED_FEATURES}
import com.linkedin.feathr.offline.anchored.anchorExtractor.SQLConfigurableAnchorExtractor
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.job.TransformedResult
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat.FeatureColumnFormat
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils
import com.linkedin.feathr.sparkcommon.SimpleAnchorExtractorSpark
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.{Column, DataFrame, SaveMode}

/**
 * Evaluator that transforms features using Spark SQL or user customized SimpleAnchorExtractorSpark implementation
 * It evaluates the extractor class against the input data to get the feature value
 */

private[offline] object DataFrameBasedSqlEvaluator {

  /**
   * Transform and add feature column to input dataframe using transformer
   * @param transformer SimpleAnchorExtractorSpark implementation
   * @param inputDf input dataframe
   * @param requestedFeatureNameAndPrefix feature names and prefix pairs
   * @param featureTypeConfigs feature name to feature type config
   * @return TransformedResult or transformed feature data.
   */
  def transform(
      transformer: SimpleAnchorExtractorSpark,
      inputDf: DataFrame,
      requestedFeatureNameAndPrefix: Seq[(String, String)],
      featureTypeConfigs: Map[String, FeatureTypeConfig]): TransformedResult = {
    val requestedFeatureName = requestedFeatureNameAndPrefix.map(_._1)
    transformer.setInternalParams(SELECTED_FEATURES, s"[${requestedFeatureName.mkString(",")}]")

    // Get DataFrame schema for tensor based on inferred tensor type.
    val featureSchemas = requestedFeatureName
      .map(featureName => {
        val tensorType = FeaturizedDatasetUtils.lookupTensorTypeForFeatureRef(featureName, None,
          featureTypeConfigs.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG))
        val schema = FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(tensorType)
        featureName -> schema
      })
      .toMap

    // Get transformed feature columns and column format

    val transformedColsAndFormats: Map[(String, Column), FeatureColumnFormat] = if (transformer.isInstanceOf[SQLConfigurableAnchorExtractor]) {
      // If instance of SQLConfigurableAnchorExtractor, get Tensor features
      transformer.asInstanceOf[SQLConfigurableAnchorExtractor].getTensorFeatures(inputDf, featureSchemas)
    } else {
      // If transform.getFeatures() returns empty Seq, then transform using transformAsColumns
      transformer.transformAsColumns(inputDf).map(c => (c, FeatureColumnFormat.RAW)).toMap
    }
    val transformedDF = createFeatureDF(inputDf, transformedColsAndFormats.keys.toSeq)
    // Infer feature types
    val inferredFeatureTypeConfigs = requestedFeatureName map {
      case featureName =>
        val featureTypeConfig = featureTypeConfigs.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG)
        val colName = featureName
        val inferredFeatureTypeConfig = if (featureTypeConfig.getFeatureType == FeatureTypes.UNSPECIFIED) {
          // infer feature type from the resulting DataFrame automatically
          val dataType = transformedDF.schema.fields(transformedDF.schema.fieldIndex(colName)).dataType
          val inferredFeatureType = FeaturizedDatasetUtils.inferFeatureTypeFromColumnDataType(dataType)
          new FeatureTypeConfig(inferredFeatureType)
        } else featureTypeConfig
        (featureName, inferredFeatureTypeConfig)
    }

    TransformedResult(
      featureNameAndPrefixPairs = requestedFeatureNameAndPrefix,
      df = transformedDF,
      featureColumnFormats = transformedColsAndFormats.map(e => e._1._1 -> e._2),
      inferredFeatureTypes = inferredFeatureTypeConfigs.toMap)
  }

  /**
   * This functions is used to remove the context fields that have the same names as the feature names, append feature
   * columns. So that we can support feature with the same name as the context field name in Spark SQL, e.g.
   * features: {
   *    foo.def.sqlExpr: foo
   * }
   * @param inputDf source dataframe
   * @param featureColumnDefs feature name to feature column definition
   * @return dataframe with features
   */
  private def createFeatureDF(inputDf: DataFrame, featureColumnDefs: Seq[(String, Column)]): DataFrame = {
    // first add a prefix to the feature column name in the schema
    val featureColumnNamePrefix = "_feathr_sql_feature_prefix_"
    val transformedDF = featureColumnDefs.foldLeft(inputDf)((baseDF, columnWithName) => {
      val columnName = featureColumnNamePrefix + columnWithName._1
      baseDF.withColumn(columnName, columnWithName._2)
    })
    val featureNames = featureColumnDefs.map(_._1)
    // drop the context column that have the same name as feature names
    val withoutDupContextFieldDF = transformedDF.drop(featureNames: _*)
    // remove the prefix we just added, so that we have a dataframe with feature names as their column names
    featureNames
      .zip(featureNames)
      .foldLeft(withoutDupContextFieldDF)((baseDF, namePair) => {
        baseDF.withColumnRenamed(featureColumnNamePrefix + namePair._1, namePair._2)
      })
  }
}
