package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.common.FeatureDerivationFunction
import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.client.plugins.{FeathrUdfPluginContext, FeatureDerivationFunctionAdaptor}
import com.linkedin.feathr.offline.evaluator.transformation.BaseDerivedFeatureOperator.applyDerivationFunction
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.updateDataframeMapAndApplyDefaults
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame

/**
 * Transformation operator for derived UDF operator.
 */
object DerivedUDFOperator extends TransformationOperator {
  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val udfClass = Class.forName(node.getFunction.getParameters.get("class"))
    print(udfClass)
    val derivationFunction = udfClass.getDeclaredConstructor().newInstance().asInstanceOf[AnyRef]
    // possibly "adapt" the derivation function, in case it doesn't implement Feathr's FeatureDerivationFunction,
    // using FeathrUdfPluginContext
    val maybeAdaptedDerivationFunction = FeathrUdfPluginContext.getRegisteredUdfAdaptor(udfClass) match {
      case Some(adaptor: FeatureDerivationFunctionAdaptor) => adaptor.adaptUdf(derivationFunction)
      case _ => derivationFunction
    }

    val derivedFunction = maybeAdaptedDerivationFunction.asInstanceOf[FeatureDerivationFunction]
    val newContextDf = applyDerivationFunction(node, derivedFunction, graphTraverser, contextDf)
    updateDataframeMapAndApplyDefaults(Seq(node), graphTraverser, newContextDf, Seq.empty) // Note here derived features don't have output key columns
  }

  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    nodes.foldLeft(contextDf)((newContextDf, node) => compute(node, graphTraverser, newContextDf, dataPathHandlers))
  }
}
