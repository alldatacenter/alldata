package com.linkedin.feathr.offline.job

import com.linkedin.feathr.offline.client.FeathrClient
import com.linkedin.feathr.offline.config.FeatureJoinConfig
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.source.dataloader.{DataLoaderFactory, DataLoaderHandler}
import com.linkedin.feathr.offline.source.{DataSource, SourceFormatType}
import com.linkedin.feathr.offline.util.FeathrTestUtils.createSparkSession
import com.linkedin.feathr.offline.util.{FeaturizedDatasetMetadata, SparkFeaturizedDataset}
import org.apache.spark.sql.SparkSession

/**
 * This object is used to help user to test/debug their workflow locally with sample data. This class serves as a
 * wrapper around [[FeathrClient]] class and invokes the FeathrClient#joinFeatures method by creating a spark session.
 */
object LocalFeatureJoinJob {

  // for user convenience, create spark session within this function, so user does not need to create one
  // this also ensure it has same setting as the real feathr join job
  val ss: SparkSession = createSparkSession(enableHiveSupport = true)

  /**
   * local debug API, used in unit test and local debug
   *
   * @param joinConfigAsHoconString  feature join config as HOCON config string
   * @param featureDefAsString feature def config
   * @param observationData  observation data
   * @return observation with joined features as a SparkFeaturizedDataset
   */
  @deprecated(message = "HOCON config is deprecated. Please use joinWithObs with PDL join config")
  def joinWithObsDFAndHoconJoinConfig(
      joinConfigAsHoconString: String,
      featureDefAsString: String,
      observationData: SparkFeaturizedDataset,
      extraParams: Array[String] = Array(),
      ss: SparkSession = ss,
      dataPathHandlers: List[DataPathHandler],
      mvelContext: Option[FeathrExpressionExecutionContext]): SparkFeaturizedDataset = {
    val joinConfig = FeatureJoinConfig.parseJoinConfig(joinConfigAsHoconString)
    val feathrClient = FeathrClient.builder(ss).addFeatureDef(featureDefAsString).addDataPathHandlers(dataPathHandlers)
      .addFeathrExpressionContext(mvelContext).build()
    val outputPath: String = FeatureJoinJob.SKIP_OUTPUT

    val defaultParams = Array(
      "--local-mode",
      "--feathr-config",
      "",
      "--output",
      outputPath)

    val jobContext = FeatureJoinJob.parseInputArgument(defaultParams ++ extraParams).jobJoinContext
    SparkFeaturizedDataset(feathrClient.joinFeatures(joinConfig, observationData, jobContext).data, FeaturizedDatasetMetadata())
  }

  /**
   * local debug API, used in unit test and local debug
   * @param joinConfigAsHoconString feature join config
   * @param featureDefAsString feature def config
   * @param observationDataPath  observation data
   * @return observation with joined features as a dataframe
   */
  def joinWithHoconJoinConfig(
      joinConfigAsHoconString: String,
      featureDefAsString: String,
      observationDataPath: String,
      extraParams: Array[String] = Array(),
      ss: SparkSession = ss,
      dataPathHandlers: List[DataPathHandler],
      mvelContext: Option[FeathrExpressionExecutionContext]=None): SparkFeaturizedDataset = {
    val dataLoaderHandlers: List[DataLoaderHandler] = dataPathHandlers.map(_.dataLoaderHandler)
    val obsDf = loadObservationAsFDS(ss, observationDataPath,dataLoaderHandlers=dataLoaderHandlers)
    joinWithObsDFAndHoconJoinConfig(joinConfigAsHoconString, featureDefAsString, obsDf, extraParams, ss, dataPathHandlers=dataPathHandlers, mvelContext)
  }

  /**
   * Load observation data as SparkFeaturizedDataset
   * @param ss spark session
   * @param obsDataPath HDFS path for observation
   * @return a SparkFeaturizedDataset
   */
  def loadObservationAsFDS(ss: SparkSession, obsDataPath: String, dataLoaderHandlers: List[DataLoaderHandler]): SparkFeaturizedDataset = {
    val source = DataSource(obsDataPath, SourceFormatType.FIXED_PATH)
    val dataLoaderFactory = DataLoaderFactory(ss, dataLoaderHandlers=dataLoaderHandlers)

    val data = source.pathList.map(dataLoaderFactory.create(_).loadDataFrame()).reduce(_ union _)
    SparkFeaturizedDataset(data, FeaturizedDatasetMetadata())
  }
}

/**
 * Convenient class to hold config strings for local testing of workflows. See [[LocalFeatureJoinJob]].
 * @param localConfig Optional local config string
 * @param featureConfig FeatureDef config string
 */
case class LocalTestConfig(localConfig: Option[String], featureConfig: Option[String])
