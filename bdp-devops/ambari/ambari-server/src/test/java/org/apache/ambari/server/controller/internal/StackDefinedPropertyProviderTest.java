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
package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.jmx.JMXPropertyProvider;
import org.apache.ambari.server.controller.jmx.TestStreamProvider;
import org.apache.ambari.server.controller.metrics.JMXPropertyProviderTest;
import org.apache.ambari.server.controller.metrics.MetricsServiceProvider;
import org.apache.ambari.server.controller.metrics.ganglia.GangliaPropertyProviderTest.TestGangliaHostProvider;
import org.apache.ambari.server.controller.metrics.ganglia.GangliaPropertyProviderTest.TestGangliaServiceProvider;
import org.apache.ambari.server.controller.metrics.timeline.MetricsRequestHelper;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheEntryFactory;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.services.MetricsRetrievalService;
import org.apache.ambari.server.state.stack.Metric;
import org.apache.ambari.server.utils.SynchronousThreadPoolExecutor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Tests the stack defined property provider.
 */
public class StackDefinedPropertyProviderTest {
  private static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID = "HostRoles/host_name";
  private static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = "HostRoles/component_name";
  private static final String HOST_COMPONENT_STATE_PROPERTY_ID = "HostRoles/state";
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final int METRICS_SERVICE_TIMEOUT = 10;

  private Clusters clusters = null;
  private Injector injector = null;
  private OrmTestHelper helper = null;

  private static TimelineMetricCacheEntryFactory cacheEntryFactory;
  private static TimelineMetricCacheProvider cacheProvider;
  private static MetricsRetrievalService metricsRetrievalService;

  @BeforeClass
  public static void setupCache() {
    cacheEntryFactory = new TimelineMetricCacheEntryFactory(new Configuration());
    cacheProvider = new TimelineMetricCacheProvider(new Configuration(), cacheEntryFactory);
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    JMXPropertyProvider.init(injector.getInstance(Configuration.class));
  }

  public class TestModuleWithCacheProvider implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(TimelineMetricCacheProvider.class).toInstance(cacheProvider);
    }
  }

  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    // Use the same cache provider to ensure there is only once instance of
    // Cache available. The @net.sf.ehcache.CacheManager is a singleton and
    // does not allow multiple instance with same cache name to be registered.
    injector = Guice.createInjector(Modules.override(module).with(new TestModuleWithCacheProvider()));
    injector.getInstance(GuiceJpaInitializer.class);
    StackDefinedPropertyProvider.init(injector);

    metricsRetrievalService = injector.getInstance(
        MetricsRetrievalService.class);

    metricsRetrievalService.startAsync();
    metricsRetrievalService.awaitRunning(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);
    metricsRetrievalService.setThreadPoolExecutor(new SynchronousThreadPoolExecutor());

    helper = injector.getInstance(OrmTestHelper.class);

    clusters = injector.getInstance(Clusters.class);
    StackId stackId = new StackId("HDP-2.0.5");

    clusters.addCluster("c2", stackId);

    Cluster cluster = clusters.getCluster("c2");

    cluster.setDesiredStackVersion(stackId);
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    Service service = cluster.addService("HDFS", repositoryVersion);
    service.addServiceComponent("NAMENODE");
    service.addServiceComponent("DATANODE");
    service.addServiceComponent("JOURNALNODE");

    service = cluster.addService("YARN", repositoryVersion);
    service.addServiceComponent("RESOURCEMANAGER");

    service = cluster.addService("HBASE", repositoryVersion);
    service.addServiceComponent("HBASE_MASTER");
    service.addServiceComponent("HBASE_REGIONSERVER");

    stackId = new StackId("HDP-2.1.1");
    repositoryVersion = helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    service = cluster.addService("STORM", repositoryVersion);
    service.addServiceComponent("STORM_REST_API");

    clusters.addHost("h1");
    Host host = clusters.getHost("h1");
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    host.setHostAttributes(hostAttributes);

    clusters.mapHostToCluster("h1", "c2");

    // Setting up Mocks for Controller, Clusters etc, queried as part of user's Role context
    // while fetching Metrics.
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);
    Clusters clustersMock = createNiceMock(Clusters.class);
    Cluster clusterMock = createNiceMock(Cluster.class);
    expect(amc.getAmbariEventPublisher()).andReturn(createNiceMock(AmbariEventPublisher.class)).anyTimes();
    expect(amc.getClusters()).andReturn(clustersMock).anyTimes();
    expect(clustersMock.getCluster(CLUSTER_NAME_PROPERTY_ID)).andReturn(clusterMock).anyTimes();
    expect(clusterMock.getResourceId()).andReturn(2L).anyTimes();
    try {
      expect(clustersMock.getCluster(anyObject(String.class))).andReturn(clusterMock).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    replay(amc, clustersMock, clusterMock);
  }

  @After
  public void teardown() throws Exception {
    if (metricsRetrievalService != null && metricsRetrievalService.isRunning()) {
      metricsRetrievalService.stopAsync();
      metricsRetrievalService.awaitTerminated(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);
    }
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testStackDefinedPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));

    testPopulateHostComponentResources();
    testCustomProviders();
    testPopulateResources_HDP2();
    testPopulateResources_HDP2_params();
    testPopulateResources_HDP2_params_singleProperty();
    testPopulateResources_HDP2_params_category();
    testPopulateResources_HDP2_params_category2();
    testPopulateResources_jmx_JournalNode();
    testPopulateResources_jmx_Storm();
    testPopulateResources_NoRegionServer();
    testPopulateResources_HBaseMaster2();
    testPopulateResources_params_category5();
    testPopulateResources_ganglia_JournalNode();
    testPopulateResources_resourcemanager_clustermetrics();
    testPopulateResourcesWithAggregateFunctionMetrics();
  }

  @Test
  public void testStackDefinedPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));

    testPopulateHostComponentResources();
    testCustomProviders();
    testPopulateResources_HDP2();
    testPopulateResources_HDP2_params();
    testPopulateResources_HDP2_params_singleProperty();
    testPopulateResources_HDP2_params_category();
    testPopulateResources_HDP2_params_category2();
    testPopulateResources_jmx_JournalNode();
    testPopulateResources_jmx_Storm();
    testPopulateResources_NoRegionServer();
    testPopulateResources_HBaseMaster2();
    testPopulateResources_params_category5();
    testPopulateResources_ganglia_JournalNode();
    testPopulateResources_resourcemanager_clustermetrics();
    testPopulateResourcesWithAggregateFunctionMetrics();
  }

  @Test
  public void testStackDefinedPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));

    testPopulateHostComponentResources();
    testCustomProviders();
    testPopulateResources_HDP2();
    testPopulateResources_HDP2_params();
    testPopulateResources_HDP2_params_singleProperty();
    testPopulateResources_HDP2_params_category();
    testPopulateResources_HDP2_params_category2();
    testPopulateResources_jmx_JournalNode();
    testPopulateResources_jmx_Storm();
    testPopulateResources_NoRegionServer();
    testPopulateResources_HBaseMaster2();
    testPopulateResources_params_category5();
    testPopulateResources_ganglia_JournalNode();
    testPopulateResources_resourcemanager_clustermetrics();
    testPopulateResourcesWithAggregateFunctionMetrics();
  }

  @Test(expected = AuthorizationException.class)
  public void testStackDefinedPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));

    testPopulateHostComponentResources();
    testCustomProviders();
    testPopulateResources_HDP2();
    testPopulateResources_HDP2_params();
    testPopulateResources_HDP2_params_singleProperty();
    testPopulateResources_HDP2_params_category();
    testPopulateResources_HDP2_params_category2();
    testPopulateResources_jmx_JournalNode();
    testPopulateResources_jmx_Storm();
    testPopulateResources_NoRegionServer();
    testPopulateResources_HBaseMaster2();
    testPopulateResources_params_category5();
    testPopulateResources_ganglia_JournalNode();
    testPopulateResources_resourcemanager_clustermetrics();
    testPopulateResourcesWithAggregateFunctionMetrics();
  }

  public void testPopulateHostComponentResources() throws Exception {
    TestJMXProvider tj = new TestJMXProvider(true);
    JMXPropertyProviderTest.TestMetricHostProvider tm = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider sdpp = new StackDefinedPropertyProvider(
        Resource.Type.HostComponent, tj, tm, serviceProvider,
        new CombinedStreamProvider(), "HostRoles/cluster_name",
        "HostRoles/host_name", "HostRoles/component_name", "HostRoles/state",
        null, null);

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty("HostRoles/host_name", "h1");
    resource.setProperty("HostRoles/component_name", "NAMENODE");
    resource.setProperty("HostRoles/state", "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet(), new HashMap<>());

    Set<Resource> set = sdpp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());

    Resource res = set.iterator().next();

    Map<String, Map<String, Object>> values = res.getPropertiesMap();

    Assert.assertTrue("Expected JMX metric 'metrics/dfs/FSNamesystem'", values.containsKey("metrics/dfs/FSNamesystem"));
    Assert.assertTrue("Expected JMX metric 'metrics/dfs/namenode'", values.containsKey("metrics/dfs/namenode"));
    Assert.assertTrue("Expected Ganglia metric 'metrics/rpcdetailed/client'", values.containsKey("metrics/rpcdetailed/client"));
    Assert.assertTrue("Expected Ganglia metric 'metrics/rpc/client'", values.containsKey("metrics/rpc/client"));
  }


  public void testCustomProviders() throws Exception {

    StackDefinedPropertyProvider sdpp = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent, null, null, null, new CombinedStreamProvider(),
      "HostRoles/cluster_name", "HostRoles/host_name", "HostRoles/component_name", "HostRoles/state",
      new EmptyPropertyProvider(), new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty("HostRoles/host_name", "h1");
    resource.setProperty("HostRoles/component_name", "DATANODE");
    resource.setProperty("HostRoles/state", "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet(), new HashMap<>());

    Set<Resource> set = sdpp.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, set.size());

    Resource res = set.iterator().next();

    Map<String, Map<String, Object>> values = res.getPropertiesMap();
    Assert.assertTrue(values.containsKey("foo/type1"));
    Assert.assertTrue(values.containsKey("foo/type2"));
    Assert.assertTrue(values.containsKey("foo/type3"));
    Assert.assertFalse(values.containsKey("foo/type4"));

    Assert.assertTrue(values.get("foo/type1").containsKey("name"));
    Assert.assertTrue(values.get("foo/type2").containsKey("name"));
    Assert.assertTrue(values.get("foo/type3").containsKey("name"));

    Assert.assertEquals("value1", values.get("foo/type1").get("name"));

  }


  private static class CombinedStreamProvider extends URLStreamProvider {

    public CombinedStreamProvider() {
      super(1000, 1000, ComponentSSLConfiguration.instance());
    }

    @Override
    public InputStream readFrom(String spec) throws IOException {
      if (spec.indexOf("jmx") > -1) {
        // jmx
        return ClassLoader.getSystemResourceAsStream("hdfs_namenode_jmx.json");
      } else {
        // ganglia
        return ClassLoader.getSystemResourceAsStream("temporal_ganglia_data.txt");
      }
    }

    @Override
    public InputStream readFrom(String spec, String requestMethod, String params)
      throws IOException {
      return readFrom(spec);
    }
  }

  private static class EmptyPropertyProvider implements PropertyProvider {

    @Override
    public Set<Resource> populateResources(Set<Resource> resources,
                                           Request request, Predicate predicate) throws SystemException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      // TODO Auto-generated method stub
      return null;
    }

  }

  /**
   * Test for empty constructor.  Public since instantiated via reflection.
   */
  public static class CustomMetricProvider1 implements PropertyProvider {
    @Override
    public Set<Resource> populateResources(Set<Resource> resources,
                                           Request request, Predicate predicate) throws SystemException {

      for (Resource r : resources) {
        r.setProperty("foo/type1/name", "value1");
      }

      return resources;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      return Collections.emptySet();
    }
  }

  /**
   * Test map constructors.  Public since instantiated via reflection.
   */
  public static class CustomMetricProvider2 implements PropertyProvider {
    private Map<String, String> providerProperties = null;

    public CustomMetricProvider2(Map<String, String> properties, Map<String, Metric> metrics) {
      providerProperties = properties;
    }

    @Override
    public Set<Resource> populateResources(Set<Resource> resources,
                                           Request request, Predicate predicate) throws SystemException {
      for (Resource r : resources) {
        r.setProperty("foo/type2/name", providerProperties.get("Type2.Metric.Name"));
      }
      return resources;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      return Collections.emptySet();
    }
  }

  /**
   * Test singleton accessor.  Public since instantiated via reflection.
   */
  public static class CustomMetricProvider3 implements PropertyProvider {
    private static CustomMetricProvider3 instance = null;
    private Map<String, String> providerProperties = new HashMap<>();

    public static CustomMetricProvider3 getInstance(Map<String, String> properties, Map<String, Metric> metrics) {
      if (null == instance) {
        instance = new CustomMetricProvider3();
        instance.providerProperties.putAll(properties);
      }
      return instance;
    }

    @Override
    public Set<Resource> populateResources(Set<Resource> resources,
                                           Request request, Predicate predicate) throws SystemException {
      for (Resource r : resources) {
        r.setProperty("foo/type3/name", providerProperties.get("Type3.Metric.Name"));
      }
      return resources;
    }

    @Override
    public Set<String> checkPropertyIds(Set<String> propertyIds) {
      return Collections.emptySet();
    }
  }

  public void testPopulateResources_HDP2() throws Exception {

    URLStreamProvider streamProvider = new TestStreamProvider();
    TestJMXProvider hostProvider = new TestJMXProvider(true);
    JMXPropertyProviderTest.TestMetricHostProvider metricsHostProvider = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      hostProvider,
      metricsHostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    // resourcemanager
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertEquals(6, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersAllocated")));
    Assert.assertEquals(6, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AppsSubmitted")));

    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumActiveNMs")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumDecommissionedNMs")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumLostNMs")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumUnhealthyNMs")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/ClusterMetrics", "NumRebootedNMs")));

    Assert.assertEquals(932118528, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));

    //namenode
    resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
  }

  public void testPopulateResources_HDP2_params() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXProvider hostProvider = new TestJMXProvider(false);
    JMXPropertyProviderTest.TestMetricHostProvider metricsHostProvider = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      hostProvider,
      metricsHostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertEquals(6, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersAllocated")));
    Assert.assertEquals(6, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AppsSubmitted")));

    Assert.assertEquals(15, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersAllocated")));
    Assert.assertEquals(12, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableMB")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableVCores")));
    Assert.assertEquals(47, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AppsSubmitted")));

    Assert.assertEquals(4, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersAllocated")));
    Assert.assertEquals(4, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersReleased")));
    Assert.assertEquals(6048, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableMB")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableVCores")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AppsSubmitted")));
  }


  public void testPopulateResources_HDP2_params_singleProperty() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXProvider hostProvider = new TestJMXProvider(false);
    JMXPropertyProviderTest.TestMetricHostProvider metricsHostProvider = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      hostProvider,
      metricsHostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/yarn/Queue/root/AvailableMB"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
  }

  public void testPopulateResources_HDP2_params_category() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXProvider hostProvider = new TestJMXProvider(false);
    JMXPropertyProviderTest.TestMetricHostProvider metricsHostProvider = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      hostProvider,
      metricsHostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/yarn/Queue"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertEquals(6, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersAllocated")));
    Assert.assertEquals(6, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AppsSubmitted")));

    Assert.assertEquals(15, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersAllocated")));
    Assert.assertEquals(12, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableMB")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableVCores")));
    Assert.assertEquals(47, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AppsSubmitted")));

    Assert.assertEquals(4, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersAllocated")));
    Assert.assertEquals(4, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersReleased")));
    Assert.assertEquals(6048, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableMB")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableVCores")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AppsSubmitted")));
  }

  public void testPopulateResources_HDP2_params_category2() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXProvider hostProvider = new TestJMXProvider(false);
    JMXPropertyProviderTest.TestMetricHostProvider metricsHostProvider = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      hostProvider,
      metricsHostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    Request request = PropertyHelper.getReadRequest(Collections.singleton("metrics/yarn/Queue/root/default"), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/resourcemanager_jmx.json for values
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersAllocated")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AggregateContainersReleased")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableMB")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AvailableVCores")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root", "AppsSubmitted")));

    Assert.assertEquals(15, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersAllocated")));
    Assert.assertEquals(12, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AggregateContainersReleased")));
    Assert.assertEquals(8192, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableMB")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AvailableVCores")));
    Assert.assertEquals(47, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default", "AppsSubmitted")));

    Assert.assertEquals(99, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AggregateContainersAllocated")));
    Assert.assertEquals(98, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AggregateContainersReleased")));
    Assert.assertEquals(9898, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AvailableMB")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AvailableVCores")));
    Assert.assertEquals(97, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/default/sub_queue", "AppsSubmitted")));

    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersAllocated")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AggregateContainersReleased")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableMB")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AvailableVCores")));
    Assert.assertNull(resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/yarn/Queue/root/second_queue", "AppsSubmitted")));
  }

  public void testPopulateResources_jmx_JournalNode() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXProvider hostProvider = new TestJMXProvider(false);
    JMXPropertyProviderTest.TestMetricHostProvider metricsHostProvider = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      hostProvider,
      metricsHostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "JOURNALNODE");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/hdfs_journalnode_jmx.json for values
    Assert.assertEquals(1377795104272L, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "startTime")));
    Assert.assertEquals(954466304, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryMax")));
    Assert.assertEquals(14569736, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "HeapMemoryUsed")));
    Assert.assertEquals(136314880, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryMax")));
    Assert.assertEquals(24993392, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "NonHeapMemoryUsed")));
    Assert.assertEquals(9100, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcCount")));
    Assert.assertEquals(31641, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "gcTimeMillis")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "logError")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "logFatal")));
    Assert.assertEquals(4163, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "logInfo")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "logWarn")));
    Assert.assertEquals(29.8125, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "memHeapCommittedM")));
    Assert.assertEquals(13.894783, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "memHeapUsedM")));
    Assert.assertEquals(24.9375, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "memNonHeapCommittedM")));
    Assert.assertEquals(23.835556, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "memNonHeapUsedM")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsBlocked")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsNew")));
    Assert.assertEquals(6, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsRunnable")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsTerminated")));
    Assert.assertEquals(3, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsTimedWaiting")));
    Assert.assertEquals(8, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/jvm", "threadsWaiting")));

    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "NumOpenConnections")));
    Assert.assertEquals(4928861, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "ReceivedBytes")));
    Assert.assertEquals(13.211112159230245, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "RpcProcessingTime_avg_time")));
    Assert.assertEquals(25067, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "RpcProcessingTime_num_ops")));
    Assert.assertEquals(0.19686821997924706, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "RpcQueueTime_avg_time")));
    Assert.assertEquals(25067, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "RpcQueueTime_num_ops")));
    Assert.assertEquals(6578899, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "SentBytes")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "callQueueLen")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "rpcAuthenticationFailures")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "rpcAuthenticationSuccesses")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "rpcAuthorizationFailures")));
    Assert.assertEquals(12459, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpc", "rpcAuthorizationSuccesses")));

    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "getJournalState_num_ops")));
    Assert.assertEquals(0.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "getJournalState_avg_time")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "newEpoch_num_ops")));
    Assert.assertEquals(60.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "newEpoch_avg_time")));
    Assert.assertEquals(4129, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "startLogSegment_num_ops")));
    Assert.assertEquals(38.25951359084413, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "startLogSegment_avg_time")));
    Assert.assertEquals(8265, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "journal_num_ops")));
    Assert.assertEquals(2.1832618025751187, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "journal_avg_time")));
    Assert.assertEquals(4129, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "finalizeLogSegment_num_ops")));
    Assert.assertEquals(11.575679542203101, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "finalizeLogSegment_avg_time")));
    Assert.assertEquals(8536, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "getEditLogManifest_num_ops")));
    Assert.assertEquals(12.55427859318747, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "getEditLogManifest_avg_time")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "prepareRecovery_num_ops")));
    Assert.assertEquals(10.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "prepareRecovery_avg_time")));
    Assert.assertEquals(1, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "acceptRecovery_num_ops")));
    Assert.assertEquals(30.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/rpcdetailed", "acceptRecovery_avg_time")));

    Assert.assertEquals(0.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/ugi", "loginFailure_avg_time")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/ugi", "loginFailure_num_ops")));
    Assert.assertEquals(0.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/ugi", "loginSuccess_avg_time")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/ugi", "loginSuccess_num_ops")));

    Assert.assertEquals("{\"mycluster\":{\"Formatted\":\"true\"}}", resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode", "journalsStatus")));

    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s_num_ops")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s50thPercentileLatencyMicros")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s75thPercentileLatencyMicros")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s90thPercentileLatencyMicros")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s95thPercentileLatencyMicros")));
    Assert.assertEquals(988, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs60s99thPercentileLatencyMicros")));
    Assert.assertEquals(4, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s_num_ops")));
    Assert.assertEquals(1027, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s50thPercentileLatencyMicros")));
    Assert.assertEquals(1037, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s75thPercentileLatencyMicros")));
    Assert.assertEquals(1037, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s90thPercentileLatencyMicros")));
    Assert.assertEquals(1037, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s95thPercentileLatencyMicros")));
    Assert.assertEquals(1037, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs300s99thPercentileLatencyMicros")));
    Assert.assertEquals(60, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s_num_ops")));
    Assert.assertEquals(1122, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s50thPercentileLatencyMicros")));
    Assert.assertEquals(1344, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s75thPercentileLatencyMicros")));
    Assert.assertEquals(1554, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s90thPercentileLatencyMicros")));
    Assert.assertEquals(1980, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s95thPercentileLatencyMicros")));
    Assert.assertEquals(8442, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "syncs3600s99thPercentileLatencyMicros")));
    Assert.assertEquals(8265, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "batchesWritten")));
    Assert.assertEquals(8265, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "txnsWritten")));
    Assert.assertEquals(107837, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "bytesWritten")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "batchesWrittenWhileLagging")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "lastPromisedEpoch")));
    Assert.assertEquals(2, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "lastWriterEpoch")));
    Assert.assertEquals(0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "currentLagTxns")));
    Assert.assertEquals(8444, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/dfs/journalnode/cluster/mycluster", "lastWrittenTxId")));
  }

  public void testPopulateResources_jmx_Storm() throws Exception {
    // Adjust stack version for cluster
    Cluster cluster = clusters.getCluster("c2");
    cluster.setDesiredStackVersion(new StackId("HDP-2.1.1"));

    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXProvider hostProvider = new TestJMXProvider(false);
    TestGangliaHostProvider gangliaHostProvider = new TestGangliaHostProvider();
    JMXPropertyProviderTest.TestMetricHostProvider metricsHostProvider = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      hostProvider,
      gangliaHostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "STORM_REST_API");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    // see test/resources/storm_rest_api_jmx.json for values
    Assert.assertEquals(28.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "tasks.total")));
    Assert.assertEquals(8.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.total")));
    Assert.assertEquals(5.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.free")));
    Assert.assertEquals(2.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "supervisors")));
    Assert.assertEquals(28.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "executors.total")));
    Assert.assertEquals(3.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "slots.used")));
    Assert.assertEquals(1.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "topologies")));
    Assert.assertEquals(4637.0, resource.getPropertyValue(PropertyHelper.getPropertyId("metrics/api/cluster/summary", "nimbus.uptime")));
  }

  public void testPopulateResources_NoRegionServer() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXProvider hostProvider = new TestJMXProvider(false);
    JMXPropertyProviderTest.TestMetricHostProvider metricsHostProvider = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      hostProvider,
      metricsHostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      null,
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_REGIONSERVER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    int preSize = resource.getPropertiesMap().size();

    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(preSize, resource.getPropertiesMap().size());
  }

  public void testPopulateResources_HBaseMaster2() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider();
    TestJMXProvider hostProvider = new TestJMXProvider(false);
    JMXPropertyProviderTest.TestMetricHostProvider metricsHostProvider = new JMXPropertyProviderTest.TestMetricHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      hostProvider,
      metricsHostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "domu-12-31-39-0e-34-e1.compute-1.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_MASTER");
    resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");


    // request with an empty set should get all supported properties
    Request request = PropertyHelper.getReadRequest(Collections.emptySet());

    Set<Resource> res = propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, res.size());

    Map<String, Map<String, Object>> map = res.iterator().next().getPropertiesMap();

    Assert.assertTrue(map.containsKey("metrics/hbase/master"));
    // uses 'tag.isActiveMaster' (name with a dot)
    Assert.assertTrue(map.get("metrics/hbase/master").containsKey("IsActiveMaster"));
  }

  public void testPopulateResources_params_category5() throws Exception {
    org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider streamProvider =
      new org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider("temporal_ganglia_data_yarn_queues.txt");

    TestJMXProvider jmxHostProvider = new TestJMXProvider(true);
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      jmxHostProvider,
      hostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());


    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "dev01.ambari.apache.org");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");

    String RM_CATEGORY_1 = "metrics/yarn/Queue/root/default";
    String RM_AVAILABLE_MEMORY_PROPERTY = PropertyHelper.getPropertyId(RM_CATEGORY_1, "AvailableMB");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(RM_CATEGORY_1, new TemporalInfoImpl(10L, 20L, 1L));

    Request request = PropertyHelper.getReadRequest(Collections.singleton(RM_CATEGORY_1), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertTrue(PropertyHelper.getProperties(resource).size() > 2);
    Assert.assertNotNull(resource.getPropertyValue(RM_AVAILABLE_MEMORY_PROPERTY));
  }

  public void testPopulateResources_ganglia_JournalNode() throws Exception {
    org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider streamProvider =
      new org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider("journalnode_ganglia_data.txt");

    TestJMXProvider jmxHostProvider = new TestJMXProvider(true);
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      jmxHostProvider,
      hostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());


    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "JOURNALNODE");


    Object[][] testData = {
      {"metrics", "boottime", 1378290058.0},
      {"metrics/cpu", "cpu_aidle", 0.0},
      {"metrics/cpu", "cpu_idle", 88.2},
      {"metrics/cpu", "cpu_nice", 0.0},
      {"metrics/cpu", "cpu_num", 2.0},
      {"metrics/cpu", "cpu_speed", 3583.0},
      {"metrics/cpu", "cpu_system", 8.4},
      {"metrics/cpu", "cpu_user", 3.3},
      {"metrics/cpu", "cpu_wio", 0.1},
      {"metrics/disk", "disk_free", 92.428},
      {"metrics/disk", "disk_total", 101.515},
      {"metrics/disk", "part_max_used", 12.8},
      {"metrics/load", "load_fifteen", 0.026},
      {"metrics/load", "load_five", 0.114},
      {"metrics/load", "load_one", 0.226},
      {"metrics/memory", "mem_buffers", 129384.0},
      {"metrics/memory", "mem_cached", 589576.0},
      {"metrics/memory", "mem_free", 1365496.0},
      {"metrics/memory", "mem_shared", 0.0},
      {"metrics/memory", "mem_total", 4055144.0},
      {"metrics/memory", "swap_free", 4128760.0},
      {"metrics/memory", "swap_total", 4128760.0},
      {"metrics/network", "bytes_in", 22547.48},
      {"metrics/network", "bytes_out", 5772.33},
      {"metrics/network", "pkts_in", 24.0},
      {"metrics/network", "pkts_out", 35.4},
      {"metrics/process", "proc_run", 4.0},
      {"metrics/process", "proc_total", 657.0},
      {"metrics/dfs/journalNode", "batchesWritten", 0.0},
      {"metrics/dfs/journalNode", "batchesWrittenWhileLagging", 0.0},
      {"metrics/dfs/journalNode", "bytesWritten", 0.0},
      {"metrics/dfs/journalNode", "currentLagTxns", 0.0},
      {"metrics/dfs/journalNode", "lastPromisedEpoch", 5.0},
      {"metrics/dfs/journalNode", "lastWriterEpoch", 5.0},
      {"metrics/dfs/journalNode", "lastWrittenTxId", 613.0},
      {"metrics/dfs/journalNode", "syncs60s50thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s75thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s90thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s95thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s99thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs60s_num_ops", 0.0},
      {"metrics/dfs/journalNode", "syncs300s50thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s75thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s90thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s95thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s99thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs300s_num_ops", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s50thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s75thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s90thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s95thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s99thPercentileLatencyMicros", 0.0},
      {"metrics/dfs/journalNode", "syncs3600s_num_ops", 0.0},
      {"metrics/dfs/journalNode", "txnsWritten", 0.0}
    };

    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    Set<String> properties = new LinkedHashSet<>();

    for (Object[] row : testData) {
      properties.add(PropertyHelper.getPropertyId(row[0].toString(), row[1].toString()));
    }

    Request request = PropertyHelper.getReadRequest(properties, temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Map<String, Object> p = PropertyHelper.getProperties(resource);

    for (String key : p.keySet()) {
      if (!properties.contains(key)) {
        System.out.printf(key);
      }
    }

    // size + properties defined before "Object[][] testData ... " above
    Assert.assertEquals(properties.size() + 3, PropertyHelper.getProperties(resource).size());

    int i = 0;
    for (String property : properties) {
      Assert.assertEquals(testData[i++][2], resource.getPropertyValue(property));
    }
  }

  public void testPopulateResources_resourcemanager_clustermetrics() throws Exception {

    String[] metrics = new String[]{
      "metrics/yarn/ClusterMetrics/NumActiveNMs",
      "metrics/yarn/ClusterMetrics/NumDecommissionedNMs",
      "metrics/yarn/ClusterMetrics/NumLostNMs",
      "metrics/yarn/ClusterMetrics/NumUnhealthyNMs",
      "metrics/yarn/ClusterMetrics/NumRebootedNMs"
    };

    org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider streamProvider =
      new org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider("yarn_ganglia_data.txt");

    TestJMXProvider jmxHostProvider = new TestJMXProvider(true);
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
    TestGangliaServiceProvider serviceProvider = new TestGangliaServiceProvider();

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.HostComponent,
      jmxHostProvider,
      hostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());

    for (String metric : metrics) {
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);

      resource.setProperty("HostRoles/cluster_name", "c2");
      resource.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
      resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");

      // only ask for one property
      Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
      temporalInfoMap.put(metric, new TemporalInfoImpl(10L, 20L, 1L));
      Request request = PropertyHelper.getReadRequest(Collections.singleton(metric), temporalInfoMap);

      Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

      Assert.assertEquals(4, PropertyHelper.getProperties(resource).size());
      Assert.assertNotNull(resource.getPropertyValue(metric));

    }

  }

  public void testPopulateResourcesWithAggregateFunctionMetrics() throws Exception {

    String metric = "metrics/rpc/NumOpenConnections._sum";

    org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider streamProvider =
      new org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider("ams/aggregate_component_metric.json");
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestJMXProvider jmxHostProvider = new TestJMXProvider(true);
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();
    MetricsServiceProvider serviceProvider = new MetricsServiceProvider() {
      @Override
      public MetricsService getMetricsServiceType() {
        return MetricsService.TIMELINE_METRICS;
      }
    };

    StackDefinedPropertyProvider propertyProvider = new StackDefinedPropertyProvider(
      Resource.Type.Component,
      jmxHostProvider,
      hostProvider,
      serviceProvider,
      streamProvider,
      PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
      PropertyHelper.getPropertyId("HostRoles", "host_name"),
      PropertyHelper.getPropertyId("HostRoles", "component_name"),
      PropertyHelper.getPropertyId("HostRoles", "state"),
      new EmptyPropertyProvider(),
      new EmptyPropertyProvider());


    Resource resource = new ResourceImpl(Resource.Type.Component);

    resource.setProperty("HostRoles/cluster_name", "c2");
    resource.setProperty("HostRoles/service_name", "HBASE");
    resource.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "HBASE_REGIONSERVER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(metric, new TemporalInfoImpl(1429824611300L, 1429825241400L, 1L));
    Request request = PropertyHelper.getReadRequest(Collections.singleton(metric), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    Assert.assertEquals(4, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(metric));
    Number[][] metricsArray = (Number[][]) resource.getPropertyValue(metric);
    Assert.assertEquals(32, metricsArray.length);
  }

  /* Since streamProviders are not injected this hack becomes necessary */
  private void injectCacheEntryFactoryWithStreamProvider(org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider streamProvider) throws Exception {
    Field field = TimelineMetricCacheEntryFactory.class.getDeclaredField("requestHelperForGets");
    field.setAccessible(true);
    field.set(cacheEntryFactory, new MetricsRequestHelper(streamProvider));
  }

  public static class TestJMXProvider extends JMXPropertyProviderTest.TestJMXHostProvider {

    public TestJMXProvider(boolean unknownPort) {
      super(unknownPort);
    }

    @Override
    public String getJMXRpcMetricTag(String clusterName, String componentName, String port) {
      return "8020".equals(port) ? "client" : null;
    }
  }
}
