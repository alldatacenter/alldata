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

/**
 * This class should be used to compare module(service) versions.
 * Base method which should be used is parse(..), This method will validate and parse
 * version which you will pass as parameter, and return object of current class with
 * parsed version. Same thing you should do with another version, with which you are
 * planning to compare previous one. After that, use method compare to get final result.
 */

public class ModuleVersion implements Comparable<ModuleVersion> {

  // RE for different version formats like N.N.N.N-bN, N.N.N-hN-bN
  private static final String VERSION_WITH_HOTFIX_AND_BUILD_PATTERN = "^([0-9]+).([0-9]+).([0-9]+).([0-9]+)-h([0-9]+)-b([0-9]+)";
  private static final String VERSION_WITH_BUILD_PATTERN = "^([0-9]+).([0-9]+).([0-9]+).([0-9]+)-b([0-9]+)";

  // Patterns for previous RE
  private static final Pattern PATTERN_WITH_HOTFIX = Pattern.compile(VERSION_WITH_HOTFIX_AND_BUILD_PATTERN);
  private static final Pattern PATTERN_WITHOUT_HOTFIX = Pattern.compile(VERSION_WITH_BUILD_PATTERN);

  // Parts of version
  private int apacheMajor;
  private int apacheMinor;
  private int internalMinor;
  private int internalMaint;
  private int hotfix;
  private int build;


  public ModuleVersion(int apacheMajor, int apacheMinor, int internalMinor, int internalMaint, int hotfix, int build) {
    this.apacheMajor = apacheMajor;
    this.apacheMinor = apacheMinor;
    this.internalMinor = internalMinor;
    this.internalMaint = internalMaint;
    this.hotfix = hotfix;
    this.build = build;
  }

  /**
   * Method which will parse module version
   * which user passed as parameter. Also
   * in this method version will be validated.
   * @param moduleVersion string
   * @return MpackVersion instance which contains parsed version
   * */
  public static ModuleVersion parse(String moduleVersion) {
    Matcher versionMatcher = validateModuleVersion(moduleVersion);
    ModuleVersion result = null;

    if (versionMatcher.pattern().pattern().equals(VERSION_WITH_HOTFIX_AND_BUILD_PATTERN)) {
      result = new ModuleVersion(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2)),
              Integer.parseInt(versionMatcher.group(3)), Integer.parseInt(versionMatcher.group(4)),
              Integer.parseInt(versionMatcher.group(5)), Integer.parseInt(versionMatcher.group(6)));

    } else {
      result = new ModuleVersion(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2)),
              Integer.parseInt(versionMatcher.group(3)), Integer.parseInt(versionMatcher.group(4)), 0,
              Integer.parseInt(versionMatcher.group(5)));

    }

    return result;
  }

  /**
   * Method validate module version not to be
   * empty or null. Also check if passed version
   * has valid format.
   * @param version string
   * @return Matcher for passed version
   * @throws IllegalArgumentException() if version empty/null/not valid
   */
  private static Matcher validateModuleVersion(String version) {
    if (StringUtils.isEmpty(version)) {
      throw new IllegalArgumentException("Module version can't be empty or null");
    }

    String moduleVersion = StringUtils.trim(version);

    Matcher versionMatcher = PATTERN_WITH_HOTFIX.matcher(moduleVersion);
    if (!versionMatcher.find()) {
      versionMatcher = PATTERN_WITHOUT_HOTFIX.matcher(moduleVersion);
      if (!versionMatcher.find()) {
        throw new IllegalArgumentException("Wrong format for module version, should be N.N.N.N-bN or N.N.N-hN-bN");
      }
    }

    return versionMatcher;
  }

  @Override
  public int compareTo(ModuleVersion other) {
    int result = this.apacheMajor - other.apacheMajor;
    if(result == 0) {
      result = this.apacheMinor - other.apacheMinor;
      if(result == 0) {
        result = this.internalMinor - other.internalMinor;
        if(result == 0) {
          result = this.internalMaint - other.internalMaint;
          if(result == 0) {
            result = this.hotfix - other.hotfix;
            if(result == 0) {
              result = this.build - other.build;
            }
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

    ModuleVersion that = (ModuleVersion) o;

    if (apacheMajor != that.apacheMajor) return false;
    if (apacheMinor != that.apacheMinor) return false;
    if (build != that.build) return false;
    if (hotfix != that.hotfix) return false;
    if (internalMaint != that.internalMaint) return false;
    if (internalMinor != that.internalMinor) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = apacheMajor;
    result = 31 * result + apacheMinor;
    result = 31 * result + internalMinor;
    result = 31 * result + internalMaint;
    result = 31 * result + hotfix;
    result = 31 * result + build;
    return result;
  }
}
