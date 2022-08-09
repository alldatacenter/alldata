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

import java.util.Map;

/**
 * The {@link NotificationDispatcher} interface represents a mechanism for dispatching a
 * {@link Notification}.
 * <p/>
 * Dispatchers should, in general, be singletons. They should also invoke the
 * appropriate methods on {@link Notification#Callback} to indicate a success or
 * failure during dispatch.
 */
public interface NotificationDispatcher {

  /**
   * Gets the type of dispatcher. The type of each different dispatcher should
   * be unique.
   *
   * @return the dispatcher's type (never {@code null}).
   */
  String getType();

  /**
   * Dispatches the specified notification.
   *
   * @param notification
   *          the notificationt to dispatch (not {@code null}).
   */
  void dispatch(Notification notification);

  /**
   * Gets whether the dispatcher supports sending a digest or summary in a
   * single {@link Notification}. Some providers may not allow the
   * {@link Notification} to contain information on more than a single event.
   *
   * @return {@code true} if digest is supported, {@code false} otherwise.
   */
  boolean isDigestSupported();

  /**
   * Validates alert target configuration. Checks if it can dispatch notifications with given properties set.
   * @param properties alert target properties
   * @return ConfigValidationResult with validation status and message
   */
  TargetConfigurationResult validateTargetConfig(Map<String, Object> properties);

  /**
   * Gets whether Ambari should attempt to generate the content of the
   * {@link Notification} before giving it to the dispatcher. Some dispatchers
   * may choose to format data in a way which is not easily represented in the
   * {alert-templates.xml}.
   * <p/>
   * If {@code false} then the {@link Notification} will only contain the
   * recipients and a map of properties for the dispatcher to use.
   *
   * @return {@code true} if Ambari should generate the {@link Notification}
   *         content, {@code false} otherwise.
   */
  boolean isNotificationContentGenerationRequired();
}
