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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * A representation of a {@link ManifestService} that is available for upgrading.  This
 * object will be serialized directly in API responses.
 */
public class AvailableService {

  @JsonProperty("name")
  private String name;

  @JsonProperty("display_name")
  @JsonSerialize(include=Inclusion.NON_NULL)
  private String displayName;

  private List<AvailableVersion> versions = new ArrayList<>();

  AvailableService(String service, String serviceDisplay) {
    name = service;
    displayName = serviceDisplay;
  }

  /**
   * @return the service name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the list of versions to append additional versions.
   */
  public List<AvailableVersion> getVersions() {
    return versions;
  }

}
