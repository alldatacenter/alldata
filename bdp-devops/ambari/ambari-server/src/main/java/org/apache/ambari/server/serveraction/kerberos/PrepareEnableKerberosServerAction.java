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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PrepareEnableKerberosServerAction is a ServerAction implementation that prepares metadata needed
 * to enable Kerberos on the cluster.
 */
public class PrepareEnableKerberosServerAction extends PrepareKerberosIdentitiesServerAction {

  private final static Logger LOG = LoggerFactory.getLogger(PrepareEnableKerberosServerAction.class);

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link KerberosServerAction#processIdentities(Map)}
   * to iterate through the Kerberos identity metadata and call
   * {@link PrepareEnableKerberosServerAction#processIdentities(Map)}
   * for each identity to process.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport indicating the result of this action
   * @throws AmbariException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws
      AmbariException, InterruptedException {

    Cluster cluster = getCluster();

    if (cluster == null) {
      throw new AmbariException("Missing cluster object");
    }

    Map<String, String> commandParameters = getCommandParameters();

    PreconfigureServiceType type = getCommandPreconfigureType();
    KerberosDescriptor kerberosDescriptor = getKerberosDescriptor(cluster, type != PreconfigureServiceType.NONE);
    if (type == PreconfigureServiceType.ALL) {
      // Force all services to be flagged for pre-configuration...
      Map<String, KerberosServiceDescriptor> serviceDescriptors = kerberosDescriptor.getServices();
      if (serviceDescriptors != null) {
        for (KerberosServiceDescriptor serviceDescriptor : serviceDescriptors.values()) {
          serviceDescriptor.setPreconfigure(true);
        }
      }
    }

    KerberosHelper kerberosHelper = getKerberosHelper();
    Map<String, ? extends Collection<String>> serviceComponentFilter = getServiceComponentFilter();
    Collection<String> hostFilter = getHostFilter();
    Collection<String> identityFilter = getIdentityFilter();
    List<ServiceComponentHost> schToProcess = kerberosHelper.getServiceComponentHostsToProcess(cluster, kerberosDescriptor, serviceComponentFilter, hostFilter);

    String dataDirectory = getCommandParameterValue(commandParameters, DATA_DIRECTORY);
    Map<String, Map<String, String>> kerberosConfigurations = new HashMap<>();

    int schCount = schToProcess.size();
    if (schCount == 0) {
      actionLog.writeStdOut("There are no components to process");
    } else if (schCount == 1) {
      actionLog.writeStdOut(String.format("Processing %d component", schCount));
    } else {
      actionLog.writeStdOut(String.format("Processing %d components", schCount));
    }

    Map<String, Set<String>> propertiesToRemove = new HashMap<>();
    Map<String, Set<String>> propertiesToIgnore = new HashMap<>();
    Set<String> services = cluster.getServices().keySet();

    // Calculate the current host-specific configurations. These will be used to replace
    // variables within the Kerberos descriptor data
    Map<String, Map<String, String>> configurations = kerberosHelper.calculateConfigurations(cluster, null, kerberosDescriptor, false, false);

    processServiceComponentHosts(cluster, kerberosDescriptor, schToProcess, identityFilter, dataDirectory,
        configurations, kerberosConfigurations, true, propertiesToIgnore);

    // Calculate the set of configurations to update and replace any variables
    // using the previously calculated Map of configurations for the host.
    kerberosConfigurations = kerberosHelper.processPreconfiguredServiceConfigurations(kerberosConfigurations, configurations, cluster, kerberosDescriptor);

    kerberosHelper.applyStackAdvisorUpdates(cluster, services, configurations, kerberosConfigurations,
          propertiesToIgnore, propertiesToRemove, true);

    processAuthToLocalRules(cluster, configurations, kerberosDescriptor, schToProcess, kerberosConfigurations, getDefaultRealm(commandParameters), true);

    // Ensure the cluster-env/security_enabled flag is set properly
    Map<String, String> clusterEnvProperties = kerberosConfigurations.get(KerberosHelper.SECURITY_ENABLED_CONFIG_TYPE);
    if (clusterEnvProperties == null) {
      clusterEnvProperties = new HashMap<>();
      kerberosConfigurations.put(KerberosHelper.SECURITY_ENABLED_CONFIG_TYPE, clusterEnvProperties);
    }
    clusterEnvProperties.put(KerberosHelper.SECURITY_ENABLED_PROPERTY_NAME, "true");

    processConfigurationChanges(dataDirectory, kerberosConfigurations, propertiesToRemove, kerberosDescriptor, getUpdateConfigurationPolicy(commandParameters));

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }

  @Override
  protected CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal, KerberosOperationHandler operationHandler, Map<String, String> kerberosConfiguration, boolean includedInFilter, Map<String, Object> requestSharedDataContext) throws AmbariException {
    throw new UnsupportedOperationException();
  }
}

