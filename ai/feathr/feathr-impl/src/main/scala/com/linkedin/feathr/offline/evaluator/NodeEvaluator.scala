package com.linkedin.feathr.offline.evaluator

import com.linkedin.feathr.compute.AnyNode
import com.linkedin.feathr.offline.graph.FCMGraphTraverser
import com.linkedin.feathr.offline.source.accessor.DataPathHandler
import org.apache.spark.sql.DataFrame

/**
 * Base trait class for all node evaluators. For each node type, the evaluate API should take a single node along with
 * the necessary inputs, perform the necessary dataloading or transformations specific to the node type, and return the
 * the context df. The batchEvaluate API is the batch version of the evaluate API. Node evaluators must ONLY evaluate the node
 * in the inputs and not evaluate any other nodes within the graph out of order.
 *
 * Note that the graphTraverser is a class object which contains metadata regarding the graph and graph traversal state
 * which are needed for node evaluation which is why FCMGraphTraverser is needed in the evaluation functions.
 * Graph metadata available in graphTraverser:
 * 1. nodeIdToDataframeAndColumnMetadataMap: Map of node id to node feature df and node metadata.
 *    See scaladocs of DataframeAndColumnMetadata for more info.
 * 2. featureColumnFormatsMap: Map of output format of feature column (RAW vs FDS)
 * 3. nodes: all nodes in resolved graph
 * 4. nodeIdToFeatureName: node id to feature name
 * 5. joinSettings: settings from join config + observation data time range for EVENT and AGGREGATION node processing
 * 6. ss: spark session for spark calls
 *
 * GRAPHTRAVERSER UPDATE REQUIREMENTS:
 * 1. nodeIdToDataframeAndColumnMetadataMap needs to be updated for datasource nodes and look up expansion nodes.
 * 2. all node evaluators which produce a feature column in the context df must mark the format in featureColumnFormatsMap
 *    if the feature column is already in FDS format.
 */
trait NodeEvaluator {
  /**
   * Evaluate a single node according to the node type and return the context df. ContextDf should contain the output
   * of the node evaluation in all cases except for Datasource nodes and seq join expansion feature nodes. Output of
   * node evaluation is feature column and feature column is joined to context df based on feature join key.
   * @param node Node to evaluate
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   * @return DataFrame
   */
  def evaluate(node: AnyNode, graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame

  /**
   * Evaluate a group of nodes and return the context df. ContextDf should contain the output
   * of all the node evaluation in all cases except for Datasource nodes and seq join expansion feature nodes. Output of
   * node evaluation is feature column and feature column is joined to context df based on feature join key.
   * @param nodes Nodes to evaluate
   * @param graphTraverser FCMGraphTraverser
   * @param contextDf Context df
   * @return DataFrame
   */
  def batchEvaluate(nodes: Seq[AnyNode], graphTraverser: FCMGraphTraverser, contextDf: DataFrame, dataPathHandlers: List[DataPathHandler]): DataFrame
}