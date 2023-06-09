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

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.store.enumerable.plan.VertexDrelConverterRule;
import org.apache.drill.exec.store.plan.PluginImplementor;
import org.apache.drill.exec.store.plan.rule.PluginAggregateRule;
import org.apache.drill.exec.store.plan.rule.PluginFilterRule;
import org.apache.drill.exec.store.plan.rule.PluginIntermediatePrelConverterRule;
import org.apache.drill.exec.store.plan.rule.PluginJoinRule;
import org.apache.drill.exec.store.plan.rule.PluginLimitRule;
import org.apache.drill.exec.store.plan.rule.PluginProjectRule;
import org.apache.drill.exec.store.plan.rule.PluginSortRule;
import org.apache.drill.exec.store.plan.rule.PluginUnionRule;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class PluginRulesProviderImpl implements PluginRulesProvider {

  private final Supplier<PluginImplementor> implementorSupplier;
  private final PluginImplementor pluginImplementor;
  private final Convention convention;

  public PluginRulesProviderImpl(Convention convention,
      Supplier<PluginImplementor> implementorSupplier) {
    this.convention = convention;
    this.implementorSupplier = implementorSupplier;
    this.pluginImplementor = implementorSupplier.get();
  }

  @Override
  public List<RelOptRule> sortRules() {
    return Arrays.asList(
        new PluginSortRule(Convention.NONE, convention, pluginImplementor),
        new PluginSortRule(DrillRel.DRILL_LOGICAL, convention, pluginImplementor)
    );
  }

  @Override
  public List<RelOptRule> limitRules() {
    return Arrays.asList(
        new PluginLimitRule(Convention.NONE, convention, pluginImplementor),
        new PluginLimitRule(DrillRel.DRILL_LOGICAL, convention, pluginImplementor)
    );
  }

  @Override
  public List<RelOptRule> filterRules() {
    return Arrays.asList(
        new PluginFilterRule(Convention.NONE, convention, pluginImplementor),
        new PluginFilterRule(DrillRel.DRILL_LOGICAL, convention, pluginImplementor)
    );
  }

  @Override
  public List<RelOptRule> projectRules() {
    return Arrays.asList(
        new PluginProjectRule(Convention.NONE, convention, pluginImplementor),
        new PluginProjectRule(DrillRel.DRILL_LOGICAL, convention, pluginImplementor)
    );
  }

  @Override
  public List<RelOptRule> aggregateRules() {
    return Arrays.asList(
        new PluginAggregateRule(Convention.NONE, convention, pluginImplementor),
        new PluginAggregateRule(DrillRel.DRILL_LOGICAL, convention, pluginImplementor)
    );
  }

  @Override
  public List<RelOptRule> unionRules() {
    return Arrays.asList(
        new PluginUnionRule(Convention.NONE, convention, pluginImplementor),
        new PluginUnionRule(DrillRel.DRILL_LOGICAL, convention, pluginImplementor)
    );
  }

  @Override
  public List<RelOptRule> joinRules() {
    return Arrays.asList(
      new PluginJoinRule(Convention.NONE, convention, pluginImplementor),
      new PluginJoinRule(DrillRel.DRILL_LOGICAL, convention, pluginImplementor)
    );
  }

  @Override
  public RelOptRule vertexRule() {
    return new VertexDrelConverterRule(convention);
  }

  @Override
  public RelOptRule prelConverterRule() {
    return new PluginIntermediatePrelConverterRule(convention, implementorSupplier);
  }
}
