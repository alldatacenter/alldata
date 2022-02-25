/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.topology;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.mockStatic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.orm.entities.TopologyHostGroupEntity;
import org.apache.ambari.server.orm.entities.TopologyHostInfoEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;


@RunWith(PowerMockRunner.class)
@PrepareForTest(AmbariServer.class)
public class LogicalRequestTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private TopologyRequest replayedTopologyRequest;

  @Mock
  private ClusterTopology clusterTopology;

  @Mock
  private TopologyLogicalRequestEntity logicalRequestEntity;

  @Mock
  private AmbariManagementController controller;

  @Mock
  private AmbariContext ambariContext;

  private final long clusterId = 2L;
  private final String clusterName = "myCluster";

  @Mock
  private Clusters clusters;

  @Mock
  private Cluster cluster;

  @Mock
  private Blueprint blueprint;

  @Mock
  private HostGroup hostGroup1;

  @Mock
  private HostGroup hostGroup2;


  @Before
  public void setup() throws Exception {
    resetAll();

    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(clusterId)).andReturn(cluster).anyTimes();
    expect(cluster.getClusterName()).andReturn(clusterName).anyTimes();

    String topologyReqestDescription = "Provision cluster";

    expect(replayedTopologyRequest.getDescription()).andReturn(topologyReqestDescription).anyTimes();
    expect(clusterTopology.getAmbariContext()).andReturn(ambariContext).anyTimes();
    expect(clusterTopology.getClusterId()).andReturn(clusterId).anyTimes();
    expect(clusterTopology.getProvisionAction()).andReturn(ProvisionAction.INSTALL_ONLY).anyTimes();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(blueprint.getName()).andReturn("blueprintDef").anyTimes();
    expect(blueprint.shouldSkipFailure()).andReturn(true).anyTimes();

    PowerMock.reset(AmbariServer.class);

    mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(controller).anyTimes();

    PowerMock.replay(AmbariServer.class);



  }

  @Test
  public void testPersistedRequestsWithReservedHosts() throws Exception {
    // Given
    Long requestId = 1L;

    TopologyHostInfoEntity host1 = new TopologyHostInfoEntity();
    host1.setId(100L);
    host1.setFqdn("host1");

    TopologyHostInfoEntity host2 = new TopologyHostInfoEntity();
    host2.setId(101L);
    host2.setFqdn("host2");

    TopologyHostInfoEntity host3 = new TopologyHostInfoEntity();
    host3.setId(103L);
    host3.setFqdn("host3");

    TopologyHostInfoEntity host4 = new TopologyHostInfoEntity();
    host4.setId(104L);
    host4.setFqdn("host4");


    TopologyHostGroupEntity hostGroupEntity1 = new TopologyHostGroupEntity();
    hostGroupEntity1.setTopologyHostInfoEntities(ImmutableSet.of(host1, host2, host3, host4));
    hostGroupEntity1.setName("host_group_1");

    // host request matched to a registered host
    TopologyHostRequestEntity hostRequestEntityHost1Matched = new TopologyHostRequestEntity();
    hostRequestEntityHost1Matched.setId(1L);
    hostRequestEntityHost1Matched.setHostName(host1.getFqdn()); //host request matched host1
    hostRequestEntityHost1Matched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost1Matched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host1.getFqdn()))).andReturn(true).anyTimes();


    TopologyHostRequestEntity hostRequestEntityHost2Matched = new TopologyHostRequestEntity();
    hostRequestEntityHost2Matched.setId(2L);
    hostRequestEntityHost2Matched.setHostName(host2.getFqdn()); // host request matched host2
    hostRequestEntityHost2Matched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost2Matched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host2.getFqdn()))).andReturn(true).anyTimes();

    // host request that hasn't been matched to any registered host yet
    TopologyHostRequestEntity hostRequestEntityHost3NotMatched = new TopologyHostRequestEntity();
    hostRequestEntityHost3NotMatched.setId(3L);
    hostRequestEntityHost3NotMatched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost3NotMatched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host3.getFqdn()))).andReturn(false).anyTimes();

    TopologyHostRequestEntity hostRequestEntityHost4NotMatched = new TopologyHostRequestEntity();
    hostRequestEntityHost4NotMatched.setId(4L);
    hostRequestEntityHost4NotMatched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost4NotMatched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host4.getFqdn()))).andReturn(false).anyTimes();

    Collection<TopologyHostRequestEntity> reservedHostRequestEntities = ImmutableSet.of(
      hostRequestEntityHost1Matched,
      hostRequestEntityHost2Matched,
      hostRequestEntityHost3NotMatched,
      hostRequestEntityHost4NotMatched);

    hostGroupEntity1.setTopologyHostRequestEntities(reservedHostRequestEntities);

    TopologyRequestEntity topologyRequestEntity = new TopologyRequestEntity();
    topologyRequestEntity.setTopologyHostGroupEntities(Collections.singleton(hostGroupEntity1));

    expect(logicalRequestEntity.getTopologyHostRequestEntities()).andReturn(reservedHostRequestEntities).atLeastOnce();


    expect(logicalRequestEntity.getTopologyRequestEntity()).andReturn(topologyRequestEntity).atLeastOnce();
    expect(blueprint.getHostGroup(eq("host_group_1"))).andReturn(hostGroup1).atLeastOnce();
    expect(hostGroup1.containsMasterComponent()).andReturn(false).atLeastOnce();

    replayAll();


    // When

    LogicalRequest req = new LogicalRequest(requestId, replayedTopologyRequest, clusterTopology, logicalRequestEntity);

    // Then
    verifyAll();

    // reserved hosts names are the ones that have no
    // matching hosts among the registered hosts
    Collection<String> actualReservedHosts = req.getReservedHosts();

    Collection<String> expectedReservedHosts = ImmutableSet.of(host3.getFqdn(), host4.getFqdn());

    assertEquals(expectedReservedHosts, actualReservedHosts);

    Collection<HostRequest> actualCompletedHostReqs = req.getCompletedHostRequests();

    assertEquals(2, actualCompletedHostReqs.size());

    Optional<HostRequest> completedHostReq1 = Iterables.tryFind(actualCompletedHostReqs, new Predicate<HostRequest>() {
      @Override
      public boolean apply(@Nullable HostRequest input) {
        return "host1".equals(input.getHostName());
      }
    });

    Optional<HostRequest> completedHostReq2 = Iterables.tryFind(actualCompletedHostReqs, new Predicate<HostRequest>() {
      @Override
      public boolean apply(@Nullable HostRequest input) {
        return "host2".equals(input.getHostName());
      }
    });

    assertTrue(completedHostReq1.isPresent() && completedHostReq2.isPresent());

  }



  @Test
  public void testPersistedRequestsWithHostPredicate() throws Exception {
    // Given
    Long requestId = 1L;

    TopologyHostInfoEntity host = new TopologyHostInfoEntity();
    host.setId(800L);
    host.setPredicate("Hosts/host_name.in(host[1-4])");


    TopologyHostGroupEntity hostGroupEntity2 = new TopologyHostGroupEntity();
    hostGroupEntity2.setTopologyHostInfoEntities(ImmutableSet.of(host));
    hostGroupEntity2.setName("host_group_2");

    // host request matched to a registered host
    TopologyHostRequestEntity hostRequestEntityHost1Matched = new TopologyHostRequestEntity();
    hostRequestEntityHost1Matched.setId(1L);
    hostRequestEntityHost1Matched.setHostName("host1"); //host request matched host1
    hostRequestEntityHost1Matched.setTopologyHostGroupEntity(hostGroupEntity2);
    hostRequestEntityHost1Matched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq("host1"))).andReturn(true).anyTimes();


    TopologyHostRequestEntity hostRequestEntityHost2Matched = new TopologyHostRequestEntity();
    hostRequestEntityHost2Matched.setId(2L);
    hostRequestEntityHost2Matched.setHostName("host2"); // host request matched host2
    hostRequestEntityHost2Matched.setTopologyHostGroupEntity(hostGroupEntity2);
    hostRequestEntityHost2Matched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq("host2"))).andReturn(true).anyTimes();

    // host request that hasn't been matched to any registered host yet
    TopologyHostRequestEntity hostRequestEntityHost3NotMatched = new TopologyHostRequestEntity();
    hostRequestEntityHost3NotMatched.setId(3L);
    hostRequestEntityHost3NotMatched.setTopologyHostGroupEntity(hostGroupEntity2);
    hostRequestEntityHost3NotMatched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq("host3"))).andReturn(false).anyTimes();

    TopologyHostRequestEntity hostRequestEntityHost4NotMatched = new TopologyHostRequestEntity();
    hostRequestEntityHost4NotMatched.setId(4L);
    hostRequestEntityHost4NotMatched.setTopologyHostGroupEntity(hostGroupEntity2);
    hostRequestEntityHost4NotMatched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq("host4"))).andReturn(false).anyTimes();

    Collection<TopologyHostRequestEntity> reservedHostRequestEntities = ImmutableSet.of(
      hostRequestEntityHost1Matched,
      hostRequestEntityHost2Matched,
      hostRequestEntityHost3NotMatched,
      hostRequestEntityHost4NotMatched);

    hostGroupEntity2.setTopologyHostRequestEntities(reservedHostRequestEntities);

    TopologyRequestEntity topologyRequestEntity = new TopologyRequestEntity();
    topologyRequestEntity.setTopologyHostGroupEntities(Collections.singleton(hostGroupEntity2));

    expect(logicalRequestEntity.getTopologyHostRequestEntities()).andReturn(reservedHostRequestEntities).atLeastOnce();


    expect(logicalRequestEntity.getTopologyRequestEntity()).andReturn(topologyRequestEntity).atLeastOnce();
    expect(blueprint.getHostGroup(eq("host_group_2"))).andReturn(hostGroup2).atLeastOnce();
    expect(hostGroup2.containsMasterComponent()).andReturn(false).atLeastOnce();

    replayAll();


    // When

    LogicalRequest req = new LogicalRequest(requestId, replayedTopologyRequest, clusterTopology, logicalRequestEntity);

    // Then
    verifyAll();

    // there should be no reserved hosts when host predicates are used
    Collection<String> actualReservedHosts = req.getReservedHosts();
    Collection<String> expectedReservedHosts = Collections.emptySet();

    assertEquals(expectedReservedHosts, actualReservedHosts);

    Collection<HostRequest> actualCompletedHostReqs = req.getCompletedHostRequests();

    assertEquals(2, actualCompletedHostReqs.size());

    Optional<HostRequest> completedHostReq1 = Iterables.tryFind(actualCompletedHostReqs, new Predicate<HostRequest>() {
      @Override
      public boolean apply(@Nullable HostRequest input) {
        return "host1".equals(input.getHostName());
      }
    });

    Optional<HostRequest> completedHostReq2 = Iterables.tryFind(actualCompletedHostReqs, new Predicate<HostRequest>() {
      @Override
      public boolean apply(@Nullable HostRequest input) {
        return "host2".equals(input.getHostName());
      }
    });

    assertTrue(completedHostReq1.isPresent() && completedHostReq2.isPresent());

  }

  @Test
  public void testRemoveHostRequestByHostName() throws Exception {
    // Given
    Long requestId = 1L;

    final TopologyHostInfoEntity host1 = new TopologyHostInfoEntity();
    host1.setId(100L);
    host1.setFqdn("host1");

    final TopologyHostInfoEntity host2 = new TopologyHostInfoEntity();
    host2.setId(101L);
    host2.setFqdn("host2");

    final TopologyHostInfoEntity host3 = new TopologyHostInfoEntity();
    host3.setId(103L);
    host3.setFqdn("host3");

    final TopologyHostInfoEntity host4 = new TopologyHostInfoEntity();
    host4.setId(104L);
    host4.setFqdn("host4");

    TopologyHostGroupEntity hostGroupEntity1 = new TopologyHostGroupEntity();
    hostGroupEntity1.setTopologyHostInfoEntities(ImmutableSet.of(host1, host2, host3));
    hostGroupEntity1.setName("host_group_1");

    // host request matched to a registered host
    TopologyHostRequestEntity hostRequestEntityHost1Matched = new TopologyHostRequestEntity();
    hostRequestEntityHost1Matched.setId(1L);
    hostRequestEntityHost1Matched.setHostName(host1.getFqdn()); //host request matched host1
    hostRequestEntityHost1Matched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost1Matched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host1.getFqdn()))).andReturn(true).anyTimes();


    TopologyHostRequestEntity hostRequestEntityHost2Matched = new TopologyHostRequestEntity();
    hostRequestEntityHost2Matched.setId(2L);
    hostRequestEntityHost2Matched.setHostName(host2.getFqdn()); // host request matched host2
    hostRequestEntityHost2Matched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost2Matched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host2.getFqdn()))).andReturn(true).anyTimes();

    // host request that hasn't been matched to any registered host yet
    TopologyHostRequestEntity hostRequestEntityHost3NotMatched = new TopologyHostRequestEntity();
    hostRequestEntityHost3NotMatched.setId(3L);
    hostRequestEntityHost3NotMatched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost3NotMatched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host3.getFqdn()))).andReturn(false).anyTimes();


    TopologyHostRequestEntity hostRequestEntityHost4Matched = new TopologyHostRequestEntity();
    hostRequestEntityHost4Matched.setId(4L);
    hostRequestEntityHost4Matched.setHostName(host4.getFqdn()); // host request matched host4
    hostRequestEntityHost4Matched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost4Matched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host4.getFqdn()))).andReturn(true).anyTimes();

    Collection<TopologyHostRequestEntity> reservedHostRequestEntities = ImmutableSet.of(
      hostRequestEntityHost1Matched,
      hostRequestEntityHost2Matched,
      hostRequestEntityHost3NotMatched,
      hostRequestEntityHost4Matched);

    hostGroupEntity1.setTopologyHostRequestEntities(reservedHostRequestEntities);

    TopologyRequestEntity topologyRequestEntity = new TopologyRequestEntity();
    topologyRequestEntity.setTopologyHostGroupEntities(Collections.singleton(hostGroupEntity1));


    expect(logicalRequestEntity.getTopologyRequestEntity()).andReturn(topologyRequestEntity).atLeastOnce();
    expect(logicalRequestEntity.getTopologyHostRequestEntities()).andReturn(reservedHostRequestEntities).atLeastOnce();
    expect(blueprint.getHostGroup(eq("host_group_1"))).andReturn(hostGroup1).atLeastOnce();
    expect(hostGroup1.containsMasterComponent()).andReturn(false).atLeastOnce();


    replayAll();

    // When

    LogicalRequest req = new LogicalRequest(requestId, replayedTopologyRequest, clusterTopology, logicalRequestEntity);
    req.removeHostRequestByHostName(host4.getFqdn());


    // Then
    verifyAll();

    Collection<HostRequest>  hostRequests = req.getHostRequests();
    assertEquals(3, hostRequests.size());

    Optional<HostRequest> hostReqHost1 = Iterables.tryFind(hostRequests, new Predicate<HostRequest>() {
      @Override
      public boolean apply(@Nullable HostRequest input) {
        return host1.getFqdn().equals(input.getHostName());
      }
    });

    Optional<HostRequest> hostReqHost2 = Iterables.tryFind(hostRequests, new Predicate<HostRequest>() {
      @Override
      public boolean apply(@Nullable HostRequest input) {
        return host2.getFqdn().equals(input.getHostName());
      }
    });


    Optional<HostRequest> hostReqHost3 = Iterables.tryFind(hostRequests, new Predicate<HostRequest>() {
      @Override
      public boolean apply(@Nullable HostRequest input) {
        return input.getHostName() == null;
      }
    });


    Optional<HostRequest> hostReqHost4 = Iterables.tryFind(hostRequests, new Predicate<HostRequest>() {
      @Override
      public boolean apply(@Nullable HostRequest input) {
        return host4.getFqdn().equals(input.getHostName());
      }
    });


    assertTrue(hostReqHost1.isPresent() && hostReqHost2.isPresent() && hostReqHost3.isPresent() && !hostReqHost4.isPresent());
  }

  @Test
  public void testRemovePendingHostRequests() throws Exception {
    // Given
    Long requestId = 1L;

    final TopologyHostInfoEntity host1 = new TopologyHostInfoEntity();
    host1.setId(100L);
    host1.setFqdn("host1");

    final TopologyHostInfoEntity host2 = new TopologyHostInfoEntity();
    host2.setId(102L);
    host2.setFqdn("host2");

    TopologyHostGroupEntity hostGroupEntity1 = new TopologyHostGroupEntity();
    hostGroupEntity1.setTopologyHostInfoEntities(ImmutableSet.of(host1, host2));
    hostGroupEntity1.setName("host_group_1");

    // host request matched to a registered host
    TopologyHostRequestEntity hostRequestEntityHost1Matched = new TopologyHostRequestEntity();
    hostRequestEntityHost1Matched.setId(1L);
    hostRequestEntityHost1Matched.setHostName(host1.getFqdn()); //host request matched host1
    hostRequestEntityHost1Matched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost1Matched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host1.getFqdn()))).andReturn(true).anyTimes();


    // host request that hasn't been matched to any registered host yet
    TopologyHostRequestEntity hostRequestEntityHost2NotMatched = new TopologyHostRequestEntity();
    hostRequestEntityHost2NotMatched.setId(2L);
    hostRequestEntityHost2NotMatched.setTopologyHostGroupEntity(hostGroupEntity1);
    hostRequestEntityHost2NotMatched.setTopologyHostTaskEntities(Collections.emptySet());
    expect(ambariContext.isHostRegisteredWithCluster(eq(clusterId), eq(host2.getFqdn()))).andReturn(false).anyTimes();


    Collection<TopologyHostRequestEntity> reservedHostRequestEntities = ImmutableSet.of(
            hostRequestEntityHost1Matched,
            hostRequestEntityHost2NotMatched);

    hostGroupEntity1.setTopologyHostRequestEntities(reservedHostRequestEntities);

    TopologyRequestEntity topologyRequestEntity = new TopologyRequestEntity();
    topologyRequestEntity.setTopologyHostGroupEntities(Collections.singleton(hostGroupEntity1));


    expect(logicalRequestEntity.getTopologyRequestEntity()).andReturn(topologyRequestEntity).atLeastOnce();
    expect(logicalRequestEntity.getTopologyHostRequestEntities()).andReturn(reservedHostRequestEntities).atLeastOnce();
    expect(blueprint.getHostGroup(eq("host_group_1"))).andReturn(hostGroup1).atLeastOnce();
    expect(hostGroup1.containsMasterComponent()).andReturn(false).atLeastOnce();

    replayAll();

    // When

    LogicalRequest req = new LogicalRequest(requestId, replayedTopologyRequest, clusterTopology, logicalRequestEntity);
    req.removePendingHostRequests(null);

    // Then
    verifyAll();

    assertEquals(1, req.getHostRequests().size());
    assertEquals(0, req.getPendingHostRequestCount());
  }

  @Test
  public void testRemovePendingHostRequestsByHostCount() throws Exception {
    // Given
    int hostCount = 3;
    LogicalRequest req = createTopologyRequestByHostCount(hostCount, "host_group");
    assertEquals(hostCount, req.getPendingHostRequestCount());

    // When
    req.removePendingHostRequests(null);

    // Then
    assertEquals(0, req.getPendingHostRequestCount());
    verifyAll();
  }

  @Test
  public void testRemovePendingHostRequestsOfSpecificHostGroupByHostCount() throws Exception {
    // Given
    int hostCount = 3;
    String hostGroupName = "host_group";
    LogicalRequest req = createTopologyRequestByHostCount(hostCount, hostGroupName);
    assertEquals(hostCount, req.getPendingHostRequestCount());

    // When
    req.removePendingHostRequests(hostGroupName);

    // Then
    assertEquals(0, req.getPendingHostRequestCount());
    verifyAll();
  }

  @Test
  public void testRemovePendingHostRequestsOfNonexistentHostGroupByHostCount() throws Exception {
    // Given
    int hostCount = 3;
    LogicalRequest req = createTopologyRequestByHostCount(hostCount, "host_group");
    assertEquals(hostCount, req.getPendingHostRequestCount());

    // When
    req.removePendingHostRequests("no_such_host_group");

    // Then
    assertEquals(hostCount, req.getPendingHostRequestCount());
    verifyAll();
  }

  private LogicalRequest createTopologyRequestByHostCount(int hostCount, String hostGroupName) throws Exception {
    final TopologyHostInfoEntity hostInfo = new TopologyHostInfoEntity();
    hostInfo.setId(100L);
    hostInfo.setHostCount(hostCount);

    TopologyHostGroupEntity hostGroupEntity = new TopologyHostGroupEntity();
    hostGroupEntity.setTopologyHostInfoEntities(ImmutableSet.of(hostInfo));
    hostGroupEntity.setName(hostGroupName);

    TopologyRequestEntity topologyRequestEntity = new TopologyRequestEntity();
    topologyRequestEntity.setTopologyHostGroupEntities(Collections.singleton(hostGroupEntity));

    Set<TopologyHostRequestEntity> hostRequests = new HashSet<>();
    for (long i = 0; i < hostCount; ++i) {
      TopologyHostRequestEntity hostRequestEntity = new TopologyHostRequestEntity();
      hostRequestEntity.setId(i);
      hostRequestEntity.setTopologyHostGroupEntity(hostGroupEntity);
      hostRequestEntity.setTopologyHostTaskEntities(Collections.emptySet());
      hostRequests.add(hostRequestEntity);
    }

    expect(logicalRequestEntity.getTopologyRequestEntity()).andReturn(topologyRequestEntity).anyTimes();
    expect(logicalRequestEntity.getTopologyHostRequestEntities()).andReturn(hostRequests).anyTimes();
    expect(blueprint.getHostGroup(eq(hostGroupEntity.getName()))).andReturn(hostGroup1).anyTimes();
    expect(hostGroup1.containsMasterComponent()).andReturn(false).anyTimes();

    replayAll();

    return new LogicalRequest(1L, replayedTopologyRequest, clusterTopology, logicalRequestEntity);
  }
}
