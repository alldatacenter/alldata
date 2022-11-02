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

/**
 * The {@link NotificationState} class represents the various states that an
 * outbound notification can exist in. It allows tracking and history for
 * notifications that are pending, have succeeded, or were processed but failed
 * to be sent successfully.
 */
public enum NotificationState {

  /**
   * The notification has not yet been processed by the notification system.
   */
  PENDING,

  /**
   * The notification was processed, but failed to be sent. It will not be
   * processed again.
   */
  FAILED,

  /**
   * The notification was processed successfully.
   */
  DELIVERED,

  /**
   * The notification was picked up and processed by the dispatcher, but there
   * is no information on whether the delivery was successful.
   */
  DISPATCHED;
}
