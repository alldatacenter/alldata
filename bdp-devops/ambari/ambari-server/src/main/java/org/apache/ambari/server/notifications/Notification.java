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
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * The {@link Notification} class is a generic way to relay content through an
 * {@link NotificationDispatcher}.
 */
public class Notification {

  /**
   * The {@link Type} enumeration represents the type of notification being sent
   * to a {@link NotificationDispatcher}.
   */
  public enum Type {
    /**
     * The notification is generic.
     */
    GENERIC,

    /**
     * The notification is an alert type.
     */
    ALERT
  }

  /**
   * A short summary of the notification.
   */
  public String Subject;

  /**
   * The main content of the notification.
   */
  public String Body;

  /**
   * The optional recipients of the notification. Some dispatchers may not
   * require explicit recipients.
   */
  public List<Recipient> Recipients;

  /**
   * A map of all of the properties that a {@link NotificationDispatcher} needs
   * in order to dispatch this notification.
   */
  public Map<String, String> DispatchProperties;

  /**
   * The optional credentials used to authenticate with the dispatcher.
   */
  public DispatchCredentials Credentials;

  /**
   * An optional callback implementation that the dispatcher can use to report
   * success/failure on delivery.
   */
  public DispatchCallback Callback;

  /**
   * An option list of unique IDs that will identify the origins of this
   * notification.
   */
  public List<String> CallbackIds;

  /**
   * Gets the type of notificaton.
   *
   * @return the type (never {@code null}).
   */
  public Type getType() {
    return Type.GENERIC;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("Notification{ ");
    buffer.append("type=").append(getType());
    buffer.append(", subject=").append(StringUtils.strip(Subject));
    buffer.append("}");
    return buffer.toString();
  }
}
