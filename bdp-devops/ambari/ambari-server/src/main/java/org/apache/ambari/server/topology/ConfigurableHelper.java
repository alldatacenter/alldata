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

package org.apache.ambari.server.topology;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.PROPERTIES_ATTRIBUTES_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.PROPERTIES_PROPERTY_ID;
import static org.apache.ambari.server.topology.ConfigurationFactory.isKeyInLegacyFormat;
import static org.apache.ambari.server.topology.ConfigurationFactory.isKeyInNewFormat;
import static org.apache.ambari.server.topology.ConfigurationFactory.splitConfigurationKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Triple;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Helper class for {@link Configurable}
 */
public class ConfigurableHelper {

  private static final ImmutableSet<String> PERMITTED_CONFIG_FIELDS = ImmutableSet.of(PROPERTIES_PROPERTY_ID, PROPERTIES_ATTRIBUTES_PROPERTY_ID);

  /**
   * Parses configuration maps The configs can be in fully structured JSON, e.g.
   * <code>
   * [{"hdfs-site":
   *  "properties": {
   *    ""dfs.replication": "3",
   *    ...
   *  },
   *  properties_attributes: {}
   * }]
   * </code>
   * or flattened like
   * <code>
   * [{
   *  "hdfs-site/properties/dfs.replication": "3",
   *  ...
   * }]
   * </code>
   * In the latter case it calls {@link ConfigurationFactory#getConfiguration(Collection)}
   */
  public static Configuration parseConfigs(@Nullable Collection<? extends Map<String, ?>> configs) {
    Configuration configuration;

    if (null == configs || configs.isEmpty()) {
      configuration = Configuration.newEmpty();
    }
    else if (configs.iterator().next().keySet().iterator().next().contains("/")) {
      // Configuration has keys with slashes like "zk.cfg/properties/dataDir" means it is coming through
      // the resource framework and must be parsed with configuration factories
      checkFlattenedConfig(configs);
      configuration = new ConfigurationFactory().getConfiguration((Collection<Map<String, String>>)configs);
    }
    else {
      // If the configuration does not have keys with slashes it means it is coming from plain JSON and needs to be
      // parsed accordingly.
      Map<String, Map<String, String>> allProperties = new HashMap<>();
      Map<String, Map<String, Map<String, String>>> allAttributes = new HashMap<>();
      configs.forEach( item -> {

        checkArgument(item.size() == 1, "Each config object must have a single property which is the name of the config," +
          " e.g. \"cluster-env\" : {...}");

        Map.Entry<String, ?> configEntry = item.entrySet().iterator().next();
        String configName = item.keySet().iterator().next();

        checkArgument(configEntry.getValue() instanceof Map,
          "The value for %s must be a JSON object (found: %s)", configName, getClassName(configEntry.getValue()));

        Map<String, ?> configData = (Map<String, ?>) configEntry.getValue();

        Set<String> extraKeys = Sets.difference(configData.keySet(), PERMITTED_CONFIG_FIELDS);
        boolean legacy = extraKeys.size() == configData.keySet().size();
        checkArgument(legacy || extraKeys.isEmpty(), "Invalid fields in %s configuration: %s", configName, extraKeys);

        if (legacy) {
          checkMap("don't care", configData, String.class);
          Map<String, String> properties = (Map<String, String>)configData;
          allProperties.put(configName, properties);
        } else {
          if (configData.containsKey(PROPERTIES_PROPERTY_ID)) {
            checkMap(PROPERTIES_PROPERTY_ID, configData.get(PROPERTIES_PROPERTY_ID), String.class);

            Map<String, String> properties = (Map<String, String>) configData.get(PROPERTIES_PROPERTY_ID);
            allProperties.put(configName, properties);
          }
          if (configData.containsKey(PROPERTIES_ATTRIBUTES_PROPERTY_ID)) {
            checkMap(PROPERTIES_ATTRIBUTES_PROPERTY_ID, configData.get(PROPERTIES_ATTRIBUTES_PROPERTY_ID), Map.class);
            Map<String, Map<String, String>> attributes =
              (Map<String, Map<String, String>>) configData.get(PROPERTIES_ATTRIBUTES_PROPERTY_ID);
            attributes.forEach((key, value) -> checkMap(key, value, String.class));

            allAttributes.put(configName, attributes);
          }
        }
      });

      configuration = new Configuration(allProperties, allAttributes);
    }
    return configuration;
  }

  /**
   * Converts {@link Configuration} objects to a collection easily serializable to Json
   * @param configuration the configuration to convert
   * @return the resulting collection
   */
  public static Collection<Map<String, Map<String, ?>>> convertConfigToMap(Configuration configuration) {
    Collection<Map<String, Map<String, ?>>> configurations = new ArrayList<>();
    Set<String> allConfigTypes = Sets.union(configuration.getProperties().keySet(), configuration.getAttributes().keySet());
    for (String configType: allConfigTypes) {
      Map<String, Map<String, ?>> configData = new HashMap<>();
      if (configuration.getProperties().containsKey(configType)) {
        configData.put(PROPERTIES_PROPERTY_ID, configuration.getProperties().get(configType));
      }
      if (configuration.getAttributes().containsKey(configType)) {
        configData.put(PROPERTIES_ATTRIBUTES_PROPERTY_ID, configuration.getAttributes().get(configType));
      }
      configurations.add(ImmutableMap.of(configType, configData));
    }
    return configurations;
  }

  private static void checkFlattenedConfig(Collection<? extends Map<String, ?>> configs) {
    configs.forEach( config -> {
      if ( !config.isEmpty() ) {
        List<String> firstKey = splitConfigurationKey(config.keySet().iterator().next());
        String configType = firstKey.get(0);
        boolean legacyConfig = isKeyInLegacyFormat(firstKey);

        config.keySet().forEach( key -> {
          List<String> keyParts = splitConfigurationKey(key);
          checkArgument(Objects.equals(configType, keyParts.get(0)),
            "Invalid config type: %s. Should be: %s", keyParts.get(0), configType);
          checkArgument(legacyConfig && isKeyInLegacyFormat(keyParts) || !legacyConfig && isKeyInNewFormat(keyParts),
            "Expected key in %s format, found [%s]",
            legacyConfig ? "[config_type/property_name]" :
              "[config_type/properties/config_name] or [config_type/properties_attributes/attribute_name/property_name]",
            key);
        });
      }
    });
  }

  private static void checkMap(String fieldName, Object mapObj, Class<?> valueType) {
    checkArgument(mapObj instanceof Map, "'%s' needs to be a JSON object. Found: %s", fieldName, getClassName(mapObj));
    Map<?, ?> map = (Map<?, ?>)mapObj;
    map.forEach( (__, value) ->
      checkArgument(valueType.isInstance(value),
        "Expected %s as value type, found %s, type: %s",
        valueType.getName(), value, getClassName(value)));
  }

  private static String getClassName(Object object) {
    return null != object ? object.getClass().getName() : null;
  }


  /**
   * Transform attibutes map from <i>attributeName -> propertyName -> value</i> to <i>propertyName -> attributeName -> value</i>
   * or vice versa
   * @param input the input map
   * @return the output map
   */
  public static Map<String, Map<String, String>> transformAttributesMap(Map<String, Map<String, String>> input) {
    return input.entrySet().stream()
      .flatMap(outer -> outer.getValue().entrySet().stream().map(inner -> Triple.of(outer.getKey(), inner.getKey(), inner.getValue())))
      .collect(groupingBy(Triple::getMiddle, toMap(Triple::getLeft, Triple::getRight)));
  }

}
