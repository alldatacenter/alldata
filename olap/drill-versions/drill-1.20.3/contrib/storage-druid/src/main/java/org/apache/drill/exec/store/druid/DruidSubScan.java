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
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.druid.common.DruidFilter;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyIterator;

/**
 * A Class containing information to read a single druid data source.
 */
@JsonTypeName("druid-datasource-scan")
public class DruidSubScan extends AbstractBase implements SubScan {

  public static final String OPERATOR_TYPE = "DRUID_SUB_SCAN";

  @JsonIgnore
  private final DruidStoragePlugin druidStoragePlugin;
  private final List<DruidSubScanSpec> scanSpec;
  private final List<SchemaPath> columns;
  private final int maxRecordsToRead;

  @JsonCreator
  public DruidSubScan(@JacksonInject StoragePluginRegistry registry,
                      @JsonProperty("userName") String userName,
                      @JsonProperty("config") StoragePluginConfig config,
                      @JsonProperty("scanSpec") LinkedList<DruidSubScanSpec> datasourceScanSpecList,
                      @JsonProperty("columns") List<SchemaPath> columns,
                      @JsonProperty("maxRecordsToRead") int maxRecordsToRead) {
    super(userName);
    druidStoragePlugin = registry.resolve(config, DruidStoragePlugin.class);
    this.scanSpec = datasourceScanSpecList;
    this.columns = columns;
    this.maxRecordsToRead = maxRecordsToRead;
  }

  public DruidSubScan(String userName,
                      DruidStoragePlugin plugin,
                      List<DruidSubScanSpec> dataSourceInfoList,
                      List<SchemaPath> columns,
                      int maxRecordsToRead) {
    super(userName);
    this.druidStoragePlugin = plugin;
    this.scanSpec = dataSourceInfoList;
    this.columns = columns;
    this.maxRecordsToRead = maxRecordsToRead;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  @JsonIgnore
  public List<DruidSubScanSpec> getScanSpec() {
    return scanSpec;
  }

  public List<SchemaPath> getColumns() {
    return columns;
  }

  public int getMaxRecordsToRead() { return maxRecordsToRead; }

  @JsonIgnore
  @Override
  public boolean isExecutable() {
    return false;
  }

  @JsonIgnore
  public DruidStoragePlugin getStorageEngine(){
    return druidStoragePlugin;
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new DruidSubScan(getUserName(), druidStoragePlugin, scanSpec, columns, maxRecordsToRead);
  }

  @JsonIgnore
  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return emptyIterator();
  }

  public static class DruidSubScanSpec {

    protected final String dataSourceName;
    protected final DruidFilter filter;
    protected final long dataSourceSize;
    protected final String maxTime;
    protected final String minTime;

    @JsonCreator
    public DruidSubScanSpec(@JsonProperty("dataSourceName") String dataSourceName,
                            @JsonProperty("filter") DruidFilter filter,
                            @JsonProperty("dataSourceSize") long dataSourceSize,
                            @JsonProperty("minTime") String minTime,
                            @JsonProperty("maxTime") String maxTime) {
      this.dataSourceName = dataSourceName;
      this.filter = filter;
      this.dataSourceSize = dataSourceSize;
      this.minTime = minTime;
      this.maxTime = maxTime;
    }

    public String getDataSourceName() {
      return dataSourceName;
    }

    public DruidFilter getFilter() { return filter; }

    public long getDataSourceSize() {
      return dataSourceSize;
    }

    public String getMinTime() {
      return minTime;
    }

    public String getMaxTime() {
      return maxTime;
    }

    @Override
    public String toString() {
      return new PlanStringBuilder(this)
        .field("dataSourceName", dataSourceName)
        .field("filter", filter)
        .field("dataSourceSize", dataSourceSize)
        .field("minTime", minTime)
        .field("maxTime", maxTime)
        .toString();
    }
  }
}
