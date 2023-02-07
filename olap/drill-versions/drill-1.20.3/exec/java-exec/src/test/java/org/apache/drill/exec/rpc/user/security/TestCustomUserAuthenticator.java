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
package org.apache.drill.exec.rpc.user.security;

import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryTestUtil;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_1;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_1_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2_PASSWORD;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.core.StringContains.containsString;

@Category(SecurityTest.class)
public class TestCustomUserAuthenticator extends ClusterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setupCluster() {
    cluster = ClusterFixture.bareBuilder(dirTestWatcher)
        .clusterSize(3)
        .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true)
        .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
        .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, UserAuthenticatorTestImpl.TYPE)
        .build();
  }

  @Test
  public void positiveUserAuth() throws Exception {
    runTest(TEST_USER_1, TEST_USER_1_PASSWORD);
    runTest(TEST_USER_2, TEST_USER_2_PASSWORD);
  }

  @Test
  public void positiveAuthDisabled() throws Exception {
    // Setup a separate cluster
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher)
        .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true)
        .configProperty(ExecConstants.INITIAL_USER_PORT, QueryTestUtil.getFreePortNumber(31170, 300))
        .configProperty(ExecConstants.INITIAL_BIT_PORT, QueryTestUtil.getFreePortNumber(31180, 300));

    // Setup specific auth settings
    ClusterFixture cluster = builder.configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, false)
        .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, "NonExistingImpl")
        .build();

    runTest(TEST_USER_1, TEST_USER_1_PASSWORD, cluster);
  }

  @Test
  public void negativeUserAuth() throws Exception {
    negativeAuthHelper(TEST_USER_1, "blah.. blah..");
    negativeAuthHelper(TEST_USER_2, "blah.. blah..");
    negativeAuthHelper("invalidUserName", "blah.. blah..");
  }

  @Test
  public void emptyPassword() throws Exception {
    thrown.expectCause(isA(RpcException.class));
    thrown.expectMessage(containsString("Insufficient credentials"));
    runTest(TEST_USER_2, "");
  }

  @Test
  public void positiveUserAuthAfterNegativeUserAuth() throws Exception {
    negativeAuthHelper("blah.. blah..", "blah.. blah..");
    runTest(TEST_USER_2, TEST_USER_2_PASSWORD);
  }

  private void negativeAuthHelper(final String user, final String password) throws Exception {
    thrown.expectCause(isA(RpcException.class));
    thrown.expectMessage(containsString("Authentication failed"));
    thrown.expectMessage(containsString("Incorrect credentials"));
    runTest(user, password);
  }

  private static void runTest(String user, String password) throws Exception {
    runTest(user, password, cluster);
  }

  private static void runTest(String user, String password, ClusterFixture cluster) throws Exception {
    ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, user)
        .property(DrillProperties.PASSWORD, password)
        .build();

    // Run few queries using the new client
    List<String> queries = Arrays.asList(
        "SHOW SCHEMAS",
        "USE INFORMATION_SCHEMA",
        "SHOW TABLES",
        "SELECT * FROM INFORMATION_SCHEMA.`TABLES` WHERE TABLE_NAME LIKE 'COLUMNS'",
        "SELECT * FROM cp.`region.json` LIMIT 5");

    for (String query : queries) {
      client.queryBuilder().sql(query).run();
    }
  }
}
