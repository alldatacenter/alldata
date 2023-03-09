package com.linkedin.feathr.compute;

import com.linkedin.data.template.IntegerMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Functions for working with instances of compute graphs.
 */
@InternalApi
public class ComputeGraphs {
  private ComputeGraphs() { }

  /**
   * Ensures the input Graph is internally consistent.
   * @param graph
   * @return
   */
  public static ComputeGraph validate(ComputeGraph graph) {
    ensureNodeIdsAreSequential(graph);
    ensureNodeReferencesExist(graph);
    ensureNoDependencyCycles(graph);
    ensureNoExternalReferencesToSelf(graph);
    return graph;
  }

  /**
   * Graph 1:
   *   A
   *   |
   *   B
   *
   * Graph 2:
   *   A
   *   |
   *   C
   *
   * Merge(Graph1, Graph2):
   *    A
   *   / \
   *  B   C
   *
   * Other cases: The graphs could have nothing in common, in which case the merged graph is "not fully connected" but
   * is still "one graph."
   *
   *
   * Example for "Derived Features"
   * e.g. featureC = featureA + featureB
   * Assume featureA, featureB are anchored.
   *
   * What the definitions look like:
   *
   *  myAnchor1: {
   *    source: "/foo/bar/baz"
   *    key: "x"
   *    features: {
   *      featureA: "source_columnA.nested_field6"
   *    }
   *  }
   *
   *  myAnchor2: {
   *    source: "..."
   *    key: "foo"
   *    features: {
   *      featureB: "field7"
   *    }
   *  }
   *
   *  featureC: "featureA + featureB"
   *
   * Algorithm to read the above:
   *  * Read 3 subgraphs, one for featureA, one for FeatureB, one for FeatureC
   *  * Merge them together,
   *  * Return
   *
   *
   *  Loading/translating definition for featureA:
   *        DataSource for FeatureA
   *                 |
   *        Transformation (the "extraction function" for FeatureA")
   *                 |
   *             (FeatureA)
   *  (FeatureB looks the same way)
   *
   *  For FeatureC's subgraph:
   *       A   B  <----- these aren't defined in FeatureC's subgraph!
   *        \ /
   *         C <------ C is defined in this graph, with it's operator (+)
   *
   *    ExternalNode(FeatureA)               ExternalNode(FeatureB)
   *                  \                       /
   *                  TransformationNode(operator=+, inputs=[the above nodes])
   *                            |
   *                         FeatureC
   *
   *
   *
   * @param inputGraphs
   * @return
   */
  public static ComputeGraph merge(Collection<ComputeGraph> inputGraphs) {
    ComputeGraphBuilder builder = new ComputeGraphBuilder();
    inputGraphs.forEach(inputGraph -> {
      int offset = builder.peekNextNodeId();
      inputGraph.getNodes().forEach(inputNode -> {
        AnyNode copy = PegasusUtils.copy(inputNode);
        Dependencies.remapDependencies(copy, i -> i + offset);
        builder.addNode(copy);
      });
      inputGraph.getFeatureNames().forEach((featureName, nodeId) -> {
        builder.addFeatureName(featureName, nodeId + offset);

      });
    });
    ComputeGraph mergedGraph = builder.build(new ComputeGraph(), false);
    return validate(removeExternalNodesForFeaturesDefinedInThisGraph(mergedGraph));
  }

  /*

            A       B
              \   /
                C

       There might be more than one way this could be represented as a ComputeGraph.
            0:A   1:B
              \   /
               2:C
       Another possibility:
            1:A   2:B
              \   /
               0:C

       If we wanted to merge:
         I:
              0:A   1:B
                \   /
                 2:C
         II:
              1:A   2:B
                \   /
                 0:C
       Assuming the only differences are the arbitrarily chosen IDs,
       we still want the output to be:
            0:A   1:B
              \   /
               2:C

       Two nodes won't just be the same because they have the same operator (e.g. +), but they also need to have the same
       inputs. Recursively.
   */

  /**
   * Removes redundant parts of the graph.
   *
   * Nodes are considered to be "twins" if:
   *  1. their contents are the same except for their node ID (just the main node ID, not the dependency node IDs!),
   *  OR:
   *  2. their contents are the same except for their node IDs, and except for any dependency node IDs that are "twins"
   *     even if their IDs are different.
   *
   * @param inputGraph an input graph
   * @return a equivalent output graph with any duplicate nodes or subgraphs removed and their dependencies updated
   */
  public static ComputeGraph removeRedundancies(ComputeGraph inputGraph) throws CloneNotSupportedException {
    /*
      The intuitive approach is to start by deduplicating all source nodes into a "standardized" set of source nodes,
      and recursively updating any nodes that depended on them, to all point to a standardized node ID for each source.
      You can then proceed "up one level" to the nodes that depend on the sources, checking them based on criterion (1)
      mentioned in the javadoc above, since by this time their dependency node IDs should already have been
      standardized. It is slightly more complex in cases where a single node may depend on the same node via multiple
      paths, potentially with a different number of edges between (so you cannot actually iterate over the graph "level
      by level").
     */

    /*
      Overall algorithm:
        0. Init "unique node set"
        1. Init IN_PROGRESS, VISITED, UNVISITED table (key is node reference)
        2. Put all nodes in a stack.
        3. While stack is not empty, pop a node:
            Is the node VISITED?
            YES: Do nothing
            NO:  Does this node have any dependencies that are not VISITED?
                 YES: Is this node marked as IN_PROGRESS?
                      YES: Fail – This indicates a cycle in the graph.
                      NO:  1. Mark this node as IN_PROGRESS
                           2. Push this node, and then each of its dependencies, onto the stack.
                 NO:  1. Is this node in the unique node set IGNORING ID?
                         YES: Rewire INBOUND REFERENCES to this node, to point to the twin in the unique node set.
                         NO:  Add this node to the unique node set.
                      2. Mark this node as VISITED.

      Algorithm for "Is this node in the unique-node set, IGNORING ID? If so rewire INBOUND REFERENCES to this node,
        to point to the twin in the unique node set.":
        - Create copies of the input nodes, with their IDs set to zero. Keep track of their IDs via a different way,
          via a nodeIndex Map<Integer, AnyNode>.
        - Represent the unique-nodes set as a uniqueNodesMap HashMap<AnyNode, Integer>. The key is the "standardized"
          node with its id still zeroed out, and the value is its actual ID.
        - To check whether a given node is in the unique-nodes set, just test whether the uniqueNodesMap contains that
          node as a "key." If so, use its corresponding value for rewiring the node's dependents.
        - To rewire the node's dependents, construct an index of "who-depends-on-me" at the top of the function, and
          use it to figure out which nodes need to be rewired.
        - Since the feature name map (map of feature names to node IDs) works differently from node-to-node
          dependencies, separately keep a "which-feature-names-depend-on-me" index and update that too (same as in
          previous step).
     */

    Map<Integer, Set<Integer>> whoDependsOnMeIndex = getReverseDependencyIndex(inputGraph);
    // More than one feature name could point to the same node, e.g. if they are aliases.
    Map<Integer, Set<String>> featureDependencyIndex = getReverseFeatureDependencyIndex(inputGraph);

    // create copies of all nodes, and set their IDs to zero
    List<AnyNode> nodes = inputGraph.getNodes().stream()
        .map(PegasusUtils::copy)
        .collect(Collectors.toList());
    nodes.forEach(node -> PegasusUtils.setNodeId(node, 0)); // set node IDs to zero, to facilitate comparison

    IntegerMap featureNameMap = inputGraph.getFeatureNames();

    // We are going to "standardize" each subgraph. This requires traversing the graph and standardizing each node
    // (after its dependencies have been standardized). This requires checking whether a node already exists in the
    // standardized set. Instead of a set, we will use a hash map. The keys are the "standardized nodes" (with IDs set
    // to zero, since we want to ignore node ID for comparison) and the values are the node's standardized ID.
    Map<AnyNode, Integer> standardizedNodes = new HashMap<>();

    // init deque with IDs from 0 to N - 1
    Deque<Integer> deque = IntStream.range(0, nodes.size()).boxed().collect(Collectors.toCollection(ArrayDeque::new));
    // init visited-state vector
    List<VisitedState> visitedState = new ArrayList<>(Collections.nCopies(nodes.size(), VisitedState.NOT_VISITED));

    while (!deque.isEmpty()) {
      int thisNodeId = deque.pop();
      if (visitedState.get(thisNodeId) == VisitedState.VISITED) {
        continue;
      }
      AnyNode thisNode = nodes.get(thisNodeId);
      Set<Integer> myDependencies = new Dependencies().getDependencies(thisNode);
      List<Integer> unfinishedDependencies = myDependencies.stream()
          .filter(i -> visitedState.get(i) != VisitedState.VISITED)
          .collect(Collectors.toList());
      if (!unfinishedDependencies.isEmpty()) {
        if (visitedState.get(thisNodeId) == VisitedState.IN_PROGRESS) {
          // If I am already in-progress, it means I depended on myself (possibly via other dependency nodes).
          throw new RuntimeException("Dependency cycle detected at node " + thisNodeId);
        }
        deque.push(thisNodeId); // Push myself back onto the deque, so that we can reprocess me later after my dependencies.
        visitedState.set(thisNodeId, VisitedState.IN_PROGRESS); // Also mark myself as in-progress (prevent infinite loop in
        // case of a cycle).
        unfinishedDependencies.forEach(deque::push);
      } else {
        // Time to standardize this node (all of its dependencies [including transitive] have been standardized).
        // 1. See if I am already standardized (check if I have a "twin" in the standardized set)
        Integer standardizedNodeId = standardizedNodes.get(thisNode);
        if (standardizedNodeId != null) {
          // 2. If I DO have a twin in the standardized set, then rewire all the nodes who depend on me, to point to
          //    my standardized twin instead.
          whoDependsOnMeIndex.getOrDefault(thisNodeId, Collections.emptySet()).forEach(nodeWhoDependsOnMe ->
              Dependencies.remapDependencies(nodes.get(nodeWhoDependsOnMe),
                  // "If it points to me, remap it to my standardized twin, else leave it unchanged."
                  id -> id == thisNodeId ? standardizedNodeId : id));
          // Do the same for the feature name map.
          featureDependencyIndex.getOrDefault(thisNodeId, Collections.emptySet()).forEach(featureThatPointsToMe ->
              featureNameMap.put(featureThatPointsToMe, standardizedNodeId));
        } else {
          // 3. If I DON'T have a twin in the standardized set, then put myself into the standardized set.
          standardizedNodes.put(thisNode, thisNodeId);
        }
        // 4. This node ahs been standardized. Mark it as VISITED.
        visitedState.set(thisNodeId, VisitedState.VISITED);
      }
    }

    // Put the IDs back into the nodes.
    standardizedNodes.forEach((node, id) -> PegasusUtils.setNodeId(node, id));

    // Reindex the nodes to ensure IDs are sequential.
    return reindexNodes(standardizedNodes.keySet(), featureNameMap);
  }

  private static ComputeGraph removeExternalNodesForFeaturesDefinedInThisGraph(ComputeGraph inputGraph) {
    Map<Integer, Integer> externalNodeRemappedIds = new HashMap<>();
    for (int id = 0; id < inputGraph.getNodes().size(); id++) {
      AnyNode node = inputGraph.getNodes().get(id);
      if (node.isExternal()) {
        Integer featureNodeId = inputGraph.getFeatureNames().get(node.getExternal().getName());
        if (featureNodeId != null) {
          // "any node who depends on me, should actually depend on that other node instead"
          externalNodeRemappedIds.put(id, featureNodeId);
        }
      }
    }
    if (externalNodeRemappedIds.isEmpty()) {
      return inputGraph;
    } else {
      inputGraph.getNodes().forEach(node -> {
        Dependencies.remapDependencies(node, id -> {
          Integer remappedId = externalNodeRemappedIds.get(id);
          if (remappedId != null) {
            return remappedId;
          } else {
            return id;
          }
        });
      });
      return removeNodes(inputGraph, externalNodeRemappedIds::containsKey);
    }
  }

  /**
   * Remove nodes from a graph.
   * @param computeGraph input graph
   * @param predicate nodes for which this predicate is true, will be removed. the predicate must return true or false
   *                  for all valid nodeIds in this graph (but could throw exceptions for other, invalid cases)
   * @return new graph with the nodes removed
   */
  static ComputeGraph removeNodes(ComputeGraph computeGraph, Predicate<Integer> predicate) {
    List<AnyNode> nodesToKeep = IntStream.range(0, computeGraph.getNodes().size()).boxed()
        .filter(predicate.negate())
        .map(computeGraph.getNodes()::get)
        .collect(Collectors.toList());
    return reindexNodes(nodesToKeep, computeGraph.getFeatureNames());
  }

  /**
   * Rebuilds a graph with a new (valid, sequential) set of IDs. The input nodes must form a valid subgraph, e.g.
   * all node references (and feature names) must point to nodes within the subgraph.
   *
   * @param nodes the nodes (WILL BE MODIFIED)
   * @param featureNames feature name map
   * @return the reindexed compute graph
   */
  static ComputeGraph reindexNodes(Collection<AnyNode> nodes, IntegerMap featureNames) {
    Map<Integer, Integer> indexRemapping = new HashMap<>();
    ComputeGraphBuilder builder = new ComputeGraphBuilder();
    nodes.forEach(node -> {
      int oldId = PegasusUtils.getNodeId(node);
      int newId = builder.addNode(node);
      indexRemapping.put(oldId, newId);
    });
    Function<Integer, Integer> remap = oldId -> {
      Integer newId = indexRemapping.get(oldId);
      if (newId == null) {
        throw new RuntimeException("Node " + oldId + " not found in subgraph.");
      }
      return newId;
    };
    // This is taking advantage of the fact that the nodes are mutable. If we switch to using an immutable API e.g.
    // with Protobuf, we'd need to change this somewhat.
    nodes.forEach(node -> Dependencies.remapDependencies(node, remap));
    featureNames.forEach((featureName, nodeId) -> builder.addFeatureName(featureName, remap.apply(nodeId)));
    return builder.build();
  }

  private static Map<Integer, Set<Integer>> getReverseDependencyIndex(ComputeGraph graph) {
    Map<Integer, Set<Integer>> reverseDependencies = new HashMap<>();
    for (int nodeId = 0; nodeId < graph.getNodes().size(); nodeId++) {
      AnyNode node = graph.getNodes().get(nodeId);
      for (int dependencyNodeId : new Dependencies().getDependencies(node)) {
        Set<Integer> dependentNodes = reverseDependencies.computeIfAbsent(dependencyNodeId, x -> new HashSet<>());
        dependentNodes.add(nodeId);
      }
    }
    return reverseDependencies;
  }

  /**
   * More than one feature name could point to the same node, e.g. if they are aliases.
   * @param graph
   * @return
   */
  static Map<Integer, Set<String>> getReverseFeatureDependencyIndex(ComputeGraph graph) {
    // More than one feature name could point to the same node, e.g. if they are aliases.
    Map<Integer, Set<String>> reverseDependencies = new HashMap<>();
    graph.getFeatureNames().forEach((featureName, nodeId) -> {
      Set<String> dependentFeatures = reverseDependencies.computeIfAbsent(nodeId, x -> new HashSet<>(1));
      dependentFeatures.add(featureName);
    });
    return reverseDependencies;
  }

  /**
   * Ensures that all the nodes are sequential.
   * @param graph
   */
  static void ensureNodeIdsAreSequential(ComputeGraph graph) {
    for (int i = 0; i < graph.getNodes().size(); i++) {
      if (PegasusUtils.getNodeId(graph.getNodes().get(i)) != i) {
        throw new RuntimeException("Graph nodes must be ID'd sequentially from 0 to N-1 where N is the number of nodes.");
      }
    }
  }

  /**
   * Ensures that all the node references exist for each of the dependencies in the graph
   * @param graph
   */
  static void ensureNodeReferencesExist(ComputeGraph graph) {
    final int minValidId = 0;
    final int maxValidId = graph.getNodes().size() - 1;
    graph.getNodes().forEach(anyNode -> {
      Set<Integer> dependencies = new Dependencies().getDependencies(anyNode);
      List<Integer> missingDependencies = dependencies.stream()
          .filter(id -> id < minValidId || id > maxValidId)
          .collect(Collectors.toList());
      if (!missingDependencies.isEmpty()) {
        throw new RuntimeException("Encountered missing dependencies " + missingDependencies + " for node " + anyNode
            + ". Graph = " + graph);
      }
    });
  }

  /**
   * Ensure that all the nodes have no concrete keys
   * @param graph
   */
  static void ensureNoConcreteKeys(ComputeGraph graph) {
    graph.getNodes().forEach(node -> {
      if ((node.isExternal() && (node.getExternal().hasConcreteKey()) || (node.isAggregation() && (
          node.getAggregation().hasConcreteKey())) || (node.isDataSource() && (
          node.getDataSource().hasConcreteKey())) || (node.isLookup() && (node.getLookup().hasConcreteKey()))
          || (node.isTransformation() && (node.getTransformation().hasConcreteKey())))) {
        throw new RuntimeException("A concrete key has already been set for the node " + node);
      }
    });
  }

  /**
   * Ensure that none of the external nodes points to a requires feature name
   * @param graph
   */
  static void ensureNoExternalReferencesToSelf(ComputeGraph graph) {
    // make sure graph does not reference external features that are actually defined within itself
    graph.getNodes().stream().filter(AnyNode::isExternal).forEach(node -> {
      String featureName = node.getExternal().getName();
      if (graph.getFeatureNames().containsKey(featureName)) {
        throw new RuntimeException("Graph contains External node " + node + " but also contains feature " + featureName
            + " in its feature name table: " + graph.getFeatureNames() + ". Graph = " + graph);
      }
    });
  }

  /**
   * Ensures that there are no dependency cycles.
   * @param graph
   */
  static void ensureNoDependencyCycles(ComputeGraph graph) {
    Deque<Integer> deque = IntStream.range(0, graph.getNodes().size()).boxed()
        .collect(Collectors.toCollection(ArrayDeque::new));
    List<VisitedState> visitedState = new ArrayList<>(Collections.nCopies(graph.getNodes().size(),
        VisitedState.NOT_VISITED));

    while (!deque.isEmpty()) {
      int nodeId = deque.pop();
      if (visitedState.get(nodeId) == VisitedState.VISITED) {
        continue;
      }
      AnyNode node = graph.getNodes().get(nodeId);
      Set<Integer> dependencies = new Dependencies().getDependencies(node);
      List<Integer> unfinishedDependencies =
          dependencies.stream().filter(i -> visitedState.get(i) != VisitedState.VISITED).collect(Collectors.toList());
      if (!unfinishedDependencies.isEmpty()) {
        if (visitedState.get(nodeId) == VisitedState.IN_PROGRESS) {
          throw new RuntimeException("Dependency cycle involving node " + nodeId);
        }
        deque.push(nodeId); // check me again later, after checking my dependencies.
        unfinishedDependencies.forEach(deque::push); // check my dependencies next.
        visitedState.set(nodeId, VisitedState.IN_PROGRESS);
      } else {
        visitedState.set(nodeId, VisitedState.VISITED);
      }
    }
  }

  private enum VisitedState { NOT_VISITED, IN_PROGRESS, VISITED }

}