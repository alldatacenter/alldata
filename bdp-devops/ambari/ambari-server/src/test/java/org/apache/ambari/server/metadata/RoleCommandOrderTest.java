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

package org.apache.ambari.server.metadata;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.utils.CollectionPresentationUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class RoleCommandOrderTest {

  private Injector injector;
  private RoleCommandOrderProvider roleCommandOrderProvider;

  private final static String TEST_RCO_DATA_FILE = "test_rco_data.json";

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    roleCommandOrderProvider = injector.getInstance(RoleCommandOrderProvider.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }


  /**
   * Tests building dependencies in GLUSTERFS cluster. Uses real dependency mapping
   * file (role_command_order.json)
   */
  @Test
  public void testInitializeAtGLUSTERFSCluster() throws AmbariException {

    StackId stackId = new StackId("HDP", "2.0.6");
    ClusterImpl cluster = createMock(ClusterImpl.class);
    Service service = createMock(Service.class);
    expect(service.getDesiredStackId()).andReturn(stackId);
    expect(cluster.getClusterId()).andReturn(1L);
    expect(cluster.getService("GLUSTERFS")).andReturn(service);
    expect(cluster.getService("HDFS")).andReturn(null);
    expect(cluster.getService("YARN")).andReturn(null);

    expect(cluster.getServices()).andReturn(ImmutableMap.<String, Service>builder()
        .put("GLUSTERFS", service)
        .build()).atLeastOnce();

    replay(cluster, service);

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);

    Map<RoleCommandPair, Set<RoleCommandPair>> deps = rco.getDependencies();
    assertTrue("Dependencies are loaded after initialization", deps.size() > 0);
    verify(cluster, service);
	// Check that HDFS components are not present in dependencies
    // Checking blocked roles
    assertFalse(dependenciesContainBlockedRole(deps, Role.DATANODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.NAMENODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.SECONDARY_NAMENODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.JOURNALNODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.NAMENODE_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockedRole(deps, Role.HDFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockedRole(deps, Role.HDFS_CLIENT));
    // Checking blocker roles
    assertFalse(dependenciesContainBlockerRole(deps, Role.SECONDARY_NAMENODE));
    assertFalse(dependenciesContainBlockerRole(deps, Role.JOURNALNODE));
    assertFalse(dependenciesContainBlockerRole(deps, Role.NAMENODE_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockerRole(deps, Role.HDFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockerRole(deps, Role.HDFS_CLIENT));

    // And that some GLUSTERFS components are present (section has been loaded)
    assertTrue(dependenciesContainBlockerRole(deps, Role.PEERSTATUS));

  }


  /**
   * Tests building dependencies in not HA-enabled HDFS cluster. Uses real
   * dependency mapping file (role_command_order.json)
   */
  @Test
  public void testInitializeAtHDFSCluster() throws AmbariException {

    ClusterImpl cluster = createMock(ClusterImpl.class);
    expect(cluster.getService("GLUSTERFS")).andReturn(null);
    expect(cluster.getClusterId()).andReturn(1L);
    Service hdfsService = createMock(Service.class);

    expect(cluster.getService("HDFS")).andReturn(hdfsService).atLeastOnce();
    expect(cluster.getService("YARN")).andReturn(null).atLeastOnce();
    expect(hdfsService.getServiceComponent("JOURNALNODE")).andReturn(null);
    expect(hdfsService.getDesiredStackId()).andReturn(new StackId("HDP", "2.0.6"));
//    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.6"));
    expect(cluster.getServices()).andReturn(ImmutableMap.<String, Service>builder()
        .put("HDFS", hdfsService)
        .build()).anyTimes();

    replay(cluster, hdfsService);

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    Map<RoleCommandPair, Set<RoleCommandPair>> deps = rco.getDependencies();
    assertTrue("Dependencies are loaded after initialization", deps.size() > 0);
    verify(cluster);
    verify(hdfsService);
    // Check that GLUSTERFS components are not present in dependencies
    // Checking blocked roles
    assertFalse(dependenciesContainBlockedRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockedRole(deps, Role.GLUSTERFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockedRole(deps, Role.GLUSTERFS_CLIENT));
    // Checking blocker roles
    assertFalse(dependenciesContainBlockerRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockerRole(deps, Role.GLUSTERFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockerRole(deps, Role.GLUSTERFS_CLIENT));

    // And that some HDFS components are present (section has been loaded)
    assertTrue(dependenciesContainBlockerRole(deps, Role.DATANODE));
    // Check that there is no HA NN dependencies present
    assertFalse(dependenciesContainBlockerRole(deps, Role.JOURNALNODE));
    assertFalse(dependenciesContainBlockedRole(deps, Role.ZKFC));
  }


  /**
   * Tests building dependencies in HA-enabled HDFS cluster. Uses real
   * dependency mapping file (role_command_order.json)
   */
  @Test
  public void testInitializeAtHaHDFSCluster() throws AmbariException {

    ClusterImpl cluster = createMock(ClusterImpl.class);
    expect(cluster.getService("GLUSTERFS")).andReturn(null);
    expect(cluster.getClusterId()).andReturn(1L);
    Service hdfsService = createMock(Service.class);
    ServiceComponent journalnodeSC = createMock(ServiceComponent.class);

    expect(cluster.getService("HDFS")).andReturn(hdfsService).atLeastOnce();
    expect(cluster.getService("YARN")).andReturn(null);
    expect(hdfsService.getServiceComponent("JOURNALNODE")).andReturn(journalnodeSC);
    expect(hdfsService.getDesiredStackId()).andReturn(new StackId("HDP", "2.0.6"));
//    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.6"));
    expect(cluster.getServices()).andReturn(ImmutableMap.<String, Service>builder()
        .put("HDFS", hdfsService)
        .build()).anyTimes();

    replay(cluster, hdfsService);

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    Map<RoleCommandPair, Set<RoleCommandPair>> deps = rco.getDependencies();
    assertTrue("Dependencies are loaded after initialization", deps.size() > 0);
    verify(cluster);
    verify(hdfsService);
    // Check that GLUSTERFS components are not present in dependencies
    // Checking blocked roles
    assertFalse(dependenciesContainBlockedRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockedRole(deps, Role.GLUSTERFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockedRole(deps, Role.GLUSTERFS_CLIENT));
    // Checking blocker roles
    assertFalse(dependenciesContainBlockerRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockerRole(deps, Role.GLUSTERFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockerRole(deps, Role.GLUSTERFS_CLIENT));

    // And that some HDFS components are present (section has been loaded)
    assertTrue(dependenciesContainBlockerRole(deps, Role.DATANODE));
    // Check that some HA NN dependencies are present
    assertTrue(dependenciesContainBlockerRole(deps, Role.JOURNALNODE));
    assertTrue(dependenciesContainBlockedRole(deps, Role.ZKFC));
  }

  @Test
  public void testInitializeAtHaRMCluster() throws AmbariException {

    ClusterImpl cluster = createMock(ClusterImpl.class);
    ServiceComponentHost sch1 = createMock(ServiceComponentHostImpl.class);
    ServiceComponentHost sch2 = createMock(ServiceComponentHostImpl.class);
    expect(cluster.getService("GLUSTERFS")).andReturn(null);
    expect(cluster.getClusterId()).andReturn(1L);

    Map<String, ServiceComponentHost> hostComponents = new HashMap<>();
    hostComponents.put("1",sch1);
    hostComponents.put("2",sch2);

    Service yarnService = createMock(Service.class);
    ServiceComponent resourcemanagerSC = createMock(ServiceComponent.class);

    expect(cluster.getService("YARN")).andReturn(yarnService).atLeastOnce();
    expect(cluster.getService("HDFS")).andReturn(null);
    expect(yarnService.getServiceComponent("RESOURCEMANAGER")).andReturn(resourcemanagerSC).anyTimes();
    expect(resourcemanagerSC.getServiceComponentHosts()).andReturn(hostComponents).anyTimes();
//    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.6"));
    expect(yarnService.getDesiredStackId()).andReturn(new StackId("HDP", "2.0.6"));
    expect(cluster.getServices()).andReturn(ImmutableMap.<String, Service>builder()
        .put("YARN", yarnService)
        .build()).anyTimes();

    replay(cluster, yarnService, sch1, sch2, resourcemanagerSC);

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    Map<RoleCommandPair, Set<RoleCommandPair>> deps = rco.getDependencies();
    assertTrue("Dependencies are loaded after initialization", deps.size() > 0);
    verify(cluster, yarnService);
    // Check that GLUSTERFS components are not present in dependencies
    // Checking blocked roles
    assertFalse(dependenciesContainBlockedRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockedRole(deps, Role.GLUSTERFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockedRole(deps, Role.GLUSTERFS_CLIENT));
    // Checking blocker roles
    assertFalse(dependenciesContainBlockerRole(deps, Role.PEERSTATUS));
    assertFalse(dependenciesContainBlockerRole(deps, Role.GLUSTERFS_SERVICE_CHECK));
    assertFalse(dependenciesContainBlockerRole(deps, Role.GLUSTERFS_CLIENT));

    // And that some HDFS components are present (section has been loaded)
    assertTrue(dependenciesContainBlockerRole(deps, Role.DATANODE));
    // Check that some HA RM dependencies are present
    RoleCommandPair rmPair = new RoleCommandPair(Role.RESOURCEMANAGER, RoleCommand.START);
    Set<RoleCommandPair> rmRoleCommandPairs = deps.get(rmPair);
    assertNotNull(rmRoleCommandPairs);
    boolean isZookeeperStartPresent = false;
    for (RoleCommandPair pair : rmRoleCommandPairs) {
      if (pair.getCmd() == RoleCommand.START && pair.getRole() == Role.ZOOKEEPER_SERVER) {
        isZookeeperStartPresent = true;
      }
    }
    assertTrue(isZookeeperStartPresent);
  }

  @Test
  public void testMissingRestartDependenciesAdded() throws Exception {
    ClusterImpl cluster = createMock(ClusterImpl.class);
    ServiceComponentHost sch1 = createMock(ServiceComponentHostImpl.class);
    ServiceComponentHost sch2 = createMock(ServiceComponentHostImpl.class);
    expect(cluster.getService("GLUSTERFS")).andReturn(null);
    expect(cluster.getClusterId()).andReturn(1L);

    Map<String, ServiceComponentHost> hostComponents = new HashMap<>();
    hostComponents.put("1", sch1);
    hostComponents.put("2", sch2);

    Service yarnService = createMock(Service.class);
    ServiceComponent resourcemanagerSC = createMock(ServiceComponent.class);

    expect(cluster.getService("YARN")).andReturn(yarnService).atLeastOnce();
    expect(cluster.getService("HDFS")).andReturn(null);
    expect(yarnService.getServiceComponent("RESOURCEMANAGER")).andReturn(resourcemanagerSC).anyTimes();
    expect(yarnService.getDesiredStackId()).andReturn(new StackId("HDP", "2.0.6")).anyTimes();
    expect(resourcemanagerSC.getServiceComponentHosts()).andReturn(hostComponents).anyTimes();
//    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.6"));
    expect(cluster.getServices()).andReturn(ImmutableMap.<String, Service>builder()
        .put("YARN", yarnService)
        .build()).anyTimes();

    replay(cluster, yarnService, sch1, sch2, resourcemanagerSC);

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);

    verify(cluster, yarnService);

    Map<RoleCommandPair, Set<RoleCommandPair>> deps = rco.getDependencies();
    assertNotNull(deps);
    Map<RoleCommandPair, Set<RoleCommandPair>> startRCOs = new HashMap<>();
    Map<RoleCommandPair, Set<RoleCommandPair>> restartRCOs = new HashMap<>();

    for (Map.Entry<RoleCommandPair, Set<RoleCommandPair>> dep : deps.entrySet()) {
      if (dep.getKey().getCmd().equals(RoleCommand.START)) {
        startRCOs.put(dep.getKey(), dep.getValue());
      }
      if (dep.getKey().getCmd().equals(RoleCommand.RESTART)) {
        restartRCOs.put(dep.getKey(), dep.getValue());
      }
    }

    assertFalse(startRCOs.isEmpty());
    assertFalse(restartRCOs.isEmpty());
    assertEquals(startRCOs.size(), restartRCOs.size());
    // Verify one
    Map.Entry<RoleCommandPair, Set<RoleCommandPair>> entry = restartRCOs.entrySet().iterator().next();
    assertEquals(RoleCommand.RESTART, entry.getKey().getCmd());
    for (RoleCommandPair pair : entry.getValue()) {
      assertEquals(RoleCommand.RESTART, pair.getCmd());
    }
    // Verify all
    for (Map.Entry<RoleCommandPair, Set<RoleCommandPair>> startEntry : startRCOs.entrySet()) {
      assertTrue(restartRCOs.containsKey(
        new RoleCommandPair(startEntry.getKey().getRole(), RoleCommand.RESTART)));
    }
  }


  @Test
  public void testAddDependencies() throws IOException {
    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);

    InputStream testJsonIS = getClass().getClassLoader().
            getResourceAsStream(TEST_RCO_DATA_FILE);

    ObjectMapper mapper = new ObjectMapper();
    Map<String,Object> testData = mapper.readValue(testJsonIS,
        new TypeReference<Map<String,Object>>() {});

    rco.addDependencies(testData);

    mapper.setVisibility(JsonMethod.ALL, JsonAutoDetect.Visibility.ANY);
    String dump = mapper.writeValueAsString(rco.getDependencies());

    // Depends on hashing, string representation can be different
    // We need a sophisticated comparison
    List<String> parts = Arrays.asList(dump.substring(1, 522).split(Pattern.quote("],")));
    assertEquals(3, parts.size());
    assertTrue(parts.contains("\"RoleCommandPair{role=SECONDARY_NAMENODE, cmd=UPGRADE}\":[{\"role\":{\"name\":\"NAMENODE\"},\"cmd\":\"UPGRADE\"}"));
    assertTrue(parts.contains("\"RoleCommandPair{role=SECONDARY_NAMENODE, cmd=START}\":[{\"role\":{\"name\":\"NAMENODE\"},\"cmd\":\"START\"}"));
    boolean datanodeCommandExists = false;
    for (String part : parts) {
      if (part.contains("RoleCommandPair{role=DATANODE, cmd=STOP}")) {
        datanodeCommandExists = true;
        String[] parts2 = part.split(Pattern.quote(":["));
        assertEquals(2, parts2.length);
        assertEquals("\"RoleCommandPair{role=DATANODE, cmd=STOP}\"", parts2[0]);
        List<String> components = Arrays.asList(new String[]{"\"role\":{\"name\":\"HBASE_MASTER\"},\"cmd\":\"STOP\"",
                                                             "\"role\":{\"name\":\"RESOURCEMANAGER\"},\"cmd\":\"STOP\"",
                                                             "\"role\":{\"name\":\"TASKTRACKER\"},\"cmd\":\"STOP\"",
                                                             "\"role\":{\"name\":\"NODEMANAGER\"},\"cmd\":\"STOP\"",
                                                             "\"role\":{\"name\":\"HISTORYSERVER\"},\"cmd\":\"STOP\"",
                                                             "\"role\":{\"name\":\"JOBTRACKER\"},\"cmd\":\"STOP\""});
        Assert.assertTrue(CollectionPresentationUtils.isStringPermutationOfCollection(parts2[1], components, "},{", 1, 1));
      }
    }
    assertTrue(datanodeCommandExists);
  }


  @Test
  public void testInitializeDefault() throws IOException {

    ClusterImpl cluster = createMock(ClusterImpl.class);
    expect(cluster.getService("GLUSTERFS")).andReturn(null);
    expect(cluster.getClusterId()).andReturn(1L);
    Service hdfsService = createMock(Service.class);

    expect(cluster.getService("HDFS")).andReturn(hdfsService).atLeastOnce();
    expect(cluster.getService("YARN")).andReturn(null);
    expect(hdfsService.getServiceComponent("JOURNALNODE")).andReturn(null);
    //There is no rco file in this stack, should use default
//    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.5"));
    expect(hdfsService.getDesiredStackId()).andReturn(new StackId("HDP", "2.0.5"));
    expect(cluster.getServices()).andReturn(ImmutableMap.<String, Service>builder()
        .put("HDFS", hdfsService)
        .build()).anyTimes();

    replay(cluster);
    replay(hdfsService);

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);

    verify(cluster);
    verify(hdfsService);
  }

  @Test
  public void testTransitiveServices() throws AmbariException {
    ClusterImpl cluster = createMock(ClusterImpl.class);

    Service hdfsService = createMock(Service.class);

    ServiceComponent namenode = createMock(ServiceComponent.class);
    expect(namenode.getName()).andReturn("NAMENODE").anyTimes();

    Map<String,ServiceComponent> hdfsComponents = Collections.singletonMap("NAMENODE", namenode);
    expect(hdfsService.getServiceComponents()).andReturn(hdfsComponents).anyTimes();

    Service hbaseService = createMock(Service.class);
    expect(cluster.getService("HBASE")).andReturn(hbaseService).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L);
    expect(hbaseService.getCluster()).andReturn(cluster).anyTimes();

    ServiceComponent hbaseMaster = createMock(ServiceComponent.class);
    expect(hbaseMaster.getName()).andReturn("HBASE_MASTER").anyTimes();

    Map<String, ServiceComponent> hbaseComponents = Collections.singletonMap(
        "HBASE_MASTER", hbaseMaster);
    expect(hbaseService.getServiceComponents()).andReturn(hbaseComponents).anyTimes();

    Map<String, Service> installedServices = new HashMap<>();
    installedServices.put("HDFS", hdfsService);
    installedServices.put("HBASE", hbaseService);
    expect(cluster.getServices()).andReturn(installedServices).atLeastOnce();

    expect(cluster.getService("HDFS")).andReturn(hdfsService).atLeastOnce();
    expect(cluster.getService("GLUSTERFS")).andReturn(null);
    expect(cluster.getService("YARN")).andReturn(null);
    expect(hdfsService.getServiceComponent("JOURNALNODE")).andReturn(null);
//    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.0.5"));
    expect(hdfsService.getDesiredStackId()).andReturn(new StackId("HDP", "2.0.5"));
    expect(hbaseService.getDesiredStackId()).andReturn(new StackId("HDP", "2.0.5"));

    //replay
    replay(cluster, hdfsService, hbaseService, hbaseMaster, namenode);

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);

    Set<Service> transitiveServices = rco.getTransitiveServices(
        cluster.getService("HBASE"), RoleCommand.START);

    // HDFS should be started before HBASE start
    Assert.assertNotNull(transitiveServices);
    Assert.assertFalse(transitiveServices.isEmpty());
    Assert.assertEquals(1, transitiveServices.size());
    Assert.assertTrue(transitiveServices.contains(hdfsService));
  }

  /**
   * Tests the ability to override any previous mapping by using the
   * {@code -OVERRIDE} placeholder. For example:
   *
   * <pre>
   * "host_ordered_upgrade" : {
   *   "DATANODE-START-OVERRIDE" : ["NAMENODE-START"],
   *   "NODEMANAGER-START-OVERRIDE": ["RESOURCEMANAGER-START"],
   *   "RESOURCEMANAGER-START-OVERRIDE": ["NAMENODE-START"]
   * }
   * </pre>
   *
   * @throws IOException
   */
  @Test
  public void testOverride() throws Exception {
    ClusterImpl cluster = createMock(ClusterImpl.class);
    expect(cluster.getService("GLUSTERFS")).andReturn(null).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).atLeastOnce();
    Service hdfsService = createMock(Service.class);

    expect(cluster.getService("HDFS")).andReturn(hdfsService).atLeastOnce();
    expect(cluster.getService("YARN")).andReturn(null).atLeastOnce();
    expect(hdfsService.getServiceComponent("JOURNALNODE")).andReturn(null);
    expect(hdfsService.getDesiredStackId()).andReturn(new StackId("HDP", "2.2.0")).anyTimes();
    expect(cluster.getServices()).andReturn(ImmutableMap.<String, Service>builder()
        .put("HDFS", hdfsService)
        .build()).anyTimes();

    // There is no rco file in this stack, should use default
//    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.2.0")).atLeastOnce();

    replay(cluster, hdfsService);

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);

    // create command pairs
    RoleCommandPair startDN = new RoleCommandPair(Role.DATANODE, RoleCommand.START);
    RoleCommandPair startNM = new RoleCommandPair(Role.NODEMANAGER, RoleCommand.START);
    RoleCommandPair startNN = new RoleCommandPair(Role.NAMENODE, RoleCommand.START);
    RoleCommandPair startRM = new RoleCommandPair(Role.RESOURCEMANAGER, RoleCommand.START);

    Set<RoleCommandPair> startDNDeps = rco.getDependencies().get(startDN);
    Set<RoleCommandPair> startNMDeps = rco.getDependencies().get(startNM);

    Assert.assertNull(startDNDeps);
    Assert.assertTrue(startNMDeps.contains(startDN));

    rco = (RoleCommandOrder) rco.clone();
    LinkedHashSet<String> keys = rco.getSectionKeys();
    keys.add("host_ordered_upgrade");
    rco.initialize(cluster, keys);

    startDNDeps = rco.getDependencies().get(startDN);
    startNMDeps = rco.getDependencies().get(startNM);

    // now ensure that the role orders have been modified correctly
    Assert.assertTrue(startDNDeps.contains(startNN));
    Assert.assertTrue(startNMDeps.contains(startRM));
    Assert.assertFalse(startNMDeps.contains(startDN));
    Assert.assertEquals(2, startNMDeps.size());

    verify(cluster);
    verify(hdfsService);
  }

  private boolean dependenciesContainBlockedRole(Map<RoleCommandPair,
          Set<RoleCommandPair>> deps, Role blocked) {
    for (RoleCommandPair blockedPair : deps.keySet()) {
      if (blockedPair.getRole() == blocked) {
        return true;
      }
    }
    return false;
  }

  private boolean dependenciesContainBlockerRole(Map<RoleCommandPair,
          Set<RoleCommandPair>> deps, Role blocker) {
    for(Set<RoleCommandPair> blockerSet: deps.values()) {
      for (RoleCommandPair roleCommandPair : blockerSet) {
        if (roleCommandPair.getRole() == blocker) {
          return true;
        }
      }
    }
    return false;
  }

}
