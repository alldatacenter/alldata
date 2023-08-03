package com.linkedin.feathr.compute;

import com.linkedin.data.template.IntegerArray;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.linkedin.feathr.compute.ComputeGraphs.*;


/**
 * Resolves a given compute graph (output by the [[FeatureDefinitionsConverter]] class) by removing redundancies and simplifies the
 * graph by taking the join config into account.
 */
public class Resolver {
  private final ComputeGraph _definitionGraph;

  public Resolver(ComputeGraph graph) {
    ensureNoConcreteKeys(graph);
    // Sanity checks for the input graph
    _definitionGraph = ComputeGraphs.validate(graph);
  }

  public static Resolver create(ComputeGraph graph) {
    return new Resolver(graph);
  }

  /**
   * This method takes in a list of requested features and optimizes the graph.
   * @param featureRequestList  Input requested features list
   * @return  An optimized compute graph
   * @throws CloneNotSupportedException
   */
  public ComputeGraph resolveForRequest(List<FeatureRequest> featureRequestList) throws CloneNotSupportedException {
    // preconditions
    // 1. all requested features are defined in the graph
    // 2. no colliding output-feature-names
    // 3. right number of keys for each feature (this would be quite hard to verity! without more info in the model.)

    List<ComputeGraph> graphParts = featureRequestList.stream()
        .map(request -> {
          try {
            return resolveForRequest(request);
          } catch (CloneNotSupportedException e) {
            e.printStackTrace();
          }
          return null;
        })
        .collect(Collectors.toList());

    return ComputeGraphs.removeRedundancies(ComputeGraphs.merge(graphParts));
  }

  public ComputeGraph resolveForRequest(FeatureRequest featureRequest) throws CloneNotSupportedException {
    return resolveForFeature(featureRequest._featureName, featureRequest._keys, featureRequest._alias);
  }

  /**
   * Resolve the unresolved dependencies required to compute a given feature. For example, we need to resolve the join keys
   * the feature. The join keys exist as a separate node inside the graph (a context datasource node). Another example is to
   * resolve the dependencies of the input feature.
   * @param featureName Name of the feature
   * @param keys  Keys of the observation datasource
   * @param alias the feature can be aliased with another name (optional field)
   * @return  A compute graph with the dependency resolved for this particular feature
   * @throws CloneNotSupportedException
   */
  public ComputeGraph resolveForFeature(String featureName, List<String> keys, String alias)
      throws CloneNotSupportedException {
    if (!_definitionGraph.getFeatureNames().containsKey(featureName)) {
      throw new IllegalArgumentException("Feature graph does not contain requested feature " + featureName);
    }
    if (alias == null) {
      alias = featureName;
    }
    ComputeGraphBuilder builder = new ComputeGraphBuilder();

    ConcreteKey concreteKey = new ConcreteKey().setKey(new IntegerArray());
    keys.forEach(key -> {
      DataSource source = builder.addNewDataSource()
          .setSourceType(DataSourceType.CONTEXT)
          .setExternalSourceRef(key);
      concreteKey.getKey().add(source.getId());
    });

    ConcreteKeyAttacher concreteKeyAttacher = new ConcreteKeyAttacher(builder);
    int newNodeId = concreteKeyAttacher.addNodeAndAttachKey(_definitionGraph.getFeatureNames().get(featureName), concreteKey);
    builder.addFeatureName(alias, newNodeId);

    return builder.build();
  }

  /**
   * Class to attach the concrete key to all the dependencies
   */
  private class ConcreteKeyAttacher {
    private final ComputeGraphBuilder _builder;

    public ConcreteKeyAttacher(ComputeGraphBuilder builder) {
      _builder = builder;
    }

    /**
     * Set the given concrete key to the given node. Also, attach the same key to all it's dependendent nodes.
     * @param nodeId node id in the original (definition) feature graph
     * @param key the "concrete key" to attach. references should be into the new (resolved) graph.
     * @return the node id of the newly created counterpart node in the new (resolved) graph
     */
    int addNodeAndAttachKey(int nodeId, ConcreteKey key) {
      AnyNode node = _definitionGraph.getNodes().get(nodeId);
      if (PegasusUtils.hasConcreteKey(node)) {
        throw new RuntimeException("Assertion failed. Did not expect to encounter key-annotated node");
      }
      AnyNode newNode = PegasusUtils.copy(node);
      PegasusUtils.setConcreteKey(newNode, key);
      attachKeyToDependencies(newNode, key);
      return _builder.addNode(newNode);
    }

    private void attachKeyToDependencies(AnyNode node, ConcreteKey key) {
      if (node.isAggregation()) {
        attachKeyToDependencies(node.getAggregation(), key);
      } else if (node.isDataSource()) {
        attachKeyToDependencies(node.getDataSource(), key);
      } else if (node.isLookup()) {
        attachKeyToDependencies(node.getLookup(), key);
      } else if (node.isTransformation()) {
        attachKeyToDependencies(node.getTransformation(), key);
      } else if (node.isExternal()) {
        attachKeyToDependencies(node.getExternal(), key);
      } else {
        throw new RuntimeException("Unhandled kind of AnyNode: " + node);
      }
    }

    private void attachKeyToDependencies(Aggregation node, ConcreteKey key) {
      NodeReference childNodeReference = node.getInput();

      // If the node is a datasource node, we assume it is the terminal node (ie - no dependencies).
      if (_definitionGraph.getNodes().get(childNodeReference.getId()).isDataSource()) {
        ArrayList keyReferenceArray = new ArrayList<KeyReference>();
        for (int i = 0; i < key.getKey().size(); i++) {
          keyReferenceArray.add(new KeyReference().setPosition(i));
        }

        KeyReferenceArray keyReferenceArray1 = new KeyReferenceArray(keyReferenceArray);
        childNodeReference.setKeyReference(keyReferenceArray1);
      }
      ConcreteKey childKey = transformConcreteKey(key, childNodeReference.getKeyReference());
      int childDefinitionNodeId = childNodeReference.getId();
      int resolvedChildNodeId = addNodeAndAttachKey(childDefinitionNodeId, childKey);
      childNodeReference.setId(resolvedChildNodeId);
    }

    private void attachKeyToDependencies(DataSource node, ConcreteKey key) {
      if (node.hasSourceType() && node.getSourceType() == DataSourceType.UPDATE) {
        node.setConcreteKey(key);
      }
    }

    /**
     * If the node is a lookup node, we will need to attach the appropriate concrete key to the input nodes
     * @param node
     * @param inputConcreteKey
     */
    private void attachKeyToDependencies(Lookup node, ConcreteKey inputConcreteKey) {
      ConcreteKey concreteLookupKey = new ConcreteKey().setKey(new IntegerArray());
      IntegerArray concreteKeyClone = new IntegerArray();
      concreteKeyClone.addAll(inputConcreteKey.getKey());
      ConcreteKey inputConcreteKeyClone = new ConcreteKey().setKey(concreteKeyClone);
      node.getLookupKey().forEach(lookupKeyPart -> {
        if (lookupKeyPart.isKeyReference()) { // We do not support this yet.
          int relativeKey = lookupKeyPart.getKeyReference().getPosition();
          concreteLookupKey.getKey().add(inputConcreteKeyClone.getKey().get(relativeKey));
        } else if (lookupKeyPart.isNodeReference()) {
          /**
           * seq_join_feature: {
           *   key: {x, y, viewerId}
           *   base: {key: x, feature: baseFeature}
           *   expansion: {key: [y, viewerId] feature: expansionFeature}
           * }
           *
           * We need to add the concrete key of 0 (x) to the base feature node (lookup key) and concrete key of 1, 2 (y, viewerId)
           * to the expansion feature node (lookup node).
           */
          NodeReference childNodeReference = lookupKeyPart.getNodeReference();
          ConcreteKey childConcreteKey = transformConcreteKey(inputConcreteKey, childNodeReference.getKeyReference());
          int childDefinitionNodeId = childNodeReference.getId();
          int resolvedChildNodeId = 0;
          resolvedChildNodeId = addNodeAndAttachKey(childDefinitionNodeId, childConcreteKey);

          // Remove all the keys which are not part of the base key features, ie - y in this case.
          IntegerArray keysToBeRemoved = childConcreteKey.getKey();
          inputConcreteKey.getKey().removeAll(keysToBeRemoved);
          childNodeReference.setId(resolvedChildNodeId);

          // Add the compute base node to the expansion keyset. Now, concreteLookupKey will have the right values.
          concreteLookupKey.getKey().add(resolvedChildNodeId);
        } else {
          throw new RuntimeException("Unhandled kind of LookupKey: " + lookupKeyPart);
        }
      });

      // The right concrete node has been calculated for the expansion feature now. We can just set it.
      int lookupDefinitionNodeId = node.getLookupNode();
      int resolvedLookupNodeId = addNodeAndAttachKey(lookupDefinitionNodeId, new ConcreteKey().setKey(concreteLookupKey.getKey()));
      inputConcreteKey.setKey(concreteKeyClone);
      node.setLookupNode(resolvedLookupNodeId);
    }

    /**
     * Attach the concrete key to all the dependencies of the transformation node.
     * @param node
     * @param key
     */
    private void attachKeyToDependencies(Transformation node, ConcreteKey key) {
      /**
       * A transformation node can have n dependencies like:-
       * derivedFeature: {
       *  key: {a, b, c}
       *  input1: {key: a, feature: AA}
       *  input2: {key: b, feature: BB}
       *  input3: {key: c, feature: CC}
       *  defintion: input1 + input2 + input3
       * }
       *
       * In this case, we need to attach concrete key 0 (a) to the input1 node, key 1 (b) to the input2 node andd key 3 (c) to the input3 node.
       */
      node.getInputs().forEach(childNodeReference -> {
        if (_definitionGraph.getNodes().get(childNodeReference.getId()).isDataSource()) {
          ArrayList keyReferenceArray = new ArrayList<KeyReference>();
          for (int i = 0; i < key.getKey().size(); i++) {
            keyReferenceArray.add(new KeyReference().setPosition(i));
          }
          KeyReferenceArray keyReferenceArray1 = new KeyReferenceArray(keyReferenceArray);
          childNodeReference.setKeyReference(keyReferenceArray1);
        }

        ConcreteKey childKey = transformConcreteKey(key, childNodeReference.getKeyReference());
        int childDefinitionNodeId = childNodeReference.getId();
        int resolvedChildNodeId = 0;
        resolvedChildNodeId = addNodeAndAttachKey(childDefinitionNodeId, childKey);

        childNodeReference.setId(resolvedChildNodeId);
      });
    }

    private void attachKeyToDependencies(External node, ConcreteKey key) {
      throw new RuntimeException("Internal error: Can't link key to external feature node not defined in this graph.");
    }
  }

  /**
   * Representation class for a feature request.
   */
  public static class FeatureRequest {
    private final String _featureName;
    private final List<String> _keys;
    private final Duration _timeDelay;
    private final String _alias;

    public FeatureRequest(String featureName, List<String> keys, Duration timeDelay, String alias) {
      _featureName = featureName;
      _keys = keys;
      _timeDelay = timeDelay;
      _alias = alias;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof FeatureRequest)) {
        return false;
      }
      FeatureRequest that = (FeatureRequest) o;
      return Objects.equals(_featureName, that._featureName) && Objects.equals(_keys, that._keys) && Objects.equals(
          _alias, that._alias);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_featureName, _keys, _alias);
    }
  }

  /**
   * In this method, we transform the original concrete key to the necessary concrete key by using a keyReference array.
   * For example, if the original key is [1, 2, 3] and the keyReferenceArray is [0,1]. Then, the resultant concrete key would be
   * [1, 2] (which is the 0th and 1st index of the original key.
   * @param original the original (or parent) key
   * @param keyReference the relative key, whose parts refer to relative positions in the parent key
   * @return the child key obtained by applying the keyReference to the parent key
   */
  private static ConcreteKey transformConcreteKey(ConcreteKey original, KeyReferenceArray keyReference) {
    return new ConcreteKey().setKey(
        keyReference.stream()
            .map(KeyReference::getPosition)
            .map(original.getKey()::get)
            .collect(Collectors.toCollection(IntegerArray::new)));
  }
}