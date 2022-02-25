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
package org.apache.ambari.server.controller.utilities;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.reset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.ServiceRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.serveraction.kerberos.Component;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class KerberosIdentityCleanerTest extends EasyMockSupport {
  @Rule public EasyMockRule mocks = new EasyMockRule(this);
  private static final String HOST = "c6401";
  private static final String HOST2 = "c6402";
  private static final String OOZIE = "OOZIE";
  private static final String OOZIE_SERVER = "OOZIE_SERVER";
  private static final String OOZIE_2 = "OOZIE2";
  private static final String OOZIE_SERVER_2 = "OOZIE_SERVER2";
  private static final String YARN_2 = "YARN2";
  private static final String RESOURCE_MANAGER_2 = "RESOURCE_MANAGER2";
  private static final String YARN = "YARN";
  private static final String RESOURCE_MANAGER = "RESOURCE_MANAGER";
  private static final String HDFS = "HDFS";
  private static final String NAMENODE = "NAMENODE";
  private static final String DATANODE = "DATANODE";
  private static final long CLUSTER_ID = 1;
  @Mock private KerberosHelper kerberosHelper;
  @Mock private Clusters clusters;
  @Mock private Cluster cluster;
  private Map<String, Service> installedServices = new HashMap<>();
  private KerberosDescriptorFactory kerberosDescriptorFactory = new KerberosDescriptorFactory();
  private KerberosIdentityCleaner kerberosIdentityCleaner;
  private KerberosDescriptor kerberosDescriptor;

  @Test
  public void removesAllKerberosIdentitesOfComponentAfterComponentWasUninstalled() throws Exception {
    installComponent(OOZIE, OOZIE_SERVER, HOST);
    kerberosHelper.deleteIdentities(cluster, singletonList(new Component(HOST, OOZIE, OOZIE_SERVER, -1l)), null);
    expectLastCall().once();
    replayAll();
    uninstallComponent(OOZIE, OOZIE_SERVER, HOST);
    verifyAll();
  }

  @Test
  public void skipsRemovingIdentityThatIsSharedByPrincipalName() throws Exception {
    installComponent(OOZIE, OOZIE_SERVER, HOST);
    installComponent(OOZIE_2, OOZIE_SERVER_2, HOST);
    kerberosHelper.deleteIdentities(cluster, singletonList(new Component(HOST, OOZIE, OOZIE_SERVER, -1l)), null);
    expectLastCall().once();
    replayAll();
    uninstallComponent(OOZIE, OOZIE_SERVER, HOST);
    verifyAll();
  }

  @Test
  public void skipsRemovingIdentityThatIsSharedByKeyTabFilePath() throws Exception {
    installComponent(YARN, RESOURCE_MANAGER, HOST);
    installComponent(YARN_2, RESOURCE_MANAGER_2, HOST);
    kerberosHelper.deleteIdentities(cluster, singletonList(new Component(HOST, YARN, RESOURCE_MANAGER, -1l)), null);
    expectLastCall().once();
    replayAll();
    uninstallComponent(YARN, RESOURCE_MANAGER, HOST);
    verifyAll();
  }

  @Test
  public void skipsRemovingIdentityWhenClusterIsNotKerberized() throws Exception {
    reset(cluster);
    expect(cluster.getSecurityType()).andReturn(SecurityType.NONE).anyTimes();

    replayAll();
    uninstallComponent(OOZIE, OOZIE_SERVER, HOST);
    verifyAll();
  }

  @Test
  public void removesServiceIdentitiesSkipComponentIdentitiesAfterServiceWasUninstalled() throws Exception {
    installComponent(OOZIE, OOZIE_SERVER, HOST);
    kerberosHelper.deleteIdentities(cluster, hdfsComponents(), null);
    expectLastCall().once();
    replayAll();
    uninstallService(HDFS, hdfsComponents());
    verifyAll();
  }

  /**
   * Ensures that when an upgrade is in progress, new requests are not created
   * to remove identities since it would interfere with the long running
   * upgrade.
   *
   * @throws Exception
   */
  @Test
  public void skipsRemovingIdentityWhenClusterIsUpgrading() throws Exception {
    installComponent(OOZIE, OOZIE_SERVER, HOST);
    reset(cluster);
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).once();
    expect(cluster.getUpgradeInProgress()).andReturn(createNiceMock(UpgradeEntity.class)).once();

    replayAll();
    uninstallComponent(OOZIE, OOZIE_SERVER, HOST);
    verifyAll();
  }

  private ArrayList<Component> hdfsComponents() {
    return newArrayList(new Component(HOST, HDFS, NAMENODE, 0l), new Component(HOST, HDFS, DATANODE, 0l));
  }

  private void installComponent(String serviceName, String componentName, String... hostNames) {
    Service service = createMock(serviceName + "_" + componentName, Service.class);
    ServiceComponent component = createMock(componentName, ServiceComponent.class);
    expect(component.getName()).andReturn(componentName).anyTimes();
    Map<String, ServiceComponentHost> hosts = new HashMap<>();
    expect(component.getServiceComponentHosts()).andReturn(hosts).anyTimes();
    for (String hostName : hostNames) {
      ServiceComponentHost host = createMock(hostName, ServiceComponentHost.class);
      expect(host.getHostName()).andReturn(hostName).anyTimes();
      hosts.put(hostName, host);
    }
    installedServices.put(serviceName, service);
    expect(service.getName()).andReturn(serviceName).anyTimes();
    expect(service.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>() {{
      put(componentName, component);
    }}).anyTimes();
  }

  private void uninstallComponent(String service, String component, String host) throws KerberosMissingAdminCredentialsException {
    kerberosIdentityCleaner.componentRemoved(new ServiceComponentUninstalledEvent(CLUSTER_ID, "any", "any", service, component, host, false, false, -1l));
  }

  private void uninstallService(String service, List<Component> components) throws KerberosMissingAdminCredentialsException {
    kerberosIdentityCleaner.serviceRemoved(new ServiceRemovedEvent(CLUSTER_ID, "any", "any", service, components));
  }

  @Before
  public void setUp() throws Exception {
    kerberosIdentityCleaner = new KerberosIdentityCleaner(new AmbariEventPublisher(), kerberosHelper, clusters);
    kerberosDescriptor = kerberosDescriptorFactory.createInstance("{" +
      "  'services': [" +
      "    {" +
      "      'name': 'OOZIE'," +
      "      'components': [" +
      "        {" +
      "          'name': 'OOZIE_SERVER'," +
      "          'identities': [" +
      "            {" +
      "              'name': '/HDFS/NAMENODE/hdfs'" +
      "            }," +
      "            {" +
      "              'name': 'oozie_server1'," +
      "              'principal': { 'value': 'oozie1/_HOST@EXAMPLE.COM' }" +
      "            }," +"" +
      "            {" +
      "              'name': 'oozie_server2'," +
      "              'principal': { 'value': 'oozie/_HOST@EXAMPLE.COM' }" +
      "            }" +
      "          ]" +
      "        }" +
      "      ]" +
      "    }," +
      "    {" +
      "      'name': 'OOZIE2'," +
      "      'components': [" +
      "        {" +
      "          'name': 'OOZIE_SERVER2'," +
      "          'identities': [" +
      "            {" +
      "              'name': 'oozie_server3'," +
      "              'principal': { 'value': 'oozie/_HOST@EXAMPLE.COM' }" +
      "            }" +"" +
      "          ]" +
      "        }" +
      "      ]" +
      "    }," +
      "    {" +
      "      'name': 'YARN'," +
      "      'components': [" +
      "        {" +
      "          'name': 'RESOURCE_MANAGER'," +
      "          'identities': [" +
      "            {" +
      "              'name': 'rm_unique'" +
      "            }," +
      "            {" +
      "              'name': 'rm1-shared'," +
      "              'keytab' : { 'file' : 'shared' }" +
      "            }" +
      "          ]" +
      "        }" +
      "      ]" +
      "    }," +
      "    {" +
      "      'name': 'YARN2'," +
      "      'components': [" +
      "        {" +
      "          'name': 'RESOURCE_MANAGER2'," +
      "          'identities': [" +
      "            {" +
      "              'name': 'rm2-shared'," +
      "              'keytab' : { 'file' : 'shared' }" +
      "            }" +
      "          ]" +
      "        }" +
      "      ]" +
      "    }," +
      "    {" +
      "      'name': 'HDFS'," +
      "      'identities': [" +
      "            {" +
      "              'name': 'hdfs-service'" +
      "            }," +
      "            {" +
      "              'name': 'shared'," +
      "              'principal': { 'value': 'oozie/_HOST@EXAMPLE.COM' }" +
      "            }," +
      "            {" +
      "              'name': '/YARN/RESOURCE_MANAGER/rm'" +
      "            }," +
      "          ]," +
      "      'components': [" +
      "        {" +
      "          'name': 'NAMENODE'," +
      "          'identities': [" +
      "            {" +
      "              'name': 'namenode'" +
      "            }" +
      "          ]" +
      "        }," +
      "        {" +
      "          'name': 'DATANODE'," +
      "          'identities': [" +
      "            {" +
      "              'name': 'datanode'" +
      "            }" +
      "          ]" +
      "        }" +
      "      ]" +
      "    }" +
      "  ]" +
      "}");
    expect(clusters.getCluster(CLUSTER_ID)).andReturn(cluster).anyTimes();
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(kerberosHelper.getKerberosDescriptor(cluster, false)).andReturn(kerberosDescriptor).anyTimes();
    expect(cluster.getServices()).andReturn(installedServices).anyTimes();
    expect(cluster.getUpgradeInProgress()).andReturn(null).once();
  }
}