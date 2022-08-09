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

package org.apache.ambari.server.serveraction.upgrades;

import static org.apache.ambari.server.controller.KerberosHelper.DEFAULT_REALM;
import static org.apache.ambari.server.controller.KerberosHelper.KERBEROS_ENV;
import static org.apache.ambari.server.controller.KerberosHelper.PRECONFIGURE_SERVICES;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.serveraction.kerberos.PreconfigureServiceType;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.kerberos.VariableReplacementHelper;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;

/**
 * PreconfigureKerberos updates existing service configurations with properties from service-level
 * Kerberos descriptors, flagged for pre-configuring, during stack upgrades in order to prevent service
 * restarts when the flagged services are installed.
 */
public class PreconfigureKerberosAction extends AbstractUpgradeServerAction {
  static final String UPGRADE_DIRECTION_KEY = "upgrade_direction";

  @Inject
  private AmbariManagementController ambariManagementController;

  @Inject
  private KerberosHelper kerberosHelper;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private VariableReplacementHelper variableReplacementHelper;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private KerberosKeytabDAO kerberosKeytabDAO;

  @Inject
  KerberosPrincipalDAO kerberosPrincipalDAO;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    Map<String, String> commandParameters = getCommandParameters();
    if (null == commandParameters || commandParameters.isEmpty()) {
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "",
          "Unable to change configuration values without command parameters");
    }

    if (!isDowngrade()) {
      String clusterName = commandParameters.get("clusterName");
      Cluster cluster = getClusters().getCluster(clusterName);

      if (cluster.getSecurityType() == SecurityType.KERBEROS) {
        StackId stackId;

        try {
          stackId = getTargetStackId(cluster);
        } catch (AmbariException e) {
          return createCommandReport(0, HostRoleStatus.FAILED, "{}", "", e.getLocalizedMessage());
        }

        if (stackId == null) {
          return createCommandReport(0, HostRoleStatus.FAILED, "{}", "",
              "The target stack Id was not specified.");
        }

        KerberosDescriptor kerberosDescriptor = kerberosHelper.getKerberosDescriptor(KerberosHelper.KerberosDescriptorType.COMPOSITE, cluster, stackId, true);

        // Calculate the current host-specific configurations. These will be used to replace
        // variables within the Kerberos descriptor data
        Map<String, Map<String, String>> configurations = kerberosHelper.calculateConfigurations(cluster, null, kerberosDescriptor, true, false);

        PreconfigureServiceType preconfigureServiceType = getPreconfigureServiceType(configurations);

        if (preconfigureServiceType != PreconfigureServiceType.NONE) {
          Map<String, Map<String, String>> kerberosConfigurations = new HashMap<>();
          Map<String, Set<String>> propertiesToRemove = new HashMap<>();
          Map<String, Set<String>> propertiesToIgnore = new HashMap<>();

          if (preconfigureServiceType == PreconfigureServiceType.ALL) {
            // Force all services to be flagged for pre-configuration...
            Map<String, KerberosServiceDescriptor> serviceDescriptors = kerberosDescriptor.getServices();
            if (serviceDescriptors != null) {
              for (KerberosServiceDescriptor serviceDescriptor : serviceDescriptors.values()) {
                serviceDescriptor.setPreconfigure(true);
              }
            }
          }

          processServiceComponentHosts(cluster, kerberosDescriptor, configurations, kerberosConfigurations, propertiesToIgnore, getDefaultRealm(configurations));

          // Calculate the set of configurations to update and replace any variables
          // using the previously calculated Map of configurations for the host.
          kerberosConfigurations = kerberosHelper.processPreconfiguredServiceConfigurations(kerberosConfigurations, configurations, cluster, kerberosDescriptor);

          Map<String, Set<String>> installedServices = calculateInstalledServices(cluster);

          kerberosHelper.applyStackAdvisorUpdates(cluster, installedServices.keySet(), configurations, kerberosConfigurations,
              propertiesToIgnore, propertiesToRemove, true);

          kerberosHelper.setAuthToLocalRules(cluster, kerberosDescriptor, getDefaultRealm(configurations), installedServices,
              configurations, kerberosConfigurations, true);

          processConfigurationChanges(cluster, stackId, kerberosDescriptor, kerberosConfigurations, propertiesToRemove, configurations);
        } else {
          actionLog.writeStdOut("Skipping: This facility is only available when kerberos-env/preconfigure_services is not \"NONE\"");
        }
      } else {
        actionLog.writeStdOut("Skipping: This facility is only available when Kerberos is enabled");
      }
    } else {
      actionLog.writeStdOut("Skipping: This facility is only available during an upgrade");
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }

  /**
   * Given a Cluster object creates a map of service names to sets of the installed components for that
   * service.
   *
   * @param cluster the cluster
   * @return a map of (installed) service names to the relevant set of (installed) component names
   */
  private Map<String, Set<String>> calculateInstalledServices(Cluster cluster) {
    Map<String, Set<String>> installedServices = new HashMap<>();
    Map<String, Service> services = cluster.getServices();

    for (Service service : services.values()) {
      installedServices.put(service.getName(), service.getServiceComponents().keySet());
    }

    return installedServices;
  }

  /**
   * Safely retrieves the specified property from the specified configuration type from a map of
   * configurations.
   *
   * @param configurations the existing configurations for the cluster
   * @return the requested value or null if the configuration does not exist
   */
  private String getValueFromConfiguration(Map<String, Map<String, String>> configurations, String configType, String propertyName) {
    String value = null;

    if (configurations != null) {
      Map<String, String> kerberosEnv = configurations.get(configType);

      if (kerberosEnv != null) {
        value = kerberosEnv.get(propertyName);
      }
    }

    return value;
  }

  /**
   * Safely retrieves the <code>realm</code> property of the <code>kerberos-env</code> configuration.
   *
   * @param configurations the existing configurations for the cluster
   * @return the requested value or null if the configuration does not exist
   * @see #getValueFromConfiguration(Map, String, String)
   */
  private String getDefaultRealm(Map<String, Map<String, String>> configurations) {
    return getValueFromConfiguration(configurations, KERBEROS_ENV, DEFAULT_REALM);
  }

  /**
   * Safely retrieves the <code>preconfigure_services</code> property of the <code>kerberos-env</code> configuration.
   *
   * @param configurations the existing configurations for the cluster
   * @return the requested value or null if the configuration does not exist
   * @see #getValueFromConfiguration(Map, String, String)
   */
  private PreconfigureServiceType getPreconfigureServiceType(Map<String, Map<String, String>> configurations) {
    String preconfigureServices = getValueFromConfiguration(configurations, KERBEROS_ENV, PRECONFIGURE_SERVICES);

    PreconfigureServiceType preconfigureServiceType = null;
    if (!StringUtils.isEmpty(preconfigureServices)) {
      try {
        preconfigureServiceType = PreconfigureServiceType.valueOf(preconfigureServices.toUpperCase());
      } catch (Throwable t) {
        preconfigureServiceType = PreconfigureServiceType.DEFAULT;
      }
    }

    return (preconfigureServiceType == null) ? PreconfigureServiceType.DEFAULT : preconfigureServiceType;
  }

  /**
   * Determines if upgrade direction is {@link Direction#UPGRADE} or {@link Direction#DOWNGRADE}.
   *
   * @return {@code true} if {@link Direction#DOWNGRADE}; {@code false} if {@link Direction#UPGRADE}
   */
  private boolean isDowngrade() {
    return Direction.DOWNGRADE.name().equalsIgnoreCase(getCommandParameterValue(UPGRADE_DIRECTION_KEY));
  }

  /**
   * Retrieves the target stack ID for the stack upgrade or downgrade operation.
   *
   * @param cluster the cluster
   * @return the target {@link StackId}
   * @throws AmbariException if multiple stack id's are detected
   */
  private StackId getTargetStackId(Cluster cluster) throws AmbariException {
    UpgradeContext upgradeContext = getUpgradeContext(cluster);

    // !!! FIXME in a per-service view, what does this become?
    Set<StackId> stackIds = new HashSet<>();

    for (Service service : cluster.getServices().values()) {
      RepositoryVersionEntity targetRepoVersion = upgradeContext.getTargetRepositoryVersion(service.getName());
      StackId targetStackId = targetRepoVersion.getStackId();
      stackIds.add(targetStackId);
    }

    if (1 != stackIds.size()) {
      throw new AmbariException("Services are deployed from multiple stacks and cannot determine a unique one.");
    }

    return stackIds.iterator().next();
  }

  /**
   * Find and iterate through the {@link ServiceComponentHost} objects for the current {@link Cluster}
   * to calculate property updates and auth-to-local rules.
   *
   * @param cluster                the cluster
   * @param kerberosDescriptor     the Kerberos descriptor
   * @param currentConfigurations  the current configurations for the cluster
   * @param kerberosConfigurations the (Kerberos-specific) configuration updates
   * @param propertiesToBeIgnored  a map to store properties that should be ignored by operations that update property values
   * @throws AmbariException if an issue occurs
   */
  private void processServiceComponentHosts(Cluster cluster, KerberosDescriptor kerberosDescriptor,
                                            Map<String, Map<String, String>> currentConfigurations,
                                            Map<String, Map<String, String>> kerberosConfigurations,
                                            Map<String, Set<String>> propertiesToBeIgnored, String realm)
      throws AmbariException {

    Collection<Host> hosts = cluster.getHosts();
    if (!hosts.isEmpty()) {
      // Create the context to use for filtering Kerberos Identities based on the state of the cluster
      Map<String, Object> filterContext = new HashMap<>();
      filterContext.put("configurations", currentConfigurations);
      filterContext.put("services", cluster.getServices().keySet());

      try {
        Map<String, Set<String>> propertiesToIgnore = null;
        HashMap<String, ResolvedKerberosKeytab> resolvedKeytabs = new HashMap<>();
        for (Host host : hosts) {
          // Iterate over the components installed on the current host to get the service and
          // component-level Kerberos descriptors in order to determine which principals,
          // keytab files, and configurations need to be created or updated.
          for (ServiceComponentHost sch : cluster.getServiceComponentHosts(host.getHostName())) {
            String hostName = sch.getHostName();

            String serviceName = sch.getServiceName();
            String componentName = sch.getServiceComponentName();

            KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(serviceName);

            if (!StringUtils.isEmpty(hostName)) {
              // Update the configurations with the relevant hostname
              Map<String, String> generalProperties = currentConfigurations.get("");
              if (generalProperties == null) {
                generalProperties = new HashMap<>();
                currentConfigurations.put("", generalProperties);
              }

              // Add the current hostname under "host" and "hostname"
              generalProperties.put("host", hostName);
              generalProperties.put("hostname", hostName);
            }

            if (serviceDescriptor != null) {
              List<KerberosIdentityDescriptor> serviceIdentities = serviceDescriptor.getIdentities(true, filterContext);

              // Add service-level principals (and keytabs)
              kerberosHelper.addIdentities(null, serviceIdentities,
                  null, hostName, host.getHostId(), serviceName, componentName, kerberosConfigurations, currentConfigurations,
                  resolvedKeytabs, realm);
              propertiesToIgnore = gatherPropertiesToIgnore(serviceIdentities, propertiesToIgnore);

              KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent(componentName);

              if (componentDescriptor != null) {
                List<KerberosIdentityDescriptor> componentIdentities = componentDescriptor.getIdentities(true, filterContext);

                // Calculate the set of configurations to update and replace any variables
                // using the previously calculated Map of configurations for the host.
                kerberosHelper.mergeConfigurations(kerberosConfigurations,
                    componentDescriptor.getConfigurations(true), currentConfigurations, null);

                // Add component-level principals (and keytabs)
                kerberosHelper.addIdentities(null, componentIdentities,
                    null, hostName, host.getHostId(), serviceName, componentName, kerberosConfigurations, currentConfigurations,
                    resolvedKeytabs,realm);
                propertiesToIgnore = gatherPropertiesToIgnore(componentIdentities, propertiesToIgnore);
              }
            }
          }
        }

        // Add ambari-server identities only if 'kerberos-env.create_ambari_principal = true'
        if (kerberosHelper.createAmbariIdentities(currentConfigurations.get(KERBEROS_ENV))) {
          List<KerberosIdentityDescriptor> ambariIdentities = kerberosHelper.getAmbariServerIdentities(kerberosDescriptor);

          for (KerberosIdentityDescriptor identity : ambariIdentities) {
            // If the identity represents the ambari-server user, use the component name "AMBARI_SERVER_SELF"
            // so it can be distinguished between other identities related to the AMBARI-SERVER
            // component.
            String componentName = KerberosHelper.AMBARI_SERVER_KERBEROS_IDENTITY_NAME.equals(identity.getName())
                ? "AMBARI_SERVER_SELF"
                : RootComponent.AMBARI_SERVER.name();

            List<KerberosIdentityDescriptor> componentIdentities = Collections.singletonList(identity);
            kerberosHelper.addIdentities(null, componentIdentities,
                null, KerberosHelper.AMBARI_SERVER_HOST_NAME, ambariServerHostID(), RootService.AMBARI.name(), componentName, kerberosConfigurations, currentConfigurations,
                resolvedKeytabs, realm);
            propertiesToIgnore = gatherPropertiesToIgnore(componentIdentities, propertiesToIgnore);
          }
        }

        if ((propertiesToBeIgnored != null) && (propertiesToIgnore != null)) {
          propertiesToBeIgnored.putAll(propertiesToIgnore);
        }

        // create database records for keytabs that must be presented on cluster
        for (ResolvedKerberosKeytab keytab : resolvedKeytabs.values()) {
          kerberosHelper.createResolvedKeytab(keytab);
        }
      } catch (IOException e) {
        throw new AmbariException(e.getMessage(), e);
      }
    }
  }

  private Map<String, Set<String>> gatherPropertiesToIgnore(List<KerberosIdentityDescriptor> identities,
                                                            Map<String, Set<String>> propertiesToIgnore) {
    Map<String, Map<String, String>> identityConfigurations = kerberosHelper.getIdentityConfigurations(identities);
    if (!MapUtils.isEmpty(identityConfigurations)) {
      if (propertiesToIgnore == null) {
        propertiesToIgnore = new HashMap<>();
      }

      for (Map.Entry<String, Map<String, String>> entry : identityConfigurations.entrySet()) {
        String configType = entry.getKey();
        Map<String, String> properties = entry.getValue();

        if (MapUtils.isEmpty(properties)) {
          Set<String> propertyNames = propertiesToIgnore.get(configType);
          if (propertyNames == null) {
            propertyNames = new HashSet<>();
            propertiesToIgnore.put(configType, propertyNames);
          }
          propertyNames.addAll(properties.keySet());
        }
      }
    }

    return propertiesToIgnore;
  }

  /**
   * Processes configuration changes to determine if any work needs to be done.
   * <p/>
   * If work is to be done, a data file containing the details is created so it they changes may be
   * processed in the appropriate stage.
   *
   * @param cluster                the cluster
   * @param targetStackId          the target stack id
   * @param kerberosConfigurations the Kerberos-specific configuration map
   * @param propertiesToBeRemoved  a map of properties to be removed from the current configuration,
   *                               grouped by configuration type.
   * @param variableReplaments     replacement values to use when attempting to perform variable replacements on the property names
   * @throws AmbariException if an issue is encountered
   */
  private void processConfigurationChanges(Cluster cluster, StackId targetStackId,
                                           KerberosDescriptor kerberosDescriptor,
                                           Map<String, Map<String, String>> kerberosConfigurations,
                                           Map<String, Set<String>> propertiesToBeRemoved,
                                           Map<String, Map<String, String>> variableReplaments)
      throws AmbariException {
    actionLog.writeStdOut("Determining configuration changes");

    if (!kerberosConfigurations.isEmpty()) {
      Map<String, Service> installedServices = cluster.getServices();

      // Build a map of configuration types to properties that indicate which properties should be altered
      // This map should contain only properties defined in service-level Kerberos descriptors that
      // have been flagged to be preconfigured and that have not yet been installed.
      Map<String, Set<String>> propertyFilter = new HashMap<>();
      Map<String, KerberosServiceDescriptor> serviceDescriptors = kerberosDescriptor.getServices();
      if (serviceDescriptors != null) {
        for (KerberosServiceDescriptor serviceDescriptor : serviceDescriptors.values()) {
          if (!installedServices.containsKey(serviceDescriptor.getName()) && serviceDescriptor.shouldPreconfigure()) {
            buildFilter(Collections.singleton(serviceDescriptor), propertyFilter, variableReplaments);
          }
        }
      }

      // Add the auth-to-local rule configuration specifications to the filter
      Map<String, Set<String>> authToLocalProperties = kerberosHelper.translateConfigurationSpecifications(kerberosDescriptor.getAllAuthToLocalProperties());
      if (!MapUtils.isEmpty(authToLocalProperties)) {
        for (Map.Entry<String, Set<String>> entry : authToLocalProperties.entrySet()) {
          Set<String> properties = entry.getValue();

          if (!CollectionUtils.isEmpty(properties)) {
            String configurationType = entry.getKey();

            Set<String> propertyNames = propertyFilter.get(configurationType);
            if (propertyNames == null) {
              propertyNames = new HashSet<>();
              propertyFilter.put(configurationType, propertyNames);
            }

            propertyNames.addAll(properties);
          }
        }
      }

      Set<String> visitedTypes = new HashSet<>();

      for (Map.Entry<String, Map<String, String>> entry : kerberosConfigurations.entrySet()) {
        String configType = entry.getKey();

        String service = cluster.getServiceByConfigType(configType);
        Set<String> allowedProperties = propertyFilter.get(configType);

        // Update properties for services that are installed and not filtered out
        if (installedServices.containsKey(service) && !CollectionUtils.isEmpty(allowedProperties)) {
          Map<String, String> propertiesToUpdate = entry.getValue();
          Set<String> propertiesToRemove = (propertiesToBeRemoved == null) ? null : propertiesToBeRemoved.get(configType);

          // Filter the properties to update
          if (propertiesToUpdate != null) {
            Iterator<Map.Entry<String, String>> mapIterator = propertiesToUpdate.entrySet().iterator();
            while (mapIterator.hasNext()) {
              Map.Entry<String, String> mapEntry = mapIterator.next();

              if (!allowedProperties.contains(mapEntry.getKey())) {
                mapIterator.remove();
              }
            }
          }

          // Filter the properties to remove
          if (propertiesToRemove != null) {
            Iterator<String> setIterator = propertiesToRemove.iterator();
            while (setIterator.hasNext()) {
              String setEntry = setIterator.next();
              if (!allowedProperties.contains(setEntry)) {
                setIterator.remove();
              }
            }
          }

          visitedTypes.add(configType);

          if (!MapUtils.isEmpty(propertiesToUpdate) || !CollectionUtils.isEmpty(propertiesToRemove)) {
            if (!MapUtils.isEmpty(propertiesToUpdate)) {
              for (Map.Entry<String, String> property : propertiesToUpdate.entrySet()) {
                actionLog.writeStdOut(String.format("Setting: %s/%s = %s", configType, property.getKey(), property.getValue()));
              }
            }

            if (!CollectionUtils.isEmpty(propertiesToRemove)) {
              for (String property : propertiesToRemove) {
                actionLog.writeStdOut(String.format("Removing: %s/%s", configType, property));
              }
            }

            configHelper.updateConfigType(cluster, targetStackId,
                ambariManagementController, configType, propertiesToUpdate, propertiesToRemove,
                ambariManagementController.getAuthName(), "Preconfiguring for Kerberos during upgrade");
          }
        }
      }

      if (!MapUtils.isEmpty(propertiesToBeRemoved)) {
        for (Map.Entry<String, Set<String>> entry : propertiesToBeRemoved.entrySet()) {
          String configType = entry.getKey();

          if (!visitedTypes.contains(configType)) {
            Set<String> propertiesToRemove = entry.getValue();

            if (!CollectionUtils.isEmpty(propertiesToRemove)) {
              for (String property : propertiesToRemove) {
                actionLog.writeStdOut(String.format("Removing: %s/%s", configType, property));
              }

              configHelper.updateConfigType(cluster, targetStackId,
                  ambariManagementController, configType, null, entry.getValue(),
                  ambariManagementController.getAuthName(), "Preconfiguring for Kerberos during upgrade");
            }
          }
        }
      }
    }
  }

  /**
   * Adds entries to the property filter (<code>propertyFilter</code>) found in the {@link KerberosConfigurationDescriptor}s
   * within the specified node of the Kerberos descriptor.
   *
   * @param containers     the Kerberos descriptor containers to process
   * @param propertyFilter the property filter map to update
   * @param replacements   replacement values to use when attempting to perform variable replacements on the property names
   * @throws AmbariException if an issue occurs while replacing variables in the property names
   */
  private void buildFilter(Collection<? extends AbstractKerberosDescriptorContainer> containers,
                           Map<String, Set<String>> propertyFilter,
                           Map<String, Map<String, String>> replacements)
      throws AmbariException {
    if (containers != null) {
      for (AbstractKerberosDescriptorContainer container : containers) {
        Map<String, KerberosConfigurationDescriptor> configurationDescriptors = container.getConfigurations(false);

        if (!MapUtils.isEmpty(configurationDescriptors)) {
          for (KerberosConfigurationDescriptor configurationDescriptor : configurationDescriptors.values()) {
            Map<String, String> properties = configurationDescriptor.getProperties();

            if (!MapUtils.isEmpty(properties)) {
              String configType = configurationDescriptor.getType();

              Set<String> propertyNames = propertyFilter.get(configType);
              if (propertyNames == null) {
                propertyNames = new HashSet<>();
                propertyFilter.put(configType, propertyNames);
              }

              // Replace variables in the property name. For example ${knox-env/knox_user}.
              for (String propertyName : properties.keySet()) {
                propertyNames.add(variableReplacementHelper.replaceVariables(propertyName, replacements));
              }
            }
          }
        }

        Collection<? extends AbstractKerberosDescriptorContainer> childContainers = container.getChildContainers();
        if (childContainers != null) {
          buildFilter(childContainers, propertyFilter, replacements);
        }
      }
    }
  }

  protected Long ambariServerHostID(){
    String ambariServerHostName = StageUtils.getHostName();
    HostEntity ambariServerHostEntity = hostDAO.findByName(ambariServerHostName);
    return (ambariServerHostEntity == null)
        ? null
        : ambariServerHostEntity.getHostId();
  }

}

