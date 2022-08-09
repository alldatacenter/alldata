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

import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * Represents a version of a {@link ManifestService} that is available for upgrading.
 *
 * This class is serialized directly in API responses.
 */
public class AvailableVersion {

  @JsonProperty("version")
  private String version;

  @JsonProperty("release_version")
  @JsonSerialize(include=Inclusion.NON_NULL)
  private String releaseVersion;

  @JsonProperty("version_id")
  @JsonSerialize(include=Inclusion.NON_NULL)
  private String versionId;

  @JsonProperty
  private Set<Component> components;

  AvailableVersion(String version, String versionId, String releaseVersion, Set<Component> components) {
    this.version = version;
    this.versionId = versionId;
    this.releaseVersion = releaseVersion;
    this.components = components;
  }

  /**
   * @return the binary version of the available service.
   */
  public String getVersion() {
    return version;
  }

  /**
   * @return the release version of the available service.
   */
  public String getReleaseVersion() {
    return releaseVersion;
  }

  static class Component {
    @JsonProperty("name")
    private String name;

    @JsonProperty("display_name")
    private String display;

    Component(String name, String display) {
      this.name = name;
      this.display = display;
    }
  }

}
