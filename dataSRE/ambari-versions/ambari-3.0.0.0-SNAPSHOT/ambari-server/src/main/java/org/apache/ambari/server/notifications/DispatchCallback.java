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
package org.apache.ambari.server.notifications;

import java.util.List;

/**
 * The {@link DispatchCallback} interface is used by an {@link NotificationDispatcher} to
 * callback to an interested party when a {@link Notification} delivery is
 * attempted.
 * <p/>
 * This class can be used to let the caller know the success or failure status
 * of the outbound {@link Notification}.
 */
public interface DispatchCallback {

  /**
   * Invoked when a {@link Notification} was successfully dispatched.
   *
   * @param callbackIds
   *          a list of unique IDs that the caller can use to identify the
   *          {@link Notification} that was dispatched.
   */
  void onSuccess(List<String> callbackIds);

  /**
   * Invoked when a {@link Notification} failed to be dispatched.
   * 
   * @param callbackIds
   *          a list of unique IDs that the caller can use to identify the
   *          {@link Notification} that was dispatched.
   */
  void onFailure(List<String> callbackIds);

}
