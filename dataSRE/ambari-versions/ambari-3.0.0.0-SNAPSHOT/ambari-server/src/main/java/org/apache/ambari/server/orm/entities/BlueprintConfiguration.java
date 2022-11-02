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

package org.apache.ambari.server.orm.entities;

/**
 * Blueprint configuration.
 */
public interface BlueprintConfiguration {
  /**
   * Set the configuration type.
   *
   * @param type configuration type
   */
  void setType(String type);

  /**
   * Get the configuration type.
   *
   * @return configuration type
   */
  String getType();

  /**
   * Set the blueprint name.
   *
   * @param blueprintName  blueprint name
   */
  void setBlueprintName(String blueprintName);

  /**
   * Get the blueprint name.
   *
   * @return blueprint name
   */
  String getBlueprintName();

  /**
   * Set the configuration properties.
   * Data must be a map of configuration properties in
   * json format.
   *
   * @param configData json representation of property map
   */
  void setConfigData(String configData);

  /**
   * Get the configuration properties.
   *
   * @return json representation of property map
   */
  String getConfigData();

  /**
   * Get the configuration attributes.
   *
   * @return json representation of attributes map
   */
  String getConfigAttributes();

  /**
   * Set the configuration attributes.
   * Data must be a map of configuration attributes in
   * json format.
   *
   * @param configAttributes json representation of attributes map
   */
  void setConfigAttributes(String configAttributes);
}
