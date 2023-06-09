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

import static java.sql.ResultSetMetaData.columnNoNulls;
import static java.sql.Types.BIGINT;
import static java.sql.Types.DATE;
import static java.sql.Types.DECIMAL;
import static java.sql.Types.INTEGER;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.impl.ScreenCreator;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.store.ischema.InfoSchemaConstants;
import org.apache.drill.exec.testing.Controls;
import org.apache.drill.categories.JdbcTest;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.junit.experimental.categories.Category;

/**
 * Test for Drill's implementation of PreparedStatement's methods.
 */
@Category(JdbcTest.class)
public class PreparedStatementTest extends JdbcTestBase {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PreparedStatementTest.class);
  private static final Random RANDOMIZER = new Random(20150304);

  private static final String SYS_VERSION_SQL = "select * from sys.version";
  private static final String SYS_RANDOM_SQL =
      "SELECT cast(random() as varchar) as myStr FROM (VALUES(1)) " +
      "union SELECT cast(random() as varchar) as myStr FROM (VALUES(1)) " +
      "union SELECT cast(random() as varchar) as myStr FROM (VALUES(1)) ";

  /** Fuzzy matcher for parameters-not-supported message assertions.  (Based on
   *  current "Prepared-statement dynamic parameters are not supported.") */
  private static final Matcher<String> PARAMETERS_NOT_SUPPORTED_MSG_MATCHER =
      allOf( containsString( "arameter" ),   // allows "Parameter"
             containsString( "not" ),        // (could have false matches)
             containsString( "support" ) );  // allows "supported"

  private static Connection connection;


  @BeforeClass
  public static void setUpConnection() throws SQLException {
    Driver.load();
    Properties properties = new Properties();
    // Increased prepared statement creation timeout so test doesn't timeout on my laptop
    properties.setProperty(ExecConstants.bootDefaultFor(ExecConstants.CREATE_PREPARE_STATEMENT_TIMEOUT_MILLIS), "30000");
    connection = DriverManager.getConnection( "jdbc:drill:zk=local", properties);
    try(Statement stmt = connection.createStatement()) {
      stmt.execute(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
    }
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    if (connection != null) {
      try (Statement stmt = connection.createStatement()) {
        stmt.execute(String.format("alter session set `%s` = false", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
      }
    }
    connection.close();
  }

  //////////
  // Basic querying-works test:

  /** Tests that basic executeQuery() (with query statement) works. */
  @Test
  public void testExecuteQueryBasicCaseWorks() throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement( "VALUES 11" )) {
      try(ResultSet rs = stmt.executeQuery()) {
        assertThat("Unexpected column count",
            rs.getMetaData().getColumnCount(), equalTo(1)
        );
        assertTrue("No expected first row", rs.next());
        assertThat(rs.getInt(1), equalTo(11));
        assertFalse("Unexpected second row", rs.next());
      }
    }
  }

  @Test
  public void testQueryMetadataInPreparedStatement() throws SQLException {
    try(PreparedStatement stmt = connection.prepareStatement(
        "SELECT " +
            "cast(1 as INTEGER ) as int_field, " +
            "cast(12384729 as BIGINT ) as bigint_field, " +
            "cast('varchar_value' as varchar(50)) as varchar_field, " +
            "timestamp '2008-2-23 10:00:20.123' as ts_field, " +
            "date '2008-2-23' as date_field, " +
            "cast('99999912399.4567' as decimal(18, 5)) as decimal_field" +
            " FROM sys.version")) {

      List<ExpectedColumnResult> exp = ImmutableList.of(
          new ExpectedColumnResult("int_field", INTEGER, columnNoNulls, 11, 0, 0, true, Integer.class.getName()),
          new ExpectedColumnResult("bigint_field", BIGINT, columnNoNulls, 20, 0, 0, true, Long.class.getName()),
          new ExpectedColumnResult("varchar_field", VARCHAR, columnNoNulls, 50, 50, 0, false, String.class.getName()),
          new ExpectedColumnResult("ts_field", TIMESTAMP, columnNoNulls, 19, 0, 0, false, Timestamp.class.getName()),
          new ExpectedColumnResult("date_field", DATE, columnNoNulls, 10, 0, 0, false, Date.class.getName()),
          new ExpectedColumnResult("decimal_field", DECIMAL, columnNoNulls, 20, 18, 5, true, BigDecimal.class.getName())
      );

      ResultSetMetaData prepareMetadata = stmt.getMetaData();
      verifyMetadata(prepareMetadata, exp);

      try (ResultSet rs = stmt.executeQuery()) {
        ResultSetMetaData executeMetadata = rs.getMetaData();
        verifyMetadata(executeMetadata, exp);

        assertTrue("No expected first row", rs.next());
        assertThat(rs.getInt(1), equalTo(1));
        assertThat(rs.getLong(2), equalTo(12384729L));
        assertThat(rs.getString(3), equalTo("varchar_value"));
        assertThat(rs.getTimestamp(4), equalTo(Timestamp.valueOf("2008-2-23 10:00:20.123")));
        assertThat(rs.getDate(5), equalTo(Date.valueOf("2008-2-23")));
        assertThat(rs.getBigDecimal(6), equalTo(new BigDecimal("99999912399.45670")));
        assertFalse("Unexpected second row", rs.next());
      }
    }
  }

  private static void verifyMetadata(ResultSetMetaData act, List<ExpectedColumnResult> exp) throws SQLException {
    assertEquals(exp.size(), act.getColumnCount());
    int i = 0;
    for(ExpectedColumnResult e : exp) {
      ++i;
      assertTrue("Failed to find the expected column metadata. Expected " + e + ". Was: " + toString(act, i), e.isEqualsTo(act, i));
    }
  }

  private static String toString(ResultSetMetaData metadata, int colNum) throws SQLException {
    return "ResultSetMetaData(" + colNum + ")[" +
        "columnName='" + metadata.getColumnName(colNum) + '\'' +
        ", type='" + metadata.getColumnType(colNum) + '\'' +
        ", nullable=" + metadata.isNullable(colNum) +
        ", displaySize=" + metadata.getColumnDisplaySize(colNum) +
        ", precision=" + metadata.getPrecision(colNum) +
        ", scale=" + metadata.getScale(colNum) +
        ", signed=" + metadata.isSigned(colNum) +
        ", className='" + metadata.getColumnClassName(colNum) + '\'' +
        ']';
  }
  private static class ExpectedColumnResult {
    final String columnName;
    final int type;
    final int nullable;
    final int displaySize;
    final int precision;
    final int scale;
    final boolean signed;
    final String className;

    ExpectedColumnResult(String columnName, int type, int nullable, int displaySize, int precision,
        int scale, boolean signed, String className) {
      this.columnName = columnName;
      this.type = type;
      this.nullable = nullable;
      this.displaySize = displaySize;
      this.precision = precision;
      this.scale = scale;
      this.signed = signed;
      this.className = className;
    }

    boolean isEqualsTo(ResultSetMetaData metadata, int colNum) throws SQLException {
      return
          metadata.getCatalogName(colNum).equals(InfoSchemaConstants.IS_CATALOG_NAME) &&
          metadata.getSchemaName(colNum).isEmpty() &&
          metadata.getTableName(colNum).isEmpty() &&
          metadata.getColumnName(colNum).equals(columnName) &&
          metadata.getColumnLabel(colNum).equals(columnName) &&
          metadata.getColumnType(colNum) == type &&
          metadata.isNullable(colNum) == nullable &&
          // There is an existing bug where query results doesn't contain the precision for VARCHAR field.
          //metadata.getPrecision(colNum) == precision &&
          metadata.getScale(colNum) == scale &&
          metadata.isSigned(colNum) == signed &&
          metadata.getColumnDisplaySize(colNum) == displaySize &&
          metadata.getColumnClassName(colNum).equals(className) &&
          metadata.isSearchable(colNum) &&
          metadata.isAutoIncrement(colNum) == false &&
          metadata.isCaseSensitive(colNum) == false &&
          metadata.isReadOnly(colNum) &&
          metadata.isWritable(colNum) == false &&
          metadata.isDefinitelyWritable(colNum) == false &&
          metadata.isCurrency(colNum) == false;
    }

    @Override
    public String toString() {
      return "ExpectedColumnResult[" +
          "columnName='" + columnName + '\'' +
          ", type='" + type + '\'' +
          ", nullable=" + nullable +
          ", displaySize=" + displaySize +
          ", precision=" + precision +
          ", scale=" + scale +
          ", signed=" + signed +
          ", className='" + className + '\'' +
          ']';
    }
  }

  /**
   * Test for reading of default query timeout
   */
  @Test
  public void testDefaultGetQueryTimeout() throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(SYS_VERSION_SQL)) {
      int timeoutValue = stmt.getQueryTimeout();
      assertEquals(0L, timeoutValue);
    }
  }

  /**
   * Test Invalid parameter by giving negative timeout
   */
  @Test
  public void testInvalidSetQueryTimeout() throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(SYS_VERSION_SQL)) {
      //Setting negative value
      int valueToSet = -10;
      try {
        stmt.setQueryTimeout(valueToSet);
      } catch (final SQLException e) {
        assertThat(e.getMessage(), containsString( "illegal timeout value") );
      }
    }
  }

  /**
   * Test setting a valid timeout
   */
  @Test
  public void testValidSetQueryTimeout() throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(SYS_VERSION_SQL)) {
      //Setting positive value
      int valueToSet = RANDOMIZER.nextInt(59) + 1;
      logger.info("Setting timeout as {} seconds", valueToSet);
      stmt.setQueryTimeout(valueToSet);
      assertEquals(valueToSet, stmt.getQueryTimeout());
    }
  }

  /**
   * Test setting timeout as zero and executing
   */
  @Test
  public void testSetQueryTimeoutAsZero() throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(SYS_RANDOM_SQL)) {
      stmt.setQueryTimeout(0);
      stmt.executeQuery();
      ResultSet rs = stmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      assertEquals(3, rowCount);
    }
  }

  /**
   * Test setting timeout for a query that actually times out
   */
  @Test
  public void testClientTriggeredQueryTimeout() throws Exception {
    //Setting to a very low value (3sec)
    int timeoutDuration = 3;
    int rowsCounted = 0;
    try (PreparedStatement stmt = connection.prepareStatement(SYS_RANDOM_SQL)) {
      stmt.setQueryTimeout(timeoutDuration);
      logger.info("Set a timeout of {} seconds", stmt.getQueryTimeout());
      ResultSet rs = stmt.executeQuery();
      //Fetch each row and pause (simulate a slow client)
      try {
        while (rs.next()) {
          rs.getString(1);
          rowsCounted++;
          //Pause briefly (a second beyond the timeout) before attempting to fetch rows
          try {
            Thread.sleep( TimeUnit.SECONDS.toMillis(timeoutDuration + 1) );
          } catch (InterruptedException e) {/*DoNothing*/}
          logger.info("Paused for {} seconds", (timeoutDuration+1));
        }
      } catch (SQLTimeoutException sqlEx) {
        logger.info("Counted "+rowsCounted+" rows before hitting timeout");
        return; //Successfully return
      }
    }
    //Throw an exception to indicate that we shouldn't have reached this point
    throw new Exception("Failed to trigger timeout of "+ timeoutDuration + " sec");
  }

  /**
   * Test setting timeout for a query that actually times out because of lack of timely server response
   */
  @Ignore ( "Pause Injection appears broken for PreparedStatement" )
  @Test ( expected = SqlTimeoutException.class )
  public void testServerTriggeredQueryTimeout() throws Exception {
    //Setting to a very low value (2sec)
    int timeoutDuration = 2;
    //Server will be paused marginally longer than the test timeout
    long serverPause = timeoutDuration + 2;
    //Additional time for JDBC timeout and server pauses to complete
    int cleanupPause = 3;

    //Simulate a lack of timely server response by injecting a pause in the Screen operator's sending-data RPC
    final String controls = Controls.newBuilder()
        .addTimedPause(ScreenCreator.class, "sending-data", 0, TimeUnit.SECONDS.toMillis(serverPause))
        .build();

    //Fetching an exclusive connection since injected pause affects all sessions on the connection
    try ( Connection exclusiveConnection = new Driver().connect( "jdbc:drill:zk=local", null )) {
      try(Statement stmt = exclusiveConnection.createStatement()) {
        assertThat(
            stmt.execute(String.format(
                "ALTER session SET `%s` = '%s'",
                ExecConstants.DRILLBIT_CONTROL_INJECTIONS, controls)),
            equalTo(true));
      }

      try (PreparedStatement pStmt = exclusiveConnection.prepareStatement(SYS_RANDOM_SQL)) {
        pStmt.setQueryTimeout(timeoutDuration);
        logger.info("Set a timeout of {} seconds", pStmt.getQueryTimeout());

        //Executing a prepared statement with the paused server. Expecting timeout to occur here
        ResultSet rs = pStmt.executeQuery();
        //Fetch rows
        while (rs.next()) {
          rs.getBytes(1);
        }
      } catch (SQLTimeoutException sqlEx) {
        logger.info("SQLTimeoutException thrown: {}", sqlEx.getMessage());
        throw (SqlTimeoutException) sqlEx;
      } finally {
        //Pause briefly to wait for server to unblock
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
    try (PreparedStatement stmt = connection.prepareStatement(SYS_VERSION_SQL)) {
      stmt.setQueryTimeout(60);
      stmt.executeQuery();
      ResultSet rs = stmt.getResultSet();
      int rowCount = 0;
      while (rs.next()) {
        rs.getBytes(1);
        rowCount++;
      }
      assertEquals(1, rowCount);
    }
  }

  //////////
  // Parameters-not-implemented tests:

  /** Tests that basic case of trying to create a prepare statement with parameters. */
  @Test( expected = SQLException.class )
  public void testSqlQueryWithParamNotSupported() throws SQLException {

    try {
      connection.prepareStatement( "VALUES ?, ?" );
    }
    catch ( final SQLException e ) {
      assertThat(
          "Check whether params.-unsupported wording changed or checks changed.",
          e.toString(), containsString("Illegal use of dynamic parameter") );
      throw e;
    }
  }

  /** Tests that "not supported" has priority over possible "no parameters"
   *  check. */
  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testParamSettingWhenNoParametersIndexSaysUnsupported() throws SQLException {
    try(PreparedStatement prepStmt = connection.prepareStatement( "VALUES 1" )) {
      try {
        prepStmt.setBytes(4, null);
      } catch (final SQLFeatureNotSupportedException e) {
        assertThat(
            "Check whether params.-unsupported wording changed or checks changed.",
            e.toString(), PARAMETERS_NOT_SUPPORTED_MSG_MATCHER
        );
        throw e;
      }
    }
  }

  /** Tests that "not supported" has priority over possible "type not supported"
   *  check. */
  @Test( expected = SQLFeatureNotSupportedException.class )
  public void testParamSettingWhenUnsupportedTypeSaysUnsupported() throws SQLException {
    try(PreparedStatement prepStmt = connection.prepareStatement( "VALUES 1" )) {
      try {
        prepStmt.setClob(2, (Clob) null);
      } catch (final SQLFeatureNotSupportedException e) {
        assertThat(
            "Check whether params.-unsupported wording changed or checks changed.",
            e.toString(), PARAMETERS_NOT_SUPPORTED_MSG_MATCHER
        );
        throw e;
      }
    }
  }
}
