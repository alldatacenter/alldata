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
package org.apache.ambari.server.metrics.system.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.ServiceConfigVersionResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metrics.system.MetricsSink;
import org.apache.ambari.server.metrics.system.SingleMetric;
import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.cache.TimelineMetricsCache;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Ambari Server Metrics Sink implementation to push collected metrics to AMS.
 */
public class AmbariMetricSinkImpl extends AbstractTimelineMetricsSink implements MetricsSink {
  private static final String AMBARI_SERVER_APP_ID = "ambari_server";
  private Collection<String> collectorHosts;

  private String collectorUri;
  private String port;
  private String protocol;
  private String hostName;
  private AmbariManagementController ambariManagementController;
  private TimelineMetricsCache timelineMetricsCache;
  private boolean isInitialized = false;
  private boolean setInstanceId = false;
  private String instanceId;

  public AmbariMetricSinkImpl(AmbariManagementController amc) {
    this.ambariManagementController = amc;
  }

  @Override
  public void init(MetricsConfiguration configuration) {

    if (ambariManagementController == null) {
      return;
    }

    InternalAuthenticationToken authenticationToken = new InternalAuthenticationToken("admin");
    authenticationToken.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters == null || clusters.getClusters().isEmpty()) {
      LOG.info("No clusters configured.");
      return;
    }

    String ambariMetricsServiceName = "AMBARI_METRICS";
    collectorHosts = new HashSet<>();

    for (Map.Entry<String, Cluster> kv : clusters.getClusters().entrySet()) {
      String clusterName = kv.getKey();
      instanceId = clusterName;
      Cluster c = kv.getValue();
      Resource.Type type = Resource.Type.ServiceConfigVersion;

      //If Metrics Collector VIP settings are configured, use that.
      boolean externalHostConfigPresent = false;
      boolean externalPortConfigPresent = false;
      Config clusterEnv = c.getDesiredConfigByType(ConfigHelper.CLUSTER_ENV);
      if (clusterEnv != null) {
        Map<String, String> configs = clusterEnv.getProperties();

        String metricsCollectorExternalHosts = configs.get("metrics_collector_external_hosts");
        if (StringUtils.isNotEmpty(metricsCollectorExternalHosts)) {
          LOG.info("Setting Metrics Collector External Host : " + metricsCollectorExternalHosts);
          collectorHosts.addAll(Arrays.asList(metricsCollectorExternalHosts.split(",")));
          externalHostConfigPresent = true;
          setInstanceId = true;
        }

        String metricsCollectorExternalPort = configs.get("metrics_collector_external_port");
        if (StringUtils.isNotEmpty(metricsCollectorExternalPort)) {
          LOG.info("Setting Metrics Collector External Port : " + metricsCollectorExternalPort);
          port = metricsCollectorExternalPort;
          externalPortConfigPresent = true;
        }
      }

      Set<String> propertyIds = new HashSet<>();
      propertyIds.add(ServiceConfigVersionResourceProvider.CONFIGURATIONS_PROPERTY_ID);

      Predicate predicate = new PredicateBuilder().property(
        ServiceConfigVersionResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals(clusterName).and().property(
        ServiceConfigVersionResourceProvider.SERVICE_NAME_PROPERTY_ID).equals(ambariMetricsServiceName).and().property(
        ServiceConfigVersionResourceProvider.IS_CURRENT_PROPERTY_ID).equals("true").toPredicate();

      Request request = PropertyHelper.getReadRequest(propertyIds);

      ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        ambariManagementController);

      try {
        if ( !externalHostConfigPresent ) {
          //get collector host(s)
          Service service = c.getService(ambariMetricsServiceName);
          if (service != null) {
            for (String component : service.getServiceComponents().keySet()) {
              ServiceComponent sc = service.getServiceComponents().get(component);
              for (ServiceComponentHost serviceComponentHost : sc.getServiceComponentHosts().values()) {
                if (serviceComponentHost.getServiceComponentName().equals("METRICS_COLLECTOR")) {
                  collectorHosts.add(serviceComponentHost.getHostName());
                }
              }
            }
          }
        }

        // get collector port and protocol
        Set<Resource> resources = provider.getResources(request, predicate);

        for (Resource resource : resources) {
          if (resource != null) {
            ArrayList<LinkedHashMap<Object, Object>> configs = (ArrayList<LinkedHashMap<Object, Object>>)
              resource.getPropertyValue(ServiceConfigVersionResourceProvider.CONFIGURATIONS_PROPERTY_ID);
            for (LinkedHashMap<Object, Object> config : configs) {
              if (config != null && config.get("type").equals("ams-site")) {
                TreeMap<Object, Object> properties = (TreeMap<Object, Object>) config.get("properties");
                String timelineWebappAddress = (String) properties.get("timeline.metrics.service.webapp.address");
                if (!externalPortConfigPresent && StringUtils.isNotEmpty(timelineWebappAddress) && timelineWebappAddress.contains(":")) {
                  port = timelineWebappAddress.split(":")[1];
                }
                String httpPolicy = (String) properties.get("timeline.metrics.service.http.policy");
                protocol = httpPolicy.equals("HTTP_ONLY") ? "http" : "https";
                break;
              }
            }
          }
        }
      } catch (Exception e) {
        LOG.info("Exception caught when retrieving Collector URI", e);
      }
    }

    hostName = configuration.getProperty("ambariserver.hostname.override", getDefaultLocalHostName());
    LOG.info("Hostname used for ambari server metrics : " + hostName);

    if (protocol.contains("https")) {
      ComponentSSLConfiguration sslConfiguration = ComponentSSLConfiguration.instance();
      String trustStorePath = sslConfiguration.getTruststorePath();
      String trustStoreType = sslConfiguration.getTruststoreType();
      String trustStorePwd = sslConfiguration.getTruststorePassword();
      loadTruststore(trustStorePath, trustStoreType, trustStorePwd);
    }

    collectorUri = getCollectorUri(findPreferredCollectHost());

    int maxRowCacheSize = Integer.parseInt(configuration.getProperty(MAX_METRIC_ROW_CACHE_SIZE,
      String.valueOf(TimelineMetricsCache.MAX_RECS_PER_NAME_DEFAULT)));
    int metricsSendInterval = Integer.parseInt(configuration.getProperty(METRICS_SEND_INTERVAL,
      String.valueOf(TimelineMetricsCache.MAX_EVICTION_TIME_MILLIS)));

    timelineMetricsCache = new TimelineMetricsCache(maxRowCacheSize, metricsSendInterval);

    if (CollectionUtils.isNotEmpty(collectorHosts)) {
      LOG.info("Metric Sink initialized with collectorHosts : " + collectorHosts.toString());
      isInitialized = true;
    }
  }

  private String getDefaultLocalHostName() {
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      LOG.info("Error getting host address");
    }
    return null;
  }

  /**
   * Publish metrics to AMS.
   * @param metrics Set of metrics
   */
  @Override
  public void publish(List<SingleMetric> metrics) {

    //If Sink not yet initialized, drop the metrics on the floor.
    if (isInitialized) {
      List<TimelineMetric> metricList = getFilteredMetricList(metrics);
      if (!metricList.isEmpty()) {
        TimelineMetrics timelineMetrics = new TimelineMetrics();
        timelineMetrics.setMetrics(metricList);
        emitMetrics(timelineMetrics);
      }
    } else {
      LOG.debug("Metric Sink not yet initialized. Discarding metrics.");
    }
  }

  @Override
  public boolean isInitialized() {
    return isInitialized;
  }


  /**
   * Get a pre-formatted URI for the collector
   *
   * @param host
   */
  @Override
  protected String getCollectorUri(String host) {
    return constructTimelineMetricUri(protocol, host, port);
  }

  @Override
  protected String getCollectorProtocol() {
    return protocol;
  }

  @Override
  protected String getCollectorPort() {
    return port;
  }

  @Override
  protected int getTimeoutSeconds() {
    return 10;
  }

  /**
   * Get the zookeeper quorum for the cluster used to find collector
   *
   * @return String "host1:port1,host2:port2"
   */
  @Override
  protected String getZookeeperQuorum() {
    //Ignoring Zk Fallback.
    return null;
  }

  /**
   * Get pre-configured list of collectors available
   *
   * @return Collection<String> host1,host2
   */
  @Override
  protected Collection<String> getConfiguredCollectorHosts() {
    return collectorHosts;
  }

  /**
   * Get hostname used for calculating write shard.
   *
   * @return String "host1"
   */
  @Override
  protected String getHostname() {
    return hostName;
  }

  @Override
  protected boolean isHostInMemoryAggregationEnabled() {
    return false;
  }

  @Override
  protected int getHostInMemoryAggregationPort() {
    return 0;
  }

  @Override
  protected String getHostInMemoryAggregationProtocol() {
    return "http";
  }

  private List<TimelineMetric> getFilteredMetricList(List<SingleMetric> metrics) {
    final List<TimelineMetric> metricList = new ArrayList<>();
    for (SingleMetric metric : metrics) {

      String metricName = metric.getMetricName();
      Double value = metric.getValue();

      TimelineMetric timelineMetric = createTimelineMetric(metric.getTimestamp(), AMBARI_SERVER_APP_ID, metricName, value);
      timelineMetricsCache.putTimelineMetric(timelineMetric, false);
      TimelineMetric cachedMetric = timelineMetricsCache.getTimelineMetric(metricName);

      if (cachedMetric != null) {
        metricList.add(cachedMetric);
      }
    }
    return metricList;
  }

  private TimelineMetric createTimelineMetric(long currentTimeMillis, String component, String attributeName,
                                              Number attributeValue) {
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName(attributeName);
    timelineMetric.setHostName(hostName);
    if (setInstanceId) {
      timelineMetric.setInstanceId(instanceId);
    }
    timelineMetric.setAppId(component);
    timelineMetric.setStartTime(currentTimeMillis);

    timelineMetric.getMetricValues().put(currentTimeMillis, attributeValue.doubleValue());
    return timelineMetric;
  }
}
