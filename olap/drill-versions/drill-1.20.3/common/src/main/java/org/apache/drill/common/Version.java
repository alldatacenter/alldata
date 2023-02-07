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
package org.apache.drill.common;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ComparisonChain;

/**
 * Encapsulates version information and provides ordering
 *
 */
public final class Version implements Comparable<Version> {
  private final String version;
  private final int major;
  private final int minor;
  private final int patch;
  private final int buildNumber;
  private final String qualifier;
  private final String lcQualifier; // lower-case qualifier for comparison

  public Version(String version, int major, int minor, int patch, int buildNumber,
      String qualifier) {
    this.version = version;
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.buildNumber = buildNumber;
    this.qualifier = qualifier;
    this.lcQualifier = qualifier.toLowerCase(Locale.ENGLISH);
  }

  /**
   * Get the version string
   *
   * @return the version number as x.y.z
   */
  public String getVersion() {
    return version;
  }

  /**
   * Get the major version
   *
   * @return x if assuming the version number is x.y.z
   */
  public int getMajorVersion() {
    return major;
  }

  /**
   * Get the minor version
   *
   * @return y if assuming the version number is x.y.z
   */
  public int getMinorVersion() {
    return minor;
  }

  /**
   * Get the patch version
   *
   * @return z if assuming the version number is x.y.z(-suffix)
   */
  public int getPatchVersion() {
    return patch;
  }

  /**
   * Get the build number
   *
   * @return b if assuming the version number is x.y.z(.b)(-suffix)
   */
  public int getBuildNumber() {
    return buildNumber;
  }

  /**
   * Get the version qualifier
   *
   * @return b if assuming the version number is x.y.z(.b)(-suffix)
   */
  public String getQualifier() {
    return qualifier;
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch, buildNumber, lcQualifier);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Version)) {
      return false;
    }
    Version dvi = (Version) obj;
    return this.major == dvi.major
        && this.minor == dvi.minor
        && this.patch == dvi.patch
        && this.buildNumber == dvi.buildNumber
        && Objects.equals(this.lcQualifier, dvi.lcQualifier);
  }

  @Override
  public String toString() {
    return String.format("Version; %s", version);
  }

  private static final Comparator<String> QUALIFIER_COMPARATOR = new Comparator<String>() {
    @Override public int compare(String q1, String q2) {
      if (q1.equals(q2)) {
        return 0;
      }

      if ("snapshot".equals(q1)) {
        return -1;
      }

      if ("snapshot".equals(q2)) {
        return 1;
      }

      return q1.compareTo(q2);
    }
  };

  @Override
  public int compareTo(Version o) {
    Preconditions.checkNotNull(o);
    return ComparisonChain.start()
        .compare(this.major, o.major)
        .compare(this.minor, o.minor)
        .compare(this.patch, o.patch)
        .compare(this.buildNumber, o.buildNumber)
        .compare(this.lcQualifier, o.lcQualifier, QUALIFIER_COMPARATOR)
        .result();
  }
}
