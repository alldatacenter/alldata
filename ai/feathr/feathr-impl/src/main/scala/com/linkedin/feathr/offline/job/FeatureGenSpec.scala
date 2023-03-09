package com.linkedin.feathr.offline.job

import java.time.temporal.ChronoUnit
import com.linkedin.feathr.common.configObj.configbuilder.{FeatureGenConfigBuilder, OutputProcessorBuilder}
import com.linkedin.feathr.common.configObj.generation.{FeatureGenConfig, OfflineOperationalConfig, OutputProcessorConfig}
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrDataOutputException}
import com.linkedin.feathr.common.{DateParam, DateTimeParam, DateTimeUtils, RichConfig}
import com.linkedin.feathr.offline.generation.outputProcessor.{FeatureMonitoringProcessor, PushToRedisOutputProcessor, WriteToHDFSOutputProcessor}
import com.linkedin.feathr.offline.util.{FeatureGenConstants, IncrementalAggUtils}
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.sparkcommon.OutputProcessor
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

/**
 * wrapper of generation config and context, also does not further processing on them.

 */
class FeatureGenSpec(private val featureGenConfig: FeatureGenConfig, dataLoaderHandlers: List[DataLoaderHandler]) {

  require(
    featureGenConfig.getOperationalConfig.isInstanceOf[OfflineOperationalConfig],
    "The operational config should " +
      "be of type OfflineOperationalConfig.")
  val offlineOperationalConfig = featureGenConfig.getOperationalConfig.asInstanceOf[OfflineOperationalConfig]
  val outputProcessorConfigs = offlineOperationalConfig.getOutputProcessorsListConfig

  // dateTimeParam is [inclusive, inclusive]
  lazy val dateTimeParam: DateTimeParam = {
    val timeConfig = DateTimeParam(offlineOperationalConfig.getTimeSetting)
    DateTimeParam.shiftEndTime(timeConfig, offlineOperationalConfig.getSimulateTimeDelay)
  }
  // dateParam is [inclusive, inclusive]
  val dateParam: DateParam = DateTimeUtils.toDateParam(dateTimeParam)
  val endTimeFormat = dateTimeParam.resolution match {
    case ChronoUnit.DAYS => "yyyy/MM/dd"
    case ChronoUnit.HOURS => "yyyy/MM/dd/HH"
  }

  val inputTimeFormat = dateTimeParam.resolution match {
    case ChronoUnit.DAYS => "yyyyMMdd"
    case ChronoUnit.HOURS => "yyyyMMddHH"
  }

  val endTimeStr = IncrementalAggUtils.transformDateString(dateParam.endDate.get, inputTimeFormat, endTimeFormat)

  val outputProcessors = outputProcessorConfigs.asScala.map(config => {
    config.getName match {
      case FeatureGenConstants.HDFS_OUTPUT_PROCESSOR_NAME =>
        val useOutputTimePath = config.getParams.getBooleanWithDefault(FeatureGenConstants.OUTPUT_TIME_PATH, default = true)
        val endTimeOpt = if (useOutputTimePath) Some(endTimeStr) else None
        new WriteToHDFSOutputProcessor(config, endTimeOpt, dataLoaderHandlers)
      case FeatureGenConstants.REDIS_OUTPUT_PROCESSOR_NAME =>
        val params = config.getParams
        val decoratedConfig = OutputProcessorBuilder.build(config.getName, params)
        new PushToRedisOutputProcessor(decoratedConfig, None)
      case FeatureGenConstants.MONITORING_OUTPUT_PROCESSOR_NAME =>
        val params = config.getParams
        val decoratedConfig = OutputProcessorBuilder.build(config.getName, params)
        new FeatureMonitoringProcessor(decoratedConfig, None)
      case _ =>
        throw new FeathrDataOutputException(ErrorLabel.FEATHR_USER_ERROR, "Custom output processor is not yet supported.")
    }
  })

  // get the incremental indicator
  def isEnableIncrementalAgg(): Boolean = {
    offlineOperationalConfig.getEnableIncremental()
  }

  // get list of output processor configs
  def getOutputProcessorConfigs: Seq[OutputProcessorConfig] = {
    offlineOperationalConfig.getOutputProcessorsListConfig.asScala
  }

  // get list of output processors
  def getProcessorList(): Seq[OutputProcessor] = outputProcessors

  // get requested feature names from feature generation config
  def getFeatures(): Seq[String] = featureGenConfig.getFeatures.asScala
}

object FeatureGenSpec {

  /**
   * parsing feature generation config string to [[FeatureGenSpec]]
   * @param featureGenConfigStr feature generation config as a string
   * @param featureGenJobContext feature generation context
   * @return Feature generation Specifications
   */
  def parse(featureGenConfigStr: String, featureGenJobContext: FeatureGenJobContext, dataLoaderHandlers: List[DataLoaderHandler]): FeatureGenSpec = {
    val withParamsOverrideConfigStr = FeatureGenConfigOverrider.applyOverride(featureGenConfigStr, featureGenJobContext.paramsOverride)
    val withParamsOverrideConfig = ConfigFactory.parseString(withParamsOverrideConfigStr)
    val featureGenConfig = FeatureGenConfigBuilder.build(withParamsOverrideConfig)
    new FeatureGenSpec(featureGenConfig, dataLoaderHandlers)
  }
}
