package com.linkedin.feathr.offline.join.workflow

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrFeatureJoinException}
import com.linkedin.feathr.common.{ErasedEntityTaggedFeature, FeatureTypeConfig}
import com.linkedin.feathr.offline
import com.linkedin.feathr.offline.FeatureDataFrame
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource.{getDefaultValues, getFeatureTypes}
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.job.FeatureTransformation.{FEATURE_NAME_PREFIX, pruneAndRenameColumnWithTags, transformFeatures}
import com.linkedin.feathr.offline.job.KeyedTransformedResult
import com.linkedin.feathr.offline.join._
import com.linkedin.feathr.offline.join.algorithms._
import com.linkedin.feathr.offline.join.util.FrequentItemEstimatorFactory
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.source.accessor.DataSourceAccessor
import com.linkedin.feathr.offline.transformation.DataFrameDefaultValueSubstituter.substituteDefaults
import com.linkedin.feathr.offline.transformation.DataFrameExt._
import com.linkedin.feathr.offline.util.FeathrUtils
import com.linkedin.feathr.offline.util.FeathrUtils.shouldCheckPoint
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.lit

/**
 * An abstract class provides default implementation of anchored feature join step
 * in a feature join workflow.
 * The class is composed with left join column extraction logic, right join extraction logic and
 * a join algorithm to support different flavors of anchored feature join.
 * @param leftJoinColumnExtractor    left join column extractor class
 * @param rightJoinColumnExtractor   right join column extractor class
 * @param joiner                     join algorithm that should be used to join the 2 sides.
 */
private[offline] class AnchoredFeatureJoinStep(
    leftJoinColumnExtractor: JoinKeyColumnsAppender,
    rightJoinColumnExtractor: JoinKeyColumnsAppender,
    joiner: SparkJoinWithJoinCondition,
    mvelContext: Option[FeathrExpressionExecutionContext])
    extends FeatureJoinStep[AnchorJoinStepInput, DataFrameJoinStepOutput] {
  @transient lazy val log = LogManager.getLogger(getClass.getName)

  /**
   * Join anchored features to the observation passed as part of the input context.
   * @param features  Non-window aggregation, basic anchored features.
   * @param input     input context for this step.
   * @param ctx       environment variable that contains join job execution context.
   * @return feature joined DataFrame with inferred types for this feature.
   */
  override def joinFeatures(features: Seq[ErasedEntityTaggedFeature], input: AnchorJoinStepInput)(
      implicit ctx: JoinExecutionContext): FeatureDataFrameOutput = {
    val AnchorJoinStepInput(observationDF, anchorDFMap) = input
    val allAnchoredFeatures: Map[String, FeatureAnchorWithSource] = ctx.featureGroups.allAnchoredFeatures
    val joinStages = ctx.logicalPlan.joinStages
    val joinOutput = joinStages
      .foldLeft(FeatureDataFrame(observationDF, Map.empty[String, FeatureTypeConfig]))((accFeatureDataFrame, joinStage) => {
        val (keyTags: Seq[Int], featureNames: Seq[String]) = joinStage
        val FeatureDataFrame(contextDF, inferredFeatureTypeMap) = accFeatureDataFrame
        // map feature name to its transformed dataframe and the join key of the dataframe
        val groupedFeatureToDFAndJoinKeys: Map[Seq[String], Seq[KeyedTransformedResult]] =
          extractFeatureDataAndJoinKeys(keyTags, featureNames, allAnchoredFeatures, anchorDFMap)
        val tagsInfo = keyTags.map(ctx.logicalPlan.keyTagIntsToStrings).toList
        // use expr to support transformed keys for observation
        // Note that dataframe.join() can handle string type join with numeric type correctly, so we don't need to cast all
        // key types to string explicitly as what we did for the RDD version
        val (leftJoinColumns, contextDFWithJoinKey) = if (isSaltedJoinRequiredForKeys(keyTags)) {
            SaltedJoinKeyColumnAppender.appendJoinKeyColunmns(tagsInfo, contextDF)
          } else {
            leftJoinColumnExtractor.appendJoinKeyColunmns(tagsInfo, contextDF)
          }

        // Compute default values and feature types, for the features joined in the stage
        val anchoredDFThisStage = anchorDFMap.filterKeys(featureNames.filter(allAnchoredFeatures.contains).map(allAnchoredFeatures).toSet)
        val defaultValuesThisStage = getDefaultValues(anchoredDFThisStage.keys.toSeq)
        val featureTypesThisStage = getFeatureTypes(anchoredDFThisStage.keys.toSeq)
        // join features within the same stage, these features might be in different dataframes, so we join the dataframes in sequential order
        val (withBasicAnchorFeaturesContextDF, inferredTypes) =
          groupedFeatureToDFAndJoinKeys
            .foldLeft((contextDFWithJoinKey, Map.empty[String, FeatureTypeConfig]))((baseDFAndInferredTypes, featureToDFAndJoinKey) => {
              val (baseDF, prevInferredTypes) = baseDFAndInferredTypes
              val df = joinFeaturesOnSingleDF(keyTags, leftJoinColumns, baseDF, featureToDFAndJoinKey)

              // substitute default values for the feature joined
              val withDefaultDF =
                substituteDefaults(
                  df,
                  featureToDFAndJoinKey._1,
                  defaultValuesThisStage,
                  featureTypesThisStage,
                  ctx.sparkSession,
                  (s: String) => s"${FEATURE_NAME_PREFIX}$s")
              // prune columns from feature data and rename key columns
              val renamedDF = pruneAndRenameColumns(tagsInfo, withDefaultDF, featureToDFAndJoinKey, baseDF.columns)
              (renamedDF, prevInferredTypes ++ featureToDFAndJoinKey._2.head.transformedResult.inferredFeatureTypes)
            })
        // remove left join key columns
        val withNoLeftJoinKeyDF = withBasicAnchorFeaturesContextDF.drop(leftJoinColumns: _*)
        if (log.isDebugEnabled) {
          log.debug("contextDF after dropping left join key columns:")
          withNoLeftJoinKeyDF.show(false)
        }
        val checkpointDF = if (shouldCheckPoint(ctx.sparkSession)) {
          // checkpoint complicated dataframe for each stage to avoid Spark failure
          withNoLeftJoinKeyDF.checkpoint(true)
        } else {
          withNoLeftJoinKeyDF
        }
        offline.FeatureDataFrame(checkpointDF, inferredFeatureTypeMap ++ inferredTypes)
      })
    FeatureDataFrameOutput(joinOutput)
  }

  /**
   * Extract feature from source data after applying transformation.
   * Get feature data and join keys.
   * @param keyTags              Key tags for the features to be joined.
   * @param featureNames         Features to be joined in this stage.
   * @param allAnchoredFeatures  All anchored features parsed from feature definition configs.
   * @param anchorDFMap          Anchor definition to source data accessor map.
   * @param ctx                   Join execution context.
   * @return a Map of collection of features to its DataFrame (where the features are loaded from source and transformed).
   */
  def extractFeatureDataAndJoinKeys(
      keyTags: Seq[Int],
      featureNames: Seq[String],
      allAnchoredFeatures: Map[String, FeatureAnchorWithSource],
      anchorDFMap: Map[FeatureAnchorWithSource, DataSourceAccessor])(implicit ctx: JoinExecutionContext): Map[Seq[String], Seq[KeyedTransformedResult]] = {
    val bloomFilter = ctx.bloomFilters.map(filters => filters(keyTags))
    val (anchoredFeatureNamesThisStage, _) = featureNames.partition(allAnchoredFeatures.contains)
    val anchoredFeaturesThisStage = featureNames.filter(allAnchoredFeatures.contains).map(allAnchoredFeatures).distinct
    val anchoredDFThisStage = anchorDFMap.filterKeys(anchoredFeaturesThisStage.toSet)
    // map feature name to its transformed dataframe and the join key of the dataframe
    val featureToDFAndJoinKeys = transformFeatures(anchoredDFThisStage, anchoredFeatureNamesThisStage, bloomFilter, None, mvelContext)
    featureToDFAndJoinKeys
      .groupBy(_._2.transformedResult.df) // group by dataframe, join one at a time
      .map(grouped => (grouped._2.keys.toSeq, grouped._2.values.toSeq)) // extract the feature names and their (dataframe,join keys) pairs
  }

  /**
   * This method joins all the features that are on the same DataFrame, to the observation.
   * The features all share the same join key columns.
   * @param keyTags                 Key tags for the features to be joined. All features share the same keyTag.
   * @param leftJoinColumns         Observation key columns.
   * @param contextDF               observation DataFrame.
   * @param featureToDFAndJoinKey   feature data to its join key map. The features are on single DataFrame.
   * @param ctx                      Join execution context.
   * @return feature joined DataFrame.
   */
  def joinFeaturesOnSingleDF(
      keyTags: Seq[Int],
      leftJoinColumns: Seq[String],
      contextDF: DataFrame,
      featureToDFAndJoinKey: (Seq[String], Seq[KeyedTransformedResult]))(implicit ctx: JoinExecutionContext): DataFrame = {
    // since we group by dataframe already, all the join keys in this featureToDFAndJoinKey are the same, just take the first one
    if (featureToDFAndJoinKey._2.map(_.joinKey).toList.distinct.size != 1) {
      throw new FeathrFeatureJoinException(
        ErrorLabel.FEATHR_ERROR,
        "In AnchoredFeatureJoinStep.joinFeaturesOnSingleDF, " +
          s"all features should have same join key size, but found ${featureToDFAndJoinKey._2.map(_.joinKey).toList}")
    }
    val rawRightJoinColumnSize = featureToDFAndJoinKey._2.head.joinKey.size
    val rawRightJoinKeys = featureToDFAndJoinKey._2.head.joinKey
    val transformedResult = featureToDFAndJoinKey._2.head.transformedResult
    val featureDF = transformedResult.df
    if (rawRightJoinColumnSize == 0) {
      // when rightDF is empty, MVEL default source key extractor might return 0 key columns
      // in such cases, we just append the right table schema to the left table,
      // so that default value can be applied later
      featureDF.columns
        .zip(featureDF.schema.fields)
        .foldRight(contextDF)((nameAndfield, inputDF) => {
          inputDF.withColumn(nameAndfield._1, lit(null).cast(nameAndfield._2.dataType))
        })
    } else {
      val isSanityCheckMode = FeathrUtils.getFeathrJobParam(ctx.sparkSession.sparkContext.getConf, FeathrUtils.ENABLE_SANITY_CHECK_MODE).toBoolean
      if (isSanityCheckMode) {
        log.info("Running in sanity check mode.")
      }
      val featureNames = featureToDFAndJoinKey._1.toSet
      FeathrUtils.dumpDebugInfo(ctx.sparkSession, contextDF, featureNames, "observation for anchored feature",
        featureNames.mkString("_") + "_observation_for_anchored")
      FeathrUtils.dumpDebugInfo(ctx.sparkSession, featureDF, featureNames, "anchored feature before join",
        featureNames.mkString("_") + "_anchored_feature_before_join")
      val joinedDf = if (isSaltedJoinRequiredForKeys(keyTags)) {
        val (rightJoinColumns, rightDF) = SaltedJoinKeyColumnAppender.appendJoinKeyColunmns(rawRightJoinKeys, featureDF)
        log.trace(s"Salted join: rightJoinColumns= [${rightJoinColumns.mkString(", ")}] features= [${featureToDFAndJoinKey._1.mkString(", ")}]")
        val saltedJoinFrequentItemDF = ctx.frequentItemEstimatedDFMap.get(keyTags)
        val saltedJoiner = new SaltedSparkJoin(ctx.sparkSession, FrequentItemEstimatorFactory.createFromCache(saltedJoinFrequentItemDF))
        val refinedContextDF = if (isSanityCheckMode) {
          contextDF.appendRows(leftJoinColumns, rightJoinColumns, rightDF)
        } else {
          contextDF
        }
        saltedJoiner.join(leftJoinColumns, refinedContextDF, rightJoinColumns, rightDF, JoinType.left_outer)
      } else {
        val (rightJoinColumns, rightDF) = rightJoinColumnExtractor.appendJoinKeyColunmns(rawRightJoinKeys, featureDF)
        log.trace(s"Spark default join: rightJoinColumns= [${rightJoinColumns.mkString(", ")}] features= [${featureToDFAndJoinKey._1.mkString(", ")}]")
        val refinedContextDF = if (isSanityCheckMode) {
          contextDF.appendRows(leftJoinColumns, rightJoinColumns, rightDF)
        } else {
          contextDF
        }
        joiner.join(leftJoinColumns, refinedContextDF, rightJoinColumns, rightDF, JoinType.left_outer)
      }
      FeathrUtils.dumpDebugInfo(ctx.sparkSession, joinedDf, featureNames, "anchored feature after join",
        featureNames.mkString("_") + "_anchored_feature_after_join")
      joinedDf
    }
  }

  /**
   * Post-join pruning and renaming columns. Rename the feature columns by adding tags
   * and drop unnecessary columns from the feature data.
   *
   * @param tagsInfo               key tags as strings.
   * @param contextDF              observation and features joined.
   * @param featureToDFAndJoinKey  feature DataFrame and feature join key.
   * @param columnsToKeep          columns that should not be pruned.
   * @param ctx                     execution context.
   * @return pruned DataFrame.
   */
  def pruneAndRenameColumns(
      tagsInfo: Seq[String],
      contextDF: DataFrame,
      featureToDFAndJoinKey: (Seq[String], Seq[KeyedTransformedResult]),
      columnsToKeep: Seq[String])(implicit ctx: JoinExecutionContext): DataFrame = {
    val rightJoinKeysColumnsToDrop = featureToDFAndJoinKey._2.flatMap(_.joinKey)
    val featuresToRename = featureToDFAndJoinKey._1 map (refStr => DataFrameColName.getEncodedFeatureRefStrForColName(refStr))
    log.trace(s"featuresToRename = $featuresToRename")

    val featureDF = featureToDFAndJoinKey._2.head.transformedResult.df
    val renamedDF = pruneAndRenameColumnWithTags(contextDF, columnsToKeep, featuresToRename, featureDF.columns, tagsInfo.toList)
    if (log.isDebugEnabled) {
      log.debug("joinNonSWAFeatures(): After pruning:")
      renamedDF.show(false)
    }
    // right join key column are duplicated
    renamedDF.drop(rightJoinKeysColumnsToDrop: _*)
  }

  /**
   * Helper method that checks if input key tags have frequent items and requires salting.
   */
  private def isSaltedJoinRequiredForKeys(keyTags: Seq[Int])(implicit ctx: JoinExecutionContext) =
    ctx.frequentItemEstimatedDFMap.isDefined && ctx.frequentItemEstimatedDFMap.get.get(keyTags).isDefined
}

/**
 * Instantiation is delegated to the companion object.
 */
private[offline] object AnchoredFeatureJoinStep {
  def apply(
      leftJoinColumnExtractor: JoinKeyColumnsAppender,
      rightJoinColumnExtractor: JoinKeyColumnsAppender,
      joiner: SparkJoinWithJoinCondition,
      mvelContext: Option[FeathrExpressionExecutionContext]): AnchoredFeatureJoinStep =
    new AnchoredFeatureJoinStep(leftJoinColumnExtractor, rightJoinColumnExtractor, joiner, mvelContext)
}
