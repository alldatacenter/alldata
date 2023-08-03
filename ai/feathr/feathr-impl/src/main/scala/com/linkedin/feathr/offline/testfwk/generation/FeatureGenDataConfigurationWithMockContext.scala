package com.linkedin.feathr.offline.testfwk.generation

/**
 * wrapper of Feathr data configurations and their mock context(parameters)
 *
 * @param dataConfiguration Feathr data configuration
 * @param dataConfigurationMockContext Mock context
 */
class FeatureGenDataConfigurationWithMockContext(
    val dataConfiguration: FeatureGenDataConfiguration,
    val dataConfigurationMockContext: FeatureGenDataConfigurationMockContext) {}
