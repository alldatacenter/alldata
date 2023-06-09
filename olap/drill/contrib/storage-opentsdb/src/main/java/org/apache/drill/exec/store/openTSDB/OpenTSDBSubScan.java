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
package org.apache.drill.exec.store.openTSDB;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.store.StoragePluginRegistry;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@JsonTypeName("openTSDB-sub-scan")
public class OpenTSDBSubScan extends AbstractBase implements SubScan {

  public static final String OPERATOR_TYPE = "OPEN_TSDB_SUB_SCAN";

  public final OpenTSDBStoragePluginConfig storage;

  private final List<SchemaPath> columns;
  private final OpenTSDBStoragePlugin openTSDBStoragePlugin;
  private final List<OpenTSDBSubScanSpec> tabletScanSpecList;

  @JsonCreator
  public OpenTSDBSubScan(@JacksonInject StoragePluginRegistry registry,
                         @JsonProperty("storage") OpenTSDBStoragePluginConfig storage,
                         @JsonProperty("tabletScanSpecList") LinkedList<OpenTSDBSubScanSpec> tabletScanSpecList,
                         @JsonProperty("columns") List<SchemaPath> columns) throws ExecutionSetupException {
    super((String) null);
    openTSDBStoragePlugin = registry.resolve(storage, OpenTSDBStoragePlugin.class);
    this.tabletScanSpecList = tabletScanSpecList;
    this.storage = storage;
    this.columns = columns;
  }

  public OpenTSDBSubScan(OpenTSDBStoragePlugin plugin, OpenTSDBStoragePluginConfig config,
                         List<OpenTSDBSubScanSpec> tabletInfoList, List<SchemaPath> columns) {
    super((String) null);
    openTSDBStoragePlugin = plugin;
    storage = config;
    this.tabletScanSpecList = tabletInfoList;
    this.columns = columns;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  @Override
  public boolean isExecutable() {
    return false;
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) throws ExecutionSetupException {
    Preconditions.checkArgument(children.isEmpty());
    return new OpenTSDBSubScan(openTSDBStoragePlugin, storage, tabletScanSpecList, columns);
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  public List<SchemaPath> getColumns() {
    return columns;
  }

  public List<OpenTSDBSubScanSpec> getTabletScanSpecList() {
    return tabletScanSpecList;
  }

  @JsonIgnore
  public OpenTSDBStoragePlugin getStorageEngine() {
    return openTSDBStoragePlugin;
  }

  @JsonProperty("storage")
  public OpenTSDBStoragePluginConfig getStorageConfig() {
    return storage;
  }

  public static class OpenTSDBSubScanSpec {

    private final String tableName;

    @JsonCreator
    public OpenTSDBSubScanSpec(@JsonProperty("tableName") String tableName) {
      this.tableName = tableName;
    }

    public String getTableName() {
      return tableName;
    }

  }
}
