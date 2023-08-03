
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

package com.netease.arctic.trino;

import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;

/**
 * Arctic config
 */
public class ArcticConfig {
  private String catalogUrl;
  private boolean hdfsImpersonationEnabled;
  private boolean tableStatisticsEnabled = true;

  private Double splitTaskByDeleteRatio = 0.05;

  private boolean enableSplitTaskByDeleteRatio = true;

  public String getCatalogUrl() {
    return catalogUrl;
  }

  public boolean getHdfsImpersonationEnabled() {
    return hdfsImpersonationEnabled;
  }

  public boolean isTableStatisticsEnabled() {
    return tableStatisticsEnabled;
  }

  public Double getSplitTaskByDeleteRatio() {
    return splitTaskByDeleteRatio;
  }

  public boolean isEnableSplitTaskByDeleteRatio() {
    return enableSplitTaskByDeleteRatio;
  }

  @Config("arctic.url")
  public void setCatalogUrl(String catalogUrl) {
    this.catalogUrl = catalogUrl;
  }

  @Config("arctic.hdfs.impersonation.enabled")
  public void setHdfsImpersonationEnabled(boolean enabled) {
    this.hdfsImpersonationEnabled = enabled;
  }

  @Config("arctic.table-statistics-enable")
  @ConfigDescription("Enable use of table statistics to Arctic table")
  public void setTableStatisticsEnabled(boolean tableStatisticsEnabled) {
    this.tableStatisticsEnabled = tableStatisticsEnabled;
  }

  @Config("arctic.enable-split-task-by-delete-ratio")
  public void setEnableSplitTaskByDeleteRatio(boolean enableSplitTaskByDeleteRatio) {
    this.enableSplitTaskByDeleteRatio = enableSplitTaskByDeleteRatio;
  }

  @Config("arctic.split-task-by-delete-ratio")
  public void setSplitTaskByDeleteRatio(double splitTaskByDeleteRatio) {
    this.splitTaskByDeleteRatio = splitTaskByDeleteRatio;
  }
}
