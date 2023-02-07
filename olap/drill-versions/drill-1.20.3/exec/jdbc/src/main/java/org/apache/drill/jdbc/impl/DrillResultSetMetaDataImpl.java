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

import java.sql.SQLException;

import org.apache.calcite.avatica.AvaticaResultSetMetaData;
import org.apache.calcite.avatica.AvaticaStatement;
import org.apache.calcite.avatica.Meta;
import org.apache.drill.jdbc.AlreadyClosedSqlException;
import org.apache.drill.jdbc.InvalidParameterSqlException;


public class DrillResultSetMetaDataImpl extends AvaticaResultSetMetaData {

  private final AvaticaStatement statement;


  public DrillResultSetMetaDataImpl(AvaticaStatement statement,
                                    Object query,
                                    Meta.Signature signature) {
    super(statement, query, signature);
    this.statement = statement;
  }

  /**
   * Throws AlreadyClosedSqlException if the associated ResultSet is closed.
   *
   * @throws  AlreadyClosedSqlException  if ResultSet is closed
   * @throws  SQLException  if error in checking ResultSet's status
   */
  private void checkOpen() throws AlreadyClosedSqlException, SQLException {
    // Statement.isClosed() call is to avoid exception from getResultSet().
    if (statement.isClosed()
        || (statement.getResultSet() != null // result set doesn't exist for prepared statement cases
            && statement.getResultSet().isClosed())) {
        throw new AlreadyClosedSqlException(
            "ResultSetMetaData's ResultSet is already closed." );
    }
  }

  private void throwIfClosedOrOutOfBounds(int columnNumber)
      throws SQLException {
    checkOpen();
    if (1 > columnNumber || columnNumber > getColumnCount()) {
      throw new InvalidParameterSqlException(
          "Column number " + columnNumber + " out of range of from 1 through "
          + getColumnCount() + " (column count)");
    }
  }


  // Note:  Using dynamic proxies would reduce the quantity (450?) of method
  // overrides by eliminating those that exist solely to check whether the
  // object is closed.  It would also eliminate the need to throw non-compliant
  // RuntimeExceptions when Avatica's method declarations won't let us throw
  // proper SQLExceptions. (Check performance before applying to frequently
  // called ResultSet.)

  // Note:  Methods are in same order as in java.sql.ResultSetMetaData.

  // No isWrapperFor(Class<?>) (it doesn't throw SQLException if already closed).
  // No unwrap(Class<T>) (it doesn't throw SQLException if already closed).

  @Override
  public int getColumnCount() throws SQLException {
    checkOpen();
    return super.getColumnCount();
  }

  @Override
  public boolean isAutoIncrement(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.isAutoIncrement(columnNumber);
  }

  @Override
  public boolean isCaseSensitive(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.isCaseSensitive(columnNumber);
  }

  @Override
  public boolean isSearchable(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.isSearchable(columnNumber);
  }

  @Override
  public boolean isCurrency(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.isCurrency(columnNumber);
  }

  @Override
  public int isNullable(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.isNullable(columnNumber);
  }

  @Override
  public boolean isSigned(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.isSigned(columnNumber);
  }

  @Override
  public int getColumnDisplaySize(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getColumnDisplaySize(columnNumber);
  }

  @Override
  public String getColumnLabel(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getColumnLabel(columnNumber);
  }

  @Override
  public String getColumnName(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getColumnName(columnNumber);
  }

  @Override
  public String getSchemaName(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getSchemaName(columnNumber);
  }

  @Override
  public int getPrecision(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getPrecision(columnNumber);
  }

  @Override
  public int getScale(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getScale(columnNumber);
  }

  @Override
  public String getTableName(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getTableName(columnNumber);
  }

  @Override
  public String getCatalogName(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getCatalogName(columnNumber);
  }

  @Override
  public int getColumnType(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getColumnType(columnNumber);
  }

  @Override
  public String getColumnTypeName(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getColumnTypeName(columnNumber);
  }

  @Override
  public boolean isReadOnly(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.isReadOnly(columnNumber);
  }

  @Override
  public boolean isWritable(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.isWritable(columnNumber);
  }

  @Override
  public boolean isDefinitelyWritable(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.isDefinitelyWritable(columnNumber);
  }

  @Override
  public String getColumnClassName(int columnNumber) throws SQLException {
    throwIfClosedOrOutOfBounds(columnNumber);
    return super.getColumnClassName(columnNumber);
  }

}
