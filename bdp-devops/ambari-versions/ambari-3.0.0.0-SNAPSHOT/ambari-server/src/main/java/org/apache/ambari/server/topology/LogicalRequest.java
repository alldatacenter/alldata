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

package org.apache.ambari.server.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ShortTaskStatus;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.TopologyHostGroupEntity;
import org.apache.ambari.server.orm.entities.TopologyHostInfoEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalRequestEntity;
import org.apache.ambari.server.state.Host;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;


/**
 * Logical Request implementation used to provision a cluster deployed by Blueprints.
 */
public class LogicalRequest extends Request {

  private final Collection<HostRequest> allHostRequests = new ArrayList<>();
  // sorted set with master host requests given priority
  private final Collection<HostRequest> outstandingHostRequests = new TreeSet<>();
  private final Map<String, HostRequest> requestsWithReservedHosts = new HashMap<>();

  private final ClusterTopology topology;

  private static AmbariManagementController controller;

  private static final AtomicLong hostIdCounter = new AtomicLong(1);

  private final static Logger LOG = LoggerFactory.getLogger(LogicalRequest.class);


  public LogicalRequest(Long id, TopologyRequest request, ClusterTopology topology)
      throws AmbariException {

    //todo: abstract usage of controller, etc ...
    super(id, topology.getClusterId(), getController().getClusters());

    setRequestContext(String.format("Logical Request: %s", request.getDescription()));

    this.topology = topology;
    createHostRequests(request, topology);
  }

  public LogicalRequest(Long id, TopologyRequest request, ClusterTopology topology,
                        TopologyLogicalRequestEntity requestEntity) throws AmbariException {

    //todo: abstract usage of controller, etc ...
    super(id, topology.getClusterId(), getController().getClusters());

    setRequestContext(String.format("Logical Request: %s", request.getDescription()));

    this.topology = topology;
    createHostRequests(topology, requestEntity);
  }

  public HostOfferResponse offer(Host host) {
    // attempt to match to a host request with an explicit host reservation first
    synchronized (requestsWithReservedHosts) {
      LOG.info("LogicalRequest.offer: attempting to match a request to a request for a reserved host to hostname = {}", host.getHostName());
      HostRequest hostRequest = requestsWithReservedHosts.remove(host.getHostName());
      if (hostRequest != null) {
        HostOfferResponse response = hostRequest.offer(host);
        if (response.getAnswer() != HostOfferResponse.Answer.ACCEPTED) {
          // host request rejected host that it explicitly requested
          throw new RuntimeException("LogicalRequest declined host offer of explicitly requested host: " +
              host.getHostName());
        } else {
          LOG.info("LogicalRequest.offer: request mapping ACCEPTED for host = {}", host.getHostName());
        }

        LOG.info("LogicalRequest.offer returning response, reservedHost list size = {}", requestsWithReservedHosts.size());

        return response;
      }
    }

    // not explicitly reserved, at least not in this request, so attempt to match to outstanding host requests
    boolean predicateRejected = false;
    synchronized (outstandingHostRequests) {
      //todo: prioritization of master host requests
      Iterator<HostRequest> hostRequestIterator = outstandingHostRequests.iterator();
      while (hostRequestIterator.hasNext()) {
        LOG.info("LogicalRequest.offer: attempting to match a request to a request for a non-reserved host to hostname = {}", host.getHostName());
        HostOfferResponse response = hostRequestIterator.next().offer(host);
        switch (response.getAnswer()) {
          case ACCEPTED:
            hostRequestIterator.remove();
            LOG.info("LogicalRequest.offer: host request matched to non-reserved host, hostname = {}, host request has been removed from list", host.getHostName());
            return response;
          case DECLINED_DONE:
            //todo: should have been done on ACCEPT
            hostRequestIterator.remove();
            LOG.info("LogicalRequest.offer: host request returned DECLINED_DONE for hostname = {}, host request has been removed from list", host.getHostName());
            break;
          case DECLINED_PREDICATE:
            LOG.info("LogicalRequest.offer: host request returned DECLINED_PREDICATE for hostname = {}", host.getHostName());
            predicateRejected = true;
            break;
        }
      }

      LOG.info("LogicalRequest.offer: outstandingHost request list size = " + outstandingHostRequests.size());
    }

    // if at least one outstanding host request rejected for predicate or we have an outstanding request
    // with a reserved host decline due to predicate, otherwise decline due to all hosts being resolved
    return predicateRejected || ! requestsWithReservedHosts.isEmpty() ?
            HostOfferResponse.DECLINED_DUE_TO_PREDICATE :
            HostOfferResponse.DECLINED_DUE_TO_DONE;
  }

  @Override
  public List<HostRoleCommand> getCommands() {
    List<HostRoleCommand> commands = new ArrayList<>();
    for (HostRequest hostRequest : allHostRequests) {
      commands.addAll(new ArrayList<>(hostRequest.getLogicalTasks()));
    }
    return commands;
  }

  public Collection<String> getReservedHosts() {
    return requestsWithReservedHosts.keySet();
  }

  public boolean hasPendingHostRequests() {
    return !(requestsWithReservedHosts.isEmpty() && outstandingHostRequests.isEmpty());
  }

  public Collection<HostRequest> getCompletedHostRequests() {
    Collection<HostRequest> completedHostRequests = new ArrayList<>(allHostRequests);
    completedHostRequests.removeAll(outstandingHostRequests);
    completedHostRequests.removeAll(requestsWithReservedHosts.values());

    return completedHostRequests;
  }

  public int getPendingHostRequestCount() {
    return outstandingHostRequests.size() + requestsWithReservedHosts.size();
  }

  //todo: this is only here for toEntity() functionality
  public Collection<HostRequest> getHostRequests() {
    return new ArrayList<>(allHostRequests);
  }

  /**
   * Removes pending host requests (outstanding requests not picked up by any host, where hostName is null) for a host group.
   * @param hostGroupName
   * @return
   */
  public Collection<HostRequest> removePendingHostRequests(String hostGroupName) {
    Collection<HostRequest> pendingHostRequests = new ArrayList<>();
    for(HostRequest hostRequest : outstandingHostRequests) {
      if(hostGroupName == null || hostRequest.getHostgroupName().equals(hostGroupName)) {
        pendingHostRequests.add(hostRequest);
      }
    }
    if (hostGroupName == null) {
      outstandingHostRequests.clear();
    } else {
      outstandingHostRequests.removeAll(pendingHostRequests);
    }

    Collection<String> pendingReservedHostNames = new ArrayList<>();
    for(String reservedHostName : requestsWithReservedHosts.keySet()) {
      HostRequest hostRequest = requestsWithReservedHosts.get(reservedHostName);
      if(hostGroupName == null || hostRequest.getHostgroupName().equals(hostGroupName)) {
        pendingHostRequests.add(hostRequest);
        pendingReservedHostNames.add(reservedHostName);
      }
    }
    requestsWithReservedHosts.keySet().removeAll(pendingReservedHostNames);

    allHostRequests.removeAll(pendingHostRequests);
    return pendingHostRequests;
  }

  public Map<String, Collection<String>> getProjectedTopology() {
    Map<String, Collection<String>> hostComponentMap = new HashMap<>();

    //todo: synchronization
    for (HostRequest hostRequest : allHostRequests) {
      HostGroup hostGroup = hostRequest.getHostGroup();
      for (String host : topology.getHostGroupInfo().get(hostGroup.getName()).getHostNames()) {
        Collection<String> hostComponents = hostComponentMap.get(host);
        if (hostComponents == null) {
          hostComponents = new HashSet<>();
          hostComponentMap.put(host, hostComponents);
        }
        hostComponents.addAll(hostGroup.getComponentNames());
      }
    }
    return hostComponentMap;
  }

  // currently we are just returning all stages for all requests
  //TODO technically StageEntity is simply a container for HostRequest info with additional redundant transformations
  public Collection<StageEntity> getStageEntities() {
    Collection<StageEntity> stages = new ArrayList<>();
    for (HostRequest hostRequest : allHostRequests) {
      StageEntity stage = new StageEntity();
      stage.setStageId(hostRequest.getStageId());
      stage.setRequestContext(getRequestContext());
      stage.setRequestId(getRequestId());
      //todo: not sure what this byte array is???
      //stage.setClusterHostInfo();
      stage.setClusterId(getClusterId());
      boolean skipFailure = hostRequest.shouldSkipFailure();
      stage.setSkippable(skipFailure);
      stage.setAutoSkipFailureSupported(skipFailure);
      // getTaskEntities() sync's state with physical tasks
      stage.setHostRoleCommands(hostRequest.getTaskEntities());

      stages.add(stage);
    }
    return stages;
  }

  public RequestStatusResponse getRequestStatus() {
    RequestStatusResponse requestStatus = new RequestStatusResponse(getRequestId());
    requestStatus.setRequestContext(getRequestContext());

    // convert HostRoleCommands to ShortTaskStatus
    List<ShortTaskStatus> shortTasks = new ArrayList<>();
    for (HostRoleCommand task : getCommands()) {
      shortTasks.add(new ShortTaskStatus(task));
    }
    requestStatus.setTasks(shortTasks);

    return requestStatus;
  }

  public Map<Long, HostRoleCommandStatusSummaryDTO> getStageSummaries() {
    Map<Long, HostRoleCommandStatusSummaryDTO> summaryMap = new HashMap<>();

    Map<Long, Collection<HostRoleCommand>> stageTasksMap = new HashMap<>();

    Map<Long, Long> taskToStageMap = new HashMap<>();


    for (HostRequest hostRequest : getHostRequests()) {
      Map<Long, Long> physicalTaskMapping = hostRequest.getPhysicalTaskMapping();
      Collection<Long> stageTasks = physicalTaskMapping.values();
      for (Long stageTask : stageTasks) {
        taskToStageMap.put(stageTask, hostRequest.getStageId());
      }
    }

    Collection<HostRoleCommand> physicalTasks = topology.getAmbariContext().getPhysicalTasks(taskToStageMap.keySet());

    for (HostRoleCommand physicalTask : physicalTasks) {
      Long stageId = taskToStageMap.get(physicalTask.getTaskId());
      Collection<HostRoleCommand> stageTasks = stageTasksMap.get(stageId);
      if (stageTasks == null) {
        stageTasks = new ArrayList<>();
        stageTasksMap.put(stageId, stageTasks);
      }
      stageTasks.add(physicalTask);
    }

    for (Long stageId : stageTasksMap.keySet()) {
      //Number minStartTime = 0;
      //Number maxEndTime = 0;
      int aborted = 0;
      int completed = 0;
      int failed = 0;
      int holding = 0;
      int holdingFailed = 0;
      int holdingTimedout = 0;
      int inProgress = 0;
      int pending = 0;
      int queued = 0;
      int timedout = 0;
      int skippedFailed = 0;

      //todo: where does this logic belong?
      for (HostRoleCommand task : stageTasksMap.get(stageId)) {
        HostRoleStatus taskStatus = task.getStatus();

        switch (taskStatus) {
          case ABORTED:
            aborted += 1;
            break;
          case COMPLETED:
            completed += 1;
            break;
          case FAILED:
            failed += 1;
            break;
          case HOLDING:
            holding += 1;
            break;
          case HOLDING_FAILED:
            holdingFailed += 1;
            break;
          case HOLDING_TIMEDOUT:
            holdingTimedout += 1;
            break;
          case IN_PROGRESS:
            inProgress += 1;
            break;
          case PENDING:
            pending += 1;
            break;
          case QUEUED:
            queued += 1;
            break;
          case TIMEDOUT:
            timedout += 1;
            break;
          case SKIPPED_FAILED:
            skippedFailed += 1;
            break;
          default:
            System.out.println("Unexpected status when creating stage summaries: " + taskStatus);
        }
      }

      HostRoleCommandStatusSummaryDTO stageSummary = new HostRoleCommandStatusSummaryDTO(
          0, 0, 0, stageId, aborted, completed, failed,
          holding, holdingFailed, holdingTimedout, inProgress, pending, queued, timedout,
          skippedFailed);

      summaryMap.put(stageId, stageSummary);
    }

    return summaryMap;
  }

  /**
   * Removes all HostRequest associated with the passed host name from internal collections
   * @param hostName name of the host
   */
  public Set<HostRequest> removeHostRequestByHostName(String hostName) {
    Set<HostRequest> removed = new HashSet<>();
    synchronized (requestsWithReservedHosts) {
      synchronized (outstandingHostRequests) {
        requestsWithReservedHosts.remove(hostName);

        Iterator<HostRequest> hostRequestIterator = outstandingHostRequests.iterator();
        while (hostRequestIterator.hasNext()) {
          HostRequest hostRequest = hostRequestIterator.next();
          if (Objects.equals(hostRequest.getHostName(), hostName)) {
            hostRequestIterator.remove();
            removed.add(hostRequest);
            break;
          }
        }

        //todo: synchronization
        Iterator<HostRequest> allHostRequestIterator = allHostRequests.iterator();
        while (allHostRequestIterator.hasNext()) {
          HostRequest hostRequest = allHostRequestIterator.next();
          if (Objects.equals(hostRequest.getHostName(), hostName)) {
            allHostRequestIterator.remove();
            removed.add(hostRequest);
            break;
          }
        }
      }
    }

    return removed;
  }

  /**
   * @return true if all the tasks in the logical request are in completed state, false otherwise
   */
  public boolean isFinished() {
    for (ShortTaskStatus ts : getRequestStatus().getTasks()) {
      if (!HostRoleStatus.valueOf(ts.getStatus()).isCompletedState()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns if all the tasks in the logical request have completed state.
   */
  public boolean isSuccessful() {
    for (ShortTaskStatus ts : getRequestStatus().getTasks()) {
      if (HostRoleStatus.valueOf(ts.getStatus()) != HostRoleStatus.COMPLETED) {
        return false;
      }
    }
    return true;
  }

  public Optional<String> getFailureReason() {
    for (HostRequest request : getHostRequests()) {
      Optional<String> failureReason = request.getStatusMessage();
      if (failureReason.isPresent()) {
        return failureReason;
      }
    }
    return Optional.absent();
  }

  private void createHostRequests(TopologyRequest request, ClusterTopology topology) {
    Map<String, HostGroupInfo> hostGroupInfoMap = request.getHostGroupInfo();
    Blueprint blueprint = topology.getBlueprint();
    boolean skipFailure = topology.getBlueprint().shouldSkipFailure();
    for (HostGroupInfo hostGroupInfo : hostGroupInfoMap.values()) {
      String groupName = hostGroupInfo.getHostGroupName();
      int hostCardinality = hostGroupInfo.getRequestedHostCount();
      List<String> hostnames = new ArrayList<>(hostGroupInfo.getHostNames());

      for (int i = 0; i < hostCardinality; ++i) {
        if (! hostnames.isEmpty()) {
          // host names are specified
          String hostname = hostnames.get(i);
          HostRequest hostRequest = new HostRequest(getRequestId(), hostIdCounter.getAndIncrement(), getClusterId(),
              hostname, blueprint.getName(), blueprint.getHostGroup(groupName), null, topology, skipFailure);
          synchronized (requestsWithReservedHosts) {
            requestsWithReservedHosts.put(hostname, hostRequest);
          }
        } else {
          // host count is specified
          HostRequest hostRequest = new HostRequest(getRequestId(), hostIdCounter.getAndIncrement(), getClusterId(),
              null, blueprint.getName(), blueprint.getHostGroup(groupName), hostGroupInfo.getPredicate(), topology, skipFailure);
          outstandingHostRequests.add(hostRequest);
        }
      }
    }
    allHostRequests.addAll(outstandingHostRequests);
    allHostRequests.addAll(requestsWithReservedHosts.values());

    LOG.info("LogicalRequest.createHostRequests: all host requests size {} , outstanding requests size = {}",
      allHostRequests.size(), outstandingHostRequests.size());
  }

  private void createHostRequests(ClusterTopology topology,
                                  TopologyLogicalRequestEntity requestEntity) {

    Collection<TopologyHostGroupEntity> hostGroupEntities = requestEntity.getTopologyRequestEntity().getTopologyHostGroupEntities();
    Map<String, Set<String>> allReservedHostNamesByHostGroups = getReservedHostNamesByHostGroupName(hostGroupEntities);
    Map<String, Set<String>> pendingReservedHostNamesByHostGroups = new HashMap<>(allReservedHostNamesByHostGroups);

    for (TopologyHostRequestEntity hostRequestEntity : requestEntity.getTopologyHostRequestEntities()) {
      TopologyHostGroupEntity hostGroupEntity = hostRequestEntity.getTopologyHostGroupEntity();
      String hostGroupName = hostGroupEntity.getName();
      String hostName = hostRequestEntity.getHostName();

      if (hostName != null && pendingReservedHostNamesByHostGroups.containsKey(hostGroupName)) {
        Set<String> pendingReservedHostNamesInHostGroup = pendingReservedHostNamesByHostGroups.get(hostGroupName);

        if (pendingReservedHostNamesInHostGroup != null) {
          pendingReservedHostNamesInHostGroup.remove(hostName);
        }
      }
    }

    boolean skipFailure = topology.getBlueprint().shouldSkipFailure();
    for (TopologyHostRequestEntity hostRequestEntity : requestEntity.getTopologyHostRequestEntities()) {
      Long hostRequestId = hostRequestEntity.getId();
      synchronized (hostIdCounter) {
        if (hostIdCounter.get() <= hostRequestId) {
          hostIdCounter.set(hostRequestId + 1);
        }
      }
      TopologyHostGroupEntity hostGroupEntity = hostRequestEntity.getTopologyHostGroupEntity();
      Set<String> pendingReservedHostsInGroup = pendingReservedHostNamesByHostGroups.get(hostGroupEntity.getName());

      // get next host name from pending host names
      String reservedHostName = Iterables.getFirst(pendingReservedHostsInGroup, null);


      //todo: move predicate processing to host request
      HostRequest hostRequest = new HostRequest(getRequestId(), hostRequestId,
          reservedHostName, topology, hostRequestEntity, skipFailure);

      allHostRequests.add(hostRequest);
      if (! hostRequest.isCompleted()) {
        if (reservedHostName != null) {
          requestsWithReservedHosts.put(reservedHostName, hostRequest);
          pendingReservedHostsInGroup.remove(reservedHostName);
          LOG.info("LogicalRequest.createHostRequests: created new request for a reserved request ID = {} for host name = {}",
            hostRequest.getId(), reservedHostName);
        } else {
          outstandingHostRequests.add(hostRequest);
          LOG.info("LogicalRequest.createHostRequests: created new outstanding host request ID = {}", hostRequest.getId());
        }
      }
    }
  }

  /**
   * Returns a map where the keys are the host group names and the values the
   * collection of FQDNs the hosts in the host group
   * @param hostGroups
   * @return
   */
  private Map<String, Set<String>> getReservedHostNamesByHostGroupName(Collection<TopologyHostGroupEntity> hostGroups) {
    Map<String, Set<String>> reservedHostNamesByHostGroups = new HashMap<>();

    for (TopologyHostGroupEntity hostGroupEntity: hostGroups) {
      String hostGroupName = hostGroupEntity.getName();

      if ( !reservedHostNamesByHostGroups.containsKey(hostGroupName) )
        reservedHostNamesByHostGroups.put(hostGroupName, new HashSet<>());

      for (TopologyHostInfoEntity hostInfoEntity: hostGroupEntity.getTopologyHostInfoEntities()) {
        if (StringUtils.isNotEmpty(hostInfoEntity.getFqdn())) {
          reservedHostNamesByHostGroups.get(hostGroupName).add(hostInfoEntity.getFqdn());
        }
      }
    }
    return reservedHostNamesByHostGroups;
  }

  private synchronized static AmbariManagementController getController() {
    if (controller == null) {
      controller = AmbariServer.getController();
    }
    return controller;
  }

  public CalculatedStatus calculateStatus() {
    return !isFinished()
      ? CalculatedStatus.PENDING
      : isSuccessful()
        ? CalculatedStatus.COMPLETED
        : CalculatedStatus.ABORTED;
  }
}
