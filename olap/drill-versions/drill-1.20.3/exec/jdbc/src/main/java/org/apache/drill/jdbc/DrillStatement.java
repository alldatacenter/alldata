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

import org.apache.calcite.avatica.AvaticaResultSet;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Drill-specific {@link Statement}.
 * @see #unwrap
 */
public interface DrillStatement extends Statement {

  /**
   * @throws  AlreadyClosedSqlException
   *            if connection is closed
   * @throws  SQLException
   *            Any other exception
   */
  @Override
  int getQueryTimeout() throws AlreadyClosedSqlException, SQLException;

  /**
   * <strong>Drill</strong>:
   * Supported (for non-zero timeout value).
   * @throws  AlreadyClosedSqlException
   *            if connection is closed
   * @throws  JdbcApiSqlException
   *            if an invalid parameter value is detected (and not above case)
   * @throws  SQLException
   *            Any other exception
   */
  @Override
  void setQueryTimeout( int seconds )
      throws AlreadyClosedSqlException,
             JdbcApiSqlException,
             SQLException;

  /**
   * {@inheritDoc}
   * <p>
   *   <strong>Drill</strong>: Does not throw SQLException.
   * </p>
   */
  @Override
  boolean isClosed();

  void setResultSet(AvaticaResultSet resultSet);

  void setUpdateCount(int value);
}
