package com.linkedin.feathr.offline.swa

import com.linkedin.feathr.common.FeatureTypeConfig
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrConfigException}
import com.linkedin.feathr.offline.anchored.WindowTimeUnit
import com.linkedin.feathr.offline.anchored.anchorExtractor.TimeWindowConfigurableAnchorExtractor
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.anchored.keyExtractor.{MVELSourceKeyExtractor, SQLSourceKeyExtractor}
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.config.FeatureJoinConfig
import com.linkedin.feathr.offline.exception.FeathrIllegalStateException
import com.linkedin.feathr.offline.job.PreprocessedDataFrameManager
import com.linkedin.feathr.offline.join.DataFrameKeyCombiner
import com.linkedin.feathr.offline.transformation.AnchorToDataSourceMapper
import com.linkedin.feathr.offline.transformation.DataFrameDefaultValueSubstituter.substituteDefaults
import com.linkedin.feathr.offline.util.FeathrUtils.shouldCheckPoint
import com.linkedin.feathr.offline.util.datetime.DateTimeInterval
import com.linkedin.feathr.offline.{FeatureDataFrame, JoinStage}
import com.linkedin.feathr.swj.{LabelData, SlidingWindowJoin}
import com.linkedin.feathr.{common, offline}
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.util.sketch.BloomFilter

import scala.collection.mutable

/**
 * Sliding window aggregation joiner
 * @param allWindowAggFeatures all window aggregation features
 */
private[offline] class SlidingWindowAggregationJoiner(
    allWindowAggFeatures: Map[String, FeatureAnchorWithSource],
    anchorToDataSourceMapper: AnchorToDataSourceMapper) {
  private val log = LogManager.getLogger(getClass)

  /**
   * Join observation data with time-based window aggregation features.
   * This method utilizes SlidingWindowJoin library for the computation.
   *
   * @param ss                        spark session
   * @param keyTagList                Mapping between keyTag (index of the list) and the feature name.
   * @param windowAggFeatureStages    Window aggregation features, grouped by the keyTag.
   *                                  WindowAgg features with the same keyTag will be joined together.
   * @param requiredWindowAggFeatures List of all window aggregation features read from Feathr config.
   * @param joinConfig                Feathr feature join config
   * @param bloomFilters              bloomfilters for observation data
   * @param obsDF                     Observation data
   * @param swaObsTimeOpt start and end time of observation data
   * @param failOnMissingPartition whether to fail the data loading if some of the date partitions are missing.
   * @return pair of :
   *         1) dataframe with feature column appended to the obsData,
   *         it can be converted to a pair RDD of (observation data record, feature record),
   *         each feature resides in a column and it is named using DataFrameColName.genFeatureColumnName(..)
   *         2) inferred feature types
   */
  def joinWindowAggFeaturesAsDF(
      ss: SparkSession,
      obsDF: DataFrame,
      joinConfig: FeatureJoinConfig,
      keyTagList: Seq[String],
      windowAggFeatureStages: Seq[JoinStage],
      requiredWindowAggFeatures: Seq[common.ErasedEntityTaggedFeature],
      bloomFilters: Option[Map[Seq[Int], BloomFilter]],
      swaObsTimeOpt: Option[DateTimeInterval],
      failOnMissingPartition: Boolean): FeatureDataFrame = {
    val joinConfigSettings = joinConfig.settings
    // extract time window settings
    if (joinConfigSettings.isEmpty) {
      throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "settings section are not defined in join config, cannot extract observation data time range")
    }

    if (joinConfigSettings.get.joinTimeSetting.isEmpty) {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        "joinTimeSettings section is not defined in join config," +
          " cannot perform window aggregation operation")
    }

    val timeWindowJoinSettings = joinConfigSettings.get.joinTimeSetting.get
    val simulatedDelay = timeWindowJoinSettings.simulateTimeDelay

    if (simulatedDelay.isEmpty && !joinConfig.featuresToTimeDelayMap.isEmpty) {
      throw new FeathrConfigException(
        ErrorLabel.FEATHR_USER_ERROR,
        "overrideTimeDelay cannot be defined without setting a simulateTimeDelay in the " +
          "joinTimeSettings")
    }

    val featuresToDelayImmutableMap: Map[String, java.time.Duration] =
       joinConfig.featuresToTimeDelayMap.mapValues(WindowTimeUnit.parseWindowTime) ++
        simulatedDelay.map(SlidingWindowFeatureUtils.DEFAULT_TIME_DELAY -> _).toMap

    // find the window aggregate feature anchor configs
    val windowAggFeatureNames = requiredWindowAggFeatures.map(_.getFeatureName).toIndexedSeq
    val windowAggAnchors = windowAggFeatureNames.map(allWindowAggFeatures)

    // get time range of observation data
    // join windowAggAnchor DataFrames with observation data
    var contextDF: DataFrame = obsDF
    // SWA Observation time range should be computed in PreProcessObservation step
    val swaObsTimeRange = swaObsTimeOpt.get

    // get a Map from each source to a list of all anchors based on this source
    val windowAggSourceToAnchor = windowAggAnchors
    // same source with different key extractor might generate different views, e.g, key extractor may 'explode' the dataframe
    // which would result in more rows, so we need to group by source and keyExtractor
      .map(anchor => {
        val featureNames = PreprocessedDataFrameManager.getPreprocessingUniquenessForAnchor(anchor)
        ((anchor.source, anchor.featureAnchor.sourceKeyExtractor.toString(), featureNames), anchor)
      })
      .groupBy(_._1)
      .map({
        case (source, grouped) => (source, grouped.map(_._2))
      })

    // If the skip_missing_features flag is set, we will skip joining the features whose is not mature, and maintain this list.
    val notJoinedFeatures = new mutable.HashSet[String]()

    // For each source, we calculate the maximum window duration that needs to be loaded across all
    // required SWA features defined on this source.
    // Then we load the source only once.
    // Then we create a map from each *anchor* to the loaded source DataFrame.
    val windowAggAnchorDFMap = windowAggSourceToAnchor.flatMap({
      case (sourceWithKeyExtractor, anchors) =>
        val maxDurationPerSource = anchors
          .map(SlidingWindowFeatureUtils.getMaxWindowDurationInAnchor(_, windowAggFeatureNames))
          .max
        log.info(s"Selected max window duration $maxDurationPerSource across all anchors for source ${sourceWithKeyExtractor._1.path}")

        // use preprocessed DataFrame if it exist. Otherwise use the original source DataFrame.
        // there are might be duplicates: Vector(f_location_avg_fare, f_location_max_fare, f_location_avg_fare, f_location_max_fare)
        val res = anchors.flatMap(x => x.featureAnchor.features)
        val featureNames = res.toSet.toSeq.sorted.mkString(",")
        val preprocessedDf = PreprocessedDataFrameManager.preprocessedDfMap.get(featureNames)
        val originalSourceDf =
          anchorToDataSourceMapper
            .getWindowAggAnchorDFMapForJoin(
              ss,
              sourceWithKeyExtractor._1,
              swaObsTimeRange,
              maxDurationPerSource,
              featuresToDelayImmutableMap.values.toArray,
              failOnMissingPartition)

        // If skip missing features flag is set and there is a data related error, an empty dataframe will be returned.
        if (originalSourceDf.isEmpty) {
          res.map(notJoinedFeatures.add)
          anchors.map(anchor => (anchor, originalSourceDf))
        } else {
        val sourceDF: DataFrame = preprocessedDf match {
          case Some(existDf) => existDf
          case None => originalSourceDf
        }

        // all the anchors here have same key sourcekey extractor, so we just use the first one to generate key column and share
        val withKeyDF = anchors.head.featureAnchor.sourceKeyExtractor match {
          // the lateral view parameter in sliding window aggregation feature is handled differently in feature join and feature generation
          // i.e., in feature generation, they are handled in key source extractor,
          // in feature join here, we don't handle lateralView parameter in key extractor, and leave it to Spark SWJ library
          case keyExtractor: SQLSourceKeyExtractor => keyExtractor.appendKeyColumns(sourceDF, false)
          case keyExtractor => keyExtractor.appendKeyColumns(sourceDF)
        }

        anchors.map(anchor => (anchor, withKeyDF))
    }}
    )

    // Filter out features dataframe if they are empty.
    val updatedWindowAggAnchorDFMap = windowAggAnchorDFMap.filter(x => {
      val df = x._2
      !df.head(1).isEmpty
    })

    val allInferredFeatureTypes = mutable.Map.empty[String, FeatureTypeConfig]

    windowAggFeatureStages.foreach({
      case (keyTags: Seq[Int], featureNames: Seq[String]) =>
        // Remove the features that are going to be skipped.
        val joinedFeatures = featureNames.diff(notJoinedFeatures.toSeq)
        if (joinedFeatures.nonEmpty) {
          log.warn(s"----SKIPPED ADDING FEATURES : ${notJoinedFeatures} ------")
        val stringKeyTags = keyTags.map(keyTagList).map(k => s"CAST (${k} AS string)") // restore keyTag to column names in join config

        // get the bloom filter for the key combinations in this stage
        val bloomFilter = bloomFilters match {
          case Some(filters) => Option(filters(keyTags))
          case None => None
        }

        // If there is no joinTimeSettings, then we will assume it is useLatestFeatureData
        val timeStampExpr = if (!timeWindowJoinSettings.useLatestFeatureData) {
          SlidingWindowFeatureUtils.constructTimeStampExpr(timeWindowJoinSettings.timestampColumn.name, timeWindowJoinSettings.timestampColumn.format)
        } else {
          "unix_timestamp()" // if useLatestFeatureData=true, return the current unix timestamp (w.r.t UTC time zone)
        }

        val labelDataDef = LabelData(contextDF, stringKeyTags, timeStampExpr)

        if (ss.sparkContext.isLocal && log.isDebugEnabled) {
          log.debug(
            s"*********Sliding window aggregation feature join stage with key: ${stringKeyTags} for feature " +
              s"${joinedFeatures.mkString(",")}*********")
          log.debug(
            s"First 3 rows in observation dataset :\n " +
              s"${labelDataDef.dataSource.collect().take(3).map(_.toString()).mkString("\n ")}")
        }
        val windowAggAnchorsThisStage = joinedFeatures.map(allWindowAggFeatures)
        val windowAggAnchorDFThisStage = updatedWindowAggAnchorDFMap.filterKeys(windowAggAnchorsThisStage.toSet)

        val factDataDefs =
          SlidingWindowFeatureUtils.getSWAAnchorGroups(windowAggAnchorDFThisStage).map {
            anchorWithSourceToDFMap =>
              val selectedFeatures = anchorWithSourceToDFMap.keySet.flatMap(_.selectedFeatures).filter(joinedFeatures.contains(_))
              val factData = anchorWithSourceToDFMap.head._2
              val anchor = anchorWithSourceToDFMap.head._1
              val filteredFactData = bloomFilter match {
                case None => factData // no bloom filter: use data as it
                case Some(filter) =>
                  // get the list of join keys.
                  if (anchor.featureAnchor.sourceKeyExtractor.isInstanceOf[MVELSourceKeyExtractor]) {
                    throw new FeathrConfigException(ErrorLabel.FEATHR_USER_ERROR, "MVELSourceKeyExtractor is not supported in sliding window aggregation")
                  }
                  val keyColumnsList = anchor.featureAnchor.sourceKeyExtractor.getKeyColumnNames(None)
                  // generate the same concatenated-key column for bloom filtering
                  val (bfFactKeyColName, factDataWithKeys) =
                    DataFrameKeyCombiner().combine(factData, keyColumnsList)
                  // use bloom filter on generated concat-key column
                  val filtered = factDataWithKeys.filter(SlidingWindowFeatureUtils.mightContain(filter)(col(bfFactKeyColName)))
                  // remove the concat-key column
                  filtered.drop(col(bfFactKeyColName))
              }
              SlidingWindowFeatureUtils.getFactDataDef(filteredFactData, anchorWithSourceToDFMap.keySet.toSeq, featuresToDelayImmutableMap, selectedFeatures)
          }
        val origContextObsColumns = labelDataDef.dataSource.columns

        contextDF = SlidingWindowJoin.join(labelDataDef, factDataDefs.toList)
        val defaults = windowAggAnchorDFThisStage.flatMap(s => s._1.featureAnchor.defaults)
        val userSpecifiedTypesConfig = windowAggAnchorDFThisStage.flatMap(_._1.featureAnchor.featureTypeConfigs)

        // Create a map from the feature name to the column format, ie - RAW or FDS_TENSOR
        val featureNameToColumnFormat = allWindowAggFeatures.map (nameToFeatureAnchor => nameToFeatureAnchor._1 ->
          nameToFeatureAnchor._2.featureAnchor.extractor
          .asInstanceOf[TimeWindowConfigurableAnchorExtractor].features(nameToFeatureAnchor._1).columnFormat)

        val FeatureDataFrame(withFDSFeatureDF, inferredTypes) =
          SlidingWindowFeatureUtils.convertSWADFToFDS(contextDF, joinedFeatures.toSet, featureNameToColumnFormat, userSpecifiedTypesConfig)
        // apply default on FDS dataset
        val withFeatureContextDF =
          substituteDefaults(withFDSFeatureDF, defaults.keys.filter(joinedFeatures.contains).toSeq, defaults, userSpecifiedTypesConfig, ss)

        allInferredFeatureTypes ++= inferredTypes
        contextDF = standardizeFeatureColumnNames(origContextObsColumns, withFeatureContextDF, joinedFeatures, keyTags.map(keyTagList))
        if (shouldCheckPoint(ss)) {
          // checkpoint complicated dataframe for each stage to avoid Spark failure
          contextDF = contextDF.checkpoint(true)
        }
        if (ss.sparkContext.isLocal && log.isDebugEnabled) {
          factDataDefs.zipWithIndex.foreach {
            case (factDataDef, idx) =>
              log.debug(
                s"First 3 rows in feature dataset ${idx}:\n " +
                  s"${factDataDef.dataSource.collect().take(3).map(_.toString()).mkString("\n ")}")
          }
        }
    }})
    offline.FeatureDataFrame(contextDF, allInferredFeatureTypes.toMap)
  }

  /**
   * Rename SWA features to internal standardized feature name
   * @param origContextObsColumns
   * @param withSWAFeatureDF
   * @param featureNames
   * @param keyTags
   * @return
   */
  def standardizeFeatureColumnNames(
      origContextObsColumns: Seq[String],
      withSWAFeatureDF: DataFrame,
      featureNames: Seq[String],
      keyTags: Seq[String]): DataFrame = {
    val inputColumnSize = origContextObsColumns.size
    val outputColumnNum = withSWAFeatureDF.columns.size
    if (outputColumnNum != inputColumnSize + featureNames.size) {
      throw new FeathrIllegalStateException(
        s"Number of columns (${outputColumnNum}) in the dataframe returned by " +
          s"sliding window aggregation does not equal to number of columns in the observation data (${inputColumnSize}) " +
          s"+ number of features (${featureNames.size}). Columns in returned dataframe are ${withSWAFeatureDF.columns}," +
          s" columns in observation dataframe are ${origContextObsColumns}")
    }

    /*
     * SWA feature column name returned here is just the feature names, need to replace SWA feature column name
     * following new feature column naming convention (same as non-SWA feature names)
     */
    val renamingPairs = featureNames map { feature =>
      val columnName = DataFrameColName.genFeatureColumnName(feature, Some(keyTags))
      (feature, columnName)
    }

    renamingPairs.foldLeft(withSWAFeatureDF) { (baseDF, renamePair) =>
      baseDF.withColumnRenamed(renamePair._1, renamePair._2)
    }
  }
}
