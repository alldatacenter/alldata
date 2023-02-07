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
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.NonTransientRpcException;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcMetrics;
import org.apache.drill.exec.rpc.control.ControlRpcMetrics;
import org.apache.drill.exec.rpc.data.DataRpcMetrics;
import org.apache.drill.exec.rpc.security.KerberosHelper;
import org.apache.drill.exec.rpc.user.UserRpcMetrics;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.kerby.kerberos.kerb.client.JaasKrbUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.security.auth.Subject;
import java.security.PrivilegedExceptionAction;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

@Category(SecurityTest.class)
public class TestUserBitKerberosEncryption extends ClusterTest {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(TestUserBitKerberosEncryption.class);

  private static KerberosHelper krbHelper;

  @BeforeClass
  public static void setupTest() throws Exception {
    krbHelper = new KerberosHelper(TestUserBitKerberosEncryption.class.getSimpleName(), null);
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
      .configNonStringProperty(ExecConstants.AUTHENTICATION_MECHANISMS, Lists.newArrayList("plain", "kerberos"))
      .configProperty(ExecConstants.USER_ENCRYPTION_SASL_ENABLED, "true");
  }

  @AfterClass
  public static void cleanTest() throws Exception {
    krbHelper.stopKdc();
  }

  @Test
  public void successKeytabWithoutChunking() throws Exception {
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
      client.runSqlSilently("SELECT * FROM cp.`region.json`");
    }
  }

  /**
   * Test connection counter values for both encrypted and unencrypted connections over all Drillbit channels.
   * Encryption is enabled only for UserRpc NOT for ControlRpc and DataRpc. Test validates corresponding connection
   * count for each channel.
   * For example: There is only 1 DrillClient so encrypted connection count of UserRpcMetrics will be 1. Before
   * running any query there should not be any connection (control or data) between Drillbits, hence those counters
   * are 0. After running a simple query since there is only 1 fragment which is root fragment the Control Connection
   * count is 0 (for unencrypted counter) since with DRILL-5721 status update of fragment to Foreman happens locally.
   * There is no Data Connection because there is no data exchange between multiple fragments.
   *
   * @throws Exception
   */
  @Test
  @Ignore("See DRILL-5387. This test works in isolation but not when sharing counters with other tests")
  public void testConnectionCounters() throws Exception {
    try (
      // Use a dedicated cluster fixture so that the tested RPC counters have a clean start.
      ClusterFixture cluster = defaultClusterConfig().build();
      ClientFixture client = cluster.clientBuilder()
      .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
      .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
      .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
      .build()
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

      // Check encrypted counters value, only user-bit encryption is enabled
      assertEquals(1, userMetrics.getEncryptedConnectionCount());
      assertEquals(0, ctrlMetrics.getEncryptedConnectionCount());
      assertEquals(0, dataMetrics.getEncryptedConnectionCount());

      // Check encrypted counters value, only user-bit encryption is enabled
      assertEquals(0, userMetrics.getUnEncryptedConnectionCount());
      assertEquals(0, ctrlMetrics.getUnEncryptedConnectionCount());
      assertEquals(0, dataMetrics.getUnEncryptedConnectionCount());
    }
  }

  @Test
  public void successTicketWithoutChunking() throws Exception {
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
  public void successKeytabWithChunking() throws Exception {
    try (
      ClusterFixture cluster = defaultClusterConfig()
        .configProperty(ExecConstants.USER_ENCRYPTION_SASL_MAX_WRAPPED_SIZE, 100)
        .build();
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .build()
    ) {
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
  public void successKeytabWithChunkingDefaultChunkSize() throws Exception {
    try (
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .build()
    ) {
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

  /**
   *  This test will not cover the data channel since we are using only 1 Drillbit and the query doesn't involve
   *  any exchange operator. But Data Channel encryption testing is covered separately in
   *  {@link org.apache.drill.exec.rpc.data.TestBitBitKerberos}
   */
  @Test
  public void successEncryptionAllChannelChunkMode() throws Exception {
    try (
      ClusterFixture cluster = defaultClusterConfig()
        .configProperty(ExecConstants.USER_ENCRYPTION_SASL_MAX_WRAPPED_SIZE, 100)
        .configProperty(ExecConstants.USER_ENCRYPTION_SASL_MAX_WRAPPED_SIZE, 10000)
        .configProperty(ExecConstants.BIT_AUTHENTICATION_ENABLED, true)
        .configProperty(ExecConstants.BIT_AUTHENTICATION_MECHANISM, "kerberos")
        .configProperty(ExecConstants.USE_LOGIN_PRINCIPAL, true)
        .configProperty(ExecConstants.BIT_ENCRYPTION_SASL_ENABLED, true)
        .configProperty(ExecConstants.BIT_ENCRYPTION_SASL_MAX_WRAPPED_SIZE, 10000)
        .build();
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .build()
    ) {
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

  /**
   *  This test will not cover the data channel since we are using only 1 Drillbit and the query doesn't involve
   *  any exchange operator. But Data Channel encryption testing is covered separately in
   *  {@link org.apache.drill.exec.rpc.data.TestBitBitKerberos}
   */
  @Test
  public void successEncryptionAllChannel() throws Exception {
    try (
      ClusterFixture cluster = defaultClusterConfig()
        .configProperty(ExecConstants.USER_ENCRYPTION_SASL_MAX_WRAPPED_SIZE, 100)
        .configProperty(ExecConstants.USER_ENCRYPTION_SASL_MAX_WRAPPED_SIZE, 10000)
        .configProperty(ExecConstants.BIT_AUTHENTICATION_ENABLED, true)
        .configProperty(ExecConstants.BIT_AUTHENTICATION_MECHANISM, "kerberos")
        .configProperty(ExecConstants.USE_LOGIN_PRINCIPAL, true)
        .configProperty(ExecConstants.BIT_ENCRYPTION_SASL_ENABLED, true)
        .build();
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .build()
    ) {
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

  /**
   * Test connection counter values for both encrypted and unencrypted connections over all Drillbit channels.
   * Encryption is enabled for UserRpc, ControlRpc and DataRpc. Test validates corresponding connection
   * count for each channel.
   * For example: There is only 1 DrillClient so encrypted connection count of UserRpcMetrics
   * will be 1. Before running any query there should not be any connection (control or data) between Drillbits,
   * hence those counters are 0. After running a simple query since there is only 1 fragment which is root fragment
   * the Control Connection count is 0 (for encrypted counter), since with DRILL-5721 status update of fragment to
   * Foreman happens locally. There is no Data Connection because there is no data exchange between multiple fragments.
   */

  @Test
  @Ignore("See DRILL-5387. This test works in isolation but not when sharing counters with other tests")
  public void testEncryptedConnectionCountersAllChannel() throws Exception {
    try (
      // Use a dedicated cluster fixture so that the tested RPC counters have a clean start.
      ClusterFixture cluster = defaultClusterConfig()
        .configProperty(ExecConstants.USER_ENCRYPTION_SASL_MAX_WRAPPED_SIZE, 100)
        .configProperty(ExecConstants.USER_ENCRYPTION_SASL_MAX_WRAPPED_SIZE, 10000)
        .configProperty(ExecConstants.BIT_AUTHENTICATION_ENABLED, true)
        .configProperty(ExecConstants.BIT_AUTHENTICATION_MECHANISM, "kerberos")
        .configProperty(ExecConstants.USE_LOGIN_PRINCIPAL, true)
        .configProperty(ExecConstants.BIT_ENCRYPTION_SASL_ENABLED, true)
        .build();
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .build()
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
      assertEquals(1, userMetrics.getEncryptedConnectionCount());
      assertEquals(0, ctrlMetrics.getEncryptedConnectionCount());
      assertEquals(0, dataMetrics.getEncryptedConnectionCount());

      // Check unencrypted counters value
      assertEquals(0, userMetrics.getUnEncryptedConnectionCount());
      assertEquals(0, ctrlMetrics.getUnEncryptedConnectionCount());
      assertEquals(0, dataMetrics.getUnEncryptedConnectionCount());
    }
  }

  @Test
  public void failurePlainMech() {
    try (
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, "anonymous")
        .property(DrillProperties.PASSWORD, "anything works!")
        .build()
    ) {
      fail();
    } catch (Exception ex) {
      assert (ex.getCause() instanceof NonTransientRpcException);
      logger.error("Caught exception: ", ex);
    }
  }

  @Test
  public void encryptionEnabledWithOnlyPlainMech() {
    try (
      ClusterFixture cluster = defaultClusterConfig()
        .configNonStringProperty(ExecConstants.AUTHENTICATION_MECHANISMS, Lists.newArrayList("plain"))
        .build();
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .build()
    ) {
      fail();
    } catch (Exception ex) {
      assert (ex.getCause() instanceof NonTransientRpcException);
      logger.error("Caught exception: ", ex);
    }
  }

  /**
   * Test to validate that older clients are not allowed to connect to secure cluster
   * with encryption enabled.
   */
  @Test
  public void failureOldClientEncryptionEnabled() {
    try (
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .property(DrillProperties.TEST_SASL_LEVEL, "1")
        .build()
    ) {
      fail();
    } catch (Exception ex) {
      assert (ex.getCause() instanceof RpcException);
      logger.error("Caught exception: ", ex);
    }
  }

  /**
   * Test to validate that older clients are successfully connecting to secure cluster
   * with encryption disabled.
   */
  @Test
  public void successOldClientEncryptionDisabled() {
    try (
      ClusterFixture cluster = defaultClusterConfig()
        .configProperty(ExecConstants.USER_ENCRYPTION_SASL_ENABLED, false)
        .build();
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .property(DrillProperties.TEST_SASL_LEVEL, "1")
        .build()
    ) {
      client.testBuilder()
        .sqlQuery("SELECT session_user FROM (SELECT * FROM sys.drillbits LIMIT 1)")
        .unOrdered()
        .baselineColumns("session_user")
        .baselineValues(krbHelper.CLIENT_SHORT_NAME)
        .go();
    } catch (Exception ex) {
      fail();
      assert (ex.getCause() instanceof NonTransientRpcException);
    }
  }

  /**
   * Test to validate that clients which needs encrypted connection fails to connect
   * to server with encryption disabled.
   */
  @Test
  public void clientNeedsEncryptionWithNoServerSupport() {
    try (
      ClusterFixture cluster = defaultClusterConfig()
        .configProperty(ExecConstants.USER_ENCRYPTION_SASL_ENABLED, false)
        .build();
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .property(DrillProperties.SASL_ENCRYPT, "true")
        .build()
    ) {
      fail();
    } catch (Exception ex) {
      assert (ex.getCause() instanceof NonTransientRpcException);
    }
  }

  /**
   * Test to validate that clients which needs encrypted connection connects
   * to server with encryption enabled.
   */
  @Test
  public void clientNeedsEncryptionWithServerSupport() {
    try (
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.SERVICE_PRINCIPAL, krbHelper.SERVER_PRINCIPAL)
        .property(DrillProperties.USER, krbHelper.CLIENT_PRINCIPAL)
        .property(DrillProperties.KEYTAB, krbHelper.clientKeytab.getAbsolutePath())
        .property(DrillProperties.SASL_ENCRYPT, "true")
        .build()
    ) {
      client.testBuilder()
        .sqlQuery("SELECT session_user FROM (SELECT * FROM sys.drillbits LIMIT 1)")
        .unOrdered()
        .baselineColumns("session_user")
        .baselineValues(krbHelper.CLIENT_SHORT_NAME)
        .go();
    } catch (Exception ex) {
      fail();
      assert (ex.getCause() instanceof NonTransientRpcException);
    }
  }
}

