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


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class should be used to compare mpack and stack versions.
 * Base method which should be used is parse/parseStackVersion, depends
 * on which versions you want to compare. This method will validate and parse
 * version which you will pass as parameter, and return object of current class with
 * parsed version. Same thing you should do with another version, with which you are
 * planning to compare previous one. After that, use method compare to get final result.
 */

public class MpackVersion implements Comparable<MpackVersion> {

  // RE for different version formats like N.N.N-hN-bN, N.N.N-bN, N.N.N.N-N
  private final static String VERSION_WITH_HOTFIX_AND_BUILD_PATTERN = "^([0-9]+).([0-9]+).([0-9]+)-h([0-9]+)-b([0-9]+)";
  private final static String VERSION_WITH_BUILD_PATTERN = "^([0-9]+).([0-9]+).([0-9]+)-b([0-9]+)";
  private final static String LEGACY_STACK_VERSION_PATTERN = "^([0-9]+).([0-9]+).([0-9]+).([0-9]+)-([0-9]+)";
  private final static String FORMAT_VERSION_PATTERN = "^([0-9]+)(\\.)?([0-9]+)?(\\.)?([0-9]+)?(-h)?([0-9]+)?(-b)?([0-9]+)?";

  // Patterns for previous RE
  private final static Pattern PATTERN_WITH_HOTFIX = Pattern.compile(VERSION_WITH_HOTFIX_AND_BUILD_PATTERN);
  private final static Pattern PATTERN_LEGACY_STACK_VERSION = Pattern.compile(LEGACY_STACK_VERSION_PATTERN);
  private final static Pattern PATTERN_WITHOUT_HOTFIX = Pattern.compile(VERSION_WITH_BUILD_PATTERN);
  private final static Pattern PATTERN_FORMAT_VERSION = Pattern.compile(FORMAT_VERSION_PATTERN);

  private final static Logger LOG = LoggerFactory.getLogger(MpackVersion.class);

  // Parts of version
  private int major;
  private int minor;
  private int maint;
  private int hotfix;
  private int build;


  public MpackVersion(int major, int minor, int maint, int hotfix, int build) {
    this.major = major;
    this.minor = minor;
    this.maint = maint;
    this.hotfix = hotfix;
    this.build = build;
  }

  /**
   * Method which will parse mpack version
   * which user passed as parameter. Also
   * in this method version will be validated.
   * @param mpackVersion string
   * @return MpackVersion instance which contains parsed version
   * */
  public static MpackVersion parse(String mpackVersion) {
    return parse(mpackVersion, true);
  }

  public static MpackVersion parse(String mpackVersion, boolean strict) {

    if(!strict) {
      mpackVersion = format(mpackVersion);
    }
    Matcher versionMatcher = validateMpackVersion(mpackVersion);
    if (versionMatcher.pattern().pattern().equals(VERSION_WITH_BUILD_PATTERN)) {
      return new MpackVersion(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2)),
              Integer.parseInt(versionMatcher.group(3)), 0, Integer.parseInt(versionMatcher.group(4)));
    } else if (versionMatcher.pattern().pattern().equals(VERSION_WITH_HOTFIX_AND_BUILD_PATTERN)) {
      return new MpackVersion(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2)),
              Integer.parseInt(versionMatcher.group(3)), Integer.parseInt(versionMatcher.group(4)), Integer.parseInt(versionMatcher.group(5)));
    } else {
      throw new IllegalArgumentException("Wrong format for mpack version");
    }
  }

  /**
   * Method to format an mpack version in {major}.{minor}.{maint}-h{hotfix}-b{build} format
   * @param mpackVersion input mpack version string
   * @return formatted mpack version string
   */
  public static String format(String mpackVersion) {
    Matcher m = PATTERN_FORMAT_VERSION.matcher(mpackVersion);
    if(m.matches()) {
      String majorVersion = m.group(1);
      String minorVersion = m.group(3);
      String maintVersion = m.group(5);
      String hotfixNum = m.group(7);
      String buildNum = m.group(9);
      if(hotfixNum != null || buildNum != null) {
        if(minorVersion == null || maintVersion == null) {
          // Both minorVersion and maintVersion should be specified
          throw new IllegalArgumentException("Wrong format for mpack version");
        }
      }
      minorVersion = minorVersion != null? minorVersion: "0";
      maintVersion = maintVersion != null? maintVersion: "0";
      hotfixNum = hotfixNum != null? hotfixNum: "0";
      buildNum = buildNum != null? buildNum: "0";
      String formattedMpackVersion = String.format("%s.%s.%s-h%s-b%s",
        majorVersion, minorVersion, maintVersion, hotfixNum, buildNum);
      return formattedMpackVersion;

    } else {
      throw new IllegalArgumentException("Wrong format for mpack version");
    }
  }

  /**
   * Method which will parse stack version
   * which user passed as parameter. Also
   * in this method version will be validated.
   * @param stackVersion string
   * @return MpackVersion instance which contains parsed version
   * */
  public static MpackVersion parseStackVersion(String stackVersion) {
    Matcher versionMatcher = validateStackVersion(stackVersion);

    if(versionMatcher.pattern().pattern().equals(LEGACY_STACK_VERSION_PATTERN)) {
      return new MpackVersion(Integer.parseInt(versionMatcher.group(1)),
        Integer.parseInt(versionMatcher.group(2)),
        Integer.parseInt(versionMatcher.group(3)), Integer.parseInt(versionMatcher.group(4)),
        Integer.parseInt(versionMatcher.group(5)));
    } else {
      throw new IllegalArgumentException("Wrong format for mpack version");
    }
  }

  /**
   * Method validate stack version not to be
   * empty or null. Also check if passed version
   * has valid format.
   * @param version string
   * @return Matcher for passed version
   * @throws IllegalArgumentException() if version empty/null/not valid
   */
  private static Matcher validateStackVersion(String version) {
    if (StringUtils.isEmpty(version)) {
      throw new IllegalArgumentException("Stack version can't be empty or null");
    }

    String stackVersion = StringUtils.trim(version);

    Matcher versionMatcher = PATTERN_LEGACY_STACK_VERSION.matcher(stackVersion);
    if (!versionMatcher.find()) {
      throw new IllegalArgumentException("Wrong format for stack version, should be N.N.N.N-N or N.N.N-hN-bN");
    }

    return versionMatcher;
  }

  /**
   * Method validate mpack version not to be
   * empty or null. Also check if passed version
   * has valid format.
   * @param version string
   * @return Matcher for passed version
   * @throws IllegalArgumentException() if version empty/null/not valid
   */
  private static Matcher validateMpackVersion(String version) {
    if (StringUtils.isEmpty(version)) {
      throw new IllegalArgumentException("Mpack version can't be empty or null");
    }

    String mpackVersion = StringUtils.trim(version);

    Matcher versionMatcher = PATTERN_WITH_HOTFIX.matcher(mpackVersion);
    if (!versionMatcher.find()) {
      versionMatcher = PATTERN_WITHOUT_HOTFIX.matcher(mpackVersion);
      if (!versionMatcher.find()) {
        throw new IllegalArgumentException("Wrong format for mpack version, should be N.N.N-bN or N.N.N-hN-bN");
      }
    }

    return versionMatcher;
  }

  @Override
  public int compareTo(MpackVersion other) {
    int result = this.major - other.major;
    if(result == 0) {
      result = this.minor - other.minor;
      if(result == 0) {
        result = this.maint - other.maint;
        if(result == 0) {
          result = this.hotfix - other.hotfix;
          if(result == 0) {
            result = this.build - other.build;
          }
        }
      }
    }
    return result > 0 ? 1 : result < 0 ? -1 : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MpackVersion that = (MpackVersion) o;

    if (build != that.build) return false;
    if (hotfix != that.hotfix) return false;
    if (maint != that.maint) return false;
    if (major != that.major) return false;
    if (minor != that.minor) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = major;
    result = 31 * result + minor;
    result = 31 * result + maint;
    result = 31 * result + hotfix;
    result = 31 * result + build;
    return result;
  }
}
