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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

/**
 * Simple resource implementation.
 */
public class ResourceImpl implements Resource {

  /**
   * The resource type.
   */
  private final Type type;

  /**
   * The map of property maps keyed by property category.
   */
  private final Map<String, Map<String, Object>> propertiesMap =
      Collections.synchronizedMap(new TreeMap<String, Map<String, Object>>());

  // ----- Constructors ------------------------------------------------------

  /**
   * Create a resource of the given type.
   *
   * @param type the resource type
   */
  public ResourceImpl(Type type) {
    this.type = type;
  }

  /**
   * Copy constructor
   *
   * @param resource the resource to copy
   */
  public ResourceImpl(Resource resource) {
    this(resource, null);
  }

  /**
   * Construct a resource from the given resource, setting only the properties
   * that are found in the given set of property and category ids.
   *
   * @param resource    the resource to copy
   * @param propertyIds the set of requested property and category ids
   */
  public ResourceImpl(Resource resource, Set<String> propertyIds) {
    this.type = resource.getType();

    for (Map.Entry<String, Map<String, Object>> categoryEntry :
        resource.getPropertiesMap().entrySet()) {
      String category = categoryEntry.getKey();
      Map<String, Object> propertyMap = categoryEntry.getValue();
      if (propertyMap != null) {
        for (Map.Entry<String, Object> propertyEntry : propertyMap.entrySet()) {
          String propertyId = PropertyHelper.getPropertyId(category, propertyEntry.getKey());
          if (propertyIds == null || propertyIds.isEmpty() || PropertyHelper.containsProperty(propertyIds, propertyId)) {
            Object propertyValue = propertyEntry.getValue();
            setProperty(propertyId, propertyValue);
          }
        }
      }
    }
  }


  // ----- Resource ----------------------------------------------------------

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public Map<String, Map<String, Object>> getPropertiesMap() {
    return propertiesMap;
  }

  @Override
  public void setProperty(String id, Object value) {
    String categoryKey = getCategoryKey(PropertyHelper.getPropertyCategory(id));

    Map<String, Object> properties = propertiesMap.get(categoryKey);
    if (properties == null) {
      properties = Collections.synchronizedMap(new TreeMap<String, Object>());
      propertiesMap.put(categoryKey, properties);
    }
    properties.put(PropertyHelper.getPropertyName(id), value);
  }

  @Override
  public void addCategory(String id) {
    String categoryKey = getCategoryKey(id);

    if (!propertiesMap.containsKey(categoryKey)) {
      propertiesMap.put(categoryKey, new HashMap<>());
    }
  }

  @Override
  public Object getPropertyValue(String id) {
    String categoryKey = getCategoryKey(PropertyHelper.getPropertyCategory(id));

    Map<String, Object> properties = propertiesMap.get(categoryKey);

    return properties == null ?
        null : properties.get(PropertyHelper.getPropertyName(id));
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("Resource : ").append(type).append("\n");
    sb.append("Properties:\n");
    sb.append(propertiesMap);

    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ResourceImpl resource = (ResourceImpl) o;

    return type == resource.type &&
        !(propertiesMap != null ? !propertiesMap.equals(resource.propertiesMap) : resource.propertiesMap != null);
  }

  @Override
  public int hashCode() {
    return 31 * type.hashCode() + (propertiesMap != null ? propertiesMap.hashCode() : 0);
  }

  // ----- utility methods ---------------------------------------------------

  private String getCategoryKey(String category) {
    return category == null ? "" : category;
  }
}
