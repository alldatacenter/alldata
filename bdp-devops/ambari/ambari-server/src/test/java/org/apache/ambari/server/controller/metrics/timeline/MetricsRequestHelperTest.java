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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.TreeMap;

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;

public class MetricsRequestHelperTest {

  @Test
  public void testFetchTimelineMetrics() throws Exception {

    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final long now = System.currentTimeMillis();
    TimelineMetrics metrics = new TimelineMetrics();
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("cpu_user");
    timelineMetric.setAppId("app1");
    TreeMap<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(now + 100, 1.0);
    metricValues.put(now + 200, 2.0);
    metricValues.put(now + 300, 3.0);
    timelineMetric.setMetricValues(metricValues);
    metrics.getMetrics().add(timelineMetric);

    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    String metricsResponse = writer.writeValueAsString(metrics);

    InputStream inputStream = IOUtils.toInputStream(metricsResponse);
    HttpURLConnection httpURLConnectionMock = createMock(HttpURLConnection.class);
    expect(httpURLConnectionMock.getInputStream()).andReturn(inputStream).once();
    expect(httpURLConnectionMock.getResponseCode()).andReturn(HttpStatus.SC_OK).once();

    URLStreamProvider urlStreamProviderMock = createMock(URLStreamProvider.class);
    expect(urlStreamProviderMock.processURL(EasyMock.isA(String.class), EasyMock.isA(String.class),
      isNull(String.class), EasyMock.anyObject())).andReturn(httpURLConnectionMock).once();

    replay(httpURLConnectionMock, urlStreamProviderMock);

    //Case 1 : No error.
    String randomSpec = "http://localhost:6188/ws/v1/timeline/metrics?metricNames=cpu_wio&hostname=host1&appId=HOST" +
      "&startTime=1447912834&endTime=1447990034&precision=SECONDS";
    MetricsRequestHelper metricsRequestHelper = new MetricsRequestHelper(urlStreamProviderMock);
    metricsRequestHelper.fetchTimelineMetrics(new URIBuilder(randomSpec), now, now + 300);

    easyMockSupport.verifyAll();

    //Case 2 : Precision Error returned first time.
    String metricsPrecisionErrorResponse = "{\"exception\": \"PrecisionLimitExceededException\",\n" +
      "\"message\": \"Requested precision (SECONDS) for given time range causes result set size of 169840, " +
      "which exceeds the limit - 15840. Please request higher precision.\",\n" +
      "\"javaClassName\": \"org.apache.hadoop.metrics2.sink.timeline.PrecisionLimitExceededException\"\n" +
      "}";

    InputStream errorStream = IOUtils.toInputStream(metricsPrecisionErrorResponse);
    inputStream = IOUtils.toInputStream(metricsResponse); //Reloading stream.

    httpURLConnectionMock = createMock(HttpURLConnection.class);
    expect(httpURLConnectionMock.getErrorStream()).andReturn(errorStream).once();
    expect(httpURLConnectionMock.getInputStream()).andReturn(inputStream).once();
    expect(httpURLConnectionMock.getResponseCode())
      .andReturn(HttpStatus.SC_BAD_REQUEST).once()
      .andReturn(HttpStatus.SC_OK).once();

    urlStreamProviderMock = createMock(URLStreamProvider.class);
    expect(urlStreamProviderMock.processURL(EasyMock.isA(String.class), EasyMock.isA(String.class),
      isNull(String.class), EasyMock.anyObject())).andReturn(httpURLConnectionMock).times(2);

    replay(httpURLConnectionMock, urlStreamProviderMock);

    metricsRequestHelper = new MetricsRequestHelper(urlStreamProviderMock);
    metricsRequestHelper.fetchTimelineMetrics(new URIBuilder(randomSpec), now, now + 300);

    easyMockSupport.verifyAll();


  }
}
