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


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.stageplanner.RoleGraphNode;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class RoleGraphTest {

  private Injector injector;
  private RoleCommandOrderProvider roleCommandOrderProvider;
  private RoleGraphFactory roleGraphFactory;
  private HostRoleCommandFactory hrcFactory;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    roleCommandOrderProvider = injector.getInstance(RoleCommandOrderProvider.class);
    roleGraphFactory = injector.getInstance(RoleGraphFactory.class);
    hrcFactory = injector.getInstance(HostRoleCommandFactory.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testValidateOrder() throws AmbariException {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));
    when(cluster.getClusterId()).thenReturn(1L);

    Service hdfsService = mock(Service.class);
    when(hdfsService.getDesiredStackId()).thenReturn(new StackId("HDP-2.0.6"));
    when (cluster.getServices()).thenReturn(ImmutableMap.<String, Service>builder()
        .put("HDFS", hdfsService)
        .build());

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);

    RoleGraphNode datanode_upgrade = new RoleGraphNode(Role.DATANODE, RoleCommand.UPGRADE);
    RoleGraphNode hdfs_client_upgrade = new RoleGraphNode(Role.HDFS_CLIENT, RoleCommand.UPGRADE);
    Assert.assertEquals(-1, rco.order(datanode_upgrade, hdfs_client_upgrade));
    Assert.assertEquals(1, rco.order(hdfs_client_upgrade, datanode_upgrade));

    RoleGraphNode namenode_upgrade = new RoleGraphNode(Role.NAMENODE, RoleCommand.UPGRADE);
    RoleGraphNode ganglia_server_upgrade = new RoleGraphNode(Role.GANGLIA_SERVER, RoleCommand.UPGRADE);
    Assert.assertEquals(1, rco.order(ganglia_server_upgrade, hdfs_client_upgrade));
    Assert.assertEquals(1, rco.order(ganglia_server_upgrade, datanode_upgrade));
    Assert.assertEquals(-1, rco.order(namenode_upgrade, ganglia_server_upgrade));

    RoleGraphNode datanode_start = new RoleGraphNode(Role.DATANODE, RoleCommand.START);
    RoleGraphNode datanode_install = new RoleGraphNode(Role.DATANODE, RoleCommand.INSTALL);
    RoleGraphNode jobtracker_start = new RoleGraphNode(Role.JOBTRACKER, RoleCommand.START);
    Assert.assertEquals(1, rco.order(datanode_start, datanode_install));
    Assert.assertEquals(1, rco.order(jobtracker_start, datanode_start));
    Assert.assertEquals(0, rco.order(jobtracker_start, jobtracker_start));


    RoleGraphNode pig_service_check = new RoleGraphNode(Role.PIG_SERVICE_CHECK, RoleCommand.SERVICE_CHECK);
    RoleGraphNode resourcemanager_start = new RoleGraphNode(Role.RESOURCEMANAGER, RoleCommand.START);
    Assert.assertEquals(-1, rco.order(resourcemanager_start, pig_service_check));

    RoleGraphNode hdfs_service_check = new RoleGraphNode(Role.HDFS_SERVICE_CHECK, RoleCommand.SERVICE_CHECK);
    RoleGraphNode snamenode_start = new RoleGraphNode(Role.SECONDARY_NAMENODE, RoleCommand.START);
    Assert.assertEquals(-1, rco.order(snamenode_start, hdfs_service_check));

    RoleGraphNode mapred2_service_check = new RoleGraphNode(Role.MAPREDUCE2_SERVICE_CHECK, RoleCommand.SERVICE_CHECK);
    RoleGraphNode rm_start = new RoleGraphNode(Role.RESOURCEMANAGER, RoleCommand.START);
    RoleGraphNode nm_start = new RoleGraphNode(Role.NODEMANAGER, RoleCommand.START);
    RoleGraphNode hs_start = new RoleGraphNode(Role.HISTORYSERVER, RoleCommand.START);

    Assert.assertEquals(-1, rco.order(rm_start, mapred2_service_check));
    Assert.assertEquals(-1, rco.order(nm_start, mapred2_service_check));
    Assert.assertEquals(-1, rco.order(hs_start, mapred2_service_check));
    Assert.assertEquals(-1, rco.order(hs_start, mapred2_service_check));
    Assert.assertEquals(1, rco.order(nm_start, rm_start));

    //Non-HA mode
    RoleGraphNode nn_start = new RoleGraphNode(Role.NAMENODE, RoleCommand.START);
    RoleGraphNode jn_start = new RoleGraphNode(Role.JOURNALNODE, RoleCommand.START);
    RoleGraphNode zk_server_start = new RoleGraphNode(Role.ZOOKEEPER_SERVER, RoleCommand.START);
    RoleGraphNode hbase_master_start = new RoleGraphNode(Role.HBASE_MASTER, RoleCommand.START);
    RoleGraphNode hive_srv_start = new RoleGraphNode(Role.HIVE_SERVER, RoleCommand.START);
    RoleGraphNode hive_ms_start = new RoleGraphNode(Role.HIVE_METASTORE, RoleCommand.START);
    RoleGraphNode mysql_start = new RoleGraphNode(Role.MYSQL_SERVER, RoleCommand.START);
    RoleGraphNode oozie_srv_start = new RoleGraphNode(Role.OOZIE_SERVER, RoleCommand.START);
    RoleGraphNode webhcat_srv_start = new RoleGraphNode(Role.WEBHCAT_SERVER, RoleCommand.START);
    RoleGraphNode flume_start = new RoleGraphNode(Role.FLUME_HANDLER, RoleCommand.START);
    RoleGraphNode zkfc_start = new RoleGraphNode(Role.ZKFC, RoleCommand.START);

    Assert.assertEquals(0, rco.order(nn_start, jn_start));
    Assert.assertEquals(0, rco.order(nn_start, zk_server_start));
    Assert.assertEquals(0, rco.order(zkfc_start, nn_start));

    Assert.assertEquals(1, rco.order(flume_start, oozie_srv_start));
    Assert.assertEquals(1, rco.order(hbase_master_start, zk_server_start));
    Assert.assertEquals(1, rco.order(hive_srv_start, mysql_start));
    Assert.assertEquals(1, rco.order(hive_ms_start, mysql_start));
    Assert.assertEquals(1, rco.order(webhcat_srv_start, datanode_start));

    // Enable HA for cluster
    Service hdfsServiceMock = mock(Service.class);
    ServiceComponent jnComponentMock = mock(ServiceComponent.class);
    when(cluster.getService("HDFS")).thenReturn(hdfsServiceMock);
    when(hdfsServiceMock.getServiceComponent("JOURNALNODE")).thenReturn(jnComponentMock);

    rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    Assert.assertEquals(1, rco.order(nn_start, jn_start));
    Assert.assertEquals(1, rco.order(nn_start, zk_server_start));
    Assert.assertEquals(1, rco.order(zkfc_start, nn_start));
  }

  /**
   * Tests the ordering of
   * {@link RoleGraph#getOrderedHostRoleCommands(java.util.Map)}.
   *
   * @throws AmbariException
   */
  @Test
  public void testGetOrderedHostRoleCommands() throws AmbariException {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));
    when(cluster.getClusterId()).thenReturn(1L);

    Service hdfsService = mock(Service.class);
    when(hdfsService.getDesiredStackId()).thenReturn(new StackId("HDP-2.0.6"));

    Service zkService = mock(Service.class);
    when(zkService.getDesiredStackId()).thenReturn(new StackId("HDP-2.0.6"));

    Service hbaseService = mock(Service.class);
    when(hbaseService.getDesiredStackId()).thenReturn(new StackId("HDP-2.0.6"));

    when(cluster.getServices()).thenReturn(ImmutableMap.<String, Service>builder()
        .put("HDFS", hdfsService)
        .put("ZOOKEEPER", zkService)
        .put("HBASE", hbaseService)
        .build());


    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph roleGraph = roleGraphFactory.createNew(rco);

    Map<String, Map<String, HostRoleCommand>> unorderedCommands = new HashMap<>();
    Map<String, HostRoleCommand> c6401Commands = new HashMap<>();
    Map<String, HostRoleCommand> c6402Commands = new HashMap<>();
    Map<String, HostRoleCommand> c6403Commands = new HashMap<>();

    HostRoleCommand hrcNameNode = hrcFactory.create("c6041", Role.NAMENODE, null, RoleCommand.START);
    HostRoleCommand hrcZooKeeperHost1 = hrcFactory.create("c6041", Role.ZOOKEEPER_SERVER, null, RoleCommand.START);
    HostRoleCommand hrcHBaseMaster = hrcFactory.create("c6042", Role.HBASE_MASTER, null, RoleCommand.START);
    HostRoleCommand hrcZooKeeperHost3 = hrcFactory.create("c6043", Role.ZOOKEEPER_SERVER, null, RoleCommand.START);

    c6401Commands.put(hrcNameNode.getRole().name(), hrcNameNode);
    c6401Commands.put(hrcZooKeeperHost1.getRole().name(), hrcZooKeeperHost1);
    c6402Commands.put(hrcHBaseMaster.getRole().name(), hrcHBaseMaster);
    c6403Commands.put(hrcZooKeeperHost3.getRole().name(), hrcZooKeeperHost3);

    unorderedCommands.put("c6401", c6401Commands);
    unorderedCommands.put("c6402", c6402Commands);
    unorderedCommands.put("c6403", c6403Commands);

    List<Map<String, List<HostRoleCommand>>> stages = roleGraph.getOrderedHostRoleCommands(unorderedCommands);

    Assert.assertEquals(2, stages.size());

    Map<String, List<HostRoleCommand>> stage1 = stages.get(0);
    Map<String, List<HostRoleCommand>> stage2 = stages.get(1);

    Assert.assertEquals(2, stage1.size());
    Assert.assertEquals(1, stage2.size());

    List<HostRoleCommand> stage1CommandsHost1 = stage1.get("c6401");
    List<HostRoleCommand> stage1CommandsHost3 = stage1.get("c6403");
    List<HostRoleCommand> stage2CommandsHost2 = stage2.get("c6402");

    Assert.assertEquals(3, stage1CommandsHost1.size() + stage1CommandsHost3.size());
    Assert.assertEquals(1, stage2CommandsHost2.size());

    List<Role> stage1Roles = Lists.newArrayList(stage1CommandsHost1.get(0).getRole(),
        stage1CommandsHost1.get(1).getRole(), stage1CommandsHost3.get(0).getRole());

    Assert.assertTrue(stage1Roles.contains(Role.NAMENODE));
    Assert.assertTrue(stage1Roles.contains(Role.ZOOKEEPER_SERVER));
    Assert.assertEquals(Role.ZOOKEEPER_SERVER, stage1CommandsHost3.get(0).getRole());
    Assert.assertEquals(Role.HBASE_MASTER, stage2CommandsHost2.get(0).getRole());
  }
}
