package com.linkedin.feathr.offline.util
import org.apache.spark.sql.DataFrame


/**
 * An in-memory representation of Featurized DataSet in Spark pipelines.
 * Contains data as a [[DataFrame]], and a matching FDS metadata.

 */
case class SparkFeaturizedDataset(data: DataFrame, fdsMetadata: FeaturizedDatasetMetadata) {
}
