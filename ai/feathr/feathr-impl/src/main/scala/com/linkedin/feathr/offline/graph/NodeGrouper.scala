package com.linkedin.feathr.offline.graph

import com.linkedin.feathr.compute.{AnyNode, ConcreteKey, NodeReference, Operators}
import com.linkedin.feathr.offline.client.plugins.{AnchorExtractorAdaptor, FeathrUdfPluginContext, SimpleAnchorExtractorSparkAdaptor}

import scala.collection.mutable

/**
 * This NodeGrouper class contains utility functions which group nodes into batches. This exists because we have optimizations
 * where SWA and anchor features are best transformed together in a group so we need to signal to the node evaluators via
 * these groupings that certain nodes (like all SWA, all transformation nodes with same extractor, etc.) can be executed
 * together as a group.
 */
object NodeGrouper {
  /**
   * Given a set of nodes, group the Aggregation nodes and return a map of node id to seq of nodes in the same group.
   * By grouping the nodes we can minimize the number of calls to the SWJ library and minimize the number of spark operations needed.
   * Grouping criteria: is we group all aggregation nodes which have the same concrete key.
   * @param nodes Buffer of nodes
   * @return Map of node id to seq of node id's in the same group
   */
  def groupSWANodes(nodes: Seq[AnyNode]): mutable.HashMap[Integer, Seq[Integer]] = {
    val allSWANodes = nodes.filter(node => node.getAggregation != null)
    val swaMap = mutable.Map[ConcreteKey, Seq[Integer]]()
    allSWANodes.map (node => {
      val concreteKey = node.getAggregation.getConcreteKey
      if (!swaMap.contains(concreteKey)) swaMap.put(concreteKey, Seq(node.getAggregation.getId()))
      else {
        val existingGroup = swaMap(concreteKey)
        val updatedGroup = existingGroup :+ node.getAggregation.getId()
        swaMap.put(concreteKey, updatedGroup)
      }
    })
    val groupedAggregationNodeMap = mutable.HashMap.empty[Integer, Seq[Integer]]
    swaMap.values.map(nodeArray => {
      nodeArray.map(node => groupedAggregationNodeMap.put(node, nodeArray))
    })
    groupedAggregationNodeMap
  }

  /**
   * Given a buffer of nodes, return a map of all SWA nodes. Map keys are node id of swa nodes and value will be
   * a seq of all swa node ids. Purpose of this grouping is that all SWA nodes should be evaluated together as a
   * group to optimize performance of SWJ library.
   * @param nodes
   * @return
   */
  def groupAllSWANodes(nodes: mutable.Buffer[AnyNode]): Map[Integer, Seq[Integer]] = {
    val allSWANodes = nodes.filter(node => node.getAggregation != null).map(node => node.getAggregation.getId)
    allSWANodes.map(node => (node, allSWANodes)).toMap
  }

  /**
   * Given a set of nodes, group specifically the anchor feature nodes and return a map of node id to seq of node id's in the same
   * group. Note here that the definition of an anchor feature node is a transformation node which has a data source node as input.
   * The purpose of grouping here is to minimize the number of calls to the different operators such that nodes that can be
   * computed in the same step will be computed in the same step. For example, we want to group all MVEL operations so that we apply
   * the MVEL transformations on each row only one time and not one time per node.
   * Grouping criteria: nodes with the same concrete key and same transformation operator will be grouped together.
   * @param nodes Buffer of nodes
   * @return Map of node id to seq of node id's in the same group
   */
  def groupTransformationNodes(nodes: mutable.Buffer[AnyNode]): Map[Integer, Seq[Integer]] = {
    val allAnchorTransformationNodes = nodes.filter(node => node.getTransformation != null && node.getTransformation.getInputs.size() == 1 &&
      nodes(node.getTransformation.getInputs.get(0).getId()).isDataSource)
    val transformationNodesMap = mutable.Map[(NodeReference, ConcreteKey, String, String), Seq[Integer]]()
    allAnchorTransformationNodes.map(node => {
      val inputNode = node.getTransformation.getInputs().get(0) // Already assumed that it is an anchored transformation node
      val concreteKey = node.getTransformation.getConcreteKey
      val transformationOperator = node.getTransformation.getFunction().getOperator()
      val extractorClass = if (transformationOperator == Operators.OPERATOR_ID_ANCHOR_JAVA_UDF_FEATURE_EXTRACTOR) {
        val className = node.getTransformation.getFunction().getParameters.get("class")
        FeathrUdfPluginContext.getRegisteredUdfAdaptor(Class.forName(className)) match {
          case Some(adaptor: AnchorExtractorAdaptor) =>
            "rowExtractor"
          case _ => className
          case None => className
        }
      } else {
        "non_java_udf"
      }

      if (!transformationNodesMap.contains((inputNode, concreteKey, transformationOperator, extractorClass))) {
        transformationNodesMap.put((inputNode, concreteKey, transformationOperator, extractorClass), Seq(node.getTransformation.getId()))
      } else {
        val existingGroup = transformationNodesMap(inputNode, concreteKey, transformationOperator, extractorClass)
        val updatedGroup = existingGroup :+ node.getTransformation.getId()
        transformationNodesMap.put((inputNode, concreteKey, transformationOperator, extractorClass), updatedGroup)
      }
    })

    transformationNodesMap.values.foldLeft(Map.empty[Integer, Seq[Integer]])((groupMap, nodes) => {
      groupMap ++ nodes.map(node => (node, nodes)).toMap
    })
  }

}
