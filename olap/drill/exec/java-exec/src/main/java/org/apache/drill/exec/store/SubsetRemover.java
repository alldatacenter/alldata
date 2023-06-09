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
package org.apache.drill.exec.store;

import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttle;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.util.Util;

/**
 * Removes {@link RelSubset} nodes from the plan.
 * It may be useful when we want to do some operations on the plan
 * which aren't supported by {@link RelSubset} during the planning phase.
 */
public class SubsetRemover extends RelShuttleImpl {
  public static RelShuttle INSTANCE = new SubsetRemover();

  @Override
  public RelNode visit(RelNode other) {
    if (other instanceof RelSubset) {
      RelSubset relSubset = (RelSubset) other;
      return Util.first(relSubset.getBest(), relSubset.getOriginal()).accept(this);
    } else {
      return super.visit(other);
    }
  }
}
