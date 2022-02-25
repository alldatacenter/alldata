/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.serveraction.kerberos;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class MITKerberosOperationHandlerTest extends KDCKerberosOperationHandlerTest {

  private static Method methodIsOpen;

  private static Method methodPrincipalExists;

  private static Method methodInvokeKAdmin;

  private static final Map<String, String> KERBEROS_ENV_MAP;

  static {
    Map<String, String> map = new HashMap<>(DEFAULT_KERBEROS_ENV_MAP);
    map.put(MITKerberosOperationHandler.KERBEROS_ENV_KDC_CREATE_ATTRIBUTES, "-attr1 -attr2 foo=345");
    KERBEROS_ENV_MAP = Collections.unmodifiableMap(map);
  }

  private Injector injector;

  @BeforeClass
  public static void beforeClassMITKerberosOperationHandlerTestC() throws Exception {
    methodIsOpen = KerberosOperationHandler.class.getDeclaredMethod("isOpen");
    methodPrincipalExists = MITKerberosOperationHandler.class.getDeclaredMethod("principalExists", String.class, boolean.class);
    methodInvokeKAdmin = MITKerberosOperationHandler.class.getDeclaredMethod("invokeKAdmin", String.class);
  }

  @Before
  public void beforeMITKerberosOperationHandlerTest() throws Exception {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.getServerOsFamily()).andReturn("redhat6").anyTimes();
        expect(configuration.getKerberosOperationRetryTimeout()).andReturn(1).anyTimes();

        bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        bind(Configuration.class).toInstance(configuration);
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });
  }


  @Test
  public void testSetPrincipalPassword() throws Exception {
    MITKerberosOperationHandler handler = createMockedHandler(methodIsOpen, methodPrincipalExists);

    expect(handler.isOpen()).andReturn(true).atLeastOnce();
    expect(handler.principalExists(DEFAULT_ADMIN_PRINCIPAL, false)).andReturn(true).atLeastOnce();
    expect(handler.principalExists(null, false)).andReturn(false).atLeastOnce();
    expect(handler.principalExists("", false)).andReturn(false).atLeastOnce();

    replayAll();

    Integer expected = 0;

    // setPrincipalPassword should always return 0
    Assert.assertEquals(expected, handler.setPrincipalPassword(DEFAULT_ADMIN_PRINCIPAL, null, false));
    Assert.assertEquals(expected, handler.setPrincipalPassword(DEFAULT_ADMIN_PRINCIPAL, "", false));

    try {
      handler.setPrincipalPassword(null, DEFAULT_ADMIN_PASSWORD, false);
      Assert.fail("Expected KerberosPrincipalDoesNotExistException");
    } catch (KerberosPrincipalDoesNotExistException e) {
      // Expected...
    }

    try {
      handler.setPrincipalPassword("", DEFAULT_ADMIN_PASSWORD, false);
      Assert.fail("Expected KerberosPrincipalDoesNotExistException");
    } catch (KerberosPrincipalDoesNotExistException e) {
      // Expected...
    }

    verifyAll();
  }

  @Test
  public void testCreateServicePrincipal_AdditionalAttributes() throws Exception {
    Capture<? extends String> query = newCapture();

    ShellCommandUtil.Result result1 = createNiceMock(ShellCommandUtil.Result.class);
    expect(result1.getStderr()).andReturn("").anyTimes();
    expect(result1.getStdout()).andReturn("Principal \"" + DEFAULT_ADMIN_PRINCIPAL + "\" created\"").anyTimes();

    ShellCommandUtil.Result result2 = createNiceMock(ShellCommandUtil.Result.class);
    expect(result2.getStderr()).andReturn("").anyTimes();
    expect(result2.getStdout()).andReturn("Key: vno 1").anyTimes();

    ShellCommandUtil.Result kinitResult = createMock(ShellCommandUtil.Result.class);
    expect(kinitResult.isSuccessful()).andReturn(true);

    MITKerberosOperationHandler handler = createMockedHandler(methodInvokeKAdmin, methodExecuteCommand);
    expect(handler.executeCommand(anyObject(String[].class), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(kinitResult)
        .once();
    expect(handler.invokeKAdmin(capture(query))).andReturn(result1).once();

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.createPrincipal(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, false);
    handler.close();

    verifyAll();

    Assert.assertTrue(query.getValue().contains(" " + KERBEROS_ENV_MAP.get(MITKerberosOperationHandler.KERBEROS_ENV_KDC_CREATE_ATTRIBUTES) + " "));
  }


  @Test
  public void testCreateServicePrincipalExceptions() throws Exception {
    ShellCommandUtil.Result kinitResult = createMock(ShellCommandUtil.Result.class);
    expect(kinitResult.isSuccessful()).andReturn(true);

    MITKerberosOperationHandler handler = createMockedHandler(methodExecuteCommand);
    expect(handler.executeCommand(anyObject(String[].class), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(kinitResult)
        .once();

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);

    try {
      handler.createPrincipal(null, DEFAULT_ADMIN_PASSWORD, false);
      Assert.fail("KerberosOperationException not thrown for null principal");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.createPrincipal("", DEFAULT_ADMIN_PASSWORD, false);
      Assert.fail("KerberosOperationException not thrown for empty principal");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    verifyAll();
  }

  @Test(expected = KerberosKDCConnectionException.class)
  public void testKDCConnectionException() throws Exception {
    ShellCommandUtil.Result kinitResult = createMock(ShellCommandUtil.Result.class);
    expect(kinitResult.isSuccessful()).andReturn(true).anyTimes();

    ShellCommandUtil.Result kadminResult = createMock(ShellCommandUtil.Result.class);
    expect(kadminResult.getExitCode()).andReturn(1).anyTimes();
    expect(kadminResult.isSuccessful()).andReturn(false).anyTimes();
    expect(kadminResult.getStderr())
        .andReturn("kadmin: Cannot contact any KDC for requested realm while initializing kadmin interface")
        .anyTimes();
    expect(kadminResult.getStdout())
        .andReturn("Authenticating as principal admin/admin with password.")
        .anyTimes();

    MITKerberosOperationHandler handler = createMockedHandler(methodExecuteCommand);
    expect(handler.executeCommand(anyObject(String[].class), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(kinitResult)
        .once();
    expect(handler.executeCommand(anyObject(String[].class), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(kadminResult)
        .once();

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();

    verifyAll();
  }

  @Test(expected = KerberosKDCConnectionException.class)
  public void testTestAdministratorCredentialsKDCConnectionException2() throws Exception {
    ShellCommandUtil.Result kinitResult = createMock(ShellCommandUtil.Result.class);
    expect(kinitResult.isSuccessful()).andReturn(true).anyTimes();

    ShellCommandUtil.Result kadminResult = createMock(ShellCommandUtil.Result.class);
    expect(kadminResult.getExitCode()).andReturn(1).anyTimes();
    expect(kadminResult.isSuccessful()).andReturn(false).anyTimes();
    expect(kadminResult.getStderr())
        .andReturn("kadmin: Cannot resolve network address for admin server in requested realm while initializing kadmin interface")
        .anyTimes();
    expect(kadminResult.getStdout())
        .andReturn("Authenticating as principal admin/admin with password.")
        .anyTimes();

    MITKerberosOperationHandler handler = createMockedHandler(methodExecuteCommand);
    expect(handler.executeCommand(anyObject(String[].class), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(kinitResult)
        .once();
    expect(handler.executeCommand(anyObject(String[].class), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(kadminResult)
        .once();

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();

    verifyAll();
  }

  @Test
  public void testGetAdminServerHost() throws KerberosOperationException {
    ShellCommandUtil.Result kinitResult = createMock(ShellCommandUtil.Result.class);
    expect(kinitResult.isSuccessful()).andReturn(true).anyTimes();

    Capture<String[]> capturedKinitCommand = newCapture(CaptureType.ALL);

    MITKerberosOperationHandler handler = createMockedHandler(methodExecuteCommand);
    expect(handler.executeCommand(capture(capturedKinitCommand), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(kinitResult)
        .anyTimes();


    Map<String,String> config = new HashMap<>();
    config.put("encryption_types", "aes des3-cbc-sha1 rc4 des-cbc-md5");
    config.put(MITKerberosOperationHandler.KERBEROS_ENV_KADMIN_PRINCIPAL_NAME, "kadmin/kdc.example.com");

    replayAll();

    config.put("admin_server_host", "kdc.example.com");
    handler.open(getAdminCredentials(), DEFAULT_REALM, config);
    Assert.assertEquals("kdc.example.com", handler.getAdminServerHost(false));
    Assert.assertEquals("kdc.example.com", handler.getAdminServerHost(true));
    handler.close();

    config.put("admin_server_host", "kdc.example.com:749");
    handler.open(getAdminCredentials(), DEFAULT_REALM, config);
    Assert.assertEquals("kdc.example.com", handler.getAdminServerHost(false));
    Assert.assertEquals("kdc.example.com:749", handler.getAdminServerHost(true));
    handler.close();

    verifyAll();

    Assert.assertTrue(capturedKinitCommand.hasCaptured());
    List<String[]> capturedValues = capturedKinitCommand.getValues();
    Assert.assertEquals(2, capturedValues.size());

    // The capture values will be an array of strings used to build the command:
    //   ["/usr/bin/kinit", "-c", "SOME_FILE_PATH", "-S", "SERVER_PRINCIPAL", "CLIENT_PRINCIPAL"]
    // We are interested in the 4th item in the array - the service's principal.
    // It must not contain the port else authentication will fail
    Assert.assertEquals("kadmin/kdc.example.com", capturedValues.get(0)[4]);
    Assert.assertEquals("kadmin/kdc.example.com", capturedValues.get(1)[4]);
  }

  @Override
  protected MITKerberosOperationHandler createMockedHandler(Method... mockedMethods) {
    MITKerberosOperationHandler handler = createMockBuilder(MITKerberosOperationHandler.class)
        .addMockedMethods(mockedMethods)
        .createMock();
    injector.injectMembers(handler);
    return handler;
  }

  @Override
  protected Map<String, String> getKerberosEnv() {
    return KERBEROS_ENV_MAP;
  }

  @Override
  protected void setupPrincipalAlreadyExists(KerberosOperationHandler handler, boolean service) throws Exception {
    ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);
    expect(result.getExitCode()).andReturn(0).anyTimes();
    expect(result.isSuccessful()).andReturn(true).anyTimes();
    expect(result.getStderr())
        .andReturn(String.format("add_principal: Principal or policy already exists while creating \"%s@EXAMPLE.COM\".", (service) ? "service/host" : "user"))
        .anyTimes();
    expect(result.getStdout())
        .andReturn("Authenticating as principal admin/admin with password.")
        .anyTimes();

    expect(handler.executeCommand(arrayContains(new String[]{"kadmin", "add_principal"}), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(result)
        .anyTimes();
  }

  @Override
  protected void setupPrincipalDoesNotExist(KerberosOperationHandler handler, boolean service) throws Exception {
    ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);
    expect(result.getExitCode()).andReturn(0).anyTimes();
    expect(result.isSuccessful()).andReturn(true).anyTimes();
    expect(result.getStderr())
        .andReturn(String.format("get_principal: Principal does not exist while retrieving \"%s@EXAMPLE.COM\".", (service) ? "service/host" : "user"))
        .anyTimes();
    expect(result.getStdout())
        .andReturn("Authenticating as principal admin/admin with password.")
        .anyTimes();

    expect(handler.executeCommand(arrayContains(new String[]{"kadmin", "get_principal"}), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(result)
        .anyTimes();
  }

  @Override
  protected void setupPrincipalExists(KerberosOperationHandler handler, boolean service) throws Exception {
    ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);
    expect(result.getExitCode()).andReturn(0).anyTimes();
    expect(result.isSuccessful()).andReturn(true).anyTimes();
    expect(result.getStderr())
        .andReturn("")
        .anyTimes();
    expect(result.getStdout())
        .andReturn(String.format("Authenticating as principal admin/admin with password.\n" +
            "Principal: %s@EXAMPLE.COM\n" +
            "Expiration date: [never]\n" +
            "Last password change: Thu Jan 08 13:09:52 UTC 2015\n" +
            "Password expiration date: [none]\n" +
            "Maximum ticket life: 1 day 00:00:00\n" +
            "Maximum renewable life: 0 days 00:00:00\n" +
            "Last modified: Thu Jan 08 13:09:52 UTC 2015 (root/admin@EXAMPLE.COM)\n" +
            "Last successful authentication: [never]\n" +
            "Last failed authentication: [never]\n" +
            "Failed password attempts: 0\n" +
            "Number of keys: 6\n" +
            "Key: vno 1, aes256-cts-hmac-sha1-96, no salt\n" +
            "Key: vno 1, aes128-cts-hmac-sha1-96, no salt\n" +
            "Key: vno 1, des3-cbc-sha1, no salt\n" +
            "Key: vno 1, arcfour-hmac, no salt\n" +
            "Key: vno 1, des-hmac-sha1, no salt\n" +
            "Key: vno 1, des-cbc-md5, no salt\n" +
            "MKey: vno 1\n" +
            "Attributes:\n" +
            "Policy: [none]", (service) ? "service/host" : "user"))
        .anyTimes();

    expect(handler.executeCommand(arrayContains(new String[]{"kadmin", "get_principal"}), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(result)
        .anyTimes();
  }
}