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

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.RuntimeFilterPOP;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;

import java.io.IOException;
import java.util.List;

public class RuntimeFilterPrel extends SinglePrel {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RuntimeFilterPrel.class);

  private long identifier;

  public RuntimeFilterPrel(Prel child, long identifier){
    super(child.getCluster(), child.getTraitSet(), child);
    this.identifier = identifier;
  }

  public RuntimeFilterPrel(RelOptCluster cluster, RelTraitSet traits, RelNode child, long identifier) {
    super(cluster, traits, child);
    this.identifier = identifier;
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new RuntimeFilterPrel(this.getCluster(), traitSet, inputs.get(0), identifier);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    RuntimeFilterPOP r =  new RuntimeFilterPOP( ((Prel)getInput()).getPhysicalOperator(creator), identifier);
    return creator.addMetadata(this, r);
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.TWO_BYTE;
  }
}
