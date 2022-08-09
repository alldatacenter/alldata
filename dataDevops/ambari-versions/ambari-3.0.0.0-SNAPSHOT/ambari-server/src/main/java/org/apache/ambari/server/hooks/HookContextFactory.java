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

package org.apache.ambari.server.hooks;

import java.util.Map;
import java.util.Set;

/**
 * Factory interface definition to control creation of HookContext implementation.
 * The stateless factory interface makes possible to leverage the IoC framework in managing instances.
 */
public interface HookContextFactory {
  /**
   * Factory method for HookContext implementations.
   *
   * @param userName the username to be inferred to the instance being created
   * @return a HookContext instance
   */
  HookContext createUserHookContext(String userName);

  /**
   * Factory method for BatchUserHookContext instances.
   *
   * @param userGroups a map with userNames as keys and group list as values.
   * @return a new BatchUserHookContext instance
   */
  HookContext createBatchUserHookContext(Map<String, Set<String>> userGroups);
}
