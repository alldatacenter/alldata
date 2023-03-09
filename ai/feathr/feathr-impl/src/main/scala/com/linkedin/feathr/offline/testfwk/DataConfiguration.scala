package com.linkedin.feathr.offline.testfwk

import com.linkedin.feathr.offline.source.DataSource

/**
 * Feathr Join configuration objects
 * @param featureDefContext feature definition context
 * @param observationSource observation sources
 * @param joinConfigAsString join config string as as string
 * @param isPdlJoinConfig when it's true, joinConfigAsString is in PDL JSON format, otherwise, the joinConfigAsString is in old HOCON format
 */
private[feathr] class DataConfiguration(
    val featureDefContext: FeatureDefContext,
    val observationSource: DataSource,
    val joinConfigAsString: String,
    val isPdlJoinConfig: Boolean) {
  val featureSources: List[DataSource] = featureDefContext.featureSources
  val localFeatureDefConfig: Option[String] = featureDefContext.featureDefConfig
}
