package com.linkedin.feathr.offline.evaluator.transformation

import com.linkedin.feathr.common.FeatureTypeConfig
import com.linkedin.feathr.compute.Transformation
import com.linkedin.feathr.offline.anchored.anchorExtractor.{SQLConfigurableAnchorExtractor, SQLKeys}
import com.linkedin.feathr.offline.anchored.keyExtractor.SQLSourceKeyExtractor
import com.linkedin.feathr.offline.config.SQLFeatureDefinition
import com.linkedin.feathr.offline.evaluator.transformation.TransformationOperatorUtils.{createFeatureDF, dropAndRenameCols, joinResultToContextDfAndApplyDefaults}
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.graph.NodeUtils.getFeatureTypeConfigsMapForTransformationNodes
import com.linkedin.feathr.offline.job.FeatureTransformation.getFeatureKeyColumnNames
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat
import com.linkedin.feathr.offline.util.FeaturizedDatasetUtils
import org.apache.spark.sql.DataFrame

object AnchorSQLOperator extends TransformationOperator {
  private val USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME = "FDSExtract"

  /**
   * Compute the SQL transformation and return the result dataframe and key columns.
   * @param nodes
   * @param graphTraverser
   * @return
   */
  def computeSQLResult(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame,
    appendKeyColumns: Boolean): (DataFrame, Seq[String]) = {
    // All nodes in SQL anchor group will have the same key expression and input node so we can just use the head.
    val inputNodeId = nodes.head.getInputs.get(0).getId // Anchor operators should only have a single input
    val keySeq = graphTraverser.nodeIdToDataframeAndColumnMetadataMap(inputNodeId).keyExpression
    val inputDf = if (appendKeyColumns) graphTraverser.nodeIdToDataframeAndColumnMetadataMap(inputNodeId).df else contextDf

    val featureTypeConfigs = getFeatureTypeConfigsMapForTransformationNodes(nodes)
    val featureNameToSqlExpr = nodes.map(node => graphTraverser.nodeIdToFeatureName(node.getId) -> SQLFeatureDefinition(
      node.getFunction.getParameters.get("expression"))).toMap
    val featureNamesInBatch = featureNameToSqlExpr.keys.toSeq
    val featureSchemas = featureNamesInBatch
      .map(featureName => {
        // Currently assumes that tensor type is undefined
        val tensorType = FeaturizedDatasetUtils.lookupTensorTypeForFeatureRef(featureName, None,
          featureTypeConfigs.getOrElse(featureName, FeatureTypeConfig.UNDEFINED_TYPE_CONFIG))
        val schema = FeaturizedDatasetUtils.tensorTypeToDataFrameSchema(tensorType)
        featureName -> schema
      })
      .toMap
    val sqlExtractor = new SQLConfigurableAnchorExtractor(SQLKeys(keySeq), featureNameToSqlExpr)

    // Apply SQL transformation and append key columns to inputDf.
    val transformedCols = sqlExtractor.getTensorFeatures(inputDf, featureSchemas)
    val sqlKeyExtractor = new SQLSourceKeyExtractor(keySeq)
    val withKeyColumnDF = if (appendKeyColumns) sqlKeyExtractor.appendKeyColumns(inputDf) else inputDf
    val withFeaturesDf = createFeatureDF(withKeyColumnDF, transformedCols.keys.toSeq)
    val outputJoinKeyColumnNames = getFeatureKeyColumnNames(sqlKeyExtractor, withFeaturesDf)

    // Mark as FDS format if it is the FDSExtract SQL function
    featureNameToSqlExpr.filter(ele => ele._2.featureExpr.contains(USER_FACING_MULTI_DIM_FDS_TENSOR_UDF_NAME))
      .foreach(nameToSql => graphTraverser.featureColumnFormatsMap(nameToSql._1) = FeatureColumnFormat.FDS_TENSOR)

    (withFeaturesDf, outputJoinKeyColumnNames)
  }
  /**
   * Operator for batch anchor SQL transformations. Given context df and a grouped set of SQL transformation nodes,
   * perform the SQL transformations and return the context df with all the SQL features joined.
   * @param nodes Seq of nodes with SQL anchor as operator
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   * @return Dataframe
   */
  override def batchCompute(nodes: Seq[Transformation], graphTraverser: FCMGraphTraverser, contextDf: DataFrame,
    dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val (transformationResult, outputJoinKeyColumnNames) = computeSQLResult(nodes, graphTraverser, contextDf, appendKeyColumns = true)
    val featureNamesInBatch = nodes.map(node => graphTraverser.nodeIdToFeatureName(node.getId))
    val (prunedResult, keyColumns) = dropAndRenameCols(transformationResult, outputJoinKeyColumnNames, featureNamesInBatch)
    joinResultToContextDfAndApplyDefaults(nodes, graphTraverser, prunedResult, keyColumns, contextDf)
  }

  override def compute(node: Transformation, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    batchCompute(Seq(node), graphTraverser, contextDf, dataPathHandlers)
  }
}
