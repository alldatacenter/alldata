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
package org.apache.drill.exec.store.phoenix.rules;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.adapter.jdbc.JdbcRules.JdbcJoin;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.logical.LogicalJoin;

public class PhoenixJoinRule extends ConverterRule {

  private final JdbcConvention out;

  public PhoenixJoinRule(RelTrait in, JdbcConvention out) {
    super(LogicalJoin.class, in, out, "PhoenixJoinRule");
    this.out = out;
  }

  @Override
  public RelNode convert(RelNode rel) {
    final List<RelNode> newInputs = new ArrayList<>();
    final Join join = (Join) rel;
    for (RelNode input : join.getInputs()) {
      if (input.getConvention() != getOutTrait()) {
        input = convert(input, input.getTraitSet().replace(out));
      }
      newInputs.add(input);
    }
    try {
      JdbcJoin jdbcJoin = new JdbcJoin(
          join.getCluster(),
          join.getTraitSet().replace(out),
          newInputs.get(0),
          newInputs.get(1),
          join.getCondition(),
          join.getVariablesSet(),
          join.getJoinType());
      return jdbcJoin;
    } catch (InvalidRelException e) {
      return null;
    }
  }
}
