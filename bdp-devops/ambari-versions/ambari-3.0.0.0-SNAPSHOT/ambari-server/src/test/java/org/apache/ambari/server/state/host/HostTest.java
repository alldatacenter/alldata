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

package org.apache.ambari.server.state.host;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HeartBeatHandler;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class HostTest {

  private Injector injector;
  private Clusters clusters;
  private HostDAO hostDAO;
  private OrmTestHelper helper;
  private static final Logger LOG = LoggerFactory.getLogger(HostTest.class);

  @Before
   public void setup() throws AmbariException{
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    hostDAO = injector.getInstance(HostDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testHostInfoImport() throws AmbariException{
    HostInfo info = new HostInfo();
    info.setMemorySize(100);
    info.setProcessorCount(10);
    info.setPhysicalProcessorCount(2);
    List<DiskInfo> mounts = new ArrayList<>();
    mounts.add(new DiskInfo("/dev/sda", "/mnt/disk1",
        "5000000", "4000000", "10%", "size", "fstype"));
    info.setMounts(mounts);

    info.setHostName("foo");
    info.setInterfaces("fip_4");
    info.setArchitecture("os_arch");
    info.setOS("os_type");
    info.setMemoryTotal(10);

    clusters.addHost("foo");
    Host host = clusters.getHost("foo");

    host.importHostInfo(info);

    Assert.assertEquals(info.getHostName(), host.getHostName());
    Assert.assertEquals(info.getFreeMemory(), host.getAvailableMemBytes());
    Assert.assertEquals(info.getMemoryTotal(), host.getTotalMemBytes());
    Assert.assertEquals(info.getProcessorCount(), host.getCpuCount());
    Assert.assertEquals(info.getPhysicalProcessorCount(), host.getPhCpuCount());
    Assert.assertEquals(info.getMounts().size(), host.getDisksInfo().size());
    Assert.assertEquals(info.getArchitecture(), host.getOsArch());
    Assert.assertEquals(info.getOS(), host.getOsType());
  }

  private void registerHost(Host host) throws Exception {
    registerHost(host, true);
  }

  @Test
  public void testHostOs() throws Exception {
    Clusters clusters = mock(Clusters.class);
    ActionManager manager = mock(ActionManager.class);
    Injector injector = mock(Injector.class);
    doNothing().when(injector).injectMembers(any());
    when(injector.getInstance(AmbariEventPublisher.class)).thenReturn(mock(AmbariEventPublisher.class));
    HeartBeatHandler handler = new HeartBeatHandler(clusters, manager, Encryptor.NONE, injector);
    String os = handler.getOsType("RedHat", "6.1");
    Assert.assertEquals("redhat6", os);
    os = handler.getOsType("RedHat", "6");
    Assert.assertEquals("redhat6", os);
    os = handler.getOsType("RedHat6","");
    Assert.assertEquals("redhat6", os);

  }

  private void registerHost(Host host, boolean firstReg) throws Exception {
    HostInfo info = new HostInfo();
    info.setMemorySize(100);
    info.setProcessorCount(10);
    List<DiskInfo> mounts = new ArrayList<>();
    mounts.add(new DiskInfo("/dev/sda", "/mnt/disk1",
        "5000000", "4000000", "10%", "size", "fstype"));
    info.setMounts(mounts);

    info.setHostName("foo");
    info.setInterfaces("fip_4");
    info.setArchitecture("os_arch");
    info.setOS("os_type");
    info.setMemoryTotal(10);

    AgentVersion agentVersion = null;
    long currentTime = System.currentTimeMillis();

    AgentEnv agentEnv = new AgentEnv();

    HostRegistrationRequestEvent e =
        new HostRegistrationRequestEvent("foo", agentVersion, currentTime,
            info, agentEnv, currentTime);

    if (!firstReg) {
      Assert.assertNotNull(host.getHostId());
    }

    host.handleEvent(e);
    Assert.assertEquals(currentTime, host.getLastRegistrationTime());

    Assert.assertNotNull(host.getLastAgentEnv());

    HostEntity entity = hostDAO.findByName(host.getHostName());
    Assert.assertEquals(currentTime,
        entity.getLastRegistrationTime().longValue());
    Assert.assertEquals("os_arch", entity.getOsArch());
    Assert.assertEquals("os_type", entity.getOsType());
    Assert.assertEquals(10, entity.getTotalMem().longValue());
  }

  private void ensureHostUpdatesReceived(Host host) throws Exception {
    HostStatusUpdatesReceivedEvent e =
        new HostStatusUpdatesReceivedEvent(host.getHostName(), 1);
    host.handleEvent(e);
  }

  private void verifyHostState(Host host, HostState state) {
    Assert.assertEquals(state, host.getState());
  }

  private void sendHealthyHeartbeat(Host host, long counter)
      throws Exception {
    HostHealthyHeartbeatEvent e = new HostHealthyHeartbeatEvent(
        host.getHostName(), counter, null, null);
    host.handleEvent(e);
  }

  private void sendUnhealthyHeartbeat(Host host, long counter)
      throws Exception {
    HostHealthStatus healthStatus = new HostHealthStatus(HealthStatus.UNHEALTHY,
        "Unhealthy server");
    HostUnhealthyHeartbeatEvent e = new HostUnhealthyHeartbeatEvent(
        host.getHostName(), counter, healthStatus);
    host.handleEvent(e);
  }

  private void timeoutHost(Host host) throws Exception {
    HostHeartbeatLostEvent e = new HostHeartbeatLostEvent(
        host.getHostName());
    host.handleEvent(e);
  }

  @Test
  public void testHostFSMInit() throws AmbariException{
    clusters.addHost("foo");
    Host host = clusters.getHost("foo");
    verifyHostState(host, HostState.INIT);
  }

  @Test
  public void testHostRegistrationFlow() throws Exception {
    clusters.addHost("foo");
    Host host = clusters.getHost("foo");
    registerHost(host);
    verifyHostState(host, HostState.WAITING_FOR_HOST_STATUS_UPDATES);

    boolean exceptionThrown = false;
    try {
      registerHost(host);
    } catch (Exception e) {
      // Expected
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("Expected invalid transition exception to be thrown");
    }

    ensureHostUpdatesReceived(host);
    verifyHostState(host, HostState.HEALTHY);

    exceptionThrown = false;
    try {
      ensureHostUpdatesReceived(host);
    } catch (Exception e) {
      // Expected
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("Expected invalid transition exception to be thrown");
    }
  }

  @Test
  public void testHostHeartbeatFlow() throws Exception {
    clusters.addHost("foo");
    Host host = clusters.getHost("foo");
    registerHost(host);
    ensureHostUpdatesReceived(host);

    // TODO need to verify audit logs generated
    // TODO need to verify health status updated properly

    long counter = 0;
    sendHealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.HEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());

    sendHealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.HEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.HEALTHY,
        host.getHealthStatus().getHealthStatus());

    sendUnhealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.UNHEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNHEALTHY,
        host.getHealthStatus().getHealthStatus());

    sendUnhealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.UNHEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNHEALTHY,
        host.getHealthStatus().getHealthStatus());

    sendHealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.HEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.HEALTHY,
        host.getHealthStatus().getHealthStatus());

    timeoutHost(host);
    verifyHostState(host, HostState.HEARTBEAT_LOST);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNKNOWN,
        host.getHealthStatus().getHealthStatus());

    timeoutHost(host);
    verifyHostState(host, HostState.HEARTBEAT_LOST);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNKNOWN,
        host.getHealthStatus().getHealthStatus());

    try {
      sendUnhealthyHeartbeat(host, ++counter);
      fail("Invalid event should have triggered an exception");
    } catch (Exception e) {
      // Expected
    }
    verifyHostState(host, HostState.HEARTBEAT_LOST);

    try {
      sendHealthyHeartbeat(host, ++counter);
      fail("Invalid event should have triggered an exception");
    } catch (Exception e) {
      // Expected
    }
    verifyHostState(host, HostState.HEARTBEAT_LOST);
  }

  @Test
  public void testHostRegistrationsInAnyState() throws Exception {
    clusters.addHost("foo");
    Host host = clusters.getHost("foo");
    host.setIPv4("ipv4");
    host.setIPv6("ipv6");

    long counter = 0;

    registerHost(host);

    ensureHostUpdatesReceived(host);
    registerHost(host, false);

    ensureHostUpdatesReceived(host);
    sendHealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.HEALTHY);
    registerHost(host, false);
    ensureHostUpdatesReceived(host);

    sendUnhealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.UNHEALTHY);
    registerHost(host, false);
    ensureHostUpdatesReceived(host);

    timeoutHost(host);
    verifyHostState(host, HostState.HEARTBEAT_LOST);
    registerHost(host, false);
    ensureHostUpdatesReceived(host);

    host.setState(HostState.INIT);
    registerHost(host, false);

  }

  @Test
  public void testHostDesiredConfig() throws Exception {
    AmbariMetaInfo metaInfo = injector.getInstance(AmbariMetaInfo.class);

    StackId stackId = new StackId("HDP-0.1");
    clusters.addCluster("c1", stackId);
    Cluster c1 = clusters.getCluster("c1");

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    Assert.assertEquals("c1", c1.getClusterName());
    clusters.addHost("h1");
    Host host = clusters.getHost("h1");
    host.setIPv4("ipv4");
    host.setIPv6("ipv6");

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    host.setHostAttributes(hostAttributes);

    c1.setDesiredStackVersion(stackId);
    clusters.mapHostToCluster("h1", "c1");

    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
    Config config = configFactory.createNew(c1, "global", "v1",
        new HashMap<String,String>() {{ put("a", "b"); put("x", "y"); }}, new HashMap<>());

    try {
      host.addDesiredConfig(c1.getClusterId(), true, null, config);
      Assert.fail("Expect failure when user is not specified.");
    }
    catch (Exception e) {
      // testing exception
    }


    host.addDesiredConfig(c1.getClusterId(), true, "_test", config);

    Map<String, DesiredConfig> map = host.getDesiredConfigs(c1.getClusterId());
    Assert.assertTrue("Expect desired config to contain global", map.containsKey("global"));

    config = configFactory.createNew(c1, "global", "v2",
        new HashMap<String,String>() {{ put("c", "d"); }}, new HashMap<>());
    host.addDesiredConfig(c1.getClusterId(), true, "_test1", config);

    map = host.getDesiredConfigs(c1.getClusterId());
    Assert.assertTrue("Expect desired config to contain global", map.containsKey("global"));
    Assert.assertEquals("Expect version to be 'v2'", "v2", map.get("global").getTag());

    host.addDesiredConfig(c1.getClusterId(), false, "_test2", config);
    map = host.getDesiredConfigs(c1.getClusterId());
    Assert.assertEquals("Expect no mapping configs", 0, map.size());

  }

  @Test
  public void testHostMaintenance() throws Exception {
    AmbariMetaInfo metaInfo = injector.getInstance(AmbariMetaInfo.class);

    StackId stackId = new StackId("HDP-0.1");
    clusters.addCluster("c1", stackId);
    Cluster c1 = clusters.getCluster("c1");
    Assert.assertEquals("c1", c1.getClusterName());
    clusters.addHost("h1");
    Host host = clusters.getHost("h1");
    host.setIPv4("ipv4");
    host.setIPv6("ipv6");

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    host.setHostAttributes(hostAttributes);

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    c1.setDesiredStackVersion(stackId);
    clusters.mapHostToCluster("h1", "c1");

    HostEntity entity = hostDAO.findByName("h1");
    HostStateEntity stateEntity = entity.getHostStateEntity();
    Assert.assertNull(stateEntity.getMaintenanceState());
    Assert.assertEquals(MaintenanceState.OFF, host.getMaintenanceState(c1.getClusterId()));

    host.setMaintenanceState(c1.getClusterId(), MaintenanceState.ON);

    entity = hostDAO.findByName("h1");
    stateEntity = entity.getHostStateEntity();
    Assert.assertNotNull(stateEntity.getMaintenanceState());
    Assert.assertEquals(MaintenanceState.ON, host.getMaintenanceState(c1.getClusterId()));
  }

  @Test
  public void testHostPersist() throws Exception {
    clusters.addHost("foo");
    Host host = clusters.getHost("foo");

    String rackInfo = "rackInfo";
    long lastRegistrationTime = System.currentTimeMillis();

    host.setRackInfo(rackInfo);
    host.setLastRegistrationTime(lastRegistrationTime);

    Assert.assertEquals(rackInfo, host.getRackInfo());
    Assert.assertEquals(lastRegistrationTime, host.getLastRegistrationTime());
  }
}
