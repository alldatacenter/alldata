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
package org.apache.ambari.server.events.listeners.upgrade;

import java.util.List;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.events.ActionFinalReportReceivedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link org.apache.ambari.server.events.listeners.upgrade.DistributeRepositoriesActionListener} class
 * handles {@link org.apache.ambari.server.events.ActionFinalReportReceivedEvent}
 * for "Distribute repositories/install packages" action.
 * It processes command reports and and updates host stack version state acordingly.
 */
@Singleton
@EagerSingleton
public class DistributeRepositoriesActionListener {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(DistributeRepositoriesActionListener.class);
  public static final String INSTALL_PACKAGES = "install_packages";

  @Inject
  private Provider<HostVersionDAO> hostVersionDAO;

  @Inject
  private RepositoryVersionDAO repoVersionDAO;

  @Inject
  private Gson gson;

  /**
   * Constructor.
   *
   * @param publisher
   */
  @Inject
  public DistributeRepositoriesActionListener(AmbariEventPublisher publisher) {
    publisher.register(this);
  }

  @Subscribe
  public void onActionFinished(ActionFinalReportReceivedEvent event) {
    // Check if it is "Distribute repositories/install packages" action.
    if (! event.getRole().equals(INSTALL_PACKAGES)) {
      return;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(event.toString());
    }

    Long clusterId = event.getClusterId();
    if (clusterId == null) {
      LOG.error("Distribute Repositories expected a cluster Id for host " + event.getHostname());
      return;
    }

    String repositoryVersion = null;
    RepositoryVersionState newHostState = RepositoryVersionState.INSTALL_FAILED;

    if (event.getCommandReport() == null) {
      LOG.error(
          "Command report is null, will set all INSTALLING versions for host {} to INSTALL_FAILED.",
          event.getHostname());
    } else if (!event.getCommandReport().getStatus().equals(HostRoleStatus.COMPLETED.toString())) {
      LOG.warn(
          "Distribute repositories did not complete, will set all INSTALLING versions for host {} to INSTALL_FAILED.",
          event.getHostname());
    } else {

      DistributeRepositoriesStructuredOutput structuredOutput = null;
      try {
        structuredOutput = gson.fromJson(event.getCommandReport().getStructuredOut(),
            DistributeRepositoriesStructuredOutput.class);
      } catch (JsonSyntaxException e) {
        LOG.error("Cannot parse structured output %s", e);
      }

      if (null == structuredOutput || null == structuredOutput.repositoryVersionId) {
        LOG.error("Received an installation reponse, but it did not contain a repository version id");
      } else {
        newHostState = RepositoryVersionState.INSTALLED;

        String actualVersion = structuredOutput.actualVersion;

        RepositoryVersionEntity repoVersion = repoVersionDAO.findByPK(structuredOutput.repositoryVersionId);

        if (null != repoVersion && StringUtils.isNotBlank(actualVersion)) {
          if (!StringUtils.equals(repoVersion.getVersion(), actualVersion)) {
            repoVersion.setVersion(actualVersion);
            repoVersion.setResolved(true);
            repoVersionDAO.merge(repoVersion);
            repositoryVersion = actualVersion;
          } else {
            // the reported versions are the same - we should ensure that the
            // repo is resolved
            if (!repoVersion.isResolved()) {
              repoVersion.setResolved(true);
              repoVersionDAO.merge(repoVersion);
            }
          }
        }
      }
    }

    List<HostVersionEntity> hostVersions = hostVersionDAO.get().findByHost(event.getHostname());
      // We have to iterate over all host versions for this host. Otherwise server-side command aborts (that do not
      // provide exact host stack version info) would be ignored
    for (HostVersionEntity hostVersion : hostVersions) {

      if (! event.isEmulated() && // Emulated events anyway can not provide actual repo version
              ! (repositoryVersion == null || hostVersion.getRepositoryVersion().getVersion().equals(repositoryVersion))) {
        continue;
      }

      // If repository version is null, it means that we were not able to determine any information (perhaps structured-out was empty),
      // so we should transition from INSTALLING to INSTALL_FAILED

      // If we know exact host stack version, there will be single execution of a code below
      if (hostVersion.getState() == RepositoryVersionState.INSTALLING) {
        hostVersion.setState(newHostState);
        hostVersionDAO.get().merge(hostVersion);
      }
    }
  }

  /**
   * Used only to parse the structured output of a distribute versions call
   */
  private static class DistributeRepositoriesStructuredOutput {
    /**
     * Either SUCCESS or FAIL
     */
    @SerializedName("package_installation_result")
    private String packageInstallationResult;

    /**
     * The actual version returned, even when a failure during install occurs.
     */
    @SerializedName("actual_version")
    private String actualVersion;

    /**
     * The repository id that is returned in structured output.
     */
    @SerializedName("repository_version_id")
    private Long repositoryVersionId = null;
  }

}
