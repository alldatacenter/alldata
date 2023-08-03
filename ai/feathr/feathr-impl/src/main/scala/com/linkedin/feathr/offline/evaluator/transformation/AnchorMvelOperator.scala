package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.anchored.anchorExtractor.SimpleConfigurableAnchorExtractor
import com.linkedin.feathr.offline.anchored.keyExtractor.MVELSourceKeyExtractor
import com.linkedin.feathr.offline.config.MVELFeatureDefinition
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.{dropAndRenameCols, joinResultToContextDfAndApplyDefaults}
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.graph.NodeUtils.getFeatureTypeConfigsMapForTransformationNodes
import com.linkedin.feathr.offline.job.FeatureTransformation.{getFeatureKeyColumnNames}
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.transformation.DataFrameBasedRowEvaluator
import org.apache.spark.sql.DataFrame

object AnchorMVELOperator extends TransformationOperator {

  /**
   * Compute the anchor MVEL transformation and return the result df and output key columns.
   * @param nodes
   * @param graphTraverser
   * @return (DataFrame, Seq[String])
   */
  def computeMVELResult(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame,
    appendKeyColumns: Boolean): (DataFrame, Seq[String]) = {
    // All nodes in MVEL anchor group will have the same key expression and input node so we can just use the head.
    val inputNodeId = nodes.head.getInputs.get(0).getId // Anchor operators should only have a single input
    val keySeq = graphTraverser.nodeIdToDataframeAndColumnMetadataMap(inputNodeId).keyExpression
    val inputDf = if (appendKeyColumns) graphTraverser.nodeIdToDataframeAndColumnMetadataMap(inputNodeId).df else contextDf

    val featureTypeConfigs = getFeatureTypeConfigsMapForTransformationNodes(nodes)
    val featureNameToMvelExpr = nodes.map(node => graphTraverser.nodeIdToFeatureName(node.getId) -> MVELFeatureDefinition(
      node.getFunction.getParameters.get("expression"), featureTypeConfigs.get(node.getFeatureName))).toMap
    val featureNamesInBatch = featureNameToMvelExpr.keys.toSeq
    val mvelExtractor = new SimpleConfigurableAnchorExtractor(keySeq, featureNameToMvelExpr)

    // Here we make the assumption that the key expression is of the same type of operator as the feature definition and
    // evaluate and append the key columns. Same logic is repeated for SQL expressions too
    val mvelKeyExtractor = new MVELSourceKeyExtractor(mvelExtractor)
    val withKeyColumnDF = if (appendKeyColumns) mvelKeyExtractor.appendKeyColumns(inputDf) else inputDf
    val outputJoinKeyColumnNames = getFeatureKeyColumnNames(mvelKeyExtractor, withKeyColumnDF)
    val transformationResult = DataFrameBasedRowEvaluator.transform(mvelExtractor, withKeyColumnDF,
      featureNamesInBatch.map((_, " ")), featureTypeConfigs, graphTraverser.mvelExpressionContext).df
    (transformationResult, outputJoinKeyColumnNames)
  }

  /**
   * Operator for batch anchor MVEL transformations. Given context df and a grouped set of MVEL transformation nodes,
   * perform the MVEL transformations and return the context df with all the MVEL features joined.
   * @param nodes Seq of nodes with MVEL anchor as operator
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   * @return Dataframe
   */
  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val (transformationResult, outputJoinKeyColumnNames) = computeMVELResult(nodes, graphTraverser, contextDf, appendKeyColumns = true)
    val featureNamesInBatch = nodes.map(node => graphTraverser.nodeIdToFeatureName(node.getId))
    val (prunedResult, keyColumns) = dropAndRenameCols(transformationResult, outputJoinKeyColumnNames, featureNamesInBatch)
    joinResultToContextDfAndApplyDefaults(nodes, graphTraverser, prunedResult, keyColumns, contextDf)
  }

  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    batchCompute(Seq(node), graphTraverser, contextDf, dataPathHandlers)
  }
}
