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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.gson.annotations.SerializedName;

/**
 * Used to hold information about Service's ability to upgrade for a repository version.
 */
public class ServiceVersionSummary {

  @SerializedName("version")
  @JsonProperty("version")
  private String m_version;

  @SerializedName("release_version")
  @JsonProperty("release_version")
  private String m_releaseVersion;

  @SerializedName("upgrade")
  @JsonProperty("upgrade")
  private boolean m_upgrade = false;

  ServiceVersionSummary() {
  }

  /**
   * Sets the version information
   *
   * @param binaryVersion   the binary version of the service
   * @param releaseVersion  the release version of the service
   */
  void setVersions(String binaryVersion, String releaseVersion) {
    m_version = binaryVersion;
    m_releaseVersion = releaseVersion;
  }

  @JsonIgnore
  /**
   * @return {@code true} if the service will be included in an upgrade
   */
  public boolean isUpgrade() {
    return m_upgrade;
  }

  /**
   * @param upgrade {@code true} if the service will be included in an upgrade
   */
  public void setUpgrade(boolean upgrade) {
    m_upgrade = upgrade;
  }

  /**
   * @return the relase version field
   */
  @JsonIgnore
  public String getReleaseVersion() {
    return m_releaseVersion;
  }


}
