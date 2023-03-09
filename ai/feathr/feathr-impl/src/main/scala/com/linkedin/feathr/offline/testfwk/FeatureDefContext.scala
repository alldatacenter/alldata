package com.linkedin.feathr.offline.testfwk

import com.linkedin.feathr.offline.source.DataSource

/**
 * Feature defintions
 * @param featureSources sources
 * @param featureDefConfig feature def as string
 */
private[feathr] class FeatureDefContext(val featureSources: List[DataSource], val featureDefConfig: Option[String]) {}
