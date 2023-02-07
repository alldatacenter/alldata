/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.server.rest.profile;

import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;

import java.util.HashMap;
import java.util.Map;

public class ProfileUtil {

  private static final Map<QueryState, String> queryStateDisplayMap = new HashMap<>(QueryState.values().length);

  static {
    queryStateDisplayMap.put(QueryState.PREPARING, "Preparing");
    queryStateDisplayMap.put(QueryState.PLANNING, "Planning");
    queryStateDisplayMap.put(QueryState.ENQUEUED, "Enqueued");
    queryStateDisplayMap.put(QueryState.STARTING, "Starting");
    queryStateDisplayMap.put(QueryState.RUNNING, "Running");
    queryStateDisplayMap.put(QueryState.COMPLETED, "Succeeded");
    queryStateDisplayMap.put(QueryState.FAILED, "Failed");
    queryStateDisplayMap.put(QueryState.CANCELLATION_REQUESTED, "Cancellation Requested");
    queryStateDisplayMap.put(QueryState.CANCELED, "Canceled");
  }

  /**
   * Utility method to return display name for query state
   * @param queryState query state
   * @return display string for query state
   */
  public static String getQueryStateDisplayName(QueryState queryState) {
    String displayName = queryStateDisplayMap.get(queryState);
    if (displayName == null) {
      displayName = queryState.name();
    }
    return displayName;
  }
}
