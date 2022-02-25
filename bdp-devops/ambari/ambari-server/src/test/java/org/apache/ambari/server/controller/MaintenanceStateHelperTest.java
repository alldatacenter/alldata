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
package org.apache.ambari.server.controller;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Injector;

/**
 * Tests the {@link MaintenanceStateHelper} class
 */
public class MaintenanceStateHelperTest {

  @Test
  public void testisOperationAllowed() throws Exception {
    // Tests that isOperationAllowed() falls
    // back to guessing req op level if operation level is not specified
    // explicitly
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createMock(Cluster.class);
    Method isOperationAllowed = MaintenanceStateHelper.class.getDeclaredMethod(
            "isOperationAllowed", new Class[]{Cluster.class, Type.class,
            String.class, String.class, String.class});
    MaintenanceStateHelper maintenanceStateHelper =
            createMockBuilder(MaintenanceStateHelper.class)
                    .withConstructor(injector)
                    .addMockedMethod(isOperationAllowed)
                    .createNiceMock();

    RequestResourceFilter filter = createMock(RequestResourceFilter.class);
    RequestOperationLevel level = createMock(RequestOperationLevel.class);
    expect(level.getLevel()).andReturn(Type.Cluster);
    expect(maintenanceStateHelper.isOperationAllowed(
            anyObject(Cluster.class), anyObject(Type.class),
            anyObject(String.class),
            anyObject(String.class), anyObject(String.class))).andStubReturn(true);
    // Case when level is defined
    replay(cluster, maintenanceStateHelper, level);
    maintenanceStateHelper.isOperationAllowed(cluster, level, filter, "service", "component", "hostname");
    verify(maintenanceStateHelper, level);

    maintenanceStateHelper =
            createMockBuilder(MaintenanceStateHelper.class)
                    .withConstructor(injector)
                    .addMockedMethod(isOperationAllowed)
                    .addMockedMethod("guessOperationLevel")
                    .createNiceMock();

    expect(maintenanceStateHelper.guessOperationLevel(anyObject(RequestResourceFilter.class))).andReturn(Type.Cluster);
    expect(maintenanceStateHelper.isOperationAllowed(
            anyObject(Cluster.class), anyObject(Type.class),
            anyObject(String.class),
            anyObject(String.class), anyObject(String.class))).andStubReturn(true);
    // Case when level is not defined
    replay(maintenanceStateHelper);
    maintenanceStateHelper.isOperationAllowed(cluster, null, filter, "service", "component", "hostname");
    verify(maintenanceStateHelper);
  }

  @Test
  public void testHostComponentImpliedState() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper =
      createMockBuilder(MaintenanceStateHelper.class)
        .withConstructor(injector)
        .createNiceMock();

    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    Service service = createNiceMock(Service.class);
    final Host host = createNiceMock(Host.class);

    expect(sch.getClusterName()).andReturn("c1").anyTimes();
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(clusters.getHost("h1")).andReturn(host).anyTimes();
    expect(sch.getHostName()).andReturn("h1").anyTimes();
    expect(sch.getServiceName()).andReturn("HDFS").anyTimes();
    expect(cluster.getService("HDFS")).andReturn(service).anyTimes();

    expect(sch.getMaintenanceState())
      .andReturn(MaintenanceState.ON).times(1)
      .andReturn(MaintenanceState.OFF).anyTimes();
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.ON);
    expect(host.getMaintenanceState(1L)).andReturn(MaintenanceState.ON);
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.ON);
    expect(host.getMaintenanceState(1L)).andReturn(MaintenanceState.OFF);
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.OFF);
    expect(host.getMaintenanceState(1L)).andReturn(MaintenanceState.ON);

    injectField(maintenanceStateHelper, clusters);

    replay(maintenanceStateHelper, clusters, cluster, sch, host, service);

    MaintenanceState state = maintenanceStateHelper.getEffectiveState(sch);
    Assert.assertEquals(MaintenanceState.ON, state);

    state = maintenanceStateHelper.getEffectiveState(sch);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST, state);

    state = maintenanceStateHelper.getEffectiveState(sch);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_SERVICE, state);

    state = maintenanceStateHelper.getEffectiveState(sch);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_HOST, state);

    verify(maintenanceStateHelper, clusters, cluster, sch, host, service);
  }

  /**
   * Tests that the host MM state is calculated correctly for an alert.
   *
   * @throws Exception
   */
  @Test
  public void testGetEffectiveStateForHostAlert() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper = createMockBuilder(
        MaintenanceStateHelper.class).withConstructor(injector).createNiceMock();

    Clusters clusters = createMock(Clusters.class);
    final Host host = createNiceMock(Host.class);

    long clusterId = 1L;
    String hostName = "c6401.ambari.apache.org";

    Alert alert = new Alert("foo-alert", null, "HDFS", "DATANODE", hostName,
        AlertState.CRITICAL);

    expect(host.getMaintenanceState(clusterId)).andReturn(MaintenanceState.ON).once();
    expect(clusters.getHost(hostName)).andReturn(host).once();

    injectField(maintenanceStateHelper, clusters);
    replay(maintenanceStateHelper, clusters, host);

    MaintenanceState state = maintenanceStateHelper.getEffectiveState(clusterId, alert);
    Assert.assertEquals(MaintenanceState.ON, state);

    verify(maintenanceStateHelper, clusters, host);
  }

  /**
   * Tests that the service MM state is calculated correctly for an alert.
   *
   * @throws Exception
   */
  @Test
  public void testGetEffectiveStateForServiceAlert() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper = createMockBuilder(
        MaintenanceStateHelper.class).withConstructor(injector).createNiceMock();

    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    final Host host = createNiceMock(Host.class);

    long clusterId = 1L;
    String hostName = "c6401.ambari.apache.org";

    Alert alert = new Alert("foo-alert", null, "HDFS", null, hostName, AlertState.CRITICAL);

    expect(host.getMaintenanceState(clusterId)).andReturn(MaintenanceState.OFF).once();
    expect(clusters.getHost(hostName)).andReturn(host).once();
    expect(clusters.getClusterById(clusterId)).andReturn(cluster).once();
    expect(cluster.getService("HDFS")).andReturn(service).once();
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.ON);

    injectField(maintenanceStateHelper, clusters);
    replay(maintenanceStateHelper, clusters, host, cluster, service);

    MaintenanceState state = maintenanceStateHelper.getEffectiveState(clusterId, alert);
    Assert.assertEquals(MaintenanceState.ON, state);

    verify(maintenanceStateHelper, clusters, host, cluster, service);
  }

  /**
   * Tests that the service MM state is calculated correctly for an alert which
   * is only for a service (such as an AGGREGATE alert).
   *
   * @throws Exception
   */
  @Test
  public void testGetEffectiveStateForServiceOnlyAlert() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper = createMockBuilder(
        MaintenanceStateHelper.class).withConstructor(injector).createNiceMock();

    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Service service = createNiceMock(Service.class);

    long clusterId = 1L;

    Alert alert = new Alert("foo-alert", null, "HDFS", null, null, AlertState.CRITICAL);

    expect(clusters.getClusterById(clusterId)).andReturn(cluster).once();
    expect(cluster.getService("HDFS")).andReturn(service).once();
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.ON);

    injectField(maintenanceStateHelper, clusters);
    replay(maintenanceStateHelper, clusters, cluster, service);

    MaintenanceState state = maintenanceStateHelper.getEffectiveState(clusterId, alert);
    Assert.assertEquals(MaintenanceState.ON, state);

    verify(maintenanceStateHelper, clusters, cluster, service);
  }

  /**
   * Tests that the service MM state is calculated correctly for an alert.
   *
   * @throws Exception
   */
  @Test
  public void testGetEffectiveStateForComponentAlert() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper = createMockBuilder(
        MaintenanceStateHelper.class).withConstructor(injector).createNiceMock();

    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent = createMock(ServiceComponent.class);
    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    final Host host = createNiceMock(Host.class);

    long clusterId = 1L;
    String hostName = "c6401.ambari.apache.org";

    Alert alert = new Alert("foo-alert", null, "HDFS", "DATANODE", hostName, AlertState.CRITICAL);

    expect(host.getMaintenanceState(clusterId)).andReturn(MaintenanceState.OFF).once();
    expect(clusters.getHost(hostName)).andReturn(host).once();
    expect(clusters.getClusterById(clusterId)).andReturn(cluster).once();
    expect(cluster.getService("HDFS")).andReturn(service).once();
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.OFF);
    expect(service.getServiceComponent("DATANODE")).andReturn(serviceComponent).once();
    expect(serviceComponent.getServiceComponentHost(hostName)).andReturn(sch).once();
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.ON).once();

    injectField(maintenanceStateHelper, clusters);
    replay(maintenanceStateHelper, clusters, host, cluster, service, serviceComponent, sch);

    MaintenanceState state = maintenanceStateHelper.getEffectiveState(clusterId, alert);
    Assert.assertEquals(MaintenanceState.ON, state);

    verify(maintenanceStateHelper, clusters, host, cluster, service, serviceComponent, sch);
  }

  @Test
  public void testServiceOperationsAllowance() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper =
            createMockBuilder(MaintenanceStateHelper.class)
                    .withConstructor(injector)
                    .createNiceMock();

    Service service = createMock(Service.class);

    // only called for Cluster level
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.ON);
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.OFF);

    replay(maintenanceStateHelper, service);


    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, service));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, service));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, service));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, service));

    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, service));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, service));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, service));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, service));

    verify(maintenanceStateHelper, service);
  }

  @Test
  public void testHostOperationsAllowance() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper =
            createMockBuilder(MaintenanceStateHelper.class)
                    .withConstructor(injector)
                    .createNiceMock();

    Host host = createMock(Host.class);

    // only called for Cluster level
    expect(host.getMaintenanceState(anyInt())).andReturn(MaintenanceState.ON);
    expect(host.getMaintenanceState(anyInt())).andReturn(MaintenanceState.OFF);

    replay(maintenanceStateHelper, host);


    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(host, 1, Resource.Type.Cluster));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(host, 1, Resource.Type.Service));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(host, 1, Resource.Type.Host));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(host, 1, Resource.Type.HostComponent));

    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(host, 1, Resource.Type.Cluster));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(host, 1, Resource.Type.Service));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(host, 1, Resource.Type.Host));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(host, 1, Resource.Type.HostComponent));

    verify(maintenanceStateHelper, host);
  }

  @Test
  public void testHostComponentOperationsAllowance() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    Method getEffectiveState = MaintenanceStateHelper.class.getMethod("getEffectiveState", new Class[]{ServiceComponentHost.class});
    MaintenanceStateHelper maintenanceStateHelper =
      createMockBuilder(MaintenanceStateHelper.class)
        .withConstructor(injector)
        .addMockedMethod(getEffectiveState)
        .createNiceMock();

    ServiceComponentHost sch = createMock(ServiceComponentHost.class);

    // Cluster
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.ON);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_HOST);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.OFF);

    // Service
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.ON);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_HOST);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.OFF);

    // Host
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.ON);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_HOST);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.OFF);

    // HostComponent
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.ON);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_HOST);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class))).andReturn(MaintenanceState.OFF);

    replay(maintenanceStateHelper, sch);

    // Cluster
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));

    // Service
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));
    Assert.assertEquals(false , maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));

    // Host
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));

    // HostComponent
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));

    verify(maintenanceStateHelper, sch);
  }

  @Test
  public void testGuessOperationLevel() {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper =
            createMockBuilder(MaintenanceStateHelper.class)
                    .withConstructor(injector)
                    .createNiceMock();
    replay(maintenanceStateHelper);

    Assert.assertEquals(Resource.Type.Cluster, maintenanceStateHelper.guessOperationLevel(null));

    RequestResourceFilter resourceFilter = new RequestResourceFilter(null, null, null);
    Assert.assertEquals(Resource.Type.Cluster, maintenanceStateHelper.guessOperationLevel(resourceFilter));

    resourceFilter = new RequestResourceFilter("HDFS", null, null);
    Assert.assertEquals(Resource.Type.Service, maintenanceStateHelper.guessOperationLevel(resourceFilter));

    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    Assert.assertEquals(Resource.Type.Service, maintenanceStateHelper.guessOperationLevel(resourceFilter));

    ArrayList<String> hosts = new ArrayList<>();
    hosts.add("host1");
    hosts.add("host2");
    resourceFilter = new RequestResourceFilter("HDFS", null, hosts);
    Assert.assertEquals(Resource.Type.Cluster, maintenanceStateHelper.guessOperationLevel(resourceFilter));

    resourceFilter = new RequestResourceFilter(null, null, hosts);
    Assert.assertEquals(Resource.Type.Host, maintenanceStateHelper.guessOperationLevel(resourceFilter));

    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", hosts);
    Assert.assertEquals(Resource.Type.HostComponent, maintenanceStateHelper.guessOperationLevel(resourceFilter));

  }

  @Test
  public void testCutOffHosts() throws AmbariException {
    MaintenanceStateHelper.HostPredicate predicate = createMock(MaintenanceStateHelper.HostPredicate.class);
    expect(predicate.shouldHostBeRemoved(eq("host1"))).andReturn(true);
    expect(predicate.shouldHostBeRemoved(eq("host2"))).andReturn(false);
    expect(predicate.shouldHostBeRemoved(eq("host3"))).andReturn(true);
    expect(predicate.shouldHostBeRemoved(eq("host4"))).andReturn(false);
    Set<String> candidates = new HashSet<>();
    candidates.add("host1");
    candidates.add("host2");
    candidates.add("host3");
    candidates.add("host4");
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper =
            createMockBuilder(MaintenanceStateHelper.class)
                    .withConstructor(injector)
                    .createNiceMock();
    replay(predicate, maintenanceStateHelper);

    Set<String> ignored = maintenanceStateHelper.filterHostsInMaintenanceState(candidates, predicate);

    verify(predicate, maintenanceStateHelper);

    Assert.assertEquals(candidates.size(), 2);
    Assert.assertTrue(candidates.contains("host2"));
    Assert.assertTrue(candidates.contains("host4"));

    Assert.assertEquals(ignored.size(), 2);
    Assert.assertTrue(ignored.contains("host1"));
    Assert.assertTrue(ignored.contains("host3"));
  }

  private static void injectField(MaintenanceStateHelper maintenanceStateHelper, Clusters clusters)
          throws NoSuchFieldException, IllegalAccessException {
    Class<?> maintenanceHelperClass = MaintenanceStateHelper.class;
    Field f = maintenanceHelperClass.getDeclaredField("clusters");
    f.setAccessible(true);
    f.set(maintenanceStateHelper, clusters);
  }

  public static MaintenanceStateHelper getMaintenanceStateHelperInstance(
    Clusters clusters) throws NoSuchFieldException, IllegalAccessException {
    Injector injector = createNiceMock(Injector.class);
    injector.injectMembers(anyObject(MaintenanceStateHelper.class));
    EasyMock.expectLastCall().once();
    replay(injector);

    MaintenanceStateHelper maintenanceStateHelper = new MaintenanceStateHelper(injector);
    injectField(maintenanceStateHelper, clusters);
    return maintenanceStateHelper;
  }
}
