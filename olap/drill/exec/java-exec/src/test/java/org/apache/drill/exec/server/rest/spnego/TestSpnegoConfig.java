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
package org.apache.drill.exec.server.rest.spnego;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.security.KerberosHelper;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.exec.server.rest.auth.SpnegoConfig;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.reflect.Field;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for validating {@link SpnegoConfig}
 */
@Category(SecurityTest.class)
public class TestSpnegoConfig extends BaseTest {
  private static KerberosHelper spnegoHelper;

  private static final String primaryName = "HTTP";

  private static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @BeforeClass
  public static void setupTest() throws Exception {
    spnegoHelper = new KerberosHelper(TestSpnegoConfig.class.getSimpleName(), primaryName);
    spnegoHelper.setupKdc(BaseDirTestWatcher.createTempDir(dirTestWatcher.getTmpDir()));

    // (1) Refresh Kerberos config.
    // This disabled call to an unsupported internal API does not appear to be
    // required and it prevents compiling with a target of JDK 8 on newer JDKs.
    // sun.security.krb5.Config.refresh();

    // (2) Reset the default realm.
    final Field defaultRealm = KerberosName.class.getDeclaredField("defaultRealm");
    defaultRealm.setAccessible(true);
    defaultRealm.set(null, KerberosUtil.getDefaultRealm());
  }

  @AfterClass
  public static void cleanTest() throws Exception {
    spnegoHelper.stopKdc();
  }

  /**
   * Test invalid {@link SpnegoConfig} with missing keytab and principal
   * @throws Exception
   */
  @Test
  public void testInvalidSpnegoConfig() throws Exception {
    // Invalid configuration for SPNEGO
    try {
      final DrillConfig newConfig = new DrillConfig(DrillConfig.create()
          .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
              ConfigValueFactory.fromAnyRef(true))
          .withValue(ExecConstants.AUTHENTICATION_MECHANISMS,
              ConfigValueFactory.fromIterable(Lists.newArrayList("plain")))
          .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
              ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE)));

      final SpnegoConfig spnegoConfig = new SpnegoConfig(newConfig);
      spnegoConfig.validateSpnegoConfig();
      fail();
    } catch (Exception ex) {
      assertTrue(ex instanceof DrillException);
    }
  }

  /**
   * Invalid configuration with keytab only and missing principal
   * @throws Exception
   */
  @Test
  public void testSpnegoConfigOnlyKeytab() throws Exception {
    try {
      final DrillConfig newConfig = new DrillConfig(DrillConfig.create().withValue(ExecConstants.USER_AUTHENTICATION_ENABLED, ConfigValueFactory.fromAnyRef(true)).withValue(ExecConstants.AUTHENTICATION_MECHANISMS, ConfigValueFactory.fromIterable(Lists.newArrayList("plain"))).withValue(ExecConstants.HTTP_SPNEGO_KEYTAB, ConfigValueFactory.fromAnyRef(spnegoHelper.serverKeytab.toString())).withValue(ExecConstants.USER_AUTHENTICATOR_IMPL, ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE)));

      final SpnegoConfig spnegoConfig = new SpnegoConfig(newConfig);
      spnegoConfig.validateSpnegoConfig();
      fail();
    } catch (Exception ex) {
      assertTrue(ex instanceof DrillException);
    }
  }

  /**
   * Invalid configuration with principal only and missing keytab
   * @throws Exception
   */
  @Test
  public void testSpnegoConfigOnlyPrincipal() throws Exception {
    try {
      final DrillConfig newConfig = new DrillConfig(DrillConfig.create().withValue(ExecConstants.USER_AUTHENTICATION_ENABLED, ConfigValueFactory.fromAnyRef(true)).withValue(ExecConstants.AUTHENTICATION_MECHANISMS, ConfigValueFactory.fromIterable(Lists.newArrayList("plain"))).withValue(ExecConstants.HTTP_SPNEGO_PRINCIPAL, ConfigValueFactory.fromAnyRef(spnegoHelper.SERVER_PRINCIPAL)).withValue(ExecConstants.USER_AUTHENTICATOR_IMPL, ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE)));

      final SpnegoConfig spnegoConfig = new SpnegoConfig(newConfig);
      spnegoConfig.validateSpnegoConfig();
      fail();
    } catch (Exception ex) {
      assertTrue(ex instanceof DrillException);
    }
  }

  /**
   * Valid Configuration with both keytab & principal
   * @throws Exception
   */
  @Test
  public void testValidSpnegoConfig() throws Exception {

    try {
      final DrillConfig newConfig = new DrillConfig(DrillConfig.create()
          .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
              ConfigValueFactory.fromAnyRef(true))
          .withValue(ExecConstants.AUTHENTICATION_MECHANISMS,
              ConfigValueFactory.fromIterable(Lists.newArrayList("plain")))
          .withValue(ExecConstants.HTTP_SPNEGO_PRINCIPAL,
              ConfigValueFactory.fromAnyRef(spnegoHelper.SERVER_PRINCIPAL))
          .withValue(ExecConstants.HTTP_SPNEGO_KEYTAB,
              ConfigValueFactory.fromAnyRef(spnegoHelper.serverKeytab.toString()))
          .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
              ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE)));

      final SpnegoConfig spnegoConfig = new SpnegoConfig(newConfig);
      spnegoConfig.validateSpnegoConfig();
      UserGroupInformation ugi = spnegoConfig.getLoggedInUgi();
      assertEquals(primaryName, ugi.getShortUserName());
      assertEquals(spnegoHelper.SERVER_PRINCIPAL, ugi.getUserName());
    } catch (Exception ex) {
      fail();
    }
  }
}
