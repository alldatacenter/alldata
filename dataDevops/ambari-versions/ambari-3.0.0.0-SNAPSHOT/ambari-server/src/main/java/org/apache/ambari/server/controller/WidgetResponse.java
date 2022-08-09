/*
® * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.ambari.server.controller;

import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.codehaus.jackson.annotate.JsonProperty;


/**
 * The {@link org.apache.ambari.server.controller.WidgetResponse} encapsulates the definition information
 * that should be serialized and returned in REST requests for alerts, groups,
 * and targets.
 */
public class WidgetResponse {

  private Long id;
  private String widgetName;
  private String widgetType;
  private String metrics;
  private Long timeCreated;
  private String author;
  private String description;
  private String displayName;
  private String scope;
  private String widgetValues;
  private String properties;
  private String clusterName;
  private String tag;

  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @JsonProperty("widget_name")
  public String getWidgetName() {
    return widgetName;
  }

  public void setWidgetName(String widgetName) {
    this.widgetName = widgetName;
  }

  @JsonProperty("widget_type")
  public String getWidgetType() {
    return widgetType;
  }

  public void setWidgetType(String widgetType) {
    this.widgetType = widgetType;
  }

  public String getMetrics() {
    return metrics;
  }

  public void setMetrics(String metrics) {
    this.metrics = metrics;
  }

  @JsonProperty("time_created")
  public Long getTimeCreated() {
    return timeCreated;
  }

  public void setTimeCreated(Long timeCreated) {
    this.timeCreated = timeCreated;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty("display_name")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  @JsonProperty("values")
  public String getWidgetValues() {
    return widgetValues;
  }

  public void setWidgetValues(String widgetValues) {
    this.widgetValues = widgetValues;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  @JsonProperty("cluster_name")
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  @Override
  public String toString() {
    return widgetName;
  }

  /**
   * Gets an {@link org.apache.ambari.server.controller.WidgetResponse} from the supplied entity.
   *
   * @param entity
   *          the entity (not {@code null}).
   * @return the response.
   */
  public static WidgetResponse coerce(WidgetEntity entity) {
    if (null == entity) {
      return null;
    }

    WidgetResponse response = new WidgetResponse();
    response.setId(entity.getId());
    response.setWidgetName(entity.getWidgetName());
    response.setWidgetType(entity.getWidgetType());
    response.setDescription(entity.getDescription());
    response.setMetrics(entity.getMetrics());
    response.setTimeCreated(entity.getTimeCreated());
    response.setAuthor(entity.getAuthor());
    response.setDisplayName(entity.getDefaultSectionName());
    response.setScope(entity.getScope());
    response.setWidgetValues(entity.getWidgetValues());
    response.setProperties(entity.getProperties());
    String clusterName = (entity.getClusterEntity() != null) ? entity.getClusterEntity().getClusterName() : null;
    response.setClusterName(clusterName);
    response.setTag(entity.getTag());

    return response;
  }
}
