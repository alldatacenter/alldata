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

package org.apache.ambari.server.controller.internal;

/**
 * Property identifier information.
 */
public class PropertyInfo {
  private final String propertyId;
  private final boolean temporal;
  private final boolean pointInTime;
  private String amsId;
  private boolean amsHostMetric;
  private String unit;

  public PropertyInfo(String propertyId, boolean temporal, boolean pointInTime) {
    this.propertyId = propertyId;
    this.temporal = temporal;
    this.pointInTime = pointInTime;
  }

  public String getPropertyId() {
    return propertyId;
  }

  public boolean isTemporal() {
    return temporal;
  }

  public boolean isPointInTime() {
    return pointInTime;
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

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }
}
