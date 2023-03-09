package com.linkedin.feathr.offline.config

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.derived.DerivedFeature
import com.linkedin.feathr.offline.derived.functions.SeqJoinDerivationFunction
import com.linkedin.feathr.offline.logical.FeatureGroups
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils
import org.apache.logging.log4j.LogManager

/**
 * This is a utility class which helps in parsing the Feathr config files, featureDefConfigs and localOverrideDefConfigs, and produces
 * a [[FeatureGroups]] object. This object comprises of AnchoredFeatures, DerivedFeatures, PassthroughFeatures, WindowAggFeatures
 * and SeqJoinFeatures.
 * The input to this class consists of feature def configs and local override def configs. local override def configs override the definition of
 * any feature defined in the feature def config.
 * The getFeatureGroups method will return the FeatureGroups object.
 */
private[offline] class FeatureGroupsGenerator(featureDefConfigs: Seq[FeathrConfig], localOverrideDefConfigs: Option[Seq[FeathrConfig]] = None) {
  private val log = LogManager.getLogger(getClass)
  // Many users use the feature def config interchangibly with the localDef Config. We want to support those use cases
  // where users write the featureDefConfig as a localDdefConfig
  private val configs = if (featureDefConfigs.isEmpty) localOverrideDefConfigs.get else featureDefConfigs

  /**
   * Returns the [[FeatureGroups]] object given a sequence of feature configs.
   * @return  [[FeatureGroups]] object
   */
  def getFeatureGroups(): FeatureGroups = {
    val allAnchoredFeatures = getAnchoredFeaturesMap()
    val allDerivedFeatures = getDerivedFeaturesMap()
    val allPassThroughFeatures = getPassThroughFeaturesMap(allAnchoredFeatures)
    val allWindowAggFeatures = getWindowFeaturesMap(allAnchoredFeatures)
    val allSeqJoinFeatures = getSeqJoinFeaturesMap(allDerivedFeatures)
    FeatureGroups(allAnchoredFeatures, allDerivedFeatures, allWindowAggFeatures, allPassThroughFeatures, allSeqJoinFeatures)
  }

  /**
   * Parse all the anchored features.
   * @return A map from the feature name to the [[FeatureAnchorWithSource]]
   */
  private def getAnchoredFeaturesMap(): Map[String, FeatureAnchorWithSource] = {
    val result = scala.collection.mutable.HashMap.empty[String, FeatureAnchorWithSource]

    // Set to check for duplicates feature names in local override feature def configs
    val localOverrideAnchorFeatureNames = scala.collection.mutable.HashSet.empty[String]
    configs
      .map(_.anchoredFeatures)
      .foreach(m =>
        m.foreach {
          case (k, v) =>
            // Check if there is any duplicate of keys:
            val exist = result.get(k)
            if (exist.isDefined) {
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                s"We detected a duplicate Anchor feature '$k' in your feature def config. Please remove one of them or rename the feature.")
            } else {
              result.put(k, v)
            }
      })
    if (!localOverrideDefConfigs.isEmpty) {
      // use local override def config to override existing anchor features
      localOverrideDefConfigs.get
        .map(_.anchoredFeatures)
        .foreach(m =>
          m.foreach {
            case (k, v) =>
              if (localOverrideAnchorFeatureNames.contains(k)) { // This feature has been repeated in one of the other local override def configs
                throw new FeathrConfigException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"We detected a duplicate Anchor feature '$k' in your local override def config. Please remove one of them or rename the feature.")
              } else {
                localOverrideAnchorFeatureNames.add(k)
              }
              val exist = result.get(k)
              if (exist.isDefined) {
                // local defintion overriding a feature definition in the feature def config
                log.info(s"Anchor feature '$k' is being overridden by '${exist.get}'")
              }
              result.put(k, v)
        })
    }
    result.toMap
  }

  /**
   * Parse all the derived features.
   * @return A map from the feature name to the [[DerivedFeature]]
   */
  private def getDerivedFeaturesMap(): Map[String, DerivedFeature] = {
    val result = scala.collection.mutable.HashMap.empty[String, DerivedFeature]
    // Set to check for duplicates feature names in local override feature names
    val localOverrideDerivedFeatureNames = scala.collection.mutable.HashSet.empty[String]
    configs
      .map(_.derivedFeatures)
      .foreach(m =>
        m.foreach {
          case (k, v) =>
            // Check if there is any duplicate of keys:
            val exist = result.get(k)
            if (exist.isDefined) {
              throw new FeathrConfigException(
                ErrorLabel.FEATHR_USER_ERROR,
                s"We detected a duplicate derived feature '$k' in your feature def config. Please remove one of them or rename the feature.")
            } else {
              result.put(k, v)
            }
      })
    if (!localOverrideDefConfigs.isEmpty) {
      // use local config to override existing derived features
      localOverrideDefConfigs.get
        .map(_.derivedFeatures)
        .foreach(m =>
          m.foreach {
            case (k, v) =>
              if (localOverrideDerivedFeatureNames.contains(k)) {
                throw new FeathrConfigException(
                  ErrorLabel.FEATHR_USER_ERROR,
                  s"We detected a duplicate derived feature '$k' in your local override def config. Please remove one of them or rename the feature.")
              } else {
                localOverrideDerivedFeatureNames.add(k)
              }
              val exist = result.get(k)
              if (exist.isDefined) {
                // local definition overriding a feature definition in the feature def config
                log.info(s"Derived feature '$k' is being overridden by '${exist.get}'")
              }
              result.put(k, v)
        })
    }
    result.toMap
  }

  /**
   * Parse all the passthrough features using the list of anchored features.
   * Passthrough features are always considered anchored.
   * @param allAnchoredFeaturesMap  Map from anchor feature name to the [[FeatureAnchorWithSource]] object
   * @return A map from the feature name to the [[FeatureAnchorWithSource]]
   */
  private def getPassThroughFeaturesMap(allAnchoredFeaturesMap: Map[String, FeatureAnchorWithSource]): Map[String, FeatureAnchorWithSource] = {
    allAnchoredFeaturesMap.filter({
      case (_: String, anchorConfig: FeatureAnchorWithSource) =>
        anchorConfig.source.path == "PASSTHROUGH"
    })
  }

  /**
   * Parse all the passthrough features using the list of anchored features.
   * Window agg features are always considered anchored.
   * @param allAnchoredFeaturesMap  Map from anchor feature name to the [[FeatureAnchorWithSource]] object
   * @return A map from the feature name to the [[FeatureAnchorWithSource]]
   */
  private def getWindowFeaturesMap(allAnchoredFeaturesMap: Map[String, FeatureAnchorWithSource]): Map[String, FeatureAnchorWithSource] = {
    allAnchoredFeaturesMap.filter({
      case (featureName: String, anchorConfig: FeatureAnchorWithSource) =>
        SlidingWindowFeatureUtils.isWindowAggAnchor(anchorConfig)
    })
  }

  /**
   * Parse all the seqJoin features using the list of derived features.
   * seq join features are always considered derived.
   * @param allDerivedFeaturesMap  Map from derived feature name to the [[DerivedFeature]] object
   * @return A map from the feature name to the [[DerivedFeature]]
   */
  private def getSeqJoinFeaturesMap(allDerivedFeaturesMap: Map[String, DerivedFeature]): Map[String, DerivedFeature] = {
    allDerivedFeaturesMap.filter({
      case (_, featureObj: DerivedFeature) => featureObj.derivation.isInstanceOf[SeqJoinDerivationFunction]
    })
  }
}

/**
 * Companion object for [[FeatureGroupsGenerator]] class which helps in instantiating the class.
 */
private[offline] object FeatureGroupsGenerator {
  def apply(featureDefconfigs: Seq[FeathrConfig], localDefConfigs: Option[Seq[FeathrConfig]] = None): FeatureGroupsGenerator = {
    if (featureDefconfigs.isEmpty && !localDefConfigs.isDefined) {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, s"Atleast one of featureDefConfig or localDefConfig must be defined")
    }
    new FeatureGroupsGenerator(featureDefconfigs, localDefConfigs)
  }

  // Common use case where there is only a featureDefConfig
  def apply(featureDefconfigs: Seq[FeathrConfig]): FeatureGroupsGenerator = {
    new FeatureGroupsGenerator(featureDefconfigs, None)
  }
}
