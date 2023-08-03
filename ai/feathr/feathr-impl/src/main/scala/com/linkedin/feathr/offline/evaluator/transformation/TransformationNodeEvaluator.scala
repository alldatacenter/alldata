package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.{AnyNode, Operators}
import com.linkedin.feathr.offline.evaluator.NodeEvaluator
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame

object TransformationNodeEvaluator extends NodeEvaluator {
  /**
   * Evaluate all the transformation nodes in the batch. Note that with the current grouping criteria, we expect all nodes
   * in a batch to have the same operator.
   * @param nodes Nodes to evaluate
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   *  @return DataFrame
   */
  override def batchEvaluate(nodes: Seq[AnyNode], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    // We require that all batch transformation nodes have the same operator so we can pattern match on the head of the
    // node seq to decide on the appropriate TransformationOperator to call.
    val transformationNodes = nodes.map(_.getTransformation)
    val transformationOperator = transformationNodes.head.getFunction.getOperator
    transformationOperator match {
      case Operators.OPERATOR_ID_ANCHOR_MVEL => AnchorMVELOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_ID_ANCHOR_SPARK_SQL_FEATURE_EXTRACTOR => AnchorSQLOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_ID_ANCHOR_JAVA_UDF_FEATURE_EXTRACTOR => AnchorUDFOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_ID_PASSTHROUGH_MVEL => PassthroughMVELOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_ID_PASSTHROUGH_SPARK_SQL_FEATURE_EXTRACTOR => PassthroughSQLOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_ID_PASSTHROUGH_JAVA_UDF_FEATURE_EXTRACTOR => PassthroughUDFOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_ID_DERIVED_MVEL => DerivedSimpleMVELOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_ID_EXTRACT_FROM_TUPLE => DerivedComplexMVELOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_ID_DERIVED_JAVA_UDF_FEATURE_EXTRACTOR => DerivedUDFOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_ID_LOOKUP_MVEL => LookupMVELOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case Operators.OPERATOR_FEATURE_ALIAS => FeatureAliasOperator.batchCompute(transformationNodes, graphTraverser, contextDf, dataPathHandlers)
      case _ => throw new UnsupportedOperationException("Unsupported operator found in Transformation node.")
    }
  }

  override def evaluate(node: AnyNode, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    batchEvaluate(Seq(node), graphTraverser, contextDf, dataPathHandlers)
  }
}
