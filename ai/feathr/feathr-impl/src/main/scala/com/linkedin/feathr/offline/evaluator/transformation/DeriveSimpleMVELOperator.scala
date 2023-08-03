package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.common.FeatureDerivationFunction
import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.config.PegasusRecordFeatureTypeConverter
import com.linkedin.feathr.offline.derived.functions.SimpleMvelDerivationFunction
import com.linkedin.feathr.offline.evaluator.transformation.BaseDerivedFeatureOperator.applyDerivationFunction
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.updateDataframeMapAndApplyDefaults
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame

/**
 * Transformation operator for simple MVEL operator.
 */
object DerivedSimpleMVELOperator extends TransformationOperator {

  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val transformationFunction = node.getFunction
    val featureName = if (node.getFeatureName == null) graphTraverser.nodeIdToFeatureName(node.getId) else node.getFeatureName
    val featureTypeConfig = PegasusRecordFeatureTypeConverter().convert(node.getFeatureVersion)
    val derivationFunction = new SimpleMvelDerivationFunction(transformationFunction.getParameters.get("expression"),
      featureName, featureTypeConfig)
      .asInstanceOf[FeatureDerivationFunction]
    val newContextDf = applyDerivationFunction(node, derivationFunction, graphTraverser, contextDf)
    updateDataframeMapAndApplyDefaults(Seq(node), graphTraverser, newContextDf, Seq.empty) // Note here derived features don't have output key columns
  }

  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    nodes.foldLeft(contextDf)((newContextDf, node) => compute(node, graphTraverser, newContextDf, dataPathHandlers))
  }
}
