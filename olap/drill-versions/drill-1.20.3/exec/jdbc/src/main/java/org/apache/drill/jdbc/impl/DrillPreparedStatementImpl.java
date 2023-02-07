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
package org.apache.drill.jdbc.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.sql.Statement;

import org.apache.calcite.avatica.AvaticaParameter;
import org.apache.calcite.avatica.AvaticaPreparedStatement;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.Meta.StatementHandle;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserProtos.PreparedStatement;
import org.apache.drill.jdbc.AlreadyClosedSqlException;
import org.apache.drill.jdbc.DrillPreparedStatement;

/**
 * Implementation of {@link java.sql.PreparedStatement} for Drill.
 *
 * <p>
 * This class has sub-classes which implement JDBC 3.0 and JDBC 4.0 APIs; it is
 * instantiated using
 * {@link org.apache.calcite.avatica.AvaticaFactory#newPreparedStatement}.
 * </p>
 */
abstract class DrillPreparedStatementImpl extends AvaticaPreparedStatement
    implements DrillPreparedStatement,
               DrillRemoteStatement {

  private final PreparedStatement preparedStatementHandle;

  protected DrillPreparedStatementImpl(DrillConnectionImpl connection,
                                       StatementHandle h,
                                       Meta.Signature signature,
                                       PreparedStatement preparedStatementHandle,
                                       int resultSetType,
                                       int resultSetConcurrency,
                                       int resultSetHoldability) throws SQLException {
    super(connection, h, signature,
          resultSetType, resultSetConcurrency, resultSetHoldability);
    connection.openStatementsRegistry.addStatement(this);
    this.preparedStatementHandle = preparedStatementHandle;
    if (preparedStatementHandle != null) {
      ((DrillColumnMetaDataList) signature.columns).updateColumnMetaData(preparedStatementHandle.getColumnsList());
    }
  }

  /**
   * Throws AlreadyClosedSqlException <i>iff</i> this PreparedStatement is closed.
   *
   * @throws  AlreadyClosedSqlException  if PreparedStatement is closed
   */
  @Override
  protected void checkOpen() throws SQLException {
    if (isClosed()) {
      throw new AlreadyClosedSqlException("PreparedStatement is already closed.");
    }
  }


  // Note:  Using dynamic proxies would reduce the quantity (450?) of method
  // overrides by eliminating those that exist solely to check whether the
  // object is closed.

  PreparedStatement getPreparedStatementHandle() {
    return preparedStatementHandle;
  }

  @Override
  protected AvaticaParameter getParameter(int param) throws SQLException {
    checkOpen();
    throw new SQLFeatureNotSupportedException(
        "Prepared-statement dynamic parameters are not supported.");
  }

  @Override
  public void cleanUp() {
    final DrillConnectionImpl connection1 = (DrillConnectionImpl) connection;
    connection1.openStatementsRegistry.removeStatement(this);
  }

  // Note:  Methods are in same order as in java.sql.PreparedStatement.

  // No isWrapperFor(Class<?>) (it doesn't throw SQLException if already closed).
  // No unwrap(Class<T>) (it doesn't throw SQLException if already closed).

  // No close() (it doesn't throw SQLException if already closed).

  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    checkOpen();
    try {
      super.setEscapeProcessing(enable);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void setCursorName(String name) throws SQLException {
    checkOpen();
    try {
      super.setCursorName(name);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    try {
      return super.getMoreResults();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean getMoreResults(int current) throws SQLException {
    try {
      return super.getMoreResults(current);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    checkOpen();
    try {
      return super.getGeneratedKeys();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    checkOpen();
    return super.executeLargeUpdate(sql, autoGeneratedKeys);
  }

  @Override
  public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException {
    checkOpen();
    return super.executeLargeUpdate(sql, columnIndexes);
  }

  @Override
  public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
    checkOpen();
    return super.executeLargeUpdate(sql, columnNames);
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    checkOpen();
    try {
      return super.executeUpdate(sql, autoGeneratedKeys);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public int executeUpdate(String sql, int columnIndexes[]) throws SQLException {
    checkOpen();
    try {
      return super.executeUpdate(sql, columnIndexes);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public int executeUpdate(String sql, String columnNames[]) throws SQLException {
    checkOpen();
    try {
      return super.executeUpdate(sql, columnNames);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    checkOpen();
    try {
      return super.execute(sql, autoGeneratedKeys);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean execute(String sql, int columnIndexes[]) throws SQLException {
    checkOpen();
    try {
      return super.execute(sql, columnIndexes);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean execute(String sql, String columnNames[]) throws SQLException {
    checkOpen();
    try {
      return super.execute(sql, columnNames);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean isClosed() {
    try {
      return super.isClosed();
    } catch (SQLException e) {
      throw new RuntimeException(
          "Unexpected " + e + " from AvaticaPreparedStatement.isClosed");
    }
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    checkOpen();
    try {
      super.setPoolable(poolable);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void setObject(int parameterIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
    checkOpen();
    super.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
  }

  @Override
  public void setObject(int parameterIndex, Object x, SQLType targetSqlType) throws SQLException {
    checkOpen();
    super.setObject(parameterIndex, x, targetSqlType);
  }

  @Override
  public void setLargeMaxRows(long maxRowCount) throws SQLException {
    super.setLargeMaxRows(maxRowCount);
    try (Statement statement = this.connection.createStatement()) {
      statement.execute("ALTER SESSION SET `" + ExecConstants.QUERY_MAX_ROWS + "`=" + maxRowCount);
    }
  }
}
