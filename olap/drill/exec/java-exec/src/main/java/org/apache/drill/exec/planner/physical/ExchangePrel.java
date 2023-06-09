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
package org.apache.drill.exec.planner.physical;

import org.apache.drill.exec.planner.physical.visitor.PrelVisitor;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.drill.exec.server.options.OptionManager;

import java.util.Collections;

public abstract class ExchangePrel extends SinglePrel {

  public ExchangePrel(RelOptCluster cluster, RelTraitSet traits, RelNode child) {
    super(cluster, traits, child);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PrelVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitExchange(this, value);
  }

  /**
   * The derived classes can override this method to create relevant mux exchanges.
   * If this method is not overridden then the default behavior is to clone itself.
   * @param child input to the new muxPrel or new Exchange node.
   * @param options options manager to check if mux is enabled.
   */
  public Prel constructMuxPrel(Prel child, OptionManager options) {
    return (Prel)copy(getTraitSet(), Collections.singletonList(((RelNode)child)));
  }
}
