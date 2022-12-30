/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.split;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.JDBCPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbClusterInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.DbShardInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.options.JdbcReaderOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.Db2Util;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.MysqlUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.OracleUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.PostgresqlUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.SqlServerUtil;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@EqualsAndHashCode
@SuppressWarnings("checkstyle:MagicNumber")
public class DbShardWithConn {

  private final String driverClassName;
  private int queryTimeoutSeconds;
  private int queryRetryTimes = 3;

  private DbShardInfo shardInfo;
  private Connection connection;
  private BasicDataSource ds;

  private String sqlTemplateFormat;
  private String initSql;
  private PreparedStatement statement;
  final Retryer<ResultSet> statementExecutor = RetryerBuilder.<ResultSet>newBuilder()
      .retryIfExceptionOfType(SQLException.class)
      .withRetryListener(new RetryListener() {
        @Override
        public <V> void onRetry(Attempt<V> attempt) {
          if (attempt.hasException()) {
            reconnect();
            log.warn("Execute query failed, retrying. SQL: [{}]. Error: {}",
                sqlTemplateFormat, attempt.getExceptionCause().getMessage(), attempt.getExceptionCause());
          }
        }
      })
      .withStopStrategy(StopStrategies.stopAfterAttempt(queryRetryTimes))
      .withWaitStrategy(WaitStrategies.exponentialWait(100, 5, TimeUnit.MINUTES))
      .build();
  @Setter
  private boolean statementReadOnly = false;
  @Setter
  private int statementFetchSize = -1;

  public DbShardWithConn(DbShardInfo shardInfo, DbClusterInfo dbClusterInfo, String sql, BitSailConfiguration inputSliceConfig, String driverName) {
    this(shardInfo, dbClusterInfo, sql, inputSliceConfig, driverName, "");
  }

  public DbShardWithConn(DbShardInfo shardInfo, DbClusterInfo dbClusterInfo, String sql, BitSailConfiguration inputSliceConfig,
                         String driverName, String initSql) {
    this.shardInfo = shardInfo;
    this.sqlTemplateFormat = sql;
    this.queryTimeoutSeconds = inputSliceConfig.get(JdbcReaderOptions.QUERY_TIMEOUT_SECONDS);
    this.queryRetryTimes = inputSliceConfig.get(JdbcReaderOptions.QUERY_RETRY_TIMES);
    this.driverClassName = driverName;
    this.ds = getDS(dbClusterInfo);
    this.initSql = initSql;
    log.debug("DbShardWithConn query timeout seconds: {}, query retry times: {}.", queryTimeoutSeconds, queryRetryTimes);
  }

  private BasicDataSource getDS(DbClusterInfo dbClusterInfo) {
    BasicDataSource ds = new BasicDataSource();
    ds.setPoolPreparedStatements(true);
    ds.setMaxOpenPreparedStatements(20);
    ds.setUrl(shardInfo.getDbURL());
    ds.setDefaultAutoCommit(false);
    ds.setDriverClassName(driverClassName);
    ds.setUsername(dbClusterInfo.getUsername());
    ds.setPassword(dbClusterInfo.getPassword());
    ds.setInitialSize(dbClusterInfo.getInitConn());
    ds.setMaxIdle(dbClusterInfo.getMaxIdle());
    ds.setMaxActive(dbClusterInfo.getMaxActive());
    ds.setMinEvictableIdleTimeMillis(dbClusterInfo.getEvictableIdleTime());
    ds.setTestOnBorrow(true);
    ds.setRemoveAbandoned(true);
    ds.setMaxWait(15000);
    /**
     * refer different db validation query
     * https://stackoverflow.com/questions/10684244/dbcp-validationquery-for-different-databases
     */
    switch (driverClassName) {
      case (MysqlUtil.DRIVER_NAME):
      case (SqlServerUtil.DRIVER_NAME):
      case (PostgresqlUtil.DRIVER_NAME):
        ds.setValidationQuery("SELECT 1");
        break;
      case (OracleUtil.DRIVER_NAME):
        ds.setValidationQuery("SELECT 1 from dual");
        break;
      case (Db2Util.DRIVER_NAME):
        ds.setValidationQuery("SELECT 1 FROM sysibm.sysdummy1");
        break;
      default:
        throw BitSailException.asBitSailException(JDBCPluginErrorCode.CONNECTION_ERROR, "Error driver name: " + driverClassName);
    }

    return ds;
  }

  public Connection getConnection() {
    if (this.connection == null) {
      connect();
    }

    return connection;
  }

  private void connect() {
    try {
      connection = ds.getConnection();
      if (!Strings.isNullOrEmpty(initSql)) {
        Statement statement = connection.createStatement();
        statement.executeQuery(initSql);
        log.info("Init sql is configured. And the init sql " + initSql + " will be executed!");
        statement.close();
      }
    } catch (SQLException e) {
      throw BitSailException.asBitSailException(JDBCPluginErrorCode.CONNECTION_ERROR,
          "Shard info: " + shardInfo.toString() + ". Error: " + e.getMessage(), e);
    }

    if (connection == null) {
      throw BitSailException.asBitSailException(JDBCPluginErrorCode.CONNECTION_ERROR,
          "Shard info: " + shardInfo.toString() + ". Null connection.");
    }
  }

  public void reconnect() {
    statement = null;

    if (null != connection) {
      try {
        connection.close();
      } catch (SQLException e) {
        log.warn("Can't close connection. Data source info: " + ds, e);
      }
    }
    connect();
  }

  public PreparedStatement getStatement(String quoteTableWithSchema, Boolean statementRefresh) {
    String sqlTemplate = Strings.isNullOrEmpty(quoteTableWithSchema) ? sqlTemplateFormat : sqlTemplateFormat.replaceFirst("%s", quoteTableWithSchema);
    try {
      if (!statementRefresh && statement != null) {
        return statement;
      }

      if (statement != null) {
        statement.close();
      }

      statement = getConnection().prepareStatement(sqlTemplate);

      // enable streaming result set by set fetch size to a positive value.
      if (statementReadOnly) {
        statement.setFetchSize(1);
      }

      if (statementFetchSize > 0) {
        statement.setFetchSize(statementFetchSize);
      }

      statement.setQueryTimeout(queryTimeoutSeconds);
      log.info("Prepared statement for sql {}, read only {}, fetch size {}.", sqlTemplate, statementReadOnly, statementFetchSize);
      return statement;
    } catch (SQLException e) {
      throw BitSailException.asBitSailException(JDBCPluginErrorCode.CONNECTION_ERROR,
          "Error while prepare fetch statement " + sqlTemplate + e.getMessage(), e);
    }
  }

  public ResultSet executeWithRetry(SQLFunction<PreparedStatement, ResultSet> statementConsumer, String quoteTableWithSchema, Boolean refreshStatement) {

    try {
      return statementExecutor.call(() -> statementConsumer.apply(getStatement(quoteTableWithSchema, refreshStatement)));
    } catch (ExecutionException | RetryException e) {
      log.error("Execute query failed. SQL: [{}]. Error: {}", sqlTemplateFormat, e.getMessage(), e);
      throw new IllegalStateException("Execute query failed." + e.getMessage(), e);
    }
  }

  public void setSql(String sqlTemplateFormat) {
    this.sqlTemplateFormat = sqlTemplateFormat;
  }

  public void close() {
    try {
      if (statement != null) {
        statement.close();
      }
      if (connection != null) {
        connection.close();
      }
      if (ds != null) {
        ds.close();
      }
    } catch (SQLException e) {
      log.error("Error while closing DbShardWithConn.", e);
      // ignore
    }
  }

  public interface SQLFunction<T, R> {
    R apply(T t) throws SQLException;
  }
}
