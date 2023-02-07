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
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection factory that caches connections based on given {@link org.apache.drill.jdbc.ConnectionInfo}.
 */
public class MultiConnectionCachingFactory implements CachingConnectionFactory {
  private static final Logger logger = LoggerFactory.getLogger(MultiConnectionCachingFactory.class);

  private final ConnectionFactory delegate;
  private final Map<ConnectionInfo, Connection> cache = Maps.newHashMap();

  public MultiConnectionCachingFactory(ConnectionFactory delegate) {
    this.delegate = delegate;
  }

  /**
   * Creates a {@link org.apache.drill.jdbc.NonClosableConnection connection} and caches it.
   *
   * The returned {@link org.apache.drill.jdbc.NonClosableConnection connection} does not support
   * {@link java.sql.Connection#close()}. Consumer must call {#close} to close the cached connections.
   */
  @Override
  public Connection getConnection(ConnectionInfo info) throws SQLException {
    Connection conn = cache.get(info);
    if (conn == null) {
      conn = delegate.getConnection(info);
      cache.put(info, conn);
    }
    return new NonClosableConnection(conn);
  }

  /**
   * Closes all active connections in the cache.
   */
  public void closeConnections() throws SQLException {
    for (Connection conn : cache.values()) {
      conn.close();
    }
  }
}