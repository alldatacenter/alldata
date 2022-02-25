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
package org.apache.ambari.server.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * Provides various utility functions to be used for version handling.
 * The compatibility matrix between server, agent, store can also be maintained
 * in this class. Currently, exact match is required between all the three.
 */
public class VersionUtils {
  public static final String DEV_VERSION = "${ambariVersion}";

  /**
   * Compares two versions strings of the form N.N.N.N or even N.N.N.N-###
   * (which should ignore everything after the dash). If the user has a custom
   * stack, e.g., 2.3.MYNAME or MYNAME.2.3, then any segment that contains
   * letters should be ignored.
   *
   * @param version1
   *          the first operand. If set to {@value #DEV_VERSION}
   *          then this will always return {@code 0)}
   * @param version2
   *          the second operand.
   * @param maxLengthToCompare
   *          The maximum length to compare - 2 means only Major and Minor 0 to
   *          compare the whole version strings
   * @return 0 if both are equal up to the length compared, -1 if first one is
   *         lower, +1 otherwise
   */
  public static int compareVersions(String version1, String version2, int maxLengthToCompare)
    throws IllegalArgumentException {
    if (version1 == null){
      throw new IllegalArgumentException("version1 cannot be null");
    }

    if (version2 == null){
      throw new IllegalArgumentException("version2 cannot be null");
    }

    version1 = StringUtils.trim(version1);
    version2 = StringUtils.trim(version2);

    if (version1.indexOf('-') >=0) {
      version1 = version1.substring(0, version1.indexOf('-'));
    }
    if (version2.indexOf('-') >=0) {
      version2 = version2.substring(0, version2.indexOf('-'));
    }

    if (version1.isEmpty()) {
      throw new IllegalArgumentException("version1 cannot be empty");
    }
    if (version2.isEmpty()) {
      throw new IllegalArgumentException("version2 cannot be empty");
    }
    if (maxLengthToCompare < 0) {
      throw new IllegalArgumentException("maxLengthToCompare cannot be less than 0");
    }

    if (DEV_VERSION.equals(version1.trim())) {
      return 0;
    }

    //String pattern = "^([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.([0-9]+).*";
    String pattern = "([0-9]+).([0-9]+).([0-9]+).?([0-9]+)?.*";
    String[] version1Parts = version1.replaceAll(pattern, "$1.$2.$3.$4").split("\\.");
    String[] version2Parts = version2.replaceAll(pattern, "$1.$2.$3.$4").split("\\.");

    int length = Math.max(version1Parts.length, version2Parts.length);
    length = maxLengthToCompare == 0 || maxLengthToCompare > length ? length : maxLengthToCompare;

    List<Integer> stack1Parts = new ArrayList<>();
    List<Integer> stack2Parts = new ArrayList<>();

    for (int i = 0; i < length; i++) {
      // Robust enough to handle strings in the version
      try {
        int stack1Part = i < version1Parts.length ?
            Integer.parseInt(version1Parts[i]) : 0;
        stack1Parts.add(stack1Part);
      } catch (NumberFormatException e) {
        stack1Parts.add(0);
      }
      try {
        int stack2Part = i < version2Parts.length ?
            Integer.parseInt(version2Parts[i]) : 0;
        stack2Parts.add(stack2Part);
      } catch (NumberFormatException e) {
        stack2Parts.add(0);
      }
    }

    length = Math.max(stack1Parts.size(), stack2Parts.size());
    for (int i = 0; i < length; i++) {
      Integer stack1Part = stack1Parts.get(i);
      Integer stack2Part = stack2Parts.get(i);

      if (stack1Part < stack2Part) {
        return -1;
      }
      if (stack1Part > stack2Part) {
        return 1;
      }
    }

    return 0;
  }

  /**
   * Compares two versions strings of the form N.N.N.N
   *
   * @param version1
   *          the first operand. If set to {@value #DEV_VERSION}
   *          then this will always return {@code 0)}
   * @param version2
   *          the second operand.
   * @param allowEmptyVersions
   *          Allow one or both version values to be null or empty string
   * @return 0 if both are equal up to the length compared, -1 if first one is
   *         lower, +1 otherwise
   */
  public static int compareVersions(String version1, String version2, boolean allowEmptyVersions) {
    if (allowEmptyVersions) {
      if (version1 != null && version1.equals(DEV_VERSION)) {
        return 0;
      }
      if (version1 == null && version2 == null) {
        return 0;
      } else {
        if (version1 == null) {
          return -1;
        }
        if (version2 == null) {
          return 1;
        }
      }

      if (version1.isEmpty() && version2.isEmpty()) {
        return 0;
      } else {
        if (version1.isEmpty()) {
          return -1;
        }
        if (version2.isEmpty()) {
          return 1;
        }
      }
    }

    return compareVersions(version1, version2, 0);
  }

  /**
   * Compares two versions strings of the form N.N.N.N
   *
   * @param version1
   *          the first operand. If set to {@value #DEV_VERSION}
   *          then this will always return {@code 0)}
   * @param version2
   *          the second operand.
   * @return 0 if both are equal, -1 if first one is lower, +1 otherwise
   */
  public static int compareVersions(String version1, String version2) {
    return compareVersions(version1, version2, 0);
  }

  /**
   * Compares two version for equality, allows empty versions
   *
   * @param version1
   *          the first operand. If set to {@value #DEV_VERSION}
   *          then this will always return {@code 0)}
   * @param version2
   *          the second operand.
   * @param allowEmptyVersions
   *          Allow one or both version values to be null or empty string
   * @return true if versions are equal; false otherwise
   */
  public static boolean areVersionsEqual(String version1, String version2, boolean allowEmptyVersions) {
    return 0 == compareVersions(version1, version2, allowEmptyVersions);
  }

  /**
   * Return N.N.N from N.N.N.xyz
   *
   * @param version
   *          the version to extract the first three sections from.
   * @return the first three sections of the specified version.
   */
  public static String getVersionSubstring(String version) {
    String[] versionParts = version.split("\\.");
    if (versionParts.length < 3) {
      throw  new IllegalArgumentException("Invalid version number");
    }

    return versionParts[0] + "." + versionParts[1] + "." + versionParts[2];
  }

  /**
   * Compares versions, using a build number using a dash separator, if one
   * exists. This is is useful when comparing repository versions with one
   * another that include build number
   *
   * @param version1
   *          the first version
   * @param version2
   *          the second version
   * @param places
   *          the number of decimal-separated places to compare
   * @return {@code -1} if {@code version1} is less than {@code version2},
   *         {@code 1} if it is greater, and {@code 0} if they are equal.
   */
  public static int compareVersionsWithBuild(String version1, String version2, int places) {
    version1 = (null == version1) ? "0" : version1;
    version2 = (null == version2) ? "0" : version2;

    // check _exact_ equality
    if (StringUtils.equals(version1, version2)) {
      return 0;
    }

    int compare = VersionUtils.compareVersions(version1, version2, places);
    if (0 != compare) {
      return compare;
    }

    int v1 = 0;
    int v2 = 0;
    if (version1.indexOf('-') > -1) {
      v1 = NumberUtils.toInt(version1.substring(version1.indexOf('-')), 0);
    }

    if (version2.indexOf('-') > -1) {
      v2 = NumberUtils.toInt(version2.substring(version2.indexOf('-')), 0);
    }

    compare = v2 - v1;

    return Integer.compare(compare, 0);
  }

  /**
   * Helper function to compare two comparable versions with null checks
   * 
   * @param v1
   *          The first version
   * @param v2
   *          The second version
   * @return {@code -1} if {@code v1} is less than {@code v2}, {@code 1} if it
   *         is greater, and {@code 0} if they are equal.
   */
  public static int compareTo(Comparable v1, Comparable v2) {
    return v1 == null ? (v2 == null ? 0 : -1) : v2 == null ? 1 : v1.compareTo(v2);
  }
}
