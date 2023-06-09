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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.apache.drill.categories.JdbcTest;
import org.apache.drill.exec.ExecConstants;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.sql.SQLException;

/**
 * Test for Drill's implementation of Statement's get/setMaxRows methods
 */
@Category(JdbcTest.class)
public class StatementMaxRowsTest extends JdbcTestBase {

  private static final Random RANDOMIZER = new Random(20150304);

  private static final String SYS_OPTIONS_SQL = "SELECT * FROM sys.options";
  private static final String SYS_OPTIONS_SQL_LIMIT_10 = "SELECT * FROM sys.options LIMIT 12";
  private static final String ALTER_SYS_OPTIONS_MAX_ROWS_LIMIT_X = "ALTER SYSTEM SET `" + ExecConstants.QUERY_MAX_ROWS + "`=";
  // Lock used by all tests to avoid corrupting test scenario
  private static final Semaphore maxRowsSysOptionLock = new Semaphore(1);

  private static Connection connection;

  @BeforeClass
  public static void setUpStatement() throws SQLException {
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    connection = new Driver().connect("jdbc:drill:zk=local", null);
  }

  @AfterClass
  public static void tearDownStatement() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  @Before
  public void getExclusiveLock() {
    // Acquire lock
    maxRowsSysOptionLock.acquireUninterruptibly();
  }

  @After
  public void releaseExclusiveLock() {
    // Release lock either way
    maxRowsSysOptionLock.release();
  }

  ////////////////////////////////////////
  // Query maxRows methods:

  /**
   * Test for reading of default max rows
   */
  @Test
  public void testDefaultGetMaxRows() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      int maxRowsValue = stmt.getMaxRows();
      assertEquals(0, maxRowsValue);
    }
  }

  /**
   * Test Invalid parameter by giving negative maxRows value
   */
  @Test
  public void testInvalidSetMaxRows() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      // Setting negative value
      int valueToSet = -10;
      int origMaxRows = stmt.getMaxRows();
      try {
        stmt.setMaxRows(valueToSet);
      } catch (final SQLException e) {
        assertThat(e.getMessage(), containsString("illegal maxRows value: " + valueToSet));
      }
      // Confirm no change
      assertEquals(origMaxRows, stmt.getMaxRows());
    }
  }

  /**
   * Test setting a valid maxRows value
   */
  @Test
  public void testValidSetMaxRows() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      // Setting positive value
      int valueToSet = RANDOMIZER.nextInt(59) + 1;
      stmt.setMaxRows(valueToSet);
      assertEquals(valueToSet, stmt.getMaxRows());
    }
  }

  /**
   * Test setting maxSize as zero (i.e. no Limit)
   */
  @Test
  public void testSetMaxRowsAsZero() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.setMaxRows(0);
      stmt.executeQuery(SYS_OPTIONS_SQL);
      ResultSet rs = stmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      rs.close();
      assertTrue(rowCount > 0);
    }
  }

  /**
   * Test setting maxSize at a value lower than existing query limit
   */
  @Test
  public void testSetMaxRowsLowerThanQueryLimit() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      int valueToSet = RANDOMIZER.nextInt(9) + 1; // range: [1-9]
      stmt.setMaxRows(valueToSet);
      stmt.executeQuery(SYS_OPTIONS_SQL_LIMIT_10);
      ResultSet rs = stmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      rs.close();
      assertEquals(valueToSet, rowCount);
    }
  }

  /**
   * Test setting maxSize at a value higher than existing query limit
   */
  @Test
  public void testSetMaxRowsHigherThanQueryLimit() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      int valueToSet = RANDOMIZER.nextInt(10) + 11; // range: [11-20]
      stmt.setMaxRows(valueToSet);
      stmt.executeQuery(SYS_OPTIONS_SQL_LIMIT_10);
      ResultSet rs = stmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      rs.close();
      assertTrue(valueToSet > rowCount);
    }
  }

  /**
   * Test setting maxSize at a value lower than existing SYSTEM limit
   */
  @Test
  public void testSetMaxRowsLowerThanSystemLimit() throws SQLException {
    int sysValueToSet = RANDOMIZER.nextInt(5) + 6; // range: [6-10]
    setSystemMaxRows(sysValueToSet);
    try (Statement stmt = connection.createStatement()) {
      int valueToSet = RANDOMIZER.nextInt(5) + 1; // range: [1-5]
      stmt.setMaxRows(valueToSet);
      stmt.executeQuery(SYS_OPTIONS_SQL);
      ResultSet rs = stmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      rs.close();
      assertEquals(valueToSet, rowCount);
    }
    setSystemMaxRows(0); // Reset value
  }

  /**
   * Test setting maxSize at a value higher than existing SYSTEM limit
   */
  @Test
  public void testSetMaxRowsHigherThanSystemLimit() throws SQLException {
    int sysValueToSet = RANDOMIZER.nextInt(5) + 6; // range: [6-10]
    setSystemMaxRows(sysValueToSet);
    try (Statement stmt = connection.createStatement()) {
      int valueToSet = RANDOMIZER.nextInt(5) + 11; // range: [11-15]
      stmt.setMaxRows(valueToSet);
      stmt.executeQuery(SYS_OPTIONS_SQL);
      ResultSet rs = stmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      rs.close();
      assertEquals(sysValueToSet, rowCount);
    }
    setSystemMaxRows(0); // Reset value
  }


  // Sets the SystemMaxRows option
  private void setSystemMaxRows(int sysValueToSet) throws SQLException {
    // Setting the value
    try (Statement stmt = connection.createStatement()) {
      stmt.executeQuery(ALTER_SYS_OPTIONS_MAX_ROWS_LIMIT_X + sysValueToSet);
      ResultSet rs = stmt.getResultSet();
      while (rs.next()) { /*Do Nothing*/ }
      rs.close();
    } catch (SQLException e) {
      throw e;
    }
  }
}
