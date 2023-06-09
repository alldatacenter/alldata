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

import java.io.IOException;

import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractWriter;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.store.StoragePluginRegistry;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JdbcWriter extends AbstractWriter {

  public static final String OPERATOR_TYPE = "JDBC_WRITER";

  private final JdbcStoragePlugin plugin;
  private final String tableName;
  private final JdbcSchema inner;

  @JsonCreator
  public JdbcWriter(
    @JsonProperty("child") PhysicalOperator child,
    @JsonProperty("name") String name,
    @JsonProperty("storage") StoragePluginConfig storageConfig,
    @JacksonInject JdbcSchema inner,
    @JacksonInject StoragePluginRegistry engineRegistry) throws IOException, ExecutionSetupException {
    super(child);
    this.plugin = engineRegistry.resolve(storageConfig, JdbcStoragePlugin.class);
    this.tableName = name;
    this.inner = inner;
  }

  JdbcWriter(PhysicalOperator child, String name, JdbcSchema inner, JdbcStoragePlugin plugin) {
    super(child);
    this.tableName = name;
    this.plugin = plugin;
    this.inner = inner;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new JdbcWriter(child, tableName, inner, plugin);
  }

  public String getTableName() {
    return tableName;
  }

  public StoragePluginConfig getStorage() {
    return plugin.getConfig();
  }

  @JsonIgnore
  public JdbcSchema getInner() { return inner; }

  @JsonIgnore
  public JdbcStoragePlugin getPlugin() {
    return plugin;
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("tableName", tableName)
      .field("storageStrategy", getStorageStrategy())
      .toString();
  }
}
