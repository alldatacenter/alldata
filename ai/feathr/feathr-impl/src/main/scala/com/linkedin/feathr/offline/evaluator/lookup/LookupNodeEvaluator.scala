package com.linkedin.feathr.offline.evaluator.lookup

import com.linkedin.feathr.common.FeatureValue
import com.linkedin.feathr.compute.{AnyNode, Lookup}
import com.linkedin.feathr.offline.PostTransformationUtil
import com.linkedin.feathr.offline.graph.{DataframeAndColumnMetadata, FCMGraphTraverser}
import com.linkedin.feathr.offline.derived.strategies.SeqJoinAggregator
import com.linkedin.feathr.offline.derived.strategies.SequentialJoinAsDerivation.getDefaultTransformation
import com.linkedin.feathr.offline.evaluator.NodeEvaluator
import com.linkedin.feathr.offline.graph.NodeUtils.getDefaultConverter
import com.linkedin.feathr.offline.join.algorithms.{JoinType, SequentialJoinConditionBuilder, SparkJoinWithJoinCondition}
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.transformation.MvelDefinition
import com.linkedin.feathr.offline.util.DataFrameSplitterMerger
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, lit, monotonically_increasing_id}

import scala.collection.JavaConverters.asScalaBufferConverter

/**
 * LookupNodeEvaluator contains processLookupNode function needed to evaluate Lookup Nodes which represent seq join where we have an
 * expansion feature which will be keyed on a base feature.
 */
object LookupNodeEvaluator extends NodeEvaluator {
  /**
   * Process look up node which represents seq join. The graph traverser is responsible for gathering the necessary info
   * to complete the look up node processing and call processLookupNode. This function will perform the seq join where
   * the expansion feature will be joined to the context df based on the base feature.
   * @param lookupNode Lookup Node
   * @param baseNode DataframeAndColumnMetadata of base feature node.
   * @param baseKeyColumns Column name of base feature.
   * @param expansionNode DataframeAndColumnMetadata of expansion feature node.
   * @param contextDf Context df
   * @param seqJoinFeatureName Seq join feature name
   * @param seqJoinJoiner Seq join joiner with seq join spark join condition
   * @param defaultValueMap Default values map to be used for default value substitution
   * @param ss Spark session
   * @return DataframeAndColumnMetadata
   */
  def processLookupNode(lookupNode: Lookup,
    baseNode: DataframeAndColumnMetadata,
    baseKeyColumns: Seq[String],
    expansionNode: DataframeAndColumnMetadata,
    contextDf: DataFrame,
    seqJoinFeatureName: String,
    seqJoinJoiner: SparkJoinWithJoinCondition,
    defaultValueMap: Map[String, FeatureValue],
    ss: SparkSession): DataframeAndColumnMetadata = {
    // Get only required expansion features
    val expansionFeatureName = expansionNode.featureColumn.get
    val expansionNodeCols = expansionNode.keyExpression ++ Seq(expansionNode.featureColumn.get)
    val expansionNodeDF = expansionNode.df.select(expansionNodeCols.map(col): _*)
    // rename columns to know which columns are to be dropped
    val expansionNodeRenamedCols = expansionNodeDF.columns.map(c => "__expansion__" + c).toSeq
    val expansionNodeDfWithRenamedCols = expansionNodeDF.toDF(expansionNodeRenamedCols: _*)

    // coerce left join keys before joining base and expansion features
    val left: DataFrame = PostTransformationUtil.transformFeatures(Seq((baseNode.featureColumn.get, baseNode.featureColumn.get)), contextDf,
      Map.empty[String, MvelDefinition], getDefaultTransformation, None)

    // Partition base feature (left) side of the join based on null values. This is an optimization so we don't waste
    // time joining nulls from the left df.
    val (coercedBaseDfWithNoNull, coercedBaseDfWithNull) = DataFrameSplitterMerger.splitOnNull(left, baseNode.featureColumn.get)

    val groupByColumn = "__frame_seq_join_group_by_id"
    /* We group by the monotonically_increasing_id to ensure we do not lose any of the observation data.
     * This is essentially grouping by all the columns in the left table
     * Note: we cannot add the monotonically_increasing_id before DataFrameSplitterMerger.splitOnNull.
     * the implementation of monotonically_increasing_id is non-deterministic because its result depends on partition IDs.
     * and it can generate duplicate ids between the withNoNull and WithNull part.
     * see: https://godatadriven.com/blog/spark-surprises-for-the-uninitiated
     */
    val leftWithUidDF = coercedBaseDfWithNoNull.withColumn(groupByColumn, monotonically_increasing_id)
    val (adjustedLeftJoinKey, explodedLeft) = SeqJoinAggregator.explodeLeftJoinKey(ss, leftWithUidDF, baseKeyColumns, seqJoinFeatureName)

    // join base feature's results with expansion feature's results
    val intermediateResult = seqJoinJoiner.join(adjustedLeftJoinKey, explodedLeft,
      expansionNode.keyExpression.map(c => "__expansion__" + c), expansionNodeDfWithRenamedCols, JoinType.left_outer)
    val producedFeatureName = "__expansion__" + expansionFeatureName

    /*
     * Substitute defaults. The Sequential Join inherits the default values from the expansion feature definition.
     * This step is done before applying aggregations becaUSE the default values should be factored in.
     */
    val expansionFeatureDefaultValue = defaultValueMap.get(expansionFeatureName)
    val intermediateResultWithDefault =
      SeqJoinAggregator.substituteDefaultValuesForSeqJoinFeature(intermediateResult, producedFeatureName, expansionFeatureDefaultValue, ss)

    // apply aggregation to non-null part
    val aggregationType = lookupNode.getAggregation
    val aggDf = SeqJoinAggregator.applyAggregationFunction(
      seqJoinFeatureName, producedFeatureName, intermediateResultWithDefault, aggregationType, groupByColumn)

    // Similarly, substitute the default values and apply aggregation function to the null part.
    val coercedBaseDfWithNullWithDefault = SeqJoinAggregator.substituteDefaultValuesForSeqJoinFeature(
      coercedBaseDfWithNull.withColumn(producedFeatureName, lit(null).cast(intermediateResult.schema(producedFeatureName).dataType)),
      producedFeatureName,
      expansionFeatureDefaultValue,
      ss)
    val coercedBaseDfWithNullWithAgg = SeqJoinAggregator.applyAggregationFunction(
      seqJoinFeatureName,
      producedFeatureName,
      coercedBaseDfWithNullWithDefault.withColumn(groupByColumn, monotonically_increasing_id),
      aggregationType,
      groupByColumn)

    // Union the rows that participated in the join and the rows with nulls
    val finalRes = DataFrameSplitterMerger.merge(aggDf, coercedBaseDfWithNullWithAgg)

    val resWithDroppedCols = finalRes.drop(expansionNode.keyExpression.map(c => "__expansion__" + c): _*)
      .drop("__base__" + baseNode.featureColumn.get)
    val finalResAfterDroppingCols = resWithDroppedCols.withColumnRenamed(producedFeatureName, seqJoinFeatureName)

    DataframeAndColumnMetadata(finalResAfterDroppingCols, baseNode.keyExpression.map(x => x.split("__").last), Some(seqJoinFeatureName))
  }

  /**
   * Given a node, return its concrete keys as a Seq[Integer]
   * @param node
   * @return
   */
  private def getLookupNodeKeys(node: AnyNode): Seq[Integer] = {
    node match {
      case n if n.isLookup => n.getLookup.getConcreteKey.getKey.asScala
      case n if n.isDataSource => if (n.getDataSource.hasConcreteKey) n.getDataSource.getConcreteKey.getKey().asScala else null
      case n if n.isTransformation => n.getTransformation.getConcreteKey.getKey.asScala
    }
  }

  /**
   * Evaluate a lookup node and set the node's DataframeAndColumnMetadata in the graph traverser to be the output of the node evaluation. Returns
   * the output of lookup joined to the context df.
   *
   * @param node           Lookup Node to evaluate
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf      Context df
   * @return DataFrame
   */
  override def evaluate(node: AnyNode, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    val lookUpNode = node.getLookup
    // Assume there is only one lookup key that is a node reference. In the future this may not be true and will have to be changed.
    // NOTE: We currently assume there is only 1 base node because that is what is supported currently in the feathr HOCON config
    // there is no such constraint on the graph model. TODO: Modify the implementation of lookup such that multiple base nodes
    // are supported.
    val baseNodeRef = lookUpNode.getLookupKey.asScala.find(x => x.isNodeReference).get.getNodeReference
    val baseNode = graphTraverser.nodeIdToDataframeAndColumnMetadataMap(baseNodeRef.getId)
    val baseKeyColumns = getLookupNodeKeys(graphTraverser.nodes(lookUpNode.getLookupNode))
      .flatMap(x => if (graphTraverser.nodeIdToDataframeAndColumnMetadataMap(x).featureColumn.isDefined) {
        Seq(graphTraverser.nodeIdToDataframeAndColumnMetadataMap(x).featureColumn.get)
      } else {
        graphTraverser.nodeIdToDataframeAndColumnMetadataMap(x).keyExpression
      })
    val expansionNodeId = lookUpNode.getLookupNode()
    val expansionNode = graphTraverser.nodeIdToDataframeAndColumnMetadataMap(expansionNodeId)
    val seqJoinFeatureName = graphTraverser.nodeIdToFeatureName(lookUpNode.getId)

    val expansionNodeDefaultConverter = getDefaultConverter(Seq(graphTraverser.nodes(expansionNodeId)))
    val lookupNodeContext = LookupNodeEvaluator.processLookupNode(lookUpNode, baseNode,
      baseKeyColumns, expansionNode, contextDf, seqJoinFeatureName, SparkJoinWithJoinCondition(SequentialJoinConditionBuilder),
      expansionNodeDefaultConverter, graphTraverser.ss)

    // Update nodeIdToDataframeAndColumnMetadataMap and return new contextDf
    graphTraverser.nodeIdToDataframeAndColumnMetadataMap(lookUpNode.getId) = lookupNodeContext
    lookupNodeContext.df
  }

  // Batch evaluate just calls single evaluate sequentially
  override def batchEvaluate(nodes: Seq[AnyNode], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame = {
    nodes.foldLeft(contextDf)((updatedContextDf, node) => evaluate(node, graphTraverser, updatedContextDf, dataPathHandlers))
  }
}
