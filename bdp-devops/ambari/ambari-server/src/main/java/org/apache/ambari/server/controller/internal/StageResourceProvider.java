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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.ExtendedResourceProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.topology.LogicalRequest;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.SecretReference;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * ResourceProvider for Stage
 */
@StaticallyInject
public class StageResourceProvider extends AbstractControllerResourceProvider implements ExtendedResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(StageResourceProvider.class);

  /**
   * Used for querying stage resources.
   */
  @Inject
  private static StageDAO dao = null;

  /**
   * Used for querying task resources.
   */
  @Inject
  private static HostRoleCommandDAO hostRoleCommandDAO = null;

  @Inject
  private static Provider<Clusters> clustersProvider = null;

  @Inject
  private static TopologyManager topologyManager;

  /**
   * Stage property constants.
   */
  public static final String STAGE_STAGE_ID = "Stage/stage_id";
  public static final String STAGE_CLUSTER_NAME = "Stage/cluster_name";
  public static final String STAGE_REQUEST_ID = "Stage/request_id";
  public static final String STAGE_LOG_INFO = "Stage/log_info";
  public static final String STAGE_CONTEXT = "Stage/context";
  public static final String STAGE_COMMAND_PARAMS = "Stage/command_params";
  public static final String STAGE_HOST_PARAMS = "Stage/host_params";
  public static final String STAGE_SKIPPABLE = "Stage/skippable";
  public static final String STAGE_PROGRESS_PERCENT = "Stage/progress_percent";
  public static final String STAGE_STATUS = "Stage/status";
  public static final String STAGE_DISPLAY_STATUS = "Stage/display_status";
  public static final String STAGE_START_TIME = "Stage/start_time";
  public static final String STAGE_END_TIME = "Stage/end_time";

  /**
   * The property ids for a stage resource.
   */
  static final Set<String> PROPERTY_IDS = ImmutableSet.<String>builder()
    .add(STAGE_STAGE_ID)
    .add(STAGE_CLUSTER_NAME)
    .add(STAGE_REQUEST_ID)
    .add(STAGE_LOG_INFO)
    .add(STAGE_CONTEXT)
    .add(STAGE_COMMAND_PARAMS)
    .add(STAGE_HOST_PARAMS)
    .add(STAGE_SKIPPABLE)
    .add(STAGE_PROGRESS_PERCENT)
    .add(STAGE_STATUS)
    .add(STAGE_DISPLAY_STATUS)
    .add(STAGE_START_TIME)
    .add(STAGE_END_TIME)
    .build();

  /**
   * The key property ids for a stage resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = ImmutableMap.<Resource.Type, String>builder()
    .put(Resource.Type.Stage, STAGE_STAGE_ID)
    .put(Resource.Type.Cluster, STAGE_CLUSTER_NAME)
    .put(Resource.Type.Request, STAGE_REQUEST_ID)
    .build();

  static {
    // properties

    // keys
  }

  /**
   * These fields may contain password in them, so have to mask with.
   */
  static final Set<String> PROPERTIES_TO_MASK_PASSWORD_IN = ImmutableSet.of(STAGE_COMMAND_PARAMS, STAGE_HOST_PARAMS);

  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor.
   *
   * @param managementController  the Ambari management controller
   */
  StageResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.Stage, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(KEY_PROPERTY_IDS.values());
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {

      Map<String,Object> updateProperties = iterator.next();

      List<StageEntity> entities = dao.findAll(request, predicate);
      for (StageEntity entity : entities) {

        String stageStatus = (String) updateProperties.get(STAGE_STATUS);
        if (stageStatus != null) {
          HostRoleStatus desiredStatus = HostRoleStatus.valueOf(stageStatus);
          dao.updateStageStatus(entity, desiredStatus,
              getManagementController().getActionManager());
        }
      }
    }
    notifyUpdate(Resource.Type.Stage, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results     = new LinkedHashSet<>();
    Set<String>   propertyIds = getRequestPropertyIds(request, predicate);

    // !!! poor mans cache.  toResource() shouldn't be calling the db
    // every time, when the request id is likely the same for each stageEntity
    Map<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> cache = new HashMap<>();

    List<StageEntity> entities = dao.findAll(request, predicate);
    for (StageEntity entity : entities) {
      results.add(StageResourceProvider.toResource(cache, entity, propertyIds));
    }

    cache.clear();

    // !!! check the id passed to see if it's a LogicalRequest.  This safeguards against
    // iterating all stages for all requests.  That is a problem when the request
    // is for an Upgrade, but was pulling the data anyway.
    Map<String, Object> map = PredicateHelper.getProperties(predicate);

    if (map.containsKey(STAGE_REQUEST_ID)) {
      Long requestId = NumberUtils.toLong(map.get(STAGE_REQUEST_ID).toString());
      LogicalRequest lr = topologyManager.getRequest(requestId);

      if (null != lr) {
        Collection<StageEntity> topologyManagerStages = lr.getStageEntities();
        // preload summaries as it contains summaries for all stages within this request
        Map<Long, HostRoleCommandStatusSummaryDTO> summary = topologyManager.getStageSummaries(requestId);
        cache.put(requestId, summary);
        for (StageEntity entity : topologyManagerStages) {
          Resource stageResource = toResource(cache, entity, propertyIds);
          if (predicate.evaluate(stageResource)) {
            results.add(stageResource);
          }
        }
      }
    } else {
      Collection<StageEntity> topologyManagerStages = topologyManager.getStages();
      for (StageEntity entity : topologyManagerStages) {
        if (!cache.containsKey(entity.getRequestId())) {
          Map<Long, HostRoleCommandStatusSummaryDTO> summary = topologyManager.getStageSummaries(entity.getRequestId());
          cache.put(entity.getRequestId(), summary);
        }
        Resource stageResource = toResource(cache, entity, propertyIds);
        if (predicate.evaluate(stageResource)) {
          results.add(stageResource);
        }
      }
    }

    return results;
  }


  // ----- ExtendedResourceProvider ------------------------------------------

  @Override
  public QueryResponse queryForResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = getResources(request, predicate);

    return new QueryResponseImpl(results, request.getSortRequest() != null, false, results.size());
  }

  /**
   * Converts the {@link StageEntity} to a {@link Resource}.
   *
   * @param entity        the entity to convert (not {@code null})
   * @param requestedIds  the properties requested (not {@code null})
   *
   * @return the new resource
   */
  static Resource toResource(
      Map<Long, Map<Long, HostRoleCommandStatusSummaryDTO>> cache,
      StageEntity entity,
      Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.Stage);

    Long clusterId = entity.getClusterId();
    if (clusterId != null && !clusterId.equals(Long.valueOf(-1L))) {
      try {
        Cluster cluster = clustersProvider.get().getClusterById(clusterId);

        setResourceProperty(resource, STAGE_CLUSTER_NAME, cluster.getClusterName(), requestedIds);
      } catch (Exception e) {
        LOG.error("Can not get information for cluster " + clusterId + ".", e );
      }
    }

    if (!cache.containsKey(entity.getRequestId())) {
      cache.put(entity.getRequestId(), hostRoleCommandDAO.findAggregateCounts(entity.getRequestId()));
    }

    Map<Long, HostRoleCommandStatusSummaryDTO> summary = cache.get(entity.getRequestId());

    setResourceProperty(resource, STAGE_STAGE_ID, entity.getStageId(), requestedIds);
    setResourceProperty(resource, STAGE_REQUEST_ID, entity.getRequestId(), requestedIds);
    setResourceProperty(resource, STAGE_CONTEXT, entity.getRequestContext(), requestedIds);

    if (isPropertyRequested(STAGE_COMMAND_PARAMS, requestedIds)) {
      String value = entity.getCommandParamsStage();
      if (!StringUtils.isBlank(value)) {
        value = SecretReference.maskPasswordInPropertyMap(value);
      }
      resource.setProperty(STAGE_COMMAND_PARAMS, value);
    }

    // this property is lazy loaded in JPA; don't use it unless requested
    if (isPropertyRequested(STAGE_HOST_PARAMS, requestedIds)) {
      String value = entity.getHostParamsStage();
      if (!StringUtils.isBlank(value)) {
        value = SecretReference.maskPasswordInPropertyMap(value);
      }
      resource.setProperty(STAGE_HOST_PARAMS, value);
    }

    setResourceProperty(resource, STAGE_SKIPPABLE, entity.isSkippable(), requestedIds);

    Long startTime = Long.MAX_VALUE;
    Long endTime = 0L;
    if (summary.containsKey(entity.getStageId())) {
      startTime = summary.get(entity.getStageId()).getStartTime();
      endTime = summary.get(entity.getStageId()).getEndTime();
    }

    setResourceProperty(resource, STAGE_START_TIME, startTime, requestedIds);
    setResourceProperty(resource, STAGE_END_TIME, endTime, requestedIds);

    CalculatedStatus status;
    if (summary.isEmpty()) {
      // Delete host might have cleared all HostRoleCommands
      status = CalculatedStatus.COMPLETED;
    } else {
      status = CalculatedStatus.statusFromStageSummary(summary, Collections.singleton(entity.getStageId()));
    }

    setResourceProperty(resource, STAGE_PROGRESS_PERCENT, status.getPercent(), requestedIds);
    setResourceProperty(resource, STAGE_STATUS, status.getStatus(), requestedIds);
    setResourceProperty(resource, STAGE_DISPLAY_STATUS, status.getDisplayStatus(), requestedIds);

    return resource;
  }

}
