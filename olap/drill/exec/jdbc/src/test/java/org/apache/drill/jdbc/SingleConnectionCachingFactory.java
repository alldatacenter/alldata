/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A connection factory that creates and caches a single connection instance.
 *
 * <p>
 *   Not thread safe.
 * </p>
 */
public class SingleConnectionCachingFactory implements CachingConnectionFactory {

  private final ConnectionFactory delegate;
  private Connection connection;

  public SingleConnectionCachingFactory(ConnectionFactory delegate) {
    this.delegate = delegate;
  }

  /**
   * {@inheritDoc}
   * <p>
   *   For this implementation, calls to {@code createConnection} without any
   *   intervening calls to {@link #closeConnections} return the same Connection
   *   instance.
   * </p>
   */
  @Override
  public Connection getConnection(ConnectionInfo info) throws SQLException {
    if (connection == null) {
      connection = delegate.getConnection(info);
    } else {
      JdbcTestBase.changeSchemaIfSupplied(connection, info.getParamsAsProperties());
    }
    return new NonClosableConnection(connection);
  }

  @Override
  public void closeConnections() throws SQLException {
    if (connection != null) {
      connection.close();
      connection = null;
    }
  }
}
