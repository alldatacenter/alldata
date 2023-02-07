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

package org.apache.drill.exec.store.splunk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.store.base.filter.ExprNode;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SplunkGroupScan extends AbstractGroupScan {

  private final SplunkPluginConfig config;
  private final List<SchemaPath> columns;
  private final SplunkScanSpec splunkScanSpec;
  private final Map<String, ExprNode.ColRelOpConstNode> filters;
  private final ScanStats scanStats;
  private final double filterSelectivity;
  private final int maxRecords;

  private int hashCode;

  /**
   * Creates a new group scan from the storage plugin.
   */
  public SplunkGroupScan (SplunkScanSpec scanSpec) {
    super("no-user");
    this.splunkScanSpec = scanSpec;
    this.config = scanSpec.getConfig();
    this.columns = ALL_COLUMNS;
    this.filters = null;
    this.filterSelectivity = 0.0;
    this.maxRecords = -1;
    this.scanStats = computeScanStats();

  }

  /**
   * Copies the group scan during many stages of Calcite operation.
   */
  public SplunkGroupScan(SplunkGroupScan that) {
    super(that);
    this.config = that.config;
    this.splunkScanSpec = that.splunkScanSpec;
    this.columns = that.columns;
    this.filters = that.filters;
    this.filterSelectivity = that.filterSelectivity;
    this.maxRecords = that.maxRecords;

    // Calcite makes many copies in the later stage of planning
    // without changing anything. Retain the previous stats.
    this.scanStats = that.scanStats;
  }

  /**
   * Applies columns. Oddly called multiple times, even when
   * the scan already has columns.
   */
  public SplunkGroupScan(SplunkGroupScan that, List<SchemaPath> columns) {
    super(that);
    this.columns = columns;
    this.splunkScanSpec = that.splunkScanSpec;
    this.config = that.config;

    // Oddly called later in planning, after earlier assigning columns,
    // to again assign columns. Retain filters, but compute new stats.
    this.filters = that.filters;
    this.filterSelectivity = that.filterSelectivity;
    this.maxRecords = that.maxRecords;
    this.scanStats = computeScanStats();

  }

  /**
   * Adds a filter to the scan.
   */
  public SplunkGroupScan(SplunkGroupScan that, Map<String, ExprNode.ColRelOpConstNode> filters,
                       double filterSelectivity) {
    super(that);
    this.columns = that.columns;
    this.splunkScanSpec = that.splunkScanSpec;
    this.config = that.config;

    // Applies a filter.
    this.filters = filters;
    this.filterSelectivity = filterSelectivity;
    this.maxRecords = that.maxRecords;
    this.scanStats = computeScanStats();
  }

  /**
   * Deserialize a group scan. Not called in normal operation. Probably used
   * only if Drill executes a logical plan.
   */
  @JsonCreator
  public SplunkGroupScan(
    @JsonProperty("config") SplunkPluginConfig config,
    @JsonProperty("columns") List<SchemaPath> columns,
    @JsonProperty("splunkScanSpec") SplunkScanSpec splunkScanSpec,
    @JsonProperty("filters") Map<String, ExprNode.ColRelOpConstNode> filters,
    @JsonProperty("filterSelectivity") double selectivity,
    @JsonProperty("maxRecords") int maxRecords
  ) {
    super("no-user");
    this.config = config;
    this.columns = columns;
    this.splunkScanSpec = splunkScanSpec;
    this.filters = filters;
    this.filterSelectivity = selectivity;
    this.maxRecords = maxRecords;
    this.scanStats = computeScanStats();
  }

  /**
   * Adds a limit to the group scan
   * @param that Previous SplunkGroupScan
   * @param maxRecords the limit pushdown
   */
  public SplunkGroupScan(SplunkGroupScan that, int maxRecords) {
    super(that);
    this.columns = that.columns;
    // Apply the limit
    this.maxRecords = maxRecords;
    this.splunkScanSpec = that.splunkScanSpec;
    this.config = that.config;
    this.filters = that.filters;
    this.filterSelectivity = that.filterSelectivity;
    this.scanStats = computeScanStats();
  }

  @JsonProperty("config")
  public SplunkPluginConfig config() { return config; }

  @JsonProperty("columns")
  public List<SchemaPath> columns() { return columns; }

  @JsonProperty("splunkScanSpec")
  public SplunkScanSpec splunkScanSpec() { return splunkScanSpec; }

  @JsonProperty("filters")
  public Map<String, ExprNode.ColRelOpConstNode> filters() { return filters; }

  @JsonProperty("maxRecords")
  public int maxRecords() { return maxRecords; }

  @JsonProperty("filterSelectivity")
  public double selectivity() { return filterSelectivity; }


  @Override
  public void applyAssignments(List<CoordinationProtos.DrillbitEndpoint> endpoints) { }

  @Override
  public SubScan getSpecificScan(int minorFragmentId) {
    return new SplunkSubScan(config, splunkScanSpec, columns, filters, maxRecords);
  }

  @Override
  public int getMaxParallelizationWidth() {
    return 1;
  }

  @Override
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return true;
  }

  @Override
  public boolean supportsLimitPushdown() {
    return true;
  }

  @Override
  public GroupScan applyLimit(int maxRecords) {
    if (maxRecords == this.maxRecords) {
      return null;
    }
    return new SplunkGroupScan(this, maxRecords);
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new SplunkGroupScan(this);
  }

  @Override
  public ScanStats getScanStats() {

    // Since this class is immutable, compute stats once and cache
    // them. If the scan changes (adding columns, adding filters), we
    // get a new scan without cached stats.
    return scanStats;
  }

  private ScanStats computeScanStats() {

    // If this config allows filters, then make the default
    // cost very high to force the planner to choose the version
    // with filters.
    if (allowsFilters() && !hasFilters() && !hasLimit()) {
      return new ScanStats(ScanStats.GroupScanProperty.ESTIMATED_TOTAL_COST,
        1E9, 1E112, 1E12);
    }

    // No good estimates at all, just make up something.
    double estRowCount = 100_000;

    // NOTE this was important! if the predicates don't make the query more
    // efficient they won't get pushed down
    if (hasFilters()) {
      estRowCount *= filterSelectivity;
    }

    if (maxRecords > 0) {
      estRowCount = estRowCount / 2;
    }

    double estColCount = Utilities.isStarQuery(columns) ? DrillScanRel.STAR_COLUMN_COST : columns.size();
    double valueCount = estRowCount * estColCount;
    double cpuCost = valueCount;
    double ioCost = valueCount;

    // Force the caller to use our costs rather than the
    // defaults (which sets IO cost to zero).
    return new ScanStats(ScanStats.GroupScanProperty.ESTIMATED_TOTAL_COST,
      estRowCount, cpuCost, ioCost);
  }

  @JsonIgnore
  public boolean hasFilters() {
    return filters != null;
  }

  @JsonIgnore
  public boolean hasLimit() { return maxRecords == -1; }

  @JsonIgnore
  public boolean allowsFilters() {
    return true;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    return new SplunkGroupScan(this, columns);
  }

  @Override
  public int hashCode() {

    // Hash code is cached since Calcite calls this method many times.
    if (hashCode == 0) {
      // Don't include cost; it is derived.
      hashCode = Objects.hash(splunkScanSpec, config, splunkScanSpec, columns);
    }
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    // Don't include cost; it is derived.
    SplunkGroupScan other = (SplunkGroupScan) obj;
    return Objects.equals(splunkScanSpec, other.splunkScanSpec())
      && Objects.equals(config, other.config())
      && Objects.equals(columns, other.columns())
      && Objects.equals(maxRecords, other.maxRecords());
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("config", config)
      .field("scan spec", splunkScanSpec)
      .field("columns", columns)
      .field("maxRecords", maxRecords)
      .toString();
  }
}
