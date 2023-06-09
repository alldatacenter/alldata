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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.adapter.jdbc.JdbcRules;
import org.apache.calcite.adapter.jdbc.JdbcRules.JdbcFilterRule;
import org.apache.calcite.adapter.jdbc.JdbcRules.JdbcJoinRule;
import org.apache.calcite.adapter.jdbc.JdbcRules.JdbcProjectRule;
import org.apache.calcite.adapter.jdbc.JdbcRules.JdbcSortRule;
import org.apache.calcite.adapter.jdbc.JdbcToEnumerableConverterRule;
import org.apache.calcite.linq4j.tree.ConstantUntypedNull;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.sql.SqlDialect;
import org.apache.drill.exec.planner.RuleInstance;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillRelFactories;
import org.apache.drill.exec.store.enumerable.plan.DrillJdbcRuleBase;
import org.apache.drill.exec.store.enumerable.plan.VertexDrelConverterRule;
import org.apache.drill.exec.store.phoenix.PhoenixStoragePlugin;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

public class PhoenixConvention extends JdbcConvention {

  private static final Set<Class<? extends RelOptRule>> EXCLUDED_CALCITE_RULES = ImmutableSet.of(
          JdbcToEnumerableConverterRule.class,
          JdbcProjectRule.class,
          JdbcFilterRule.class,
          JdbcSortRule.class,
          JdbcJoinRule.class);

  private final ImmutableSet<RelOptRule> rules;
  private final PhoenixStoragePlugin plugin;

  public PhoenixConvention(SqlDialect dialect, String name, PhoenixStoragePlugin plugin) {
    super(dialect, ConstantUntypedNull.INSTANCE, name);
    this.plugin = plugin;
    List<RelOptRule> calciteJdbcRules = JdbcRules
        .rules(this, DrillRelFactories.LOGICAL_BUILDER).stream()
        .filter(rule -> !EXCLUDED_CALCITE_RULES.contains(rule.getClass()))
        .collect(Collectors.toList());

    List<RelTrait> inputTraits = Arrays.asList(
      Convention.NONE,
      DrillRel.DRILL_LOGICAL);

    ImmutableSet.Builder<RelOptRule> builder = ImmutableSet.<RelOptRule>builder()
      .addAll(calciteJdbcRules)
      .add(new PhoenixIntermediatePrelConverterRule(this))
      .add(new VertexDrelConverterRule(this))
      .add(RuleInstance.FILTER_SET_OP_TRANSPOSE_RULE)
      .add(RuleInstance.PROJECT_REMOVE_RULE);
    for (RelTrait inputTrait : inputTraits) {
      builder
        .add(new DrillJdbcRuleBase.DrillJdbcProjectRule(inputTrait, this))
        .add(new DrillJdbcRuleBase.DrillJdbcFilterRule(inputTrait, this))
        .add(new DrillJdbcRuleBase.DrillJdbcSortRule(inputTrait, this))
        .add(new DrillJdbcRuleBase.DrillJdbcLimitRule(inputTrait, this))
        .add(new PhoenixJoinRule(inputTrait, this));
    }

    this.rules = builder.build();
  }

  @Override
  public void register(RelOptPlanner planner) {
    rules.forEach(planner :: addRule);
  }

  public Set<RelOptRule> getRules() {
    return rules;
  }

  public PhoenixStoragePlugin getPlugin() {
    return plugin;
  }
}
