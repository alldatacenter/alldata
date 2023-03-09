package com.linkedin.feathr.offline.job

import com.linkedin.feathr.common.TaggedFeatureName
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.offline.client.FeathrClient
import com.linkedin.feathr.offline.config.{FeathrConfig, FeathrConfigLoader}
import com.linkedin.feathr.offline.generation.outputProcessor.WriteToHDFSOutputProcessor
import com.linkedin.feathr.offline.util.FeathrTestUtils.createSparkSession
import com.linkedin.feathr.offline.util.SparkFeaturizedDataset
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import org.apache.log4j.{Level, Logger}

/**
 * This object is used to help user to test/debug their workflow locally with sample data. This class serves as a
 * wrapper around [[FeathrClient]] class and invokes the FeathrClient#generateFeatures method by creating a spark session.
 */
object LocalFeatureGenJob {

  private val feathrConfigLoader = FeathrConfigLoader()

  // for user convenience, create spark session within this function, so user does not need to create one
  // this also ensure it has same setting as the real feathr join job
  val ss = createSparkSession(enableHiveSupport = true)

  /**
   * generate features according to feature generation config and feature definition config
   * this API is intended to use in unit test and debug
   * @param featureGenConfigStr
   * @param featureDefAsString
   * @return tagged feature name to SparkFeaturizedDataset
   */
  def localFeatureGenerate(featureGenConfigStr: String, featureDefAsString: String, dataPathHandlers: List[DataPathHandler]): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    val workDir = "featureGen/localFeatureGenerate/"
    val jobContext = new FeatureGenJobContext(workDir)
    localFeatureGenerate(featureGenConfigStr, featureDefAsString, jobContext, dataPathHandlers)
  }

  /**
   * generate features according to just a feature list and feature definition config
   * this API is intended to use in unit test and debug, will use default settings
   * @param features
   * @param featureDefAsString
   * @return
   */
  def localFeatureGenerate(features: Seq[String], featureDefAsString: String, dataPathHandlers: List[DataPathHandler]): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    val configs = Seq(feathrConfigLoader.load(featureDefAsString))
    val outputDir = "featureGen/generateWithDefaultParams/"
    val jobContext = new FeatureGenJobContext(outputDir)

    val featureGenConfigStr =
      s"""
         |operational: {
         |  name: generateWithDefaultParams
         |  endTime: 2022-12-10
         |  endTimeFormat: "yyyy-MM-dd"
         |  resolution: DAILY
         |  output:[]
         |}
         |features: [${features.mkString(",")}]
      """.stripMargin
    generateFeatures(featureGenConfigStr, configs, jobContext, dataPathHandlers)
  }

  /**
   * generate features according to feature generation config and feature definition config and extra params
   * this API is intended to use in unit test and debug
   * @param featureGenConfigStr
   * @param featureDefAsString
   * @extraParams other job parameters
   * @return tagged feature name to SparkFeaturizedDataset
   */
  private[offline] def localFeatureGenerate(
      featureGenConfigStr: String,
      featureDefAsString: String,
      extraParams: Array[String],
      dataPathHandlers: List[DataPathHandler]): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    val defaultParams = Array("--work-dir", "featureGen/localFeatureGenerate/")
    val jobContext = FeatureGenJobContext.parse(defaultParams ++ extraParams)
    localFeatureGenerate(featureGenConfigStr, featureDefAsString, jobContext, dataPathHandlers)
  }

  /**
   * generate features according to feature generation config and feature definition config
   * this API is intended to use in unit test and debug
   * this is intended for feathr internal use
   * @param featureGenConfigStr
   * @param featureDefAsString
   * @return tagged feature name to SparkFeaturizedDataset
   */
  private[offline] def localFeatureGenerate(
      featureGenConfigStr: String,
      featureDefAsString: String,
      jobContext: FeatureGenJobContext,
      dataPathHandlers: List[DataPathHandler]): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    Logger.getRootLogger.setLevel(Level.ERROR)
    val (overriddenFeatureDef, _) = FeatureGenConfigOverrider.overrideFeatureDefs(Some(featureDefAsString), None, jobContext)
    val configs = Seq(feathrConfigLoader.load(overriddenFeatureDef.get))
    generateFeatures(featureGenConfigStr, configs, jobContext, dataPathHandlers)
  }

  /**
   * generate features by providing feature generation config and FeathrConfig objects. Instatiate a [[FeathrClient]] object.
   * @param featureGenConfig feature generation config as a string, e.g,
   * @param feathrConfigs     list of FeathrConfigs
   * @param jobContext       job context
   * @return generated feature data
   */
  private def generateFeatures(
      featureGenConfig: String,
      feathrConfigs: Seq[FeathrConfig],
      jobContext: FeatureGenJobContext,
      dataPathHandlers: List[DataPathHandler]): Map[TaggedFeatureName, SparkFeaturizedDataset] = {
    val feathrClient = FeathrClient.builder(ss)
    .addFeatureDefConfs(Some(feathrConfigs.toList))
    .addDataPathHandlers(dataPathHandlers)
    .build()
    val dataLoaderHandlers: List[DataLoaderHandler] = dataPathHandlers.map(_.dataLoaderHandler)
    val featureGenSpec = FeatureGenSpec.parse(featureGenConfig, jobContext, dataLoaderHandlers)
    val fdsOutputProcessors = featureGenSpec.getProcessorList.collect {
      case processor: WriteToHDFSOutputProcessor => processor
    }
    if (fdsOutputProcessors.nonEmpty && fdsOutputProcessors.size != featureGenSpec.getProcessorList.size) {
      throw new FeathrException(
        ErrorLabel.FEATHR_USER_ERROR,
        s"Either none or all of the output processors in the generation config " +
          s"should have output format of FDS. ")
    }
    feathrClient.generateFeatures(featureGenSpec)
  }
}
