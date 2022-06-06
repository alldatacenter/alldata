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

import java.util.List;
import java.util.Set;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;

/**
 * The {@link UpgradeCheck} is used before an upgrade in order to present the
 * administrator with a warning or a failure about an upgrade. All
 * implementations of this class should used the {@link UpgradeCheckInfo}
 * annotation to specify information about the check, such as its associated
 * upgrade type(s) and ordering.
 */
public interface UpgradeCheck {

  /**
   * Gets the set of services that this check is associated with. If the check
   * is not associated with a particular service, then this should be an empty
   * set.
   *
   * @return a set of services which will determine whether this check is
   *         applicable, or an empty set.
   */
  Set<String> getApplicableServices();

  /**
   * Gets any additional qualifications which an upgrade check should run in
   * order to determine if it's applicable to the upgrade.
   *
   * @return a list of qualifications, or an empty list.
   */
  List<CheckQualification> getQualifications();

  /**
   * Executes check against given cluster.
   * @param request pre upgrade check request
   * @return TODO
   *
   * @throws AmbariException if server error happens
   */
  UpgradeCheckResult perform(UpgradeCheckRequest request)
      throws AmbariException;

  /**
   * The {@link UpgradeCheckDescription} which includes the name, description, and
   * success/failure messages for a {@link UpgradeCheck}.
   *
   * @return the check description.
   */
  UpgradeCheckDescription getCheckDescription();
}