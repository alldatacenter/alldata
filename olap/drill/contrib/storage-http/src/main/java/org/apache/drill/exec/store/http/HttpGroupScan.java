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
package org.apache.drill.exec.store.http;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.expression.SchemaPath;

import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.http.util.SimpleHttp;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;


@JsonTypeName("http-scan")
public class HttpGroupScan extends AbstractGroupScan {

  private final List<SchemaPath> columns;
  private final HttpScanSpec httpScanSpec;
  private final Map<String, String> filters;
  private final ScanStats scanStats;
  private final double filterSelectivity;
  private final int maxRecords;

  // Used only in planner, not serialized
  private int hashCode;

  /**
   * Creates a new group scan from the storage plugin.
   */
  public HttpGroupScan (HttpScanSpec scanSpec) {
    super("no-user");
    this.httpScanSpec = scanSpec;
    this.columns = ALL_COLUMNS;
    this.filters = null;
    this.filterSelectivity = 0.0;
    this.scanStats = computeScanStats();
    this.maxRecords = -1;
  }

  /**
   * Copies the group scan during many stages of Calcite operation.
   */
  public HttpGroupScan(HttpGroupScan that) {
    super(that);
    this.httpScanSpec = that.httpScanSpec;
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
  public HttpGroupScan(HttpGroupScan that, List<SchemaPath> columns) {
    super(that);
    this.columns = columns;
    this.httpScanSpec = that.httpScanSpec;

    // Oddly called later in planning, after earlier assigning columns,
    // to again assign columns. Retain filters, but compute new stats.
    this.filters = that.filters;
    this.filterSelectivity = that.filterSelectivity;
    this.scanStats = computeScanStats();
    this.maxRecords = that.maxRecords;
  }

  /**
   * Adds a filter to the scan.
   */
  public HttpGroupScan(HttpGroupScan that, Map<String, String> filters,
      double filterSelectivity) {
    super(that);
    this.columns = that.columns;
    this.httpScanSpec = that.httpScanSpec;

    // Applies a filter.
    this.filters = filters;
    this.filterSelectivity = filterSelectivity;
    this.scanStats = computeScanStats();
    this.maxRecords = that.maxRecords;
  }

  /**
   * Adds a limit to the scan.
   */
  public HttpGroupScan(HttpGroupScan that, int maxRecords) {
    super(that);
    this.columns = that.columns;
    this.httpScanSpec = that.httpScanSpec;

    // Applies a filter.
    this.filters = that.filters;
    this.filterSelectivity = that.filterSelectivity;
    this.scanStats = computeScanStats();
    this.maxRecords = maxRecords;
  }


  /**
   * Deserialize a group scan. Not called in normal operation. Probably used
   * only if Drill executes a logical plan.
   */
  @JsonCreator
  public HttpGroupScan(
    @JsonProperty("columns") List<SchemaPath> columns,
    @JsonProperty("httpScanSpec") HttpScanSpec httpScanSpec,
    @JsonProperty("filters") Map<String, String> filters,
    @JsonProperty("filterSelectivity") double selectivity,
    @JsonProperty("maxRecords") int maxRecords
  ) {
    super("no-user");
    this.columns = columns;
    this.httpScanSpec = httpScanSpec;
    this.filters = filters;
    this.filterSelectivity = selectivity;
    this.scanStats = computeScanStats();
    this.maxRecords = maxRecords;
  }

  @JsonProperty("columns")
  public List<SchemaPath> columns() { return columns; }

  @JsonProperty("httpScanSpec")
  public HttpScanSpec httpScanSpec() { return httpScanSpec; }

  @JsonProperty("filters")
  public Map<String, String> filters() { return filters; }

  @JsonProperty("filterSelectivity")
  public double selectivity() { return filterSelectivity; }

  @Override
  public void applyAssignments(List<DrillbitEndpoint> endpoints) { }

  @Override
  @JsonIgnore
  public int getMaxParallelizationWidth() {
    return 1;
  }

  @Override
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return true;
  }

  @JsonIgnore
  public HttpApiConfig getHttpConfig() {
    return httpScanSpec.connectionConfig();
  }

  @Override
  public SubScan getSpecificScan(int minorFragmentId) {
    return new HttpSubScan(httpScanSpec, columns, filters, maxRecords);
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    return new HttpGroupScan(this, columns);
  }

  @Override
  @JsonIgnore
  public String getDigest() {
    return toString();
  }

  @JsonProperty("maxRecords")
  public int maxRecords() { return maxRecords; }


  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new HttpGroupScan(this);
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
    if (allowsFilters() && !hasFilters()) {
      return new ScanStats(ScanStats.GroupScanProperty.ESTIMATED_TOTAL_COST,
          1E9, 1E112, 1E12);
    }

    // No good estimates at all, just make up something.  Make it smaller if there is a limit.
    double estRowCount = 10_000;
    // If the limit is greater than 10_000 then use a smaller number so the limit is pushed down.
    if (maxRecords >= -1) {
      estRowCount = Math.min(maxRecords, 10_000);
      estRowCount = estRowCount / 2;
    }


    // NOTE this was important! if the predicates don't make the query more
    // efficient they won't get pushed down
    if (hasFilters()) {
      estRowCount *= filterSelectivity;
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

  @Override
  public boolean supportsLimitPushdown() {
    return true;
  }

  @Override
  public GroupScan applyLimit(int maxRecords) {
    if (maxRecords == this.maxRecords) {
      return null;
    }
    return new HttpGroupScan(this, maxRecords);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("scan spec", httpScanSpec)
      .field("columns", columns)
      .field("filters", filters)
      .field("maxRecords", maxRecords)
      .toString();
  }

  @Override
  public int hashCode() {

    // Hash code is cached since Calcite calls this method many times.
    if (hashCode == 0) {
      // Don't include cost; it is derived.
      hashCode = Objects.hash(httpScanSpec, columns, filters);
    }
    return hashCode;
  }

  @JsonIgnore
  public boolean allowsFilters() {
    // Return true if the query has either parameters specified in the URL or URL params.
    return (getHttpConfig().params() != null) || SimpleHttp.hasURLParameters(getHttpConfig().getHttpUrl());
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
    HttpGroupScan other = (HttpGroupScan) obj;
    return Objects.equals(httpScanSpec, other.httpScanSpec())
        && Objects.equals(columns, other.columns())
        && Objects.equals(filters, other.filters());
  }
}
