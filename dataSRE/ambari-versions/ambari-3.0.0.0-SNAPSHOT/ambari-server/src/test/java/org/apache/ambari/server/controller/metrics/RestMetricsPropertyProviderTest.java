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

package org.apache.ambari.server.controller.metrics;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.StackDefinedPropertyProvider;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.jmx.TestStreamProvider;
import org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.services.MetricsRetrievalService;
import org.apache.ambari.server.state.stack.Metric;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.ambari.server.utils.SynchronousThreadPoolExecutor;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;


/**
 * Rest metrics property provider tests.
 */
public class RestMetricsPropertyProviderTest {

  public static final String WRAPPED_METRICS_KEY = "WRAPPED_METRICS_KEY";
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  protected static final String HOST_COMPONENT_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "state");
  protected static final Map<String, String> metricsProperties = new HashMap<>();
  protected static final Map<String, Metric> componentMetrics = new HashMap<>();
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String DEFAULT_STORM_UI_PORT = "8745";
  public static final int NUMBER_OF_RESOURCES = 400;
  private static final int METRICS_SERVICE_TIMEOUT = 10;
  private static Injector injector;
  private static Clusters clusters;
  private static Cluster c1;
  private static AmbariManagementController amc;
  private static MetricsRetrievalService metricsRetrievalService;

  {
    metricsProperties.put("default_port", DEFAULT_STORM_UI_PORT);
    metricsProperties.put("port_config_type", "storm-site");
    metricsProperties.put("port_property_name", "ui.port");
    metricsProperties.put("protocol", "http");
    componentMetrics.put("metrics/api/cluster/summary/tasks.total", new Metric("/api/cluster/summary##tasks.total", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/slots.total", new Metric("/api/cluster/summary##slots.total", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/slots.free", new Metric("/api/cluster/summary##slots.free", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/supervisors", new Metric("/api/cluster/summary##supervisors", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/executors.total", new Metric("/api/cluster/summary##executors.total", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/slots.used", new Metric("/api/cluster/summary##slots.used", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/topologies", new Metric("/api/cluster/summary##topologies", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/nimbus.uptime", new Metric("/api/cluster/summary##nimbus.uptime", false, false, false, "unitless"));
    componentMetrics.put("metrics/api/cluster/summary/wrong.metric", new Metric(null, false, false, false, "unitless"));
  }


  @BeforeClass
  public static void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    StackDAO stackDAO = injector.getInstance(StackDAO.class);


    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("2.1.1");
    stackDAO.create(stackEntity);


    clusters.addCluster("c1", new StackId("HDP-2.1.1"));
    c1 = clusters.getCluster("c1");




    // disable request TTL for these tests
    Configuration configuration = injector.getInstance(Configuration.class);
    configuration.setProperty(Configuration.METRIC_RETRIEVAL_SERVICE_REQUEST_TTL_ENABLED.getKey(),
        "false");

    JMXPropertyProvider.init(configuration);

    metricsRetrievalService = injector.getInstance(
        MetricsRetrievalService.class);

    metricsRetrievalService.startAsync();
    metricsRetrievalService.awaitRunning(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);
    metricsRetrievalService.setThreadPoolExecutor(new SynchronousThreadPoolExecutor());

    // Setting up Mocks for Controller, Clusters etc, queried as part of user's Role context
    // while fetching Metrics.
    amc = createNiceMock(AmbariManagementController.class);
    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);

    ConfigHelper configHelperMock = createNiceMock(ConfigHelper.class);
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(amc.getAmbariEventPublisher()).andReturn(createNiceMock(AmbariEventPublisher.class)).anyTimes();
    expect(amc.findConfigurationTagsWithOverrides(eq(c1), anyString())).andReturn(Collections.singletonMap("storm-site",
        Collections.singletonMap("tag", "version1"))).anyTimes();
    expect(amc.getConfigHelper()).andReturn(configHelperMock).anyTimes();
    expect(configHelperMock.getEffectiveConfigProperties(eq(c1),
        EasyMock.anyObject())).andReturn(Collections.singletonMap("storm-site",
        Collections.singletonMap("ui.port", DEFAULT_STORM_UI_PORT))).anyTimes();
    replay(amc, configHelperMock);
  }

  @AfterClass
  public static void after() throws AmbariException, SQLException, TimeoutException {
    if (metricsRetrievalService != null && metricsRetrievalService.isRunning()) {
      metricsRetrievalService.stopAsync();
      metricsRetrievalService.awaitTerminated(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);
    }
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private RestMetricsPropertyProvider createRestMetricsPropertyProvider(MetricDefinition metricDefinition,
      HashMap<String, Map<String, PropertyInfo>> componentMetrics, StreamProvider streamProvider,
      TestMetricsHostProvider metricsHostProvider) throws Exception {

    MetricPropertyProviderFactory factory = injector.getInstance(MetricPropertyProviderFactory.class);
    RestMetricsPropertyProvider restMetricsPropertyProvider = factory.createRESTMetricsPropertyProvider(
        metricDefinition.getProperties(),
        componentMetrics,
        streamProvider,
        metricsHostProvider,
        PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
        PropertyHelper.getPropertyId("HostRoles", "host_name"),
        PropertyHelper.getPropertyId("HostRoles", "component_name"),
        PropertyHelper.getPropertyId("HostRoles", "state"),
        "STORM_REST_API"
    );

    Field field = RestMetricsPropertyProvider.class.getDeclaredField("amc");
    field.setAccessible(true);
    field.set(restMetricsPropertyProvider, amc);

    return restMetricsPropertyProvider;
  }


  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testRestMetricsPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test
  public void testRestMetricsPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test
  public void testRestMetricsPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test(expected = AuthorizationException.class)
  public void testRestMetricsPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));
    testPopulateResources();
    testPopulateResources_singleProperty();
    testPopulateResources_category();
    testPopulateResourcesUnhealthyResource();
    testPopulateResourcesMany();
    testPopulateResourcesTimeout();
  }

  @Test
  public void testResolvePort() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);

    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    RestMetricsPropertyProvider restMetricsPropertyProvider = createRestMetricsPropertyProvider(metricDefinition, componentMetrics, streamProvider,
        metricsHostProvider);

    // a property with a port doesn't exist, should return a default
    Map<String, String> customMetricsProperties = new HashMap<>(metricsProperties);
    customMetricsProperties.put("port_property_name", "wrong_property");
    String resolvedPort = restMetricsPropertyProvider.resolvePort(c1, "domu-12-31-39-0e-34-e1.compute-1.internal",
        "STORM_REST_API", customMetricsProperties, "http");
    Assert.assertEquals(DEFAULT_STORM_UI_PORT, resolvedPort);

    // a port property exists (8745). Should return it, not a default_port (8746)
    customMetricsProperties = new HashMap<>(metricsProperties);
    // custom default
    customMetricsProperties.put("default_port", "8746");
    resolvedPort = restMetricsPropertyProvider.resolvePort(c1, "domu-12-31-39-0e-34-e1.compute-1.internal",
        "STORM_REST_API", customMetricsProperties, "http");
    Assert.assertEquals(DEFAULT_STORM_UI_PORT, resolvedPort);

  }

  public void testPopulateResources() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);

    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    RestMetricsPropertyProvider restMetricsPropertyProvider = createRestMetricsPropertyProvider(metricDefinition, componentMetrics, streamProvider,
        metricsHostProvider);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());
    Assert.assertEquals(1, restMetricsPropertyProvider.populateResources(Collections.singleton(resource), request, null).size());
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "wrong.metric")));

    //STORM_REST_API
    Assert.assertEquals(28.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "tasks.total")));
    Assert.assertEquals(8.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.total")));
    Assert.assertEquals(5.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.free")));
    Assert.assertEquals(2.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "supervisors")));
    Assert.assertEquals(28.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "executors.total")));
    Assert.assertEquals(3.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.used")));
    Assert.assertEquals(1.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "topologies")));
    Assert.assertEquals(4637.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "nimbus.uptime")));
  }

  public void testPopulateResources_singleProperty() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    RestMetricsPropertyProvider restMetricsPropertyProvider = createRestMetricsPropertyProvider(metricDefinition, componentMetrics, streamProvider,
        metricsHostProvider);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, restMetricsPropertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(28.0, resource.getPropertyValue("metrics/api/cluster/summary/tasks.total"));
    Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/taskstotal"));
  }

  public void testPopulateResources_category() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    RestMetricsPropertyProvider restMetricsPropertyProvider = createRestMetricsPropertyProvider(metricDefinition, componentMetrics, streamProvider,
        metricsHostProvider);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/api/cluster"), temporalInfoMap);

    Assert.assertEquals(1, restMetricsPropertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/hdfs_namenode_jmx.json for values
    Assert.assertEquals(28.0, resource.getPropertyValue("metrics/api/cluster/summary/tasks.total"));
    Assert.assertEquals(2.0, resource.getPropertyValue("metrics/api/cluster/summary/supervisors"));
    Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/taskstotal"));
  }

  public void testPopulateResourcesUnhealthyResource() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    RestMetricsPropertyProvider restMetricsPropertyProvider = createRestMetricsPropertyProvider(metricDefinition, componentMetrics, streamProvider,
        metricsHostProvider);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c1");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "INSTALLED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, restMetricsPropertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // Assert that the stream provider was never called.
    Assert.assertNull(streamProvider.getLastSpec());
  }

  public void testPopulateResourcesMany() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    Set<Resource> resources = new HashSet<>();

    RestMetricsPropertyProvider restMetricsPropertyProvider = createRestMetricsPropertyProvider(metricDefinition, componentMetrics, streamProvider,
        metricsHostProvider);

    for (int i = 0; i < NUMBER_OF_RESOURCES; ++i) {
      // strom_rest_api
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);

      resource.setProperty("HostRoles/cluster_name", "c1");
      resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
      resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
      resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");
      resource.setProperty("unique_id", i);

      resources.add(resource);
    }

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Set<Resource> resourceSet = restMetricsPropertyProvider.populateResources(resources, request, null);

    Assert.assertEquals(NUMBER_OF_RESOURCES, resourceSet.size());

    for (Resource resource : resourceSet) {
      Assert.assertEquals(28.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "tasks.total")));
      Assert.assertEquals(8.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.total")));
      Assert.assertEquals(5.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.free")));
      Assert.assertEquals(2.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "supervisors")));
    }
  }

  public void testPopulateResourcesTimeout() throws Exception {
    MetricDefinition metricDefinition = createNiceMock(MetricDefinition.class);
    expect(metricDefinition.getMetrics()).andReturn(componentMetrics);
    expect(metricDefinition.getType()).andReturn("org.apache.ambari.server.controller.metrics.RestMetricsPropertyProvider");
    expect(metricDefinition.getProperties()).andReturn(metricsProperties);
    replay(metricDefinition);
    Map<String, PropertyInfo> metrics = StackDefinedPropertyProvider.getPropertyInfo(metricDefinition);
    HashMap<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<>();
    componentMetrics.put(WRAPPED_METRICS_KEY, metrics);
    TestStreamProvider streamProvider = new TestStreamProvider(100L);
    TestMetricsHostProvider metricsHostProvider = new TestMetricsHostProvider();

    Set<Resource> resources = new HashSet<>();

    RestMetricsPropertyProvider restMetricsPropertyProvider = createRestMetricsPropertyProvider(metricDefinition, componentMetrics, streamProvider,
        metricsHostProvider);

    // set the provider timeout to -1 millis to guarantee timeout
    restMetricsPropertyProvider.setPopulateTimeout(-1L);
    try {
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);

      resource.setProperty("HostRoles/cluster_name", "c1");
      resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
      resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");

      resources.add(resource);

      // request with an empty set should get all supported properties
      Request request = PropertyHelper.getReadRequest(Collections.emptySet());

      Set<Resource> resourceSet = restMetricsPropertyProvider.populateResources(resources, request, null);

      // make sure that the thread running the stream provider has completed
      Thread.sleep(150L);

      Assert.assertEquals(0, resourceSet.size());

      // assert that properties never get set on the resource
      Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/tasks.total"));
      Assert.assertNull(resource.getPropertyValue("metrics/api/cluster/summary/supervisors"));
    } finally {
      // reset default value
      restMetricsPropertyProvider.setPopulateTimeout(injector.getInstance(Configuration.class)
          .getPropertyProvidersCompletionServiceTimeout());
    }
  }

  public static class TestMetricsHostProvider implements MetricHostProvider {

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
