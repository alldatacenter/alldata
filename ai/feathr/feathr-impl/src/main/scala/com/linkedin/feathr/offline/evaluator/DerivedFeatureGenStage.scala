package com.linkedin.feathr.offline.evaluator


import com.linkedin.feathr.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.offline.{FeatureDataFrame, FeatureDataWithJoinKeys, client}
import com.linkedin.feathr.offline.client.{DataFrameColName}
import com.linkedin.feathr.offline.derived.{DerivedFeature, DerivedFeatureEvaluator}
import com.linkedin.feathr.offline.job.FeatureTransformation.FEATURE_TAGS_PREFIX
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlan}
import org.apache.spark.sql.DataFrame

/**
 * The case class represents DataFrame and associated metadata required to compute a derived feature.
 * @param featureDataFrame base Datafeathr.
 * @param joinKeys         columns of DataFrame used for joins.
 * @param featureNames     evaluated features on the Datafeathr.
 */
private[offline] case class BaseDataFrameMetadata(featureDataFrame: FeatureDataFrame, joinKeys: Seq[String], featureNames: Seq[String])

/**
 * A concrete implementation of [[StageEvaluator]]. It processes derived features that are planned to be evaluated
 * in the stage.
 * @param featureGroups        features identified by the group they belong to.
 * @param logicalPlan          feature information after analyzing the requested features.
 * @param derivedFeatureUtils  reference to derivations executor.
 */
private[offline] class DerivedFeatureGenStage(featureGroups: FeatureGroups, logicalPlan: MultiStageJoinPlan, derivedFeatureUtils: DerivedFeatureEvaluator)
  extends StageEvaluator[FeatureDataWithJoinKeys, FeatureDataWithJoinKeys] {

  /**
   * Computes derivations for the input features. Before applying the derivations, it ensures that
   * the dependent features required for computation are available on a single Datafeathr.
   * @param features  derived features to evaluate in this stage.
   * @param keyTags   key tags for the stage.
   * @param context   features evaluated thus far.
   * @return all evaluated features including the ones processed here.
   */
  override def evaluate(features: Seq[String], keyTags: Seq[Int], context: FeatureDataWithJoinKeys): FeatureDataWithJoinKeys = {
    val featureToDerivationMap = features.map(f => (featureGroups.allDerivedFeatures(f), f)).toMap
    featureToDerivationMap.foldLeft(context)((accumulator: FeatureDataWithJoinKeys, currFeatureDerivation) => {
      val (derivation, derivedFeatureName) = currFeatureDerivation
      val featureColumnName = DataFrameColName.genFeatureColumnName(derivedFeatureName)
      // Compute the base DataFrame that can be used to compute the derived feature.
      val BaseDataFrameMetadata(baseFeatureDataFrame, joinKeys, featuresOnBaseDf) =
        evaluateBaseDataFrameForDerivation(derivedFeatureName, derivation, accumulator)
      val derivedFeatureDataFrame =
        if (baseFeatureDataFrame.df.columns.contains(featureColumnName)) { // if DataFrame already has the feature, no need to apply derivations
          baseFeatureDataFrame
        } else {
          derivedFeatureUtils.evaluate(keyTags, logicalPlan.keyTagIntsToStrings, baseFeatureDataFrame.df, derivation)
        }
      val columnRenamedDf = dropFrameTagsAndRenameColumn(derivedFeatureDataFrame.df, featureColumnName)
      // Update featureTypeMap and features on DataFrame metadata
      val updatedFeatureTypeMap = baseFeatureDataFrame.inferredFeatureType ++ derivedFeatureDataFrame.inferredFeatureType
      val updatedFeaturesOnDf = featuresOnBaseDf :+ derivedFeatureName
      accumulator ++ updatedFeaturesOnDf.map(f => f -> (FeatureDataFrame(columnRenamedDf, updatedFeatureTypeMap), joinKeys)).toMap
    })
  }

  /**
   * Prepares a Base DataFrame that can be used to compute the derived features.
   * The dependent features of the derived feature may be present on different DataFrames.
   * In such cases, the DataFrames are joined so that the dependent features are available on a single Datafeathr.
   * @param derivedFeatureName  derived feature name.
   * @param derivedFeatureRef   derived feature representation.
   * @param evaluatedFeatures   features evaluated thus far.
   * @return BaseDataFrameMetadata that contains all required features to compute a derived feature.
   */
  def evaluateBaseDataFrameForDerivation(
    derivedFeatureName: String,
    derivedFeatureRef: DerivedFeature,
    evaluatedFeatures: FeatureDataWithJoinKeys): BaseDataFrameMetadata = {
    val featuresGroupedByDf = evaluatedFeatures.groupBy(_._2._1.df).mapValues(_.keySet) // features grouped by DataFrames
    val consumedFeatures = derivedFeatureRef.consumedFeatureNames.map(_.getFeatureName.toString)
    if (!consumedFeatures.forall(evaluatedFeatures.contains)) {
      throw new FeathrException(
        ErrorLabel.FEATHR_ERROR,
        s"Error when processing derived feature $derivedFeatureName. " +
          s"Requires following features to be generated [${consumedFeatures.mkString(", ")}], " +
          s"but found [${evaluatedFeatures.keySet.mkString(", ")}]")
    }
    val (headFeatureDataFrame, joinKeys) = evaluatedFeatures(consumedFeatures.head)
    val initialBaseDataFrame = BaseDataFrameMetadata(headFeatureDataFrame, joinKeys, featuresGroupedByDf(headFeatureDataFrame.df).toSeq)
    if (consumedFeatures.forall(featuresGroupedByDf(headFeatureDataFrame.df).contains)) {
      // if all dependent features are present in a single DataFrame, just return that.
      initialBaseDataFrame
    } else {
      // else, join all the DataFrames with "full_outer" join
      consumedFeatures.tail.foldLeft(initialBaseDataFrame)((accumulator: BaseDataFrameMetadata, consumedFeature) => {
        val FeatureDataFrame(leftDf, leftFeatureType) = accumulator.featureDataFrame
        val (currentFeatureDataFrame, currentJoinKey) = evaluatedFeatures(consumedFeature)
        val FeatureDataFrame(currentDf, currFeatureType) = currentFeatureDataFrame
        val featuresOnCurrentDf = featuresGroupedByDf(currentDf).toSeq
        if (joinKeys.size != currentJoinKey.size) {
          throw new FeathrException(
            ErrorLabel.FEATHR_ERROR,
            s"Error when processing derived feature $derivedFeatureName. " +
              s"Join Keys for dependent feature do not match. " +
              s"Expected join key: [${joinKeys.mkString(", ")}], Found join key: [${currentJoinKey.mkString(", ")}]")
        }
        val rightJoinKey = currentJoinKey.map(k => s"${k}_right_key")
        val rightDataFrame =
          currentJoinKey.zip(rightJoinKey).foldLeft(currentDf)((accumulatorDf, keyPair) => accumulatorDf.withColumnRenamed(keyPair._1, keyPair._2))
        val joinConditions = joinKeys
          .zip(rightJoinKey)
          .map { case (leftKey, rightKey) => leftDf(leftKey) === rightDataFrame(rightKey) }
          .reduce(_ and _)
        val joinedDataFrame = leftDf.join(rightDataFrame, joinConditions, "full_outer") // "full" is same as full_outer
        BaseDataFrameMetadata(
          // merge feature type mapping for features joined to the Datafeathr.
          FeatureDataFrame(joinedDataFrame.drop(rightJoinKey: _*), leftFeatureType ++ currFeatureType),
          joinKeys,
          (accumulator.featureNames ++ featuresOnCurrentDf).distinct)
      })
    }
  }

  /**
   * Feature transformation does not have the tag information when generating the features.
   * So during feature generation the evaluated features remain in a column that do not have tag information.
   * However, derived feature columns are created with tags. This helper method bridges the gap.
   * This helper method
   */
  private def dropFrameTagsAndRenameColumn(df: DataFrame, featureName: String): DataFrame = {
    val columnsInDf = df.columns
    columnsInDf.find(c => c.contains(featureName)) match {
      case Some(x) =>
        val y = x.split(FEATURE_TAGS_PREFIX).head
        df.withColumnRenamed(x, y)
      case None =>
        throw new FeathrException(
          ErrorLabel.FEATHR_ERROR,
          s"Unexpected Intenal Error: Could not find feature column $featureName " +
            s"in DataFrame with columns [${columnsInDf.mkString(", ")}]")
    }
  }
}

/**
 * Companion object to provide a better instantiation syntax.
 */
private[offline] object DerivedFeatureGenStage {
  def apply(featureGroups: FeatureGroups, logicalPlan: MultiStageJoinPlan, derivedFeatureUtils: DerivedFeatureEvaluator): DerivedFeatureGenStage =
    new DerivedFeatureGenStage(featureGroups, logicalPlan, derivedFeatureUtils)
}
