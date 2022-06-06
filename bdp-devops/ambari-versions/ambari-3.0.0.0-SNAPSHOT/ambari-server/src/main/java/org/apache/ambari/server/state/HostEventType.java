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

public enum HostEventType {
  /**
   * Event to denote when a registration request is received from a Host
   */
  HOST_REGISTRATION_REQUEST,
  /**
   * Host status check response received.
   */
  HOST_STATUS_UPDATES_RECEIVED,
  /**
   * A healthy heartbeat event received from the Host.
   */
  HOST_HEARTBEAT_HEALTHY,
  /**
   * No heartbeat received from the Host within the defined expiry interval.
   */
  HOST_HEARTBEAT_LOST,
  /**
   * A non-healthy heartbeat event received from the Host.
   */
  HOST_HEARTBEAT_UNHEALTHY
}
