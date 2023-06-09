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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.calcite.avatica.AvaticaConnection;
import org.apache.calcite.avatica.AvaticaStatement;
import org.apache.calcite.avatica.Helper;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.Meta.StatementHandle;
import org.apache.calcite.avatica.QueryState;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.client.ServerMethod;
import org.apache.drill.exec.proto.UserProtos.CreatePreparedStatementResp;
import org.apache.drill.exec.proto.UserProtos.RequestStatus;
import org.apache.drill.exec.rpc.DrillRpcFuture;


/**
 * Implementation of {@link org.apache.calcite.avatica.AvaticaFactory} for Drill and
 * JDBC 4.1 (corresponds to JDK 1.7).
 */
// Note:  Must be public so org.apache.calcite.avatica.UnregisteredDriver can
// (reflectively) call no-args constructor.
public class DrillJdbc41Factory extends DrillFactory {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(DrillJdbc41Factory.class);

  /** Creates a factory for JDBC version 4.1. */
  // Note:  Must be public so org.apache.calcite.avatica.UnregisteredDriver can
  // (reflectively) call this constructor.
  public DrillJdbc41Factory() {
    this(4, 1);
  }

  /** Creates a JDBC factory with given major/minor version number. */
  protected DrillJdbc41Factory(int major, int minor) {
    super(major, minor);
  }


  @Override
  DrillConnectionImpl newDrillConnection(DriverImpl driver,
                                         DrillFactory factory,
                                         String url,
                                         Properties info) throws SQLException {
    return new DrillConnectionImpl(driver, factory, url, info);
  }

  @Override
  public DrillDatabaseMetaDataImpl newDatabaseMetaData(AvaticaConnection connection) {
    return new DrillDatabaseMetaDataImpl((DrillConnectionImpl) connection);
  }


  @Override
  public DrillStatementImpl newStatement(AvaticaConnection connection,
                                         StatementHandle h,
                                         int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability) {
    return new DrillStatementImpl((DrillConnectionImpl) connection,
                                  h,
                                  resultSetType,
                                  resultSetConcurrency,
                                  resultSetHoldability);
  }

  @Override
  public DrillJdbc41PreparedStatement newPreparedStatement(AvaticaConnection connection,
                                                       StatementHandle h,
                                                       Meta.Signature signature,
                                                       int resultSetType,
                                                       int resultSetConcurrency,
                                                       int resultSetHoldability)
      throws SQLException {
    DrillConnectionImpl drillConnection = (DrillConnectionImpl) connection;
    DrillClient client = drillConnection.getClient();
    if (drillConnection.getConfig().isServerPreparedStatementDisabled() || !client.getSupportedMethods().contains(ServerMethod.PREPARED_STATEMENT)) {
      // fallback to client side prepared statement
      return new DrillJdbc41PreparedStatement(drillConnection, h, signature, null, resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    return newServerPreparedStatement(drillConnection, h, signature, resultSetType,
        resultSetConcurrency, resultSetHoldability);
  }

  private DrillJdbc41PreparedStatement newServerPreparedStatement(DrillConnectionImpl connection,
                                                                  StatementHandle h,
                                                                  Meta.Signature signature,
                                                                  int resultSetType,
                                                                  int resultSetConcurrency,
                                                                  int resultSetHoldability
      ) throws SQLException {
    String sql = signature.sql;

    try {
      DrillRpcFuture<CreatePreparedStatementResp> respFuture = connection.getClient().createPreparedStatement(signature.sql);

      CreatePreparedStatementResp resp;
      try {
        resp = respFuture.get();
      } catch (InterruptedException e) {
        // Preserve evidence that the interruption occurred so that code higher up
        // on the call stack can learn of the interruption and respond to it if it
        // wants to.
        Thread.currentThread().interrupt();

        throw new SQLException( "Interrupted", e );
      }

      final RequestStatus status = resp.getStatus();
      if (status != RequestStatus.OK) {
        final String errMsgFromServer = resp.getError() != null ? resp.getError().getMessage() : "";

        if (status == RequestStatus.TIMEOUT) {
          logger.error("Request timed out to create prepare statement: {}", errMsgFromServer);
          throw new SQLTimeoutException("Failed to create prepared statement: " + errMsgFromServer);
        }

        if (status == RequestStatus.FAILED) {
          logger.error("Failed to create prepared statement: {}", errMsgFromServer);
          throw new SQLException("Failed to create prepared statement: " + errMsgFromServer);
        }

        logger.error("Failed to create prepared statement. Unknown status: {}, Error: {}", status, errMsgFromServer);
        throw new SQLException(String.format(
            "Failed to create prepared statement. Unknown status: %s, Error: %s", status, errMsgFromServer));
      }

      return new DrillJdbc41PreparedStatement(connection,
          h,
          signature,
          resp.getPreparedStatement(),
          resultSetType,
          resultSetConcurrency,
          resultSetHoldability);
    } catch (SQLException e) {
      throw e;
    } catch (Exception e) {
      throw Helper.INSTANCE.createException("Error while preparing statement [" + sql + "]", e);
    }
  }

  @Override
  public DrillResultSetImpl newResultSet(AvaticaStatement statement,
                                         QueryState state,
                                         Meta.Signature signature,
                                         TimeZone timeZone,
                                         Meta.Frame firstFrame) throws SQLException {
    final ResultSetMetaData metaData =
        newResultSetMetaData(statement, signature);
    return new DrillResultSetImpl(statement, state, signature, metaData, timeZone, firstFrame);
  }

  @Override
  public ResultSetMetaData newResultSetMetaData(AvaticaStatement statement,
                                                Meta.Signature signature) {
    return new DrillResultSetMetaDataImpl(statement, null, signature);
  }


  /**
   * JDBC 4.1 version of {@link DrillPreparedStatementImpl}.
   */
  private static class DrillJdbc41PreparedStatement extends DrillPreparedStatementImpl {

    DrillJdbc41PreparedStatement(DrillConnectionImpl connection,
                                 StatementHandle h,
                                 Meta.Signature signature,
                                 org.apache.drill.exec.proto.UserProtos.PreparedStatement pstmt,
                                 int resultSetType,
                                 int resultSetConcurrency,
                                 int resultSetHoldability) throws SQLException {
      super(connection, h, signature, pstmt,
            resultSetType, resultSetConcurrency, resultSetHoldability);
    }
  }

}

// End DrillJdbc41Factory.java
