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

package org.apache.ambari.server.api.services;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class RootServiceComponentConfiguration {

  private final Map<String, String> properties;
  private final Map<String, String> propertyTypes;

  public RootServiceComponentConfiguration() {
    this(new TreeMap<>(), new TreeMap<>());
  }

  public RootServiceComponentConfiguration(Map<String, String> properties, Map<String, String> propertyTypes) {
    this.properties = properties == null ? new TreeMap<>() : properties;
    this.propertyTypes = propertyTypes == null ? new TreeMap<>() : propertyTypes;
  }

  public void addProperty(String propertyName, String propertyValue) {
    properties.put(propertyName, propertyValue);
  }

  public void addPropertyType(String propertyName, String propertyType) {
    propertyTypes.put(propertyName, propertyType);
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public Map<String, String> getPropertyTypes() {
    return propertyTypes;
  }

  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(3, 19, this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
