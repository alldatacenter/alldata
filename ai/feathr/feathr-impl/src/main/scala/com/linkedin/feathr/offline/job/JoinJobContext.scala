package com.linkedin.feathr.offline.job

import com.linkedin.feathr.offline.client.InputData
import com.linkedin.feathr.offline.config.location.{DataLocation, SimplePath}


object JoinJobContext {
}

/**
 * Class the holds the context for the join job. This includes parameters specifying configs and observation data,
 * among other parameters.
 * @param feathrLocalConfig Optional. Path to local FeatureDef config, that is, config in user's own repo.
 * @param feathrFeatureConfig Optional. Path to FeatureDef config.
 * @param localOverrideAll If set to true, local definitions override the ones in FeatureDef config
 * @param inputData Observation data
 * @param outputPath HDFS path to which the joined observation and feature data will be written to
 * @param numParts Number of output partitions
 * @param rowBloomFilterThreshold A threshold for applying BloomFilter to feature data set. If set to 0, it's disabled,
 *                                if set to -1, it's force enabled, and for other values > 0, BloomFilter is applied
 *                                if observation data set rows < threshold. If not set, it's internal value is set to -1.
 */
case class JoinJobContext(
                           feathrLocalConfig: Option[String] = None,
                           feathrFeatureConfig: Option[String] = None,
                           inputData: Option[InputData] = None,
                           outputPath: DataLocation = SimplePath("/join_output"),
                           numParts: Int = 1
                          ) {
}
