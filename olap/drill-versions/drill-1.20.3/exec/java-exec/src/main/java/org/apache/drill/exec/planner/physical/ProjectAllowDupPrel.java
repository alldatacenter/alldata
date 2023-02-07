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

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.Project;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.Pair;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class ProjectAllowDupPrel extends ProjectPrel {

  public ProjectAllowDupPrel(RelOptCluster cluster, RelTraitSet traits, RelNode child, List<RexNode> exps,
                             RelDataType rowType) {
    this(cluster, traits, child, exps, rowType, false);
  }

  public ProjectAllowDupPrel(RelOptCluster cluster, RelTraitSet traits, RelNode child, List<RexNode> exps,
      RelDataType rowType, boolean outputProj) {
    super(cluster, traits, child, exps, rowType, outputProj);
  }

  @Override
  public ProjectAllowDupPrel copy(RelTraitSet traitSet, RelNode input, List<RexNode> exps, RelDataType rowType) {
    return new ProjectAllowDupPrel(getCluster(), traitSet, input, exps, rowType, outputProj);
  }

  @Override
  public PhysicalOperator getPhysicalOperator(PhysicalPlanCreator creator) throws IOException {
    Prel child = (Prel) this.getInput();

    PhysicalOperator childPOP = child.getPhysicalOperator(creator);

    Project p = new Project(this.getProjectExpressions(new DrillParseContext(PrelUtil.getSettings(getCluster()))),
        childPOP, outputProj);
    return creator.addMetadata(this, p);
  }

  @Override
  protected List<NamedExpression> getProjectExpressions(DrillParseContext context) {
    List<NamedExpression> expressions = Lists.newArrayList();
    for (Pair<RexNode, String> pair : Pair.zip(exps, getRowType().getFieldNames())) {
      LogicalExpression expr = DrillOptiq.toDrill(context, getInput(), pair.left);
      expressions.add(new NamedExpression(expr, FieldReference.getWithQuotedRef(pair.right)));
    }
    return expressions;
  }

}
