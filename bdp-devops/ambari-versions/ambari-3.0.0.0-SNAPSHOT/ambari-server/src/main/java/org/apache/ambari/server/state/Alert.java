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
package org.apache.ambari.server.state;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
/**
 * An alert represents a problem or notice for a cluster.
 */
public class Alert {
  private String name;
  private String instance;
  private String service;
  private String component;
  private String hostName;
  private AlertState state = AlertState.UNKNOWN;
  private String label;
  private String text;
  private long timestamp;
  private Long clusterId;
  private String uuid;

  // Maximum string size for MySql TEXT (utf8) column data type
  protected final static int MAX_ALERT_TEXT_SIZE = 32617;


  /**
   * Constructor.
   * @param alertName the name of the alert
   * @param alertInstance instance specific information in the event that two alert
   *    types can be run, ie Flume.
   * @param serviceName the service
   * @param componentName the component
   * @param hostName the host
   * @param alertState the state of the alertable event
   */
  public Alert(String alertName, String alertInstance, String serviceName,
      String componentName,  String hostName, AlertState alertState) {
    name = alertName;
    instance = alertInstance;
    service = serviceName;
    component = componentName;
    this.hostName = hostName;
    state = alertState;
    timestamp = System.currentTimeMillis();
  }

  public Alert() {
  }

  /**
   * @return the name
   */

  @JsonProperty("name")
  @com.fasterxml.jackson.annotation.JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * @return the service
   */
  @JsonProperty("service")
  @com.fasterxml.jackson.annotation.JsonProperty("service")
  public String getService() {
    return service;
  }

  /**
   * @return the component
   */
  @JsonProperty("component")
  @com.fasterxml.jackson.annotation.JsonProperty("component")
  public String getComponent() {
    return component;
  }

  /**
   * @return the host
   */
  @JsonProperty("host")
  @com.fasterxml.jackson.annotation.JsonProperty("host")
  public String getHostName() {
    return hostName;
  }

  /**
   * @return the state
   */
  @JsonProperty("state")
  @com.fasterxml.jackson.annotation.JsonProperty("state")
  public AlertState getState() {
    return state;
  }

  /**
   * @return a short descriptive label for the alert
   */
  @JsonProperty("label")
  @com.fasterxml.jackson.annotation.JsonProperty("label")
  public String getLabel() {
    return label;
  }

  /**
   * @param alertLabel a short descriptive label for the alert
   */
  @JsonProperty("label")
  @com.fasterxml.jackson.annotation.JsonProperty("label")
  public void setLabel(String alertLabel) {
    label = alertLabel;
  }

  /**
   * @return detail text about the alert
   */
  @JsonProperty("text")
  @com.fasterxml.jackson.annotation.JsonProperty("text")
  public String getText() {
    return text;
  }

  /**
   * @param alertText detail text about the alert
   */
  @JsonProperty("text")
  @com.fasterxml.jackson.annotation.JsonProperty("text")
  public void setText(String alertText) {
    // middle-ellipsize the text to reduce the size to 32617 characters
    text = StringUtils.abbreviateMiddle(alertText, "…", MAX_ALERT_TEXT_SIZE);
  }

  @JsonProperty("instance")
  @com.fasterxml.jackson.annotation.JsonProperty("instance")
  public String getInstance() {
    return instance;
  }

  @JsonProperty("instance")
  @com.fasterxml.jackson.annotation.JsonProperty("instance")
  public void setInstance(String instance) {
    this.instance = instance;
  }

  @JsonProperty("name")
  @com.fasterxml.jackson.annotation.JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("service")
  @com.fasterxml.jackson.annotation.JsonProperty("service")
  public void setService(String service) {
    this.service = service;
  }

  @JsonProperty("component")
  @com.fasterxml.jackson.annotation.JsonProperty("component")
  public void setComponent(String component) {
    this.component = component;
  }

  @JsonProperty("host")
  @com.fasterxml.jackson.annotation.JsonProperty("host")
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @JsonProperty("state")
  @com.fasterxml.jackson.annotation.JsonProperty("state")
  public void setState(AlertState state) {
    this.state = state;
  }

  @JsonProperty("timestamp")
  @com.fasterxml.jackson.annotation.JsonProperty("timestamp")
  public void setTimestamp(long ts) {
    timestamp = ts;
  }

  @JsonProperty("timestamp")
  @com.fasterxml.jackson.annotation.JsonProperty("timestamp")
  public long getTimestamp() {
    return timestamp;
  }

  @com.fasterxml.jackson.annotation.JsonProperty("clusterId")
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  @com.fasterxml.jackson.annotation.JsonProperty("uuid")
  public String getUUID() {
    return uuid;
  }

  public void setUUID(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, name, service, component, hostName, instance, clusterId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Alert other = (Alert) obj;

    return Objects.equals(state, other.state) &&
      Objects.equals(name, other.name) &&
      Objects.equals(service, other.service) &&
      Objects.equals(component, other.component) &&
      Objects.equals(hostName, other.hostName) &&
      Objects.equals(instance, other.instance) &&
      Objects.equals(clusterId, other.clusterId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("clusterId=").append(clusterId).append(", ");
    sb.append("state=").append(state).append(", ");
    sb.append("name=").append(name).append(", ");
    sb.append("service=").append(service).append(", ");
    sb.append("component=").append(component).append(", ");
    sb.append("host=").append(hostName).append(", ");
    sb.append("instance=").append(instance).append(", ");
    sb.append("text='").append(text).append("'");
    sb.append('}');
    return sb.toString();
  }
}
