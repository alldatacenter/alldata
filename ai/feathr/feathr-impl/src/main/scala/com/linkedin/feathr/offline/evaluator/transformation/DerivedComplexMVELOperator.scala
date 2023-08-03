package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.{NodeReference, Transformation}
import com.linkedin.feathr.offline.config.{PegasusRecordFeatureTypeConverter, TaggedDependency}
import com.linkedin.feathr.offline.derived.functions.MvelFeatureDerivationFunction
import com.linkedin.feathr.offline.evaluator.transformation.BaseDerivedFeatureOperator.applyDerivationFunction
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.updateDataframeMapAndApplyDefaults
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame

/**
 * Transformation operator for complex MVEL operator.
 */
object DerivedComplexMVELOperator extends TransformationOperator {
  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val featureName = if (node.getFeatureName == null) graphTraverser.nodeIdToFeatureName(node.getId) else node.getFeatureName
    val inputFeatureNames = node.getInputs.toArray.map(input => {
      val inp = input.asInstanceOf[NodeReference]
      graphTraverser.nodeIdToFeatureName(inp.getId)
    }).sorted // Sort by input feature name to create the derivation function. Sort is crucial here to properly link input features.

    // We convert from array to map with dummy values in order to reuse MvelFeatureDerivationFunction from feathr.
    val featureTypeConfig = PegasusRecordFeatureTypeConverter().convert(node.getFeatureVersion)
    val featuresMap = inputFeatureNames.map(name => (name, TaggedDependency(Seq(""), ""))).toMap
    val derivationFunction = new MvelFeatureDerivationFunction(featuresMap, node.getFunction.getParameters.get("expression"), featureName,
      featureTypeConfig)
    val newContextDf = applyDerivationFunction(node, derivationFunction, graphTraverser, contextDf)
    updateDataframeMapAndApplyDefaults(Seq(node), graphTraverser, newContextDf, Seq.empty) // Note here derived features don't have output key columns
  }

  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    nodes.foldLeft(contextDf)((newContextDf, node) => compute(node, graphTraverser, newContextDf, dataPathHandlers))
  }
}
