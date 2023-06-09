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

/**
 * SQLException for invalid-cursor-state conditions, e.g., calling a column
 * accessor method before calling {@link ResultSet#next()} or after
 * {@link ResultSet#next()} returns false.
 */
public class InvalidCursorStateSqlException extends JdbcApiSqlException {

  private static final long serialVersionUID = 2014_12_09L;


  /**
   * See {@link JdbcApiSqlException#JdbcApiSqlException(String, String, int)}.
   */
  public InvalidCursorStateSqlException( String reason,
                                         String SQLState,
                                         int vendorCode ) {
    super( reason, SQLState, vendorCode );
  }

  /**
   * See {@link JdbcApiSqlException#JdbcApiSqlException(String, String)}.
   */
  public InvalidCursorStateSqlException( String reason, String SQLState ) {
    super( reason, SQLState );
  }

  /**
   * See {@link JdbcApiSqlException#JdbcApiSqlException(String)}.
   */
  public InvalidCursorStateSqlException( String reason ) {
    super( reason );
  }

  /**
   * See {@link JdbcApiSqlException#JdbcApiSqlException()}.
   */
  public InvalidCursorStateSqlException() {
    super();
  }

  /**
   * See {@link JdbcApiSqlException#JdbcApiSqlException(Throwable cause)}.
   */
  public InvalidCursorStateSqlException( Throwable cause ) {
    super( cause );
  }

  /**
   * See {@link JdbcApiSqlException#JdbcApiSqlException(String, Throwable)}.
   */
  public InvalidCursorStateSqlException( String reason, Throwable cause ) {
    super( reason, cause );
  }

  /**
   * See
   * {@link JdbcApiSqlException#JdbcApiSqlException(String, String, Throwable)}.
   */
  public InvalidCursorStateSqlException( String reason, String sqlState,
                                         Throwable cause ) {
    super( reason, sqlState, cause );
  }

  /**
   * See
   * {@link JdbcApiSqlException#JdbcApiSqlException(String, String, int, Throwable)}.
   */
  public InvalidCursorStateSqlException( String reason,
                                         String sqlState,
                                         int vendorCode,
                                         Throwable cause ) {
    super( reason, sqlState, vendorCode, cause );
  }

}
