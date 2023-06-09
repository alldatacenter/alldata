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
package org.apache.drill.jdbc.proxy;

import org.apache.drill.test.DrillTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.drill.categories.JdbcTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test of TracingProxyDriver other than loading of driver classes.
 */
@Category(JdbcTest.class)
public class TracingProxyDriverTest extends DrillTest {

  private static Driver proxyDriver;
  private static Connection proxyConnection;

  @BeforeClass
  public static void setUpTestCase() throws SQLException,
                                            ClassNotFoundException {
    Class.forName( "org.apache.drill.jdbc.proxy.TracingProxyDriver" );
    proxyDriver =
        DriverManager.getDriver(
            "jdbc:proxy:org.apache.drill.jdbc.Driver:jdbc:drill:zk=local" );
    proxyConnection =
        DriverManager.getConnection( "jdbc:proxy::jdbc:drill:zk=local" );
  }

  @Test
  public void testBasicProxying() throws SQLException {
    try ( final Statement stmt = proxyConnection.createStatement() ) {
      final ResultSet rs =
          stmt.executeQuery( "SELECT * FROM INFORMATION_SCHEMA.CATALOGS" );
      assertTrue( rs.next() );
      assertThat( rs.getString( 1 ), equalTo( "DRILL" ) );
      assertThat( rs.getObject( 1 ), equalTo( (Object) "DRILL" ) );
    }
  }

  private static class StdErrCapturer {
    private final PrintStream savedStdErr;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final PrintStream capturingStream = new PrintStream( buffer );
    private boolean redirected;

    StdErrCapturer() {
      savedStdErr = System.err;
    }

    void redirect() {
      assertFalse( redirected );
      redirected = true;
      System.setErr( capturingStream );
    }

    void unredirect() {
      assertTrue( redirected );
      redirected = false;
      System.setErr( savedStdErr );
    }

    String getOutput() {
      assertFalse( redirected );
      return new String( buffer.toByteArray(), StandardCharsets.UTF_8 );
    }
  }

  @Test
  public void testBasicReturnTrace() throws SQLException {
    final StdErrCapturer nameThis = new StdErrCapturer();

    try {
      nameThis.redirect();
      proxyConnection.isClosed();
    }
    finally {
      nameThis.unredirect();
    }

    // Check captured System.err:

    final String output = nameThis.getOutput();
    final String[] lines = output.split( "\n" );
    assertThat( "Not 2 lines: \"\"\"" + output + "\"\"\"",
                lines.length, equalTo( 2 ) );
    final String callLine = lines[ 0 ];
    final String returnLine = lines[ 1 ];

    // Expect something like current:
    // TRACER: CALL:   ((Connection) <id=3> ...) . isClosed()
    // TRACER: RETURN: ((Connection) <id=3> ...) . isClosed(), RESULT: (boolean) false

    assertThat( callLine,   containsString( " CALL:" ) );
    assertThat( returnLine, containsString( " RETURN:" ) );
    assertThat( callLine,   containsString( "(Connection)" ) );
    assertThat( returnLine, containsString( "(Connection)" ) );
    assertThat( callLine,   containsString( "isClosed()" ) );
    assertThat( returnLine, containsString( "isClosed()" ) );
    assertThat( callLine,   not( containsString( " (boolean) " ) ) );
    assertThat( returnLine,      containsString( " (boolean) " ) );
    assertThat( callLine,   not( containsString( "false" ) ) );
    assertThat( returnLine,      containsString( "false" ) );
  }

  @Test
  public void testBasicThrowTrace() throws SQLException {
    final StdErrCapturer stdErrCapturer = new StdErrCapturer();

    final Statement statement = proxyConnection.createStatement();
    statement.close();

    try {
      stdErrCapturer.redirect();
      statement.execute( "" );
    }
    catch ( final SQLException e ) {
      // "already closed" is expected
    }
    finally {
      stdErrCapturer.unredirect();
    }

    // Check captured System.err:

    final String output = stdErrCapturer.getOutput();
    final String[] lines = output.split( "\n" );
    assertThat( "Not 2 lines: \"\"\"" + output + "\"\"\"",
                lines.length, equalTo( 2 ) );
    final String callLine = lines[ 0 ];
    final String returnLine = lines[ 1 ];

    // Expect something like current:
    // TRACER: CALL:   ((Statement) <id=6> ...) . execute( (String) "" )
    // TRACER: THROW:  ((Statement) <id=6> ...) . execute( (String) "" ), th\
    // rew: (org.apache.drill.jdbc.AlreadyClosedSqlException) org.apache.dri\
    // ll.jdbc.AlreadyClosedSqlException: Statement is already closed.

    assertThat( callLine,   containsString( " CALL:" ) );
    assertThat( returnLine, containsString( " THROW:" ) );
    assertThat( callLine,   containsString( "(Statement)" ) );
    assertThat( returnLine, containsString( "(Statement)" ) );
    assertThat( callLine,   containsString( "execute(" ) );
    assertThat( returnLine, containsString( "execute(" ) );
    assertThat( callLine,   not( containsString( "threw:" ) ) );
    assertThat( returnLine,      containsString( "threw:" ) );
    assertThat( callLine,   not( anyOf( containsString( "exception" ),
                                        containsString( "Exception" ) ) ) );
    assertThat( returnLine,      anyOf( containsString( "exception" ),
                                        containsString( "Exception" ) )  );
    assertThat( callLine,   not( anyOf( containsString( "closed" ),
                                        containsString( "Closed" ) ) ) );
    assertThat( returnLine,      anyOf( containsString( "closed" ),
                                        containsString( "Closed" ) )  );
  }

  // TODO:  Clean up these assorted remnants; probably move into separate test
  // methods.
  @Test
  public void testUnsortedMethods() throws SQLException {

    // Exercise these, even though we don't check results.
    proxyDriver.getMajorVersion();
    proxyDriver.getMinorVersion();
    proxyDriver.jdbcCompliant();
    proxyDriver.getParentLogger();
    proxyDriver.getPropertyInfo( "jdbc:proxy::jdbc:drill:zk=local", new Properties() );

    final DatabaseMetaData dbMetaData = proxyConnection.getMetaData();
    assertThat( dbMetaData, instanceOf( DatabaseMetaData.class ) );
    assertThat( dbMetaData, notNullValue() );

    assertThat( dbMetaData.getConnection(), sameInstance( proxyConnection ) );


    dbMetaData.allTablesAreSelectable();
    try {
      dbMetaData.ownUpdatesAreVisible( ResultSet.TYPE_FORWARD_ONLY );
      fail();
    }
    catch ( SQLException | RuntimeException e ) {
      // expected
    }

    final ResultSet catalogsResultSet = dbMetaData.getCatalogs();
    assertThat( catalogsResultSet, notNullValue() );
    assertThat( catalogsResultSet, instanceOf( ResultSet.class ) );

    catalogsResultSet.next();
    catalogsResultSet.getString( 1 );
    catalogsResultSet.getObject( 1 );

    final ResultSetMetaData rsMetaData = catalogsResultSet.getMetaData();
    assertThat( rsMetaData, notNullValue() );
    assertThat( rsMetaData, instanceOf( ResultSetMetaData.class ) );

    int colCount = rsMetaData.getColumnCount();
    for ( int cx = 1; cx <= colCount; cx++ ) {
      catalogsResultSet.getObject( cx );
      catalogsResultSet.getString( cx );
      try {
        catalogsResultSet.getInt( cx );
        fail( "Expected some kind of string-to-int exception.");
      }
      catch ( SQLException e ) {
        // expected;
      }
    }

    assertThat( proxyConnection.getMetaData(), sameInstance( dbMetaData ) );
    assertThat( catalogsResultSet.getMetaData(), sameInstance( rsMetaData ) );
  }
} // class ProxyDriverTest
