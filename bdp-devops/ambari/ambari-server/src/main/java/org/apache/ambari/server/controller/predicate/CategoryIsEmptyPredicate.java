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

package org.apache.ambari.server.controller.predicate;

import java.util.Map;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Predicate that checks if the associated property category is empty.  If the associated
 * property id references a Map property then treat the Map as a category.
 */
public class CategoryIsEmptyPredicate extends CategoryPredicate {

  public CategoryIsEmptyPredicate(String propertyId) {
    super(propertyId);
  }

  @Override
  public boolean evaluate(Resource resource) {
    String propertyId = getPropertyId();

    // If the property exists as a Map then check isEmpty
    Object value = resource.getPropertyValue(propertyId);
    if (value instanceof Map) {
      Map<?,?> mapValue = (Map) value;
      return mapValue.isEmpty();
    }
    // Get the category
    Map<String, Object> properties = resource.getPropertiesMap().get(propertyId);
    return properties == null ? true : properties.isEmpty();
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public String toString() {
    return "isEmpty(" + getPropertyId() + ")";
  }
}
