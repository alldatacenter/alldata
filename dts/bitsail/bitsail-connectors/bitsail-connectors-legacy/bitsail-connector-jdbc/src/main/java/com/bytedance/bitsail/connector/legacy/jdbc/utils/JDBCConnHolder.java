/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Data
@Slf4j
public class JDBCConnHolder {
  private static final int DEFAULT_TIMEOUT = 5;
  public Connection dbConn;
  public String driverName;
  public String dbURL;
  public String username;
  public String password;
  public Boolean isSupportTransaction;

  public JDBCConnHolder(String driverName, String dbURL, String username, String password, Boolean isSupportTransaction) {
    this.driverName = driverName;
    this.dbURL = dbURL;
    this.username = username;
    this.password = password;
    this.isSupportTransaction = isSupportTransaction;
  }

  public void establishConnection() throws IOException, ClassNotFoundException {
    Class.forName(driverName);
    initConnection();
  }

  public void reconnect(PreparedStatement st) throws IOException {
    closeDBResource(st);
    initConnection();
  }

  public void initConnection() throws IOException {
    try {
      if (username == null) {
        dbConn = DriverManager.getConnection(dbURL);
      } else {
        dbConn = DriverManager.getConnection(dbURL, username, password);
      }
      if (isSupportTransaction) {
        dbConn.setAutoCommit(false);
      }
    } catch (SQLException e) {
      throw new IOException("Exception when initialize jdbc connection", e);
    }
  }

  public void closeDBResource(PreparedStatement st) {
    closeResource(st);
    closeResource(dbConn);
    dbConn = null;
  }

  public void closeResource(AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception e) {
        log.info("JDBC resource could not be closed: " + e.getMessage());
      }
    }
  }

  public void rollBackTransaction() throws SQLException {
    if (dbConn != null && isSupportTransaction) {
      dbConn.rollback();
    }
  }

  public void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      //ignore
    }
  }

  public boolean isValid() throws SQLException {
    return dbConn.isValid(DEFAULT_TIMEOUT);
  }

  public void commit() throws SQLException {
    if (dbConn != null && isSupportTransaction) {
      dbConn.commit();
    }
  }

  public PreparedStatement prepareStatement(String query) throws SQLException {
    return dbConn.prepareStatement(query);
  }
}
