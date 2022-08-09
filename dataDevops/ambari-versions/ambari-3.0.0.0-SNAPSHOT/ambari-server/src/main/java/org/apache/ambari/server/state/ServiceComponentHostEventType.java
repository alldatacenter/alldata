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

public enum ServiceComponentHostEventType {
  /**
   * Operation in progress
   */
  HOST_SVCCOMP_OP_IN_PROGRESS,
  /**
   * Operation succeeded
   */
  HOST_SVCCOMP_OP_SUCCEEDED,
  /**
   * Operation failed.
   */
  HOST_SVCCOMP_OP_FAILED,
  /**
   * Re-starting a failed operation.
   */
  HOST_SVCCOMP_OP_RESTART,
  /**
   * Triggering an install.
   */
  HOST_SVCCOMP_INSTALL,
  /**
   * Triggering a start.
   */
  HOST_SVCCOMP_START,
  /**
   * Triggering a stop.
   */
  HOST_SVCCOMP_STOP,
  /**
   * Start completed.
   */
  HOST_SVCCOMP_STARTED,
  /**
   * Stop completed.
   */
  HOST_SVCCOMP_STOPPED,
  /**
   * Triggering an uninstall.
   */
  HOST_SVCCOMP_UNINSTALL,
  /**
   * Triggering a wipe-out ( restore to clean state ).
   */
  HOST_SVCCOMP_WIPEOUT,
  /**
   * Triggering a host component upgrade.
   */
  HOST_SVCCOMP_UPGRADE,
  /**
   * Putting host component into disabled state
   */
  HOST_SVCCOMP_DISABLE,
  /**
   * Recovering host component from disable state
   */
  HOST_SVCCOMP_RESTORE,
  /**
   * Triggering a server-side action
   */
  HOST_SVCCOMP_SERVER_ACTION

}
