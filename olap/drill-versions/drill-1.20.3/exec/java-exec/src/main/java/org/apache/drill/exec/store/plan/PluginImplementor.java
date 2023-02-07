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
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.common.DrillLimitRelBase;
import org.apache.drill.exec.store.plan.rel.PluginAggregateRel;
import org.apache.drill.exec.store.plan.rel.PluginFilterRel;
import org.apache.drill.exec.store.plan.rel.PluginJoinRel;
import org.apache.drill.exec.store.plan.rel.PluginLimitRel;
import org.apache.drill.exec.store.plan.rel.PluginProjectRel;
import org.apache.drill.exec.store.plan.rel.PluginRel;
import org.apache.drill.exec.store.plan.rel.PluginSortRel;
import org.apache.drill.exec.store.plan.rel.PluginUnionRel;
import org.apache.drill.exec.store.plan.rel.StoragePluginTableScan;

import java.io.IOException;

/**
 * Callback for the implementation process that checks whether a specific operator
 * can be converted and converts a tree of {@link PluginRel} nodes into expressions
 * that can be consumed by the storage plugin.
 */
public interface PluginImplementor {

  void implement(PluginAggregateRel aggregate) throws IOException;

  void implement(PluginFilterRel filter) throws IOException;

  void implement(PluginLimitRel limit) throws IOException;

  void implement(PluginProjectRel project) throws IOException;

  void implement(PluginSortRel sort) throws IOException;

  void implement(PluginUnionRel union) throws IOException;

  void implement(PluginJoinRel join) throws IOException;

  void implement(StoragePluginTableScan scan) throws IOException;

  boolean canImplement(Aggregate aggregate);

  boolean canImplement(Filter filter);

  boolean canImplement(DrillLimitRelBase limit);

  boolean canImplement(Project project);

  boolean canImplement(Sort sort);

  boolean canImplement(Union union);

  boolean canImplement(Join scan);

  boolean canImplement(TableScan scan);

  GroupScan getPhysicalOperator() throws IOException;

  default void visitChild(RelNode input) throws IOException {
    ((PluginRel) input).implement(this);
  }

  boolean splitProject(Project project);

  /**
   * If the plugin doesn't support native limit pushdown,
   * but the reader can limit the number of rows to read.
   * In this case limit operator on top of the scan should be preserved
   * to ensure returning the correct rows number.
   */
  boolean artificialLimit();
}
