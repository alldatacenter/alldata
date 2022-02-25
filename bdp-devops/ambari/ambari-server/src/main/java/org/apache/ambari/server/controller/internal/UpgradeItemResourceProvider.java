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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.utils.SecretReference;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Manages the ability to get the status of upgrades.
 */
@StaticallyInject
public class UpgradeItemResourceProvider extends ReadOnlyResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeItemResourceProvider.class);

  public static final String UPGRADE_CLUSTER_NAME = "UpgradeItem/cluster_name";
  public static final String UPGRADE_REQUEST_ID = "UpgradeItem/request_id";
  public static final String UPGRADE_GROUP_ID = "UpgradeItem/group_id";
  public static final String UPGRADE_ITEM_STAGE_ID = "UpgradeItem/stage_id";
  public static final String UPGRADE_ITEM_TEXT = "UpgradeItem/text";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<>(
    Arrays.asList(UPGRADE_REQUEST_ID, UPGRADE_ITEM_STAGE_ID));
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();
  private static Map<String, String> STAGE_MAPPED_IDS = new HashMap<>();

  @Inject
  private static UpgradeDAO s_dao;

  @Inject
  private static StageDAO s_stageDao;

  @Inject
  private static HostRoleCommandDAO s_hostRoleCommandDAO;

  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_ITEM_STAGE_ID);
    PROPERTY_IDS.add(UPGRADE_GROUP_ID);
    PROPERTY_IDS.add(UPGRADE_REQUEST_ID);
    PROPERTY_IDS.add(UPGRADE_ITEM_TEXT);

    // !!! boo
    for (String p : StageResourceProvider.PROPERTY_IDS) {
      STAGE_MAPPED_IDS.put(p, p.replace("Stage/", "UpgradeItem/"));
    }

    PROPERTY_IDS.addAll(STAGE_MAPPED_IDS.values());

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.UpgradeItem, UPGRADE_ITEM_STAGE_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.UpgradeGroup, UPGRADE_GROUP_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_REQUEST_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, UPGRADE_CLUSTER_NAME);
  }

  /**
   * Constructor.
   *
   * @param controller  the controller
   */
  UpgradeItemResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.UpgradeItem, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    // the request should contain a single map of update properties...
    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      Map<String,Object> updateProperties = iterator.next();

      String statusPropertyId = STAGE_MAPPED_IDS.get(StageResourceProvider.STAGE_STATUS);
      String stageStatus = (String) updateProperties.get(statusPropertyId);

      if (null == stageStatus) {
        throw new IllegalArgumentException("Upgrade items can only have their status changed.");
      }

      HostRoleStatus desiredStatus = HostRoleStatus.valueOf(stageStatus);
      Set<Resource> resources = getResources(PropertyHelper.getReadRequest(), predicate);

      for (Resource resource : resources) {
        final String clusterName = (String)resource.getPropertyValue(UPGRADE_CLUSTER_NAME);
        final Cluster cluster;

        try {
          cluster = getManagementController().getClusters().getCluster(clusterName);
        } catch (AmbariException e) {
          throw new NoSuchParentResourceException(
              String.format("Cluster %s could not be loaded", clusterName));
        }


        if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
            EnumSet.of(RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK))) {
          throw new AuthorizationException("The authenticated user does not have authorization to " +
              "manage upgrade and downgrade");
        }


        // Set the desired status on the underlying stage.
        Long requestId = (Long) resource.getPropertyValue(UPGRADE_REQUEST_ID);
        Long stageId = (Long) resource.getPropertyValue(UPGRADE_ITEM_STAGE_ID);

        StageEntityPK primaryKey = new StageEntityPK();
        primaryKey.setRequestId(requestId);
        primaryKey.setStageId(stageId);

        StageEntity stageEntity = s_stageDao.findByPK(primaryKey);
        if (null == stageEntity) {
          LOG.warn(
              "Unable to change the status of request {} and stage {} to {} because it does not exist",
              requestId, stageId, desiredStatus);

          return getRequestStatus(null);
        }

        s_stageDao.updateStageStatus(stageEntity, desiredStatus,
            getManagementController().getActionManager());
      }
    }

    notifyUpdate(Resource.Type.UpgradeItem, request, predicate);
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new LinkedHashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String requestIdStr = (String) propertyMap.get(UPGRADE_REQUEST_ID);
      String groupIdStr = (String) propertyMap.get(UPGRADE_GROUP_ID);
      String stageIdStr = (String) propertyMap.get(UPGRADE_ITEM_STAGE_ID);

      if (null == requestIdStr || requestIdStr.isEmpty()) {
        throw new IllegalArgumentException("The upgrade id is required when querying for upgrades");
      }

      if (null == groupIdStr || groupIdStr.isEmpty()) {
        throw new IllegalArgumentException("The upgrade group id is required when querying for upgrades");
      }

      Long requestId = Long.valueOf(requestIdStr);
      Long groupId = Long.valueOf(groupIdStr);
      Long stageId = null;
      if (null != stageIdStr) {
        stageId = Long.valueOf(stageIdStr);
      }

      List<UpgradeItemEntity> entities = new ArrayList<>();
      if (null == stageId) {
        UpgradeGroupEntity group = s_dao.findUpgradeGroup(groupId);

        if (null == group || null == group.getItems()) {
          throw new NoSuchResourceException(String.format("Cannot load upgrade for %s", requestIdStr));
        }

        entities = group.getItems();

      } else {
        UpgradeItemEntity entity = s_dao.findUpgradeItemByRequestAndStage(requestId, stageId);
        if (null != entity) {
          entities.add(entity);
        }
      }

      Map<Long, HostRoleCommandStatusSummaryDTO> requestAggregateCounts = s_hostRoleCommandDAO.findAggregateCounts(requestId);
      Map<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> cache = new HashMap<>();
      cache.put(requestId, requestAggregateCounts);

      // !!! need to do some lookup for stages, so use a stageid -> resource for
      // when that happens
      for (UpgradeItemEntity entity : entities) {
        Resource upgradeItemResource = toResource(entity, requestPropertyIds);

        StageEntityPK stagePrimaryKey = new StageEntityPK();
        stagePrimaryKey.setRequestId(requestId);
        stagePrimaryKey.setStageId(entity.getStageId());

        StageEntity stageEntity = s_stageDao.findByPK(stagePrimaryKey);
        Resource stageResource = StageResourceProvider.toResource(cache, stageEntity,
            StageResourceProvider.PROPERTY_IDS);

        for (String propertyId : StageResourceProvider.PROPERTY_IDS) {
          // Attempt to mask any passwords in fields that are property maps.
          Object value = stageResource.getPropertyValue(propertyId);
          if (StageResourceProvider.PROPERTIES_TO_MASK_PASSWORD_IN.contains(propertyId)
              && value.getClass().equals(String.class) && !StringUtils.isBlank((String) value)) {
            value = SecretReference.maskPasswordInPropertyMap((String) value);
          }

          setResourceProperty(upgradeItemResource, STAGE_MAPPED_IDS.get(propertyId), value,
              requestPropertyIds);
        }

        results.add(upgradeItemResource);
      }
    }
    return results;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  private Resource toResource(UpgradeItemEntity item, Set<String> requestedIds) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.UpgradeItem);

    UpgradeGroupEntity group = item.getGroupEntity();
    UpgradeEntity upgrade = group.getUpgradeEntity();

    setResourceProperty(resource, UPGRADE_REQUEST_ID, upgrade.getRequestId(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_ID, group.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_ITEM_STAGE_ID, item.getStageId(), requestedIds);
    setResourceProperty(resource, UPGRADE_ITEM_TEXT, item.getText(), requestedIds);

    return resource;
  }

}
