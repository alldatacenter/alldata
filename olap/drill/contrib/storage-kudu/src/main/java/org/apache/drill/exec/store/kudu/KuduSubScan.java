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
package org.apache.drill.exec.store.kudu;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.store.StoragePluginRegistry;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Information for reading a single Kudu tablet
 */
@JsonTypeName("kudu-sub-scan")
public class KuduSubScan extends AbstractBase implements SubScan {

  public static final String OPERATOR_TYPE = "KUDU_SUB_SCAN";

  private final KuduStoragePlugin kuduStoragePlugin;
  private final List<KuduSubScanSpec> tabletScanSpecList;
  private final List<SchemaPath> columns;

  @JsonCreator
  public KuduSubScan(@JacksonInject StoragePluginRegistry registry,
                      @JsonProperty("kuduStoragePluginConfig") KuduStoragePluginConfig kuduStoragePluginConfig,
                      @JsonProperty("tabletScanSpecList") LinkedList<KuduSubScanSpec> tabletScanSpecList,
                      @JsonProperty("columns") List<SchemaPath> columns) throws ExecutionSetupException {
    super((String) null);
    kuduStoragePlugin = registry.resolve(kuduStoragePluginConfig, KuduStoragePlugin.class);
    this.tabletScanSpecList = tabletScanSpecList;
    this.columns = columns;
  }

  public KuduSubScan(KuduStoragePlugin plugin, List<KuduSubScanSpec> tabletInfoList, List<SchemaPath> columns) {
    super((String) null);
    this.kuduStoragePlugin = plugin;
    this.tabletScanSpecList = tabletInfoList;
    this.columns = columns;
  }

  public KuduStoragePluginConfig getKuduStoragePluginConfig() {
    return kuduStoragePlugin.getConfig();
  }

  public List<KuduSubScanSpec> getTabletScanSpecList() {
    return tabletScanSpecList;
  }

  public List<SchemaPath> getColumns() {
    return columns;
  }

  @Override
  public boolean isExecutable() {
    return false;
  }

  @JsonIgnore
  public KuduStoragePlugin getStorageEngine(){
    return kuduStoragePlugin;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new KuduSubScan(kuduStoragePlugin, tabletScanSpecList, columns);
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return ImmutableSet.<PhysicalOperator>of().iterator();
  }

  public static class KuduSubScanSpec {

    private final String tableName;
    private final byte[] startKey;
    private final byte[] endKey;

    @JsonCreator
    public KuduSubScanSpec(@JsonProperty("tableName") String tableName,
                           @JsonProperty("startKey") byte[] startKey,
                           @JsonProperty("endKey") byte[] endKey) {
      this.tableName = tableName;
      this.startKey = startKey;
      this.endKey = endKey;
    }

    public String getTableName() { return tableName; }

    public byte[] getStartKey() { return startKey; }

    public byte[] getEndKey() { return endKey; }
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
