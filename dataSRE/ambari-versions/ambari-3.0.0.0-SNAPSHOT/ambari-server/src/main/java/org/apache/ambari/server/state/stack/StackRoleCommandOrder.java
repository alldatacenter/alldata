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

package org.apache.ambari.server.state.stack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.slf4j.Logger;

/**
 * Represents the stack <code>role_command_order.json</code> file.
 */

public class StackRoleCommandOrder {

  private final static String GENERAL_DEPS_KEY = "general_deps";
  private final static String GLUSTERFS_DEPS_KEY = "optional_glusterfs";
  private final static String NO_GLUSTERFS_DEPS_KEY = "optional_no_glusterfs";
  private final static String NAMENODE_HA_DEPS_KEY = "namenode_optional_ha";
  private final static String RESOURCEMANAGER_HA_DEPS_KEY = "resourcemanager_optional_ha";
  private final static String HOST_ORDERED_UPGRADES_DEPS_KEY = "host_ordered_upgrade";

  private HashMap<String, Object> content;

  /**
   * Initialize object
   */
  public StackRoleCommandOrder() {
  }

  /**
   * Initialize object
   *
   * @param content role command order content
   */
  public StackRoleCommandOrder(HashMap<String, Object> content) {
    this.content = content;
  }

  /**
   * Get role command order content
   *
   * @return role command order content
   */

  public HashMap<String, Object> getContent() {
    return content;
  }

  /**
   * Set role command order content
   *
   * @param content role command order content
   */

  public void setContent(HashMap<String, Object> content) {
    this.content = content;
  }

  /**
   * merge StackRoleCommandOrder content with parent
   *
   * @param parent parent StackRoleCommandOrder instance
   */

  public void merge(StackRoleCommandOrder parent) {
    merge(parent, false);
  }

  /**
   * merge StackRoleCommandOrder content with parent
   *
   * @param parent parent StackRoleCommandOrder instance
   */

  public void merge(StackRoleCommandOrder parent, boolean mergeProperties) {
    HashMap<String, Object> mergedRoleCommandOrders = new HashMap<>();
    HashMap<String, Object> parentData = parent.getContent();

    List<String> keys = Arrays.asList(GENERAL_DEPS_KEY, GLUSTERFS_DEPS_KEY, NO_GLUSTERFS_DEPS_KEY,
        NAMENODE_HA_DEPS_KEY, RESOURCEMANAGER_HA_DEPS_KEY, HOST_ORDERED_UPGRADES_DEPS_KEY);

    for (String key : keys) {
      if (parentData.containsKey(key) && content.containsKey(key)) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> parentProperties = (Map<String, Object>) parentData.get(key);
        Map<String, Object> childProperties = (Map<String, Object>) content.get(key);
        MultiValueMap childAndParentProperties = null;

        childAndParentProperties = new MultiValueMap();
        childAndParentProperties.putAll(childProperties);
        childAndParentProperties.putAll(parentProperties);

        for (Object property : childAndParentProperties.keySet()) {
          List propertyValues = (List) childAndParentProperties.get(property);
          Object values = propertyValues.get(0);
          if (mergeProperties) {
            List<String> valueList = new ArrayList<>();
            for (Object value : propertyValues) {
              if (value instanceof List) {
                valueList.addAll((List<String>) value);
              } else {
                valueList.add(value.toString());
              }
            }
            values = valueList;
          }

          result.put((String) property, values);
        }
        mergedRoleCommandOrders.put(key, result);
      } else if (content.containsKey(key)) {
        mergedRoleCommandOrders.put(key, content.get(key));
      } else if (parentData.containsKey(key)) {
        mergedRoleCommandOrders.put(key, parentData.get(key));
      }
    }
    content = mergedRoleCommandOrders;
  }

  public void printRoleCommandOrder(Logger LOG) {
    Map<String, Object> map = getContent();
    List<String> keys = Arrays.asList(GENERAL_DEPS_KEY, GLUSTERFS_DEPS_KEY,
      NO_GLUSTERFS_DEPS_KEY, NAMENODE_HA_DEPS_KEY, RESOURCEMANAGER_HA_DEPS_KEY);

    for (String key : keys) {
      LOG.debug(key);
      Object value = map.get(key);
      if (value instanceof Map) {
		Map<String, Object> deps = (Map<String, Object>) map.get(key);
		for (String depKey : deps.keySet()) {
		  Object depValue = deps.get(depKey);
		  if (depValue instanceof Collection) {
			StringBuilder buffer = new StringBuilder();
			for (Object o : ((Collection) depValue)) {
				if (buffer.length() > 0) {
          buffer.append(",");
        }
				buffer.append(o);
			}
			depValue = buffer.toString();
		  }
      LOG.debug("{} => {}", depKey, depValue);
		}
      }
    }
  }

}

