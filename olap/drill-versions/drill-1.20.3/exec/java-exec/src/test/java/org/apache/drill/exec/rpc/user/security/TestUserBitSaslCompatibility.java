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
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.NonTransientRpcException;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.test.BaseTestQuery;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.security.sasl.SaslException;
import java.util.Properties;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

/**
 * Helps to test different scenarios based on security configuration on client and Drillbit side with respect to SASL
 * and specifically using PLAIN mechanism
 */
@Category({SecurityTest.class})
public class TestUserBitSaslCompatibility extends BaseTestQuery {
  @BeforeClass
  public static void setup() {
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
            ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE))
        .withValue(ExecConstants.AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("plain")))
        .withValue(ExecConstants.USER_ENCRYPTION_SASL_ENABLED,
            ConfigValueFactory.fromAnyRef(false)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, "anonymous");
    connectionProps.setProperty(DrillProperties.PASSWORD, "anything works!");

    updateTestCluster(1, newConfig, connectionProps);
  }

  /**
   * Test showing when Drillbit is not configured for authentication whereas client explicitly requested for PLAIN
   * authentication then connection succeeds without authentication.
   * @throws Exception
   */
  @Test
  public void testDisableDrillbitAuth_EnableClientAuthPlain() throws Exception {

    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(false)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, "anonymous");
    connectionProps.setProperty(DrillProperties.PASSWORD, "anything works!");

    try {
      updateTestCluster(1, newConfig, connectionProps);
    } catch (Exception ex) {
      fail();
    }
  }

  /**
   * Test showing when Drillbit is not configured for authentication whereas client explicitly requested for Kerberos
   * authentication then connection fails due to new check before SASL Handshake.
   * @throws Exception
   */
  @Test
  public void testDisableDrillbitAuth_EnableClientAuthKerberos() throws Exception {

    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(false)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.AUTH_MECHANISM, "kerberos");

    try {
      updateTestCluster(1, newConfig, connectionProps);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getCause() instanceof NonTransientRpcException);
      assertTrue(!(ex.getCause().getCause() instanceof SaslException));
    }
  }

  /**
   * Test showing failure before SASL handshake when Drillbit is not configured for authentication whereas client
   * explicitly requested for encrypted connection.
   * @throws Exception
   */
  @Test
  public void testDisableDrillbitAuth_EnableClientEncryption() throws Exception {
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(false)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, "anonymous");
    connectionProps.setProperty(DrillProperties.PASSWORD, "anything works!");
    connectionProps.setProperty(DrillProperties.SASL_ENCRYPT, "true");

    try {
      updateTestCluster(1, newConfig, connectionProps);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getCause() instanceof NonTransientRpcException);
      assertTrue(!(ex.getCause().getCause() instanceof SaslException));
    }
  }

  /**
   * Test showing failure before SASL handshake when Drillbit is not configured for encryption whereas client explicitly
   * requested for encrypted connection.
   * @throws Exception
   */
  @Test
  public void testDisableDrillbitEncryption_EnableClientEncryption() throws Exception {
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
            ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE))
        .withValue(ExecConstants.AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("plain")))
        .withValue(ExecConstants.USER_ENCRYPTION_SASL_ENABLED,
            ConfigValueFactory.fromAnyRef(false)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, "anonymous");
    connectionProps.setProperty(DrillProperties.PASSWORD, "anything works!");
    connectionProps.setProperty(DrillProperties.SASL_ENCRYPT, "true");

    try {
      updateTestCluster(1, newConfig, connectionProps);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getCause() instanceof NonTransientRpcException);
      assertTrue(!(ex.getCause().getCause() instanceof SaslException));
    }
  }

  /**
   * Test showing failure in SASL handshake when Drillbit is configured for authentication only whereas client doesn't
   * provide any security properties like username/password in this case.
   * @throws Exception
   */
  @Test
  public void testEnableDrillbitAuth_DisableClientAuth() throws Exception {
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
            ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE))
        .withValue(ExecConstants.AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("plain")))
        .withValue(ExecConstants.USER_ENCRYPTION_SASL_ENABLED,
            ConfigValueFactory.fromAnyRef(false)));

    final Properties connectionProps = new Properties();

    try {
      updateTestCluster(1, newConfig, connectionProps);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getCause() instanceof NonTransientRpcException);
      assertTrue(ex.getCause().getCause() instanceof SaslException);
    }
  }

  /**
   * Test showing failure in SASL handshake when Drillbit is configured for encryption whereas client doesn't provide any
   * security properties like username/password in this case.
   * @throws Exception
   */
  @Test
  public void testEnableDrillbitEncryption_DisableClientAuth() throws Exception {
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
            ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE))
        .withValue(ExecConstants.AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("plain")))
        .withValue(ExecConstants.USER_ENCRYPTION_SASL_ENABLED,
            ConfigValueFactory.fromAnyRef(true)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, "anonymous");
    connectionProps.setProperty(DrillProperties.PASSWORD, "anything works!");

    try {
      updateTestCluster(1, newConfig, connectionProps);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getCause() instanceof NonTransientRpcException);
      assertTrue(ex.getCause().getCause() instanceof SaslException);
    }
  }

  /**
   * Test showing successful SASL handshake when both Drillbit and client side authentication is enabled using PLAIN
   * mechanism.
   * @throws Exception
   */
  @Test
  public void testEnableDrillbitClientAuth() throws Exception {
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
            ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE))
        .withValue(ExecConstants.AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("plain")))
        .withValue(ExecConstants.USER_ENCRYPTION_SASL_ENABLED,
            ConfigValueFactory.fromAnyRef(false)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, "anonymous");
    connectionProps.setProperty(DrillProperties.PASSWORD, "anything works!");

    try {
      updateTestCluster(1, newConfig, connectionProps);
    } catch (Exception ex) {
      fail();
    }
  }

  /**
   * Below test shows the failure in Sasl layer with client and Drillbit side encryption enabled using PLAIN
   * mechanism. This is expected since PLAIN mechanism doesn't support encryption using SASL. Whereas same test
   * setup using Kerberos or any other mechanism with encryption support will result in successful SASL handshake.
   * @throws Exception
   */
  @Test
  public void testEnableDrillbitClientEncryption_UsingPlain() throws Exception {
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
            ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE))
        .withValue(ExecConstants.AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("plain")))
        .withValue(ExecConstants.USER_ENCRYPTION_SASL_ENABLED,
            ConfigValueFactory.fromAnyRef(true)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, "anonymous");
    connectionProps.setProperty(DrillProperties.PASSWORD, "anything works!");
    connectionProps.setProperty(DrillProperties.SASL_ENCRYPT, "true");

    try {
      updateTestCluster(1, newConfig, connectionProps);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getCause() instanceof NonTransientRpcException);
      assertTrue(ex.getCause().getCause() instanceof SaslException);
    }
  }

  /**
   * Test showing successful handshake when authentication is disabled on Drillbit side and client also
   * doesn't provide any security properties in connection URL.
   * @throws Exception
   */
  @Test
  public void testDisableDrillbitClientAuth() throws Exception {
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(false)));

    final Properties connectionProps = new Properties();

    try {
      updateTestCluster(1, newConfig, connectionProps);
    } catch (Exception ex) {
      fail();
    }
  }

  /**
   * Test showing successful handshake when authentication is disabled but impersonation is enabled on Drillbit side
   * and client only provides USERNAME as a security property in connection URL.
   * @throws Exception
   */
  @Test
  public void testEnableDrillbitImpersonation_DisableClientAuth() throws Exception {
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(false))
        .withValue(ExecConstants.IMPERSONATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.IMPERSONATION_MAX_CHAINED_USER_HOPS,
            ConfigValueFactory.fromAnyRef(3)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, "anonymous");

    try {
      updateTestCluster(1, newConfig, connectionProps);
    } catch (Exception ex) {
      fail();
    }
  }
}

