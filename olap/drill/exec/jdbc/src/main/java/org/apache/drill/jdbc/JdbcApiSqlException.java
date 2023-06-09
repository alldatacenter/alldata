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

import java.sql.SQLNonTransientException;

/**
 * SQLException for JDBC API calling-sequence/state problems.
 *
 * <p>
 *   {@code JdbcApiSqlException} is intended for errors in using the JDBC API,
 *   such as calling {@link ResultSet#getString} before calling
 *   {@link ResultSet#next}.
 * </p>
 * <p>
 *   ({@code JdbcApiSqlException} is not for errors that are not under direct
 *   control of the programmer writing JDBC API calls, for example, invalid SQL
 *   syntax errors (which should use {@link SQLSyntaxErrorException}), errors
 *   from SQL-vs.-data mismatches (which likely should use {@link SQLDataException}),
 *   data file format errors, resource availability errors (which might use
 *   {@link SQLTransientException}), or internal Drill errors.)
 * </p>
 * <p>
 *  TODO:  Consider using ANSI-/XOPEN-standard SQL State values.  (See:
 * </p>
 * <ul>
 *   <li>
 *     <a href="
 *       http://stackoverflow.com/questions/1399574/what-are-all-the-possible-values-for-sqlexception-getsqlstate
 *       ">
 *      http://stackoverflow.com/questions/1399574/what-are-all-the-possible-values-for-sqlexception-getsqlstate
 *     </a>
 *   </li>
 *   <li>
 *     <a href="
 *       https://github.com/olamedia/kanon/blob/master/src/mvc-model/storageDrivers/SQLSTATE.txt
 *     ">
 *       https://github.com/olamedia/kanon/blob/master/src/mvc-model/storageDrivers/SQLSTATE.txt
 *     </a>
 *   </li>
 *   <li>
 *     <a href="
 *       http://kanon-framework.googlecode.com/svn/trunk/src/mvc-model/storageDrivers/SQLSTATE.txt
 *     ">
 *       http://kanon-framework.googlecode.com/svn/trunk/src/mvc-model/storageDrivers/SQLSTATE.txt
 *     </a>
 *   </li>
 *   <li>
 *     <a href="
 *       http://www-01.ibm.com/support/knowledgecenter/api/content/nl/en-us/SSVHEW_6.2.0/com.ibm.rcp.tools.doc.db2e/adg/sql11.html
 *     ">
 *       http://www-01.ibm.com/support/knowledgecenter/api/content/nl/en-us/SSVHEW_6.2.0/com.ibm.rcp.tools.doc.db2e/adg/sql11.html
 *     </a>
 *   </li>
 *   <li>
 *     <a href="
 *       ftp://ftp.software.ibm.com/ps/products/db2/info/vr6/htm/db2m0/db2state.htm
 *     ">
 *       ftp://ftp.software.ibm.com/ps/products/db2/info/vr6/htm/db2m0/db2state.htm
 *     </a>
 *   </li>
 *   <li>
 *     <a href="
 *       https://docs.oracle.com/cd/E15817_01/appdev.111/b31230/ch2.htm
 *     ">
 *       https://docs.oracle.com/cd/E15817_01/appdev.111/b31230/ch2.htm
 *     </a>
 *   </li>
 * </ul>
 * <p>
 *   etc.)
 * </p>
 */
public class JdbcApiSqlException extends SQLNonTransientException {

  private static final long serialVersionUID = 2014_12_12L;


  /**
   * See {@link SQLException#SQLException(String, String, int)}.
   */
  public JdbcApiSqlException( String reason, String SQLState, int vendorCode ) {
    super( reason, SQLState, vendorCode );
  }

  /**
   * See {@link SQLException#SQLException(String, String)}.
   */
  public JdbcApiSqlException( String reason, String SQLState ) {
    super( reason, SQLState );
  }

  /**
   * See {@link SQLException#SQLException(String)}.
   */
  public JdbcApiSqlException( String reason ) {
    super( reason );
  }

  /**
   * See {@link SQLException#SQLException()}.
   * */
  public JdbcApiSqlException() {
    super();
  }

  /**
   * See {@link SQLException#SQLException(Throwable cause)}.
   */
  public JdbcApiSqlException( Throwable cause ) {
    super( cause );
  }

  /**
   * See {@link SQLException#SQLException(String, Throwable)}.
   */
  public JdbcApiSqlException( String reason, Throwable cause ) {
    super( reason, cause );
  }

  /**
   * See {@link SQLException#SQLException(String, String, Throwable)}.
   */
  public JdbcApiSqlException( String reason, String sqlState, Throwable cause ) {
    super( reason, sqlState, cause );
  }

  /**
   * See {@link SQLException#SQLException(String, String, int, Throwable)}.
   */
  public JdbcApiSqlException( String reason,
                              String sqlState,
                              int vendorCode,
                              Throwable cause ) {
    super( reason, sqlState, vendorCode, cause );
  }

}
