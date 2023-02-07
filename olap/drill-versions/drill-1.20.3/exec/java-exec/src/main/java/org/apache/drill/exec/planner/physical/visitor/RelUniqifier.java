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
package org.apache.drill.exec.planner.physical.visitor;

import java.util.List;
import java.util.Set;

import org.apache.drill.exec.planner.physical.Prel;
import org.apache.calcite.rel.RelNode;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

public class RelUniqifier extends BasePrelVisitor<Prel, Set<Prel>, RuntimeException>{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RelUniqifier.class);

  private static final RelUniqifier INSTANCE = new RelUniqifier();

  public static Prel uniqifyGraph(Prel p) {
    Set<Prel> data = Sets.newIdentityHashSet();
    return p.accept(INSTANCE, data);
  }

  @Override
  public Prel visitPrel(Prel prel, Set<Prel> data) throws RuntimeException {
    List<RelNode> children = Lists.newArrayList();
    boolean childrenChanged = false;
    for (Prel child : prel) {
      Prel newChild = visitPrel(child, data);
      if (newChild != child) {
        childrenChanged = true;
      }
      children.add(newChild);
    }

    if (data.contains(prel) || childrenChanged) {
      return (Prel) prel.copy(prel.getTraitSet(), children);
    } else {
      return prel;
    }
  }

}
