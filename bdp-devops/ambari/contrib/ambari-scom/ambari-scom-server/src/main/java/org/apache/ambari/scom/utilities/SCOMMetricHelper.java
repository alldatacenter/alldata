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
package org.apache.ambari.scom.utilities;

import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.Resource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that provides Property helper methods.
 */
public class SCOMMetricHelper {
  private static final String SQLSERVER_PROPERTIES_FILE = "sqlserver_properties.json";
  private static final String JMX_PROPERTIES_FILE = "jmx_properties.json";

  private static final Map<Resource.InternalType, Map<String, Map<String, PropertyInfo>>> JMX_PROPERTY_IDS = readPropertyProviderIds(JMX_PROPERTIES_FILE);
  private static final Map<Resource.InternalType, Map<String, Map<String, PropertyInfo>>> SQLSERVER_PROPERTY_IDS = readPropertyProviderIds(SQLSERVER_PROPERTIES_FILE);

  public static Map<String, Map<String, PropertyInfo>> getSqlServerPropertyIds(Resource.Type resourceType) {
    return SQLSERVER_PROPERTY_IDS.get(resourceType.getInternalType());
  }

  public static Map<String, Map<String, PropertyInfo>> getJMXPropertyIds(Resource.Type resourceType) {
    return JMX_PROPERTY_IDS.get(resourceType.getInternalType());
  }

  protected static class Metric {
    private String metric;
    private boolean pointInTime;
    private boolean temporal;

    private Metric() {
    }

    protected Metric(String metric, boolean pointInTime, boolean temporal) {
      this.metric = metric;
      this.pointInTime = pointInTime;
      this.temporal = temporal;
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
  }

  private static Map<Resource.InternalType, Map<String, Map<String, PropertyInfo>>> readPropertyProviderIds(String filename) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      Map<Resource.InternalType, Map<String, Map<String, Metric>>> resourceMetricMap =
              mapper.readValue(ClassLoader.getSystemResourceAsStream(filename),
                      new TypeReference<Map<Resource.InternalType, Map<String, Map<String, Metric>>>>() {});

      Map<Resource.InternalType, Map<String, Map<String, PropertyInfo>>> resourceMetrics =
              new HashMap<Resource.InternalType, Map<String, Map<String, PropertyInfo>>>();

      for (Map.Entry<Resource.InternalType, Map<String, Map<String, Metric>>> resourceEntry : resourceMetricMap.entrySet()) {
        Map<String, Map<String, PropertyInfo>> componentMetrics = new HashMap<String, Map<String, PropertyInfo>>();

        for (Map.Entry<String, Map<String, Metric>> componentEntry : resourceEntry.getValue().entrySet()) {
          Map<String, PropertyInfo> metrics = new HashMap<String, PropertyInfo>();

          for (Map.Entry<String, Metric> metricEntry : componentEntry.getValue().entrySet()) {
            String property = metricEntry.getKey();
            Metric metric = metricEntry.getValue();

            metrics.put(property, new PropertyInfo(metric.getMetric(), metric.isTemporal(), metric.isPointInTime()));
          }
          componentMetrics.put(componentEntry.getKey(), metrics);
        }
        resourceMetrics.put(resourceEntry.getKey(), componentMetrics);
      }
      return resourceMetrics;
    }
    catch (IOException e) {
      throw new IllegalStateException("Can't read properties file " + filename, e);
    }
  }
}
