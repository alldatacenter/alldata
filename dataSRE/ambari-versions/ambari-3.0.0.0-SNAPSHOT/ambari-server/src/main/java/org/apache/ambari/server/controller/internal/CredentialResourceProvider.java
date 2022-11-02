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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * A write-only resource provider for securely stored credentials
 */
@StaticallyInject
public class CredentialResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(CredentialResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  public static final String CREDENTIAL_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Credential", "cluster_name");
  public static final String CREDENTIAL_ALIAS_PROPERTY_ID = PropertyHelper.getPropertyId("Credential", "alias");
  public static final String CREDENTIAL_PRINCIPAL_PROPERTY_ID = PropertyHelper.getPropertyId("Credential", "principal");
  public static final String CREDENTIAL_KEY_PROPERTY_ID = PropertyHelper.getPropertyId("Credential", "key");
  public static final String CREDENTIAL_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId("Credential", "type");

  private static final Set<String> PK_PROPERTY_IDS;
  private static final Set<String> PROPERTY_IDS;
  private static final Map<Type, String> KEY_PROPERTY_IDS;

  static {
    Set<String> set;
    set = new HashSet<>();
    set.add(CREDENTIAL_CLUSTER_NAME_PROPERTY_ID);
    set.add(CREDENTIAL_ALIAS_PROPERTY_ID);
    PK_PROPERTY_IDS = Collections.unmodifiableSet(set);

    set = new HashSet<>();
    set.add(CREDENTIAL_CLUSTER_NAME_PROPERTY_ID);
    set.add(CREDENTIAL_ALIAS_PROPERTY_ID);
    set.add(CREDENTIAL_PRINCIPAL_PROPERTY_ID);
    set.add(CREDENTIAL_KEY_PROPERTY_ID);
    set.add(CREDENTIAL_TYPE_PROPERTY_ID);
    PROPERTY_IDS = Collections.unmodifiableSet(set);

    HashMap<Type, String> map = new HashMap<>();
    map.put(Type.Cluster, CREDENTIAL_CLUSTER_NAME_PROPERTY_ID);
    map.put(Type.Credential, CREDENTIAL_ALIAS_PROPERTY_ID);
    KEY_PROPERTY_IDS = Collections.unmodifiableMap(map);
  }

  /**
   * The secure storage facility to use to store credentials.
   */
  @Inject
  private CredentialStoreService credentialStoreService;


  /**
   * Create a new resource provider.
   */
  @AssistedInject
  public CredentialResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Type.Credential, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);

    EnumSet<RoleAuthorization> authorizations = EnumSet.of(
        RoleAuthorization.CLUSTER_MANAGE_CREDENTIALS,
        RoleAuthorization.CLUSTER_TOGGLE_KERBEROS);

    setRequiredCreateAuthorizations(authorizations);
    setRequiredGetAuthorizations(authorizations);
    setRequiredUpdateAuthorizations(authorizations);
    setRequiredDeleteAuthorizations(authorizations);
  }

  @Override
  protected RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    for (final Map<String, Object> properties : request.getProperties()) {
      createResources(new CreateResourcesCommand(properties));
    }

    notifyCreate(Type.Credential, request);
    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();
    boolean sendNotFoundErrorIfEmpty = false;

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(CREDENTIAL_CLUSTER_NAME_PROPERTY_ID);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException("Invalid argument, cluster name is required");
      }

      String alias = (String) propertyMap.get(CREDENTIAL_ALIAS_PROPERTY_ID);
      if (!StringUtils.isEmpty(alias)) {
        try {
          if (credentialStoreService.containsCredential(clusterName, alias)) {
            resources.add(toResource(clusterName, alias, credentialStoreService.getCredentialStoreType(clusterName, alias), requestedIds));
          }
          else {
            // Only throw a NoSuchResourceException if a specific item is being requested and it
            // wasn't found. If multiple resources are queried, one or may not exist and this
            // sendNotFoundErrorIfEmpty will be set to true.  However if at least one resource is
            // found, the resources Set will not be empty and NoSuchResourceException will not be
            // thrown
            sendNotFoundErrorIfEmpty = true;
          }
        } catch (AmbariException e) {
          throw new SystemException(e.getLocalizedMessage(), e);
        }
      } else {
        try {
          Map<String, CredentialStoreType> results = credentialStoreService.listCredentials(clusterName);
          if (results != null) {
            for (Map.Entry<String, CredentialStoreType> entry : results.entrySet()) {
              resources.add(toResource(clusterName, entry.getKey(), entry.getValue(), requestedIds));
            }
          }
        } catch (AmbariException e) {
          throw new SystemException(e.getLocalizedMessage(), e);
        }
      }
    }

    if (sendNotFoundErrorIfEmpty && resources.isEmpty()) {
      throw new NoSuchResourceException("The requested resource doesn't exist: Credential not found, " + predicate);
    }

    return resources;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    for (Map<String, Object> requestPropMap : request.getProperties()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(requestPropMap, predicate)) {
        if (modifyResources(new ModifyResourcesCommand(propertyMap)) == null) {
          throw new NoSuchResourceException("The requested resource doesn't exist: Credential not found, " + getAlias(propertyMap));
        }
      }
    }

    notifyUpdate(Type.Credential, request, predicate);
    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    for (final Map<String, Object> properties : propertyMaps) {
      modifyResources(new DeleteResourcesCommand(properties));
    }

    notifyDelete(Type.Credential, predicate);
    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  /**
   * Give a map of credential-related properties attempts to create a Credential (PrincipalKeyCredential)
   * after validating that the required properties are present.
   * <p/>
   * The credential's principal is required, however a warning will be logged if a value for the key
   * is not supplied.
   *
   * @param properties a map of properties
   * @return a new Credential
   * @throws IllegalArgumentException if the map of properties does not contain enough information to create
   *                                  a new PrincipalKeyCredential instance
   */
  private Credential createCredential(Map<String, Object> properties) throws IllegalArgumentException {
    String principal;
    String key;

    if (properties.get(CREDENTIAL_PRINCIPAL_PROPERTY_ID) == null) {
      throw new IllegalArgumentException("Property " + CREDENTIAL_PRINCIPAL_PROPERTY_ID + " must be provided");
    } else {
      principal = String.valueOf(properties.get(CREDENTIAL_PRINCIPAL_PROPERTY_ID));
    }

    if (properties.get(CREDENTIAL_KEY_PROPERTY_ID) == null) {
      LOG.warn("The credential is being added without a key");
      key = null;
    } else {
      key = String.valueOf(properties.get(CREDENTIAL_KEY_PROPERTY_ID));
    }

    return new PrincipalKeyCredential(principal, key);
  }

  /**
   * Retrieves the <code>Credential/persist</code> property from the property map and determined if the value
   * represents Boolean true or false.
   * <p/>
   * If the <code>Credential/type</code> property is not is not available or is <code>null</code>,
   * an AmbariException will be thrown.
   *
   * @param properties a map of properties
   * @return true or false
   * @throws IllegalArgumentException if the <code>Credential/type</code> property is not set to
   *                                  either <code>persisted</code> or <code>temporary</code>
   */
  private CredentialStoreType getCredentialStoreType(Map<String, Object> properties) throws IllegalArgumentException {
    Object propertyValue = properties.get(CREDENTIAL_TYPE_PROPERTY_ID);
    if (propertyValue == null) {
      throw new IllegalArgumentException("Property " + CREDENTIAL_TYPE_PROPERTY_ID + " must be provided");
    } else if (propertyValue instanceof String) {
      try {
        return CredentialStoreType.valueOf(((String) propertyValue).toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Property " + CREDENTIAL_TYPE_PROPERTY_ID + " must be either 'persisted' or 'temporary'", e);
      }
    } else {
      throw new IllegalArgumentException("Property " + CREDENTIAL_TYPE_PROPERTY_ID + " must be a String");
    }
  }

  /**
   * Retrieves the <code>Credential/cluster_name</code> property from the property map
   *
   * @param properties a map of properties
   * @return the cluster name
   * @throws IllegalArgumentException if <code>Credential/cluster_name</code> is not in the map or
   *                                  <code>null</code>
   */
  private String getClusterName(Map<String, Object> properties) throws IllegalArgumentException {
    if (properties.get(CREDENTIAL_CLUSTER_NAME_PROPERTY_ID) == null) {
      throw new IllegalArgumentException("Property " + CREDENTIAL_CLUSTER_NAME_PROPERTY_ID + " must be provided");
    } else {
      return String.valueOf(properties.get(CREDENTIAL_CLUSTER_NAME_PROPERTY_ID));
    }
  }

  /**
   * Retrieves the <code>Credential/alias</code> property from the property map
   *
   * @param properties a map of properties
   * @return the alias name
   * @throws IllegalArgumentException if <code>Credential/alias</code> is not in the map or
   *                                  <code>null</code>
   */
  private String getAlias(Map<String, Object> properties) throws IllegalArgumentException {
    if (properties.get(CREDENTIAL_ALIAS_PROPERTY_ID) == null) {
      throw new IllegalArgumentException("Property " + CREDENTIAL_ALIAS_PROPERTY_ID + " must be provided");
    } else {
      return String.valueOf(properties.get(CREDENTIAL_ALIAS_PROPERTY_ID));
    }
  }

  /**
   * Validates that the CredentialStoreService is available and has been initialized to support the
   * requested persisted or temporary storage facility.
   *
   * @param credentialStoreType a CredentialStoreType indicating which credential store facility to use
   * @throws IllegalArgumentException if the requested facility has not been initialized
   */
  private void validateForCreateOrModify(CredentialStoreType credentialStoreType) throws IllegalArgumentException {
    if (!credentialStoreService.isInitialized(credentialStoreType)) {
      if (CredentialStoreType.PERSISTED == credentialStoreType) {
        // Throw IllegalArgumentException to cause a 400 error to be returned...
        throw new IllegalArgumentException("Credentials cannot be stored in Ambari's persistent secure " +
            "credential store since secure persistent storage has not yet be configured.  " +
            "Use ambari-server setup-security to enable this feature.");
      } else if (CredentialStoreType.TEMPORARY == credentialStoreType) {
        // Throw IllegalArgumentException to cause a 400 error to be returned...
        throw new IllegalArgumentException("Credentials cannot be stored in Ambari's temporary secure " +
            "credential store since secure temporary storage has not yet be configured.");
      }
    }
  }

  /**
   * Creates a new resource from the given cluster name, alias, and persist values.
   *
   * @param clusterName         a cluster name
   * @param alias               an alias
   * @param credentialStoreType the relevant credential store type
   * @param requestedIds        the properties to include in the resulting resource instance
   * @return a resource
   */
  private Resource toResource(String clusterName, String alias, CredentialStoreType credentialStoreType, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Type.Credential);
    setResourceProperty(resource, CREDENTIAL_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);
    setResourceProperty(resource, CREDENTIAL_ALIAS_PROPERTY_ID, alias, requestedIds);
    setResourceProperty(resource, CREDENTIAL_TYPE_PROPERTY_ID, credentialStoreType.name().toLowerCase(), requestedIds);
    return resource;
  }

  /**
   * CreateResourcesCommand implements a Command that performs the steps to create a new credential
   * resources and store the data into the persisted or temporary credential stores, as indicated.
   */
  private class CreateResourcesCommand implements Command<String> {
    private final Map<String, Object> properties;

    public CreateResourcesCommand(Map<String, Object> properties) {
      this.properties = properties;
    }

    @Override
    public String invoke() throws AmbariException {
      CredentialStoreType credentialStoreType = getCredentialStoreType(properties);

      validateForCreateOrModify(credentialStoreType);

      String clusterName = getClusterName(properties);
      String alias = getAlias(properties);

      if (credentialStoreService.containsCredential(clusterName, alias)) {
        throw new DuplicateResourceException("A credential with the alias of " + alias + " already exists");
      }

      credentialStoreService.setCredential(clusterName, alias, createCredential(properties), credentialStoreType);
      return alias;
    }
  }

  /**
   * ModifyResourcesCommand implements a Command that performs the steps to update existing credential
   * resources.
   */
  private class ModifyResourcesCommand implements Command<String> {
    private final Map<String, Object> properties;

    public ModifyResourcesCommand(Map<String, Object> properties) {
      this.properties = properties;
    }

    @Override
    public String invoke() throws AmbariException {
      String clusterName = getClusterName(properties);
      String alias = getAlias(properties);

      CredentialStoreType credentialStoreType = properties.containsKey(CREDENTIAL_TYPE_PROPERTY_ID)
          ? getCredentialStoreType(properties)
          : credentialStoreService.getCredentialStoreType(clusterName, alias);

      validateForCreateOrModify(credentialStoreType);

      Credential credential = credentialStoreService.getCredential(clusterName, alias);
      if (credential instanceof PrincipalKeyCredential) {
        PrincipalKeyCredential principalKeyCredential = (PrincipalKeyCredential) credential;

        Map<String, Object> credentialProperties = new HashMap<>();

        // Make sure the credential to update is removed from the persisted or temporary store... the
        // updated data may change the persistence type.
        credentialStoreService.removeCredential(clusterName, alias);

        if (properties.containsKey(CREDENTIAL_PRINCIPAL_PROPERTY_ID)) {
          credentialProperties.put(CREDENTIAL_PRINCIPAL_PROPERTY_ID, properties.get(CREDENTIAL_PRINCIPAL_PROPERTY_ID));
        } else {
          credentialProperties.put(CREDENTIAL_PRINCIPAL_PROPERTY_ID, principalKeyCredential.getPrincipal());
        }

        if (properties.containsKey(CREDENTIAL_KEY_PROPERTY_ID)) {
          credentialProperties.put(CREDENTIAL_KEY_PROPERTY_ID, properties.get(CREDENTIAL_KEY_PROPERTY_ID));
        } else {
          char[] credentialKey = principalKeyCredential.getKey();
          if (credentialKey != null) {
            credentialProperties.put(CREDENTIAL_KEY_PROPERTY_ID, String.valueOf(credentialKey));
          }
        }

        credentialStoreService.setCredential(clusterName, alias, createCredential(credentialProperties), credentialStoreType);
        return alias;
      } else {
        return null;
      }
    }
  }

  /**
   * DeleteResourcesCommand implements a Command that performs the steps to delete existing credential
   * resources. Credential resources will be deleted from both storage facilities, if necessary.
   */
  private class DeleteResourcesCommand implements Command<String> {
    private final Map<String, Object> properties;

    public DeleteResourcesCommand(Map<String, Object> properties) {
      this.properties = properties;
    }

    @Override
    public String invoke() throws AmbariException {
      String clusterName = getClusterName(properties);
      String alias = getAlias(properties);
      credentialStoreService.removeCredential(clusterName, alias);
      return alias;
    }
  }
}
