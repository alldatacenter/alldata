/**
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

package org.apache.ambari.msi;

import org.apache.ambari.scom.ClusterDefinitionProvider;
import org.apache.ambari.scom.HostInfoProvider;
import org.apache.ambari.scom.TestClusterDefinitionProvider;
import org.apache.ambari.scom.TestHostInfoProvider;
import org.junit.Assert;
import org.junit.Test;
import java.util.Set;

import static org.easymock.EasyMock.*;

/**
 */
public class ClusterDefinitionTest {
  @Test
  public void testGetServices() throws Exception {

    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());

    Set<String> services = clusterDefinition.getServices();

    Assert.assertTrue(services.contains("HDFS"));
    Assert.assertTrue(services.contains("FLUME"));
    Assert.assertTrue(services.contains("OOZIE"));
    Assert.assertTrue(services.contains("MAPREDUCE"));
    Assert.assertTrue(services.contains("HBASE"));
    Assert.assertTrue(services.contains("ZOOKEEPER"));
    Assert.assertTrue(services.contains("HIVE"));
  }

  @Test
  public void testGetHosts() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());

    Set<String> hosts = clusterDefinition.getHosts();

    Assert.assertTrue(hosts.contains("NAMENODE_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("SECONDARY_NAMENODE_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("FLUME_SERVICE1.acme.com"));
    Assert.assertTrue(hosts.contains("FLUME_SERVICE2.acme.com"));
    Assert.assertTrue(hosts.contains("FLUME_SERVICE3.acme.com"));
    Assert.assertTrue(hosts.contains("HBASE_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("HIVE_SERVER_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("JOBTRACKER_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("OOZIE_SERVER_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("slave1.acme.com"));
    Assert.assertTrue(hosts.contains("slave2.acme.com"));
    Assert.assertTrue(hosts.contains("slave3.acme.com"));
    Assert.assertTrue(hosts.contains("WEBHCAT_MASTER.acme.com"));
  }

  @Test
  public void testGetComponents() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());

    Set<String> components = clusterDefinition.getComponents("HDFS");
    Assert.assertTrue(components.contains("NAMENODE"));
    Assert.assertTrue(components.contains("SECONDARY_NAMENODE"));
    Assert.assertTrue(components.contains("DATANODE"));

    components = clusterDefinition.getComponents("MAPREDUCE");
    Assert.assertTrue(components.contains("JOBTRACKER"));
    Assert.assertTrue(components.contains("TASKTRACKER"));

    components = clusterDefinition.getComponents("FLUME");
    Assert.assertTrue(components.contains("FLUME_SERVER"));

    components = clusterDefinition.getComponents("OOZIE");
    Assert.assertTrue(components.contains("OOZIE_SERVER"));

    components = clusterDefinition.getComponents("HBASE");
    Assert.assertTrue(components.contains("HBASE_MASTER"));
    Assert.assertTrue(components.contains("HBASE_REGIONSERVER"));

    components = clusterDefinition.getComponents("ZOOKEEPER");
    Assert.assertTrue(components.contains("ZOOKEEPER_SERVER"));

    components = clusterDefinition.getComponents("HIVE");
    Assert.assertTrue(components.contains("HIVE_SERVER"));

    clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider("clusterproperties_HDP2_HA.txt", "myCluster", "HDP-2.0.6"), new TestHostInfoProvider());
    components = clusterDefinition.getComponents("HDFS");
    Assert.assertTrue(components.contains("NAMENODE"));
    Assert.assertTrue(components.contains("SECONDARY_NAMENODE"));
    Assert.assertTrue(components.contains("DATANODE"));
    Assert.assertTrue(components.contains("ZKFC"));
    Assert.assertTrue(components.contains("JOURNALNODE"));

    clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider("clusterproperties_HDP21_HA.txt", "myCluster", "HDP-2.1.2"), new TestHostInfoProvider());
    components = clusterDefinition.getComponents("HDFS");
    Assert.assertTrue(components.contains("NAMENODE"));
    Assert.assertTrue(components.contains("ZKFC"));
    Assert.assertTrue(components.contains("JOURNALNODE"));

    components = clusterDefinition.getComponents("YARN");
    Assert.assertTrue(components.contains("RESOURCEMANAGER"));
  }

  @Test
  public void testGetHostComponents() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());

    Set<String> hostComponents = clusterDefinition.getHostComponents("HDFS", "NAMENODE_MASTER.acme.com");

    Assert.assertTrue(hostComponents.contains("NAMENODE"));

    hostComponents = clusterDefinition.getHostComponents("HDFS", "slave1.acme.com");

    Assert.assertTrue(hostComponents.contains("DATANODE"));

    hostComponents = clusterDefinition.getHostComponents("HDFS", "slave2.acme.com");

    Assert.assertTrue(hostComponents.contains("DATANODE"));

    clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider("clusterproperties_HDP2_HA.txt", "myCluster", "HDP-2.0.6"), new TestHostInfoProvider());
    hostComponents = clusterDefinition.getHostComponents("HDFS", "WINHDP-1");
    Assert.assertTrue(hostComponents.contains("NAMENODE"));
    Assert.assertTrue(hostComponents.contains("JOURNALNODE"));
    Assert.assertTrue(hostComponents.contains("ZKFC"));
    Assert.assertFalse(hostComponents.contains("DATANODE"));
    Assert.assertFalse(hostComponents.contains("SECONDARY_NAMENODE"));

    hostComponents = clusterDefinition.getHostComponents("HDFS", "WINHDP-2");
    Assert.assertTrue(hostComponents.contains("NAMENODE"));
    Assert.assertTrue(hostComponents.contains("JOURNALNODE"));
    Assert.assertTrue(hostComponents.contains("ZKFC"));
    Assert.assertTrue(hostComponents.contains("DATANODE"));
    Assert.assertTrue(hostComponents.contains("SECONDARY_NAMENODE"));

    clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider("clusterproperties_HDP21_HA.txt", "myCluster", "HDP-2.1.2"), new TestHostInfoProvider());
    hostComponents = clusterDefinition.getHostComponents("YARN", "WINHDP-1");
    Assert.assertTrue(hostComponents.contains("RESOURCEMANAGER"));

    hostComponents = clusterDefinition.getHostComponents("HDFS", "WINHDP-1");
    Assert.assertTrue(hostComponents.contains("NAMENODE"));
    Assert.assertTrue(hostComponents.contains("JOURNALNODE"));

    hostComponents = clusterDefinition.getHostComponents("YARN", "WINHDP-2");
    Assert.assertTrue(hostComponents.contains("RESOURCEMANAGER"));

    hostComponents = clusterDefinition.getHostComponents("HDFS", "WINHDP-2");
    Assert.assertTrue(hostComponents.contains("NAMENODE"));
    Assert.assertTrue(hostComponents.contains("JOURNALNODE"));
  }

  @Test
  public void testGetHostState() throws Exception {
    TestStateProvider stateProvider = new TestStateProvider();
    TestClusterDefinitionProvider definitionProvider = new TestClusterDefinitionProvider();
    TestHostInfoProvider hostInfoProvider = new TestHostInfoProvider();

    ClusterDefinition clusterDefinition = new ClusterDefinition(stateProvider, definitionProvider, hostInfoProvider);
    Assert.assertEquals("HEALTHY", clusterDefinition.getHostState("NAMENODE_MASTER.acme.com"));

    stateProvider.setState(StateProvider.State.Stopped);
    Assert.assertEquals("UNHEALTHY", clusterDefinition.getHostState("NAMENODE_MASTER.acme.com"));

    stateProvider.setState(StateProvider.State.Paused);
    Assert.assertEquals("UNHEALTHY", clusterDefinition.getHostState("NAMENODE_MASTER.acme.com"));

    stateProvider.setState(StateProvider.State.Unknown);
    Assert.assertEquals("UNHEALTHY", clusterDefinition.getHostState("NAMENODE_MASTER.acme.com"));
  }

  @Test
  public void testSetServiceState_IfStateAlreadySetToDesired() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider();

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Running).times(5);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertEquals(-1, clusterDefinition.setServiceState("HDFS", "STARTED"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testSetServiceState_IfStateUnknown() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider();

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertEquals(-1, clusterDefinition.setServiceState("HDFS", "UNKNOWN"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testSetServiceState_FromInstalledToStarted() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider();

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);
    //checking if a service state already set
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Stopped);
    //checking if a component state not set yet
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Stopped);
    expect(mockStateProvider.setRunningState(anyObject(String.class), anyObject(String.class), eq(StateProvider.State.Running))).andReturn(null);
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Stopped);
    expect(mockStateProvider.setRunningState(anyObject(String.class), anyObject(String.class), eq(StateProvider.State.Running))).andReturn(null);
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Stopped);
    expect(mockStateProvider.setRunningState(anyObject(String.class), anyObject(String.class), eq(StateProvider.State.Running))).andReturn(null);
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Stopped);
    expect(mockStateProvider.setRunningState(anyObject(String.class), anyObject(String.class), eq(StateProvider.State.Running))).andReturn(null);
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Stopped);
    expect(mockStateProvider.setRunningState(anyObject(String.class), anyObject(String.class), eq(StateProvider.State.Running))).andReturn(null);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertEquals(1, clusterDefinition.setServiceState("HDFS", "STARTED"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testSetServiceStateFromInstalledToStartedWhenOneOfTheComponentsAlreadyStarted() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider();

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);
    //checking if a service state already set
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Stopped);
    //checking if a component state not set yet
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Running).times(4);
    expect(mockStateProvider.getRunningState(anyObject(String.class), anyObject(String.class))).andReturn(StateProvider.State.Stopped);
    expect(mockStateProvider.setRunningState(anyObject(String.class), anyObject(String.class), eq(StateProvider.State.Running))).andReturn(null);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertEquals(1, clusterDefinition.setServiceState("HDFS", "STARTED"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testSetHostComponentState_IfStateUnknown() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider();

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertEquals(-1, clusterDefinition.setHostComponentState("hostName", "DATANODE", "UNKNOWN"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testSetHostComponentState_IfStateAlreadySetToDesired() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider();

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);

    expect(mockStateProvider.getRunningState(isA(String.class), isA(String.class))).andReturn(StateProvider.State.Running);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertEquals(-1, clusterDefinition.setHostComponentState("hostName", "DATANODE", "STARTED"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testSetHostComponentState_FromInstalledToStarted() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider();

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);

    expect(mockStateProvider.getRunningState(isA(String.class), isA(String.class))).andReturn(StateProvider.State.Stopped);
    expect(mockStateProvider.setRunningState(anyObject(String.class), anyObject(String.class), eq(StateProvider.State.Running))).andReturn(null);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertEquals(1, clusterDefinition.setHostComponentState("hostName", "DATANODE", "STARTED"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testHDP2ServicesAndComponents() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider("clusterproperties_HDP2.txt", "myCluster", "HDP-2.0.6");

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertFalse(clusterDefinition.getServices().contains("MAPREDUCE"));
    Assert.assertTrue(clusterDefinition.getServices().contains("PIG"));
    Assert.assertTrue(clusterDefinition.getServices().contains("SQOOP"));
    Assert.assertTrue(clusterDefinition.getServices().contains("YARN"));
    Assert.assertTrue(clusterDefinition.getServices().contains("MAPREDUCE2"));
    Assert.assertTrue(clusterDefinition.getComponents("MAPREDUCE2").contains("MAPREDUCE2_CLIENT"));
    Assert.assertTrue(clusterDefinition.getComponents("YARN").contains("NODEMANAGER"));
    Assert.assertTrue(clusterDefinition.getComponents("YARN").contains("RESOURCEMANAGER"));
    Assert.assertTrue(clusterDefinition.getComponents("YARN").contains("YARN_CLIENT"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testSetServiceState_IfServiceIsClientOnly() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider();

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertEquals(-1, clusterDefinition.setServiceState("PIG", "STARTED"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testSetHostComponentState_IfHostComponentIsClientOnly() {
    StateProvider mockStateProvider = createStrictMock(StateProvider.class);
    ClusterDefinitionProvider mockClusterDefinitionProvider = createStrictMock(ClusterDefinitionProvider.class);
    HostInfoProvider mockHostInfoProvider = createStrictMock(HostInfoProvider.class);

    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider();

    expect(mockClusterDefinitionProvider.getClusterName()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getVersionId()).andDelegateTo(testClusterDefinitionProvider);
    expect(mockClusterDefinitionProvider.getInputStream()).andDelegateTo(testClusterDefinitionProvider);

    replay(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);

    ClusterDefinition clusterDefinition = new ClusterDefinition(mockStateProvider, mockClusterDefinitionProvider, mockHostInfoProvider);
    Assert.assertEquals(-1, clusterDefinition.setHostComponentState("hostName", "SQOOP", "STARTED"));

    verify(mockClusterDefinitionProvider, mockHostInfoProvider, mockStateProvider);
  }

  @Test
  public void testGetMajorStackVersion() {
    TestClusterDefinitionProvider testClusterDefinitionProvider = new TestClusterDefinitionProvider("clusterproperties_HDP2.txt", "myCluster", "HDP-2.0.6");
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), testClusterDefinitionProvider, new TestHostInfoProvider());

    Integer majorVersion = clusterDefinition.getMajorStackVersion();
    Integer minorVersion = clusterDefinition.getMinorStackVersion();

    Assert.assertTrue(2 == majorVersion);
    Assert.assertTrue(0 == minorVersion);
  }
}
