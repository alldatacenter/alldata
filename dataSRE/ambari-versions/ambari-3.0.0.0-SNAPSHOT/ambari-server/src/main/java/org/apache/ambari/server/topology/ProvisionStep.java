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
package org.apache.ambari.server.topology;

import static org.apache.ambari.server.controller.AmbariManagementControllerImpl.CLUSTER_PHASE_INITIAL_INSTALL;
import static org.apache.ambari.server.controller.AmbariManagementControllerImpl.CLUSTER_PHASE_INITIAL_START;
import static org.apache.ambari.server.controller.AmbariManagementControllerImpl.CLUSTER_PHASE_PROPERTY;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.DO_NOT_SKIP_INSTALL_FOR_COMPONENTS;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.FOR_ALL_COMPONENTS;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.FOR_NO_COMPONENTS;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.SKIP_INSTALL_FOR_COMPONENTS;

import java.util.Map;

import org.apache.ambari.server.state.State;

import com.google.common.collect.ImmutableMap;

/**
 * Steps of service provisioning.
 */
public enum ProvisionStep {

  INSTALL {
    @Override
    public State getDesiredStateToSet() {
      return State.INSTALLED;
    }

    @Override
    public Map<String, String> getProvisionProperties() {
      return ImmutableMap.of(CLUSTER_PHASE_PROPERTY, CLUSTER_PHASE_INITIAL_INSTALL);
    }
  },

  /**
   * This special step is used for START_ONLY services/components, because state
   * transition cannot skip INSTALLED in the INIT -> INSTALLED -> STARTED sequence.
   */
  SKIP_INSTALL {
    @Override
    public State getDesiredStateToSet() {
      return State.INSTALLED;
    }

    @Override
    public Map<String, String> getProvisionProperties() {
      return ImmutableMap.of(
        SKIP_INSTALL_FOR_COMPONENTS,        FOR_ALL_COMPONENTS,
        DO_NOT_SKIP_INSTALL_FOR_COMPONENTS, FOR_NO_COMPONENTS,
        CLUSTER_PHASE_PROPERTY,             CLUSTER_PHASE_INITIAL_INSTALL
      );
    }
  },

  START {
    @Override
    public State getDesiredStateToSet() {
      return State.STARTED;
    }

    @Override
    public Map<String, String> getProvisionProperties() {
      return ImmutableMap.of(CLUSTER_PHASE_PROPERTY, CLUSTER_PHASE_INITIAL_START);
    }
  },
  ;

  public abstract State getDesiredStateToSet();
  public abstract Map<String, String> getProvisionProperties();

}
