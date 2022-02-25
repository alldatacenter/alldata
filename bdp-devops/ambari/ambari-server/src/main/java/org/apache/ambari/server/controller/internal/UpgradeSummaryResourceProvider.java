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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
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
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.state.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
/**
 * Get a summary of an upgrade request.
 */
@StaticallyInject
public class UpgradeSummaryResourceProvider extends AbstractControllerResourceProvider {
  protected static final String UPGRADE_SUMMARY_CLUSTER_NAME = "UpgradeSummary/cluster_name";
  protected static final String UPGRADE_SUMMARY_REQUEST_ID = "UpgradeSummary/request_id";

  protected static final String UPGRADE_SUMMARY_FAIL_REASON = PropertyHelper.getPropertyId("UpgradeSummary", "fail_reason");

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<>(
    Arrays.asList(UPGRADE_SUMMARY_REQUEST_ID, UPGRADE_SUMMARY_CLUSTER_NAME));
  private static final Set<String> PROPERTY_IDS = new HashSet<>();
  private static Map<String, String> TASK_MAPPED_IDS = new HashMap<>();

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  @Inject
  private static UpgradeDAO s_upgradeDAO = null;

  @Inject
  private static HostRoleCommandDAO s_hostRoleCommandDAO = null;

  /**
   * Used to request a resource for a given task.
   */
  @Inject
  private static UpgradeHelper s_upgradeHelper;

  static {
    // Properties
    PROPERTY_IDS.add(UPGRADE_SUMMARY_CLUSTER_NAME);
    PROPERTY_IDS.add(UPGRADE_SUMMARY_REQUEST_ID);
    PROPERTY_IDS.add(UPGRADE_SUMMARY_FAIL_REASON);

    // Inherit all of the properties from a Task as well to return data about the current task if it failed.
    for (String p : TaskResourceProvider.PROPERTY_IDS) {
      TASK_MAPPED_IDS.put(p, p.replace("Tasks/", "UpgradeSummary/failed_task/"));
    }
    PROPERTY_IDS.addAll(TASK_MAPPED_IDS.values());

    // Keys
    KEY_PROPERTY_IDS.put(Resource.Type.UpgradeSummary, UPGRADE_SUMMARY_REQUEST_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, UPGRADE_SUMMARY_CLUSTER_NAME);
  }

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeSummaryResourceProvider.class);

  /**
   * Constructor.
   *
   * @param controller the controller
   */
  public UpgradeSummaryResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.UpgradeSummary, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus createResources(final Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    throw new UnsupportedOperationException("Resource only supports GET operation.");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Set<Resource> resources = new HashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(UPGRADE_SUMMARY_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException(
            "The cluster name is required when querying for upgrades");
      }

      Cluster cluster;
      try {
        cluster = getManagementController().getClusters().getCluster(clusterName);
      } catch (AmbariException e) {
        throw new NoSuchResourceException(
            String.format("Cluster %s could not be loaded", clusterName));
      }

      List<UpgradeEntity> upgrades = new ArrayList<>();
      String upgradeRequestIdStr = (String) propertyMap.get(UPGRADE_SUMMARY_REQUEST_ID);
      if (null != upgradeRequestIdStr) {
        UpgradeEntity upgrade = s_upgradeDAO.findUpgradeByRequestId(Long.valueOf(upgradeRequestIdStr));

        if (null != upgrade) {
          upgrades.add(upgrade);
        }
      } else {
        upgrades = s_upgradeDAO.findUpgrades(cluster.getClusterId());
      }

      for (UpgradeEntity entity : upgrades) {
        Resource resource = new ResourceImpl(Resource.Type.UpgradeSummary);
        Long upgradeRequestId = entity.getRequestId();

        setResourceProperty(resource, UPGRADE_SUMMARY_CLUSTER_NAME, clusterName, requestPropertyIds);
        setResourceProperty(resource, UPGRADE_SUMMARY_REQUEST_ID, entity.getRequestId(), requestPropertyIds);

        HostRoleCommandEntity mostRecentFailure = s_hostRoleCommandDAO.findMostRecentFailure(upgradeRequestId);

        String displayText = null;
        HostRoleCommandEntity failedTask = null;
        if (mostRecentFailure != null) {
          UpgradeSummary summary = new UpgradeSummary(mostRecentFailure);
          displayText = summary.getDisplayText();
          failedTask = summary.getFailedTask();

          Resource taskResource = s_upgradeHelper.getTaskResource(clusterName, failedTask.getRequestId(),
              failedTask.getStageId(), failedTask.getTaskId());

          // Include properties from the failed task.
          if (taskResource != null) {
            for (Map.Entry<String, String> property : TASK_MAPPED_IDS.entrySet()) {
              String taskPropertyId = property.getKey();
              String upgradeSummaryPropertyId = property.getValue();

              setResourceProperty(resource, upgradeSummaryPropertyId, taskResource.getPropertyValue(taskPropertyId), requestPropertyIds);
            }
          }
        }
        setResourceProperty(resource, UPGRADE_SUMMARY_FAIL_REASON, displayText, requestPropertyIds);
        resources.add(resource);
      }
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {
    throw new UnsupportedOperationException("Resource only supports GET operation.");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Resource only supports GET operation.");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }
}
