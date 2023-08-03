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

package org.apache.uniffle.coordinator.access;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;

public class AccessInfo {
  private final String accessId;
  private final Set<String> tags;
  private final Map<String, String> extraProperties;
  private final String user;

  public AccessInfo(String accessId, Set<String> tags, Map<String, String> extraProperties, String user) {
    this.accessId = accessId;
    this.tags = tags;
    this.extraProperties = extraProperties == null ? Collections.emptyMap() : extraProperties;
    this.user = user;
  }

  @VisibleForTesting
  public AccessInfo(String accessId) {
    this(accessId, Sets.newHashSet(), Collections.emptyMap(), "");
  }

  public String getAccessId() {
    return accessId;
  }

  public Set<String> getTags() {
    return tags;
  }

  public Map<String, String> getExtraProperties() {
    return extraProperties;
  }

  public String getUser() {
    return user;
  }

  @Override
  public String toString() {
    return "AccessInfo{"
            + "accessId='" + accessId + '\''
            + ", user= " + user
            + ", tags=" + tags
            + ", extraProperties=" + extraProperties
            + '}';
  }
}
