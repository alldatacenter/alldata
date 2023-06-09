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
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.store.base.filter.ExprNode;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonTypeName("splunk-sub-scan")
public class SplunkSubScan extends AbstractBase implements SubScan {

  private final SplunkPluginConfig config;
  private final SplunkScanSpec splunkScanSpec;
  private final List<SchemaPath> columns;
  private final Map<String, ExprNode.ColRelOpConstNode> filters;
  private final int maxRecords;

  @JsonCreator
  public SplunkSubScan(
    @JsonProperty("config") SplunkPluginConfig config,
    @JsonProperty("tableSpec") SplunkScanSpec splunkScanSpec,
    @JsonProperty("columns") List<SchemaPath> columns,
    @JsonProperty("filters") Map<String, ExprNode.ColRelOpConstNode> filters,
    @JsonProperty("maxRecords") int maxRecords) {
      super("user");
      this.config = config;
      this.splunkScanSpec = splunkScanSpec;
      this.columns = columns;
      this.filters = filters;
      this.maxRecords = maxRecords;
  }

  @JsonProperty("config")
  public SplunkPluginConfig getConfig() {
    return config;
  }

  @JsonProperty("tableSpec")
  public SplunkScanSpec getScanSpec() {
    return splunkScanSpec;
  }

  @JsonProperty("columns")
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty("filters")
  public Map<String, ExprNode.ColRelOpConstNode> getFilters() {
    return filters;
  }

  @JsonProperty("maxRecords")
  public int getMaxRecords() {
    return maxRecords;
  }

  @Override
  public <T, X, E extends Throwable> T accept(
    PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    return new SplunkSubScan(config, splunkScanSpec, columns, filters, maxRecords);
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return ImmutableSet.<PhysicalOperator>of().iterator();
  }

  @Override
  @JsonIgnore
  public String getOperatorType() {
    return "SPLUNK";
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("config", config)
      .field("tableSpec", splunkScanSpec)
      .field("columns", columns)
      .field("filters", filters)
      .field("maxRecords", maxRecords)
      .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(config, splunkScanSpec, columns, filters, maxRecords);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SplunkSubScan other = (SplunkSubScan) obj;
    return Objects.equals(splunkScanSpec, other.splunkScanSpec)
      && Objects.equals(config, other.config)
      && Objects.equals(columns, other.columns)
      && Objects.equals(filters, other.filters)
      && Objects.equals(maxRecords, other.maxRecords);
  }
}
