package com.linkedin.feathr.offline.config

import com.linkedin.feathr.common.{FeatureTypeConfig, PegasusFeatureTypeResolver}
import com.linkedin.feathr.compute.FeatureVersion

/**
 * Class to convert [[FeatureTypeConfig]] from [[FeatureVersion]]
 */
private[offline] class PegasusRecordFeatureTypeConverter private (pegasusFeatureTypeResolver: PegasusFeatureTypeResolver) {

  private val _pegasusFeatureTypeResolver = pegasusFeatureTypeResolver

  /**
   * Convert feathr-Core FeatureTypeConfig to Offline [[FeatureTypeConfig]]
   */
  def convert(featureVersion: FeatureVersion): Option[FeatureTypeConfig] = {
    // for now, convert CommonFeatureTypeConfig to CoreFeatureTypeConfig
    // TODO after integ, remove CoreFeatureTypeConfig, and use CommonFeautreTypeConfig everywhere
    if (featureVersion.hasType) {
      val commonFeatureTypeConfig = _pegasusFeatureTypeResolver.resolveFeatureType(featureVersion)
      val featureTypeConfig = new FeatureTypeConfig(commonFeatureTypeConfig.getFeatureType, commonFeatureTypeConfig.getTensorType, "No documentation")
      Some(featureTypeConfig)
    } else None
  }

  /**
   * Convert [[Option[FeatureTypeConfig]]] to a Map:
   * 1. if [[FeatureTypeConfig]] exist, then create a singleton map from feature name to the [[FeatureTypeConfig]] object
   * 2. otherwise return an empty Map
   * @param featureNameRef feature name
   * @param typeConfig Option of [[FeatureTypeConfig]]
   * @return mapping from feature name to the [[FeatureTypeConfig]] object
   */
  def parseFeatureTypeAsMap(featureNameRef: String, typeConfig: Option[FeatureTypeConfig]): Map[String, FeatureTypeConfig] = {
    typeConfig match {
      case Some(typeInfo) => Map(featureNameRef -> typeInfo)
      case None => Map.empty
    }
  }
}

private[offline] object PegasusRecordFeatureTypeConverter {
  def apply(): PegasusRecordFeatureTypeConverter = {
    new PegasusRecordFeatureTypeConverter(PegasusFeatureTypeResolver.getInstance)
  }

  def apply(pegasusFeatureTypeResolver: PegasusFeatureTypeResolver): PegasusRecordFeatureTypeConverter = {
    new PegasusRecordFeatureTypeConverter(pegasusFeatureTypeResolver)
  }
}

