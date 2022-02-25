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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.api.resources.OperatingSystemResourceDefinition;
import org.apache.ambari.server.api.resources.RepositoryResourceDefinition;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
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
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.ManifestServiceInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

/**
 * Resource provider for repository versions resources.
 */
public class RepositoryVersionResourceProvider extends AbstractAuthorizedResourceProvider {

  // ----- Property ID constants ---------------------------------------------
  public static final String REPOSITORY_VERSION                                = "RepositoryVersions";
  public static final String REPOSITORY_VERSION_ID_PROPERTY_ID                 = PropertyHelper.getPropertyId("RepositoryVersions", "id");
  public static final String REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID         = PropertyHelper.getPropertyId("RepositoryVersions", "stack_name");
  public static final String REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID      = PropertyHelper.getPropertyId("RepositoryVersions", "stack_version");
  public static final String REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("RepositoryVersions", "repository_version");
  public static final String REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID       = PropertyHelper.getPropertyId("RepositoryVersions", "display_name");
  public static final String REPOSITORY_VERSION_HIDDEN_PROPERTY_ID             = PropertyHelper.getPropertyId("RepositoryVersions", "hidden");
  public static final String REPOSITORY_VERSION_RESOLVED_PROPERTY_ID           = PropertyHelper.getPropertyId("RepositoryVersions", "resolved");
  public static final String SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID         = new OperatingSystemResourceDefinition().getPluralName();
  public static final String SUBRESOURCE_REPOSITORIES_PROPERTY_ID              = new RepositoryResourceDefinition().getPluralName();

  public static final String REPOSITORY_VERSION_TYPE_PROPERTY_ID               = "RepositoryVersions/type";
  public static final String REPOSITORY_VERSION_RELEASE_VERSION                = "RepositoryVersions/release/version";
  public static final String REPOSITORY_VERSION_RELEASE_BUILD                  = "RepositoryVersions/release/build";
  public static final String REPOSITORY_VERSION_RELEASE_NOTES                  = "RepositoryVersions/release/notes";
  public static final String REPOSITORY_VERSION_RELEASE_COMPATIBLE_WITH        = "RepositoryVersions/release/compatible_with";
  public static final String REPOSITORY_VERSION_AVAILABLE_SERVICES             = "RepositoryVersions/services";
  public static final String REPOSITORY_VERSION_STACK_SERVICES                 = "RepositoryVersions/stack_services";

  public static final String REPOSITORY_VERSION_PARENT_ID                      = "RepositoryVersions/parent_id";
  public static final String REPOSITORY_VERSION_HAS_CHILDREN                   = "RepositoryVersions/has_children";

  @SuppressWarnings("serial")
  private static final Set<String> pkPropertyIds = ImmutableSet.of(REPOSITORY_VERSION_ID_PROPERTY_ID);

  @SuppressWarnings("serial")
  public static final Set<String> propertyIds = ImmutableSet.of(
      REPOSITORY_VERSION_ID_PROPERTY_ID,
      REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID,
      REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
      REPOSITORY_VERSION_HIDDEN_PROPERTY_ID,
      REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
      REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
      SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID,
      REPOSITORY_VERSION_TYPE_PROPERTY_ID,
      REPOSITORY_VERSION_RELEASE_BUILD,
      REPOSITORY_VERSION_RELEASE_COMPATIBLE_WITH,
      REPOSITORY_VERSION_RELEASE_NOTES,
      REPOSITORY_VERSION_RELEASE_VERSION,
      REPOSITORY_VERSION_PARENT_ID,
      REPOSITORY_VERSION_HAS_CHILDREN,
      REPOSITORY_VERSION_AVAILABLE_SERVICES,
      REPOSITORY_VERSION_STACK_SERVICES,
      REPOSITORY_VERSION_RESOLVED_PROPERTY_ID);

  @SuppressWarnings("serial")
  public static final Map<Type, String> keyPropertyIds = new ImmutableMap.Builder<Type, String>()
      .put(Type.Stack, REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID)
      .put(Type.StackVersion, REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID)
      .put(Type.RepositoryVersion, REPOSITORY_VERSION_ID_PROPERTY_ID)
      .build();

  @Inject
  private Gson gson;

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private RepositoryVersionHelper repositoryVersionHelper;

  /**
   * Data access object used for lookup up stacks.
   */
  @Inject
  private StackDAO stackDAO;

  @Inject
  HostVersionDAO hostVersionDAO;

  /**
   * Create a new resource provider.
   *
   */
  public RepositoryVersionResourceProvider() {
    super(Resource.Type.RepositoryVersion, propertyIds, keyPropertyIds);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS));
    setRequiredUpdateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS, RoleAuthorization.AMBARI_EDIT_STACK_REPOS));

    setRequiredGetAuthorizations(EnumSet.of(
        RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS,
        RoleAuthorization.AMBARI_EDIT_STACK_REPOS,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK));
  }

  @Override
  protected RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    for (final Map<String, Object> properties : request.getProperties()) {
      createResources(new Command<Void>() {

        @Override
        public Void invoke() throws AmbariException {
          final String[] requiredProperties = {
            REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
            SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID,
            REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
            REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
            REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID
          };

          for (String propertyName : requiredProperties) {
            if (properties.get(propertyName) == null) {
              throw new AmbariException("Property " + propertyName + " should be provided");
            }
          }

          RepositoryVersionEntity entity = toRepositoryVersionEntity(properties);

          if (repositoryVersionDAO.findByDisplayName(entity.getDisplayName()) != null) {
            throw new DuplicateResourceException("Repository version with name " + entity.getDisplayName() + " already exists");
          }
          if (repositoryVersionDAO.findByStackAndVersion(entity.getStack(), entity.getVersion()) != null) {
            throw new DuplicateResourceException("Repository version for stack " + entity.getStack() + " and version " + entity.getVersion() + " already exists");
          }

          validateRepositoryVersion(repositoryVersionDAO, ambariMetaInfo, entity);

          repositoryVersionDAO.create(entity);
          notifyCreate(Resource.Type.RepositoryVersion, request);
          return null;
        }
      });
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);


    List<RepositoryVersionEntity> requestedEntities = new ArrayList<>();
    for (Map<String, Object> propertyMap: propertyMaps) {
      final StackId stackId = getStackInformationFromUrl(propertyMap);

      if (stackId != null && propertyMaps.size() == 1 && propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID) == null) {
        requestedEntities.addAll(repositoryVersionDAO.findByStack(stackId));
      } else {
        final Long id;
        try {
          id = Long.parseLong(propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID).toString());
        } catch (Exception ex) {
          throw new SystemException("Repository version should have numerical id");
        }
        final RepositoryVersionEntity entity = repositoryVersionDAO.findByPK(id);

        if (entity == null) {
          throw new NoSuchResourceException("There is no repository version with id " + id);
        } else {
          requestedEntities.add(entity);
        }
      }
    }

    for (RepositoryVersionEntity entity: requestedEntities) {
      final Resource resource = new ResourceImpl(Resource.Type.RepositoryVersion);

      setResourceProperty(resource, REPOSITORY_VERSION_ID_PROPERTY_ID, entity.getId(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, entity.getStackName(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, entity.getStackVersion(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, entity.getDisplayName(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_HIDDEN_PROPERTY_ID, entity.isHidden(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, entity.getVersion(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_TYPE_PROPERTY_ID, entity.getType(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_RESOLVED_PROPERTY_ID, entity.isResolved(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_PARENT_ID, entity.getParentId(), requestedIds);

      List<RepositoryVersionEntity> children = entity.getChildren();
      setResourceProperty(resource, REPOSITORY_VERSION_HAS_CHILDREN,
          null != children && !children.isEmpty(), requestedIds);

      final VersionDefinitionXml xml;
      try {
        xml = entity.getRepositoryXml();
      } catch (Exception e) {
        throw new SystemException(String.format("Could not load xml for Repository %s", entity.getId()), e);
      }

      final StackInfo stack;
      try {
        stack = ambariMetaInfo.getStack(entity.getStackName(), entity.getStackVersion());
      } catch (AmbariException e) {
        throw new SystemException(String.format("Could not load stack %s for Repository %s",
            entity.getStackId().toString(), entity.getId()));
      }

      final List<ManifestServiceInfo> stackServices;

      if (null != xml) {
        setResourceProperty(resource, REPOSITORY_VERSION_RELEASE_VERSION, xml.release.version, requestedIds);
        setResourceProperty(resource, REPOSITORY_VERSION_RELEASE_BUILD, xml.release.build, requestedIds);
        setResourceProperty(resource, REPOSITORY_VERSION_RELEASE_COMPATIBLE_WITH, xml.release.compatibleWith, requestedIds);
        setResourceProperty(resource, REPOSITORY_VERSION_RELEASE_NOTES, xml.release.releaseNotes, requestedIds);
        setResourceProperty(resource, REPOSITORY_VERSION_AVAILABLE_SERVICES, xml.getAvailableServices(stack), requestedIds);
        stackServices = xml.getStackServices(stack);
      } else {
        stackServices = new ArrayList<>();

        for (ServiceInfo si : stack.getServices()) {
          stackServices.add(new ManifestServiceInfo(si.getName(), si.getDisplayName(), si.getComment(),
              Collections.singleton(si.getVersion())));
        }

      }

      setResourceProperty(resource, REPOSITORY_VERSION_STACK_SERVICES, stackServices, requestedIds);

      resources.add(resource);
    }
    return resources;
  }

  @Override
  @Transactional
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Map<String, Object>> propertyMaps = request.getProperties();

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        for (Map<String, Object> propertyMap : propertyMaps) {
          final Long id;
          try {
            id = Long.parseLong(propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID).toString());
          } catch (Exception ex) {
            throw new AmbariException("Repository version should have numerical id");
          }

          final RepositoryVersionEntity entity = repositoryVersionDAO.findByPK(id);
          if (entity == null) {
            throw new ObjectNotFoundException("There is no repository version with id " + id);
          }

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID)))) {
            if (!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_EDIT_STACK_REPOS)) {
              throw new AuthorizationException("The authenticated user does not have authorization to modify stack repositories");
            }

            final Object operatingSystems = propertyMap.get(SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID);
            final String operatingSystemsJson = gson.toJson(operatingSystems);
            try {
              entity.addRepoOsEntities(repositoryVersionHelper.parseOperatingSystems(operatingSystemsJson));
            } catch (Exception ex) {
              throw new AmbariException("Json structure for operating systems is incorrect", ex);
            }
          }

          if (StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID)))) {
            entity.setDisplayName(propertyMap.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID).toString());
          }

          if(StringUtils.isNotBlank(ObjectUtils.toString(propertyMap.get(REPOSITORY_VERSION_HIDDEN_PROPERTY_ID)))){
            boolean isHidden = Boolean.valueOf(ObjectUtils.toString(propertyMap.get(REPOSITORY_VERSION_HIDDEN_PROPERTY_ID)));
            entity.setHidden(isHidden);
          }

          validateRepositoryVersion(repositoryVersionDAO, ambariMetaInfo, entity);

          repositoryVersionDAO.merge(entity);
        }
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    final List<RepositoryVersionEntity> entitiesToBeRemoved = new ArrayList<>();
    for (Map<String, Object> propertyMap : propertyMaps) {
      final Long id;
      try {
        id = Long.parseLong(propertyMap.get(REPOSITORY_VERSION_ID_PROPERTY_ID).toString());
      } catch (Exception ex) {
        throw new SystemException("Repository version should have numerical id");
      }

      final RepositoryVersionEntity entity = repositoryVersionDAO.findByPK(id);
      if (entity == null) {
        throw new NoSuchResourceException("There is no repository version with id " + id);
      }

      final Set<RepositoryVersionState> forbiddenToDeleteStates = Sets.newHashSet(
          RepositoryVersionState.CURRENT,
          RepositoryVersionState.INSTALLED,
          RepositoryVersionState.INSTALLING);

      List<HostVersionEntity> hostVersions = hostVersionDAO.findByRepositoryAndStates(
          entity, forbiddenToDeleteStates);

      if (CollectionUtils.isNotEmpty(hostVersions)) {
        Map<RepositoryVersionState, Set<String>> hostsInUse = new HashMap<>();

        for (HostVersionEntity hostVersion : hostVersions) {
          if (!hostsInUse.containsKey(hostVersion.getState())) {
            hostsInUse.put(hostVersion.getState(), new HashSet<>());
          }

          hostsInUse.get(hostVersion.getState()).add(hostVersion.getHostName());
        }

        Set<String> errors = new HashSet<>();
        for (Entry<RepositoryVersionState, Set<String>> entry : hostsInUse.entrySet()) {
          errors.add(String.format("%s on %s", entry.getKey(), StringUtils.join(entry.getValue(), ", ")));
        }


        throw new SystemException(
            String.format("Repository version can't be deleted as it is used by the following hosts: %s",
                StringUtils.join(errors, ';')));
      }

      entitiesToBeRemoved.add(entity);
    }

    for (RepositoryVersionEntity entity: entitiesToBeRemoved) {
      repositoryVersionDAO.remove(entity);
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Validates newly created repository versions to contain actual information.
   *
   * @throws AmbariException exception with error message
   */
  protected static void validateRepositoryVersion(RepositoryVersionDAO dao,
      AmbariMetaInfo metaInfo, RepositoryVersionEntity repositoryVersion) throws AmbariException {
    validateRepositoryVersion(dao, metaInfo, repositoryVersion, false);
  }

  /**
   * Validates newly created repository versions to contain actual information.  Optionally
   * skip url duplication.
   *
   * @throws AmbariException exception with error message
   */
  protected static void validateRepositoryVersion(RepositoryVersionDAO dao,
      AmbariMetaInfo metaInfo, RepositoryVersionEntity repositoryVersion, boolean skipUrlCheck) throws AmbariException {
    final StackId requiredStack = new StackId(repositoryVersion.getStack());

    final String requiredStackName = requiredStack.getStackName();
    final String requiredStackVersion = requiredStack.getStackVersion();
    final String requiredStackId = requiredStack.getStackId();

    // List of all repo urls that are already added at stack
    Set<String> existingRepoUrls = new HashSet<>();
    List<RepositoryVersionEntity> existingRepoVersions = dao.findByStack(requiredStack);
    for (RepositoryVersionEntity existingRepoVersion : existingRepoVersions) {
      for (RepoOsEntity operatingSystemEntity : existingRepoVersion.getRepoOsEntities()) {
        for (RepoDefinitionEntity repositoryEntity : operatingSystemEntity.getRepoDefinitionEntities()) {
          if (repositoryEntity.isUnique() && !existingRepoVersion.getId().equals(repositoryVersion.getId())) { // Allow modifying already defined repo version
            existingRepoUrls.add(repositoryEntity.getBaseUrl());
          }
        }
      }
    }

    // check that repositories contain only supported operating systems
    final Set<String> osSupported = new HashSet<>();
    for (OperatingSystemInfo osInfo: metaInfo.getOperatingSystems(requiredStackName, requiredStackVersion)) {
      osSupported.add(osInfo.getOsType());
    }

    final Set<String> osRepositoryVersion = new HashSet<>();

    for (RepoOsEntity os : repositoryVersion.getRepoOsEntities()) {
      osRepositoryVersion.add(os.getFamily());

      for (RepoDefinitionEntity repositoryEntity : os.getRepoDefinitionEntities()) {
        String baseUrl = repositoryEntity.getBaseUrl();
        if (!skipUrlCheck && os.isAmbariManaged() && existingRepoUrls.contains(baseUrl)) {
          throw new DuplicateResourceException("Base url " + baseUrl + " is already defined for another repository version. " +
                  "Setting up base urls that contain the same versions of components will cause stack upgrade to fail.");
        }
      }
    }

    if (osRepositoryVersion.isEmpty()) {
      throw new AmbariException("At least one set of repositories for OS should be provided");
    }

    for (String os: osRepositoryVersion) {
      if (!osSupported.contains(os)) {
        throw new AmbariException("Operating system type " + os + " is not supported by stack " + requiredStackId);
      }
    }

    if (!RepositoryVersionEntity.isVersionInStack(repositoryVersion.getStackId(), repositoryVersion.getVersion())) {
      throw new AmbariException(MessageFormat.format("Version {0} needs to belong to stack {1}",
          repositoryVersion.getVersion(), repositoryVersion.getStackName() + "-" + repositoryVersion.getStackVersion()));
    }
  }

  /**
   * Transforms map of json properties to repository version entity.
   *
   * @param properties json map
   * @return constructed entity
   * @throws AmbariException if some properties are missing or json has incorrect structure
   */
  protected RepositoryVersionEntity toRepositoryVersionEntity(Map<String, Object> properties) throws AmbariException {
    final RepositoryVersionEntity entity = new RepositoryVersionEntity();

    final String stackName = properties.get(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).toString();
    final String stackVersion = properties.get(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).toString();

    StackEntity stackEntity = stackDAO.find(stackName, stackVersion);

    entity.setDisplayName(properties.get(REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID).toString());
    entity.setStack(stackEntity);

    entity.setVersion(properties.get(REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID).toString());
    final Object operatingSystems = properties.get(SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID);
    final String operatingSystemsJson = gson.toJson(operatingSystems);
    try {
      entity.addRepoOsEntities(repositoryVersionHelper.parseOperatingSystems(operatingSystemsJson));
    } catch (Exception ex) {
      throw new AmbariException("Json structure for operating systems is incorrect", ex);
    }
    return entity;
  }

  protected StackId getStackInformationFromUrl(Map<String, Object> propertyMap) {
    if (propertyMap.containsKey(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID) && propertyMap.containsKey(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID)) {
      return new StackId(propertyMap.get(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).toString(), propertyMap.get(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).toString());
    }
    return null;
  }

  @Override
  protected ResourceType getResourceType(Request request, Predicate predicate) {
    // This information is not associated with any particular resource
    return null;
  }
}
