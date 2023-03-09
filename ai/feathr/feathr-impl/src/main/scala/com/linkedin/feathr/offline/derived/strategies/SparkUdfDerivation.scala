package com.linkedin.feathr.offline.derived.strategies


import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.offline.ErasedEntityTaggedFeature
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.exception.FeatureTransformationException
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.sparkcommon.FeatureDerivationFunctionSpark
import org.apache.spark.sql.DataFrame

import scala.collection.JavaConverters._

/**
 * This class executes custom derivation logic defined in an implementation of FeatureDerivationFunctionSpark.
 */
class SparkUdfDerivation extends SparkUdfDerivationStrategy {

  /**
   * Calculate a user customized derived feature.
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
      derivationFunction: FeatureDerivationFunctionSpark,
      mvelContext: Option[FeathrExpressionExecutionContext]): DataFrame = {
    if (derivedFeature.parameterNames.isEmpty) {
      throw new FeathrException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"User customized derived feature ${derivedFeature.producedFeatureNames} does not have parameter names")
    }
    // 1. create input feature columns
    val linkedInputParams = derivedFeature.consumedFeatureNames.map {
      case ErasedEntityTaggedFeature(calleeTag, featureName) =>
        ErasedEntityTaggedFeature(calleeTag.map(keyTags), featureName)
    }
    // featureColumn -> derived feature argument name map
    val featureColumnNameToParamNameMap = linkedInputParams.zip(derivedFeature.parameterNames.get).map { taggedWithParam =>
      val tagged = taggedWithParam._1
      // tagged has been parse by calleeTag in linkedInputParams, so just need to direct map keyTagList
      val tags = Some(tagged.getBinding.asScala.map(keyTagList(_)))
      val featureColumnName = DataFrameColName.genFeatureColumnName(tagged.getFeatureName, tags)
      (featureColumnName, taggedWithParam._2)
    }
    // rename all dependency column names to the parameter/argument names that user specified
    val withRenamedFeaturesDF =
      featureColumnNameToParamNameMap.foldLeft(df)((acc, featureNameToArg) => acc.withColumn(featureNameToArg._2, acc(featureNameToArg._1)))
    // 2. calculate derived feature and append it as a column
    val transformedDF = derivationFunction.transform(withRenamedFeaturesDF)
    val callerKeyTags = derivedFeature.producedFeatureNames.map(ErasedEntityTaggedFeature(keyTags, _))
    // rename output feature column name with tags, as the output column name is just the feature name
    val featureNameToStandardizedColumnNameMap = callerKeyTags.map { tagged =>
      val tags = Some(tagged.getBinding.asScala.map(keyTagList(_)))
      val standardizedName = DataFrameColName.genFeatureColumnName(tagged.getFeatureName, tags)
      (tagged.getFeatureName, standardizedName)
    }
    // 3. validate transformation results
    val expectColumns = (withRenamedFeaturesDF.columns ++ featureNameToStandardizedColumnNameMap.map(_._1)).toSet
    val returnedColumns = transformedDF.columns.toSet
    // some transformer (e.g., advanced derivation function) produces more than one features,
    // and in the config, user may only declare a subset of all the provided features, we need to remove all
    // the undeclared columns from the transformed dataframe, otherwise, it may end up with duplicate columns
    val undeclaredColumns = returnedColumns.diff(expectColumns).toSeq
    val missingColumns = expectColumns.diff(returnedColumns)
    if (missingColumns.nonEmpty) {
      throw new FeatureTransformationException(
        s"Expected columns: ${missingColumns.mkString(",")} are missing after derived feature " +
          s"${derivedFeature} evaluation, its derivation function should generate or passthrough all expected columns")
    }

    // 4. renaming the derived feature names to the standardized format
    val withOutputFeatureDF = featureNameToStandardizedColumnNameMap.foldLeft(transformedDF)((acc, featueNameToStandardizeName) =>
      acc.withColumnRenamed(featueNameToStandardizeName._1, featueNameToStandardizeName._2))

    // 5. remove input/argument feature columns
    withOutputFeatureDF
      .drop(featureColumnNameToParamNameMap.map(_._2): _*)
      .drop(undeclaredColumns: _*)
  }
}
