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
package org.apache.drill.metastore.rdbms;

import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.JDBCUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides SQL queries executor configured based on given data source and SQL dialect.
 */
public class QueryExecutorProvider implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(QueryExecutorProvider.class);

  private final HikariDataSource dataSource;
  private final SQLDialect dialect;

  public QueryExecutorProvider(HikariDataSource dataSource) {
    this.dataSource = dataSource;
    this.dialect = defineDialect();
  }

  /**
   * Provides query executor which can be used to execute various SQL statements.
   * Executor transforms programmatically created queries into configured SQL dialect,
   * executes them using connections from provided data source. Allows to execute
   * SQL queries in transaction.
   * Note: always close executor to release open connections.
   *
   * @return query executor
   */
  public DSLContext executor() {
    return DSL.using(dataSource, dialect);
  }

  @Override
  public void close() {
    dataSource.close();
  }

  /**
   * Defines SQL dialect based on data source connection.
   * If unable to define the dialect, uses {@link SQLDialect#DEFAULT}.
   *
   * @return SQL dialect
   */
  private SQLDialect defineDialect() {
    SQLDialect dialect = SQLDialect.DEFAULT;
    try (Connection connection = dataSource.getConnection()) {
      dialect = JDBCUtils.dialect(connection);
    } catch (SQLException e) {
      logger.debug("Unable to connect to data source in order to define SQL dialect: {}", e.getMessage(), e);
      // ignore exception and fallback to default dialect
    }
    logger.info("RDBMS Metastore is configured to use {} SQL dialect", dialect);
    return dialect;
  }
}
