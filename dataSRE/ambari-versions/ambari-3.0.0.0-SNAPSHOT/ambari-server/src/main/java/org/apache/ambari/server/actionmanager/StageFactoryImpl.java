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

package org.apache.ambari.server.actionmanager;

import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Clusters;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;

@Singleton
public class StageFactoryImpl implements StageFactory {
  private Injector injector;

  @Inject
  public StageFactoryImpl(Injector injector) {
    this.injector = injector;
  }

  /**
   * Constructor via the factory.
   * @param requestId Unique identifier for the request
   * @param logDir Directory to log to
   * @param clusterName Cluster name
   * @param clusterId Cluster ID
   * @param requestContext Information about the context of the request
   * @param commandParamsStage Information about the command parameters
   * @param hostParamsStage Information about the host parameters for the stage
   * @return An instance of a Stage with the provided params.
   */
  @Override
  public Stage createNew(long requestId,
                         @Assisted("logDir") String logDir,
                         @Assisted("clusterName") String clusterName,
                         @Assisted("clusterId") long clusterId,
                         @Assisted("requestContext") String requestContext,
                         @Assisted("commandParamsStage") String commandParamsStage,
                         @Assisted("hostParamsStage") String hostParamsStage) {
    return new Stage(requestId, logDir, clusterName, clusterId, requestContext, commandParamsStage, hostParamsStage,
        injector.getInstance(HostRoleCommandFactory.class),
        injector.getInstance(ExecutionCommandWrapperFactory.class));
  }

  /**
   * Constructor via the factory.
   * @param stageEntity Existing stage entity to copy fields form.
   * @return An instance of a Stage that is created using the provided stage as input.
   */
  @Override
  public Stage createExisting(@Assisted StageEntity stageEntity) {
    return new Stage(stageEntity, injector.getInstance(HostRoleCommandDAO.class),
        injector.getInstance(ActionDBAccessor.class), injector.getInstance(Clusters.class),
        injector.getInstance(HostRoleCommandFactory.class),
        injector.getInstance(ExecutionCommandWrapperFactory.class));
  }
}
