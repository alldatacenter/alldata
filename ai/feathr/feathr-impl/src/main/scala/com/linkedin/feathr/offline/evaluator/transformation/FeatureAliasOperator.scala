package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.graph.{DataframeAndColumnMetadata, FCMGraphTraverser}
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col

object FeatureAliasOperator extends TransformationOperator {
  /**
   * Compute feature alias via a withColumn call on the context df.
   * @param node
   * @param graphTraverser
   * @param contextDf
   * @return
   */
  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    // In the case of a feature alias operator we can optimize this by just doing a withColumn call on the contextDf instead of doing a join.
    val inputNodeId = node.getInputs.get(0).getId
    val featureName = if (node.getFeatureName == null) graphTraverser.nodeIdToFeatureName(node.getId) else node.getFeatureName
    val modifiedContextDf = contextDf.withColumn(featureName, col(graphTraverser.nodeIdToFeatureName(inputNodeId)))
    graphTraverser.nodeIdToDataframeAndColumnMetadataMap(node.getId) = DataframeAndColumnMetadata(modifiedContextDf,
      graphTraverser.nodeIdToDataframeAndColumnMetadataMap(inputNodeId).keyExpression)
    modifiedContextDf
  }

  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    nodes.foldLeft(contextDf)((newContextDf, node) => compute(node, graphTraverser, newContextDf, dataPathHandlers))
  }
}
