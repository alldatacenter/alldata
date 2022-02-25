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

package org.apache.ambari.server.controller.jmx;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.ThreadPoolEnabledPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.state.services.MetricsRetrievalService;
import org.apache.ambari.server.state.services.MetricsRetrievalService.MetricSourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * The {@link JMXPropertyProvider} is used to retrieve JMX metrics from a given
 * {@link Request}. This class will delegate responsibility for actually
 * retrieving JMX data from a remote URL to the {@link MetricsRetrievalService}.
 * It will also leverage the {@link MetricsRetrievalService} to provide cached
 * {@link JMXMetricHolder} instances for given URLs.
 * <p/>
 * This is because the REST API workflow will attempt to read data from this
 * provider during the context of a live Jetty thread. As a result, any attempt
 * to read remote resources will cause a delay in returning a response code. On
 * small clusters this mormally isn't a problem. However, as the cluster
 * increases in size, the thread pool would not be able to keep pace and would
 * eventually cause REST API request threads to wait while remote JMX data is
 * retrieved.
 * <p/>
 * In general, this type of federated data collection is a poor design. Even
 * with a large enough threadpool there are simple use cases where the model
 * breaks down:
 * <ul>
 * <li>Concurrent users logged in, each creating their own requests and
 * exhausting the threadpool
 * <li>Misbehaving JMX endpoints which don't respond in a timely manner
 * </ul>
 * <p/>
 * For these reasons the {@link JMXPropertyProvider} will use a completely
 * asynchronous model through the {@link MetricsRetrievalService}. It should be
 * noted that this provider is still an instance of a
 * {@link ThreadPoolEnabledPropertyProvider} due to the nature of how the cached
 * {@link JMXMetricHolder} instances need to be looped over an parsed.
 */
public class JMXPropertyProvider extends ThreadPoolEnabledPropertyProvider {

  private static final String NAME_KEY = "name";
  private static final String PORT_KEY = "tag.port";
  private static final String DOT_REPLACEMENT_CHAR = "#";

  private static final Map<String, String> DEFAULT_JMX_PORTS = new HashMap<>();

  /**
   * When Ambari queries NameNode's HA state (among other metrics), it retrieves all metrics from "NN_URL:port/jmx".
   * But some metrics may compete for the NameNode lock and a request to /jmx may take much time.
   * <p>
   * The properties from this map will be retrieved using a provided URL query.
   * Even if JMX is locked and a request for all metrics is waiting (/jmx is unavailable),
   * HAState will be updated via a separate JMX call.
   * <p>
   * Currently org.apache.hadoop.jmx.JMXJsonServlet can provide only one property per a request,
   * each property from this list adds a request to JMX.
   */
  private static final Map<String, Map<String, String>> AD_HOC_PROPERTIES = new HashMap<>();

  static {
    DEFAULT_JMX_PORTS.put("NAMENODE",           "50070");
    DEFAULT_JMX_PORTS.put("DATANODE",           "50075");
    DEFAULT_JMX_PORTS.put("HBASE_MASTER",       "60010");
    DEFAULT_JMX_PORTS.put("HBASE_REGIONSERVER", "60030");
    DEFAULT_JMX_PORTS.put("RESOURCEMANAGER",     "8088");
    DEFAULT_JMX_PORTS.put("HISTORYSERVER",      "19888");
    DEFAULT_JMX_PORTS.put("NODEMANAGER",         "8042");
    DEFAULT_JMX_PORTS.put("JOURNALNODE",         "8480");
    DEFAULT_JMX_PORTS.put("STORM_REST_API",      "8745");
    DEFAULT_JMX_PORTS.put("OZONE_MANAGER",       "9874");
    DEFAULT_JMX_PORTS.put("STORAGE_CONTAINER_MANAGER", "9876");

    AD_HOC_PROPERTIES.put("NAMENODE",
        Collections.singletonMap("metrics/dfs/FSNamesystem/HAState",
                                 "/jmx?get=Hadoop:service=NameNode,name=FSNamesystem::tag.HAState"));
  }

  private static final Logger LOG =
      LoggerFactory.getLogger(JMXPropertyProvider.class);

  private static final Pattern dotReplacementCharPattern =
    Pattern.compile(DOT_REPLACEMENT_CHAR);

  private final StreamProvider streamProvider;

  private final JMXHostProvider jmxHostProvider;

  private final String clusterNamePropertyId;

  private final String hostNamePropertyId;

  private final String componentNamePropertyId;

  private final String statePropertyId;

  private final Map<String, String> clusterComponentPortsMap;

  /**
   * Used to submit asynchronous requests for remote metrics as well as querying
   * cached metrics.
   */
  @Inject
  private MetricsRetrievalService metricsRetrievalService;

  // ----- Constructors ------------------------------------------------------

  /**
   * Create a JMX property provider.
   *
   * @param componentMetrics         the map of supported metrics
   * @param streamProvider           the stream provider
   * @param jmxHostProvider          the JMX host mapping
   * @param metricHostProvider      the host mapping
   * @param clusterNamePropertyId    the cluster name property id
   * @param hostNamePropertyId       the host name property id
   * @param componentNamePropertyId  the component name property id
   * @param statePropertyId          the state property id
   */
  @AssistedInject
  JMXPropertyProvider(
      @Assisted("componentMetrics") Map<String, Map<String, PropertyInfo>> componentMetrics,
      @Assisted("streamProvider") StreamProvider streamProvider,
      @Assisted("jmxHostProvider") JMXHostProvider jmxHostProvider,
      @Assisted("metricHostProvider") MetricHostProvider metricHostProvider,
      @Assisted("clusterNamePropertyId") String clusterNamePropertyId,
      @Assisted("hostNamePropertyId") @Nullable String hostNamePropertyId,
      @Assisted("componentNamePropertyId") String componentNamePropertyId,
      @Assisted("statePropertyId") @Nullable String statePropertyId) {

    super(componentMetrics, hostNamePropertyId, metricHostProvider, clusterNamePropertyId);

    this.streamProvider           = streamProvider;
    this.jmxHostProvider          = jmxHostProvider;
    this.clusterNamePropertyId    = clusterNamePropertyId;
    this.hostNamePropertyId       = hostNamePropertyId;
    this.componentNamePropertyId  = componentNamePropertyId;
    this.statePropertyId          = statePropertyId;
    clusterComponentPortsMap = new HashMap<>();
  }

  // ----- helper methods ----------------------------------------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) throws SystemException {
    clusterComponentPortsMap.clear();
    return super.populateResources(resources, request, predicate);
  }

  /**
   * Populate a resource by obtaining the requested JMX properties.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   * @param ticket    a valid ticket
   *
   * @return the populated resource; null if the resource should NOT be part of the result set for the given predicate
   */
  @Override
  protected Resource populateResource(Resource resource, Request request, Predicate predicate, Ticket ticket)
      throws SystemException {

    Set<String> ids = getRequestPropertyIds(request, predicate);
    Set<String> unsupportedIds = new HashSet<>();
    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);

    if (getComponentMetrics().get(componentName) == null) {
      // If there are no metrics defined for the given component then there is nothing to do.
      return resource;
    }

    for (String id : ids) {
      if (request.getTemporalInfo(id) != null) {
        unsupportedIds.add(id);
      }
      if (!isSupportedPropertyId(componentName, id)) {
        unsupportedIds.add(id);
      }
    }

    ids.removeAll(unsupportedIds);

    if (ids.isEmpty()) {
      // no properties requested
      return resource;
    }

    // Don't attempt to get the JMX properties if the resource is in an unhealthy state
    if (statePropertyId != null) {
      String state = (String) resource.getPropertyValue(statePropertyId);
      if (state != null && !healthyStates.contains(state)) {
        return resource;
      }
    }

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    String protocol = jmxHostProvider.getJMXProtocol(clusterName, componentName);

    boolean httpsEnabled = false;

    if (protocol.equals("https")){
      httpsEnabled = true;
    }

    Set<String> hostNames = getHosts(resource, clusterName, componentName);
    if (hostNames == null || hostNames.isEmpty()) {
      LOG.warn("Unable to get JMX metrics.  No host name for " + componentName);
      return resource;
    }

    String spec = null;
    for (String hostName : hostNames) {
      try {
        String port = getPort(clusterName, componentName, hostName, httpsEnabled);
        String publicHostName = jmxHostProvider.getPublicHostName(clusterName, hostName);

        if (port == null) {
          LOG.warn("Unable to get JMX metrics.  No port value for " + componentName);
          return resource;
        }

        // build the URL
        String jmxUrl = getSpec(protocol, hostName, port, "/jmx");

        // always submit a request to cache the latest data
        metricsRetrievalService.submitRequest(MetricSourceType.JMX, streamProvider, jmxUrl);

        // check to see if there is a cached value and use it if there is
        JMXMetricHolder jmxMetricHolder = metricsRetrievalService.getCachedJMXMetric(jmxUrl);

        if( jmxMetricHolder == null && !hostName.equalsIgnoreCase(publicHostName)) {
          // build the URL using public host name
          String publicJmxUrl = getSpec(protocol, publicHostName, port, "/jmx");

          // always submit a request to cache the latest data
          metricsRetrievalService.submitRequest(MetricSourceType.JMX, streamProvider, publicJmxUrl);

          // check to see if there is a cached value and use it if there is
          jmxMetricHolder = metricsRetrievalService.getCachedJMXMetric(publicJmxUrl);
        }

        // if the ticket becomes invalid (timeout) then bail out
        if (!ticket.isValid()) {
          return resource;
        }

        if (null != jmxMetricHolder) {
          getHadoopMetricValue(jmxMetricHolder, ids, resource, request, ticket);
        }

        if (AD_HOC_PROPERTIES.containsKey(componentName)) {
          for (String propertyId : ids) {
            for (String adHocId : AD_HOC_PROPERTIES.get(componentName).keySet()) {
              String queryURL = null;
              // if all metrics from "metrics/dfs/FSNamesystem" were requested, retrieves HAState.
              if (adHocId.equals(propertyId) || adHocId.startsWith(propertyId + '/')) {
                queryURL = AD_HOC_PROPERTIES.get(componentName).get(adHocId);
              }
              if (queryURL != null) {
                String adHocUrl = getSpec(protocol, hostName, port, queryURL);
                metricsRetrievalService.submitRequest(MetricSourceType.JMX, streamProvider, adHocUrl);
                JMXMetricHolder adHocJMXMetricHolder = metricsRetrievalService.getCachedJMXMetric(adHocUrl);

                if( adHocJMXMetricHolder == null && !hostName.equalsIgnoreCase(publicHostName)) {
                  // build the adhoc URL using public host name
                  String publicAdHocUrl = getSpec(protocol, publicHostName, port, queryURL);

                  // always submit a request to cache the latest data
                  metricsRetrievalService.submitRequest(MetricSourceType.JMX, streamProvider, publicAdHocUrl);

                  // check to see if there is a cached value and use it if there is
                  adHocJMXMetricHolder = metricsRetrievalService.getCachedJMXMetric(publicAdHocUrl);
                }

                // if the ticket becomes invalid (timeout) then bail out
                if (!ticket.isValid()) {
                  return resource;
                }
                if (null != adHocJMXMetricHolder) {
                  getHadoopMetricValue(adHocJMXMetricHolder, Collections.singleton(propertyId), resource, request, ticket);
                }
              }
            }
          }
        }
      } catch (IOException e) {
        AmbariException detailedException = new AmbariException(String.format(
            "Unable to get JMX metrics from the host %s for the component %s. Spec: %s", hostName,
            componentName, spec), e);
        logException(detailedException);
      }
    }

    return resource;
  }

  /**
   * Hadoop-specific metrics fetching
   */
  private void getHadoopMetricValue(JMXMetricHolder metricHolder, Set<String> ids,
                       Resource resource, Request request, Ticket ticket) throws IOException {

    Map<String, Map<String, Object>> categories = new HashMap<>();
    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    for (Map<String, Object> bean : metricHolder.getBeans()) {
      String category = getCategory(bean, clusterName, componentName);
      if (category != null) {
        categories.put(category, bean);
      }
    }

    for (String propertyId : ids) {
      Map<String, PropertyInfo> propertyInfoMap = getPropertyInfoMap(componentName, propertyId);

      String requestedPropertyId = propertyId;

      for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {

        PropertyInfo propertyInfo = entry.getValue();
        propertyId = entry.getKey();

        if (propertyInfo.isPointInTime()) {

          String property = propertyInfo.getPropertyId();
          String category = "";

          List<String> keyList = new LinkedList<>();

          int keyStartIndex = property.indexOf('[');
          if (-1 != keyStartIndex) {
            int keyEndIndex = property.indexOf(']', keyStartIndex);
            if (-1 != keyEndIndex && keyEndIndex > keyStartIndex) {
              keyList.add(property.substring(keyStartIndex+1, keyEndIndex));
            }
          }

          if (!containsArguments(propertyId)) {
            int dotIndex = property.indexOf('.', property.indexOf('='));
            if (-1 != dotIndex) {
              category = property.substring(0, dotIndex);
              property = (-1 == keyStartIndex) ?
                      property.substring(dotIndex+1) :
                      property.substring(dotIndex+1, keyStartIndex);
            }
          } else {
            int firstKeyIndex = keyStartIndex > -1 ? keyStartIndex : property.length();
            int dotIndex = property.lastIndexOf('.', firstKeyIndex);

            if (dotIndex != -1) {
              category = property.substring(0, dotIndex);
              property = property.substring(dotIndex + 1, firstKeyIndex);
            }
          }

          if (containsArguments(propertyId)) {
            Pattern pattern = Pattern.compile(category);

            // find all jmx categories that match the regex
            for (String jmxCat : categories.keySet()) {
              Matcher matcher = pattern.matcher(jmxCat);
              if (matcher.matches()) {
                String newPropertyId = propertyId;
                for (int i = 0; i < matcher.groupCount(); i++) {
                  newPropertyId = substituteArgument(newPropertyId, "$" + (i + 1), matcher.group(i + 1));
                }
                // We need to do the final filtering here, after the argument substitution
                if (isRequestedPropertyId(newPropertyId, requestedPropertyId, request)) {
                  if (!ticket.isValid()) {
                    return;
                  }
                  setResourceValue(resource, categories, newPropertyId, jmxCat, property, keyList);
                }
              }
            }
          } else {
            if (!ticket.isValid()) {
              return;
            }
            setResourceValue(resource, categories, propertyId, category, property, keyList);
          }
        }
      }
    }
  }

  private void setResourceValue(Resource resource, Map<String, Map<String, Object>> categories, String propertyId,
                                String category, String property, List<String> keyList) {
    Map<String, Object> properties = categories.get(category);
    if (property.contains(DOT_REPLACEMENT_CHAR)) {
      property = dotReplacementCharPattern.matcher(property).replaceAll(".");
    }
    if (properties != null && properties.containsKey(property)) {
      Object value = properties.get(property);
      if (keyList.size() > 0 && value instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) value;
        for (String key : keyList) {
          value = map.get(key);
          if (value instanceof Map) {
            map = (Map<?, ?>) value;
          }
          else {
            break;
          }
        }
      }
      resource.setProperty(propertyId, value);
    }
  }

  private String getPort(String clusterName, String componentName, String hostName, boolean httpsEnabled) throws SystemException {
    String portMapKey = String.format("%s-%s-%s", clusterName, componentName, httpsEnabled);
    String port = clusterComponentPortsMap.get(portMapKey);
    if (port==null) {
      port = jmxHostProvider.getPort(clusterName, componentName, hostName, httpsEnabled);
      port = port == null ? DEFAULT_JMX_PORTS.get(componentName) : port;
      clusterComponentPortsMap.put(portMapKey, port);
    }
    return port;
  }


  private Set<String> getHosts(Resource resource, String clusterName, String componentName) {
    return hostNamePropertyId == null ?
            jmxHostProvider.getHostNames(clusterName, componentName) :
            Collections.singleton((String) resource.getPropertyValue(hostNamePropertyId));
  }

  private String getCategory(Map<String, Object> bean, String clusterName, String componentName) {
    if (bean.containsKey(NAME_KEY)) {
      String name = (String) bean.get(NAME_KEY);

      if (bean.containsKey(PORT_KEY)) {
        String port = (String) bean.get(PORT_KEY);
        String tag = jmxHostProvider.getJMXRpcMetricTag(clusterName, componentName, port);
        name = name.replace("ForPort" + port, tag == null ? "" : ",tag=" + tag);
      }
      return name;
    }
    return null;
  }
}
