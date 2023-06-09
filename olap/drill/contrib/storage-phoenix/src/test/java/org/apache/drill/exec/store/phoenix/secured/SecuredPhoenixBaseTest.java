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
package org.apache.drill.exec.store.phoenix.secured;

import ch.qos.logback.classic.Level;
import com.sun.security.auth.module.Krb5LoginModule;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.security.ServerAuthenticationHandler;
import org.apache.drill.exec.rpc.security.kerberos.KerberosFactory;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.phoenix.PhoenixDataSource;
import org.apache.drill.exec.store.phoenix.PhoenixStoragePluginConfig;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.LogFixture;
import org.apache.hadoop.hbase.security.HBaseKerberosUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.phoenix.queryserver.server.QueryServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.drill.exec.store.phoenix.PhoenixBaseTest.createSampleData;
import static org.apache.drill.exec.store.phoenix.PhoenixBaseTest.createSchema;
import static org.apache.drill.exec.store.phoenix.PhoenixBaseTest.createTables;
import static org.apache.drill.exec.store.phoenix.secured.HttpParamImpersonationQueryServerIT.environment;
import static org.apache.drill.exec.store.phoenix.secured.HttpParamImpersonationQueryServerIT.getUrlTemplate;
import static org.apache.drill.exec.store.phoenix.secured.HttpParamImpersonationQueryServerIT.grantUsersToGlobalPhoenixUserTables;
import static org.apache.drill.exec.store.phoenix.secured.HttpParamImpersonationQueryServerIT.grantUsersToPhoenixSystemTables;
import static org.apache.drill.exec.store.phoenix.secured.SecuredPhoenixTestSuite.initPhoenixQueryServer;

public abstract class SecuredPhoenixBaseTest extends ClusterTest {
  private static final Logger logger = LoggerFactory.getLogger(PhoenixDataSource.class);

  protected static LogFixture logFixture;
  private final static Level CURRENT_LOG_LEVEL = Level.INFO;

  private final static AtomicInteger initCount = new AtomicInteger(0);

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    initPhoenixQueryServer();
    startSecuredDrillCluster();
    initializeDatabase();
  }

  private static void startSecuredDrillCluster() throws Exception {
    logFixture = LogFixture.builder()
      .toConsole()
      .logger(QueryServerEnvironment.class, CURRENT_LOG_LEVEL)
      .logger(SecuredPhoenixBaseTest.class, CURRENT_LOG_LEVEL)
      .logger(KerberosFactory.class, CURRENT_LOG_LEVEL)
      .logger(Krb5LoginModule.class, CURRENT_LOG_LEVEL)
      .logger(QueryServer.class, CURRENT_LOG_LEVEL)
      .logger(ServerAuthenticationHandler.class, CURRENT_LOG_LEVEL)
      .build();

    Map.Entry<String, File> user1 = environment.getUser(1);
    Map.Entry<String, File> user2 = environment.getUser(2);
    Map.Entry<String, File> user3 = environment.getUser(3);

    dirTestWatcher.start(SecuredPhoenixTestSuite.class); // until DirTestWatcher ClassRule is implemented for JUnit5
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
        .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, UserAuthenticatorTestImpl.TYPE)
        .configNonStringProperty(ExecConstants.AUTHENTICATION_MECHANISMS, Lists.newArrayList("kerberos"))
        .configProperty(ExecConstants.IMPERSONATION_ENABLED, true)
        .configProperty(ExecConstants.BIT_AUTHENTICATION_ENABLED, true)
        .configProperty(ExecConstants.BIT_AUTHENTICATION_MECHANISM, "kerberos")
        .configProperty(ExecConstants.SERVICE_PRINCIPAL, HBaseKerberosUtils.getPrincipalForTesting())
        .configProperty(ExecConstants.SERVICE_KEYTAB_LOCATION, environment.getServiceKeytab().getAbsolutePath())
        .configClientProperty(DrillProperties.SERVICE_PRINCIPAL, HBaseKerberosUtils.getPrincipalForTesting())
        .configClientProperty(DrillProperties.USER, user1.getKey())
        .configClientProperty(DrillProperties.KEYTAB, user1.getValue().getAbsolutePath());
    startCluster(builder);
    Properties user2ClientProperties = new Properties();
    user2ClientProperties.setProperty(DrillProperties.SERVICE_PRINCIPAL, HBaseKerberosUtils.getPrincipalForTesting());
    user2ClientProperties.setProperty(DrillProperties.USER, user2.getKey());
    user2ClientProperties.setProperty(DrillProperties.KEYTAB, user2.getValue().getAbsolutePath());
    cluster.addClientFixture(user2ClientProperties);
    Properties user3ClientProperties = new Properties();
    user3ClientProperties.setProperty(DrillProperties.SERVICE_PRINCIPAL, HBaseKerberosUtils.getPrincipalForTesting());
    user3ClientProperties.setProperty(DrillProperties.USER, user3.getKey());
    user3ClientProperties.setProperty(DrillProperties.KEYTAB, user3.getValue().getAbsolutePath());
    cluster.addClientFixture(user3ClientProperties);

    Map<String, Object> phoenixProps = new HashMap<>();
    phoenixProps.put("phoenix.query.timeoutMs", 90000);
    phoenixProps.put("phoenix.query.keepAliveMs", "30000");
    phoenixProps.put("phoenix.queryserver.withRemoteUserExtractor", true);
    StoragePluginRegistry registry = cluster.drillbit().getContext().getStorage();
    final String doAsUrl = String.format(getUrlTemplate(), "$user");
    logger.debug("Phoenix Query Server URL: {}", environment.getPqsUrl());
    PhoenixStoragePluginConfig config = new PhoenixStoragePluginConfig(null, 0, null, null,
      doAsUrl, null, phoenixProps);
    config.setEnabled(true);
    registry.put(PhoenixStoragePluginConfig.NAME + "123", config);
  }


  /**
   * Initialize HBase via Phoenix
   */
  private static void initializeDatabase() throws Exception {
    dirTestWatcher.copyResourceToRoot(Paths.get(""));
    if (initCount.incrementAndGet() == 1) {
      final Map.Entry<String, File> user1 = environment.getUser(1);
      final Map.Entry<String, File> user2 = environment.getUser(2);
      // Build the JDBC URL by hand with the doAs
      final UserGroupInformation serviceUgi = ImpersonationUtil.getProcessUserUGI();
      serviceUgi.doAs((PrivilegedExceptionAction<Void>) () -> {
        createSchema(environment.getPqsUrl());
        createTables(environment.getPqsUrl());
        createSampleData(environment.getPqsUrl());
        grantUsersToPhoenixSystemTables(Arrays.asList(user1.getKey(), user2.getKey()));
        grantUsersToGlobalPhoenixUserTables(Arrays.asList(user1.getKey()));
        return null;
      });
    }
  }

  protected interface TestWrapper {
    void apply() throws Exception;
  }

  public void runForThreeClients(SecuredPhoenixSQLTest.TestWrapper wrapper) throws Exception {
    runForThreeClients(wrapper, UserRemoteException.class, RuntimeException.class);
  }

  /**
   * @param wrapper actual test case execution
   * @param user2ExpectedException the expected Exception for user2, which can be impersonated, but hasn't permissions to the tables
   * @param user3ExpectedException the expected Exception for user3, isn't impersonated
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void runForThreeClients(SecuredPhoenixSQLTest.TestWrapper wrapper, Class user2ExpectedException, Class user3ExpectedException) throws Exception {
    try {
      client = cluster.client(0);
      wrapper.apply();
      client = cluster.client(1);
      // original is AccessDeniedException: Insufficient permissions for user 'user2'
      Assertions.assertThrows(user2ExpectedException, wrapper::apply);
      client = cluster.client(2);
      // RuntimeException for user3, Failed to execute HTTP Request, got HTTP/401
      Assertions.assertThrows(user3ExpectedException, wrapper::apply);
    } finally {
      client = cluster.client(0);
    }
  }

  @AfterAll
  public static void tearDownCluster() throws Exception {
    if (!SecuredPhoenixTestSuite.isRunningSuite() && environment != null) {
      HttpParamImpersonationQueryServerIT.stopEnvironment();
    }
  }
}
