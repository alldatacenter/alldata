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
package org.apache.drill.exec.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.regex.Pattern;

/**
 * A collection of utility methods to retrieve and parse the values of Java system properties.
 *
 * This is a modified version of Netty's internal system property utility class.
 */
public final class SystemPropertyUtil {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SystemPropertyUtil.class);

  private static final Pattern INTEGER_PATTERN = Pattern.compile("-?[0-9]+");

  /**
   * Returns {@code true} if and only if the system property with the specified {@code key}
   * exists.
   */
  public static boolean contains(String key) {
    return get(key) != null;
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to {@code null} if the property access fails.
   *
   * @return the property value or {@code null}
   */
  public static String get(String key) {
    return get(key, null);
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to the specified default value if
   * the property access fails.
   *
   * @return the property value.
   *         {@code def} if there's no such property or if an access to the
   *         specified property is not allowed.
   */
  public static String get(final String key, String def) {
    if (key == null) {
      throw new NullPointerException("key");
    }
    if (key.isEmpty()) {
      throw new IllegalArgumentException("key must not be empty.");
    }

    String value = null;
    try {
      if (System.getSecurityManager() == null) {
        value = System.getProperty(key);
      } else {
        value = AccessController.doPrivileged(new PrivilegedAction<String>() {
          @Override
          public String run() {
            return System.getProperty(key);
          }
        });
      }
    } catch (Exception e) {
        logger.warn("Unable to retrieve a system property '" + key + "'; default values will be used.", e);
    }

    if (value == null) {
      return def;
    }

    return value;
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to the specified default value if
   * the property access fails.
   *
   * @return the property value.
   *         {@code def} if there's no such property or if an access to the
   *         specified property is not allowed.
   */
  public static boolean getBoolean(String key, boolean def) {
    String value = get(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase();
    if (value.isEmpty()) {
      return true;
    }

    if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
      return true;
    }

    if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
      return false;
    }

    logger.warn("Unable to parse the boolean system property '{}':{} - using the default value: {}",
        key, value, def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to the specified default value if
   * the property access fails.
   *
   * @return the property value.
   *         {@code def} if there's no such property or if an access to the
   *         specified property is not allowed.
   */
  public static int getInt(String key, int def) {
    String value = get(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase();
    if (INTEGER_PATTERN.matcher(value).matches()) {
      try {
        return Integer.parseInt(value);
      } catch (Exception e) {
        // Ignore
      }
    }

    logger.warn("Unable to parse the integer system property '{}':{} - using the default value: {}",
        key, value, def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified
   * {@code key}, while falling back to the specified default value if
   * the property access fails.
   *
   * @return the property value.
   *         {@code def} if there's no such property or if an access to the
   *         specified property is not allowed.
   */
  public static long getLong(String key, long def) {
    String value = get(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase();
    if (INTEGER_PATTERN.matcher(value).matches()) {
      try {
        return Long.parseLong(value);
      } catch (Exception e) {
        // Ignore
      }
    }

    logger.warn("Unable to parse the long integer system property '{}':{} - using the default value: {}",
        key, value, def);

    return def;
  }

  // prevent instantiation
  private SystemPropertyUtil() {
  }
}
