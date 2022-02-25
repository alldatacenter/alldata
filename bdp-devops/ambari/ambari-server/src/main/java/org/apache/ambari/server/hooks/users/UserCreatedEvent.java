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

import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.hooks.HookContext;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Event signaling a user creation.
 */
public class UserCreatedEvent extends AmbariEvent {

  private HookContext context;

  @AssistedInject
  public UserCreatedEvent(@Assisted HookContext context) {
    super(AmbariEventType.USER_CREATED);
    this.context = context;
  }

  public HookContext getContext() {
    return context;
  }
}
