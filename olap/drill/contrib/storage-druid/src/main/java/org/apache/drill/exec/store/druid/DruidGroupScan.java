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
package org.apache.drill.exec.store.druid;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.store.StoragePluginRegistry;

import org.apache.drill.exec.store.schedule.AffinityCreator;
import org.apache.drill.exec.store.schedule.AssignmentCreator;
import org.apache.drill.exec.store.schedule.CompleteWork;
import org.apache.drill.exec.store.schedule.EndpointByteMap;
import org.apache.drill.exec.store.schedule.EndpointByteMapImpl;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("druid-scan")
public class DruidGroupScan extends AbstractGroupScan {

  private static final Logger logger = LoggerFactory.getLogger(DruidGroupScan.class);
  private static final long DEFAULT_TABLET_SIZE = 1000;
  private final DruidScanSpec scanSpec;
  private final DruidStoragePlugin storagePlugin;

  private List<SchemaPath> columns;
  private boolean filterPushedDown = false;
  private int maxRecordsToRead;
  private List<DruidWork> druidWorkList = new ArrayList<>();
  private ListMultimap<Integer,DruidWork> assignments;
  private List<EndpointAffinity> affinities;

  @JsonCreator
  public DruidGroupScan(@JsonProperty("userName") String userName,
                        @JsonProperty("scanSpec") DruidScanSpec scanSpec,
                        @JsonProperty("storagePluginConfig") DruidStoragePluginConfig storagePluginConfig,
                        @JsonProperty("columns") List<SchemaPath> columns,
                        @JsonProperty("maxRecordsToRead") int maxRecordsToRead,
                        @JacksonInject StoragePluginRegistry pluginRegistry) {
    this(userName,
        pluginRegistry.resolve(storagePluginConfig, DruidStoragePlugin.class),
        scanSpec,
        columns,
        maxRecordsToRead);
  }

  public DruidGroupScan(String userName,
                        DruidStoragePlugin storagePlugin,
                        DruidScanSpec scanSpec,
                        List<SchemaPath> columns,
                        int maxRecordsToRead) {
    super(userName);
    this.storagePlugin = storagePlugin;
    this.scanSpec = scanSpec;
    this.columns = columns == null || columns.size() == 0? ALL_COLUMNS : columns;
    this.maxRecordsToRead = maxRecordsToRead;
    init();
  }

  /**
   * Private constructor, used for cloning.
   * @param that The DruidGroupScan to clone
   */
  private DruidGroupScan(DruidGroupScan that) {
    super(that);
    this.columns = that.columns;
    this.maxRecordsToRead = that.maxRecordsToRead;
    this.scanSpec = that.scanSpec;
    this.storagePlugin = that.storagePlugin;
    this.filterPushedDown = that.filterPushedDown;
    this.druidWorkList = that.druidWorkList;
    this.assignments = that.assignments;
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    DruidGroupScan newScan = new DruidGroupScan(this);
    newScan.columns = columns;
    return newScan;
  }

  public GroupScan clone(int maxRecordsToRead) {
    DruidGroupScan newScan = new DruidGroupScan(this);
    newScan.maxRecordsToRead = maxRecordsToRead;
    return newScan;
  }

  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    if (affinities == null) {
      affinities = AffinityCreator.getAffinityMap(druidWorkList);
    }
    return affinities;
  }

  @Override
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return true;
  }

  @Override
  public boolean supportsLimitPushdown() {
    return true;
  }

  @Override
  public GroupScan applyLimit(int maxRecords) {
    if (maxRecordsToRead == maxRecords) {
      return null;
    }
    return clone(maxRecords);
  }

  @JsonIgnore
  public boolean isFilterPushedDown() {
    return filterPushedDown;
  }

  @JsonIgnore
  public void setFilterPushedDown(boolean filterPushedDown) {
    this.filterPushedDown = filterPushedDown;
  }

  private void init() {
    logger.debug("Adding Druid Work for Table - {}. Filter - {}", getTableName(), getScanSpec().getFilter());

    DruidWork druidWork =
      new DruidWork(
        new DruidSubScan.DruidSubScanSpec(
          getTableName(),
          getScanSpec().getFilter(),
          getDatasourceSize(),
          getDataSourceMinTime(),
          getDataSourceMaxTime()
        )
      );
    druidWorkList.add(druidWork);
  }

  private static class DruidWork implements CompleteWork {
    private final EndpointByteMapImpl byteMap = new EndpointByteMapImpl();
    private final DruidSubScan.DruidSubScanSpec druidSubScanSpec;

    public DruidWork(DruidSubScan.DruidSubScanSpec druidSubScanSpec) {
      this.druidSubScanSpec = druidSubScanSpec;
    }

    public DruidSubScan.DruidSubScanSpec getDruidSubScanSpec() {
      return druidSubScanSpec;
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

  public ScanStats getScanStats() {
    long recordCount = 100000L * druidWorkList.size();
    return new ScanStats(
        ScanStats.GroupScanProperty.NO_EXACT_ROW_COUNT,
        recordCount,
        1,
        recordCount * storagePlugin.getConfig().getAverageRowSizeBytes());
  }

  @Override
  public void applyAssignments(List<CoordinationProtos.DrillbitEndpoint> endpoints) {
    assignments = AssignmentCreator.getMappings(endpoints, druidWorkList);
  }

  @Override
  public DruidSubScan getSpecificScan(int minorFragmentId) {

    List<DruidWork> workList = assignments.get(minorFragmentId);

    List<DruidSubScan.DruidSubScanSpec> scanSpecList = Lists.newArrayList();
    for (DruidWork druidWork : workList) {
      scanSpecList
        .add(
          new DruidSubScan.DruidSubScanSpec(
            druidWork.getDruidSubScanSpec().getDataSourceName(),
            druidWork.getDruidSubScanSpec().getFilter(),
            druidWork.getDruidSubScanSpec().getDataSourceSize(),
            druidWork.getDruidSubScanSpec().getMinTime(),
            druidWork.getDruidSubScanSpec().getMaxTime()
          )
        );
    }

    return new DruidSubScan(getUserName(), storagePlugin, scanSpecList, this.columns, this.maxRecordsToRead);
  }

  @JsonIgnore
  public String getTableName() {
    return getScanSpec().getDataSourceName();
  }

  @JsonIgnore
  public long getDatasourceSize() {
    return getScanSpec().getDataSourceSize();
  }

  @JsonIgnore
  public String getDataSourceMinTime() {
    return getScanSpec().getDataSourceMinTime();
  }

  @JsonIgnore
  public String getDataSourceMaxTime() {
    return getScanSpec().getDataSourceMaxTime();
  }

  @Override
  public int getMaxParallelizationWidth() {
    return druidWorkList.size();
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @JsonProperty("druidScanSpec")
  public DruidScanSpec getScanSpec() {
    return scanSpec;
  }

  @JsonIgnore
  public DruidStoragePlugin getStoragePlugin() {
    return storagePlugin;
  }

  @JsonProperty
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty
  public int getMaxRecordsToRead() {
    return maxRecordsToRead;
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("druidScanSpec", scanSpec)
        .field("columns", columns)
        .field("druidStoragePlugin", storagePlugin)
        .toString();
  }

  @Override
  @JsonIgnore
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new DruidGroupScan(this);
  }
}
