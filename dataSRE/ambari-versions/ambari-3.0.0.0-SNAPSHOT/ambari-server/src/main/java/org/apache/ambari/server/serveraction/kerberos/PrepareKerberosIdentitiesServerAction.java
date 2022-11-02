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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.UpdateConfigurationPolicy;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PrepareKerberosIdentitiesServerAction is a ServerAction implementation that prepares metadata needed
 * to process Kerberos identities (principals and keytabs files).
 */
public class PrepareKerberosIdentitiesServerAction extends AbstractPrepareKerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(PrepareKerberosIdentitiesServerAction.class);

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link KerberosServerAction#processIdentities(Map)}
   * to iterate through the Kerberos identity metadata and call
   * {@link PrepareKerberosIdentitiesServerAction#processIdentities(Map)}
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

    KerberosHelper kerberosHelper = getKerberosHelper();

    KerberosDescriptor kerberosDescriptor = getKerberosDescriptor(cluster, false);
    Map<String, String> commandParameters = getCommandParameters();
    OperationType operationType = getOperationType(getCommandParameters());

    Map<String, ? extends Collection<String>> serviceComponentFilter = getServiceComponentFilter();
    Collection<String> hostFilter = getHostFilter();
    Collection<String> identityFilter = getIdentityFilter();
    // If the operationType is default, use the getServiceComponentHostsToProcess method to determine
    // which ServiceComponentHosts to process based on the filters.  However if we are regenerating
    // keytabs for a specific set of components, build the identity filter below so we can
    // customized what needs to be done.
    List<ServiceComponentHost> schToProcess = kerberosHelper.getServiceComponentHostsToProcess(cluster, kerberosDescriptor,
        (operationType == OperationType.DEFAULT) ? serviceComponentFilter : null, hostFilter);

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

    Set<String> services = cluster.getServices().keySet();
    Map<String, Set<String>> propertiesToRemove = new HashMap<>();
    Map<String, Set<String>> propertiesToIgnore = new HashMap<>();
    boolean includeAmbariIdentity = "true".equalsIgnoreCase(getCommandParameterValue(commandParameters, KerberosServerAction.INCLUDE_AMBARI_IDENTITY));

    // If we are including the Ambari identity; then ensure that if a host filter is set, do not the Ambari service identity.
    includeAmbariIdentity &= (hostFilter == null);

    if (serviceComponentFilter != null) {
      // If we are including the Ambari identity; then ensure that if a service/component filter is set,
      // it contains the AMBARI/AMBARI_SERVER component; else do not include the Ambari service identity.
      includeAmbariIdentity &= (serviceComponentFilter.get(RootService.AMBARI.name()) != null)
          && serviceComponentFilter.get(RootService.AMBARI.name()).contains(RootComponent.AMBARI_SERVER.name());

      if ((operationType != OperationType.DEFAULT)) {
        // Update the identity filter, if necessary
        identityFilter = updateIdentityFilter(kerberosDescriptor, identityFilter, serviceComponentFilter);
      }
    }

    // Calculate the current host-specific configurations. These will be used to replace
    // variables within the Kerberos descriptor data
    Map<String, Map<String, String>> configurations = kerberosHelper.calculateConfigurations(cluster, null, kerberosDescriptor, false, false);

    processServiceComponentHosts(cluster, kerberosDescriptor, schToProcess, identityFilter, dataDirectory,
        configurations, kerberosConfigurations, includeAmbariIdentity, propertiesToIgnore);

    UpdateConfigurationPolicy updateConfigurationPolicy = getUpdateConfigurationPolicy(commandParameters);

    if (updateConfigurationPolicy != UpdateConfigurationPolicy.NONE) {
      if (updateConfigurationPolicy.invokeStackAdvisor()) {
        kerberosHelper.applyStackAdvisorUpdates(cluster, services, configurations, kerberosConfigurations,
            propertiesToIgnore, propertiesToRemove, true);
      }

      // TODO: Determine if we need to do this again since it is done a few lines above.
      Map<String, Map<String, String>> calculatedConfigurations = kerberosHelper.calculateConfigurations(cluster, null, kerberosDescriptor, false, false);

      if (updateConfigurationPolicy.applyIdentityChanges()) {
        processAuthToLocalRules(cluster, calculatedConfigurations, kerberosDescriptor, schToProcess, kerberosConfigurations, getDefaultRealm(commandParameters), false);
      }

      processConfigurationChanges(dataDirectory, kerberosConfigurations, propertiesToRemove, kerberosDescriptor, updateConfigurationPolicy);
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }

  /**
   * Calls {@link KerberosHelper#getKerberosDescriptor(Cluster, boolean)}
   *
   * @param cluster                 cluster instance
   * @param includePreconfigureData <code>true</code> to include the preconfigure data; <code>false</code> otherwise
   * @return the kerberos descriptor associated with the specified cluster
   * @throws AmbariException if unable to obtain the descriptor
   * @see KerberosHelper#getKerberosDescriptor(Cluster, boolean)
   */
  protected KerberosDescriptor getKerberosDescriptor(Cluster cluster, boolean includePreconfigureData)
      throws AmbariException {
    return getKerberosHelper().getKerberosDescriptor(cluster, includePreconfigureData);
  }

  /**
   * Conditionally calls {@link KerberosHelper#setAuthToLocalRules(Cluster, KerberosDescriptor, String, Map, Map, Map, boolean)}
   * if there are ServiceComponentHosts to process
   *
   * @param cluster                  the cluster
   * @param calculatedConfiguration  the configurations for the current cluster, used for replacements
   * @param kerberosDescriptor       the current Kerberos descriptor
   * @param schToProcess             a list of ServiceComponentHosts to process
   * @param kerberosConfigurations   the Kerberos-specific configuration map
   * @param defaultRealm             the default realm
   * @param includePreconfiguredData true to include services flagged to be pre-configured; false otherwise
   * @throws AmbariException
   * @see KerberosHelper#setAuthToLocalRules(Cluster, KerberosDescriptor, String, Map, Map, Map, boolean)
   */
  void processAuthToLocalRules(Cluster cluster, Map<String, Map<String, String>> calculatedConfiguration,
                               KerberosDescriptor kerberosDescriptor,
                               List<ServiceComponentHost> schToProcess,
                               Map<String, Map<String, String>> kerberosConfigurations,
                               String defaultRealm, boolean includePreconfiguredData)
      throws AmbariException {
    if (!schToProcess.isEmpty()) {
      actionLog.writeStdOut("Creating auth-to-local rules");

      Map<String, Set<String>> services = new HashMap<>();
      for (ServiceComponentHost sch : schToProcess) {
        Set<String> components = services.get(sch.getServiceName());
        if (components == null) {
          components = new HashSet<>();
          services.put(sch.getServiceName(), components);
        }

        components.add(sch.getServiceComponentName());
      }

      KerberosHelper kerberosHelper = getKerberosHelper();
      kerberosHelper.setAuthToLocalRules(cluster, kerberosDescriptor, defaultRealm, services,
          calculatedConfiguration, kerberosConfigurations, includePreconfiguredData);
    }
  }

  /**
   * Iterate through the identities in the Kerberos descriptor to find the relevant identities to
   * add to the identity filter.
   * <p>
   * The set of identities to include in the filter are determined by whether they are explicit
   * identities set in a component or service in the supplied service/component filter.
   *
   * @param kerberosDescriptor     the Kerberos descriptor
   * @param identityFilter         the existing identity filter
   * @param serviceComponentFilter the service/component filter
   * @return a new collection of paths (including any existing paths) to act as the updated identity filter
   */
  private Collection<String> updateIdentityFilter(KerberosDescriptor kerberosDescriptor,
                                                  Collection<String> identityFilter,
                                                  Map<String, ? extends Collection<String>> serviceComponentFilter) {

    Set<String> updatedFilter = (identityFilter == null) ? new HashSet<>() : new HashSet<>(identityFilter);

    Map<String, KerberosServiceDescriptor> serviceDescriptors = kerberosDescriptor.getServices();

    if (serviceDescriptors != null) {
      for (KerberosServiceDescriptor serviceDescriptor : serviceDescriptors.values()) {
        String serviceName = serviceDescriptor.getName();

        if (serviceComponentFilter.containsKey("*") || serviceComponentFilter.containsKey(serviceName)) {
          Collection<String> componentFilter = serviceComponentFilter.get(serviceName);
          boolean anyComponent = ((componentFilter == null) || componentFilter.contains("*"));

          // Only include the service-wide identities if the component filter is null contains "*", which indicates
          // that all component for the given service are to be processed.
          if (anyComponent) {
            addIdentitiesToFilter(serviceDescriptor.getIdentities(), updatedFilter, true);
          }

          Map<String, KerberosComponentDescriptor> componentDescriptors = serviceDescriptor.getComponents();
          if (componentDescriptors != null) {
            for (KerberosComponentDescriptor componentDescriptor : componentDescriptors.values()) {
              String componentName = componentDescriptor.getName();
              if (anyComponent || (componentFilter.contains(componentName))) {
                addIdentitiesToFilter(componentDescriptor.getIdentities(), updatedFilter, true);
              }
            }
          }
        }
      }
    }

    return updatedFilter;
  }

  /**
   * Add the path of each identity in the collection of identities to the supplied identity filter
   * if that identity is not a reference to another identity or if references are allowed.
   *
   * @param identityDescriptors the collection of identity descriptors to process
   * @param identityFilter      the identity filter to modify
   * @param skipReferences
   */
  private void addIdentitiesToFilter(List<KerberosIdentityDescriptor> identityDescriptors,
                                     Collection<String> identityFilter, boolean skipReferences) {
    if (!CollectionUtils.isEmpty(identityDescriptors)) {
      for (KerberosIdentityDescriptor identityDescriptor : identityDescriptors) {
        if (!skipReferences || !identityDescriptor.isReference()) {
          String identityPath = identityDescriptor.getPath();

          if (!StringUtils.isEmpty(identityPath)) {
            identityFilter.add(identityPath);

            // Find and add the references TO this identity to ensure the new/updated keytab file is
            // sent to the appropriate host(s)
            addIdentitiesToFilter(identityDescriptor.findReferences(), identityFilter, false);
          }
        }
      }
    }
  }
}

