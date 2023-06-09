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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphAlgos {
  static final Logger logger = LoggerFactory.getLogger(GraphAlgos.class);

  public static class TopoSorter<V extends GraphValue<V>> {
    final List<AdjacencyList<V>.Node> sorted = new LinkedList<AdjacencyList<V>.Node>();
    final AdjacencyList<V> rGraph;

    private TopoSorter(AdjacencyList<V> graph, boolean reverse) {
      graph.clearVisited();

      if (reverse) {
        this.rGraph = graph.getReversedList();
      } else {
        this.rGraph = graph;
      }
      Collection<AdjacencyList<V>.Node> sourceNodes = rGraph.getInternalRootNodes();

      for (AdjacencyList<V>.Node n : sourceNodes) {
        visit(n);
      }
    }

    private void visit(AdjacencyList<V>.Node n) {
      if (n.visited) {
        return;
      }

      n.visited = true;
      List<Edge<AdjacencyList<V>.Node>> edges = rGraph.getAdjacent(n);
      if (edges != null) {
        for (Edge<AdjacencyList<V>.Node> e : edges) {
          visit(e.to);
        }
      }

      sorted.add(n);

    }

    /**
     * Execute a depth-first sort on the reversed DAG.
     *
     * @param graph the adjacency list for the DAG.
     * @param reverse true if reversed, otherwise false
     *
     * @return
     */
    static <V extends GraphValue<V>> List<AdjacencyList<V>.Node> sortInternal(AdjacencyList<V> graph, boolean reverse) {
      TopoSorter<V> ts = new TopoSorter<V>(graph, reverse);
      return ts.sorted;
    }

    public static <V extends GraphValue<V>> List<V> sort(Graph<V, ?, ?> graph) {
      AdjacencyList<V> l = graph.getAdjList();
      return l.convert(sortInternal(l, true));
    }

      public static <V extends GraphValue<V>> List<V> sortLogical(Graph<V, ?, ?> graph) {
          AdjacencyList<V> l = graph.getAdjList();
          return l.convert(sortInternal(l, false));
      }
  }

  static <V extends GraphValue<V>> List<List<AdjacencyList<V>.Node>> checkDirected(AdjacencyList<V> graph) {
    Tarjan<V> t = new Tarjan<V>();
    List<List<AdjacencyList<V>.Node>> subgraphs = t.executeTarjan(graph);
    for (Iterator<List<AdjacencyList<V>.Node>> i = subgraphs.iterator(); i.hasNext();) {
      List<AdjacencyList<V>.Node> l = i.next();
      if (l.size() == 1) {
        i.remove();
      }
    }
    return subgraphs;
  }

  public static <V extends GraphValue<V>> List<List<AdjacencyList<V>.Node>> checkDirected(Graph<V, ?, ?> graph) {
    return checkDirected(graph.getAdjList());
  }

  public static class Tarjan<V extends GraphValue<V>> {

    private int index = 0;
    private List<AdjacencyList<V>.Node> stack = new LinkedList<AdjacencyList<V>.Node>();
    private List<List<AdjacencyList<V>.Node>> SCC = new LinkedList<List<AdjacencyList<V>.Node>>();

    public List<List<AdjacencyList<V>.Node>> executeTarjan(AdjacencyList<V> graph) {
      SCC.clear();
      index = 0;
      stack.clear();
      if (graph != null) {
        List<AdjacencyList<V>.Node> nodeList = new LinkedList<AdjacencyList<V>.Node>(graph.getNodeSet());
        for (AdjacencyList<V>.Node node : nodeList) {
          if (node.index == -1) {
            tarjan(node, graph);
          }
        }
      }
      return SCC;
    }

    private List<List<AdjacencyList<V>.Node>> tarjan(AdjacencyList<V>.Node v, AdjacencyList<V> list) {
      v.index = index;
      v.lowlink = index;
      index++;
      stack.add(0, v);
      List<Edge<AdjacencyList<V>.Node>> l = list.getAdjacent(v);
      if (l != null) {
        for (Edge<AdjacencyList<V>.Node> e : l) {
          AdjacencyList<V>.Node n = e.to;
          if (n.index == -1) {
            tarjan(n, list);
            v.lowlink = Math.min(v.lowlink, n.lowlink);
          } else if (stack.contains(n)) {
            v.lowlink = Math.min(v.lowlink, n.index);
          }
        }
      }
      if (v.lowlink == v.index) {
        AdjacencyList<V>.Node n;
        List<AdjacencyList<V>.Node> component = new LinkedList<AdjacencyList<V>.Node>();
        do {
          n = stack.remove(0);
          component.add(n);
        } while (n != v);
        SCC.add(component);
      }
      return SCC;
    }
  }

}
