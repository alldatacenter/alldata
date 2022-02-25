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
package org.apache.ambari.spi.upgrade;

/**
 * The {@link UpgradeCheckGroup} enum is used to organize pre-upgrade checks
 * into specific groups that have their own relational ordering.
 * <p>
 * The order for each group is determined by a {@code float} so that new groups
 * can be added in between others without the need to reorder all of the groups.
 */
public enum UpgradeCheckGroup {

  /**
   * Check for masters in maintenance mode and then services in maintenance
   * mode.
   */
  MAINTENANCE_MODE(1.0f),

  /**
   * Checks the repository version on the hosts.
   */
  REPOSITORY_VERSION(2.0f),

  /**
   * Checks for NameNode HA and checks that depend on NameNode HA.
   */
  NAMENODE_HA(3.0f),

  /**
   * Checks for the topology of service and components.
   */
  TOPOLOGY(4.0f),

  /**
   * Checks for the state of a host or service being alive and responsive.
   */
  LIVELINESS(5.0f),

  /**
   * Checks for the client retry properties to be set in clients that support
   * this.
   */
  CLIENT_RETRY_PROPERTY(6.0f),

  /**
   * Checks for various HA components, such as multiple metastores, are
   * available.
   */
  MULTIPLE_COMPONENT_WARNING(7.0f),

  /**
   * A general group for warning about configuration properties.
   */
  CONFIGURATION_WARNING(8.0f),

  /***
   * Checks the component version on the hosts.
   */
  COMPONENT_VERSION(9.0f),

  /**
   * A general group for related to Kerberos checks.
   */
  KERBEROS(10.0f),

  /**
   * A general group for informational warning checks.
   */
  INFORMATIONAL_WARNING(100.0f),

  /**
   * All other checks.
   */
  DEFAULT(Float.MAX_VALUE);

  /**
   * The order of upgrade check groups.
   */
  private final Float m_order;

  /**
   * Constructor.
   *
   * @param order
   */
  UpgradeCheckGroup(Float order) {
    m_order = order;
  }

  /**
   * Gets the group's order.
   *
   * @return the order of the group.
   */
  public Float getOrder() {
    return m_order;
  }
}
