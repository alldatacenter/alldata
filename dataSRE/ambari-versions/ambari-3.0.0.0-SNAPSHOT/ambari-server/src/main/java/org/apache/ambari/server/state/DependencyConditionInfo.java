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

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Represents stack component dependency condition information provided in
 * metainfo.xml file.
 */
@XmlTransient
@XmlSeeAlso({ PropertyExists.class, PropertyValueEquals.class })
public interface DependencyConditionInfo {
  /**
   * Returns true if the dependency condition is satisfied
   *
   * @param properties
   * @return boolean
   */
  boolean isResolved(Map<String, Map<String, String>> properties);
}

/**
 * This class represents a dependency condition that is based on existence of a
 * certain property.
 */
class PropertyExists implements DependencyConditionInfo {
  /**
   * configType of the conditional dependency
   */
  protected String configType;

  /**
   * property of the conditional dependency
   */
  protected String property;

  /**
   * type of conditional dependency
   */
  protected String type = this.getClass().getSimpleName();

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlElement
  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  @XmlElement
  public String getConfigType() {
    return configType;
  }

  public void setConfigType(String configType) {
    this.configType = configType;
  }

  @Override
  public boolean isResolved(Map<String, Map<String, String>> properties) {
    return (properties.get(configType).containsKey(property));
  }
}

/**
 * This class represents a dependency condition that is based on value of a
 * certain property.
 */
class PropertyValueEquals extends PropertyExists {
  /**
   * propertyValue of the property
   */
  protected String propertyValue;

  @XmlElement
  public String getPropertyValue() {
    return propertyValue;
  }

  public void setPropertyValue(String propertyValue) {
    this.propertyValue = propertyValue;
  }

  @Override
  public boolean isResolved(Map<String, Map<String, String>> properties) {
    return (super.isResolved(properties) && propertyValue.equals(properties.get(configType).get(property)));
  }
}
