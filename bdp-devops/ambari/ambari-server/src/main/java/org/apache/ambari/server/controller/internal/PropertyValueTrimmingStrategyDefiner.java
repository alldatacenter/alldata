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


import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.ValueAttributesInfo;

import com.google.common.collect.ImmutableSet;

public class PropertyValueTrimmingStrategyDefiner {

  private static final Set<String> SET_OF_URL_PROPERTIES = ImmutableSet.of(
    "javax.jdo.option.ConnectionURL",
    "oozie.service.JPAService.jdbc.url"
  );

  private static TrimmingStrategy getTrimmingStrategyForConfigProperty(Stack.ConfigProperty configProperty) {
    if (configProperty != null) {
      ValueAttributesInfo valueAttributesInfo = configProperty.getPropertyValueAttributes();
      if (valueAttributesInfo != null) {
        String type = valueAttributesInfo.getType();
        if ("directory".equals(type) || "directories".equals(type)) {
          return TrimmingStrategy.DIRECTORIES;
        } else if ("host".equals(type)) {
          return TrimmingStrategy.DEFAULT;
        }
      }
      if (configProperty.getPropertyTypes() != null && configProperty.getPropertyTypes().
              contains(org.apache.ambari.server.state.PropertyInfo.PropertyType.PASSWORD)) {
        return TrimmingStrategy.PASSWORD;
      }
    }
    return null;
  }

  private static TrimmingStrategy getTrimmingStrategyByPropertyName(String propertyName) {
    if (SET_OF_URL_PROPERTIES.contains(propertyName)) {
      return TrimmingStrategy.DEFAULT;
    } else {
      return TrimmingStrategy.DELETE_SPACES_AT_END;
    }
  }

  public static TrimmingStrategy defineTrimmingStrategy(Stack stack, String propertyName, String configType) {
    TrimmingStrategy result = null;
    String service = stack.getServiceForConfigType(configType);
    if (service != null) {
      Map<String, Stack.ConfigProperty> map = stack.getConfigurationPropertiesWithMetadata(service, configType);
      if (map != null) {
        Stack.ConfigProperty configProperty = map.get(propertyName);
        if (configProperty != null) {
          result = getTrimmingStrategyForConfigProperty(configProperty);
        }
      }
    }
    if (result == null) {
      result = getTrimmingStrategyByPropertyName(propertyName);
    }
    return result;
  }
}
