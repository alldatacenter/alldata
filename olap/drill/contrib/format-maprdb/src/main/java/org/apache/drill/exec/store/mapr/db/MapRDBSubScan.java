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
package org.apache.drill.exec.store.mapr.db;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractDbSubScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.StoragePluginRegistry;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

// Class containing information for reading a single HBase region
@JsonTypeName("maprdb-sub-scan")

public class MapRDBSubScan extends AbstractDbSubScan {

  public static final String OPERATOR_TYPE = "MAPRDB_SUB_SCAN";

  private final MapRDBFormatPlugin formatPlugin;
  private final List<MapRDBSubScanSpec> regionScanSpecList;
  private final List<SchemaPath> columns;
  private final int maxRecordsToRead;
  private final String tableType;
  private final TupleMetadata schema;

  @JsonCreator
  public MapRDBSubScan(@JacksonInject StoragePluginRegistry engineRegistry,
                       @JsonProperty("userName") String userName,
                       @JsonProperty("formatPluginConfig") MapRDBFormatPluginConfig formatPluginConfig,
                       @JsonProperty("storageConfig") StoragePluginConfig storageConfig,
                       @JsonProperty("regionScanSpecList") List<MapRDBSubScanSpec> regionScanSpecList,
                       @JsonProperty("columns") List<SchemaPath> columns,
                       @JsonProperty("maxRecordsToRead") int maxRecordsToRead,
                       @JsonProperty("tableType") String tableType,
                       @JsonProperty("schema") TupleMetadata schema) throws ExecutionSetupException {
    this(userName,
        engineRegistry.resolveFormat(storageConfig, formatPluginConfig, MapRDBFormatPlugin.class),
        regionScanSpecList,
        columns,
        maxRecordsToRead,
        tableType,
        schema);
  }

  public MapRDBSubScan(String userName, MapRDBFormatPlugin formatPlugin,
      List<MapRDBSubScanSpec> maprSubScanSpecs, List<SchemaPath> columns, String tableType, TupleMetadata schema) {
    this(userName, formatPlugin, maprSubScanSpecs, columns, -1, tableType, schema);
  }

  public MapRDBSubScan(String userName, MapRDBFormatPlugin formatPlugin,
                       List<MapRDBSubScanSpec> maprSubScanSpecs, List<SchemaPath> columns, int maxRecordsToRead, String tableType, TupleMetadata schema) {
    super(userName);
    this.formatPlugin = formatPlugin;
    this.regionScanSpecList = maprSubScanSpecs;
    this.columns = columns;
    this.maxRecordsToRead = maxRecordsToRead;
    this.tableType = tableType;
    this.schema = schema;
  }


  @JsonProperty("formatPluginConfig")
  public MapRDBFormatPluginConfig getFormatPluginConfig() {
    return formatPlugin.getConfig();
  }

  @JsonProperty("storageConfig")
  public StoragePluginConfig getStorageConfig(){
    return formatPlugin.getStorageConfig();
  }

  @JsonProperty("regionScanSpecList")
  public List<MapRDBSubScanSpec> getRegionScanSpecList() {
    return regionScanSpecList;
  }

  @JsonProperty("columns")
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty("maxRecordsToRead")
  public int getMaxRecordsToRead() {
    return maxRecordsToRead;
  }

  @JsonProperty("tableType")
  public String getTableType() {
    return tableType;
  }

  @JsonProperty("schema")
  public TupleMetadata getSchema() {
    return schema;
  }

  @Override
  public boolean isExecutable() {
    return false;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new MapRDBSubScan(getUserName(), formatPlugin, regionScanSpecList, columns, tableType, schema);
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  @JsonIgnore
  public MapRDBFormatPlugin getFormatPlugin() {
    return formatPlugin;
  }

}
