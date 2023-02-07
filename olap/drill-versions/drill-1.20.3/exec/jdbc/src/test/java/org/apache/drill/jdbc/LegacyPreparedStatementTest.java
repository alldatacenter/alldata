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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.categories.JdbcTest;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test that prepared statements works even if not supported on server, to some extent.
 */
@Category(JdbcTest.class)
public class LegacyPreparedStatementTest extends JdbcTestBase {
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
    properties.setProperty("server.preparedstatement.disabled", "true");

    connection = DriverManager.getConnection( "jdbc:drill:zk=local", properties);
    assertTrue(((DrillConnection) connection).getConfig().isServerPreparedStatementDisabled());

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

  //////////
  // Parameters-not-implemented tests:

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
