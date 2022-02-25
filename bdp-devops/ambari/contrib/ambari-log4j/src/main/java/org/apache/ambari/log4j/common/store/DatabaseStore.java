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
package org.apache.ambari.log4j.common.store;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.ambari.log4j.common.LogStore;
import org.apache.ambari.log4j.common.LogStoreUpdateProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.util.StringUtils;
import org.apache.log4j.spi.LoggingEvent;

public class DatabaseStore implements LogStore {

  final private String database;
  final private String user;
  final private String password;
  final private LogStoreUpdateProvider updateProvider;
  final private String driver;
  private Connection connection;
  private boolean initialized;

  private static final Log LOG = LogFactory.getLog(DatabaseStore.class);

  public DatabaseStore(String driver,
                       String database, String user, String password,
                       LogStoreUpdateProvider updateProvider)
      throws IOException {
    this.initialized = false;
    this.driver = driver;
    try {
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      System.err.println("Can't load driver - " + driver);
      throw new RuntimeException("Can't load driver - " + driver);
    }
    this.database = database;
    this.user = (user == null) ? "" : user;
    this.password = (password == null) ? "" : password;
    this.updateProvider = updateProvider;
  }

  @Override
  public void persist(LoggingEvent originalEvent, Object parsedEvent)
      throws IOException {
    if (!this.initialized) {
      synchronized (DatabaseStore.class) {
        if (!this.initialized) {
          try {
            this.connection =
                DriverManager.getConnection(this.database, this.user, this.password);
          } catch (SQLException sqle) {
            LOG.debug("Failed to connect to db " + this.database, sqle);
            System.err.println("Failed to connect to db " + this.database +
                " as user " + this.user + " password " + this.password +
                " and driver " + this.driver + " with " +
                StringUtils.stringifyException(sqle));
            throw new IOException("Can't connect to database " + this.database, sqle);
          } catch (Exception e) {
            LOG.debug("Failed to connect to db " + this.database, e);
            System.err.println("Failed to connect to db " + this.database +
                " as user " + this.user + " password " + this.password +
                " and driver " + this.driver + " with " +
                StringUtils.stringifyException(e));
            throw new RuntimeException(
                "Failed to create database store for " + this.database, e);
          }
          this.updateProvider.init(this.connection);
          this.initialized = true;
        }
      }
    }

    updateProvider.update(originalEvent, parsedEvent);
  }

  @Override
  public void close() throws IOException {
    try {
      if (this.initialized && this.connection != null) {
        connection.close();
      }
    } catch (SQLException sqle) {
      throw new IOException(
          "Failed to close connection to database " + this.database, sqle);
    }
  }
}
