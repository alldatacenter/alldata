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

package org.apache.ambari.server.controller.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connection factory implementation for job history postgres db.
 */
public class JobHistoryPostgresConnectionFactory implements ConnectionFactory {
  private static final String DEFAULT_HOSTNAME = "localhost";
  private static final String DEFAULT_DBNAME = "ambarirca";
  private static final String DEFAULT_USERNAME = "mapred";
  private static final String DEFAULT_PASSWORD = "mapred";

  private String url;
  private String username;
  private String password;

  /**
   * Create a connection factory with default parameters.
   */
  public JobHistoryPostgresConnectionFactory() {
    this(DEFAULT_HOSTNAME, DEFAULT_DBNAME, DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }

  /**
   * Create a connection factory with given parameters.
   * 
   * @param hostname host running postgres
   * @param dbname name of the postgres db
   * @param username username for postgres db
   * @param password password for postgres db
   */
  public JobHistoryPostgresConnectionFactory(String hostname, String dbname, String username, String password) {
    url = "jdbc:postgresql://" + hostname + "/" + dbname;
    this.username = username;
    this.password = password;
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Can't load postgresql", e);
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(url, username, password);
  }
}
