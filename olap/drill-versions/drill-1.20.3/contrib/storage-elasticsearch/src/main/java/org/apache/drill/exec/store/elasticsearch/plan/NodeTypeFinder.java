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
package org.apache.drill.exec.store.elasticsearch.plan;

import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.util.Util;

public class NodeTypeFinder extends RelShuttleImpl {
  public boolean containsNode = false;

  private final Class<?> clazz;

  public NodeTypeFinder(Class<?> clazz) {
    this.clazz = clazz;
  }

  @Override
  public RelNode visit(RelNode other) {
    if (other.getClass().isAssignableFrom(clazz)) {
      this.containsNode = true;
      return other;
    } else if (other instanceof RelSubset) {
      RelSubset relSubset = (RelSubset) other;
      return Util.first(relSubset.getBest(), relSubset.getOriginal()).accept(this);
    }
    return super.visit(other);
  }
}
