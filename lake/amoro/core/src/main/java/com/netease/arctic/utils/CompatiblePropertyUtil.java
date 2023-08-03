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

package com.netease.arctic.utils;

import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.util.PropertyUtil;

import java.util.Map;

/**
 * PropertyUtil compatible with legacy properties
 */
public class CompatiblePropertyUtil {

  private CompatiblePropertyUtil() {
  }

  public static boolean propertyAsBoolean(Map<String, String> properties,
                                          String property, boolean defaultValue) {
    return PropertyUtil.propertyAsBoolean(properties, getCompatibleProperty(properties, property), defaultValue);
  }

  public static double propertyAsDouble(Map<String, String> properties,
                                        String property, double defaultValue) {
    return PropertyUtil.propertyAsDouble(properties, getCompatibleProperty(properties, property), defaultValue);
  }

  public static int propertyAsInt(Map<String, String> properties,
                                  String property, int defaultValue) {
    return PropertyUtil.propertyAsInt(properties, getCompatibleProperty(properties, property), defaultValue);
  }

  public static long propertyAsLong(Map<String, String> properties,
                                    String property, long defaultValue) {
    return PropertyUtil.propertyAsLong(properties, getCompatibleProperty(properties, property), defaultValue);
  }

  public static String propertyAsString(Map<String, String> properties,
                                        String property, String defaultValue) {
    return PropertyUtil.propertyAsString(properties, getCompatibleProperty(properties, property), defaultValue);
  }

  private static String getCompatibleProperty(Map<String, String> properties, String property) {
    String legacyProperty = getLegacyProperty(property);
    if (legacyProperty != null && properties.containsKey(legacyProperty) && !properties.containsKey(property)) {
      return legacyProperty;
    } else {
      return property;
    }
  }

  private static String getLegacyProperty(String property) {
    if (property == null) {
      return null;
    }
    switch (property) {
      case TableProperties.ENABLE_SELF_OPTIMIZING:
        return TableProperties.ENABLE_OPTIMIZE;
      case TableProperties.SELF_OPTIMIZING_GROUP:
        return TableProperties.OPTIMIZE_GROUP;
      case TableProperties.SELF_OPTIMIZING_QUOTA:
        return TableProperties.OPTIMIZE_QUOTA;
      case TableProperties.SELF_OPTIMIZING_EXECUTE_RETRY_NUMBER:
        return TableProperties.OPTIMIZE_RETRY_NUMBER;
      case TableProperties.SELF_OPTIMIZING_MAX_FILE_CNT:
        return TableProperties.OPTIMIZE_MAX_FILE_COUNT;
      case TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT:
        return TableProperties.MINOR_OPTIMIZE_TRIGGER_DELETE_FILE_COUNT;
      case TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_INTERVAL:
        return TableProperties.MINOR_OPTIMIZE_TRIGGER_MAX_INTERVAL;
      case TableProperties.SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL:
        return TableProperties.FULL_OPTIMIZE_TRIGGER_MAX_INTERVAL;
      case TableProperties.ENABLE_TABLE_EXPIRE:
        return TableProperties.ENABLE_TABLE_EXPIRE_LEGACY;
      case TableProperties.ENABLE_ORPHAN_CLEAN:
        return TableProperties.ENABLE_ORPHAN_CLEAN_LEGACY;
      case TableProperties.ENABLE_LOG_STORE:
        return TableProperties.ENABLE_LOG_STORE_LEGACY;
      default:
        return null;
    }
  }
}
