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
package org.apache.ambari.server.stack.upgrade;

import java.util.List;

import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.MoreObjects;

/**
 * The {@link HostOrderItem} class represents the orchestration order of hosts
 * and service checks in an {@link UpgradeType#HOST_ORDERED} upgrade.
 */
public class HostOrderItem {

  /**
   * The {@link HostOrderActionType} defines the type of action which should be
   * taken on the list of action items.
   */
  public enum HostOrderActionType {
    /**
     * Represents that the action items are services which should have service
     * checks scheduled.
     */
    SERVICE_CHECK,

    /**
     * Represents that the action items are hosts which need to be scheduled for
     * upgrade.
     */
    HOST_UPGRADE;
  }

  /**
   * The type of action.
   */
  private final HostOrderActionType m_type;

  /**
   * The items to take action on. If {@link HostOrderActionType#HOST_UPGRADE},
   * then this should be a list of hosts. If
   * {@link HostOrderActionType#SERVICE_CHECK}, then this should be a list of
   * services.
   */
  private final List<String> m_actionItems;

  /**
   * Constructor.
   *
   * @param type
   * @param actionItems
   */
  public HostOrderItem(HostOrderActionType type, List<String> actionItems) {
    m_type = type;
    m_actionItems = actionItems;
  }

  /**
   * Gets the type of action to take.
   *
   * @return the type of action to take.
   */
  public HostOrderActionType getType() {
    return m_type;
  }

  /**
   * Gets the list of action items to take action on. This could be a list of
   * host names or a list of services.
   *
   * @return the list of action items.
   */
  public List<String> getActionItems() {
    return m_actionItems;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("type", m_type).add("items",
        StringUtils.join(m_actionItems, ", ")).omitNullValues().toString();
  }
}
