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

import java.util.List;
import java.util.Map;

public class CatalogRegisterInfo {
  String name;
  String type;
  String optimizerGroup;
  List<String> tableFormatList;
  Map<String, String> storageConfig;
  Map<String, String> authConfig;
  Map<String, String> properties;

  public CatalogRegisterInfo() {

  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getOptimizerGroup() {
    return optimizerGroup;
  }

  public void setOptimizerGroup(String optimizerGroup) {
    this.optimizerGroup = optimizerGroup;
  }

  public Map<String, String> getStorageConfig() {
    return storageConfig;
  }

  public void setStorageConfig(Map<String, String> storageConfig) {
    this.storageConfig = storageConfig;
  }

  public Map<String, String> getAuthConfig() {
    return authConfig;
  }

  public void setAuthConfig(Map<String, String> authConfig) {
    this.authConfig = authConfig;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public List<String> getTableFormatList() {
    return tableFormatList;
  }

  public void setTableFormatList(List<String> tableFormatList) {
    this.tableFormatList = tableFormatList;
  }
}
