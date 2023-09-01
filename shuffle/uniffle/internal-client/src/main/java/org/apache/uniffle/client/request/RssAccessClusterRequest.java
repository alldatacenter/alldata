/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.client.request;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class RssAccessClusterRequest {

  private final String accessId;
  private final Set<String> tags;
  private final int timeoutMs;
  private final String user;
  /**
   * The map is to pass the extra data to the coordinator and to
   * extend more pluggable {@code AccessCheckers} easily.
   */
  private final Map<String, String> extraProperties;

  public RssAccessClusterRequest(String accessId, Set<String> tags, int timeoutMs, String user) {
    this.accessId = accessId;
    this.tags = tags;
    this.timeoutMs = timeoutMs;
    this.extraProperties = Collections.emptyMap();
    this.user = user;
  }

  public RssAccessClusterRequest(
      String accessId,
      Set<String> tags,
      int timeoutMs,
      Map<String, String> extraProperties,
      String user) {
    this.accessId = accessId;
    this.tags = tags;
    this.timeoutMs = timeoutMs;
    this.extraProperties = extraProperties;
    this.user = user;
  }

  public String getAccessId() {
    return accessId;
  }

  public Set<String> getTags() {
    return tags;
  }

  public int getTimeoutMs() {
    return timeoutMs;
  }

  public Map<String, String> getExtraProperties() {
    return extraProperties;
  }

  public String getUser() {
    return user;
  }
}
