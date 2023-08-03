package com.linkedin.feathr.offline.transformation

import com.linkedin.feathr.common.FeatureTypes

/**
 * This case class encapsulates an MVEL definition for a feature. It contains the feature extraction as a String
 * and the feature type (if specified).
 * @param mvelDef      MVEL based feature extraction expression.
 * @param featureType  Feature Type if specified, else defaults to UNSPECIFIED.
 */
private[offline] case class MvelDefinition(mvelDef: String, featureType: FeatureTypes = FeatureTypes.UNSPECIFIED)
