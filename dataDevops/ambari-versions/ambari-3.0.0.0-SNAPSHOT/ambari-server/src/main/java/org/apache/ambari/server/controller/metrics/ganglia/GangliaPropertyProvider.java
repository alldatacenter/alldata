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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.MetricsPropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract property provider implementation for a Ganglia source.
 */
public abstract class GangliaPropertyProvider extends MetricsPropertyProvider {

  /**
   * Map of Ganglia cluster names keyed by component type.
   */
  static final Map<String, List<String>> GANGLIA_CLUSTER_NAME_MAP = new HashMap<>();

  
  static {
    GANGLIA_CLUSTER_NAME_MAP.put("NAMENODE",           Collections.singletonList("HDPNameNode"));
    GANGLIA_CLUSTER_NAME_MAP.put("DATANODE",           Arrays.asList("HDPDataNode", "HDPSlaves"));
    GANGLIA_CLUSTER_NAME_MAP.put("JOBTRACKER",         Collections.singletonList("HDPJobTracker"));
    GANGLIA_CLUSTER_NAME_MAP.put("TASKTRACKER",        Arrays.asList("HDPTaskTracker", "HDPSlaves"));
    GANGLIA_CLUSTER_NAME_MAP.put("RESOURCEMANAGER",    Collections.singletonList("HDPResourceManager"));
    GANGLIA_CLUSTER_NAME_MAP.put("NODEMANAGER",        Arrays.asList("HDPNodeManager", "HDPSlaves"));
    GANGLIA_CLUSTER_NAME_MAP.put("HISTORYSERVER",      Collections.singletonList("HDPHistoryServer"));
    GANGLIA_CLUSTER_NAME_MAP.put("HBASE_MASTER",       Collections.singletonList("HDPHBaseMaster"));
    GANGLIA_CLUSTER_NAME_MAP.put("HBASE_REGIONSERVER", Arrays.asList("HDPHBaseRegionServer", "HDPSlaves"));
    GANGLIA_CLUSTER_NAME_MAP.put("FLUME_HANDLER",      Arrays.asList("HDPFlumeServer", "HDPSlaves"));
    GANGLIA_CLUSTER_NAME_MAP.put("JOURNALNODE",        Arrays.asList("HDPJournalNode", "HDPSlaves"));
    GANGLIA_CLUSTER_NAME_MAP.put("NIMBUS",             Collections.singletonList("HDPNimbus"));
    GANGLIA_CLUSTER_NAME_MAP.put("SUPERVISOR",         Collections.singletonList("HDPSupervisor"));
  }

  private static final Logger LOG =
      LoggerFactory.getLogger(GangliaPropertyProvider.class);

  // ----- Constructors ------------------------------------------------------

  public GangliaPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                                 URLStreamProvider streamProvider,
                                 ComponentSSLConfiguration configuration,
                                 MetricHostProvider hostProvider,
                                 String clusterNamePropertyId,
                                 String hostNamePropertyId,
                                 String componentNamePropertyId) {

    super(componentPropertyInfoMap, streamProvider,configuration,
      hostProvider, clusterNamePropertyId, hostNamePropertyId,
      componentNamePropertyId);
  }


  // ----- PropertyProvider --------------------------------------------------

  @Override
  public Set<Resource> populateResourcesWithProperties(Set<Resource> resources,
            Request request, Set<String> propertyIds) throws SystemException {

    Map<String, Map<TemporalInfo, RRDRequest>> requestMap = getRRDRequests(resources, request, propertyIds);

    // For each cluster...
    for (Map.Entry<String, Map<TemporalInfo, RRDRequest>> clusterEntry : requestMap.entrySet()) {
      // For each request ...
      for (RRDRequest rrdRequest : clusterEntry.getValue().values() ) {
        //todo: property provider can reduce set of resources
        rrdRequest.populateResources();
      }
    }
    return resources;
  }


  // ----- GangliaPropertyProvider -------------------------------------------


  /**
   * Get the ganglia cluster name for the given resource.
   *
   *
   * @param resource  the resource
   *
   * @return the ganglia cluster name
   */
  protected abstract Set<String> getGangliaClusterNames(Resource resource, String clusterName);


  /**
   * Get the component name property id.
   *
   * @return the component name property id
   */
  protected String getComponentNamePropertyId() {
    return componentNamePropertyId;
  }

  /**
   * Get the host name property id.
   *
   * @return the host name property id
   */
  protected String getHostNamePropertyId() {
    return hostNamePropertyId;
  }

  /**
   * Get the stream provider.
   *
   * @return the stream provider
   */
  public StreamProvider getStreamProvider() {
    return streamProvider;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get the request objects containing all the information required to
   * make single requests to the Ganglia rrd script.
   * Requests are created per cluster name / temporal information but
   * can span multiple resources and metrics.
   *
   * @param resources  the resources being populated
   * @param request    the request
   * @param ids        the relevant property ids
   *
   * @return a map of maps of rrd requests keyed by cluster name / temporal info
   */
  private Map<String, Map<TemporalInfo, RRDRequest>> getRRDRequests(Set<Resource> resources,
                                                                    Request request,
                                                                    Set<String> ids) {

    Map<String, Map<TemporalInfo, RRDRequest>> requestMap =
      new HashMap<>();

    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);
      Map<TemporalInfo, RRDRequest> requests = requestMap.get(clusterName);
      if (requests == null) {
        requests = new HashMap<>();
        requestMap.put(clusterName, requests);
      }

      Set<String> gangliaClusterNames = getGangliaClusterNames(resource, clusterName);

      for (String gangliaClusterName : gangliaClusterNames) {
        ResourceKey key =
            new ResourceKey(getHostName(resource), gangliaClusterName);

        for (String id : ids) {
          Map<String, PropertyInfo> propertyInfoMap = new HashMap<>();

          Map<String, PropertyInfo> componentMetricMap =
            getComponentMetrics().get(getComponentName(resource));

          // Not all components have metrics
          if (componentMetricMap != null &&
              !componentMetricMap.containsKey(id)) {
            updateComponentMetricMap(componentMetricMap, id);
          }

          updatePropertyInfoMap(getComponentName(resource), id, propertyInfoMap);

          for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {
            String propertyId = entry.getKey();
            PropertyInfo propertyInfo = entry.getValue();

            TemporalInfo temporalInfo = request.getTemporalInfo(id);

            if ((temporalInfo == null && propertyInfo.isPointInTime()) || (temporalInfo != null && propertyInfo.isTemporal())) {
              RRDRequest rrdRequest = requests.get(temporalInfo);
              if (rrdRequest == null) {
                rrdRequest = new RRDRequest(clusterName, temporalInfo);
                requests.put(temporalInfo, rrdRequest);
              }
              rrdRequest.putResource(key, resource);              
              rrdRequest.putPropertyId(propertyInfo.getPropertyId(), propertyId);
            }
          }
        }
      }
    }
    return requestMap;
  }

  /**
   * Get the spec to locate the Ganglia stream from the given
   * request info.
   *
   * @param clusterName   the cluster name
   * @param clusterSet    the set of ganglia cluster names
   * @param hostSet       the set of host names
   * @param metricSet     the set of metric names
   * @param temporalInfo  the temporal information
   *
   * @return the spec, like http://example.com/path?param1=val1&paramn=valn
   *
   * @throws org.apache.ambari.server.controller.spi.SystemException if unable to get the Ganglia Collector host name
   */
  private String getSpec(String clusterName,
                         Set<String> clusterSet,
                         Set<String> hostSet,
                         Set<String> metricSet,
                         TemporalInfo temporalInfo) throws SystemException {
    
    String clusters = getSetString(clusterSet, -1);
    String hosts = getSetString(hostSet, -1);
    String metrics = getSetString(metricSet, -1);
    
    URIBuilder uriBuilder = new URIBuilder();

    if (configuration.isHttpsEnabled()) {
      uriBuilder.setScheme("https");
    } else {
      uriBuilder.setScheme("http");
    }

    uriBuilder.setHost(hostProvider.getCollectorHostName(clusterName, GANGLIA));

    uriBuilder.setPath("/cgi-bin/rrd.py");
    
    uriBuilder.setParameter("c", clusters);

    if (hosts.length() > 0) {
      uriBuilder.setParameter("h", hosts);
    }

    if (metrics.length() > 0) {
      uriBuilder.setParameter("m", metrics);
    } else {
      // get all metrics
      uriBuilder.setParameter("m", ".*");
    }

    if (temporalInfo != null) {
      long startTime = temporalInfo.getStartTime();
      if (startTime != -1) {
        uriBuilder.setParameter("s", String.valueOf(startTime));
      }

      long endTime = temporalInfo.getEndTime();
      if (endTime != -1) {
        uriBuilder.setParameter("e", String.valueOf(endTime));
      }

      long step = temporalInfo.getStep();
      if (step != -1) {
        uriBuilder.setParameter("r", String.valueOf(step));
      }
    } else {
      uriBuilder.setParameter("e", "now");
      uriBuilder.setParameter("pt", "true");
    }

    return uriBuilder.toString();
  }
  

  /**
   * Get value from the given metric.
   *
   * @param metric      the metric
   * @param isTemporal  indicates whether or not this a temporal metric
   *
   * @return a range of temporal data or a point in time value if not temporal
   */
  private static Object getValue(GangliaMetric metric, boolean isTemporal) {
    Number[][] dataPoints = metric.getDatapoints();

    int length = dataPoints.length;
    if (isTemporal) {
      return length > 0 ? dataPoints : null;
    } else {
      // return the value of the last data point
      return length > 0 ? dataPoints[length - 1][0] : 0;
    }
  }

  // ----- inner classes -----------------------------------------------------


  // ----- RRDRequest ----------------------------------------------------

  /**
   * The information required to make a single RRD request.
   */
  private class RRDRequest {
    private static final int POPULATION_TIME_UPPER_LIMIT = 5;
    private final String clusterName;
    private final TemporalInfo temporalInfo;
    private final Map<ResourceKey, Set<Resource>> resources = new HashMap<>();
    private final Map<String, Set<String>> metrics = new HashMap<>();
    private final Set<String> clusterSet = new HashSet<>();
    private final Set<String> hostSet = new HashSet<>();


    private RRDRequest(String clusterName, TemporalInfo temporalInfo) {
      this.clusterName  = clusterName;
      this.temporalInfo = temporalInfo;
    }

    public void putResource(ResourceKey key, Resource resource) {
      clusterSet.add(key.getClusterName());
      hostSet.add(key.getHostName());
      Set<Resource> resourceSet = resources.get(key);
      if (resourceSet == null) {
        resourceSet = new HashSet<>();
        resources.put(key, resourceSet);
      }
      resourceSet.add(resource);
    }

    public void putPropertyId(String metric, String id) {
      Set<String> propertyIds = metrics.get(metric);

      if (propertyIds == null) {
        propertyIds = new HashSet<>();
        metrics.put(metric, propertyIds);
      }
      propertyIds.add(id);
    }

    /**
     * Populate the associated resources by making the rrd request.
     *
     * @return a collection of populated resources
     *
     * @throws org.apache.ambari.server.controller.spi.SystemException if unable to populate the resources
     */
    public Collection<Resource> populateResources() throws SystemException {

      //Get full url with parameters
      String specWithParams = getSpec(clusterName, clusterSet, hostSet, metrics.keySet(), temporalInfo);
      
      //URL
      String spec = null;
      //Parameters
      String params = null;
      
      String[] tokens = questionMarkPattern.split(specWithParams, 2);

      try {
        spec = tokens[0];
        params = tokens[1];
      } catch (ArrayIndexOutOfBoundsException e) {
        LOG.info(e.toString());
      }
      

      BufferedReader reader = null;
      try {
        
        //Check if host is live
        if (!hostProvider.isCollectorHostLive(clusterName, GANGLIA)) {
          LOG.info("Ganglia host is not live");
          return Collections.emptySet();
        }

        //Check if Ganglia server component is live
        if (!hostProvider.isCollectorComponentLive(clusterName, GANGLIA)) {
          LOG.info("Ganglia server component is not live");
          return Collections.emptySet();
        }

        reader = new BufferedReader(new InputStreamReader(
            getStreamProvider().readFrom(spec, "POST", params)));

        String feedStart = reader.readLine();
        if (feedStart == null || feedStart.isEmpty()) {
          LOG.info("Empty feed while getting ganglia metrics for spec => "+
            spec);
          return Collections.emptySet();
        }
        int startTime = convertToNumber(feedStart).intValue();

        String dsName = reader.readLine();
        if (dsName == null || dsName.isEmpty()) {
          LOG.info("Feed without body while reading ganglia metrics for spec " +
            "=> " + spec);
          return Collections.emptySet();
        }

        while(!"[~EOF]".equals(dsName)) {
          GangliaMetric metric = new GangliaMetric();
          List<GangliaMetric.TemporalMetric> listTemporalMetrics =
            new ArrayList<>();

          metric.setDs_name(dsName);
          metric.setCluster_name(reader.readLine());
          metric.setHost_name(reader.readLine());
          metric.setMetric_name(reader.readLine());

          String timeStr = reader.readLine();
          String stepStr = reader.readLine();
          if (timeStr == null || timeStr.isEmpty() || stepStr == null
              || stepStr.isEmpty()) {
            LOG.info("Unexpected end of stream reached while getting ganglia " +
                "metrics for spec => " + spec);
            return Collections.emptySet();
          }
          int time = convertToNumber(timeStr).intValue();
          int step = convertToNumber(stepStr).intValue();

          String val     = reader.readLine();
          String lastVal = null;

          while(val!=null && !"[~EOM]".equals(val)) {
            if (val.startsWith("[~r]")) {
              int repeat = Integer.parseInt(val.substring(4)) - 1;
              for (int i = 0; i < repeat; ++i) {
                if (! "[~n]".equals(lastVal)) {
                  GangliaMetric.TemporalMetric tm = new GangliaMetric.TemporalMetric(lastVal, time);
                  if (tm.isValid()) listTemporalMetrics.add(tm);
                }
                time += step;
              }
            } else {
              if (! "[~n]".equals(val)) {
                GangliaMetric.TemporalMetric tm = new GangliaMetric.TemporalMetric(val, time);
                if (tm.isValid()) listTemporalMetrics.add(tm);
              }
              time += step;
            }
            lastVal = val;
            val = reader.readLine();
          }

          metric.setDatapointsFromList(listTemporalMetrics);

          ResourceKey key = new ResourceKey(metric.getHost_name(), metric.getCluster_name());
          Set<Resource> resourceSet = resources.get(key);
          if (resourceSet != null) {
            for (Resource resource : resourceSet) {
              populateResource(resource, metric);
            }
          }

          dsName = reader.readLine();
          if (dsName == null || dsName.isEmpty()) {
            LOG.info("Unexpected end of stream reached while getting ganglia " +
              "metrics for spec => " + spec);
            return Collections.emptySet();
          }
        }
        String feedEnd = reader.readLine();
        if (feedEnd == null || feedEnd.isEmpty()) {
          LOG.info("Error reading end of feed while getting ganglia metrics " +
            "for spec => " + spec);
        } else {

          int endTime = convertToNumber(feedEnd).intValue();
          int totalTime = endTime - startTime;
          if (LOG.isInfoEnabled() && totalTime > POPULATION_TIME_UPPER_LIMIT) {
            LOG.info("Ganglia resource population time: " + totalTime);
          }
        }
      } catch (IOException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Caught exception getting Ganglia metrics : spec=" + spec);
        }
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
              LOG.warn("Unable to close http input steam : spec=" + spec, e);
            }
          }
        }
      }
      //todo: filter out resources and return keepers
      return Collections.emptySet();
    }


    /**
     * Populate the given resource with the given Ganglia metric.
     *
     * @param resource       the resource
     * @param gangliaMetric  the Ganglia metrics
     */
    private void populateResource(Resource resource, GangliaMetric gangliaMetric) {
      String metric_name = gangliaMetric.getMetric_name();
      Set<String> propertyIdSet = null;
      List<String> parameterList = null;
      if (metric_name != null) {
        propertyIdSet = metrics.get(metric_name);
        parameterList = new LinkedList<>();

        if (propertyIdSet == null) {
          for (Map.Entry<String, Set<String>> entry : metrics.entrySet()) {

            String key = entry.getKey();

            Pattern pattern = Pattern.compile(key);
            Matcher matcher = pattern.matcher(metric_name);

            if (matcher.matches()) {
              propertyIdSet = entry.getValue();
              // get parameters
              for (int i = 0; i < matcher.groupCount(); ++i) {
                parameterList.add(matcher.group(i + 1));
              }
              break;
            }
          }
        }
      }
      if (propertyIdSet != null) {
        Map<String, PropertyInfo> metricsMap = getComponentMetrics().get(getComponentName(resource));
        if (metricsMap != null) {
          for (String propertyId : propertyIdSet) {
            if (propertyId != null) {
              if (metricsMap.containsKey(propertyId)) {
                if (containsArguments(propertyId)) {
                  int i = 1;
                  for (String param : parameterList) {
                    propertyId = substituteArgument(propertyId, "$" + i, param);
                    ++i;
                  }
                }
                Object value = getValue(gangliaMetric, temporalInfo != null);
                if (value != null) {
                  resource.setProperty(propertyId, value);
                }
              }
            }
          }
        }
      }
    }

    private Number convertToNumber(String s) {
      return s.contains(".") ? Double.parseDouble(s) : Long.parseLong(s);
    }
  }


  // ----- ResourceKey ---------------------------------------------------

  /**
   * Key used to associate information from a Ganglia metric to a resource.
   */
  private static class ResourceKey {
    private final String hostName;
    private final String gangliaClusterName;

    private ResourceKey(String hostName, String gangliaClusterName) {
      this.hostName           = hostName;
      this.gangliaClusterName = gangliaClusterName;
    }

    public String getHostName() {
      return hostName;
    }

    public String getClusterName() {
      return gangliaClusterName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ResourceKey that = (ResourceKey) o;

      return
          !(gangliaClusterName != null ? !gangliaClusterName.equals(that.gangliaClusterName) : that.gangliaClusterName != null) &&
          !(hostName != null ? !hostName.equals(that.hostName) : that.hostName != null);

    }

    @Override
    public int hashCode() {
      int result = hostName != null ? hostName.hashCode() : 0;
      result = 31 * result + (gangliaClusterName != null ? gangliaClusterName.hashCode() : 0);
      return result;
    }
  }
}
