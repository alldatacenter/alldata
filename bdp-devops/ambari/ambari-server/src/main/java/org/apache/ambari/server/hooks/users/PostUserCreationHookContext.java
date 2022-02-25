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

package org.apache.ambari.server.hooks.users;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.hooks.HookContext;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class PostUserCreationHookContext implements HookContext {
  private Map<String, Set<String>> userGroups = new HashMap<>();

  @AssistedInject
  public PostUserCreationHookContext(@Assisted Map<String, Set<String>> userGroups) {
    this.userGroups = userGroups;
  }

  @AssistedInject
  public PostUserCreationHookContext(@Assisted String userName) {
    userGroups.put(userName, Collections.emptySet());
  }


  public Map<String, Set<String>> getUserGroups() {
    return userGroups;
  }

  @Override
  public String toString() {
    return "BatchUserHookContext{" +
        "userGroups=" + userGroups +
        '}';
  }
}
