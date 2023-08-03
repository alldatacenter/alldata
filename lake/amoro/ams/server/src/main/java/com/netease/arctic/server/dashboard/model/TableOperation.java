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

public class TableOperation {
  long ts;
  String operation;

  public TableOperation() {
  }

  public TableOperation(long ts, String operation) {
    this.ts = ts;
    this.operation = operation;
  }

  public long getTs() {
    return ts;
  }

  public void setTs(long ts) {
    this.ts = ts;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public static TableOperation buildFromDDLInfo(DDLInfo ddlInfo) {
    return ddlInfo != null ? new TableOperation(ddlInfo.getCommitTime(), ddlInfo.getDdl()) : null;
  }
}
