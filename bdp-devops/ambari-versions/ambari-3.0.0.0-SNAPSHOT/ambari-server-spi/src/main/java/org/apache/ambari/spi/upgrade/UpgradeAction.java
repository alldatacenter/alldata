/**
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

import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.exceptions.UpgradeActionException;

/**
 * The {@link UpgradeAction} interface is used to provide a
 * {@link UpgradeActionOperations} which will instruct the server to perform some
 * sort of action during the upgrade. This can be used for operations like
 * changing configurations when the XML markup in the upgrade packs is not
 * sufficient.
 */
public interface UpgradeAction {

  /**
   * Gets the changes which this upgrade operation is asking the server to make
   * during the upgrade.
   *
   * @param clusterInformation
   *          the cluster information, such as topology, configurations, etc.
   * @param upgradeInformation
   *          the upgrade type, direction, services, repository versions, etc.
   * @return the changes to perform during the upgrade, such as updating
   *         configurations.
   * @throws UpgradeActionException
   *           if the class is unable to create the operations for the Ambari
   *           Server to execute.
   */
  UpgradeActionOperations getOperations(ClusterInformation clusterInformation,
      UpgradeInformation upgradeInformation) throws UpgradeActionException;
}
