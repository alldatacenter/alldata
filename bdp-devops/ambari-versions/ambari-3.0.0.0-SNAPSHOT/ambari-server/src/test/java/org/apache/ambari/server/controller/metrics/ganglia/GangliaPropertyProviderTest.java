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
package org.apache.ambari.server.controller.metrics.ganglia;

import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.GANGLIA;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.ComponentSSLConfigurationTest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.MetricsServiceProvider;
import org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.CollectionPresentationUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test the Ganglia property provider.
 */
@RunWith(Parameterized.class)
@PrepareForTest({ MetricHostProvider.class })
public class GangliaPropertyProviderTest {

  private static final String PROPERTY_ID = PropertyHelper.getPropertyId("metrics/process", "proc_total");
  private static final String PROPERTY_ID2 = PropertyHelper.getPropertyId("metrics/cpu", "cpu_wio");
  private static final String FLUME_CHANNEL_CAPACITY_PROPERTY = "metrics/flume/flume/CHANNEL/c1/ChannelCapacity";
  private static final String FLUME_CATEGORY = "metrics/flume";
  private static final String FLUME_CATEGORY2 = "metrics/flume/flume";
  private static final String FLUME_CATEGORY3 = "metrics/flume/flume/CHANNEL";
  private static final String FLUME_CATEGORY4 = "metrics/flume/flume/CHANNEL/c1";
  private static final String CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  private static final String HOST_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "host_name");
  private static final String COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");

  
  private ComponentSSLConfiguration configuration;

  @Parameterized.Parameters
  public static Collection<Object[]> configs() {
    ComponentSSLConfiguration configuration1 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", false);

    ComponentSSLConfiguration configuration2 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", true);

    ComponentSSLConfiguration configuration3 =
        ComponentSSLConfigurationTest.getConfiguration("tspath", "tspass", "tstype", false);

    return Arrays.asList(new Object[][]{
        {configuration1},
        {configuration2},
        {configuration3}
    });
  }

  public GangliaPropertyProviderTest(ComponentSSLConfiguration configuration) {
    this.configuration = configuration;
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testGangliaPropertyProviderAsClusterAdministrator() throws Exception {
    //Setup user with Role 'ClusterAdministrator'.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("ClusterAdmin", 2L));
    testPopulateResources();
    testPopulateResources_checkHostComponent();
    testPopulateResources_checkHost();
    testPopulateManyResources();
    testPopulateResources__LargeNumberOfHostResources();
    testPopulateResources_params();
    testPopulateResources_paramsMixed();
    testPopulateResources_paramsAll();
    testPopulateResources_params_category1();
    testPopulateResources_params_category2();
    testPopulateResources_params_category3();
    testPopulateResources_params_category4();
  }

  @Test
  public void testGangliaPropertyProviderAsAdministrator() throws Exception {
    //Setup user with Role 'Administrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("Admin"));
    testPopulateResources();
    testPopulateResources_checkHostComponent();
    testPopulateResources_checkHost();
    testPopulateManyResources();
    testPopulateResources__LargeNumberOfHostResources();
    testPopulateResources_params();
    testPopulateResources_paramsMixed();
    testPopulateResources_paramsAll();
    testPopulateResources_params_category1();
    testPopulateResources_params_category2();
    testPopulateResources_params_category3();
    testPopulateResources_params_category4();
  }

  @Test
  public void testGangliaPropertyProviderAsServiceAdministrator() throws Exception {
    //Setup user with 'ServiceAdministrator'
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createServiceAdministrator("ServiceAdmin", 2L));
    testPopulateResources();
    testPopulateResources_checkHostComponent();
    testPopulateResources_checkHost();
    testPopulateManyResources();
    testPopulateResources__LargeNumberOfHostResources();
    testPopulateResources_params();
    testPopulateResources_paramsMixed();
    testPopulateResources_paramsAll();
    testPopulateResources_params_category1();
    testPopulateResources_params_category2();
    testPopulateResources_params_category3();
    testPopulateResources_params_category4();
  }

  @Test(expected = AuthorizationException.class)
  public void testGangliaPropertyProviderAsViewUser() throws Exception {
    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
    // Setup user with 'ViewUser'
    // ViewUser doesn't have the 'CLUSTER_VIEW_METRICS', 'HOST_VIEW_METRICS' and 'SERVICE_VIEW_METRICS', thus
    // can't retrieve the Metrics.
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser("ViewUser", 2L));
    testPopulateResources();
    testPopulateResources_checkHostComponent();
    testPopulateResources_checkHost();
    testPopulateManyResources();
    testPopulateResources__LargeNumberOfHostResources();
    testPopulateResources_params();
    testPopulateResources_paramsMixed();
    testPopulateResources_paramsAll();
    testPopulateResources_params_category1();
    testPopulateResources_params_category2();
    testPopulateResources_params_category3();
    testPopulateResources_params_category4();
  }

  public void testPopulateResources() throws Exception {
    setUpCommonMocks();
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // namenode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());


    String expected = (configuration.isHttpsEnabled() ? "https" : "http") +
        "://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPDataNode%2CHDPSlaves&h=domU-12-31-39-0E-34-E1.compute-1.internal&m=proc_total&s=10&e=20&r=1";
    Assert.assertEquals(expected, streamProvider.getLastSpec());

    Assert.assertEquals(4, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID));


    // tasktracker
    resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "TASKTRACKER");

    // only ask for one property
    temporalInfoMap = new HashMap<>();

    Set<String> properties = new HashSet<>();
    String shuffle_exceptions_caught = PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_exceptions_caught");
    String shuffle_failed_outputs    = PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_failed_outputs");
    String shuffle_output_bytes      = PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_output_bytes");
    String shuffle_success_outputs   = PropertyHelper.getPropertyId("metrics/mapred/shuffleOutput", "shuffle_success_outputs");

    properties.add(shuffle_exceptions_caught);
    properties.add(shuffle_failed_outputs);
    properties.add(shuffle_output_bytes);
    properties.add(shuffle_success_outputs);
    request = PropertyHelper.getReadRequest(properties, temporalInfoMap);

    temporalInfoMap.put(shuffle_exceptions_caught, new TemporalInfoImpl(10L, 20L, 1L));
    temporalInfoMap.put(shuffle_failed_outputs, new TemporalInfoImpl(10L, 20L, 1L));
    temporalInfoMap.put(shuffle_output_bytes, new TemporalInfoImpl(10L, 20L, 1L));
    temporalInfoMap.put(shuffle_success_outputs, new TemporalInfoImpl(10L, 20L, 1L));

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    
    List<String> metricsRegexes = new ArrayList<>();
    
    metricsRegexes.add("metrics/mapred/shuffleOutput/shuffle_exceptions_caught");
    metricsRegexes.add("metrics/mapred/shuffleOutput/shuffle_failed_outputs");
    metricsRegexes.add("metrics/mapred/shuffleOutput/shuffle_output_bytes");
    metricsRegexes.add("metrics/mapred/shuffleOutput/shuffle_success_outputs");
    
    
    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "TASKTRACKER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isHttpsEnabled() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPTaskTracker,HDPSlaves");
    expectedUri.setParameter("h", "domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    

    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));

    Assert.assertEquals(7, PropertyHelper.getProperties(resource).size());

    Assert.assertNotNull(resource.getPropertyValue(shuffle_exceptions_caught));

    Number[][] dataPoints = (Number[][]) resource.getPropertyValue(shuffle_exceptions_caught);

    Assert.assertEquals(106, dataPoints.length);
    for (int i = 0; i < dataPoints.length; ++i) {
      Assert.assertEquals(i >=10 && i < 20 ? 7 : 0.0, dataPoints[i][0]);
      Assert.assertEquals(360 * i + 1358434800, dataPoints[i][1]);
    }

    Assert.assertNotNull(resource.getPropertyValue(shuffle_failed_outputs));
    Assert.assertNotNull(resource.getPropertyValue(shuffle_output_bytes));
    Assert.assertNotNull(resource.getPropertyValue(shuffle_success_outputs));
  }
  
  public void testPopulateResources_checkHostComponent() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
    MetricHostProvider hostProvider =  PowerMock.createPartialMock(MetricHostProvider.class,
        "isCollectorHostLive", "isCollectorComponentLive");

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // datanode
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "DATANODE");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);
    
    expect(hostProvider.getCollectorHostName(anyObject(String.class), eq(GANGLIA))).andReturn("ganglia-host");
    expect(hostProvider.isCollectorComponentLive(anyObject(String.class), eq(GANGLIA))).andReturn(true).once();
    expect(hostProvider.isCollectorHostLive(anyObject(String.class), eq(GANGLIA))).andReturn(true).once();
    
    
    PowerMock.replay(hostProvider);
    
    Set<Resource> populateResources = propertyProvider.populateResources(Collections.singleton(resource), request, null);
    
    PowerMock.verify(hostProvider);
    
    Assert.assertEquals(1, populateResources.size());
    
  }

  public void testPopulateResources_checkHost() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("host_temporal_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostPropertyProvider(
        PropertyHelper.getMetricPropertyIds(Resource.Type.Host),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID
    );

    // host
    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "corp-hadoopda05.client.ext");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put("metrics/process/proc_total", new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton("metrics/process/proc_total"), temporalInfoMap);

    Set<Resource> populateResources = propertyProvider.populateResources(Collections.singleton(resource), request, null);

    Assert.assertEquals(1, populateResources.size());

    Resource res = populateResources.iterator().next();

    Number[][] val = (Number[][]) res.getPropertyValue("metrics/process/proc_total");
    Assert.assertEquals(226, val.length);
  }

  public void testPopulateManyResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data_1.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostPropertyProvider(
        PropertyHelper.getMetricPropertyIds(Resource.Type.Host),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID
    );

    Set<Resource> resources = new HashSet<>();

    // host
    Resource resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E1.compute-1.internal");
    resources.add(resource);

    resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E2.compute-1.internal");
    resources.add(resource);

    resource = new ResourceImpl(Resource.Type.Host);
    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "domU-12-31-39-0E-34-E3.compute-1.internal");
    resources.add(resource);

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);

    Assert.assertEquals(3, propertyProvider.populateResources(resources, request, null).size());
    
    URIBuilder uriBuilder = new URIBuilder();

    uriBuilder.setScheme((configuration.isHttpsEnabled() ? "https" : "http"));
    uriBuilder.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    uriBuilder.setPath("/cgi-bin/rrd.py");
    uriBuilder.setParameter("c", "HDPJobTracker,HDPHBaseMaster,HDPResourceManager,HDPFlumeServer,HDPSlaves,HDPHistoryServer,HDPJournalNode,HDPTaskTracker,HDPHBaseRegionServer,HDPNameNode");
    uriBuilder.setParameter("h", "domU-12-31-39-0E-34-E3.compute-1.internal,domU-12-31-39-0E-34-E1.compute-1.internal,domU-12-31-39-0E-34-E2.compute-1.internal");
    uriBuilder.setParameter("m", "proc_total");
    uriBuilder.setParameter("s", "10");
    uriBuilder.setParameter("e", "20");
    uriBuilder.setParameter("r", "1");

    String expected = uriBuilder.toString();

    // Depends on hashing, string representation can be different
    List<String> components = Arrays.asList(new String[]{"HDPJobTracker", "HDPHBaseMaster", "HDPResourceManager", "HDPFlumeServer",
        "HDPSlaves", "HDPHistoryServer", "HDPJournalNode", "HDPTaskTracker", "HDPHBaseRegionServer", "HDPNameNode"});
    List<String> hosts = Arrays.asList(new String[]{"domU-12-31-39-0E-34-E3.compute-1.internal", "domU-12-31-39-0E-34-E1.compute-1.internal",
        "domU-12-31-39-0E-34-E2.compute-1.internal"});
    int httpsVariation = configuration.isHttpsEnabled() ? 1 : 0;

    Assert.assertEquals(expected.substring(0, 66 + httpsVariation), streamProvider.getLastSpec().substring(0, 66 + httpsVariation));
    Assert.assertTrue(CollectionPresentationUtils.isStringPermutationOfCollection(streamProvider.getLastSpec().substring(66 + httpsVariation, 236 + httpsVariation), components, "%2C", 0, 0));
    Assert.assertTrue(CollectionPresentationUtils.isStringPermutationOfCollection(streamProvider.getLastSpec().substring(239 + httpsVariation, 368 + httpsVariation), hosts, "%2C", 0, 0));
    Assert.assertEquals(expected.substring(369 + httpsVariation), streamProvider.getLastSpec().substring(369 + httpsVariation));

    for (Resource res : resources) {
      Assert.assertEquals(3, PropertyHelper.getProperties(res).size());
      Assert.assertNotNull(res.getPropertyValue(PROPERTY_ID));
    }
  }

  public void testPopulateResources__LargeNumberOfHostResources() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("temporal_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostPropertyProvider(
        PropertyHelper.getMetricPropertyIds(Resource.Type.Host),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID
    );

    Set<Resource> resources = new HashSet<>();

    StringBuilder hostsList = new StringBuilder();
    
    for (int i = 0; i < 150; ++i) {
      Resource resource = new ResourceImpl(Resource.Type.Host);
      resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
      resource.setProperty(HOST_NAME_PROPERTY_ID, "host" + i);
      resources.add(resource);
      
      if (hostsList.length() != 0)
        hostsList.append(",host").append(i);
      else
        hostsList.append("host").append(i);
    }

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(PROPERTY_ID, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(PROPERTY_ID), temporalInfoMap);

    Assert.assertEquals(150, propertyProvider.populateResources(resources, request, null).size());

    
    URIBuilder expectedUri = new URIBuilder();
    
    expectedUri.setScheme((configuration.isHttpsEnabled() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPJobTracker,HDPHBaseMaster,HDPResourceManager,HDPFlumeServer,HDPSlaves,HDPHistoryServer,HDPJournalNode,HDPTaskTracker,HDPHBaseRegionServer,HDPNameNode");
   
    expectedUri.setParameter("h", hostsList.toString());
    expectedUri.setParameter("m", "proc_total");
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());
    
    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));
  }
  
  public void testPopulateResources_params() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(FLUME_CHANNEL_CAPACITY_PROPERTY, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CHANNEL_CAPACITY_PROPERTY), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> metricsRegexes = new ArrayList<>();
    
    metricsRegexes.add(FLUME_CHANNEL_CAPACITY_PROPERTY);

    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_HANDLER");
    
    URIBuilder expectedUri = new URIBuilder();
    
    expectedUri.setScheme((configuration.isHttpsEnabled() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPFlumeServer,HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    
    
    Assert.assertEquals(4, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  public void testPopulateResources_paramsMixed() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);
    
    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();

    Set<String> ids = new HashSet<>();
    ids.add(FLUME_CATEGORY2);
    ids.add(PROPERTY_ID2);

    Request request = PropertyHelper.getReadRequest(ids, temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> metricsRegexes = new ArrayList<>();
    
    metricsRegexes.add("metrics/flume");
    metricsRegexes.add("metrics/cpu/cpu_wio");
    
    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_HANDLER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isHttpsEnabled() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPFlumeServer,HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("e", "now");
    expectedUri.setParameter("pt", "true");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));
       
    Assert.assertEquals(23, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(PROPERTY_ID2));
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  public void testPopulateResources_paramsAll() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent),
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");

    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    Request  request = PropertyHelper.getReadRequest(Collections.emptySet(), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    String expected = (configuration.isHttpsEnabled() ? "https" : "http") +
        "://domU-12-31-39-0E-34-E1.compute-1.internal/cgi-bin/rrd.py?c=HDPFlumeServer%2CHDPSlaves&h=ip-10-39-113-33.ec2.internal&m=";

    // Depends on hashing, string representation can be different
    List<String> components = Arrays.asList(new String[]{"HDPFlumeServer", "HDPSlaves"});
    int httpsVariation = configuration.isHttpsEnabled() ? 1 : 0;

    Assert.assertEquals(expected.substring(0, 66 + httpsVariation), streamProvider.getLastSpec().substring(0, 66 + httpsVariation));
    Assert.assertTrue(CollectionPresentationUtils.isStringPermutationOfCollection(streamProvider.getLastSpec().substring(66 + httpsVariation, 92 + httpsVariation), components, "%2C", 0, 0));
    Assert.assertTrue(streamProvider.getLastSpec().substring(92 + httpsVariation).startsWith(expected.substring(92 + httpsVariation)));

    Assert.assertEquals(34, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  public void testPopulateResources_params_category1() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(FLUME_CATEGORY, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CATEGORY), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> metricsRegexes = new ArrayList<>();
    
    metricsRegexes.add("metrics/flume");
    
    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_HANDLER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isHttpsEnabled() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPFlumeServer,HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    

    Assert.assertEquals(22, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  public void testPopulateResources_params_category2() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(FLUME_CATEGORY2, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CATEGORY2), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());

    List<String> metricsRegexes = new ArrayList<>();
    
    metricsRegexes.add("metrics/flume/");
    
    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_HANDLER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isHttpsEnabled() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPFlumeServer,HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));

    Assert.assertEquals(22, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  public void testPopulateResources_params_category3() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(FLUME_CATEGORY3, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CATEGORY3), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
    
    List<String> metricsRegexes = new ArrayList<>();
    
    metricsRegexes.add("metrics/flume/$1/CHANNEL/$2/");
    metricsRegexes.add(FLUME_CHANNEL_CAPACITY_PROPERTY);

    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_HANDLER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isHttpsEnabled() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPFlumeServer,HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    

    Assert.assertEquals(12, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  public void testPopulateResources_params_category4() throws Exception {
    TestStreamProvider streamProvider  = new TestStreamProvider("flume_ganglia_data.txt");
    TestGangliaHostProvider hostProvider = new TestGangliaHostProvider();

    Map<String, Map<String, PropertyInfo>> gangliaPropertyIds = PropertyHelper.getMetricPropertyIds(Resource.Type.HostComponent);
    GangliaPropertyProvider propertyProvider = new GangliaHostComponentPropertyProvider(
        gangliaPropertyIds,
        streamProvider,
        configuration,
        hostProvider,
        CLUSTER_NAME_PROPERTY_ID,
        HOST_NAME_PROPERTY_ID,
        COMPONENT_NAME_PROPERTY_ID);

    // flume
    Resource resource = new ResourceImpl(Resource.Type.HostComponent);

    resource.setProperty(CLUSTER_NAME_PROPERTY_ID, "c1");
    resource.setProperty(HOST_NAME_PROPERTY_ID, "ip-10-39-113-33.ec2.internal");
    resource.setProperty(COMPONENT_NAME_PROPERTY_ID, "FLUME_HANDLER");

    // only ask for one property
    Map<String, TemporalInfo> temporalInfoMap = new HashMap<>();
    temporalInfoMap.put(FLUME_CATEGORY4, new TemporalInfoImpl(10L, 20L, 1L));
    Request  request = PropertyHelper.getReadRequest(Collections.singleton(FLUME_CATEGORY4), temporalInfoMap);

    Assert.assertEquals(1, propertyProvider.populateResources(Collections.singleton(resource), request, null).size());
    
    List<String> metricsRegexes = new ArrayList<>();
    
    metricsRegexes.add("metrics/flume/$1/CHANNEL/$2");
    metricsRegexes.add(FLUME_CHANNEL_CAPACITY_PROPERTY);

    String metricsList = getMetricsRegexes(metricsRegexes, gangliaPropertyIds, "FLUME_HANDLER");
    
    URIBuilder expectedUri = new URIBuilder();

    expectedUri.setScheme((configuration.isHttpsEnabled() ? "https" : "http"));
    expectedUri.setHost("domU-12-31-39-0E-34-E1.compute-1.internal");
    expectedUri.setPath("/cgi-bin/rrd.py");
    expectedUri.setParameter("c", "HDPFlumeServer,HDPSlaves");
    expectedUri.setParameter("h", "ip-10-39-113-33.ec2.internal");
    expectedUri.setParameter("m", metricsList);
    expectedUri.setParameter("s", "10");
    expectedUri.setParameter("e", "20");
    expectedUri.setParameter("r", "1");
    
    URIBuilder actualUri = new URIBuilder(streamProvider.getLastSpec());

    Assert.assertEquals(expectedUri.getScheme(), actualUri.getScheme());
    Assert.assertEquals(expectedUri.getHost(), actualUri.getHost());
    Assert.assertEquals(expectedUri.getPath(), actualUri.getPath());
    
    Assert.assertTrue(isUrlParamsEquals(actualUri, expectedUri));    
    
    Assert.assertEquals(12, PropertyHelper.getProperties(resource).size());
    Assert.assertNotNull(resource.getPropertyValue(FLUME_CHANNEL_CAPACITY_PROPERTY));
  }

  private boolean isUrlParamsEquals(URIBuilder actualUri, URIBuilder expectedUri) {
    for (final NameValuePair expectedParam : expectedUri.getQueryParams()) {
      NameValuePair actualParam = (NameValuePair) CollectionUtils.find(actualUri.getQueryParams(), new Predicate() {
        
        @Override
        public boolean evaluate(Object arg0) {
          if (!(arg0 instanceof NameValuePair))
            return false;
          
          NameValuePair otherObj = (NameValuePair) arg0;
          return otherObj.getName().equals(expectedParam.getName());
        }
      });
      
      if (actualParam == null) {
        return false;
      }
      List<String> actualParamList = new ArrayList<>(Arrays.asList(actualParam.getValue().split(",")));
      List<String> expectedParamList = new ArrayList<>(Arrays.asList(expectedParam.getValue().split(",")));
      
      Collections.sort(actualParamList);
      Collections.sort(expectedParamList);
      
      if (!actualParamList.equals(expectedParamList))
        return false;
    }
    
    return true;
  }
  
  private String getMetricsRegexes(List<String> metricsRegexes,
      Map<String, Map<String, PropertyInfo>> gangliaPropertyIds,
      String componentName) {
    
    StringBuilder metricsBuilder = new StringBuilder();
    
    for (Map.Entry<String, PropertyInfo> entry : gangliaPropertyIds.get(componentName).entrySet())
    {
      for (String metricRegex: metricsRegexes)
      {
        if (entry.getKey().startsWith(metricRegex)) {
          metricsBuilder.append(entry.getValue().getPropertyId()).append(",");
        }
      }
    }
    return metricsBuilder.toString();
  }

  private void setUpCommonMocks() throws AmbariException, NoSuchFieldException, IllegalAccessException {
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
    PowerMock.replayAll();
  }

  public static class TestGangliaServiceProvider implements MetricsServiceProvider {

    @Override
    public MetricsService getMetricsServiceType() {
      return MetricsService.GANGLIA;
    }
  }

  public static class TestGangliaHostProvider implements MetricHostProvider {

    private boolean isHostLive;
    private boolean isComponentLive;
    
    public TestGangliaHostProvider() {
      this(true, true);
    }

    public TestGangliaHostProvider(boolean isHostLive, boolean isComponentLive) {
      this.isHostLive = isHostLive;
      this.isComponentLive = isComponentLive;
    }

    @Override
    public String getCollectorHostName(String clusterName, MetricsService service) {
      return "domU-12-31-39-0E-34-E1.compute-1.internal";
    }

    @Override
    public String getHostName(String clusterName, String componentName) throws SystemException {
      return null;
    }

    @Override
    public String getCollectorPort(String clusterName, MetricsService service) throws SystemException {
      return null;
    }

    @Override
    public boolean isCollectorHostLive(String clusterName, MetricsService service)
        throws SystemException {
      return isHostLive;
    }

    @Override
    public boolean isCollectorComponentLive(String clusterName, MetricsService service)
        throws SystemException {
      return isComponentLive;
    }

    @Override
    public boolean isCollectorHostExternal(String clusterName) {
      return false;
    }
  }
}
