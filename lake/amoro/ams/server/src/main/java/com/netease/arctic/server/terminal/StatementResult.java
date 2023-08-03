/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.terminal;

import com.clearspring.analytics.util.Lists;

import java.util.List;

/**
 * result of single statement.
 */
public class StatementResult {
  private int lineNumber;
  private String statement;
  private boolean success;
  private String logs;
  private List<String> columns;
  private List<Object[]> datas = Lists.newArrayList();
  private boolean empty;

  public StatementResult(String statement, int lineNumber, List<String> columns) {
    this.statement = statement;
    this.lineNumber = lineNumber;
    this.columns = columns;
    this.success = true;
    this.empty = false;
  }

  public void appendRow(Object[] row) {
    this.datas.add(row);
  }

  public void withExceptionLog(String log) {
    this.logs = log;
    this.success = false;
  }

  public String getStatement() {
    return statement;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getLogs() {
    return logs;
  }

  public List<String> getColumns() {
    return columns;
  }

  public List<Object[]> getDatas() {
    return datas;
  }

  public List<List<String>> getDataAsStringList() {
    List<List<String>> results = Lists.newArrayList();
    for (Object[] row : datas) {
      List<String> rowStringList = Lists.newArrayList();
      for (Object o : row) {
        if (o == null) {
          rowStringList.add("null");
        } else {
          rowStringList.add(o.toString());
        }
      }
      results.add(rowStringList);
    }
    return results;
  }

  public boolean isEmpty() {
    return empty;
  }
}
