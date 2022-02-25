/**
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

package org.apache.ambari.scom;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.jdbc.ConnectionFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Factory for the sink database connection.
 */
public class SinkConnectionFactory implements ConnectionFactory {

  /**
   * The database URL.
   */
  private String databaseUrl;

  /**
   * The database driver.
   */
  private String databaseDriver;

  /**
   * Indicates whether or not the driver has been initialized
   */
  private boolean connectionInitialized = false;

  /**
   * The singleton.
   */
  private static SinkConnectionFactory singleton = new SinkConnectionFactory();

  // ----- Constants ---------------------------------------------------------

  protected static final String SCOM_SINK_DB_URL    = "scom.sink.db.url";
  protected static final String SCOM_SINK_DB_DRIVER = "scom.sink.db.driver";


  // ----- Constructor -------------------------------------------------------

  protected SinkConnectionFactory() {
  }


  // ----- SinkConnectionFactory ---------------------------------------------

  /**
   * Initialize with the given configuration.
   *
   * @param configuration  the configuration
   */
  public void init(Configuration configuration) {
    this.databaseUrl    = configuration.getProperty(SCOM_SINK_DB_URL);
    this.databaseDriver = configuration.getProperty(SCOM_SINK_DB_DRIVER);
  }

  /**
   * Get the singleton instance.
   *
   * @return the singleton instance
   */
  public static SinkConnectionFactory instance() {
    return singleton;
  }

  /**
   * Get the database URL.
   *
   * @return the database URL
   */
  public String getDatabaseUrl() {
    return databaseUrl;
  }

  /**
   * Get the database driver.
   *
   * @return the database driver
   */
  public String getDatabaseDriver() {
    return databaseDriver;
  }

// ----- ConnectionFactory -----------------------------------------------

  @Override
  public Connection getConnection() throws SQLException {
    synchronized (this) {
      if (!connectionInitialized) {
        connectionInitialized = true;
        try {
          Class.forName(databaseDriver);
        } catch (ClassNotFoundException e) {
          throw new SQLException("Can't load the driver class.", e);
        }
      }
    }
    return DriverManager.getConnection(databaseUrl);
  }
}
