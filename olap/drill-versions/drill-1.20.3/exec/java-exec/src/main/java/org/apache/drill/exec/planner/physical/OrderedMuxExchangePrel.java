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

import java.io.IOException;
import java.util.List;

import org.apache.calcite.rel.RelFieldCollation;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.OrderedMuxExchange;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.linq4j.Ord;


/**
 * OrderedMuxExchangePrel is mux exchange created to multiplex the streams for a MergeReceiver.
 */
public class OrderedMuxExchangePrel extends ExchangePrel {
  private final RelCollation fieldCollation;

  public OrderedMuxExchangePrel(RelOptCluster cluster, RelTraitSet traits, RelNode child, RelCollation collation) {
    super(cluster, traits, child);
    this.fieldCollation = collation;
  }

  @Override
  public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
    return new OrderedMuxExchangePrel(getCluster(), traitSet, sole(inputs), fieldCollation);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel child = (Prel) this.getInput();

    PhysicalOperator childPOP = child.getPhysicalOperator(creator);

    OrderedMuxExchange p = new OrderedMuxExchange(childPOP, PrelUtil.getOrdering(fieldCollation, getInput().getRowType()));
    return creator.addMetadata(this, p);
  }

  @Override
  public RelWriter explainTerms(RelWriter pw) {
    super.explainTerms(pw);
    for (Ord<RelFieldCollation> ord : Ord.zip(this.fieldCollation.getFieldCollations())) {
      pw.item("sort" + ord.i, ord.e);
    }
    return pw;
  }

  @Override
  public SelectionVectorMode getEncoding() {
    return SelectionVectorMode.NONE;
  }
}
