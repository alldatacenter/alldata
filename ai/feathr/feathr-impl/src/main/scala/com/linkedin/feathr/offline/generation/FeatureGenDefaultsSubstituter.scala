package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureValue}
import com.linkedin.feathr.offline.{FeatureDataFrame, FeatureDataWithJoinKeys}
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.job.FeatureTransformation
import com.linkedin.feathr.offline.transformation.DataFrameDefaultValueSubstituter
import org.apache.spark.sql.SparkSession

/**
 * Substitutes defaults for all generated anchored features.
 */
private[offline] class FeatureGenDefaultsSubstituter() {
  val featureColNameResolver: String => String = (fName: String) => s"${FeatureTransformation.FEATURE_NAME_PREFIX}$fName"

  /**
   * Process input feature data to substitute defaults.
   * This method groups the input feature data by DataFrame and
   * substitutes defaults for all features in the DataFrame.
   * The substitution is executed by defaultsSubstituteWorker.
   * @param ss                        Spark session.
   * @param featureData               Generated anchored feature data.
   * @param featureDefaultsMap        Parsed user input defaults.
   * @param featureTypeConfigMap      Parsed user input featureTypeConfig.
   * @param defaultsSubstituteWorker  Worker code that actually does the substitution.
   * @return default substituted anchored feature data.
   */
  def substitute(
      ss: SparkSession,
      featureData: FeatureDataWithJoinKeys,
      featureDefaultsMap: Map[String, FeatureValue],
      featureTypeConfigMap: Map[String, FeatureTypeConfig],
      defaultsSubstituteWorker: DataFrameDefaultValueSubstituter = DataFrameDefaultValueSubstituter): FeatureDataWithJoinKeys = {
    featureData.groupBy(_._2._1).flatMap {
      case (FeatureDataFrame(df, inferredTypeConfig), featuresWithKeys) =>
        val featuresToProcess = featuresWithKeys.keys.toSeq
        val joinKeys = featuresWithKeys.head._2._2
        val withDefaultDF =
          defaultsSubstituteWorker.substituteDefaults(df, featuresToProcess, featureDefaultsMap, featureTypeConfigMap, ss, featureColNameResolver)
        // Drop rows if all features at this stage have null values
        val withNullsDroppedDF =
          FeatureTransformation.dropIfNullValuesForAllColumns(
            withDefaultDF,
            featuresWithKeys.keys.map(FeatureTransformation.FEATURE_NAME_PREFIX + DataFrameColName.getEncodedFeatureRefStrForColName(_)).toSeq)
        //  If there're multiple rows with same join key, keep one record for these duplicate records(same behavior as Feature join API)
        val withoutDupDF = withNullsDroppedDF.dropDuplicates(joinKeys)
        // Return features processed in this iteration
        featuresWithKeys.map(f => (f._1, (FeatureDataFrame(withoutDupDF, inferredTypeConfig), joinKeys)))
    }
  }
}

/**
 * Companion object.
 */
private[offline] object FeatureGenDefaultsSubstituter {
  def apply(): FeatureGenDefaultsSubstituter =
    new FeatureGenDefaultsSubstituter()
}
