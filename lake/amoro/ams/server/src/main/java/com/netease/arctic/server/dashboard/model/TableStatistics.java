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

import java.util.Map;

public class TableStatistics {
  TableIdentifier tableIdentifier;
  FilesStatistics totalFilesStat;
  Map<String, String> summary;

  public TableStatistics() {
  }

  public TableStatistics(TableStatistics tableStatistics) {
    this.setTableIdentifier(tableStatistics.getTableIdentifier());
    this.setTotalFilesStat(tableStatistics.getTotalFilesStat());
    this.setSummary(tableStatistics.getSummary());
  }

  public TableStatistics(TableIdentifier tableIdentifier, FilesStatistics totalFilesStat, Map<String, String> summary) {
    this.tableIdentifier = tableIdentifier;
    this.totalFilesStat = totalFilesStat;
    this.summary = summary;
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public FilesStatistics getTotalFilesStat() {
    return totalFilesStat;
  }

  public void setTotalFilesStat(FilesStatistics totalFilesStat) {
    this.totalFilesStat = totalFilesStat;
  }

  public void setTotalFilesStat(FilesStatistics changeFs, FilesStatistics baseFs) {
    this.totalFilesStat = totalFilesStat;
  }

  public Map<String, String> getSummary() {
    return summary;
  }

  public void setSummary(Map<String, String> summary) {
    this.summary = summary;
  }
}
