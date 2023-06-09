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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.drill.categories.JdbcTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


// NOTE:  Currently, must not inherit from anything that loads the Drill driver
// class (and must not be run in JVM where the Drill driver class has already
// been loaded).

/**
 * Test of TracingProxyDriver's loading of driver class.
 */
@Category(JdbcTest.class)
public class TracingProxyDriverClassLoadingTest extends DrillTest {

  @Ignore( "except when run in own JVM (so Drill Driver not already loaded)" )
  @Test
  public void testClassLoading() throws SQLException, ClassNotFoundException {

    // Note:  Throwing exceptions for test setup problems so they're JUnit
    // errors (red in Eclipse's JUnit view), not just JUnit test failures
    // (blue in Eclipse).

    // 1.  Confirm that Drill driver is not loaded/registered.
    try {
      DriverManager.getDriver( "jdbc:drill:zk=local" );
      throw new IllegalStateException(
          "Drill driver seems loaded already; can't test loading." );
    }
    catch ( SQLException e ) {
      // (Message as of JDK 1.7.)
      assertThat( "Not expected messsage.  (Did JDK change?)",
                  e.getMessage(), equalTo( "No suitable driver" ) );
    }
    try {
      DriverManager.getConnection( "jdbc:drill:zk=local", null );
      throw new IllegalStateException(
          "Drill driver seems loaded already; can't test loading." );
    }
    catch ( SQLException e ) {
      // (Message form as of JDK 1.7.)
      assertThat( "Not expected messsage.  (Did JDK change?)",
                  e.getMessage(),
                  equalTo( "No suitable driver found for jdbc:drill:zk=local" ) );
    }

    // 2.  Confirm that TracingProxyDriver is not loaded/registered.
    try {
      DriverManager.getDriver( "jdbc:proxy::jdbc:drill:zk=local" );
      throw new IllegalStateException(
         "Proxy driver seems loaded already; can't test loading." );
    }
    catch ( SQLException e ) {
      assertThat( "Not expected messsage.  (Did JDK change?)",
                  e.getMessage(), equalTo( "No suitable driver" ) );
    }
    try {
      DriverManager.getConnection( "jdbc:proxy::jdbc:drill:zk=local", null );
      throw new IllegalStateException(
         "Proxy driver seems loaded already; can't test loading." );
    }
    catch ( SQLException e ) {
      assertThat(
          "Not expected messsage.  (Did JDK change?)",
          e.getMessage(),
          equalTo( "No suitable driver found for jdbc:proxy::jdbc:drill:zk=local" ) );
    }

    // 3.  Load TracingProxyDriver.
    Class.forName( "org.apache.drill.jdbc.proxy.TracingProxyDriver" );

    // 4.  Confirm that Drill driver still is not registered.
    try {
      DriverManager.getConnection( "jdbc:proxy::jdbc:drill:zk=local", null );
      throw new IllegalStateException(
          "Drill driver seems loaded already; can't test loading." );
    }
    catch ( ProxySetupSQLException e ) {
      assertThat(
          "Not expected messsage.  (Was it just modified?)",
          e.getMessage(),
          equalTo(
              "Error getting driver from DriverManager for proxied URL"
              + " \"jdbc:drill:zk=local\" (from proxy driver URL"
              + " \"jdbc:proxy::jdbc:drill:zk=local\" (after third colon))"
              + ": java.sql.SQLException: No suitable driver" ) );
    }

    // 5.  Test that TracingProxyDriver can load and use a specified Driver class.
    final Driver driver =
        DriverManager.getDriver(
            "jdbc:proxy:org.apache.drill.jdbc.Driver:jdbc:drill:zk=local" );

    assertThat( driver.acceptsURL( "jdbc:proxy::jdbc:drill:zk=local" ),
                equalTo( true ) );
    assertThat( driver.acceptsURL( "jdbc:drill:zk=local" ), equalTo( false ) );

    // 7.  Test minimally that driver can get connection that works.
    final Connection proxyConnection  =
        DriverManager.getConnection( "jdbc:proxy::jdbc:drill:zk=local", null );
    assertThat( proxyConnection, notNullValue() );

    final DatabaseMetaData dbMetaData = proxyConnection.getMetaData();
    assertThat( dbMetaData, instanceOf( DatabaseMetaData.class ) );
  }

} // class TracingProxyDriverClassLoadingTest
