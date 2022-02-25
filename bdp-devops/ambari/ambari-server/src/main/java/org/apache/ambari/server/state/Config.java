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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single instance of a 'Config Type'
 */
public interface Config {
  Map<PropertyInfo.PropertyType, Set<String>> getPropertiesTypes();

  void setPropertiesTypes(Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes);

  /**
   * @return Config Type
   */
  String getType();

  /**
   * @return Version Tag this config instance is mapped to
   */
  String getTag();

  /**
   * Gets the stack that this configuration belongs to.
   *
   * @return the stack (not {@code null).
   */
  StackId getStackId();

  /**
   *
   * @return version of config by type
   */
  Long getVersion();

  /**
   * @return Properties that define this config instance
   */
  Map<String, String> getProperties();

  /**
   * @return Map of attributes in this config-type to value per property
   */
  Map<String, Map<String, String>> getPropertiesAttributes();

  /**
   * Replace properties with new provided set
   * @param properties Property Map to replace existing one
   */
  void setProperties(Map<String, String> properties);

  /**
   * Replace property attributes with new provided set
   * @param propertiesAttributes Property Attributes Map to replace existing one
   */
  void setPropertiesAttributes(Map<String, Map<String, String>> propertiesAttributes);

  /**
   * Update provided properties' values.
   * @param properties Property Map with updated values
   */
  void updateProperties(Map<String, String> properties);

  /**
   * Ger service config versions containing this config
   * @return
   */
  List<Long> getServiceConfigVersions();

  /**
   * Delete certain properties
   * @param properties Property keys to be deleted
   */
  void deleteProperties(List<String> properties);

  /**
   * Persist the configuration.
   */
  void save();

  /**
   * @return the cluster where this config belongs to
   */
  Cluster getCluster();
}
