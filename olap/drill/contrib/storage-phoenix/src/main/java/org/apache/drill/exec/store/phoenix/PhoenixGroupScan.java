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
package org.apache.drill.exec.store.phoenix;

import java.util.List;
import java.util.Objects;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.PhysicalOperatorSetupException;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.StoragePluginRegistry;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("phoenix-scan")
public class PhoenixGroupScan extends AbstractGroupScan {

  private final String sql;
  private final List<SchemaPath> columns;
  private final PhoenixScanSpec scanSpec;
  private final PhoenixStoragePlugin plugin;

  private int hashCode;

  @JsonCreator
  public PhoenixGroupScan(
      @JsonProperty("userName") String userName,
      @JsonProperty("sql") String sql,
      @JsonProperty("columns") List<SchemaPath> columns,
      @JsonProperty("scanSpec") PhoenixScanSpec scanSpec,
      @JsonProperty("config") PhoenixStoragePluginConfig config,
      @JacksonInject StoragePluginRegistry plugins) {
    super(userName);
    this.sql = sql;
    this.columns = columns;
    this.scanSpec = scanSpec;
    this.plugin = plugins.resolve(config, PhoenixStoragePlugin.class);
  }

  public PhoenixGroupScan(String userName, PhoenixScanSpec scanSpec, PhoenixStoragePlugin plugin) {
    super(userName);
    this.sql = scanSpec.getSql();
    this.columns = ALL_COLUMNS;
    this.scanSpec = scanSpec;
    this.plugin = plugin;
  }

  public PhoenixGroupScan(PhoenixGroupScan scan) {
    super(scan);
    this.sql = scan.sql;
    this.columns = scan.columns;
    this.scanSpec = scan.scanSpec;
    this.plugin = scan.plugin;
  }

  public PhoenixGroupScan(PhoenixGroupScan scan, List<SchemaPath> columns) {
    super(scan);
    this.sql = scan.sql;
    this.columns = columns;
    this.scanSpec = scan.scanSpec;
    this.plugin = scan.plugin;
  }

  public PhoenixGroupScan(String user, String sql, List<SchemaPath> columns, PhoenixScanSpec scanSpec,
                          PhoenixStoragePlugin plugin) {
    super(user);
    this.sql = sql;
    this.columns = columns;
    this.scanSpec = scanSpec;
    this.plugin = plugin;
  }

  @JsonProperty("sql")
  public String sql() {
    return sql;
  }

  @JsonProperty("columns")
  public List<SchemaPath> columns() {
    return columns;
  }

  @JsonProperty("scanSpec")
  public PhoenixScanSpec scanSpec() {
    return scanSpec;
  }

  @JsonIgnore
  public PhoenixStoragePlugin plugin() {
    return plugin;
  }

  @JsonProperty("config")
  public StoragePluginConfig config() {
    return plugin.getConfig();
  }

  @Override
  public void applyAssignments(List<DrillbitEndpoint> endpoints) throws PhysicalOperatorSetupException {  }

  @Override
  public SubScan getSpecificScan(int minorFragmentId) throws ExecutionSetupException {
    return new PhoenixSubScan(userName, sql, columns, scanSpec, plugin);
  }

  @Override
  public int getMaxParallelizationWidth() {
    return 1;
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) throws ExecutionSetupException {
    return new PhoenixGroupScan(this);
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    return new PhoenixGroupScan(this, columns);
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = Objects.hash(sql, columns, scanSpec, plugin.getConfig());
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
    PhoenixGroupScan groupScan = (PhoenixGroupScan) obj;
    return Objects.equals(sql, groupScan.sql())
        && Objects.equals(columns, groupScan.columns())
        && Objects.equals(scanSpec, groupScan.scanSpec())
        && Objects.equals(plugin.getConfig(), groupScan.config());
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("sql", sql)
      .field("columns", columns)
      .field("scanSpec", scanSpec)
      .field("config", plugin.getConfig())
      .toString();
  }
}
