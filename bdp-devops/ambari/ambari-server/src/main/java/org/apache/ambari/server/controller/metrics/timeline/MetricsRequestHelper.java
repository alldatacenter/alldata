/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.metrics.timeline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.HttpMethod;

import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to call AMS backend that is utilized by @AMSPropertyProvider
 * and @AMSReportPropertyProvider as well as @TimelineMetricCacheEntryFactory
 */
public class MetricsRequestHelper {
  private final static Logger LOG = LoggerFactory.getLogger(MetricsRequestHelper.class);
  private final static ObjectMapper mapper;
  private final static ObjectReader timelineObjectReader;
  private final URLStreamProvider streamProvider;

  static {
    mapper = new ObjectMapper();
    AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
    mapper.setAnnotationIntrospector(introspector);
    //noinspection deprecation
    mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    timelineObjectReader = mapper.reader(TimelineMetrics.class);
  }

  public MetricsRequestHelper(URLStreamProvider streamProvider) {
    this.streamProvider = streamProvider;
  }

  public TimelineMetrics fetchTimelineMetrics(URIBuilder uriBuilder, Long startTime, Long endTime) throws IOException {
    LOG.debug("Metrics request url = {}", uriBuilder);
    BufferedReader reader = null;
    TimelineMetrics timelineMetrics = null;
    try {

      HttpURLConnection connection = streamProvider.processURL(uriBuilder.toString(), HttpMethod.GET,
        (String) null, Collections.emptyMap());

      if (!checkConnectionForPrecisionException(connection)) {
        //Try one more time with higher precision
        String higherPrecision = getHigherPrecision(uriBuilder, startTime, endTime);
        if (higherPrecision != null) {
          LOG.debug("Requesting metrics with higher precision : {}", higherPrecision);
          uriBuilder.setParameter("precision", higherPrecision);
          String newSpec = uriBuilder.toString();
          connection = streamProvider.processURL(newSpec, HttpMethod.GET, (String) null,
            Collections.<String, List<String>>  emptyMap());
          if (!checkConnectionForPrecisionException(connection)) {
            throw new IOException("Encountered Precision exception : Higher precision request also failed.");
          }
        } else {
          throw new IOException("Encountered Precision exception : Unable to request higher precision");
        }
      }

      InputStream inputStream = connection.getInputStream();
      reader = new BufferedReader(new InputStreamReader(inputStream));
      timelineMetrics = timelineObjectReader.readValue(reader);

      if (LOG.isTraceEnabled()) {
        for (TimelineMetric metric : timelineMetrics.getMetrics()) {
          LOG.trace("metric: {}, size = {}, host = {}, app = {}, instance = {}, startTime = {}",
            metric.getMetricName(), metric.getMetricValues().size(), metric.getHostName(), metric.getAppId(), metric.getInstanceId(),
            new Date(metric.getStartTime()));
        }
      }
    } catch (IOException io) {
      String errorMsg = "Error getting timeline metrics : " + io.getMessage();
      LOG.error(errorMsg);
      if (LOG.isDebugEnabled()) {
        LOG.debug(errorMsg, io);
      }

      if (io instanceof SocketTimeoutException || io instanceof ConnectException) {
        errorMsg = "Cannot connect to collector: SocketTimeoutException for " + uriBuilder.getHost();
        LOG.error(errorMsg);
        throw io;
      }
    } catch (URISyntaxException e) {
      String errorMsg = "Error getting timeline metrics : " + e.getMessage();
      LOG.error(errorMsg);
      if (LOG.isDebugEnabled()) {
        LOG.debug(errorMsg, e);
      }
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          if (LOG.isWarnEnabled()) {
            if (LOG.isDebugEnabled()) {
              LOG.warn("Unable to close http input stream : spec=" + uriBuilder, e);
            } else {
              LOG.warn("Unable to close http input stream : spec=" + uriBuilder);
            }
          }
        }
      }
    }
    return timelineMetrics;
  }

  private boolean checkConnectionForPrecisionException(HttpURLConnection connection)
    throws IOException, URISyntaxException {

    if (connection != null && connection.getResponseCode() == HttpStatus.SC_BAD_REQUEST) {
      InputStream errorStream = connection.getErrorStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
      String errorMessage = reader.readLine();
      if (errorMessage != null && errorMessage.contains("PrecisionLimitExceededException")) {
        LOG.debug("Encountered Precision exception while requesting metrics : {}", errorMessage);
        return false;
      } else {
        throw new IOException(errorMessage);
      }
    }
    return true;
  }

  private String getHigherPrecision(URIBuilder uriBuilder, Long startTime, Long endTime) throws URISyntaxException {

    Precision currentPrecision = null;
    List<NameValuePair> queryParams = uriBuilder.getQueryParams();
    for (Iterator<NameValuePair> it = queryParams.iterator(); it.hasNext();) {
      NameValuePair nvp = it.next();
      if (nvp.getName().equals("precision")) {
        currentPrecision = Precision.getPrecision(nvp.getValue());
      }
    }
    if (currentPrecision == null && startTime != null && endTime != null) {
      currentPrecision = Precision.getPrecision(startTime, endTime);
    }
    Precision higherPrecision = Precision.getHigherPrecision(currentPrecision);

    return (higherPrecision != null) ? higherPrecision.toString() : null;
  }

}
