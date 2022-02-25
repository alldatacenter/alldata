/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.utils.ambari;


import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class ServicesTest extends EasyMockSupport {

  private static final String HTTP_RM_URL1 = "http://c1.ambari.apache.org:8088";
  private static final String HTTP_RM_URL2 = "http://c2.ambari.apache.org:8088";
  private static final String HTTPS_RM_URL1 = "https://c1.ambari.apache.org:8088";
  private static final String HTTPS_RM_URL2 = "https://c2.ambari.apache.org:8088";
  private static final String RM_URL1_HOST = "c1.ambari.apache.org";
  private static final String RM_URL2_HOST = "c2.ambari.apache.org";
  private static final String RM_URL1_HOST_PORT = "c1.ambari.apache.org:8088";
  private static final String RM_URL2_HOST_PORT = "c2.ambari.apache.org:8088";
  private static final String RM_INFO_API_ENDPOINT = Services.RM_INFO_API_ENDPOINT;

  @Test(expected = AmbariApiException.class)
  public void shouldCheckForEmptyATSUrlInCustomConfig() {
    ViewContext viewContext = getViewContext(new HashMap<String, String>());
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    expect(ambariApi.isClusterAssociated()).andReturn(false);

    replay(viewContext);

    Services services = new Services(ambariApi, viewContext);
    services.getTimelineServerUrl();

  }

  @Test
  public void shouldReturnATSUrlConfiguredInCustomMode() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("yarn.ats.url", HTTP_RM_URL1);
    ViewContext viewContext = getViewContext(map);

    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    expect(ambariApi.isClusterAssociated()).andReturn(false);

    replay(viewContext);

    Services services = new Services(ambariApi, viewContext);
    assertEquals(HTTP_RM_URL1, services.getTimelineServerUrl());
  }

  @Test(expected = AmbariApiException.class)
  public void shouldThrowExceptionIfNoProtocolInCustomMode() {
    Map<String, String> map = new HashMap<>();
    map.put("yarn.ats.url", RM_URL1_HOST_PORT);
    ViewContext viewContext = getViewContext(map);

    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    expect(ambariApi.isClusterAssociated()).andReturn(false);

    replay(viewContext);

    Services services = new Services(ambariApi, viewContext);
    services.getTimelineServerUrl();
  }


  @Test
  public void shouldReturnATSUrlFromYarnSiteInClusteredMode() throws Exception {
    ViewContext viewContext = getViewContext(new HashMap<String, String>());
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Services services = new Services(ambariApi, viewContext);

    expect(ambariApi.isClusterAssociated()).andReturn(true).anyTimes();
    setClusterExpectation(cluster, "HTTP_ONLY");
    expect(viewContext.getCluster()).andReturn(cluster).anyTimes();
    replayAll();

    assertEquals(HTTP_RM_URL1, services.getTimelineServerUrl());

    reset(cluster);
    setClusterExpectation(cluster, "HTTPS_ONLY");
    replay(cluster);

    assertEquals(HTTPS_RM_URL2, services.getTimelineServerUrl());

  }

  @Test(expected = AmbariApiException.class)
  public void shouldCheckForEmptyYarnRMUrlInCustomConfig() {
    ViewContext viewContext = getViewContext(new HashMap<String, String>());
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    expect(ambariApi.isClusterAssociated()).andReturn(false);

    replay(viewContext);

    Services services = new Services(ambariApi, viewContext);
    services.getRMUrl();


  }

  @Test(expected = AmbariApiException.class)
  public void shouldCheckIfAllRMUrlsHaveProtocolInCustomConfig() {
    Map<String, String> map = new HashMap<>();
    map.put("yarn.resourcemanager.url", HTTP_RM_URL1 + "," + RM_URL2_HOST_PORT);
    ViewContext viewContext = getViewContext(map);
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    expect(ambariApi.isClusterAssociated()).andReturn(false);

    replay(viewContext);

    Services services = new Services(ambariApi, viewContext);
    services.getRMUrl();
  }

  @Test
  public void shouldReturnUrlIfSingleIsConfiguredInCustomConfig() {
    Map<String, String> map = new HashMap<>();
    map.put("yarn.resourcemanager.url", HTTP_RM_URL1);
    ViewContext viewContext = getViewContext(map);
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    expect(ambariApi.isClusterAssociated()).andReturn(false);

    replay(viewContext);

    Services services = new Services(ambariApi, viewContext);
    assertEquals(HTTP_RM_URL1, services.getRMUrl());
  }

  @Test
  public void shouldConnectToFirstUrlWhenMultipleRMUrlIsConfiguredInCustomConfig() throws IOException {
    Map<String, String> map = new HashMap<>();
    map.put("yarn.resourcemanager.url", HTTP_RM_URL1 + ", " + HTTP_RM_URL2);
    ViewContext viewContext = getViewContext(map);

    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);

    InputStream inputStream = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"ACTIVE\"}}");

    expect(ambariApi.isClusterAssociated()).andReturn(false);
    expect(viewContext.getURLStreamProvider()).andReturn(urlStreamProvider);
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL1 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);

    replayAll();

    Services services = new Services(ambariApi, viewContext);
    assertEquals(HTTP_RM_URL1, services.getRMUrl());

  }

  @Test
  public void shouldConnectToSecondUrlWhenTheFirstURLTimesOut() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("yarn.resourcemanager.url", HTTP_RM_URL1 + ", " + HTTP_RM_URL2);
    ViewContext viewContext = getViewContext(map);

    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);

    InputStream inputStream = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"ACTIVE\"}}");

    expect(ambariApi.isClusterAssociated()).andReturn(false);
    expect(viewContext.getURLStreamProvider()).andReturn(urlStreamProvider).anyTimes();
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL1 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andThrow(new IOException());
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL2 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);

    replayAll();

    Services services = new Services(ambariApi, viewContext);
    assertEquals(HTTP_RM_URL2, services.getRMUrl());

  }

  @Test(expected = AmbariApiException.class)
  public void shouldThrowExceptionWhenAllUrlCannotBeReached() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("yarn.resourcemanager.url", HTTP_RM_URL1 + ", " + HTTP_RM_URL2);
    ViewContext viewContext = getViewContext(map);

    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);

    expect(ambariApi.isClusterAssociated()).andReturn(false);
    expect(viewContext.getURLStreamProvider()).andReturn(urlStreamProvider).anyTimes();
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL1 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andThrow(new IOException());
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL2 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andThrow(new IOException());

    replayAll();

    Services services = new Services(ambariApi, viewContext);
    services.getRMUrl();
  }

  @Test
  public void shouldReturnActiveRMUrlWhenConnectingToStandby() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("yarn.resourcemanager.url", HTTP_RM_URL1 + ", " + HTTP_RM_URL2);
    ViewContext viewContext = getViewContext(map);

    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);

    InputStream inputStream = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"STANDBY\"}}");

    expect(ambariApi.isClusterAssociated()).andReturn(false);
    expect(viewContext.getURLStreamProvider()).andReturn(urlStreamProvider).anyTimes();
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL1 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);

    InputStream inputStreamActive = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"ACTIVE\"}}");
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL2 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStreamActive);

    replayAll();

    Services services = new Services(ambariApi, viewContext);
    assertEquals(HTTP_RM_URL2, services.getRMUrl());

    verify(urlStreamProvider);

  }

  @Test
  public void shouldConnectToRMConfiguredInClusterMode() throws Exception {
    ViewContext viewContext = getViewContext(new HashMap<String, String>());
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    Cluster cluster = createNiceMock(Cluster.class);

    expect(ambariApi.isClusterAssociated()).andReturn(true).anyTimes();
    setClusterExpectation(cluster, "HTTP_ONLY");
    expect(viewContext.getCluster()).andReturn(cluster).anyTimes();

    replayAll();

    Services services = new Services(ambariApi, viewContext);
    assertEquals(HTTP_RM_URL1, services.getRMUrl());

    reset(cluster);
    setClusterExpectation(cluster, "HTTPS_ONLY");
    replay(cluster);

    assertEquals(HTTPS_RM_URL2, services.getRMUrl());

    reset(cluster);
    setClusterExpectation(cluster, "HTTPS_ONLY_XYZ");
    replay(cluster);

    assertEquals(HTTP_RM_URL1, services.getRMUrl());
  }

  @Test
  public void shouldConnectToDefaultHostPortInClusterModeWhenWebaddressConfigIsEmpty() throws Exception {
    ViewContext viewContext = getViewContext(new HashMap<String, String>());
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    Cluster cluster = createNiceMock(Cluster.class);

    expect(ambariApi.isClusterAssociated()).andReturn(true).anyTimes();
    setClusterExpectationWithEmptyWebappConfig(cluster, "HTTP_ONLY");
    expect(viewContext.getCluster()).andReturn(cluster).anyTimes();

    Services services = new Services(ambariApi, viewContext);

    replayAll();
    assertEquals("http://" + RM_URL1_HOST + ":8088", services.getRMUrl());

    reset(cluster);
    setClusterExpectationWithEmptyWebappConfig(cluster, "HTTPS_ONLY");
    replay(cluster);

    assertEquals("https://" + RM_URL1_HOST + ":8090", services.getRMUrl());

  }

  @Test
  public void shouldConnectToDefaultHostPortInClusterModeWithHAWhenWebaddressConfigIsEmpty() throws Exception {
    ViewContext viewContext = getViewContext(new HashMap<String, String>());
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    Cluster cluster = createNiceMock(Cluster.class);
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);

    expect(ambariApi.isClusterAssociated()).andReturn(true).anyTimes();
    setClusterExpectationInHAWithEmptyWebappConfig(cluster, "HTTP_ONLY");
    expect(viewContext.getCluster()).andReturn(cluster).anyTimes();

    Services services = new Services(ambariApi, viewContext);

    InputStream inputStream = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"ACTIVE\"}}");
    expect(viewContext.getURLStreamProvider()).andReturn(urlStreamProvider).anyTimes();
    expect(urlStreamProvider.readFrom(eq("http://" + RM_URL1_HOST + ":8088" + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);

    replayAll();
    assertEquals("http://" + RM_URL1_HOST + ":8088", services.getRMUrl());

    reset(cluster, urlStreamProvider);
    setClusterExpectationInHAWithEmptyWebappConfig(cluster, "HTTPS_ONLY");
    inputStream = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"ACTIVE\"}}");
    expect(urlStreamProvider.readFrom(eq("https://" + RM_URL1_HOST + ":8090" + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);
    replay(cluster, urlStreamProvider);

    assertEquals("https://" + RM_URL1_HOST + ":8090", services.getRMUrl());

  }

  @Test
  public void shouldFetchRMUrlsWhileHAEnabledInClusterMode() throws Exception {
    ViewContext viewContext = getViewContext(new HashMap<String, String>());
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    Cluster cluster = createNiceMock(Cluster.class);
    URLStreamProvider urlStreamProvider = createNiceMock(URLStreamProvider.class);
    Services services = new Services(ambariApi, viewContext);

    InputStream inputStream = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"ACTIVE\"}}");



    expect(ambariApi.isClusterAssociated()).andReturn(true).anyTimes();
    setClusterExpectationInHA(cluster, "HTTP_ONLY");
    expect(viewContext.getCluster()).andReturn(cluster).anyTimes();
    expect(viewContext.getURLStreamProvider()).andReturn(urlStreamProvider).anyTimes();
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL1 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);

    replayAll();

    assertEquals(HTTP_RM_URL1, services.getRMUrl());

    reset(cluster, urlStreamProvider);
    setClusterExpectationInHA(cluster, "HTTP_ONLY");
    inputStream = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"ACTIVE\"}}");
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL1 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andThrow(new IOException());
    expect(urlStreamProvider.readFrom(eq(HTTP_RM_URL2 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);
    replay(cluster, urlStreamProvider);

    assertEquals(HTTP_RM_URL2, services.getRMUrl());

    reset(cluster, urlStreamProvider);
    setClusterExpectationInHA(cluster, "HTTPS_ONLY");
    inputStream = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"ACTIVE\"}}");
    expect(urlStreamProvider.readFrom(eq(HTTPS_RM_URL1 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);
    replay(cluster, urlStreamProvider);

    assertEquals(HTTPS_RM_URL1, services.getRMUrl());

    reset(cluster, urlStreamProvider);
    setClusterExpectationInHA(cluster, "HTTPS_ONLY");
    inputStream = IOUtils.toInputStream("{\"clusterInfo\": {\"haState\": \"ACTIVE\"}}");
    expect(urlStreamProvider.readFrom(eq(HTTPS_RM_URL1 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andThrow(new IOException());
    expect(urlStreamProvider.readFrom(eq(HTTPS_RM_URL2 + RM_INFO_API_ENDPOINT), eq("GET"), anyString(), EasyMock.<Map<String, String>>anyObject())).andReturn(inputStream);
    replay(cluster, urlStreamProvider);

    assertEquals(HTTPS_RM_URL2, services.getRMUrl());
  }

  @Test
  public void basicGetYARNProtocol() throws Exception {
    ViewContext viewContext = getViewContext(new HashMap<String, String>());
    AmbariApi ambariApi = createNiceMock(AmbariApi.class);
    Cluster cluster = createNiceMock(Cluster.class);

    expect(ambariApi.isClusterAssociated()).andReturn(true).anyTimes();
    setClusterExpectationWithEmptyWebappConfig(cluster, "HTTP_ONLY");
    expect(viewContext.getCluster()).andReturn(cluster).anyTimes();

    Services services = new Services(ambariApi, viewContext);

    replayAll();
    assertEquals("http", services.getYARNProtocol());
  }

  private void setClusterExpectation(Cluster cluster, String httpPolicy) {
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.ha.enabled")).andReturn("false");
    expect(cluster.getConfigurationValue("yarn-site", "yarn.http.policy")).andReturn(httpPolicy);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.webapp.address")).andReturn(RM_URL1_HOST_PORT);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.webapp.https.address")).andReturn(RM_URL2_HOST_PORT);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.timeline-service.webapp.address")).andReturn(RM_URL1_HOST_PORT);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.timeline-service.webapp.https.address")).andReturn(RM_URL2_HOST_PORT);
  }

  private void setClusterExpectationInHA(Cluster cluster, String httpPolicy) {
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.ha.enabled")).andReturn("true");
    expect(cluster.getConfigurationValue("yarn-site", "yarn.http.policy")).andReturn(httpPolicy);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.ha.rm-ids")).andReturn("rm1,rm2");
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.webapp.address.rm1")).andReturn(RM_URL1_HOST_PORT);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.webapp.address.rm2")).andReturn(RM_URL2_HOST_PORT);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.webapp.https.address.rm1")).andReturn(RM_URL1_HOST_PORT);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.webapp.https.address.rm2")).andReturn(RM_URL2_HOST_PORT);
  }

  private void setClusterExpectationInHAWithEmptyWebappConfig(Cluster cluster, String httpPolicy) {
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.ha.enabled")).andReturn("true");
    expect(cluster.getConfigurationValue("yarn-site", "yarn.http.policy")).andReturn(httpPolicy);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.ha.rm-ids")).andReturn("rm1,rm2");
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.hostname.rm1")).andReturn(RM_URL1_HOST);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.hostname.rm2")).andReturn(RM_URL2_HOST);

  }

  private void setClusterExpectationWithEmptyWebappConfig(Cluster cluster, String httpPolicy) {
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.ha.enabled")).andReturn("false");
    expect(cluster.getConfigurationValue("yarn-site", "yarn.http.policy")).andReturn(httpPolicy);
    expect(cluster.getConfigurationValue("yarn-site", "yarn.resourcemanager.hostname")).andReturn(RM_URL1_HOST);
  }

  private ViewContext getViewContext(Map<String, String> map) {
    ViewContext viewContextMock = createNiceMock(ViewContext.class);
    expect(viewContextMock.getProperties()).andReturn(map);
    return viewContextMock;
  }
}
