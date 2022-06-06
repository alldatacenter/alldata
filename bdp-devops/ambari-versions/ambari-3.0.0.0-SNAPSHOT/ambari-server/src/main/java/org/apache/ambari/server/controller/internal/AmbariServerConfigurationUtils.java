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

import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.configuration.ConfigurationPropertyType;

/**
 * Provides useful utility methods for AMBARI-level configuration related tasks.
 */
public class AmbariServerConfigurationUtils {

  /**
   * Returns the relevant {@link AmbariServerConfigurationKey}
   *
   * @param category     the name of the category
   * @param propertyName the name of the property
   * @return the {@link AmbariServerConfigurationKey representing the given category/property if such category/property
   * exists; {@code null} otherwise
   */
  public static AmbariServerConfigurationKey getConfigurationKey(String category, String propertyName) {
    return getConfigurationKey(AmbariServerConfigurationCategory.translate(category), propertyName);
  }

  /**
   * Returns the relevant {@link AmbariServerConfigurationKey}
   *
   * @param category     the {@link AmbariServerConfigurationCategory}
   * @param propertyName the name of the property
   * @return the {@link AmbariServerConfigurationKey representing the given category/property if such category/property
   * exists; {@code null} otherwise
   */
  public static AmbariServerConfigurationKey getConfigurationKey(AmbariServerConfigurationCategory category, String propertyName) {
    return AmbariServerConfigurationKey.translate(category, propertyName);
  }

  /**
   * Returns the {@link ConfigurationPropertyType} for the specified Ambari Server configuration property
   *
   * @param category     the name of the category
   * @param propertyName the name of the property
   * @return the type of the given category/property if such category/property
   * exists; {@link ConfigurationPropertyType#UNKNOWN} otherwise
   */
  public static ConfigurationPropertyType getConfigurationPropertyType(String category, String propertyName) {
    return getConfigurationPropertyType(getConfigurationKey(category, propertyName));
  }

  /**
   * Returns the {@link ConfigurationPropertyType} for the specified Ambari Server configuration property
   *
   * @param category     the category
   * @param propertyName the name of the property
   * @return the type of the given category/property if such category/property
   * exists; {@link ConfigurationPropertyType#UNKNOWN} otherwise
   */
  public static ConfigurationPropertyType getConfigurationPropertyType(AmbariServerConfigurationCategory category, String propertyName) {
    return getConfigurationPropertyType(getConfigurationKey(category, propertyName));
  }

  /**
   * Returns the {@link ConfigurationPropertyType} for the specified Ambari Server configuration property
   *
   * @param configurationKey a {@link AmbariServerConfigurationKey}
   * @return the type of the given category/property if such category/property
   * exists; {@link ConfigurationPropertyType#UNKNOWN} otherwise
   */
  private static ConfigurationPropertyType getConfigurationPropertyType(AmbariServerConfigurationKey configurationKey) {
    return (configurationKey == null) ? ConfigurationPropertyType.UNKNOWN : configurationKey.getConfigurationPropertyType();
  }

  /**
   * @param category     the name of the category
   * @param propertyName the name of the property
   * @return the String representation of the type if such category/property
   * exists; {@code null} otherwise
   */
  public static String getConfigurationPropertyTypeName(AmbariServerConfigurationCategory category, String propertyName) {
    final ConfigurationPropertyType configurationPropertyType = getConfigurationPropertyType(category, propertyName);
    return configurationPropertyType == null ? null : configurationPropertyType.name();
  }

  /**
   * @param category     the name of the category
   * @param propertyName the name of the property
   * @return the String representation of the type if such category/property
   * exists; {@code null} otherwise
   */
  public static String getConfigurationPropertyTypeName(String category, String propertyName) {
    final ConfigurationPropertyType configurationPropertyType = getConfigurationPropertyType(category, propertyName);
    return configurationPropertyType == null ? null : configurationPropertyType.name();
  }

  /**
   * Indicates whether the given property's type is a {@link ConfigurationPropertyType#PASSWORD}
   *
   * @param category     the name of the category
   * @param propertyName the name of the property
   * @return {@code true} in case the given property's type is
   * {@link ConfigurationPropertyType#PASSWORD}; {@code false} otherwise
   */
  public static boolean isPassword(String category, String propertyName) {
    return isPassword(getConfigurationKey(category, propertyName));
  }

  /**
   * Indicates whether the given property's type is a {@link ConfigurationPropertyType#PASSWORD}
   *
   * @param category     the name of the category
   * @param propertyName the name of the property
   * @return {@code true} in case the given property's type is
   * {@link ConfigurationPropertyType#PASSWORD}; {@code false} otherwise
   */
  public static boolean isPassword(AmbariServerConfigurationCategory category, String propertyName) {
    return isPassword(getConfigurationKey(category, propertyName));
  }

  /**
   * Indicates whether the given property's type is a {@link ConfigurationPropertyType#PASSWORD}
   *
   * @param configurationKey the Ambari Server configiration key
   * @return {@code true} in case the given property's type is
   * {@link ConfigurationPropertyType#PASSWORD}; {@code false} otherwise
   */
  public static boolean isPassword(AmbariServerConfigurationKey configurationKey) {
    return ConfigurationPropertyType.PASSWORD.equals(getConfigurationPropertyType(configurationKey));
  }
}
