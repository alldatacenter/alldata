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
package org.apache.drill.exec.work.prepare;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

import io.netty.util.concurrent.Future;
import org.apache.drill.common.exceptions.ErrorHelper;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.materialize.QueryDataPackage;
import org.apache.drill.exec.proto.ExecProtos.ServerPreparedStatementState;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.proto.UserProtos.ColumnSearchability;
import org.apache.drill.exec.proto.UserProtos.ColumnUpdatability;
import org.apache.drill.exec.proto.UserProtos.CreatePreparedStatementReq;
import org.apache.drill.exec.proto.UserProtos.CreatePreparedStatementResp;
import org.apache.drill.exec.proto.UserProtos.PreparedStatement;
import org.apache.drill.exec.proto.UserProtos.PreparedStatementHandle;
import org.apache.drill.exec.proto.UserProtos.RequestStatus;
import org.apache.drill.exec.proto.UserProtos.ResultColumnMetadata;
import org.apache.drill.exec.proto.UserProtos.RpcType;
import org.apache.drill.exec.proto.UserProtos.RunQuery;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.rpc.AbstractDisposableUserClientConnection;
import org.apache.drill.exec.rpc.Acks;
import org.apache.drill.exec.rpc.Response;
import org.apache.drill.exec.rpc.ResponseSender;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.store.ischema.InfoSchemaConstants;
import org.apache.drill.exec.work.user.UserWorker;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.SocketAddress;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.drill.exec.ExecConstants.CREATE_PREPARE_STATEMENT_TIMEOUT_MILLIS;
import static org.apache.drill.exec.proto.UserProtos.RequestStatus.FAILED;
import static org.apache.drill.exec.proto.UserProtos.RequestStatus.OK;
import static org.apache.drill.exec.proto.UserProtos.RequestStatus.TIMEOUT;

/**
 * Contains worker {@link Runnable} for creating a prepared statement and helper methods.
 */
public class PreparedStatementProvider {
  private static final Logger logger = LoggerFactory.getLogger(PreparedStatementProvider.class);

  /**
   * Static list of mappings from {@link MinorType} to JDBC ResultSet class name
   * (to be returned through {@link ResultSetMetaData#getColumnClassName(int)}.
   */
  private static final Map<MinorType, String> DRILL_TYPE_TO_JDBC_CLASSNAME = ImmutableMap.<MinorType, String>builder()
      .put(MinorType.INT, Integer.class.getName())
      .put(MinorType.BIGINT, Long.class.getName())
      .put(MinorType.FLOAT4, Float.class.getName())
      .put(MinorType.FLOAT8, Double.class.getName())
      .put(MinorType.VARCHAR, String.class.getName())
      .put(MinorType.BIT, Boolean.class.getName())
      .put(MinorType.DATE, Date.class.getName())
      .put(MinorType.DECIMAL9, BigDecimal.class.getName())
      .put(MinorType.DECIMAL18, BigDecimal.class.getName())
      .put(MinorType.DECIMAL28SPARSE, BigDecimal.class.getName())
      .put(MinorType.DECIMAL38SPARSE, BigDecimal.class.getName())
      .put(MinorType.VARDECIMAL, BigDecimal.class.getName())
      .put(MinorType.TIME, Time.class.getName())
      .put(MinorType.TIMESTAMP, Timestamp.class.getName())
      .put(MinorType.VARBINARY, byte[].class.getName())
      .put(MinorType.INTERVAL, Period.class.getName())
      .put(MinorType.INTERVALYEAR, Period.class.getName())
      .put(MinorType.INTERVALDAY, Period.class.getName())
      .put(MinorType.MAP, Object.class.getName())
      .put(MinorType.LIST, Object.class.getName())
      .put(MinorType.UNION, Object.class.getName())
      .build();

  /**
   * Runnable that creates a prepared statement for given {@link CreatePreparedStatementReq} and
   * sends the response at the end.
   */
  public static class PreparedStatementWorker implements Runnable {
    private final UserClientConnection connection;
    private final UserWorker userWorker;
    private final ResponseSender responseSender;
    private final CreatePreparedStatementReq req;

    public PreparedStatementWorker(final UserClientConnection connection, final UserWorker userWorker,
        final ResponseSender responseSender, final CreatePreparedStatementReq req) {
      this.connection = connection;
      this.userWorker = userWorker;
      this.responseSender = responseSender;
      this.req = req;
    }

    @Override
    public void run() {
      final CreatePreparedStatementResp.Builder respBuilder = CreatePreparedStatementResp.newBuilder();
      try {
        UserClientConnectionWrapper wrapper = new UserClientConnectionWrapper(connection);

        final RunQuery limit0Query =
            RunQuery.newBuilder()
                .setType(QueryType.SQL)
                .setPlan(String.format("SELECT * FROM (%s) LIMIT 0", req.getSqlQuery()))
                .build();

        final QueryId limit0QueryId = userWorker.submitWork(wrapper, limit0Query);

        final long timeoutMillis =
            userWorker.getSystemOptions().getOption(CREATE_PREPARE_STATEMENT_TIMEOUT_MILLIS).num_val;

        try {
          if (!wrapper.await(timeoutMillis)) {
            logger.error("LIMIT 0 query (QueryId: {}) for prepared statement took longer than {} ms. Cancelling.",
                limit0QueryId, timeoutMillis);
            userWorker.cancelQuery(limit0QueryId);
            final String errorMsg = String.format(
                "LIMIT 0 query (QueryId: %s) for prepared statement took longer than %d ms. " +
                    "Query cancellation requested.\n" +
                    "Retry after changing the option '%s' to a higher value.",
                limit0QueryId, timeoutMillis, CREATE_PREPARE_STATEMENT_TIMEOUT_MILLIS);
            setErrorHelper(respBuilder, TIMEOUT, null, errorMsg, ErrorType.SYSTEM);
            return;
          }
        } catch (InterruptedException ex) {
          setErrorHelper(respBuilder, FAILED, ex, "Prepared statement creation interrupted.", ErrorType.SYSTEM);
          return;
        }

        if (wrapper.getError() != null) {
          setErrorHelper(respBuilder, wrapper.getError(), "Failed to get result set schema for prepare statement.");
          return;
        }

        final PreparedStatement.Builder prepStmtBuilder = PreparedStatement.newBuilder();

        for (SerializedField field : wrapper.getFields()) {
          prepStmtBuilder.addColumns(serializeColumn(field));
        }

        prepStmtBuilder.setServerHandle(
            PreparedStatementHandle.newBuilder()
                .setServerInfo(
                    ServerPreparedStatementState.newBuilder()
                        .setSqlQuery(req.getSqlQuery())
                        .build().toByteString()
                )
        );

        respBuilder.setStatus(OK);
        respBuilder.setPreparedStatement(prepStmtBuilder.build());
      } catch (Throwable e) {
        setErrorHelper(respBuilder, FAILED, e, "Failed to create prepared statement.", ErrorType.SYSTEM);
      } finally {
        responseSender.send(new Response(RpcType.PREPARED_STATEMENT, respBuilder.build()));
      }
    }
  }

  /**
   * Helper method to create {@link DrillPBError} and set it in {@code respBuilder}
   */
  private static void setErrorHelper(final CreatePreparedStatementResp.Builder respBuilder,
      final RequestStatus status,
      final Throwable ex, final String message, final ErrorType errorType) {
    respBuilder.setStatus(status);
    final String errorId = UUID.randomUUID().toString();
    if (ex != null) {
      logger.error("{} ErrorId: {}", message, errorId, ex);
    } else {
      logger.error("{} ErrorId: {}", message, errorId);
    }

    final DrillPBError.Builder builder = DrillPBError.newBuilder();
    builder.setErrorType(errorType);
    builder.setErrorId(errorId);
    builder.setMessage(message);

    if (ex != null) {
      builder.setException(ErrorHelper.getWrapper(ex));
    }

    respBuilder.setError(builder.build());
  }

  /**
   * Helper method to log error and set given {@link DrillPBError} in {@code respBuilder}
   */
  private static void setErrorHelper(final CreatePreparedStatementResp.Builder respBuilder, final DrillPBError error,
      final String message) {
    respBuilder.setStatus(FAILED);
    final String errorId = UUID.randomUUID().toString();
    logger.error("{} ErrorId: {}", message, errorId);

    respBuilder.setError(error);
  }

  /**
   * Decorator around {@link UserClientConnection} to tap the query results for LIMIT 0 query.
   */
  private static class UserClientConnectionWrapper extends AbstractDisposableUserClientConnection {
    private final UserClientConnection inner;

    private volatile List<SerializedField> fields;

    UserClientConnectionWrapper(UserClientConnection inner) {
      this.inner = inner;
    }

    @Override
    public UserSession getSession() {
      return inner.getSession();
    }

    @Override
    public Future<Void> getClosureFuture() {
      return inner.getClosureFuture();
    }

    @Override
    public SocketAddress getRemoteAddress() {
      return inner.getRemoteAddress();
    }

    @Override
    public void sendData(RpcOutcomeListener<Ack> listener, QueryDataPackage data) {
      // Save the query results schema and release the buffers.
      VectorContainer batch = data.batch();
      if (batch != null) {
        if (fields == null) {
          fields = data.fields();
        }
        batch.zeroVectors();
      }

      listener.success(Acks.OK, null);
    }

    /**
     * @return Schema returned in query result batch.
     */
    public List<SerializedField> getFields() {
      return fields;
    }
  }

  /**
   * Serialize the given {@link SerializedField} into a {@link ResultColumnMetadata}.
   * @param field
   * @return
   */
  private static ResultColumnMetadata serializeColumn(SerializedField field) {
    final ResultColumnMetadata.Builder builder = ResultColumnMetadata.newBuilder();
    final MajorType majorType = field.getMajorType();
    final MinorType minorType = majorType.getMinorType();

    /**
     * Defaults to "DRILL" as drill has as only one catalog.
     */
    builder.setCatalogName(InfoSchemaConstants.IS_CATALOG_NAME);

    /**
     * Designated column's schema name. Empty string if not applicable. Initial
     * implementation defaults to empty string as we use LIMIT 0 queries to get
     * the schema and schema info is lost. If we derive the schema from plan, we
     * may get the right value.
     */
    builder.setSchemaName("");

    /**
     * Designated column's table name. Not set if not applicable. Initial
     * implementation defaults to empty string as we use LIMIT 0 queries to get
     * the schema and table info is lost. If we derive the table from plan, we
     * may get the right value.
     */
    builder.setTableName("");

    builder.setColumnName(field.getNamePart().getName());

    /**
     * Column label name for display or print purposes.
     * Ex. a column named "empName" might be labeled as "Employee Name".
     * Initial implementation defaults to same value as column name.
     */
    builder.setLabel(field.getNamePart().getName());

    /**
     * Data type in string format. Value is SQL standard type.
     */
    builder.setDataType(Types.getSqlTypeName(majorType));

    builder.setIsNullable(majorType.getMode() == DataMode.OPTIONAL);

    /**
     * For numeric data, this is the maximum precision.
     * For character data, this is the length in characters.
     * For datetime data types, this is the length in characters of the String representation
     *    (assuming the maximum allowed precision of the fractional seconds component).
     * For binary data, this is the length in bytes.
     * For all other types 0 is returned where the column size is not applicable.
     */
    builder.setPrecision(Types.getPrecision(field.getMajorType()));

    /**
     * Column's number of digits to right of the decimal point. 0 is returned
     * for types where the scale is not applicable
     */
    builder.setScale(Types.getScale(majorType));

    /**
     * Indicates whether values in the designated column are signed numbers.
     */
    builder.setSigned(Types.isNumericType(majorType));

    /**
     * Maximum number of characters required to display data from the column.
     */
    builder.setDisplaySize(Types.getJdbcDisplaySize(majorType));

    /**
     * Is the column an aliased column. Initial implementation defaults to true
     * as we derive schema from LIMIT 0 query and not plan
     */
    builder.setIsAliased(true);

    builder.setSearchability(ColumnSearchability.ALL);
    builder.setUpdatability(ColumnUpdatability.READ_ONLY);
    builder.setAutoIncrement(false);
    builder.setCaseSensitivity(false);
    builder.setSortable(Types.isSortable(minorType));

    /**
     * Returns the fully-qualified name of the Java class whose instances are manufactured if the method
     * ResultSet.getObject is called to retrieve a value from the column. Applicable only to JDBC clients.
     */
    builder.setClassName(DRILL_TYPE_TO_JDBC_CLASSNAME.get(minorType));

    builder.setIsCurrency(false);

    return builder.build();
  }
}
