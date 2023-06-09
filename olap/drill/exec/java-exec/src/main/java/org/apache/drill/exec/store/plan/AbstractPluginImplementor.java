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
package org.apache.drill.exec.store.plan;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.core.Union;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.util.function.CheckedFunction;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillLimitRelBase;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.store.plan.rel.PluginAggregateRel;
import org.apache.drill.exec.store.plan.rel.PluginFilterRel;
import org.apache.drill.exec.store.plan.rel.PluginJoinRel;
import org.apache.drill.exec.store.plan.rel.PluginLimitRel;
import org.apache.drill.exec.store.plan.rel.PluginProjectRel;
import org.apache.drill.exec.store.plan.rel.PluginSortRel;
import org.apache.drill.exec.store.plan.rel.PluginUnionRel;
import org.apache.drill.exec.store.plan.rel.StoragePluginTableScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Abstract base implementation of {@link PluginImplementor} that can be used by
 * plugin implementors which can support only a subset of all provided operations.
 */
public abstract class AbstractPluginImplementor implements PluginImplementor {
  private static final Logger logger = LoggerFactory.getLogger(AbstractPluginImplementor.class);

  @Override
  public void implement(PluginAggregateRel aggregate) throws IOException {
    throw getUnsupported("aggregate");
  }

  @Override
  public void implement(PluginFilterRel filter) throws IOException {
    throw getUnsupported("filter");
  }

  @Override
  public void implement(PluginLimitRel limit) throws IOException {
    throw getUnsupported("limit");
  }

  @Override
  public void implement(PluginProjectRel project) throws IOException {
    throw getUnsupported("project");
  }

  @Override
  public void implement(PluginSortRel sort) throws IOException {
    throw getUnsupported("sort");
  }

  @Override
  public void implement(PluginUnionRel union) throws IOException {
    throw getUnsupported("union");
  }

  @Override
  public void implement(PluginJoinRel join) throws IOException {
    throw getUnsupported("join");
  }

  @Override
  public void implement(StoragePluginTableScan scan) throws IOException {
    throw getUnsupported("scan");
  }

  @Override
  public boolean canImplement(Aggregate aggregate) {
    return false;
  }

  @Override
  public boolean canImplement(Filter filter) {
    return false;
  }

  @Override
  public boolean canImplement(DrillLimitRelBase limit) {
    return false;
  }

  @Override
  public boolean canImplement(Project project) {
    return false;
  }

  @Override
  public boolean canImplement(Sort sort) {
    return false;
  }

  @Override
  public boolean canImplement(Union union) {
    return false;
  }

  @Override
  public boolean canImplement(TableScan scan) {
    return false;
  }

  @Override
  public boolean canImplement(Join scan) {
    return false;
  }

  @Override
  public boolean splitProject(Project project) {
    return false;
  }

  @Override
  public boolean artificialLimit() {
    return false;
  }

  private UserException getUnsupported(String rel) {
    return UserException.unsupportedError()
        .message("Plugin implementor doesn't support push down for %s", rel)
        .build(logger);
  }

  protected GroupScan findGroupScan(RelNode node) {
    CheckedFunction<DrillTable, GroupScan, IOException> groupScanFunction = DrillTable::getGroupScan;
    return Optional.ofNullable(DrillRelOptUtil.findScan(node))
      .map(DrillRelOptUtil::getDrillTable)
      .filter(this::supportsDrillTable)
      .map(groupScanFunction)
      .orElse(null);
  }

  private boolean supportsDrillTable(DrillTable table) {
    return supportedPlugin().isInstance(table.getPlugin());
  }

  protected abstract Class<? extends StoragePlugin> supportedPlugin();

  protected abstract boolean hasPluginGroupScan(RelNode node);
}
