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
package org.apache.ambari.server.serveraction.upgrades;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

/**
 * Tests upgrade-related server side actions
*/

public class KerberosKeytabsActionTest {

  private Injector m_injector;
  private Clusters m_clusters;
  private KerberosHelper m_kerberosHelper;
  private Config m_kerberosConfig;

  @Before
  public void setup() throws Exception {

    m_clusters = EasyMock.createMock(Clusters.class);
    UnitOfWork unitOfWork = EasyMock.createMock(UnitOfWork.class);

    Map<String, String> mockProperties = new HashMap<String, String>() {{
      put("kerberos-env", "");
    }};

    m_kerberosConfig = EasyMock.createNiceMock(Config.class);
    expect(m_kerberosConfig.getType()).andReturn("kerberos-env").anyTimes();
    expect(m_kerberosConfig.getProperties()).andReturn(mockProperties).anyTimes();

    Cluster cluster = EasyMock.createMock(Cluster.class);

    expect(cluster.getDesiredConfigByType("kerberos-env")).andReturn(m_kerberosConfig).atLeastOnce();
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();

    replay(m_clusters, cluster, m_kerberosConfig);

    m_injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        PartialNiceMockBinder.newBuilder().addClustersBinding().addLdapBindings().build().configure(binder());

        bind(Clusters.class).toInstance(m_clusters);
        bind(OsFamily.class).toInstance(EasyMock.createNiceMock(OsFamily.class));
        bind(UpgradeHelper.class).toInstance(EasyMock.createNiceMock(UpgradeHelper.class));
        bind(StackManagerFactory.class).toInstance(EasyMock.createNiceMock(StackManagerFactory.class));
        bind(StackDAO.class).toInstance(EasyMock.createNiceMock(StackDAO.class));
        bind(EntityManager.class).toInstance(EasyMock.createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(EasyMock.createNiceMock(DBAccessor.class));
        bind(AmbariMetaInfo.class).toInstance(EasyMock.createNiceMock(AmbariMetaInfo.class));
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
      }
    });

    m_kerberosHelper = m_injector.getInstance(KerberosHelper.class);
  }

  @Test
  public void testAction_NotKerberized() throws Exception {
    reset(m_kerberosHelper);
    expect(m_kerberosHelper.isClusterKerberosEnabled(EasyMock.anyObject(Cluster.class))).andReturn(Boolean.FALSE).atLeastOnce();
    replay(m_kerberosHelper);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    KerberosKeytabsAction action = m_injector.getInstance(KerberosKeytabsAction.class);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Assert.assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());
    Assert.assertTrue(StringUtils.contains(report.getStdOut(), "Cluster c1 is not secured by Kerberos"));
    Assert.assertTrue(StringUtils.contains(report.getStdOut(), "No action required."));
  }

  @Test
  public void testAction_NoKdcType() throws Exception {
    reset(m_kerberosHelper);
    expect(m_kerberosHelper.isClusterKerberosEnabled(EasyMock.anyObject(Cluster.class))).andReturn(Boolean.TRUE).atLeastOnce();
    replay(m_kerberosHelper);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    KerberosKeytabsAction action = m_injector.getInstance(KerberosKeytabsAction.class);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Assert.assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());
    Assert.assertTrue(StringUtils.contains(report.getStdOut(), "KDC Type is NONE"));
    Assert.assertTrue(StringUtils.contains(report.getStdOut(), "No action required."));
  }

  @Test
  public void testAction_Kerberized() throws Exception {
    reset(m_kerberosHelper);
    expect(m_kerberosHelper.isClusterKerberosEnabled(EasyMock.anyObject(Cluster.class))).andReturn(Boolean.TRUE).atLeastOnce();
    replay(m_kerberosHelper);
    m_kerberosConfig.getProperties().put("kdc_type", "mit-kdc");

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    KerberosKeytabsAction action = m_injector.getInstance(KerberosKeytabsAction.class);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Assert.assertEquals(HostRoleStatus.HOLDING.name(), report.getStatus());
    Assert.assertTrue(StringUtils.contains(report.getStdOut(), "Checking KDC type... MIT_KDC"));
    Assert.assertTrue(StringUtils.contains(report.getStdOut(), "Regenerate keytabs after upgrade is complete."));
  }
}
