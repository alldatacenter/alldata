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
package org.apache.ambari.server.state.repository;

import java.util.Comparator;

import org.apache.ambari.server.utils.VersionUtils;
import org.apache.ambari.spi.stack.StackReleaseInfo;
import org.apache.ambari.spi.stack.StackReleaseVersion;
import org.apache.commons.lang.StringUtils;

/**
 * This is the default implementation if no stack provides an implementation.
 * Before this mechanism, Ambari assumed the version was in the format X.X.X.X-YYYY
 */
public class DefaultStackVersion implements StackReleaseVersion {

  @Override
  public String getFullVersion(StackReleaseInfo info) {

    StringBuilder sb = new StringBuilder(info.getVersion());

    if (StringUtils.isNotBlank(info.getBuild())) {
      sb.append('-').append(StringUtils.trim(info.getBuild()));
    }

    return sb.toString();
  }

  @Override
  public Comparator<StackReleaseInfo> getComparator() {

    return new Comparator<StackReleaseInfo>() {
      @Override
      public int compare(StackReleaseInfo o1, StackReleaseInfo o2) {
        return VersionUtils.compareVersionsWithBuild(
            getFullVersion(o1), getFullVersion(o2), 4);
      }
    };
  }

  @Override
  public StackReleaseInfo parse(String versionString) {
    String version = "0";
    String build = "0";

    String[] parts = StringUtils.split(versionString, '-');
    if (1 == parts.length) {
      version = parts[0];
    } else if (parts.length > 1) {
      version = parts[0];
      build = parts[1];
    }

    return new StackReleaseInfo(version, "0", build);
  }


}
