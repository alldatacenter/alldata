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
package org.apache.drill.exec.store.plan.rule;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.drill.exec.store.plan.PluginImplementor;
import org.apache.drill.exec.store.plan.rel.PluginJoinRel;

/**
 * The rule that converts provided join operator to plugin-specific implementation.
 */
public class PluginJoinRule extends PluginConverterRule {

  public PluginJoinRule(RelTrait in, Convention out, PluginImplementor pluginImplementor) {
    super(Join.class, in, out, "PluginProjectRule", pluginImplementor);
  }

  @Override
  public RelNode convert(RelNode rel) {
    Join join = (Join) rel;
    RelTraitSet traits = join.getTraitSet().replace(getOutConvention());
    return new PluginJoinRel(
        join.getCluster(),
        traits,
        convert(join.getLeft(), traits),
        convert(join.getRight(), traits),
        join.getCondition(),
        join.getJoinType());
  }
}
