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
package org.apache.ambari.server.controller.utilities;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.collect.ImmutableList;

/**
 * Utility class that provides Property helper methods.
 */
public class PropertyHelper {

  private static final String GANGLIA_PROPERTIES_FILE = "ganglia_properties.json";
  private static final String SQLSERVER_PROPERTIES_FILE = "sqlserver_properties.json";
  private static final String JMX_PROPERTIES_FILE = "jmx_properties.json";
  public static final char EXTERNAL_PATH_SEP = '/';

  /**
   * Aggregate functions implicitly supported by the Metrics Service
   */
  public static final List<String> AGGREGATE_FUNCTION_IDENTIFIERS =
    ImmutableList.of("._sum", "._max", "._min", "._avg", "._rate");

  private static final List<Resource.InternalType> REPORT_METRIC_RESOURCES =
    ImmutableList.of(Resource.InternalType.Cluster, Resource.InternalType.Host);

  private static final Map<Resource.InternalType, Set<String>> PROPERTY_IDS = new HashMap<>();
  private static final Map<Resource.InternalType, Map<String, Map<String, PropertyInfo>>> JMX_PROPERTY_IDS = readPropertyProviderIds(JMX_PROPERTIES_FILE);
  private static final Map<Resource.InternalType, Map<String, Map<String, PropertyInfo>>> GANGLIA_PROPERTY_IDS = readPropertyProviderIds(GANGLIA_PROPERTIES_FILE);
  private static final Map<Resource.InternalType, Map<String, Map<String, PropertyInfo>>> SQLSERVER_PROPERTY_IDS = readPropertyProviderIds(SQLSERVER_PROPERTIES_FILE);
  private static final Map<Resource.InternalType, Map<Resource.Type, String>> KEY_PROPERTY_IDS = new HashMap<>();

  // Suffixes to add for Namenode rpc metrics prefixes
  private static final Map<String, List<String>> RPC_METRIC_SUFFIXES = new HashMap<>();

  /**
   * Regular expression to check for replacement arguments (e.g. $1) in a property id.
   */
  private static final Pattern CHECK_FOR_METRIC_ARGUMENTS_REGEX = Pattern.compile(".*\\$\\d+.*");

  /**
   * This regular expression will match on every {@code /} in a given string
   * that is not inside of quotes. The following string would be tokenized like
   * so:
   * <p/>
   * {@code foo/$1.substring(5)/bar/$2.replaceAll(\"/a/b//c///\")/baz}
   * <ul>
   * <li>foo</li>
   * <li>$1.substring(5)</li>
   * <li>bar</li>
   * <li>$2.replaceAll(\"/a/b//c///\")</li>
   * <li>baz</li>
   * </ul>
   *
   * This is necessary to be able to properly tokenize a property with {@code /}
   * while also ensuring we don't match on {@code /} that appears inside of
   * quotes.
   */
  private static final Pattern METRIC_CATEGORY_TOKENIZE_REGEX = Pattern.compile("/+(?=([^\"\\\\\\\\]*(\\\\\\\\.|\"([^\"\\\\\\\\]*\\\\\\\\.)*[^\"\\\\\\\\]*\"))*[^\"]*$)");

  static {
    RPC_METRIC_SUFFIXES.put("metrics/rpc/", ImmutableList.of("client", "datanode", "healthcheck"));
    RPC_METRIC_SUFFIXES.put("metrics/rpcdetailed/", ImmutableList.of("client", "datanode", "healthcheck"));
  }

  public static String getPropertyId(String category, String name) {
    String propertyId =  (category == null || category.isEmpty())? name :
        (name == null || name.isEmpty()) ? category : category + EXTERNAL_PATH_SEP + name;

    if (propertyId.endsWith("/")) {
      propertyId = propertyId.substring(0, propertyId.length() - 1);
    }
    return propertyId;
  }


  public static Set<String> getPropertyIds(Resource.Type resourceType) {
    Set<String> propertyIds = PROPERTY_IDS.get(resourceType.getInternalType());
    return propertyIds == null ? Collections.emptySet() : propertyIds;
  }

  public static void setPropertyIds(Resource.Type resourceType, Set<String> propertyIds) {
    PROPERTY_IDS.put(resourceType.getInternalType(), propertyIds);
  }

  /**
   * Extract the set of property ids from a component PropertyInfo map.
   *
   * @param componentPropertyInfoMap  the map
   *
   * @return the set of property ids
   */
  public static Set<String> getPropertyIds(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap ) {
    Set<String> propertyIds = new HashSet<>();

    for (Map.Entry<String, Map<String, PropertyInfo>> entry : componentPropertyInfoMap.entrySet()) {
      propertyIds.addAll(entry.getValue().keySet());
    }
    return propertyIds;
  }

  public static Map<String, Map<String, PropertyInfo>> getMetricPropertyIds(Resource.Type resourceType) {
    return GANGLIA_PROPERTY_IDS.get(resourceType.getInternalType());
  }

  public static Map<String, Map<String, PropertyInfo>> getSQLServerPropertyIds(Resource.Type resourceType) {
    return SQLSERVER_PROPERTY_IDS.get(resourceType.getInternalType());
  }

  public static Map<String, Map<String, PropertyInfo>> getJMXPropertyIds(Resource.Type resourceType) {
    return JMX_PROPERTY_IDS.get(resourceType.getInternalType());
  }

  public static Map<Resource.Type, String> getKeyPropertyIds(Resource.Type resourceType) {
    return KEY_PROPERTY_IDS.get(resourceType.getInternalType());
  }

  public static void setKeyPropertyIds(Resource.Type resourceType, Map<Resource.Type, String> keyPropertyKeys) {
    KEY_PROPERTY_IDS.put(resourceType.getInternalType(), keyPropertyKeys);
  }

  /**
   * Helper to get a property name from a string.
   *
   * @param absProperty  the fully qualified property
   *
   * @return the property name
   */
  public static String getPropertyName(String absProperty) {
    int lastPathSep = absProperty.lastIndexOf(EXTERNAL_PATH_SEP);

    return lastPathSep == -1 ? absProperty : absProperty.substring(lastPathSep + 1);
  }

  /**
   * Gets the parent category from a given property string. This method is used
   * in many places by many different consumers. In general, it will check to
   * see if the property contains arguments. If not, then a simple
   * {@link String#substring(int, int)} is used along with
   * {@link #EXTERNAL_PATH_SEP}.
   * <p/>
   * In the event that a property contains a $d parameter, it will attempt to
   * strip out any embedded methods in the various tokens. For example, if a
   * property of {@code foo/$1.substring(5)/bar/$2.substring(1)/baz} is given,
   * the expected recursive categories would be:
   * <ul>
   * <li>foo</li>
   * <li>foo/$1</li>
   * <li>foo/$1/bar</li>
   * <li>foo/$1/bar</li>
   * <li>foo/$1/bar/$2</li>
   * </ul>
   *
   * {@code foo/$1.substring(5)/bar} is incorrect as a category.
   *
   * @param property
   *          the fully qualified property
   *
   * @return the property category; null if there is no category
   */
  public static String getPropertyCategory(String property) {
    int lastPathSep = -1;

    if( !containsArguments(property) ){
      lastPathSep = property.lastIndexOf(EXTERNAL_PATH_SEP);
      return lastPathSep == -1 ? null : property.substring(0, lastPathSep);
    }

    // attempt to split the property into its parts
    String[] tokens = METRIC_CATEGORY_TOKENIZE_REGEX.split(property);
    if (null == tokens || tokens.length == 0) {
      return null;
    }

    StringBuilder categoryBuilder = new StringBuilder();
    for (int i = 0; i < tokens.length - 1; i++) {
      String token = tokens[i];

      // if the token contains arguments, turn $1.method() into $1,
      if (containsArguments(token)) {
        int methodIndex = token.indexOf('.');

        if (methodIndex != -1) {
          // normally knowing where $1.method() is would be enough, but some
          // properties may omit the / (like FLUME) so the property would look
          // like /$1.method()FailureCount instead of /$1.method()/FailureCount
          int parensIndex = token.lastIndexOf(')');
          if (parensIndex < token.length() - 1) {
            // cut out $1
            String temp = token.substring(0, methodIndex);

            // append FailureCount
            temp += token.substring(parensIndex + 1);

            // $1.method()FailureCount -> $1FailureCount
            token = temp;
          } else {
            token = token.substring(0, methodIndex);
          }
        }
      }

      // only append a / if this is not the last part of the parent category
      categoryBuilder.append(token);
      if (i < tokens.length - 2) {
        categoryBuilder.append('/');
      }
    }

    String category = categoryBuilder.toString();
    return category;
  }

  /**
   * Get the set of categories for the given property ids.
   *
   * @param propertyIds  the property ids
   *
   * @return the set of categories
   */
  public static Set<String> getCategories(Set<String> propertyIds) {
    Set<String> categories = new HashSet<>();
    for (String property : propertyIds) {
      String category = PropertyHelper.getPropertyCategory(property);
      while (category != null) {
        categories.add(category);
        category = PropertyHelper.getPropertyCategory(category);
      }
    }
    return categories;
  }

  /**
   * Check if the given property id or one of its parent category ids is contained
   * in the given set of property ids.
   *
   * @param propertyIds  the set of property ids
   * @param propertyId   the property id
   *
   * @return true if the given property id of one of its parent category ids is
   *         contained in the given set of property ids
   */
  public static boolean containsProperty(Set<String> propertyIds, String propertyId) {

    if (propertyIds.contains(propertyId)){
      return true;
    }

    String category = PropertyHelper.getPropertyCategory(propertyId);
    while (category != null) {
      if ( propertyIds.contains(category)) {
        return true;
      }
      category = PropertyHelper.getPropertyCategory(category);
    }
    return false;
  }

  /**
   * Check to see if the given property id contains replacement arguments (e.g. $1)
   *
   * @param propertyId  the property id to check
   *
   * @return true if the given property id contains any replacement arguments
   */
  public static boolean containsArguments(String propertyId) {
    if (!propertyId.contains("$")) {
      return false;
    }
    Matcher matcher = CHECK_FOR_METRIC_ARGUMENTS_REGEX.matcher(propertyId);
    return matcher.find();
  }


  /**
   * Get all of the property ids associated with the given request.
   *
   * @param request  the request
   *
   * @return the associated properties
   */
  public static Set<String> getAssociatedPropertyIds(Request request) {
    Set<String> ids = request.getPropertyIds();

    if (ids != null) {
      ids = new HashSet<>(ids);
    } else {
      ids = new HashSet<>();
    }

    Set<Map<String, Object>> properties = request.getProperties();
    if (properties != null) {
      for (Map<String, Object> propertyMap : properties) {
        ids.addAll(propertyMap.keySet());
      }
    }
    return ids;
  }

  /**
   * Get a map of all the property values keyed by property id for the given resource.
   *
   * @param resource  the resource
   *
   * @return the map of properties for the given resource
   */
  public static Map<String, Object> getProperties(Resource resource) {
    Map<String, Object> properties = new HashMap<>();

    Map<String, Map<String, Object>> categories = resource.getPropertiesMap();

    for (Map.Entry<String, Map<String, Object>> categoryEntry : categories.entrySet()) {
      for (Map.Entry<String, Object>  propertyEntry : categoryEntry.getValue().entrySet()) {
        properties.put(getPropertyId(categoryEntry.getKey(), propertyEntry.getKey()), propertyEntry.getValue());
      }
    }
    return properties;
  }

  /**
   * Factory method to create a create request from the given set of property maps.
   * Each map contains the properties to be used to create a resource.  Multiple maps in the
   * set should result in multiple creates.
   *
   * @param properties             resource properties associated with the request; may be null
   * @param requestInfoProperties  request specific properties; may be null
   */
  public static Request getCreateRequest(Set<Map<String, Object>> properties,
                                         Map<String, String> requestInfoProperties) {
    return new RequestImpl(null, properties, requestInfoProperties, null);
  }

  /**
   * Factory method to create a read request from the given set of property ids.  The set of
   * property ids represents the properties of interest for the query.
   *
   * @param propertyIds  the property ids associated with the request; may be null
   */
  public static Request getReadRequest(Set<String> propertyIds) {
    return PropertyHelper.getReadRequest(propertyIds, null);
  }

  /**
   * Factory method to create a read request from the given set of property ids.  The set of
   * property ids represents the properties of interest for the query.
   *
   * @param propertyIds      the property ids associated with the request; may be null
   * @param mapTemporalInfo  the temporal info
   */
  public static Request getReadRequest(Set<String> propertyIds, Map<String,
      TemporalInfo> mapTemporalInfo) {
    return PropertyHelper.getReadRequest(propertyIds, null, mapTemporalInfo,
        null, null);
  }

  /**
   * Factory method to create a read request from the given set of property ids.
   * The set of property ids represents the properties of interest for the
   * query.
   *
   * @param propertyIds
   *          the property ids associated with the request; may be null
   * @param requestInfoProperties
   *          request info properties
   * @param mapTemporalInfo
   *          the temporal info
   * @param pageRequest
   *          an optional page request, or {@code null} for none.
   * @param sortRequest
   *          an optional sort request, or {@code null} for none.
   */
  public static Request getReadRequest(Set<String> propertyIds,
      Map<String, String> requestInfoProperties,
      Map<String, TemporalInfo> mapTemporalInfo, PageRequest pageRequest,
      SortRequest sortRequest) {

    return  new RequestImpl(propertyIds, null, requestInfoProperties,
        mapTemporalInfo, sortRequest, pageRequest);
  }

  /**
   * Factory method to create a read request from the given set of property ids.  The set of
   * property ids represents the properties of interest for the query.
   *
   * @param propertyIds  the property ids associated with the request; may be null
   */
  public static Request getReadRequest(String ... propertyIds) {
    return PropertyHelper.getReadRequest(new HashSet<>(
      Arrays.asList(propertyIds)));
  }

  /**
   * Factory method to create an update request from the given map of properties.
   * The properties values in the given map are used to update the resource.
   *
   * @param properties             resource properties associated with the request; may be null
   * @param requestInfoProperties  request specific properties; may be null
   */
  public static Request getUpdateRequest(Map<String, Object> properties,
                                         Map<String, String> requestInfoProperties) {
    return new RequestImpl(null, Collections.singleton(properties), requestInfoProperties, null);
  }

  private static Map<Resource.InternalType, Map<String, Map<String, PropertyInfo>>> readPropertyProviderIds(String filename) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      Map<Resource.InternalType, Map<String, Map<String, Metric>>> resourceMetricMap =
          mapper.readValue(ClassLoader.getSystemResourceAsStream(filename),
              new TypeReference<Map<Resource.InternalType, Map<String, Map<String, Metric>>>>() {});

      Map<Resource.InternalType, Map<String, Map<String, PropertyInfo>>> resourceMetrics =
        new HashMap<>();

      for (Map.Entry<Resource.InternalType, Map<String, Map<String, Metric>>> resourceEntry : resourceMetricMap.entrySet()) {
        Map<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<>();

        for (Map.Entry<String, Map<String, Metric>> componentEntry : resourceEntry.getValue().entrySet()) {
          Map<String, PropertyInfo> metrics = new HashMap<>();

          for (Map.Entry<String, Metric> metricEntry : componentEntry.getValue().entrySet()) {
            String property = metricEntry.getKey();
            Metric metric   = metricEntry.getValue();
            PropertyInfo propertyInfo = new PropertyInfo(metric.getMetric(),
              metric.isTemporal(), metric.isPointInTime());
            propertyInfo.setAmsId(metric.getAmsId());
            propertyInfo.setAmsHostMetric(metric.isAmsHostMetric());
            propertyInfo.setUnit(metric.getUnit());
            metrics.put(property, propertyInfo);
          }
          componentMetrics.put(componentEntry.getKey(), metrics);
        }
        if (REPORT_METRIC_RESOURCES.contains(resourceEntry.getKey())) {
          updateMetricsWithAggregateFunctionSupport(componentMetrics);
        }
        resourceMetrics.put(resourceEntry.getKey(), componentMetrics);
      }
      return resourceMetrics;
    } catch (IOException e) {
      throw new IllegalStateException("Can't read properties file " + filename, e);
    }
  }

  private static Map<Resource.InternalType, Set<String>> readPropertyIds(String filename) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      return mapper.readValue(ClassLoader.getSystemResourceAsStream(filename), new TypeReference<Map<Resource.InternalType, Set<String>>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("Can't read properties file " + filename, e);
    }
  }

  private static Map<Resource.InternalType, Map<Resource.Type, String>> readKeyPropertyIds(String filename) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      Map<Resource.InternalType, Map<Resource.InternalType, String>> map =
          mapper.readValue(ClassLoader.getSystemResourceAsStream(filename),
              new TypeReference<Map<Resource.InternalType, Map<Resource.InternalType, String>>>() {});

      Map<Resource.InternalType, Map<Resource.Type, String>> returnMap =
        new HashMap<>();

      // convert inner maps from InternalType to Type
      for (Map.Entry<Resource.InternalType, Map<Resource.InternalType, String>> entry : map.entrySet()) {
        Map<Resource.Type, String> innerMap = new HashMap<>();

        for (Map.Entry<Resource.InternalType, String> entry1 : entry.getValue().entrySet()) {
          innerMap.put(Resource.Type.valueOf(entry1.getKey().name()), entry1.getValue());
        }
        returnMap.put(entry.getKey(), innerMap);
      }
      return returnMap;
    } catch (IOException e) {
      throw new IllegalStateException("Can't read properties file " + filename, e);
    }
  }

  protected static class Metric {
    private String metric;
    private boolean pointInTime;
    private boolean temporal;
    private String amsId;
    private boolean amsHostMetric;
    private String unit = "unitless";

    private Metric() {
    }

    protected Metric(String metric, boolean pointInTime, boolean temporal, String unit) {
      this.metric = metric;
      this.pointInTime = pointInTime;
      this.temporal = temporal;
      this.unit = unit;
    }

    public String getMetric() {
      return metric;
    }

    public void setMetric(String metric) {
      this.metric = metric;
    }

    public boolean isPointInTime() {
      return pointInTime;
    }

    public void setPointInTime(boolean pointInTime) {
      this.pointInTime = pointInTime;
    }

    public boolean isTemporal() {
      return temporal;
    }

    public void setTemporal(boolean temporal) {
      this.temporal = temporal;
    }

    public String getAmsId() {
      return amsId;
    }

    public void setAmsId(String amsId) {
      this.amsId = amsId;
    }

    public boolean isAmsHostMetric() {
      return amsHostMetric;
    }

    public void setAmsHostMetric(boolean amsHostMetric) {
      this.amsHostMetric = amsHostMetric;
    }

    public void setUnit(String unit) {
      this.unit = unit;
    }

    public String getUnit() {
      return unit;
    }
  }

  /**
   * This method adds supported propertyInfo for component metrics with
   * aggregate function ids. API calls with multiple aggregate functions
   * applied to a single metric need this support.
   *
   * Currently this support is added only for Component & Report metrics.
   * This can be easily extended by making the call from the appropriate
   * sub class of: @AMSPropertyProvider.
   *
   */
  public static void updateMetricsWithAggregateFunctionSupport(Map<String, Map<String, PropertyInfo>> metrics) {

    if (metrics == null || metrics.isEmpty()) {
      return;
    }

    // For every component
    for (Map.Entry<String, Map<String, PropertyInfo>> metricInfoEntry : metrics.entrySet()) {
      Map<String, PropertyInfo> metricInfo = metricInfoEntry.getValue();

      Map<String, PropertyInfo> aggregateMetrics = new HashMap<>();
      // For every metric
      for (Map.Entry<String, PropertyInfo> metricEntry : metricInfo.entrySet()) {
        // For each aggregate function id
        for (String identifierToAdd : AGGREGATE_FUNCTION_IDENTIFIERS) {
          String metricInfoKey = metricEntry.getKey() + identifierToAdd;
          // This disallows metric key suffix of the form "._sum._sum" for
          // the sake of avoiding duplicates
          if (metricInfo.containsKey(metricInfoKey)) {
            continue;
          }

          PropertyInfo propertyInfo = metricEntry.getValue();
          PropertyInfo metricInfoValue = new PropertyInfo(
            propertyInfo.getPropertyId() + identifierToAdd,
            propertyInfo.isTemporal(),
            propertyInfo.isPointInTime());
          metricInfoValue.setAmsHostMetric(propertyInfo.isAmsHostMetric());
          metricInfoValue.setAmsId(!StringUtils.isEmpty(propertyInfo.getAmsId()) ?
            propertyInfo.getAmsId() + identifierToAdd : null);
          metricInfoValue.setUnit(propertyInfo.getUnit());

          aggregateMetrics.put(metricInfoKey, metricInfoValue);
        }
      }
      metricInfo.putAll(aggregateMetrics);
    }
  }

  /**
   * Check if property ends with a trailing suffix of function id.
   */
  public static boolean hasAggregateFunctionSuffix(String propertyId) {
    for (String aggregateFunctionId : AGGREGATE_FUNCTION_IDENTIFIERS) {
      if (propertyId.endsWith(aggregateFunctionId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Special handle rpc port tags added to metric names for HDFS Namenode
   *
   * Returns the replacement definitions
   */
  public static Map<String, org.apache.ambari.server.state.stack.Metric> processRpcMetricDefinition(String metricType,
      String componentName, String propertyId, org.apache.ambari.server.state.stack.Metric metric) {
    Map<String, org.apache.ambari.server.state.stack.Metric> replacementMap = null;
    if (componentName.equalsIgnoreCase("NAMENODE")) {
      for (Map.Entry<String, List<String>> entry : RPC_METRIC_SUFFIXES.entrySet()) {
        String prefix = entry.getKey();
        if (propertyId.startsWith(prefix)) {
          replacementMap = new HashMap<>();
          for (String suffix : entry.getValue()) {
            String newMetricName;
            if ("jmx".equals(metricType)) {
              newMetricName = insertTagIntoCategoty(suffix, metric.getName());
            } else {
              newMetricName = insertTagInToMetricName(suffix, metric.getName(), prefix);
            }
            org.apache.ambari.server.state.stack.Metric newMetric = new org.apache.ambari.server.state.stack.Metric(
              newMetricName,
              metric.isPointInTime(),
              metric.isTemporal(),
              metric.isAmsHostMetric(),
              metric.getUnit()
            );

            replacementMap.put(insertTagInToMetricName(suffix, propertyId, prefix), newMetric);
          }
        }
      }
    }
    return replacementMap;
  }

  /**
   * Returns tag inserted metric name after the prefix.
   * @param tag E.g.: client
   * @param metricName : rpc.rpc.CallQueueLength Or metrics/rpc/CallQueueLen
   * @param prefix : rpc.rpc
   * @return rpc.rpc.client.CallQueueLength Or metrics/rpc/client/CallQueueLen
   */
  static String insertTagInToMetricName(String tag, String metricName, String prefix) {
    String sepExpr = "\\.";
    String seperator = ".";
    if (metricName.indexOf(EXTERNAL_PATH_SEP) != -1) {
      sepExpr = Character.toString(EXTERNAL_PATH_SEP);
      seperator = sepExpr;
    }
    String prefixSep = prefix.contains(".") ? "\\." : "" + EXTERNAL_PATH_SEP;

    // Remove separator if any
    if (prefix.substring(prefix.length() - 1).equals(prefixSep)) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    int pos = prefix.split(prefixSep).length - 1;
    String[] parts = metricName.split(sepExpr);
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < parts.length; i++) {
      sb.append(parts[i]);
      if (i < parts.length - 1) {
        sb.append(seperator);
      }
      if (i == pos) { // append the tag
        sb.append(tag);
        sb.append(seperator);
      }
    }
    return sb.toString();
  }

  static String insertTagIntoCategoty(String tag, String metricName) {
    int pos = metricName.lastIndexOf('.');
    return metricName.substring(0, pos) + ",tag=" + tag + metricName.substring(pos);
  }
}
