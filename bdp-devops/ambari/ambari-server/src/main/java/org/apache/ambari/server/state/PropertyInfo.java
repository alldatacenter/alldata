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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlList;

import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.w3c.dom.Element;

@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyInfo {
  private String name;
  private String value;
  private String description;

  @XmlElement(name = "display-name")
  private String displayName;

  private String filename;
  private boolean deleted;

  @XmlElement(name="on-ambari-upgrade", required = true)
  private PropertyUpgradeBehavior propertyAmbariUpgradeBehavior;

  @XmlElement(name="on-stack-upgrade")
  private PropertyStackUpgradeBehavior propertyStackUpgradeBehavior = new PropertyStackUpgradeBehavior();

  @XmlAttribute(name = "require-input")
  private boolean requireInput;

  @XmlElement(name = "property-type")
  @XmlList
  private Set<PropertyType> propertyTypes = new HashSet<>();

  @XmlAnyElement
  private List<Element> propertyAttributes = new ArrayList<>();

  @XmlElement(name = "value-attributes")
  private ValueAttributesInfo propertyValueAttributes =
    new ValueAttributesInfo();

  @XmlElementWrapper(name="depends-on")
  @XmlElement(name = "property")
  private Set<PropertyDependencyInfo> dependsOnProperties =
    new HashSet<>();

  @XmlElementWrapper(name="property_depended_by")
  private Set<PropertyDependencyInfo> dependedByProperties =
    new HashSet<>();

  /**
   * The list of properties that use this property.
   * Password properties may be used by other properties in
   * the same config type or different config type, typically
   * when asking for user name and password pairs.
   */
  @XmlElementWrapper(name="used-by")
  @XmlElement(name = "property")
  private Set<PropertyDependencyInfo> usedByProperties =
          new HashSet<>();

  @XmlElementWrapper(name="supported-refresh-commands")
  @XmlElement(name="refresh-command")
  private Set<RefreshCommand> supportedRefreshCommands = new HashSet<>();


  //This method is called after all the properties (except IDREF) are unmarshalled for this object,
  //but before this object is set to the parent object.
  void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    // Iterate through propertyTypes and remove any unrecognized property types
    // that may be introduced with custom service definitions
    propertyTypes.remove(null);
  }

  public PropertyInfo() {
    propertyAmbariUpgradeBehavior = new PropertyUpgradeBehavior();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<PropertyDependencyInfo> getUsedByProperties() {
    return usedByProperties;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }
  
  public Set<PropertyType> getPropertyTypes() {
    return propertyTypes;
  }

  public void setPropertyTypes(Set<PropertyType> propertyTypes) {
    this.propertyTypes = propertyTypes;
  }

  public PropertyUpgradeBehavior getPropertyAmbariUpgradeBehavior() {
    return propertyAmbariUpgradeBehavior;
  }

  public void setPropertyAmbariUpgradeBehavior(PropertyUpgradeBehavior propertyAmbariUpgradeBehavior) {
    this.propertyAmbariUpgradeBehavior = propertyAmbariUpgradeBehavior;
  }

  public StackConfigurationResponse convertToResponse() {
    return new StackConfigurationResponse(getName(), getValue(),
      getDescription(), getDisplayName() , getFilename(), isRequireInput(),
      getPropertyTypes(), getAttributesMap(), getPropertyValueAttributes(),
      getDependsOnProperties());
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public Map<String, String> getAttributesMap() {
    Map<String, String> attributes = new HashMap<>();
    for (Element propertyAttribute : propertyAttributes) {
      attributes.put(propertyAttribute.getTagName(), propertyAttribute.getFirstChild().getNodeValue());
    }

    // inject "hidden" property_value_attribute into property_attributes, see AMBARI-17223
    String hidden = getPropertyValueAttributes().getHidden();
    if (hidden != null) {
      attributes.putIfAbsent("hidden", hidden);
    }

    return attributes;
  }

  public ValueAttributesInfo getPropertyValueAttributes() {
    return propertyValueAttributes;
  }

  public Set<PropertyDependencyInfo> getDependsOnProperties() {
    return dependsOnProperties;
  }

  public void setPropertyValueAttributes(ValueAttributesInfo propertyValueAttributes) {
    this.propertyValueAttributes = propertyValueAttributes;
  }

  public Set<PropertyDependencyInfo> getDependedByProperties() {
    return dependedByProperties;
  }

  public boolean isRequireInput() {
    return requireInput;
  }

  public void setRequireInput(boolean requireInput) {
    this.requireInput = requireInput;
  }

  public List<Element> getPropertyAttributes() {
    return propertyAttributes;
  }

  public void setPropertyAttributes(List<Element> propertyAttributes) {
    this.propertyAttributes = propertyAttributes;
  }

  public Set<RefreshCommand> getSupportedRefreshCommands() {
    return supportedRefreshCommands;
  }

  public void setSupportedRefreshCommands(Set<RefreshCommand> supportedRefreshCommands) {
    this.supportedRefreshCommands = supportedRefreshCommands;
  }

  /**
   * Willcard properties should not be included to stack configurations.
   * @return
   */
  public boolean shouldBeConfigured() {
    return !getName().contains("*");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((filename == null) ? 0 : filename.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PropertyInfo other = (PropertyInfo) obj;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (filename == null) {
      if (other.filename != null)
        return false;
    } else if (!filename.equals(other.filename))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "PropertyInfo{" +
      "name='" + name + '\'' +
      ", value='" + value + '\'' +
      ", description='" + description + '\'' +
      ", filename='" + filename + '\'' +
      ", deleted=" + deleted +
      ", requireInput=" + requireInput +
      ", propertyTypes=" + propertyTypes +
      ", propertyAttributes=" + propertyAttributes +
      ", propertyValueAttributes=" + propertyValueAttributes +
      ", dependsOnProperties=" + dependsOnProperties +
      ", dependedByProperties=" + dependedByProperties +
      '}';
  }

  public PropertyStackUpgradeBehavior getPropertyStackUpgradeBehavior() {
    return propertyStackUpgradeBehavior;
  }

  public void setPropertyStackUpgradeBehavior(PropertyStackUpgradeBehavior propertyStackUpgradeBehavior) {
    this.propertyStackUpgradeBehavior = propertyStackUpgradeBehavior;
  }

  public enum PropertyType {
    PASSWORD,
    USER,
    UID,
    GROUP,
    GID,
    TEXT,
    ADDITIONAL_USER_PROPERTY,
    NOT_MANAGED_HDFS_PATH,
    VALUE_FROM_PROPERTY_FILE,
    KERBEROS_PRINCIPAL
  }
}
