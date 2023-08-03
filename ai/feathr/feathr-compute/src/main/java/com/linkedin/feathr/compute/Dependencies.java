package com.linkedin.feathr.compute;

import com.google.common.collect.Sets;
import com.linkedin.data.template.IntegerArray;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Utility class for working with nodes' dependencies.
 *
 * If AnyNode had been a interface instead of a Pegasus record, .getDependencies() and .remapDependencies() would
 * have been interface methods for it. But since Pegasus records don't have custom methods (and don't have inheritance),
 * use this class to deal with nodes' dependencies instead.
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@InternalApi
public class Dependencies {
  /**
   * Get the dependencies for any kind of node. Note that a dependency is a reference to another node.
   *
   * @param anyNode the node
   * @return the set of ids of the nodes the input node depends on
   */
  public Set<Integer> getDependencies(AnyNode anyNode) {
    return Sets.union(getKeyDependencies(anyNode), getNodeDependencies(anyNode));
  }

  private Set<Integer> getKeyDependencies(AnyNode anyNode) {
    if (PegasusUtils.hasConcreteKey(anyNode)) {
      return new HashSet<>(PegasusUtils.getConcreteKey(anyNode).getKey());
    } else {
      return Collections.emptySet();
    }
  }

  private static Set<Integer> getNodeDependencies(AnyNode anyNode) {
    if (anyNode.isAggregation()) {
      return getNodeDependencies(anyNode.getAggregation());
    } else if (anyNode.isDataSource()) {
      return getNodeDependencies(anyNode.getDataSource());
    } else if (anyNode.isLookup()) {
      return getNodeDependencies(anyNode.getLookup());
    } else if (anyNode.isTransformation()) {
      return getNodeDependencies(anyNode.getTransformation());
    } else if (anyNode.isExternal()) {
      return getNodeDependencies(anyNode.getExternal());
    } else {
      throw new RuntimeException("Unhandled kind of AnyNode: " + anyNode);
    }
  }

  private static Set<Integer> getNodeDependencies(Aggregation node) {
    return Collections.singleton(node.getInput().getId());
  }

  private static Set<Integer> getNodeDependencies(Transformation node) {
    return node.getInputs().stream().map(NodeReference::getId).collect(Collectors.toSet());
  }

  private static Set<Integer> getNodeDependencies(Lookup node) {
    Set<Integer> dependencies = new HashSet<>();
    node.getLookupKey().stream()
        // Only NodeReferences matter for determining dependencies on other nodes.
        .filter(Lookup.LookupKey::isNodeReference)
        .map(Lookup.LookupKey::getNodeReference)
        .map(NodeReference::getId)
        .forEach(dependencies::add);
    dependencies.add(node.getLookupNode());
    return dependencies;
  }

  private static Set<Integer> getNodeDependencies(DataSource node) {
    return Collections.emptySet();
  }

  private static Set<Integer> getNodeDependencies(External node) {
    return Collections.emptySet();
  }

  /**
   * Modify a node's dependencies' ids based on a given id-mapping function.
   * This can be useful for modifying a graph, merging graphs together, removing duplicate parts of graphs, etc.
   *
   * @param anyNode the nodes whose dependencies (if it has any) should be modified according to the mapping function;
   *                must not be null.
   * @param idMapping a mapping function that converts from "what the nodes' dependencies currently look like" to "what
   *                  they should look like after the change." For any node id that should NOT change, the the function
   *                  must return the input if that node id is passed in. For any node ids that the caller expects will
   *                  never be encountered, it would be ok for the idMapping function to throw an exception if that node
   *                  id is passed in. The idMapping function can assume its input will never be null, and should NOT
   *                  return null.
   */
  static void remapDependencies(AnyNode anyNode, Function<Integer, Integer> idMapping) {
    remapKeyDependencies(anyNode, idMapping);
    remapNodeDependencies(anyNode, idMapping);
  }

  private static void remapKeyDependencies(AnyNode anyNode, Function<Integer, Integer> idMapping) {
    if (PegasusUtils.hasConcreteKey(anyNode)) {
      ConcreteKey concreteKey = PegasusUtils.getConcreteKey(anyNode);
      IntegerArray newKeyDependencies = concreteKey.getKey().stream()
          .map(idMapping)
          .collect(Collectors.toCollection(IntegerArray::new));
      concreteKey.setKey(newKeyDependencies);
    }
  }

  private static void remapNodeDependencies(AnyNode anyNode, Function<Integer, Integer> idMapping) {
    if (anyNode.isAggregation()) {
      remapNodeDependencies(anyNode.getAggregation(), idMapping);
    } else if (anyNode.isDataSource()) {
      // data source has no dependencies
    } else if (anyNode.isLookup()) {
      remapNodeDependencies(anyNode.getLookup(), idMapping);
    } else if (anyNode.isTransformation()) {
      remapNodeDependencies(anyNode.getTransformation(), idMapping);
    } else if (anyNode.isExternal()) {
      // no dependencies
    } else {
      throw new RuntimeException("Unhandled kind of AnyNode: " + anyNode);
    }
  }

  private static void remapNodeDependencies(Aggregation node, Function<Integer, Integer> idMapping) {
    int oldInputNodeId = node.getInput().getId();
    int newNodeId = idMapping.apply(oldInputNodeId); // An NPE on this line would mean that the mapping is not complete,
    // which should be impossible and would indicate a bug in the graph
    // processing code.
    node.getInput().setId(newNodeId);
  }

  private static void remapNodeDependencies(Transformation node, Function<Integer, Integer> idMapping) {
    node.getInputs().forEach(input -> {
      int oldInputNodeId = input.getId();
      int newNodeId = idMapping.apply(oldInputNodeId);
      input.setId(newNodeId);
    });
  }

  private static void remapNodeDependencies(Lookup node, Function<Integer, Integer> idMapping) {
    int oldLookupNodeId = node.getLookupNode();
    int newLookupNodeId = idMapping.apply(oldLookupNodeId);
    node.setLookupNode(newLookupNodeId);

    node.getLookupKey().forEach(lookupKey -> {
      if (lookupKey.isNodeReference()) {
        NodeReference nodeReference = lookupKey.getNodeReference();
        int oldReferenceNodeId = nodeReference.getId();
        int newReferenceNodeId = idMapping.apply(oldReferenceNodeId);
        nodeReference.setId(newReferenceNodeId);
      }
    });
  }
}