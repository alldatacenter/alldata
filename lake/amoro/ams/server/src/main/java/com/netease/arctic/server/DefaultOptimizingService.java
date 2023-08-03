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

package com.netease.arctic.server;

import com.google.common.base.Preconditions;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import com.netease.arctic.ams.api.OptimizingService;
import com.netease.arctic.ams.api.OptimizingTask;
import com.netease.arctic.ams.api.OptimizingTaskId;
import com.netease.arctic.ams.api.OptimizingTaskResult;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.api.resource.Resource;
import com.netease.arctic.ams.api.resource.ResourceGroup;
import com.netease.arctic.ams.api.resource.ResourceManager;
import com.netease.arctic.server.exception.ObjectNotExistsException;
import com.netease.arctic.server.exception.PluginRetryAuthException;
import com.netease.arctic.server.optimizing.OptimizingQueue;
import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.server.persistence.StatedPersistentBase;
import com.netease.arctic.server.persistence.mapper.OptimizerMapper;
import com.netease.arctic.server.persistence.mapper.ResourceMapper;
import com.netease.arctic.server.resource.OptimizerInstance;
import com.netease.arctic.server.resource.OptimizerManager;
import com.netease.arctic.server.table.DefaultTableService;
import com.netease.arctic.server.table.RuntimeHandlerChain;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableConfiguration;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.table.TableRuntimeMeta;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * DefaultOptimizingService is implementing the OptimizerManager Thrift service, which manages the optimization tasks
 * for ArcticTable. It includes methods for authenticating optimizers, polling tasks from the optimizing queue,
 * acknowledging tasks,and completing tasks. The code uses several data structures, including maps for optimizing queues
 * ,task runtimes, and authenticated optimizers.
 * <p>
 * The code also includes a TimerTask for detecting and removing expired optimizers and suspending tasks.
 */
public class DefaultOptimizingService extends StatedPersistentBase implements OptimizingService.Iface,
    OptimizerManager, ResourceManager {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultOptimizingService.class);

  private final long optimizerTouchTimeout;
  private final long taskAckTimeout;
  @StatedPersistentBase.StateField
  private final Map<String, OptimizingQueue> optimizingQueueByGroup = new ConcurrentHashMap<>();
  private final Map<String, OptimizingQueue> optimizingQueueByToken = new ConcurrentHashMap<>();
  private final DefaultTableService tableManager;
  private final RuntimeHandlerChain tableHandlerChain;
  private Timer optimizerMonitorTimer;

  public DefaultOptimizingService(Configurations serviceConfig, DefaultTableService tableService) {
    this.optimizerTouchTimeout = serviceConfig.getLong(ArcticManagementConf.OPTIMIZER_HB_TIMEOUT);
    this.taskAckTimeout = serviceConfig.getLong(ArcticManagementConf.OPTIMIZER_TASK_ACK_TIMEOUT);
    this.tableManager = tableService;
    this.tableHandlerChain = new TableRuntimeHandlerImpl();
  }

  public RuntimeHandlerChain getTableRuntimeHandler() {
    return tableHandlerChain;
  }

  //TODO optimizing code
  public void loadOptimizingQueues(List<TableRuntimeMeta> tableRuntimeMetaList) {
    List<ResourceGroup> optimizerGroups = getAs(ResourceMapper.class, ResourceMapper::selectResourceGroups);
    List<OptimizerInstance> optimizers = getAs(OptimizerMapper.class, OptimizerMapper::selectAll);
    Map<String, List<OptimizerInstance>> optimizersByGroup =
        optimizers.stream().collect(Collectors.groupingBy(OptimizerInstance::getGroupName));
    Map<String, List<TableRuntimeMeta>> groupToTableRuntimes = tableRuntimeMetaList.stream()
        .collect(Collectors.groupingBy(TableRuntimeMeta::getOptimizerGroup));
    optimizerGroups.forEach(group -> {
      String groupName = group.getName();
      List<TableRuntimeMeta> tableRuntimeMetas = groupToTableRuntimes.remove(groupName);
      List<OptimizerInstance> optimizersUnderGroup = optimizersByGroup.get(groupName);
      OptimizingQueue optimizingQueue = new OptimizingQueue(tableManager, group,
          Optional.ofNullable(tableRuntimeMetas).orElseGet(ArrayList::new),
          Optional.ofNullable(optimizersUnderGroup).orElseGet(ArrayList::new),
          optimizerTouchTimeout, taskAckTimeout);
      optimizingQueueByGroup.put(groupName, optimizingQueue);
      if (CollectionUtils.isNotEmpty(optimizersUnderGroup)) {
        optimizersUnderGroup.forEach(optimizer -> optimizingQueueByToken.put(optimizer.getToken(), optimizingQueue));
      }
    });
    groupToTableRuntimes.keySet().forEach(groupName -> LOG.warn("Unloaded task runtime in group " + groupName));
  }

  @Override
  public void ping() {
  }

  @Override
  public void touch(String authToken) {
    LOG.debug("Optimizer {} touching", authToken);
    OptimizingQueue queue = getQueueByToken(authToken);
    queue.touch(authToken);
  }

  @Override
  public OptimizingTask pollTask(String authToken, int threadId) {
    OptimizingQueue queue = getQueueByToken(authToken);
    OptimizingTask task = queue.pollTask(authToken, threadId);
    if (task != null) {
      LOG.info("Optimizer {} polling task {}", authToken, task.getTaskId());
    }
    return task;
  }

  @Override
  public void ackTask(String authToken, int threadId, OptimizingTaskId taskId) {
    LOG.info("Ack task {} by optimizer {}.", taskId, authToken);
    OptimizingQueue queue = getQueueByToken(authToken);
    queue.ackTask(authToken, threadId, taskId);
  }

  @Override
  public void completeTask(String authToken, OptimizingTaskResult taskResult) {
    LOG.info("Optimizer {} complete task {}", authToken, taskResult.getTaskId());
    OptimizingQueue queue = getQueueByToken(authToken);
    queue.completeTask(authToken, taskResult);
  }

  @Override
  public String authenticate(OptimizerRegisterInfo registerInfo) {
    LOG.info("Register optimizer {}.", registerInfo);
    OptimizingQueue queue = getQueueByGroup(registerInfo.getGroupName());
    String token = queue.authenticate(registerInfo);
    optimizingQueueByToken.put(token, queue);
    return token;
  }

  /**
   * Get optimizing queue.
   *
   * @return OptimizeQueueItem
   */
  private OptimizingQueue getQueueByGroup(String optimizerGroup) {
    return getOptionalQueueByGroup(optimizerGroup)
        .orElseThrow(() -> new ObjectNotExistsException("Optimizer group " + optimizerGroup));
  }

  private Optional<OptimizingQueue> getOptionalQueueByGroup(String optimizerGroup) {
    Preconditions.checkArgument(
        optimizerGroup != null,
        "optimizerGroup can not be null");
    return Optional.ofNullable(optimizingQueueByGroup.get(optimizerGroup));
  }

  private OptimizingQueue getQueueByToken(String token) {
    Preconditions.checkArgument(
        token != null,
        "optimizer token can not be null");
    return Optional.ofNullable(optimizingQueueByToken.get(token))
        .orElseThrow(() -> new PluginRetryAuthException("Optimizer has not been authenticated"));
  }

  @Override
  public List<OptimizerInstance> listOptimizers() {
    return optimizingQueueByGroup.values()
        .stream()
        .flatMap(queue -> queue.getOptimizers().stream())
        .collect(Collectors.toList());
  }

  @Override
  public List<OptimizerInstance> listOptimizers(String group) {
    return getQueueByGroup(group).getOptimizers();
  }

  @Override
  public void deleteOptimizer(String group, String resourceId) {
    getQueueByGroup(group).removeOptimizer(resourceId);
    List<OptimizerInstance> deleteOptimizers =
        getAs(OptimizerMapper.class, mapper -> mapper.selectByResourceId(resourceId));
    deleteOptimizers.forEach(optimizer -> {
      String token = optimizer.getToken();
      optimizingQueueByToken.remove(token);
      doAs(OptimizerMapper.class, mapper -> mapper.deleteOptimizer(token));
    });
  }

  @Override
  public void createResourceGroup(ResourceGroup resourceGroup) {
    invokeConsisitency(() ->
        doAsTransaction(() -> {
          doAs(ResourceMapper.class, mapper -> mapper.insertResourceGroup(resourceGroup));
          String groupName = resourceGroup.getName();
          OptimizingQueue optimizingQueue = new OptimizingQueue(
              tableManager,
              resourceGroup,
              new ArrayList<>(),
              new ArrayList<>(),
              optimizerTouchTimeout,
              taskAckTimeout);
          optimizingQueueByGroup.put(groupName, optimizingQueue);
        })
    );
  }

  @Override
  public void deleteResourceGroup(String groupName) {
    if (canDeleteResourceGroup(groupName)) {
      invokeConsisitency(() -> {
        optimizingQueueByGroup.remove(groupName);
        doAs(ResourceMapper.class, mapper -> mapper.deleteResourceGroup(groupName));
      });
    } else {
      throw new RuntimeException(String.format("The resource group %s cannot be deleted because it is currently in " +
          "use.", groupName));
    }
  }

  @Override
  public void updateResourceGroup(ResourceGroup resourceGroup) {
    Preconditions.checkNotNull(resourceGroup, "The resource group cannot be null.");
    Optional.ofNullable(optimizingQueueByGroup.get(resourceGroup.getName()))
        .ifPresent(queue -> queue.updateOptimizerGroup(resourceGroup));
    doAs(ResourceMapper.class, mapper -> mapper.updateResourceGroup(resourceGroup));
  }

  @Override
  public void createResource(Resource resource) {
    doAs(ResourceMapper.class, mapper -> mapper.insertResource(resource));
  }

  @Override
  public void deleteResource(String resourceId) {
    doAs(ResourceMapper.class, mapper -> mapper.deleteResource(resourceId));
  }

  @Override
  public List<ResourceGroup> listResourceGroups() {
    return getAs(ResourceMapper.class, ResourceMapper::selectResourceGroups);
  }

  @Override
  public List<ResourceGroup> listResourceGroups(String containerName) {
    return getAs(ResourceMapper.class, ResourceMapper::selectResourceGroups).stream()
        .filter(group -> group.getContainer().equals(containerName))
        .collect(Collectors.toList());
  }

  @Override
  public ResourceGroup getResourceGroup(String groupName) {
    return getAs(ResourceMapper.class, mapper -> mapper.selectResourceGroup(groupName));
  }

  @Override
  public List<Resource> listResourcesByGroup(String groupName) {
    return getAs(ResourceMapper.class, mapper -> mapper.selectResourcesByGroup(groupName));
  }

  @Override
  public Resource getResource(String resourceId) {
    return getAs(ResourceMapper.class, mapper -> mapper.selectResource(resourceId));
  }

  public boolean canDeleteResourceGroup(String name) {
    for (CatalogMeta catalogMeta : tableManager.listCatalogMetas()) {
      if (catalogMeta.getCatalogProperties() != null &&
          catalogMeta.getCatalogProperties()
              .getOrDefault(
                  CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.SELF_OPTIMIZING_GROUP,
                  TableProperties.SELF_OPTIMIZING_GROUP_DEFAULT)
              .equals(name)) {
        return false;
      }
    }
    for (OptimizerInstance optimizer : listOptimizers()) {
      if (optimizer.getGroupName().equals(name)) {
        return false;
      }
    }
    for (ServerTableIdentifier identifier : tableManager.listManagedTables()) {
      if (optimizingQueueByGroup.containsKey(name) && optimizingQueueByGroup.get(name).containsTable(identifier)) {
        return false;
      }
    }
    return true;
  }

  private class TableRuntimeHandlerImpl extends RuntimeHandlerChain {

    @Override
    public void handleStatusChanged(TableRuntime tableRuntime, OptimizingStatus originalStatus) {
      if (!tableRuntime.getOptimizingStatus().isProcessing()) {
        getOptionalQueueByGroup(tableRuntime.getOptimizerGroup()).ifPresent(q -> q.refreshTable(tableRuntime));
      }
    }

    @Override
    public void handleConfigChanged(TableRuntime tableRuntime, TableConfiguration originalConfig) {
      String originalGroup = originalConfig.getOptimizingConfig().getOptimizerGroup();
      if (!tableRuntime.getOptimizerGroup().equals(originalGroup)) {
        getOptionalQueueByGroup(originalGroup).ifPresent(q -> q.releaseTable(tableRuntime));
      }
      getOptionalQueueByGroup(tableRuntime.getOptimizerGroup()).ifPresent(q -> q.refreshTable(tableRuntime));
    }

    @Override
    public void handleTableAdded(ArcticTable table, TableRuntime tableRuntime) {
      getOptionalQueueByGroup(tableRuntime.getOptimizerGroup()).ifPresent(q -> q.refreshTable(tableRuntime));
    }

    @Override
    public void handleTableRemoved(TableRuntime tableRuntime) {
      getOptionalQueueByGroup(tableRuntime.getOptimizerGroup()).ifPresent(queue -> queue.releaseTable(tableRuntime));
    }

    @Override
    protected void initHandler(List<TableRuntimeMeta> tableRuntimeMetaList) {
      LOG.info("OptimizerManagementService begin initializing");
      loadOptimizingQueues(tableRuntimeMetaList);
      optimizerMonitorTimer = new Timer("OptimizerMonitor", true);
      optimizerMonitorTimer.schedule(
          new SuspendingDetector(),
          optimizerTouchTimeout,
          ArcticServiceConstants.OPTIMIZER_CHECK_INTERVAL);
      LOG.info("init SuspendingDetector for Optimizer with delay {} ms, interval {} ms", optimizerTouchTimeout,
          ArcticServiceConstants.OPTIMIZER_CHECK_INTERVAL);
      LOG.info("OptimizerManagementService initializing has completed");
    }

    @Override
    protected void doDispose() {
      optimizerMonitorTimer.cancel();
    }
  }

  private class SuspendingDetector extends TimerTask {

    @Override
    public void run() {
      try {
        optimizingQueueByGroup.values().forEach(optimizingQueue -> {
          List<String> expiredOptimizers = optimizingQueue.checkSuspending();
          expiredOptimizers.forEach(optimizerToken -> {
            doAs(OptimizerMapper.class, mapper -> mapper.deleteOptimizer(optimizerToken));
            optimizingQueueByToken.remove(optimizerToken);
          });
        });
      } catch (RuntimeException e) {
        LOG.error("Update optimizer status abnormal failed. try next round", e);
      }
    }
  }
}
