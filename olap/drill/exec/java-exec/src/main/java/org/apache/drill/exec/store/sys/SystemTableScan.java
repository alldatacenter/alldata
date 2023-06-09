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
package org.apache.drill.exec.store.sys;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.planner.fragment.DistributionAffinity;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.StoragePluginRegistry;

@JsonTypeName("sys")
public class SystemTableScan extends AbstractGroupScan implements SubScan {

  public static final String OPERATOR_TYPE = "SYSTEM_TABLE_SCAN";

  private final SystemTable table;
  private final SystemTablePlugin plugin;
  private final int maxRecordsToRead;

  @JsonCreator
  public SystemTableScan(@JsonProperty("table") SystemTable table,
                         @JsonProperty("maxRecordsToRead") int maxRecordsToRead,
                         @JacksonInject StoragePluginRegistry engineRegistry) throws ExecutionSetupException {
    this(table, maxRecordsToRead,
        engineRegistry.resolve(SystemTablePluginConfig.INSTANCE, SystemTablePlugin.class));
  }

  public SystemTableScan(SystemTable table, SystemTablePlugin plugin) {
    this(table, Integer.MAX_VALUE, plugin);
  }

  public SystemTableScan(SystemTable table, int maxRecordsToRead, SystemTablePlugin plugin) {
    super((String) null);
    this.table = table;
    this.maxRecordsToRead = maxRecordsToRead;
    this.plugin = plugin;
  }

  /**
   * System tables do not need stats.
   * @return a trivial stats table
   */
  @Override
  public ScanStats getScanStats() {
    return ScanStats.TRIVIAL_TABLE;
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    return new SystemTableScan(table, maxRecordsToRead, plugin);
  }

  @Override
  public void applyAssignments(List<DrillbitEndpoint> endpoints) {
  }

  @JsonIgnore
  @Override
  public List<SchemaPath> getColumns() {
    return super.getColumns();
  }

  @Override
  public SubScan getSpecificScan(int minorFragmentId) {
    return this;
  }

  // If distributed, the scan needs to happen on every node.
  @Override
  public int getMaxParallelizationWidth() {
    return table.isDistributed() ? plugin.getContext().getBits().size() : 1;
  }

  // If distributed, the scan needs to happen on every node.
  @Override
  public int getMinParallelizationWidth() {
    return table.isDistributed() ? plugin.getContext().getBits().size() : 1;
  }

  @Override
  public long getInitialAllocation() {
    return initialAllocation;
  }

  @Override
  public long getMaxAllocation() {
    return maxAllocation;
  }

  /**
   * Example: SystemTableScan [table=OPTION, distributed=false, maxRecordsToRead=1]
   *
   * @return string representation of system table scan
   */
  @Override
  public String getDigest() {
    StringBuilder builder = new StringBuilder();
    builder.append("SystemTableScan [");
    builder.append("table=").append(table.name()).append(", ");
    builder.append("distributed=").append(table.isDistributed());
    if (maxRecordsToRead != Integer.MAX_VALUE) {
      builder.append(", maxRecordsToRead=").append(maxRecordsToRead);
    }
    builder.append("]");

    return builder.toString();
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  /**
   * If distributed, the scan needs to happen on every node. Since width is enforced, the number of fragments equals
   * number of Drillbits. And here we set, each endpoint as mandatory assignment required to ensure every
   * Drillbit executes a fragment.
   * @return the Drillbit endpoint affinities
   */
  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    if (table.isDistributed()) {
      final List<EndpointAffinity> affinities = Lists.newArrayList();
      final Collection<DrillbitEndpoint> bits = plugin.getContext().getBits();
      final double affinityPerNode = 1d / bits.size();
      for (final DrillbitEndpoint endpoint : bits) {
        affinities.add(new EndpointAffinity(endpoint, affinityPerNode, true, /* maxWidth = */ 1));
      }
      return affinities;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public DistributionAffinity getDistributionAffinity() {
    return table.isDistributed() ? DistributionAffinity.HARD : DistributionAffinity.SOFT;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    return this;
  }

  public GroupScan clone(SystemTableScan systemTableScan, int maxRecordsToRead) {
    return new SystemTableScan(systemTableScan.getTable(), maxRecordsToRead, systemTableScan.getPlugin());
  }

  @Override
  public boolean supportsLimitPushdown() {
    return true;
  }

  @Override
  public GroupScan applyLimit(int maxRecords) {
    if (maxRecordsToRead == maxRecords) {
      return null;
    }
    return clone(this, maxRecords);
  }

  public SystemTable getTable() {
    return table;
  }

  public int getMaxRecordsToRead() {
    return maxRecordsToRead;
  }

  @JsonIgnore
  public SystemTablePlugin getPlugin() {
    return plugin;
  }
}
