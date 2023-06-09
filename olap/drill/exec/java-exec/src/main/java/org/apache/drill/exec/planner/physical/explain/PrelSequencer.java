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
package org.apache.drill.exec.planner.physical.explain;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.drill.exec.planner.physical.ExchangePrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.visitor.BasePrelVisitor;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.sql.SqlExplainLevel;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class PrelSequencer extends BasePrelVisitor<Void, PrelSequencer.Frag, RuntimeException> {

  private List<Frag> frags = Lists.newLinkedList();

  public static String printWithIds(final Prel rel, SqlExplainLevel explainlevel) {
      if (rel == null) {
        return null;
      }
      final StringWriter sw = new StringWriter();
      final RelWriter planWriter = new NumberingRelWriter(getIdMap(rel), new PrintWriter(sw), explainlevel);
      rel.explain(planWriter);
      return sw.toString();

  }

  public static Map<Prel, OpId> getIdMap(Prel rel) {
    PrelSequencer s = new PrelSequencer();
    return s.go(rel);
  }

  static class Frag implements Iterable<Frag> {
    Prel root;
    int majorFragmentId;
    final List<Frag> children = Lists.newArrayList();
    public Frag(Prel root) {
      super();
      this.root = root;
    }

    @Override
    public Iterator<Frag> iterator() {
      return children.iterator();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((children == null) ? 0 : children.hashCode());
      result = prime * result + majorFragmentId;
      result = prime * result + ((root == null) ? 0 : root.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Frag other = (Frag) obj;
      if (children == null) {
        if (other.children != null) {
          return false;
        }
      } else if (!children.equals(other.children)) {
        return false;
      }
      if (majorFragmentId != other.majorFragmentId) {
        return false;
      }
      if (root == null) {
        if (other.root != null) {
          return false;
        }
      } else if (!root.equals(other.root)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      final int maxLen = 10;
      return "Frag [root=" + root + ", majorFragmentId=" + majorFragmentId + ", children="
          + (children != null ? children.subList(0, Math.min(children.size(), maxLen)) : null) + "]";
    }

  }

  public static class OpId{
    int fragmentId;
    int opId;
    public OpId(int fragmentId, int opId) {
      super();
      this.fragmentId = fragmentId;
      this.opId = opId;
    }

    public int getFragmentId() {
      return fragmentId;
    }


    public int getOpId() {
      return opId;
    }

    public int getAsSingleInt() {
      return (fragmentId << 16) + opId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + fragmentId;
      result = prime * result + opId;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      OpId other = (OpId) obj;
      if (fragmentId != other.fragmentId) {
        return false;
      }
      if (opId != other.opId) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return fragmentId + ":*:" + opId;
    }

  }

  public Map<Prel, OpId> go(Prel root) {

    // get fragments.
    Frag rootFrag = new Frag(root);
    frags.add(rootFrag);
    root.accept(this, rootFrag);

    // do depth first traversal of fragments to assign major fragment ids.
    Queue<Frag> q = Lists.newLinkedList();

    q.add(rootFrag);
    int majorFragmentId = 0;
    while (!q.isEmpty()) {
      Frag frag = q.remove();

      frag.majorFragmentId = majorFragmentId++;

      for (Frag child : frag) {
        q.add(child);
      }
    }

    // for each fragment, do a dfs of operators to assign operator ids.
    Map<Prel, OpId> ids = Maps.newIdentityHashMap();

    ids.put(rootFrag.root, new OpId(0, 0));
    for (Frag f : frags) {
      int id = 1;
      Queue<Prel> ops = Lists.newLinkedList();
      ops.add(f.root);
      while (!ops.isEmpty()) {
        Prel p = ops.remove();
        boolean isExchange = p instanceof ExchangePrel;

        if (p != f.root) {      // we account for exchanges as receviers to guarantee unique identifiers.
          ids.put(p, new OpId(f.majorFragmentId, id++) );
        }

        if (!isExchange || p == f.root) {
          List<Prel> children = Lists.reverse(Lists.newArrayList(p.iterator()));
          for (Prel child : children) {
            ops.add(child);
          }
        }
      }
    }


    return ids;
  }

  @Override
  public Void visitExchange(ExchangePrel prel, Frag value) throws RuntimeException {
    Frag newFrag = new Frag(prel);
    frags.add(newFrag);
    value.children.add(newFrag);
    for (Prel child : prel) {
      child.accept(this, newFrag);
    }

    return null;
  }

  @Override
  public Void visitPrel(Prel prel, Frag value) throws RuntimeException {
    for (Prel children : prel) {
      children.accept(this, value);
    }
    return null;
  }

}
