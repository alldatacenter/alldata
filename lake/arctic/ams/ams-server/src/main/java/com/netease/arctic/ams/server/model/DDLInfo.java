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

package com.netease.arctic.ams.server.model;

import com.netease.arctic.ams.api.TableIdentifier;

public class DDLInfo {
  private TableIdentifier tableIdentifier;
  private String ddl;
  private String ddlType;
  private Long commitTime;

  public static DDLInfo of(
      TableIdentifier tableIdentifier,
      String ddl,
      String ddlType,
      Long commitTime) {
    DDLInfo ddlInfo = new DDLInfo();
    ddlInfo.setTableIdentifier(tableIdentifier);
    ddlInfo.setDdl(ddl);
    ddlInfo.setCommitTime(commitTime);
    ddlInfo.setDdlType(ddlType);
    return ddlInfo;
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public String getDdl() {
    return ddl;
  }

  public void setDdl(String ddl) {
    this.ddl = ddl;
  }

  public String getDdlType() {
    return ddlType;
  }

  public void setDdlType(String ddlType) {
    this.ddlType = ddlType;
  }

  public Long getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(Long commitTime) {
    this.commitTime = commitTime;
  }
}
