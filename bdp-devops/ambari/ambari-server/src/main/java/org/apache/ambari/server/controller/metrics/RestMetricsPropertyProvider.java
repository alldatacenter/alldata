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


package org.apache.ambari.server.controller.metrics;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.StackDefinedPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.services.MetricsRetrievalService;
import org.apache.ambari.server.state.services.MetricsRetrievalService.MetricSourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resolves metrics like api/cluster/summary/nimbus.uptime For every metric,
 * finds a relevant JSON value and returns it as a resource property.
 * <p/>
 * This class will delegate responsibility for actually retrieving JSON data
 * from a remote URL to the {@link MetricsRetrievalService}. It will also
 * leverage the {@link MetricsRetrievalService} to provide cached {@link Map}
 * instances for given URLs.
 * <p/>
 * This is because the REST API workflow will attempt to read data from this
 * provider during the context of a live Jetty thread. As a result, any attempt
 * to read remote resources will cause a delay in returning a response code. On
 * small clusters this mormally isn't a problem. However, as the cluster
 * increases in size, the thread pool would not be able to keep pace and would
 * eventually cause REST API request threads to wait while remote JSON data is
 * retrieved.
 */
public class RestMetricsPropertyProvider extends ThreadPoolEnabledPropertyProvider {

  private static final Logger LOG =
      LoggerFactory.getLogger(RestMetricsPropertyProvider.class);

  @Inject
  private AmbariManagementController amc;

  @Inject
  private Clusters clusters;

  /**
   * Used to parse the REST JSON metrics.
   */
  @Inject
  private Gson gson;

  /**
   * Used to submit asynchronous requests for remote metrics as well as querying
   * cached metrics.
   */
  @Inject
  private MetricsRetrievalService metricsRetrievalService;

  private final Map<String, String> metricsProperties;
  private final StreamProvider streamProvider;
  private final String clusterNamePropertyId;
  private final String componentNamePropertyId;
  private final String statePropertyId;
  private final String componentName;

  private static final String DEFAULT_PORT_PROPERTY = "default_port";
  private static final String PORT_CONFIG_TYPE_PROPERTY = "port_config_type";
  private static final String PORT_PROPERTY_NAME_PROPERTY = "port_property_name";
  private static final String HTTPS_PORT_PROPERTY_NAME_PROPERTY = "https_port_property_name";

  /**
   * Protocol to use when connecting
   */
  private static final String PROTOCOL_OVERRIDE_PROPERTY = "protocol";
  private static final String HTTPS_PROTOCOL_PROPERTY = "https_property_name";
  private static final String HTTP_PROTOCOL = "http";
  private static final String HTTPS_PROTOCOL = "https";
  private static final String DEFAULT_PROTOCOL = HTTP_PROTOCOL;

  /**
   * String that separates JSON URL from path inside JSON in metrics path
   */
  public static final String URL_PATH_SEPARATOR = "##";

  /**
   * Symbol that separates names of nested JSON sections in metrics path
   */
  public static final String DOCUMENT_PATH_SEPARATOR = "#";


  /**
   * Create a REST property provider.
   *
   * @param metricsProperties       the map of per-component metrics properties
   * @param componentMetrics        the map of supported metrics for component
   * @param streamProvider          the stream provider
   * @param metricHostProvider     metricsHostProvider instance
   * @param clusterNamePropertyId   the cluster name property id
   * @param hostNamePropertyId      the host name property id
   * @param componentNamePropertyId the component name property id
   * @param statePropertyId         the state property id
   */
  @AssistedInject
  RestMetricsPropertyProvider(
      @Assisted("metricsProperties") Map<String, String> metricsProperties,
      @Assisted("componentMetrics") Map<String, Map<String, PropertyInfo>> componentMetrics,
      @Assisted("streamProvider") StreamProvider streamProvider,
      @Assisted("metricHostProvider") MetricHostProvider metricHostProvider,
      @Assisted("clusterNamePropertyId") String clusterNamePropertyId,
      @Assisted("hostNamePropertyId") @Nullable String hostNamePropertyId,
      @Assisted("componentNamePropertyId") String componentNamePropertyId,
      @Assisted("statePropertyId") @Nullable String statePropertyId,
      @Assisted("componentName") @Nullable String componentName) {
    super(componentMetrics, hostNamePropertyId, metricHostProvider, clusterNamePropertyId);
    this.metricsProperties = metricsProperties;
    this.streamProvider = streamProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;
    this.componentNamePropertyId = componentNamePropertyId;
    this.statePropertyId = statePropertyId;
    this.componentName = componentName;
  }

  // ----- MetricsProvider implementation ------------------------------------


  /**
   * Populate a resource by obtaining the requested REST properties.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   * @return the populated resource; null if the resource should NOT be
   *         part of the result set for the given predicate
   */
  @Override
  protected Resource populateResource(Resource resource,
                                      Request request, Predicate predicate, Ticket ticket)
      throws SystemException {

    // Remove request properties that request temporal information
    Set<String> ids = getRequestPropertyIds(request, predicate);
    Set<String> temporalIds = new HashSet<>();
    String resourceComponentName = (String) resource.getPropertyValue(componentNamePropertyId);

    if (!componentName.equals(resourceComponentName)) {
      return resource;
    }

    for (String id : ids) {
      if (request.getTemporalInfo(id) != null) {
        temporalIds.add(id);
      }
    }
    ids.removeAll(temporalIds);

    if (ids.isEmpty()) {
      // no properties requested
      return resource;
    }

    // Don't attempt to get REST properties if the resource is in
    // an unhealthy state
    if (statePropertyId != null) {
      String state = (String) resource.getPropertyValue(statePropertyId);
      if (state != null && !healthyStates.contains(state)) {
        return resource;
      }
    }

    Map<String, PropertyInfo> propertyInfos =
        getComponentMetrics().get(StackDefinedPropertyProvider.WRAPPED_METRICS_KEY);
    if (propertyInfos == null) {
      // If there are no metrics defined for the given component then there is nothing to do.
      return resource;
    }
    String protocol = null;
    String port = "-1";
    String hostname = null;
    try {
      String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);
      Cluster cluster = clusters.getCluster(clusterName);
      hostname = getHost(resource, clusterName, resourceComponentName);
      if (hostname == null) {
        String msg = String.format("Unable to get component REST metrics. " +
            "No host name for %s.", resourceComponentName);
        LOG.warn(msg);
        return resource;
      }
      protocol = resolveProtocol(cluster, hostname);
      port = resolvePort(cluster, hostname, resourceComponentName, metricsProperties, protocol);
    } catch (Exception e) {
      rethrowSystemException(e);
    }

    Set<String> resultIds = new HashSet<>();
    for (String id : ids){
      for (String metricId : propertyInfos.keySet()){
        if (metricId.startsWith(id)){
          resultIds.add(metricId);
        }
      }
    }

    // Extract set of URLs for metrics
    HashMap<String, Set<String>> urls = extractPropertyURLs(resultIds, propertyInfos);

    for (String url : urls.keySet()) {
      String spec = getSpec(protocol, hostname, port, url);

      // always submit a request to cache the latest data
      metricsRetrievalService.submitRequest(MetricSourceType.REST, streamProvider, spec);

      // check to see if there is a cached value and use it if there is
      Map<String, String> jsonMap = metricsRetrievalService.getCachedRESTMetric(spec);
      if (null == jsonMap) {
        return resource;
      }

      if (!ticket.isValid()) {
        return resource;
      }

      try {
        extractValuesFromJSON(jsonMap, urls.get(url), resource, propertyInfos);
      } catch (AmbariException ambariException) {
        AmbariException detailedException = new AmbariException(String.format(
            "Unable to get REST metrics from the for %s at %s", resourceComponentName, spec),
            ambariException);

        logException(detailedException);
      }
    }
    return resource;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> unsupported = new HashSet<>();
    for (String propertyId : propertyIds) {
      if (!getComponentMetrics().
          get(StackDefinedPropertyProvider.WRAPPED_METRICS_KEY).
          containsKey(propertyId)) {
        unsupported.add(propertyId);
      }
    }
    return unsupported;
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * If protocol is equal to HTTPS_PROTOCOL than returns HTTPS_PORT_PROPERTY_NAME_PROPERTY value from PORT_CONFIG_TYPE_PROPERTY
   * else uses port_config_type, port_property_name, default_port parameters from
   * metricsProperties to find out right port value for service
   *
   * @return determines REST port for service
   */
  protected String resolvePort(Cluster cluster, String hostname, String componentName,
                          Map<String, String> metricsProperties, String protocol)
      throws AmbariException {
    String portConfigType = null;
    String portPropertyNameInMetricsProperties = protocol.equalsIgnoreCase(HTTPS_PROTOCOL) ? HTTPS_PORT_PROPERTY_NAME_PROPERTY : PORT_PROPERTY_NAME_PROPERTY;
    String portPropertyName = null;
    if (metricsProperties.containsKey(PORT_CONFIG_TYPE_PROPERTY) &&
        metricsProperties.containsKey(portPropertyNameInMetricsProperties)) {
      portConfigType = metricsProperties.get(PORT_CONFIG_TYPE_PROPERTY);
      portPropertyName = metricsProperties.get(portPropertyNameInMetricsProperties);
    }
    String portStr = getPropertyValueByNameAndConfigType(portPropertyName, portConfigType, cluster, hostname);
    if (portStr == null) {
      if (metricsProperties.containsKey(DEFAULT_PORT_PROPERTY)) {
        portStr = metricsProperties.get(DEFAULT_PORT_PROPERTY);
      } else {
        String message = String.format("Can not determine REST port for " +
                "component %s. " +
                "Default REST port property %s is not defined at metrics.json " +
                "file for service, and there is no any other available ways " +
                "to determine port information.",
            componentName, DEFAULT_PORT_PROPERTY);
        throw new AmbariException(message);
      }
    }
      return portStr;
  }

  /**
   * Tries to get propertyName property from configType config for specified cluster and hostname
   * @param propertyName
   * @param configType
   * @param cluster
   * @param hostname
     * @return
     */
  private String getPropertyValueByNameAndConfigType(String propertyName, String configType, Cluster cluster, String hostname){
    String result = null;
    if (configType != null && propertyName != null) {
      try {
        Map<String, Map<String, String>> configTags =
                amc.findConfigurationTagsWithOverrides(cluster, hostname);
        if (configTags.containsKey(configType)) {
          Map<String, Map<String, String>> properties = amc.getConfigHelper().getEffectiveConfigProperties(cluster,
                  Collections.singletonMap(configType, configTags.get(configType)));
          Map<String, String> config = properties.get(configType);
          if (config != null && config.containsKey(propertyName)) {
            result = config.get(propertyName);
          }
        }
      } catch (AmbariException e) {
        String message = String.format("Can not extract configs for " +
                        "component = %s, hostname = %s, config type = %s, property name = %s", componentName,
                hostname, configType, propertyName);
        LOG.warn(message, e);
      }
      if (result == null) {
        String message = String.format(
                "Can not extract property for " +
                        "component %s from configurations. " +
                        "Config tag = %s, config key name = %s, " +
                        "hostname = %s. Probably metrics.json file for " +
                        "service is misspelled.",
                componentName, configType,
                propertyName, hostname);
        LOG.debug(message);
      }
    }
    return result;
  }

  /**
   * if HTTPS_PROTOCOL_PROPERTY is present in metrics properties then checks if it is present in PORT_CONFIG_TYPE_PROPERTY and returns "https" if it is.
   *
   * Otherwise extracts protocol type from metrics properties. If no protocol is defined,
   * uses default protocol.
   */
  private String resolveProtocol(Cluster cluster, String hostname) {
    String protocol = DEFAULT_PROTOCOL;
    if (metricsProperties.containsKey(PORT_CONFIG_TYPE_PROPERTY) && metricsProperties.containsKey(HTTPS_PROTOCOL_PROPERTY)) {
      String configType = metricsProperties.get(PORT_CONFIG_TYPE_PROPERTY);
      String propertyName = metricsProperties.get(HTTPS_PROTOCOL_PROPERTY);
      String value = getPropertyValueByNameAndConfigType(propertyName, configType, cluster, hostname);
      if (value != null) {
        return HTTPS_PROTOCOL;
      }
    }
    if (metricsProperties.containsKey(PROTOCOL_OVERRIDE_PROPERTY)) {
      protocol = metricsProperties.get(PROTOCOL_OVERRIDE_PROPERTY).toLowerCase();
      if (!protocol.equals(HTTP_PROTOCOL) && !protocol.equals(HTTPS_PROTOCOL)) {
        String message = String.format(
            "Unsupported protocol type %s, falling back to %s",
            protocol, DEFAULT_PROTOCOL);
        LOG.warn(message);
        protocol = DEFAULT_PROTOCOL;
      }
    } else {
      protocol = DEFAULT_PROTOCOL;
    }
    return protocol;
  }


  /**
   * Extracts JSON URL from metricsPath
   */
  private String extractMetricsURL(String metricsPath)
      throws IllegalArgumentException {
    return validateAndExtractPathParts(metricsPath)[0];
  }

  /**
   * Extracts part of metrics path that contains path through nested
   * JSON sections
   */
  private String extractDocumentPath(String metricsPath)
      throws IllegalArgumentException {
    return validateAndExtractPathParts(metricsPath)[1];
  }

  /**
   * Returns [MetricsURL, DocumentPath] or throws an exception
   * if metricsPath is invalid.
   */
  private String[] validateAndExtractPathParts(String metricsPath)
      throws IllegalArgumentException {
    String[] pathParts = metricsPath.split(URL_PATH_SEPARATOR);
    if (pathParts.length == 2) {
      return pathParts;
    } else {
      // This warning is expected to occur only on development phase
      String message = String.format(
          "Metrics path %s does not contain or contains" +
              "more than one %s sequence. That probably " +
              "means that the mentioned metrics path is misspelled. " +
              "Please check the relevant metrics.json file",
          metricsPath, URL_PATH_SEPARATOR);
      throw new IllegalArgumentException(message);
    }
  }


  /**
   * Returns a map <document_url, requested_property_ids>.
   * requested_property_ids contain a set of property IDs
   * that should be fetched for this URL. Doing
   * that allows us to extract document only once when getting few properties
   * from this document.
   *
   * @param ids set of property IDs that should be fetched
   */
  private HashMap<String, Set<String>> extractPropertyURLs(Set<String> ids,
                                                           Map<String, PropertyInfo> propertyInfos) {
    HashMap<String, Set<String>> result = new HashMap<>();
    for (String requestedPropertyId : ids) {
      PropertyInfo propertyInfo = propertyInfos.get(requestedPropertyId);

      String metricsPath = propertyInfo.getPropertyId();
      String url = extractMetricsURL(metricsPath);
      Set<String> set;
      if (!result.containsKey(url)) {
        set = new HashSet<>();
        result.put(url, set);
      } else {
        set = result.get(url);
      }
      set.add(requestedPropertyId);
    }
    return result;
  }


  /**
   * Extracts requested properties from a parsed {@link Map} of {@link String}.
   *
   * @param requestedPropertyIds
   *          a set of property IDs that should be fetched for this URL
   * @param resource
   *          all extracted values are placed into resource
   */
  private void extractValuesFromJSON(Map<String, String> jsonMap,
      Set<String> requestedPropertyIds, Resource resource, Map<String, PropertyInfo> propertyInfos)
      throws AmbariException {
    Type type = new TypeToken<Map<Object, Object>>() {}.getType();
    for (String requestedPropertyId : requestedPropertyIds) {
      PropertyInfo propertyInfo = propertyInfos.get(requestedPropertyId);
      String metricsPath = propertyInfo.getPropertyId();
      String documentPath = extractDocumentPath(metricsPath);
      String[] docPath = documentPath.split(DOCUMENT_PATH_SEPARATOR);
      Map<String, String> subMap = jsonMap;
      for (int i = 0; i < docPath.length; i++) {
        String pathElement = docPath[i];
        if (!subMap.containsKey(pathElement)) {
          String message = String.format(
              "Can not fetch %dth element of document path (%s) " +
                  "from json. Wrong metrics path: %s",
              i, pathElement, metricsPath);

          throw new AmbariException(message);
        }
        Object jsonSubElement = jsonMap.get(pathElement);
        if (i == docPath.length - 1) { // Reached target document section
          // Extract property value
          resource.setProperty(requestedPropertyId, jsonSubElement);
        } else { // Navigate to relevant document section
          subMap = gson.fromJson((JsonElement) jsonSubElement, type);
        }
      }
    }
  }

}
