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
package org.apache.ambari.server.controller.metrics.timeline;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
import org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCache;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheEntryFactory;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.http.client.utils.URIBuilder;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.context.SecurityContextHolder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AMSPropertyProvider.class, AmbariServer.class})
@PowerMockIgnore({"javax.xml.parsers.*", "org.xml.sax.*", "net.sf.ehcache.*", "org.apache.log4j.*"})
public class AMSPropertyProviderTest {
  private static final String PROPERTY_ID1 = PropertyHelper.getPropertyId("metrics/cpu", "cpu_user");
  private static final String PROPERTY_ID2 = PropertyHelper.getPropertyId("metrics/memory", "mem_free");
  private static final String PROPERTY_ID3 = PropertyHelper.getPropertyId("metrics/dfs/datanode", "blocks_replicated");
  private static final String PROPERTY_ID4 = PropertyHelper.getPropertyId("metrics/dfs/datanode", "blocks_removed");
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");

  private static final String FILE_PATH_PREFIX = "ams" + File.separator;
  private static final String SINGLE_HOST_METRICS_FILE_PATH = FILE_PATH_PREFIX + "single_host_metric.json";
  private static final String MULTIPLE_HOST_METRICS_FILE_PATH = FILE_PATH_PREFIX + "multiple_host_metrics.json";
  private static final String SINGLE_COMPONENT_METRICS_FILE_PATH = FILE_PATH_PREFIX + "single_component_metrics.json";
  private static final String MULTIPLE_COMPONENT_REGEXP_METRICS_FILE_PATH = FILE_PATH_PREFIX + "multiple_component_regexp_metrics.json";
  private static final String EMBEDDED_METRICS_FILE_PATH = FILE_PATH_PREFIX + "embedded_host_metric.json";
  private static final String AGGREGATE_METRICS_FILE_PATH = FILE_PATH_PREFIX + "aggregate_component_metric.json";

  private static TimelineMetricCacheEntryFactory cacheEntryFactory;

  @Before
  public void setupCache() {
    cacheEntryFactory = new TimelineMetricCacheEntryFactory(new Configuration());
    InternalAuthenticationToken authenticationToken = new InternalAuthenticationToken("admin");
    authenticationToken.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
  }

  //    SecurityContextHolder.getContext().setAuthentication(null);

  @Test
  public void testRbacForAMSPropertyProvider() throws Exception {

    SecurityContextHolder.getContext().setAuthentication(null);
    //Cluster Administrator
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));
    SecurityContextHolder.getContext();
    testPopulateResourcesForSingleHostMetric();

    SecurityContextHolder.getContext().setAuthentication(null);
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));
    SecurityContextHolder.getContext();
    testPopulateResourcesForSingleHostMetricPointInTime();

    SecurityContextHolder.getContext().setAuthentication(null);
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));
    SecurityContextHolder.getContext();
    try {
      testPopulateResourcesForMultipleHostMetricscPointInTime();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof AuthorizationException);
    }
  }

  @Test
  public void testAMSPropertyProviderAsViewUser() throws Exception {
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));

    try {
      testPopulateResourcesForSingleHostMetric();
      Assert.fail();
    } catch (AuthorizationException ignored) {
    }

    try {
      testPopulateResourcesForSingleHostMetric();
      Assert.fail();
    } catch (AuthorizationException ignored) {
    }

    try {
      testPopulateResourcesForMultipleHostMetrics();
      Assert.fail();
    } catch (AuthorizationException ignored) {
    }

    try {
      testPopulateResourcesForSingleComponentMetric();
      Assert.fail();
    } catch (AuthorizationException ignored) {
    }

    try {
      testAggregateFunctionForComponentMetrics();
      Assert.fail();
    } catch (AuthorizationException ignored) {
    }

    try {
      testPopulateResourcesForHostComponentHostMetrics();
      Assert.fail();
    } catch (AuthorizationException ignored) {
    }

    try {
      testPopulateResourcesForHostComponentMetricsForMultipleHosts();
      Assert.fail();
    } catch (AuthorizationException ignored) {
    }

    try {
      testPopulateResourcesHostBatches();
      Assert.fail();
    } catch (AuthorizationException ignored) {
    }

    try {
      testPopulateResourcesForMultipleComponentsMetric();
      Assert.fail();
    } catch (AuthorizationException ignored) {
    }

  }

  @Test
  public void testPopulateResourcesForSingleHostMetric() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_HOST_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(PROPERTY_ID1, new TemporalInfoImpl(1416445244800L, 1416448936474L, 15L));
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID1), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "cpu_user");
    uriBuilder.addParameter("hostname", "h1");
    uriBuilder.addParameter("appId", "HOST");
    uriBuilder.addParameter("startTime", "1416445244800");
    uriBuilder.addParameter("endTime", "1416448936474");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertNotNull("No value for property " + PROPERTY_ID1, val);
    Assert.assertEquals(111, val.length);
  }

  @Test
  public void testPopulateResourcesForSingleHostMetricPointInTime() throws Exception {
    setUpCommonMocks();

    // given
    TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_HOST_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    Map<String, TemporalInfo> temporalInfoMap = Collections.emptyMap();
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID1), temporalInfoMap);

    // when
    Set<Resource> resources = propertyProvider.populateResources(Collections.singleton(resource), request, null);

    // then
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(res);
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "cpu_user");
    uriBuilder.addParameter("hostname", "h1");
    uriBuilder.addParameter("appId", "HOST");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Double val = (Double) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertEquals(41.088, val, 0.001);
  }

  @Test
  public void testPopulateResourcesForMultipleHostMetricscPointInTime() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(MULTIPLE_HOST_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    Map<String, TemporalInfo> temporalInfoMap = Collections.emptyMap();
    Request request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{
        add(PROPERTY_ID1);
        add(PROPERTY_ID2);
      }}, temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "cpu_user,mem_free");
    uriBuilder.addParameter("hostname", "h1");
    uriBuilder.addParameter("appId", "HOST");

    URIBuilder uriBuilder2 = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder2.addParameter("metricNames", "mem_free,cpu_user");
    uriBuilder2.addParameter("hostname", "h1");
    uriBuilder2.addParameter("appId", "HOST");
    Assert.assertTrue(uriBuilder.toString().equals(streamProvider.getLastSpec())
      || uriBuilder2.toString().equals(streamProvider.getLastSpec()));
    Double val1 = (Double) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertNotNull("No value for property " + PROPERTY_ID1, val1);
    Assert.assertEquals(41.088, val1, 0.001);
    Double val2 = (Double) res.getPropertyValue(PROPERTY_ID2);
    Assert.assertNotNull("No value for property " + PROPERTY_ID2, val2);
    Assert.assertEquals(2.47025664E8, val2, 0.1);
  }

  @Test
  public void testPopulateResourcesForMultipleHostMetrics() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(MULTIPLE_HOST_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(PROPERTY_ID1, new TemporalInfoImpl(1416445244701L, 1416448936564L, 15L));
    temporalInfoMap.put(PROPERTY_ID2, new TemporalInfoImpl(1416445244701L, 1416448936564L, 15L));
    Request request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{
        add(PROPERTY_ID1);
        add(PROPERTY_ID2);
        add("params/padding/NONE"); // Ignore padding to match result size
      }}, temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder1 = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder1.addParameter("metricNames", "cpu_user,mem_free");
    uriBuilder1.addParameter("hostname", "h1");
    uriBuilder1.addParameter("appId", "HOST");
    uriBuilder1.addParameter("startTime", "1416445244701");
    uriBuilder1.addParameter("endTime", "1416448936564");

    URIBuilder uriBuilder2 = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder2.addParameter("metricNames", "mem_free,cpu_user");
    uriBuilder2.addParameter("hostname", "h1");
    uriBuilder2.addParameter("appId", "HOST");
    uriBuilder2.addParameter("startTime", "1416445244701");
    uriBuilder2.addParameter("endTime", "1416448936564");

    List<String> allSpecs = new ArrayList<>(streamProvider.getAllSpecs());
    Assert.assertEquals(1, allSpecs.size());
    Assert.assertTrue(uriBuilder1.toString().equals(allSpecs.get(0))
      || uriBuilder2.toString().equals(allSpecs.get(0)));
    Number[][] val = (Number[][]) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertEquals(111, val.length);
    val = (Number[][]) res.getPropertyValue(PROPERTY_ID2);
    Assert.assertEquals(86, val.length);
  }

  @Test
  public void testPopulateResourcesForRegexpMetrics() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(MULTIPLE_COMPONENT_REGEXP_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds =
      new HashMap<String, Map<String, PropertyInfo>>() {{
        put("RESOURCEMANAGER", new HashMap<String, PropertyInfo>() {{
          put("metrics/yarn/Queue/$1.replaceAll(\"([.])\",\"/\")/AvailableMB",
            new PropertyInfo("yarn.QueueMetrics.Queue=(.+).AvailableMB", true, false));
        }});
      }};

    AMSPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );


    String propertyId1 = "metrics/yarn/Queue/root/AvailableMB";
    Resource resource = new ResourceImpl(Resource.Type.Component);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");// should be set?
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "RESOURCEMANAGER");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(propertyId1, new TemporalInfoImpl(1416528759233L, 1416531129231L, 1L));
    Request request = PropertyHelper.getReadRequest(
      Collections.singleton(propertyId1), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "yarn.QueueMetrics.Queue=root.AvailableMB");
    uriBuilder.addParameter("appId", "RESOURCEMANAGER");
    uriBuilder.addParameter("startTime", "1416528759233");
    uriBuilder.addParameter("endTime", "1416531129231");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue("metrics/yarn/Queue/root/AvailableMB");
    Assert.assertNotNull("No value for property metrics/yarn/Queue/root/AvailableMB", val);
    Assert.assertEquals(238, val.length);
  }

  @Test
  public void testPopulateResourcesForSingleComponentMetric() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_COMPONENT_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds =
      PropertyHelper.getMetricPropertyIds(Resource.Type.Component);

    AMSPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    String propertyId = PropertyHelper.getPropertyId("metrics/rpc", "RpcQueueTime_avg_time");
    Resource resource = new ResourceImpl(Resource.Type.Component);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "NAMENODE");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(propertyId, new TemporalInfoImpl(1416528759233L, 1416531129231L, 1L));
    Request request = PropertyHelper.getReadRequest(
      Collections.singleton(propertyId), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "rpc.rpc.RpcQueueTimeAvgTime");
    uriBuilder.addParameter("appId", "NAMENODE");
    uriBuilder.addParameter("startTime", "1416528759233");
    uriBuilder.addParameter("endTime", "1416531129231");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue(propertyId);
    Assert.assertNotNull("No value for property " + propertyId, val);
    Assert.assertEquals(238, val.length);
  }

  @Test
  public void testPopulateResourcesForMultipleComponentsMetric() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_COMPONENT_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();
    Map<String, Map<String, PropertyInfo>> propertyIds =
            PropertyHelper.getMetricPropertyIds(Resource.Type.Component);

    AMSPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
            propertyIds,
            streamProvider,
            sslConfiguration,
            cacheProviderMock,
            metricHostProvider,
            CLUSTER_NAME_PROPERTY_ID,
            COMPONENT_NAME_PROPERTY_ID
    );
    Set<String> requestedPropertyIds = new HashSet<>(Arrays.asList("metrics/hbase/master", "metrics/cpu/cpu_wio"));
    Resource resource = new ResourceImpl(Resource.Type.Component);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "METRICS_COLLECTOR");
    Resource namenodeResource = new ResourceImpl(Resource.Type.Component);
    namenodeResource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    namenodeResource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    namenodeResource.setProperty(COMPONENT_NAME_PROPERTY_ID, "NAMENODE");

    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    for (String propertyId : requestedPropertyIds) {
      temporalInfoMap.put(propertyId, new TemporalInfoImpl(1416528759233L, 1416531129231L, 1L));
    }
    Request request = PropertyHelper.getReadRequest(
            requestedPropertyIds, temporalInfoMap);
    Set<Resource> resources =
            propertyProvider.populateResources(new HashSet<>(Arrays.asList(resource, namenodeResource)), request, null);
    Assert.assertEquals(2, resources.size());
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "cpu_wio");
    uriBuilder.addParameter("appId", "NAMENODE");
    uriBuilder.addParameter("startTime", "1416528759233");
    uriBuilder.addParameter("endTime", "1416531129231");
    Assert.assertTrue(streamProvider.getAllSpecs().contains(uriBuilder.toString()));
    List<String> allSpecs = new ArrayList<>(streamProvider.getAllSpecs());
    Assert.assertEquals(2, allSpecs.size());
  }

  @Test
  public void testPopulateMetricsForEmbeddedHBase() throws Exception {
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(amc).anyTimes();
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("HostRoles/cluster_name")).andReturn(cluster).anyTimes();
    expect(cluster.getResourceId()).andReturn(2L).anyTimes();

    StackId stackId = new StackId("HDP", "2.2");
    try {
      expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }

    Service amsService = createNiceMock(Service.class);
    expect(amsService.getDesiredStackId()).andReturn(stackId);
    expect(amsService.getName()).andReturn("AMS");
    expect(cluster.getServiceByComponentName("METRICS_COLLECTOR")).andReturn(amsService);

    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(amc.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(ambariMetaInfo.getComponentToService("HDP", "2.2", "METRICS_COLLECTOR")).andReturn("AMS").anyTimes();
    expect(ambariMetaInfo.getComponent("HDP", "2.2", "AMS", "METRICS_COLLECTOR"))
      .andReturn(componentInfo).anyTimes();
    expect(componentInfo.getTimelineAppid()).andReturn("AMS-HBASE");
    replay(amc, clusters, cluster, amsService, ambariMetaInfo, componentInfo);
    PowerMock.replayAll();

    TestStreamProvider streamProvider = new TestStreamProvider(EMBEDDED_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds =
      PropertyHelper.getMetricPropertyIds(Resource.Type.Component);

    AMSPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    String propertyId = PropertyHelper.getPropertyId("metrics/hbase/regionserver", "requests");
    Resource resource = new ResourceImpl(Resource.Type.Component);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "METRICS_COLLECTOR");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(propertyId, new TemporalInfoImpl(1421694000L, 1421697600L, 1L));
    Request request = PropertyHelper.getReadRequest(
      Collections.singleton(propertyId), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "regionserver.Server.totalRequestCount");
    uriBuilder.addParameter("appId", "AMS-HBASE");
    uriBuilder.addParameter("startTime", "1421694000");
    uriBuilder.addParameter("endTime", "1421697600");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue(propertyId);
    Assert.assertEquals(189, val.length);
  }

  @Test
  public void testAggregateFunctionForComponentMetrics() throws Exception {
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(amc).anyTimes();
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    StackId stackId = new StackId("HDP", "2.2");
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("HostRoles/cluster_name")).andReturn(cluster).anyTimes();
    expect(cluster.getResourceId()).andReturn(2L).anyTimes();

    try {
      expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }

    Service hbaseService = createNiceMock(Service.class);
    expect(hbaseService.getDesiredStackId()).andReturn(stackId);
    expect(hbaseService.getName()).andReturn("HBASE");
    expect(cluster.getServiceByComponentName("HBASE_REGIONSERVER")).andReturn(hbaseService);


    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(amc.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(ambariMetaInfo.getComponentToService("HDP", "2.2", "HBASE_REGIONSERVER")).andReturn("HBASE").anyTimes();
    expect(ambariMetaInfo.getComponent("HDP", "2.2", "HBASE", "HBASE_REGIONSERVER"))
      .andReturn(componentInfo).anyTimes();
    expect(componentInfo.getTimelineAppid()).andReturn("HBASE");
    replay(amc, clusters, cluster, hbaseService, ambariMetaInfo, componentInfo);
    PowerMock.replayAll();

    TestStreamProvider streamProvider = new TestStreamProvider(AGGREGATE_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds =
      PropertyHelper.getMetricPropertyIds(Resource.Type.Component);
    PropertyHelper.updateMetricsWithAggregateFunctionSupport(propertyIds);

    AMSComponentPropertyProvider propertyProvider = new AMSComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    String propertyId = PropertyHelper.getPropertyId("metrics/rpc", "NumOpenConnections._sum");
    Resource resource = new ResourceImpl(Resource.Type.Component);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "HBASE_REGIONSERVER");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(propertyId, new TemporalInfoImpl(1429824611300L, 1429825241400L, 1L));
    Request request = PropertyHelper.getReadRequest(
      Collections.singleton(propertyId), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "rpc.rpc.NumOpenConnections._sum");
    uriBuilder.addParameter("appId", "HBASE");
    uriBuilder.addParameter("startTime", "1429824611300");
    uriBuilder.addParameter("endTime", "1429825241400");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue(propertyId);
    Assert.assertEquals(32, val.length);
  }

  @Test
  public void testFilterOutOfBandMetricData() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_HOST_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);
    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    // Chopped a section in the middle
    temporalInfoMap.put(PROPERTY_ID1, new TemporalInfoImpl(1416446744801L, 1416447224801L, 1L));
    Request request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID1), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "cpu_user");
    uriBuilder.addParameter("hostname", "h1");
    uriBuilder.addParameter("appId", "HOST");
    uriBuilder.addParameter("startTime", "1416446744801");
    uriBuilder.addParameter("endTime", "1416447224801");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertNotNull("No value for property " + PROPERTY_ID1, val);
    // 4 entries fit into the default allowance limit
    Assert.assertEquals(25, val.length);
  }

  static class TestStreamProviderForHostComponentHostMetricsTest extends TestStreamProvider {
    String hostMetricFilePath = FILE_PATH_PREFIX + "single_host_metric.json";
    String hostComponentMetricFilePath = FILE_PATH_PREFIX + "single_host_component_metrics.json";


    public TestStreamProviderForHostComponentHostMetricsTest(String fileName) {
      super(fileName);
    }

    @Override
    public InputStream readFrom(String spec) throws IOException {
      if (spec.contains("HOST")) {
        this.fileName = hostMetricFilePath;
      } else {
        this.fileName = hostComponentMetricFilePath;
      }

      specs.add(spec);

      return super.readFrom(spec);
    }
  }

  @Test
  public void testPopulateResourcesForHostComponentHostMetrics() throws Exception {
    setUpCommonMocks();
    TestStreamProviderForHostComponentHostMetricsTest streamProvider =
      new TestStreamProviderForHostComponentHostMetricsTest(null);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);
    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    AMSPropertyProvider propertyProvider = new AMSHostComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    // Set same time ranges to make sure the query comes in as grouped and
    // then turns into a separate query to the backend
    temporalInfoMap.put(PROPERTY_ID1, new TemporalInfoImpl(1416445244801L, 1416448936464L, 1L));
    temporalInfoMap.put(PROPERTY_ID3, new TemporalInfoImpl(1416445244801L, 1416448936464L, 1L));
    Request request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{
        add(PROPERTY_ID1);
        add(PROPERTY_ID3);
        add("params/padding/NONE"); // Ignore padding to match result size
      }},
      temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);

    Set<String> specs = streamProvider.getAllSpecs();
    Assert.assertEquals(2, specs.size());
    String hostMetricSpec = null;
    String hostComponentMetricsSpec = null;
    for (String spec : specs) {
      if (spec.contains("HOST")) {
        hostMetricSpec = spec;
      } else {
        hostComponentMetricsSpec = spec;
      }
    }
    Assert.assertNotNull(hostMetricSpec);
    Assert.assertNotNull(hostComponentMetricsSpec);
    // Verify calls
    URIBuilder uriBuilder1 = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder1.addParameter("metricNames", "dfs.datanode.BlocksReplicated");
    uriBuilder1.addParameter("hostname", "h1");
    uriBuilder1.addParameter("appId", "DATANODE");
    uriBuilder1.addParameter("startTime", "1416445244801");
    uriBuilder1.addParameter("endTime", "1416448936464");
    Assert.assertEquals(uriBuilder1.toString(), hostComponentMetricsSpec);

    URIBuilder uriBuilder2 = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder2.addParameter("metricNames", "cpu_user");
    uriBuilder2.addParameter("hostname", "h1");
    uriBuilder2.addParameter("appId", "HOST");
    uriBuilder2.addParameter("startTime", "1416445244801");
    uriBuilder2.addParameter("endTime", "1416448936464");
    Assert.assertEquals(uriBuilder2.toString(), hostMetricSpec);

    Number[][] val = (Number[][]) res.getPropertyValue(PROPERTY_ID1);
    Assert.assertEquals(111, val.length);
    val = (Number[][]) res.getPropertyValue(PROPERTY_ID3);
    Assert.assertNotNull("No value for property " + PROPERTY_ID3, val);
    Assert.assertEquals(8, val.length);
  }

  static class TestStreamProviderForHostComponentMultipleHostsMetricsTest extends TestStreamProvider {
    String hostComponentMetricFilePath_h1 = FILE_PATH_PREFIX + "single_host_component_metrics_h1.json";
    String hostComponentMetricFilePath_h2 = FILE_PATH_PREFIX + "single_host_component_metrics_h2.json";

    public TestStreamProviderForHostComponentMultipleHostsMetricsTest(String fileName) {
      super(fileName);
    }

    @Override
    public InputStream readFrom(String spec) throws IOException {
      if (spec.contains("h1")) {
        this.fileName = hostComponentMetricFilePath_h1;
      } else {
        this.fileName = hostComponentMetricFilePath_h2;
      }

      specs.add(spec);

      return super.readFrom(spec);
    }
  }

  @Test
  public void testPopulateResourcesHostBatches() throws Exception {
    setUpCommonMocks();
    TestStreamProviderForHostComponentMultipleHostsMetricsTest streamProvider =
            new TestStreamProviderForHostComponentMultipleHostsMetricsTest(null);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider("h1");
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);

    Set<Resource> resources = new HashSet<>();
    //value "100" should be equal to AMSPropertyProvider.HOST_NAMES_BATCH_REQUEST_SIZE
    for (int i = 0; i < 100 + 1; i++) {
      Resource resource = new ResourceImpl(Resource.Type.Host);
      resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
      resource.setProperty(HOST_NAME_PROPERTY_ID, "h" + i);
      resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");

      resources.add(resource);
    }

    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(PROPERTY_ID4, new TemporalInfoImpl(1416445244801L, 1416448936464L, 1L));
    Request request = PropertyHelper.getReadRequest(
            new HashSet<String>() {{
              add(PROPERTY_ID4);
              add("params/padding/NONE"); // Ignore padding to match result size
            }},
            temporalInfoMap);

    AMSPropertyProvider propertyProvider = new AMSHostComponentPropertyProvider(
            propertyIds,
            streamProvider,
            sslConfiguration,
            cacheProviderMock,
            metricHostProvider,
            CLUSTER_NAME_PROPERTY_ID,
            HOST_NAME_PROPERTY_ID,
            COMPONENT_NAME_PROPERTY_ID
    );

    Set<Resource> resources1 =
            propertyProvider.populateResources(resources, request, null);
    List<String> allSpecs = new ArrayList<>(streamProvider.getAllSpecs());
    Assert.assertEquals(2, allSpecs.size());
  }

  @Test
  public void testPopulateResourcesForHostComponentMetricsForMultipleHosts() throws Exception {
    setUpCommonMocks();
    TestStreamProviderForHostComponentMultipleHostsMetricsTest streamProvider =
      new TestStreamProviderForHostComponentMultipleHostsMetricsTest(null);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider("h1");
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    TimelineMetricCacheProvider cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    TimelineMetricCache cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);

    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();

    temporalInfoMap.put(PROPERTY_ID4, new TemporalInfoImpl(1416445244801L, 1416448936464L, 1L));
    Request request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{
        add(PROPERTY_ID4);
        add("params/padding/NONE"); // Ignore padding to match result size
      }},
      temporalInfoMap);

    AMSPropertyProvider propertyProvider = new AMSHostComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    Set<Resource> resources1 =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources1.size());
    Resource res1 = resources1.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources1.iterator().next());
    Assert.assertNotNull(properties);

    /////////////////////////////////////
    metricHostProvider = new TestMetricHostProvider("h2");
    cacheProviderMock = EasyMock.createMock(TimelineMetricCacheProvider.class);
    cacheMock = EasyMock.createMock(TimelineMetricCache.class);
    expect(cacheProviderMock.getTimelineMetricsCache()).andReturn(cacheMock).anyTimes();

    resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "h2");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");
    temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(PROPERTY_ID4, new TemporalInfoImpl(1416445244801L, 1416448936464L, 1L));
    request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{
        add(PROPERTY_ID4);
        add("params/padding/NONE"); // Ignore padding to match result size
      }},
      temporalInfoMap);

    propertyProvider = new AMSHostComponentPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      cacheProviderMock,
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID
    );

    resource.setProperty(HOST_NAME_PROPERTY_ID, "h2");

    Set<Resource> resources2 =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources2.size());
    Resource res2 = resources2.iterator().next();
    properties = PropertyHelper.getProperties(resources2.iterator().next());
    Assert.assertNotNull(properties);

    Set<String> specs = streamProvider.getAllSpecs();
    Assert.assertEquals(2, specs.size());

    URIBuilder uriBuilder1 = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    Number[][] val;

    for (String spec : specs) {
      Assert.assertNotNull(spec);

      if (spec.contains("h2")) {
        uriBuilder1.setParameter("metricNames", "dfs.datanode.blocks_removed");
        uriBuilder1.setParameter("hostname", "h2");
        uriBuilder1.setParameter("appId", "DATANODE");
        uriBuilder1.setParameter("startTime", "1416445244801");
        uriBuilder1.setParameter("endTime", "1416448936464");
        Assert.assertEquals(uriBuilder1.toString(), spec);
        val = (Number[][]) res2.getPropertyValue(PROPERTY_ID4);
        Assert.assertNotNull("No value for property " + PROPERTY_ID4, val);
        Assert.assertEquals(9, val.length);
      } else {
        uriBuilder1.setParameter("metricNames", "dfs.datanode.blocks_removed");
        uriBuilder1.setParameter("hostname", "h1");
        uriBuilder1.setParameter("appId", "DATANODE");
        uriBuilder1.setParameter("startTime", "1416445244801");
        uriBuilder1.setParameter("endTime", "1416448936464");
        Assert.assertEquals(uriBuilder1.toString(), spec);
        val = (Number[][]) res1.getPropertyValue(PROPERTY_ID4);
        Assert.assertNotNull("No value for property " + PROPERTY_ID4, val);
        Assert.assertEquals(8, val.length);
      }
    }
  }

  @Test
  public void testSocketTimeoutExceptionBehavior() throws Exception {
    setUpCommonMocks();

    SecurityContextHolder.getContext().setAuthentication(
      TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));

    URLStreamProvider streamProvider = createNiceMock(URLStreamProvider.class);
    HttpURLConnection connection = createNiceMock(HttpURLConnection.class);

    expect(streamProvider.processURL((String) anyObject(), (String) anyObject(),
      (String) anyObject(),  (Map<String, List<String>>) anyObject())).andReturn(connection);

    expect(connection.getInputStream()).andThrow(
      new SocketTimeoutException("Unit test raising Exception")).once();

    replay(streamProvider, connection);

    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.Host);

    AMSPropertyProvider propertyProvider = new AMSHostPropertyProvider(
      propertyIds,
      streamProvider,
      sslConfiguration,
      new TimelineMetricCacheProvider(new Configuration(), cacheEntryFactory),
      metricHostProvider,
      CLUSTER_NAME_PROPERTY_ID,
      HOST_NAME_PROPERTY_ID
    );

    final Resource resource1 = new ResourceImpl(Resource.Type.Host);
    resource1.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource1.setProperty(HOST_NAME_PROPERTY_ID, "h1");
    final Resource resource2 = new ResourceImpl(Resource.Type.Host);
    resource2.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource2.setProperty(HOST_NAME_PROPERTY_ID, "h2");

    // Separating temporal info to ensure multiple requests made
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(PROPERTY_ID1, new TemporalInfoImpl(1416445244801L, 1416448936464L, 1L));
    temporalInfoMap.put(PROPERTY_ID2, new TemporalInfoImpl(1416445344901L, 1416448946564L, 1L));

    Request request = PropertyHelper.getReadRequest(
      new HashSet<String>() {{
        add(PROPERTY_ID1);
        add(PROPERTY_ID2);
      }}, temporalInfoMap);

    Set<Resource> resources =
      propertyProvider.populateResources(
        new HashSet<Resource>() {{ add(resource1); add(resource2); }}, request, null);

    verify(streamProvider, connection);

    Assert.assertEquals(2, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    Assert.assertNull(res.getPropertyValue(PROPERTY_ID1));
    Assert.assertNull(res.getPropertyValue(PROPERTY_ID2));
  }

  public static class TestMetricHostProvider implements MetricHostProvider {

    private String hostName;

    public TestMetricHostProvider() {
    }

    public TestMetricHostProvider(String hostName) {
      this.hostName = hostName;
    }

    @Override
    public String getCollectorHostName(String clusterName, MetricsService service)
      throws SystemException {
      return "localhost";
    }

    @Override
    public String getHostName(String clusterName, String componentName) throws SystemException {
      return (hostName != null) ? hostName : "h1";
    }

    @Override
    public String getCollectorPort(String clusterName, MetricsService service) throws SystemException {
      return "6188";
    }

    @Override
    public boolean isCollectorHostLive(String clusterName, MetricsService service) throws SystemException {
      return true;
    }

    @Override
    public boolean isCollectorComponentLive(String clusterName, MetricsService service) throws SystemException {
      return true;
    }

    @Override
    public boolean isCollectorHostExternal(String clusterName) {
      return false;
    }
  }

  // Helper function to setup common Mocks.
  private void setUpCommonMocks() throws AmbariException {
    AmbariManagementController ams = createNiceMock(AmbariManagementController.class);
    PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(ams).anyTimes();
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    StackId stackId= new StackId("HDP","2.2");
    expect(ams.getClusters()).andReturn(clusters).anyTimes();
    try {
      expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
    } catch (AmbariException e) {
      e.printStackTrace();
    }
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(ams.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(ambariMetaInfo.getComponentToService(anyObject(String.class),
      anyObject(String.class), anyObject(String.class))).andReturn("HDFS").anyTimes();
    expect(ambariMetaInfo.getComponent(anyObject(String.class), anyObject(String.class),
      anyObject(String.class), anyObject(String.class)))
      .andReturn(componentInfo).anyTimes();
    expect(componentInfo.getTimelineAppid()).andReturn(null).anyTimes();

    replay(ams, clusters, cluster, ambariMetaInfo, componentInfo);
    PowerMock.replayAll();
  }

  /* Since streamProviders are not injected this hack becomes necessary */
  private void injectCacheEntryFactoryWithStreamProvider(URLStreamProvider streamProvider) throws Exception {
    Field field = TimelineMetricCacheEntryFactory.class.getDeclaredField("requestHelperForGets");
    field.setAccessible(true);
    field.set(cacheEntryFactory, new MetricsRequestHelper(streamProvider));
  }
}
