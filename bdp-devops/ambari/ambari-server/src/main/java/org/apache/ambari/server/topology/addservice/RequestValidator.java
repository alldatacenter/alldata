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
package org.apache.ambari.server.topology.addservice;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.internal.UnitUpdater;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.topology.StackFactory;
import org.apache.ambari.server.utils.LoggingPreconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;

/**
 * Validates a specific {@link AddServiceRequest}.
 */
public class RequestValidator {

  private static final Logger LOG = LoggerFactory.getLogger(RequestValidator.class);
  private static final LoggingPreconditions CHECK = new LoggingPreconditions(LOG);

  private static final Set<String> NOT_ALLOWED_CONFIG_TYPES = ImmutableSet.of("kerberos-env", "krb5-conf");

  private final AddServiceRequest request;
  private final Cluster cluster;
  private final AmbariManagementController controller;
  private final ConfigHelper configHelper;
  private final StackFactory stackFactory;
  private final KerberosDescriptorFactory kerberosDescriptorFactory;
  private final AtomicBoolean serviceInfoCreated = new AtomicBoolean();
  private final SecurityConfigurationFactory securityConfigurationFactory;

  private State state;

  @Inject
  public RequestValidator(
    @Assisted AddServiceRequest request, @Assisted Cluster cluster,
    AmbariManagementController controller, ConfigHelper configHelper,
    StackFactory stackFactory, KerberosDescriptorFactory kerberosDescriptorFactory,
    SecurityConfigurationFactory securityConfigurationFactory
  ) {
    this.state = State.INITIAL;
    this.request = request;
    this.cluster = cluster;
    this.controller = controller;
    this.configHelper = configHelper;
    this.stackFactory = stackFactory;
    this.kerberosDescriptorFactory = kerberosDescriptorFactory;
    this.securityConfigurationFactory = securityConfigurationFactory;
  }

  /**
   * Perform validation of the request.
   */
  void validate() {
    validateStack();
    validateServicesAndComponents();
    validateSecurity();
    validateHosts();
    validateConfiguration();
  }

  /**
   * Create an {@link AddServiceInfo} based on the validated request.
   */
  AddServiceInfo createValidServiceInfo(ActionManager actionManager, RequestFactory requestFactory) {
    final State state = this.state;

    CHECK.checkState(state.isValid(), "The request needs to be validated first");
    CHECK.checkState(!serviceInfoCreated.getAndSet(true), "Can create only one instance for each validated add service request");

    RequestStageContainer stages = new RequestStageContainer(actionManager.getNextRequestId(), null, requestFactory, actionManager);
    AddServiceInfo validatedRequest = new AddServiceInfo.Builder()
      .setRequest(request)
      .setClusterName(cluster.getClusterName())
      .setStages(stages)
      .setStack(state.getStack())
      .setConfig(state.getConfig())
      .setNewServices(state.getNewServices())
      .setKerberosDescriptor(state.getKerberosDescriptor())
      .build();
    stages.setRequestContext(validatedRequest.describe());
    return validatedRequest;
  }

  @VisibleForTesting
  State getState() {
    return state;
  }

  @VisibleForTesting
  void setState(State state) {
    this.state = state;
  }

  @VisibleForTesting
  void validateSecurity() {
    request.getSecurity().ifPresent(requestSecurity -> {
      CHECK.checkArgument(!strictValidation() || requestSecurity.getType() == cluster.getSecurityType(),
        "Security type in the request (%s), if specified, should match cluster's security type (%s)",
        requestSecurity.getType(), cluster.getSecurityType()
      );

      boolean hasDescriptor = requestSecurity.getDescriptor().isPresent();
      boolean hasDescriptorReference = requestSecurity.getDescriptorReference() != null;
      boolean secureCluster = cluster.getSecurityType() == SecurityType.KERBEROS;

      CHECK.checkArgument(secureCluster || !hasDescriptor,
        "Kerberos descriptor cannot be set for security type %s", cluster.getSecurityType());
      CHECK.checkArgument(secureCluster || !hasDescriptorReference,
        "Kerberos descriptor reference cannot be set for security type %s", cluster.getSecurityType());
      CHECK.checkArgument(!hasDescriptor || !hasDescriptorReference,
        "Kerberos descriptor and reference cannot be both set");

      Optional<Map<?,?>> kerberosDescriptor = hasDescriptor
        ? requestSecurity.getDescriptor()
        : hasDescriptorReference
          ? loadKerberosDescriptor(requestSecurity.getDescriptorReference())
          : Optional.empty();

      kerberosDescriptor.ifPresent(descriptorMap -> {
        CHECK.checkState(state.getNewServices() != null,
          "Services need to be validated before security settings");

        KerberosDescriptor descriptor = kerberosDescriptorFactory.createInstance(descriptorMap);

        if (strictValidation()) {
          Map<String, KerberosServiceDescriptor> descriptorServices = descriptor.getServices();
          Set<String> servicesWithNewDescriptor = descriptorServices != null ? descriptorServices.keySet() : ImmutableSet.of();
          Set<String> newServices = state.getNewServices().keySet();
          Set<String> nonNewServices = ImmutableSet.copyOf(Sets.difference(servicesWithNewDescriptor, newServices));

          CHECK.checkArgument(nonNewServices.isEmpty(),
            "Kerberos descriptor should be provided only for new services, but found other services: %s",
            nonNewServices
          );
        }

        try {
          descriptor.toMap();
        } catch (Exception e) {
          CHECK.wrapInUnchecked(e, IllegalArgumentException::new, "Error validating Kerberos descriptor: %s", e);
        }

        state = state.with(descriptor);
      });
    });
  }

  @VisibleForTesting
  void validateStack() {
    Optional<StackId> requestStackId = request.getStackId();
    StackId stackId = requestStackId.orElseGet(cluster::getCurrentStackVersion);
    try {
      Stack stack = stackFactory.createStack(stackId.getStackName(), stackId.getStackVersion(), controller);
      state = state.with(stack);
    } catch (AmbariException e) {
      CHECK.wrapInUnchecked(e,
        requestStackId.isPresent() ? IllegalArgumentException::new : IllegalStateException::new,
        "Stack %s not found", stackId
      );
    }
  }

  @VisibleForTesting
  void validateServicesAndComponents() {
    Stack stack = state.getStack();
    Map<String, Map<String, Set<String>>> newServices = new LinkedHashMap<>();
    Map<String, Map<String, Multiset<String>>> withAllHosts = new LinkedHashMap<>();

    Set<String> existingServices = cluster.getServices().keySet();

    // process service declarations
    for (Service service : request.getServices()) {
      String serviceName = service.getName();

      CHECK.checkArgument(stack.getServices().contains(serviceName),
        "Unknown service %s in %s", service, stack);
      CHECK.checkArgument(!existingServices.contains(serviceName),
        "Service %s already exists in cluster %s", serviceName, cluster.getClusterName());

      newServices.computeIfAbsent(serviceName, __ -> new HashMap<>());
    }

    // process component declarations
    for (Component requestedComponent : request.getComponents()) {
      String componentName = requestedComponent.getName();
      String serviceName = stack.getServiceForComponent(componentName);

      CHECK.checkArgument(serviceName != null,
        "No service found for component %s in %s", componentName, stack);
      CHECK.checkArgument(!existingServices.contains(serviceName),
        "Service %s (for component %s) already exists in cluster %s", serviceName, componentName, cluster.getClusterName());

      List<String> hosts = requestedComponent.getHosts().stream().map(Host::getFqdn).collect(toList());
      newServices.computeIfAbsent(serviceName, __ -> new HashMap<>())
        .computeIfAbsent(componentName, __ -> new HashSet<>())
        .addAll(hosts);
      withAllHosts.computeIfAbsent(serviceName, __ -> new HashMap<>())
        .computeIfAbsent(componentName, __ -> HashMultiset.create())
        .addAll(hosts);
    }

    CHECK.checkArgument(!newServices.isEmpty(), "Request should have at least one new service or component to be added");

    newServices.forEach(
      (service, components) -> components.forEach(
        (component, hosts) -> {
          Multiset<String> allHosts = withAllHosts.get(service).get(component);
          Multisets.removeOccurrences(allHosts, hosts);
          CHECK.checkArgument(allHosts.isEmpty(), "Some hosts appear multiple times for the same component (%s) in the request: %s", component, allHosts);
        }
      )
    );

    state = state.withNewServices(newServices);
  }

  @VisibleForTesting
  void validateConfiguration() {
    Configuration config = request.getConfiguration();

    if (strictValidation()) {
      for (String type : NOT_ALLOWED_CONFIG_TYPES) {
        CHECK.checkArgument(!config.getProperties().containsKey(type), "Cannot change '%s' configuration in Add Service request", type);
      }
    }

    Configuration clusterConfig = getClusterDesiredConfigs();
    clusterConfig.setParentConfiguration(state.getStack().getDefaultConfig());
    config.setParentConfiguration(clusterConfig);

    UnitUpdater.removeUnits(config, state.getStack()); // stack advisor doesn't like units; they'll be added back after recommendation
    state = state.with(config);
  }

  @VisibleForTesting
  void validateHosts() {
    Set<String> clusterHosts = cluster.getHostNames();
    Set<String> requestHosts = state.getNewServices().values().stream()
      .flatMap(componentHosts -> componentHosts.values().stream())
      .flatMap(Collection::stream)
      .collect(toSet());
    Set<String> unknownHosts = new TreeSet<>(Sets.difference(requestHosts, clusterHosts));

    CHECK.checkArgument(unknownHosts.isEmpty(),
      "Requested host not associated with cluster %s: %s", cluster.getClusterName(), unknownHosts);
  }

  private boolean strictValidation() {
    return request.getValidationType().strictValidation();
  }

  private Configuration getClusterDesiredConfigs() {
    try {
      return Configuration.of(configHelper.calculateExistingConfigs(cluster));
    } catch (AmbariException e) {
      return CHECK.wrapInUnchecked(e, IllegalStateException::new, "Error getting effective configuration of cluster %s", cluster.getClusterName());
    }
  }

  private Optional<Map<?,?>> loadKerberosDescriptor(String descriptorReference) {
    return securityConfigurationFactory.loadSecurityConfigurationByReference(descriptorReference).getDescriptor();
  }

  @VisibleForTesting
  static class State {

    static final State INITIAL = new State(null, null, null, null);

    private final Stack stack;
    private final Map<String, Map<String, Set<String>>> newServices;
    private final Configuration config;
    private final KerberosDescriptor kerberosDescriptor;

    State(Stack stack, Map<String, Map<String, Set<String>>> newServices, Configuration config, KerberosDescriptor kerberosDescriptor) {
      this.stack = stack;
      this.newServices = newServices;
      this.config = config;
      this.kerberosDescriptor = kerberosDescriptor;
    }

    boolean isValid() {
      return stack != null && newServices != null && config != null;
    }

    State with(Stack stack) {
      return new State(stack, newServices, config, kerberosDescriptor);
    }

    State withNewServices(Map<String, Map<String, Set<String>>> newServices) {
      return new State(stack, newServices, config, kerberosDescriptor);
    }

    State with(Configuration config) {
      return new State(stack, newServices, config, kerberosDescriptor);
    }

    State with(KerberosDescriptor kerberosDescriptor) {
      return new State(stack, newServices, config, kerberosDescriptor);
    }

    Stack getStack() {
      return stack;
    }

    Map<String, Map<String, Set<String>>> getNewServices() {
      return newServices;
    }

    Configuration getConfig() {
      return config;
    }

    KerberosDescriptor getKerberosDescriptor() {
      return kerberosDescriptor;
    }
  }

}
