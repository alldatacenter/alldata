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
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexFieldAccess;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.Unnest;
import org.apache.drill.exec.planner.common.DrillUnnestRelBase;


public class DrillUnnestRel extends DrillUnnestRelBase implements DrillRel {


  public DrillUnnestRel(RelOptCluster cluster, RelTraitSet traits,
                        RelDataType rowType, RexNode ref) {
    super(cluster, traits, ref);
    this.rowType = rowType;
  }

  @Override
  public LogicalOperator implement(DrillImplementor implementor) {
    if(getRef() instanceof RexFieldAccess) {
      final RexFieldAccess fldAccess = (RexFieldAccess)getRef();
      return new Unnest(SchemaPath.getSimplePath(fldAccess.getField().getName()));
    }

    return null;
  }

}
