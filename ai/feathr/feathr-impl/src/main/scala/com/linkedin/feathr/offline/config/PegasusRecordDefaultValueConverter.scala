package com.linkedin.feathr.offline.config

import com.linkedin.feathr.common.{FeatureValue, PegasusDefaultFeatureValueResolver}
import com.linkedin.feathr.compute.FeatureVersion

private[offline] class PegasusRecordDefaultValueConverter private (
  pegasusDefaultFeatureValueResolver: PegasusDefaultFeatureValueResolver) {

  private val _pegasusDefaultFeatureValueResolver = pegasusDefaultFeatureValueResolver

  /**
   * Convert feathr-Core FeatureTypeConfig to Offline [[FeatureTypeConfig]]
   */
  def convert(features: Map[String, FeatureVersion]): Map[String, FeatureValue] = {
    features
      .transform((k, v) => _pegasusDefaultFeatureValueResolver.resolveDefaultValue(k, v))
      .filter(_._2.isPresent)
      .mapValues(_.get)
      // get rid of not serializable exception:
      // https://stackoverflow.com/questions/32900862/map-can-not-be-serializable-in-scala/32945184
      .map(identity)
  }
}

private[offline] object PegasusRecordDefaultValueConverter {
  def apply(): PegasusRecordDefaultValueConverter = {
    new PegasusRecordDefaultValueConverter(PegasusDefaultFeatureValueResolver.getInstance)
  }
}
