package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.evaluator.transformation.AnchorSQLOperator.computeSQLResult
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.updateDataframeMapAndApplyDefaults
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame

object PassthroughSQLOperator extends TransformationOperator {
  /**
   * Operator for batch passthrough SQL transformations. Given context df and a grouped set of SQL transformation nodes,
   * perform the SQL transformations. Since this is a passthrough operator, we don't append key columns or join to context.
   * @param nodes Seq of nodes with UDF anchor as operator
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   * @return Dataframe
   */
  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val (result, keyColumns) = computeSQLResult(nodes, graphTraverser, contextDf, appendKeyColumns = false)
    updateDataframeMapAndApplyDefaults(nodes, graphTraverser, result, keyColumns)
  }

  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    batchCompute(Seq(node), graphTraverser, contextDf, dataPathHandlers)
  }
}
