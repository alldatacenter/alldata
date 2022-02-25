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

import java.util.concurrent.Executor;

/**
 * The {@link DispatchRunnable} class is a simple {@link Runnable} that can be
 * used to pass a {@link Notification} to an {@link NotificationDispatcher} via an
 * {@link Executor}.
 */
public final class DispatchRunnable implements Runnable {

  /**
   * The dispatcher to dispatch to.
   */
  private final NotificationDispatcher m_dispatcher;

  /**
   * The notification to dispatch.
   */
  private final Notification m_notification;

  /**
   * Constructor.
   * 
   * @param dispatcher
   *          the dispatcher to dispatch to (not {@code null}).
   * @param notification
   *          the notification to dispatch (not {@code null}).
   */
  public DispatchRunnable(NotificationDispatcher dispatcher, Notification notification) {
    m_dispatcher = dispatcher;
    m_notification = notification;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    m_dispatcher.dispatch(m_notification);
  }
}
