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
import static org.easymock.EasyMock.replay;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class IPAKerberosOperationHandlerTest extends KDCKerberosOperationHandlerTest {

  private static Injector injector;

  private static final Map<String, String> KERBEROS_ENV_MAP;

  static {
    Map<String, String> map = new HashMap<>(DEFAULT_KERBEROS_ENV_MAP);
    map.put(IPAKerberosOperationHandler.KERBEROS_ENV_USER_PRINCIPAL_GROUP, "");
    KERBEROS_ENV_MAP = Collections.unmodifiableMap(map);
  }


  @BeforeClass
  public static void beforeIPAKerberosOperationHandlerTest() throws Exception {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Configuration configuration = EasyMock.createNiceMock(Configuration.class);
        expect(configuration.getServerOsFamily()).andReturn("redhat6").anyTimes();
        replay(configuration);

        bind(Clusters.class).toInstance(EasyMock.createNiceMock(Clusters.class));
        bind(Configuration.class).toInstance(configuration);
        bind(OsFamily.class).toInstance(EasyMock.createNiceMock(OsFamily.class));
      }
    });
  }

  @Test
  public void testGetAdminServerHost() throws KerberosOperationException {
    ShellCommandUtil.Result kinitResult = createMock(ShellCommandUtil.Result.class);
    expect(kinitResult.isSuccessful()).andReturn(true).anyTimes();

    Capture<String[]> capturedKinitCommand = newCapture(CaptureType.ALL);

    IPAKerberosOperationHandler handler = createMockedHandler(methodExecuteCommand);
    expect(handler.executeCommand(capture(capturedKinitCommand), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(kinitResult)
        .anyTimes();


    Map<String,String> config = new HashMap<>();
    config.put("encryption_types", "aes des3-cbc-sha1 rc4 des-cbc-md5");

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
  }

  @Override
  protected IPAKerberosOperationHandler createMockedHandler(Method... mockedMethods) {
    IPAKerberosOperationHandler handler = createMockBuilder(IPAKerberosOperationHandler.class)
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
    expect(result.getExitCode()).andReturn(1).anyTimes();
    expect(result.isSuccessful()).andReturn(false).anyTimes();
    if(service) {
      expect(result.getStderr()).andReturn("ipa: ERROR: service with name \"service/host@EXAMPLE.COM\" already exists").anyTimes();
    }
    else {
      expect(result.getStderr()).andReturn("ipa: ERROR: user with name \"user\" already exists").anyTimes();
    }
    expect(result.getStdout()).andReturn("").anyTimes();

    expect(handler.executeCommand(arrayContains(new String[]{"ipa", (service) ? "service-add" : "user-add"}), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(result)
        .anyTimes();
  }

  @Override
  protected void setupPrincipalDoesNotExist(KerberosOperationHandler handler, boolean service) throws Exception {
    ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);
    expect(result.getExitCode()).andReturn(2).anyTimes();
    expect(result.isSuccessful()).andReturn(false).anyTimes();
    expect(result.getStderr()).andReturn(String.format("ipa: ERROR: %s: user not found", (service) ? "service/host" : "user")).anyTimes();
    expect(result.getStdout()).andReturn("").anyTimes();


    expect(handler.executeCommand(arrayContains(new String[]{"ipa", (service) ? "service-show" : "user-show"}), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(result)
        .anyTimes();
  }

  @Override
  protected void setupPrincipalExists(KerberosOperationHandler handler, boolean service) throws Exception {
    ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);
    expect(result.getExitCode()).andReturn(0).anyTimes();
    expect(result.isSuccessful()).andReturn(true).anyTimes();
    expect(result.getStderr()).andReturn("").anyTimes();
    expect(result.getStdout()).andReturn(String.format("  User login: %s\n" +
        "  Last name: User\n" +
        "  Home directory: /home/user\n" +
        "  Login shell: /bin/bash\n" +
        "  Principal alias: user@EXAMPLE.COM\n" +
        "  UID: 324200000\n" +
        "  GID: 324200000\n" +
        "  Account disabled: False\n" +
        "  Password: True\n" +
        "  Member of groups: users\n" +
        "  Kerberos keys available: True", (service) ? "service/host" : "user")).anyTimes();

    expect(handler.executeCommand(arrayContains(new String[]{"ipa", (service) ? "service-show" : "user-show"}), anyObject(Map.class), anyObject(KDCKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andReturn(result)
        .anyTimes();
  }
}
