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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.RpcMetrics;
import org.apache.drill.exec.rpc.control.ControlRpcMetrics;
import org.apache.drill.exec.rpc.data.DataRpcMetrics;
import org.apache.drill.exec.rpc.security.KerberosHelper;
import org.apache.drill.exec.rpc.user.UserRpcMetrics;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.kerby.kerberos.kerb.client.JaasKrbUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.security.auth.Subject;
import java.security.PrivilegedExceptionAction;

import static junit.framework.TestCase.assertEquals;

@Category(SecurityTest.class)
public class TestUserBitKerberos extends ClusterTest {

  private static KerberosHelper krbHelper;

  @BeforeClass
  public static void setupTest() throws Exception {
    krbHelper = new KerberosHelper(TestUserBitKerberos.class.getSimpleName(), null);
    krbHelper.setupKdc(BaseDirTestWatcher.createTempDir(dirTestWatcher.getTmpDir()));
    cluster = defaultClusterConfig().build();
  }

  private static ClusterFixtureBuilder defaultClusterConfig() {
    return ClusterFixture.bareBuilder(dirTestWatcher)
      .clusterSize(1)
      .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
      .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, UserAuthenticatorTestImpl.TYPE)
      .configProperty(ExecConstants.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
      .configProperty(ExecConstants.SERVICE_KEYTAB_LOCATION, krbHelper.serverKeytab.toString())
      .configNonStringProperty(ExecConstants.AUTHENTICATION_MECHANISMS, Lists.newArrayList("plain", "kerberos"));
  }

  @Test
  public void successKeytab() throws Exception {
    try (
      ClientFixture client = cluster.clientBuilder()
      .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
      .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
      .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
      .build()
    ) {

      // Run few queries using the new client
      client.testBuilder()
        .sqlQuery("SELECT session_user FROM (SELECT * FROM sys.drillbits LIMIT 1)")
        .unOrdered()
        .baselineColumns("session_user")
        .baselineValues(krbHelper.CLIENT_SHORT_NAME)
        .go();

      client.runSqlSilently("SHOW SCHEMAS");
      client.runSqlSilently("USE INFORMATION_SCHEMA");
      client.runSqlSilently("SHOW TABLES");
      client.runSqlSilently("SELECT * FROM INFORMATION_SCHEMA.`TABLES` WHERE TABLE_NAME LIKE 'COLUMNS'");
      client.runSqlSilently("SELECT * FROM cp.`region.json` LIMIT 5");
    }
  }

  @Test
  public void successTicket() throws Exception {
    Subject clientSubject = JaasKrbUtil.loginUsingKeytab(
      krbHelper.CLIENT_PRINCIPAL,
      krbHelper.clientKeytab.getAbsoluteFile()
    );

    try (
      ClientFixture client = Subject.doAs(
        clientSubject,
        (PrivilegedExceptionAction<ClientFixture>) () -> cluster.clientBuilder()
          .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
          .property(DrillProperties.KERBEROS_FROM_SUBJECT, "true")
          .build()
      )
    ) {

    // Run few queries using the new client
    client.testBuilder()
      .sqlQuery("SELECT session_user FROM (SELECT * FROM sys.drillbits LIMIT 1)")
      .unOrdered()
      .baselineColumns("session_user")
      .baselineValues(krbHelper.CLIENT_SHORT_NAME)
      .go();

    client.runSqlSilently("SHOW SCHEMAS");
    client.runSqlSilently("USE INFORMATION_SCHEMA");
    client.runSqlSilently("SHOW TABLES");
    client.runSqlSilently("SELECT * FROM INFORMATION_SCHEMA.`TABLES` WHERE TABLE_NAME LIKE 'COLUMNS'");
    client.runSqlSilently("SELECT * FROM cp.`region.json` LIMIT 5");
    }
   }

  @Test
  @Ignore("See DRILL-5387. This test works in isolation but not when sharing counters with other tests")
  public void testUnencryptedConnectionCounter() throws Exception {
    Subject clientSubject = JaasKrbUtil.loginUsingKeytab(
      krbHelper.CLIENT_PRINCIPAL,
      krbHelper.clientKeytab.getAbsoluteFile()
    );

    try (
      // Use a dedicated cluster fixture so that the tested RPC counters have a clean start.
      ClusterFixture cluster = defaultClusterConfig().build();
      ClientFixture client = Subject.doAs(
        clientSubject,
        (PrivilegedExceptionAction<ClientFixture>) () -> cluster.clientBuilder()
          .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
          .property(DrillProperties.KERBEROS_FROM_SUBJECT, "true")
          .build()
      )
    ) {
      client.testBuilder()
          .sqlQuery("SELECT session_user FROM (SELECT * FROM sys.drillbits LIMIT 1)")
          .unOrdered()
          .baselineColumns("session_user")
          .baselineValues(krbHelper.CLIENT_SHORT_NAME)
          .go();

      RpcMetrics userMetrics = UserRpcMetrics.getInstance(),
        ctrlMetrics = ControlRpcMetrics.getInstance(),
        dataMetrics = DataRpcMetrics.getInstance();

      // Check encrypted counters value
      assertEquals(0, userMetrics.getEncryptedConnectionCount());
      assertEquals(0, ctrlMetrics.getEncryptedConnectionCount());
      assertEquals(0, dataMetrics.getEncryptedConnectionCount());

      // Check unencrypted counters value
      assertEquals(1, userMetrics.getUnEncryptedConnectionCount());
      assertEquals(0, ctrlMetrics.getUnEncryptedConnectionCount());
      assertEquals(0, dataMetrics.getUnEncryptedConnectionCount());
    }
  }

  @Test
  @Ignore("See DRILL-5387. This test works in isolation but not when sharing counters with other tests")
  public void testUnencryptedConnectionCounter_LocalControlMessage() throws Exception {
    Subject clientSubject = JaasKrbUtil.loginUsingKeytab(
      krbHelper.CLIENT_PRINCIPAL,
      krbHelper.clientKeytab.getAbsoluteFile()
    );

    try (
      // Use a dedicated cluster fixture so that the tested RPC counters have a clean start.
      ClusterFixture cluster = defaultClusterConfig().build();
      ClientFixture client = Subject.doAs(
        clientSubject,
        (PrivilegedExceptionAction<ClientFixture>) () -> cluster.clientBuilder()
          .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
          .property(DrillProperties.KERBEROS_FROM_SUBJECT, "true")
          .build()
      )
     ) {
      // Run query on memory system table this sends remote fragments to all Drillbit and Drillbits then send data
      // using data channel. In this test we have only 1 Drillbit so there should not be any control connection but a
      // local data connections
      client.runSqlSilently("SELECT * FROM sys.memory");

      RpcMetrics userMetrics = UserRpcMetrics.getInstance(),
        ctrlMetrics = ControlRpcMetrics.getInstance(),
        dataMetrics = DataRpcMetrics.getInstance();

      // Check encrypted counters value
      assertEquals(0, userMetrics.getEncryptedConnectionCount());
      assertEquals(0, ctrlMetrics.getEncryptedConnectionCount());
      assertEquals(0, dataMetrics.getEncryptedConnectionCount());

      // Check unencrypted counters value
      assertEquals(1, userMetrics.getUnEncryptedConnectionCount());
      assertEquals(0, ctrlMetrics.getUnEncryptedConnectionCount());
      assertEquals(2, dataMetrics.getUnEncryptedConnectionCount());
    }
  }

  @AfterClass
  public static void cleanTest() throws Exception {
    krbHelper.stopKdc();
  }
}
