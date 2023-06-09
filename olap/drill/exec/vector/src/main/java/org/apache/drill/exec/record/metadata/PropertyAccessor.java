/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.record.metadata;

/**
 * Utilities to get/set typed values within a propertied object
 */
public class PropertyAccessor {

  private PropertyAccessor() { }

  public static String getString(Propertied props, String key, String defaultValue) {
    final String value = props.property(key);
    return value == null ? defaultValue : value;
  }

  public static int getInt(Propertied props, String key, int defaultValue) {
    final String value = props.property(key);
    try {
      return value == null ? defaultValue : Integer.parseInt(value);
    } catch (final NumberFormatException e) {
      throw new IllegalStateException(String.format(
          "Invalid int property %s: %s", key, value), e);
    }
  }

  public static int getInt(Propertied props, String key) {
    return getInt(props, key, 0);
  }

  public static boolean getBoolean(Propertied props, String key, boolean defaultValue) {
    final String value = props.property(key);
    return value == null ? defaultValue : Boolean.parseBoolean(value);
  }

  public static boolean getBoolean(Propertied props, String key) {
    return getBoolean(props, key, false);
  }

  public static void set(Propertied props, String key, int value) {
    props.setProperty(key, Integer.toString(value));
  }

  public static void set(Propertied props, String key, boolean value) {
    props.setProperty(key, Boolean.toString(value));
  }
}
