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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class JDBCResultSet implements TerminalSession.ResultSet {

  private ResultSet rs;
  private Statement statement;

  private List<String> columns = Lists.newArrayList();

  public JDBCResultSet(ResultSet rs, Statement sts) {
    this.rs = rs;
    this.statement = sts;
    if (rs != null) {
      try {
        int columnCount = rs.getMetaData().getColumnCount();
        for (int i = 0; i < columnCount; i++) {
          String col = rs.getMetaData().getColumnName(i + 1);
          columns.add(col);
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public List<String> columns() {
    return this.columns;
  }

  @Override
  public boolean next() {
    try {
      return rs != null && rs.next();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object[] rowData() {
    Object[] rows = new Object[columns.size()];
    for (int i = 0; i < rows.length; i++) {
      try {
        rows[i] = rs.getObject(i + 1);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    return rows;
  }

  @Override
  public boolean empty() {
    return columns.isEmpty();
  }

  @Override
  public void close() {
    try {
      if (this.rs != null) {
        this.rs.close();
      }
    } catch (SQLException e) {
      // pass
    }
    try {
      this.statement.close();
    } catch (SQLException e) {
      // pass
    }
  }
}
