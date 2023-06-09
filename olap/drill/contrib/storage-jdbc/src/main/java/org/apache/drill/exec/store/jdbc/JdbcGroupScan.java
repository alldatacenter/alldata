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
package org.apache.drill.exec.store.jdbc;

import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.ScanStats.GroupScanProperty;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.StoragePluginRegistry;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("jdbc-scan")
public class JdbcGroupScan extends AbstractGroupScan {

  private final String sql;
  private final List<SchemaPath> columns;
  private final JdbcStoragePlugin plugin;
  private final double rows;

  @JsonCreator
  public JdbcGroupScan(
      @JsonProperty("sql") String sql,
      @JsonProperty("columns") List<SchemaPath> columns,
      @JsonProperty("config") StoragePluginConfig config,
      @JsonProperty("rows") double rows,
      @JacksonInject StoragePluginRegistry plugins) throws ExecutionSetupException {
    super("");
    this.sql = sql;
    this.columns = columns;
    this.plugin = plugins.resolve(config, JdbcStoragePlugin.class);
    this.rows = rows;
  }

  JdbcGroupScan(String sql, List<SchemaPath> columns, JdbcStoragePlugin plugin, double rows) {
    super("");
    this.sql = sql;
    this.columns = columns;
    this.plugin = plugin;
    this.rows = rows;
  }

  @Override
  public void applyAssignments(List<DrillbitEndpoint> endpoints) {
  }

  @Override
  public SubScan getSpecificScan(int minorFragmentId) {
    return new JdbcSubScan(sql, columns, plugin);
  }

  @Override
  public int getMaxParallelizationWidth() {
    return 1;
  }

  @Override
  public ScanStats getScanStats() {
    return new ScanStats(
        GroupScanProperty.NO_EXACT_ROW_COUNT,
        (long) Math.max(rows, 1),
        1,
        1);
  }

  public String getSql() {
    return sql;
  }

  @Override
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @Override
  public String getDigest() {
    return sql + plugin.getConfig();
  }

  public StoragePluginConfig getConfig() {
    return plugin.getConfig();
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    return new JdbcGroupScan(sql, columns, plugin, rows);
  }
}
