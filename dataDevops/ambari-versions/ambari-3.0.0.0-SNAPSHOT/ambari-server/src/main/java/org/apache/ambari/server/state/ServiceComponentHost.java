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
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;


public interface ServiceComponentHost {

  /**
   * Get the Cluster that this object maps to
   */
  long getClusterId();

  /**
   * Get the Cluster that this object maps to
   */
  String getClusterName();

  /**
   * Get the Service this object maps to
   * @return Name of the Service
   */
  String getServiceName();

  boolean isClientComponent();

  /**
   * Get the ServiceComponent this object maps to
   * @return Name of the ServiceComponent
   */
  String getServiceComponentName();

  /**
   * Get the Host this object maps to
   * @return Host's hostname
   */
  String getHostName();

  /**
   * Get the public host name this object maps to
   * @return Host's public hostname
   */
  String getPublicHostName();

  /**
   * Get the Host this object maps to
   * @return Host Object
   */
  Host getHost();

  /**
   * Get whether recovery is enabled for
   * this component or not.
   * @return True or false.
   */
  boolean isRecoveryEnabled();

  /**
   * Send a ServiceComponentHostState event to the StateMachine
   * @param event Event to handle
   * @throws InvalidStateTransitionException
   */
  void handleEvent(ServiceComponentHostEvent event)
      throws InvalidStateTransitionException;

  State getDesiredState();

  void setDesiredState(State state);

  State getState();

  void setState(State state);

  /**
   * Gets the version of the component.
   *
   * @return component version
   */
  String getVersion();

  /**
   * Sets the version of the component from the stack.
   *
   * @param version component version (e.g. 2.2.0.0-2041)
   */
  void setVersion(String version) throws AmbariException;

  /**
   * @param upgradeState the upgrade state
   */
  void setUpgradeState(UpgradeState upgradeState);

  /**
   * @return the upgrade state. Valid values:
   * NONE  - means that component is installed and good to go, no upgrade in progress
   * IN_PROGRESS - means that component is being upgraded
   * COMPLETE - means that component has reported a correct new version during upgrade
   * FAILED - means that failed and component did not get upgraded
   * VERSION_MISMATCH - means that component reported unexpected version
   */
  UpgradeState getUpgradeState();

  HostComponentAdminState getComponentAdminState();

  void setComponentAdminState(HostComponentAdminState attribute);

  /**
   * Builds a {@link ServiceComponentHostResponse}.
   *
   * @param desiredConfigs
   *          the desired configurations for the cluster. Obtaining these can be
   *          expensive and since this method operates on SCH's, it could be
   *          called 10,000's of times when generating cluster/host responses.
   *          Therefore, the caller should build these once and pass them in. If
   *          {@code null}, then this method will retrieve them at runtime,
   *          incurring a performance penality.
   * @return
   */
  ServiceComponentHostResponse convertToResponse(Map<String, DesiredConfig> desiredConfigs);
  ServiceComponentHostResponse convertToResponseStatusOnly(Map<String, DesiredConfig> desiredConfigs,
                                                           boolean collectStaleConfigsStatus);

  void debugDump(StringBuilder sb);

  boolean canBeRemoved();

  void delete(DeleteHostComponentStatusMetaData deleteMetaData);

  /**
   * Gets the actual config tags, if known.
   * @return the actual config map
   */
  Map<String, HostConfig> getActualConfigs();

  HostState getHostState();

  /**
   * @param state the maintenance state
   */
  void setMaintenanceState(MaintenanceState state);

  /**
   * @return the maintenance state
   */
  MaintenanceState getMaintenanceState();

  /**
   * @param procs a list containing a map describing each process
   */
  void setProcesses(List<Map<String, String>> procs);


  /**
   * @return the list of maps describing each process
   */
  List<Map<String, String>> getProcesses();

  /**
   * @return whether restart required
   */
  boolean isRestartRequired();

  boolean isRestartRequired(HostComponentDesiredStateEntity hostComponentDesiredStateEntity);

  /**
   * @param restartRequired the restartRequired flag
   */
  void setRestartRequired(boolean restartRequired);

  /**
   * Set restartRequired flag for appropriate HostComponentDesiredStateEntity
   * @param restartRequired the restartRequired flag.
   * @return true when restartRequired flag was changed.
   */
  boolean setRestartRequiredWithoutEventPublishing(boolean restartRequired);


  HostComponentDesiredStateEntity getDesiredStateEntity();

  /**
   * Gets the service component.
   *
   * @return the service component (never {@code null}).
   */
  ServiceComponent getServiceComponent();

  /**
   * Updates an existing {@link HostVersionEntity} for the desired repository of
   * this component, or create one if it doesn't exist.
   *
   * @return Returns either the newly created or the updated Host Version
   *         Entity.
   * @throws AmbariException
   */
  HostVersionEntity recalculateHostVersionState() throws AmbariException;

  /**
   * Convenience method to get the desired stack id from the service component
   *
   * @return the desired stack id
   */
  StackId getDesiredStackId();

}
