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
package org.apache.ambari.server.controller.metrics.timeline;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.metrics.ganglia.TestStreamProvider;
import org.apache.ambari.server.controller.metrics.timeline.AMSPropertyProviderTest.TestMetricHostProvider;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheEntryFactory;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AMSReportPropertyProviderTest {
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Clusters", "cluster_name");
  private static final String FILE_PATH_PREFIX = "ams" + File.separator;
  private static final String SINGLE_HOST_METRICS_FILE_PATH = FILE_PATH_PREFIX + "single_host_metric.json";
  private static final String AGGREGATE_CLUSTER_METRICS_FILE_PATH = FILE_PATH_PREFIX + "aggregate_cluster_metrics.json";

  private static TimelineMetricCacheEntryFactory cacheEntryFactory;
  private static TimelineMetricCacheProvider cacheProvider;

  @BeforeClass
  public static void setupCache() {
    cacheEntryFactory = new TimelineMetricCacheEntryFactory(new Configuration());
    cacheProvider = new TimelineMetricCacheProvider(new Configuration(), cacheEntryFactory);
  }

  @Test
  public void testPopulateResources() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider(SINGLE_HOST_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds =
      PropertyHelper.getMetricPropertyIds(Resource.Type.Cluster);

    AMSReportPropertyProvider propertyProvider =
      new AMSReportPropertyProvider(
        propertyIds,
        streamProvider,
        sslConfiguration,
        cacheProvider,
        metricHostProvider,
        CLUSTER_NAME_PROPERTY_ID
    );

    String propertyId = PropertyHelper.getPropertyId("metrics/cpu", "User");
    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(propertyId, new TemporalInfoImpl(1416445244800L, 1416448936474L, 1L));
    Request request = PropertyHelper.getReadRequest(
      Collections.singleton(propertyId), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "cpu_user");
    uriBuilder.addParameter("appId", "HOST");
    uriBuilder.addParameter("startTime", "1416445244800");
    uriBuilder.addParameter("endTime", "1416448936474");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue("metrics/cpu/User");
    Assert.assertEquals(111, val.length);
  }

  @Test
  public void testPopulateResourceWithAggregateFunction() throws Exception {
    TestStreamProvider streamProvider = new TestStreamProvider(AGGREGATE_CLUSTER_METRICS_FILE_PATH);
    injectCacheEntryFactoryWithStreamProvider(streamProvider);
    TestMetricHostProvider metricHostProvider = new TestMetricHostProvider();
    ComponentSSLConfiguration sslConfiguration = mock(ComponentSSLConfiguration.class);

    Map<String, Map<String, PropertyInfo>> propertyIds =
      PropertyHelper.getMetricPropertyIds(Resource.Type.Cluster);

    AMSReportPropertyProvider propertyProvider =
      new AMSReportPropertyProvider(
        propertyIds,
        streamProvider,
        sslConfiguration,
        cacheProvider,
        metricHostProvider,
        CLUSTER_NAME_PROPERTY_ID
    );

    String propertyId = PropertyHelper.getPropertyId("metrics/cpu", "User._sum");
    Resource resource = new ResourceImpl(Resource.Type.Cluster);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(propertyId, new TemporalInfoImpl(1432033257812L, 1432035927922L, 1L));
    Request request = PropertyHelper.getReadRequest(
      Collections.singleton(propertyId), temporalInfoMap);
    Set<Resource> resources =
      propertyProvider.populateResources(Collections.singleton(resource), request, null);
    Assert.assertEquals(1, resources.size());
    Resource res = resources.iterator().next();
    Map<String, Object> properties = PropertyHelper.getProperties(resources.iterator().next());
    Assert.assertNotNull(properties);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder("localhost", 6188, false);
    uriBuilder.addParameter("metricNames", "cpu_user._sum");
    uriBuilder.addParameter("appId", "HOST");
    uriBuilder.addParameter("startTime", "1432033257812");
    uriBuilder.addParameter("endTime", "1432035927922");
    Assert.assertEquals(uriBuilder.toString(), streamProvider.getLastSpec());
    Number[][] val = (Number[][]) res.getPropertyValue("metrics/cpu/User._sum");
    Assert.assertEquals(90, val.length);
  }

  /* Since streamProviders are not injected this hack becomes necessary */
  private void injectCacheEntryFactoryWithStreamProvider(URLStreamProvider streamProvider) throws Exception {
    Field field = TimelineMetricCacheEntryFactory.class.getDeclaredField("requestHelperForGets");
    field.setAccessible(true);
    field.set(cacheEntryFactory, new MetricsRequestHelper(streamProvider));
  }
}
