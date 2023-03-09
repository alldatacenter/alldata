package com.linkedin.feathr.offline.generation.outputProcessor

import com.linkedin.feathr.offline.util.Transformations.sortColumns
import com.linkedin.feathr.common.configObj.generation.OutputProcessorConfig
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrDataOutputException, FeathrException}
import com.linkedin.feathr.common.{Header, RichConfig, TaggedFeatureName}
import com.linkedin.feathr.offline.config.location.{DataLocation, SimplePath}
import com.linkedin.feathr.offline.generation.{FeatureDataHDFSProcessUtils, FeatureGenerationPathName}
import com.linkedin.feathr.offline.util.{FeatureGenConstants, IncrementalAggUtils}
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.sparkcommon.OutputProcessor
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 *
 * feature generation output processor used to write data to HDFS
 * @param config config object of output processor, built from the feature generation config,
 *               an example:
 * @param dataLoaderHandlers additional data loader handlers that contain hooks for dataframe creation and manipulation
 *
 operational: {
  name: testFeatureGen
  endTime: 2019-05-01
  endTimeFormat: "yyyy-MM-dd"
  resolution: DAILY
  output:[
    {
      name: HDFS
      params: {
        path: "/user/featureGen/hdfsResult/"
      }
    }
  ]
}
features: [mockdata_a_ct_gen, mockdata_a_sample_gen]
 */
private[offline] class WriteToHDFSOutputProcessor(val config: OutputProcessorConfig, endTimeOpt: Option[String] = None, dataLoaderHandlers: List[DataLoaderHandler]) extends OutputProcessor(config, endTimeOpt=None) {

  /**
   * write feature data to hdfs
   *
   * @param featureData each entry is a string tagged feature name to [dataframe, header] pair
   */
  override def processAll(ss: SparkSession, featureData: Map[TaggedFeatureName, (DataFrame, Header)]): Map[TaggedFeatureName, (DataFrame, Header)] = {
    val basePath = config.getParams().getStringOpt(PATH).getOrElse("")
    val result = processAllHelper(ss, featureData, basePath)
    result
  }

  /**
   * write feature data to hdfs helper
   * folder structure would be:
   *|
   *|-df_xxx
   *|   |_ meta
   *|   |    |_ .. (meta files, include feature name and its column name, feature type, etc. in the dataframe)
   *|   |
   *|   |_ data/..(dataframe partition files)
   *|
   *|-df_yyy
   *|   |_ meta
   *|   |    |_ .. (meta files, include feature name and its column name, feature type, etc. in the dataframe)
   *|   |
   *|   |_ data/..(dataframe partition files)
   *|...
   *
   * @param featureData each entry is a string tagged feature name to [dataframe, header] pair
   * @param outputBasePath path to store data
   */
  private def processAllHelper(
      ss: SparkSession,
      featureData: Map[TaggedFeatureName, (DataFrame, Header)],
      outputBasePath: String): Map[TaggedFeatureName, (DataFrame, Header)] = {
    val allFeatureList = featureData.keySet.map(_.getFeatureName).toSeq
    val featureListToJoin = config.getParams().getStringListOpt(FeatureGenerationPathName.FEATURES)
    val storeName = config.getParams().getStringOpt(FeatureGenerationPathName.STORE_NAME)

    if (featureListToJoin.isDefined) {
      val selectedFeatureNames = featureListToJoin.getOrElse(allFeatureList)
      // filter unwanted feature
      val selectedFeatureData = featureData.filter(f => selectedFeatureNames.contains(f._1.getFeatureName))
      // groupby dataframe and take the first from each group
      val groupedFeatureData = selectedFeatureData.groupBy(_._2._1)
      // The features should have already been grouped at this point.
      // Validate that the grouped features are on single DataFrame.
      if (groupedFeatureData.size != 1) {
        throw new FeathrException(ErrorLabel.FEATHR_ERROR, s"Output processing failed! Features are not grouped together. Please contact ask_feathr.")
      }
      val joinedFeatureData = groupedFeatureData.head._2
      val df = joinedFeatureData.head._2._1
      val header = joinedFeatureData.head._2._2

      val folderName = storeName.getOrElse("")
      val parentPath = outputBasePath + "/" + folderName
      val (processedDF, processedHeader) = processSingle(ss, df, header, parentPath)
      selectedFeatureData.mapValues(_ => (processedDF, processedHeader))
    } else {
      val result = featureData
        .groupBy(_._2._1) // each dataframe in one folder
        .zipWithIndex // give each dataframe an Id
        .map {
          case ((_, groupedFeatureToDF), idx) =>
            val folderName = storeName.getOrElse("df" + idx)
            val parentPath = outputBasePath + "/" + folderName
            val df = groupedFeatureToDF.head._2._1
            val header = groupedFeatureToDF.head._2._2
            val (processedDF, processedHeader) = processSingle(ss, df, header, parentPath)
            header.featureInfoMap.mapValues(_ => (processedDF, processedHeader))
        }
      result.reduceLeft(_ ++ _)
    }
  }

  /**
   * process single dataframe, e.g, convert feature data schema
   * @param ss spark session
   * @param df feature dataframe
   * @param header meta info of the input dataframe
   * @param parentPath path to save feature data
   * @return processed dataframe and header
   */
  override def processSingle(ss: SparkSession, df: DataFrame, header: Header, parentPath: String): (DataFrame, Header) = {
    // In some use case such feature p17n, the columns need to be deterministic, hence we sort the columns in the DataFrame.
    val orderedDf = df.transform(sortColumns("asc"))
    val taggedFeatureNames = header.featureInfoMap.keySet

    val outputWithTimestamp =
      config.getParams().getBooleanWithDefault(FeatureGenConstants.OUTPUT_WITH_TIMESTAMP, false)
    val outputTimestampFormat =
      config.getParams().getStringWithDefault(FeatureGenConstants.OUTPUT_TIMESTAMP_FORMAT, FeatureGenConstants.DEFAULT_OUTPUT_TIMESTAMP_FORMAT)
    // augment the df with timestamp information
    val timestampOpt = if (outputWithTimestamp) {
      // the output timestamp value is end time(stored in endTimeOpt), need to make sure end time exists
      if (endTimeOpt.isEmpty) {
        throw new FeathrDataOutputException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"endTime is not provided. endTime should be provided when ${FeatureGenConstants.OUTPUT_WITH_TIMESTAMP} is set to true.")
      }
      Some(IncrementalAggUtils.transformDateString(endTimeOpt.get, FeatureGenConstants.END_TIME_FORMAT, outputTimestampFormat))
    } else {
      None
    }
    val augmentedDF = if (outputWithTimestamp) {
      orderedDf.withColumn(FeatureGenConstants.FEATHR_AUTO_GEN_TIMESTAMP_FIELD, lit(timestampOpt.get))
    } else {
      orderedDf
    }
    val featuresToDF = taggedFeatureNames.map(featureToDF => (featureToDF, (augmentedDF, header))).toMap

    // If it's local, we can't write to HDFS.
    val skipWrite = if (ss.sparkContext.isLocal) true else false
    location match {
      case Some(l) => {
        // We have a DataLocation to write the df
        l.writeDf(ss, augmentedDF, Some(header))
        (augmentedDF, header)
      }
      case None => {
        FeatureDataHDFSProcessUtils.processFeatureDataHDFS(ss, featuresToDF, parentPath, config, skipWrite = skipWrite, endTimeOpt, timestampOpt, dataLoaderHandlers)
      }
    }
  }

  private val location: Option[DataLocation] = {
    if (!config.getParams.getStringWithDefault("type", "").isEmpty) {
      // The location param contains 'type' key, assuming it's a DataLocation
      Some(DataLocation(config.getParams))
    } else {
      None
    }
  }

  // path parameter name
  private val PATH = "path"
}
