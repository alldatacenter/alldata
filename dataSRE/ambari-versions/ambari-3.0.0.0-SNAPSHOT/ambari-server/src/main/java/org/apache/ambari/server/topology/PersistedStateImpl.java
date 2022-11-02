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

package org.apache.ambari.server.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.controller.internal.BaseClusterRequest;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.TopologyHostGroupDAO;
import org.apache.ambari.server.orm.dao.TopologyHostInfoDAO;
import org.apache.ambari.server.orm.dao.TopologyHostRequestDAO;
import org.apache.ambari.server.orm.dao.TopologyLogicalRequestDAO;
import org.apache.ambari.server.orm.dao.TopologyLogicalTaskDAO;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.TopologyHostGroupEntity;
import org.apache.ambari.server.orm.entities.TopologyHostInfoEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyHostTaskEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalTaskEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.topology.tasks.TopologyTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

/**
 * Implementation which uses Ambari Database DAO and Entity objects for persistence
 * of topology related information.
 */
@Singleton
public class PersistedStateImpl implements PersistedState {

  private static final Logger LOG = LoggerFactory.getLogger(PersistedState.class);

  @Inject
  private TopologyRequestDAO topologyRequestDAO;

  @Inject
  private TopologyHostInfoDAO topologyHostInfoDAO;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private TopologyHostGroupDAO hostGroupDAO;

  @Inject
  private TopologyHostRequestDAO hostRequestDAO;

  @Inject
  private TopologyLogicalRequestDAO topologyLogicalRequestDAO;

  @Inject
  private TopologyLogicalTaskDAO topologyLogicalTaskDAO;

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private HostRoleCommandDAO physicalTaskDAO;

  @Inject
  private BlueprintFactory blueprintFactory;

  @Inject
  private LogicalRequestFactory logicalRequestFactory;

  @Inject
  private AmbariContext ambariContext;

  private static Gson jsonSerializer = new Gson();


  @Override
  public  PersistedTopologyRequest persistTopologyRequest(BaseClusterRequest request) {
    TopologyRequestEntity requestEntity = toEntity(request);
    topologyRequestDAO.create(requestEntity);
    return new PersistedTopologyRequest(requestEntity.getId(), request);
  }

  @Override
  public void persistLogicalRequest(LogicalRequest logicalRequest, long topologyRequestId) {
    TopologyRequestEntity topologyRequestEntity = topologyRequestDAO.findById(topologyRequestId);
    TopologyLogicalRequestEntity entity = toEntity(logicalRequest, topologyRequestEntity);
    topologyRequestEntity.setTopologyLogicalRequestEntity(entity);
    //todo: how to handle missing topology request entity?

    //logicalRequestDAO.create(entity);

    topologyRequestDAO.merge(topologyRequestEntity);
  }

  @Override
  @Transactional
  public void removeHostRequests(long logicalRequestId, Collection<HostRequest> hostRequests) {
    TopologyLogicalRequestEntity logicalRequest = topologyLogicalRequestDAO.findById(logicalRequestId);
    for (HostRequest hostRequest : hostRequests) {
      TopologyHostRequestEntity hostRequestEntity = hostRequestDAO.findById(hostRequest.getId());
      if (logicalRequest != null)  {
        logicalRequest.getTopologyHostRequestEntities().remove(hostRequestEntity);
      }
      hostRequestDAO.remove(hostRequestEntity);
    }
    if (logicalRequest != null && logicalRequest.getTopologyHostRequestEntities().isEmpty()) {
      Long topologyRequestId = logicalRequest.getTopologyRequestId();
      topologyLogicalRequestDAO.remove(logicalRequest);
      topologyRequestDAO.removeByPK(topologyRequestId);
    }
  }

  @Override
  public void setHostRequestStatus(long hostRequestId, HostRoleStatus status, String message) {
    TopologyHostRequestEntity hostRequestEntity = hostRequestDAO.findById(hostRequestId);
    if (hostRequestEntity != null) {
      hostRequestEntity.setStatus(status);
      hostRequestEntity.setStatusMessage(message);
      hostRequestDAO.merge(hostRequestEntity);
    }
  }

  @Override
  public void registerPhysicalTask(long logicalTaskId, long physicalTaskId) {
    TopologyLogicalTaskEntity entity = topologyLogicalTaskDAO.findById(logicalTaskId);
    HostRoleCommandEntity physicalEntity = hostRoleCommandDAO.findByPK(physicalTaskId);
    entity.setHostRoleCommandEntity(physicalEntity);

    topologyLogicalTaskDAO.merge(entity);
  }

  @Override
  public void registerHostName(long hostRequestId, String hostName) {
    TopologyHostRequestEntity entity = hostRequestDAO.findById(hostRequestId);
    if (entity.getHostName() == null) {
      entity.setHostName(hostName);
      hostRequestDAO.merge(entity);
    }
  }

  @Override
  public void registerInTopologyHostInfo(Host host) {
    TopologyHostInfoEntity entity = topologyHostInfoDAO.findByHostname(host.getHostName());
    if(entity != null && entity.getHostEntity() == null) {
      entity.setHostEntity(hostDAO.findById(host.getHostId()));
      topologyHostInfoDAO.merge(entity);
    }
  }

  @Override
  public LogicalRequest getProvisionRequest(long clusterId) {
    Collection<TopologyRequestEntity> entities = topologyRequestDAO.findByClusterId(clusterId);
    for (TopologyRequestEntity entity : entities) {
      if(TopologyRequest.Type.PROVISION == TopologyRequest.Type.valueOf(entity.getAction())) {
        TopologyLogicalRequestEntity logicalRequestEntity = entity.getTopologyLogicalRequestEntity();
        TopologyRequest replayedRequest = new ReplayedTopologyRequest(entity, blueprintFactory);
        try {
          ClusterTopology clusterTopology = new ClusterTopologyImpl(ambariContext, replayedRequest);
          Long logicalId = logicalRequestEntity.getId();
          return logicalRequestFactory.createRequest(logicalId, replayedRequest, clusterTopology, logicalRequestEntity);
        } catch (InvalidTopologyException e) {
          throw new RuntimeException("Failed to construct cluster topology while replaying request: " + e, e);
        } catch (AmbariException e) {
          throw new RuntimeException("Failed to construct logical request during replay: " + e, e);
        }
      }
    }

    return null;
  }

  @Override
  public Map<ClusterTopology, List<LogicalRequest>> getAllRequests() {
    //todo: we only currently support a single request per ambari instance so there should only
    //todo: be a single cluster topology
    Map<ClusterTopology, List<LogicalRequest>> allRequests = new HashMap<>();
    Collection<TopologyRequestEntity> entities = topologyRequestDAO.findAll();

    Map<Long, ClusterTopology> topologyRequests = new HashMap<>();
    for (TopologyRequestEntity entity : entities) {
      TopologyRequest replayedRequest = new ReplayedTopologyRequest(entity, blueprintFactory);
      ClusterTopology clusterTopology = topologyRequests.get(replayedRequest.getClusterId());
      if (clusterTopology == null) {
        try {
          clusterTopology = new ClusterTopologyImpl(ambariContext, replayedRequest);
          if (entity.getProvisionAction() != null) {
            clusterTopology.setProvisionAction(entity.getProvisionAction());
          }
          topologyRequests.put(replayedRequest.getClusterId(), clusterTopology);
          allRequests.put(clusterTopology, new ArrayList<>());
        } catch (InvalidTopologyException e) {
          throw new RuntimeException("Failed to construct cluster topology while replaying request: " + e, e);
        }
      } else {
        // ensure all host groups are provided in the combined cluster topology
        for (Map.Entry<String, HostGroupInfo> groupInfoEntry : replayedRequest.getHostGroupInfo().entrySet()) {
          String name = groupInfoEntry.getKey();
          if (! clusterTopology.getHostGroupInfo().containsKey(name)) {
            clusterTopology.getHostGroupInfo().put(name, groupInfoEntry.getValue());
          }
        }
      }

      TopologyLogicalRequestEntity logicalRequestEntity = entity.getTopologyLogicalRequestEntity();
      if (logicalRequestEntity != null) {
        try {
          Long logicalId = logicalRequestEntity.getId();

          //todo: fix initialization of ActionManager.requestCounter to account for logical requests
          //todo: until this is fixed, increment the counter for every recovered logical request
          //todo: this will cause gaps in the request id's after recovery
          ambariContext.getNextRequestId();
          allRequests.get(clusterTopology).add(logicalRequestFactory.createRequest(
                  logicalId, replayedRequest, clusterTopology, logicalRequestEntity));
        } catch (AmbariException e) {
          throw new RuntimeException("Failed to construct logical request during replay: " + e, e);
        }
      }

    }

    return allRequests;
  }

  private TopologyRequestEntity toEntity(BaseClusterRequest request) {
    TopologyRequestEntity entity = new TopologyRequestEntity();

    //todo: this isn't set for a scaling operation because we had intended to allow multiple
    //todo: bp's to be used to scale a cluster although this isn't currently supported by
    //todo: new topology infrastructure
    entity.setAction(request.getType().name());
    if (request.getBlueprint() != null) {
      entity.setBlueprintName(request.getBlueprint().getName());
    }

    entity.setClusterAttributes(attributesAsString(request.getConfiguration().getAttributes()));
    entity.setClusterId(request.getClusterId());
    entity.setClusterProperties(propertiesAsString(request.getConfiguration().getProperties()));
    entity.setDescription(request.getDescription());

    if (request.getProvisionAction() != null) {
      entity.setProvisionAction(request.getProvisionAction());
    }

    // host groups
    Collection<TopologyHostGroupEntity> hostGroupEntities = new ArrayList<>();
    for (HostGroupInfo groupInfo : request.getHostGroupInfo().values())  {
      hostGroupEntities.add(toEntity(groupInfo, entity));
    }
    entity.setTopologyHostGroupEntities(hostGroupEntities);

    return entity;
  }

  private TopologyLogicalRequestEntity toEntity(LogicalRequest request, TopologyRequestEntity topologyRequestEntity) {
    TopologyLogicalRequestEntity entity = new TopologyLogicalRequestEntity();

    entity.setDescription(request.getRequestContext());
    entity.setId(request.getRequestId());
    entity.setTopologyRequestEntity(topologyRequestEntity);
    entity.setTopologyRequestId(topologyRequestEntity.getId());

    // host requests
    Collection<TopologyHostRequestEntity> hostRequests = new ArrayList<>();
    entity.setTopologyHostRequestEntities(hostRequests);
    for (HostRequest hostRequest : request.getHostRequests()) {
      hostRequests.add(toEntity(hostRequest, entity));
    }
    return entity;
  }

  private TopologyHostRequestEntity toEntity(HostRequest request, TopologyLogicalRequestEntity logicalRequestEntity) {
    TopologyHostRequestEntity entity = new TopologyHostRequestEntity();
    entity.setHostName(request.getHostName());
    entity.setId(request.getId());
    entity.setStageId(request.getStageId());

    entity.setTopologyLogicalRequestEntity(logicalRequestEntity);
    entity.setTopologyHostGroupEntity(hostGroupDAO.findByRequestIdAndName(
        logicalRequestEntity.getTopologyRequestId(), request.getHostgroupName()));

    // logical tasks
    Collection<TopologyHostTaskEntity> hostRequestTaskEntities = new ArrayList<>();
    entity.setTopologyHostTaskEntities(hostRequestTaskEntities);
    // for now only worry about install and start tasks
    for (TopologyTask task : request.getTopologyTasks()) {
      if (task.getType() == TopologyTask.Type.INSTALL || task.getType() == TopologyTask.Type.START) {
        TopologyHostTaskEntity topologyTaskEntity = new TopologyHostTaskEntity();
        hostRequestTaskEntities.add(topologyTaskEntity);
        topologyTaskEntity.setType(task.getType().name());
        topologyTaskEntity.setTopologyHostRequestEntity(entity);
        Collection<TopologyLogicalTaskEntity> logicalTaskEntities = new ArrayList<>();
        topologyTaskEntity.setTopologyLogicalTaskEntities(logicalTaskEntities);
        for (Long logicalTaskId : request.getLogicalTasksForTopologyTask(task).values()) {
          TopologyLogicalTaskEntity logicalTaskEntity = new TopologyLogicalTaskEntity();
          logicalTaskEntities.add(logicalTaskEntity);
          HostRoleCommand logicalTask = request.getLogicalTask(logicalTaskId);
          logicalTaskEntity.setId(logicalTaskId);
          logicalTaskEntity.setComponentName(logicalTask.getRole().name());
          logicalTaskEntity.setTopologyHostTaskEntity(topologyTaskEntity);
          Long physicalId = request.getPhysicalTaskId(logicalTaskId);
          if (physicalId != null) {
            logicalTaskEntity.setHostRoleCommandEntity(physicalTaskDAO.findByPK(physicalId));
          }
          logicalTaskEntity.setTopologyHostTaskEntity(topologyTaskEntity);
        }
      }
    }
    return entity;
  }

  private TopologyHostGroupEntity toEntity(HostGroupInfo groupInfo, TopologyRequestEntity topologyRequestEntity) {
    TopologyHostGroupEntity entity = new TopologyHostGroupEntity();
    entity.setGroupAttributes(attributesAsString(groupInfo.getConfiguration().getAttributes()));
    entity.setGroupProperties(propertiesAsString(groupInfo.getConfiguration().getProperties()));
    entity.setName(groupInfo.getHostGroupName());
    entity.setTopologyRequestEntity(topologyRequestEntity);

    // host info
    Collection<TopologyHostInfoEntity> hostInfoEntities = new ArrayList<>();
    entity.setTopologyHostInfoEntities(hostInfoEntities);

    Collection<String> hosts = groupInfo.getHostNames();
    if (hosts.isEmpty()) {
      TopologyHostInfoEntity hostInfoEntity = new TopologyHostInfoEntity();
      hostInfoEntity.setTopologyHostGroupEntity(entity);
      hostInfoEntity.setHostCount(groupInfo.getRequestedHostCount());
      if (groupInfo.getPredicate() != null) {
        hostInfoEntity.setPredicate(groupInfo.getPredicateString());
      }
      hostInfoEntities.add(hostInfoEntity);
    } else {
      for (String hostName : hosts) {
        TopologyHostInfoEntity hostInfoEntity = new TopologyHostInfoEntity();
        hostInfoEntity.setTopologyHostGroupEntity(entity);
        if (groupInfo.getPredicate() != null) {
          hostInfoEntity.setPredicate(groupInfo.getPredicateString());
        }
        hostInfoEntity.setFqdn(hostName);
        hostInfoEntity.setRackInfo(groupInfo.getHostRackInfo().get(hostName));
        hostInfoEntity.setHostCount(0);
        hostInfoEntities.add(hostInfoEntity);
      }
    }
    return entity;
  }

  private static String propertiesAsString(Map<String, Map<String, String>> configurationProperties) {
    return jsonSerializer.toJson(configurationProperties);
  }

  private static String attributesAsString(Map<String, Map<String, Map<String, String>>> configurationAttributes) {
    return jsonSerializer.toJson(configurationAttributes);
  }

  private static class ReplayedTopologyRequest implements TopologyRequest {
    private final Long clusterId;
    private final Type type;
    private final String description;
    private final Blueprint blueprint;
    private final Configuration configuration;
    private final Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();

    public ReplayedTopologyRequest(TopologyRequestEntity entity, BlueprintFactory blueprintFactory) {
      clusterId = entity.getClusterId();
      type = Type.valueOf(entity.getAction());
      description = entity.getDescription();

      try {
        blueprint = blueprintFactory.getBlueprint(entity.getBlueprintName());
      } catch (NoSuchStackException e) {
        throw new RuntimeException("Unable to load blueprint while replaying topology request: " + e, e);
      }
      configuration = createConfiguration(entity.getClusterProperties(), entity.getClusterAttributes());
      configuration.setParentConfiguration(blueprint.getConfiguration());

      parseHostGroupInfo(entity);
    }

    @Override
    public Long getClusterId() {
      return clusterId;
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public Blueprint getBlueprint() {
      return blueprint;
    }

    @Override
    public Configuration getConfiguration() {
      return configuration;
    }

    @Override
    public Map<String, HostGroupInfo> getHostGroupInfo() {
      return hostGroupInfoMap;
    }


    @Override
    public String getDescription() {
      return description;
    }

    private Configuration createConfiguration(String propString, String attributeString) {
      Map<String, Map<String, String>> properties = jsonSerializer.
          <Map<String, Map<String, String>>>fromJson(propString, Map.class);

      Map<String, Map<String, Map<String, String>>> attributes = jsonSerializer.
          <Map<String, Map<String, Map<String, String>>>>fromJson(attributeString, Map.class);

      //todo: config parent
      return new Configuration(properties, attributes);
    }

    private void parseHostGroupInfo(TopologyRequestEntity entity) {
      for (TopologyHostGroupEntity hostGroupEntity : entity.getTopologyHostGroupEntities()) {
        for (TopologyHostInfoEntity hostInfoEntity : hostGroupEntity.getTopologyHostInfoEntities()) {
          String groupName = hostGroupEntity.getName();
          HostGroupInfo groupInfo = hostGroupInfoMap.get(groupName);
          if (groupInfo == null) {
            groupInfo = new HostGroupInfo(groupName);
            hostGroupInfoMap.put(groupName, groupInfo);
          }

          // if host names are specified, there will be one group info entity per name
          // otherwise there is a single entity with requested count and predicate
          String hostname = hostInfoEntity.getFqdn();
          if (hostname != null && ! hostname.isEmpty()) {
            groupInfo.addHost(hostname);
            groupInfo.addHostRackInfo(hostname, hostInfoEntity.getRackInfo());
          } else {
            // should not be more than one group info if host count is specified
            groupInfo.setRequestedCount(hostInfoEntity.getHostCount());
            String hostPredicate = hostInfoEntity.getPredicate();
            if (hostPredicate != null) {
              try {
                groupInfo.setPredicate(hostPredicate);
              } catch (InvalidQueryException e) {
                // log error but proceed with now predicate set
                LOG.error(String.format(
                    "Failed to compile predicate '%s' during request replay: %s", hostPredicate, e), e);
              }
            }
          }

          String groupConfigProperties = hostGroupEntity.getGroupProperties();
          String groupConfigAttributes = hostGroupEntity.getGroupAttributes();
          groupInfo.setConfiguration(createConfiguration(groupConfigProperties, groupConfigAttributes));
        }
      }
    }
  }
}
