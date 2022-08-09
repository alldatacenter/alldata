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

import static org.easymock.EasyMock.expect;
import static org.mockito.Matchers.anyBoolean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

import junit.framework.Assert;

public class FinalizeKerberosServerActionTest extends EasyMockSupport {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private final TopologyHolder topologyHolder = createNiceMock(TopologyHolder.class);

  @Test
  @Ignore("Update accordingly to changes")
  public void executeMITKDCOption() throws Exception {
    String clusterName = "c1";
    String clusterId = "1";
    Injector injector = setup(clusterName);

    File dataDirectory = createDataDirectory();

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put(KerberosServerAction.KDC_TYPE, KDCType.MIT_KDC.name());
    commandParams.put(KerberosServerAction.DATA_DIRECTORY, dataDirectory.getAbsolutePath());

    ExecutionCommand executionCommand = createMockExecutionCommand(clusterId, clusterName, commandParams);
    HostRoleCommand hostRoleCommand = createMockHostRoleCommand();

    PrincipalKeyCredential principleKeyCredential = createMock(PrincipalKeyCredential.class);

    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);
    expect(kerberosHelper.getKDCAdministratorCredentials(clusterName)).andReturn(principleKeyCredential).anyTimes();

    replayAll();

    ConcurrentMap<String, Object> requestSharedDataContext = new ConcurrentHashMap<>();

    FinalizeKerberosServerAction action = new FinalizeKerberosServerAction(topologyHolder);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    Assert.assertTrue(dataDirectory.exists());

    CommandReport commandReport = action.execute(requestSharedDataContext);

    assertSuccess(commandReport);
    Assert.assertTrue(!dataDirectory.exists());

    verifyAll();
  }

  @Test
  public void executeManualOption() throws Exception {
    String clusterName = "c1";
    String clusterId = "1";
    Injector injector = setup(clusterName);

    File dataDirectory = createDataDirectory();

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put(KerberosServerAction.DATA_DIRECTORY, dataDirectory.getAbsolutePath());

    ExecutionCommand executionCommand = createMockExecutionCommand(clusterId, clusterName, commandParams);
    HostRoleCommand hostRoleCommand = createMockHostRoleCommand();

    replayAll();

    ConcurrentMap<String, Object> requestSharedDataContext = new ConcurrentHashMap<>();

    FinalizeKerberosServerAction action = new FinalizeKerberosServerAction(topologyHolder);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    Assert.assertTrue(dataDirectory.exists());

    CommandReport commandReport = action.execute(requestSharedDataContext);

    assertSuccess(commandReport);

    Assert.assertTrue(!dataDirectory.exists());

    verifyAll();
  }

  private File createDataDirectory() throws IOException {
    File directory = folder.newFolder();
    File dataDirectory = new File(directory, KerberosServerAction.DATA_DIRECTORY_PREFIX + "_test");

    Assert.assertTrue(dataDirectory.mkdir());

    return dataDirectory;
  }

  private void assertSuccess(CommandReport commandReport) {
    Assert.assertEquals(0, commandReport.getExitCode());
    Assert.assertEquals(HostRoleStatus.COMPLETED.name(), commandReport.getStatus());
    Assert.assertEquals("{}", commandReport.getStructuredOut());
  }

  private ExecutionCommand createMockExecutionCommand(String clusterId, String clusterName, Map<String, String> commandParams) {
    ExecutionCommand executionCommand = createMock(ExecutionCommand.class);
    expect(executionCommand.getClusterId()).andReturn(clusterId).anyTimes();
    expect(executionCommand.getClusterName()).andReturn(clusterName).anyTimes();
    expect(executionCommand.getCommandParams()).andReturn(commandParams).anyTimes();
    expect(executionCommand.getRoleCommand()).andReturn(RoleCommand.EXECUTE).anyTimes();
    expect(executionCommand.getRole()).andReturn(Role.AMBARI_SERVER_ACTION.name()).anyTimes();
    expect(executionCommand.getServiceName()).andReturn(RootComponent.AMBARI_SERVER.name()).anyTimes();
    expect(executionCommand.getTaskId()).andReturn(3L).anyTimes();

    return executionCommand;
  }

  private HostRoleCommand createMockHostRoleCommand() {
    HostRoleCommand hostRoleCommand = createMock(HostRoleCommand.class);

    expect(hostRoleCommand.getRequestId()).andReturn(1L).anyTimes();
    expect(hostRoleCommand.getStageId()).andReturn(2L).anyTimes();
    expect(hostRoleCommand.getTaskId()).andReturn(3L).anyTimes();

    return hostRoleCommand;
  }

  private Injector setup(String clusterName) throws AmbariException {
    final Map<String, Host> clusterHostMap = new HashMap<>();
    clusterHostMap.put("host1", createMock(Host.class));

    final ServiceComponentHost serviceComponentHost = createMock(ServiceComponentHost.class);
    expect(serviceComponentHost.getServiceName()).andReturn("SERVICE1").anyTimes();
    expect(serviceComponentHost.getServiceComponentName()).andReturn("COMPONENT1A").anyTimes();
    expect(serviceComponentHost.getHostName()).andReturn("host1").anyTimes();

    final List<ServiceComponentHost> serviceComponentHosts = new ArrayList<>();
    serviceComponentHosts.add(serviceComponentHost);

    final Cluster cluster = createMock(Cluster.class);
    expect(cluster.getClusterName()).andReturn(clusterName).anyTimes();
    expect(cluster.getServiceComponentHosts("host1")).andReturn(serviceComponentHosts).anyTimes();

    final Clusters clusters = createMock(Clusters.class);
    expect(clusters.getHostsForCluster(clusterName)).andReturn(clusterHostMap).anyTimes();
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();

    final TopologyUpdateEvent event = createNiceMock(TopologyUpdateEvent.class);
    expect(topologyHolder.getCurrentData()).andReturn(event).once();
    expect(topologyHolder.updateData(event)).andReturn(anyBoolean()).once();

    return Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(KerberosHelper.class).toInstance(createMock(KerberosHelper.class));
        bind(Clusters.class).toInstance(clusters);
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLogger.class));
        bind(EntityManager.class).toProvider(EasyMock.createNiceMock(Provider.class));
      }
    });
  }

}