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

import java.util.ArrayList;
import java.util.List;

/**
 * sql running info for frontend
 */
public class SqlRunningInfo {
  private List<SqlResult> sqlResults = new ArrayList<>();
  private List<String> logs = new ArrayList<>();
  private String logStatus = SqlStatus.CREATED.getName();
  private Thread executeThread;
  private String sql;

  public List<SqlResult> getSqlResults() {
    return sqlResults;
  }

  public void setSqlResults(List<SqlResult> sqlResults) {
    this.sqlResults = sqlResults;
  }

  public List<String> getLogs() {
    return logs;
  }

  public void setLogs(List<String> logs) {
    this.logs = logs;
  }

  public String getLogStatus() {
    return logStatus;
  }

  public void setLogStatus(String logStatus) {
    this.logStatus = logStatus;
  }

  public Thread getExecuteThread() {
    return executeThread;
  }

  public void setExecuteThread(Thread executeThread) {
    this.executeThread = executeThread;
  }

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }
}
