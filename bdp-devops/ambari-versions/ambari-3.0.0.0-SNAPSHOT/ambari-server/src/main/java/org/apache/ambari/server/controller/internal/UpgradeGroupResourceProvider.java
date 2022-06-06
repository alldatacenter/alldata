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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
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
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;

import com.google.inject.Inject;

/**
 * Manages groupings of upgrade items.
 */
@StaticallyInject
public class UpgradeGroupResourceProvider extends AbstractControllerResourceProvider {

  protected static final String UPGRADE_REQUEST_ID = "UpgradeGroup/request_id";
  protected static final String UPGRADE_GROUP_ID = "UpgradeGroup/group_id";
  protected static final String UPGRADE_CLUSTER_NAME = "UpgradeGroup/cluster_name";
  protected static final String UPGRADE_GROUP_NAME = "UpgradeGroup/name";
  protected static final String UPGRADE_GROUP_TITLE = "UpgradeGroup/title";
  protected static final String UPGRADE_GROUP_PROGRESS_PERCENT = "UpgradeGroup/progress_percent";
  protected static final String UPGRADE_GROUP_STATUS = "UpgradeGroup/status";
  protected static final String UPGRADE_GROUP_DISPLAY_STATUS = "UpgradeGroup/display_status";

  protected static final String UPGRADE_GROUP_TOTAL_TASKS = "UpgradeGroup/total_task_count";
  protected static final String UPGRADE_GROUP_IN_PROGRESS_TASKS = "UpgradeGroup/in_progress_task_count";
  protected static final String UPGRADE_GROUP_COMPLETED_TASKS = "UpgradeGroup/completed_task_count";


  private static final Set<String> PK_PROPERTY_IDS = new HashSet<>(
    Arrays.asList(UPGRADE_REQUEST_ID, UPGRADE_GROUP_ID));
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  @Inject
  private static UpgradeDAO m_dao = null;

  @Inject
  private static HostRoleCommandDAO s_hostRoleCommandDao;

  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_REQUEST_ID);
    PROPERTY_IDS.add(UPGRADE_GROUP_ID);
    PROPERTY_IDS.add(UPGRADE_GROUP_NAME);
    PROPERTY_IDS.add(UPGRADE_GROUP_TITLE);
    PROPERTY_IDS.add(UPGRADE_GROUP_PROGRESS_PERCENT);
    PROPERTY_IDS.add(UPGRADE_GROUP_STATUS);
    PROPERTY_IDS.add(UPGRADE_GROUP_DISPLAY_STATUS);
    PROPERTY_IDS.add(UPGRADE_GROUP_TOTAL_TASKS);
    PROPERTY_IDS.add(UPGRADE_GROUP_IN_PROGRESS_TASKS);
    PROPERTY_IDS.add(UPGRADE_GROUP_COMPLETED_TASKS);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.UpgradeGroup, UPGRADE_GROUP_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_REQUEST_ID);
  }

  /**
   * Constructor.
   *
   * @param controller the controller
   */
  UpgradeGroupResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.UpgradeGroup, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new SystemException("Upgrade Groups can only be created with an upgrade");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String upgradeIdStr = (String) propertyMap.get(UPGRADE_REQUEST_ID);

      if (null == upgradeIdStr || upgradeIdStr.isEmpty()) {
        throw new IllegalArgumentException("The upgrade id is required when querying for upgrades");
      }

      Long upgradeId = Long.valueOf(upgradeIdStr);
      UpgradeEntity upgrade = m_dao.findUpgradeByRequestId(upgradeId);

      Long requestId = upgrade.getRequestId();

      List<UpgradeGroupEntity> groups = upgrade.getUpgradeGroups();
      if (null != groups) {

        Map<Long, HostRoleCommandStatusSummaryDTO> map = s_hostRoleCommandDao.findAggregateCounts(requestId);

        for (UpgradeGroupEntity group : groups) {
          Resource r = toResource(upgrade, group, requestPropertyIds);

          Set<Long> stageIds = new HashSet<>();
          for (UpgradeItemEntity itemEntity : group.getItems()) {
            stageIds.add(itemEntity.getStageId());
          }

          aggregate(map, r, requestId, stageIds, requestPropertyIds);

          results.add(r);
        }
      }
    }
    return results;
  }

  @Override
  public RequestStatus updateResources(final Request request,
      Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    throw new SystemException("Upgrade groups cannot be modified");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Upgrade groups cannot be removed");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  private Resource toResource(UpgradeEntity upgrade, UpgradeGroupEntity group, Set<String> requestedIds) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.UpgradeGroup);

    setResourceProperty(resource, UPGRADE_REQUEST_ID, upgrade.getRequestId(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_ID, group.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_NAME, group.getName(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_TITLE, group.getTitle(), requestedIds);

    return resource;
  }

  /**
   * Aggregates status, percent complete, and count information for stages and
   * puts the results on the upgrade group
   *
   * @param upgradeGroup  the resource representing an upgrade group
   * @param stageIds      the set of resources ids of the stages
   * @param requestedIds  the ids for the request
   */
  private void aggregate(Map<Long, HostRoleCommandStatusSummaryDTO> smap,
      Resource upgradeGroup, Long requestId, Set<Long> stageIds, Set<String> requestedIds) {

    int size = 0;

    Map<HostRoleStatus, Integer> counts = CalculatedStatus.calculateTaskStatusCounts(smap, stageIds);

    CalculatedStatus stageStatus = CalculatedStatus.statusFromStageSummary(smap, stageIds);

    for (Integer i : counts.values()) {
      size += i.intValue();
    }

    Integer inProgress = 0;
    Integer completed = 0;

    for (Entry<HostRoleStatus, Integer> entry : counts.entrySet()) {
      if (entry.getKey().isCompletedState()) {
        completed += entry.getValue();
      } else if (entry.getKey().isInProgress()) {
        inProgress += entry.getValue();
      }
    }
    setResourceProperty(upgradeGroup, UPGRADE_GROUP_TOTAL_TASKS, size, requestedIds);
    setResourceProperty(upgradeGroup, UPGRADE_GROUP_IN_PROGRESS_TASKS, inProgress, requestedIds);
    setResourceProperty(upgradeGroup, UPGRADE_GROUP_COMPLETED_TASKS, completed, requestedIds);

    setResourceProperty(upgradeGroup, UPGRADE_GROUP_STATUS, stageStatus.getStatus(), requestedIds);
    setResourceProperty(upgradeGroup, UPGRADE_GROUP_DISPLAY_STATUS, stageStatus.getDisplayStatus(), requestedIds);
    setResourceProperty(upgradeGroup, UPGRADE_GROUP_PROGRESS_PERCENT, stageStatus.getPercent(), requestedIds);
  }
}
