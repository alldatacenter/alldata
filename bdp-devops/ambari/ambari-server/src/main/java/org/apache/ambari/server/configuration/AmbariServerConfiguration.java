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
package org.apache.ambari.server.configuration;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AmbariServerConfiguration is an abstract class to be extended by Ambari server configuration classes
 * that encapsulate difference configuration categories such as 'ldap-configuration' and 'sso-configuration'.
 *
 * @see AmbariServerConfigurationCategory
 * @see AmbariServerConfigurationKey
 */
public abstract class AmbariServerConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerConfiguration.class);
  
  protected final Map<String, String> configurationMap = new HashMap<>();

  protected AmbariServerConfiguration(Map<String, String> configurationMap) {
    if (configurationMap != null) {
      this.configurationMap.putAll(configurationMap);
    }
  }

  /**
   * Gets the configuration value for given {@link AmbariServerConfigurationKey}.
   * <p>
   * If no value is found in the supplied configuration map, the default value will be returned.
   *
   * @param ambariServerConfigurationKey the {@link AmbariServerConfigurationKey} for which to retrieve a value
   * @param configurationMap             a map of configuration property names to values
   * @return the requested configuration value
   */
  protected String getValue(AmbariServerConfigurationKey ambariServerConfigurationKey, Map<String, String> configurationMap) {
    return getValue(ambariServerConfigurationKey.key(), configurationMap, ambariServerConfigurationKey.getDefaultValue());
  }

  /**
   * Gets the configuration value for given {@link AmbariServerConfigurationKey}.
   * <p>
   * If no value is found in the supplied configuration map, the default value will be returned.
   *
   * @param propertyName     the property name for which to retrieve a value
   * @param configurationMap a map of configuration property names to values
   * @param defaultValue     the default value to use in the event the propery was not set in the configuration map
   * @return the requested configuration value
   */
  protected String getValue(String propertyName, Map<String, String> configurationMap, String defaultValue) {
    if ((configurationMap != null) && configurationMap.containsKey(propertyName)) {
      return configurationMap.get(propertyName);
    } else {
      LOGGER.debug("Ambari server configuration property [{}] hasn't been set; using default value", propertyName);
      return defaultValue;
    }
  }
  
  /**
   * @return this configuration represented as a map
   */
  public Map<String, String> toMap() {
    return new HashMap<>(configurationMap);
  }

  /**
   * Sets the given value for the given configuration
   * 
   * @param configName
   *          the name of the configuration to set the value for
   * @param value
   *          the new value
   *
   * @throws IllegalArgumentException
   *           in case the supplied configuration does not belong to the
   *           configuration category of this instance
   */
  public void setValueFor(String configName, String value) {
    AmbariServerConfigurationKey ambariServerConfigurationKey = AmbariServerConfigurationKey.translate(getCategory(), configName);
    if (ambariServerConfigurationKey != null) {
      setValueFor(ambariServerConfigurationKey, value);
    }
  }

  /**
   * Sets the given value for the given configuration
   * 
   * @param ambariServerConfigurationKey
   *          the configuration key to set the value for
   * @param value
   *          the new value
   *
   * @throws IllegalArgumentException
   *           in case the supplied configuration does not belong to the
   *           configuration category of this instance
   */
  public void setValueFor(AmbariServerConfigurationKey ambariServerConfigurationKey, String value) {
    if (ambariServerConfigurationKey.getConfigurationCategory() != getCategory()) {
      throw new IllegalArgumentException(ambariServerConfigurationKey.key() + " is not a valid " + getCategory().getCategoryName());
    }
    configurationMap.put(ambariServerConfigurationKey.key(), value);
  }
  
  protected abstract AmbariServerConfigurationCategory getCategory();

}
