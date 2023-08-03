package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException, FeathrException}
import com.linkedin.feathr.common.{DateTimeParam, DateTimeUtils, JoiningFeatureParams, TaggedFeatureName}
import com.linkedin.feathr.offline.anchored.anchorExtractor.TimeWindowConfigurableAnchorExtractor
import com.linkedin.feathr.offline.job.FeatureGenSpec
import com.linkedin.feathr.offline.logical.FeatureGroups

import scala.annotation.tailrec
import scala.collection.convert.wrapAll._

/**
 * Trait for key tag analysis during feature generation.
 * The trait can be used to create "mock" instance for unit testing.
 */
private[offline] trait FeatureGenKeyTagAnalyzer {

  /**
   * Infer key tags for anchored features in [[FeatureGroups]].
   */
  def inferKeyTagsForAnchoredFeatures(featureGenSpec: FeatureGenSpec, featureGroups: FeatureGroups): Seq[JoiningFeatureParams]

  /**
   * Infer key tags for derived features in [[FeatureGroups]].
   */
  def inferKeyTagsForDerivedFeatures(
      featureGenSpec: FeatureGenSpec,
      featureGroups: FeatureGroups,
      keyTaggedAnchoredFeatures: Seq[JoiningFeatureParams]): Seq[JoiningFeatureParams]
}

/**
 * This class encapsulates API that are used to assign keyTags to features during feature generation.
 */
private[offline] object FeatureGenKeyTagAnalyzer extends FeatureGenKeyTagAnalyzer {

  /**
   * infer the key tags for a list of feature names.
   * This is ONLY used in feature generation, not feature join, as the inferred key tag might not be a valid field
   * name or field name based expression, hence cannot be evaluated against any dataset (e.g., 'observation' data
   * in feature join). It is only intended to be used to group features that share same underlying key tags in
   * feature generation, and provide a valid column name(without non-alphaNum charactors) in the feature join and feature
   * generation output dataframe.
   *
   * This is achieved by inspecting all the 'keyAlias' or 'key' field in the feature definition config file,
   * and/or the getKeyColumnNames() of KeySourceExtractor
   *
   * @return the inferred key tagged feature names
   */
  def inferKeyTagsForAnchoredFeatures(featureGenSpec: FeatureGenSpec, featureGroups: FeatureGroups): Seq[JoiningFeatureParams] = {
    val features = getDependentFeatures(featureGenSpec.getFeatures(), featureGroups)
    // Feature generation does not support Passthrough features.
    val passthroughFeatures = features.filter(featureGroups.allPassthroughFeatures.contains)
    if (passthroughFeatures.nonEmpty) {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Feature generation does not support Passthrough features. Following passthrough features are explicitly requested," +
          s" or have been evaluated as required to generate derived features: [${passthroughFeatures.mkString(",")}]")
    }
    val anchoredFeatures = features.filter(featureGroups.allAnchoredFeatures.contains)
    val allAnchoredFeatures = featureGroups.allAnchoredFeatures

    // group feature by source
    val inferredAnchorFeatures = anchoredFeatures
      .groupBy(f => allAnchoredFeatures(f).source)
      .flatMap {
        case (_, featureWithSameSource) =>
          val featureToKeyExprs = featureWithSameSource.map(f => {
            val keyExtractor = allAnchoredFeatures(f).featureAnchor.sourceKeyExtractor
            val keyAlias = keyExtractor.getKeyColumnAlias()
            // if keyColumns is empty, that means it is using the user customized interface AnchorExtractor[], and they don't
            // provide the keyAlias either, in this case, we cannot infer their key tags, we could just assign a unique key
            // to make each of them as in one group, e.g., use UUID, however, this would likely result in much more feature
            // groups in feature generation, which would affect the performance significantly. So we decide to force user to
            // provide keyAlias, which should require just trivial effort.
            if (keyAlias.isEmpty) {
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                s"Inferred key tags from ${keyExtractor} is empty, this seems to " +
                  s"be an unsupported user customized class extends AnchorExtractor[_], please add keyAlias to the feature definition of the feature $f")
            }
            val keyExtractorIdentifier = keyExtractor.getClass.getCanonicalName
            f -> (keyExtractorIdentifier, keyAlias)
          })
          // group by (key extractor, keyExpressions), because even same key expression/parameters in the context
          // of different key extractors (mvel/sql) might have different evaluation results
          featureToKeyExprs.groupBy(_._2).flatMap {
            case ((_, keyTags), featureWithSameKeys) =>
              val featureNames = featureWithSameKeys.map(_._1)
              featureNames.map(f => new TaggedFeatureName(keyTags, f))
          }
      }
      .toSeq
    convertToJoiningFeatureParams(inferredAnchorFeatures, featureGenSpec, featureGroups)
  }

  // convert tagged feature name to a string tagged feature name with DateParam
  private def convertToJoiningFeatureParams(
      taggedFeature: Seq[TaggedFeatureName],
      featureGenSpec: FeatureGenSpec,
      featureGroups: FeatureGroups): Seq[JoiningFeatureParams] = {
    val refTime = featureGenSpec.dateTimeParam
    taggedFeature.map(f => {
      val featureName = f.getFeatureName
      val featureAnchorWithSource = featureGroups.allAnchoredFeatures(featureName)
      val dateParam = featureAnchorWithSource.featureAnchor.extractor match {
        case extractor: TimeWindowConfigurableAnchorExtractor =>
          val aggFeature = extractor.features(featureName)
          val dateTimeParam = DateTimeParam.shiftStartTime(refTime, aggFeature.window)
          DateTimeUtils.toDateParam(dateTimeParam)
        case _ =>
          featureGenSpec.dateParam
      }
      new JoiningFeatureParams(f.getKeyTag, f.getFeatureName, Option(dateParam))
    })
  }

  /**
   * Visit the dependency tree (if any) for features and retrieve all features.
   * @param features a collection of input features. Can be anchored / derived.
   * @return a collection of all distinct required features.
   */
  private def getDependentFeatures(features: Seq[String], featureGroups: FeatureGroups): Seq[String] = {
    @tailrec
    def visit(acc: Seq[String], derivedFeatures: Seq[String]): Seq[String] = {
      if (derivedFeatures.isEmpty) {
        acc // base condition
      } else {
        val dependentFeatures = derivedFeatures.map(featureGroups.allDerivedFeatures).flatMap(f => f.consumedFeatureNames.map(_.getFeatureName))
        val (derived, _) = dependentFeatures.partition(featureGroups.allDerivedFeatures.contains)
        visit(acc ++ dependentFeatures, derived)
      }
    }
    val (derived, _) = features.partition(featureGroups.allDerivedFeatures.contains)
    visit(features, derived).distinct
  }

  /**
   * Compute KeyTags for derived features.
   */
  def inferKeyTagsForDerivedFeatures(
      featureGenSpec: FeatureGenSpec,
      featureGroups: FeatureGroups,
      keyTaggedAnchoredFeatures: Seq[JoiningFeatureParams]): Seq[JoiningFeatureParams] = {
    val allAnchoredFeatures = featureGroups.allAnchoredFeatures
    val allDerivedFeatures = featureGroups.allDerivedFeatures

    /**
     * Helper method to detect cross join scenarios.
     */
    def validateNoCrossJoin(inferredKeyTagsMap: Map[String, Seq[String]], derivedFeatures: Seq[String]): Unit = {
      val failedFeatures = derivedFeatures.filter(f =>
        allDerivedFeatures(f).consumedFeatureNames.map(_.getFeatureName).forall(x => inferredKeyTagsMap(x) != inferredKeyTagsMap(f)))
      if (failedFeatures.nonEmpty) {
        throw new FeathrException(
          ErrorLabel.FEATHR_ERROR,
          s"Feature Generation currently does not support cross join and sequential join, at the moment. " +
            s"Features violating this constraint: : [${failedFeatures.mkString(", ")}]")
      }
    }

    /**
     * Retrieve keyTags for dependent features.
     * Recursively computes keyTags if a dependent feature is a derived feature.
     * @param feature              feature to compute keyTags for.
     * @param anchoredToKeyTagMap  inferred keyTag map for anchored features.
     * @return keyTags for specified feature.
     */
    def getKeyTagsForConsumedFeature(feature: String, anchoredToKeyTagMap: Map[String, Seq[String]]): Seq[String] = {
      feature match {
        case anchoredFeature if allAnchoredFeatures.contains(anchoredFeature) =>
          if (!anchoredToKeyTagMap.contains(anchoredFeature)) {
            throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Could not find inferred keyTags for anchored feature ${feature}")
          } else {
            anchoredToKeyTagMap(anchoredFeature)
          }
        case derivedFeature if allDerivedFeatures.contains(derivedFeature) =>
          allDerivedFeatures(derivedFeature).consumedFeatureNames
            .map(_.getFeatureName)
            .foldLeft(Seq.empty[String])((acc, feature) => acc ++ getKeyTagsForConsumedFeature(feature, anchoredToKeyTagMap))
        case _ =>
          throw new FeathrException(
            ErrorLabel.FEATHR_ERROR,
            s"Unrecognized feature group for feature generation. Feature $feature does not belong to anchored or derived feature groups.")
      }
    }
    val derivedFeatures = featureGenSpec.getFeatures().filter(allDerivedFeatures.contains)
    val anchoredToKeyTagsMap = keyTaggedAnchoredFeatures.map { case joiningFeatureParams => (joiningFeatureParams.featureName,
      joiningFeatureParams.keyTags) }.toMap
    val derivedToInferredKeyTagsMap = derivedFeatures.map(f => (f, getKeyTagsForConsumedFeature(f, anchoredToKeyTagsMap).distinct)).toMap
    validateNoCrossJoin(anchoredToKeyTagsMap ++ derivedToInferredKeyTagsMap, derivedFeatures)
    derivedToInferredKeyTagsMap.map {
      case (feature, keyTags) => JoiningFeatureParams(keyTags, feature)
    }.toSeq
  }
}
