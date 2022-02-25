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
package org.apache.ambari.spi.stack;

/**
 * Provides information about a version's release.  Ambari provides instances
 * for consumption.
 */
public class StackReleaseInfo {

  private String m_version;
  private String m_hotfix;
  private String m_build;
 
  /**
   * @param version
   *          the version string
   * @param hotfix
   *          the hotfix string
   * @param build
   *          the build string
   */
  public StackReleaseInfo(String version, String hotfix, String build) {
    m_version = version;
    m_hotfix = hotfix;
    m_build = build;
  }
  
  /**
   * @return the version string
   */
  public String getVersion() {
    return m_version;
  }

  /**
   * @return the hotfix string
   */
  public String getHotfix() {
    return m_hotfix;
  }
  
  /**
   * @return the build string
   */
  public String getBuild() {
    return m_build;
  }

}
