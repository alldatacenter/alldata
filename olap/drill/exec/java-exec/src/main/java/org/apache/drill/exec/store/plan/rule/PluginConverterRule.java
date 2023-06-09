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
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.rel.core.Union;
import org.apache.drill.exec.planner.common.DrillLimitRelBase;
import org.apache.drill.exec.planner.logical.DrillRelFactories;
import org.apache.drill.exec.store.plan.PluginImplementor;

import java.util.function.Predicate;

/**
 * Abstract base class for a rule that converts provided operator to plugin-specific implementation.
 */
public abstract class PluginConverterRule extends ConverterRule {

  private final PluginImplementor pluginImplementor;

  protected PluginConverterRule(Class<? extends RelNode> clazz,
      RelTrait in, Convention out, String description, PluginImplementor pluginImplementor) {
    super(clazz, (Predicate<RelNode>) input -> true, in, out, DrillRelFactories.LOGICAL_BUILDER, description);
    this.pluginImplementor = pluginImplementor;
  }

  public PluginImplementor getPluginImplementor() {
    return pluginImplementor;
  }

  @Override
  public boolean matches(RelOptRuleCall call) {
    RelNode rel = call.rel(0);
    boolean canImplement = false;
    // cannot use visitor pattern here, since RelShuttle supports only logical rel implementations
    if (rel instanceof Aggregate) {
      canImplement = pluginImplementor.canImplement(((Aggregate) rel));
    } else if (rel instanceof Filter) {
      canImplement = pluginImplementor.canImplement(((Filter) rel));
    } else if (rel instanceof DrillLimitRelBase) {
      canImplement = pluginImplementor.canImplement(((DrillLimitRelBase) rel));
    } else if (rel instanceof Project) {
      canImplement = pluginImplementor.canImplement(((Project) rel));
    } else if (rel instanceof Sort) {
      canImplement = pluginImplementor.canImplement(((Sort) rel));
    } else if (rel instanceof Union) {
      canImplement = pluginImplementor.canImplement(((Union) rel));
    } else if (rel instanceof Join) {
      canImplement = pluginImplementor.canImplement(((Join) rel));
    }
    return canImplement && super.matches(call);
  }
}
