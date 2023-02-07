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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.apache.drill.categories.JdbcTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.impl.ScreenCreator;
import org.apache.drill.exec.testing.Controls;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.sql.SQLTimeoutException;
import java.sql.SQLException;

/**
 * Test for Drill's implementation of Statement's methods (most).
 */
@Category(JdbcTest.class)
public class StatementTest extends JdbcTestBase {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StatementTest.class);
  private static final Random RANDOMIZER = new Random(20150304);

  private static final String SYS_VERSION_SQL = "select * from sys.version";
  private static final String SYS_RANDOM_SQL =
      "SELECT cast(random() as varchar) as myStr FROM (VALUES(1)) " +
      "union SELECT cast(random() as varchar) as myStr FROM (VALUES(1)) " +
      "union SELECT cast(random() as varchar) as myStr FROM (VALUES(1)) ";

  private static Connection connection;

  @BeforeClass
  public static void setUpStatement() throws SQLException {
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    connection = new Driver().connect( "jdbc:drill:zk=local", null );
  }

  @AfterClass
  public static void tearDownStatement() throws SQLException {
    connection.close();
  }

  ////////////////////////////////////////
  // Query timeout methods:

  //////////
  // getQueryTimeout():

  /**
   * Test for reading of default query timeout
   */
  @Test
  public void testDefaultGetQueryTimeout() throws SQLException {
    try(Statement stmt = connection.createStatement()) {
      int timeoutValue = stmt.getQueryTimeout();
      assertEquals(0, timeoutValue);
    }
  }

  //////////
  // setQueryTimeout(...):

  /**
   * Test Invalid parameter by giving negative timeout
   */
  @Test
  public void testInvalidSetQueryTimeout() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      // Setting negative value
      int valueToSet = -10;
      try {
        stmt.setQueryTimeout(valueToSet);
      } catch (final SQLException e) {
        assertThat(e.getMessage(), containsString( "illegal timeout value"));
      }
    }
  }

  /**
   * Test setting a valid timeout
   */
  @Test
  public void testValidSetQueryTimeout() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      // Setting positive value
      int valueToSet = RANDOMIZER.nextInt(59) + 1;
      logger.info("Setting timeout as {} seconds", valueToSet);
      stmt.setQueryTimeout(valueToSet);
      assertEquals(valueToSet, stmt.getQueryTimeout());
    }
  }


  /**
   * Test setting timeout that never gets triggered
   */
  @Test
  public void testSetQueryTimeoutAsZero() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.setQueryTimeout(0);
      stmt.executeQuery(SYS_RANDOM_SQL);
      ResultSet rs = stmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      assertEquals( 3, rowCount );
    }
  }

  /**
   * Test setting timeout for a query that actually times out because of lack of timely client response
   */
  @Test
  public void testClientTriggeredQueryTimeout() throws Exception {
    // Setting to a very low value (3sec)
    int timeoutDuration = 3;
    int rowsCounted = 0;
    try (Statement stmt = connection.createStatement()) {
      stmt.setQueryTimeout(timeoutDuration);
      logger.info("Set a timeout of {} seconds", stmt.getQueryTimeout());
      ResultSet rs = stmt.executeQuery(SYS_RANDOM_SQL);
      // Fetch each row and pause (simulate a slow client)
      try {
        while (rs.next()) {
          rs.getString(1);
          rowsCounted++;
          // Pause briefly (a second beyond the timeout) before attempting to fetch rows
          try {
            Thread.sleep( TimeUnit.SECONDS.toMillis(timeoutDuration + 1) );
          } catch (InterruptedException e) {/*DoNothing*/}
          logger.info("Paused for {} seconds", (timeoutDuration+1));
        }
      } catch (SQLTimeoutException sqlEx) {
        logger.info("Counted "+rowsCounted+" rows before hitting timeout");
        return; // Successfully return
      }
    }
    //Throw an exception to indicate that we shouldn't have reached this point
    throw new Exception("Failed to trigger timeout of "+ timeoutDuration + " sec");
  }

  /**
   * Test setting timeout for a query that actually times out because of lack of timely server response
   */
  @Test ( expected = SqlTimeoutException.class )
  public void testServerTriggeredQueryTimeout() throws Exception {
    // Setting to a very low value (2sec)
    int timeoutDuration = 2;
    // Server will be paused marginally longer than the test timeout
    long serverPause = timeoutDuration + 2;
    // Additional time for JDBC timeout and server pauses to complete
    int cleanupPause = 3;

    // Simulate a lack of timely server response by injecting a pause in the Screen operator's sending-data RPC
    final String controls = Controls.newBuilder()
        .addTimedPause(ScreenCreator.class, "sending-data", 0, TimeUnit.SECONDS.toMillis(serverPause))
        .build();

    // Fetching an exclusive connection since injected pause affects all sessions on the connection
    try ( Connection exclusiveConnection = new Driver().connect( "jdbc:drill:zk=local", null )) {
      try(Statement stmt = exclusiveConnection.createStatement()) {
        assertThat(
            stmt.execute(String.format(
                "ALTER session SET `%s` = '%s'",
                ExecConstants.DRILLBIT_CONTROL_INJECTIONS, controls)),
            equalTo(true));
      }

      try(Statement stmt = exclusiveConnection.createStatement()) {
        stmt.setQueryTimeout(timeoutDuration);
        logger.info("Set a timeout of {} seconds", stmt.getQueryTimeout());

        // Executing a query with the paused server. Expecting timeout to occur here
        ResultSet rs = stmt.executeQuery(SYS_VERSION_SQL);
        // Fetch rows
        while (rs.next()) {
          rs.getBytes(1);
        }
      } catch (SQLTimeoutException sqlEx) {
        logger.info("SQLTimeoutException thrown: {}", sqlEx.getMessage());
        throw (SqlTimeoutException) sqlEx;
      } finally {
        // Pause briefly to wait for server to unblock
        try {
          Thread.sleep( TimeUnit.SECONDS.toMillis(cleanupPause) );
        } catch (InterruptedException e) {/*DoNothing*/}
      }
    }
  }


  /**
   * Test setting timeout that never gets triggered
   */
  @Test
  public void testNonTriggeredQueryTimeout() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.setQueryTimeout(60);
      stmt.executeQuery(SYS_VERSION_SQL);
      ResultSet rs = stmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      assertEquals( 1, rowCount );
    }
  }
}
