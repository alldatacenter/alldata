package com.linkedin.feathr.offline.testfwk

/**
 * class to host to context to generate the mock data
 * @param featureDefMockContext mock parameters for feature sources
 * @param observationMockParams mock parameters for observation
 * @param rewrittenJoinDef rewritten feathr join config
 * @param isPdlJoinConfig when it's true, rewrittenJoinDef is in PDL JSON format, otherwise, the joinConfigAsString is in old HOCON format
 */
private[feathr] class DataConfigurationMockContext(
    val featureDefMockContext: FeatureDefMockContext,
    val observationMockParams: SourceMockParam,
    val rewrittenJoinDef: String,
    val isPdlJoinConfig: Boolean) {}
