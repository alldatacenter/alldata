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
package org.apache.drill.exec.store.jdbc;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.adapter.jdbc.JdbcRules;
import org.apache.calcite.adapter.jdbc.JdbcRules.JdbcFilterRule;
import org.apache.calcite.adapter.jdbc.JdbcRules.JdbcProjectRule;
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
import org.apache.drill.exec.store.jdbc.rules.JdbcLimitRule;
import org.apache.drill.exec.store.jdbc.rules.JdbcSortRule;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

/**
 * Convention with set of rules to register for jdbc plugin
 */
public class DrillJdbcConvention extends JdbcConvention {

  /**
   * Unwanted Calcite's JdbcRules are filtered out using this set
   */
  private static final Set<Class<? extends RelOptRule>> EXCLUDED_CALCITE_RULES = ImmutableSet.of(
      JdbcToEnumerableConverterRule.class, JdbcFilterRule.class, JdbcProjectRule.class, JdbcRules.JdbcSortRule.class);

  private final ImmutableSet<RelOptRule> rules;
  private final JdbcStoragePlugin plugin;

  DrillJdbcConvention(SqlDialect dialect, String name, JdbcStoragePlugin plugin) {
    super(dialect, ConstantUntypedNull.INSTANCE, name);
    this.plugin = plugin;
    List<RelOptRule> calciteJdbcRules = JdbcRules.rules(this, DrillRelFactories.LOGICAL_BUILDER).stream()
        .filter(rule -> !EXCLUDED_CALCITE_RULES.contains(rule.getClass()))
        .collect(Collectors.toList());

    List<RelTrait> inputTraits = Arrays.asList(
      Convention.NONE,
      DrillRel.DRILL_LOGICAL);

    ImmutableSet.Builder<RelOptRule> builder = ImmutableSet.<RelOptRule>builder()
      .addAll(calciteJdbcRules)
      .add(new JdbcIntermediatePrelConverterRule(this))
      .add(new VertexDrelConverterRule(this))
      .add(RuleInstance.FILTER_SET_OP_TRANSPOSE_RULE)
      .add(RuleInstance.PROJECT_REMOVE_RULE);
    for (RelTrait inputTrait : inputTraits) {
      builder
        .add(new DrillJdbcRuleBase.DrillJdbcProjectRule(inputTrait, this))
        .add(new DrillJdbcRuleBase.DrillJdbcFilterRule(inputTrait, this))
        .add(new JdbcSortRule(inputTrait, this))
        .add(new JdbcLimitRule(inputTrait, this));
    }

    this.rules = builder.build();
  }

  @Override
  public void register(RelOptPlanner planner) {
    rules.forEach(planner::addRule);
  }

  public Set<RelOptRule> getRules() {
    return rules;
  }

  public JdbcStoragePlugin getPlugin() {
    return plugin;
  }
}
