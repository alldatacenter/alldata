package com.linkedin.feathr.offline.graph

import com.linkedin.feathr.compute.{AnyNode, ComputeGraph, Dependencies}
import com.linkedin.feathr.offline.FeatureDataFrame
import com.linkedin.feathr.offline.client.{IN_PROGRESS, NOT_VISITED, VISITED, VisitedState}
import com.linkedin.feathr.offline.config.{FeatureJoinConfig, JoinConfigSettings}
import com.linkedin.feathr.offline.evaluator.aggregation.AggregationNodeEvaluator
import com.linkedin.feathr.offline.evaluator.datasource.DataSourceNodeEvaluator
import com.linkedin.feathr.offline.evaluator.lookup.LookupNodeEvaluator
import com.linkedin.feathr.offline.evaluator.transformation.TransformationNodeEvaluator
import com.linkedin.feathr.offline.graph.NodeGrouper.{groupAllSWANodes, groupTransformationNodes}
import com.linkedin.feathr.offline.graph.NodeUtils.getFeatureTypeConfigsMap
import com.linkedin.feathr.offline.job.FeatureTransformation.convertFCMResultDFToFDS
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext
import com.linkedin.feathr.offline.source.DataSource
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import com.linkedin.feathr.offline.swa.SlidingWindowFeatureUtils
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat
import com.linkedin.feathr.offline.transformation.FeatureColumnFormat.FeatureColumnFormat
import com.linkedin.feathr.offline.util.datetime.DateTimeInterval
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Case class to hold DataFrame and column metadata.
 * @param df
 * @param keyExpression
 * @param featureColumn
 * @param dataSource
 * @param timestampColumn
 */
case class DataframeAndColumnMetadata(df: DataFrame, keyExpression: Seq[String], featureColumn: Option[String] = None,
  dataSource: Option[DataSource] = None, timestampColumn: Option[String] = None)

/**
 * Case class to hold config settings extracted from the join config + observation data which is needed for evaluation
 * of EVENT and AGGREGATION nodes.
 * @param timeConfigSettings
 * @param featuresToTimeDelayMap
 * @param obsTimeRange
 */
case class TimeConfigSettings(timeConfigSettings: Option[JoinConfigSettings], featuresToTimeDelayMap: Map[String, String], obsTimeRange: DateTimeInterval)

/**
 * The main purpose of the FCMGraphTraverser is to walk a resolved compute graph and perform the feature join specified by the graph + join config.
 * The main API is traverseGraph() which will actually execute the resolve graph. In the initialization of the class, the necessary information
 * like nodes, join config settings, spark session etc. will be extracted from the inputs and the public member variables needed for graph
 * traversal will be created. See the scaladocs of traverseGraph for more info on graph traversal algo.
 * @param inputSparkSession
 * @param featureJoinConfig
 * @param resolvedGraph
 * @param observationDf
 */
class FCMGraphTraverser(inputSparkSession: SparkSession,
  featureJoinConfig: FeatureJoinConfig,
  resolvedGraph: ComputeGraph,
  observationDf: DataFrame,
  dataPathHandlers: List[DataPathHandler],
  mvelContext: Option[FeathrExpressionExecutionContext]) {
  private val log = LogManager.getLogger(getClass.getName)
  // nodeIdToDataframeAndColumnMetadataMap will be a map of node id -> DataframeAndColumnMetadata which will be updated as each node is processed.
  val nodeIdToDataframeAndColumnMetadataMap: mutable.HashMap[Int, DataframeAndColumnMetadata] = mutable.HashMap[Int, DataframeAndColumnMetadata]()

  // Create a map of requested feature names to FeatureColumnFormat (Raw or FDS) for FDS conversion sake at the end of
  // execution. All features will default to Raw unless specified otherwise. Purpose is that some operators will do
  // FDS conversion while others will not.
  val featureColumnFormatsMap: mutable.HashMap[String, FeatureColumnFormat] =
  mutable.HashMap[String, FeatureColumnFormat](featureJoinConfig.joinFeatures.map(joinFeature => (joinFeature.featureName, FeatureColumnFormat.RAW)): _*)

  val nodes: mutable.Buffer[AnyNode] = resolvedGraph.getNodes().asScala
  val nodeIdToFeatureName: Map[Integer, String] = getNodeIdToFeatureNameMap(nodes)
  val mvelExpressionContext: Option[FeathrExpressionExecutionContext] = mvelContext

  // Join info needed from join config + obs data for EVENT and AGGREGATION nodes
  val timeConfigSettings: TimeConfigSettings = getJoinSettings
  val ss: SparkSession = inputSparkSession

  /**
   * Create join settings case object from join config + observation data time range.
   * @return
   */
  private def getJoinSettings: TimeConfigSettings = {
    val obsTimeRange: DateTimeInterval = if (featureJoinConfig.settings.isDefined) {
      SlidingWindowFeatureUtils.getObsSwaDataTimeRange(observationDf, featureJoinConfig.settings)._1.get
    } else null
    TimeConfigSettings(timeConfigSettings = featureJoinConfig.settings,
      featuresToTimeDelayMap = featureJoinConfig.featuresToTimeDelayMap, obsTimeRange = obsTimeRange)
  }

  /**
   * Create map of node ID to feature name
   * @param nodes Buffer of all nodes in compute graph
   * @return Map of node id to feature name
   */
  private def getNodeIdToFeatureNameMap(nodes: mutable.Buffer[AnyNode]): Map[Integer, String] = {
    val derivedFeatureAliasMap: Map[Integer, String] = resolvedGraph.getFeatureNames.asScala.map(x => x._2 -> x._1).toMap
    nodes.filter(node => node.isLookup || node.isAggregation || node.isTransformation).map(node =>
      if (node.isLookup) {
        if (derivedFeatureAliasMap.contains(node.getLookup.getId)) {
          (node.getLookup.getId, derivedFeatureAliasMap(node.getLookup.getId))
        } else {
          (node.getLookup.getId, node.getLookup.getFeatureName)
        }
      } else if (node.isAggregation) {
        if (derivedFeatureAliasMap.contains(node.getAggregation.getId)) {
          (node.getAggregation.getId, derivedFeatureAliasMap(node.getAggregation.getId))
        } else {
          (node.getAggregation.getId, node.getAggregation.getFeatureName)
        }
      } else {
        if (derivedFeatureAliasMap.contains(node.getTransformation.getId)) {
          (node.getTransformation.getId, derivedFeatureAliasMap(node.getTransformation.getId))
        } else if (node.getTransformation.hasFeatureName) {
          (node.getTransformation.getId, node.getTransformation.getFeatureName)
        } else {
          (node.getTransformation.getId, "__seq__join__feature") // TODO: Currently have hacky hard coded names, should add logic for generating names.
        }
      }
    ).toMap
  }

  /**
   * Given a node, return the unfinished dependencies as a set of node ids.
   * @param node
   * @return
   */
  private def getUnfinishedDependencies(node: AnyNode, visitedState: Array[VisitedState]): Set[Integer] = {
    val dependencies = new Dependencies().getDependencies(node).asScala
    dependencies.filter(visitedState(_) != VISITED).toSet
  }

  /**
   * The main graph traversal function for FCMGraphTraverser. Graph traversal algo:
   * 1. Create optimizedGrouping map which specifies if nodes should be executed in the same group.
   * 2. Push all requested nodes onto a stack.
   * 3. Pop a node and evaluate it.
   *    a. For each node evaluation, first check if all the node's dependecies have been visited. If they have not,
   *       push all dependency nodes onto the stack and push the node back onto the stack after marking it as IN_PROGRESS.
   *    b. If all node's dependecies have been visited, pass the node to the appropriate node evaluator.
   *    c. Update the contextDf with the output of the node evaluation.
   *    d. Mark node as VISITED
   * 4. Convert contextDf to FDS and return as FeatureDataFrame
   * @return FeatureDataFrame
   */
  def traverseGraph(): FeatureDataFrame = {
    // Set up stack for graph traversal
    val stack = mutable.Stack[Int]()
    var contextDf: DataFrame = observationDf

    // Optimization: Group all transformation nodes with the same input nodes, keys and transformation function operators.
    val optimizedGroupingMap = groupTransformationNodes(nodes) ++ groupAllSWANodes(nodes)
    val nodeRankingMap = resolvedGraph.getFeatureNames.asScala.values.map(x => if (nodes(x).isAggregation) x -> 1 else x -> 2).toMap
    // Push all requested nodes onto stack processing.
    val visitedState: Array[VisitedState] = Array.fill[VisitedState](nodes.length)(NOT_VISITED)
    resolvedGraph.getFeatureNames.asScala.values.foreach(x => stack.push(x))
    while (stack.nonEmpty) {
      stack.sortBy {case(i) => nodeRankingMap.get(i) }
      val nodeId = stack.pop
      if (visitedState(nodeId) != VISITED) {
        val node = nodes(nodeId)
        // If node is part of an optimized grouping, we have to consider the dependencies of the other nodes in the group also
        val unfinishedDependencies = optimizedGroupingMap.getOrElse(nodeId, Seq(new Integer(nodeId)))
          .foldLeft(Set.empty[Integer])((unfinishedSet, currNodeId) => {
            unfinishedSet ++ getUnfinishedDependencies(nodes(currNodeId), visitedState)
          })
        if (unfinishedDependencies.nonEmpty) {
          if (visitedState(nodeId) == IN_PROGRESS) {
            throw new RuntimeException("Encountered dependency cycle involving node " + nodeId)
          }
          stack.push(nodeId) // revisit this node after its dependencies
          unfinishedDependencies.foreach(stack.push(_)) // visit dependencies
          visitedState(nodeId) = IN_PROGRESS
        } else {
          // actually handle this node, since all its dependencies (if any) are ready
          assert(!nodeIdToDataframeAndColumnMetadataMap.contains(nodeId))
          // If the optimized grouping map contains this nodeId and all the dependencies are finished, we know we can batch evaluate these nodes now.
          // We assume all nodes in a group are the same type, if the grouping fails this criteria then we will throw an error within the evaluator.
          contextDf = if (optimizedGroupingMap.contains(nodeId)) {
            node match {
              // Currently the batch datasource and batch lookup case will not be used as we do not have an optimization for those node types.
              case node if node.isDataSource => DataSourceNodeEvaluator.batchEvaluate(optimizedGroupingMap(nodeId).map(nodes(_)), this, contextDf,
                dataPathHandlers)
              case node if node.isLookup => LookupNodeEvaluator.batchEvaluate(optimizedGroupingMap(nodeId).map(nodes(_)), this, contextDf, dataPathHandlers)
              case node if node.isTransformation => TransformationNodeEvaluator.batchEvaluate(optimizedGroupingMap(nodeId).map(nodes(_)), this, contextDf, dataPathHandlers)
              case node if node.isAggregation => AggregationNodeEvaluator.batchEvaluate(optimizedGroupingMap(nodeId).map(nodes(_)), this, contextDf, dataPathHandlers)
              case node if node.isExternal => throw new RuntimeException(s"External node found in resolved graph traversal. Node information: $node")
            }
          } else {
            node match {
              case node if node.isDataSource => DataSourceNodeEvaluator.evaluate(node, this, contextDf, dataPathHandlers)
              case node if node.isLookup => LookupNodeEvaluator.evaluate(node, this, contextDf, dataPathHandlers)
              case node if node.isTransformation => TransformationNodeEvaluator.evaluate(node, this, contextDf, dataPathHandlers)
              case node if node.isAggregation => AggregationNodeEvaluator.evaluate(node, this, contextDf, dataPathHandlers) // No processing needed for SWA nodes at this stage.
              case node if node.isExternal => throw new RuntimeException(s"External node found in resolved graph traversal. Node information: $node")
            }
          }
          // Mark batch or single node as visited.
          if (optimizedGroupingMap.contains(nodeId)) {
            optimizedGroupingMap(nodeId).foreach(visitedState(_) = VISITED)
          } else {
            visitedState(nodeId) = VISITED
          }
        }
      }
    }

    // Drop all unneeded columns and return the result after FDS conversion
    val featureTypeConfigs = getFeatureTypeConfigsMap(nodes)
    val necessaryColumns = resolvedGraph.getFeatureNames.asScala.keys ++ observationDf.columns
    val toDropCols = contextDf.columns diff necessaryColumns.toSeq
    contextDf = contextDf.drop(toDropCols: _*)
    convertFCMResultDFToFDS(resolvedGraph.getFeatureNames.asScala.keys.toSeq,
      featureColumnFormatsMap.toMap, contextDf, featureTypeConfigs)
  }
}
