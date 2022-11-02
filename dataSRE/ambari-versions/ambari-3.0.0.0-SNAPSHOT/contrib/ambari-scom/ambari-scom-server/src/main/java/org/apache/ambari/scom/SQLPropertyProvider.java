/**
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

package org.apache.ambari.scom;

import org.apache.ambari.server.controller.internal.AbstractPropertyProvider;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.jdbc.ConnectionFactory;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQL based property/metrics provider required for ambari-scom.
 */
public class SQLPropertyProvider extends AbstractPropertyProvider {

  private final HostInfoProvider hostProvider;

  private final String clusterNamePropertyId;

  private final String hostNamePropertyId;

  private final String componentNamePropertyId;

  private final String serviceNamePropertyId;

  private final ConnectionFactory connectionFactory;


  // ----- Constants ---------------------------------------------------------

  private static final String GET_METRICS_STATEMENT =
    "SELECT  s.RecordTypeContext, s.RecordTypeName, s.TagPairs, s.NodeName, s.ServiceName, mn.Name AS MetricName, s.RecordTimeStamp, mp.MetricValue\n" +
      "FROM HadoopMetrics.dbo.MetricPair mp\n" +
      "     INNER JOIN (\n" +
      "         SELECT mr.RecordID AS RecordID, mr.RecordTimeStamp AS RecordTimeStamp, rt.Context AS RecordTypeContext, rt.Name AS RecordTypeName, ts.TagPairs AS TagPairs, nd.Name AS NodeName, sr.Name AS ServiceName\n" +
      "         FROM HadoopMetrics.dbo.MetricRecord mr\n" +
      "              INNER JOIN HadoopMetrics.dbo.RecordType rt ON (mr.RecordTypeId = rt.RecordTypeId)\n" +
      "              INNER JOIN HadoopMetrics.dbo.TagSet ts ON (mr.TagSetID = ts.TagSetID)\n" +
      "              INNER JOIN HadoopMetrics.dbo.Node nd ON (mr.NodeID = nd.NodeID)\n" +
      "              INNER JOIN HadoopMetrics.dbo.Service sr ON (mr.ServiceID = sr.ServiceID)\n" +
      "         WHERE rt.Context in (%s)\n" +
      "               AND rt.Name in (%s)\n" +
      "               AND (ts.TagPairs LIKE %s)\n" +
      "               AND (nd.Name in (%s))\n" +
      "               AND (sr.Name in (%s))\n" +
      "               AND mr.RecordTimestamp >= %d\n" +
      "               AND mr.RecordTimestamp <= %d\n" +
      "     ) s ON (mp.RecordID = s.RecordID)\n" +
      "     INNER JOIN HadoopMetrics.dbo.MetricName mn ON (mp.MetricID = mn.MetricID)\n" +
      "WHERE (mn.Name in (%s))";

  protected final static Logger LOG = LoggerFactory.getLogger(SQLPropertyProvider.class);


  // ----- Constructors ------------------------------------------------------

  public SQLPropertyProvider(
    Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
    HostInfoProvider hostProvider,
    String clusterNamePropertyId,
    String hostNamePropertyId,
    String componentNamePropertyId,
    String serviceNamePropertyId,
    ConnectionFactory connectionFactory) {
    super(componentPropertyInfoMap);
    this.hostProvider = hostProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;
    this.hostNamePropertyId = hostNamePropertyId;
    this.componentNamePropertyId = componentNamePropertyId;
    this.serviceNamePropertyId = serviceNamePropertyId;
    this.connectionFactory = connectionFactory;
  }


  // ----- PropertyProvider --------------------------------------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate)
    throws SystemException {
    Set<Resource> keepers = new HashSet<Resource>();
    try {
      Connection connection = connectionFactory.getConnection();
      try {
        Statement statement = connection.createStatement();
        try {
          for (Resource resource : resources) {
            if (populateResource(resource, request, predicate, statement)) {
              keepers.add(resource);
            }
          }
        } finally {
          statement.close();
        }
      } finally {
        connection.close();
      }
    } catch (SQLException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error during populateResources call.");
        LOG.debug("Error during populateResources call : caught exception", e);
      }
    }
    return keepers;
  }


  // ----- helper methods ----------------------------------------------------

  // Populate the given resource
  private boolean populateResource(Resource resource, Request request, Predicate predicate, Statement statement) throws SystemException {

    Set<String> ids = getRequestPropertyIds(request, predicate);
    if (ids.isEmpty()) {
      // no properties requested ... nothing to do.
      return true;
    }

    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);
    String serviceName = (String) resource.getPropertyValue(serviceNamePropertyId);

    if (getComponentMetrics().get(componentName) == null) {
      // no metrics defined for the given component ... nothing to do.
      return true;
    }

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);
    String hostName = getHost(resource, clusterName, componentName);

    if (hostName == null) {
      throw new SystemException(
        "Unable to get metrics.  No host name for " + componentName, null);
    }

    Set<MetricDefinition> metricsDefinitionSet = new HashSet<MetricDefinition>();
    for (String id : ids) {
      Map<String, PropertyInfo> propertyInfoMap = getPropertyInfoMap(componentName, id);

      for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {
        String propertyKey = entry.getKey();
        PropertyInfo propertyInfo = entry.getValue();
        if (containsArguments(propertyKey)) {
          propertyInfo = updatePropertyInfo(propertyKey, id, propertyInfo);
        }

        String propertyId = propertyInfo.getPropertyId();
        TemporalInfo temporalInfo = request.getTemporalInfo(id);

        if ((propertyInfo.isPointInTime() && temporalInfo == null) ||
          (propertyInfo.isTemporal() && temporalInfo != null)) {

          long startTime;
          long endTime;

          if (temporalInfo != null) {
            Long endTimeSeconds = temporalInfo.getEndTime();

            endTime = endTimeSeconds != -1 ? endTimeSeconds * 1000 : Long.MAX_VALUE;
            startTime = temporalInfo.getStartTime() * 1000;
          } else {
            startTime = 0L;
            endTime = Long.MAX_VALUE;
          }

          String category = "";
          String recordTypeContext = "";
          String recordTypeName = "";
          String metricName = "";
          String tagPairsPattern = "";
          int dotIndex = propertyId.lastIndexOf('.');
          if (dotIndex != -1) {
            category = propertyId.substring(0, dotIndex);
            metricName = propertyId.substring(dotIndex + 1);
          }
          String[] parts = category.split("\\.");
          if (parts.length >= 2) {
            recordTypeContext = parts[0];
            recordTypeName = parts[1];
            if (containsArguments(propertyKey) && parts.length > 2) {
              tagPairsPattern = StringUtils.join(Arrays.copyOfRange(parts, 2, parts.length), ".");
            }
            metricsDefinitionSet.add(
              new MetricDefinition(
                startTime,
                endTime,
                recordTypeContext,
                recordTypeName,
                tagPairsPattern,
                metricName,
                serviceName != null && serviceName.toLowerCase().equals("hbase") ? serviceName.toLowerCase() : componentName.toLowerCase(),
                hostName,
                propertyKey,
                id,
                temporalInfo)
            );
          } else {
            if (LOG.isWarnEnabled()) {
              LOG.warn("Can't get metrics for " + id + " : " + propertyId);
            }
          }
        }
      }
    }

    Map<MetricDefinition, List<DataPoint>> results = getMetric(metricsDefinitionSet, statement);

    for (MetricDefinition metricDefinition : metricsDefinitionSet) {
      List<DataPoint> dataPoints = results.containsKey(metricDefinition) ? results.get(metricDefinition) : new ArrayList<DataPoint>();
      TemporalInfo temporalInfo = metricDefinition.getTemporalInfo();
      String propertyKey = metricDefinition.getPropertyKey();
      String requestedPropertyKey = metricDefinition.getRequestedPropertyKey();
      if (dataPoints != null) {
        if (temporalInfo == null) {
          // return the value of the last data point
          int length = dataPoints.size();
          Serializable value = length > 0 ? dataPoints.get(length - 1).getValue() : 0;
          resource.setProperty(propertyKey, value);
        } else {
          Number[][] dp = new Number[dataPoints.size()][2];
          for (int i = 0; i < dp.length; i++) {
            dp[i][0] = dataPoints.get(i).getValue();
            dp[i][1] = dataPoints.get(i).getTimestamp() / 1000;
          }
          if (containsArguments(propertyKey)) {
            resource.setProperty(requestedPropertyKey, dp);
          } else {
            resource.setProperty(propertyKey, dp);
          }
        }
      }
    }

    return true;
  }

  // get a metric from a sql connection
  private Map<MetricDefinition, List<DataPoint>> getMetric(Set<MetricDefinition> metricDefinitionSet, Statement statement) throws SystemException {
    Map<MetricDefinition, List<DataPoint>> results = new HashMap<MetricDefinition, List<DataPoint>>();
    try {
      StringBuilder query = new StringBuilder();
      Set<String> recordTypeContexts = new HashSet<String>();
      Set<String> recordTypeNamess = new HashSet<String>();
      Set<String> tagPairsPatterns = new HashSet<String>();
      Set<String> nodeNames = new HashSet<String>();
      Set<String> serviceNames = new HashSet<String>();
      Set<String> metricNames = new HashSet<String>();
      long startTime = 0, endTime = 0;
      for (MetricDefinition metricDefinition : metricDefinitionSet) {
        if (metricDefinition.getRecordTypeContext() == null || metricDefinition.getRecordTypeName() == null || metricDefinition.getNodeName() == null) {
          continue;
        }

        recordTypeContexts.add(metricDefinition.getRecordTypeContext());
        recordTypeNamess.add(metricDefinition.getRecordTypeName());
        tagPairsPatterns.add(metricDefinition.getTagPairsPattern());
        nodeNames.add(metricDefinition.getNodeName());
        serviceNames.add(metricDefinition.getServiceName());
        metricNames.add(metricDefinition.getMetricName());
        startTime = metricDefinition.getStartTime();
        endTime = metricDefinition.getEndTime();
      }

      for (String tagPairsPattern : tagPairsPatterns) {
        if (query.length() != 0) {
          query.append("\nUNION\n");
        }
        query.append(String.format(GET_METRICS_STATEMENT,
          "'" + StringUtils.join(recordTypeContexts, "','") + "'",
          "'" + StringUtils.join(recordTypeNamess, "','") + "'",
          "'%" + tagPairsPattern + "%'",
          "'" + StringUtils.join(nodeNames, "','") + "'",
          "'" + StringUtils.join(serviceNames, "','") + "'",
          startTime,
          endTime,
          "'" + StringUtils.join(metricNames, "','") + "'"
        ));
      }

      ResultSet rs = null;
      if (query.length() != 0) {
        rs = statement.executeQuery(query.toString());
      }

      if (rs != null) {
        //(RecordTimeStamp bigint, MetricValue NVARCHAR(512))
        while (rs.next()) {
          MetricDefinition metricDefinition = new MetricDefinition(rs.getString("RecordTypeContext"), rs.getString("RecordTypeName"), rs.getString("TagPairs"), rs.getString("MetricName"), rs.getString("ServiceName"), rs.getString("NodeName"));
          ParsePosition parsePosition = new ParsePosition(0);
          NumberFormat numberFormat = NumberFormat.getInstance();
          Number parsedNumber = numberFormat.parse(rs.getNString("MetricValue"), parsePosition);
          if (results.containsKey(metricDefinition)) {
            results.get(metricDefinition).add(new DataPoint(rs.getLong("RecordTimeStamp"), parsedNumber));
          } else {
            List<DataPoint> dataPoints = new ArrayList<DataPoint>();
            dataPoints.add(new DataPoint(rs.getLong("RecordTimeStamp"), parsedNumber));
            results.put(metricDefinition, dataPoints);
          }
        }
      }
    } catch (SQLException e) {
      throw new SystemException("Error during getMetric call : caught exception - ", e);
    }
    return results;
  }

  // get the hostname for a given resource
  private String getHost(Resource resource, String clusterName, String componentName) throws SystemException {
    return hostNamePropertyId == null ?
      hostProvider.getHostName(clusterName, componentName) :
      hostProvider.getHostName((String) resource.getPropertyValue(hostNamePropertyId));
  }


  // ----- inner class : DataPoint -------------------------------------------

  /**
   * Structure to hold a single datapoint (value/timestamp pair) retrieved from the db.
   */
  private static class DataPoint {
    private final long timestamp;
    private final Number value;

    // ----- Constructor -------------------------------------------------

    /**
     * Construct a data point from the given value and timestamp.
     *
     * @param timestamp the timestamp
     * @param value     the value
     */
    private DataPoint(long timestamp, Number value) {
      this.timestamp = timestamp;
      this.value = value;
    }

    // ----- DataPoint ---------------------------------------------------

    /**
     * Get the timestamp value.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
      return timestamp;
    }

    /**
     * Get the value.
     *
     * @return the value
     */
    public Number getValue() {
      return value;
    }

    // ----- Object overrides --------------------------------------------

    @Override
    public String toString() {
      return "{" + value + " : " + timestamp + "}";
    }
  }

  private class MetricDefinition {
    long startTime;
    long endTime;

    String recordTypeContext;
    String recordTypeName;
    String tagPairsPattern;
    String metricName;
    String serviceName;
    String nodeName;

    String propertyKey;
    String requestedPropertyKey;
    TemporalInfo temporalInfo;

    private MetricDefinition(long startTime, long endTime, String recordTypeContext, String recordTypeName, String tagPairsPattern, String metricName, String serviceName, String nodeName, String propertyKey, String requestedPropertyKey, TemporalInfo temporalInfo) {
      this.startTime = startTime;
      this.endTime = endTime;
      this.recordTypeContext = recordTypeContext;
      this.recordTypeName = recordTypeName;
      this.tagPairsPattern = tagPairsPattern;
      this.metricName = metricName;
      this.serviceName = serviceName;
      this.nodeName = nodeName;
      this.propertyKey = propertyKey;
      this.requestedPropertyKey = requestedPropertyKey;
      this.temporalInfo = temporalInfo;
    }

    private MetricDefinition(String recordTypeContext, String recordTypeName, String tagPairsPattern, String metricName, String serviceName, String nodeName) {
      this.recordTypeContext = recordTypeContext;
      this.recordTypeName = recordTypeName;
      this.tagPairsPattern = tagPairsPattern;
      this.metricName = metricName;
      this.serviceName = serviceName;
      this.nodeName = nodeName;
    }

    public long getStartTime() {
      return startTime;
    }

    public void setStartTime(long startTime) {
      this.startTime = startTime;
    }

    public long getEndTime() {
      return endTime;
    }

    public void setEndTime(long endTime) {
      this.endTime = endTime;
    }

    public String getRecordTypeContext() {
      return recordTypeContext;
    }

    public void setRecordTypeContext(String recordTypeContext) {
      this.recordTypeContext = recordTypeContext;
    }

    public String getRecordTypeName() {
      return recordTypeName;
    }

    public void setRecordTypeName(String recordTypeName) {
      this.recordTypeName = recordTypeName;
    }

    public String getTagPairsPattern() {
      return tagPairsPattern;
    }

    public void getTagPairsPattern(String tagPairsPattern) {
      this.tagPairsPattern = tagPairsPattern;
    }

    public String getMetricName() {
      return metricName;
    }

    public void setMetricName(String metricName) {
      this.metricName = metricName;
    }

    public String getServiceName() {
      return serviceName;
    }

    public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
    }

    public String getNodeName() {
      return nodeName;
    }

    public void setNodeName(String nodeName) {
      this.nodeName = nodeName;
    }

    public String getPropertyKey() {
      return propertyKey;
    }

    public void setPropertyKey(String propertyKey) {
      this.propertyKey = propertyKey;
    }

    public String getRequestedPropertyKey() {
      return requestedPropertyKey;
    }

    public void setRequestedPropertyKey(String requestedPropertyKey) {
      this.requestedPropertyKey = requestedPropertyKey;
    }

    public TemporalInfo getTemporalInfo() {
      return temporalInfo;
    }

    public void setTemporalInfo(TemporalInfo temporalInfo) {
      this.temporalInfo = temporalInfo;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MetricDefinition that = (MetricDefinition) o;

      if (metricName != null ? !metricName.equals(that.metricName) : that.metricName != null) return false;
      if (nodeName != null ? !nodeName.equalsIgnoreCase(that.nodeName) : that.nodeName != null) return false;
      if (recordTypeContext != null ? !recordTypeContext.equals(that.recordTypeContext) : that.recordTypeContext != null)
        return false;
      if (recordTypeName != null ? !recordTypeName.equals(that.recordTypeName) : that.recordTypeName != null)
        return false;
      if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
      if (tagPairsPattern != null ? !(tagPairsPattern.contains(that.tagPairsPattern) ||
        that.tagPairsPattern.contains(tagPairsPattern)) : that.tagPairsPattern != null)
        return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = recordTypeContext != null ? recordTypeContext.hashCode() : 0;
      result = 31 * result + (recordTypeName != null ? recordTypeName.hashCode() : 0);
      result = 31 * result + (metricName != null ? metricName.hashCode() : 0);
      result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
      result = 31 * result + (nodeName != null ? nodeName.toLowerCase().hashCode() : 0);
      return result;
    }
  }
}
