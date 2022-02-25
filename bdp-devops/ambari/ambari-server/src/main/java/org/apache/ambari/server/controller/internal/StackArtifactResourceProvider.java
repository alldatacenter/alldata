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

package org.apache.ambari.server.controller.internal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptorFactory;
import org.apache.ambari.server.state.stack.Metric;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.ambari.server.state.stack.WidgetLayout;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

/**
 * Provider for stack and stack service artifacts.
 * Artifacts contain an artifact name as the PK and artifact data in the form of
 * a map which is the content of the artifact.
 * <p>
 * An example of an artifact is a kerberos descriptor.
 * <p>
 * Stack artifacts are part of the stack definition and therefore can't
 * be created, updated or deleted.
 */
@StaticallyInject
public class StackArtifactResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(StackArtifactResourceProvider.class);

  /**
   * stack name
   */
  public static final String STACK_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Artifacts", "stack_name");

  /**
   * stack version
   */
  public static final String STACK_VERSION_PROPERTY_ID =
      PropertyHelper.getPropertyId("Artifacts", "stack_version");

  /**
   * stack service name
   */
  public static final String STACK_SERVICE_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Artifacts", "service_name");

  /**
   * stack service name
   */
  public static final String STACK_COMPONENT_NAME_PROPERTY_ID =
    PropertyHelper.getPropertyId("Artifacts", "component_name");

  /**
   * artifact name
   */
  public static final String ARTIFACT_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Artifacts", "artifact_name");

  /**
   * artifact data
   */
  public static final String ARTIFACT_DATA_PROPERTY_ID = "artifact_data";

  /**
   * primary key fields
   */
  public static final Set<String> pkPropertyIds = ImmutableSet.<String>builder()
    .add(ARTIFACT_NAME_PROPERTY_ID)
    .build();

  /**
   * map of resource type to fk field
   */
  public static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
    .put(Resource.Type.StackArtifact, ARTIFACT_NAME_PROPERTY_ID)
    .put(Resource.Type.Stack, STACK_NAME_PROPERTY_ID)
    .put(Resource.Type.StackVersion, STACK_VERSION_PROPERTY_ID)
    .put(Resource.Type.StackService, STACK_SERVICE_NAME_PROPERTY_ID)
    .build();

  /**
   * resource properties
   */
  public static final Set<String> propertyIds = ImmutableSet.<String>builder()
    .add(STACK_NAME_PROPERTY_ID)
    .add(STACK_VERSION_PROPERTY_ID)
    .add(STACK_SERVICE_NAME_PROPERTY_ID)
    .add(ARTIFACT_NAME_PROPERTY_ID)
    .add(ARTIFACT_DATA_PROPERTY_ID)
    .build();

  /**
   * name of the kerberos descriptor artifact.
   */
  public static final String KERBEROS_DESCRIPTOR_NAME = "kerberos_descriptor";

  /**
   * name of the metrics descriptor artifact.
   */
  public static final String METRICS_DESCRIPTOR_NAME = "metrics_descriptor";

  /**
   * name of the widgets descriptor artifact.
   */
  public static final String WIDGETS_DESCRIPTOR_NAME = "widgets_descriptor";

  /**
   * KerberosDescriptorFactory used to create KerberosDescriptor instances
   */
  @Inject
  private static KerberosDescriptorFactory kerberosDescriptorFactory;

  /**
   * KerberosServiceDescriptorFactory used to create KerberosServiceDescriptor instances
   */
  @Inject
  private static KerberosServiceDescriptorFactory kerberosServiceDescriptorFactory;

  Type widgetLayoutType = new TypeToken<Map<String, List<WidgetLayout>>>(){}.getType();
  Gson gson = new Gson();

  /**
   * Constructor.
   *
   * @param managementController ambari controller
   */
  protected StackArtifactResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.StackArtifact, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException,
             UnsupportedPropertyException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    Set<Resource> resources = new HashSet<>();

    resources.addAll(getKerberosDescriptors(request, predicate));
    resources.addAll(getMetricsDescriptors(request, predicate));
    resources.addAll(getWidgetsDescriptors(request, predicate));
    // add other artifacts types here

    if (resources.isEmpty()) {
      throw new NoSuchResourceException(
          "The requested resource doesn't exist: Artifact not found, " + predicate);
    }

    return resources;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    throw new UnsupportedOperationException("Creating stack artifacts is not supported");
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException,
             UnsupportedPropertyException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    throw new UnsupportedOperationException("Updating of stack artifacts is not supported");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException,
             UnsupportedPropertyException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    throw new UnsupportedOperationException("Deletion of stack artifacts is not supported");
  }

  /**
   * Get all stack and stack service kerberos descriptor resources.
   *
   * @param request    user request
   * @param predicate  request predicate
   *
   * @return set of all stack related kerberos descriptor resources; will not return null
   *
   * @throws SystemException                if an unexpected exception occurs
   * @throws UnsupportedPropertyException   if an unsupported property was requested
   * @throws NoSuchParentResourceException  if a specified parent resource doesn't exist
   * @throws NoSuchResourceException        if the requested resource doesn't exist
   */
  private Set<Resource> getKerberosDescriptors(Request request, Predicate predicate)
      throws SystemException,
             UnsupportedPropertyException,
             NoSuchParentResourceException,
             NoSuchResourceException {

    Set<Resource> resources = new HashSet<>();

    for (Map<String, Object> properties : getPropertyMaps(predicate)) {
      String artifactName = (String) properties.get(ARTIFACT_NAME_PROPERTY_ID);
      if (artifactName == null || artifactName.equals(KERBEROS_DESCRIPTOR_NAME)) {
        String stackName = (String) properties.get(STACK_NAME_PROPERTY_ID);
        String stackVersion = (String) properties.get(STACK_VERSION_PROPERTY_ID);
        String stackService = (String) properties.get(STACK_SERVICE_NAME_PROPERTY_ID);

        Map<String, Object> descriptor;
        try {
          descriptor = getKerberosDescriptor(stackName, stackVersion, stackService);
        } catch (IOException e) {
          LOG.error("Unable to process Kerberos Descriptor. Properties: " + properties, e);
          throw new SystemException("An internal exception occurred while attempting to build a Kerberos Descriptor " +
              "artifact. See ambari server logs for more information", e);
        }

        if (descriptor != null) {
          Resource resource = new ResourceImpl(Resource.Type.StackArtifact);
          Set<String> requestedIds = getRequestPropertyIds(request, predicate);
          setResourceProperty(resource, ARTIFACT_NAME_PROPERTY_ID, KERBEROS_DESCRIPTOR_NAME, requestedIds);
          setResourceProperty(resource, ARTIFACT_DATA_PROPERTY_ID, descriptor, requestedIds);
          setResourceProperty(resource, STACK_NAME_PROPERTY_ID, stackName, requestedIds);
          setResourceProperty(resource, STACK_VERSION_PROPERTY_ID, stackVersion, requestedIds);
          if (stackService != null) {
            setResourceProperty(resource, STACK_SERVICE_NAME_PROPERTY_ID, stackService, requestedIds);
          }
          resources.add(resource);
        }
      }
    }
    return resources;
  }

  /**
   * Get all stack and stack service metrics descriptor resources.
   *
   * @param request    user request
   * @param predicate  request predicate
   *
   * @return set of all stack related kerberos descriptor resources; will not return null
   *
   * @throws SystemException                if an unexpected exception occurs
   * @throws UnsupportedPropertyException   if an unsupported property was requested
   * @throws NoSuchParentResourceException  if a specified parent resource doesn't exist
   * @throws NoSuchResourceException        if the requested resource doesn't exist
   */
  private Set<Resource> getMetricsDescriptors(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
           NoSuchParentResourceException, NoSuchResourceException {

    Set<Resource> resources = new HashSet<>();

    for (Map<String, Object> properties : getPropertyMaps(predicate)) {
      String artifactName = (String) properties.get(ARTIFACT_NAME_PROPERTY_ID);
      if (artifactName == null || artifactName.equals(METRICS_DESCRIPTOR_NAME)) {
        String stackName = (String) properties.get(STACK_NAME_PROPERTY_ID);
        String stackVersion = (String) properties.get(STACK_VERSION_PROPERTY_ID);
        String stackService = (String) properties.get(STACK_SERVICE_NAME_PROPERTY_ID);
        String componentName = (String) properties.get(STACK_COMPONENT_NAME_PROPERTY_ID);

        Map<String, Object> descriptor;
        AmbariMetaInfo metaInfo = getManagementController().getAmbariMetaInfo();

        try {
          List<MetricDefinition> componentMetrics;
          Map<String, Map<String, List<MetricDefinition>>> serviceMetrics;
          if (stackService != null) {
            if (componentName == null) {
              // Service
              serviceMetrics = removeAggregateFunctions(metaInfo.getServiceMetrics(stackName,
                      stackVersion, stackService));
              descriptor = Collections.singletonMap(stackService, (Object) serviceMetrics);
            } else {
              // Component
              componentMetrics = removeAggregateFunctions(metaInfo.getMetrics(stackName,
                      stackVersion, stackService, componentName, Resource.Type.Component.name()));
              descriptor = Collections.singletonMap(componentName, (Object) componentMetrics);
            }
          } else {
            // Cluster
            Map<String, Map<String, PropertyInfo>> clusterMetrics =
              PropertyHelper.getMetricPropertyIds(Resource.Type.Cluster);
            // Host
            Map<String, Map<String, PropertyInfo>> hostMetrics =
              PropertyHelper.getMetricPropertyIds(Resource.Type.Host);

            descriptor = new HashMap<>();
            descriptor.put(Resource.Type.Cluster.name(), clusterMetrics);
            descriptor.put(Resource.Type.Host.name(), hostMetrics);
          }


        } catch (IOException e) {
          LOG.error("Unable to process Metrics Descriptor. Properties: " + properties, e);
          throw new SystemException("An internal exception occurred while attempting to build a Metrics Descriptor " +
            "artifact. See ambari server logs for more information", e);
        }

        Resource resource = new ResourceImpl(Resource.Type.StackArtifact);
        Set<String> requestedIds = getRequestPropertyIds(request, predicate);
        setResourceProperty(resource, ARTIFACT_NAME_PROPERTY_ID, METRICS_DESCRIPTOR_NAME, requestedIds);
        setResourceProperty(resource, ARTIFACT_DATA_PROPERTY_ID, descriptor, requestedIds);
        setResourceProperty(resource, STACK_NAME_PROPERTY_ID, stackName, requestedIds);
        setResourceProperty(resource, STACK_VERSION_PROPERTY_ID, stackVersion, requestedIds);
        if (stackService != null) {
          setResourceProperty(resource, STACK_SERVICE_NAME_PROPERTY_ID, stackService, requestedIds);
        }
        resources.add(resource);
      }
    }
    return resources;
  }

  private Set<Resource> getWidgetsDescriptors(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchParentResourceException, NoSuchResourceException {

    Set<Resource> resources = new HashSet<>();

    for (Map<String, Object> properties : getPropertyMaps(predicate)) {
      String artifactName = (String) properties.get(ARTIFACT_NAME_PROPERTY_ID);
      if (artifactName == null || artifactName.equals(WIDGETS_DESCRIPTOR_NAME)) {
        String stackName = (String) properties.get(STACK_NAME_PROPERTY_ID);
        String stackVersion = (String) properties.get(STACK_VERSION_PROPERTY_ID);
        String stackService = (String) properties.get(STACK_SERVICE_NAME_PROPERTY_ID);

        Map<String, Object> descriptor;
        try {
          descriptor = getWidgetsDescriptor(stackName, stackVersion, stackService);
        } catch (IOException e) {
          LOG.error("Unable to process Widgets Descriptor. Properties: " + properties, e);
          throw new SystemException("An internal exception occurred while attempting to build a Widgets Descriptor " +
            "artifact. See ambari server logs for more information", e);
        }

        if (descriptor != null) {
          Resource resource = new ResourceImpl(Resource.Type.StackArtifact);
          Set<String> requestedIds = getRequestPropertyIds(request, predicate);
          setResourceProperty(resource, ARTIFACT_NAME_PROPERTY_ID, WIDGETS_DESCRIPTOR_NAME, requestedIds);
          setResourceProperty(resource, ARTIFACT_DATA_PROPERTY_ID, descriptor, requestedIds);
          setResourceProperty(resource, STACK_NAME_PROPERTY_ID, stackName, requestedIds);
          setResourceProperty(resource, STACK_VERSION_PROPERTY_ID, stackVersion, requestedIds);
          if (stackService != null) {
            setResourceProperty(resource, STACK_SERVICE_NAME_PROPERTY_ID, stackService, requestedIds);
          }
          resources.add(resource);
        }
      }
    }
    return resources;
  }

  private Map<String, Object> getWidgetsDescriptor(String stackName,
      String stackVersion, String serviceName)
        throws NoSuchParentResourceException, IOException {

    AmbariManagementController controller = getManagementController();
    StackInfo stackInfo;
    try {
      stackInfo = controller.getAmbariMetaInfo().getStack(stackName, stackVersion);
    } catch (StackAccessException e) {
      throw new NoSuchParentResourceException(String.format(
        "Parent stack resource doesn't exist: stackName='%s', stackVersion='%s'", stackName, stackVersion));
    }

    if (StringUtils.isEmpty(serviceName)) {
      return null;
    } else {
      return getWidgetsDescriptorForService(stackInfo, serviceName);
    }
  }

  public Map<String, Object> getWidgetsDescriptorForService(StackInfo stackInfo, String serviceName)
      throws NoSuchParentResourceException, IOException {

    Map<String, Object> widgetDescriptor = null;

    ServiceInfo serviceInfo = stackInfo.getService(serviceName);
    if (serviceInfo == null) {
      throw new NoSuchParentResourceException("Service not found. serviceName" + " = " + serviceName);
    }

    File widgetDescriptorFile = serviceInfo.getWidgetsDescriptorFile();
    if (widgetDescriptorFile != null && widgetDescriptorFile.exists()) {
      widgetDescriptor = gson.fromJson(new FileReader(widgetDescriptorFile), widgetLayoutType);
    }

    return widgetDescriptor;
  }

  /**
   * Get a kerberos descriptor.
   *
   * @param stackName     stack name
   * @param stackVersion  stack version
   * @param serviceName   service name
   *
   * @return map of kerberos descriptor data or null if no descriptor exists
   *
   * @throws IOException if unable to parse the associated kerberos descriptor file
   * @throws NoSuchParentResourceException if the parent stack or stack service doesn't exist
   */
  private Map<String, Object> getKerberosDescriptor(String stackName, String stackVersion, String serviceName)
      throws NoSuchParentResourceException, IOException {

      return serviceName == null ?
          buildStackDescriptor(stackName, stackVersion) :
          getServiceDescriptor(stackName, stackVersion, serviceName);
  }

  /**
   * Build a kerberos descriptor for the specified stack. This descriptor is for the entire stack version
   * and will contain both the stack descriptor as well as all service descriptors.
   *
   * @return map of kerberos descriptor data or null if no descriptor exists
   *
   * @throws IOException     if unable to read the kerberos descriptor file
   * @throws AmbariException if unable to parse the kerberos descriptor file json
   * @throws NoSuchParentResourceException if the parent stack doesn't exist
   */
  private Map<String, Object> buildStackDescriptor(String stackName, String stackVersion)
      throws NoSuchParentResourceException, IOException {

    KerberosDescriptor kerberosDescriptor = new KerberosDescriptor();

    AmbariManagementController controller = getManagementController();
    StackInfo stackInfo;
    try {
      stackInfo = controller.getAmbariMetaInfo().getStack(stackName, stackVersion);
    } catch (StackAccessException e) {
      throw new NoSuchParentResourceException(String.format(
          "Parent stack resource doesn't exist: stackName='%s', stackVersion='%s'", stackName, stackVersion));
    }

    Collection<KerberosServiceDescriptor> serviceDescriptors = getServiceDescriptors(stackInfo);

    serviceDescriptors.forEach(kerberosDescriptor::putService);
    return kerberosDescriptor.toMap();
  }

  /**
   * Get the kerberos descriptor for the specified stack service.
   *
   * @param stackName     stack name
   * @param stackVersion  stack version
   * @param serviceName   service name
   *
   * @return map of kerberos descriptor data or null if no descriptor exists
   *
   * @throws IOException if unable to read or parse the kerberos descriptor file
   * @throws NoSuchParentResourceException if the parent stack or stack service doesn't exist
   */
  private Map<String, Object> getServiceDescriptor(
      String stackName, String stackVersion, String serviceName) throws NoSuchParentResourceException, IOException {

    AmbariManagementController controller = getManagementController();

    ServiceInfo serviceInfo;
    try {
      serviceInfo = controller.getAmbariMetaInfo().getService(stackName, stackVersion, serviceName);
    } catch (StackAccessException e) {
      throw new NoSuchParentResourceException(String.format(
          "Parent stack/service resource doesn't exist: stackName='%s', stackVersion='%s', serviceName='%s'",
          stackName, stackVersion, serviceName));
    }
    File kerberosFile = serviceInfo.getKerberosDescriptorFile();

    if (kerberosFile != null) {
      KerberosServiceDescriptor serviceDescriptor =
          kerberosServiceDescriptorFactory.createInstance(kerberosFile, serviceName);

      if (serviceDescriptor != null) {
        return serviceDescriptor.toMap();
      }
    }
    return null;
  }

  /**
   * Get a collection of all service descriptors for the specified stack.
   *
   * @param stack  stack name
   *
   * @return collection of all service descriptors for the stack; will not return null
   *
   * @throws IOException if unable to read or parse a descriptor file
   */
  private Collection<KerberosServiceDescriptor> getServiceDescriptors(StackInfo stack) throws IOException {
    Collection<KerberosServiceDescriptor> serviceDescriptors = new ArrayList<>();
    for (ServiceInfo service : stack.getServices()) {
      File descriptorFile = service.getKerberosDescriptorFile();
      if (descriptorFile != null) {
        KerberosServiceDescriptor descriptor =
            kerberosServiceDescriptorFactory.createInstance(descriptorFile, service.getName());

        if (descriptor != null) {
          serviceDescriptors.add(descriptor);
        }
      }
    }
    return serviceDescriptors;
  }

  private static Map<String, Map<String, List<MetricDefinition>>> removeAggregateFunctions(
          Map<String, Map<String, List<MetricDefinition>>> serviceMetrics  ) {
    Map<String, Map<String, List<MetricDefinition>>> filteredServiceMetrics = null;

    if (serviceMetrics != null) {
      filteredServiceMetrics = new HashMap<>();
      // For every Component
      for (String component : serviceMetrics.keySet()) {
        Map<String, List<MetricDefinition>> componentMetricsCopy = new HashMap<>();
        Map<String, List<MetricDefinition>> componentMetrics = serviceMetrics.get(component);
        // For every Component / HostComponent category retrieve metrics definitions
        for (String category : componentMetrics.keySet()) {
          componentMetricsCopy.put(category,
            removeAggregateFunctions(componentMetrics.get(category)));
        }
        filteredServiceMetrics.put(component, componentMetricsCopy);
      }
    }
    return filteredServiceMetrics;
  }

  private static List<MetricDefinition> removeAggregateFunctions(List<MetricDefinition> metricsDefinitions) {
    List<MetricDefinition> filteredComponentMetrics = null;

    if (metricsDefinitions != null) {
      filteredComponentMetrics = new ArrayList<>();
      // For every metric definition
      for (MetricDefinition metricDefinition : metricsDefinitions) {
        Map<String, Map<String, Metric>> categorizedMetricsCopy = new HashMap<>();
        Map<String, Map<String, Metric>> categorizedMetrics = metricDefinition.getMetricsByCategory();
        for (String category : categorizedMetrics.keySet()) {
          Map<String, Metric> namedMetricsCopy = new HashMap<>();
          Map<String, Metric> namedMetrics = categorizedMetrics.get(category);
          for (String metricName : namedMetrics.keySet()) {
            // Metrics System metrics only
            if (!(metricDefinition.getType().equals("ganglia") && PropertyHelper.hasAggregateFunctionSuffix(metricName))) {
              namedMetricsCopy.put(metricName, namedMetrics.get(metricName));
            }
          }
          categorizedMetricsCopy.put(category, namedMetricsCopy);
        }
        filteredComponentMetrics.add(
          new MetricDefinition(metricDefinition.getType(), metricDefinition.getProperties(), categorizedMetricsCopy));
      }
    }
    return filteredComponentMetrics;
  }
}
