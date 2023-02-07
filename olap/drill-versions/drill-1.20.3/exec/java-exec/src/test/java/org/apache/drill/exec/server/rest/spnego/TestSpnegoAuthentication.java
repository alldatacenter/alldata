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
import org.apache.commons.codec.binary.Base64;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.rpc.security.AuthenticatorProviderImpl;
import org.apache.drill.exec.rpc.security.KerberosHelper;
import org.apache.drill.exec.rpc.security.plain.PlainFactory;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.server.rest.auth.DrillHttpSecurityHandlerProvider;
import org.apache.drill.exec.server.rest.auth.DrillSpnegoLoginService;
import org.apache.drill.exec.server.rest.auth.SpnegoConfig;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.kerby.kerberos.kerb.client.JaasKrbUtil;
import org.eclipse.jetty.server.UserIdentity;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import javax.security.auth.Subject;
import java.lang.reflect.Field;
import java.security.PrivilegedExceptionAction;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for validating {@link DrillSpnegoLoginService}
 */
@Category(SecurityTest.class)
public class TestSpnegoAuthentication extends BaseTest {

  private static KerberosHelper spnegoHelper;

  private static final String primaryName = "HTTP";

  private static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @BeforeClass
  public static void setupTest() throws Exception {
    spnegoHelper = new KerberosHelper(TestSpnegoAuthentication.class.getSimpleName(), primaryName);
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

  /**
   * Both SPNEGO and FORM mechanism is enabled for WebServer in configuration. Test to see if the respective security
   * handlers are created successfully or not.
   */
  @Test
  public void testSPNEGOAndFORMEnabled() throws Exception {

    final DrillConfig newConfig = new DrillConfig(DrillConfig.create()
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.HTTP_AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("form", "spnego")))
        .withValue(ExecConstants.HTTP_SPNEGO_PRINCIPAL,
            ConfigValueFactory.fromAnyRef(spnegoHelper.SERVER_PRINCIPAL))
        .withValue(ExecConstants.HTTP_SPNEGO_KEYTAB,
            ConfigValueFactory.fromAnyRef(spnegoHelper.serverKeytab.toString())));

    final ScanResult scanResult = ClassPathScanner.fromPrescan(newConfig);
    final AuthenticatorProviderImpl authenticatorProvider = Mockito.mock(AuthenticatorProviderImpl.class);
    Mockito.when(authenticatorProvider.containsFactory(PlainFactory.SIMPLE_NAME)).thenReturn(true);

    final DrillbitContext context = Mockito.mock(DrillbitContext.class);
    Mockito.when(context.getClasspathScan()).thenReturn(scanResult);
    Mockito.when(context.getConfig()).thenReturn(newConfig);
    Mockito.when(context.getAuthProvider()).thenReturn(authenticatorProvider);

    final DrillHttpSecurityHandlerProvider securityProvider = new DrillHttpSecurityHandlerProvider(newConfig, context);
    assertTrue(securityProvider.isFormEnabled());
    assertTrue(securityProvider.isSpnegoEnabled());
  }

  /**
   * Validate if FORM security handler is created successfully when only form is configured as auth mechanism
   */
  @Test
  public void testOnlyFORMEnabled() throws Exception {

    final DrillConfig newConfig = new DrillConfig(DrillConfig.create()
        .withValue(ExecConstants.HTTP_AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("form")))
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.HTTP_SPNEGO_PRINCIPAL,
            ConfigValueFactory.fromAnyRef(spnegoHelper.SERVER_PRINCIPAL))
        .withValue(ExecConstants.HTTP_SPNEGO_KEYTAB,
            ConfigValueFactory.fromAnyRef(spnegoHelper.serverKeytab.toString())));

    final ScanResult scanResult = ClassPathScanner.fromPrescan(newConfig);
    final AuthenticatorProviderImpl authenticatorProvider = Mockito.mock(AuthenticatorProviderImpl.class);
    Mockito.when(authenticatorProvider.containsFactory(PlainFactory.SIMPLE_NAME)).thenReturn(true);

    final DrillbitContext context = Mockito.mock(DrillbitContext.class);
    Mockito.when(context.getClasspathScan()).thenReturn(scanResult);
    Mockito.when(context.getConfig()).thenReturn(newConfig);
    Mockito.when(context.getAuthProvider()).thenReturn(authenticatorProvider);

    final DrillHttpSecurityHandlerProvider securityProvider = new DrillHttpSecurityHandlerProvider(newConfig, context);
    assertTrue(securityProvider.isFormEnabled());
    assertTrue(!securityProvider.isSpnegoEnabled());
  }

  /**
   * Validate failure in creating FORM security handler when PAM authenticator is absent. PAM authenticator is provided
   * via {@link PlainFactory#getAuthenticator()}
   */
  @Test
  public void testFORMEnabledWithPlainDisabled() throws Exception {
    try {
      final DrillConfig newConfig = new DrillConfig(DrillConfig.create()
          .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
              ConfigValueFactory.fromAnyRef(true))
          .withValue(ExecConstants.HTTP_AUTHENTICATION_MECHANISMS,
              ConfigValueFactory.fromIterable(Lists.newArrayList("form")))
          .withValue(ExecConstants.HTTP_SPNEGO_PRINCIPAL,
              ConfigValueFactory.fromAnyRef(spnegoHelper.SERVER_PRINCIPAL))
          .withValue(ExecConstants.HTTP_SPNEGO_KEYTAB,
              ConfigValueFactory.fromAnyRef(spnegoHelper.serverKeytab.toString())));

      final ScanResult scanResult = ClassPathScanner.fromPrescan(newConfig);
      final AuthenticatorProviderImpl authenticatorProvider = Mockito.mock(AuthenticatorProviderImpl.class);
      Mockito.when(authenticatorProvider.containsFactory(PlainFactory.SIMPLE_NAME)).thenReturn(false);

      final DrillbitContext context = Mockito.mock(DrillbitContext.class);
      Mockito.when(context.getClasspathScan()).thenReturn(scanResult);
      Mockito.when(context.getConfig()).thenReturn(newConfig);
      Mockito.when(context.getAuthProvider()).thenReturn(authenticatorProvider);

      final DrillHttpSecurityHandlerProvider securityProvider =
          new DrillHttpSecurityHandlerProvider(newConfig, context);
      fail();
    } catch(Exception ex) {
      assertTrue(ex instanceof DrillbitStartupException);
    }
  }

  /**
   * Validate only SPNEGO security handler is configured properly when enabled via configuration
   */
  @Test
  public void testOnlySPNEGOEnabled() throws Exception {

    final DrillConfig newConfig = new DrillConfig(DrillConfig.create()
        .withValue(ExecConstants.HTTP_AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("spnego")))
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.HTTP_SPNEGO_PRINCIPAL,
            ConfigValueFactory.fromAnyRef(spnegoHelper.SERVER_PRINCIPAL))
        .withValue(ExecConstants.HTTP_SPNEGO_KEYTAB,
            ConfigValueFactory.fromAnyRef(spnegoHelper.serverKeytab.toString())));

    final ScanResult scanResult = ClassPathScanner.fromPrescan(newConfig);
    final AuthenticatorProviderImpl authenticatorProvider = Mockito.mock(AuthenticatorProviderImpl.class);
    Mockito.when(authenticatorProvider.containsFactory(PlainFactory.SIMPLE_NAME)).thenReturn(false);

    final DrillbitContext context = Mockito.mock(DrillbitContext.class);
    Mockito.when(context.getClasspathScan()).thenReturn(scanResult);
    Mockito.when(context.getConfig()).thenReturn(newConfig);
    Mockito.when(context.getAuthProvider()).thenReturn(authenticatorProvider);

    final DrillHttpSecurityHandlerProvider securityProvider = new DrillHttpSecurityHandlerProvider(newConfig, context);

    assertTrue(!securityProvider.isFormEnabled());
    assertTrue(securityProvider.isSpnegoEnabled());
  }

  /**
   * Validate when none of the security mechanism is specified in the
   * {@link ExecConstants#HTTP_AUTHENTICATION_MECHANISMS}, FORM security handler is still configured correctly when
   * authentication is enabled along with PAM authenticator module.
   */
  @Test
  public void testConfigBackwardCompatibility() throws Exception {

    final DrillConfig newConfig = new DrillConfig(DrillConfig.create()
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true)));

    final ScanResult scanResult = ClassPathScanner.fromPrescan(newConfig);
    final AuthenticatorProviderImpl authenticatorProvider = Mockito.mock(AuthenticatorProviderImpl.class);
    Mockito.when(authenticatorProvider.containsFactory(PlainFactory.SIMPLE_NAME)).thenReturn(true);

    final DrillbitContext context = Mockito.mock(DrillbitContext.class);
    Mockito.when(context.getClasspathScan()).thenReturn(scanResult);
    Mockito.when(context.getConfig()).thenReturn(newConfig);
    Mockito.when(context.getAuthProvider()).thenReturn(authenticatorProvider);

    final DrillHttpSecurityHandlerProvider securityProvider = new DrillHttpSecurityHandlerProvider(newConfig, context);

    assertTrue(securityProvider.isFormEnabled());
    assertTrue(!securityProvider.isSpnegoEnabled());
  }

  /**
   * Validate successful {@link DrillSpnegoLoginService#login(String, Object, javax.servlet.ServletRequest)}
   * when provided with client token for a configured service principal.
   */
  @Test
  @Ignore("See DRILL-5387")
  public void testDrillSpnegoLoginService() throws Exception {

    // Create client subject using it's principal and keytab
    final Subject clientSubject = JaasKrbUtil.loginUsingKeytab(spnegoHelper.CLIENT_PRINCIPAL,
      spnegoHelper.clientKeytab.getAbsoluteFile());

    // Generate a SPNEGO token for the peer SERVER_PRINCIPAL from this CLIENT_PRINCIPAL
    final String token = Subject.doAs(clientSubject, new PrivilegedExceptionAction<String>() {
      @Override
      public String run() throws Exception {

        final GSSManager gssManager = GSSManager.getInstance();
        GSSContext gssContext = null;
        try {
          final Oid oid = new Oid(SpnegoConfig.GSS_SPNEGO_MECH_OID);
          final GSSName serviceName = gssManager.createName(spnegoHelper.SERVER_PRINCIPAL, GSSName.NT_USER_NAME, oid);

          gssContext = gssManager.createContext(serviceName, oid, null, GSSContext.DEFAULT_LIFETIME);
          gssContext.requestCredDeleg(true);
          gssContext.requestMutualAuth(true);

          byte[] outToken = new byte[0];
          outToken = gssContext.initSecContext(outToken, 0, outToken.length);
          return Base64.encodeBase64String(outToken);

        } finally {
          if (gssContext != null) {
            gssContext.dispose();
          }
        }
      }
    });

    // Create a DrillbitContext with service principal and keytab for DrillSpnegoLoginService
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create()
      .withValue(ExecConstants.HTTP_AUTHENTICATION_MECHANISMS,
        ConfigValueFactory.fromIterable(Lists.newArrayList("spnego")))
      .withValue(ExecConstants.HTTP_SPNEGO_PRINCIPAL,
        ConfigValueFactory.fromAnyRef(spnegoHelper.SERVER_PRINCIPAL))
      .withValue(ExecConstants.HTTP_SPNEGO_KEYTAB,
        ConfigValueFactory.fromAnyRef(spnegoHelper.serverKeytab.toString())));


    final SystemOptionManager optionManager = Mockito.mock(SystemOptionManager.class);
    Mockito.when(optionManager.getOption(ExecConstants.ADMIN_USERS_VALIDATOR))
      .thenReturn(ExecConstants.ADMIN_USERS_VALIDATOR.DEFAULT_ADMIN_USERS);
    Mockito.when(optionManager.getOption(ExecConstants.ADMIN_USER_GROUPS_VALIDATOR))
      .thenReturn(ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.DEFAULT_ADMIN_USER_GROUPS);

    final DrillbitContext drillbitContext = Mockito.mock(DrillbitContext.class);
    Mockito.when(drillbitContext.getConfig()).thenReturn(newConfig);
    Mockito.when(drillbitContext.getOptionManager()).thenReturn(optionManager);

    final DrillSpnegoLoginService loginService = new DrillSpnegoLoginService(drillbitContext);

    // Authenticate the client using its SPNEGO token
    final UserIdentity user = loginService.login(null, token, null);

    // Validate the UserIdentity of authenticated client
    assertNotNull(user);
    assertEquals(user.getUserPrincipal().getName(), spnegoHelper.CLIENT_SHORT_NAME);
    assertTrue(user.isUserInRole("authenticated", null));
  }

  @AfterClass
  public static void cleanTest() throws Exception {
    spnegoHelper.stopKdc();
  }
}
