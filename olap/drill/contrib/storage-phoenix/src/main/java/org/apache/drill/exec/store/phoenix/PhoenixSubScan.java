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

import java.util.Iterator;
import java.util.List;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("phoenix-sub-scan")
public class PhoenixSubScan extends AbstractBase implements SubScan {

  private final String sql;
  private final List<SchemaPath> columns;
  private final PhoenixScanSpec scanSpec;
  private final PhoenixStoragePlugin plugin;

  @JsonCreator
  public PhoenixSubScan(
      @JsonProperty("userName") String userName,
      @JsonProperty("sql") String sql,
      @JsonProperty("columns") List<SchemaPath> columns,
      @JsonProperty("scanSpec") PhoenixScanSpec scanSpec,
      @JsonProperty("config") StoragePluginConfig config,
      @JacksonInject StoragePluginRegistry registry) {
    super(userName);
    this.sql = sql;
    this.columns = columns;
    this.scanSpec = scanSpec;
    this.plugin = registry.resolve(config, PhoenixStoragePlugin.class);
  }

  public PhoenixSubScan(String userName, String sql, List<SchemaPath> columns, PhoenixScanSpec scanSpec, PhoenixStoragePlugin plugin) {
    super(userName);
    this.sql = sql;
    this.columns = columns;
    this.scanSpec = scanSpec;
    this.plugin = plugin;
  }

  @JsonProperty("sql")
  public String getSql() {
    return sql;
  }

  @JsonProperty("columns")
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty("scanSpec")
  public PhoenixScanSpec getScanSpec() {
    return scanSpec;
  }

  @JsonIgnore
  public PhoenixStoragePlugin getPlugin() {
    return plugin;
  }

  @JsonProperty("config")
  public StoragePluginConfig getConfig() {
    return plugin.getConfig();
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    return new PhoenixSubScan(userName, sql, columns, scanSpec, plugin);
  }

  @Override
  public String getOperatorType() {
    return PhoenixStoragePluginConfig.NAME.toUpperCase();
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return ImmutableSet.<PhysicalOperator>of().iterator();
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
