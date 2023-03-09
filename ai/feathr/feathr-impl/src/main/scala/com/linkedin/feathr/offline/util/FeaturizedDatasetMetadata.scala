package com.linkedin.feathr.offline.util

import com.linkedin.feathr.common.Header
/**
 * The metadata for FeaturizedDataset
 * @param meta extra metadata
 * @param header feature type header info
 */
case class FeaturizedDatasetMetadata(meta: Map[String, String] = Map(), header: Option[Header] = None ) {
}
