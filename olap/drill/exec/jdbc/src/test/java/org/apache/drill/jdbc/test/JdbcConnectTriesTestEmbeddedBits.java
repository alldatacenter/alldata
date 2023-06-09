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

import org.apache.drill.exec.client.InvalidConnectionInfoException;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.jdbc.Driver;
import org.apache.drill.jdbc.JdbcTestBase;

import org.apache.drill.categories.JdbcTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.sql.SQLException;
import java.sql.Connection;

import java.util.concurrent.ExecutionException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Category(JdbcTest.class)
public class JdbcConnectTriesTestEmbeddedBits extends JdbcTestBase {

  public static Driver testDrillDriver;

  @BeforeClass
  public static void testSetUp() throws Exception {
    testDrillDriver = new Driver();
  }

  @Test
  public void testDirectConnectionConnectTriesEqualsDrillbitCount() throws SQLException {
    Connection connection = null;
    try {
      connection = testDrillDriver.connect("jdbc:drill:drillbit=127.0.0.1:5000,127.0.0.1:5001;" + "tries=2",
                                           getDefaultProperties());
      fail();
    } catch (SQLException ex) {
      assertNull(connection);
      assertTrue(ex.getCause() instanceof RpcException);
      assertTrue(ex.getCause().getCause() instanceof ExecutionException);
    }
  }

  @Test
  public void testDirectConnectionConnectTriesGreaterThanDrillbitCount() throws SQLException {
    Connection connection = null;
    try {
      connection = testDrillDriver.connect("jdbc:drill:drillbit=127.0.0.1:5000,127.0.0.1:5001;tries=5",
                                           getDefaultProperties());
      fail();
    } catch (SQLException ex) {
      assertNull(connection);
      assertTrue(ex.getCause() instanceof RpcException);
      assertTrue(ex.getCause().getCause() instanceof ExecutionException);
    }
  }

  @Test
  public void testDirectConnectionConnectTriesLessThanDrillbitCount() throws SQLException {
    Connection connection = null;
    try {
      connection = testDrillDriver.connect("jdbc:drill:drillbit=127.0.0.1:5000,127.0.0.1:5001;tries=1",
                                           getDefaultProperties());
      fail();
    } catch (SQLException ex) {
      assertNull(connection);
      assertTrue(ex.getCause() instanceof RpcException);
      assertTrue(ex.getCause().getCause() instanceof ExecutionException);
    }
  }

  @Test
  public void testDirectConnectionInvalidConnectTries() throws SQLException {
    Connection connection = null;
    try {
      connection = testDrillDriver.connect("jdbc:drill:drillbit=127.0.0.1:5000,127.0.0.1:5001;tries=abc",
                                           getDefaultProperties());
      fail();
    } catch (SQLException ex) {
      assertNull(connection);
      assertTrue(ex.getCause() instanceof InvalidConnectionInfoException);
    }
  }

  @Test
  public void testDirectConnectionZeroConnectTries() throws SQLException {
    Connection connection = null;
    try {
      connection = testDrillDriver.connect("jdbc:drill:drillbit=127.0.0.1:5000,127.0.0.1:5001;tries=0",
                                           getDefaultProperties());
      fail();
    } catch (SQLException ex) {
      assertNull(connection);
      assertTrue(ex.getCause() instanceof RpcException);
      assertTrue(ex.getCause().getCause() instanceof ExecutionException);
    }
  }

  @Test
  public void testDirectConnectionNegativeConnectTries() throws SQLException {
    Connection connection = null;
    try {
      connection = testDrillDriver.connect("jdbc:drill:drillbit=127.0.0.1:5000,127.0.0.1:5001;tries=-5",
                                           getDefaultProperties());
      fail();
    } catch (SQLException ex) {
      assertNull(connection);
      assertTrue(ex.getCause() instanceof RpcException);
      assertTrue(ex.getCause().getCause() instanceof ExecutionException);
    }
  }

  @Test
  public void testZKSuccessfulConnectionZeroConnectTries() throws SQLException {
    Connection connection = testDrillDriver.connect("jdbc:drill:zk=local;tries=0", getDefaultProperties());
    assertNotNull(connection);
    connection.close();
  }

  @Test
  public void testZKSuccessfulConnectionNegativeConnectTries() throws SQLException {
    Connection connection = testDrillDriver.connect("jdbc:drill:zk=local;tries=-1", getDefaultProperties());
    assertNotNull(connection);
    connection.close();
  }

  @Test
  public void testZKSuccessfulConnectionGreaterThanConnectTries() throws SQLException {
    Connection connection = testDrillDriver.connect("jdbc:drill:zk=local;tries=7", getDefaultProperties());
    assertNotNull(connection);
    connection.close();
  }

  @Test
  public void testZKConnectionInvalidConnectTries() throws SQLException {
    Connection connection = null;
    try {
      connection = testDrillDriver.connect("jdbc:drill:zk=local;tries=abc", getDefaultProperties());
      fail();
    } catch (SQLException ex) {
      assertNull(connection);
      assertTrue(ex.getCause() instanceof InvalidConnectionInfoException);
    }
  }
}
