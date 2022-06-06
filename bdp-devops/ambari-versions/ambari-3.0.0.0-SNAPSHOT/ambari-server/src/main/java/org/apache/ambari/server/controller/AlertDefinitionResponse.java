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

import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.alert.SourceType;
import org.codehaus.jackson.annotate.JsonProperty;


/**
 * The {@link AlertDefinitionResponse} encapsulates the definition information
 * that should be serialized and returned in REST requests for alerts, groups,
 * and targets.
 */
public class AlertDefinitionResponse {

  private String serviceName = null;
  private String componentName = null;
  private String name = null;
  private String label = null;
  private String description = null;
  private Long definitionId;
  private boolean enabled = true;
  private SourceType sourceType;

  /**
   * @return the definitionId
   */
  @JsonProperty("id")
  public Long getDefinitionId() {
    return definitionId;
  }

  /**
   * @param definitionId
   *          the definitionId to set
   */
  public void setDefinitionId(Long definitionId) {
    this.definitionId = definitionId;
  }

  /**
   * @return the service name
   */
  @JsonProperty("service_name")
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param name the service name
   */
  public void setServiceName(String name) {
    serviceName = name;
  }

  /**
   * @return the component name
   */
  @JsonProperty("component_name")
  public String getComponentName() {
    return componentName;
  }

  /**
   *
   * @param name the component name
   */
  public void setComponentName(String name) {
    componentName = name;
  }

  /**
   * @return the name
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * @param definitionName
   *          the definition name.
   */
  public void setName(String definitionName) {
    name = definitionName;
  }

  /**
   * @return the label for the definition or {@code null} if none.
   */
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  /**
   * Sets the label for this definition.
   *
   * @param definitionLabel
   */
  public void setLabel(String definitionLabel) {
    label = definitionLabel;
  }

  /**
   * @return the description for the definition or {@code null} if none.
   */
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description for this definition.
   *
   * @param definitionDescription
   */
  public void setDescription(String definitionDescription) {
    description = definitionDescription;
  }

  /**
   * Gets whether this definition is enabled.
   *
   * @return {@code true} if enabled.
   */
  @JsonProperty("enabled")
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets whether this definition is enabled.
   *
   * @param enabled
   *          {@code true} if enabled.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Gets the type of alert.
   *
   * @return the sourceType
   */
  @JsonProperty("source_type")
  public SourceType getSourceType() {
    return sourceType;
  }

  /**
   * Sets the type of alert.
   *
   * @param sourceType
   *          the sourceType to set
   */
  public void setSourceType(SourceType sourceType) {
    this.sourceType = sourceType;
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Gets an {@link AlertDefinitionResponse} from the supplied entity.
   *
   * @param entity
   *          the entity (not {@code null}).
   * @return the response.
   */
  public static AlertDefinitionResponse coerce(AlertDefinitionEntity entity) {
    if (null == entity) {
      return null;
    }

    AlertDefinitionResponse response = new AlertDefinitionResponse();
    response.setDefinitionId(entity.getDefinitionId());
    response.setComponentName(entity.getComponentName());
    response.setLabel(entity.getLabel());
    response.setDescription(entity.getDescription());
    response.setName(entity.getDefinitionName());
    response.setServiceName(entity.getServiceName());
    response.setEnabled(entity.getEnabled());
    response.setSourceType(entity.getSourceType());

    return response;
  }
}
