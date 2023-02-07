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
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.openTSDB.OpenTSDBSubScan.OpenTSDBSubScanSpec;
import org.apache.drill.exec.store.openTSDB.client.services.ServiceImpl;
import org.apache.drill.exec.store.openTSDB.dto.MetricDTO;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.drill.exec.store.openTSDB.Util.fromRowData;

@JsonTypeName("openTSDB-scan")
public class OpenTSDBGroupScan extends AbstractGroupScan {

  private final OpenTSDBStoragePluginConfig storagePluginConfig;
  private final OpenTSDBScanSpec openTSDBScanSpec;
  private final OpenTSDBStoragePlugin storagePlugin;

  private List<SchemaPath> columns;

  @JsonCreator
  public OpenTSDBGroupScan(@JsonProperty("openTSDBScanSpec") OpenTSDBScanSpec openTSDBScanSpec,
                           @JsonProperty("storage") OpenTSDBStoragePluginConfig openTSDBStoragePluginConfig,
                           @JsonProperty("columns") List<SchemaPath> columns,
                           @JacksonInject StoragePluginRegistry pluginRegistry) throws IOException, ExecutionSetupException {
    this(pluginRegistry.resolve(openTSDBStoragePluginConfig, OpenTSDBStoragePlugin.class),
        openTSDBScanSpec, columns);
  }

  public OpenTSDBGroupScan(OpenTSDBStoragePlugin storagePlugin,
                           OpenTSDBScanSpec scanSpec, List<SchemaPath> columns) {
    super((String) null);
    this.storagePlugin = storagePlugin;
    this.storagePluginConfig = storagePlugin.getConfig();
    this.openTSDBScanSpec = scanSpec;
    this.columns = columns == null || columns.size() == 0 ? ALL_COLUMNS : columns;
  }

  /**
   * Private constructor, used for cloning.
   *
   * @param that The OpenTSDBGroupScan to clone
   */
  private OpenTSDBGroupScan(OpenTSDBGroupScan that) {
    super((String) null);
    this.columns = that.columns;
    this.openTSDBScanSpec = that.openTSDBScanSpec;
    this.storagePlugin = that.storagePlugin;
    this.storagePluginConfig = that.storagePluginConfig;
  }

  @Override
  public int getMaxParallelizationWidth() {
    return 1;
  }

  @Override
  public void applyAssignments(List<DrillbitEndpoint> incomingEndpoints) {
  }

  @Override
  public OpenTSDBSubScan getSpecificScan(int minorFragmentId) {
    List<OpenTSDBSubScanSpec> scanSpecList = Lists.newArrayList();
    scanSpecList.add(new OpenTSDBSubScanSpec(getTableName()));
    return new OpenTSDBSubScan(storagePlugin, storagePluginConfig, scanSpecList, this.columns);
  }

  @Override
  public ScanStats getScanStats() {
    ServiceImpl client = storagePlugin.getClient();
    Map<String, String> params = fromRowData(openTSDBScanSpec.getTableName());
    Set<MetricDTO> allMetrics = client.getAllMetrics(params);
    long numMetrics = allMetrics.size();
    float approxDiskCost = 0;
    if (numMetrics != 0) {
      MetricDTO metricDTO  = allMetrics.iterator().next();
      // This method estimates the sizes of Java objects (number of bytes of memory they occupy).
      // more detailed information about how this estimation method work you can find in this article
      // http://www.javaworld.com/javaworld/javaqa/2003-12/02-qa-1226-sizeof.html
      approxDiskCost = SizeEstimator.estimate(metricDTO) * numMetrics;
    }
    return new ScanStats(ScanStats.GroupScanProperty.EXACT_ROW_COUNT, numMetrics, 1, approxDiskCost);
  }

  @Override
  @JsonIgnore
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new OpenTSDBGroupScan(this);
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  @JsonIgnore
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return true;
  }

  @JsonIgnore
  public String getTableName() {
    return getOpenTSDBScanSpec().getTableName();
  }

  @JsonProperty
  public OpenTSDBScanSpec getOpenTSDBScanSpec() {
    return openTSDBScanSpec;
  }

  @JsonProperty("storage")
  public OpenTSDBStoragePluginConfig getStoragePluginConfig() {
    return storagePluginConfig;
  }

  @Override
  @JsonProperty
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    OpenTSDBGroupScan newScan = new OpenTSDBGroupScan(this);
    newScan.columns = columns;
    return newScan;
  }

  @Override
  public String toString() {
    return "OpenTSDBGroupScan [OpenTSDBScanSpec=" + openTSDBScanSpec + ", columns=" + columns
            + "]";
  }
}
