package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.evaluator.transformation.AnchorMVELOperator.computeMVELResult
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.updateDataframeMapAndApplyDefaults
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame

object PassthroughMVELOperator extends TransformationOperator {
  /**
   * Operator for batch passthrough MVEL transformations. Given context df and a grouped set of MVEL transformation nodes,
   * perform the MVEL transformations. Since this is a passthrough operator, we don't append key columns or join to context.
   * @param nodes Seq of nodes with MVEL anchor as operator
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   * @return Dataframe
   */
  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val (result, keyColumns) = computeMVELResult(nodes, graphTraverser, contextDf, appendKeyColumns = false)
    updateDataframeMapAndApplyDefaults(nodes, graphTraverser, result, keyColumns)
  }

  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    batchCompute(Seq(node), graphTraverser, contextDf, dataPathHandlers)
  }
}
