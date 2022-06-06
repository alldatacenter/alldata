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
package org.apache.ambari.server.serveraction.kerberos;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.internal.ArtifactResourceProvider;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.SecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to perform Kerberos Cleanup Operations as part of the Unkerberization process
 */
public class CleanupServerAction extends KerberosServerAction {

  private final static Logger LOG = LoggerFactory.getLogger(CleanupServerAction.class);

  @Override
  protected boolean pruneServiceFilter() {
    return false;
  }

  /**
   * Processes an identity as necessary.
   * <p/>
   * This method is not used since the {@link #processIdentities(java.util.Map)} is not invoked
   *
   * @param resolvedPrincipal        a ResolvedKerberosPrincipal object to process
   * @param operationHandler         a KerberosOperationHandler used to perform Kerberos-related
   *                                 tasks for specific Kerberos implementations
   *                                 (MIT, Active Directory, etc...)
   * @param kerberosConfiguration    a Map of configuration properties from kerberos-env
   * @param includedInFilter         a Boolean value indicating whather the principal is included in
   *                                 the current filter or not
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return null, always
   * @throws AmbariException if an error occurs while processing the identity record
   */
  @Override
  protected CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, String> kerberosConfiguration,
                                          boolean includedInFilter,
                                          Map<String, Object> requestSharedDataContext)
      throws AmbariException {
    return null;
  }

  /**
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return
   * @throws AmbariException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {

    Cluster cluster = getCluster();
    if (cluster.getSecurityType().equals(SecurityType.NONE)) { // double check this is done in a non secure environment
      removeKerberosArtifact(cluster);
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }

  /**
   * Removes the Kerberos descriptor artifact from the database
   *
   * @param cluster targeted for the remove process
   * @throws AmbariException
   */
  private void removeKerberosArtifact(Cluster cluster) throws AmbariException {
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property("Artifacts/cluster_name").equals(cluster.getClusterName()).and().
      property(ArtifactResourceProvider.ARTIFACT_NAME_PROPERTY).equals("kerberos_descriptor").
      end().toPredicate();

    ClusterController clusterController = ClusterControllerHelper.getClusterController();

    ResourceProvider artifactProvider = clusterController.ensureResourceProvider(Resource.Type.Artifact);

    try {
      artifactProvider.deleteResources(new RequestImpl(null, null, null, null), predicate);
      LOG.info("Kerberos descriptor removed successfully.");
      actionLog.writeStdOut("Kerberos descriptor removed successfully.");
    } catch (NoSuchResourceException e) {
      LOG.warn("The Kerberos descriptor was not found in the database while attempting to remove.");
      actionLog.writeStdOut("The Kerberos descriptor was not found in the database while attempting to remove.");
    } catch (Exception e) {
      throw new AmbariException("An unknown error occurred while trying to delete the cluster Kerberos descriptor", e);
    }
  }

}
