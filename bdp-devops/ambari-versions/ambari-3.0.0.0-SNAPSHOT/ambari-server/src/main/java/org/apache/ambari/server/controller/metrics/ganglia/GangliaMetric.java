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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Data structure for temporal data returned from Ganglia Web.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GangliaMetric {

  // Note that the member names correspond to the names in the JSON returned from Ganglia Web.

  /**
   * The name.
   */
  private String ds_name;

  /**
   * The ganglia cluster name.
   */
  private String cluster_name;

  /**
   * The graph type.
   */
  private String graph_type;

  /**
   * The host name.
   */
  private String host_name;

  /**
   * The metric name.
   */
  private String metric_name;

  /**
   * The temporal data points.
   */
  private Number[][] datapoints;
  
  
  private static final Set<String> PERCENTAGE_METRIC;

  //BUG-3386 Cluster CPU Chart is off the charts
  // Here can be added other percentage metrics
  static {
    Set<String> temp = new HashSet<>();
    temp.add("cpu_wio");
    temp.add("cpu_idle");
    temp.add("cpu_nice");
    temp.add("cpu_aidle");
    temp.add("cpu_system");
    temp.add("cpu_user");
    PERCENTAGE_METRIC = Collections.unmodifiableSet(temp);
  }


  // ----- GangliaMetric -----------------------------------------------------

  public String getDs_name() {
    return ds_name;
  }

  public void setDs_name(String ds_name) {
    this.ds_name = ds_name;
  }

  public String getCluster_name() {
    return cluster_name;
  }

  public void setCluster_name(String cluster_name) {
    this.cluster_name = cluster_name;
  }

  public String getGraph_type() {
    return graph_type;
  }

  public void setGraph_type(String graph_type) {
    this.graph_type = graph_type;
  }

  public String getHost_name() {
    return host_name;
  }

  public void setHost_name(String host_name) {
    this.host_name = host_name;
  }

  public String getMetric_name() {
    return metric_name;
  }

  public void setMetric_name(String metric_name) {
    this.metric_name = metric_name;
  }

  public Number[][] getDatapoints() {
    return datapoints;
  }


  public void setDatapoints(Number[][] datapoints) {
    this.datapoints = datapoints;
  } 
  
  public void setDatapointsFromList(List<GangliaMetric.TemporalMetric> listTemporalMetrics) { 
    //this.datapoints = datapoints;
    Number[][] datapointsArray = new Number[listTemporalMetrics.size()][2];
    int cnt = 0;
    if (PERCENTAGE_METRIC.contains(metric_name)) {
      int firstIndex = 0;
      int lastIndex = listTemporalMetrics.size() - 1;
      for (int i = firstIndex; i <= lastIndex; ++i) {
        GangliaMetric.TemporalMetric m = listTemporalMetrics.get(i);
        Number val = m.getValue();
        if (100.0 >= val.doubleValue()) {
          datapointsArray[cnt][0] = val;
          datapointsArray[cnt][1] = m.getTime();
          cnt++;
        }
      }
    } else {
      int firstIndex = 0;
      int lastIndex = listTemporalMetrics.size() - 1;
      for (int i = firstIndex; i <= lastIndex; ++i) {
        GangliaMetric.TemporalMetric m = listTemporalMetrics.get(i);
        datapointsArray[i][0] = m.getValue();
        datapointsArray[i][1] = m.getTime();
        cnt++;
      }
    }

    this.datapoints = new Number[cnt][2];
    for (int i = 0; i < this.datapoints.length; i++) {
      this.datapoints[i][0] = datapointsArray[i][0];
      this.datapoints[i][1] = datapointsArray[i][1];
    }

  }

  // ----- Object overrides --------------------------------------------------

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append("\n");
    stringBuilder.append("name=");
    stringBuilder.append(ds_name);
    stringBuilder.append("\n");
    stringBuilder.append("cluster name=");
    stringBuilder.append(cluster_name);
    stringBuilder.append("\n");
    stringBuilder.append("graph type=");
    stringBuilder.append(graph_type);
    stringBuilder.append("\n");
    stringBuilder.append("host name=");
    stringBuilder.append(host_name);
    stringBuilder.append("\n");
    stringBuilder.append("api name=");
    stringBuilder.append(metric_name);
    stringBuilder.append("\n");

    stringBuilder.append("datapoints (value/timestamp):");
    stringBuilder.append("\n");


    boolean first = true;
    stringBuilder.append("[");
    for (Number[] m : datapoints) {
      if (!first) {
        stringBuilder.append(",");
      }
      stringBuilder.append("[");
      stringBuilder.append(m[0]);
      stringBuilder.append(",");
      stringBuilder.append(m[1].longValue());
      stringBuilder.append("]");
      first = false;
    }
    stringBuilder.append("]");

    return stringBuilder.toString();
  }

  public static class TemporalMetric {
    private Number m_value;
    private Number m_time;
    private boolean valid;

    public boolean isValid() {
      return valid;
    }

    public TemporalMetric(String value, Number time) {
      valid = true;
      try{
        m_value = convertToNumber(value);
      } catch (NumberFormatException e) {
        valid = false;
      }
      m_time = time;
    }

    public Number getValue() {
      return m_value;
    }

    public Number getTime() {
      return m_time;
    }
    
    private Number convertToNumber(String s) throws NumberFormatException {
      Number res;
      if(s.contains(".")){
        Double d = Double.parseDouble(s);
        if(d.isNaN() || d.isInfinite()){
          throw new NumberFormatException(s);
        } else {
          res = d;
        } 
      } else {
        res = Long.parseLong(s);
      }
      return res;
    }
    
  }
}
