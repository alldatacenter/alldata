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
package org.apache.drill.exec.store.ischema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.ScanStats.GroupScanProperty;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.List;

@JsonTypeName("info-schema")
public class InfoSchemaGroupScan extends AbstractGroupScan {

  private final InfoSchemaTableType table;
  private final InfoSchemaFilter filter;

  public InfoSchemaGroupScan(InfoSchemaTableType table) {
    this(table, null);
  }

  @JsonCreator
  public InfoSchemaGroupScan(@JsonProperty("table") InfoSchemaTableType table,
                             @JsonProperty("filter") InfoSchemaFilter filter) {
    super((String) null);
    this.table = table;
    this.filter = filter;
  }

  private InfoSchemaGroupScan(InfoSchemaGroupScan that) {
    super(that);
    this.table = that.table;
    this.filter = that.filter;
  }

  @JsonProperty("table")
  public InfoSchemaTableType getTable() {
    return table;
  }

  @JsonProperty("filter")
  public InfoSchemaFilter getSchemaFilter() {
    return filter;
  }

  @JsonIgnore
  @Override
  public List<SchemaPath> getColumns() {
    return super.getColumns();
  }

  @Override
  public void applyAssignments(List<DrillbitEndpoint> endpoints) {
    Preconditions.checkArgument(endpoints.size() == 1);
  }

  @Override
  public SubScan getSpecificScan(int minorFragmentId) {
    Preconditions.checkArgument(minorFragmentId == 0);
    return new InfoSchemaSubScan(table, filter);
  }

  @Override
  public ScanStats getScanStats() {
    if (filter == null) {
      return ScanStats.TRIVIAL_TABLE;
    } else {
      // If the filter is available, return stats that is lower in cost than TRIVIAL_TABLE cost so that
      // Scan with Filter is chosen.
      return new ScanStats(GroupScanProperty.NO_EXACT_ROW_COUNT, 10, 1, 0);
    }
  }

  @Override
  public int getMaxParallelizationWidth() {
    return 1;
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    return new InfoSchemaGroupScan(this);
  }

  @Override
  public String getDigest() {
    return table.toString() + ", filter=" + filter;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    return new InfoSchemaGroupScan(this);
  }

  @JsonIgnore
  public boolean isFilterPushedDown() {
    return filter != null;
  }
}
