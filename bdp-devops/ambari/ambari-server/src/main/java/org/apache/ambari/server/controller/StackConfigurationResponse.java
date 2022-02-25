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

package org.apache.ambari.server.controller;


import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.state.ValueAttributesInfo;

import io.swagger.annotations.ApiModelProperty;


public class StackConfigurationResponse {

  /**
   * Stack configuration response.
   * @param propertyName Property Key
   * @param propertyValue Property Value
   * @param propertyDescription Property Description
   * @param type Configuration type
   * @param propertyAttributes Attributes map
   */
  public StackConfigurationResponse(String propertyName, String propertyValue, String propertyDescription,
                                    String type, Map<String, String> propertyAttributes) {
    setPropertyName(propertyName);
    setPropertyValue(propertyValue);
    setPropertyDescription(propertyDescription);
    setType(type);
    setPropertyAttributes(propertyAttributes);
  }

  /**
   * Stack configuration response with all properties.
   * @param propertyName Property Key
   * @param propertyValue Property Value
   * @param propertyDescription Property Description
   * @param type Configuration type
   * @param isRequired Is required to be set
   * @param propertyTypes Property Types
   * @param propertyAttributes Attributes map
   * @param propertyValueAttributes Value Attributes
   * @param dependsOnProperties depends on properties set
   */
  public StackConfigurationResponse(String propertyName, String propertyValue,
                                    String propertyDescription, String propertyDisplayName, String type,
                                    Boolean isRequired,
                                    Set<PropertyType> propertyTypes,
                                    Map<String, String> propertyAttributes,
                                    ValueAttributesInfo propertyValueAttributes,
                                    Set<PropertyDependencyInfo> dependsOnProperties) {
    setPropertyName(propertyName);
    setPropertyValue(propertyValue);
    setPropertyDescription(propertyDescription);
    setPropertyDisplayName(propertyDisplayName);
    setType(type);
    setRequired(isRequired);
    setPropertyType(propertyTypes);
    setPropertyAttributes(propertyAttributes);
    setPropertyValueAttributes(propertyValueAttributes);
    setDependsOnProperties(dependsOnProperties);
  }

  private String stackName;
  private String stackVersion;
  private String serviceName;
  private String propertyName;
  private String propertyValue;
  private String propertyDescription;
  private String propertyDisplayName;
  private String type;
  private Map<String, String> propertyAttributes;
  private ValueAttributesInfo propertyValueAttributes;
  private Set<PropertyDependencyInfo> dependsOnProperties;
  private Boolean isRequired;
  private Set<PropertyType> propertyTypes;

  @ApiModelProperty(name = "stack_name")
  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  @ApiModelProperty(name = "stack_version")
  public String getStackVersion() {
    return stackVersion;
  }

  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  @ApiModelProperty(name = "service_name")
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @ApiModelProperty(name = "property_name")
  public String getPropertyName() {
    return propertyName;
  }

  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  @ApiModelProperty(name = "property_value")
  public String getPropertyValue() {
    return propertyValue;
  }

  public void setPropertyValue(String propertyValue) {
    this.propertyValue = propertyValue;
  }

  @ApiModelProperty(name = "property_description")
  public String getPropertyDescription() {
    return propertyDescription;
  }

  public void setPropertyDescription(String propertyDescription) {
    this.propertyDescription = propertyDescription;
  }

  @ApiModelProperty(name = "property_display_name")
  public String getPropertyDisplayName() {
    return propertyDisplayName;
  }

  public void setPropertyDisplayName(String propertyDisplayName) {
    this.propertyDisplayName = propertyDisplayName;
  }

  /**
   * Configuration type
   * @return Configuration type (*-site.xml)
   */
  public String getType() {
    return type;
  }

  @ApiModelProperty(name = "type")
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Provides attributes of this configuration.
   *
   * @return Map of attribute name to attribute value
   */
  @ApiModelProperty(hidden = true)
  public Map<String, String> getPropertyAttributes() {
    return propertyAttributes;
  }

  /**
   * Sets attributes for this configuration.
   *
   * @param propertyAttributes Map of attribute name to attribute value
   */
  public void setPropertyAttributes(Map<String, String> propertyAttributes) {
    this.propertyAttributes = propertyAttributes;
  }

  /**
   * Provides value attributes of this configuration.
   *
   * @return value attributes
   */
  @ApiModelProperty(name = "property_value_attributes")
  public ValueAttributesInfo getPropertyValueAttributes() {
    return propertyValueAttributes;
  }

  /**
   * Sets value attributes for this configuration.
   *
   * @param propertyValueAttributes
   */
  public void setPropertyValueAttributes(ValueAttributesInfo propertyValueAttributes) {
    this.propertyValueAttributes = propertyValueAttributes;
  }

  /**
   * Provides depends on properties of this configuration.
   *
   * @return depends on properties set
   */
  @ApiModelProperty(name = "dependencies")
  public Set<PropertyDependencyInfo> getDependsOnProperties() {
    return dependsOnProperties;
  }

  /**
   * Sets depends on properties set for this configuration.
   *
   * @param dependsOnProperties
   */
  public void setDependsOnProperties(Set<PropertyDependencyInfo> dependsOnProperties) {
    this.dependsOnProperties = dependsOnProperties;
  }

  /**
   * Is property a isRequired property
   * @return True/False
   */
  @ApiModelProperty(hidden = true)
  public Boolean isRequired() {
    return isRequired;
  }

  /**
   * Set required attribute on this property.
   * @param required True/False.
   */
  public void setRequired(Boolean required) {
    this.isRequired = required;
  }

  /**
   * Get type of property as set in the stack definition.
   * @return Property type.
   */
  @ApiModelProperty(name = "property_type")
  public Set<PropertyType> getPropertyType() {
    return propertyTypes;
  }

  public void setPropertyType(Set<PropertyType> propertyTypes) {
    this.propertyTypes = propertyTypes;
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface StackConfigurationResponseSwagger extends ApiModel {
    @ApiModelProperty(name = "StackConfigurations")
    public StackConfigurationResponse getStackConfigurationResponse();
  }
}
