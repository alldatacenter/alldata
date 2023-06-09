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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.collect.ListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.ScanStats.GroupScanProperty;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.kudu.KuduSubScan.KuduSubScanSpec;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.exec.store.schedule.AffinityCreator;
import org.apache.drill.exec.store.schedule.AssignmentCreator;
import org.apache.drill.exec.store.schedule.CompleteWork;
import org.apache.drill.exec.store.schedule.EndpointByteMap;
import org.apache.drill.exec.store.schedule.EndpointByteMapImpl;
import org.apache.kudu.client.LocatedTablet;
import org.apache.kudu.client.LocatedTablet.Replica;

@JsonTypeName("kudu-scan")
public class KuduGroupScan extends AbstractGroupScan {
  private static final long DEFAULT_TABLET_SIZE = 1000;

  private KuduStoragePlugin kuduStoragePlugin;
  private List<SchemaPath> columns;
  private KuduScanSpec kuduScanSpec;

  private boolean filterPushedDown = false;
  private List<KuduWork> kuduWorkList = Lists.newArrayList();
  private ListMultimap<Integer,KuduWork> assignments;
  private List<EndpointAffinity> affinities;


  @JsonCreator
  public KuduGroupScan(@JsonProperty("kuduScanSpec") KuduScanSpec kuduScanSpec,
                        @JsonProperty("kuduStoragePluginConfig") KuduStoragePluginConfig kuduStoragePluginConfig,
                        @JsonProperty("columns") List<SchemaPath> columns,
                        @JacksonInject StoragePluginRegistry pluginRegistry) throws IOException, ExecutionSetupException {
    this(pluginRegistry.resolve(kuduStoragePluginConfig, KuduStoragePlugin.class), kuduScanSpec, columns);
  }

  public KuduGroupScan(KuduStoragePlugin kuduStoragePlugin,
                       KuduScanSpec kuduScanSpec,
                       List<SchemaPath> columns) {
    super((String) null);
    this.kuduStoragePlugin = kuduStoragePlugin;
    this.kuduScanSpec = kuduScanSpec;
    this.columns = columns == null || columns.size() == 0? ALL_COLUMNS : columns;
    init();
  }

  private void init() {
    String tableName = kuduScanSpec.getTableName();
    Collection<DrillbitEndpoint> endpoints = kuduStoragePlugin.getContext().getBits();
    Map<String,DrillbitEndpoint> endpointMap = Maps.newHashMap();
    for (DrillbitEndpoint endpoint : endpoints) {
      endpointMap.put(endpoint.getAddress(), endpoint);
    }
    try {
      List<LocatedTablet> locations = kuduStoragePlugin.getClient().openTable(tableName).getTabletsLocations(10000);
      for (LocatedTablet tablet : locations) {
        KuduWork work = new KuduWork(tablet.getPartition().getPartitionKeyStart(), tablet.getPartition().getPartitionKeyEnd());
        for (Replica replica : tablet.getReplicas()) {
          String host = replica.getRpcHost();
          DrillbitEndpoint ep = endpointMap.get(host);
          if (ep != null) {
            work.getByteMap().add(ep, DEFAULT_TABLET_SIZE);
          }
        }
        kuduWorkList.add(work);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static class KuduWork implements CompleteWork {

    private final EndpointByteMapImpl byteMap = new EndpointByteMapImpl();
    private final byte[] partitionKeyStart;
    private final byte[] partitionKeyEnd;

    public KuduWork(byte[] partitionKeyStart, byte[] partitionKeyEnd) {
      this.partitionKeyStart = partitionKeyStart;
      this.partitionKeyEnd = partitionKeyEnd;
    }

    public byte[] getPartitionKeyStart() {
      return partitionKeyStart;
    }

    public byte[] getPartitionKeyEnd() {
      return partitionKeyEnd;
    }

    @Override
    public long getTotalBytes() {
      return DEFAULT_TABLET_SIZE;
    }

    @Override
    public EndpointByteMap getByteMap() {
      return byteMap;
    }

    @Override
    public int compareTo(CompleteWork o) {
      return 0;
    }
  }

  /**
   * Private constructor, used for cloning.
   * @param that The KuduGroupScan to clone
   */
  private KuduGroupScan(KuduGroupScan that) {
    super(that);
    this.kuduStoragePlugin = that.kuduStoragePlugin;
    this.columns = that.columns;
    this.kuduScanSpec = that.kuduScanSpec;
    this.filterPushedDown = that.filterPushedDown;
    this.kuduWorkList = that.kuduWorkList;
    this.assignments = that.assignments;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    KuduGroupScan newScan = new KuduGroupScan(this);
    newScan.columns = columns;
    return newScan;
  }

  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    if (affinities == null) {
      affinities = AffinityCreator.getAffinityMap(kuduWorkList);
    }
    return affinities;
  }


  @Override
  public int getMaxParallelizationWidth() {
    return kuduWorkList.size();
  }


  /**
   *
   * @param incomingEndpoints
   */
  @Override
  public void applyAssignments(List<DrillbitEndpoint> incomingEndpoints) {
    assignments = AssignmentCreator.getMappings(incomingEndpoints, kuduWorkList);
  }


  @Override
  public KuduSubScan getSpecificScan(int minorFragmentId) {
    List<KuduWork> workList = assignments.get(minorFragmentId);

    List<KuduSubScanSpec> scanSpecList = Lists.newArrayList();

    for (KuduWork work : workList) {
      scanSpecList.add(new KuduSubScanSpec(getTableName(), work.getPartitionKeyStart(), work.getPartitionKeyEnd()));
    }

    return new KuduSubScan(kuduStoragePlugin, scanSpecList, this.columns);
  }

  // KuduStoragePlugin plugin, KuduStoragePluginConfig config,
  // List<KuduSubScanSpec> tabletInfoList, List<SchemaPath> columns
  @Override
  public ScanStats getScanStats() {
    long recordCount = 100000 * kuduWorkList.size();
    return new ScanStats(GroupScanProperty.NO_EXACT_ROW_COUNT, recordCount, 1, recordCount);
  }

  @Override
  @JsonIgnore
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new KuduGroupScan(this);
  }

  @JsonIgnore
  public KuduStoragePlugin getStoragePlugin() {
    return kuduStoragePlugin;
  }

  @JsonIgnore
  public String getTableName() {
    return getKuduScanSpec().getTableName();
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  public String toString() {
    return "KuduGroupScan [KuduScanSpec="
        + kuduScanSpec + ", columns="
        + columns + "]";
  }

  @JsonProperty
  public KuduStoragePluginConfig getKuduStoragePluginConfig() {
    return kuduStoragePlugin.getConfig();
  }

  @Override
  @JsonProperty
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty
  public KuduScanSpec getKuduScanSpec() {
    return kuduScanSpec;
  }

  @Override
  @JsonIgnore
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return true;
  }

  @JsonIgnore
  public void setFilterPushedDown(boolean b) {
    this.filterPushedDown = true;
  }

  @JsonIgnore
  public boolean isFilterPushedDown() {
    return filterPushedDown;
  }

  /**
   * Empty constructor, do not use, only for testing.
   */
  @VisibleForTesting
  public KuduGroupScan() {
    super((String)null);
  }

  /**
   * Do not use, only for testing.
   */
  @VisibleForTesting
  public void setKuduScanSpec(KuduScanSpec kuduScanSpec) {
    this.kuduScanSpec = kuduScanSpec;
  }

}
