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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.ambari.server.topology.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * Handles most of type-specific behavior for blueprint export.
 */
public enum BlueprintExportType {
  /**
   * The exported blueprint contains all properties of all config types
   * for services present in the cluster.
   */
  FULL {
    @Override
    public Configuration filter(Configuration actualConfig, Configuration defaultConfig) {
      // no-op
      return actualConfig;
    }

    @Override
    public boolean include(String value, String defaultValue) {
      return true;
    }

    @Override
    public boolean include(Collection<?> collection) {
      return true;
    }

    @Override
    public boolean include(Map<?, ?> map) {
      return true;
    }
  },

  /**
   * The exported blueprint contains only the properties that do not match default values
   * as defined in the stack.  Empty lists/maps are also omitted.
   */
  MINIMAL {
    @Override
    public Configuration filter(Configuration actualConfig, Configuration defaultConfig) {
      for (Map.Entry<String, Map<String, String>> configTypeEntry : ImmutableSet.copyOf(actualConfig.getProperties().entrySet())) {
        String configType = configTypeEntry.getKey();
        Map<String, String> properties = configTypeEntry.getValue();
        for (Map.Entry<String, String> propertyEntry : ImmutableSet.copyOf(properties.entrySet())) {
          String propertyName = propertyEntry.getKey();
          String propertyValue = propertyEntry.getValue();
          String defaultValue = defaultConfig.getPropertyValue(configType, propertyName);
          if (include(propertyValue, defaultValue))  {
            LOG.debug("Including {}/{} in exported blueprint, as default value and actual value differ:\n{}\nvs\n{}", configType, propertyName, defaultValue, propertyValue);
          } else {
            LOG.debug("Omitting {}/{} from exported blueprint, as it has the default value of {}", configType, propertyName, propertyValue);
            actualConfig.removeProperty(configType, propertyName);
          }
        }
        if (properties.isEmpty()) {
          actualConfig.getProperties().remove(configType);
        }
      }

      for (Map.Entry<String, Map<String, Map<String, String>>> configTypeEntry : ImmutableSet.copyOf(actualConfig.getAttributes().entrySet())) {
        String configType = configTypeEntry.getKey();
        Map<String, Map<String, String>> attributes = configTypeEntry.getValue();
        for (Map.Entry<String, Map<String, String>> attributeEntry : ImmutableSet.copyOf(attributes.entrySet())) {
          String attributeName = attributeEntry.getKey();
          Map<String, String> properties = attributeEntry.getValue();
          for (Map.Entry<String, String> propertyEntry : ImmutableSet.copyOf(properties.entrySet())) {
            String propertyName = propertyEntry.getKey();
            String attributeValue = propertyEntry.getValue();
            String defaultValue = defaultConfig.getAttributeValue(configType, propertyName, attributeName);
            if (include(attributeValue, defaultValue))  {
              LOG.debug("Including {}/{}/{} in exported blueprint, as default value and actual value differ:\n{}\nvs\n{}", configType, attributeName, propertyName, defaultValue, attributeValue);
            } else {
              LOG.debug("Omitting {}/{}/{} from exported blueprint, as it has the default value of {}", configType, attributeName, propertyName, attributeValue);
              properties.remove(propertyName);
            }
          }
          if (properties.isEmpty()) {
            attributes.remove(attributeName);
          }
        }
        if (attributes.isEmpty()) {
          actualConfig.getAttributes().remove(configType);
        }
      }

      return actualConfig;
    }

    @Override
    public boolean include(String value, String defaultValue) {
      return value != null && (
        defaultValue == null ||
          !Objects.equals(StringUtils.trim(defaultValue), StringUtils.trim(value))
      );
    }

    @Override
    public boolean include(Collection<?> collection) {
      return collection != null && !collection.isEmpty();
    }

    @Override
    public boolean include(Map<?, ?> map) {
      return map != null && !map.isEmpty();
    }
  },
  ;

  public abstract Configuration filter(Configuration actualConfig, Configuration defaultConfig);
  public abstract boolean include(String value, String defaultValue);
  public abstract boolean include(Collection<?> collection);
  public abstract boolean include(Map<?, ?> map);

  public static final BlueprintExportType DEFAULT = MINIMAL;
  public static final String PREFIX = "blueprint";
  private static final String SEPARATOR = "_";
  private static final Logger LOG = LoggerFactory.getLogger(BlueprintExportType.class);

  public static Optional<BlueprintExportType> parse(String input) {
    if (input == null || !input.startsWith(PREFIX)) {
      return Optional.empty();
    }

    int separatorPos = input.indexOf(SEPARATOR);
    if (separatorPos == -1 || separatorPos + 1 == input.length()) {
      return Optional.of(DEFAULT);
    }

    switch (input.substring(separatorPos + 1)) {
      case "full": return Optional.of(FULL);
      case "minimal": return Optional.of(MINIMAL);
      default: return Optional.of(DEFAULT);
    }
  }
}
