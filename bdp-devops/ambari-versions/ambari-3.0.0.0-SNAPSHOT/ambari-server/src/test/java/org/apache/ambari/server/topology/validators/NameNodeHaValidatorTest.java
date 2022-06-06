/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology.validators;


import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.ClusterTopologyImpl;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@RunWith(EasyMockRunner.class)
public class NameNodeHaValidatorTest {

  Set<String> hosts = new HashSet<>();
  Set<String> nameNodes = new HashSet<>();
  Map<String, HostGroupInfo> hostGroupInfos = new HashMap<>();
  Map<String, String> hdfsSite = new HashMap<>();
  Map<String, String> hadoopEnv = new HashMap<>();
  Map<String, Map<String, String>> fullProperties = ImmutableMap.of("hdfs-site", hdfsSite, "hadoop-env", hadoopEnv);
  NameNodeHaValidator validator = new NameNodeHaValidator();

  @Mock
  Configuration clusterConfig;

  @Mock
  ClusterTopology clusterTopology;

  @Before
  public void setUp() {
    expect(clusterTopology.getConfiguration()).andReturn(clusterConfig).anyTimes();
    expect(clusterTopology.getAllHosts()).andReturn(hosts).anyTimes();
    expect(clusterTopology.getHostAssignmentsForComponent("NAMENODE")).andReturn(nameNodes).anyTimes();
    expect(clusterTopology.isNameNodeHAEnabled())
      .andAnswer(() -> ClusterTopologyImpl.isNameNodeHAEnabled(fullProperties))
      .anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(hostGroupInfos).anyTimes();
    expect(clusterConfig.getFullProperties()).andReturn(fullProperties).anyTimes();
    replay(clusterTopology, clusterConfig);
    createDefaultHdfsSite();
    hosts.addAll(ImmutableSet.of("internalhost1", "internalhost2"));
  }

  private void createDefaultHdfsSite() {
    hdfsSite.put("dfs.nameservices", "demo1, demo2");
    hdfsSite.put("dfs.ha.namenodes.demo1", "nn1, nn2");
    hdfsSite.put("dfs.ha.namenodes.demo2", "nn1, nn2");
  }

  private void addExternalNameNodesToHdfsSite() {
    hdfsSite.put("dfs.namenode.rpc-address.demo1.nn1", "demohost1:8020");
    hdfsSite.put("dfs.namenode.rpc-address.demo1.nn2", "demohost2:8020");
    hdfsSite.put("dfs.namenode.rpc-address.demo2.nn1", "demohost3:8020");
    hdfsSite.put("dfs.namenode.rpc-address.demo2.nn2", "demohost4:8020");
  }

  @Test
  public void nonHA() throws Exception {
     hdfsSite.clear();
     validator.validate(clusterTopology);
  }

  @Test
  public void externalNameNodes_properConfig1() throws Exception {
    addExternalNameNodesToHdfsSite();
    validator.validate(clusterTopology);
  }

  @Test
  public void externalNameNodes_properConfig2() throws Exception {
    addExternalNameNodesToHdfsSite();
    hdfsSite.remove("dfs.nameservices");
    hdfsSite.put("dfs.internal.nameservices", "demo1, demo2");
    validator.validate(clusterTopology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void externalNameNodes_missingNameNodes() throws Exception {
    addExternalNameNodesToHdfsSite();
    hdfsSite.remove("dfs.ha.namenodes.demo2");
    validator.validate(clusterTopology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void externalNameNodes_missingRpcAddress() throws Exception {
    addExternalNameNodesToHdfsSite();
    hdfsSite.remove("dfs.namenode.rpc-address.demo2.nn1");
    validator.validate(clusterTopology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void externalNameNodes_localhost() throws Exception {
    addExternalNameNodesToHdfsSite();
    hdfsSite.put("dfs.namenode.rpc-address.demo2.nn1", "localhost:8020");
    validator.validate(clusterTopology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void externalNameNodes_hostGroupToken() throws Exception {
    addExternalNameNodesToHdfsSite();
    hdfsSite.put("dfs.namenode.rpc-address.demo2.nn1", "%HOSTGROUP::group1%:8020");
    validator.validate(clusterTopology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void externalNameNodes_missingPort() throws Exception {
    addExternalNameNodesToHdfsSite();
    hdfsSite.put("dfs.namenode.rpc-address.demo2.nn1", "demohost3");
    validator.validate(clusterTopology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void externalNameNodes_internalHost() throws Exception {
    addExternalNameNodesToHdfsSite();
    hdfsSite.put("dfs.namenode.rpc-address.demo2.nn1", "internalhost1:8020");
    validator.validate(clusterTopology);
  }

  @Test
  public void validationSkippedDueToMissingHostInformation() throws Exception {
    // this is an invalid HA topology as there is only one namenode
    nameNodes.add(hosts.iterator().next());

    // adding a host group without hosts prohibits validation
    HostGroupInfo groupInfo = new HostGroupInfo("group1");
    hostGroupInfos.put(groupInfo.getHostGroupName(), groupInfo);

    // yet this call should not throw exception as validation should be skipped
    validator.validate(clusterTopology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void notEnoughNameNodesForHa() throws Exception {
    nameNodes.add(hosts.iterator().next());
    validator.validate(clusterTopology);
  }

  @Test
  public void haNoInitialSetup() throws Exception {
    List<String> nameNodeHosts = ImmutableList.copyOf(hosts).subList(0, 2);
    nameNodes.addAll(nameNodeHosts);
    validator.validate(clusterTopology);
  }

  @Test
  public void haProperInitialSetup() throws Exception {
    List<String> nameNodeHosts = ImmutableList.copyOf(hosts).subList(0, 2);
    nameNodes.addAll(nameNodeHosts);

    hadoopEnv.put("dfs_ha_initial_namenode_active", nameNodeHosts.get(1));

    validator.validate(clusterTopology);
  }

  @Test
  public void haProperInitialSetupWithHostGroups() throws Exception {
    List<String> nameNodeHosts = ImmutableList.copyOf(hosts).subList(0, 2);
    nameNodes.addAll(nameNodeHosts);

    hadoopEnv.put("dfs_ha_initial_namenode_active", "%HOSTGROUP::group1%");
    hadoopEnv.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP::group2%");

    validator.validate(clusterTopology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void haInvalidInitialActive() throws Exception {
    List<String> nameNodeHosts = ImmutableList.copyOf(hosts).subList(0, 2);
    nameNodes.addAll(nameNodeHosts);

    hadoopEnv.put("dfs_ha_initial_namenode_active", "externalhost");
    hadoopEnv.put("dfs_ha_initial_namenode_standby", nameNodeHosts.get(0));

    validator.validate(clusterTopology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void haInvalidInitialStandby() throws Exception {
    List<String> nameNodeHosts = ImmutableList.copyOf(hosts).subList(0, 2);
    nameNodes.addAll(nameNodeHosts);

    hadoopEnv.put("dfs_ha_initial_namenode_active", nameNodeHosts.get(0));
    hadoopEnv.put("dfs_ha_initial_namenode_standby", "externalhost");

    validator.validate(clusterTopology);
  }

}