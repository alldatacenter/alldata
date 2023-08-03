package com.linkedin.feathr.offline.graph

import com.linkedin.feathr.common.{FeatureTypeConfig, FeatureValue, JoiningFeatureParams}
import com.linkedin.feathr.compute.{AnyNode, Transformation}
import com.linkedin.feathr.compute.Resolver.FeatureRequest
import com.linkedin.feathr.offline.anchored.WindowTimeUnit
import com.linkedin.feathr.offline.config.{FeatureJoinConfig, PegasusRecordDefaultValueConverter, PegasusRecordFeatureTypeConverter}
import com.linkedin.feathr.offline.util.FCMUtils.makeFeatureNameForDuplicates

import java.time.Duration
import scala.collection.JavaConverters.seqAsJavaListConverter

/**
 * This object class contains helper functions which extract information (like feature type and default values) from nodes
 * and returns them in data formats which our API's can work with.
 */
object NodeUtils {
  /**
   * Given the feathr join config, create the list of FeatureRequest to be consumed by the FCM graph resolver.
   * @param joinConfig feathr join config
   * @return List of FeatureRequest to be consumed by FCM graph resolver
   */
  def getFeatureRequestsFromJoinConfig(joinConfig: FeatureJoinConfig): List[FeatureRequest] = {
    val featureNames = joinConfig.joinFeatures.map(_.featureName)
    val duplicateFeatureNames = featureNames.diff(featureNames.distinct).distinct
    joinConfig.joinFeatures.map {
      case JoiningFeatureParams(keyTags, featureName, dateParam, timeDelay, featureAlias) =>
        val delay = if (timeDelay.isDefined) {
          WindowTimeUnit.parseWindowTime(timeDelay.get)
        } else {
          if (joinConfig.settings.isDefined && joinConfig.settings.get.joinTimeSetting.isDefined &&
            joinConfig.settings.get.joinTimeSetting.get.simulateTimeDelay.isDefined) {
            joinConfig.settings.get.joinTimeSetting.get.simulateTimeDelay.get
          } else {
            Duration.ZERO
          }
        }
        // In the case of duplicate feature names in the join config, according to feathr offline specs the feature name will be created as
        // keys + __ + name. For example a feature "foo" with keys key0 and key1 will be named key0_key1__foo.
        if (duplicateFeatureNames.contains(featureName)) {
          new FeatureRequest(featureName, keyTags.toList.asJava, delay, makeFeatureNameForDuplicates(keyTags, featureName))
        } else {
          new FeatureRequest(featureName, keyTags.toList.asJava, delay, featureAlias.orNull)
        }
    }.toList
  }

  /**
   * Create map of feature name to feature type config
   * @param nodes Seq of any nodes.
   * @return Map of node id to feature type config
   */
  def getFeatureTypeConfigsMap(nodes: Seq[AnyNode]): Map[String, FeatureTypeConfig] = {
    nodes.filter(node => node.isLookup || node.isAggregation || node.isTransformation).map {
      case n if n.isTransformation => n.getTransformation.getFeatureName -> PegasusRecordFeatureTypeConverter().convert(n.getTransformation.getFeatureVersion)
      case n if n.isLookup => n.getLookup.getFeatureName -> PegasusRecordFeatureTypeConverter().convert(n.getLookup.getFeatureVersion)
      case n if n.isAggregation => n.getAggregation.getFeatureName -> PegasusRecordFeatureTypeConverter().convert(n.getAggregation.getFeatureVersion)
    }.collect { case (key, Some(value)) => (key, value) }.toMap // filter out Nones and get rid of Option
  }

  /**
   * Create map of feature name to feature type config
   * @param nodes Seq of Transformation nodes
   * @return Map of node id to feature type config
   */
  def getFeatureTypeConfigsMapForTransformationNodes(nodes: Seq[Transformation]): Map[String, FeatureTypeConfig] = {
      nodes.map { n => n.getFeatureName -> PegasusRecordFeatureTypeConverter().convert(n.getFeatureVersion)
    }.collect { case (key, Some(value)) => (key, value) }.toMap // filter out Nones and get rid of Option
  }

  /**
   * Create default value converter for nodes
   * @param nodes Seq of any nodes
   * @return Map[String, FeatureValue] where key is feature name.
   */
  def getDefaultConverter(nodes: Seq[AnyNode]): Map[String, FeatureValue] = {
    val featureVersionMap = nodes.filter(node => node.isLookup || node.isAggregation || node.isTransformation).map {
      case n if n.isTransformation => n.getTransformation.getFeatureName -> n.getTransformation.getFeatureVersion
      case n if n.isLookup => n.getLookup.getFeatureName -> n.getLookup.getFeatureVersion
      case n if n.isAggregation => n.getAggregation.getFeatureName -> n.getAggregation.getFeatureVersion
    }.toMap
    PegasusRecordDefaultValueConverter().convert(featureVersionMap)
  }

  /**
   * Create default value converter for Transformation nodes
   * @param nodes Seq of Transformation
   * @return Map[String, FeatureValue] where key is feature name.
   */
  def getDefaultConverterForTransformationNodes(nodes: Seq[Transformation]): Map[String, FeatureValue] = {
    val featureVersionMap = nodes.map { n => n.getFeatureName -> n.getFeatureVersion }.toMap
    PegasusRecordDefaultValueConverter().convert(featureVersionMap)
  }
}

