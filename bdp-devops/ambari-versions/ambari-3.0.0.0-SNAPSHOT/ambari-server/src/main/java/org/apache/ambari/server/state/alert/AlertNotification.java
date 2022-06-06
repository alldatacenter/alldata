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
package org.apache.ambari.server.state.alert;

import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.state.services.AlertNoticeDispatchService;
import org.apache.ambari.server.state.services.AlertNoticeDispatchService.AlertInfo;

/**
 * The {@link AlertNotification} represents a concrete {@link Notification} that
 * is specific to the {@link AlertNoticeDispatchService}.
 */
public class AlertNotification extends Notification {

  /**
   * The alert information.
   */
  private AlertInfo m_alertInfo;

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return Type.ALERT;
  }

  /**
   * @return the alertInfo
   */
  public AlertInfo getAlertInfo() {
    return m_alertInfo;
  }

  /**
   * @param alertInfo
   *          the alertInfo to set
   */
  public void setAlertInfo(AlertInfo alertInfo) {
    m_alertInfo = alertInfo;
  }
}
