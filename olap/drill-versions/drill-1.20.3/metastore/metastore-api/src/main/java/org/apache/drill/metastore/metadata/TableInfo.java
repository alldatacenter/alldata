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
package org.apache.drill.metastore.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.expressions.FilterExpression;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * General table information.
 */
@JsonTypeName("tableInfo")
@JsonDeserialize(builder = TableInfo.TableInfoBuilder.class)
public class TableInfo {
  public static final String UNKNOWN = "UNKNOWN";
  public static final TableInfo UNKNOWN_TABLE_INFO = TableInfo.builder()
      .storagePlugin(UNKNOWN)
      .workspace(UNKNOWN)
      .name(UNKNOWN)
      .type(UNKNOWN)
      .owner(UNKNOWN)
      .build();

  private final String storagePlugin;
  private final String workspace;
  private final String name;
  private final String type;
  private final String owner;

  private TableInfo(TableInfoBuilder builder) {
    this.storagePlugin = builder.storagePlugin;
    this.workspace = builder.workspace;
    this.name = builder.name;
    this.type = builder.type;
    this.owner = builder.owner;
  }

  @JsonProperty
  public String storagePlugin() {
    return storagePlugin;
  }

  @JsonProperty
  public String workspace() {
    return workspace;
  }

  @JsonProperty
  public String name() {
    return name;
  }

  @JsonProperty
  public String type() {
    return type;
  }

  @JsonProperty
  public String owner() {
    return owner;
  }

  public FilterExpression toFilter() {
    FilterExpression storagePluginFilter = FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, storagePlugin);
    FilterExpression workspaceFilter = FilterExpression.equal(MetastoreColumn.WORKSPACE, workspace);
    FilterExpression tableNameFilter = FilterExpression.equal(MetastoreColumn.TABLE_NAME, name);
    return FilterExpression.and(storagePluginFilter, workspaceFilter, tableNameFilter);
  }

  public void toMetadataUnitBuilder(TableMetadataUnit.Builder builder) {
    builder.storagePlugin(storagePlugin);
    builder.workspace(workspace);
    builder.tableName(name);
    builder.tableType(type);
    builder.owner(owner);
  }

  @Override
  public int hashCode() {
    return Objects.hash(storagePlugin, workspace, name, type, owner);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableInfo tableInfo = (TableInfo) o;
    return Objects.equals(storagePlugin, tableInfo.storagePlugin)
      && Objects.equals(workspace, tableInfo.workspace)
      && Objects.equals(name, tableInfo.name)
      && Objects.equals(type, tableInfo.type)
      && Objects.equals(owner, tableInfo.owner);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", TableInfo.class.getSimpleName() + "[", "]")
      .add("storagePlugin=" + storagePlugin)
      .add("workspace=" + workspace)
      .add("name=" + name)
      .add("type=" + type)
      .add("owner=" + owner)
      .toString();
  }

  public static TableInfoBuilder builder() {
    return new TableInfoBuilder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class TableInfoBuilder {
    private String storagePlugin;
    private String workspace;
    private String name;
    private String type;
    private String owner;

    public TableInfoBuilder storagePlugin(String storagePlugin) {
      this.storagePlugin = storagePlugin;
      return this;
    }

    public TableInfoBuilder workspace(String workspace) {
      this.workspace = workspace;
      return this;
    }

    public TableInfoBuilder name(String name) {
      // removes leading slash characters since such table names are equivalent
      this.name = DrillStringUtils.removeLeadingSlash(name);
      return this;
    }

    public TableInfoBuilder type(String type) {
      this.type = type;
      return this;
    }

    public TableInfoBuilder owner(String owner) {
      this.owner = owner;
      return this;
    }

    public TableInfoBuilder metadataUnit(TableMetadataUnit unit) {
      return storagePlugin(unit.storagePlugin())
        .workspace(unit.workspace())
        .name(unit.tableName())
        .type(unit.tableType())
        .owner(unit.owner());
    }

    public TableInfo build() {
      Objects.requireNonNull(storagePlugin, "storagePlugin was not set");
      Objects.requireNonNull(workspace, "workspace was not set");
      Objects.requireNonNull(name, "name was not set");
      return new TableInfo(this);
    }

  }
}
