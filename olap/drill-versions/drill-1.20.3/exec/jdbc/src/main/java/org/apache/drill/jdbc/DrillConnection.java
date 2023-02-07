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
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;
import java.util.concurrent.Executor;

import org.apache.drill.exec.client.DrillClient;


/**
 * Drill-specific {@link Connection}.
 * @see #unwrap
 */
public interface DrillConnection extends Connection {


  /**
   * {@inheritDoc}
   * <p>
   *   <strong>Drill</strong>:
   *   Accepts {@code DrillConnection.class}.
   * </p>
   */
  @Override
  <T> T unwrap(Class<T> iface) throws SQLException;

  /**
   * {@inheritDoc}
   * <p>
   *   <strong>Drill</strong>:
   *   Returns true for {@code DrillConnection.class}.
   * </p>
   */
  @Override
  boolean isWrapperFor(Class<?> iface) throws SQLException;


  /**
   * {@inheritDoc}
   * <p>
   *   <strong>Drill</strong>:
   *   Accepts only {@code true}.
   * </p>
   * @throws SQLFeatureNotSupportedException if called with {@code false}
   */
  @Override
  void setAutoCommit(boolean autoCommit) throws SQLFeatureNotSupportedException,
                                                SQLException;
  /**
   * {@inheritDoc}
   * <p>
   *   <strong>Drill</strong>:
   *   Always returns {@code true}.
   * </p>
   */
  @Override
  boolean getAutoCommit() throws SQLException;


  /**
   * <strong>Drill</strong>:
   * Not supported.  Always throws {@link SQLFeatureNotSupportedException} (or
   * {@link AlreadyClosedSqlException}).
   */
  @Override
  void commit() throws SQLException;


  /**
   * <strong>Drill</strong>:
   * Not supported.  Always throws {@link SQLFeatureNotSupportedException} (or
   * {@link AlreadyClosedSqlException}).
   */
  @Override
  void rollback() throws SQLException;


  /**
   * {@inheritDoc}
   * <p>
   *   <strong>Drill</strong>:
   *   Does not throw SQLException.
   * </p>
   */
  @Override
  boolean isClosed();


  /**
   * {@inheritDoc}
   * <p>
   *   <strong>Drill</strong>:
   *   Accepts only {@link Connection#TRANSACTION_NONE}.
   * </p>
   *
   * @throws SQLFeatureNotSupportedException if {@code level} is not
   * {@link Connection#TRANSACTION_NONE}.
   */
  @Override
  void setTransactionIsolation(int level) throws SQLFeatureNotSupportedException,
                                                 SQLException;

  /**
   * {@inheritDoc}
   * <p>
   *   <strong>Drill</strong>:
   *   Always returns {@link Connection#TRANSACTION_NONE}.
   * </p>
   */
  @Override
  int getTransactionIsolation() throws SQLException;


  /**
   * <strong>Drill</strong>:
   * Not supported.  Always throws {@link SQLFeatureNotSupportedException} (or
   * {@link AlreadyClosedSqlException}).
   */
  @Override
  Savepoint setSavepoint() throws SQLException;

  /**
   * <strong>Drill</strong>:
   * Not supported.  Always throws {@link SQLFeatureNotSupportedException} (or
   * {@link AlreadyClosedSqlException}).
   */
  @Override
  Savepoint setSavepoint(String name) throws SQLException;

  /**
   * <strong>Drill</strong>:
   * Not supported.  Always throws {@link SQLFeatureNotSupportedException} (or
   * {@link AlreadyClosedSqlException}).
   */
  @Override
  void rollback(Savepoint savepoint) throws SQLException;

  /**
   * <strong>Drill</strong>:
   * Not supported.  Always throws {@link SQLFeatureNotSupportedException} (or
   * {@link AlreadyClosedSqlException}).
   */
  @Override
  void releaseSavepoint(Savepoint savepoint) throws SQLException;


  // In java.sql.Connection from JDK 1.7, but declared here to allow other JDKs.
  void setSchema(String schema) throws SQLException;

  // In java.sql.Connection from JDK 1.7, but declared here to allow other JDKs.
  String getSchema() throws SQLException;


  /**
   * <strong>Drill</strong>:
   * Not supported (for non-zero timeout value).
   * <p>
   *   Normally, just throws {@link SQLFeatureNotSupportedException} unless
   *   request is trivially for no timeout (zero {@code milliseconds} value).
   * </p>
   * @throws  AlreadyClosedSqlException
   *            if connection is closed
   * @throws  JdbcApiSqlException
   *            if an invalid parameter value is detected (and not above case)
   * @throws  SQLFeatureNotSupportedException
   *            if timeout is non-zero (and not above case)
   */
  @Override
  void setNetworkTimeout( Executor executor, int milliseconds )
      throws AlreadyClosedSqlException,
             JdbcApiSqlException,
             SQLFeatureNotSupportedException;

  /**
   * <strong>Drill</strong>:
   * Returns zero.
   * {@inheritDoc}
   * @throws  AlreadyClosedSqlException
   *            if connection is closed
   */
  @Override
  int getNetworkTimeout() throws AlreadyClosedSqlException;


  //////////////////////////////////////////////////////////////////////
  // Drill extensions.

  /**
   * Returns a view onto this connection's configuration properties. Code
   * within Optiq should use this view rather than calling
   * {@link java.util.Properties#getProperty(String)}.
   */
  DrillConnectionConfig getConfig();

  DrillClient getClient();

}
