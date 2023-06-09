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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category({SlowTest.class, JdbcTest.class})
public class DrillResultSetTest extends JdbcTestBase {

  // TODO: Move Jetty status server disabling to DrillTest.
  private static final String STATUS_SERVER_PROPERTY_NAME =
      ExecConstants.HTTP_ENABLE;

  private static final String origStatusServerPropValue =
      System.getProperty(STATUS_SERVER_PROPERTY_NAME, "true");

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  // Disable Jetty status server so unit tests run (outside Maven setup).
  // (TODO:  Move this to base test class and/or have Jetty try other ports.)
  @BeforeClass
  public static void setUpClass() {
    System.setProperty(STATUS_SERVER_PROPERTY_NAME, "false");
  }

  @AfterClass
  public static void tearDownClass() {
    System.setProperty(STATUS_SERVER_PROPERTY_NAME, origStatusServerPropValue);
  }


  @Test
  public void test_next_blocksFurtherAccessAfterEnd()
      throws SQLException {
    Connection connection = connect();
    Statement statement = connection.createStatement();
    ResultSet resultSet =
        statement.executeQuery("SELECT 1 AS x \n" +
            "FROM cp.`donuts.json` \n" +
            "LIMIT 2");

    // Advance to first row; confirm can access data.
    assertThat(resultSet.next(), is(true));
    assertThat(resultSet.getInt(1), is(1));

    // Advance from first to second (last) row, confirming data access.
    assertThat(resultSet.next(), is(true));
    assertThat(resultSet.getInt(1), is(1));

    // Now advance past last row.
    assertThat(resultSet.next(), is(false));

    // Main check:  That row data access methods now throw SQLException.
    thrown.expect(InvalidCursorStateSqlException.class);
    thrown.expectMessage(containsString("Result set cursor is already positioned past all rows."));
    resultSet.getInt(1);

    assertThat(resultSet.next(), is(false));

    // TODO:  Ideally, test all other accessor methods.
  }

  @Test
  public void test_next_blocksFurtherAccessWhenNoRows()
      throws Exception {
    Connection connection = connect();
    Statement statement = connection.createStatement();
    ResultSet resultSet =
        statement.executeQuery("SELECT 'Hi' AS x \n" +
            "FROM cp.`donuts.json` \n" +
            "WHERE false");

    // Do initial next(). (Advance from before results to next possible
    // position (after the set of zero rows).
    assertThat(resultSet.next(), is(false));

    // Main check:  That row data access methods throw SQLException.
    thrown.expect(InvalidCursorStateSqlException.class);
    thrown.expectMessage(containsString("Result set cursor is already positioned past all rows."));
    resultSet.getString(1);

    assertThat(resultSet.next(), is(false));

    // TODO:  Ideally, test all other accessor methods.
  }

  @Test
  public void test_getRow_isOneBased()
      throws Exception {
    Connection connection = connect();
    Statement statement = connection.createStatement();
    ResultSet resultSet =
        statement.executeQuery("VALUES (1), (2)");

    // Expect 0 when before first row:
    assertThat("getRow() before first next()", resultSet.getRow(), equalTo(0));

    resultSet.next();

    // Expect 1 at first row:
    assertThat("getRow() at first row", resultSet.getRow(), equalTo(1));

    resultSet.next();

    // Expect 2 at second row:
    assertThat("getRow() at second row", resultSet.getRow(), equalTo(2));

    resultSet.next();

    // Expect 0 again when after last row:
    assertThat("getRow() after last row", resultSet.getRow(), equalTo(0));
    resultSet.next();
    assertThat("getRow() after last row", resultSet.getRow(), equalTo(0));
  }

  @Test
  public void testGetObjectNull() throws SQLException {
    Connection connection = connect();
    Statement statement = connection.createStatement();
    ResultSet resultSet =
        statement.executeQuery(
            "select coalesce(a1, b1) " +
            "from cp.`testGetObjectNull.parquet` " +
            "limit 1");
    resultSet.next();
    assertNull(resultSet.getObject(1));
  }

  // TODO:  Ideally, test other methods.

}
