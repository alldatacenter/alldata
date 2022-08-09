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

package org.apache.ambari.server.state;

public enum HostState {
  /**
   * New host state
   */
  INIT,
  /**
   * State when a registration request is received from the Host but
   * the host has not responded to its status update check.
   */
  WAITING_FOR_HOST_STATUS_UPDATES,
  /**
   * State when the server is receiving heartbeats regularly from the Host
   * and the state of the Host is healthy
   */
  HEALTHY,
  /**
   * State when the server has not received a heartbeat from the Host in the
   * configured heartbeat expiry window.
   */
  HEARTBEAT_LOST,
  /**
   * Host is in unhealthy state as reported either by the Host itself or via
   * any other additional means ( monitoring layer )
   */
  UNHEALTHY;
}
