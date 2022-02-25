/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

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
import org.apache.ambari.server.orm.dao.KerberosDescriptorDAO;
import org.apache.ambari.server.orm.entities.KerberosDescriptorEntity;
import org.apache.ambari.server.topology.KerberosDescriptor;
import org.apache.ambari.server.topology.KerberosDescriptorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.assistedinject.Assisted;

public class KerberosDescriptorResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(KerberosDescriptorResourceProvider.class);

  static final String KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("KerberosDescriptors", "kerberos_descriptor_name");

  private static final String KERBEROS_DESCRIPTOR_TEXT_PROPERTY_ID =
      PropertyHelper.getPropertyId("KerberosDescriptors", "kerberos_descriptor_text");

  /**
   * The key property ids for a KerberosDescriptor resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = ImmutableMap.of(Resource.Type.KerberosDescriptor, KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID);

  /**
   * The property ids for a KerberosDescriptor resource.
   */
  private static final Set<String> PROPERTY_IDS = ImmutableSet.of(KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID, KERBEROS_DESCRIPTOR_TEXT_PROPERTY_ID);

  private KerberosDescriptorDAO kerberosDescriptorDAO;

  private KerberosDescriptorFactory kerberosDescriptorFactory;

  // keep constructors hidden
  @Inject
  KerberosDescriptorResourceProvider(KerberosDescriptorDAO kerberosDescriptorDAO,
                                     KerberosDescriptorFactory kerberosDescriptorFactory,
                                     @Assisted AmbariManagementController managementController) {
    super(Resource.Type.KerberosDescriptor, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
    this.kerberosDescriptorDAO = kerberosDescriptorDAO;
    this.kerberosDescriptorFactory = kerberosDescriptorFactory;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    LOGGER.debug("Skipping property id validation for kerberos descriptor resources");
    return Collections.emptySet();
  }

  @Override
  public RequestStatus createResources(Request request) throws ResourceAlreadyExistsException {
    String name = getNameFromRequest(request);
    String descriptor = getRawKerberosDescriptorFromRequest(request);

    if (kerberosDescriptorDAO.findByName(name) != null) {
      String msg = String.format("Kerberos descriptor named %s already exists", name);
      LOGGER.info(msg);
      throw new ResourceAlreadyExistsException(msg);
    }

    KerberosDescriptor kerberosDescriptor = kerberosDescriptorFactory.createKerberosDescriptor(name, descriptor);
    kerberosDescriptorDAO.create(kerberosDescriptor.toEntity());

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws NoSuchResourceException, NoSuchParentResourceException {

    List<KerberosDescriptorEntity> results = null;
    boolean applyPredicate = false;

    if (predicate != null) {
      Set<Map<String, Object>> requestProps = getPropertyMaps(predicate);
      if (requestProps.size() == 1) {
        String name = (String) requestProps.iterator().next().get(
            KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID);

        if (name != null) {
          KerberosDescriptorEntity entity = kerberosDescriptorDAO.findByName(name);
          results = entity == null ? Collections.emptyList() :
              Collections.singletonList(entity);
        }
      }
    }

    if (results == null) {
      applyPredicate = true;
      results = kerberosDescriptorDAO.findAll();
    }

    Set<Resource> resources = new HashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);
    for (KerberosDescriptorEntity entity : results) {
      Resource resource = new ResourceImpl(Resource.Type.KerberosDescriptor);
      toResource(resource, entity, requestPropertyIds);

      if (predicate == null || !applyPredicate || predicate.evaluate(resource)) {
        resources.add(resource);
      }
    }

    if (predicate != null && resources.isEmpty()) {
      throw new NoSuchResourceException(
          "The requested resource doesn't exist: Kerberos Descriptor not found, " + predicate);
    }

    return resources;
  }

  private static void toResource(Resource resource, KerberosDescriptorEntity entity, Set<String> requestPropertyIds) {
    setResourceProperty(resource, KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID, entity.getName(), requestPropertyIds);
    setResourceProperty(resource, KERBEROS_DESCRIPTOR_TEXT_PROPERTY_ID, entity.getKerberosDescriptorText(), requestPropertyIds);
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> setResources = getResources(new RequestImpl(null, null, null, null), predicate);

    for (Resource resource : setResources) {
      final String kerberosDescriptorName =
          (String) resource.getPropertyValue(KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID);
      LOGGER.debug("Deleting resource with name: {}", kerberosDescriptorName);
      kerberosDescriptorDAO.removeByName(kerberosDescriptorName);
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return ImmutableSet.copyOf(KEY_PROPERTY_IDS.values());
  }

  /**
   * @throws IllegalArgumentException if descriptor text is not found or is empty
   */
  private static String getRawKerberosDescriptorFromRequest(Request request) {
    Map<String, String> requestInfoProperties = request.getRequestInfoProperties();
    if (requestInfoProperties != null) {
      String descriptorText = requestInfoProperties.get(Request.REQUEST_INFO_BODY_PROPERTY);
      if (!Strings.isNullOrEmpty(descriptorText)) {
        return descriptorText;
      }
    }

    String msg = "No Kerberos descriptor found in the request body";
    LOGGER.error(msg);
    throw new IllegalArgumentException(msg);
  }

  /**
   * @throws IllegalArgumentException if name is not found or is empty
   */
  private static String getNameFromRequest(Request request) {
    if (request.getProperties() != null && !request.getProperties().isEmpty()) {
      Map<String, Object> properties = request.getProperties().iterator().next();
      Object name = properties.get(KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID);
      if (name != null) {
        return String.valueOf(name);
      }
    }

    String msg = "No name provided for the Kerberos descriptor";
    LOGGER.error(msg);
    throw new IllegalArgumentException(msg);
  }

}
