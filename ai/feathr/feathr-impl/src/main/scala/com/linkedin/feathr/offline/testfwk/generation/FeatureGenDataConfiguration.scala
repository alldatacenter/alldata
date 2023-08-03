package com.linkedin.feathr.offline.testfwk.generation

import com.linkedin.feathr.offline.job.FeatureGenSpec
import com.linkedin.feathr.offline.source.DataSource

/**
 * Feathr Feature generation configuration objects
 *
 * @param featureSources feature sources
 * @param genConfigAsString feature generation config string as as string
 * @param localFeatureDefConfig local feature definition as a string
 * @param featureGenSpec parsed feature generation config object
 */
private[feathr] class FeatureGenDataConfiguration(
    val featureSources: List[DataSource],
    val genConfigAsString: String,
    val localFeatureDefConfig: Option[String],
    val featureGenSpec: FeatureGenSpec) {}
