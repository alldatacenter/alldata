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
package org.apache.ambari.server.state.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.jmx.JMXMetricHolder;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.services.MetricsRetrievalService.MetricSourceType;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.SynchronousThreadPoolExecutor;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import junit.framework.Assert;

/**
 * Tests the {@link MetricsRetrievalService}.
 */
public class MetricsRetrievalServiceTest extends EasyMockSupport {

  private Injector m_injector;

  private static final String JMX_URL = "http://jmx-endpoint";
  private static final String REST_URL = "http://rest-endpoint";
  private static final int METRICS_SERVICE_TIMEOUT = 10;

  MetricsRetrievalService m_service = new MetricsRetrievalService();

  /**
   *
   */
  @Before
  public void before() {
    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(new MockModule());
    m_injector.injectMembers(m_service);
  }

  @After
  public void after() throws TimeoutException {
    //stop metrics service
    if (m_service != null && m_service.isRunning()) {
      m_service.stopAsync();
      m_service.awaitTerminated(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);
    }
  }

  /**
   * Tests that initial missing values are returned correctly as {@code null}.
   */
  @Test
  public void testCachedValueRetrievalDoesNotRequest() throws Exception {
    m_service.startAsync();
    m_service.awaitRunning(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);

    JMXMetricHolder jmxMetricHolder = m_service.getCachedJMXMetric(JMX_URL);
    Assert.assertNull(jmxMetricHolder);

    Map<String, String> restMetrics = m_service.getCachedRESTMetric(REST_URL);
    Assert.assertNull(restMetrics);
  }

  /**
   * Tests retrieval of metrics.
   */
  @Test
  public void testRetrievalOfMetrics() throws Exception {
    InputStream jmxInputStream = IOUtils.toInputStream("{ \"beans\": [] }");
    InputStream restInputStream = IOUtils.toInputStream("{}");

    StreamProvider streamProvider = createNiceMock(StreamProvider.class);
    EasyMock.expect(streamProvider.readFrom(JMX_URL)).andReturn(jmxInputStream).once();
    EasyMock.expect(streamProvider.readFrom(REST_URL)).andReturn(restInputStream).once();

    replayAll();

    m_service.startAsync();
    m_service.awaitRunning(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);

    // make the service synchronous
    m_service.setThreadPoolExecutor(new SynchronousThreadPoolExecutor());

    JMXMetricHolder jmxMetricHolder = m_service.getCachedJMXMetric(JMX_URL);
    Assert.assertNull(jmxMetricHolder);

    Map<String, String> restMetrics = m_service.getCachedRESTMetric(REST_URL);
    Assert.assertNull(restMetrics);

    m_service.submitRequest(MetricSourceType.JMX, streamProvider, JMX_URL);
    jmxMetricHolder = m_service.getCachedJMXMetric(JMX_URL);
    Assert.assertNotNull(jmxMetricHolder);

    m_service.submitRequest(MetricSourceType.REST, streamProvider, REST_URL);
    restMetrics = m_service.getCachedRESTMetric(REST_URL);
    Assert.assertNotNull(restMetrics);

    verifyAll();
  }

  /**
   * Test removing cached values if request failed with IOException.
   */
  @Test
  public void testRemovingValuesFromCacheOnFail() throws Exception {

    Configuration configuration = m_injector.getInstance(Configuration.class);
    configuration.setProperty(
            Configuration.METRIC_RETRIEVAL_SERVICE_REQUEST_TTL.getKey(), "1");

    InputStream jmxInputStream = IOUtils.toInputStream("{ \"beans\": [] }");
    InputStream restInputStream = IOUtils.toInputStream("{}");

    StreamProvider streamProvider = createNiceMock(StreamProvider.class);

    EasyMock.expect(streamProvider.readFrom(JMX_URL)).andReturn(jmxInputStream).once();
    EasyMock.expect(streamProvider.readFrom(REST_URL)).andReturn(restInputStream).once();

    EasyMock.expect(streamProvider.readFrom(JMX_URL)).andThrow(new IOException()).once();
    EasyMock.expect(streamProvider.readFrom(REST_URL)).andThrow(new IOException()).once();

    replayAll();

    m_service.startAsync();
    m_service.awaitRunning(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);

    // make the service synchronous
    m_service.setThreadPoolExecutor(new SynchronousThreadPoolExecutor());

    JMXMetricHolder jmxMetricHolder = m_service.getCachedJMXMetric(JMX_URL);
    Assert.assertNull(jmxMetricHolder);

    Map<String, String> restMetrics = m_service.getCachedRESTMetric(REST_URL);
    Assert.assertNull(restMetrics);

    m_service.submitRequest(MetricSourceType.JMX, streamProvider, JMX_URL);
    jmxMetricHolder = m_service.getCachedJMXMetric(JMX_URL);
    Assert.assertNotNull(jmxMetricHolder);

    m_service.submitRequest(MetricSourceType.REST, streamProvider, REST_URL);
    restMetrics = m_service.getCachedRESTMetric(REST_URL);
    Assert.assertNotNull(restMetrics);


    jmxMetricHolder = m_service.getCachedJMXMetric(JMX_URL);
    Assert.assertNotNull(jmxMetricHolder);

    restMetrics = m_service.getCachedRESTMetric(REST_URL);
    Assert.assertNotNull(restMetrics);

    Thread.sleep(1000);

    m_service.submitRequest(MetricSourceType.JMX, streamProvider, JMX_URL);
    jmxMetricHolder = m_service.getCachedJMXMetric(JMX_URL);
    Assert.assertNull(jmxMetricHolder);

    m_service.submitRequest(MetricSourceType.REST, streamProvider, REST_URL);
    restMetrics = m_service.getCachedRESTMetric(REST_URL);
    Assert.assertNull(restMetrics);

    verifyAll();
  }

  /**
   * Tests handling NaN in JSON.
   */
  @Test
  public void testJsonNaN() throws Exception {

    InputStream jmxInputStream = IOUtils.toInputStream("{ \"beans\": [ " +
            " {\n" +
            "    \"name\" : \"Hadoop:service=HBase,name=RegionServer,sub=Server\",\n" +
            "    \"modelerType\" : \"RegionServer,sub=Server\",  \"l1CacheMissCount\" : 0,\n" +
            "    \"l1CacheHitRatio\" : NaN,\n" +
            "    \"l1CacheMissRatio\" : NaN,\n" +
            "    \"l2CacheHitCount\" : 0" +
            " }] " +
            "}");

    StreamProvider streamProvider = createNiceMock(StreamProvider.class);

    EasyMock.expect(streamProvider.readFrom(JMX_URL)).andReturn(jmxInputStream).once();

    replayAll();

    m_service.startAsync();
    m_service.awaitRunning(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);

    // make the service synchronous
    m_service.setThreadPoolExecutor(new SynchronousThreadPoolExecutor());

    JMXMetricHolder jmxMetricHolder = m_service.getCachedJMXMetric(JMX_URL);
    Assert.assertNull(jmxMetricHolder);

    m_service.submitRequest(MetricSourceType.JMX, streamProvider, JMX_URL);
    jmxMetricHolder = m_service.getCachedJMXMetric(JMX_URL);
    Assert.assertNotNull(jmxMetricHolder);
  }

  /**
   * Tests that many requests to the same URL do not invoke the stream provider
   * more than once.
   */
  @Test
  public void testRequestTTL() throws Exception {
    InputStream jmxInputStream = IOUtils.toInputStream("{ \"beans\": [] }");

    // only allow a single call to the mock
    StreamProvider streamProvider = createStrictMock(StreamProvider.class);
    EasyMock.expect(streamProvider.readFrom(JMX_URL)).andReturn(jmxInputStream).once();

    replayAll();

    m_service.startAsync();
    m_service.awaitRunning(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);

    // make the service synchronous
    m_service.setThreadPoolExecutor(new SynchronousThreadPoolExecutor());

    // make 100 requests in rapid succession to the same URL
    for (int i = 0; i < 100; i++) {
      m_service.submitRequest(MetricSourceType.JMX, streamProvider, JMX_URL);
    }

    verifyAll();
  }

  /**
   * Tests that disabling the request TTL allows subsequent requests for the
   * same resource.
   */
  @Test
  public void testRequestTTLDisabled() throws Exception {
    Configuration configuration = m_injector.getInstance(Configuration.class);
    configuration.setProperty(
        Configuration.METRIC_RETRIEVAL_SERVICE_REQUEST_TTL_ENABLED.getKey(), "false");

    InputStream jmxInputStream = IOUtils.toInputStream("{ \"beans\": [] }");

    // allow 100 calls to the mock exactly
    StreamProvider streamProvider = createStrictMock(StreamProvider.class);
    EasyMock.expect(streamProvider.readFrom(JMX_URL)).andReturn(jmxInputStream).times(100);

    replayAll();

    m_service.startAsync();
    m_service.awaitRunning(METRICS_SERVICE_TIMEOUT, TimeUnit.SECONDS);

    // make the service synchronous
    m_service.setThreadPoolExecutor(new SynchronousThreadPoolExecutor());

    // make 100 requests in rapid succession to the same URL
    for (int i = 0; i < 100; i++) {
      m_service.submitRequest(MetricSourceType.JMX, streamProvider, JMX_URL);
    }

    verifyAll();
  }

  /**
   *
   */
  private class MockModule implements Module {
    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Binder binder) {
      Cluster cluster = EasyMock.createNiceMock(Cluster.class);
      binder.bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
      binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
      binder.bind(Cluster.class).toInstance(cluster);
      binder.bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
    }
  }

}
