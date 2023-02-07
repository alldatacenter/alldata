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
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.categories.JdbcTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.experimental.categories.Category;

/**
 * Test for Drill's implementation of PreparedStatement's get/setMaxRows methods
 */
@Category(JdbcTest.class)
public class PreparedStatementMaxRowsTest extends JdbcTestBase {

  private static final Random RANDOMIZER = new Random(20150304);

  private static final String SYS_OPTIONS_SQL = "SELECT * FROM sys.options";
  private static final String SYS_OPTIONS_SQL_LIMIT_10 = "SELECT * FROM sys.options LIMIT 12";
  private static final String ALTER_SYS_OPTIONS_MAX_ROWS_LIMIT_X = "ALTER SYSTEM SET `" + ExecConstants.QUERY_MAX_ROWS + "`=";
  // Lock used by all tests to avoid corrupting test scenario
  private static final Semaphore maxRowsSysOptionLock = new Semaphore(1);

  private static Connection connection;


  @BeforeClass
  public static void setUpConnection() throws SQLException {
    Driver.load();
    Properties properties = new Properties();
    // Increased prepared statement creation timeout to avoid timeout failures
    properties.setProperty(ExecConstants.bootDefaultFor(ExecConstants.CREATE_PREPARE_STATEMENT_TIMEOUT_MILLIS), "30000");
    connection = DriverManager.getConnection("jdbc:drill:zk=local", properties);
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
    }
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
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
    try (PreparedStatement pStmt = connection.prepareStatement(SYS_OPTIONS_SQL)) {
      int maxRowsValue = pStmt.getMaxRows();
      assertEquals(0, maxRowsValue);
    }
  }

  /**
   * Test Invalid parameter by giving negative maxRows value
   */
  @Test
  public void testInvalidSetMaxRows() throws SQLException {
    try (PreparedStatement pStmt = connection.prepareStatement(SYS_OPTIONS_SQL)) {
      //Setting negative value
      int valueToSet = -10;
      int origMaxRows = pStmt.getMaxRows();
      try {
        pStmt.setMaxRows(valueToSet);
      } catch (final SQLException e) {
        assertThat(e.getMessage(), containsString("illegal maxRows value: " + valueToSet));
      }
      // Confirm no change
      assertEquals(origMaxRows, pStmt.getMaxRows());
    }
  }

  /**
   * Test setting a valid maxRows value
   */
  @Test
  public void testValidSetMaxRows() throws SQLException {
    try (PreparedStatement pStmt = connection.prepareStatement(SYS_OPTIONS_SQL)) {
      //Setting positive value
      int valueToSet = RANDOMIZER.nextInt(59) + 1;
      pStmt.setMaxRows(valueToSet);
      assertEquals(valueToSet, pStmt.getMaxRows());
    }
  }

  /**
   * Test setting maxSize as zero (i.e. no Limit)
   */
  @Test
  public void testSetMaxRowsAsZero() throws SQLException {
    try (PreparedStatement pStmt = connection.prepareStatement(SYS_OPTIONS_SQL)) {
      pStmt.setMaxRows(0);
      pStmt.execute();
      ResultSet rs = pStmt.getResultSet();
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
    try (PreparedStatement pStmt = connection.prepareStatement(SYS_OPTIONS_SQL_LIMIT_10)) {
      int valueToSet = RANDOMIZER.nextInt(9) + 1; // range: [1-9]
      pStmt.setMaxRows(valueToSet);
      pStmt.executeQuery();
      ResultSet rs = pStmt.getResultSet();
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
    try (PreparedStatement pStmt = connection.prepareStatement(SYS_OPTIONS_SQL_LIMIT_10)) {
      int valueToSet = RANDOMIZER.nextInt(10) + 11; // range: [11-20]
      pStmt.setMaxRows(valueToSet);
      pStmt.executeQuery();
      ResultSet rs = pStmt.getResultSet();
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
    try (PreparedStatement pStmt = connection.prepareStatement(SYS_OPTIONS_SQL)) {
      int valueToSet = RANDOMIZER.nextInt(5) + 1; // range: [1-5]
      pStmt.setMaxRows(valueToSet);
      pStmt.executeQuery();
      ResultSet rs = pStmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      rs.close();
      assertEquals(valueToSet, rowCount);
    }
    setSystemMaxRows(0); //RESET
  }

  /**
   * Test setting maxSize at a value higher than existing SYSTEM limit
   */
  @Test
  public void testSetMaxRowsHigherThanSystemLimit() throws SQLException {
    int sysValueToSet = RANDOMIZER.nextInt(5) + 6; // range: [6-10]
    setSystemMaxRows(sysValueToSet);
    try (PreparedStatement pStmt = connection.prepareStatement(SYS_OPTIONS_SQL)) {
      int valueToSet = RANDOMIZER.nextInt(5) + 11; // range: [11-15]
      pStmt.setMaxRows(valueToSet);
      pStmt.executeQuery();
      ResultSet rs = pStmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      rs.close();
      assertEquals(sysValueToSet, rowCount);
    }
    setSystemMaxRows(0); //RESET
  }


  //////////
  // Parameters-not-implemented tests:

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
