package com.linkedin.feathr.compute;

import com.linkedin.data.template.RecordTemplate;


/**
 * Helper functions for dealing with the generated Pegasus APIs for the Compute Model. For example, Pegasus doesn't
 * really support inheritance, so we have some helper functions here to give polymorphism-like behavior.
 */
public class PegasusUtils {
  private PegasusUtils() {
  }

  static AnyNode copy(AnyNode node) {
    try {
      return node.copy();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e); // this should never happen, based on Pegasus's guarantees, AFAIK
    }
  }

  /**
   * Makes an AnyNode, for some given kind of specific node RecordTemplate (any of Aggregation, DataSource, Lookup,
   * Transformation, or External). Throws an exception if any other kind of record is passed in.
   * @param node the specific node
   * @return the node wrapped as an AnyNode
   */
  static AnyNode wrapAnyNode(RecordTemplate node) {
    if (node instanceof Aggregation) {
      return AnyNode.create((Aggregation) node);
    } else if (node instanceof DataSource) {
      return AnyNode.create((DataSource) node);
    } else if (node instanceof Lookup) {
      return AnyNode.create((Lookup) node);
    } else if (node instanceof Transformation) {
      return AnyNode.create((Transformation) node);
    } else if (node instanceof External) {
      return AnyNode.create((External) node);
    } else {
      throw new RuntimeException("Unhandled kind of node: " + node);
    }
  }

  /**
   * Unwraps an AnyNode into its specific node type (Aggregation, DataSource, Lookup, Transformation, or External).
   * @param anyNode the AnyNode
   * @return the specific node that had been wrapped inside
   */
  static RecordTemplate unwrapAnyNode(AnyNode anyNode) {
    if (anyNode.isAggregation()) {
      return anyNode.getAggregation();
    } else if (anyNode.isDataSource()) {
      return anyNode.getDataSource();
    } else if (anyNode.isLookup()) {
      return anyNode.getLookup();
    } else if (anyNode.isTransformation()) {
      return anyNode.getTransformation();
    } else if (anyNode.isExternal()) {
      return anyNode.getExternal();
    } else {
      throw new RuntimeException("Unhandled kind of AnyNode: " + anyNode);
    }
  }

  /**
   * Gets the id for the node wrapped inside the provided AnyNode
   * @param anyNode any node
   * @return the id
   */
  static int getNodeId(AnyNode anyNode) {
    return abstractNode(anyNode).getId();
  }

  public static int getNodeId(RecordTemplate node) {
    return abstractNode(node).getId();
  }

  /**
   * Sets the id for the node wrapped inside the provided AnyNode
   * @param node the node
   * @param id the id to set
   */
  static void setNodeId(AnyNode node, int id) {
    abstractNode(node).setId(id);
  }

  static boolean hasConcreteKey(AnyNode anyNode) {
    return abstractNode(anyNode).hasConcreteKey();
  }

  static ConcreteKey getConcreteKey(AnyNode anyNode) {
    return abstractNode(anyNode).getConcreteKey();
  }

  static void setConcreteKey(AnyNode anyNode, ConcreteKey concreteKey) {
    abstractNode(anyNode).setConcreteKey(concreteKey);
  }

  private static AbstractNode abstractNode(AnyNode anyNode) {
    return new AbstractNode(unwrapAnyNode(anyNode).data());
  }

  private static AbstractNode abstractNode(RecordTemplate anyNode) {
    return new AbstractNode(anyNode.data());
  }
}