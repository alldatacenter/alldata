package com.linkedin.feathr.offline.join

import com.linkedin.feathr.common._
import com.linkedin.feathr.offline
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.client.DataFrameColName.getFeatureAlias
import com.linkedin.feathr.offline.config.FeatureJoinConfig
import com.linkedin.feathr.offline.config.sources.FeatureGroupsUpdater
import com.linkedin.feathr.offline.derived.DerivedFeatureEvaluator
import com.linkedin.feathr.offline.job.FeatureTransformation.transformSingleAnchorDF
import com.linkedin.feathr.offline.job.{FeatureTransformation, TransformedResult}
import com.linkedin.feathr.offline.join.algorithms._
import com.linkedin.feathr.offline.join.util.{FrequentItemEstimatorFactory, FrequentItemEstimatorType}
import com.linkedin.feathr.offline.join.workflow._
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlan, MultiStageJoinPlanner}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.swa.SlidingWindowAggregationJoiner
import com.linkedin.feathr.offline.transformation.AnchorToDataSourceMapper
import com.linkedin.feathr.offline.transformation.DataFrameDefaultValueSubstituter.substituteDefaults
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat.FeatureColumnFormat
import com.linkedin.feathr.offline.util.FeathrUtils
import com.linkedin.feathr.offline.util.datetime.DateTimeInterval
import com.linkedin.feathr.offline.{ErasedEntityTaggedFeature, FeatureDataFrame}
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.util.sketch.BloomFilter

import scala.collection.JavaConverters._

/**
 * Joiner to join observation with feature data using Spark DataFrame API
 * @param logicalPlan analyzed feature info
 */
private[offline] class DataFrameFeatureJoiner(logicalPlan: MultiStageJoinPlan, dataPathHandlers: List[DataPathHandler], mvelContext: Option[FeathrExpressionExecutionContext]) extends Serializable {
  @transient lazy val log = LogManager.getLogger(getClass.getName)
  @transient lazy val anchorToDataSourceMapper = new AnchorToDataSourceMapper(dataPathHandlers)
  private val windowAggFeatureStages = logicalPlan.windowAggFeatureStages
  private val joinStages = logicalPlan.joinStages
  private val postJoinDerivedFeatures = logicalPlan.postJoinDerivedFeatures
  private val requiredWindowAggFeatures = logicalPlan.requiredWindowAggFeatures
  private val requiredNonWindowAggFeatures = logicalPlan.requiredNonWindowAggFeatures
  private val requestedSeqJoinFeatures = logicalPlan.seqJoinFeatures
  private val keyTagIntsToStrings = logicalPlan.keyTagIntsToStrings
  private val allRequiredFeatures = logicalPlan.allRequiredFeatures
  private val allRequestedFeatures = logicalPlan.allRequestedFeatures

  /**
   * join anchored passthrough features to the observation dataset
   * @param ss spark session
   * @param contextDF observation datset
   * @param featureGroups all related features
   * @return 1. observation dataset with all anchored passthrough features joined,
   *         each feature corresponds to a column named as __feathr_feature_[featureName]
   *         2. feature name to inferred feature type
   */
  def joinAnchoredPassthroughFeatures(ss: SparkSession, contextDF: DataFrame, featureGroups: FeatureGroups): FeatureDataFrame = {
    val allAnchoredPassthroughFeatures =
      featureGroups.allPassthroughFeatures.filter(feature => allRequiredFeatures.map(_.getFeatureName).contains(feature._1))
    val withFeatureDf = if (allAnchoredPassthroughFeatures.nonEmpty) {
      // collect anchored passthrough feature information
      val passthroughFeatureMapping = allAnchoredPassthroughFeatures
        .groupBy(_._2)
        .map {
          case (featureAnchorWithSource, featureGroup) =>
            (featureAnchorWithSource, featureGroup.keySet)
        }
      // transform anchored passthrough features
      val transformedInfoWithoutKey =
        passthroughFeatureMapping.foldLeft(TransformedResult(Seq[(String, String)](), contextDF, Map.empty[String, FeatureColumnFormat], Map()))(
          (dfWithFeatureNames, featureAnchorWithSourcePair) => {
            val featureAnchorWithSource = featureAnchorWithSourcePair._1
            val requestedFeatures = featureAnchorWithSourcePair._2.toSeq
            val resultWithoutKey = transformSingleAnchorDF(featureAnchorWithSource, dfWithFeatureNames.df, requestedFeatures, None, mvelContext)
            val namePrefixPairs = dfWithFeatureNames.featureNameAndPrefixPairs ++ resultWithoutKey.featureNameAndPrefixPairs
            val inferredFeatureTypeConfigs = dfWithFeatureNames.inferredFeatureTypes ++ resultWithoutKey.inferredFeatureTypes
            val featureColumnFormats = resultWithoutKey.featureColumnFormats ++ dfWithFeatureNames.featureColumnFormats
            TransformedResult(namePrefixPairs, resultWithoutKey.df, featureColumnFormats, inferredFeatureTypeConfigs)
          })
      val allRequestedFeatures = allAnchoredPassthroughFeatures.keySet.toSeq
      // convert to FDS in tensor mode
      val userSpecifiedFeatureTypeConfigs = allAnchoredPassthroughFeatures.flatMap(_._2.featureAnchor.featureTypeConfigs)
      val FeatureDataFrame(convertedDF, featureTypesThisStage) =
        FeatureTransformation.convertTransformedDFToFDS(
          allRequestedFeatures,
          transformedInfoWithoutKey,
          transformedInfoWithoutKey.df,
          userSpecifiedFeatureTypeConfigs)
      // apply default value
      val defaultValuesThisStage = allAnchoredPassthroughFeatures.map(_._2.featureAnchor.defaults).foldLeft(Map.empty[String, FeatureValue])(_ ++ _)
      val withDefaultDF =
        substituteDefaults(convertedDF, allRequestedFeatures, defaultValuesThisStage, featureTypesThisStage, ss)
      val renameMap = allAnchoredPassthroughFeatures.keySet.map(fName => (fName, DataFrameColName.genFeatureColumnName(fName)))
      // Rename to follow internal naming convention
      val renamedDF = renameMap.foldLeft(withDefaultDF)((df, namePair) => {
        df.withColumnRenamed(namePair._1, namePair._2)
      })
      offline.FeatureDataFrame(renamedDF, featureTypesThisStage)
    } else {
      offline.FeatureDataFrame(contextDF, Map())
    }
    val featureNames = allAnchoredPassthroughFeatures.map(_._1).toSet
    FeathrUtils.dumpDebugInfo(ss, withFeatureDf.df, featureNames, "context DF after joining passthrough feature",
      featureNames.mkString("_") + "_after_join_with_passthrough_features")
    withFeatureDf
  }

  /**
   * Join all requested feature to the observation dataset
   *
   * @param ss                 spark session
   * @param joinConfig         join config
   * @param featureGroups      required features
   * @param keyTaggedFeatures  requested features
   * @param observationDF      observation data
   * @param rowBloomFilterThreshold
   * @return pair of (joined dataframe, header)
   */
  def joinFeaturesAsDF(
      ss: SparkSession,
      joinConfig: FeatureJoinConfig,
      featureGroups: FeatureGroups,
      keyTaggedFeatures: Seq[JoiningFeatureParams],
      observationDF: DataFrame,
      rowBloomFilterThreshold: Option[Int] = None): (DataFrame, Header) = {

    /*
     * Get the feature data RDDs for non window-aggregation features,
     * apply filtering (bloom filters & column filters) on the way.
     * Feature data RDDs for window-aggregation features are handled by joinWindowAggFeatures().
     */
    // 0. Before the join starts, log the key information related to the join
    log.info("=========== Key infos for Feathr FeatureJoin ==========")
    log.info(s"user requested features: $keyTaggedFeatures")
    log.info(s"keyTag mapping: ${keyTagIntsToStrings.zipWithIndex}")
    log.info(s"resolved dependencies list: $allRequiredFeatures")
    log.info(s"join stages: $joinStages, post-join derived features: $postJoinDerivedFeatures")
    log.info(s"windowAggFeatures that needs to be computed: $requiredWindowAggFeatures")
    log.info(s"non-windowAggFeatures that needs to be computed: $requiredNonWindowAggFeatures")
    log.info(s"seqJoin features that needs to be computed: $requestedSeqJoinFeatures")

    val allJoinStages = (windowAggFeatureStages ++ joinStages).distinct

    val useSaltedJoin = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.ENABLE_SALTED_JOIN).toBoolean
    // Slick join and salted join have different JoinKeyAppenders which handle join key differently
    // Currently, we cannot use both at the same time.
    val useSlickJoin = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.ENABLE_SLICK_JOIN).toBoolean && !useSaltedJoin
    val failOnMissingPartition = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.FAIL_ON_MISSING_PARTITION).toBoolean
    val saltedJoinParameters =
      if (useSaltedJoin) {
        val estimatorType = FrequentItemEstimatorType.withName(FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.SALTED_JOIN_FREQ_ITEM_ESTIMATOR))
        val estimator = FrequentItemEstimatorFactory.create(estimatorType)
        val frequentItemThreshold = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.SALTED_JOIN_FREQ_ITEM_THRESHOLD).toFloat
        Some(SaltedSparkJoin.JoinParameters(estimator, frequentItemThreshold))
      } else {
        None
      }
    // 1. Calculate anchored pass through first, as slick join will not be able to preserve the columns
    //    they might need in the observation data.
    val FeatureDataFrame(withPassthroughFeatureDF, anchoredPassthroughFeatureTypes) =
      joinAnchoredPassthroughFeatures(ss, observationDF, featureGroups)

    val allRequestAnchoredPassthroughFeatures =
      featureGroups.allPassthroughFeatures.filter(feature => allRequestedFeatures.map(_.getFeatureName).contains(feature._1)).keySet
    val passthroughFeatureColumns = withPassthroughFeatureDF.columns.diff(observationDF.columns)

    // 2. Preprocess observation data, e.g. trims the observation to do slick join, i.e. trims the observation to roughly
    // only have its join keys, later use it to join the features, and finally join back other fields of the original observation data.
    // This reduces the shuffle size during the feature join, as the observation/context data will be much smaller during the feature join
    val PreprocessedObservation(bloomFilters, withJoinKeyAndUidObs, origObsWithUid, swaObsTime, extraColumnsInSlickJoin, saltedJoinFrequentItemDFs) =
      OptimizerUtils.preProcessObservation(
        withPassthroughFeatureDF,
        joinConfig,
        allJoinStages,
        keyTagIntsToStrings,
        rowBloomFilterThreshold,
        saltedJoinParameters,
        passthroughFeatureColumns)
    val extraSlickColumns2Remove = withJoinKeyAndUidObs.columns.diff(observationDF.columns).diff(passthroughFeatureColumns)
    val obsToJoinWithFeatures = if (useSlickJoin) withJoinKeyAndUidObs else withPassthroughFeatureDF

    /*
     * Get source accessor for all required, non-SWA anchored features.
     */
    val requiredRegularFeatureAnchors = requiredNonWindowAggFeatures.filter {
      case ErasedEntityTaggedFeature(_, featureName) =>
        featureGroups.allAnchoredFeatures.contains(featureName) && !featureGroups.allPassthroughFeatures.contains(featureName)
    }.distinct

    val anchorSourceAccessorMap = anchorToDataSourceMapper.getBasicAnchorDFMapForJoin(
      ss,
      requiredRegularFeatureAnchors
        .map(_.getFeatureName)
        .toIndexedSeq
        .map(featureGroups.allAnchoredFeatures),
      failOnMissingPartition)
    val shouldSkipFeature = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.SKIP_MISSING_FEATURE).toBoolean
    val updatedSourceAccessorMap = anchorSourceAccessorMap.filter(anchorEntry => anchorEntry._2.isDefined)
      .map(anchorEntry => anchorEntry._1 -> anchorEntry._2.get)

    val (updatedFeatureGroups, updatedLogicalPlan) = if (shouldSkipFeature) {
      val (newFeatureGroups, newKeyTaggedFeatures) = FeatureGroupsUpdater().removeMissingFeatures(featureGroups,
        updatedSourceAccessorMap.keySet.flatMap(featureAnchorWithSource => featureAnchorWithSource.featureAnchor.features).toSeq, keyTaggedFeatures)

      val newLogicalPlan = MultiStageJoinPlanner().getLogicalPlan(newFeatureGroups, newKeyTaggedFeatures)
      (newFeatureGroups, newLogicalPlan)
    } else (featureGroups, logicalPlan)

    implicit val joinExecutionContext: JoinExecutionContext =
      JoinExecutionContext(ss, updatedLogicalPlan, updatedFeatureGroups, bloomFilters, Some(saltedJoinFrequentItemDFs))
    // 3. Join sliding window aggregation features
    val FeatureDataFrame(withWindowAggFeatureDF, inferredSWAFeatureTypes) =
      joinSWAFeatures(ss, obsToJoinWithFeatures, joinConfig, featureGroups, failOnMissingPartition, bloomFilters, swaObsTime)

    // 4. Join basic anchored features
    val anchoredFeatureJoinStep =
      if (useSlickJoin) {
        AnchoredFeatureJoinStep(
          SlickJoinLeftJoinKeyColumnAppender,
          SlickJoinRightJoinKeyColumnAppender,
          SparkJoinWithJoinCondition(EqualityJoinConditionBuilder), mvelContext)
      } else {
        AnchoredFeatureJoinStep(
          SqlTransformedLeftJoinKeyColumnAppender,
          IdentityJoinKeyColumnAppender,
          SparkJoinWithJoinCondition(EqualityJoinConditionBuilder), mvelContext)
      }
    val FeatureDataFrameOutput(FeatureDataFrame(withAllBasicAnchoredFeatureDF, inferredBasicAnchoredFeatureTypes)) =
      anchoredFeatureJoinStep.joinFeatures(requiredRegularFeatureAnchors, AnchorJoinStepInput(withWindowAggFeatureDF, updatedSourceAccessorMap))
    // 5. If useSlickJoin, restore(join back) all observation fields before we evaluate post derived features, sequential join and passthrough
    // anchored features, as they might require other columns in the original observation data, while the current observation
    // dataset does not have these fields (were removed in the preProcessObservation)
    val withSlickJoinedDF = if (useSlickJoin) {
      // we kept the some non join key columns in the trimmed observation (e.g. time stamp column for SWA),
      // we needs to drop before we join to observation, or we will have duplicated column names for join keys
      val cleanedFeaturesDF = withAllBasicAnchoredFeatureDF.drop(extraColumnsInSlickJoin: _*)
      origObsWithUid
        .join(cleanedFeaturesDF.drop(passthroughFeatureColumns: _*), DataFrameColName.UidColumnName)
        .drop(extraSlickColumns2Remove: _*)
    } else withAllBasicAnchoredFeatureDF

    // 6. Join Derived Features
    val derivedFeatureEvaluator = DerivedFeatureEvaluator(ss=ss, featureGroups=featureGroups, dataPathHandlers=dataPathHandlers, mvelContext)
    val derivedFeatureJoinStep = DerivedFeatureJoinStep(derivedFeatureEvaluator)
    val FeatureDataFrameOutput(FeatureDataFrame(withDerivedFeatureDF, inferredDerivedFeatureTypes)) =
      derivedFeatureJoinStep.joinFeatures(allRequiredFeatures.filter {
        case ErasedEntityTaggedFeature(_, featureName) => featureGroups.allDerivedFeatures.contains(featureName)
      }, BaseJoinStepInput(withSlickJoinedDF))

    // Create a set with only the combination of keyTags and the feature name. This will uniquely determine the column name.
    val taggedFeatureSet = keyTaggedFeatures
      .map(
        featureParams =>
          // If there are multiple delays with a feature alias, then this is treated as a separate feature
          if (featureParams.featureAlias.isDefined && featureParams.timeDelay.isDefined) {
            new TaggedFeatureName(featureParams.keyTags.asJava, featureParams.featureAlias.get)
          } else {
            new TaggedFeatureName(featureParams.keyTags.asJava, featureParams.featureName)
        })
      .toSet

    // 7. Remove unwanted feature columns, e.g, required but not requested features
    val cleanedDF = withDerivedFeatureDF.drop(withDerivedFeatureDF.columns.filter(col => {
      val requested = DataFrameColName
        .getFeatureRefStrFromColumnNameOpt(col)
        .forall { featureRefStr =>
          val featureTags = DataFrameColName.getFeatureTagListFromColumn(col, false)
          // If it is a requested anchored passthrough feature, keep it in the output
          if (allRequestAnchoredPassthroughFeatures.contains(featureRefStr)) true
          else {
            taggedFeatureSet.contains(new TaggedFeatureName(featureTags.asJava, featureRefStr))
          }
        } // preserve all non-feature columns, they are from observation
      !requested
    }): _*)
    FeathrUtils.dumpDebugInfo(ss, cleanedDF, Set(), "context DF after join and " +
        "remove unwanted columns", "context_after_join_and_clean")

    val allInferredFeatureTypes = anchoredPassthroughFeatureTypes ++
      inferredBasicAnchoredFeatureTypes ++ inferredSWAFeatureTypes ++ inferredDerivedFeatureTypes

    // 8. Rename the columns to the expected feature name or if an alias is present, to the featureAlias name.
    val taggedFeatureToColumnNameMap = DataFrameColName.getTaggedFeatureToNewColumnName(cleanedDF)

    val taggedFeatureToUpdatedColumnMap: Map[TaggedFeatureName, (String, String)] = taggedFeatureToColumnNameMap.map {
      case (taggedFeatureName, (oldName, newName)) =>
        val featureAlias = getFeatureAlias(keyTaggedFeatures, taggedFeatureName.getFeatureName, taggedFeatureName.getKeyTag.asScala, None, None)
        if (featureAlias.isDefined) (taggedFeatureName, (oldName, featureAlias.get)) else (taggedFeatureName, (oldName, newName))
    }

    // 9. Get the final dataframe and the headers for every column.
    val (finalDF, header) =
      DataFrameColName.adjustFeatureColNamesAndGetHeader(
        cleanedDF,
        taggedFeatureToUpdatedColumnMap,
        featureGroups.allAnchoredFeatures,
        featureGroups.allDerivedFeatures,
        allInferredFeatureTypes)

    FeathrUtils.dumpDebugInfo(ss, finalDF, Set(), "final df", "final_df_returned")
    (finalDF, header)
  }

  /**
   * Join observation data with the request sliding window aggregation features
   *
   * @param ss spark session
   * @param obsToJoinWithFeatures observation data
   * @param joinConfig Feature Join config.
   * @param featureGroups feature groups
   * @param failOnMissingPartition flag to indicate if the join job should fail if a missing date partition is found.
   * @param bloomFilters bloomfilters, map string key tag ids to bloomfilter
   * @param swaObsTime observation start and end time for sliding window aggregation, if not None, will try to infer
   * @return observation data joined with sliding window aggregation features
   */
  def joinSWAFeatures(
      ss: SparkSession,
      obsToJoinWithFeatures: DataFrame,
      joinConfig: FeatureJoinConfig,
      featureGroups: FeatureGroups,
      failOnMissingPartition: Boolean,
      bloomFilters: Option[Map[Seq[Int], BloomFilter]],
      swaObsTime: Option[DateTimeInterval]): FeatureDataFrame = {
    if (windowAggFeatureStages.isEmpty) {
      offline.FeatureDataFrame(obsToJoinWithFeatures, Map())
    } else {
      val swaJoiner = new SlidingWindowAggregationJoiner(featureGroups.allWindowAggFeatures, anchorToDataSourceMapper)
      swaJoiner.joinWindowAggFeaturesAsDF(
        ss,
        obsToJoinWithFeatures,
        joinConfig,
        keyTagIntsToStrings,
        windowAggFeatureStages,
        requiredWindowAggFeatures,
        bloomFilters,
        swaObsTime,
        failOnMissingPartition)
    }
  }
}
