package com.linkedin.feathr.offline.generation

import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrException}
import com.linkedin.feathr.common.{Header, JoiningFeatureParams, TaggedFeatureName}
import com.linkedin.feathr.offline
import com.linkedin.feathr.offline.anchored.feature.FeatureAnchorWithSource.{getDefaultValues, getFeatureTypes}
import com.linkedin.feathr.offline.config.sources.FeatureGroupsUpdater
import com.linkedin.feathr.offline.derived.functions.SeqJoinDerivationFunction
import com.linkedin.feathr.offline.derived.strategies._
import com.linkedin.feathr.offline.derived.{DerivedFeature, DerivedFeatureEvaluator}
import com.linkedin.feathr.offline.evaluator.DerivedFeatureGenStage
import com.linkedin.feathr.offline.job.{FeatureGenSpec, FeatureTransformation}
import com.linkedin.feathr.offline.logical.{FeatureGroups, MultiStageJoinPlan, MultiStageJoinPlanner}
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import com.linkedin.feathr.offline.transformation.AnchorToDataSourceMapper
import com.linkedin.feathr.offline.util.{AnchorUtils, FeathrUtils}
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Feature generator that is responsible for generating anchored and derived features.
 * @param logicalPlan        logical plan for feature generation job.
 */
private[offline] class DataFrameFeatureGenerator(logicalPlan: MultiStageJoinPlan,
                                                 dataPathHandlers: List[DataPathHandler],
                                                 mvelContext: Option[FeathrExpressionExecutionContext]) extends Serializable {
  @transient val incrementalAggSnapshotLoader = IncrementalAggSnapshotLoader
  @transient val anchorToDataFrameMapper = new AnchorToDataSourceMapper(dataPathHandlers)
  @transient val featureGenFeatureGrouper = FeatureGenFeatureGrouper()
  @transient val featureGenDefaultsSubstituter = FeatureGenDefaultsSubstituter()
  @transient val postGenPruner = PostGenPruner()

  /**
   * Generate anchored and derived features and return the feature DataFrame and feature metadata.
   *
   * @param ss                 input spark session.
   * @param featureGenSpec     specification for a feature generation job.
   * @param featureGroups      all features in scope grouped under different types.
   * @param keyTaggedFeatures  key tagged feature
   * @return generated feature data with header
   */
  def generateFeaturesAsDF(
      ss: SparkSession,
      featureGenSpec: FeatureGenSpec,
      featureGroups: FeatureGroups,
      keyTaggedFeatures: Seq[JoiningFeatureParams]): Map[TaggedFeatureName, (DataFrame, Header)] = {

    val failOnMissingPartition = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.FAIL_ON_MISSING_PARTITION).toBoolean

    // 1. call analyze features, e.g. group features
    val requiredAnchorFeatures = keyTaggedFeatures.filter(f => featureGroups.allAnchoredFeatures.contains(f.featureName))
    val featureDateMap = AnchorUtils.getFeatureDateMap(requiredAnchorFeatures)
    val joinStages = logicalPlan.joinStages ++ logicalPlan.windowAggFeatureStages
    val allRequiredFeatures = logicalPlan.requiredNonWindowAggFeatures ++ logicalPlan.requiredWindowAggFeatures

    // 2. Get AnchorDFMap for Anchored features.
    val dataLoaderHandlers: List[DataLoaderHandler] = dataPathHandlers.map(_.dataLoaderHandler)
    val incrementalAggContext = incrementalAggSnapshotLoader.load(featureGenSpec=featureGenSpec, dataLoaderHandlers=dataLoaderHandlers)
    val allRequiredFeatureAnchorWithSourceAndTime = allRequiredFeatures
      .map(_.getFeatureName)
      .filter(featureGroups.allAnchoredFeatures.contains)
      .map(f => f -> AnchorUtils.getAnchorsWithDate(f, featureDateMap, featureGroups.allAnchoredFeatures).get)
      .toMap
    val requiredRegularFeatureAnchorsWithTime = allRequiredFeatureAnchorWithSourceAndTime.values.toSeq
    val anchorDFRDDMap = anchorToDataFrameMapper.getAnchorDFMapForGen(ss, requiredRegularFeatureAnchorsWithTime, Some(incrementalAggContext), failOnMissingPartition)

    val updatedAnchorDFRDDMap = anchorDFRDDMap.filter(anchorEntry => anchorEntry._2.isDefined).map(anchorEntry => anchorEntry._1 -> anchorEntry._2.get)

    // It could happen that all features are skipped, then return empty result
    if(updatedAnchorDFRDDMap.isEmpty) return Map()

    // 3. Load user specified default values and feature types, if any.
    val featureToDefaultValueMap = getDefaultValues(allRequiredFeatureAnchorWithSourceAndTime.values.toSeq)
    val featureToTypeConfigMap = getFeatureTypes(allRequiredFeatureAnchorWithSourceAndTime.values.toSeq)

    val shouldSkipFeature = FeathrUtils.getFeathrJobParam(ss.sparkContext.getConf, FeathrUtils.SKIP_MISSING_FEATURE).toBoolean

    // 4. Calculate anchored features
    val allStageFeatures = joinStages.flatMap {
      case (_: Seq[Int], featureNames: Seq[String]) =>
        val (anchoredFeatureNamesThisStage, _) = featureNames.partition(featureGroups.allAnchoredFeatures.contains)
        val anchoredFeaturesThisStage = featureNames.filter(featureGroups.allAnchoredFeatures.contains).map(allRequiredFeatureAnchorWithSourceAndTime).distinct
        val anchoredDFThisStage = updatedAnchorDFRDDMap.filterKeys(anchoredFeaturesThisStage.toSet)

        FeatureTransformation
          .transformFeatures(anchoredDFThisStage, anchoredFeatureNamesThisStage, None, Some(incrementalAggContext), mvelContext)
          .map(f => (f._1, (offline.FeatureDataFrame(f._2.transformedResult.df, f._2.transformedResult.inferredFeatureTypes), f._2.joinKey)))
    }.toMap
    allStageFeatures map { featureDfWithKey =>
      FeathrUtils.dumpDebugInfo(ss, featureDfWithKey._2._1.df, Set(featureDfWithKey._1),
      "transformed df in feature generation", "transformed_df_in_generation")
    }

    // Update features based on skip missing feature flag and empty dataframe
    val updatedAllStageFeatures = if (shouldSkipFeature) {
      allStageFeatures.filter(keyValue => !keyValue._2._1.df.isEmpty)
    } else allStageFeatures

    val (updatedFeatureGroups, updatedKeyTaggedFeatures) = FeatureGroupsUpdater().getUpdatedFeatureGroups(featureGroups,
      updatedAllStageFeatures, keyTaggedFeatures)

    val updatedLogicalPlan = MultiStageJoinPlanner().getLogicalPlan(updatedFeatureGroups, updatedKeyTaggedFeatures)

    // 5. Group features based on grouping specified in output processors
    val groupedAnchoredFeatures = featureGenFeatureGrouper.group(updatedAllStageFeatures, featureGenSpec.getOutputProcessorConfigs,
      updatedFeatureGroups.allDerivedFeatures)

    // 6. Substitute defaults at this stage since all anchored features are generated and grouped together.
    // Substitute before generating derived features.
    val defaultSubstitutedFeatures =
      featureGenDefaultsSubstituter.substitute(ss, groupedAnchoredFeatures, featureToDefaultValueMap, featureToTypeConfigMap)

    defaultSubstitutedFeatures map { featureDfWithKey =>
      FeathrUtils.dumpDebugInfo(ss, featureDfWithKey._2._1.df, Set(featureDfWithKey._1),
        "df after default applied in feature generation", "with_default_df_in_generation")
    }
    // 7. Calculate derived features.
    val derivedFeatureEvaluator = getDerivedFeatureEvaluatorInstance(ss, featureGroups)
    val derivedFeatureGenerator = DerivedFeatureGenStage(updatedFeatureGroups, updatedLogicalPlan, derivedFeatureEvaluator)

    val derivationsEvaluatedFeatures = (updatedLogicalPlan.joinStages ++ updatedLogicalPlan.convertErasedEntityTaggedToJoinStage(logicalPlan.postJoinDerivedFeatures))
      .foldLeft(defaultSubstitutedFeatures)((accFeatureData, currentStage) => {
        val (keyTags, featureNames) = currentStage
        val derivedFeatureNamesThisStage = featureNames.filter(featureGroups.allDerivedFeatures.contains)
        derivedFeatureGenerator.evaluate(derivedFeatureNamesThisStage, keyTags, accFeatureData)
      })

    derivationsEvaluatedFeatures map { featureDfWithKey =>
      FeathrUtils.dumpDebugInfo(ss, featureDfWithKey._2._1.df, Set(featureDfWithKey._1),
        "df after derivation in feature generation", "with_derivation_df_in_generation")
    }
    // 8. Prune feature columns before handing it off to output processors.
    // As part of the pruning columns are renamed to a standard and unwanted columns, features are dropped.
    val decoratedFeatures: Map[TaggedFeatureName, (DataFrame, Header)] =
      postGenPruner.prune(derivationsEvaluatedFeatures, featureGenSpec.getFeatures(), logicalPlan, featureGroups)

    decoratedFeatures map { featureDfWithKey =>
      FeathrUtils.dumpDebugInfo(ss, featureDfWithKey._2._1, Set(featureDfWithKey._1.getFeatureName),
        "df after prune in feature generation", "after_prune_df_in_generation")
    }
    // 9. apply outputProcessors
    val outputProcessors = featureGenSpec.getProcessorList()
    if (outputProcessors.isEmpty) {
      decoratedFeatures
    } else {
      val processedResults = outputProcessors.map(_.processAll(ss, decoratedFeatures))
      processedResults.reduceLeft(_ ++ _)
    }
  }

  /**
   * Helper method to configure and retrieve derived feature evaluator.
   */
  private def getDerivedFeatureEvaluatorInstance(ss: SparkSession, featureGroups: FeatureGroups) =
    DerivedFeatureEvaluator(
      DerivationStrategies(
        new SparkUdfDerivation(),
        new RowBasedDerivation(featureGroups.allTypeConfigs, mvelContext),
        new SequentialJoinDerivationStrategy {
          override def apply(
              keyTags: Seq[Int],
              keyTagList: Seq[String],
              df: DataFrame,
              derivedFeature: DerivedFeature,
              derivationFunction: SeqJoinDerivationFunction, mvelContext: Option[FeathrExpressionExecutionContext]): DataFrame = {
            // Feature generation does not support sequential join features
            throw new FeathrException(
              ErrorLabel.FEATHR_ERROR,
              s"Feature Generation does not support Sequential Join features : ${derivedFeature.producedFeatureNames.head}")
          }
        },
        new SqlDerivationSpark()
      ), mvelContext)
}
