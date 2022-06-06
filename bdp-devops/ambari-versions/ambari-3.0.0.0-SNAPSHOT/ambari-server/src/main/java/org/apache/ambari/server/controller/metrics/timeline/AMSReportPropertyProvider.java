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

import static org.apache.ambari.server.controller.metrics.MetricsPaddingMethod.ZERO_PADDING_PARAM;
import static org.apache.ambari.server.controller.metrics.MetricsServiceProvider.MetricsService.TIMELINE_METRICS;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.MetricsPaddingMethod;
import org.apache.ambari.server.controller.metrics.MetricsPropertyProvider;
import org.apache.ambari.server.controller.metrics.MetricsReportPropertyProvider;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineAppMetricCacheKey;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCache;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.MetricsCollectorHostDownEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMSReportPropertyProvider extends MetricsReportPropertyProvider {
  private static final Logger LOG = LoggerFactory.getLogger(AMSReportPropertyProvider.class);
  private MetricsPaddingMethod metricsPaddingMethod;
  private final TimelineMetricCache metricCache;
  MetricsRequestHelper requestHelper;
  private static AtomicInteger printSkipPopulateMsgHostCounter = new AtomicInteger(0);
  private static AtomicInteger printSkipPopulateMsgHostCompCounter = new AtomicInteger(0);
  private AmbariEventPublisher ambariEventPublisher;

  public AMSReportPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                                   URLStreamProvider streamProvider,
                                 ComponentSSLConfiguration configuration,
                                 TimelineMetricCacheProvider cacheProvider,
                                 MetricHostProvider hostProvider,
                                 String clusterNamePropertyId) {

    super(componentPropertyInfoMap, streamProvider, configuration,
      hostProvider, clusterNamePropertyId);

    this.metricCache = cacheProvider.getTimelineMetricsCache();
    this.requestHelper = new MetricsRequestHelper(streamProvider);
    if (AmbariServer.getController() != null) {
      this.ambariEventPublisher = AmbariServer.getController().getAmbariEventPublisher();
    }
  }

  /**
   * Support properties with aggregate functions and metrics padding method.
   */
  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> supportedIds = new HashSet<>();
    for (String propertyId : propertyIds) {
      if (propertyId.startsWith(ZERO_PADDING_PARAM)
          || PropertyHelper.hasAggregateFunctionSuffix(propertyId)) {
        supportedIds.add(propertyId);
      }
    }
    propertyIds.removeAll(supportedIds);
    return propertyIds;
  }

  @Override
  public Set<Resource> populateResources(Set<Resource> resources,
               Request request, Predicate predicate) throws SystemException {

    Set<Resource> keepers = new HashSet<>();
    for (Resource resource : resources) {
      populateResource(resource, request, predicate);
      keepers.add(resource);
    }
    return keepers;
  }

  /**
   * Populate a resource by obtaining the requested Ganglia RESOURCE_METRICS.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   *
   * @return true if the resource was successfully populated with the requested properties
   *
   * @throws SystemException if unable to populate the resource
   */
  private boolean populateResource(Resource resource, Request request, Predicate predicate)
      throws SystemException {

    Set<String> propertyIds = getPropertyIds();

    if (propertyIds.isEmpty()) {
      return true;
    }

    metricsPaddingMethod = DEFAULT_PADDING_METHOD;

    Set<String> requestPropertyIds = request.getPropertyIds();
    if (requestPropertyIds != null && !requestPropertyIds.isEmpty()) {
      for (String propertyId : requestPropertyIds) {
        if (propertyId.startsWith(ZERO_PADDING_PARAM)) {
          String paddingStrategyStr = propertyId.substring(ZERO_PADDING_PARAM.length() + 1);
          metricsPaddingMethod = new MetricsPaddingMethod(
            MetricsPaddingMethod.PADDING_STRATEGY.valueOf(paddingStrategyStr));
        }
      }
    }

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    // Check liveliness of host
    if (!hostProvider.isCollectorHostLive(clusterName, TIMELINE_METRICS)) {
      if (printSkipPopulateMsgHostCounter.getAndIncrement() == 0) {
        LOG.info("METRICS_COLLECTOR host is not live. Skip populating " +
          "resources with metrics, next message will be logged after 1000 " +
          "attempts.");
      } else {
        printSkipPopulateMsgHostCounter.compareAndSet(1000, 0);
      }

      return true;
    }
    // reset
    printSkipPopulateMsgHostCompCounter.set(0);

    // Check liveliness of Collector
    if (!hostProvider.isCollectorComponentLive(clusterName, TIMELINE_METRICS)) {
      if (printSkipPopulateMsgHostCompCounter.getAndIncrement() == 0) {
        LOG.info("METRICS_COLLECTOR is not live. Skip populating resources" +
          " with metrics, next message will be logged after 1000 " +
          "attempts.");
      } else {
        printSkipPopulateMsgHostCompCounter.compareAndSet(1000, 0);
      }

      return true;
    }
    // reset
    printSkipPopulateMsgHostCompCounter.set(0);

    setProperties(resource, clusterName, request, getRequestPropertyIds(request, predicate));

    return true;
  }

  private void setProperties(Resource resource, String clusterName,
                            Request request, Set<String> ids) throws SystemException {

    Map<String, MetricReportRequest> reportRequestMap = getPropertyIdMaps(request, ids);
    String host = hostProvider.getCollectorHostName(clusterName, TIMELINE_METRICS);
    String port = hostProvider.getCollectorPort(clusterName, TIMELINE_METRICS);
    URIBuilder uriBuilder = AMSPropertyProvider.getAMSUriBuilder(host,
      port != null ? Integer.parseInt(port) : 6188, configuration.isHttpsEnabled());

    for (Map.Entry<String, MetricReportRequest> entry : reportRequestMap.entrySet()) {
      MetricReportRequest reportRequest = entry.getValue();
      TemporalInfo temporalInfo = reportRequest.getTemporalInfo();
      Map<String, String> propertyIdMap = reportRequest.getPropertyIdMap();

      uriBuilder.removeQuery();
      // Call with hostname = null
      uriBuilder.addParameter("metricNames",
        MetricsPropertyProvider.getSetString(propertyIdMap.keySet(), -1));

      uriBuilder.setParameter("appId", "HOST");

      if (clusterName != null && hostProvider.isCollectorHostExternal(clusterName)) {
        uriBuilder.setParameter("instanceId", clusterName);
      }

      long startTime = temporalInfo.getStartTime();
      if (startTime != -1) {
        uriBuilder.setParameter("startTime", String.valueOf(startTime));
      }

      long endTime = temporalInfo.getEndTime();
      if (endTime != -1) {
        uriBuilder.setParameter("endTime", String.valueOf(endTime));
      }

      TimelineAppMetricCacheKey metricCacheKey =
        new TimelineAppMetricCacheKey(propertyIdMap.keySet(), "HOST", temporalInfo);

      metricCacheKey.setSpec(uriBuilder.toString());

      // Self populating cache updates itself on every get with latest results
      TimelineMetrics timelineMetrics;
      try {
        if (metricCache != null && metricCacheKey.getTemporalInfo() != null) {
          timelineMetrics = metricCache.getAppTimelineMetricsFromCache(metricCacheKey);
        } else {
          timelineMetrics = requestHelper.fetchTimelineMetrics(uriBuilder,
            temporalInfo.getStartTimeMillis(),
            temporalInfo.getEndTimeMillis());
        }
      } catch (IOException io) {
        timelineMetrics = null;
        if (io instanceof SocketTimeoutException || io instanceof ConnectException) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Skip populating metrics on socket timeout exception.");
          }
          if (ambariEventPublisher != null) {
            ambariEventPublisher.publish(new MetricsCollectorHostDownEvent(clusterName, host));
          }
          break;
        }
      }

      if (timelineMetrics != null) {
        for (TimelineMetric metric : timelineMetrics.getMetrics()) {
          if (metric.getMetricName() != null && metric.getMetricValues() != null) {
            // Pad zeros or nulls if needed to a clone so we do not cache
            // padded values
            TimelineMetric timelineMetricClone = new TimelineMetric(metric);
            metricsPaddingMethod.applyPaddingStrategy(timelineMetricClone, temporalInfo);

            String propertyId = propertyIdMap.get(metric.getMetricName());
            if (propertyId != null) {
              resource.setProperty(propertyId, getValue(timelineMetricClone, temporalInfo));
            }
          }
        }
      }
    }
  }

  private Map<String, MetricReportRequest> getPropertyIdMaps(Request request, Set<String> ids) {
    Map<String, MetricReportRequest> propertyMap = new HashMap<>();

    for (String id : ids) {
      Map<String, PropertyInfo> propertyInfoMap = getPropertyInfoMap("*", id);

      for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {
        PropertyInfo propertyInfo = entry.getValue();
        String propertyId = entry.getKey();
        String amsId = propertyInfo.getAmsId();

        TemporalInfo temporalInfo = request.getTemporalInfo(id);

        if (temporalInfo != null && propertyInfo.isTemporal()) {
          String propertyName = propertyInfo.getPropertyId();
          String report = null;
          // format : report_name.metric_name
          int dotIndex = propertyName.lastIndexOf('.');
          if (dotIndex != -1){
            report = propertyName.substring(0, dotIndex);
          }
          if (report !=  null) {
            MetricReportRequest reportRequest = propertyMap.get(report);
            if (reportRequest == null) {
              reportRequest = new MetricReportRequest();
              propertyMap.put(report, reportRequest);
              reportRequest.setTemporalInfo(temporalInfo);
            }
            reportRequest.addPropertyId(amsId, propertyId);
          }
        }
      }
    }
    return propertyMap;
  }

  class MetricReportRequest {
    private TemporalInfo temporalInfo;
    private Map<String, String> propertyIdMap = new HashMap<>();

    public TemporalInfo getTemporalInfo() {
      return temporalInfo;
    }

    public void setTemporalInfo(TemporalInfo temporalInfo) {
      this.temporalInfo = temporalInfo;
    }

    public Map<String, String> getPropertyIdMap() {
      return propertyIdMap;
    }

    public void addPropertyId(String propertyName, String propertyId) {
      propertyIdMap.put(propertyName, propertyId);
    }
  }
}
