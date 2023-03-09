package com.linkedin.feathr.compute;

import com.linkedin.data.template.IntegerMap;
import com.linkedin.data.template.LongMap;
import com.linkedin.data.template.RecordTemplate;


/**
 * Builder class for Compute Graph
 */
@InternalApi
public class ComputeGraphBuilder {
  IntegerMap _featureNameMap = new IntegerMap();
  LongMap _dataSourceMap = new LongMap();
  AnyNodeArray _nodes = new AnyNodeArray();

  /**
   * MODIFIES THE INPUT NODE by assigning it a new ID for this graph being built, and adds it to the graph.
   * NOTE that this function doesn't/can't update the node's edges/dependencies so that they correctly point to nodes
   * in the new graph! The caller is responsible for doing this.
   *
   * @param node the node to be modified, assigned a new ID, and inserted into the graph
   * @return the node's new ID in this graph being built
   */
  public int addNode(AnyNode node) {
    int newId = _nodes.size();
    PegasusUtils.setNodeId(node, newId);
    _nodes.add(node);
    return newId;
  }

  public DataSource addNewDataSource() {
    return addNodeHelper(new DataSource());
  }

  public Transformation addNewTransformation() {
    return addNodeHelper(new Transformation());
  }

  public Aggregation addNewAggregation() {
    return addNodeHelper(new Aggregation());
  }

  public Lookup addNewLookup() {
    return addNodeHelper(new Lookup());
  }

  public External addNewExternal() {
    return addNodeHelper(new External());
  }

  public <T extends RecordTemplate> T addNodeHelper(T node) {
    addNode(PegasusUtils.wrapAnyNode(node));
    return node;
  }

  /**
   * Adds a feature name mapping to this graph being built.
   * @param featureName the feature name
   * @param nodeId node Id
   */
  public void addFeatureName(String featureName, Integer nodeId) {
    if (nodeId >= _nodes.size()) {
      throw new IllegalArgumentException("Node id " + nodeId + " is not defined in the graph being built: " + this);
    }
    if (_featureNameMap.containsKey(featureName)) {
      throw new IllegalArgumentException("Feature " + featureName + " is already defined in the graph being built: "
          + this);
    }
    _featureNameMap.put(featureName, nodeId);
  }

  public int peekNextNodeId() {
    return _nodes.size();
  }

  public ComputeGraph build() {
    return build(new ComputeGraph());
  }

  public ComputeGraph build(ComputeGraph reuse) {
    return build(reuse, true);
  }

  /**
   * Allows to build the graph without validating it. (Internal use case: Build a merged graph first, and remove
   * internally-pointing External-feature nodes later.) Be careful.
   */
  ComputeGraph build(ComputeGraph reuse, boolean validate) {
    reuse.setFeatureNames(_featureNameMap).setNodes(_nodes);
    if (validate) {
      ComputeGraphs.validate(reuse);
    }
    return reuse;
  }

  @Override
  public String toString() {
    return "ComputeGraphBuilder{" + "_featureNameMap=" + _featureNameMap + ", _nodes=" + _nodes + '}';
  }
}