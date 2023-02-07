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

import java.util.HashMap;
import java.util.Map;

 class AdjacencyListBuilder<V extends GraphValue<V>> implements GraphVisitor<V> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdjacencyListBuilder.class);

  private Map<V, AdjacencyList<V>.Node> ops = new HashMap<V, AdjacencyList<V>.Node>();
  private final AdjacencyList<V> parent;

  public AdjacencyListBuilder(AdjacencyList<V> parent) {
    this.parent = parent;
  }

  protected boolean requireDirected() {
    return true;
  }

  public boolean enter(V o) {
    visit(o);
    return true;
  }

  @Override
  public void leave(V o) {
  }

  @Override
  public boolean visit(V o) {
    if (o == null) {
      throw new IllegalArgumentException("Null operator.");
    }

    if (!ops.containsKey(o)) {
      ops.put(o, parent.getNewNode(o));
      return true;
    }

    return true;
  }

  public AdjacencyList<V> getAdjacencyList() {
//    logger.debug("Values; {}", ops.values().toArray());
    AdjacencyList<V> a = new AdjacencyList<V>();

    for (AdjacencyList<V>.Node from : ops.values()) {
      for (V t : from.getNodeValue()) {
        AdjacencyList<V>.Node to = ops.get(t);
        a.addEdge(from, to, 0);
      }

    }
    a.fix(true);
    return a;
  }

}
