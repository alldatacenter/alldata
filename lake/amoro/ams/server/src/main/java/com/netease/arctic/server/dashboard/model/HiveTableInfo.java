/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.dashboard.model;

import com.netease.arctic.table.TableIdentifier;

import java.util.List;
import java.util.Map;

public class HiveTableInfo {
  private TableIdentifier tableIdentifier;
  private TableMeta.TableType tableType;
  private List<AMSColumnInfo> schema;
  private List<AMSColumnInfo> partitionColumnList;
  private Map<String, String> properties;
  private int createTime;

  public HiveTableInfo(TableIdentifier tableIdentifier, TableMeta.TableType tableType, List<AMSColumnInfo> schema,
                       List<AMSColumnInfo> partitionColumnList, Map<String, String> properties, int createTime) {
    this.tableIdentifier = tableIdentifier;
    this.tableType = tableType;
    this.schema = schema;
    this.partitionColumnList = partitionColumnList;
    this.properties = properties;
    this.createTime = createTime;
  }

  public HiveTableInfo() {
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public TableMeta.TableType getTableType() {
    return tableType;
  }

  public void setTableType(TableMeta.TableType tableType) {
    this.tableType = tableType;
  }

  public List<AMSColumnInfo> getSchema() {
    return schema;
  }

  public void setSchema(List<AMSColumnInfo> schema) {
    this.schema = schema;
  }

  public List<AMSColumnInfo> getPartitionColumnList() {
    return partitionColumnList;
  }

  public void setPartitionColumnList(List<AMSColumnInfo> partitionColumnList) {
    this.partitionColumnList = partitionColumnList;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public int getCreateTime() {
    return createTime;
  }

  public void setCreateTime(int createTime) {
    this.createTime = createTime;
  }
}
