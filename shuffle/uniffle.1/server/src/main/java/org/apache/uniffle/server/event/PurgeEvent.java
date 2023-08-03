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

package org.apache.uniffle.server.event;

import java.util.List;

// todo: introduce the unified abstract dispatcher to handle events,
// mentioned in https://github.com/apache/incubator-uniffle/pull/249#discussion_r983001435
public abstract class PurgeEvent {
  private String appId;
  private String user;
  private List<Integer> shuffleIds;

  public PurgeEvent(String appId, String user, List<Integer> shuffleIds) {
    this.appId = appId;
    this.user = user;
    this.shuffleIds = shuffleIds;
  }

  public String getAppId() {
    return appId;
  }

  public String getUser() {
    return user;
  }

  public List<Integer> getShuffleIds() {
    return shuffleIds;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{"
        + "appId='" + appId + '\''
        + ", user='" + user + '\''
        + ", shuffleIds=" + shuffleIds
        + '}';
  }
}
