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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.calcite.avatica.AvaticaResultSet;
import org.apache.calcite.avatica.AvaticaSite;
import org.apache.calcite.avatica.AvaticaStatement;
import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.QueryState;
import org.apache.calcite.avatica.util.Cursor;
import org.apache.drill.jdbc.AlreadyClosedSqlException;
import org.apache.drill.jdbc.DrillResultSet;
import org.apache.drill.jdbc.ExecutionCanceledSqlException;
import org.apache.drill.jdbc.SqlTimeoutException;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

/**
 * Drill's implementation of {@link java.sql.ResultSet}.
 */
public class DrillResultSetImpl extends AvaticaResultSet implements DrillResultSet {
  @SuppressWarnings("unused")
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(DrillResultSetImpl.class);

  private final DrillConnectionImpl connection;
  private volatile boolean hasPendingCancellationNotification = false;

  //Timeout Support Variables
  private Stopwatch elapsedTimer;
  private long queryTimeoutInMilliseconds;

  DrillResultSetImpl(AvaticaStatement statement, QueryState state, Meta.Signature signature,
                     ResultSetMetaData resultSetMetaData, TimeZone timeZone,
                     Meta.Frame firstFrame) throws SQLException {
    super(statement, state, signature, resultSetMetaData, timeZone, firstFrame);
    connection = (DrillConnectionImpl) statement.getConnection();
  }

  /**
   * Throws AlreadyClosedSqlException or QueryCanceledSqlException if this
   * ResultSet is closed.
   *
   * @throws ExecutionCanceledSqlException if ResultSet is closed because of
   *         cancellation and no QueryCanceledSqlException has been thrown yet
   *         for this ResultSet
   * @throws AlreadyClosedSqlException if ResultSet is closed
   * @throws SQLException if error in calling {@link #isClosed()}
   */
  @Override
  protected void checkOpen() throws SQLException {
    if (isClosed()) {
      if (cursor instanceof DrillCursor && hasPendingCancellationNotification) {
        hasPendingCancellationNotification = false;
        throw new ExecutionCanceledSqlException(
            "SQL statement execution canceled; ResultSet now closed.");
      } else {
        throw new AlreadyClosedSqlException("ResultSet is already closed.");
      }
    }

    //Implicit check for whether timeout is set
    if (elapsedTimer != null) {
      //The timer has already been started by the DrillCursor at this point
      if (elapsedTimer.elapsed(TimeUnit.MILLISECONDS) > this.queryTimeoutInMilliseconds) {
        throw new SqlTimeoutException(TimeUnit.MILLISECONDS.toSeconds(this.queryTimeoutInMilliseconds));
      }
    }
  }


  // Note:  Using dynamic proxies would reduce the quantity (450?) of method
  // overrides by eliminating those that exist solely to check whether the
  // object is closed.  It would also eliminate the need to throw non-compliant
  // RuntimeExceptions when Avatica's method declarations won't let us throw
  // proper SQLExceptions. (Check performance before applying to frequently
  // called ResultSet.)

  @Override
  protected void cancel() {
    if (cursor instanceof DrillCursor) {
      hasPendingCancellationNotification = true;
      ((DrillCursor) cursor).cancel();
    } else {
      super.cancel();
    }
  }

  ////////////////////////////////////////
  // ResultSet-defined methods (in same order as in ResultSet):

  // No isWrapperFor(Class<?>) (it doesn't throw SQLException if already closed).
  // No unwrap(Class<T>) (it doesn't throw SQLException if already closed).

  // (Not delegated.)
  @Override
  public boolean next() throws SQLException {
    checkOpen();

    // TODO:  Resolve following comments (possibly obsolete because of later
    // addition of preceding call to checkOpen.  Also, NOTE that the
    // following check, and maybe some checkOpen() calls, probably must
    // synchronize on the statement, per the comment on AvaticaStatement's
    // openResultSet:

    // Next may be called after close has been called (for example after a user
    // cancellation) which in turn sets the cursor to null.  So we must check
    // before we call next.
    // TODO: handle next() after close is called in the Avatica code.
    if (cursor != null) {
      return super.next();
    } else {
      return false;
    }
  }

  @Override
  public void close() {
    // Note:  No already-closed exception for close().
    super.close();
  }

  @Override
  public String getCursorName() throws SQLException {
    checkOpen();
    try {
      return super.getCursorName();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public Object getObject(int columnIndex) throws SQLException {
    checkOpen();

    Cursor.Accessor accessor;
    try {
      accessor = accessorList.get(columnIndex - 1);
    } catch (RuntimeException e) {
      throw new SQLException(e);
    }
    ColumnMetaData metaData = columnMetaDataList.get(columnIndex - 1);
    // Drill returns a float (4bytes) for a SQL Float whereas Calcite would return a double (8bytes)
    int typeId = (metaData.type.id != Types.FLOAT) ? metaData.type.id : Types.REAL;
    return typeId != Types.NULL ? AvaticaSite.get(accessor, typeId, localCalendar) : null;
  }

  //--------------------------JDBC 2.0-----------------------------------

  //---------------------------------------------------------------------
  // Traversal/Positioning
  //---------------------------------------------------------------------
  @Override
  public boolean isLast() throws SQLException {
    checkOpen();
    try {
      return super.isLast();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void beforeFirst() throws SQLException {
    checkOpen();
    try {
      super.beforeFirst();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void afterLast() throws SQLException {
    checkOpen();
    try {
      super.afterLast();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean first() throws SQLException {
    checkOpen();
    try {
      return super.first();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean last() throws SQLException {
    checkOpen();
    try {
      return super.last();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean absolute(int row) throws SQLException {
    checkOpen();
    try {
      return super.absolute(row);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean relative(int rows) throws SQLException {
    checkOpen();
    try {
      return super.relative(rows);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean previous() throws SQLException {
    checkOpen();
    try {
      return super.previous();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  //---------------------------------------------------------------------
  // Updates
  //---------------------------------------------------------------------
  @Override
  public void updateNull(int columnIndex) throws SQLException {
    checkOpen();
    try {
      super.updateNull(columnIndex);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBoolean(int columnIndex, boolean x) throws SQLException {
    checkOpen();
    try {
      super.updateBoolean(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateByte(int columnIndex, byte x) throws SQLException {
    checkOpen();
    try {
      super.updateByte(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateShort(int columnIndex, short x) throws SQLException {
    checkOpen();
    try {
      super.updateShort(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateInt(int columnIndex, int x) throws SQLException {
    checkOpen();
    try {
      super.updateInt(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateLong(int columnIndex, long x) throws SQLException {
    checkOpen();
    try {
      super.updateLong(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateFloat(int columnIndex, float x) throws SQLException {
    checkOpen();
    try {
      super.updateFloat(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateDouble(int columnIndex, double x) throws SQLException {
    checkOpen();
    try {
      super.updateDouble(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
    checkOpen();
    try {
      super.updateBigDecimal(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateString(int columnIndex, String x) throws SQLException {
    checkOpen();
    try {
      super.updateString(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBytes(int columnIndex, byte[] x) throws SQLException {
    checkOpen();
    try {
      super.updateBytes(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateDate(int columnIndex, Date x) throws SQLException {
    checkOpen();
    try {
      super.updateDate(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateTime(int columnIndex, Time x) throws SQLException {
    checkOpen();
    try {
      super.updateTime(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
    checkOpen();
    try {
      super.updateTimestamp(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
    checkOpen();
    try {
      super.updateAsciiStream(columnIndex, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBinaryStream(int columnIndex, InputStream x,
                                 int length) throws SQLException {
    checkOpen();
    try {
      super.updateBinaryStream(columnIndex, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateCharacterStream(int columnIndex, Reader x,
                                    int length) throws SQLException {
    checkOpen();
    try {
      super.updateCharacterStream(columnIndex, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateObject(int columnIndex, Object x,
                           int scaleOrLength) throws SQLException {
    checkOpen();
    try {
      super.updateObject(columnIndex, x, scaleOrLength);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateObject(int columnIndex, Object x) throws SQLException {
    checkOpen();
    try {
      super.updateObject(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateObject(int columnIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
    checkOpen();
    super.updateObject(columnIndex, x, targetSqlType, scaleOrLength);
  }

  @Override
  public void updateObject(String columnLabel, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
    checkOpen();
    super.updateObject(columnLabel, x, targetSqlType, scaleOrLength);
  }

  @Override
  public void updateObject(int columnIndex, Object x, SQLType targetSqlType) throws SQLException {
    checkOpen();
    super.updateObject(columnIndex, x, targetSqlType);
  }

  @Override
  public void updateObject(String columnLabel, Object x, SQLType targetSqlType) throws SQLException {
    checkOpen();
    super.updateObject(columnLabel, x, targetSqlType);
  }

  @Override
  public void updateNull(String columnLabel) throws SQLException {
    checkOpen();
    try {
      super.updateNull(columnLabel);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBoolean(String columnLabel, boolean x) throws SQLException {
    checkOpen();
    try {
      super.updateBoolean(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateByte(String columnLabel, byte x) throws SQLException {
    checkOpen();
    try {
      super.updateByte(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateShort(String columnLabel, short x) throws SQLException {
    checkOpen();
    try {
      super.updateShort(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateInt(String columnLabel, int x) throws SQLException {
    checkOpen();
    try {
      super.updateInt(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateLong(String columnLabel, long x) throws SQLException {
    checkOpen();
    try {
      super.updateLong(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateFloat(String columnLabel, float x) throws SQLException {
    checkOpen();
    try {
      super.updateFloat(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateDouble(String columnLabel, double x) throws SQLException {
    checkOpen();
    try {
      super.updateDouble(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBigDecimal(String columnLabel,
                               BigDecimal x) throws SQLException {
    checkOpen();
    try {
      super.updateBigDecimal(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateString(String columnLabel, String x) throws SQLException {
    checkOpen();
    try {
      super.updateString(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBytes(String columnLabel, byte[] x) throws SQLException {
    checkOpen();
    try {
      super.updateBytes(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateDate(String columnLabel, Date x) throws SQLException {
    checkOpen();
    try {
      super.updateDate(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateTime(String columnLabel, Time x) throws SQLException {
    checkOpen();
    try {
      super.updateTime(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
    checkOpen();
    try {
      super.updateTimestamp(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateAsciiStream(String columnLabel, InputStream x,
                                int length) throws SQLException {
    checkOpen();
    try {
      super.updateAsciiStream(columnLabel, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBinaryStream(String columnLabel, InputStream x,
                                 int length) throws SQLException {
    checkOpen();
    try {
      super.updateBinaryStream(columnLabel, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateCharacterStream(String columnLabel, Reader reader,
                                    int length) throws SQLException {
    checkOpen();
    try {
      super.updateCharacterStream(columnLabel, reader, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateObject(String columnLabel, Object x,
                           int scaleOrLength) throws SQLException {
    checkOpen();
    try {
      super.updateObject(columnLabel, x, scaleOrLength);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateObject(String columnLabel, Object x) throws SQLException {
    checkOpen();
    try {
      super.updateObject(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void insertRow() throws SQLException {
    checkOpen();
    try {
      super.insertRow();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateRow() throws SQLException {
    checkOpen();
    try {
      super.updateRow();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void deleteRow() throws SQLException {
    checkOpen();
    try {
      super.deleteRow();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void refreshRow() throws SQLException {
    checkOpen();
    try {
      super.refreshRow();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void cancelRowUpdates() throws SQLException {
    checkOpen();
    try {
      super.cancelRowUpdates();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void moveToInsertRow() throws SQLException {
    checkOpen();
    try {
      super.moveToInsertRow();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void moveToCurrentRow() throws SQLException {
    checkOpen();
    try {
      super.moveToCurrentRow();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  //-------------------------- JDBC 3.0 ----------------------------------------

  @Override
  public void updateRef(int columnIndex, Ref x) throws SQLException {
    checkOpen();
    try {
      super.updateRef(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateRef(String columnLabel, Ref x) throws SQLException {
    checkOpen();
    try {
      super.updateRef(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBlob(int columnIndex, Blob x) throws SQLException {
    checkOpen();
    try {
      super.updateBlob(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBlob(String columnLabel, Blob x) throws SQLException {
    checkOpen();
    try {
      super.updateBlob(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateClob(int columnIndex, Clob x) throws SQLException {
    checkOpen();
    try {
      super.updateClob(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateClob(String columnLabel, Clob x) throws SQLException {
    checkOpen();
    try {
      super.updateClob(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateArray(int columnIndex, Array x) throws SQLException {
    checkOpen();
    try {
      super.updateArray(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateArray(String columnLabel, Array x) throws SQLException {
    checkOpen();
    try {
      super.updateArray(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  //------------------------- JDBC 4.0 -----------------------------------
  @Override
  public RowId getRowId(int columnIndex) throws SQLException {
    checkOpen();
    try {
      return super.getRowId(columnIndex);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public RowId getRowId(String columnLabel) throws SQLException {
    checkOpen();
    try {
      return super.getRowId(columnLabel);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateRowId(int columnIndex, RowId x) throws SQLException {
    checkOpen();
    try {
      super.updateRowId(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateRowId(String columnLabel, RowId x) throws SQLException {
    checkOpen();
    try {
      super.updateRowId(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNString(int columnIndex, String nString) throws SQLException {
    checkOpen();
    try {
      super.updateNString(columnIndex, nString);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNString(String columnLabel,
                            String nString) throws SQLException {
    checkOpen();
    try {
      super.updateNString(columnLabel, nString);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    checkOpen();
    try {
      super.updateNClob(columnIndex, nClob);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
    checkOpen();
    try {
      super.updateNClob(columnLabel, nClob);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateSQLXML(int columnIndex,
                           SQLXML xmlObject) throws SQLException {
    checkOpen();
    try {
      super.updateSQLXML(columnIndex, xmlObject);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateSQLXML(String columnLabel,
                           SQLXML xmlObject) throws SQLException {
    checkOpen();
    try {
      super.updateSQLXML(columnLabel, xmlObject);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNCharacterStream(int columnIndex, Reader x,
                                     long length) throws SQLException {
    checkOpen();
    try {
      super.updateNCharacterStream(columnIndex, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNCharacterStream(String columnLabel, Reader reader,
                                     long length) throws SQLException {
    checkOpen();
    try {
      super.updateNCharacterStream(columnLabel, reader, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateAsciiStream(int columnIndex, InputStream x,
                                long length) throws SQLException {
    checkOpen();
    try {
      super.updateAsciiStream(columnIndex, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBinaryStream(int columnIndex, InputStream x,
                                 long length) throws SQLException {
    checkOpen();
    try {
      super.updateBinaryStream(columnIndex, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateCharacterStream(int columnIndex, Reader x,
                                    long length) throws SQLException {
    checkOpen();
    try {
      super.updateCharacterStream(columnIndex, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateAsciiStream(String columnLabel, InputStream x,
                                long length) throws SQLException {
    checkOpen();
    try {
      super.updateAsciiStream(columnLabel, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBinaryStream(String columnLabel, InputStream x,
                                 long length) throws SQLException {
    checkOpen();
    try {
      super.updateBinaryStream(columnLabel, x, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateCharacterStream(String columnLabel, Reader reader,
                                    long length) throws SQLException {
    checkOpen();
    try {
      super.updateCharacterStream(columnLabel, reader, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBlob(int columnIndex, InputStream inputStream,
                         long length) throws SQLException {
    checkOpen();
    try {
      super.updateBlob(columnIndex, inputStream, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBlob(String columnLabel, InputStream inputStream,
                         long length) throws SQLException {
    checkOpen();
    try {
      super.updateBlob(columnLabel, inputStream, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateClob(int columnIndex, Reader reader,
                         long length) throws SQLException {
    checkOpen();
    try {
      super.updateClob(columnIndex, reader, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateClob(String columnLabel, Reader reader,
                         long length) throws SQLException {
    checkOpen();
    try {
      super.updateClob(columnLabel, reader, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNClob(int columnIndex, Reader reader,
                          long length) throws SQLException {
    checkOpen();
    try {
      super.updateNClob(columnIndex, reader, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNClob(String columnLabel, Reader reader,
                          long length) throws SQLException {
    checkOpen();
    try {
      super.updateNClob(columnLabel, reader, length);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  //---
  @Override
  public void updateNCharacterStream(int columnIndex,
                                     Reader x) throws SQLException {
    checkOpen();
    try {
      super.updateNCharacterStream(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNCharacterStream(String columnLabel,
                                     Reader reader) throws SQLException {
    checkOpen();
    try {
      super.updateNCharacterStream(columnLabel, reader);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateAsciiStream(int columnIndex,
                                InputStream x) throws SQLException {
    checkOpen();
    try {
      super.updateAsciiStream(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBinaryStream(int columnIndex,
                                 InputStream x) throws SQLException {
    checkOpen();
    try {
      super.updateBinaryStream(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateCharacterStream(int columnIndex,
                                    Reader x) throws SQLException {
    checkOpen();
    try {
      super.updateCharacterStream(columnIndex, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateAsciiStream(String columnLabel,
                                InputStream x) throws SQLException {
    checkOpen();
    try {
      super.updateAsciiStream(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBinaryStream(String columnLabel,
                                 InputStream x) throws SQLException {
    checkOpen();
    try {
      super.updateBinaryStream(columnLabel, x);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateCharacterStream(String columnLabel,
                                    Reader reader) throws SQLException {
    checkOpen();
    try {
      super.updateCharacterStream(columnLabel, reader);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBlob(int columnIndex,
                         InputStream inputStream) throws SQLException {
    checkOpen();
    try {
      super.updateBlob(columnIndex, inputStream);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateBlob(String columnLabel,
                         InputStream inputStream) throws SQLException {
    checkOpen();
    try {
      super.updateBlob(columnLabel, inputStream);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateClob(int columnIndex, Reader reader) throws SQLException {
    checkOpen();
    try {
      super.updateClob(columnIndex, reader);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateClob(String columnLabel, Reader reader) throws SQLException {
    checkOpen();
    try {
      super.updateClob(columnLabel, reader);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNClob(int columnIndex, Reader reader) throws SQLException {
    checkOpen();
    try {
      super.updateNClob(columnIndex, reader);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void updateNClob(String columnLabel, Reader reader) throws SQLException {
    checkOpen();
    try {
      super.updateNClob(columnLabel, reader);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  ////////////////////////////////////////
  // DrillResultSet methods:

  @Override
  public String getQueryId() throws SQLException {
    checkOpen();
    if (cursor instanceof DrillCursor) {
      return ((DrillCursor) cursor).getQueryId();
    }
    return null;
  }


  ////////////////////////////////////////

  @Override
  protected DrillResultSetImpl execute() throws SQLException {
    connection.getDriver().handler.onStatementExecute(statement, null);

    if (signature.cursorFactory != null) {
      super.execute();
    } else {
      DrillCursor drillCursor = new DrillCursor(connection, statement, signature);
      //Getting handle to elapsed timer for timeout purposes
      this.elapsedTimer = drillCursor.getElapsedTimer();
      //Setting this to ensure future calls to change timeouts for an active Statement doesn't affect ResultSet
      this.queryTimeoutInMilliseconds = drillCursor.getTimeoutInMilliseconds();
      super.execute2(drillCursor, this.signature.columns);

      // Read first (schema-only) batch to initialize result-set metadata from
      // (initial) schema before Statement.execute...(...) returns result set:
      drillCursor.loadInitialSchema();
    }

    return this;
  }
}
