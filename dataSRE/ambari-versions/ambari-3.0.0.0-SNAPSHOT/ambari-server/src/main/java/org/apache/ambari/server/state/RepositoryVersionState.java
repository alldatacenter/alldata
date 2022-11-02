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

import java.util.List;

/**
 * The {@link RepositoryVersionState} represents the state of a repository on a
 * particular host. Because hosts can contain a mixture of components from
 * different repositories, there can be any combination of
 * {@link RepositoryVersionState#CURRENT}} entries for a single host. A host may
 * not have multiple entries for the same repository.
 * <p/>
 *
 *
 * <pre>
 * Step 1: Initial Configuration
 * Version 1 is CURRENT
 *
 * Step 2: Add another repository and trigger distributing repositories/installing packages
 * Version 1: CURRENT
 * Version 2: INSTALLING
 *
 * Step 3: distributing repositories/installing packages action finishes successfully or fails
 * Version 1: CURRENT
 * Version 2: INSTALLED
 *
 * or
 *
 * Version 1: CURRENT
 * Version 2: INSTALL_FAILED (a retry can set this back to INSTALLING)
 *
 * Step 4: Perform an upgrade of every component on the host from version 1 to version 2
 * Version 1: INSTALLED
 * Version 2: CURRENT
 *
 * Step 4a: Perform an upgrade of a single component, leaving other components on the prior version
 * Version 1: CURRENT
 * Version 2: CURRENT
 *
 * Step 4b: May revert to the original version via a downgrade, which is technically still an upgrade to a version
 * and eventually becomes
 *
 * Version 1: CURRENT
 * Version 2: INSTALLED
 *
 * *********************************************
 * Start states: NOT_REQUIRED, INSTALLING, CURRENT
 * Allowed Transitions:
 * INSTALLED -> CURRENT
 * INSTALLING -> INSTALLED | INSTALL_FAILED | OUT_OF_SYNC
 * INSTALLED -> INSTALLED | INSTALLING | OUT_OF_SYNC
 * OUT_OF_SYNC -> INSTALLING
 * INSTALL_FAILED -> INSTALLING
 * CURRENT -> INSTALLED
 * </pre>
 */
public enum RepositoryVersionState {
  /**
   * Repository version is not required
   */
  NOT_REQUIRED(0),

  /**
   * Repository version that is in the process of being installed.
   */
  INSTALLING(3),

  /**
   * Repository version that is installed and supported but not the active version.
   */
  INSTALLED(2),

  /**
   * Repository version that during the install process failed to install some components.
   */
  INSTALL_FAILED(5),

  /**
   * Repository version that is installed for some components but not for all.
   */
  OUT_OF_SYNC(4),

  /**
   * Repository version that is installed and supported and is the active version.
   */
  CURRENT(1);

  private final int weight;

  /**
   * Constructor.
   *
   * @param weight
   *          the weight of the state.
   */
  private RepositoryVersionState(int weight) {
    this.weight = weight;
  }

  /**
   * Gets a single representation of the repository state based on the supplied
   * states.
   *
   * @param states
   *          the states to calculate the aggregate for.
   * @return the "heaviest" state.
   */
  public static RepositoryVersionState getAggregateState(List<RepositoryVersionState> states) {
    if (null == states || states.isEmpty()) {
      return NOT_REQUIRED;
    }

    RepositoryVersionState heaviestState = states.get(0);
    for (RepositoryVersionState state : states) {
      if (state.weight > heaviestState.weight) {
        heaviestState = state;
      }
    }

    return heaviestState;
  }

}
