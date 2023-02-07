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
package org.apache.drill.exec.planner.common;

import java.util.List;

import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Union;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.type.RelDataType;

/**
 * Base class for logical and physical Union implemented in Drill
 */
public abstract class DrillUnionRelBase extends Union implements DrillRelNode {

  public DrillUnionRelBase(RelOptCluster cluster, RelTraitSet traits,
      List<RelNode> inputs, boolean all, boolean checkCompatibility) throws InvalidRelException {
    super(cluster, traits, inputs, all);
    if (checkCompatibility &&
        !this.isCompatible(false /* don't compare names */, true /* allow substrings */)) {
      throw new InvalidRelException("Input row types of the Union are not compatible.");
    }
  }

  public boolean isCompatible(boolean compareNames, boolean allowSubstring) {
    RelDataType unionType = getRowType();
    for (RelNode input : getInputs()) {
      if (! DrillRelOptUtil.areRowTypesCompatible(
          input.getRowType(), unionType, compareNames, allowSubstring)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isDistinct() {
    return !this.all;
  }

}
