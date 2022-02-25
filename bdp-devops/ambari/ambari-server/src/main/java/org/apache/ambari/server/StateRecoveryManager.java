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

package org.apache.ambari.server;

import java.util.List;

import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Is executed on server start.
 * Checks server state and recovers it to valid if required.
 */
public class StateRecoveryManager {

  private static final Logger LOG = LoggerFactory.getLogger(StateRecoveryManager.class);

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private ServiceComponentDesiredStateDAO serviceComponentDAO;

  public void doWork() {
    checkHostAndClusterVersions();
  }

  void checkHostAndClusterVersions() {
    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();
    for (HostVersionEntity hostVersion : hostVersions) {
      if (hostVersion.getState().equals(RepositoryVersionState.INSTALLING)) {
        hostVersion.setState(RepositoryVersionState.INSTALL_FAILED);
        String msg = String.format(
                "Recovered state of host version %s on host %s from %s to %s",
                hostVersion.getRepositoryVersion().getDisplayName(),
                hostVersion.getHostName(),
                RepositoryVersionState.INSTALLING,
                RepositoryVersionState.INSTALL_FAILED);
        LOG.warn(msg);
        hostVersionDAO.merge(hostVersion);
      }
    }

    List<ServiceComponentDesiredStateEntity> components = serviceComponentDAO.findAll();
    for (ServiceComponentDesiredStateEntity component : components) {
      if (RepositoryVersionState.INSTALLING == component.getRepositoryState()) {
        component.setRepositoryState(RepositoryVersionState.INSTALL_FAILED);
        serviceComponentDAO.merge(component);
        String msg = String.format(
            "Recovered state of cluster %s of component %s/%s for version %s from %s to %s",
            component.getClusterId(),
            component.getServiceName(),
            component.getComponentName(),
            component.getDesiredRepositoryVersion().getDisplayName(),
            RepositoryVersionState.INSTALLING,
            RepositoryVersionState.INSTALL_FAILED);
        LOG.warn(msg);
      }
    }
  }


}
