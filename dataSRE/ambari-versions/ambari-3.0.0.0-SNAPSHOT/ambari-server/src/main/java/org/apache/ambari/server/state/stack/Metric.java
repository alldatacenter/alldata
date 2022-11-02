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
package org.apache.ambari.server.state.stack;

public class Metric {
  private String metric = null;
  private boolean pointInTime = false;
  private boolean temporal = false;
  private boolean amsHostMetric = false;
  private String unit = "unitless";

  public Metric() {
  }

  public Metric(String metric, boolean pointInTime, boolean temporal,
                boolean amsHostMetric, String unit) {
    this.metric = metric;
    this.pointInTime = pointInTime;
    this.temporal = temporal;
    this.amsHostMetric = amsHostMetric;
    this.unit = unit;
  }

  public String getName() {
    return metric;
  }

  public boolean isPointInTime() {
    return pointInTime;
  }

  public boolean isTemporal() {
    return temporal;
  }

  /**
   * Indicates whether this hostcomponent metric is a host metric for AMS.
   */
  public boolean isAmsHostMetric() {
    return amsHostMetric;
  }

  public String getUnit() {
    return unit;
  }

  @Override
  public String toString() {
    return "Metric{" +
      "metric='" + metric + '\'' +
      ", pointInTime=" + pointInTime +
      ", temporal=" + temporal +
      ", amsHostMetric=" + amsHostMetric +
      ", unit='" + unit + '\'' +
      '}';
  }
}
