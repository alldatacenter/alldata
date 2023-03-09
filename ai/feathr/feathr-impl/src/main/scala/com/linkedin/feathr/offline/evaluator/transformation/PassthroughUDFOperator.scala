package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.evaluator.transformation.AnchorUDFOperator.computeUDFResult
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.updateDataframeMapAndApplyDefaults
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame

object PassthroughUDFOperator extends TransformationOperator {
  /**
   * Operator for batch passthrough UDF transformations. Given context df and a grouped set of UDF transformation nodes,
   * perform the UDF transformations. Since this is a passthrough operator, we don't append key columns or join to context.
   * @param nodes Seq of nodes with UDF anchor as operator
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   * @return Dataframe
   */
  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val (result, keyColumns) = computeUDFResult(nodes, graphTraverser, contextDf, appendKeyColumns = false, dataPathHandlers)
    updateDataframeMapAndApplyDefaults(nodes, graphTraverser, result, keyColumns)
  }

  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    batchCompute(Seq(node), graphTraverser, contextDf, dataPathHandlers)
  }
}
