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
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.security.KerberosHelper;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.server.rest.WebServerConstants;
import org.apache.drill.exec.server.rest.auth.DrillSpnegoAuthenticator;
import org.apache.drill.exec.server.rest.auth.DrillSpnegoLoginService;
import org.apache.drill.exec.server.rest.auth.SpnegoConfig;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.kerby.kerberos.kerb.client.JaasKrbUtil;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Authentication;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.security.PrivilegedExceptionAction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for validating {@link DrillSpnegoAuthenticator}
 */
@Category(SecurityTest.class)
public class TestDrillSpnegoAuthenticator extends BaseTest {

  private static KerberosHelper spnegoHelper;

  private static final String primaryName = "HTTP";

  private static DrillSpnegoAuthenticator spnegoAuthenticator;

  private static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @BeforeClass
  public static void setupTest() throws Exception {
    spnegoHelper = new KerberosHelper(TestDrillSpnegoAuthenticator.class.getSimpleName(), primaryName);
    spnegoHelper.setupKdc(BaseDirTestWatcher.createTempDir(dirTestWatcher.getTmpDir()));

    // (1) Refresh Kerberos config.
    // This disabled call to an unsupported internal API does not appear to be
    // required and it prevents compiling with a target of JDK 8 on newer JDKs.
    // sun.security.krb5.Config.refresh();

    // (2) Reset the default realm.
    final Field defaultRealm = KerberosName.class.getDeclaredField("defaultRealm");
    defaultRealm.setAccessible(true);
    defaultRealm.set(null, KerberosUtil.getDefaultRealm());

    // Create a DrillbitContext with service principal and keytab for DrillSpnegoLoginService
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create()
        .withValue(ExecConstants.HTTP_AUTHENTICATION_MECHANISMS,
            ConfigValueFactory.fromIterable(Lists.newArrayList("spnego")))
        .withValue(ExecConstants.HTTP_SPNEGO_PRINCIPAL,
            ConfigValueFactory.fromAnyRef(spnegoHelper.SERVER_PRINCIPAL))
        .withValue(ExecConstants.HTTP_SPNEGO_KEYTAB,
            ConfigValueFactory.fromAnyRef(spnegoHelper.serverKeytab.toString())));

    // Create mock objects for optionManager and AuthConfiguration
    final SystemOptionManager optionManager = Mockito.mock(SystemOptionManager.class);
    Mockito.when(optionManager.getOption(ExecConstants.ADMIN_USERS_VALIDATOR))
        .thenReturn(ExecConstants.ADMIN_USERS_VALIDATOR.DEFAULT_ADMIN_USERS);
    Mockito.when(optionManager.getOption(ExecConstants.ADMIN_USER_GROUPS_VALIDATOR))
        .thenReturn(ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.DEFAULT_ADMIN_USER_GROUPS);

    final DrillbitContext drillbitContext = Mockito.mock(DrillbitContext.class);
    Mockito.when(drillbitContext.getConfig()).thenReturn(newConfig);
    Mockito.when(drillbitContext.getOptionManager()).thenReturn(optionManager);

    Authenticator.AuthConfiguration authConfiguration = Mockito.mock(Authenticator.AuthConfiguration.class);

    spnegoAuthenticator = new DrillSpnegoAuthenticator("SPNEGO");
    DrillSpnegoLoginService spnegoLoginService = new DrillSpnegoLoginService(drillbitContext);

    Mockito.when(authConfiguration.getLoginService()).thenReturn(spnegoLoginService);
    Mockito.when(authConfiguration.getIdentityService()).thenReturn(new DefaultIdentityService());
    Mockito.when(authConfiguration.isSessionRenewedOnAuthentication()).thenReturn(true);

    // Set the login service and identity service inside SpnegoAuthenticator
    spnegoAuthenticator.setConfiguration(authConfiguration);
  }

  @AfterClass
  public static void cleanTest() throws Exception {
    spnegoHelper.stopKdc();
  }

  /**
   * Test to verify response when request is sent for {@link WebServerConstants#SPENGO_LOGIN_RESOURCE_PATH} from
   * unauthenticated session. Expectation is client will receive response with Negotiate header.
   */
  @Test
  public void testNewSessionReqForSpnegoLogin() throws Exception {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    final HttpSession session = Mockito.mock(HttpSession.class);

    Mockito.when(request.getSession(true)).thenReturn(session);
    Mockito.when(request.getRequestURI()).thenReturn(WebServerConstants.SPENGO_LOGIN_RESOURCE_PATH);

    final Authentication authentication = spnegoAuthenticator.validateRequest(request, response, false);

    assertEquals(authentication, Authentication.SEND_CONTINUE);
    verify(response).sendError(401);
    verify(response).setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), HttpHeader.NEGOTIATE.asString());
  }

  /**
   * Test to verify response when request is sent for {@link WebServerConstants#SPENGO_LOGIN_RESOURCE_PATH} from
   * authenticated session. Expectation is server will find the authenticated UserIdentity.
   */
  @Test
  public void testAuthClientRequestForSpnegoLoginResource() throws Exception {

    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    final HttpSession session = Mockito.mock(HttpSession.class);
    final Authentication authentication = Mockito.mock(UserAuthentication.class);

    Mockito.when(request.getSession(true)).thenReturn(session);
    Mockito.when(request.getRequestURI()).thenReturn(WebServerConstants.SPENGO_LOGIN_RESOURCE_PATH);
    Mockito.when(session.getAttribute(SessionAuthentication.__J_AUTHENTICATED)).thenReturn(authentication);

    final UserAuthentication returnedAuthentication = (UserAuthentication) spnegoAuthenticator.validateRequest
        (request, response, false);
    assertEquals(authentication, returnedAuthentication);
    verify(response, never()).sendError(401);
    verify(response, never()).setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), HttpHeader.NEGOTIATE.asString());
  }

  /**
   * Test to verify response when request is sent for any other resource other than
   * {@link WebServerConstants#SPENGO_LOGIN_RESOURCE_PATH} from authenticated session. Expectation is server will
   * find the authenticated UserIdentity and will not perform the authentication again for new resource.
   */
  @Test
  public void testAuthClientRequestForOtherPage() throws Exception {

    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    final HttpSession session = Mockito.mock(HttpSession.class);
    final Authentication authentication = Mockito.mock(UserAuthentication.class);

    Mockito.when(request.getSession(true)).thenReturn(session);
    Mockito.when(request.getRequestURI()).thenReturn(WebServerConstants.WEBSERVER_ROOT_PATH);
    Mockito.when(session.getAttribute(SessionAuthentication.__J_AUTHENTICATED)).thenReturn(authentication);

    final UserAuthentication returnedAuthentication = (UserAuthentication) spnegoAuthenticator.validateRequest
        (request, response, false);
    assertEquals(authentication, returnedAuthentication);
    verify(response, never()).sendError(401);
    verify(response, never()).setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), HttpHeader.NEGOTIATE.asString());
  }

  /**
   * Test to verify that when request is sent for {@link WebServerConstants#LOGOUT_RESOURCE_PATH} then the UserIdentity
   * will be removed from the session and returned authentication will be null from
   * {@link DrillSpnegoAuthenticator#validateRequest(javax.servlet.ServletRequest, javax.servlet.ServletResponse, boolean)}
   */
  @Test
  @Ignore("See DRILL-5387")
  public void testAuthClientRequestForLogOut() throws Exception {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    final HttpSession session = Mockito.mock(HttpSession.class);
    final Authentication authentication = Mockito.mock(UserAuthentication.class);

    Mockito.when(request.getSession(true)).thenReturn(session);
    Mockito.when(request.getRequestURI()).thenReturn(WebServerConstants.LOGOUT_RESOURCE_PATH);
    Mockito.when(session.getAttribute(SessionAuthentication.__J_AUTHENTICATED)).thenReturn(authentication);

    final UserAuthentication returnedAuthentication = (UserAuthentication) spnegoAuthenticator.validateRequest
        (request, response, false);
    assertNull(returnedAuthentication);
    verify(session).removeAttribute(SessionAuthentication.__J_AUTHENTICATED);
    verify(response, never()).sendError(401);
    verify(response, never()).setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), HttpHeader.NEGOTIATE.asString());
  }

  /**
   * Test to verify authentication fails when client sends invalid SPNEGO token for the
   * {@link WebServerConstants#SPENGO_LOGIN_RESOURCE_PATH} resource.
   */
  @Test
  public void testSpnegoLoginInvalidToken() throws Exception {

    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    final HttpSession session = Mockito.mock(HttpSession.class);

    // Create client subject using it's principal and keytab
    final Subject clientSubject = JaasKrbUtil.loginUsingKeytab(spnegoHelper.CLIENT_PRINCIPAL,
        spnegoHelper.clientKeytab.getAbsoluteFile());

    // Generate a SPNEGO token for the peer SERVER_PRINCIPAL from this CLIENT_PRINCIPAL
    final String token = Subject.doAs(clientSubject, (PrivilegedExceptionAction<String>) () -> {

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
    });

    Mockito.when(request.getSession(true)).thenReturn(session);

    final String httpReqAuthHeader = String.format("%s:%s", HttpHeader.NEGOTIATE.asString(), String.format
        ("%s%s","1234", token));
    Mockito.when(request.getHeader(HttpHeader.AUTHORIZATION.asString())).thenReturn(httpReqAuthHeader);
    Mockito.when(request.getRequestURI()).thenReturn(WebServerConstants.SPENGO_LOGIN_RESOURCE_PATH);

    assertEquals(spnegoAuthenticator.validateRequest(request, response, false), Authentication.UNAUTHENTICATED);

    verify(session, never()).setAttribute(SessionAuthentication.__J_AUTHENTICATED, null);
    verify(response, never()).sendError(401);
    verify(response, never()).setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), HttpHeader.NEGOTIATE.asString());
  }
}
