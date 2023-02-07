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

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.apache.calcite.avatica.AvaticaConnection;
import org.apache.calcite.avatica.AvaticaFactory;
import org.apache.calcite.avatica.AvaticaStatement;
import org.apache.calcite.avatica.Meta.ExecuteResult;
import org.apache.calcite.avatica.Meta.MetaResultSet;
import org.apache.calcite.avatica.NoSuchStatementException;
import org.apache.calcite.avatica.QueryState;
import org.apache.calcite.avatica.UnregisteredDriver;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.client.InvalidConnectionInfoException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserProtos;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.jdbc.AlreadyClosedSqlException;
import org.apache.drill.jdbc.DrillConnection;
import org.apache.drill.jdbc.DrillConnectionConfig;
import org.apache.drill.jdbc.InvalidParameterSqlException;
import org.apache.drill.jdbc.JdbcApiSqlException;
import org.slf4j.Logger;

import org.apache.drill.shaded.guava.com.google.common.base.Throwables;
import org.slf4j.LoggerFactory;

/**
 * Drill's implementation of {@link java.sql.Connection}.
 */
// (Was abstract to avoid errors _here_ if newer versions of JDBC added
// interface methods, but now newer versions would probably use Java 8's default
// methods for compatibility.)
public class DrillConnectionImpl extends AvaticaConnection implements DrillConnection {

  private static final Logger logger = LoggerFactory.getLogger(DrillConnectionImpl.class);

  final DrillStatementRegistry openStatementsRegistry = new DrillStatementRegistry();
  final DrillConnectionConfig config;

  private final DrillClient client;
  private final BufferAllocator allocator;
  private Drillbit bit;
  private RemoteServiceSet serviceSet;


  public DrillConnectionImpl(DriverImpl driver, AvaticaFactory factory,
                                String url, Properties info) throws SQLException {
    super(driver, factory, url, info);

    // Initialize transaction-related settings per Drill behavior.
    super.setTransactionIsolation(TRANSACTION_NONE);
    super.setAutoCommit(true);
    super.setReadOnly(false);

    this.config = new DrillConnectionConfig(info);
    try {
      try {
        String connect = null;

        if (config.isLocal()) {
          try {
            Class.forName("org.eclipse.jetty.server.Handler");
          } catch (ClassNotFoundException e) {
            throw new SQLNonTransientConnectionException(
                "Running Drill in embedded mode using Drill's jdbc-all JDBC"
                    + " driver Jar file alone is not supported.");
          }

          final DrillConfig dConfig = DrillConfig.create(info);
          this.allocator = RootAllocatorFactory.newRoot(dConfig);
          RemoteServiceSet set = GlobalServiceSetReference.SETS.get();
          if (set == null) {
            // We're embedded; start a local drill bit.
            serviceSet = RemoteServiceSet.getLocalServiceSet();
            set = serviceSet;
            try {
              bit = new Drillbit(dConfig, serviceSet);
              bit.run();
            } catch (final UserException e) {
              throw new SQLException(
                  "Failure in starting embedded Drillbit: " + e.getMessage(),
                  e);
            } catch (Exception e) {
              // (Include cause exception's text in wrapping exception's text so
              // it's more likely to get to user (e.g., via SQLLine), and use
              // toString() since getMessage() text doesn't always mention error:)
              throw new SQLException("Failure in starting embedded Drillbit: " + e, e);
            }
          } else {
            serviceSet = null;
            bit = null;
          }

          this.client = new DrillClient(dConfig, set.getCoordinator());
        } else if(config.isDirect()) {
          final DrillConfig dConfig = DrillConfig.forClient();
          this.allocator = RootAllocatorFactory.newRoot(dConfig);
          this.client = new DrillClient(dConfig, true); // Get a direct connection
          connect = config.getZookeeperConnectionString();
        } else {
          final DrillConfig dConfig = DrillConfig.forClient();
          this.allocator = RootAllocatorFactory.newRoot(dConfig);
          // TODO:  Check:  Why does new DrillClient() create another DrillConfig,
          // with enableServerConfigs true, and cause scanning for function
          // implementations (needed by a server, but not by a client-only
          // process, right?)?  Probably pass dConfig to construction.
          this.client = new DrillClient();
          connect = config.getZookeeperConnectionString();
        }
        this.client.setClientName("Apache Drill JDBC Driver");
        this.client.connect(connect, info);
      } catch (OutOfMemoryException e) {
        throw new SQLNonTransientConnectionException("Failure creating root allocator", e);
      } catch (InvalidConnectionInfoException e) {
        throw new SQLNonTransientConnectionException("Invalid parameter in connection string: " + e.getMessage(), e);
      } catch (RpcException e) {
        // (Include cause exception's text in wrapping exception's text so
        // it's more likely to get to user (e.g., via SQLLine), and use
        // toString() since getMessage() text doesn't always mention error:)
        throw new SQLNonTransientConnectionException("Failure in connecting to Drill: " + e, e);
      } catch(SQLException e) {
        throw e;
      } catch (Exception e) {
        throw new SQLException("Failure in creating DrillConnectionImpl: " + e, e);
      }
    } catch (Throwable t) {
      close();
      throw t;
    }
  }


  @Override
  protected ResultSet createResultSet(MetaResultSet metaResultSet, QueryState state) throws SQLException {
    return super.createResultSet(metaResultSet, state);
  }

  @Override
  protected ExecuteResult prepareAndExecuteInternal(AvaticaStatement statement, String sql, long maxRowCount)
      throws SQLException, NoSuchStatementException {
    try {
      return super.prepareAndExecuteInternal(statement, sql, maxRowCount);
    } catch (RuntimeException e) {
      Throwables.throwIfInstanceOf(e.getCause(), SQLException.class);
      throw e;
    }
  }
  /**
   * Throws AlreadyClosedSqlException <i>iff</i> this Connection is closed.
   *
   * @throws  AlreadyClosedSqlException  if Connection is closed
   */
  @Override
  protected void checkOpen() throws AlreadyClosedSqlException {
    if (isClosed()) {
      throw new AlreadyClosedSqlException("Connection is already closed.");
    }
  }

  @Override
  public DrillConnectionConfig getConfig() {
    return config;
  }

  BufferAllocator getAllocator() {
    return allocator;
  }

  @Override
  public DrillClient getClient() {
    return client;
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    this.checkOpen();
  }

  @Override
  public void commit() throws SQLException {
    checkOpen();
    if (getAutoCommit()) {
      throw new JdbcApiSqlException("Can't call commit() in auto-commit mode.");
    } else {
      // (Currently not reachable.)
      throw new SQLFeatureNotSupportedException(
          "Connection.commit() is not supported.  (Drill is not transactional.)");
    }
  }

  @Override
  public void rollback() throws SQLException {
    checkOpen();
    if (getAutoCommit()) {
      throw new JdbcApiSqlException("Can't call rollback() in auto-commit mode.");
    } else {
      // (Currently not reachable.)
      throw new SQLFeatureNotSupportedException(
          "Connection.rollback() is not supported.  (Drill is not transactional.)");
    }
  }


  @Override
  public boolean isClosed() {
    try {
      if (super.isClosed()) {
        return true;
      } else {
        return client != null ? !client.connectionIsActive() : false;
      }
    } catch (SQLException e) {
      // Currently can't happen, since AvaticaConnection.isClosed() never throws
      // SQLException.
      throw new DrillRuntimeException(
          "Unexpected exception from " + getClass().getSuperclass()
            + ".isClosed(): " + e,
          e );
    }
  }


  @Override
  public Savepoint setSavepoint() throws SQLException {
    checkOpen();
    throw new SQLFeatureNotSupportedException(
        "Savepoints are not supported. (Drill is not transactional.)" );
  }

  @Override
  public Savepoint setSavepoint(String name) throws SQLException {
    checkOpen();
    throw new SQLFeatureNotSupportedException(
        "Savepoints are not supported. (Drill is not transactional.)" );
  }

  @Override
    public void rollback(Savepoint savepoint) throws SQLException {
    checkOpen();
    throw new SQLFeatureNotSupportedException(
        "Savepoints are not supported. (Drill is not transactional.)" );
  }

  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    checkOpen();
    throw new SQLFeatureNotSupportedException(
        "Savepoints are not supported. (Drill is not transactional.)" );
  }


  private String isolationValueToString(int level) {
    switch (level) {
      case TRANSACTION_NONE:
        return "TRANSACTION_NONE";
      case TRANSACTION_READ_UNCOMMITTED:
        return "TRANSACTION_READ_UNCOMMITTED";
      case TRANSACTION_READ_COMMITTED:
        return "TRANSACTION_READ_COMMITTED";
      case TRANSACTION_REPEATABLE_READ:
        return "TRANSACTION_REPEATABLE_READ";
      case TRANSACTION_SERIALIZABLE:
        return "TRANSACTION_SERIALIZABLE";
      default:
        return "<Unknown transaction isolation level value " + level + ">";
    }
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    checkOpen();
    switch (level) {
      case TRANSACTION_NONE:
        // No-op.  (Is already set in constructor, and we disallow changing it.)
        break;
      case TRANSACTION_READ_UNCOMMITTED:
      case TRANSACTION_READ_COMMITTED:
      case TRANSACTION_REPEATABLE_READ:
      case TRANSACTION_SERIALIZABLE:
        throw new SQLFeatureNotSupportedException(
            "Can't change transaction isolation level to Connection."
                + isolationValueToString(level) + " (from Connection."
                + isolationValueToString(getTransactionIsolation()) + "). "
                + "(Drill is not transactional.)");
      default:
        // Invalid value (or new one unknown to code).
        throw new JdbcApiSqlException(
            "Invalid transaction isolation level value " + level);
        //break;
    }
  }

  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds)
      throws JdbcApiSqlException, SQLFeatureNotSupportedException {
    checkOpen();
    if (null == executor) {
      throw new InvalidParameterSqlException(
          "Invalid (null) \"executor\" parameter to setNetworkTimeout(...)");
    } else if (milliseconds < 0) {
      throw new InvalidParameterSqlException(
          "Invalid (negative) \"milliseconds\" parameter to"
          + " setNetworkTimeout(...) (" + milliseconds + ")");
    } else {
      if (0 != milliseconds) {
        throw new SQLFeatureNotSupportedException(
            "Setting network timeout is not supported.");
      }
    }
  }

  @Override
  public int getNetworkTimeout() throws AlreadyClosedSqlException {
    checkOpen();
    return 0;  // (No timeout.)
  }

  @Override
  public DrillStatementImpl createStatement(int resultSetType,
                                            int resultSetConcurrency,
                                            int resultSetHoldability) throws SQLException {
    return (DrillStatementImpl) super.createStatement(resultSetType,
        resultSetConcurrency, resultSetHoldability);
  }

  @Override
  public TimeZone getTimeZone() {
    return config.getTimeZone();
  }


  // Note:  Using dynamic proxies would reduce the quantity (450?) of method
  // overrides by eliminating those that exist solely to check whether the
  // object is closed.  It would also eliminate the need to throw non-compliant
  // RuntimeExceptions when Avatica's method declarations won't let us throw
  // proper SQLExceptions. (Check performance before applying to frequently
  // called ResultSet.)

  // No isWrapperFor(Class<?>) (it doesn't throw SQLException if already closed).
  // No unwrap(Class<T>) (it doesn't throw SQLException if already closed).

  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {
    checkOpen();
    try {
      return super.prepareCall(sql);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public String nativeSQL(String sql) throws SQLException {
    checkOpen();
    try {
      return super.nativeSQL(sql);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  // No close() (it doesn't throw SQLException if already closed).

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType,
                                       int resultSetConcurrency) throws SQLException {
    checkOpen();
    try {
      return super.prepareCall(sql, resultSetType, resultSetConcurrency);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public Map<String,Class<?>> getTypeMap() throws SQLException {
    checkOpen();
    try {
      return super.getTypeMap();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void setTypeMap(Map<String,Class<?>> map) throws SQLException {
    checkOpen();
    try {
      super.setTypeMap(map);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType,
                                       int resultSetConcurrency,
                                       int resultSetHoldability) throws SQLException {
    checkOpen();
    try {
      return super.prepareCall(sql, resultSetType, resultSetConcurrency,
        resultSetHoldability);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public PreparedStatement prepareStatement(String sql,
                                            int autoGeneratedKeys) throws SQLException {
    checkOpen();
    try {
      return super.prepareStatement(sql, autoGeneratedKeys);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public PreparedStatement prepareStatement(String sql,
                                            int columnIndexes[]) throws SQLException {
    checkOpen();
    try {
      return super.prepareStatement(sql, columnIndexes);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public PreparedStatement prepareStatement(String sql,
                                            String columnNames[]) throws SQLException {
    checkOpen();
    try {
      return super.prepareStatement(sql, columnNames);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public Clob createClob() throws SQLException {
    checkOpen();
    try {
      return super.createClob();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public Blob createBlob() throws SQLException {
    checkOpen();
    try {
      return super.createBlob();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public NClob createNClob() throws SQLException {
    checkOpen();
    try {
      return super.createNClob();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {
    checkOpen();
    try {
      return super.createSQLXML();
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public boolean isValid(int timeout) throws SQLException {
    if (timeout < 0) {
      throw new SQLException(String.format("Invalid timeout (%d<0).", timeout));
    }
    return !isClosed()
      && client.hasPing(timeout);
  }

  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    try {
      checkOpen();
    } catch (AlreadyClosedSqlException e) {
      throw new SQLClientInfoException(e.getMessage(), null, e);
    }
    try {
      super.setClientInfo(name, value);
    } catch (UnsupportedOperationException e) {
      SQLFeatureNotSupportedException intended =
          new SQLFeatureNotSupportedException(e.getMessage(), e);
      throw new SQLClientInfoException(e.getMessage(), null, intended);
    }
  }

  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    try {
      checkOpen();
    } catch (AlreadyClosedSqlException e) {
      throw new SQLClientInfoException(e.getMessage(), null, e);
    }
    try {
      super.setClientInfo(properties);
    } catch (UnsupportedOperationException e) {
      SQLFeatureNotSupportedException intended =
          new SQLFeatureNotSupportedException(e.getMessage(), e);
      throw new SQLClientInfoException(e.getMessage(), null, intended);
    }
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    checkOpen();
    try {
      return super.createStruct(typeName, attributes);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  @Override
  public void setSchema(String schema) throws SQLException {
    checkOpen();
    try {
      client.runQuery(UserBitShared.QueryType.SQL, String.format("use %s", schema));
    } catch (RpcException e) {
      throw new SQLException("Error when setting schema", e);
    }
  }

  @Override
  public String getSchema() throws SQLException {
    checkOpen();
    try {
      UserProtos.GetServerMetaResp response = client.getServerMeta().get();
      if (response.getStatus() != UserProtos.RequestStatus.OK) {
        UserBitShared.DrillPBError drillError = response.getError();
        throw new SQLException("Error when getting server meta: " + drillError.getMessage());
      }
      UserProtos.ServerMeta serverMeta = response.getServerMeta();
      String currentSchema = serverMeta.hasCurrentSchema() ? serverMeta.getCurrentSchema() : null;
      return StringUtils.isEmpty(currentSchema) ? null : currentSchema;
    } catch (InterruptedException | ExecutionException e) {
      throw new SQLException("Error when getting server meta", e);
    }
  }

  @Override
  public void abort(Executor executor) throws SQLException {
    checkOpen();
    try {
      super.abort(executor);
    } catch (UnsupportedOperationException e) {
      throw new SQLFeatureNotSupportedException(e.getMessage(), e);
    }
  }

  // do not make public
  UnregisteredDriver getDriver() {
    return driver;
  }

  // do not make public
  AvaticaFactory getFactory() {
    return factory;
  }

  private static void closeOrWarn(AutoCloseable autoCloseable, String message, Logger logger) {
    if (autoCloseable == null) {
      return;
    }

    try {
      autoCloseable.close();
    } catch(Exception e) {
      logger.warn(message, e);
    }
  }

  @Override
  public void close() throws SQLException {
    super.close();
    // First close any open JDBC Statement objects, to close any open ResultSet
    // objects and release their buffers/vectors.
    openStatementsRegistry.close();

    // TODO all of these should use DeferredException when it is available from DRILL-2245
    closeOrWarn(client, "Exception while closing client.", logger);
    closeOrWarn(allocator, "Exception while closing allocator.", logger);

    if (bit != null) {
      bit.close();
    }

    closeOrWarn(serviceSet, "Exception while closing service set.", logger);
  }

  protected Drillbit getDrillbit() {
    return bit;
  }
}
