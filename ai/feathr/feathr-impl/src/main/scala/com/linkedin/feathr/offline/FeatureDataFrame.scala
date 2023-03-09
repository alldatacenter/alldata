package com.linkedin.feathr.offline

import com.linkedin.feathr.common.FeatureTypeConfig
import org.apache.spark.sql.DataFrame

/**
 * Representation of feature data in Feathr offline.
 * Contains the DataFrame holding the data and the type information for the features.
 *
 * @param df                  transformed feature dataframe
 * @param inferredFeatureType inferred feature type
 */
private[offline] case class FeatureDataFrame(df: DataFrame, inferredFeatureType: Map[String, FeatureTypeConfig])
