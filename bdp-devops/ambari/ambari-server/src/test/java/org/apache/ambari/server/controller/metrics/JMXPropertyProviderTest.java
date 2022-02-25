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

package org.apache.ambari.server.controller.metrics;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.jmx.JMXHostProvider;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.jmx.TestStreamProvider;
import org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.services.MetricsRetrievalService;
import org.apache.ambari.server.utils.SynchronousThreadPoolExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * JMX property provider tests.
 */
public class JMXPropertyProviderTest {
  protected static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  protected static final String HOST_COMPONENT_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "state");

  public static final int NUMBER_OF_RESOURCES = 400;
  private static final int METRICS_SERVICE_TIMEOUT = 10;

  public static final Map<String, Map<String, PropertyInfo>> jmxPropertyIds = PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent);
  public static final Map<String, Map<String, PropertyInfo>> jmxPropertyIdsWithHAState;

  static {
    jmxPropertyIdsWithHAState = new HashMap<>(jmxPropertyIds);
    jmxPropertyIdsWithHAState.get("NAMENODE").put("metrics/dfs/FSNamesystem/HAState", new PropertyInfo("Hadoop:service=NameNode,name=FSNamesystem.tag#HAState", false, true));
  }

  private static MetricPropertyProviderFactory metricPropertyProviderFactory;
  private static MetricsRetrievalService metricsRetrievalService;

  @BeforeClass
  public static void setupClass() throws TimeoutException {
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());

    // disable request TTL for these tests
    Configuration configuration = injector.getInstance(Configuration.class);
    configuration.setProperty(Configuration.METRIC_RETRIEVAL_SERVICE_REQUEST_TTL_ENABLED.getKey(),
        "false");

    JMXPropertyProvider.init(configuration);

    metricPropertyProviderFactory = injector.getInstance(MetricPropertyProviderFactory.class);

    metricsRetrievalService = injector.getInstance(
        MetricsRetrievalService.class);

    metricsRetrievalService.startAsync();
    metricsRetrievalService.awaitRunning(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);
    metricsRetrievalService.setThreadPoolExecutor(new SynchronousThreadPoolExecutor());
  }

  @AfterClass
  public static void stopService() throws TimeoutException {
    if (metricsRetrievalService != null && metricsRetrievalService.isRunning()) {
      metricsRetrievalService.stopAsync();
      metricsRetrievalService.awaitTerminated(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);
    }
  }

  @Before
  public void setUpCommonMocks() throws AmbariException, NoSuchFieldException, IllegalAccessException {
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(CLUSTER_NAME_PROPERTY_ID)).andReturn(cluster).anyTimes();
    expect(cluster.getResourceId()).andReturn(2L).anyTimes();

    try {
      expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }

    replay(amc, clusters, cluster);
    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testJMXPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesWithUnknownPort();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
    testPopulateResources_HAState_request();
  }

  @Test
  public void testJMXPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesWithUnknownPort();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
    testPopulateResources_HAState_request();
  }

  @Test
  public void testJMXPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesWithUnknownPort();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
    testPopulateResources_HAState_request();
  }

  @Test(expected = AuthorizationException.class)
  public void testJMXPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesWithUnknownPort();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
    testPopulateResources_HAState_request();
  }

  public void testPopulateResources() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();
    JMXPropertyProvider propertyProvider = metricPropertyProviderFactory.createJMXPropertyProvider(
      jmxPropertyIdsWithHAState,
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));

    // set the provider timeout to 5000 millis
    propertyProvider.setPopulateTimeout(5000);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());
    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> expectedSpecs = new ArrayList<>();
    expectedSpecs.add(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"));
    expectedSpecs.add(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx?get=Hadoop:service=NameNode,name=FSNamesystem::tag.HAState"));
    Assert.assertEquals(expectedSpecs, streamProvider.getSpecs());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(28, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/namenode", "CreateFileOps")));
    Assert.assertEquals(1006632960, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(473433016, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(23634400, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(887717691390L, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/FSNamesystem", "CapacityTotal")));
    Assert.assertEquals(184320, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/FSNamesystem", "CapacityUsed")));
    Assert.assertEquals(842207944704L, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/FSNamesystem", "CapacityRemaining")));
    Assert.assertEquals(45509562366L, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/FSNamesystem", "CapacityNonDFSUsed")));

    // datanode
    resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");

    // request with an empty set should get all supported properties
    request = PropertyHelper.getReadRequest(Collections.emptySet());

    propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-14-ee-b3.compute-1.internal", "50075", "/jmx"), streamProvider.getLastSpec());

    // see test/resources/hdfs_datanode_jmx.json for values
    Assert.assertEquals(856, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(954466304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(9772616, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(21933376, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));

    // hbase master
    resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_MASTER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for specific properties
    Set<String> properties = new HashSet<>();
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax"));
    properties.add(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed"));
    properties.add(PropertyHelper.getPropertyId("metrics/load", "AverageLoad"));
    request = PropertyHelper.getReadRequest(properties);

    propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-14-ee-b3.compute-1.internal", "60010", "/jmx"), streamProvider.getLastSpec());

    Assert.assertEquals(9, PropertyHelper.getProperties(resource).size());
    Assert.assertEquals(1069416448, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(4806976, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(28971240, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(3.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/load", "AverageLoad")));

    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcCount")));
  }

  public void testPopulateResources_singleProperty() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();

    JMXPropertyProvider propertyProvider = metricPropertyProviderFactory.createJMXPropertyProvider(
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/rpc/ReceivedBytes"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"), streamProvider.getLastSpec());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605, resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
    Assert.assertNull(resource.getPropertyValue("metrics/dfs/namenode/CreateFileOps"));
  }

  public void testPopulateResources_category() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();

    JMXPropertyProvider propertyProvider = metricPropertyProviderFactory.createJMXPropertyProvider(
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/dfs"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> expectedSpecs = new ArrayList<>();
    expectedSpecs.add(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"));
    expectedSpecs.add(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx?get=Hadoop:service=NameNode,name=FSNamesystem::tag.HAState"));
    Assert.assertEquals(expectedSpecs, streamProvider.getSpecs());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(184320, resource.getPropertyValue("metrics/dfs/FSNamesystem/CapacityUsed"));
    Assert.assertEquals(21, resource.getPropertyValue("metrics/dfs/FSNamesystem/UnderReplicatedBlocks"));
    Assert.assertNull(resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
  }

  public void testPopulateResources_HAState_request() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(false);
    TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();

    JMXPropertyProvider propertyProvider = metricPropertyProviderFactory.createJMXPropertyProvider(
      jmxPropertyIdsWithHAState,
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/dfs/FSNamesystem"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> expectedSpecs = new ArrayList<>();
    expectedSpecs.add(propertyProvider.getSpec("http","domu-12-31-39-0e-34-e1.compute-1.internal","50070","/jmx"));
    expectedSpecs.add(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx?get=Hadoop:service=NameNode,name=FSNamesystem::tag.HAState"));

    Assert.assertEquals(expectedSpecs, streamProvider.getSpecs());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(184320, resource.getPropertyValue("metrics/dfs/FSNamesystem/CapacityUsed"));
    Assert.assertEquals(21, resource.getPropertyValue("metrics/dfs/FSNamesystem/UnderReplicatedBlocks"));
    Assert.assertEquals("customState", resource.getPropertyValue("metrics/dfs/FSNamesystem/HAState"));
    Assert.assertNull(resource.getPropertyValue("metrics/rpc/ReceivedBytes"));

    // namenode
    resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    streamProvider.getSpecs().clear();

    // request with an empty set should get all supported properties
    // only ask for one property
    temporalInfoMap = new HashMap<>();
    request = PropertyHelper.getReadRequest(Collections.singleton("metrics/dfs/FSNamesystem/CapacityUsed"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // HAState isn't requested. It shouldn't be retrieved.
    expectedSpecs.clear();
    expectedSpecs.add(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"));
    Assert.assertEquals(expectedSpecs, streamProvider.getSpecs());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(184320, resource.getPropertyValue("metrics/dfs/FSNamesystem/CapacityUsed"));
    Assert.assertNull(resource.getPropertyValue("metrics/dfs/FSNamesystem/HAState"));
    Assert.assertNull(resource.getPropertyValue("metrics/rpc/ReceivedBytes"));

  }

  public void testPopulateResourcesWithUnknownPort() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
    TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();

    JMXPropertyProvider propertyProvider = metricPropertyProviderFactory.createJMXPropertyProvider(
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> expectedSpecs = new ArrayList<>();
    expectedSpecs.add(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx"));
    expectedSpecs.add(propertyProvider.getSpec("http", "domu-12-31-39-0e-34-e1.compute-1.internal", "50070", "/jmx?get=Hadoop:service=NameNode,name=FSNamesystem::tag.HAState"));
    Assert.assertEquals(expectedSpecs, streamProvider.getSpecs());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(13670605, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(28, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/namenode", "CreateFileOps")));
    Assert.assertEquals(1006632960, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(473433016, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(23634400, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
  }

  public void testPopulateResourcesUnhealthyResource() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
    TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();

    JMXPropertyProvider propertyProvider = metricPropertyProviderFactory.createJMXPropertyProvider(
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "INSTALLED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // Assert that the stream provider was never called.
    Assert.assertNull(streamProvider.getLastSpec());
  }

  public void testPopulateResourcesMany() throws Exception {
    // Set the provider to take 50 millis to return the JMX values
    TestStreamProvider streamProvider = new TestStreamProvider(50L);
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
    TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();
    Set<Resource> resources = new HashSet<>();

    JMXPropertyProvider propertyProvider = metricPropertyProviderFactory.createJMXPropertyProvider(
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"));

    // set the provider timeout to 5000 millis
    propertyProvider.setPopulateTimeout(5000);

    for (int i = 0; i < NUMBER_OF_RESOURCES; ++i) {
      // datanode
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);
      resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
      resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
      resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");
      resource.setProperty("unique_id", i);

      resources.add(resource);
    }
    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Set<Resource> resourceSet = propertyProvider.populateResources(resources, request, null);

    Assert.assertEquals(NUMBER_OF_RESOURCES, resourceSet.size());
    for (Resource resource : resourceSet) {
      // see test/resources/hdfs_datanode_jmx.json for values
      Assert.assertEquals(856, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
      Assert.assertEquals(954466304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
      Assert.assertEquals(9772616, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
      Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
      Assert.assertEquals(21933376, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    }
  }

  public void testPopulateResourcesTimeout() throws Exception {
    // Set the provider to take 100 millis to return the JMX values
    TestStreamProvider streamProvider = new TestStreamProvider(100L);
    TestJMXHostProvider hostProvider = new TestJMXHostProvider(true);
    TestMetricHostProvider metricsHostProvider = new TestMetricHostProvider();
    Set<Resource> resources = new HashSet<>();

    JMXPropertyProvider propertyProvider = metricPropertyProviderFactory.createJMXPropertyProvider(
      PropertyHelper.getJMXPropertyIds(Resource.Type.HostComponent),
      streamProvider,
      hostProvider,
      metricsHostProvider,
      "HostRoles/cluster_name",
      "HostRoles/host_name",
      "HostRoles/component_name",
      "HostRoles/state");

    // set the provider timeout to 50 millis
    propertyProvider.setPopulateTimeout(50L);

    // datanode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-14-ee-b3.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "DATANODE");

    resources.add(resource);

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Set<Resource> resourceSet = propertyProvider.populateResources(resources, request, null);

    // make sure that the thread running the stream provider has completed
    Thread.sleep(150L);

    Assert.assertEquals(0, resourceSet.size());

    // assert that properties never get set on the resource
    Assert.assertNull(resource.getPropertyValue("metrics/rpc/ReceivedBytes"));
    Assert.assertNull(resource.getPropertyValue("metrics/jvm/HeapMemoryMax"));
    Assert.assertNull(resource.getPropertyValue("metrics/jvm/HeapMemoryUsed"));
    Assert.assertNull(resource.getPropertyValue("metrics/jvm/NonHeapMemoryMax"));
    Assert.assertNull(resource.getPropertyValue("metrics/jvm/NonHeapMemoryUsed"));
  }

  public static class TestJMXHostProvider implements JMXHostProvider {
    private final boolean unknownPort;

    public TestJMXHostProvider(boolean unknownPort) {
      this.unknownPort = unknownPort;
    }

    @Override public String getPublicHostName(final String clusterName, final String hostName) {
      return null;
    }

    @Override
    public Set<String> getHostNames(String clusterName, String componentName) {
      return null;
    }

    @Override
    public String getPort(String clusterName, String componentName, String hostName, boolean httpsEnabled) {
      return getPort(clusterName, componentName, hostName);
    }

    public String getPort(String clusterName, String componentName, String hostName) {

      if (unknownPort) {
        return null;
      }

      if (componentName.equals("NAMENODE")) {
        return "50070";
      } else if (componentName.equals("DATANODE")) {
        return "50075";
      } else if (componentName.equals("HBASE_MASTER")) {
        if(clusterName.equals("c2")) {
          return "60011";
        } else {
          // Caters the case where 'clusterName' is null or
          // any other name (includes hardcoded name "c1").
          return "60010";
        }
      } else if (componentName.equals("JOURNALNODE")) {
        return "8480";
      } else if (componentName.equals("STORM_REST_API")) {
        return "8745";
      } else {
        return null;
      }
    }

    @Override
    public String getJMXProtocol(String clusterName, String componentName) {
      return "http";
    }

    @Override
    public String getJMXRpcMetricTag(String clusterName, String componentName, String port) {
      return null;
    }

  }

  public static class TestMetricHostProvider implements MetricHostProvider {

    @Override
    public String getCollectorHostName(String clusterName, MetricsService service) throws SystemException {
      return null;
    }

    @Override
    public String getHostName(String clusterName, String componentName) {
      return null;
    }

    @Override
    public String getCollectorPort(String clusterName, MetricsService service) throws SystemException {
      return null;
    }

    @Override
    public boolean isCollectorHostLive(String clusterName, MetricsService service) throws SystemException {
      return false;
    }

    @Override
    public boolean isCollectorComponentLive(String clusterName, MetricsService service) throws SystemException {
      return false;
    }

    @Override
    public boolean isCollectorHostExternal(String clusterName) {
      return false;
    }
  }
}
