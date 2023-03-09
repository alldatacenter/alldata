package com.linkedin.feathr.offline.join.workflow

import com.linkedin.feathr.common.FeatureTypeConfig
import com.linkedin.feathr.offline.FeatureDataFrame
import com.linkedin.feathr.offline.client.DataFrameColName
import com.linkedin.feathr.offline.derived.DerivedFeatureEvaluator
import com.linkedin.feathr.offline.join.JoinExecutionContext
import com.linkedin.feathr.offline.util.FeathrUtils
import com.linkedin.feathr.offline.util.FeathrUtils.isDebugMode
import com.linkedin.feathr.{common, offline}
import org.apache.logging.log4j.LogManager

/**
 * A step in Feature Join Workflow that joins the derived features.
 */
class DerivedFeatureJoinStep(derivedFeatureEvaluator: DerivedFeatureEvaluator) extends FeatureJoinStep[DataFrameJoinStepInput, FeatureDataFrameOutput] {
  @transient lazy val log = LogManager.getLogger(getClass.getName)

  override def joinFeatures(features: Seq[common.ErasedEntityTaggedFeature], input: DataFrameJoinStepInput)(
      implicit ctx: JoinExecutionContext): FeatureDataFrameOutput = {
    val allDerivedFeatures = ctx.featureGroups.allDerivedFeatures
    // In-stage derived features and post join derived features are evaluated together
    val joinStages = ctx.logicalPlan.joinStages ++ ctx.logicalPlan.convertErasedEntityTaggedToJoinStage(ctx.logicalPlan.postJoinDerivedFeatures)
    val inputDF = input.observation

    val resultFeatureDataFrame =
      joinStages.foldLeft(FeatureDataFrame(inputDF, Map.empty[String, FeatureTypeConfig]))((accFeatureDataFrame, joinStage) => {
        val (keyTags: Seq[Int], featureNames: Seq[String]) = joinStage
        val FeatureDataFrame(contextDF, inferredFeatureTypeMap) = accFeatureDataFrame
        val (derivedFeaturesThisStage, _) = featureNames.partition(allDerivedFeatures.contains)

        // calculate derived feature scheduled in this stage
        val derivations = derivedFeaturesThisStage.map(f => (allDerivedFeatures(f), f))
        derivations.foldLeft(offline.FeatureDataFrame(contextDF, inferredFeatureTypeMap))((derivedAccDataFrame, derivedFeature) => {
          val FeatureDataFrame(baseDF, inferredTypes) = derivedAccDataFrame
          val (derived, featureName) = derivedFeature
          val tagsInfo = keyTags.map(ctx.logicalPlan.keyTagIntsToStrings)
          val featureColumnName = DataFrameColName.genFeatureColumnName(featureName, Some(tagsInfo))
          // some features might have been generated together with other derived features already, i.e.,
          // if the derived feature column exist in the contextDF, skip the calculation, this is due to
          // multiple derived features can be provided by same derivation function, i.e., advanced derivation function.
          if (!baseDF.columns.contains(featureColumnName)) {
            val FeatureDataFrame(withDerivedContextDF, newInferredTypes) =
              derivedFeatureEvaluator.evaluate(keyTags, ctx.logicalPlan.keyTagIntsToStrings, baseDF, derived)

            if (isDebugMode(ctx.sparkSession)) {
              log.debug(s"Final output after joining non-SWA features:")
              contextDF.show(false)
            }
            offline.FeatureDataFrame(withDerivedContextDF, inferredTypes ++ newInferredTypes)
          } else {
            offline.FeatureDataFrame(baseDF, inferredTypes)
          }
        })
      })
    val result = FeatureDataFrameOutput(resultFeatureDataFrame)
    val featureNames = features.map(_.getFeatureName).toSet
    FeathrUtils.dumpDebugInfo(ctx.sparkSession, result.obsAndFeatures.df, featureNames, "derived feature",
       featureNames.mkString("_") + "_derived_feature")
    result
  }
}

object DerivedFeatureJoinStep {
  def apply(derivedFeatureEvaluator: DerivedFeatureEvaluator): DerivedFeatureJoinStep =
    new DerivedFeatureJoinStep(derivedFeatureEvaluator: DerivedFeatureEvaluator)
}
