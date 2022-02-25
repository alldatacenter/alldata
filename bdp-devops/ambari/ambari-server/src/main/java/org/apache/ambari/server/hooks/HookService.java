/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.ambari.server.hooks;

/**
 * Interface defining a contract for hook services. Hook services are responsible for executing additional logic  / hooks that can be tied
 * to different events or steps in the application.
 */
public interface HookService {

  /**
   * Entrypoint for the hook logic.
   *
   * @param hookContext the context on which the hook logic is to be executed
   *
   * @return true if the hook gets triggered, false otherwise
   */
  boolean execute(HookContext hookContext);
}
