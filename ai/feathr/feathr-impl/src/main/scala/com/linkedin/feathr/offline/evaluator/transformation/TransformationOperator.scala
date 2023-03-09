package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame

/**
 * Trait class for transformation operators. The task of operators is to compute their operation (i.e. MVEL, SQL, etc)
 * and ensure that the result is available in the graphTraverser nodeIdToDataframeAndColumnMetadataMap map,
 * the result is present in the context dataframe, and return the context df.
 */
trait TransformationOperator {
  /**
   * Perform operation on seq of transformation nodes and return context df.
   *
   * @param nodes
   * @param graphTraverser
   * @param contextDf
   */
  def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame

  /**
   * Perform operation on a single transformation node and return context df.
   *
   * @param node
   * @param graphTraverser
   * @param contextDf
   */
  def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame
}
