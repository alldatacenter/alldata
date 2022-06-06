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
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.serveraction.kerberos.stageutils.KerberosKeytabController;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class KerberosServerActionTest extends EasyMockSupport {

  private  static final Map<String, String> KERBEROS_ENV_PROPERTIES = Collections.singletonMap("admin_server_host", "kdc.example.com");

  Map<String, String> commandParams = new HashMap<>();
  File temporaryDirectory;
  private Injector injector;
  private KerberosServerAction action;
  private Cluster cluster;
  private KerberosKeytabController kerberosKeytabController;

  @Before
  public void setUp() throws Exception {

    Config kerberosEnvConfig = createMock(Config.class);
    expect(kerberosEnvConfig.getProperties()).andReturn(KERBEROS_ENV_PROPERTIES).anyTimes();

    cluster = createMock(Cluster.class);
    expect(cluster.getDesiredConfigByType("kerberos-env")).andReturn(kerberosEnvConfig).anyTimes();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster(anyString())).andReturn(cluster).anyTimes();

    ExecutionCommand mockExecutionCommand = createMock(ExecutionCommand.class);
    HostRoleCommand mockHostRoleCommand = createMock(HostRoleCommand.class);
    kerberosKeytabController = createMock(KerberosKeytabController.class);
    expect(kerberosKeytabController.adjustServiceComponentFilter(anyObject(), eq(true), anyObject())).andReturn(null).anyTimes();
    expect(kerberosKeytabController.getFilteredKeytabs((Collection<KerberosIdentityDescriptor>)null, null, null))
      .andReturn(
        Sets.newHashSet(new ResolvedKerberosKeytab(
          null,
          null,
          null,
          null,
          null,
          Sets.newHashSet(new ResolvedKerberosPrincipal(1l, "host", "principal", true, "/tmp", "SERVICE", "COMPONENT", "/tmp")),
          true,
          true))
      ).anyTimes();

    action = new KerberosServerAction() {

      @Override
      protected CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal,
                                              KerberosOperationHandler operationHandler,
                                              Map<String, String> kerberosConfiguration,
                                              boolean includedInFilter,
                                              Map<String, Object> requestSharedDataContext)
          throws AmbariException {
        Assert.assertNotNull(requestSharedDataContext);

        if (requestSharedDataContext.get("FAIL") != null) {
          return createCommandReport(1, HostRoleStatus.FAILED, "{}", "ERROR", "ERROR");
        } else {
          requestSharedDataContext.put(resolvedPrincipal.getPrincipal(), resolvedPrincipal.getPrincipal());
          return null;
        }
      }

      @Override
      public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
          throws AmbariException, InterruptedException {
        return processIdentities(requestSharedDataContext);
      }
    };
    action.setExecutionCommand(mockExecutionCommand);
    action.setHostRoleCommand(mockHostRoleCommand);

    injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(KerberosHelper.class).toInstance(createMock(KerberosHelper.class));
        bind(KerberosServerAction.class).toInstance(action);
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));

        bind(Clusters.class).toInstance(clusters);
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
        bind(KerberosOperationHandlerFactory.class).toInstance(createMock(KerberosOperationHandlerFactory.class));
        bind(KerberosKeytabController.class).toInstance(kerberosKeytabController);
      }
    });

    temporaryDirectory = File.createTempFile("ambari_ut_", ".d");

    Assert.assertTrue(temporaryDirectory.delete());
    Assert.assertTrue(temporaryDirectory.mkdirs());

    // Create a data file
    KerberosIdentityDataFileWriter writer =
        new KerberosIdentityDataFileWriter(new File(temporaryDirectory, KerberosIdentityDataFileWriter.DATA_FILE_NAME));
    for (int i = 0; i < 10; i++) {
      writer.writeRecord("hostName", "serviceName" + i, "serviceComponentName" + i,
          "principal|_HOST|_REALM" + i, "principal_type", "keytabFilePath" + i,
          "keytabFileOwnerName" + i, "keytabFileOwnerAccess" + i,
          "keytabFileGroupName" + i, "keytabFileGroupAccess" + i,
          "false");
    }
    writer.close();

    commandParams.put(KerberosServerAction.DATA_DIRECTORY, temporaryDirectory.getAbsolutePath());
    commandParams.put(KerberosServerAction.DEFAULT_REALM, "REALM.COM");
    commandParams.put(KerberosServerAction.KDC_TYPE, KDCType.MIT_KDC.toString());

    expect(mockExecutionCommand.getCommandParams()).andReturn(commandParams).anyTimes();
    expect(mockExecutionCommand.getClusterName()).andReturn("c1").anyTimes();
    expect(mockExecutionCommand.getClusterId()).andReturn("1").anyTimes();
    expect(mockExecutionCommand.getConfigurations()).andReturn(Collections.emptyMap()).anyTimes();
    expect(mockExecutionCommand.getRoleCommand()).andReturn(null).anyTimes();
    expect(mockExecutionCommand.getRole()).andReturn(null).anyTimes();
    expect(mockExecutionCommand.getServiceName()).andReturn(null).anyTimes();
    expect(mockExecutionCommand.getTaskId()).andReturn(1L).anyTimes();

    expect(mockHostRoleCommand.getRequestId()).andReturn(1L).anyTimes();
    expect(mockHostRoleCommand.getStageId()).andReturn(1L).anyTimes();
  }

  @After
  public void tearDown() throws Exception {
    if (temporaryDirectory != null) {
      new File(temporaryDirectory, KerberosIdentityDataFileWriter.DATA_FILE_NAME).delete();
      temporaryDirectory.delete();
    }
  }

  @Test
  public void testGetCommandParameterValueStatic() throws Exception {
    Assert.assertNull(KerberosServerAction.getCommandParameterValue(commandParams, "nonexistingvalue"));
    Assert.assertEquals("REALM.COM", KerberosServerAction.getCommandParameterValue(commandParams, KerberosServerAction.DEFAULT_REALM));
  }

  @Test
  public void testGetDefaultRealmStatic() throws Exception {
    Assert.assertEquals("REALM.COM", KerberosServerAction.getDefaultRealm(commandParams));
  }

  @Test
  public void testGetKDCTypeStatic() throws Exception {
    Assert.assertEquals(KDCType.MIT_KDC, KerberosServerAction.getKDCType(commandParams));
  }

  @Test
  public void testGetDataDirectoryPathStatic() throws Exception {
    Assert.assertEquals(temporaryDirectory.getAbsolutePath(),
        KerberosServerAction.getDataDirectoryPath(commandParams));
  }

  @Test
  public void testSetPrincipalPasswordMapStatic() throws Exception {
    ConcurrentMap<String, Object> sharedMap = new ConcurrentHashMap<>();
    Map<String, String> dataMap = new HashMap<>();

    KerberosServerAction.setPrincipalPasswordMap(sharedMap, dataMap);
    Assert.assertSame(dataMap, KerberosServerAction.getPrincipalPasswordMap(sharedMap));
  }

  @Test
  public void testGetPrincipalPasswordMapStatic() throws Exception {
    ConcurrentMap<String, Object> sharedMap = new ConcurrentHashMap<>();
    Assert.assertNotNull(KerberosServerAction.getPrincipalPasswordMap(sharedMap));
  }

  @Test
  public void testGetDataDirectoryPath() throws Exception {
    replayAll();
    Assert.assertEquals(temporaryDirectory.getAbsolutePath(), action.getDataDirectoryPath());
    verifyAll();
  }

  @Test
  public void testProcessIdentitiesSuccess() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);
    expect(kerberosHelper.getKDCAdministratorCredentials(anyObject(String.class)))
        .andReturn(new PrincipalKeyCredential("principal", "password"))
        .anyTimes();

    KerberosOperationHandler kerberosOperationHandler = createMock(KerberosOperationHandler.class);
    kerberosOperationHandler.open(anyObject(PrincipalKeyCredential.class), anyString(), anyObject(Map.class));
    expectLastCall().atLeastOnce();
    kerberosOperationHandler.close();
    expectLastCall().atLeastOnce();

    KerberosOperationHandlerFactory factory = injector.getInstance(KerberosOperationHandlerFactory.class);
    expect(factory.getKerberosOperationHandler(KDCType.MIT_KDC)).andReturn(kerberosOperationHandler).once();

    replayAll();

    ConcurrentMap<String, Object> sharedMap = new ConcurrentHashMap<>();
    CommandReport report = action.processIdentities(sharedMap);
    Assert.assertNotNull(report);
    Assert.assertEquals(HostRoleStatus.COMPLETED.toString(), report.getStatus());

    for (Map.Entry<String, Object> entry : sharedMap.entrySet()) {
      Assert.assertEquals(entry.getValue(), entry.getKey());
    }

    verifyAll();
  }

  @Test
  public void testProcessIdentitiesFail() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);
    expect(kerberosHelper.getKDCAdministratorCredentials(anyObject(String.class)))
        .andReturn(new PrincipalKeyCredential("principal", "password"))
        .anyTimes();

    KerberosOperationHandler kerberosOperationHandler = createMock(KerberosOperationHandler.class);
    kerberosOperationHandler.open(anyObject(PrincipalKeyCredential.class), anyString(), anyObject(Map.class));
    expectLastCall().atLeastOnce();
    kerberosOperationHandler.close();
    expectLastCall().atLeastOnce();

    KerberosOperationHandlerFactory factory = injector.getInstance(KerberosOperationHandlerFactory.class);
    expect(factory.getKerberosOperationHandler(KDCType.MIT_KDC)).andReturn(kerberosOperationHandler).once();

    replayAll();

    ConcurrentMap<String, Object> sharedMap = new ConcurrentHashMap<>();
    sharedMap.put("FAIL", "true");

    CommandReport report = action.processIdentities(sharedMap);
    Assert.assertNotNull(report);
    Assert.assertEquals(HostRoleStatus.FAILED.toString(), report.getStatus());

    verifyAll();
  }

  @Test
  public void testGetConfigurationProperties() throws AmbariException {
    Config emptyConfig = createMock(Config.class);
    expect(emptyConfig.getProperties()).andReturn(Collections.emptyMap()).once();

    Config missingPropertiesConfig = createMock(Config.class);
    expect(missingPropertiesConfig.getProperties()).andReturn(null).once();

    expect(cluster.getDesiredConfigByType("invalid-type")).andReturn(null).once();
    expect(cluster.getDesiredConfigByType("missing-properties-type")).andReturn(missingPropertiesConfig).once();
    expect(cluster.getDesiredConfigByType("empty-type")).andReturn(emptyConfig).once();

    replayAll();

    Assert.assertNull(action.getConfigurationProperties(null));
    Assert.assertNull(action.getConfigurationProperties("invalid-type"));
    Assert.assertNull(action.getConfigurationProperties("missing-properties-type"));
    Assert.assertEquals(Collections.emptyMap(), action.getConfigurationProperties("empty-type"));
    Assert.assertEquals(KERBEROS_ENV_PROPERTIES, action.getConfigurationProperties("kerberos-env"));

    verifyAll();
  }
}
