package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.FeatureName
import com.linkedin.feathr.offline.config.location.SimplePath
import com.linkedin.feathr.offline.job.FeatureGenSpec
import com.linkedin.feathr.offline.source.dataloader.BatchDataLoader
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.offline.util.IncrementalAggUtils
import com.linkedin.feathr.offline.util.datetime.OfflineDateTimeUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.JavaConverters._

/**
 * This case class represents the context for Incremental Aggregation feature generation.
 * @param isIncrementalAggEnabled       Is incremental aggregation enabled.
 * @param daysSinceLastAgg              Number of days since last aggregation snapshot was taken.
 * @param previousSnapshotMap           Latest aggregation snapshot.
 * @param previousSnapshotRootDirMap    The root directory for previously aggregated results.
 */
private[offline] case class IncrementalAggContext(
    // the indicator for enabling incremental mode or not
    isIncrementalAggEnabled: Boolean,
    // days gap between date with latest aggregation snapshot and the end time
    daysSinceLastAgg: Option[Long],
    // the latest aggregation snapshot
    previousSnapshotMap: Map[FeatureName, DataFrame],
    // the root directory map, map from feature name to root directory of the previously aggregated result,
    // which has daily subfolder structure, to store the previous aggregation snapshots
    previousSnapshotRootDirMap: Map[FeatureName, String])

/**
 * This feature is used for Incremental Aggregation during AFG.
 * Loads the latest aggregation snapshot and sets the aggregation root directory.
 */
private[offline] trait IncrementalAggSnapshotLoader {

  /**
   * Loads previously aggregated results and builds the context required for incremental aggregation.
   * @param featureGenSpec Feature Generation spec.
   * @return Incremental aggregation context.
   */
  def load(featureGenSpec: FeatureGenSpec, dataLoaderHandlers: List[DataLoaderHandler]): IncrementalAggContext = {
    load(featureGenSpec=featureGenSpec,
         fs=FileSystem.get(new Configuration()),
         dataLoaderHandlers=dataLoaderHandlers)
  }

  /**
   * Loads previously aggregated results and builds the context required for incremental aggregation.
   * This API is similar to the previous one but allows the caller to specify an instance of Filesystem
   * that can be used to access the previously aggregated results.
   * @param featureGenSpec Feature Generation spec.
   * @param fs             Filesystem in which to look for the previously aggregated results.
   * @return Incremental aggregation context.
   */
  private[generation] def load(featureGenSpec: FeatureGenSpec, fs: FileSystem, dataLoaderHandlers: List[DataLoaderHandler]): IncrementalAggContext
}

private[offline] object IncrementalAggSnapshotLoader extends IncrementalAggSnapshotLoader {
  private val logger = LogManager.getLogger(getClass)
  private[generation] override def load(featureGenSpec: FeatureGenSpec, fs: FileSystem, dataLoaderHandlers: List[DataLoaderHandler]): IncrementalAggContext = {
    val isIncrementalAggEnabled = featureGenSpec.isEnableIncrementalAgg()
    if (!isIncrementalAggEnabled) {
      IncrementalAggContext(isIncrementalAggEnabled, None, Map.empty[FeatureName, DataFrame], Map.empty[FeatureName, String])
    } else {
      val outputProcessorConfigs = featureGenSpec.getOutputProcessorConfigs
      val hdfsProcessorConfig = outputProcessorConfigs.filter(outputConfig => outputConfig.getName == "HDFS")
      var resolvedDayGapBetweenPreAggAndEndTime: Option[Long] = None
      val incrementalAggDirToFeatureAndDF: Seq[(String, (Seq[String], Option[DataFrame]))] = hdfsProcessorConfig collect {
        // to support incremental aggregation, the HDFS output processor must use FDS format and
        // provide storeName and features list.
        case config
            if ( config.getParams.hasPath(FeatureGenerationPathName.STORE_NAME)
              && config.getParams.hasPath(FeatureGenerationPathName.FEATURES)) =>
          val params = config.getParams
          val path = params.getString("path")
          val basePath = path + "/" + params.getString(FeatureGenerationPathName.STORE_NAME)
          val preAggRootDir = FeatureGenerationPathName.getDataPath(basePath, None)
          resolvedDayGapBetweenPreAggAndEndTime =
            IncrementalAggUtils.getDaysGapBetweenLatestAggSnapshotAndEndTime(preAggRootDir, featureGenSpec.endTimeStr, featureGenSpec.endTimeFormat)
          if (fs.exists(new Path(basePath)) && resolvedDayGapBetweenPreAggAndEndTime.isDefined) {
            val endDate = OfflineDateTimeUtils.createTimeFromString(featureGenSpec.endTimeStr, featureGenSpec.endTimeFormat).toLocalDateTime
            val directory = IncrementalAggUtils.getLatestAggSnapshotDFPath(preAggRootDir, endDate).get
            val spark = SparkSession.builder().getOrCreate()
            val preAggSnapshot = new BatchDataLoader(ss=spark,
                                                     location=SimplePath(directory),
                                                     dataLoaderHandlers=dataLoaderHandlers
                                                    ).loadDataFrame()
            val features = params.getStringList(FeatureGenerationPathName.FEATURES).asScala
            // user may have added new features in this run
            val oldFeatures = features.filter(preAggSnapshot.columns.contains)
            preAggRootDir -> (oldFeatures, Some(preAggSnapshot))
          } else {
            "" -> (Seq(), None)
          }
      }
      // If there are no incremental aggregation stores specified.
      if (incrementalAggDirToFeatureAndDF.isEmpty) {
        throw new FeathrConfigException(
          ErrorLabel.FEATHR_USER_ERROR,
          s"In order to support incremental aggregation, please specify " +
            s"${FeatureGenerationPathName.STORE_NAME} and ${FeatureGenerationPathName.FEATURES} in the HDFS output" +
            s" processors with FDS type")
      }

      val preAggRootDirMap = getPreAggRootDirMap(incrementalAggDirToFeatureAndDF)
      val preAggSnapshotMap = getPreAggSnapshotMap(incrementalAggDirToFeatureAndDF)
      IncrementalAggContext(isIncrementalAggEnabled, resolvedDayGapBetweenPreAggAndEndTime, preAggSnapshotMap, preAggRootDirMap)
    }
  }

  /**
   * Helper method to build a map from feature to its previously computed aggregation snapshot directory.
   */
  private[generation] def getPreAggRootDirMap(aggDirToFeatureAndDF: Seq[(String, (Seq[String], Option[DataFrame]))]): Map[FeatureName, String] = {
    aggDirToFeatureAndDF.flatMap {
      case (preAggDir, (features, _)) => features.map(featureName => featureName -> preAggDir)
    }.toMap
  }

  /**
   * Helper method to build a map from feature to its previously computed aggregation DataFrame.
   */
  private[generation] def getPreAggSnapshotMap(aggDirToFeatureAndDF: Seq[(String, (Seq[String], Option[DataFrame]))]): Map[FeatureName, DataFrame] = {
    val featureNameToPreAggDF = aggDirToFeatureAndDF.collect {
      case (_, (features, Some(preAggDF))) => features.map(featureName => featureName -> preAggDF)
    }
    if (featureNameToPreAggDF.nonEmpty) {
      featureNameToPreAggDF.reduce(_ union _).toMap
    } else {
      // do nothing, this means no preAgg dataset exist so far, i.e. first time running incremental agg, cold start
      logger.info("No preAgg dataset exist so far, i.e. first time running incremental agg (cold start)")
      Map.empty[FeatureName, DataFrame]
    }
  }
}
