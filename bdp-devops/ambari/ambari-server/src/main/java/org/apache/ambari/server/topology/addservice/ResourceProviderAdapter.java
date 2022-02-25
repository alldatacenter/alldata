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
import static org.apache.ambari.server.controller.internal.RequestResourceProvider.CONTEXT;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.internal.ArtifactResourceProvider;
import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
import org.apache.ambari.server.controller.internal.CredentialResourceProvider;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.topology.Credential;
import org.apache.ambari.server.topology.ProvisionStep;
import org.apache.ambari.server.utils.LoggingPreconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Creates resources using the resource providers.
 * Translates {@link AddServiceInfo} to internal requests accepted by those.
 */
@Singleton
public class ResourceProviderAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceProviderAdapter.class);
  private static final LoggingPreconditions CHECK = new LoggingPreconditions(LOG);

  private final KerberosDescriptorFactory descriptorFactory = new KerberosDescriptorFactory();

  @Inject
  private AmbariManagementController controller;

  // business methods

  public void createServices(AddServiceInfo request) {
    LOG.info("Creating service resources for {}", request);

    Set<Map<String, Object>> properties = request.newServices().keySet().stream()
      .map(service -> createServiceRequestProperties(request, service))
      .collect(toSet());

    createResources(request, Resource.Type.Service, properties, null, false);
  }

  public void createComponents(AddServiceInfo request) {
    LOG.info("Creating component resources for {}", request);

    Set<Map<String, Object>> properties = request.newServices().entrySet().stream()
      .flatMap(componentsOfService -> componentsOfService.getValue().keySet().stream()
        .map(component -> createComponentRequestProperties(request, componentsOfService.getKey(), component)))
      .collect(toSet());

    createResources(request, Resource.Type.Component, properties, null, false);
  }

  public void createHostComponents(AddServiceInfo request) {
    LOG.info("Creating host component resources for {}", request);

    Set<Map<String, Object>> properties = request.newServices().entrySet().stream()
      .flatMap(componentsOfService -> componentsOfService.getValue().entrySet().stream()
        .flatMap(hostsOfComponent -> hostsOfComponent.getValue().stream()
          .map(host -> createHostComponentRequestProperties(request, componentsOfService.getKey(), hostsOfComponent.getKey(), host))))
      .collect(toSet());

    createResources(request, Resource.Type.HostComponent, properties, null, false);
  }

  public void createConfigs(AddServiceInfo request) {
    LOG.info("Creating configurations for {}", request);
    Set<ClusterRequest> requests = createConfigRequestsForNewServices(request);
    updateCluster(request, requests, "Error creating configurations for %s");
  }

  public void createCredentials(AddServiceInfo request) {
    if (!request.getRequest().getCredentials().isEmpty()) {
      LOG.info("Creating {} credential(s) for {}", request.getRequest().getCredentials().size(), request);

      request.getRequest().getCredentials().values().stream()
        .peek(credential -> LOG.debug("Creating credential {}", credential))
        .map(credential -> createCredentialRequestProperties(request.clusterName(), credential))
        .forEach(
          properties -> createResources(request, Resource.Type.Credential, ImmutableSet.of(properties), null, true)
        );
    }
  }

  /**
   * @return {@code Optional} with the cluster's kerberos descriptor artifact if it exists, otherwise empty {@code Optional}
   */
  public Optional<KerberosDescriptor> getKerberosDescriptor(AddServiceInfo request) {
    Set<String> propertyIds = ImmutableSet.of(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY);
    Predicate predicate = predicateForKerberosDescriptorArtifact(request.clusterName());

    Set<Resource> resources = getResources(request, propertyIds, Resource.Type.Artifact, predicate);

    if (resources == null || resources.isEmpty()) {
      return Optional.empty();
    }

    CHECK.checkArgument(resources.size() == 1,
      "Expected only one artifact of type %s, but got %d",
      ArtifactResourceProvider.KERBEROS_DESCRIPTOR,
      resources.size()
    );

    return Optional.of(descriptorFactory.createInstance(resources.iterator().next().getPropertiesMap().get(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY)));
  }

  public void createKerberosDescriptor(AddServiceInfo request, KerberosDescriptor descriptor) {
    LOG.info("Creating Kerberos descriptor for {}", request);
    Map<String, Object> properties = createKerberosDescriptorRequestProperties(request.clusterName());
    Map<String, String> requestInfo = requestInfoForKerberosDescriptor(descriptor);
    createResources(request, Resource.Type.Artifact, ImmutableSet.of(properties), requestInfo, false);
  }

  public void updateKerberosDescriptor(AddServiceInfo request, KerberosDescriptor descriptor) {
    LOG.info("Updating Kerberos descriptor from {}", request);
    Map<String, Object> properties = createKerberosDescriptorRequestProperties(request.clusterName());
    Map<String, String> requestInfo = requestInfoForKerberosDescriptor(descriptor);
    Predicate predicate = predicateForKerberosDescriptorArtifact(request.clusterName());
    updateResources(request, ImmutableSet.of(properties), Resource.Type.Artifact, predicate, requestInfo);
  }

  public void updateExistingConfigs(AddServiceInfo request, Set<String> existingServices) {
    LOG.info("Updating existing configurations for {}", request);
    Set<ClusterRequest> requests = createConfigRequestsForExistingServices(request, existingServices);
    updateCluster(request, requests, "Error updating configurations for %s");
  }

  public void updateServiceDesiredState(AddServiceInfo request, State desiredState) {
    LOG.info("Updating service desired state to {} for {}", desiredState, request);

    Set<Map<String, Object>> properties = ImmutableSet.of(ImmutableMap.of(
      ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, desiredState.name()
    ));
    Map<String, String> requestInfo = RequestOperationLevel.propertiesFor(Resource.Type.Service, request.clusterName());
    Predicate predicate = predicateForNewServices(request);
    updateResources(request, properties, Resource.Type.Service, predicate, requestInfo);
  }

  public void updateHostComponentDesiredState(AddServiceInfo request, Predicate predicate, ProvisionStep step) {
    State desiredState = step.getDesiredStateToSet();
    LOG.info("Updating host component desired state to {} per {} for {}", desiredState, step, request);
    LOG.debug("Using predicate {}", predicate);

    Set<Map<String, Object>> properties = ImmutableSet.of(ImmutableMap.of(
      HostComponentResourceProvider.STATE, desiredState.name(),
      CONTEXT, String.format("Put new components to %s state", desiredState)
    ));

    Map<String, String> requestInfo = new ImmutableMap.Builder<String, String>()
      .putAll(RequestOperationLevel.propertiesFor(Resource.Type.HostComponent, request.clusterName()))
      .putAll(step.getProvisionProperties())
      .build();

    HostComponentResourceProvider rp = (HostComponentResourceProvider) getClusterController().ensureResourceProvider(Resource.Type.HostComponent);
    Request internalRequest = createRequest(properties, requestInfo, null);
    try {
      rp.doUpdateResources(request.getStages(), internalRequest, predicate, false, false, false);
    } catch (UnsupportedPropertyException | SystemException | NoSuchParentResourceException | NoSuchResourceException e) {
      CHECK.wrapInUnchecked(e, RuntimeException::new, "Error updating host component desired state for %s", request);
    }
  }

  // ResourceProvider calls

  private static Set<Resource> getResources(AddServiceInfo request, Set<String> propertyIds, Resource.Type resourceType, Predicate predicate) {
    Request internalRequest = createRequest(null, null, propertyIds);
    ResourceProvider rp = getClusterController().ensureResourceProvider(resourceType);
    try {
      return rp.getResources(internalRequest, predicate);
    } catch (NoSuchResourceException e) {
      return ImmutableSet.of();
    } catch (UnsupportedPropertyException | SystemException | NoSuchParentResourceException e) {
      return CHECK.wrapInUnchecked(e, RuntimeException::new, "Error getting resources %s for %s", resourceType, request);
    }
  }

  private static void createResources(AddServiceInfo request, Resource.Type resourceType, Set<Map<String, Object>> properties, Map<String, String> requestInfo, boolean okIfExists) {
    Request internalRequest = createRequest(properties, requestInfo, null);
    ResourceProvider rp = getClusterController().ensureResourceProvider(resourceType);
    try {
      rp.createResources(internalRequest);
    } catch (UnsupportedPropertyException | SystemException | ResourceAlreadyExistsException | NoSuchParentResourceException e) {
      if (okIfExists && e instanceof ResourceAlreadyExistsException) {
        LOG.info("Resource already exists: {}, no need to create", e.getMessage());
      } else {
        CHECK.wrapInUnchecked(e, RuntimeException::new, "Error creating resources %s for %s", resourceType, request);
      }
    }
  }

  private static void updateResources(AddServiceInfo request, Set<Map<String, Object>> properties, Resource.Type resourceType, Predicate predicate, Map<String, String> requestInfo) {
    Request internalRequest = createRequest(properties, requestInfo, null);
    ResourceProvider rp = getClusterController().ensureResourceProvider(resourceType);
    try {
      rp.updateResources(internalRequest, predicate);
    } catch (UnsupportedPropertyException | SystemException | NoSuchParentResourceException | NoSuchResourceException e) {
      CHECK.wrapInUnchecked(e, RuntimeException::new, "Error updating resources %s for %s", resourceType, request);
    }
  }

  private void updateCluster(AddServiceInfo addServiceRequest, Set<ClusterRequest> requests, String errorMessageFormat) {
    try {
      controller.updateClusters(requests, null);
    } catch (AmbariException | AuthorizationException e) {
      CHECK.wrapInUnchecked(e, RuntimeException::new, errorMessageFormat, addServiceRequest);
    }
  }

  private static Request createRequest(Set<Map<String, Object>> properties, Map<String, String> requestInfoProperties, Set<String> propertyIds) {
    return new RequestImpl(propertyIds, properties, requestInfoProperties, null);
  }

  public static Map<String, String> requestInfoForKerberosDescriptor(KerberosDescriptor descriptor) {
    return ImmutableMap.of(Request.REQUEST_INFO_BODY_PROPERTY, ArtifactResourceProvider.toArtifactDataJson(descriptor.toMap()));
  }

  // request creation (as map of properties)

  private static Map<String, Object> createServiceRequestProperties(AddServiceInfo request, String service) {
    ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();

    properties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, request.clusterName());
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, service);
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, State.INIT.name());

    return properties.build();
  }

  private static Map<String, Object> createComponentRequestProperties(AddServiceInfo request, String service, String component) {
    ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();

    properties.put(ComponentResourceProvider.CLUSTER_NAME, request.clusterName());
    properties.put(ComponentResourceProvider.SERVICE_NAME, service);
    properties.put(ComponentResourceProvider.COMPONENT_NAME, component);
    properties.put(ComponentResourceProvider.STATE, State.INIT.name());

    return properties.build();
  }

  private static Map<String, Object> createHostComponentRequestProperties(AddServiceInfo request, String service, String component, String host) {
    ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();

    properties.put(HostComponentResourceProvider.CLUSTER_NAME, request.clusterName());
    properties.put(HostComponentResourceProvider.SERVICE_NAME, service);
    properties.put(HostComponentResourceProvider.COMPONENT_NAME, component);
    properties.put(HostComponentResourceProvider.HOST_NAME, host);
    properties.put(HostComponentResourceProvider.STATE, State.INIT.name());

    return properties.build();
  }

  public static Map<String, Object> createCredentialRequestProperties(String clusterName, Credential credential) {
    ImmutableMap.Builder<String, Object> properties = ImmutableMap.builder();

    properties.put(CredentialResourceProvider.CREDENTIAL_CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(CredentialResourceProvider.CREDENTIAL_ALIAS_PROPERTY_ID, credential.getAlias());
    properties.put(CredentialResourceProvider.CREDENTIAL_PRINCIPAL_PROPERTY_ID, credential.getPrincipal());
    properties.put(CredentialResourceProvider.CREDENTIAL_KEY_PROPERTY_ID, credential.getKey());
    properties.put(CredentialResourceProvider.CREDENTIAL_TYPE_PROPERTY_ID, credential.getType().name());

    return properties.build();
  }

  public static Map<String, Object> createKerberosDescriptorRequestProperties(String clusterName) {
    return ImmutableMap.of(
      ArtifactResourceProvider.CLUSTER_NAME_PROPERTY, clusterName,
      ArtifactResourceProvider.ARTIFACT_NAME_PROPERTY, ArtifactResourceProvider.KERBEROS_DESCRIPTOR
    );
  }

  // ClusterRequest creation (for configuration)

  private static Set<ClusterRequest> createConfigRequestsForNewServices(AddServiceInfo request) {
    Map<String, Map<String, String>> fullProperties = request.getConfig().getFullProperties();
    Map<String, Map<String, Map<String, String>>> fullAttributes = request.getConfig().getFullAttributes();

    return createConfigRequestsForServices(
      request.newServices().keySet(),
      configType -> !Objects.equals(configType, ConfigHelper.CLUSTER_ENV),
      request, fullProperties, fullAttributes
    );
  }

  private Set<ClusterRequest> createConfigRequestsForExistingServices(AddServiceInfo request, Set<String> existingServices) {
    Set<String> configTypesInRequest = ImmutableSet.copyOf(
      Sets.difference(
        Sets.union(
          request.getConfig().getProperties().keySet(),
          request.getConfig().getAttributes().keySet()),
        ImmutableSet.of(ConfigHelper.CLUSTER_ENV))
    );

    Map<String, Map<String, String>> fullProperties = request.getConfig().getFullProperties();
    Map<String, Map<String, Map<String, String>>> fullAttributes = request.getConfig().getFullAttributes();

    Set<ClusterRequest> clusterRequests = createConfigRequestsForServices(
      existingServices,
      configTypesInRequest::contains,
      request, fullProperties, fullAttributes
    );

    if (request.getConfig().getProperties().containsKey(ConfigHelper.CLUSTER_ENV)) {
      Optional<ClusterRequest> clusterEnvRequest = createConfigRequestForConfigTypes(Stream.of(ConfigHelper.CLUSTER_ENV),
        request, fullProperties, fullAttributes);
      clusterEnvRequest.ifPresent(clusterRequests::add);
    }

    return clusterRequests;
  }

  private static Set<ClusterRequest> createConfigRequestsForServices(
    Set<String> services,
    java.util.function.Predicate<String> predicate,
    AddServiceInfo request,
    Map<String, Map<String, String>> fullProperties,
    Map<String, Map<String, Map<String, String>>> fullAttributes
  ) {
    return services.stream()
      .map(service -> createConfigRequestForConfigTypes(
        request.getStack().getConfigurationTypes(service).stream()
          .filter(predicate),
        request, fullProperties, fullAttributes
      ))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toSet());
  }

  /**
   * Creates a {@link ConfigurationRequest} for each config type in the {@code configTypes} stream.
   *
   * @return an {@code Optional} {@link ClusterRequest} with desired configs set to all the {@code ConfigurationRequests},
   * or an empty {@code Optional} if the incoming {@code configTypes} stream is empty
   */
  private static Optional<ClusterRequest> createConfigRequestForConfigTypes(
    Stream<String> configTypes,
    AddServiceInfo addServiceRequest,
    Map<String, Map<String, String>> fullProperties,
    Map<String, Map<String, Map<String, String>>> fullAttributes
  ) {
    List<ConfigurationRequest> configRequests = configTypes
      .peek(configType -> LOG.info("Creating request for config type {} for {}", configType, addServiceRequest))
      .map(configType -> new ConfigurationRequest(addServiceRequest.clusterName(), configType, "ADD_SERVICE_" + System.currentTimeMillis(),
        fullProperties.getOrDefault(configType, ImmutableMap.of()),
        fullAttributes.getOrDefault(configType, ImmutableMap.of())))
      .collect(toList());

    if (configRequests.isEmpty()) {
      return Optional.empty();
    }

    ClusterRequest clusterRequest = new ClusterRequest(null, addServiceRequest.clusterName(), null, null);
    clusterRequest.setDesiredConfig(configRequests);
    return Optional.of(clusterRequest);
  }

  // Predicate creation

  public static Predicate predicateForKerberosDescriptorArtifact(String clusterName) {
    return new PredicateBuilder().begin()
      .property(ArtifactResourceProvider.CLUSTER_NAME_PROPERTY).equals(clusterName)
      .and()
      .property(ArtifactResourceProvider.ARTIFACT_NAME_PROPERTY).equals(ArtifactResourceProvider.KERBEROS_DESCRIPTOR)
      .end().toPredicate();
  }

  private static Predicate predicateForNewServices(AddServiceInfo request) {
    return new AndPredicate(
      new EqualsPredicate<>(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, request.clusterName()),
      OrPredicate.of(
        request.newServices().keySet().stream()
          .map(service -> new EqualsPredicate<>(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, service))
          .collect(toList())
      )
    );
  }

  // TODO should be injected
  private static ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }

}
