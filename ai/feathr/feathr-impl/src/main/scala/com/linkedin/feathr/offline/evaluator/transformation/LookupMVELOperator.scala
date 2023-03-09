package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.anchored.anchorExtractor.SimpleConfigurableAnchorExtractor
import com.linkedin.feathr.offline.config.{MVELFeatureDefinition, PegasusRecordFeatureTypeConverter}
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.updateDataframeMapAndApplyDefaults
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.transformation.{DataFrameBasedRowEvaluator, FeatureColumnFormat}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col

/**
 * Operator for specifically the transformation applied for look up base nodes. Note that we have to treat this
 * differently than a derived MVEL feature for parity sakes with feathr v16.
 */
object LookupMVELOperator extends TransformationOperator {

  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val featureName = if (node.getFeatureName == null) graphTraverser.nodeIdToFeatureName(node.getId) else node.getFeatureName
    val featureTypeConfig = PegasusRecordFeatureTypeConverter().convert(node.getFeatureVersion)
    val mvelExpr = node.getFunction.getParameters.get("expression")
    val mvelExtractor = new SimpleConfigurableAnchorExtractor(Seq.empty,
      Map(featureName -> MVELFeatureDefinition(mvelExpr, featureTypeConfig)))


    val transformedDf = DataFrameBasedRowEvaluator.transform(mvelExtractor, contextDf, Seq((featureName, "")),
      Map(featureName -> featureTypeConfig.get), graphTraverser.mvelExpressionContext).df

    // Apply feature alias here if needed.
    val result = if (graphTraverser.nodeIdToFeatureName(node.getId) != node.getFeatureName) {
      val featureAlias = graphTraverser.nodeIdToFeatureName(node.getId)
      graphTraverser.featureColumnFormatsMap(featureAlias) = FeatureColumnFormat.RAW
      transformedDf.withColumn(featureAlias, col(featureName))
    } else transformedDf
    updateDataframeMapAndApplyDefaults(Seq(node), graphTraverser, result, Seq.empty) // Note here lookup MVEL features don't have output key columns
  }

  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame,
    dataPathHandlers: List[DataPathHandler]): DataFrame = {
    nodes.foldLeft(contextDf)((newContextDf, node) => compute(node, graphTraverser, newContextDf, dataPathHandlers))
  }
}
