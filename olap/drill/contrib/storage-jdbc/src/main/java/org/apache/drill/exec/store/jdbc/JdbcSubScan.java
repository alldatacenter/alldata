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

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractSubScan;
import org.apache.drill.exec.store.StoragePluginRegistry;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeName("jdbc-sub-scan")
public class JdbcSubScan extends AbstractSubScan {

  public static final String OPERATOR_TYPE = "JDBC_SCAN";

  private final String sql;
  private final JdbcStoragePlugin plugin;
  private final List<SchemaPath> columns;

  @JsonCreator
  public JdbcSubScan(
      @JsonProperty("sql") String sql,
      @JsonProperty("columns") List<SchemaPath> columns,
      @JsonProperty("config") StoragePluginConfig config,
      @JacksonInject StoragePluginRegistry plugins) throws ExecutionSetupException {
    super("");
    this.sql = sql;
    this.columns = columns;
    this.plugin = plugins.resolve(config, JdbcStoragePlugin.class);
  }

  JdbcSubScan(String sql, List<SchemaPath> columns, JdbcStoragePlugin plugin) {
    super("");
    this.sql = sql;
    this.columns = columns;
    this.plugin = plugin;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  public String getSql() {
    return sql;
  }

  public List<SchemaPath> getColumns() {
    return columns;
  }

  public JdbcStorageConfig getConfig() {
    return plugin.getConfig();
  }

  @JsonIgnore
  public JdbcStoragePlugin getPlugin() {
    return plugin;
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("sql", sql)
      .field("columns", columns)
      .toString();
  }
}
