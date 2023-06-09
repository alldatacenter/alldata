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
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Sort;
import org.apache.drill.exec.store.plan.PluginImplementor;
import org.apache.drill.exec.store.plan.rel.PluginSortRel;

/**
 * The rule that converts provided sort operator to plugin-specific implementation.
 */
public class PluginSortRule extends PluginConverterRule {

  public PluginSortRule(RelTrait in, Convention out, PluginImplementor pluginImplementor) {
    super(Sort.class, in, out, "PluginSortRule", pluginImplementor);
  }

  @Override
  public RelNode convert(RelNode rel) {
    Sort sort = (Sort) rel;
    RelNode input = convert(sort.getInput(), sort.getInput().getTraitSet().replace(getOutConvention()).simplify());
    return new PluginSortRel(
        rel.getCluster(),
        sort.getTraitSet().replace(getOutConvention()).replace(sort.getCollation()),
        input,
        sort.getCollation(),
        sort.offset,
        sort.fetch);
  }
}
