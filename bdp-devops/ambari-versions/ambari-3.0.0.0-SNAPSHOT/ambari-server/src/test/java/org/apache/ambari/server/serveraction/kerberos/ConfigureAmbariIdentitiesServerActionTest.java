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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import javax.persistence.EntityManager;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.serveraction.ActionLog;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class ConfigureAmbariIdentitiesServerActionTest extends EasyMockSupport {
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void installAmbariServerIdentity() throws Exception {
    installAmbariServerIdentity(createNiceMock(ActionLog.class), true);
  }

  @Test
  public void installAmbariServerIdentityWithNoAgentOnAmbariServer() throws Exception {
    installAmbariServerIdentity(createNiceMock(ActionLog.class), false);
  }

  @Test
  public void installAmbariServerIdentityWithNullActionLog() throws Exception {
    installAmbariServerIdentity(null, true);
  }

  private void installAmbariServerIdentity(ActionLog actionLog, boolean ambariServerHasAgent) throws Exception {

    String principal = "ambari-server@EXAMPLE.COM";
    File srcKeytabFile = testFolder.newFile();
    File destKeytabFile = new File(testFolder.getRoot().getAbsolutePath(), "ambari-server.keytab");

    Injector injector = createInjector();

    HostDAO hostDAO = injector.getInstance(HostDAO.class);

    HostEntity hostEntity;
    if (ambariServerHasAgent) {
      hostEntity = createMock(HostEntity.class);
      expect(hostEntity.getHostId()).andReturn(1L).once();
      expect(hostDAO.findById(1L)).andReturn(hostEntity).once();
    } else {
      hostEntity = null;
    }

    expect(hostDAO.findByName(StageUtils.getHostName())).andReturn(hostEntity).once();
    KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO = injector.getInstance(KerberosKeytabPrincipalDAO.class);
    KerberosKeytabPrincipalEntity kke = createNiceMock(KerberosKeytabPrincipalEntity.class);
    expect(kerberosKeytabPrincipalDAO.findOrCreate(anyObject(), eq(hostEntity), anyObject())).andReturn(kke).once();
    expect(kerberosKeytabPrincipalDAO.merge(kke)).andReturn(createNiceMock(KerberosKeytabPrincipalEntity.class)).once();

    // Mock the methods that do the actual file manipulation to avoid having to deal with ambari-sudo.sh used in
    // ShellCommandUtil#mkdir, ShellCommandUtil#copyFile, etc..
    Method methodCopyFile = ConfigureAmbariIdentitiesServerAction.class.getDeclaredMethod("copyFile",
        String.class, String.class);
    Method methodSetFileACL = ConfigureAmbariIdentitiesServerAction.class.getDeclaredMethod("setFileACL",
        String.class, String.class, boolean.class, boolean.class, String.class, boolean.class, boolean.class);

    ConfigureAmbariIdentitiesServerAction action = createMockBuilder(ConfigureAmbariIdentitiesServerAction.class)
        .addMockedMethod(methodCopyFile)
        .addMockedMethod(methodSetFileACL)
        .createMock();

    action.copyFile(srcKeytabFile.getAbsolutePath(), destKeytabFile.getAbsolutePath());
    expectLastCall().once();

    action.setFileACL(destKeytabFile.getAbsolutePath(), "user1", true, true, "groupA", true, false);
    expectLastCall().once();

    replayAll();

    injector.injectMembers(action);
    action.installAmbariServerIdentity(
      new ResolvedKerberosPrincipal(
        null,
        null,
        principal,
        false,
        null,
        RootService.AMBARI.name(),
        RootComponent.AMBARI_SERVER.name(),
        destKeytabFile.getPath()
      ), srcKeytabFile.getAbsolutePath(), destKeytabFile.getAbsolutePath(),
        "user1", "rw", "groupA", "r", actionLog);

    verifyAll();

    // There is no need to verify that the file was copied. We are not testing the ability to copy
    // and we have mocked the method that does the actual copying to avoid having to deal with
    // ambari-sudo.sh via the ShellCommandUtil class.
  }

  @Test
  public void configureJAAS() throws Exception {
    configureJAAS(createNiceMock(ActionLog.class));
  }

  @Test
  public void configureJAASWithNullActionLog() throws Exception {
    configureJAAS(null);
  }

  private void configureJAAS(ActionLog actionLog) throws Exception {
    String principal = "ambari-server@EXAMPLE.COM";
    String keytabFilePath = "/etc/security/keytabs/ambari.server.keytab";

    File jaasConfFile = testFolder.newFile();
    File jaasConfFileBak = new File(jaasConfFile.getAbsolutePath() + ".bak");
    String originalJAASFileContent =
        "com.sun.security.jgss.krb5.initiate {\n" +
            "    com.sun.security.auth.module.Krb5LoginModule required\n" +
            "    renewTGT=false\n" +
            "    doNotPrompt=true\n" +
            "    useKeyTab=true\n" +
            "    keyTab=\"/etc/security/keytabs/ambari.keytab\"\n" +
            "    principal=\"ambari@EXAMPLE.COM\"\n" +
            "    storeKey=true\n" +
            "    useTicketCache=false;\n" +
            "};\n";

    FileUtils.writeStringToFile(jaasConfFile, originalJAASFileContent, Charset.defaultCharset());

    Injector injector = createInjector();

    Method methodGetJAASConfFilePath = ConfigureAmbariIdentitiesServerAction.class.getDeclaredMethod("getJAASConfFilePath");

    ConfigureAmbariIdentitiesServerAction action = createMockBuilder(ConfigureAmbariIdentitiesServerAction.class)
        .addMockedMethod(methodGetJAASConfFilePath)
        .createMock();

    expect(action.getJAASConfFilePath()).andReturn(jaasConfFile.getAbsolutePath());

    replayAll();

    injector.injectMembers(action);
    action.configureJAAS(principal, keytabFilePath, actionLog);

    verifyAll();

    Assert.assertEquals(
        "com.sun.security.jgss.krb5.initiate {\n" +
            "    com.sun.security.auth.module.Krb5LoginModule required\n" +
            "    renewTGT=false\n" +
            "    doNotPrompt=true\n" +
            "    useKeyTab=true\n" +
            "    keyTab=\"/etc/security/keytabs/ambari.server.keytab\"\n" +
            "    principal=\"ambari-server@EXAMPLE.COM\"\n" +
            "    storeKey=true\n" +
            "    useTicketCache=false;\n" +
            "};\n",
            FileUtils.readFileToString(jaasConfFile, Charset.defaultCharset())
    );

    // Ensure the backup file matches the original content
    Assert.assertEquals(originalJAASFileContent,
            FileUtils.readFileToString(jaasConfFileBak, Charset.defaultCharset()));
  }


  private Injector createInjector() {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
        bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelper.class));

        bind(HostDAO.class).toInstance(createMock(HostDAO.class));
        bind(KerberosKeytabPrincipalDAO.class).toInstance(createMock(KerberosKeytabPrincipalDAO.class));
//        bind(KerberosPrincipalHostDAO.class).toInstance(createMock(KerberosPrincipalHostDAO.class));
      }
    });
  }

}