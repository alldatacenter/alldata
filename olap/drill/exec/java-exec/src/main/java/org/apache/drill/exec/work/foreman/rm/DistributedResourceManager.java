/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.work.foreman.rm;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.config.ResourcePoolTree;
import org.apache.drill.exec.resourcemgr.config.ResourcePoolTreeImpl;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.work.foreman.Foreman;

public class DistributedResourceManager implements ResourceManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DistributedResourceManager.class);

  private final ResourcePoolTree rmPoolTree;

  private final DrillbitContext context;

  private final DrillConfig rmConfig;

  private final ResourceManager delegatedRM;

  public DistributedResourceManager(DrillbitContext context) throws DrillRuntimeException {
    try {
      this.context = context;
      this.rmConfig = DrillConfig.createForRM();
      rmPoolTree = new ResourcePoolTreeImpl(rmConfig, DrillConfig.getMaxDirectMemory(),
        Runtime.getRuntime().availableProcessors(), 1);
      logger.debug("Successfully parsed RM config \n{}", rmConfig.getConfig(ResourcePoolTreeImpl.ROOT_POOL_CONFIG_KEY));
      this.delegatedRM = new DefaultResourceManager();
    } catch (RMConfigException ex) {
      throw new DrillRuntimeException(String.format("Failed while parsing Drill RM Configs. Drillbit won't be started" +
        " unless config is fixed or RM is disabled by setting %s to false", ExecConstants.RM_ENABLED), ex);
    }
  }
  @Override
  public long memoryPerNode() {
    return delegatedRM.memoryPerNode();
  }

  @Override
  public int cpusPerNode() {
    return delegatedRM.cpusPerNode();
  }

  @Override
  public QueryResourceAllocator newResourceAllocator(QueryContext queryContext) {
    return delegatedRM.newResourceAllocator(queryContext);
  }

  @Override
  public QueryResourceManager newQueryRM(Foreman foreman) {
    return delegatedRM.newQueryRM(foreman);
  }

  public ResourcePoolTree getRmPoolTree() {
    return rmPoolTree;
  }

  @Override
  public void close() {
    delegatedRM.close();
  }
}
