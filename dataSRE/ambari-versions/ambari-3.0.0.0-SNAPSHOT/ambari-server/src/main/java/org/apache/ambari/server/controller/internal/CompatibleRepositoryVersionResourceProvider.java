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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.resources.OperatingSystemResourceDefinition;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.ManifestServiceInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;


/**
 * Resource provider for repository versions resources.
 */
@StaticallyInject
public class CompatibleRepositoryVersionResourceProvider extends ReadOnlyResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(CompatibleRepositoryVersionResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  public static final String REPOSITORY_VERSION_ID_PROPERTY_ID                 = "CompatibleRepositoryVersions/id";
  public static final String REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID         = "CompatibleRepositoryVersions/stack_name";
  public static final String REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID      = "CompatibleRepositoryVersions/stack_version";
  public static final String REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID = "CompatibleRepositoryVersions/repository_version";
  public static final String REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID       = "CompatibleRepositoryVersions/display_name";
  public static final String REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID            = "CompatibleRepositoryVersions/upgrade_types";
  public static final String REPOSITORY_VERSION_SERVICES                       = "CompatibleRepositoryVersions/services";
  public static final String REPOSITORY_VERSION_STACK_SERVICES                 = "CompatibleRepositoryVersions/stack_services";
  public static final String SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID         = new OperatingSystemResourceDefinition().getPluralName();
  private static final String REPOSITORY_STACK_VALUE                           = "stack_value";

  private static final Set<String> pkPropertyIds = Collections.singleton(REPOSITORY_VERSION_ID_PROPERTY_ID);

  static final Set<String> propertyIds = ImmutableSet.of(
    REPOSITORY_STACK_VALUE,
    REPOSITORY_VERSION_ID_PROPERTY_ID,
    REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID,
    REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
    REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
    REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
    SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID,
    REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID,
    REPOSITORY_VERSION_SERVICES,
    REPOSITORY_VERSION_STACK_SERVICES);

  static final Map<Type, String> keyPropertyIds = new ImmutableMap.Builder<Type, String>()
    .put(Type.Stack, REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID)
    .put(Type.StackVersion, REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID)
    .put(Type.Upgrade, REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID)
    .put(Type.CompatibleRepositoryVersion, REPOSITORY_VERSION_ID_PROPERTY_ID)
    .build();

  @Inject
  private static RepositoryVersionDAO s_repositoryVersionDAO;

  @Inject
  private static Provider<AmbariMetaInfo> s_ambariMetaInfo;

  /**
   * Create a new resource provider.
   */
  public CompatibleRepositoryVersionResourceProvider(AmbariManagementController amc) {
    super(Type.CompatibleRepositoryVersion, propertyIds, keyPropertyIds, amc);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    Long currentStackUniqueId = null;
    Map<Long, CompatibleRepositoryVersion> compatibleRepositoryVersionsMap = new HashMap<>();

    StackId stackId = null;
    // !!! this is the case where the predicate was altered to include all stacks
    for (Map<String, Object> propertyMap : propertyMaps) {
      if (propertyMap.containsKey(REPOSITORY_STACK_VALUE)) {
        stackId = new StackId(propertyMap.get(REPOSITORY_STACK_VALUE).toString());
        break;
      }
    }

    if (null == stackId) {
      if (propertyMaps.size() == 1) {
        Map<String, Object> propertyMap = propertyMaps.iterator().next();
        stackId = getStackInformationFromUrl(propertyMap);
      } else {
        LOG.error("Property Maps size is NOT equal to 1. Current 'propertyMaps' size = {}", propertyMaps.size());
      }
    }

    if (null == stackId) {
      LOG.error("Could not determine stack to process.  Returning empty set.");
      return resources;
    }

    for (RepositoryVersionEntity repositoryVersionEntity : s_repositoryVersionDAO.findByStack(stackId)) {
      currentStackUniqueId = repositoryVersionEntity.getId();
      compatibleRepositoryVersionsMap.put(repositoryVersionEntity.getId(),
          new CompatibleRepositoryVersion(repositoryVersionEntity));
      if (LOG.isDebugEnabled()) {
        LOG.debug("Added current stack id: {} to map", repositoryVersionEntity.getId());
      }
    }

    Map<String, UpgradePack> packs = s_ambariMetaInfo.get().getUpgradePacks(
      stackId.getStackName(), stackId.getStackVersion());

    for (UpgradePack up : packs.values()) {
      if (null != up.getTargetStack()) {
        StackId targetStackId = new StackId(up.getTargetStack());
        List<RepositoryVersionEntity> repositoryVersionEntities = s_repositoryVersionDAO.findByStack(targetStackId);

        for (RepositoryVersionEntity repositoryVersionEntity : repositoryVersionEntities) {
          if (compatibleRepositoryVersionsMap.containsKey(repositoryVersionEntity.getId())) {
            compatibleRepositoryVersionsMap.get(repositoryVersionEntity.getId()).addUpgradePackType(up.getType());
            if (LOG.isDebugEnabled()) {
              LOG.debug("Stack id: {} exists in map.  Appended new upgrade type {}{}", up.getType(), repositoryVersionEntity.getId());
            }
          } else {
            CompatibleRepositoryVersion compatibleRepositoryVersionEntity = new CompatibleRepositoryVersion(repositoryVersionEntity);
            compatibleRepositoryVersionEntity.addUpgradePackType(up.getType());
            compatibleRepositoryVersionsMap.put(repositoryVersionEntity.getId(), compatibleRepositoryVersionEntity);
            if (LOG.isDebugEnabled()) {
              LOG.debug("Added Stack id: {} to map with upgrade type {}", repositoryVersionEntity.getId(), up.getType());
            }
          }
        }
      } else {
        if (currentStackUniqueId != null) {
          compatibleRepositoryVersionsMap.get(currentStackUniqueId).addUpgradePackType(up.getType());
          if (LOG.isDebugEnabled()) {
            LOG.debug("Current Stack id: {} retrieved from map. Added upgrade type {}", currentStackUniqueId, up.getType());
          }
        } else {
          LOG.error("Couldn't retrieve Current stack entry from Map.");
        }
      }
    }

    for (CompatibleRepositoryVersion entity : compatibleRepositoryVersionsMap.values()) {

      RepositoryVersionEntity repositoryVersionEntity = entity.getRepositoryVersionEntity();
      final Resource resource = new ResourceImpl(Resource.Type.CompatibleRepositoryVersion);
      setResourceProperty(resource, REPOSITORY_VERSION_ID_PROPERTY_ID, repositoryVersionEntity.getId(), requestedIds);

      setResourceProperty(resource, REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, repositoryVersionEntity.getStackName(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, repositoryVersionEntity.getStackVersion(), requestedIds);

      setResourceProperty(resource, REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, repositoryVersionEntity.getDisplayName(), requestedIds);
      setResourceProperty(resource, REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, repositoryVersionEntity.getVersion(), requestedIds);
      setResourceProperty(resource, REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID, entity.getSupportedTypes(), requestedIds);

      final VersionDefinitionXml xml;
      try {
        xml = repositoryVersionEntity.getRepositoryXml();
      } catch (Exception e) {
        throw new SystemException(String.format("Could not load xml for Repository %s", repositoryVersionEntity.getId()), e);
      }

      final StackInfo stack;
      try {
        stack = s_ambariMetaInfo.get().getStack(repositoryVersionEntity.getStackName(), repositoryVersionEntity.getStackVersion());
      } catch (AmbariException e) {
        throw new SystemException(String.format("Could not load stack %s for Repository %s",
            repositoryVersionEntity.getStackId().toString(), repositoryVersionEntity.getId()));
      }

      final List<ManifestServiceInfo> stackServices;

      if (null != xml) {
        setResourceProperty(resource, REPOSITORY_VERSION_SERVICES, xml.getAvailableServices(stack), requestedIds);
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
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Gets the stack id from the request map
   *
   * @param propertyMap the request map
   * @return the StackId, or {@code null} if not found.
   */
  protected StackId getStackInformationFromUrl(Map<String, Object> propertyMap) {
    if (propertyMap.containsKey(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID) && propertyMap.containsKey(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID)) {
      return new StackId(propertyMap.get(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).toString(), propertyMap.get(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).toString());
    }
    return null;
  }

  /**
   * Identify the case where this is a top-level request, containing ONLY the stack name
   * and stack version. Determine all other compatible stacks and make a new Predicate
   * out of them.
   *
   * Consider a stack STACK-2.2 that is compatible with 2.3 and 2.4.  The result of this
   * call will be a predicate like so (abbreviated):
   * <pre>
   * in -> AndPredicate([stack_name=STACK],[stack_version=2.2])
   * out-> OrPredicate(
   *         AndPredicate([stack_name=STACK],[stack_version=2.2]),
   *         AndPredicate([stack_name=STACK],[stack_version=2.3]),
   *         AndPredicate([stack_name=STACK],[stack_version=2.4]) )
   * </pre>
   *
   * Any input predicate that does not conform to ONLY stack_name/stack_version will
   * revert to the that predicate (return {@code null}).
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Predicate amendPredicate(Predicate predicate) {
    if (!AndPredicate.class.isInstance(predicate)) {
      return null;
    }

    AndPredicate ap = (AndPredicate) predicate;
    if (2 != ap.getPropertyIds().size()) {
      return null;
    }

    if (!ap.getPropertyIds().contains(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID) &&
        !ap.getPropertyIds().contains(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID)) {
      return null;
    }

    Predicate[] predicates = ap.getPredicates();
    if (!EqualsPredicate.class.isInstance(predicates[0]) || !EqualsPredicate.class.isInstance(predicates[1])) {
      return null;
    }

    EqualsPredicate pred1 = (EqualsPredicate) predicates[0];
    EqualsPredicate pred2 = (EqualsPredicate) predicates[1];

    StackId stackId = null;
    if (pred1.getPropertyId().equals(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID)) {
      stackId = new StackId(pred1.getValue().toString(), pred2.getValue().toString());
    } else {
      stackId = new StackId(pred2.getValue().toString(), pred1.getValue().toString());
    }

    Map<String, UpgradePack> packs = s_ambariMetaInfo.get().getUpgradePacks(
        stackId.getStackName(), stackId.getStackVersion());

    Set<String> stackIds = new HashSet<>();

    for (Entry<String, UpgradePack> entry : packs.entrySet()) {
      UpgradePack pack = entry.getValue();
      String packStack = pack.getTargetStack();
      if (null == packStack || !packStack.equals(stackId.toString())) {
        stackIds.add(packStack);
      }
    }


    // !!! use the one passed in, it already includes the one of interest
    List<Predicate> usable = new ArrayList<>();
    usable.add(predicate);

    // !!! add predicate for each of the compatible stacks as found by the upgrade packs
    for (String requiredStack : stackIds) {
      StackId targetStack = new StackId(requiredStack);
      Predicate p = new PredicateBuilder().property(REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals(targetStack.getStackName())
          .and().property(REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals(targetStack.getStackVersion()).toPredicate();
      usable.add(p);
    }

    // !!! add the stack that is used to find compatible versions for
    Predicate p = new PredicateBuilder().property(REPOSITORY_STACK_VALUE).equals(stackId.toString()).toPredicate();
    usable.add(p);

    p = new OrPredicate(usable.toArray(new Predicate[usable.size()]));

    return p;
  }

}
