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
package org.apache.drill.jdbc.test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.jdbc.Driver;
import org.apache.drill.jdbc.JdbcTestBase;
import org.junit.experimental.categories.Category;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Category(JdbcTest.class)
public class TestExecutionExceptionsToClient extends JdbcTestBase {

  private static Connection connection;

  @BeforeClass
  public static void setUpConnection() throws Exception {
    connection = new Driver().connect( "jdbc:drill:zk=local", null );
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    connection.close();
  }

  @Test(expected = SQLException.class)
  public void testExecuteQueryThrowsRight1() throws Exception {
    final Statement statement = connection.createStatement();
    try {
      statement.executeQuery("SELECT one case of syntax error");
    } catch (SQLException e) {
      assertThat("Null getCause(); missing expected wrapped exception",
        e.getCause(), notNullValue());

      assertThat("Unexpectedly wrapped another SQLException",
        e.getCause(), not(instanceOf(SQLException.class)));

      assertThat("getCause() not UserRemoteException as expected",
        e.getCause(), instanceOf(UserRemoteException.class));

      assertThat("No expected current \"SYSTEM ERROR\"/eventual \"PARSE ERROR\"",
        e.getMessage(), anyOf(startsWith("SYSTEM ERROR"), startsWith("PARSE ERROR")));
      throw e;
    }
  }

  @Test(expected = SQLException.class)
  public void testExecuteThrowsRight1() throws Exception {
    final Statement statement = connection.createStatement();
    try {
      statement.execute("SELECT one case of syntax error");
    } catch (SQLException e) {
      assertThat("Null getCause(); missing expected wrapped exception",
        e.getCause(), notNullValue());

      assertThat("Unexpectedly wrapped another SQLException",
        e.getCause(), not(instanceOf(SQLException.class)));

      assertThat("getCause() not UserRemoteException as expected",
        e.getCause(), instanceOf(UserRemoteException.class));

      assertThat("No expected current \"SYSTEM ERROR\"/eventual \"PARSE ERROR\"",
        e.getMessage(), anyOf(startsWith("SYSTEM ERROR"), startsWith("PARSE ERROR")));
      throw e;
    }
  }

  @Test(expected = SQLException.class)
  public void testExecuteUpdateThrowsRight1() throws Exception {
    final Statement statement = connection.createStatement();
    try {
      statement.executeUpdate("SELECT one case of syntax error");
    } catch (SQLException e) {
      assertThat("Null getCause(); missing expected wrapped exception",
        e.getCause(), notNullValue());

      assertThat("Unexpectedly wrapped another SQLException",
        e.getCause(), not(instanceOf(SQLException.class)));

      assertThat("getCause() not UserRemoteException as expected",
        e.getCause(), instanceOf(UserRemoteException.class));

      assertThat("No expected current \"SYSTEM ERROR\"/eventual \"PARSE ERROR\"",
        e.getMessage(), anyOf(startsWith("SYSTEM ERROR"), startsWith("PARSE ERROR")));
      throw e;
    }
  }

  @Test(expected = SQLException.class)
  public void testExecuteQueryThrowsRight2() throws Exception {
    final Statement statement = connection.createStatement();
    try {
      statement.executeQuery("BAD QUERY 1");
    } catch (SQLException e) {
      assertThat("Null getCause(); missing expected wrapped exception",
        e.getCause(), notNullValue());

      assertThat("Unexpectedly wrapped another SQLException",
        e.getCause(), not(instanceOf(SQLException.class)));

      assertThat("getCause() not UserRemoteException as expected",
        e.getCause(), instanceOf(UserRemoteException.class));

      assertThat("No expected current \"SYSTEM ERROR\"/eventual \"PARSE ERROR\"",
        e.getMessage(), anyOf(startsWith("SYSTEM ERROR"), startsWith("PARSE ERROR")));
      throw e;
    }
  }

  @Test(expected = SQLException.class)
  public void testExecuteThrowsRight2() throws Exception {
    final Statement statement = connection.createStatement();
    try {
      statement.execute("worse query 2");
    } catch (SQLException e) {
      assertThat("Null getCause(); missing expected wrapped exception",
        e.getCause(), notNullValue());

      assertThat("Unexpectedly wrapped another SQLException",
        e.getCause(), not(instanceOf(SQLException.class)));

      assertThat("getCause() not UserRemoteException as expected",
        e.getCause(), instanceOf(UserRemoteException.class));

      assertThat("No expected current \"SYSTEM ERROR\"/eventual \"PARSE ERROR\"",
        e.getMessage(), anyOf(startsWith("SYSTEM ERROR"), startsWith("PARSE ERROR")));
      throw e;
    }
  }

  @Test(expected = SQLException.class)
  public void testExecuteUpdateThrowsRight2() throws Exception {
    final Statement statement = connection.createStatement();
    try {
      statement.executeUpdate("naughty, naughty query 3");
    } catch (SQLException e) {
      assertThat("Null getCause(); missing expected wrapped exception",
        e.getCause(), notNullValue());

      assertThat("Unexpectedly wrapped another SQLException",
        e.getCause(), not(instanceOf(SQLException.class)));

      assertThat("getCause() not UserRemoteException as expected",
        e.getCause(), instanceOf(UserRemoteException.class));

      assertThat("No expected current \"SYSTEM ERROR\"/eventual \"PARSE ERROR\"",
        e.getMessage(), anyOf(startsWith("SYSTEM ERROR"), startsWith("PARSE ERROR")));
      throw e;
    }
  }

  @Test(expected = SQLException.class)
  public void testMaterializingError() throws Exception {
    final Statement statement = connection.createStatement();
    try {
      statement.executeUpdate("select (res1 = 2016/09/22) res2 from (select (case when (false) then null else "
        + "cast('2016/09/22' as date) end) res1 from (values(1)) foo) foobar");
    } catch (SQLException e) {
      assertThat("Null getCause(); missing expected wrapped exception",
        e.getCause(), notNullValue());

      assertThat("Unexpectedly wrapped another SQLException",
        e.getCause(), not(instanceOf(SQLException.class)));

      assertThat("getCause() not UserRemoteException as expected",
        e.getCause(), instanceOf(UserRemoteException.class));

      assertThat("No expected current \"PLAN ERROR\"",
        e.getMessage(), startsWith("PLAN ERROR"));
      throw e;
    }
  }

  @Test(expected = SQLException.class)
  public void testConvertFromError() throws Exception {
    final Statement statement = connection.createStatement();
    try {
      statement.executeUpdate("select CONVERT_FROM('1','INTEGER') from (values(1))");
    } catch (SQLException e) {
      assertThat("Null getCause(); missing expected wrapped exception",
        e.getCause(), notNullValue());

      assertThat("Unexpectedly wrapped another SQLException",
        e.getCause(), not(instanceOf(SQLException.class)));

      assertThat("getCause() not UserRemoteException as expected",
        e.getCause(), instanceOf(UserRemoteException.class));

      assertTrue("No expected current \"UNSUPPORTED_OPERATION ERROR\" and/or \"Did you mean\"",
        e.getMessage().matches("^UNSUPPORTED_OPERATION ERROR(.|\\n)*Did you mean(.|\\n)*"));
      throw e;
    }
  }
}
