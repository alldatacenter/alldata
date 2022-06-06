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

/**
 * Used when formulating manifest info for API consumption.
 */
public class ManifestServiceInfo {

  @JsonProperty("name")
  String m_name;

  @JsonProperty("display_name")
  String m_display;

  @JsonProperty("comment")
  String m_comment;

  @JsonProperty("versions")
  Set<String> m_versions;

  /**
   * @param name      the service name
   * @param display   the display name
   * @param comment   the comment for the service info
   * @param versions  the set of strings for the service versions
   */
  public ManifestServiceInfo(String name, String display, String comment, Set<String> versions) {
    m_name = name;
    m_display = display;
    m_comment = comment;
    m_versions = versions;
  }


}
