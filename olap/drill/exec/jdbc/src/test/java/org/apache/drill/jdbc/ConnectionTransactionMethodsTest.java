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

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import org.apache.drill.categories.JdbcTest;
import org.apache.drill.test.BaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.sql.Connection;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;
import java.sql.SQLException;

/**
 * Test for Drill's implementation of Connection's main transaction-related
 * methods.
 */
@Category(JdbcTest.class)
public class ConnectionTransactionMethodsTest extends BaseTest {

  private static Connection connection;

  @BeforeClass
  public static void setUpConnection() throws SQLException {
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    connection = new Driver().connect( "jdbc:drill:zk=local", null );
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    connection.close();
  }


  ////////////////////////////////////////
  // Transaction mode methods:

  //////////
  // Transaction isolation level:

  @Test
  public void testGetTransactionIsolationSaysNone() throws SQLException {
    assertThat( connection.getTransactionIsolation(), equalTo( TRANSACTION_NONE ) );
  }

  @Test
  public void testSetTransactionIsolationNoneExitsNormally() throws SQLException {
    connection.setTransactionIsolation( TRANSACTION_NONE );
  }

  // Test trying to set to unsupported isolation levels:

  // (Sample message:  "Can't change transaction isolation level to
  // Connection.TRANSACTION_REPEATABLE_READ (from Connection.TRANSACTION_NONE).
  // (Drill is not transactional.)" (as of 2015-04-22))

  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testSetTransactionIsolationReadUncommittedThrows() throws SQLException {
    try {
      connection.setTransactionIsolation( TRANSACTION_READ_UNCOMMITTED );
    }
    catch ( SQLFeatureNotSupportedException e ) {
      // Check a few things in an error message:
      assertThat( "Missing requested-level string",
                  e.getMessage(), containsString( "TRANSACTION_READ_UNCOMMITTED" ) );
      assertThat( "Missing (or reworded) expected description",
                  e.getMessage(), containsString( "transaction isolation level" ) );
      assertThat( "Missing current-level string",
                  e.getMessage(), containsString( "TRANSACTION_NONE" ) );
      throw e;
    }
  }

  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testSetTransactionIsolationReadCommittedThrows() throws SQLException {
    connection.setTransactionIsolation( TRANSACTION_READ_COMMITTED );
  }
  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testSetTransactionIsolationRepeatableReadThrows() throws SQLException {
    connection.setTransactionIsolation( TRANSACTION_REPEATABLE_READ );
  }
  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testSetTransactionIsolationSerializableThrows() throws SQLException {
    connection.setTransactionIsolation( TRANSACTION_SERIALIZABLE );
  }

  @Test( expected = JdbcApiSqlException.class )
  public void testSetTransactionIsolationBadIntegerThrows() throws SQLException {
    connection.setTransactionIsolation( 15 );  // not any TRANSACTION_* value
  }


  //////////
  // Auto-commit mode.

  @Test
  public void testGetAutoCommitSaysAuto() throws SQLException {
    // Auto-commit should always be true.
    assertThat( connection.getAutoCommit(), equalTo( true ) );
  }

  @Test
  public void testSetAutoCommitTrueExitsNormally() throws SQLException {
    // Setting auto-commit true (redundantly) shouldn't throw exception.
    connection.setAutoCommit( true );
  }


  ////////////////////////////////////////
  // Transaction operation methods:

  @Test( expected = JdbcApiSqlException.class )
  public void testCommitThrows() throws SQLException {
    // Should fail saying because in auto-commit mode (or maybe because not
    // supported).
    connection.commit();
  }

  @Test( expected = JdbcApiSqlException.class )
  public void testRollbackThrows() throws SQLException {
    // Should fail saying because in auto-commit mode (or maybe because not
    // supported).
    connection.rollback();
  }


  ////////////////////////////////////////
  // Savepoint methods:

  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testSetSavepointUnamed() throws SQLException {
    connection.setSavepoint();
  }

  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testSetSavepointNamed() throws SQLException {
    connection.setSavepoint( "savepoint name" );
  }

  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testRollbackSavepoint() throws SQLException {
    connection.rollback( (Savepoint) null );
  }

  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testReleaseSavepoint() throws SQLException {
    connection.releaseSavepoint( (Savepoint) null );
  }

}
