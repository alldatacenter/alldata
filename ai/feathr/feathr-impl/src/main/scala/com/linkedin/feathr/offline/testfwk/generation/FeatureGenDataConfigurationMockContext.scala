package com.linkedin.feathr.offline.testfwk.generation

import com.linkedin.feathr.offline.testfwk.FeatureDefMockContext

/**
 * class to host to context to generate the mock data
 *
 * @param rewrittenFeatureGenDef rewritten feathr join config
 */
private[feathr] class FeatureGenDataConfigurationMockContext(val featureDefMockContext: FeatureDefMockContext, val rewrittenFeatureGenDef: String) {}
