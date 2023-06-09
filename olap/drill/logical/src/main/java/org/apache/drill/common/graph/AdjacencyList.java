/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common.graph;

import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.ListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


class AdjacencyList<V extends GraphValue<V>> {
  private Set<Node> allNodes = new HashSet<Node>();
  private ListMultimap<Node, Edge<Node>> adjacencies = ArrayListMultimap.create();

  void addEdge(Node source, Node target, int weight) {
    adjacencies.put(source, new Edge<Node>(source, target, weight));
    allNodes.add(source);
    allNodes.add(target);
  }

  void clearVisited() {
    for (Edge<Node> e : adjacencies.values()) {
      e.from.visited = false;
      e.to.visited = false;
    }
  }

  Node getNewNode(V value) {
    return new Node(value);
  }

  public List<Edge<Node>> getAdjacent(AdjacencyList<V>.Node source) {
    return adjacencies.get(source);
  }

  public void printEdges() {
    for (Edge<Node> e : adjacencies.values()) {
      System.out.println(e.from.index + " -> " + e.to.index);
    }
  }

  public AdjacencyList<V> getReversedList() {
    AdjacencyList<V> newlist = new AdjacencyList<V>();
    for (Edge<Node> e : adjacencies.values()) {
      newlist.addEdge(e.to, e.from, e.weight);
    }
    return newlist;
  }

  public Set<Node> getNodeSet() {
    return adjacencies.keySet();
  }


  Collection<Node> getInternalLeafNodes() {
    // we have to use the allNodes list as otherwise destination only nodes won't be found.
    List<Node> nodes = new LinkedList<Node>(allNodes);

    for (Iterator<Node> i = nodes.iterator(); i.hasNext();) {
      final Node n = i.next();

      // remove any nodes that have one or more outbound edges.
      List<Edge<Node>> adjList = this.getAdjacent(n);
      if (adjList != null && !adjList.isEmpty()) {
        i.remove();
      }

    }
    return nodes;
  }

  /**
   * Get a list of nodes that have no outbound edges.
   *
   * @return
   */
  public Collection<V> getLeafNodes() {
    return convert(getInternalLeafNodes());
  }


  Collection<Node> getInternalRootNodes() {
    Set<Node> nodes = new HashSet<Node>(getNodeSet());
    for (Edge<Node> e : adjacencies.values()) {
      nodes.remove(e.to);
    }
    return nodes;
  }

  /**
   * Get a list of all nodes that have no incoming edges.
   *
   * @return
   */
  public List<V> getRootNodes() {
    return convert(getInternalRootNodes());
  }

  public Collection<Edge<Node>> getAllEdges() {
    return adjacencies.values();
  }

  public void fix(boolean requireDirected) {
    adjacencies = Multimaps.unmodifiableListMultimap(adjacencies);
    allNodes = Collections.unmodifiableSet(allNodes);

    if (requireDirected) {
      List<List<Node>> cyclicReferences = GraphAlgos.checkDirected(this);
      if (cyclicReferences.size() > 0) {
        throw new IllegalArgumentException(
            "A logical plan must be a valid DAG.  You have cyclic references in your graph.  " + cyclicReferences);
      }
    }
  }

  List<V> convert(Collection<Node> nodes) {
    List<V> out = new ArrayList<V>(nodes.size());
    for (Node o : nodes) {
      out.add(o.getNodeValue());
    }
    return out;
  }

  class Node implements Comparable<Node> {
    final V nodeValue;
    boolean visited = false; // used for Kosaraju's algorithm and Edmonds's
                             // algorithm
    int lowlink = -1; // used for Tarjan's algorithm
    int index = -1; // used for Tarjan's algorithm

    public Node(final V operator) {
      if (operator == null) {
        throw new IllegalArgumentException("Operator node was null.");
      }
      this.nodeValue = operator;
    }

    public int compareTo(final Node argNode) {
      // just do an identity compare since elsewhere you should ensure that only one node exists for each nodeValue.
      return argNode == this ? 0 : -1;
    }

    @Override
    public int hashCode() {
      return nodeValue.hashCode();
    }

    @Override
    public boolean equals(Object that) {
      if (this == that) {
        return true;
      } else if (that == null || getClass() != that.getClass()) {
        return false;
      }
      return Objects.equals(nodeValue, ((Node) that).getNodeValue());
    }

    public V getNodeValue() {
      return nodeValue;
    }

    @Override
    public String toString() {
      return "Node [val=" + nodeValue + "]";
    }

  }

  public static <V extends GraphValue<V>> AdjacencyList<V> newInstance(Collection<V> nodes) {
    AdjacencyList<V> list = new AdjacencyList<V>();
    AdjacencyListBuilder<V> builder = new AdjacencyListBuilder<V>(list);
    for (V v : nodes) {
      v.accept(builder);
    }
    return builder.getAdjacencyList();
  }

}
