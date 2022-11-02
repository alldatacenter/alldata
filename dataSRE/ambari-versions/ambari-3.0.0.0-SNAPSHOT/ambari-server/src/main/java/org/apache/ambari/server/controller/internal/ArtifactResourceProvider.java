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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.StaticallyInject;
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
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.state.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * Provider for cluster artifacts.
 * Artifacts contain an artifact name as the PK and artifact data in the form of
 * a map which is the content of the artifact.
 * <p>
 * An example of an artifact is a kerberos descriptor.
 */
//todo: implement ExtendedResourceProvider???
@StaticallyInject
public class ArtifactResourceProvider extends AbstractResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ArtifactResourceProvider.class);

  public static final String RESPONSE_KEY = "Artifacts";
  public static final String ARTIFACT_NAME = "artifact_name";
  public static final String CLUSTER_NAME = "cluster_name";
  public static final String SERVICE_NAME = "service_name";
  public static final String ARTIFACT_DATA_PROPERTY = "artifact_data";
  public static final String ARTIFACT_NAME_PROPERTY = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + ARTIFACT_NAME;
  public static final String CLUSTER_NAME_PROPERTY = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_NAME;
  public static final String SERVICE_NAME_PROPERTY = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + SERVICE_NAME;

  // artifact names
  public static final String KERBEROS_DESCRIPTOR = "kerberos_descriptor";

  /**
   * primary key fields
   */
  private static final Set<String> pkPropertyIds = new HashSet<>();

  /**
   * map of resource type to fk field
   */
  private static final Map<Resource.Type, String> keyPropertyIds =
    new HashMap<>();

  /**
   * resource properties
   */
  private static final Set<String> propertyIds = new HashSet<>();

  /**
   * map of resource type to type registration
   */
  private static final Map<Resource.Type, TypeRegistration> typeRegistrations =
    new HashMap<>();

  /**
   * map of foreign key field to type registration
   */
  private static final Map<String, TypeRegistration> typeRegistrationsByFK =
    new HashMap<>();

  /**
   * map of short foreign key field to type registration
   */
  private static final Map<String, TypeRegistration> typeRegistrationsByShortFK =
    new HashMap<>();

  /**
   * serializer used to convert json to map
   */
  private static final Gson jsonSerializer = new Gson();

  /**
   * artifact data access object
   */
  @Inject
  private static ArtifactDAO artifactDAO;


  /*
   * set resource properties, pk and fk's
   */
  static {
    // resource properties
    propertyIds.add(ARTIFACT_NAME_PROPERTY);
    propertyIds.add(ARTIFACT_DATA_PROPERTY);

    // pk property
    pkPropertyIds.add(ARTIFACT_NAME_PROPERTY);

    // key properties
    keyPropertyIds.put(Resource.Type.Artifact, ARTIFACT_NAME_PROPERTY);

    //todo: external registration
    // cluster registration
    ClusterTypeRegistration clusterTypeRegistration = new ClusterTypeRegistration();
    typeRegistrations.put(clusterTypeRegistration.getType(), clusterTypeRegistration);

    //service registration
    ServiceTypeRegistration serviceTypeRegistration = new ServiceTypeRegistration();
    typeRegistrations.put(serviceTypeRegistration.getType(), serviceTypeRegistration);

    //todo: detect resource type and fk name collisions during registration
    for (TypeRegistration registration: typeRegistrations.values()) {
      String fkProperty = registration.getFKPropertyName();
      keyPropertyIds.put(registration.getType(), fkProperty);
      propertyIds.add(fkProperty);

      typeRegistrationsByFK.put(fkProperty, registration);
      typeRegistrationsByShortFK.put(registration.getShortFKPropertyName(), registration);

      for (Map.Entry<Resource.Type, String> ancestor : registration.getForeignKeyInfo().entrySet()) {
        Resource.Type ancestorType = ancestor.getKey();
        if (! keyPropertyIds.containsKey(ancestorType)) {
          String ancestorFK = ancestor.getValue();
          keyPropertyIds.put(ancestorType, ancestorFK);
          propertyIds.add(ancestorFK);
        }
      }
    }
  }

  /**
   * Constructor.
   *
   * @param controller  management controller
   */
  @Inject
  protected ArtifactResourceProvider(AmbariManagementController controller) {
    super(propertyIds, keyPropertyIds);

    for (TypeRegistration typeRegistration : typeRegistrations.values()) {
      typeRegistration.setManagementController(controller);
    }
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

    for (Map<String, Object> properties : request.getProperties()) {
      createResources(getCreateCommand(properties, request.getRequestInfoProperties()));
    }
    notifyCreate(Resource.Type.Artifact, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException,
             UnsupportedPropertyException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    Set<Map<String, Object>> requestProps = getPropertyMaps(predicate);
    Set<Resource> resources = new LinkedHashSet<>();

    for (Map<String, Object> props : requestProps) {
      resources.addAll(getResources(getGetCommand(request, predicate, props)));
    }

    if (resources.isEmpty() && isInstanceRequest(requestProps)) {
      throw new NoSuchResourceException(
          "The requested resource doesn't exist: Artifact not found, " + predicate);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException,
             UnsupportedPropertyException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    for (Resource resource : getResources(request, predicate)) {
      modifyResources(getUpdateCommand(request, resource));
    }

    notifyUpdate(Resource.Type.Artifact, request, predicate);
    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException,
             UnsupportedPropertyException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    // get resources to update
    Set<Resource> setResources = getResources(
        new RequestImpl(null, null, null, null), predicate);

    for (final Resource resource : setResources) {
      modifyResources(getDeleteCommand(resource));
    }

    notifyDelete(Resource.Type.Artifact, predicate);
    return getRequestStatus(null);
  }

  /**
   * Create a command to create a resource.
   *
   * @param properties        request properties
   * @param requestInfoProps  request info properties
   *
   * @return a new create command
   */
  private Command<Void> getCreateCommand(final Map<String, Object> properties,
                                         final Map<String, String> requestInfoProps) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        // ensure that parent exists
        validateParent(properties);

        String artifactName = String.valueOf(properties.get(ARTIFACT_NAME_PROPERTY));
        TreeMap<String, String> foreignKeyMap = createForeignKeyMap(properties);

        if (artifactDAO.findByNameAndForeignKeys(artifactName, foreignKeyMap) != null) {
          throw new DuplicateResourceException(String.format(
              "Attempted to create an artifact which already exists, artifact_name='%s', foreign_keys='%s'",
              artifactName, getRequestForeignKeys(properties)));
        }

        LOG.debug("Creating Artifact Resource with name '{}'. Parent information: {}",
            artifactName, getRequestForeignKeys(properties));

        artifactDAO.create(toEntity(properties, requestInfoProps.get(Request.REQUEST_INFO_BODY_PROPERTY)));

        return null;
      }
    };
  }

  /**
   * Create a command to get the requested resources.
   *
   * @param properties  request properties
   *
   * @return a new get command
   */
  private Command<Set<Resource>> getGetCommand(final Request request,
                                               final Predicate predicate,
                                               final Map<String, Object> properties) {
    return new Command<Set<Resource>>() {
      @Override
      public Set<Resource> invoke() throws AmbariException {
        String name = (String) properties.get(ARTIFACT_NAME_PROPERTY);
        validateParent(properties);

        Set<Resource> matchingResources = new HashSet<>();
        TreeMap<String, String> foreignKeys = createForeignKeyMap(properties);
        Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);
        if (name != null) {
          // find instance using name and foreign keys
          ArtifactEntity entity = artifactDAO.findByNameAndForeignKeys(name, foreignKeys);
          if (entity != null) {
            Resource instance = (toResource(entity, requestPropertyIds));
            if (predicate.evaluate(instance)) {
              matchingResources.add(instance);
            }
          }
        } else {
          // find collection using foreign keys only
          List<ArtifactEntity> results = artifactDAO.findByForeignKeys(foreignKeys);
          for (ArtifactEntity entity : results) {
            Resource resource = toResource(entity, requestPropertyIds);
            if (predicate.evaluate(resource)) {
              matchingResources.add(resource);
            }
          }
        }
        return matchingResources;
      }
    };
  }

  /**
   * Create a command to update a resource.
   *
   * @param request   update request
   * @param resource  resource to update
   *
   * @return  a update resource command
   */
  private Command<Void> getUpdateCommand(final Request request, final Resource resource) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        Map<String, Object> entityUpdateProperties =
          new HashMap<>(request.getProperties().iterator().next());

        // ensure name is set.  It won't be in case of query
        entityUpdateProperties.put(ARTIFACT_NAME_PROPERTY,
            String.valueOf(resource.getPropertyValue(ARTIFACT_NAME_PROPERTY)));

        artifactDAO.merge(toEntity(entityUpdateProperties,
            request.getRequestInfoProperties().get(Request.REQUEST_INFO_BODY_PROPERTY)));

        return null;
      }
    };
  }

  /**
   * Create a command to delete a resource.
   *
   * @param resource the resource to delete
   *
   * @return  a delete resource command
   */
  private Command<Void> getDeleteCommand(final Resource resource) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        // flatten out key properties as is expected by createForeignKeyMap()
        Map<String, Object> keyProperties = new HashMap<>();
        for (Map.Entry<String, Object> entry : resource.getPropertiesMap().get(RESPONSE_KEY).entrySet()) {
          keyProperties.put(String.format("Artifacts/%s", entry.getKey()), entry.getValue());
        }

        // find entity to remove
        String artifactName = String.valueOf(resource.getPropertyValue(ARTIFACT_NAME_PROPERTY));
        TreeMap<String, String> artifactForeignKeys = createForeignKeyMap(keyProperties);
        ArtifactEntity entity = artifactDAO.findByNameAndForeignKeys(artifactName, artifactForeignKeys);

        if (entity != null) {
          LOG.info("Deleting Artifact: name = {}, foreign keys = {}",
              entity.getArtifactName(), entity.getForeignKeys());
          artifactDAO.remove(entity);
        } else {
          LOG.info("Cannot find Artifact to delete, ignoring: name = {}, foreign keys = {}",
              artifactName, artifactForeignKeys);
        }

        return null;
      }
    };
  }

  /**
   * Validate that parent resources exist.
   *
   * @param properties  request properties
   *
   * @throws ParentObjectNotFoundException  if the parent resource doesn't exist
   * @throws AmbariException if an error occurred while attempting to validate the parent
   */
  private void validateParent(Map<String, Object> properties) throws AmbariException {
    Resource.Type parentType = getRequestType(properties);
    if (! typeRegistrations.get(parentType).instanceExists(keyPropertyIds, properties)) {
      throw new ParentObjectNotFoundException(String.format(
       "Parent resource doesn't exist: %s", getRequestForeignKeys(properties)));
    }
  }

  /**
   * Get the type of the parent resource from the request properties.
   *
   * @param properties  request properties
   *
   * @return the parent resource type based on the request properties
   *
   * @throws AmbariException  if unable to determine the parent resource type
   */
  private Resource.Type getRequestType(Map<String, Object> properties) throws AmbariException {
    Set<String> requestFKs = getRequestForeignKeys(properties).keySet();
    for (TypeRegistration registration : typeRegistrations.values()) {
      Collection<String> typeFKs = new HashSet<>(registration.getForeignKeyInfo().values());
      typeFKs.add(registration.getFKPropertyName());
      if (requestFKs.equals(typeFKs)) {
        return registration.getType();
      }
    }
    throw new AmbariException("Couldn't determine resource type based on request properties");
  }

  /**
   * Get a map of foreign key to value for the given request properties.
   * The foreign key map will only include the foreign key properties which
   * are included in the request properties.  This is useful for reporting
   * errors back to the user.
   * .
   * @param properties  request properties
   *
   * @return map of foreign key to value for the provided request properties
   */
  private Map<String, String> getRequestForeignKeys(Map<String, Object> properties) {
    Map<String, String> requestFKs = new HashMap<>();
    for (String property : properties.keySet()) {
      if (! property.equals(ARTIFACT_NAME_PROPERTY) && ! property.startsWith(ARTIFACT_DATA_PROPERTY)) {
        requestFKs.put(property, String.valueOf(properties.get(property)));
      }
    }
    return requestFKs;
  }

  /**
   * Convert a map of properties to an artifact entity.
   *
   * @param properties  property map
   *
   * @return new artifact entity
   */
  @SuppressWarnings("unchecked")
  private ArtifactEntity toEntity(Map<String, Object> properties, String rawRequestBody)
      throws AmbariException {

    String name = (String) properties.get(ARTIFACT_NAME_PROPERTY);
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Artifact name must be provided");
    }

    ArtifactEntity artifact = new ArtifactEntity();
    artifact.setArtifactName(name);
    artifact.setForeignKeys(createForeignKeyMap(properties));

    Map<String, Object> rawBodyMap = jsonSerializer.<Map<String, Object>>fromJson(
        rawRequestBody, Map.class);

    Object artifactData = rawBodyMap.get(ARTIFACT_DATA_PROPERTY);
    if (artifactData == null) {
      throw new IllegalArgumentException("artifact_data property must be provided");
    }
    if (! (artifactData instanceof Map)) {
      throw new IllegalArgumentException("artifact_data property must be a map");
    }
    artifact.setArtifactData((Map<String, Object>) artifactData);

    return artifact;
  }

  /**
   * Create a map of foreign keys and values which can be persisted.
   * This map will include the short fk names of the key properties as well
   * as the 'persist id' representation of the value which is returned
   * by the type registration.
   *
   * @param properties  request properties
   * @return an ordered map of key name to value
   *
   * @throws AmbariException an unexpected exception occurred
   */
  private TreeMap<String, String> createForeignKeyMap(Map<String, Object> properties) throws AmbariException {
    TreeMap<String, String> foreignKeys = new TreeMap<>();
    for (String keyProperty : keyPropertyIds.values()) {
      if (! keyProperty.equals(ARTIFACT_NAME_PROPERTY)) {
        String origValue = (String) properties.get(keyProperty);
        if (origValue != null && ! origValue.isEmpty()) {
          TypeRegistration typeRegistration = typeRegistrationsByFK.get(keyProperty);
          foreignKeys.put(typeRegistration.getShortFKPropertyName(), typeRegistration.toPersistId(origValue));
        }
      }
    }
    return foreignKeys;
  }

  /**
   * Create a resource instance from an artifact entity.
   * This will convert short fk property names to the full property name as well
   * as converting the value from the 'persist id' representation which is written
   * to the database.
   *
   * @param entity        artifact entity
   * @param requestedIds  requested id's
   *
   * @return a new resource instance for the given artifact entity
   */
  private Resource toResource(ArtifactEntity entity, Set<String> requestedIds) throws AmbariException {
    Resource resource = new ResourceImpl(Resource.Type.Artifact);
    setResourceProperty(resource, ARTIFACT_NAME_PROPERTY, entity.getArtifactName(), requestedIds);
    setResourceProperty(resource, ARTIFACT_DATA_PROPERTY, entity.getArtifactData(), requestedIds);

    for (Map.Entry<String, String> entry : entity.getForeignKeys().entrySet()) {
      TypeRegistration typeRegistration = typeRegistrationsByShortFK.get(entry.getKey());
      setResourceProperty(resource, typeRegistration.getFKPropertyName(),
          typeRegistration.fromPersistId(entry.getValue()), requestedIds);
    }
    return resource;
  }

  /**
   * Determine if the request was for an instance resource.
   *
   * @param requestProps  request properties
   *
   * @return true if the request was for a specific instance, false otherwise
   */
  private boolean isInstanceRequest(Set<Map<String, Object>> requestProps) {
    return requestProps.size() == 1 &&
        requestProps.iterator().next().get(ARTIFACT_NAME_PROPERTY) != null;
  }

  public static String toArtifactDataJson(Map<?,?> properties) {
    return String.format("{ \"%s\": %s }", ARTIFACT_DATA_PROPERTY, jsonSerializer.toJson(properties));
  }

  //todo: when static registration is changed to external registration, this interface
  //todo: should be extracted as a first class interface.
  /**
   * Used to register a dynamic sub-resource with an existing resource type.
   */
  public interface TypeRegistration {
    /**
     * Allows the management controller to be set on the registration.
     * This is called as part of the registration process.
     * For registrations that need access to the management controller,
     * they should assign this controller to a member field.
     *
     * @param  controller  management controller
     */
    void setManagementController(AmbariManagementController controller);

    /**
     * Get the type of the registering resource.
     *
     * @return type of the register resource
     */
    Resource.Type getType();

    /**
     * Full foreign key property name to use in the artifact resource.
     * At this time, all foreign key properties should be in the "Artifacts" category.
     *
     * @return  the absolute foreign key property name.
     *          For example: "Artifacts/cluster_name
     */
    //todo: use relative property names
    String getFKPropertyName();

    /**
     * Shortened foreign key name that is written to the database.
     * This name doesn't need to be in any category but must be unique
     * across all registrations.
     *
     * @return short fk name.  For example: "cluster_name"
     */
    String getShortFKPropertyName();

    /**
     * Convert the foreign key value to a value that is persisted to the database.
     * In most cases this will be the original value.
     * <p>
     * An example of when this will be different is when the fk value value needs
     * to be converted to the unique id for the resource.
     * <p>
     * For example, the cluster_name to the cluster_id.
     * <p>
     * This returned value will later be converted back to the normal form via
     * {@link #fromPersistId(String)}.
     *
     * @param value normal form of the fk value used by the api
     *
     * @return persist id form of the fk value
     *
     * @throws AmbariException if unable to convert the value
     */
    String toPersistId(String value) throws AmbariException;

    /**
     * Convert the persist id form of the foreign key which is written to the database
     * to the form used by the api. In most cases, this will be the same.
     * <p>
     * This method takes the value returned from {@link #toPersistId(String)} and converts
     * it back to the original value which is used by the api.
     * <p>
     * An  example of this is the converting the cluster name to the cluster id in
     * {@link #toPersistId(String)} and then back to the cluster name by this method.  The
     * api always uses the cluster name so we wouldn't want to return the id back as the
     * value for a cluster_name foreign key.
     *
     * @param value  persist id form of the fk value
     *
     * @return  normal form of the fk value used by the api
     *
     * @throws AmbariException if unable to convert the value
     */
    String fromPersistId(String value) throws AmbariException;

    /**
     * Get a map of ancestor type to foreign key.
     * <p>
     * <b>Note: Currently, if a parent resource has also registered the same dynamic resource,
     * the foreign key name used here has to match the value returned by the parent resource
     * in {@link #getFKPropertyName()}</b>
     *
     * @return map of ancestor type to foreign key
     */
    //todo: look at the need to use the same name as specified by ancestors
    Map<Resource.Type, String> getForeignKeyInfo();

    /**
     * Determine if the instance identified by the provided properties exists.
     *
     * @param keyMap      map of resource type to foreign key properties
     * @param properties  request properties
     *
     * @return true if the resource instance exists, false otherwise
     *
     * @throws AmbariException  an exception occurs trying to determine if the instance exists
     */
    boolean instanceExists(Map<Resource.Type, String> keyMap,
                           Map<String, Object> properties) throws AmbariException;
  }


  //todo: Registration should be done externally and these implementations should be moved
  //todo: to a location where the registering resource definition has access to them.
  /**
   * Cluster resource registration.
   */
  private static class ClusterTypeRegistration implements TypeRegistration {
    /**
     * management controller instance
     */
    private AmbariManagementController controller = null;

    /**
     * cluster name property name
     */

    @Override
    public void setManagementController(AmbariManagementController controller) {
      this.controller = controller;
    }

    @Override
    public Resource.Type getType() {
      return Resource.Type.Cluster;
    }

    @Override
    public String getFKPropertyName() {
      return CLUSTER_NAME_PROPERTY;
    }

    @Override
    public String getShortFKPropertyName() {
      return "cluster";
    }

    @Override
    public String toPersistId(String value) throws AmbariException {
      return String.valueOf(controller.getClusters().getCluster(value).getClusterId());
    }

    @Override
    public String fromPersistId(String value) throws AmbariException {
      return controller.getClusters().getClusterById(Long.parseLong(value)).getClusterName();
    }

    @Override
    public Map<Resource.Type, String> getForeignKeyInfo() {
      return Collections.emptyMap();
    }

    @Override
    public boolean instanceExists(Map<Resource.Type, String> keyMap,
                                  Map<String, Object> properties) throws AmbariException {
      try {
        String clusterName = String.valueOf(properties.get(CLUSTER_NAME_PROPERTY));
        controller.getClusters().getCluster(clusterName);
        return true;
      } catch (ObjectNotFoundException e) {
        // doesn't exist
      }
      return false;
    }
  }

  /**
   * Service resource registration.
   */
  private static class ServiceTypeRegistration implements TypeRegistration {
    /**
     * management controller instance
     */
    private AmbariManagementController controller = null;

    /**
     * service name property name
     */

    @Override
    public void setManagementController(AmbariManagementController controller) {
      this.controller = controller;
    }

    @Override
    public Resource.Type getType() {
      return Resource.Type.Service;
    }

    @Override
    public String getFKPropertyName() {
      return SERVICE_NAME_PROPERTY;
    }

    @Override
    public String getShortFKPropertyName() {
      return "service";
    }

    @Override
    public String toPersistId(String value) {
      return value;
    }

    @Override
    public String fromPersistId(String value) {
      return value;
    }

    @Override
    public Map<Resource.Type, String> getForeignKeyInfo() {
      return Collections.singletonMap(Resource.Type.Cluster, CLUSTER_NAME_PROPERTY);
    }

    @Override
    public boolean instanceExists(Map<Resource.Type, String> keyMap,
                                  Map<String, Object> properties) throws AmbariException {

      String clusterName = String.valueOf(properties.get(keyMap.get(Resource.Type.Cluster)));
      try {
        Cluster cluster = controller.getClusters().getCluster(clusterName);
        cluster.getService(String.valueOf(properties.get(SERVICE_NAME_PROPERTY)));
        return true;
      } catch (ObjectNotFoundException e) {
        // doesn't exist
      }
      return false;
    }
  }
}
