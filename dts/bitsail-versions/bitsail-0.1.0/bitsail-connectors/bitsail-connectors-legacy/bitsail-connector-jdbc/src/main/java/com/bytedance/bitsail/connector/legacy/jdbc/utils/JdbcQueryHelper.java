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

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class JdbcQueryHelper implements Serializable {
  private static final int DEFAULT_DELETE_INTERVAL = 100;
  private String driverName;
  private String username;
  private String dbURL;
  private String password;

  /**
   * mysql option
   */
  private int deleteInterval;

  public JdbcQueryHelper(String dbURL, String username, String password, String driverName) {
    this(dbURL, username, password, DEFAULT_DELETE_INTERVAL, driverName);
  }

  public JdbcQueryHelper(String dbURL, String username, String password, int deleteInterval, String driverName) {
    this.dbURL = dbURL;
    this.username = username;
    this.password = password;
    this.deleteInterval = deleteInterval;
    this.driverName = driverName;
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  public void executeLimitDeleteInNewConnection(String query) {
    Connection connection = null;
    Statement statement = null;

    try {
      Class.forName(driverName);
      if (username == null) {
        connection = DriverManager.getConnection(dbURL);
      } else {
        connection = DriverManager.getConnection(dbURL, username, password);
      }

      statement = connection.createStatement();

      long startTime = System.currentTimeMillis();
      boolean continueDelete = true;
      int totalDeleteNum = 0;

      while (continueDelete) {
        int deleteNum = statement.executeUpdate(query);
        if (isDeleteFinished(deleteNum)) {
          continueDelete = false;
        }
        totalDeleteNum += deleteNum;

        long currentTime = System.currentTimeMillis();
        if (currentTime - startTime > 10000) {
          startTime = currentTime;
          log.info("execute query: {}, current delete num: {}", query, totalDeleteNum);
        }
        Thread.sleep(deleteInterval);
      }
      log.info("finish execute query: {}, final delete num: {}", query, totalDeleteNum);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR,
          "Executing query in new connection failed, query: " + query, e);
    } finally {
      closeStatement(statement);
      closeConnection(connection);
    }
  }

  private boolean isDeleteFinished(int deleteNum) {
    return deleteNum == 0;
  }

  public String executeQueryInNewConnection(String query, boolean isUpdate) {
    Connection connection = null;
    Statement statement = null;

    try {
      Class.forName(driverName);
      if (username == null) {
        connection = DriverManager.getConnection(dbURL);
      } else {
        connection = DriverManager.getConnection(dbURL, username, password);
      }

      statement = connection.createStatement();
      if (isUpdate) {
        statement.executeUpdate(query);
        return "";
      } else {
        final ResultSet resultSet = statement.executeQuery(query);
        resultSet.next();
        return resultSet.getString(1);
      }
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR,
          "Executing query in new connection failed, query: " + query, e);
    } finally {
      closeStatement(statement);
      closeConnection(connection);
    }
  }

  public void closeStatement(Statement statement) {
    if (statement != null) {
      // close the connection
      try {
        statement.close();
      } catch (SQLException e) {
        log.info("JDBC statement could not be closed: " + e.getMessage());
      }
    }
  }

  public void closeConnection(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException se) {
        log.info("JDBC connection could not be closed: " + se.getMessage());
      }
    }
  }
}
