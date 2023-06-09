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
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Union;
import org.apache.drill.exec.store.plan.PluginImplementor;
import org.apache.drill.exec.store.plan.rel.PluginUnionRel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The rule that converts provided union operator to plugin-specific implementation.
 */
public class PluginUnionRule extends PluginConverterRule {
  private static final Logger logger = LoggerFactory.getLogger(PluginUnionRule.class);

  public PluginUnionRule(RelTrait in, Convention out, PluginImplementor pluginImplementor) {
    super(Union.class, in, out, "PluginUnionRule", pluginImplementor);
  }

  @Override
  public RelNode convert(RelNode rel) {
    Union union = (Union) rel;
    try {
      return new PluginUnionRel(
          rel.getCluster(),
          union.getTraitSet().replace(getOutConvention()),
          convertList(union.getInputs(), getOutConvention()),
          union.all,
          true);
    } catch (InvalidRelException e) {
      logger.warn(e.getMessage());
      return null;
    }
  }
}
